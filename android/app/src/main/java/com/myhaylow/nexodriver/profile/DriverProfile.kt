package com.myhaylow.nexodriver.profile

import com.myhaylow.nexodriver.decision.DecisionProfile
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

data class DriverProfile(
    val id: String,
    val name: String,
    val minPerKm: Double = 1.80,
    val targetPerHour: Double = 40.0,
    val maxPickupDistanceKm: Double = 3.5,
    val maxPickupMinutes: Double = 8.0,
    val destinationMode: Boolean = false,
) {
    init {
        require(id.isNotBlank() && name.isNotBlank())
        require(listOf(minPerKm, targetPerHour, maxPickupDistanceKm, maxPickupMinutes).all { it.isFinite() && it >= 0 })
    }

    fun decisionProfile() = DecisionProfile(id, name, minPerKm, targetPerHour, maxPickupDistanceKm, maxPickupMinutes)
}

data class ProfileSchedule(
    val id: String,
    val profileId: String,
    val days: Set<DayOfWeek>,
    val start: LocalTime,
    val end: LocalTime,
    val priority: Int = 0,
    val enabled: Boolean = true,
) {
    init { require(id.isNotBlank() && profileId.isNotBlank() && days.isNotEmpty()) }

    fun contains(moment: LocalDateTime): Boolean {
        if (!enabled || start == end) return false
        val time = moment.toLocalTime()
        return if (start < end) {
            moment.dayOfWeek in days && time >= start && time < end
        } else {
            (moment.dayOfWeek in days && time >= start) ||
                (moment.dayOfWeek.minusOne() in days && time < end)
        }
    }

    private fun DayOfWeek.minusOne() = DayOfWeek.of(if (value == 1) 7 else value - 1)
}

data class ProfileSelection(val profile: DriverProfile, val automatic: Boolean)

object ProfileScheduler {
    /** Highest priority wins; equal priorities use schedule id for stable, deterministic selection. */
    fun resolve(
        profiles: List<DriverProfile>, schedules: List<ProfileSchedule>, manualProfileId: String,
        automaticEnabled: Boolean, moment: LocalDateTime,
    ): ProfileSelection {
        require(profiles.isNotEmpty())
        val manual = profiles.firstOrNull { it.id == manualProfileId } ?: profiles.first()
        if (!automaticEnabled) return ProfileSelection(manual, false)
        val selected = schedules.asSequence().filter { it.contains(moment) }
            .sortedWith(compareByDescending<ProfileSchedule> { it.priority }.thenBy { it.id })
            .mapNotNull { schedule -> profiles.firstOrNull { it.id == schedule.profileId } }
            .firstOrNull()
        return selected?.let { ProfileSelection(it, true) } ?: ProfileSelection(manual, false)
    }
}

