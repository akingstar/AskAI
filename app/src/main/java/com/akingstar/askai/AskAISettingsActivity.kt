package com.akingstar.askai

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class AskAISettingsActivity : Activity() {
    private lateinit var geminiStatusView: TextView
    private lateinit var groqStatusView: TextView
    private lateinit var usageStatusView: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("askai", MODE_PRIVATE)
        val keys = ProviderKeyStore(this)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        fun heading(value: String) {
            layout.addView(TextView(this).apply {
                text = value
                textSize = 18f
                setPadding(0, 20, 0, 8)
            })
        }

        fun message(value: String) {
            Toast.makeText(this, value, Toast.LENGTH_SHORT).show()
        }

        layout.addView(TextView(this).apply {
            text = "Ask AI सेटिंग"
            textSize = 26f
        })

        heading("AI ऑनलाइन उपयोग सीमा")
        usageStatusView = TextView(this).apply {
            textSize = 16f
            setPadding(0, 4, 0, 8)
        }
        layout.addView(usageStatusView)
        layout.addView(TextView(this).apply {
            text = "रोज़ अधिकतम 20 ऑनलाइन HTTP कोशिशें। Gemini, Groq और Worker retries गिने जाते हैं। Offline कमांड इस सीमा में नहीं गिनी जातीं। यह provider की वास्तविक billing limit नहीं है।"
            textSize = 13f
            setPadding(0, 0, 0, 12)
        })

        heading("परिवार की सुरक्षा")
        val family = EditText(this).apply {
            hint = "परिवार का आपातकालीन नंबर"
            inputType = InputType.TYPE_CLASS_PHONE
            setText(prefs.getString("family_number", ""))
        }
        layout.addView(family)

        heading("Gemini AI")
        geminiStatusView = TextView(this)
        layout.addView(geminiStatusView)
        val gemini = EditText(this).apply {
            hint = "Gemini API key"
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setSingleLine(true)
        }
        layout.addView(gemini)

        layout.addView(Button(this).apply {
            text = "Gemini key सुरक्षित सेव करें"
            setOnClickListener {
                try {
                    if (keys.saveGeminiKey(gemini.text.toString())) {
                        gemini.text.clear()
                        message("Gemini key encrypted storage में सेव हुई।")
                        refreshStatus()
                    } else {
                        message("Valid API key डालें। खाली key सेव नहीं होगी।")
                    }
                } catch (_: Exception) {
                    message("Secure storage उपलब्ध नहीं है। Key सेव नहीं हुई।")
                }
            }
        })

        layout.addView(Button(this).apply {
            text = "Gemini key हटाएँ"
            setOnClickListener {
                try {
                    keys.clearGeminiKey()
                    gemini.text.clear()
                    refreshStatus()
                    message("Gemini key हटाई गई।")
                } catch (_: Exception) {
                    message("Key हटाई नहीं जा सकी।")
                }
            }
        })

        heading("Groq AI")
        groqStatusView = TextView(this)
        layout.addView(groqStatusView)
        val groq = EditText(this).apply {
            hint = "Groq API key"
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setSingleLine(true)
        }
        layout.addView(groq)

        layout.addView(Button(this).apply {
            text = "Groq key सुरक्षित सेव करें"
            setOnClickListener {
                try {
                    if (keys.saveGroqKey(groq.text.toString())) {
                        groq.text.clear()
                        message("Groq key encrypted storage में सेव हुई।")
                        refreshStatus()
                    } else {
                        message("Valid API key डालें। खाली key सेव नहीं होगी।")
                    }
                } catch (_: Exception) {
                    message("Secure storage उपलब्ध नहीं है। Key सेव नहीं हुई।")
                }
            }
        })

        layout.addView(Button(this).apply {
            text = "Groq key हटाएँ"
            setOnClickListener {
                try {
                    keys.clearGroqKey()
                    groq.text.clear()
                    refreshStatus()
                    message("Groq key हटाई गई।")
                } catch (_: Exception) {
                    message("Key हटाई नहीं जा सकी।")
                }
            }
        })

        layout.addView(TextView(this).apply {
            text = "सुरक्षा: API keys फोन के encrypted storage में रहेंगी। इन्हें GitHub, source code या chat में न डालें।"
            textSize = 14f
            setPadding(0, 16, 0, 16)
        })

        heading("अन्य सेटिंग")
        layout.addView(Button(this).apply {
            text = "सेव करें"
            setOnClickListener {
                prefs.edit().putString(
                    "family_number",
                    family.text.toString().filter { it.isDigit() || it == '+' }
                ).apply()
                message("सेटिंग सेव हुई।")
            }
        })

        layout.addView(Button(this).apply {
            text = "मदद / उदाहरण कमांड"
            setOnClickListener {
                startActivity(
                    android.content.Intent(
                        this@AskAISettingsActivity,
                        HelpActivity::class.java
                    )
                )
            }
        })

        setContentView(
            layout,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        refreshStatus()
        refreshUsage()
    }

    override fun onResume() {
        super.onResume()
        if (::usageStatusView.isInitialized) refreshUsage()
        if (::geminiStatusView.isInitialized && ::groqStatusView.isInitialized) {
            refreshStatus()
        }
    }

    private fun refreshUsage() {
        val usage = AIUsageGuard.snapshot(this)
        usageStatusView.text =
            "आज ऑनलाइन प्रयास: ${usage.used} / ${usage.limit}\nशेष प्रयास: ${usage.remaining}"
    }

    private fun refreshStatus() {
        try {
            val keys = ProviderKeyStore(this)
            geminiStatusView.text = if (keys.hasGeminiKey())
                "स्थिति: Gemini key सुरक्षित है"
            else
                "स्थिति: Gemini key सेट नहीं है"

            groqStatusView.text = if (keys.hasGroqKey())
                "स्थिति: Groq key सुरक्षित है"
            else
                "स्थिति: Groq key सेट नहीं है"
        } catch (_: Exception) {
            geminiStatusView.text = "स्थिति: Secure storage error"
            groqStatusView.text = "स्थिति: Secure storage error"
        }
    }
}
