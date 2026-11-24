package com.akingstar.askai

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class LocalHistoryActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = TextView(this)
        text.textSize = 18f
        text.setPadding(24, 24, 24, 24)
        text.text = getSharedPreferences("askai_history", MODE_PRIVATE)
            .getString("last_notification", "अभी कोई सूचना सेव नहीं है।")
        setContentView(text)
    }
}
