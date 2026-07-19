package com.myhaylow.nexodriver.lab

import com.myhaylow.nexodriver.decision.DecisionResult
import com.myhaylow.nexodriver.uber.UberOffer
import java.util.concurrent.CopyOnWriteArraySet

data class LabSnapshot(
    val grossFare: Double,
    val readingConfidence: Double,
    val pickupDistanceKm: Double,
    val tripDistanceKm: Double,
    val pickupMinutes: Int,
    val tripMinutes: Int,
    val result: DecisionResult,
    val estimatedCostPerKm: Double? = null,
    val estimatedOperatingCost: Double? = null,
    val estimatedNetProfit: Double? = null,
) {
    val profitPerMinute: Double? = estimatedNetProfit?.div(result.metrics.totalMinutes)
}

object LabState {
    private const val MAX_DIAGNOSTICS = 20
    private val listeners = CopyOnWriteArraySet<(LabSnapshot?) -> Unit>()
    private val diagnostics = ArrayDeque<LabSnapshot>()

    @Volatile var current: LabSnapshot? = null
        private set

    @Synchronized
    fun publish(offer: UberOffer, result: DecisionResult, estimatedOperatingCost: Double? = null,
        estimatedCostPerKm: Double? = null) {
        val snapshot = LabSnapshot(
            grossFare = offer.grossFare,
            readingConfidence = offer.confidence,
            pickupDistanceKm = offer.pickup.distanceKm,
            tripDistanceKm = offer.trip.distanceKm,
            pickupMinutes = offer.pickup.minutes,
            tripMinutes = offer.trip.minutes,
            result = result,
            estimatedCostPerKm = estimatedCostPerKm,
            estimatedOperatingCost = estimatedOperatingCost,
            estimatedNetProfit = estimatedOperatingCost?.let { offer.grossFare - it },
        )
        diagnostics.addLast(snapshot)
        while (diagnostics.size > MAX_DIAGNOSTICS) diagnostics.removeFirst()
        current = snapshot
        listeners.forEach { it(snapshot) }
    }

    @Synchronized
    fun clearCurrent() {
        current = null
        listeners.forEach { it(null) }
    }

    @Synchronized
    fun clearDiagnostics() {
        current = null
        diagnostics.clear()
        listeners.forEach { it(null) }
    }

    @Synchronized fun diagnosticCount(): Int = diagnostics.size
    fun addListener(listener: (LabSnapshot?) -> Unit) { listeners += listener }
    fun removeListener(listener: (LabSnapshot?) -> Unit) { listeners -= listener }
}
