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
            val retryMatch = Regex(
                "^(?:routine|रूटीन)\\s+(?:retry|दोबारा|फिर से)\\s+(.+)$",
                RegexOption.IGNORE_CASE
            ).find(command)
            val deleteMatch = Regex(
                "^(?:routine|रूटीन)\\s+(?:delete|हटाओ|मिटाओ)\\s+(.+)$",
                RegexOption.IGNORE_CASE
            ).find(command)
            val updateMatch = Regex(
                "^(?:routine|रूटीन)\\s+(?:update|edit|अपडेट|बदल दो)\\s+(.+)$",
                RegexOption.IGNORE_CASE
            ).find(command)
            val renameMatch = Regex(
                "^(?:routine|रूटीन)\\s+(?:rename|नाम बदलो)\\s+(.+?)\\s+(?:to|से)\\s+(.+)$",
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
                    "^(?:routine reminders?|reminders|रूटीन रिमाइंडर(?: दिखाओ)?|रिमाइंडर दिखाओ)$",
                    RegexOption.IGNORE_CASE
                )) -> {
                    status.text = "Scheduled reminders देख रहा हूँ..."
                    tts.speak("जी, आपके रिमाइंडर देख रहा हूँ।")
                    routineScheduler.listScheduled(automationStore.list()) { message ->
                        runOnUiThread {
                            status.text = message
                            val spoken = if (message.startsWith("अभी कोई")) {
                                message
                            } else {
                                "आपके scheduled reminders स्क्रीन पर दिखा दिए हैं। " +
                                    message.lines().drop(1).take(3).joinToString("। ")
                            }
                            tts.speak(spoken.take(350))
                        }
                    }
                    true
                }
                command.matches(Regex(
                    "^(?:routine history clear|रूटीन हिस्ट्री साफ करो|रूटीन इतिहास मिटाओ)$",
                    RegexOption.IGNORE_CASE
                )) -> {
                    android.app.AlertDialog.Builder(this@MainActivity)
                        .setTitle("हिस्ट्री साफ करें?")
                        .setMessage("रूटीन की पूरी execution history हट जाएगी।")
                        .setNegativeButton("रहने दें", null)
                        .setPositiveButton("साफ करें") { _, _ ->
                            val ok = automationHistory.clear()
                            val message = if (ok) "रूटीन हिस्ट्री साफ हो गई।"
                                          else "हिस्ट्री साफ नहीं हो सकी।"
                            status.text = message
                            tts.speak(message)
                        }
                        .show()
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

                renameMatch != null -> {
                    val oldName = renameMatch.groupValues[1].trim()
                    val newName = renameMatch.groupValues[2].trim()
                    val existing = automationStore.list()
                        .firstOrNull { it.name.equals(oldName, true) }

                    if (existing == null) {
                        val message = "यह रूटीन नहीं मिली।"
                        status.text = message
                        tts.speak(message)
                    } else {
                        android.app.AlertDialog.Builder(this@MainActivity)
                            .setTitle("रूटीन का नाम बदलें?")
                            .setMessage("$oldName का नाम बदलकर $newName किया जाएगा। पुराने नाम का reminder cancel होगा।")
                            .setNegativeButton("रहने दें", null)
                            .setPositiveButton("नाम बदलें") { _, _ ->
                                val ok = automationStore.rename(oldName, newName)
                                if (ok) routineScheduler.cancel(oldName)
                                val message = if (ok) {
                                    "रूटीन का नाम बदल दिया। नए नाम के लिए reminder दोबारा schedule करें।"
                                } else {
                                    "नाम नहीं बदला। नाम की लंबाई या duplicate नाम जाँचिए।"
                                }
                                status.text = message
                                tts.speak(message)
                            }
                            .show()
                    }
                    true
                }

                updateMatch != null -> {
                    val name = updateMatch.groupValues[1].trim()
                    val existing = automationStore.list()
                        .firstOrNull { it.name.equals(name, true) }
                    val plan = lastPlan

                    when {
                        existing == null -> {
                            val message = "यह सेव रूटीन नहीं मिली।"
                            status.text = message
                            tts.speak(message)
                        }
                        plan == null -> {
                            val message = "पहले नया काम बोलिए, फिर रूटीन अपडेट कहिए।"
                            status.text = message
                            tts.speak(message)
                        }
                        else -> {
                            android.app.AlertDialog.Builder(this@MainActivity)
                                .setTitle("रूटीन अपडेट करें?")
                                .setMessage("$name की पुरानी instructions नई बोली गई instructions से बदलें?")
                                .setNegativeButton("रहने दें", null)
                                .setPositiveButton("अपडेट करें") { _, _ ->
                                    val ok = automationStore.save(
                                        existing.name,
                                        PlanValidator.encode(plan)
                                    )
                                    val message = if (ok) {
                                        "$name रूटीन अपडेट हो गई। इसे चलाने के लिए अलग से बोलें।"
                                    } else {
                                        "रूटीन अपडेट नहीं हो सकी।"
                                    }
                                    status.text = message
                                    tts.speak(message)
                                }
                                .show()
                        }
                    }
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

                runMatch != null || retryMatch != null -> {
                    val isRetry = retryMatch != null
                    val name = (runMatch ?: retryMatch!!).groupValues[1].trim()
                    val routine = automationStore.list()
                        .firstOrNull { it.name.equals(name, true) }
                    val plan = routine?.let { PlanValidator.parse(it.planJson) }

                    if (plan == null) {
                        val message = "यह रूटीन नहीं मिली।"
                        status.text = message
                        tts.speak(message)
                    } else {
                        val savedPlanJson = routine?.planJson.orEmpty()
                        val retryIndices = if (isRetry) {
                            automationHistory.latestFailedStepIndices(name, savedPlanJson)
                        } else {
                            plan.steps.indices.toList()
                        }

                        when {
                            retryIndices == null -> {
                                val message = "पिछला विस्तृत रिकॉर्ड नहीं मिला या रूटीन बदल गई है। पहले रूटीन चलाइए।"
                                status.text = message
                                tts.speak(message)
                            }
                            retryIndices.isEmpty() -> {
                                val message = "कोई retry योग्य failed काम नहीं है।"
                                status.text = message
                                tts.speak(message)
                            }
                            else -> {
                                status.text = if (isRetry) {
                                    "$name के failed काम दोबारा शुरू कर रहा हूँ।"
                                } else {
                                    "$name रूटीन शुरू कर रहा हूँ।"
                                }
                                tts.speak(
                                    if (isRetry) "जी, सिर्फ असफल काम दोबारा करने की कोशिश कर रहा हूँ।"
                                    else "जी, रूटीन शुरू कर रहा हूँ।"
                                )
                                val started = automationHistory.beginExecution(
                                    name = name,
                                    planJson = savedPlanJson,
                                    selectedIndices = retryIndices,
                                    merge = isRetry
                                )
                                if (!started) {
                                    val message = "Execution record सुरक्षित नहीं हो सका। कोई काम शुरू नहीं किया गया।"
                                    status.text = message
                                    tts.speak(message)
                                } else {
                                    automationHistory.record(
                                        name,
                                        if (isRetry) "RETRY_STARTED" else "STARTED"
                                    )
                                    executor.executeSelected(
                                        plan = plan,
                                        requestedIndices = retryIndices,
                                        onStepResult = { stepResult ->
                                            automationHistory.recordStep(
                                                name, savedPlanJson, stepResult
                                            )
                                        },
                                        onComplete = { result ->
                                            val finalStatus = when {
                                                result.failedSteps == 0 &&
                                                    result.cancelledSteps == 0 &&
                                                    result.skippedSteps == 0 -> "SUCCESS"
                                                result.succeededSteps == 0 &&
                                                    result.failedSteps == 0 &&
                                                    result.skippedSteps == 0 -> "CANCELLED"
                                                result.succeededSteps == 0 && result.failedSteps > 0 -> "FAILED"
                                                else -> "PARTIAL"
                                            }
                                            val saved = automationHistory.finishExecution(finalStatus)
                                            automationHistory.record(
                                                name,
                                                if (saved) finalStatus else "PERSISTENCE_FAILED"
                                            )
                                            val detail = if (saved) {
                                                "$name: $finalStatus (${result.succeededSteps}/${result.totalSteps})"
                                            } else {
                                                "$name: परिणाम सुरक्षित नहीं हो सके। दोबारा retry करने से पहले जाँच जरूरी है।"
                                            }
                                            status.text = detail
                                            tts.speak(
                                                if (!saved) {
                                                    "काम पूरे हुए, लेकिन उनका रिकॉर्ड सुरक्षित नहीं हो सका।"
                                                } else when (finalStatus) {
                                                    "SUCCESS" -> if (isRetry) {
                                                        "असफल कामों की retry पूरी हो गई।"
                                                    } else {
                                                        "रूटीन पूरी हो गई।"
                                                    }
                                                    "CANCELLED" -> "रूटीन के कुछ काम रद्द किए गए।"
                                                    "FAILED" -> "रूटीन पूरी नहीं हो सकी।"
                                                    else -> "रूटीन आंशिक रूप से पूरी हुई।"
                                                }
                                            )
                                        }
                                    )
                                }
                            }
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
                                if (ok) routineScheduler.cancel(routine.name)
                                val message = if (ok) "रूटीन और उसका reminder हटा दिया गया।"
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
                AIClient.request(this@MainActivity, heard) { result ->
                    when (result) {
                        is AIRequestResult.Success -> {
                            val plan = result.plan
                            lastPlan = plan
                            status.text = if (result.offline) {
                                "Offline कमांड मिली: ${plan.steps.size} काम"
                            } else {
                                "कमांड मिली: ${plan.steps.size} काम"
                            }
                            tts.speak("जी, समझ गया।")
                            executor.execute(plan)
                        }

                        AIRequestResult.UnrecognizedCommand -> {
                            status.text = "कमांड समझ नहीं आई। कृपया दोबारा बोलें।"
                            tts.speak("माफ कीजिए, यह कमांड समझ नहीं आई। कृपया दोबारा बोलें।")
                        }

                        AIRequestResult.InvalidEndpoint -> {
                            status.text = "Worker URL गलत है। HTTPS URL जाँचें।"
                            tts.speak("AI सेवा का सुरक्षित पता सही नहीं है।")
                        }

                        AIRequestResult.NetworkError -> {
                            status.text = "इंटरनेट कनेक्शन या नेटवर्क में समस्या है।"
                            tts.speak("इंटरनेट कनेक्शन में समस्या है। कृपया बाद में कोशिश करें।")
                        }

                        is AIRequestResult.ServerError -> {
                            status.text = "AI सेवा में समस्या है। HTTP ${result.statusCode}"
                            tts.speak("AI सेवा अभी उपलब्ध नहीं है।")
                        }

                        AIRequestResult.InvalidResponse -> {
                            status.text = "AI से सही जवाब नहीं मिला। कृपया दोबारा कोशिश करें।"
                            tts.speak("AI से सही जवाब नहीं मिला।")
                        }
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
