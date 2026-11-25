package com.akingstar.askai

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * API credentials are stored only in AndroidX encrypted preferences.
 * There is deliberately no plaintext fallback.
 */
class ProviderKeyStore(context: Context) {
    private val appContext = context.applicationContext

    private val preferences by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            appContext,
            "askai_provider_keys",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun hasGeminiKey(): Boolean = get("gemini_key").isNotBlank()
    fun hasGroqKey(): Boolean = get("groq_key").isNotBlank()

    fun getGeminiKey(): String = get("gemini_key")
    fun getGroqKey(): String = get("groq_key")

    fun saveGeminiKey(value: String): Boolean = save("gemini_key", value)
    fun saveGroqKey(value: String): Boolean = save("groq_key", value)

    fun clearGeminiKey() = clear("gemini_key")
    fun clearGroqKey() = clear("groq_key")

    private fun get(name: String): String =
        preferences.getString(name, "").orEmpty()

    private fun save(name: String, value: String): Boolean {
        val clean = value.trim()
        if (clean.isBlank() || clean.length > 512) return false
        preferences.edit().putString(name, clean).apply()
        return true
    }

    private fun clear(name: String) {
        preferences.edit().remove(name).apply()
    }
}
