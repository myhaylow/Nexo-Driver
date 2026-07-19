package com.myhaylow.nexodriver.profile

import android.content.Context
import com.myhaylow.nexodriver.finance.OperatingCosts
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

class ProfileRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("nexo_local_settings", Context.MODE_PRIVATE)

    fun profiles(): List<DriverProfile> {
        val stored = prefs.getStringSet("profiles", null)?.mapNotNull(::decodeProfile).orEmpty()
        return stored.ifEmpty { listOf(DEFAULT, DESTINATION) }.sortedBy { it.name.lowercase() }
    }

    fun save(profile: DriverProfile) {
        val updated = profiles().filterNot { it.id == profile.id } + profile
        prefs.edit().putStringSet("profiles", updated.map(::encodeProfile).toSet()).apply()
    }

    fun duplicate(id: String): DriverProfile? = profiles().firstOrNull { it.id == id }?.let {
        it.copy(id = UUID.randomUUID().toString(), name = "${it.name} (cópia)").also(::save)
    }

    fun delete(id: String): Boolean {
        if (id == DEFAULT.id || id == DESTINATION.id) return false
        val remaining = profiles().filterNot { it.id == id }
        prefs.edit().putStringSet("profiles", remaining.map(::encodeProfile).toSet())
            .putString("active_profile", if (activeProfileId() == id) DEFAULT.id else activeProfileId()).apply()
        return true
    }

    fun activeProfileId() = prefs.getString("active_profile", DEFAULT.id) ?: DEFAULT.id
    fun activate(id: String) { if (profiles().any { it.id == id }) prefs.edit().putString("active_profile", id).apply() }
    fun automaticEnabled() = prefs.getBoolean("schedule_enabled", false)
    fun setAutomaticEnabled(value: Boolean) { prefs.edit().putBoolean("schedule_enabled", value).apply() }

    fun schedules(): List<ProfileSchedule> = prefs.getStringSet("schedules", emptySet()).orEmpty().mapNotNull(::decodeSchedule)
    fun saveSchedule(value: ProfileSchedule) {
        val updated = schedules().filterNot { it.id == value.id } + value
        prefs.edit().putStringSet("schedules", updated.map(::encodeSchedule).toSet()).apply()
    }

    fun deleteSchedule(id: String) = prefs.edit().putStringSet("schedules", schedules().filterNot { it.id == id }.map(::encodeSchedule).toSet()).apply()

    fun costs() = OperatingCosts(
        number("fuel"), number("consumption"), number("maintenance"), number("tires"),
        number("insurance"), number("ipva"), number("other"), number("monthly_km"),
    )

    fun saveCosts(value: OperatingCosts) {
        val editor = prefs.edit()
        mapOf("fuel" to value.fuelPricePerLiter, "consumption" to value.consumptionKmPerLiter,
            "maintenance" to value.maintenancePerKm, "tires" to value.tiresPerKm,
            "insurance" to value.insuranceMonthly, "ipva" to value.ipvaYearly,
            "other" to value.otherMonthly, "monthly_km" to value.expectedKmMonthly).forEach { (key, number) ->
            if (number == null) editor.remove(key) else editor.putString(key, number.toString())
        }
        editor.apply()
    }

    private fun number(key: String) = prefs.getString(key, null)?.toDoubleOrNull()
    private fun encodeProfile(p: DriverProfile) = listOf(p.id, p.name, p.minPerKm, p.targetPerHour,
        p.maxPickupDistanceKm, p.maxPickupMinutes, p.destinationMode).joinToString(SEPARATOR)
    private fun decodeProfile(raw: String): DriverProfile? = runCatching {
        val v = raw.split(SEPARATOR); DriverProfile(v[0], v[1], v[2].toDouble(), v[3].toDouble(), v[4].toDouble(), v[5].toDouble(), v[6].toBooleanStrict())
    }.getOrNull()
    private fun encodeSchedule(s: ProfileSchedule) = listOf(s.id, s.profileId, s.days.joinToString(",") { it.value.toString() },
        s.start, s.end, s.priority, s.enabled).joinToString(SEPARATOR)
    private fun decodeSchedule(raw: String): ProfileSchedule? = runCatching {
        val v = raw.split(SEPARATOR); ProfileSchedule(v[0], v[1], v[2].split(',').map { DayOfWeek.of(it.toInt()) }.toSet(),
            LocalTime.parse(v[3]), LocalTime.parse(v[4]), v[5].toInt(), v[6].toBooleanStrict())
    }.getOrNull()

    companion object {
        private const val SEPARATOR = "\u001f"
        val DEFAULT = DriverProfile("default", "Perfil padrão confirmado")
        val DESTINATION = DriverProfile("destination", "Destino próprio", 1.60, 36.0, 3.5, 8.0, true)
    }
}
