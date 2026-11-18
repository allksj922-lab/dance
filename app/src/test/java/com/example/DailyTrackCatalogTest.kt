package com.example

import com.example.alarm.AlarmScheduler
import com.example.model.DailyTrackCatalog
import com.example.model.DanceDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DailyTrackCatalogTest {

    @Test
    fun `consecutive days generate distinct dance tracks and genres`() {
        val start = DanceDate(2026, 11, 18)
        val lineup = DailyTrackCatalog.getLineupAround(centerDate = start, daysBefore = 1, daysAfter = 10)
        assertEquals(12, lineup.size)

        for (i in 0 until lineup.size - 1) {
            val current = lineup[i]
            val next = lineup[i + 1]
            assertNotEquals("Track ID should differ daily", current.id, next.id)
            assertNotEquals("Genre should rotate daily", current.genre, next.genre)
        }
    }

    @Test
    fun `danceDate epochDay round trip is exact across leap years`() {
        val dates = listOf(
            DanceDate(2024, 2, 29),
            DanceDate(2026, 1, 1),
            DanceDate(2026, 11, 18),
            DanceDate(2026, 12, 31)
        )
        dates.forEach { d ->
            val roundTrip = DanceDate.fromEpochDay(d.toEpochDay())
            assertEquals(d, roundTrip)
        }
    }

    @Test
    fun `computeNextTriggerMillis returns future timestamp on enabled weekday`() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 18, 8, 0, 0) // Wednesday 08:00
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Only Thursdays (index 3) at 07:00
        val nextTrigger = AlarmScheduler.computeNextTriggerMillis(
            hour = 7,
            minute = 0,
            activeDays = setOf(3),
            nowMillis = now
        )
        assertTrue(nextTrigger > now)
        val cal = Calendar.getInstance().apply { timeInMillis = nextTrigger }
        assertEquals(Calendar.THURSDAY, cal.get(Calendar.DAY_OF_WEEK))
        assertEquals(7, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
    }
}
