package com.myhaylow.nexodriver.finance

data class OperatingCosts(
    val fuelPricePerLiter: Double? = null,
    val consumptionKmPerLiter: Double? = null,
    val maintenancePerKm: Double? = null,
    val tiresPerKm: Double? = null,
    val insuranceMonthly: Double? = null,
    val ipvaYearly: Double? = null,
    val otherMonthly: Double? = null,
    val expectedKmMonthly: Double? = null,
)

data class OfferProfit(val costPerKm: Double, val offerCost: Double, val netProfit: Double, val profitPerMinute: Double)

object FinanceCalculator {
    fun costPerKm(costs: OperatingCosts): Double? {
        val values = listOf(costs.fuelPricePerLiter, costs.consumptionKmPerLiter, costs.maintenancePerKm,
            costs.tiresPerKm, costs.insuranceMonthly, costs.ipvaYearly, costs.otherMonthly, costs.expectedKmMonthly)
        if (values.any { it == null || !it.isFinite() || it < 0 }) return null
        val consumption = costs.consumptionKmPerLiter!!
        val monthlyKm = costs.expectedKmMonthly!!
        if (consumption <= 0 || monthlyKm <= 0) return null
        val monthlyFixed = costs.insuranceMonthly!! + costs.otherMonthly!! + costs.ipvaYearly!! / 12.0
        return costs.fuelPricePerLiter!! / consumption + costs.maintenancePerKm!! + costs.tiresPerKm!! + monthlyFixed / monthlyKm
    }

    fun evaluate(costs: OperatingCosts, grossFare: Double, distanceKm: Double, minutes: Double): OfferProfit? {
        if (!grossFare.isFinite() || !distanceKm.isFinite() || !minutes.isFinite() || grossFare < 0 || distanceKm <= 0 || minutes <= 0) return null
        val perKm = costPerKm(costs) ?: return null
        val offerCost = perKm * distanceKm
        val net = grossFare - offerCost
        return OfferProfit(perKm, offerCost, net, net / minutes)
    }
}
