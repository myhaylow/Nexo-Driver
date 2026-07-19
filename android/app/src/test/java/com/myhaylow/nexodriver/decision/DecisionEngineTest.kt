package com.myhaylow.nexodriver.decision

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class DecisionEngineTest {
    @Test
    fun `matches every versioned Node decision scenario`() {
        scenarios.forEach { scenario ->
            val result = DecisionEngine.evaluate(scenario.offer, scenario.profile)
            assertEquals(scenario.id, scenario.decision, result.decision)
            assertEquals(scenario.id, scenario.reason, result.reason)
            assertEquals(scenario.id, scenario.profile.id, result.profileId)
            assertEquals(scenario.id, scenario.offer.destinationMatches, result.destination)
        }
    }

    @Test
    fun `calculates the same rounded metrics as Node`() {
        val metrics = DecisionEngine.calculateMetrics(offer(24.0, 2.0, 10.0, 5.0, 25.0))
        assertEquals(12.0, metrics.totalDistanceKm, 0.0)
        assertEquals(30.0, metrics.totalMinutes, 0.0)
        assertEquals(2.0, metrics.perKm, 0.0)
        assertEquals(48.0, metrics.perHour, 0.0)
    }

    @Test
    fun `uses JavaScript half-up rounding with epsilon`() {
        val metrics = DecisionEngine.calculateMetrics(offer(1.005, 0.5, 0.5, 30.0, 30.0))
        assertEquals(1.01, metrics.perKm, 0.0)
    }

    @Test
    fun `scenario ids stay synchronized with the shared fixture`() {
        val fixture = File("../../fixtures/decision-scenarios.json").readText().substringAfter("\"scenarios\"")
        val fixtureIds = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"")
            .findAll(fixture).map { it.groupValues[1] }.toList()
        assertEquals(fixtureIds, scenarios.map { it.id })
    }

    private data class Scenario(val id: String, val profile: DecisionProfile, val offer: DecisionOffer,
        val decision: Decision, val reason: Reason)

    companion object {
        private fun offer(fare: Double, pickupKm: Double, tripKm: Double, pickupMin: Double,
            tripMin: Double, confidence: Double = 0.99, supermarket: Boolean = false,
            geographic: Boolean = false, traffic: TrafficContext = TrafficContext.NORMAL,
            destination: Boolean = false) = DecisionOffer(fare, pickupKm, tripKm, pickupMin, tripMin,
            confidence, supermarket, geographic, traffic, destination)

        private val scenarios = listOf(
            Scenario("accept-default-targets", DecisionProfiles.DEFAULT, offer(24.0, 2.0, 10.0, 5.0, 25.0), Decision.ACCEPT, Reason.KM_AND_HOUR_APPROVED),
            Scenario("reject-km-below-minimum", DecisionProfiles.DEFAULT, offer(17.0, 2.0, 8.0, 4.0, 16.0), Decision.REJECT, Reason.KM_BELOW_MINIMUM),
            Scenario("analyze-low-confidence", DecisionProfiles.DEFAULT, offer(30.0, 2.0, 10.0, 4.0, 20.0, 0.80), Decision.ANALYZE, Reason.LOW_READING_CONFIDENCE),
            Scenario("reject-supermarket", DecisionProfiles.DEFAULT, offer(40.0, 1.0, 9.0, 3.0, 17.0, supermarket = true), Decision.REJECT, Reason.SUPERMARKET_BLOCKED),
            Scenario("reject-geographic-block", DecisionProfiles.DEFAULT, offer(40.0, 1.0, 9.0, 3.0, 17.0, geographic = true), Decision.REJECT, Reason.GEOGRAPHIC_BLOCKED),
            Scenario("accept-pickup-tolerance", DecisionProfiles.DEFAULT, offer(30.0, 3.8, 8.2, 8.5, 21.5), Decision.ACCEPT, Reason.PICKUP_TOLERANCE_APPLIED),
            Scenario("reject-above-pickup-tolerance", DecisionProfiles.DEFAULT, offer(40.0, 4.0, 8.0, 9.0, 21.0), Decision.REJECT, Reason.PICKUP_ABOVE_LIMIT),
            Scenario("accept-km-compensates-hour", DecisionProfiles.DEFAULT, offer(21.0, 2.0, 8.0, 7.0, 27.0), Decision.ACCEPT, Reason.KM_COMPENSATED_HOUR),
            Scenario("analyze-no-compensation-in-traffic", DecisionProfiles.DEFAULT, offer(21.0, 2.0, 8.0, 7.0, 27.0, traffic = TrafficContext.INTENSE), Decision.ANALYZE, Reason.HOUR_BELOW_TARGET),
            Scenario("accept-destination-profile", DecisionProfiles.DESTINATION, offer(18.0, 2.0, 8.0, 5.0, 25.0, destination = true), Decision.ACCEPT, Reason.KM_AND_HOUR_APPROVED),
        )
    }
}
