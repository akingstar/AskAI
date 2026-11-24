package com.akingstar.askai

import com.akingstar.askai.automation.AutomationStore
import com.akingstar.askai.automation.AutomationHistoryStore
import com.akingstar.askai.automation.RoutineScheduler

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
        val automationStore = AutomationStore(this)
        val automationHistory = AutomationHistoryStore(this)
        val routineScheduler = RoutineScheduler(this)
        var lastPlan: ActionPlan? = null

        speech = SpeechController(this, { heard ->
            status.text = "आपने कहा: $heard"
            val command = heard.trim()
            val saveMatch = Regex(
                "^(?:routine|रूटीन)\\s+(?:save|सेव(?:\\s+करो)?)\\s+(.+)$",
                RegexOption.IGNORE_CASE
            ).find(command)
            val runMatch = Regex(
                "^(?:routine|रूटीन)\\s+(?:run|chalao|चलाओ)\\s+(.+)$",
                RegexOption.IGNORE_CASE
            ).find(command)
            val deleteMatch = Regex(
                "^(?:routine|रूटीन)\\s+(?:delete|हटाओ|मिटाओ)\\s+(.+)$",
                RegexOption.IGNORE_CASE
            ).find(command)

            val handled = when {
                Regex(
                    "^(?:routine)\\s+every\\s+(.+)\\s+(\\d{1,6})\\s*(?:minutes?|mins?)$|^रूटीन\\s+हर\\s+(.+)\\s+(\\d{1,6})\\s*मिनट$",
                    RegexOption.IGNORE_CASE
                ).find(command) != null -> {
                    val match = Regex(
                        "^(?:routine)\\s+every\\s+(.+)\\s+(\\d{1,6})\\s*(?:minutes?|mins?)$|^रूटीन\\s+हर\\s+(.+)\\s+(\\d{1,6})\\s*मिनट$",
                        RegexOption.IGNORE_CASE
                    ).find(command)!!
                    val name = (match.groupValues[1].ifBlank { match.groupValues[3] }).trim()
                    val interval = (match.groupValues[2].ifBlank { match.groupValues[4] }).toLongOrNull() ?: 0L
                    val exists = automationStore.list().any { it.name.equals(name, true) }
                    val ok = exists && routineScheduler.scheduleRecurring(name, interval)
                    val message = when {
                        !exists -> "यह सेव रूटीन नहीं मिली।"
                        ok -> "$name का रिमाइंडर हर $interval मिनट में दोहराया जाएगा।"
                        else -> "दोहराने का अंतर कम से कम 15 मिनट होना चाहिए।"
                    }
                    status.text = message
                    tts.speak(message)
                    true
                }



                Regex(
                    "^(?:routine|रूटीन)\\s+(?:schedule|remind|याद दिलाना|रिमाइंडर)\\s+(.+)\\s+(\\d{1,6})\\s*(?:minutes?|mins?|मिनट)?$",
                    RegexOption.IGNORE_CASE
                ).find(command) != null -> {
                    val match = Regex(
                        "^(?:routine|रूटीन)\\s+(?:schedule|remind|याद दिलाना|रिमाइंडर)\\s+(.+)\\s+(\\d{1,6})\\s*(?:minutes?|mins?|मिनट)?$",
                        RegexOption.IGNORE_CASE
                    ).find(command)!!
                    val name = match.groupValues[1].trim()
                    val minutes = match.groupValues[2].toLongOrNull() ?: 0L
                    val exists = automationStore.list().any {
                        it.name.equals(name, true)
                    }
                    val ok = exists && routineScheduler.schedule(name, minutes)
                    val message = when {
                        !exists -> "यह सेव रूटीन नहीं मिली। पहले रूटीन सेव कीजिए।"
                        ok -> "$name रूटीन का रिमाइंडर $minutes मिनट बाद आएगा।"
                        else -> "रिमाइंडर शेड्यूल नहीं हो सका। समय 1 मिनट से अधिक होना चाहिए।"
                    }
                    status.text = message
                    tts.speak(message)
                    true
                }

                Regex(
                    "^(?:routine|रूटीन)\\s+(?:cancel|unschedule|रिमाइंडर हटाओ)\\s+(.+)$",
                    RegexOption.IGNORE_CASE
                ).find(command) != null -> {
                    val name = Regex(
                        "^(?:routine|रूटीन)\\s+(?:cancel|unschedule|रिमाइंडर हटाओ)\\s+(.+)$",
                        RegexOption.IGNORE_CASE
                    ).find(command)!!.groupValues[1].trim()
                    val ok = routineScheduler.cancel(name)
                    val message = if (ok) {
                        "$name रूटीन का रिमाइंडर रद्द कर दिया।"
                    } else {
                        "रिमाइंडर रद्द नहीं हो सका।"
                    }
                    status.text = message
                    tts.speak(message)
                    true
                }


                command.matches(Regex(
                    "^(?:routine history|रूटीन हिस्ट्री|रूटीन इतिहास)$",
                    RegexOption.IGNORE_CASE
                )) -> {
                    val message = automationHistory.summary()
                    status.text = message
                    tts.speak(message)
                    true
                }

                command.matches(Regex(
                    "^(?:meri\\s+)?(?:routines|routine list|रूटीन(?:\\s+दिखाओ)?)$",
                    RegexOption.IGNORE_CASE
                )) || command.equals("मेरी रूटीन दिखाओ", true) -> {
                    val items = automationStore.list()
                    val message = if (items.isEmpty()) {
                        "अभी कोई रूटीन सेव नहीं है।"
                    } else {
                        "आपकी सेव रूटीन हैं: " +
                            items.joinToString("، ") { it.name }
                    }
                    status.text = message
                    tts.speak(message)
                    true
                }

                saveMatch != null -> {
                    val plan = lastPlan
                    val name = saveMatch.groupValues[1].trim()
                    val ok = plan != null &&
                        automationStore.save(name, PlanValidator.encode(plan))
                    val message = when {
                        plan == null -> "पहले कोई काम बोलकर करवाइए, फिर रूटीन सेव कीजिए।"
                        ok -> "$name रूटीन सेव हो गई।"
                        else -> "रूटीन सेव नहीं हो सकी। नाम या कमांड जाँचिए।"
                    }
                    status.text = message
                    tts.speak(message)
                    true
                }

                runMatch != null -> {
                    val name = runMatch.groupValues[1].trim()
                    val routine = automationStore.list()
                        .firstOrNull { it.name.equals(name, true) }
                    val plan = routine?.let { PlanValidator.parse(it.planJson) }
                    if (plan == null) {
                        val message = "यह रूटीन नहीं मिली।"
                        status.text = message
                        tts.speak(message)
                    } else {
                        status.text = "$name रूटीन शुरू कर रहा हूँ।"
                        tts.speak("जी, रूटीन शुरू कर रहा हूँ।")
                        executor.execute(plan) { result ->
                            val finalStatus = when {
                                result.failedSteps == 0 && result.cancelledSteps == 0 -> "SUCCESS"
                                result.succeededSteps == 0 && result.failedSteps == 0 -> "CANCELLED"
                                result.succeededSteps == 0 -> "FAILED"
                                else -> "PARTIAL"
                            }
                            automationHistory.record(name, finalStatus)
                            val detail = "$name: $finalStatus (${result.succeededSteps}/${result.totalSteps})"
                            status.text = detail
                            tts.speak(
                                when (finalStatus) {
                                    "SUCCESS" -> "रूटीन पूरी हो गई।"
                                    "CANCELLED" -> "रूटीन के कुछ काम रद्द किए गए।"
                                    "FAILED" -> "रूटीन पूरी नहीं हो सकी।"
                                    else -> "रूटीन आंशिक रूप से पूरी हुई।"
                                }
                            )
                        }
                    }
                    true
                }

                deleteMatch != null -> {
                    val name = deleteMatch.groupValues[1].trim()
                    val routine = automationStore.list()
                        .firstOrNull { it.name.equals(name, true) }
                    if (routine == null) {
                        status.text = "यह रूटीन नहीं मिली।"
                        tts.speak("यह रूटीन नहीं मिली।")
                    } else {
                        android.app.AlertDialog.Builder(this@MainActivity)
                            .setTitle("रूटीन हटाएँ?")
                            .setMessage("$name रूटीन हमेशा के लिए हट जाएगी।")
                            .setNegativeButton("रहने दें", null)
                            .setPositiveButton("हटाएँ") { _, _ ->
                                val ok = automationStore.delete(routine.id)
                                val message = if (ok) "रूटीन हटा दी गई।"
                                              else "रूटीन नहीं हट सकी।"
                                status.text = message
                                tts.speak(message)
                            }
                            .show()
                    }
                    true
                }

                else -> false
            }

            if (!handled) {
                AIClient.request(this@MainActivity, heard) { plan ->
                    if (plan == null) {
                        status.text = "कमांड समझ नहीं आई।"
                        tts.speak("माफ कीजिए, मैं समझ नहीं पाया।")
                    } else {
                        lastPlan = plan
                        status.text = "कमांड मिली: ${plan.steps.size} काम"
                        tts.speak("जी, समझ गया।")
                        executor.execute(plan)
                    }
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
