package com.myhaylow.nexodriver.uber

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UberOfferParserTest {
    private val parser = UberOfferParser()

    @Test
    fun `parses all ten anonymized card trees`() {
        samples.forEach { sample ->
            val result = parser.parse(sample.tree())
            val parsed = when (result) {
                is ParseResult.Offer -> result.value
                is ParseResult.Matched -> result.value
                else -> error("${sample.id}: $result")
            }
            assertEquals(sample.id, sample.presentation, parsed.presentation)
            assertEquals(sample.id, sample.category, parsed.category)
            assertEquals(sample.id, sample.fare, parsed.grossFare, 0.001)
            assertEquals(sample.id, sample.pickup, parsed.pickup)
            assertEquals(sample.id, sample.trip, parsed.trip)
            assertEquals(sample.id, sample.expectedPerKm, parsed.perKm, 0.001)
            assertEquals(sample.id, sample.displayedPerKm, parsed.displayedPerKm)
            assertEquals(sample.id, sample.rating, parsed.rating)
            assertEquals(sample.id, sample.bonus, parsed.priorityBonus)
            assertEquals(sample.id, sample.indicators, parsed.indicators)
            assertEquals(sample.id, 1.0, parsed.confidence, 0.001)
        }
    }

    @Test
    fun `matched result is not an offer for the decision engine`() {
        val result = parser.parse(samples.single { it.presentation == OfferPresentation.MATCHED_RESULT }.tree())
        assertTrue(result is ParseResult.Matched)
        assertTrue(result !is ParseResult.Offer)
    }

    @Test
    fun `ignores foreign windows including convincing overlays`() {
        val foreign = card(
            packageName = "com.example.overlay",
            "R$ 999,99", "Categoria: Fraud", "Coleta: 1 min • 0.1 km",
            "Viagem: 1 min • 0.1 km", "Selecionar",
        )
        assertEquals(ParseResult.Ignored("foreign_package"), parser.parse(foreign))

        val uberWithForeignChild = samples.last().tree().copy(children = samples.last().tree().children + foreign)
        val offer = (parser.parse(uberWithForeignChild) as ParseResult.Offer).value
        assertEquals(21.39, offer.grossFare, 0.001)
        assertNull(offer.priorityBonus)
    }

    @Test
    fun `fare does not depend on bonus node order`() {
        val original = samples[1].tree()
        val reversed = original.copy(children = original.children.reversed())
        val offer = (parser.parse(reversed) as ParseResult.Offer).value
        assertEquals(23.11, offer.grossFare, 0.001)
        assertEquals(1.89, offer.priorityBonus!!, 0.001)
    }

    @Test
    fun `rejects a zero total distance instead of dividing by zero`() {
        val tree = card(
            UberOfferParser.UBER_DRIVER_PACKAGE,
            "R$ 10,00", "Categoria: UberX", "Coleta: 1 min • 0.0 km",
            "Viagem: 1 min • 0.0 km", "Selecionar",
        )
        assertEquals(ParseResult.Invalid("invalid_total_distance"), parser.parse(tree))
    }

    @Test
    fun `does not combine fields from sibling cards during a transition`() {
        val firstCard = samples.first().tree()
        val secondCard = samples[3].tree()
        val window = AccessibilityTree(
            UberOfferParser.UBER_DRIVER_PACKAGE,
            children = listOf(firstCard, secondCard),
        )
        val offer = (parser.parse(window) as ParseResult.Offer).value
        assertEquals(10.54, offer.grossFare, 0.001)
        assertEquals("UberX", offer.category)
        assertEquals(RouteLeg(6, 1.8), offer.pickup)
        assertEquals(RouteLeg(11, 5.2), offer.trip)
    }

    private data class Sample(
        val id: String,
        val presentation: OfferPresentation,
        val category: String,
        val fare: Double,
        val pickup: RouteLeg,
        val trip: RouteLeg,
        val displayedPerKm: Double? = null,
        val rating: RiderRating,
        val bonus: Double? = null,
        val indicators: Set<OfferIndicator> = emptySet(),
    ) {
        val expectedPerKm = money(fare / (pickup.distanceKm + trip.distanceKm))

        fun tree(): AccessibilityTree {
            val cta = when (presentation) {
                OfferPresentation.RADAR -> "Selecionar"
                OfferPresentation.DIRECT_EXCLUSIVE -> "Aceitar"
                OfferPresentation.MATCHED_RESULT -> "Iniciar navegação"
            }
            val texts = buildList {
                add("R$ ${fare.br()}")
                add("Categoria: $category")
                add("Coleta: ${pickup.minutes} min • ${pickup.distanceKm} km")
                add("Viagem: ${trip.minutes} min • ${trip.distanceKm} km")
                displayedPerKm?.let { add("${it.br()} R$/km est.") }
                add("Nota: ${rating.score.br()} (${rating.count})")
                bonus?.let { add("Bônus de embarque prioritário R$ ${it.br()}") }
                if (OfferIndicator.EXCLUSIVE in indicators) add("Exclusivo")
                if (OfferIndicator.PRIORITY_PICKUP in indicators) add("Embarque prioritário")
                if (OfferIndicator.VERIFIED_RIDER in indicators) add("Passageiro verificado")
                if (OfferIndicator.TOWARD_DESTINATION in indicators) add("Em direção ao destino")
                add(cta)
            }
            // Vary nesting and place text in both text/contentDescription; no geometry exists.
            val midpoint = texts.size / 2
            return AccessibilityTree(
                UberOfferParser.UBER_DRIVER_PACKAGE,
                children = listOf(
                    AccessibilityTree(UberOfferParser.UBER_DRIVER_PACKAGE, children = texts.take(midpoint).map(::node)),
                    AccessibilityTree(UberOfferParser.UBER_DRIVER_PACKAGE, children = texts.drop(midpoint).map(::node)),
                ),
            )
        }
    }

    companion object {
        private fun node(value: String) = AccessibilityTree(
            UberOfferParser.UBER_DRIVER_PACKAGE,
            text = value.takeUnless { it.startsWith("Nota") },
            contentDescription = value.takeIf { it.startsWith("Nota") },
        )

        private fun card(packageName: String, vararg texts: String) = AccessibilityTree(
            packageName,
            children = texts.map { AccessibilityTree(packageName, text = it) },
        )

        private fun Double.br() = String.format(java.util.Locale.US, "%.2f", this).replace('.', ',')
        private fun money(value: Double) = value.toBigDecimal().setScale(2, java.math.RoundingMode.HALF_UP).toDouble()

        private val samples = listOf(
            Sample("radar-uberx-standard", OfferPresentation.RADAR, "UberX", 10.54, RouteLeg(6, 1.8), RouteLeg(11, 5.2), 1.51, RiderRating(4.87, 94)),
            Sample("radar-priority-with-bonus", OfferPresentation.RADAR, "Priority", 23.11, RouteLeg(14, 6.6), RouteLeg(11, 6.5), 1.76, RiderRating(5.0, 2), 1.89, setOf(OfferIndicator.PRIORITY_PICKUP)),
            Sample("radar-short-trip-long-pickup", OfferPresentation.RADAR, "UberX", 9.40, RouteLeg(12, 7.4), RouteLeg(3, 0.9), 1.13, RiderRating(4.93, 389), indicators = setOf(OfferIndicator.VERIFIED_RIDER)),
            Sample("exclusive-comfort", OfferPresentation.DIRECT_EXCLUSIVE, "Comfort", 16.15, RouteLeg(2, 0.8), RouteLeg(11, 4.8), rating = RiderRating(4.95, 853), indicators = setOf(OfferIndicator.EXCLUSIVE)),
            Sample("exclusive-delivery-car", OfferPresentation.DIRECT_EXCLUSIVE, "Envios Carro", 7.67, RouteLeg(4, 1.7), RouteLeg(7, 2.3), rating = RiderRating(4.95, 775), indicators = setOf(OfferIndicator.EXCLUSIVE, OfferIndicator.VERIFIED_RIDER)),
            Sample("radar-long-trip", OfferPresentation.RADAR, "UberX", 34.20, RouteLeg(10, 4.0), RouteLeg(28, 27.6), 1.08, RiderRating(4.91, 176), indicators = setOf(OfferIndicator.VERIFIED_RIDER)),
            Sample("radar-transition-matched", OfferPresentation.MATCHED_RESULT, "UberX", 10.99, RouteLeg(4, 1.4), RouteLeg(14, 6.6), 1.37, RiderRating(5.0, 4)),
            Sample("radar-transition-offer", OfferPresentation.RADAR, "UberX", 10.99, RouteLeg(4, 1.4), RouteLeg(14, 6.6), 1.37, RiderRating(5.0, 4)),
            Sample("exclusive-comfort-toward-destination", OfferPresentation.DIRECT_EXCLUSIVE, "Comfort", 20.59, RouteLeg(4, 1.4), RouteLeg(18, 11.7), rating = RiderRating(4.71, 252), indicators = setOf(OfferIndicator.EXCLUSIVE, OfferIndicator.TOWARD_DESTINATION)),
            Sample("exclusive-uberx-foreign-overlay", OfferPresentation.DIRECT_EXCLUSIVE, "UberX", 21.39, RouteLeg(3, 1.6), RouteLeg(25, 17.8), rating = RiderRating(4.84, 2023), indicators = setOf(OfferIndicator.EXCLUSIVE)),
        )
    }
}
