package com.myhaylow.nexodriver.lab

import com.myhaylow.nexodriver.decision.Decision
import com.myhaylow.nexodriver.decision.Reason
import com.myhaylow.nexodriver.uber.OfferPresentation
import com.myhaylow.nexodriver.uber.ParseResult
import com.myhaylow.nexodriver.uber.RouteLeg
import com.myhaylow.nexodriver.uber.UberOffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UberDecisionBridgeTest {
    @Test
    fun `only offers are evaluated and matched clears current state`() {
        val offer = UberOffer(OfferPresentation.RADAR, 24.0, "Categoria fictícia",
            RouteLeg(5, 2.0), RouteLeg(25, 10.0), 2.0, null, null, null, emptySet(), 0.99)
        UberDecisionBridge.consume(ParseResult.Offer(offer))
        assertEquals(Decision.ACCEPT, LabState.current?.result?.decision)

        UberDecisionBridge.consume(ParseResult.Matched(offer.copy(presentation = OfferPresentation.MATCHED_RESULT)))
        assertNull(LabState.current)
        assertEquals(1, LabState.diagnosticCount())
        LabState.clearDiagnostics()
    }

    @Test
    fun `diagnostic buffer is bounded and contains normalized numbers only`() {
        LabState.clearDiagnostics()
        repeat(25) { index ->
            val offer = UberOffer(OfferPresentation.RADAR, 24.0 + index, "Categoria fictícia",
                RouteLeg(5, 2.0), RouteLeg(25, 10.0), 2.0, null, null, null, emptySet(), 0.99)
            UberDecisionBridge.consume(ParseResult.Offer(offer))
        }
        assertEquals(20, LabState.diagnosticCount())
        LabState.clearDiagnostics()
    }

    @Test
    fun `validated external context can reach blocking rules`() {
        LabState.clearDiagnostics()
        val offer = UberOffer(OfferPresentation.RADAR, 40.0, "Categoria fictícia",
            RouteLeg(3, 1.0), RouteLeg(17, 9.0), 4.0, null, null, null, emptySet(), 0.99)
        UberDecisionBridge.consume(ParseResult.Offer(offer),
            UberDecisionBridge.Context(supermarketBlocked = true))
        assertEquals(Reason.SUPERMARKET_BLOCKED, LabState.current?.result?.reason)
        LabState.clearDiagnostics()
    }
}
