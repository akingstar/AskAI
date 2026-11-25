ASK AI — UNIVERSAL CONTROL DISCOVERY AUDIT
Date: 20261125-083356
Commit: 9ab01a646102393f81557bb5ae0d822cd1498224


========== app/src/main/AndroidManifest.xml ==========
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.RECORD_AUDIO"/>
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
    <uses-permission android:name="android.permission.CALL_PHONE"/>
    <uses-permission android:name="android.permission.READ_CONTACTS"/>
    <uses-permission android:name="android.permission.SEND_SMS"/>
    <uses-permission android:name="android.permission.ANSWER_PHONE_CALLS"/>
    <uses-permission android:name="android.permission.READ_PHONE_STATE"/>
    <uses-permission android:name="android.permission.READ_CALL_LOG"/>
    <uses-permission android:name="android.permission.CAMERA"/>
    <uses-permission android:name="android.permission.VIBRATE"/>
    <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS"/>
    <uses-permission android:name="android.permission.WRITE_SETTINGS"/>
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE"/>
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
    <uses-permission android:name="android.permission.ACCESS_NOTIFICATION_POLICY"/>
    <uses-feature android:name="android.hardware.microphone" android:required="false"/>
    <uses-feature android:name="android.hardware.camera" android:required="false"/>
    <queries>
        <intent><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent>
        <intent><action android:name="android.speech.RecognitionService"/></intent>
        <package android:name="com.whatsapp"/>
        <package android:name="com.google.android.youtube"/>
        <package android:name="com.google.android.apps.maps"/>
        <package android:name="com.android.chrome"/>
    </queries>
    <application android:allowBackup="false" android:label="@string/app_name"
        android:icon="@drawable/ic_askai" android:theme="@style/AppTheme"
        android:supportsRtl="true">
        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>
        <activity android:name=".LocalHistoryActivity" android:exported="false"/>
        <activity android:name=".OnboardingActivity" android:exported="false"/>
        <activity android:name=".AskAISettingsActivity" android:exported="false"/>
        <activity android:name=".HelpActivity" android:exported="false"/>
        <service android:name=".ListeningService" android:exported="false" android:foregroundServiceType="microphone"/>
        <service android:name=".AskAIAccessibilityService"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
            android:exported="true">
            <intent-filter><action android:name="android.accessibilityservice.AccessibilityService"/></intent-filter>
            <meta-data android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config"/>
        </service>
        <service android:name=".AskAINotificationListener"
            android:label="Ask AI Notifications"
            android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
            android:exported="true">
            <intent-filter><action android:name="android.service.notification.NotificationListenerService"/></intent-filter>
        </service>
    </application>
</manifest>

========== app/src/main/java/com/akingstar/askai/AskAIAccessibilityService.kt ==========
package com.akingstar.askai

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AskAIAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        activeService = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        if (activeService === this) activeService = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        if (activeService === this) activeService = null
        super.onDestroy()
    }

    private fun performCommand(
        action: String,
        value: String,
        target: String
    ): String? {
        return when (action) {
            "tap_text" ->
                if (target.isNotBlank() && tapText(target)) "टैप कर दिया।" else null

            "long_press_text" ->
                if (target.isNotBlank() && longPressText(target)) "लॉन्ग प्रेस किया।" else null

            "type_text" ->
                if (target.isNotBlank() && setText(value, target)) "टेक्स्ट लिख दिया।" else null

            "scroll_up" ->
                if (scroll("up")) "ऊपर स्क्रॉल किया।" else null

            "scroll_down" ->
                if (scroll("down")) "नीचे स्क्रॉल किया।" else null

            "home" ->
                if (global("home")) "होम स्क्रीन खोल दी।" else null

            "back" ->
                if (global("back")) "पीछे आ गया।" else null

            "recents" ->
                if (global("recents")) "हाल के ऐप खोल दिए।" else null

            "notifications" ->
                if (global("notifications")) "नोटिफिकेशन खोल दिए।" else null

            "quick_settings" ->
                if (global("quick_settings")) "क्विक सेटिंग खोल दी।" else null

            "read_screen" ->
                readScreen().takeIf { it.isNotBlank() }

            else -> null
        }
    }

    companion object {
        @Volatile
        private var activeService: AskAIAccessibilityService? = null

        fun perform(
            action: String,
            value: String = "",
            target: String = ""
        ): String? {
            if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
                return null
            }
            val service = activeService ?: return null
            return runCatching {
                service.performCommand(action, value, target)
            }.getOrNull()
        }
    }

    fun global(action: String): Boolean {
        val code = when (action) {
            "home" -> GLOBAL_ACTION_HOME
            "back" -> GLOBAL_ACTION_BACK
            "recents" -> GLOBAL_ACTION_RECENTS
            "notifications" -> GLOBAL_ACTION_NOTIFICATIONS
            "quick_settings" -> GLOBAL_ACTION_QUICK_SETTINGS
            "power" -> GLOBAL_ACTION_POWER_DIALOG
            "lock" -> GLOBAL_ACTION_LOCK_SCREEN
            "screenshot" -> if (Build.VERSION.SDK_INT >= 28) GLOBAL_ACTION_TAKE_SCREENSHOT else return false
            else -> return false
        }
        return performGlobalAction(code)
    }

    fun tapText(text: String): Boolean {
        if (text.isBlank() || text.length > 100) return false
        val root = rootInActiveWindow ?: return false
        val node = findText(root, text) ?: return false
        val clickable = clickableParent(node) ?: return false
        return clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    fun longPressText(text: String): Boolean {
        if (text.isBlank() || text.length > 100) return false
        val root = rootInActiveWindow ?: return false
        val node = findText(root, text) ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
    }

    fun setText(text: String, fieldHint: String): Boolean {
        if (text.isBlank() || text.length > 500 ||
            fieldHint.isBlank() || fieldHint.length > 100
        ) return false

        val root = rootInActiveWindow ?: return false
        val matches = findEditables(root).filter { node ->
            listOfNotNull(
                node.text?.toString(),
                node.hintText?.toString(),
                node.contentDescription?.toString()
            ).any { it.contains(fieldHint, ignoreCase = true) }
        }

        // Never guess when the field hint matches zero or multiple fields.
        val node = matches.singleOrNull() ?: return false

        return node.performAction(
            AccessibilityNodeInfo.ACTION_SET_TEXT,
            android.os.Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    text
                )
            }
        )
    }

    fun scroll(direction: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = findScrollable(root) ?: return false
        val action = when (direction) {
            "up" -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            "down" -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            else -> return false
        }
        return node.performAction(action)
    }

    fun readScreen(): String {
        val root = rootInActiveWindow ?: return ""
        val out = StringBuilder()
        collect(root, out)
        return SensitiveDataFilter.mask(out.toString()).take(4000)
    }

    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, duration: Long = 350): Boolean {
        if (Build.VERSION.SDK_INT < 24) return false
        val path = Path().apply { moveTo(x1, y1); lineTo(x2, y2) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    fun tap(x: Float, y: Float): Boolean = swipe(x, y, x, y, 80)

    private fun findText(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val label = listOfNotNull(node.text?.toString(), node.contentDescription?.toString()).joinToString(" ")
        if (label.contains(text, true)) return node
        for (i in 0 until node.childCount) {
            val found = node.getChild(i)?.let { findText(it, text) }
            if (found != null) return found
        }
        return null
    }

    private fun findEditables(
        node: AccessibilityNodeInfo
    ): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()

        fun collectEditable(current: AccessibilityNodeInfo) {
            if (current.isEditable) result.add(current)
            for (i in 0 until current.childCount) {
                current.getChild(i)?.let(::collectEditable)
            }
        }

        collectEditable(node)
        return result
    }

    private fun findScrollable(
        node: AccessibilityNodeInfo
    ): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val found = node.getChild(i)?.let(::findScrollable)
            if (found != null) return found
        }
        return null
    }

    private fun clickableParent(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) return current
            current = current.parent
        }
        return null
    }

    private fun collect(node: AccessibilityNodeInfo, out: StringBuilder) {
        val text = node.text?.toString() ?: node.contentDescription?.toString()
        if (!text.isNullOrBlank()) out.append(text).append(". ")
        for (i in 0 until node.childCount) node.getChild(i)?.let { collect(it, out) }
    }
}

========== app/src/main/java/com/akingstar/askai/service/AskAccessibilityService.kt ==========
package com.akingstar.askai.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AskAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* Screen changes observed only while service is enabled. */ }
    override fun onInterrupt() {}
    fun visibleText(): String {
        val root: AccessibilityNodeInfo = rootInActiveWindow ?: return "इस स्क्रीन का पाठ उपलब्ध नहीं है।"
        val out = StringBuilder()
        fun walk(node: AccessibilityNodeInfo, depth: Int) {
            if (depth > 12 || out.length > 5000) return
            node.text?.toString()?.takeIf { it.isNotBlank() }?.let { out.append(it).append('\n') }
            node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { out.append(it).append('\n') }
            for (i in 0 until node.childCount) node.getChild(i)?.let { child -> walk(child, depth + 1); child.recycle() }
        }
        walk(root, 0); root.recycle()
        return out.toString().ifBlank { "इस स्क्रीन पर पढ़ने योग्य पाठ नहीं मिला।" }
    }
}

========== app/src/main/java/com/akingstar/askai/ActionPlan.kt ==========
package com.akingstar.askai

import org.json.JSONArray
import org.json.JSONObject

import com.akingstar.askai.automation.RoutineCondition
import com.akingstar.askai.automation.RoutineConditionType
import com.akingstar.askai.automation.RoutineConditionOperator

data class ActionStep(
    val action: String,
    val target: String = "",
    val value: String = "",
    val condition: RoutineCondition? = null
)
data class ActionPlan(val language: String, val steps: List<ActionStep>)

object PlanValidator {
    private val allowed = setOf(
        "tap_text", "long_press_text", "type_text",
        "scroll_up", "scroll_down", "home", "back",
        "recents", "notifications", "quick_settings",
        "read_screen", "accessibility_settings",
        "open_app", "alarm", "timer", "camera", "selfie", "gallery",
        "torch", "torch_off", "volume_up", "volume_down", "brightness",
        "dnd", "ringer", "rotation", "youtube", "maps", "web", "music",
        "phonepe", "paytm", "gpay", "irctc", "jio", "airtel", "hotstar",
        "wifi_settings", "bluetooth_settings", "mobile_settings",
        "hotspot_settings", "airplane_settings", "location_settings",
        "battery", "time", "date", "storage", "network", "location"
    )

    private fun safeString(value: Any?): String? {
        val text = value as? String ?: return null
        if (text.any { it.isISOControl() }) return null
        return text
    }

    fun parse(json: String): ActionPlan? {
        return try {
            if (json.length > 8192) return null

            val root = JSONObject(json)
            if (root.keys().asSequence().toSet() != setOf("language", "steps")) {
                return null
            }

            val language = safeString(root.opt("language")) ?: return null
            if (language.isBlank() || language.length > 20) return null

            val array = root.opt("steps") as? JSONArray ?: return null
            if (array.length() !in 1..5) return null

            val steps = ArrayList<ActionStep>()

            for (i in 0 until array.length()) {
                val item = array.opt(i) as? JSONObject ?: return null

                val keys = item.keys().asSequence().toSet()
                if (keys != setOf("action", "target", "value") &&
                    keys != setOf("action", "target", "value", "condition")
                ) return null

                val action = safeString(item.opt("action")) ?: return null
                val target = safeString(item.opt("target")) ?: return null
                val value = safeString(item.opt("value")) ?: return null

                if (action !in allowed ||
                    target.length > 160 ||
                    value.length > 500 ||
                    (action == "open_app" && target.isBlank() && value.isBlank())
                ) return null

                if (action in setOf("tap_text", "long_press_text") &&
                    (target.isBlank() || value.isNotBlank())
                ) return null

                if (action == "type_text" &&
                    (target.isBlank() || value.isBlank())
                ) return null

                if (action in setOf(
                    "scroll_up", "scroll_down", "home", "back",
                    "recents", "notifications", "quick_settings",
                    "read_screen", "accessibility_settings"
                ) && (target.isNotBlank() || value.isNotBlank())
                ) return null

                if (SafetyGate.isNeverAllowedAction(action)) return null

                val condition = if (keys.contains("condition")) {
                    parseCondition(item.opt("condition")) ?: return null
                } else null

                if (i == 0 &&
                    condition?.type == RoutineConditionType.PREVIOUS_STEP
                ) return null

                steps.add(ActionStep(action, target, value, condition))
            }

            ActionPlan(language, steps)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseCondition(raw: Any?): RoutineCondition? {
        val obj = raw as? JSONObject ?: return null
        if (obj.keys().asSequence().toSet() !=
            setOf("type", "operator", "value", "upperValue")
        ) return null

        val typeName = safeString(obj.opt("type")) ?: return null
        val operatorName = safeString(obj.opt("operator")) ?: return null
        val type = runCatching { RoutineConditionType.valueOf(typeName) }.getOrNull()
            ?: return null
        val operator = runCatching {
            RoutineConditionOperator.valueOf(operatorName)
        }.getOrNull() ?: return null

        fun integer(key: String): Int? {
            val number = obj.opt(key) as? Number ?: return null
            val value = number.toDouble()
            if (!value.isFinite() || value % 1.0 != 0.0 ||
                value < Int.MIN_VALUE || value > Int.MAX_VALUE
            ) return null
            return value.toInt()
        }

        val value = integer("value") ?: return null
        val upperValue = integer("upperValue") ?: return null

        val valid = when (type) {
            RoutineConditionType.BATTERY_PERCENT ->
                value in 0..100 && upperValue == 0 &&
                    operator in setOf(
                        RoutineConditionOperator.LESS_THAN,
                        RoutineConditionOperator.AT_LEAST,
                        RoutineConditionOperator.EQUALS
                    )

            RoutineConditionType.TIME_OF_DAY ->
                value in 0..1439 && upperValue in 0..1439 &&
                    operator == RoutineConditionOperator.BETWEEN

            RoutineConditionType.PREVIOUS_STEP ->
                value == 0 && upperValue == 0 &&
                    operator in setOf(
                        RoutineConditionOperator.SUCCEEDED,
                        RoutineConditionOperator.FAILED
                    )
        }

        if (!valid) return null
        return RoutineCondition(type, operator, value, upperValue)
    }

    fun encode(plan: ActionPlan): String {
        val arr = JSONArray()
        plan.steps.forEach {
            arr.put(
                JSONObject()
                    .put("action", it.action)
                    .put("target", it.target)
                    .put("value", it.value)
                    .apply {
                        it.condition?.let { condition ->
                            put(
                                "condition",
                                JSONObject()
                                    .put("type", condition.type.name)
                                    .put("operator", condition.operator.name)
                                    .put("value", condition.value)
                                    .put("upperValue", condition.upperValue)
                            )
                        }
                    }
            )
        }
        return JSONObject()
            .put("language", plan.language)
            .put("steps", arr)
            .toString()
    }
}

========== app/src/main/java/com/akingstar/askai/ActionExecutor.kt ==========
package com.akingstar.askai

import android.app.AlertDialog
import com.akingstar.askai.automation.RoutineConditionEvaluator
import com.akingstar.askai.automation.RoutineConditionSnapshot

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import android.media.AudioManager
import android.hardware.camera2.CameraManager
import android.widget.Toast

data class StepExecutionResult(
    val index: Int,
    val action: String,
    val target: String,
    val status: String,
    val reason: String = ""
)

data class ExecutionSummary(
    val totalSteps: Int,
    val succeededSteps: Int,
    val failedSteps: Int,
    val cancelledSteps: Int,
    val stepResults: List<StepExecutionResult> = emptyList(),
    val skippedSteps: Int = 0
)

class ActionExecutor(private val context: Context, private val speak: (String) -> Unit) {
    private val packages = mapOf(
        "youtube" to "com.google.android.youtube",
        "maps" to "com.google.android.apps.maps",
        "chrome" to "com.android.chrome",
        "phonepe" to "com.phonepe.app",
        "paytm" to "net.one97.paytm",
        "gpay" to "com.google.android.apps.nbu.paisa.user",
        "irctc" to "cris.org.in.prs.ima",
        "jio" to "com.jio.myjio",
        "airtel" to "com.myairtelapp",
        "hotstar" to "in.startv.hotstar",
        "whatsapp" to "com.whatsapp"
    )

    fun execute(plan: ActionPlan) {
        execute(plan) {}
    }

    fun execute(plan: ActionPlan, onComplete: (ExecutionSummary) -> Unit) {
        executeSelected(plan, plan.steps.indices.toList(), onComplete = onComplete)
    }

    fun executeSelected(
        plan: ActionPlan,
        requestedIndices: List<Int>,
        onStepResult: (StepExecutionResult) -> Boolean = { true },
        onComplete: (ExecutionSummary) -> Unit
    ) {
        val selectedIndices = requestedIndices
            .filter { it in plan.steps.indices }
            .distinct()
            .sorted()
        executeAt(
            plan.steps,
            selectedIndices,
            0,
            0,
            0,
            0,
            0,
            mutableListOf(),
            onStepResult,
            onComplete
        )
    }

    private fun executeAt(
        steps: List<ActionStep>,
        selectedIndices: List<Int>,
        position: Int,
        succeeded: Int,
        failed: Int,
        cancelled: Int,
        skipped: Int,
        stepResults: MutableList<StepExecutionResult>,
        onStepResult: (StepExecutionResult) -> Boolean,
        onComplete: (ExecutionSummary) -> Unit
    ) {
        if (position >= selectedIndices.size) {
            onComplete(
                ExecutionSummary(
                    totalSteps = selectedIndices.size,
                    succeededSteps = succeeded,
                    failedSteps = failed,
                    cancelledSteps = cancelled,
                    stepResults = stepResults.toList(),
                    skippedSteps = skipped
                )
            )
            return
        }

        val originalIndex = selectedIndices[position]
        val step = steps[originalIndex]
        val next = { ok: Boolean, resultStatus: String, reason: String,
                     nextSucceeded: Int, nextFailed: Int,
                     nextCancelled: Int, nextSkipped: Int ->
            val stepResult = StepExecutionResult(
                index = originalIndex,
                action = step.action,
                target = step.target,
                status = resultStatus,
                reason = reason
            )
            if (!onStepResult(stepResult)) {
                stepResults.add(
                    stepResult.copy(
                        status = "FAILED",
                        reason = "Execution result could not be saved; stopped safely"
                    )
                )
                onComplete(
                    ExecutionSummary(
                        totalSteps = selectedIndices.size,
                        succeededSteps = nextSucceeded,
                        failedSteps = nextFailed + 1,
                        cancelledSteps = nextCancelled,
                        stepResults = stepResults.toList(),
                        skippedSteps = nextSkipped
                    )
                )
            } else {
                stepResults.add(stepResult)
                executeAt(
                    steps,
                    selectedIndices,
                    position + 1,
                    nextSucceeded,
                    nextFailed,
                    nextCancelled,
                    nextSkipped,
                    stepResults,
                    onStepResult,
                    onComplete
                )
            }
        }

        step.condition?.let { condition ->
            val previousStatus = stepResults.lastOrNull()
                ?.takeIf { it.index == originalIndex - 1 }
                ?.status

            val batteryIntent = context.registerReceiver(
                null,
                android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val level = batteryIntent?.getIntExtra(
                android.os.BatteryManager.EXTRA_LEVEL, -1
            ) ?: -1
            val scale = batteryIntent?.getIntExtra(
                android.os.BatteryManager.EXTRA_SCALE, -1
            ) ?: -1
            val batteryPercent = if (scale > 0 && level in 0..scale) {
                (level * 100) / scale
            } else null

            val calendar = java.util.Calendar.getInstance()
            val minuteOfDay =
                calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
                calendar.get(java.util.Calendar.MINUTE)

            val evaluated = RoutineConditionEvaluator.evaluate(
                condition,
                RoutineConditionSnapshot(
                    batteryPercent = batteryPercent,
                    minuteOfDay = minuteOfDay,
                    previousStepStatus = previousStatus
                )
            )

            when (evaluated) {
                false -> {
                    next(
                        false, "SKIPPED", "Condition evaluated false",
                        succeeded, failed, cancelled, skipped + 1
                    )
                    return
                }
                null -> {
                    next(
                        false,
                        "FAILED",
                        "Condition could not be evaluated safely",
                        succeeded,
                        failed + 1,
                        cancelled,
                        skipped
                    )
                    return
                }
                true -> Unit
            }
        }

        val sensitiveText = "${step.action} ${step.target} ${step.value}"
        val appWarning = if (step.action == "open_app") {
            SafetyGate.warningForApp(step.target)
        } else null

        val riskyUiTap =
            step.action == "tap_text" &&
                SafetyGate.isRiskyTapTarget(step.target)

        val sensitiveField =
            step.action == "type_text" &&
                SafetyGate.isSensitiveInputTarget(step.target)

        if (SafetyGate.blocked(sensitiveText) ||
            SafetyGate.isNeverAllowedAction(step.action) ||
            riskyUiTap ||
            sensitiveField
        ) {
            speak("सुरक्षा के लिए यह काम रोक दिया गया है।")
            next(false, "BLOCKED", "Safety policy blocked this action",
                succeeded, failed + 1, cancelled, skipped)
            return
        }

        if (SafetyGate.needsConfirmation(step.action, step.target) || appWarning != null) {
            AlertDialog.Builder(context)
                .setTitle("Ask AI — पुष्टि जरूरी")
                .setMessage(appWarning ?: "क्या आप यह काम करना चाहते हैं?\n${step.action}: ${step.target}")
                .setPositiveButton("हाँ, आगे बढ़ें") { _, _ ->
                    val ok = runStep(step)
                    next(
                        ok,
                        if (ok) "SUCCESS" else "FAILED",
                        if (ok) "" else "The action could not be completed",
                        succeeded + if (ok) 1 else 0,
                        failed + if (ok) 0 else 1,
                        cancelled,
                        skipped
                    )
                }
                .setNegativeButton("नहीं") { _, _ ->
                    speak("ठीक है जी, यह काम रद्द कर दिया।")
                    next(false, "CANCELLED", "User declined confirmation",
                        succeeded, failed, cancelled + 1, skipped)
                }
                .setOnCancelListener {
                    speak("काम रद्द कर दिया।")
                    next(false, "CANCELLED", "Confirmation dialog dismissed",
                        succeeded, failed, cancelled + 1, skipped)
                }
                .show()
        } else {
            val ok = runStep(step)
            next(
                ok,
                if (ok) "SUCCESS" else "FAILED",
                if (ok) "" else "The action could not be completed",
                succeeded + if (ok) 1 else 0,
                failed + if (ok) 0 else 1,
                cancelled,
                skipped
            )
        }
    }

    private fun runStep(step: ActionStep): Boolean {
        return try {
            executeStep(step)
        } catch (_: Exception) {
            speak("यह काम इस फोन पर पूरा नहीं हो सका।")
            false
        }
    }

    private fun executeStep(step: ActionStep): Boolean {
        val value = step.value.ifBlank { step.target }
        return when (step.action) {
            "tap_text" ->
                accessibilityAction("tap_text", target = step.target)

            "long_press_text" ->
                accessibilityAction("long_press_text", target = step.target)

            "type_text" ->
                accessibilityAction(
                    "type_text",
                    value = step.value,
                    target = step.target
                )

            "scroll_up" -> accessibilityAction("scroll_up")
            "scroll_down" -> accessibilityAction("scroll_down")
            "home" -> accessibilityAction("home")
            "back" -> accessibilityAction("back")
            "recents" -> accessibilityAction("recents")
            "notifications" -> accessibilityAction("notifications")
            "quick_settings" -> accessibilityAction("quick_settings")

            "read_screen" -> {
                val screen = AskAIAccessibilityService.perform("read_screen")
                if (screen.isNullOrBlank()) {
                    speak("स्क्रीन पढ़ने के लिए Accessibility अनुमति चालू करें।")
                    false
                } else {
                    speak(screen.take(700))
                    true
                }
            }

            "accessibility_settings" ->
                openIntent(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

            "open_app", "phonepe", "paytm", "gpay", "irctc", "jio", "airtel", "hotstar" -> {
                val key = (step.target.ifBlank { step.action }).lowercase()
                if (step.action in setOf("phonepe", "paytm", "gpay", "irctc", "jio", "airtel", "hotstar")) {
                    openPackage(packages[step.action] ?: "")
                } else {
                    val match = packages.entries.firstOrNull { key.contains(it.key) || it.key.contains(key) }
                    if (match != null) {
                        openPackage(match.value)
                    } else {
                        val launch = context.packageManager.getInstalledApplications(0)
                            .firstOrNull { it.loadLabel(context.packageManager).toString().contains(key, true) }
                            ?.let { context.packageManager.getLaunchIntentForPackage(it.packageName) }

                        if (launch == null) {
                            speak("यह ऐप नहीं मिला।")
                            false
                        } else {
                            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(launch)
                            true
                        }
                    }
                }
            }
            "torch" -> torch(true)
            "torch_off" -> torch(false)
            "volume_up" -> {
                audio().adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                true
            }
            "volume_down" -> {
                audio().adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                true
            }
            "camera", "selfie" -> openIntent(Intent(MediaStoreIntent).apply {
                putExtra("android.intent.extras.CAMERA_FACING", if (step.action == "selfie") 1 else 0)
            })
            "gallery" -> openIntent(Intent(Intent.ACTION_VIEW, Uri.parse("content://media/internal/images/media")))
            "wifi_settings" -> openIntent(Intent(Settings.Panel.ACTION_WIFI))
            "bluetooth_settings" -> openIntent(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            "mobile_settings" -> openIntent(Intent(Settings.ACTION_DATA_ROAMING_SETTINGS))
            "hotspot_settings" -> openIntent(Intent(Settings.ACTION_WIRELESS_SETTINGS))
            "airplane_settings" -> openIntent(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS))
            "location_settings" -> openIntent(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            "dnd" -> openIntent(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            "ringer" -> openIntent(Intent(Settings.ACTION_SOUND_SETTINGS))
            "rotation" -> openIntent(Intent(Settings.ACTION_DISPLAY_SETTINGS))
            "brightness" -> openIntent(Intent(Settings.ACTION_DISPLAY_SETTINGS))
            "alarm" -> openIntent(Intent(AlarmClock.ACTION_SET_ALARM))
            "timer" -> openIntent(Intent(AlarmClock.ACTION_SET_TIMER))
            "youtube" -> {
                val openOnly = value.equals("youtube", true) ||
                    value.lowercase() in setOf("यूट्यूब", "यूट्यूब खोलो", "youtube kholo", "open youtube")
                if (openOnly) openPackage(packages["youtube"]!!)
                else openIntent(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(value)}")))
            }
            "web" -> openIntent(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(value)}")))
            "maps" -> {
                val openOnly = value.equals("maps", true) ||
                    value.lowercase() in setOf("मैप खोलो", "गूगल मैप खोलो", "maps kholo", "open maps", "google maps kholo")
                if (openOnly) openPackage(packages["maps"]!!)
                else openIntent(Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${Uri.encode(value)}")))
            }
            "music" -> openIntent(Intent(Intent.ACTION_VIEW, Uri.parse("https://music.youtube.com/")))
            "time", "date" -> {
                speak(java.text.DateFormat.getDateTimeInstance().format(java.util.Date()))
                true
            }
            "battery" -> {
                val b = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val level = b?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
                if (level !in 0..100) {
                    speak("बैटरी की जानकारी अभी नहीं मिल सकी।")
                    false
                } else {
                    speak("बैटरी $level प्रतिशत है।")
                    true
                }
            }
            "storage" -> {
                val stat = android.os.StatFs(android.os.Environment.getDataDirectory().path)
                speak("खाली जगह ${stat.availableBytes / (1024 * 1024)} MB है।")
                true
            }
            "network" -> openIntent(Intent(Settings.ACTION_WIRELESS_SETTINGS))
            "location" -> openIntent(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            else -> {
                speak("यह कमांड अभी उपलब्ध नहीं है।")
                false
            }
        }
    }

    private fun accessibilityAction(
        action: String,
        value: String = "",
        target: String = ""
    ): Boolean {
        val result = AskAIAccessibilityService.perform(action, value, target)

        if (result == null) {
            speak(
                "यह काम नहीं हो सका। Ask AI की Accessibility अनुमति चालू करें " +
                    "और सही स्क्रीन खुली रखें।"
            )
            return false
        }

        if (result.isNotBlank()) speak(result)
        return true
    }

    private fun openPackage(pkg: String): Boolean {
        if (pkg.isBlank()) {
            speak("ऐप की पहचान नहीं हो सकी।")
            return false
        }
        val i = context.packageManager.getLaunchIntentForPackage(pkg)
        if (i == null) {
            speak("ऐप इंस्टॉल नहीं है।")
            return false
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(i)
        return true
    }

    private fun openIntent(i: Intent): Boolean {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(i)
        return true
    }

    private fun audio() = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private fun torch(on: Boolean): Boolean {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = manager.cameraIdList.firstOrNull {
            manager.getCameraCharacteristics(it)
                .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
        if (id == null) {
            speak("इस फोन में फ्लैश उपलब्ध नहीं है।")
            return false
        }
        manager.setTorchMode(id, on)
        return true
    }

    companion object {
        const val MediaStoreIntent = "android.media.action.IMAGE_CAPTURE"
    }
}

========== app/src/main/java/com/akingstar/askai/engine/ActionExecutor.kt ==========
package com.akingstar.askai.engine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore

class ActionExecutor(private val context: Context) {
    fun execute(step: PlanStep): String {
        val intent = when (step.action) {
            "speak" -> return step.value
            "stop" -> return "ठीक है जी, रोक दिया।"
            "call" -> return "किस व्यक्ति को कॉल करना है? Family Setup में संपर्क जोड़ें; कॉल से पहले पुष्टि जरूरी है।"
            "whatsapp" -> context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                ?: return "WhatsApp इस फोन में नहीं मिला। पहले WhatsApp install करें।"
            "camera" -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            "alarm" -> Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            "youtube" -> Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(step.value.ifBlank { "भजन" })))
            "maps" -> Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(step.value)))
            "settings" -> Intent(android.provider.Settings.ACTION_SETTINGS)
            "sos" -> return "SOS के लिए Family Setup में आपातकालीन संपर्क और SMS/Location permissions सेट करना आवश्यक है। अभी स्वतः SMS नहीं भेजा गया।"
            else -> return "यह काम अभी उपलब्ध नहीं है।"
        }
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            when(step.action) { "camera" -> "कैमरा खोल रहा हूँ जी।"; "alarm" -> "अलार्म सेट करने का पेज खोल रहा हूँ।"; "youtube" -> "YouTube खोल रहा हूँ जी।"; "maps" -> "मैप खोल रहा हूँ जी।"; else -> "ऐप खोल रहा हूँ जी।" }
        } catch (_: Exception) { "माफ कीजिए जी, Android ने यह काम करने नहीं दिया।" }
    }
}

========== app/src/main/java/com/akingstar/askai/AIClient.kt ==========
package com.akingstar.askai

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object AIClient {
    const val SYSTEM_PROMPT = """You are Ask AI, a safe Hindi-first Android phone assistant. Understand Hindi, Hinglish and English and reply in the user's language. Return JSON only. Root schema: {"language":"hi-IN","steps":[{"action":"open_app","target":"YouTube","value":""}]}. Root keys must be exactly language and steps. Use 1 to 5 steps. Every step must have action,target,value strings and may optionally have condition. A condition gates only its own step: if false, that step is skipped; if its inputs are unavailable or invalid, execution fails safely. Condition schema must be exactly {"type":"BATTERY_PERCENT|TIME_OF_DAY|PREVIOUS_STEP","operator":"...","value":0,"upperValue":0}. BATTERY_PERCENT supports LESS_THAN, AT_LEAST, EQUALS with value 0..100 and upperValue 0. TIME_OF_DAY supports BETWEEN with value and upperValue as minutes after midnight, each 0..1439; overnight ranges are allowed. PREVIOUS_STEP supports SUCCEEDED or FAILED, with value and upperValue both 0; it refers only to the immediately preceding step and must never be used on the first step. Do not invent conditions or add condition when the user did not request one. Never invent phone numbers. Never execute calls, SMS, contact access, payments, money transfers, OTP, PIN, password, factory reset, deletion or uninstall actions. Accessibility actions: tap_text and long_press_text use target as the exact visible label and require empty value; type_text uses target as the exact field hint and value as the exact text; scroll_up, scroll_down, home, back, recents, notifications, quick_settings and read_screen require empty target and value. accessibility_settings opens Android Accessibility settings. Never guess labels or field hints. Never type passwords, OTPs, PINs, CVV or payment credentials. Never tap Send, Submit, Delete, Remove, Pay, Transfer, Purchase or similar risky controls. Use only supported safe actions. If uncertain, ask for clarification rather than guessing. No markdown and no prose outside JSON."""

    fun isSecureEndpoint(value: String): Boolean {
        if (value.isBlank() || value.length > 2048) return false
        return try {
            val url = URL(value.trim())
            url.protocol.equals("https", true) &&
                url.host.isNotBlank() &&
                url.userInfo == null
        } catch (_: Exception) {
            false
        }
    }

    private fun readBounded(input: java.io.InputStream, maxBytes: Int): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(4096)

        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (output.size() + count > maxBytes) {
                throw IOException("Response too large")
            }
            output.write(buffer, 0, count)
        }

        return output.toString("UTF-8")
    }

    internal fun request(
        context: Context,
        text: String,
        conversation: List<ConversationTurn> = emptyList(),
        callback: (AIRequestResult) -> Unit
    ) {
        val prefs = context.getSharedPreferences("askai", Context.MODE_PRIVATE)
        val endpoint = prefs.getString("worker_url", "").orEmpty().trim()

        if (AIClientFallbackPolicy.shouldUseOfflineParser(endpoint)) {
            val plan = OfflineParser.parse(text)
            callback(
                if (plan != null) AIRequestResult.Success(plan, offline = true)
                else AIRequestResult.UnrecognizedCommand
            )
            return
        }

        if (!isSecureEndpoint(endpoint)) {
            callback(AIRequestResult.InvalidEndpoint)
            return
        }

        Thread {
            var result: AIRequestResult? = null
            var lastFailure: AIRequestResult = AIRequestResult.NetworkError

            repeat(2) {
                if (result == null) {
                    var connection: HttpURLConnection? = null
                    try {
                        connection = URL(endpoint).openConnection() as HttpURLConnection
                        connection.requestMethod = "POST"
                        connection.connectTimeout = 9000
                        connection.readTimeout = 12000
                        connection.doOutput = true
                        connection.doInput = true
                        connection.useCaches = false
                        connection.instanceFollowRedirects = false
                        connection.setRequestProperty("Content-Type", "application/json")
                        connection.setRequestProperty("Accept", "application/json")

                        val contextArray = org.json.JSONArray()
                        conversation.takeLast(3).forEach { turn ->
                            contextArray.put(
                                JSONObject()
                                    .put("command", turn.command)
                                    .put("outcome", turn.outcome)
                            )
                        }

                        val body = JSONObject()
                            .put("text", text)
                            .put("system", SYSTEM_PROMPT)
                            .put("context", contextArray)
                            .toString()

                        connection.outputStream.use {
                            it.write(body.toByteArray(Charsets.UTF_8))
                        }

                        val code = connection.responseCode
                        if (code in 200..299) {
                            val raw = connection.inputStream.use {
                                readBounded(it, 64 * 1024)
                            }

                            val plan = try {
                                val response = JSONObject(raw)
                                val planJson = response.opt("result") as? String
                                if (planJson.isNullOrBlank()) null
                                else PlanValidator.parse(planJson)
                            } catch (_: Exception) {
                                null
                            }

                            result = if (plan != null) {
                                AIRequestResult.Success(plan)
                            } else {
                                lastFailure = AIRequestResult.InvalidResponse
                                null
                            }
                        } else {
                            lastFailure = AIRequestResult.ServerError(code)
                            if (code in 400..499 && code != 429) {
                                result = lastFailure
                            }
                        }
                    } catch (_: Exception) {
                        lastFailure = AIRequestResult.NetworkError
                    } finally {
                        connection?.disconnect()
                    }
                }
            }

            val finalResult = result ?: lastFailure
            Handler(Looper.getMainLooper()).post {
                callback(finalResult)
            }
        }.start()
    }
}

========== app/src/main/java/com/akingstar/askai/MainActivity.kt ==========
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
        status = findViewById(R.id.status)
        permissionList = findViewById(R.id.permissionList)
        micButton = findViewById(R.id.micButton)
        val spinner = findViewById<Spinner>(R.id.languageSpinner)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages.map { it.first })
        tts = TextToSpeechController(this) {}
        executor = ActionExecutor(this) { tts.speak(it) }
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
                                    status.text = "AI सेवा में समस्या है। HTTP ${result.statusCode}"
                                    tts.speak("AI सेवा अभी उपलब्ध नहीं है।")
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

========== app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt ==========
package com.akingstar.askai

import org.junit.Assert.*
import org.junit.Test

class PlanValidatorTest {
    @Test fun acceptsAccessibilityNavigationActions() {
        val actions = listOf(
            "scroll_up", "scroll_down", "home", "back",
            "recents", "notifications", "quick_settings",
            "read_screen", "accessibility_settings"
        )
        actions.forEach { action ->
            val json = """{"language":"hi-IN","steps":[{"action":"$action","target":"","value":""}]}"""
            assertNotNull("$action should be accepted", PlanValidator.parse(json))
        }
    }

    @Test fun acceptsTextActionsWithRequiredArguments() {
        val tap = """{"language":"hi-IN","steps":[{"action":"tap_text","target":"Settings","value":""}]}"""
        val press = """{"language":"hi-IN","steps":[{"action":"long_press_text","target":"Photo","value":""}]}"""
        val type = """{"language":"hi-IN","steps":[{"action":"type_text","target":"Search","value":"weather"}]}"""
        assertNotNull(PlanValidator.parse(tap))
        assertNotNull(PlanValidator.parse(press))
        assertNotNull(PlanValidator.parse(type))
    }

    @Test fun rejectsAmbiguousOrMalformedTextActions() {
        val noTarget = """{"language":"hi-IN","steps":[{"action":"tap_text","target":"","value":""}]}"""
        val typeNoHint = """{"language":"hi-IN","steps":[{"action":"type_text","target":"","value":"hello"}]}"""
        val typeNoValue = """{"language":"hi-IN","steps":[{"action":"type_text","target":"Search","value":""}]}"""
        val navWithTarget = """{"language":"hi-IN","steps":[{"action":"home","target":"YouTube","value":""}]}"""
        assertNull(PlanValidator.parse(noTarget))
        assertNull(PlanValidator.parse(typeNoHint))
        assertNull(PlanValidator.parse(typeNoValue))
        assertNull(PlanValidator.parse(navWithTarget))
    }

    @Test fun acceptsExactSchema() {
        val p = """{"language":"hi-IN","steps":[{"action":"torch","target":"","value":""}]}"""
        assertNotNull(PlanValidator.parse(p))
    }
    @Test fun rejectsUnknownAction() {
        val p = """{"language":"hi-IN","steps":[{"action":"hack","target":"","value":""}]}"""
        assertNull(PlanValidator.parse(p))
    }


    @Test fun rejectsExtraRootField() {
        val p = """{"language":"hi-IN","steps":[{"action":"torch","target":"","value":""}],"extra":true}"""
        assertNull(PlanValidator.parse(p))
    }

    @Test fun rejectsMissingRootField() {
        val p = """{"language":"hi-IN","steps":[]}"""
        assertNull(PlanValidator.parse(p))
    }

    @Test fun rejectsEmptySteps() {
        val p = """{"language":"hi-IN","steps":[]}"""
        assertNull(PlanValidator.parse(p))
    }

    @Test fun rejectsMoreThanFiveSteps() {
        val steps = List(6) { """{"action":"torch","target":"","value":""}""" }
        val p = """{"language":"hi-IN","steps":[${steps.joinToString(",")}]}"""
        assertNull(PlanValidator.parse(p))
    }

    @Test fun rejectsExtraStepField() {
        val p = """{"language":"hi-IN","steps":[{"action":"torch","target":"","value":"","extra":1}]}"""
        assertNull(PlanValidator.parse(p))
    }

    @Test fun rejectsWrongFieldTypes() {
        val p = """{"language":"hi-IN","steps":[{"action":"torch","target":12,"value":""}]}"""
        assertNull(PlanValidator.parse(p))
    }

    @Test fun rejectsControlCharacters() {
        val p = """{"language":"hi-IN","steps":[{"action":"torch","target":"\u0001","value":""}]}"""
        assertNull(PlanValidator.parse(p))
    }

    @Test fun rejectsOversizedTarget() {
        val target = "a".repeat(161)
        val p = """{"language":"hi-IN","steps":[{"action":"torch","target":"$target","value":""}]}"""
        assertNull(PlanValidator.parse(p))
    }

    @Test fun rejectsOversizedValue() {
        val value = "a".repeat(501)
        val p = """{"language":"hi-IN","steps":[{"action":"torch","target":"","value":"$value"}]}"""
        assertNull(PlanValidator.parse(p))
    }

    @Test fun rejectsOversizedJson() {
        assertNull(PlanValidator.parse(" ".repeat(8193)))
    }

    @Test fun rejectsInvalidLanguage() {
        val blank = """{"language":" ","steps":[{"action":"torch","target":"","value":""}]}"""
        val long = """{"language":"${"a".repeat(21)}","steps":[{"action":"torch","target":"","value":""}]}"""
        assertNull(PlanValidator.parse(blank))
        assertNull(PlanValidator.parse(long))
    }

    @Test fun rejectsMalformedJson() {
        assertNull(PlanValidator.parse("""{"language":"hi-IN","steps":["""))
    }


    @Test fun acceptsLegacyRoutineWithoutCondition() {
        val json = """{"language":"hi-IN","steps":[{"action":"torch","target":"","value":""}]}"""
        val plan = PlanValidator.parse(json)
        assertNotNull(plan)
        assertNull(plan!!.steps[0].condition)
    }

    @Test fun parsesAndEncodesBatteryCondition() {
        val json = """{"language":"hi-IN","steps":[{"action":"torch","target":"","value":"","condition":{"type":"BATTERY_PERCENT","operator":"LESS_THAN","value":20,"upperValue":0}}]}"""
        val plan = PlanValidator.parse(json)
        assertNotNull(plan)
        assertEquals(20, plan!!.steps[0].condition!!.value)
        val restored = PlanValidator.parse(PlanValidator.encode(plan))
        assertEquals(plan, restored)
    }

    @Test fun rejectsInvalidConditionOperator() {
        val json = """{"language":"hi-IN","steps":[{"action":"torch","target":"","value":"","condition":{"type":"BATTERY_PERCENT","operator":"BETWEEN","value":20,"upperValue":50}}]}"""
        assertNull(PlanValidator.parse(json))
    }

    @Test fun rejectsOutOfRangeConditionValue() {
        val json = """{"language":"hi-IN","steps":[{"action":"torch","target":"","value":"","condition":{"type":"BATTERY_PERCENT","operator":"LESS_THAN","value":101,"upperValue":0}}]}"""
        assertNull(PlanValidator.parse(json))
    }

    @Test fun rejectsUnknownConditionField() {
        val json = """{"language":"hi-IN","steps":[{"action":"torch","target":"","value":"","condition":{"type":"BATTERY_PERCENT","operator":"LESS_THAN","value":20,"upperValue":0,"extra":true}}]}"""
        assertNull(PlanValidator.parse(json))
    }

    @Test fun rejectsFractionalConditionValue() {
        val json = """{"language":"hi-IN","steps":[{"action":"torch","target":"","value":"","condition":{"type":"BATTERY_PERCENT","operator":"LESS_THAN","value":20.5,"upperValue":0}}]}"""
        assertNull(PlanValidator.parse(json))
    }


    @Test fun rejectsPreviousStepConditionOnFirstStep() {
        val json = """{"language":"hi-IN","steps":[{"action":"torch","target":"","value":"","condition":{"type":"PREVIOUS_STEP","operator":"SUCCEEDED","value":0,"upperValue":0}}]}"""
        assertNull(PlanValidator.parse(json))
    }

    @Test fun acceptsPreviousStepConditionAfterFirstStep() {
        val json = """{"language":"hi-IN","steps":[{"action":"camera","target":"","value":""},{"action":"torch","target":"","value":"","condition":{"type":"PREVIOUS_STEP","operator":"SUCCEEDED","value":0,"upperValue":0}}]}"""
        val plan = PlanValidator.parse(json)
        assertNotNull(plan)
        assertEquals(
            com.akingstar.askai.automation.RoutineConditionType.PREVIOUS_STEP,
            plan!!.steps[1].condition!!.type
        )
    }

    @Test fun acceptsOvernightTimeCondition() {
        val json = """{"language":"hi-IN","steps":[{"action":"torch","target":"","value":"","condition":{"type":"TIME_OF_DAY","operator":"BETWEEN","value":1320,"upperValue":360}}]}"""
        val plan = PlanValidator.parse(json)
        assertNotNull(plan)
        assertEquals(1320, plan!!.steps[0].condition!!.value)
        assertEquals(360, plan.steps[0].condition!!.upperValue)
    }

    @Test fun rejectsOutOfRangeTimeCondition() {
        val json = """{"language":"hi-IN","steps":[{"action":"torch","target":"","value":"","condition":{"type":"TIME_OF_DAY","operator":"BETWEEN","value":1440,"upperValue":360}}]}"""
        assertNull(PlanValidator.parse(json))
    }

}

========== SERVICE / EXECUTOR REFERENCES ==========
app/src/main/java/com/akingstar/askai/MainActivity.kt:81:        executor = ActionExecutor(this) { tts.speak(it) }
app/src/main/java/com/akingstar/askai/MainActivity.kt:348:                    val plan = routine?.let { PlanValidator.parse(it.planJson) }
app/src/main/java/com/akingstar/askai/service/AskAccessibilityService.kt:7:class AskAccessibilityService : AccessibilityService() {
app/src/main/java/com/akingstar/askai/engine/ActionExecutor.kt:9:class ActionExecutor(private val context: Context) {
app/src/main/java/com/akingstar/askai/ActionPlan.kt:20:        "tap_text", "long_press_text", "type_text",
app/src/main/java/com/akingstar/askai/ActionPlan.kt:21:        "scroll_up", "scroll_down", "home", "back",
app/src/main/java/com/akingstar/askai/ActionPlan.kt:23:        "read_screen", "accessibility_settings",
app/src/main/java/com/akingstar/askai/ActionPlan.kt:74:                if (action in setOf("tap_text", "long_press_text") &&
app/src/main/java/com/akingstar/askai/ActionPlan.kt:78:                if (action == "type_text" &&
app/src/main/java/com/akingstar/askai/ActionPlan.kt:83:                    "scroll_up", "scroll_down", "home", "back",
app/src/main/java/com/akingstar/askai/ActionPlan.kt:85:                    "read_screen", "accessibility_settings"
app/src/main/java/com/akingstar/askai/OfflineParser.kt:17:        listOf("scroll_up", "scroll up", "ऊपर स्क्रॉल"),
app/src/main/java/com/akingstar/askai/OfflineParser.kt:18:        listOf("scroll_down", "scroll down", "नीचे स्क्रॉल"),
app/src/main/java/com/akingstar/askai/OfflineParser.kt:19:        listOf("read_screen", "read screen", "स्क्रीन पढ़ो", "यहाँ क्या लिखा है"),
app/src/main/java/com/akingstar/askai/AIClient.kt:13:    const val SYSTEM_PROMPT = """You are Ask AI, a safe Hindi-first Android phone assistant. Understand Hindi, Hinglish and English and reply in the user's language. Return JSON only. Root schema: {"language":"hi-IN","steps":[{"action":"open_app","target":"YouTube","value":""}]}. Root keys must be exactly language and steps. Use 1 to 5 steps. Every step must have action,target,value strings and may optionally have condition. A condition gates only its own step: if false, that step is skipped; if its inputs are unavailable or invalid, execution fails safely. Condition schema must be exactly {"type":"BATTERY_PERCENT|TIME_OF_DAY|PREVIOUS_STEP","operator":"...","value":0,"upperValue":0}. BATTERY_PERCENT supports LESS_THAN, AT_LEAST, EQUALS with value 0..100 and upperValue 0. TIME_OF_DAY supports BETWEEN with value and upperValue as minutes after midnight, each 0..1439; overnight ranges are allowed. PREVIOUS_STEP supports SUCCEEDED or FAILED, with value and upperValue both 0; it refers only to the immediately preceding step and must never be used on the first step. Do not invent conditions or add condition when the user did not request one. Never invent phone numbers. Never execute calls, SMS, contact access, payments, money transfers, OTP, PIN, password, factory reset, deletion or uninstall actions. Accessibility actions: tap_text and long_press_text use target as the exact visible label and require empty value; type_text uses target as the exact field hint and value as the exact text; scroll_up, scroll_down, home, back, recents, notifications, quick_settings and read_screen require empty target and value. accessibility_settings opens Android Accessibility settings. Never guess labels or field hints. Never type passwords, OTPs, PINs, CVV or payment credentials. Never tap Send, Submit, Delete, Remove, Pay, Transfer, Purchase or similar risky controls. Use only supported safe actions. If uncertain, ask for clarification rather than guessing. No markdown and no prose outside JSON."""
app/src/main/java/com/akingstar/askai/AIClient.kt:114:                                else PlanValidator.parse(planJson)
app/src/main/java/com/akingstar/askai/ActionExecutor.kt:33:class ActionExecutor(private val context: Context, private val speak: (String) -> Unit) {
app/src/main/java/com/akingstar/askai/ActionExecutor.kt:215:            step.action == "tap_text" &&
app/src/main/java/com/akingstar/askai/ActionExecutor.kt:219:            step.action == "type_text" &&
app/src/main/java/com/akingstar/askai/ActionExecutor.kt:286:            "tap_text" ->
app/src/main/java/com/akingstar/askai/ActionExecutor.kt:287:                accessibilityAction("tap_text", target = step.target)
app/src/main/java/com/akingstar/askai/ActionExecutor.kt:292:            "type_text" ->
app/src/main/java/com/akingstar/askai/ActionExecutor.kt:294:                    "type_text",
app/src/main/java/com/akingstar/askai/ActionExecutor.kt:299:            "scroll_up" -> accessibilityAction("scroll_up")
app/src/main/java/com/akingstar/askai/ActionExecutor.kt:300:            "scroll_down" -> accessibilityAction("scroll_down")
app/src/main/java/com/akingstar/askai/ActionExecutor.kt:307:            "read_screen" -> {
app/src/main/java/com/akingstar/askai/ActionExecutor.kt:308:                val screen = AskAIAccessibilityService.perform("read_screen")
app/src/main/java/com/akingstar/askai/ActionExecutor.kt:419:        val result = AskAIAccessibilityService.perform(action, value, target)
app/src/main/java/com/akingstar/askai/SafetyGate.kt:87:            "security_settings", "tap_text", "long_press_text",
app/src/main/java/com/akingstar/askai/SafetyGate.kt:88:            "type_text"
app/src/main/java/com/akingstar/askai/AskAIAccessibilityService.kt:10:class AskAIAccessibilityService : AccessibilityService() {
app/src/main/java/com/akingstar/askai/AskAIAccessibilityService.kt:36:            "tap_text" ->
app/src/main/java/com/akingstar/askai/AskAIAccessibilityService.kt:42:            "type_text" ->
app/src/main/java/com/akingstar/askai/AskAIAccessibilityService.kt:45:            "scroll_up" ->
app/src/main/java/com/akingstar/askai/AskAIAccessibilityService.kt:48:            "scroll_down" ->
app/src/main/java/com/akingstar/askai/AskAIAccessibilityService.kt:66:            "read_screen" ->
app/src/main/java/com/akingstar/askai/AskAIAccessibilityService.kt:75:        private var activeService: AskAIAccessibilityService? = null
app/src/main/java/com/akingstar/askai/automation/AutomationStore.kt:22:        if (planJson.length > 8192 || PlanValidator.parse(planJson) == null) return false
app/src/main/java/com/akingstar/askai/automation/AutomationStore.kt:105:                    PlanValidator.parse(plan) != null) {
app/src/main/java/com/akingstar/askai/automation/AutomationHistoryStore.kt:44:        val parsed = com.akingstar.askai.PlanValidator.parse(planJson)
app/src/main/java/com/akingstar/askai/automation/AutomationHistoryStore.kt:188:        val planSize = com.akingstar.askai.PlanValidator.parse(planJson)?.steps?.size
app/src/main/AndroidManifest.xml:46:        <service android:name=".AskAIAccessibilityService"
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:9:            "scroll_up", "scroll_down", "home", "back",
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:11:            "read_screen", "accessibility_settings"
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:15:            assertNotNull("$action should be accepted", PlanValidator.parse(json))
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:20:        val tap = """{"language":"hi-IN","steps":[{"action":"tap_text","target":"Settings","value":""}]}"""
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:22:        val type = """{"language":"hi-IN","steps":[{"action":"type_text","target":"Search","value":"weather"}]}"""
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:23:        assertNotNull(PlanValidator.parse(tap))
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:24:        assertNotNull(PlanValidator.parse(press))
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:25:        assertNotNull(PlanValidator.parse(type))
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:29:        val noTarget = """{"language":"hi-IN","steps":[{"action":"tap_text","target":"","value":""}]}"""
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:30:        val typeNoHint = """{"language":"hi-IN","steps":[{"action":"type_text","target":"","value":"hello"}]}"""
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:31:        val typeNoValue = """{"language":"hi-IN","steps":[{"action":"type_text","target":"Search","value":""}]}"""
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:33:        assertNull(PlanValidator.parse(noTarget))
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:34:        assertNull(PlanValidator.parse(typeNoHint))
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:35:        assertNull(PlanValidator.parse(typeNoValue))
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:36:        assertNull(PlanValidator.parse(navWithTarget))
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:41:        assertNotNull(PlanValidator.parse(p))
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:45:        assertNull(PlanValidator.parse(p))
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:51:        assertNull(PlanValidator.parse(p))
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:56:        assertNull(PlanValidator.parse(p))
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:61:        assertNull(PlanValidator.parse(p))
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:67:        assertNull(PlanValidator.parse(p))
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:72:        assertNull(PlanValidator.parse(p))
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:77:        assertNull(PlanValidator.parse(p))
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:82:        assertNull(PlanValidator.parse(p))
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:88:        assertNull(PlanValidator.parse(p))
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:94:        assertNull(PlanValidator.parse(p))
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:98:        assertNull(PlanValidator.parse(" ".repeat(8193)))
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:104:        assertNull(PlanValidator.parse(blank))
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:105:        assertNull(PlanValidator.parse(long))
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:109:        assertNull(PlanValidator.parse("""{"language":"hi-IN","steps":["""))
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:115:        val plan = PlanValidator.parse(json)
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:122:        val plan = PlanValidator.parse(json)
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:125:        val restored = PlanValidator.parse(PlanValidator.encode(plan))
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:131:        assertNull(PlanValidator.parse(json))
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:136:        assertNull(PlanValidator.parse(json))
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:141:        assertNull(PlanValidator.parse(json))
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:146:        assertNull(PlanValidator.parse(json))
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:152:        assertNull(PlanValidator.parse(json))
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:157:        val plan = PlanValidator.parse(json)
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:167:        val plan = PlanValidator.parse(json)
app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt:175:        assertNull(PlanValidator.parse(json))
app/src/test/java/com/akingstar/askai/SafetyGateTest.kt:28:        assertTrue(SafetyGate.needsConfirmation("tap_text", "Settings"))
app/src/test/java/com/akingstar/askai/SafetyGateTest.kt:30:        assertTrue(SafetyGate.needsConfirmation("type_text", "Search"))
app/src/test/java/com/akingstar/askai/AccessibilityOfflineParserTest.kt:17:        assertEquals("scroll_up", OfflineParser.parse("ऊपर स्क्रॉल")?.steps?.first()?.action)
app/src/test/java/com/akingstar/askai/AccessibilityOfflineParserTest.kt:18:        assertEquals("scroll_down", OfflineParser.parse("नीचे स्क्रॉल")?.steps?.first()?.action)
app/src/test/java/com/akingstar/askai/AccessibilityOfflineParserTest.kt:19:        assertEquals("read_screen", OfflineParser.parse("स्क्रीन पढ़ो")?.steps?.first()?.action)

========== ACCESSIBILITY CONFIG FILES ==========
app/src/main/res/xml/accessibility_service_config.xml
