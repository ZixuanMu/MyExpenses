package org.totschnig.myexpenses.model

import android.content.Context
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.LocalDateFormatter
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.TextUtils
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.viewmodel.data.DateInfo
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale
import kotlin.math.min

/**
 * grouping of transactions
 */
enum class Grouping {
    NONE,
    DAY,
    WEEK,
    MONTH {
        override val minValue = 0
    },
    YEAR;

    open val minValue = 1

    fun calculateGroupId(year: Int, second: Int) = if (this == NONE) 1 else groupId(year, second)

    /**
     * @param groupYear           the year of the group to display
     * @param groupSecond         the number of the group in the second dimension (day, week or month)
     * @param dateInfo            information about the current date
     * @return a human readable String representing the group as header or activity title
     */
    fun getDisplayTitle(
        ctx: Context,
        groupYear: Int,
        groupSecond: Int,
        dateInfo: DateInfo,
    ): String {
        val locale = ctx.resources.configuration.locale
        return try {
            when (this) {
                NONE -> ctx.getString(R.string.menu_aggregates)
                DAY -> {
                    val today = LocalDate.ofYearDay(dateInfo.thisYear, dateInfo.thisDay)
                    val day = LocalDate.ofYearDay(groupYear, groupSecond)
                    val title = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale).format(day)
                    when(ChronoUnit.DAYS.between(day, today)) {
                        1L -> R.string.yesterday
                        0L -> R.string.today
                        -1L -> R.string.tomorrow
                        else -> null
                    }?.let { ctx.getString(it) + " (" + title + ")" } ?: title
                }

                WEEK -> {
                    val thisWeek = dateInfo.thisWeek
                    val thisYearOfWeekStart = dateInfo.thisYearOfWeekStart
                    val dateFormat = Utils.localizedYearLessDateFormat(ctx)
                    val weekRange = (" (" + Utils.convDateTime(
                        dateInfo.weekStart,
                        dateFormat
                    )
                            + " - " + Utils.convDateTime(
                        dateInfo.weekEnd,
                        dateFormat
                    ) + " )")
                    val yearPrefix = if (groupYear == thisYearOfWeekStart) {
                        if (groupSecond == thisWeek) return ctx.getString(R.string.grouping_this_week) + weekRange else if (groupSecond == thisWeek - 1) return ctx.getString(
                            R.string.grouping_last_week
                        ) + weekRange
                        ""
                    } else "$groupYear, "
                    yearPrefix + ctx.getString(R.string.grouping_week) + " " + groupSecond + weekRange
                }

                MONTH -> {
                    getDisplayTitleForMonth(
                        groupYear,
                        groupSecond,
                        FormatStyle.LONG,
                        locale,
                        ctx.injector.prefHandler().monthStart
                    )
                }

                YEAR -> groupYear.toString()
            }
        } catch (e: Exception) {
            "Error while generating title: ${e.safeMessage}"
        }
    }

    companion object {

        fun groupId(year: Int, second: Int) = year * 1000 + second

        fun getMonthRange(groupYear: Int, groupSecond: Int, monthStarts: Int): Pair<LocalDate, LocalDate> {
            val startMonth = groupSecond + 1
            val yearMonth = YearMonth.of(groupYear, startMonth)
            return if (monthStarts == 1) {
               yearMonth.atDay(1) to yearMonth.atEndOfMonth()
            } else {
                val nextMonth = yearMonth.plusMonths(1)
                (if (monthStarts > yearMonth.lengthOfMonth())
                    nextMonth.atDay(1) else yearMonth.atDay(monthStarts)) to
                nextMonth.atDay(min(monthStarts - 1,nextMonth.lengthOfMonth()))
            }
        }

        fun getDisplayTitleForMonth(
            groupYear: Int,
            groupSecond: Int,
            style: FormatStyle,
            userPreferredLocale: Locale,
            monthStarts: Int
        ): String {
            return if (monthStarts == 1) {
                DateTimeFormatter.ofPattern("MMMM y").format(YearMonth.of(groupYear, groupSecond +1))
            } else {
                val dateFormat = DateTimeFormatter.ofLocalizedDate(style).withLocale(userPreferredLocale)
                val range = getMonthRange(groupYear, groupSecond, monthStarts)
                "(${dateFormat.format(range.first)} - ${dateFormat.format(range.second)})"
            }
        }

        @JvmField
        val JOIN: String = TextUtils.joinEnum(Grouping::class.java)
    }
}