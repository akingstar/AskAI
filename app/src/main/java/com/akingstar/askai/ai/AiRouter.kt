package com.akingstar.askai.ai
import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
class KeyStorePrefs(context:Context){ private val master=MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(); private val prefs=EncryptedSharedPreferences.create(context,"askai_secure",master,EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM); fun get(name:String)=prefs.getString(name,"")?:""; fun put(name:String,value:String){prefs.edit().putString(name,value.trim()).apply()} }
object AiConfig {
 const val GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
 const val GEMINI_MODEL = "gemini-2.5-flash"
 const val GROQ_URL = "https://api.groq.com/openai/v1/chat/completions"
 const val GROQ_MODEL = "llama-3.3-70b-versatile"
}
class AiRouter(private val keys:KeyStorePrefs){ private val client=OkHttpClient.Builder().connectTimeout(12,TimeUnit.SECONDS).readTimeout(20,TimeUnit.SECONDS).build()
 private fun post(url:String,key:String,body:JSONObject,header:String):String?=runCatching{val b=Request.Builder().url(url).post(body.toString().toRequestBody("application/json".toMediaType()));if(key.isNotBlank())b.addHeader(header,if(header=="Authorization")"Bearer $key" else key);client.newCall(b.build()).execute().use{if(it.isSuccessful)it.body?.string()else null}}.getOrNull()
 suspend fun suggest(command:String):String? { val system="Return only JSON: {\"steps\":[{\"action\":\"speak\",\"value\":\"Hindi response\",\"requiresConfirmation\":false}]}. Allowed actions: speak, call, whatsapp, camera, alarm, youtube, maps, settings, sos, stop. Max 5 steps. Never handle OTP, PIN, passwords."
  val g=keys.get("gemini_key"); if(g.isNotBlank()){val body=JSONObject().put("contents",org.json.JSONArray().put(JSONObject().put("parts",org.json.JSONArray().put(JSONObject().put("text","$system\nUser: $command")))));val out=post("${AiConfig.GEMINI_URL}${AiConfig.GEMINI_MODEL}:generateContent?key=$g","",body,"");if(out!=null)runCatching{JSONObject(out).getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")}.getOrNull()?.let{return it}}
  val k=keys.get("groq_key");if(k.isNotBlank()){val body=JSONObject().put("model",AiConfig.GROQ_MODEL).put("temperature",0).put("messages",org.json.JSONArray().put(JSONObject().put("role","system").put("content",system)).put(JSONObject().put("role","user").put("content",command)));val out=post("${AiConfig.GROQ_URL}",k,body,"Authorization");if(out!=null)runCatching{JSONObject(out).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")}.getOrNull()?.let{return it}}
  return null }
}
