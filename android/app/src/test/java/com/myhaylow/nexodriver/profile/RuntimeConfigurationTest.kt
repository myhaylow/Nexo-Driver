package com.myhaylow.nexodriver.profile

import com.myhaylow.nexodriver.finance.OperatingCosts
import com.myhaylow.nexodriver.uber.UberOfferAccessibilityService
import java.time.DayOfWeek.MONDAY
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RuntimeConfigurationTest {
    private var service: UberOfferAccessibilityService? = null

    @After fun cleanUp() {
        service?.onDestroy()
        RuntimeConfiguration.resetForTest()
        RuntimeEnvironment.getApplication().getSharedPreferences("nexo_local_settings", 0).edit().clear().commit()
    }

    @Test fun `service started before activity loads persisted profiles schedules and costs`() {
        val repository = ProfileRepository(RuntimeEnvironment.getApplication())
        val persisted = DriverProfile("persisted", "Perfil persistido", minPerKm = 2.25)
        repository.save(persisted)
        repository.saveSchedule(ProfileSchedule("morning", persisted.id, setOf(MONDAY), LocalTime.of(8, 0), LocalTime.of(12, 0)))
        repository.setAutomaticEnabled(true)
        val costs = OperatingCosts(fuelPricePerLiter = 6.0)
        repository.saveCosts(costs)

        // Simulate a fresh process in which Android restores the service before creating an Activity.
        RuntimeConfiguration.resetForTest()
        service = Robolectric.buildService(UberOfferAccessibilityService::class.java).create().get()
        service!!.javaClass.getDeclaredMethod("onServiceConnected").apply { isAccessible = true }.invoke(service)
        val configured = RuntimeConfiguration.current(LocalDateTime.parse("2026-07-20T09:00"))!!

        assertEquals(persisted, configured.first.profile)
        assertTrue(configured.first.automatic)
        assertEquals(costs, configured.second)
    }
}
