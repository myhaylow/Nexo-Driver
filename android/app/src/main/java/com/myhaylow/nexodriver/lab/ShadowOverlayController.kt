package com.myhaylow.nexodriver.lab

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.myhaylow.nexodriver.decision.Decision
import java.util.Locale

class ShadowOverlayController(context: Context) {
    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(WindowManager::class.java)
    private var view: LinearLayout? = null

    fun show(snapshot: LabSnapshot) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || !Settings.canDrawOverlays(appContext)) return
        hide()
        val destination = snapshot.result.destination
        val color = when {
            destination -> Color.rgb(103, 80, 164)
            snapshot.result.decision == Decision.ACCEPT -> Color.rgb(0, 170, 90)
            snapshot.result.decision == Decision.ANALYZE -> Color.rgb(245, 190, 0)
            else -> Color.rgb(215, 45, 55)
        }
        view = LinearLayout(appContext).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 14, 22, 14)
            isClickable = false
            isFocusable = false
            background = GradientDrawable().apply {
                setColor(Color.rgb(20, 23, 30)); setStroke(7, color); cornerRadius = 18f
            }
            addLine(if (destination) "MODO DESTINO • ${snapshot.result.decision.pt()}" else snapshot.result.decision.pt(), color, 17f)
            addLine("R$/km ${money(snapshot.result.metrics.perKm)}  •  R$/hora ${money(snapshot.result.metrics.perHour)}")
            addLine("Lucro líquido ${optionalMoney(snapshot.estimatedNetProfit)}  •  Lucro/min ${optionalMoney(snapshot.profitPerMinute)}")
            addLine("${number(snapshot.result.metrics.totalDistanceKm)} km  •  ${number(snapshot.result.metrics.totalMinutes)} min")
        }
        windowManager.addView(view, WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            android.graphics.PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; y = 120 })
    }

    fun hide() { view?.let { windowManager.removeView(it) }; view = null }

    private fun LinearLayout.addLine(value: String, color: Int = Color.WHITE, size: Float = 14f) {
        addView(TextView(appContext).apply { text = value; textSize = size; setTextColor(color) })
    }
    private fun Decision.pt() = when (this) { Decision.ACCEPT -> "ACEITAR"; Decision.ANALYZE -> "ANALISAR"; Decision.REJECT -> "RECUSAR" }
    private fun money(value: Double) = String.format(Locale.forLanguageTag("pt-BR"), "%.2f", value)
    private fun number(value: Double) = String.format(Locale.forLanguageTag("pt-BR"), "%.1f", value)
    private fun optionalMoney(value: Double?) = value?.let { "R$ ${money(it)}" } ?: "—"
}
