package com.akingstar.askai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var permissionList: LinearLayout
    private lateinit var speech: SpeechController
    private lateinit var tts: TextToSpeechController
    private lateinit var executor: ActionExecutor
    private val permissions = arrayListOf(
        Manifest.permission.RECORD_AUDIO
    )
    private val languages = listOf("हिन्दी" to "hi-IN", "English" to "en-IN")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        status = findViewById(R.id.status)
        permissionList = findViewById(R.id.permissionList)
        val spinner = findViewById<Spinner>(R.id.languageSpinner)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages.map { it.first })
        tts = TextToSpeechController(this) {}
        executor = ActionExecutor(this) { tts.speak(it) }
        speech = SpeechController(this, { heard ->
            status.text = "आपने कहा: $heard"
            AIClient.request(this, heard) { plan ->
                if (plan == null) {
                    status.text = "कमांड समझ नहीं आई।"
                    tts.speak("माफ कीजिए, मैं समझ नहीं पाया।")
                } else {
                    status.text = "कमांड मिली: ${plan.steps.size} काम"
                    tts.speak("जी, समझ गया।")
                    executor.execute(plan)
                }
            }
        }, { status.text = it })
        findViewById<Button>(R.id.micButton).setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                speech.language = languages[spinner.selectedItemPosition].second
                tts.setLanguage(speech.language)
                speech.start()
            } else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
        }
        findViewById<Button>(R.id.checkPermissions).setOnClickListener { showPermissions() }
        val prefs = getSharedPreferences("askai", MODE_PRIVATE)
        val url = findViewById<EditText>(R.id.workerUrl)
        url.setText(prefs.getString("worker_url", ""))
        findViewById<Button>(R.id.saveWorker).setOnClickListener {
            val endpoint = url.text.toString().trim()
            if (endpoint.isNotEmpty() && !AIClient.isSecureEndpoint(endpoint)) {
                status.text = "सुरक्षित HTTPS Worker URL डालें।"
                return@setOnClickListener
            }
            prefs.edit().putString("worker_url", endpoint).apply()
            status.text = if (endpoint.isBlank()) {
                "Offline मोड सेव हो गया। आपकी कमांड फोन पर ही समझी जाएगी।"
            } else {
                "HTTPS Worker सेव हुआ। आपकी कमांड का टेक्स्ट इस सर्वर पर भेजा जाएगा।"
            }
        }
        showPermissions()
    }

    private fun showPermissions() {
        permissionList.removeAllViews()
        val all = permissions + if (Build.VERSION.SDK_INT >= 33) listOf(Manifest.permission.POST_NOTIFICATIONS) else emptyList()
        all.forEach { permission ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(4, 8, 4, 8) }
            val granted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
            val label = TextView(this).apply { text = permission.substringAfterLast(".") + if (granted) " ✓" else " आवश्यक"; textSize = 15f }
            val button = Button(this).apply { text = "अनुमति"; setOnClickListener { ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), 101) } }
            row.addView(label, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(button)
            permissionList.addView(row)
        }
        permissionList.addView(Button(this).apply {
            text = "Accessibility सेवा सेटिंग"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        showPermissions()
    }

    override fun onDestroy() {
        speech.stop()
        tts.close()
        super.onDestroy()
    }
}
