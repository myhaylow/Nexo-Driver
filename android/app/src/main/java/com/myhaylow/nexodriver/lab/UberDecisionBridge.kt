package com.myhaylow.nexodriver.lab

import com.myhaylow.nexodriver.decision.DecisionEngine
import com.myhaylow.nexodriver.decision.DecisionOffer
import com.myhaylow.nexodriver.decision.DecisionProfiles
import com.myhaylow.nexodriver.decision.TrafficContext
import com.myhaylow.nexodriver.uber.OfferIndicator
import com.myhaylow.nexodriver.uber.ParseResult
import com.myhaylow.nexodriver.finance.FinanceCalculator
import com.myhaylow.nexodriver.profile.RuntimeConfiguration

object UberDecisionBridge {
    data class Context(
        val supermarketBlocked: Boolean = false,
        val geographicBlocked: Boolean = false,
        val trafficContext: TrafficContext = TrafficContext.NORMAL,
    )

    fun consume(result: ParseResult, context: Context = Context()) {
        when (result) {
            is ParseResult.Matched -> LabState.clearCurrent()
            is ParseResult.Offer -> evaluate(result, context)
            is ParseResult.Ignored, is ParseResult.Invalid -> Unit
        }
    }

    private fun evaluate(result: ParseResult.Offer, context: Context) {
        val value = result.value
        val destination = OfferIndicator.TOWARD_DESTINATION in value.indicators
        val input = DecisionOffer(
            grossFare = value.grossFare,
            pickupDistanceKm = value.pickup.distanceKm,
            tripDistanceKm = value.trip.distanceKm,
            pickupMinutes = value.pickup.minutes.toDouble(),
            tripMinutes = value.trip.minutes.toDouble(),
            readingConfidence = value.confidence,
            supermarketBlocked = context.supermarketBlocked,
            geographicBlocked = context.geographicBlocked,
            trafficContext = context.trafficContext,
            destinationMatches = destination,
        )
        val configured = if (destination) RuntimeConfiguration.destination() else RuntimeConfiguration.current()
        val profile = configured?.first?.profile?.decisionProfile()
            ?: if (destination) DecisionProfiles.DESTINATION else DecisionProfiles.DEFAULT
        val profit = configured?.let { FinanceCalculator.evaluate(it.second, value.grossFare,
            input.pickupDistanceKm + input.tripDistanceKm, input.pickupMinutes + input.tripMinutes) }
        LabState.publish(value, DecisionEngine.evaluate(input, profile), profit?.offerCost, profit?.costPerKm)
    }
}
