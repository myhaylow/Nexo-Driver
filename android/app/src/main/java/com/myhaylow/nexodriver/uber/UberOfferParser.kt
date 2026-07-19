package com.myhaylow.nexodriver.uber

import java.math.RoundingMode

class UberOfferParser {
    fun parse(root: AccessibilityTree): ParseResult {
        if (root.packageName != UBER_DRIVER_PACKAGE) return ParseResult.Ignored("foreign_package")

        val scopes = buildList { collectScopes(root, 0, this) }.sortedByDescending { it.second }
        var lastInvalid: ParseResult.Invalid = ParseResult.Invalid("unknown_card_state")
        for ((scope, _) in scopes) {
            when (val result = parseScope(scope)) {
                is ParseResult.Invalid -> lastInvalid = result
                else -> return result
            }
        }
        return lastInvalid
    }

    private fun collectScopes(
        node: AccessibilityTree,
        depth: Int,
        destination: MutableList<Pair<AccessibilityTree, Int>>,
    ) {
        if (node.packageName != UBER_DRIVER_PACKAGE) return
        if (node.semanticTexts(UBER_DRIVER_PACKAGE).any(::isCta)) destination += node to depth
        node.children.forEach { collectScopes(it, depth + 1, destination) }
    }

    private fun isCta(text: String) = text.equals("Selecionar", true) ||
        text.equals("Aceitar", true) || text.equals("Iniciar navegação", true) ||
        text.equals("Iniciar navegacao", true)

    private fun parseScope(root: AccessibilityTree): ParseResult {
        val texts = root.semanticTexts(UBER_DRIVER_PACKAGE)
        val presentation = when {
            texts.any { it.equals("Iniciar navegação", true) || it.equals("Iniciar navegacao", true) } -> OfferPresentation.MATCHED_RESULT
            texts.any { it.equals("Aceitar", true) } && texts.any { it.equals("Exclusivo", true) } -> OfferPresentation.DIRECT_EXCLUSIVE
            texts.any { it.equals("Selecionar", true) } -> OfferPresentation.RADAR
            else -> return ParseResult.Invalid("unknown_card_state")
        }

        val fare = texts
            .filterNot { BONUS.containsMatchIn(it) || PER_KM.containsMatchIn(it) }
            .firstNotNullOfOrNull { FARE.find(it)?.groupValues?.get(1)?.currency() }
            ?: return ParseResult.Invalid("missing_fare")
        val pickup = findLeg(texts, PICKUP) ?: return ParseResult.Invalid("missing_pickup")
        val trip = findLeg(texts, TRIP) ?: return ParseResult.Invalid("missing_trip")
        val category = texts.firstNotNullOfOrNull { CATEGORY.find(it)?.groupValues?.get(1)?.trim() }
            ?: return ParseResult.Invalid("missing_category")
        val displayedPerKm = texts.firstNotNullOfOrNull { PER_KM.find(it)?.groupValues?.get(1)?.currency() }
        val totalDistance = pickup.distanceKm + trip.distanceKm
        if (totalDistance <= 0.0) return ParseResult.Invalid("invalid_total_distance")
        val calculatedPerKm = (fare / totalDistance).roundMoney()
        val rating = texts.firstNotNullOfOrNull { text ->
            RATING.find(text)?.let { match ->
                RiderRating(match.groupValues[1].currency(), match.groupValues[2].takeIf(String::isNotEmpty)?.toInt())
            }
        }
        val bonus = texts.firstNotNullOfOrNull { BONUS.find(it)?.groupValues?.get(1)?.currency() }
        val indicators = buildSet {
            if (texts.any { it.equals("Exclusivo", true) }) add(OfferIndicator.EXCLUSIVE)
            if (texts.any { it.contains("embarque prioritário", true) || it.contains("embarque prioritario", true) }) add(OfferIndicator.PRIORITY_PICKUP)
            if (texts.any { it.contains("passageiro verificado", true) }) add(OfferIndicator.VERIFIED_RIDER)
            if (texts.any { it.contains("em direção ao destino", true) || it.contains("em direcao ao destino", true) }) add(OfferIndicator.TOWARD_DESTINATION)
        }
        val displayedConsistent = displayedPerKm == null || displayedPerKm == calculatedPerKm
        val confidence = if (displayedConsistent) 1.0 else 0.8
        val offer = UberOffer(presentation, fare, category, pickup, trip, calculatedPerKm,
            displayedPerKm, rating, bonus, indicators, confidence)
        return if (presentation == OfferPresentation.MATCHED_RESULT) ParseResult.Matched(offer) else ParseResult.Offer(offer)
    }

    private fun findLeg(texts: List<String>, regex: Regex): RouteLeg? = texts.firstNotNullOfOrNull {
        regex.find(it)?.let { match -> RouteLeg(match.groupValues[1].toInt(), match.groupValues[2].distance()) }
    }

    private fun String.currency(): Double {
        val decimalSeparator = maxOf(lastIndexOf(','), lastIndexOf('.'))
        if (decimalSeparator < 0) return filter(Char::isDigit).toDouble()
        val whole = substring(0, decimalSeparator).filter(Char::isDigit)
        val fraction = substring(decimalSeparator + 1).filter(Char::isDigit)
        return "$whole.$fraction".toDouble()
    }
    private fun String.distance() = replace(',', '.').toDouble()
    private fun Double.roundMoney() = toBigDecimal().setScale(2, RoundingMode.HALF_UP).toDouble()

    companion object {
        const val UBER_DRIVER_PACKAGE = "com.ubercab.driver"
        private val FARE = Regex("R\\$\\s*([0-9]+(?:[.,][0-9]{2}))", RegexOption.IGNORE_CASE)
        private val CATEGORY = Regex("^(?:Categoria\\s*[:：]\\s*)(.+)$", RegexOption.IGNORE_CASE)
        private val PICKUP = Regex("^(?:Coleta)\\s*[:：]?\\s*(\\d+)\\s*min(?:utos?)?\\s*[•·|-]?\\s*([0-9]+(?:[.,][0-9]+))\\s*km", RegexOption.IGNORE_CASE)
        private val TRIP = Regex("^(?:Viagem)\\s*[:：]?\\s*(\\d+)\\s*min(?:utos?)?\\s*[•·|-]?\\s*([0-9]+(?:[.,][0-9]+))\\s*km", RegexOption.IGNORE_CASE)
        private val PER_KM = Regex("([0-9]+(?:[.,][0-9]{2}))\\s*R\\$/km\\s*est\\.?", RegexOption.IGNORE_CASE)
        private val RATING = Regex("Nota\\s*[:：]?\\s*([0-5](?:[.,][0-9]{1,2}))(?:\\s*\\((\\d+)\\))?", RegexOption.IGNORE_CASE)
        private val BONUS = Regex("(?:Bônus|Bonus).*?R\\$\\s*([0-9]+(?:[.,][0-9]{2}))", RegexOption.IGNORE_CASE)
    }
}
