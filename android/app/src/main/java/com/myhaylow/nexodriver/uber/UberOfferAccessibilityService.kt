package com.myhaylow.nexodriver.uber

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.myhaylow.nexodriver.lab.LabSnapshot
import com.myhaylow.nexodriver.lab.LabState
import com.myhaylow.nexodriver.lab.ShadowOverlayController
import com.myhaylow.nexodriver.lab.UberDecisionBridge
import com.myhaylow.nexodriver.profile.ProfileRepository
import com.myhaylow.nexodriver.profile.RuntimeConfiguration

class UberOfferAccessibilityService : AccessibilityService() {
    /** Injection point for the future decision bridge. Matched cards are deliberately never emitted. */
    internal var onOffer: (UberOffer) -> Unit = {}

    private val parser = UberOfferParser()
    private lateinit var overlay: ShadowOverlayController
    private val labListener: (LabSnapshot?) -> Unit = { snapshot ->
        if (snapshot == null) overlay.hide() else overlay.show(snapshot)
    }

    override fun onServiceConnected() {
        RuntimeConfiguration.initialize(ProfileRepository(this))
        overlay = ShadowOverlayController(this)
        LabState.addListener(labListener)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName?.toString() != UberOfferParser.UBER_DRIVER_PACKAGE) return
        windows.asSequence()
            .mapNotNull { it.root }
            .filter { it.packageName?.toString() == UberOfferParser.UBER_DRIVER_PACKAGE }
            .map { parser.parse(AccessibilityTree.from(it)) }
            .forEach { result ->
                UberDecisionBridge.consume(result)
                if (result is ParseResult.Offer) onOffer(result.value)
            }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        LabState.removeListener(labListener)
        if (::overlay.isInitialized) overlay.hide()
        super.onDestroy()
    }
}
