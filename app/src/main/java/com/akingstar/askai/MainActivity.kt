package com.akingstar.askai

import com.akingstar.askai.automation.AutomationStore
import com.akingstar.askai.automation.AutomationHistoryStore
import com.akingstar.askai.automation.RoutineScheduler

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
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
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var permissionList: LinearLayout
    private lateinit var speech: SpeechController
    private lateinit var tts: TextToSpeechController
    private lateinit var executor: ActionExecutor
    private lateinit var micButton: Button
    private val commandExecutionGate = CommandExecutionGate()
    private val conversationMemory = ConversationMemory()
    private val whatsAppDraftFlow = WhatsAppDraftFlow()
    private val whatsAppVoiceSessionGuard = WhatsAppVoiceSessionGuard()
    private var pendingWhatsAppNumber: String? = null
    private var whatsAppVoiceConsent = false
    private var whatsAppResumeListening = false
    private var activityResumed = false
    private val whatsAppSpeechRetryPolicy = WhatsAppSpeechRetryPolicy()

    private fun invalidateWhatsAppVoiceSession() {
        whatsAppVoiceSessionGuard.invalidate()
        if (::speech.isInitialized) {
            speech.onFailure = null
            speech.stop()
        }
    }

    private fun acquireCommandExecution(): Long? {
        val token = commandExecutionGate.tryAcquire() ?: return null
        if (::micButton.isInitialized && !isFinishing && !isDestroyed) {
            micButton.isEnabled = false
        }
        return token
    }

    private fun releaseCommandExecution(token: Long) {
        if (!commandExecutionGate.release(token)) return
        if (::micButton.isInitialized && !isFinishing && !isDestroyed) {
            micButton.isEnabled = true
        }
    }
    private val permissions = arrayListOf(
        Manifest.permission.RECORD_AUDIO
    )
    private val languages = listOf("हिन्दी" to "hi-IN", "English" to "en-IN")
    private val contactPickRequestCode = 2026

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getSharedPreferences("askai_history", MODE_PRIVATE)
            .edit().remove("last_notification").apply()
        status = findViewById(R.id.status)
        permissionList = findViewById(R.id.permissionList)
        micButton = findViewById(R.id.micButton)
        val spinner = findViewById<Spinner>(R.id.languageSpinner)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages.map { it.first })
        tts = TextToSpeechController(this) {}
        executor = ActionExecutor(
            this,
            { tts.speak(it) },
            { ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                103
            ) },
            {
                val intent = Intent(
                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    android.net.Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        )
        val automationStore = AutomationStore(this)
        val automationHistory = AutomationHistoryStore(this)
        val routineScheduler = RoutineScheduler(this)
        var lastPlan: ActionPlan? = null

        speech = SpeechController(this, { heard ->
            if (whatsAppDraftFlow.state != WhatsAppDraftFlow.State.IDLE) {
                whatsAppSpeechRetryPolicy.onSuccess()
                handleWhatsAppVoiceInput(heard)
            } else if (commandExecutionGate.isBusy()) {
                status.text = "पिछला काम अभी चल रहा है। कृपया उसके पूरा होने तक रुकें।"
                tts.speak("पिछला काम अभी चल रहा है।")
            } else {
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
                                val executionToken = acquireCommandExecution()
                                if (executionToken == null) {
                                    val message = "दूसरा काम अभी चल रहा है। कृपया उसके पूरा होने तक रुकें।"
                                    status.text = message
                                    tts.speak(message)
                                } else {
                                try {
                                val started = automationHistory.beginExecution(
                                    name = name,
                                    planJson = savedPlanJson,
                                    selectedIndices = retryIndices,
                                    merge = isRetry
                                )
                                if (!started) {
                                    releaseCommandExecution(executionToken)
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
                                            try {
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
                                            } catch (_: Exception) {
                        runCatching {
                            status.text = "रूटीन पूरी करते समय रिकॉर्ड या स्थिति अपडेट नहीं हो सकी।"
                        }
                        runCatching {
                            tts.speak("रूटीन पूरी करते समय समस्या हुई।")
                        }
                    } finally {
                                                releaseCommandExecution(executionToken)
                                            }
                                        }
                                    )
                                }
                                } catch (_: Exception) {
                                    releaseCommandExecution(executionToken)
                                    status.text = "रूटीन शुरू नहीं हो सकी। रिकॉर्ड और स्थिति जाँचें।"
                                    tts.speak("रूटीन शुरू नहीं हो सकी।")
                                }
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
                val executionToken = acquireCommandExecution()
                if (executionToken == null) {
                    val message = "दूसरा काम अभी चल रहा है। कृपया उसके पूरा होने तक रुकें।"
                    status.text = message
                    tts.speak(message)
                } else {
                    val callbackHandled = AtomicBoolean(false)
                    var executionStarted = false
                    try {
                        AIClient.request(this@MainActivity, heard, conversationMemory.snapshot()) { result ->
                            if (!callbackHandled.compareAndSet(false, true)) return@request
                            if (isFinishing || isDestroyed) {
                                releaseCommandExecution(executionToken)
                                return@request
                            }

                            try {
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
                                    try {
                                        executionStarted = true
                        executor.execute(plan) { summary ->
                                            try {
                                            if (!result.offline && !isFinishing && !isDestroyed) {
                                                conversationMemory.record(heard, summary)
                                            }
                                            } finally {
                                                releaseCommandExecution(executionToken)
                                            }
                                        }
                                    } catch (_: Exception) {
                                        releaseCommandExecution(executionToken)
                                        status.text = "काम शुरू नहीं हो सका। कृपया दोबारा कोशिश करें।"
                                        tts.speak("काम शुरू नहीं हो सका।")
                                    }
                                }

                                AIRequestResult.UnrecognizedCommand -> {
                                    status.text = "कमांड समझ नहीं आई। कृपया दोबारा बोलें।"
                                    tts.speak("माफ कीजिए, यह कमांड समझ नहीं आई। कृपया दोबारा बोलें।")
                                    releaseCommandExecution(executionToken)
                                }

                                AIRequestResult.DuplicateRequest -> {
                                    status.text = "यह कमांड अभी भेजी गई थी। थोड़ी देर बाद दोबारा कोशिश करें।"
                                    tts.speak("यह कमांड अभी भेजी गई थी। कृपया थोड़ी देर रुकें।")
                                    releaseCommandExecution(executionToken)
                                }

                                AIRequestResult.UsageLimitReached -> {
                                    status.text = "आज की 20 ऑनलाइन AI कोशिशें पूरी हो गईं। Offline कमांड उपलब्ध हैं।"
                                    tts.speak("आज की ऑनलाइन AI सीमा पूरी हो गई है। ऑफलाइन कमांड अभी भी उपलब्ध हैं।")
                                    releaseCommandExecution(executionToken)
                                }

                                AIRequestResult.InvalidEndpoint -> {
                                    status.text = "Worker URL गलत है। HTTPS URL जाँचें।"
                                    tts.speak("AI सेवा का सुरक्षित पता सही नहीं है।")
                                    releaseCommandExecution(executionToken)
                                }

                                AIRequestResult.NetworkError -> {
                                    status.text = "इंटरनेट कनेक्शन या नेटवर्क में समस्या है।"
                                    tts.speak("इंटरनेट कनेक्शन में समस्या है। कृपया बाद में कोशिश करें।")
                                    releaseCommandExecution(executionToken)
                                }

                                is AIRequestResult.ServerError -> {
                                    status.text = AIErrorMessages.forServerError(result.statusCode).first
                                    tts.speak(AIErrorMessages.forServerError(result.statusCode).second)
                                    releaseCommandExecution(executionToken)
                                }

                                AIRequestResult.InvalidResponse -> {
                                    status.text = "AI से सही जवाब नहीं मिला। कृपया दोबारा कोशिश करें।"
                                    tts.speak("AI से सही जवाब नहीं मिला।")
                                    releaseCommandExecution(executionToken)
                                }
                            }
                        } catch (_: Exception) {
                            if (!executionStarted) {
                                releaseCommandExecution(executionToken)
                            }
                            runCatching {
                                status.text = "AI request संभालने में समस्या हुई। कृपया दोबारा कोशिश करें।"
                            }
                            runCatching {
                                tts.speak("AI request में समस्या हुई।")
                            }
                        }

                        }
                    } catch (_: Exception) {
                        releaseCommandExecution(executionToken)
                        status.text = "AI request शुरू नहीं हो सकी। कृपया दोबारा कोशिश करें।"
                        tts.speak("AI सेवा से संपर्क नहीं हो सका।")
                    }
                }
            }
            }
        }, { status.text = it })
        micButton.setOnClickListener {
            if (commandExecutionGate.isBusy()) {
                status.text = "पिछला काम अभी चल रहा है।"
                return@setOnClickListener
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                speech.language = languages[spinner.selectedItemPosition].second
                tts.setLanguage(speech.language)
                speech.start()
            } else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
        }
        findViewById<Button>(R.id.chooseContact).setOnClickListener {
            launchContactPicker()
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
        permissionList.addView(Button(this).apply {
            text = "Ask AI Provider सेटिंग"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, AskAISettingsActivity::class.java))
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 102) {
            val granted = grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (granted && whatsAppDraftFlow.state != WhatsAppDraftFlow.State.IDLE) {
                whatsAppResumeListening = false
                showWhatsAppListeningPrompt()
            } else {
                invalidateWhatsAppVoiceSession()
                whatsAppVoiceConsent = false
                whatsAppResumeListening = false
                speech.onFailure = null
                whatsAppDraftFlow.reset()
                whatsAppSpeechRetryPolicy.reset()
                pendingWhatsAppNumber = null
                status.text = "माइक्रोफोन की अनुमति नहीं मिली।"
                tts.speak("माइक्रोफोन की अनुमति नहीं मिली।")
            }
        } else if (requestCode == 100) {
            val granted = grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED

            if (granted && !commandExecutionGate.isBusy()) {
                val spinner = findViewById<Spinner>(R.id.languageSpinner)
                val selectedIndex = spinner.selectedItemPosition
                    .coerceIn(0, languages.lastIndex)

                speech.language = languages[selectedIndex].second
                tts.setLanguage(speech.language)
                speech.start()
            } else {
                status.text = "Voice इस्तेमाल करने के लिए माइक्रोफोन की अनुमति जरूरी है।"
                tts.speak("Voice इस्तेमाल करने के लिए माइक्रोफोन की अनुमति दें।")
            }
        } else if (requestCode == 103) {
            val granted = grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            val message = if (granted) {
                "Camera अनुमति मिल गई। अब टॉर्च कमांड दोबारा बोलें।"
            } else {
                "Camera अनुमति नहीं मिली। टॉर्च चालू नहीं की गई।"
            }
            status.text = message
            tts.speak(message)
        }

        showPermissions()
    }

    private fun launchContactPicker() {
        val intent = Intent(
            Intent.ACTION_PICK,
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        )
        try {
            startActivityForResult(intent, contactPickRequestCode)
        } catch (_: Exception) {
            status.text = "इस फोन में संपर्क चुनने की सुविधा उपलब्ध नहीं है।"
            tts.speak("संपर्क चुनने की सुविधा उपलब्ध नहीं है।")
        }
    }

    @Deprecated("Uses the platform contact picker result")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != contactPickRequestCode || resultCode != RESULT_OK) return

        val selected = data?.data?.let { uri ->
            try {
                contentResolver.query(
                    uri,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    ),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (!cursor.moveToFirst()) {
                        null
                    } else {
                        val nameIndex = cursor.getColumnIndex(
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                        )
                        val numberIndex = cursor.getColumnIndex(
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                        )
                        val name = if (nameIndex >= 0) {
                            cursor.getString(nameIndex).orEmpty()
                        } else ""
                        val number = if (numberIndex >= 0) {
                            cursor.getString(numberIndex).orEmpty()
                        } else ""
                        name to number
                    }
                }
            } catch (_: Exception) {
                null
            }
        }

        if (selected == null) {
            status.text = "संपर्क का नंबर नहीं मिल सका।"
            tts.speak("संपर्क का नंबर नहीं मिल सका।")
            return
        }

        val (name, rawNumber) = selected
        val number = PhoneNumberValidator.normalize(rawNumber)

        if (number == null) {
            status.text = "चुने गए संपर्क का नंबर मान्य नहीं है।"
            tts.speak("यह फोन नंबर मान्य नहीं है।")
            return
        }

        val displayName = name.ifBlank { "चुना गया संपर्क" }
        showContactActionDialog(displayName, number)
    }

    private fun showContactActionDialog(name: String, number: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle("संपर्क: $name")
            .setMessage("$number\n\nआप क्या करना चाहते हैं?")
            .setItems(arrayOf("कॉल", "SMS", "WhatsApp", "रद्द करें")) { _, which ->
                when (which) {
                    0 -> {
                        try {
                            startActivity(
                                Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null))
                            )
                            status.text = "Dialer खुल गया। नंबर जाँचकर कॉल करें।"
                            tts.speak("Dialer खुल गया है। नंबर जाँच लें।")
                        } catch (_: Exception) {
                            status.text = "Dialer नहीं खुल सका।"
                            tts.speak("Dialer नहीं खुल सका।")
                        }
                    }
                    1 -> showSmsEditor(name, number)
                    2 -> beginWhatsAppDraft(name, number)
                    else -> status.text = "कोई कार्रवाई नहीं की गई।"
                }
            }
            .show()
    }

    private fun beginWhatsAppDraft(name: String, number: String) {
        if (commandExecutionGate.isBusy()) {
            status.text = "पहले चल रहा काम पूरा होने दें।"
            tts.speak("पहले चल रहा काम पूरा होने दें।")
            return
        }

        if (!number.matches(Regex("^\\+?[0-9]{7,15}$"))) {
            status.text = "WhatsApp के लिए नंबर मान्य नहीं है।"
            tts.speak("फोन नंबर मान्य नहीं है।")
            return
        }

        pendingWhatsAppNumber = number
        android.app.AlertDialog.Builder(this)
            .setTitle("WhatsApp voice message")
            .setMessage(
                "$name के लिए संदेश बोलकर लिखा जाएगा। " +
                "माइक्रोफोन आपकी अनुमति के बाद ही शुरू होगा। " +
                "WhatsApp में भेजने के लिए आपको खुद Send दबाना होगा।"
            )
            .setNegativeButton("रद्द करें") { _, _ ->
                invalidateWhatsAppVoiceSession()
                whatsAppDraftFlow.reset()
                whatsAppSpeechRetryPolicy.reset()
                pendingWhatsAppNumber = null
            }
            .setPositiveButton("अनुमति दें") { _, _ ->
                invalidateWhatsAppVoiceSession()
                whatsAppVoiceSessionGuard.begin()
                whatsAppSpeechRetryPolicy.reset()
                whatsAppDraftFlow.begin()
                whatsAppVoiceConsent = true
                whatsAppResumeListening = false
                if (ContextCompat.checkSelfPermission(
                        this, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    showWhatsAppListeningPrompt()
                } else {
                    ActivityCompat.requestPermissions(
                        this, arrayOf(Manifest.permission.RECORD_AUDIO), 102
                    )
                }
            }
            .show()
    }

    private fun showWhatsAppListeningPrompt() {
        if (whatsAppDraftFlow.state == WhatsAppDraftFlow.State.IDLE ||
            !whatsAppVoiceConsent
        ) return

        val prompt = if (
            whatsAppDraftFlow.state == WhatsAppDraftFlow.State.AWAITING_MESSAGE
        ) {
            "अब अपना WhatsApp संदेश बोलिए।"
        } else {
            "अब हाँ, बदलो या रद्द करो बोलिए।"
        }

        speakThenListen(prompt)
    }

    private fun speakThenListen(prompt: String) {
        if (!whatsAppVoiceConsent ||
            whatsAppDraftFlow.state == WhatsAppDraftFlow.State.IDLE
        ) return

        status.text = prompt
        val sessionId = whatsAppVoiceSessionGuard.currentSessionId()
            ?: return
        tts.speak(prompt) {
            if (whatsAppVoiceSessionGuard.isActive(sessionId)) {
                startWhatsAppListening(sessionId)
            }
        }
    }

    private fun startWhatsAppListening(sessionId: Long) {
        if (!whatsAppVoiceSessionGuard.isActive(sessionId) ||
            !whatsAppVoiceConsent ||
            whatsAppDraftFlow.state == WhatsAppDraftFlow.State.IDLE ||
            !activityResumed
        ) return

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.RECORD_AUDIO), 102
            )
            return
        }

        val spinner = findViewById<Spinner>(R.id.languageSpinner)
        val index = spinner.selectedItemPosition.coerceIn(0, languages.lastIndex)
        speech.language = languages[index].second
        tts.setLanguage(speech.language)

        speech.onFailure = { failure ->
            if (whatsAppVoiceSessionGuard.isActive(sessionId) &&
                whatsAppVoiceConsent &&
                whatsAppDraftFlow.state != WhatsAppDraftFlow.State.IDLE
            ) {
                when (failure) {
                    SpeechFailure.RETRYABLE -> {
                        if (activityResumed &&
                            whatsAppSpeechRetryPolicy.shouldRetry()
                        ) {
                            speakThenListen(
                                "आवाज़ साफ़ नहीं मिली। " +
                                "कृपया फिर बोलिए।"
                            )
                        } else {
                            status.text =
                                "दोबारा आवाज़ नहीं समझ आई। माइक दबाकर फिर कोशिश करें।"
                            tts.speak(
                                "आवाज़ समझ नहीं आई। " +
                                "फिर कोशिश करने के लिए माइक दबाएँ।"
                            )
                        }
                    }

                    SpeechFailure.PERMISSION -> {
                        invalidateWhatsAppVoiceSession()
                        whatsAppVoiceConsent = false
                        whatsAppResumeListening = false
                        whatsAppDraftFlow.reset()
                        whatsAppSpeechRetryPolicy.reset()
                        pendingWhatsAppNumber = null
                        status.text = "माइक्रोफोन की अनुमति जरूरी है।"
                        tts.speak("माइक्रोफोन की अनुमति जरूरी है।")
                    }

                    SpeechFailure.UNAVAILABLE,
                    SpeechFailure.OTHER -> {
                        status.text =
                            "Voice सेवा उपलब्ध नहीं है। माइक से दोबारा कोशिश करें।"
                        tts.speak(
                            "Voice सेवा अभी उपलब्ध नहीं है। " +
                            "माइक दबाकर दोबारा कोशिश करें।"
                        )
                    }
                }
            }
        }

        status.text = "अब बोलिए।"
        speech.start()
    }

    private fun handleWhatsAppVoiceInput(heard: String) {
        if (!whatsAppVoiceConsent) return

        when (val result = whatsAppDraftFlow.accept(heard)) {
            WhatsAppDraftFlow.Result.AskForMessage -> {
                speakThenListen("अब अपना नया संदेश बोलिए।")
            }

            is WhatsAppDraftFlow.Result.Preview -> {
                showWhatsAppDraftPreview(result.message)
            }

            is WhatsAppDraftFlow.Result.OpenDraft -> {
                openWhatsAppDraft(result.message)
            }

            WhatsAppDraftFlow.Result.Cancelled -> {
                invalidateWhatsAppVoiceSession()
                whatsAppVoiceConsent = false
                whatsAppResumeListening = false
                pendingWhatsAppNumber = null
                status.text = "WhatsApp संदेश रद्द कर दिया गया।"
                tts.speak("संदेश रद्द कर दिया गया।")
            }

            WhatsAppDraftFlow.Result.RepeatPreview -> {
                whatsAppDraftFlow.currentDraft()?.let {
                    showWhatsAppDraftPreview(it)
                }
            }

            WhatsAppDraftFlow.Result.MessageRejected -> {
                speakThenListen("संदेश खाली या बहुत लंबा है। कृपया फिर बोलिए।")
            }

            WhatsAppDraftFlow.Result.Clarify -> {
                speakThenListen("कृपया हाँ, बदलो या रद्द करो बोलिए।")
            }

            WhatsAppDraftFlow.Result.Ignored -> {
                speakThenListen("माफ कीजिए, समझ नहीं आया। कृपया फिर बोलिए।")
            }
        }
    }

    private fun showWhatsAppDraftPreview(message: String) {
        if (!whatsAppVoiceConsent) return
        val sessionId = whatsAppVoiceSessionGuard.currentSessionId()
            ?: return

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("WhatsApp संदेश जाँचें")
            .setMessage(
                "संदेश:\\n\\n$message\\n\\n" +
                "आवाज़ से 'हाँ', 'बदलो' या 'रद्द करो' बोल सकते हैं। " +
                "भेजने के लिए WhatsApp में खुद Send दबाएँ।"
            )
            .setItems(
                arrayOf(
                    "WhatsApp में draft खोलें",
                    "आवाज़ से पुष्टि करें",
                    "संदेश बदलें",
                    "रद्द करें"
                )
            ) { _, which ->
                when (which) {
                    0 -> {
                        val result = whatsAppDraftFlow.accept("हाँ")
                        if (result is WhatsAppDraftFlow.Result.OpenDraft) {
                            openWhatsAppDraft(result.message)
                        }
                    }
                    1 -> showWhatsAppListeningPrompt()
                    2 -> {
                        if (whatsAppDraftFlow.accept("बदलो") ==
                            WhatsAppDraftFlow.Result.AskForMessage
                        ) {
                            showWhatsAppListeningPrompt()
                        }
                    }
                    else -> {
                        whatsAppDraftFlow.reset()
                        whatsAppSpeechRetryPolicy.reset()
                        invalidateWhatsAppVoiceSession()
                        whatsAppVoiceConsent = false
                        whatsAppResumeListening = false
                        pendingWhatsAppNumber = null
                        status.text = "WhatsApp draft रद्द कर दिया गया।"
                        tts.speak("संदेश रद्द कर दिया गया।")
                    }
                }
            }
            .setOnCancelListener {
                whatsAppDraftFlow.reset()
                whatsAppSpeechRetryPolicy.reset()
                invalidateWhatsAppVoiceSession()
                whatsAppVoiceConsent = false
                whatsAppResumeListening = false
                pendingWhatsAppNumber = null
            }
            .create()

        dialog.show()

        tts.speak("आपका संदेश है। $message। " +
            "खोलने के लिए हाँ, बदलने के लिए बदलो, रद्द करने के लिए रद्द करो बोलिए।"
        ) {
            if (whatsAppVoiceSessionGuard.isActive(sessionId)) {
                startWhatsAppListening(sessionId)
            }
        }
    }

    private fun openWhatsAppDraft(message: String) {
        invalidateWhatsAppVoiceSession()
        whatsAppVoiceConsent = false
        whatsAppResumeListening = false
        val number = pendingWhatsAppNumber
        if (number.isNullOrBlank()) {
            whatsAppDraftFlow.reset()
            whatsAppSpeechRetryPolicy.reset()
            status.text = "संपर्क नंबर उपलब्ध नहीं है।"
            tts.speak("संपर्क नंबर उपलब्ध नहीं है।")
            return
        }

        val digits = number.filter { it.isDigit() }
        if (digits.length !in 7..15) {
            whatsAppDraftFlow.reset()
            whatsAppSpeechRetryPolicy.reset()
            pendingWhatsAppNumber = null
            status.text = "WhatsApp नंबर मान्य नहीं है।"
            tts.speak("WhatsApp नंबर मान्य नहीं है।")
            return
        }

        try {
            val uri = Uri.parse("https://wa.me/$digits")
                .buildUpon()
                .appendQueryParameter("text", message)
                .build()
            startActivity(
                Intent(Intent.ACTION_VIEW, uri).setPackage("com.whatsapp")
            )
            status.text = "WhatsApp draft खुल गया। भेजने के लिए खुद Send दबाएँ।"
            tts.speak("WhatsApp खुल गया है। संदेश जाँचकर खुद भेजें।")
        } catch (_: Exception) {
            status.text = "WhatsApp नहीं खुल सका। ऐप इंस्टॉल है या नहीं जाँचें।"
            tts.speak("WhatsApp नहीं खुल सका।")
        } finally {
            pendingWhatsAppNumber = null
            whatsAppDraftFlow.reset()
            whatsAppSpeechRetryPolicy.reset()
        }
    }

    private fun showSmsEditor(name: String, number: String) {
        val editor = EditText(this).apply {
            hint = "अपना संदेश लिखें"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            minLines = 3
            maxLines = 6
            gravity = android.view.Gravity.TOP
            setPadding(32, 24, 32, 24)
        }

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("SMS लिखें")
            .setMessage("प्राप्तकर्ता: $name\\n$number")
            .setView(editor)
            .setNegativeButton("रद्द करें", null)
            .setPositiveButton("Preview देखें", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val message = SmsDraftValidator.normalize(
                        editor.text.toString()
                    )

                    if (message == null) {
                        status.text = "संदेश खाली या अमान्य है। 1000 अक्षरों तक लिखें।"
                        tts.speak("कृपया सही संदेश लिखें।")
                        return@setOnClickListener
                    }

                    dialog.dismiss()
                    showSmsPreview(name, number, message)
                }
        }
        dialog.show()
    }

    private fun showSmsPreview(name: String, number: String, message: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle("SMS भेजने से पहले जाँचें")
            .setMessage("प्राप्तकर्ता: $name\\nनंबर: $number\\n\\nसंदेश:\\n$message\\n\\nअगली स्क्रीन पर SMS ऐप खुलेगी। संदेश भेजने के लिए आपको खुद Send दबाना होगा।")
            .setNegativeButton("रद्द करें", null)
            .setPositiveButton("SMS ऐप खोलें") { _, _ ->
                try {
                    val smsIntent = Intent(
                        Intent.ACTION_SENDTO,
                        Uri.fromParts("smsto", number, null)
                    ).putExtra("sms_body", message)

                    startActivity(smsIntent)
                    status.text = "SMS ऐप खुल गई। भेजने से पहले संदेश जाँचें और खुद Send दबाएँ।"
                    tts.speak("SMS ऐप खुल गई है। संदेश जाँचकर खुद भेजें।")
                } catch (_: Exception) {
                    status.text = "SMS ऐप नहीं खुल सकी।"
                    tts.speak("SMS ऐप नहीं खुल सकी।")
                }
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        activityResumed = true
        if (whatsAppResumeListening && whatsAppVoiceConsent &&
            whatsAppDraftFlow.state != WhatsAppDraftFlow.State.IDLE &&
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            whatsAppResumeListening = false
            showWhatsAppListeningPrompt()
        }
    }

    override fun onPause() {
        activityResumed = false
        if (whatsAppVoiceConsent &&
            whatsAppDraftFlow.state != WhatsAppDraftFlow.State.IDLE
        ) {
            whatsAppResumeListening = true
        }
        if (::speech.isInitialized) speech.stop()
        super.onPause()
    }

    override fun onDestroy() {
        invalidateWhatsAppVoiceSession()
        tts.close()
        super.onDestroy()
    }
}
