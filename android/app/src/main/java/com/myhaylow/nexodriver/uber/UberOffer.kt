package com.myhaylow.nexodriver.uber

enum class OfferPresentation { RADAR, DIRECT_EXCLUSIVE, MATCHED_RESULT }

enum class OfferIndicator {
    EXCLUSIVE,
    PRIORITY_PICKUP,
    VERIFIED_RIDER,
    TOWARD_DESTINATION,
}

data class RouteLeg(val minutes: Int, val distanceKm: Double)

data class RiderRating(val score: Double, val count: Int?)

data class UberOffer(
    val presentation: OfferPresentation,
    val grossFare: Double,
    val category: String,
    val pickup: RouteLeg,
    val trip: RouteLeg,
    val perKm: Double,
    val displayedPerKm: Double?,
    val rating: RiderRating?,
    val priorityBonus: Double?,
    val indicators: Set<OfferIndicator>,
    val confidence: Double,
)

sealed interface ParseResult {
    data class Offer(val value: UberOffer) : ParseResult
    data class Matched(val value: UberOffer) : ParseResult
    data class Ignored(val reason: String) : ParseResult
    data class Invalid(val reason: String) : ParseResult
}
