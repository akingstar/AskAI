package com.akingstar.askai

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class OnboardingActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        box.addView(TextView(this).apply {
            text = "नमस्ते! Ask AI में आपका स्वागत है।\n\nआवाज़ से फोन चलाने के लिए नीचे दी गई अनुमतियाँ अपनी इच्छा से चालू करें।"
            textSize = 22f
        })
        val items = listOf(
            "Accessibility सेवा" to Settings.ACTION_ACCESSIBILITY_SETTINGS,
            "Notification access" to Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS,
            "Overlay permission" to Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "Battery optimization" to Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS,
            "Restricted settings जानकारी" to Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        )
        items.forEach { (label, action) ->
            box.addView(Button(this).apply {
                text = label
                setOnClickListener {
                    val intent = Intent(action)
                    if (action == Settings.ACTION_MANAGE_OVERLAY_PERMISSION ||
                        action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
                        intent.data = android.net.Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                }
            })
        }
        box.addView(Button(this).apply {
            text = "मुख्य स्क्रीन खोलें"
            setOnClickListener { startActivity(Intent(this@OnboardingActivity, MainActivity::class.java)) }
        })
        setContentView(box)
    }
}
