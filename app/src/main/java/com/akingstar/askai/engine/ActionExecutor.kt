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
