package org.totschnig.myexpenses.test.provider

import android.content.ContentUris
import android.content.ContentValues
import org.totschnig.myexpenses.db2.budgetAllocationQueryUri
import org.totschnig.myexpenses.db2.budgetAllocationUri
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET_ROLLOVER_PREVIOUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SECOND_GROUP
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_YEAR
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.testutils.BaseDbTest
import org.totschnig.shared_test.CursorSubject

class BudgetTest : BaseDbTest() {

    private fun insertOneTimeBudget() = ContentUris.parseId(
        mockContentResolver.insert(
            TransactionProvider.BUDGETS_URI,
            BudgetInfo(
                setupTestAccount(),
                "budget 1",
                "description",
                400,
                Grouping.NONE,
                "2023-12-01",
                "2023-12-18"
            ).contentValues,
        )!!
    )

    private fun insertMonthlyBudget() = ContentUris.parseId(
        mockContentResolver.insert(
            TransactionProvider.BUDGETS_URI,
            BudgetInfo(
                setupTestAccount(),
                "budget 1",
                "description",
                400,
                Grouping.MONTH
            ).contentValues,
        )!!
    )

    private fun assertBudgetAmount(
        budgetId: Long,
        budgetAmount: Long,
        rollOver: Long = 0,
        grouping: Grouping = Grouping.NONE,
        year: Int? = null,
        second: Int? = null
    ) {
        mockContentResolver.query(
            budgetAllocationQueryUri(budgetId, 0, grouping, year?.toString(), second?.toString()),
            null,
            null,
            null,
            null
        )!!.use {
            with(CursorSubject.assertThat(it)) {
                movesToFirst()
                hasLong(KEY_BUDGET, budgetAmount)
                hasLong(KEY_BUDGET_ROLLOVER_PREVIOUS, rollOver)
            }
        }
    }

    fun testUpdateOfOneTimeBudget() {
        val budgetId = insertOneTimeBudget()
        assertBudgetAmount(budgetId, 400)
        mockContentResolver.update(
            budgetAllocationUri(budgetId, 0),
            ContentValues(1).apply {
                put(KEY_BUDGET, 500)
            }, null, null
        )
        assertBudgetAmount(budgetId, 500)
    }

    fun testUpdateOfRepeatingBudget() {
        val budgetId = insertMonthlyBudget()
        assertBudgetAmount(budgetId, 400, grouping = Grouping.MONTH, year = 2023, second = 11)
        mockContentResolver.update(
            budgetAllocationUri(budgetId, 0),
            ContentValues(3).apply {
                put(KEY_YEAR, 2023)
                put(KEY_SECOND_GROUP, 12)
                put(KEY_BUDGET, 500)
            }, null, null
        )
        assertBudgetAmount(budgetId, 400, grouping = Grouping.MONTH, year = 2023, second = 11)
        assertBudgetAmount(budgetId, 500, grouping = Grouping.MONTH, year = 2023, second = 12)
        mockContentResolver.update(
            budgetAllocationUri(budgetId, 0),
            ContentValues(3).apply {
                put(KEY_YEAR, 2023)
                put(KEY_SECOND_GROUP, 12)
                put(KEY_BUDGET_ROLLOVER_PREVIOUS, 50)
            }, null, null
        )
        assertBudgetAmount(budgetId, 500, rollOver = 50, grouping = Grouping.MONTH, year = 2023, second = 12)
        mockContentResolver.update(
            budgetAllocationUri(budgetId, 0),
            ContentValues(3).apply {
                put(KEY_YEAR, 2023)
                put(KEY_SECOND_GROUP, 12)
                put(KEY_BUDGET, 600)
            }, null, null
        )
        assertBudgetAmount(budgetId, 600, rollOver = 50, grouping = Grouping.MONTH, year = 2023, second = 12)
    }
}