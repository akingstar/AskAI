package com.akingstar.askai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.akingstar.askai.ai.AiRouter
import com.akingstar.askai.ai.KeyStorePrefs
import com.akingstar.askai.data.AskDatabase
import com.akingstar.askai.data.HistoryEntry
import com.akingstar.askai.engine.ActionExecutor
import com.akingstar.askai.engine.PlanStep
import com.akingstar.askai.engine.Planner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null
    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) listenNow() }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); tts = TextToSpeech(this, this); setContent { AskAiScreen(::listenNow, ::handleCommand) } }
    override fun onInit(status: Int) { if (status == TextToSpeech.SUCCESS) tts?.language = Locale("hi", "IN") }
    private fun say(text: String) { runOnUiThread { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "askai-${System.currentTimeMillis()}") } }
    private fun listenNow() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { micPermission.launch(Manifest.permission.RECORD_AUDIO); return }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) { say("इस फोन में speech recognition उपलब्ध नहीं है।"); return }
        runCatching {
            if (recognizer == null) recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { sr ->
                sr.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) { say("जी, बोलिए।") }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) { say("आवाज़ साफ़ नहीं मिली। कृपया फिर बोलिए।") }
                    override fun onResults(results: Bundle?) { results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(::handleCommand) }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
            val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN").putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN").putExtra(RecognizerIntent.EXTRA_PROMPT, "बोलिए जी, क्या करना है?")
            recognizer?.startListening(i)
        }.onFailure { say("इस फोन में speech recognition शुरू नहीं हो पाई।") }
    }
    private fun handleCommand(command: String) {
        if (Planner.isStop(command)) { say("ठीक है जी, रोक दिया।"); return }
        val warning = Planner.scamWarning(command)
        if (warning != null) { say(warning); save(command, warning); return }
        lifecycleScope.launch {
            val keys = KeyStorePrefs(this@MainActivity)
            val router = AiRouter(keys)
            val remote = withContext(Dispatchers.IO) { router.suggest(command) }
            var plan = remote?.let { Planner.validate(it) }
            if (plan == null && remote != null) {
                val repaired = withContext(Dispatchers.IO) { router.suggest("Repair the following into valid JSON matching the allowed ActionPlan schema. Return JSON only: $remote") }
                plan = repaired?.let { Planner.validate(it) }
            }
            plan = plan ?: Planner.plan(command)
            if (plan.steps.any { it.requiresConfirmation || it.action in setOf("call", "sos") }) {
                val msg = "इस अनुरोध में पुष्टि जरूरी है। अभी सुरक्षा के लिए इसे execute नहीं किया गया।"
                say(msg); save(command, msg); return@launch
            }
            val executor = ActionExecutor(this@MainActivity)
            val results = mutableListOf<String>()
            for (step in plan.steps) { if (Planner.isStop(command)) break; val result = executor.execute(step); results += result; say(result) }
            save(command, results.joinToString(" ").ifBlank { "रोक दिया गया।" })
        }
    }
    private fun save(command: String, result: String) { lifecycleScope.launch(Dispatchers.IO) { AskDatabase.get(this@MainActivity).historyDao().add(HistoryEntry(command = command, result = result)) } }
    override fun onDestroy() { recognizer?.destroy(); recognizer = null; tts?.stop(); tts?.shutdown(); super.onDestroy() }
}

@Composable private fun AskAiScreen(onMic: () -> Unit, onCommand: (String) -> Unit) {
    val context = LocalContext.current
    var message by remember { mutableStateOf("नमस्ते जी! माइक दबाकर बोलिए।") }
    var command by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    val prefs = remember { KeyStorePrefs(context) }
    MaterialTheme(colorScheme = lightColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF174EA6))) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("Ask AI", fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text("बोलो, हो जाएगा", fontSize = 22.sp)
            Text(message, fontSize = 21.sp)
            Button(onClick = onMic, modifier = Modifier.fillMaxWidth().height(112.dp)) { Icon(Icons.Default.Mic, contentDescription = "बोलने के लिए माइक"); Spacer(Modifier.width(12.dp)); Text("बोलिए जी", fontSize = 28.sp) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { onCommand("call") }, modifier = Modifier.weight(1f).height(70.dp)) { Icon(Icons.Default.Call, contentDescription = "कॉल"); Text("कॉल") }
                Button(onClick = { onCommand("WhatsApp kholo") }, modifier = Modifier.weight(1f).height(70.dp)) { Icon(Icons.Default.Message, contentDescription = "WhatsApp"); Text("WhatsApp") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { onCommand("photo") }, modifier = Modifier.weight(1f).height(70.dp)) { Icon(Icons.Default.CameraAlt, contentDescription = "फोटो"); Text("फोटो") }
                Button(onClick = { onCommand("SOS madad") }, modifier = Modifier.weight(1f).height(70.dp)) { Icon(Icons.Default.Warning, contentDescription = "मदद"); Text("मदद") }
            }
            OutlinedTextField(value = command, onValueChange = { command = it }, label = { Text("यहाँ लिखें (वैकल्पिक)") }, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 20.sp))
            Button(onClick = { if (command.isNotBlank()) { onCommand(command); command = "" } }, modifier = Modifier.fillMaxWidth()) { Text("करें", fontSize = 22.sp) }
            TextButton(onClick = { showSettings = !showSettings }) { Text("Family Setup / Settings") }
            if (showSettings) {
                Text("API keys (वैकल्पिक)", fontWeight = FontWeight.Bold)
                var gemini by remember { mutableStateOf(prefs.get("gemini_key")) }
                var groq by remember { mutableStateOf(prefs.get("groq_key")) }
                var pin by remember { mutableStateOf("") }
                OutlinedTextField(gemini, { gemini = it }, label = { Text("Gemini API key") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(groq, { groq = it }, label = { Text("Groq API key") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(pin, { pin = it }, label = { Text("Family PIN (अभी लागू नहीं — demo field)") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { prefs.put("gemini_key", gemini); prefs.put("groq_key", groq); message = "Keys इस फोन में सुरक्षित storage में रखी गई हैं।" }) { Text("Keys सुरक्षित करें") }
                Text("Permissions manually enable करें: Android Settings → Accessibility / Notification access / Display over other apps.", fontSize = 16.sp)
            }
        }
    }
}
