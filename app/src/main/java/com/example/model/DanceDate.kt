package com.example.model

import java.util.Calendar

/**
 * API-24-compatible calendar date representation for deterministic daily dance track scheduling.
 */
data class DanceDate(
    val year: Int,
    val month: Int, // 1..12
    val dayOfMonth: Int // 1..31
) : Comparable<DanceDate> {

    /**
     * Exact days since 1970-01-01 using Howard Hinnant's civil calendar algorithm.
     */
    fun toEpochDay(): Long {
        val y = if (month <= 2) year - 1L else year.toLong()
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = (y - era * 400).toInt()
        val m = month
        val doy = (153 * (m + (if (m > 2) -3 else 9)) + 2) / 5 + dayOfMonth - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097 + doe - 719468
    }

    fun plusDays(days: Long): DanceDate = fromEpochDay(toEpochDay() + days)

    fun dayOfWeekIndex(): Int {
        // 1970-01-01 was Thursday (index 3 if Monday=0..Sunday=6)
        val mod = ((toEpochDay() + 3) % 7 + 7) % 7
        return mod.toInt() // 0=Mon, 1=Tue, 2=Wed, 3=Thu, 4=Fri, 5=Sat, 6=Sun
    }

    fun dayOfWeekKoreanShort(): String = when (dayOfWeekIndex()) {
        0 -> "월"
        1 -> "화"
        2 -> "수"
        3 -> "목"
        4 -> "금"
        5 -> "토"
        else -> "일"
    }

    fun dayOfWeekKoreanFull(): String = "${dayOfWeekKoreanShort()}요일"

    fun dayOfYear(): Int {
        val startOfYear = DanceDate(year, 1, 1).toEpochDay()
        return (toEpochDay() - startOfYear).toInt() + 1
    }

    fun formattedKorean(): String = "${month}월 ${dayOfMonth}일 (${dayOfWeekKoreanShort()})"

    fun formattedShort(): String = "${month}/${dayOfMonth} ${dayOfWeekKoreanShort()}"

    override fun compareTo(other: DanceDate): Int = toEpochDay().compareTo(other.toEpochDay())

    companion object {
        fun today(): DanceDate {
            val cal = Calendar.getInstance()
            return DanceDate(
                year = cal.get(Calendar.YEAR),
                month = cal.get(Calendar.MONTH) + 1,
                dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            )
        }

        fun fromEpochDay(epochDay: Long): DanceDate {
            val z = epochDay + 719468
            val era = (if (z >= 0) z else z - 146096) / 146097
            val doe = (z - era * 146097).toInt()
            val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
            val y = yoe + era * 400
            val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
            val mp = (5 * doy + 2) / 153
            val d = doy - (153 * mp + 2) / 5 + 1
            val m = mp + (if (mp < 10) 3 else -9)
            val finalYear = (y + (if (m <= 2) 1 else 0)).toInt()
            return DanceDate(finalYear, m, d)
        }
    }
}
