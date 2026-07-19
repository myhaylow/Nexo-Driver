package com.myhaylow.nexodriver.uber

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class UberOfferAccessibilityService : AccessibilityService() {
    /** Injection point for the future decision bridge. Matched cards are deliberately never emitted. */
    internal var onOffer: (UberOffer) -> Unit = {}

    private val parser = UberOfferParser()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName?.toString() != UberOfferParser.UBER_DRIVER_PACKAGE) return
        windows.asSequence()
            .mapNotNull { it.root }
            .filter { it.packageName?.toString() == UberOfferParser.UBER_DRIVER_PACKAGE }
            .map { parser.parse(AccessibilityTree.from(it)) }
            .filterIsInstance<ParseResult.Offer>()
            .forEach { onOffer(it.value) }
    }

    override fun onInterrupt() = Unit
}
