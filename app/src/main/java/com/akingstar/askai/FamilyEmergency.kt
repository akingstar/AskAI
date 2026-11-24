package com.akingstar.askai

import android.content.Context
import android.content.Intent
import android.net.Uri

object FamilyEmergency {
    fun launch(context: Context, latitude: Double?, longitude: Double?) {
        val number = context.getSharedPreferences("askai", Context.MODE_PRIVATE)
            .getString("family_number", "").orEmpty()
        if (number.isBlank()) return
        val location = if (latitude != null && longitude != null)
            " https://maps.google.com/?q=$latitude,$longitude" else ""
        val message = "मुझे मदद चाहिए। कृपया मुझसे संपर्क करें।$location"
        val sms = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(number)}"))
            .putExtra("sms_body", message).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(sms)
    }
}
