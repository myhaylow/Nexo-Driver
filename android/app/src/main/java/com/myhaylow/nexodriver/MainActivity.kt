package com.myhaylow.nexodriver

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(11, 14, 20))
            setPadding(48, 48, 48, 48)
        }

        root.addView(TextView(this).apply {
            text = "NEXO DRIVER"
            textSize = 28f
            setTextColor(Color.rgb(124, 77, 255))
            gravity = Gravity.CENTER
        })

        root.addView(TextView(this).apply {
            text = "Fundacao de desenvolvimento pronta"
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 0)
        })

        setContentView(root)
    }
}
