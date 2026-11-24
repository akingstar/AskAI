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

        if (SafetyGate.blocked(sensitiveText) ||
            SafetyGate.isNeverAllowedAction(step.action)
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
