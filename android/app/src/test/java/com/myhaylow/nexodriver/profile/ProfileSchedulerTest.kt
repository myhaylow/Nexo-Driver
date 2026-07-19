package com.myhaylow.nexodriver.profile

import java.time.DayOfWeek.*
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test

class ProfileSchedulerTest {
    private val manual = DriverProfile("manual", "Manual")
    private val work = DriverProfile("work", "Trabalho")

    @Test fun `disabled schedule falls back to manual selection`() {
        val result = ProfileScheduler.resolve(listOf(manual, work), emptyList(), "manual", false, LocalDateTime.parse("2026-07-20T09:00"))
        assertEquals(manual, result.profile); assertFalse(result.automatic)
    }

    @Test fun `overnight range includes next day until exclusive end`() {
        val range = ProfileSchedule("night", "work", setOf(MONDAY), LocalTime.of(22, 0), LocalTime.of(2, 0))
        assertTrue(range.contains(LocalDateTime.parse("2026-07-20T23:30")))
        assertTrue(range.contains(LocalDateTime.parse("2026-07-21T01:59")))
        assertFalse(range.contains(LocalDateTime.parse("2026-07-21T02:00")))
    }

    @Test fun `range is active only on selected weekdays`() {
        val range = ProfileSchedule("weekday", "work", setOf(WEDNESDAY), LocalTime.of(8, 0), LocalTime.of(12, 0))
        assertTrue(range.contains(LocalDateTime.parse("2026-07-22T09:00")))
        assertFalse(range.contains(LocalDateTime.parse("2026-07-23T09:00")))
    }

    @Test fun `sunday overnight range continues into monday`() {
        val range = ProfileSchedule("sunday-night", "work", setOf(SUNDAY), LocalTime.of(22, 0), LocalTime.of(2, 0))
        assertTrue(range.contains(LocalDateTime.parse("2026-07-19T23:30")))
        assertTrue(range.contains(LocalDateTime.parse("2026-07-20T01:59")))
        assertFalse(range.contains(LocalDateTime.parse("2026-07-20T02:00")))
    }

    @Test fun `overlap selects higher priority even when its id sorts later`() {
        val schedules = listOf(
            ProfileSchedule("a", "manual", setOf(MONDAY), LocalTime.of(8,0), LocalTime.of(18,0), 2),
            ProfileSchedule("z", "work", setOf(MONDAY), LocalTime.of(8,0), LocalTime.of(18,0), 3),
        )
        val result = ProfileScheduler.resolve(listOf(manual, work), schedules, "manual", true, LocalDateTime.parse("2026-07-20T10:00"))
        assertEquals("work", result.profile.id); assertTrue(result.automatic)
    }

    @Test fun `equal priority overlap uses lowest schedule id`() {
        val schedules = listOf(
            ProfileSchedule("b", "manual", setOf(MONDAY), LocalTime.of(8,0), LocalTime.of(18,0), 3),
            ProfileSchedule("a", "work", setOf(MONDAY), LocalTime.of(8,0), LocalTime.of(18,0), 3),
        )
        val result = ProfileScheduler.resolve(listOf(manual, work), schedules, "manual", true, LocalDateTime.parse("2026-07-20T10:00"))
        assertEquals("work", result.profile.id); assertTrue(result.automatic)
    }
}
