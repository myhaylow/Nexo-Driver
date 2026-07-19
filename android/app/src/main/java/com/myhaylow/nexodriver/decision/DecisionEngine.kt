package com.myhaylow.nexodriver.decision

import kotlin.math.pow
import kotlin.math.floor

enum class Decision { ACCEPT, ANALYZE, REJECT }

enum class Reason {
    LOW_READING_CONFIDENCE, SUPERMARKET_BLOCKED, GEOGRAPHIC_BLOCKED,
    KM_BELOW_MINIMUM, PICKUP_ABOVE_LIMIT, PICKUP_TOLERANCE_APPLIED,
    KM_AND_HOUR_APPROVED, KM_COMPENSATED_HOUR, HOUR_BELOW_TARGET,
}

enum class TrafficContext { NORMAL, INTENSE }

data class DecisionOffer(
    val grossFare: Double,
    val pickupDistanceKm: Double,
    val tripDistanceKm: Double,
    val pickupMinutes: Double,
    val tripMinutes: Double,
    val readingConfidence: Double,
    val supermarketBlocked: Boolean = false,
    val geographicBlocked: Boolean = false,
    val trafficContext: TrafficContext = TrafficContext.NORMAL,
    val destinationMatches: Boolean = false,
)

data class DecisionProfile(
    val id: String,
    val name: String,
    val minPerKm: Double,
    val targetPerHour: Double,
    val maxPickupDistanceKm: Double,
    val maxPickupMinutes: Double,
)

data class DecisionPolicy(
    val readingConfidenceMinimum: Double = 0.95,
    val pickupTolerance: Double = 0.10,
    val strongKmBonus: Double = 0.10,
    val hourTolerance: Double = 0.10,
)

data class DecisionMetrics(
    val totalDistanceKm: Double,
    val totalMinutes: Double,
    val perKm: Double,
    val perHour: Double,
)

data class DecisionResult(
    val decision: Decision,
    val reason: Reason,
    val destination: Boolean,
    val metrics: DecisionMetrics,
    val profileId: String,
    val details: Map<String, Any> = emptyMap(),
)

object DecisionEngine {
    fun calculateMetrics(offer: DecisionOffer): DecisionMetrics {
        requireFinite(offer.grossFare, offer.pickupDistanceKm, offer.tripDistanceKm,
            offer.pickupMinutes, offer.tripMinutes, offer.readingConfidence)
        val distance = offer.pickupDistanceKm + offer.tripDistanceKm
        val minutes = offer.pickupMinutes + offer.tripMinutes
        require(offer.grossFare >= 0 && distance > 0 && minutes > 0) {
            "Oferta deve possuir valor nao negativo, distancia e tempo totais positivos"
        }
        return DecisionMetrics(round(distance), round(minutes), round(offer.grossFare / distance),
            round((offer.grossFare / minutes) * 60))
    }

    fun evaluate(
        offer: DecisionOffer,
        profile: DecisionProfile,
        policy: DecisionPolicy = DecisionPolicy(),
    ): DecisionResult {
        requireFinite(profile.minPerKm, profile.targetPerHour, profile.maxPickupDistanceKm,
            profile.maxPickupMinutes)
        val metrics = calculateMetrics(offer)
        fun result(decision: Decision, reason: Reason, details: Map<String, Any> = emptyMap()) =
            DecisionResult(decision, reason, offer.destinationMatches, metrics, profile.id, details)

        if (offer.readingConfidence < policy.readingConfidenceMinimum) return result(
            Decision.ANALYZE, Reason.LOW_READING_CONFIDENCE,
            mapOf("actual" to offer.readingConfidence, "required" to policy.readingConfidenceMinimum))
        if (offer.supermarketBlocked) return result(Decision.REJECT, Reason.SUPERMARKET_BLOCKED)
        if (offer.geographicBlocked) return result(Decision.REJECT, Reason.GEOGRAPHIC_BLOCKED)
        if (metrics.perKm < profile.minPerKm) return result(Decision.REJECT, Reason.KM_BELOW_MINIMUM,
            mapOf("actual" to metrics.perKm, "required" to profile.minPerKm))

        val maxDistance = profile.maxPickupDistanceKm * (1 + policy.pickupTolerance)
        val maxMinutes = profile.maxPickupMinutes * (1 + policy.pickupTolerance)
        val strict = offer.pickupDistanceKm <= profile.maxPickupDistanceKm &&
            offer.pickupMinutes <= profile.maxPickupMinutes
        val tolerated = offer.pickupDistanceKm <= maxDistance && offer.pickupMinutes <= maxMinutes
        val hourApproved = metrics.perHour >= profile.targetPerHour
        if (!tolerated) return result(Decision.REJECT, Reason.PICKUP_ABOVE_LIMIT,
            mapOf("maxDistanceKm" to round(maxDistance), "maxMinutes" to round(maxMinutes)))
        if (!strict) return if (hourApproved) {
            result(Decision.ACCEPT, Reason.PICKUP_TOLERANCE_APPLIED,
                mapOf("tolerance" to policy.pickupTolerance))
        } else {
            result(Decision.REJECT, Reason.PICKUP_ABOVE_LIMIT,
                mapOf("toleranceAvailable" to true, "hourApproved" to false))
        }
        if (hourApproved) return result(Decision.ACCEPT, Reason.KM_AND_HOUR_APPROVED)

        val strongKm = metrics.perKm >= profile.minPerKm * (1 + policy.strongKmBonus)
        val hourInsideTolerance = metrics.perHour >= profile.targetPerHour * (1 - policy.hourTolerance)
        if (strongKm && hourInsideTolerance && offer.trafficContext != TrafficContext.INTENSE) {
            return result(Decision.ACCEPT, Reason.KM_COMPENSATED_HOUR, mapOf(
                "strongKmBonus" to policy.strongKmBonus,
                "hourTolerance" to policy.hourTolerance,
                "trafficContext" to offer.trafficContext.name,
            ))
        }
        return result(Decision.ANALYZE, Reason.HOUR_BELOW_TARGET, mapOf(
            "actual" to metrics.perHour, "target" to profile.targetPerHour,
            "trafficContext" to offer.trafficContext.name,
        ))
    }

    private fun requireFinite(vararg values: Double) = require(values.all(Double::isFinite)) {
        "Campos numericos devem ser finitos"
    }

    private fun round(value: Double, places: Int = 2): Double {
        val factor = 10.0.pow(places)
        return floor((value + Math.ulp(1.0)) * factor + 0.5) / factor
    }
}

object DecisionProfiles {
    val DEFAULT = DecisionProfile("default", "Perfil padrão", 1.80, 40.0, 3.5, 8.0)
    val DESTINATION = DecisionProfile("destination", "Modo Destino", 1.60, 36.0, 3.5, 8.0)
}
