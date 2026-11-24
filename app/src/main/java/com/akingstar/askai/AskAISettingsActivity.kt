package com.akingstar.askai

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class AskAISettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("askai", MODE_PRIVATE)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        layout.addView(TextView(this).apply { text = "Ask AI सेटिंग"; textSize = 26f })
        val family = EditText(this).apply {
            hint = "परिवार का आपातकालीन नंबर"
            inputType = 3
            setText(prefs.getString("family_number", ""))
        }
        layout.addView(family)
        layout.addView(Button(this).apply {
            text = "सेव करें"
            setOnClickListener {
                prefs.edit().putString("family_number", family.text.toString().filter { it.isDigit() || it == '+' }).apply()
            }
        })
        layout.addView(Button(this).apply {
            text = "मदद / उदाहरण कमांड"
            setOnClickListener { startActivity(android.content.Intent(this@AskAISettingsActivity, HelpActivity::class.java)) }
        })
        setContentView(layout)
    }
}
