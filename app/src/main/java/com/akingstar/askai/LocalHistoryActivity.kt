package com.akingstar.askai

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import com.akingstar.askai.automation.AutomationHistoryStore
import com.akingstar.askai.automation.AutomationStore
import com.akingstar.askai.automation.RoutineScheduler

class LocalHistoryActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = TextView(this).apply {
            textSize = 17f
            setPadding(24, 24, 24, 24)
            text = "रूटीन हिस्ट्री और रिमाइंडर लोड हो रहे हैं..."
        }
        setContentView(text)

        val history = AutomationHistoryStore(this)
        val routines = AutomationStore(this).list()

        RoutineScheduler(this).listScheduled(routines) { reminders ->
            runOnUiThread {
                val notificationHistory = getSharedPreferences(
                    "askai_history",
                    MODE_PRIVATE
                ).getString("last_notification", "अभी कोई अतिरिक्त सूचना सेव नहीं है।")

                text.text = buildString {
                    append("ASK AI — ROUTINE HISTORY\\n\\n")
                    append(history.summary())
                    append("\\n\\nSCHEDULED REMINDERS\\n\\n")
                    append(reminders)
                    append("\\n\\nOTHER NOTIFICATIONS\\n\\n")
                    append(notificationHistory)
                }
            }
        }
    }
}
