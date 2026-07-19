package com.myhaylow.nexodriver

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.myhaylow.nexodriver.decision.DecisionEngine
import com.myhaylow.nexodriver.decision.DecisionOffer
import com.myhaylow.nexodriver.decision.DecisionProfiles
import com.myhaylow.nexodriver.lab.LabSnapshot
import com.myhaylow.nexodriver.lab.LabState
import com.myhaylow.nexodriver.lab.ShadowOverlayController
import com.myhaylow.nexodriver.uber.OfferPresentation
import com.myhaylow.nexodriver.uber.RouteLeg
import com.myhaylow.nexodriver.uber.UberOffer
import com.myhaylow.nexodriver.uber.UberOfferAccessibilityService
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var diagnostics: TextView
    private lateinit var overlay: ShadowOverlayController
    private val listener: (LabSnapshot?) -> Unit = { runOnUiThread { render(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.DEBUG) {
            setContentView(label("Nexo Driver", 25f, Color.WHITE))
            return
        }
        overlay = ShadowOverlayController(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(Color.rgb(11, 14, 20))
            setPadding(36, 40, 36, 40)
            addView(label("NEXO DRIVER • MODO LAB", 25f, Color.rgb(150, 110, 255)))
            addView(label("Diagnóstico local, temporário e anonimizado", 14f, Color.LTGRAY))
        }
        status = label("", 15f, Color.WHITE).also(content::addView)
        diagnostics = label("Nenhuma oferta lida.", 16f, Color.WHITE).also(content::addView)
        content.addView(button("Abrir configurações de Acessibilidade") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        content.addView(button("Permitir sobreposição") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")))
        })
        content.addView(button("Preview fictício do overlay") { showPreview() })
        content.addView(button("Limpar diagnósticos") { LabState.clearDiagnostics(); overlay.hide() })
        setContentView(ScrollView(this).apply { addView(content) })
        LabState.addListener(listener)
    }

    override fun onResume() {
        super.onResume()
        if (BuildConfig.DEBUG) render(LabState.current)
    }

    override fun onDestroy() {
        if (BuildConfig.DEBUG) {
            LabState.removeListener(listener)
            if (::overlay.isInitialized) overlay.hide()
        }
        super.onDestroy()
    }

    private fun render(snapshot: LabSnapshot?) {
        val accessibility = if (isAccessibilityEnabled()) "ATIVA" else "INATIVA"
        val permission = if (Settings.canDrawOverlays(this)) "CONCEDIDA" else "NÃO CONCEDIDA"
        status.text = "\nAcessibilidade: $accessibility\nSobreposição: $permission\nBuffer: ${LabState.diagnosticCount()}/20\n"
        diagnostics.text = snapshot?.let {
            val metrics = it.result.metrics
            "Última oferta normalizada\n" +
                "Decisão: ${it.result.decision}\nMotivo: ${it.result.reason}\n" +
                "Perfil: ${it.result.profileId}\nConfiança: ${number(it.readingConfidence)}\n" +
                "R$/km: ${money(metrics.perKm)}\nR$/hora: ${money(metrics.perHour)}\n" +
                "Distância total: ${number(metrics.totalDistanceKm)} km\n" +
                "Tempo total: ${number(metrics.totalMinutes)} min\n"
        } ?: "Nenhuma oferta lida."
    }

    private fun showPreview() {
        val offer = UberOffer(OfferPresentation.RADAR, 24.0, "Categoria fictícia",
            RouteLeg(5, 2.0), RouteLeg(25, 10.0), 2.0, null, null, null, emptySet(), 0.99)
        val input = DecisionOffer(24.0, 2.0, 10.0, 5.0, 25.0, 0.99)
        val result = DecisionEngine.evaluate(input, DecisionProfiles.DEFAULT)
        LabState.publish(offer, result, estimatedOperatingCost = 7.20)
        if (!isAccessibilityEnabled()) LabState.current?.let(overlay::show)
    }

    private fun isAccessibilityEnabled(): Boolean {
        val component = ComponentName(this, UberOfferAccessibilityService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        return enabled.split(':').any { it.equals(component, true) }
    }

    private fun label(value: String, size: Float, color: Int) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color); gravity = Gravity.CENTER
        setPadding(8, 14, 8, 14)
    }
    private fun button(value: String, action: () -> Unit) = Button(this).apply {
        text = value; isAllCaps = false; setOnClickListener { action() }
    }
    private fun money(value: Double) = String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", value)
    private fun number(value: Double) = String.format(Locale.forLanguageTag("pt-BR"), "%.2f", value)
}
