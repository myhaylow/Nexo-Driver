package com.myhaylow.nexodriver.finance

import org.junit.Assert.*
import org.junit.Test

class FinanceCalculatorTest {
    @Test fun `missing configuration remains not configured`() {
        assertNull(FinanceCalculator.costPerKm(OperatingCosts()))
        assertNull(FinanceCalculator.evaluate(OperatingCosts(), 30.0, 10.0, 30.0))
    }

    @Test fun `calculates offer cost net profit and profit per minute`() {
        val costs = OperatingCosts(6.0, 12.0, .10, .05, 120.0, 1200.0, 80.0, 2000.0)
        val result = FinanceCalculator.evaluate(costs, 30.0, 10.0, 20.0)!!
        assertEquals(.8, result.costPerKm, 0.0001)
        assertEquals(8.0, result.offerCost, 0.0001)
        assertEquals(22.0, result.netProfit, 0.0001)
        assertEquals(1.1, result.profitPerMinute, 0.0001)
    }

    @Test fun `invalid zero denominators are not configured`() {
        val costs = OperatingCosts(6.0, 0.0, .1, .05, 100.0, 1000.0, 50.0, 0.0)
        assertNull(FinanceCalculator.costPerKm(costs))
    }
}
