package com.codemarket.lsa3d

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

private val Ink = Color(0xFF17243A)
private val Teal = Color(0xFF087E8B)
private val Pale = Color(0xFFF3F7FA)
private val Muted = Color(0xFF65758B)

class MainActivity : ComponentActivity() {
    private var speechRecognizer: SpeechRecognizer? = null
    private var onText: ((String) -> Unit)? = null
    private var onStatus: ((String) -> Unit)? = null
    private var onFinal: ((String) -> Unit)? = null
    private var keepListening = false
    private val handler = Handler(Looper.getMainLooper())
    private val lexicon by lazy { Lexicon.load(this) }

    private val microphonePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListening()
        else { keepListening = false; onStatus?.invoke("Se necesita permiso de micrófono para escuchar la misa.") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var transcript by remember { mutableStateOf("") }
                var status by remember { mutableStateOf("Listo para comenzar") }
                var listening by remember { mutableStateOf(false) }
                var avatar by remember { mutableStateOf("Femenino") }

                var items by remember { mutableStateOf<List<PlanItem>>(emptyList()) }
                var timeline by remember { mutableStateOf<Timeline?>(null) }
                var pose by remember { mutableStateOf(Pose.REST) }
                var activeIdx by remember { mutableIntStateOf(-1) }
                var typed by remember { mutableStateOf("") }

                val translate: (String) -> Unit = { phrase ->
                    val plan = Planner.plan(phrase, lexicon)
                    items = plan
                    timeline = if (plan.isEmpty()) null else Timeline.build(plan)
                }

                LaunchedEffect(timeline) {
                    val tl = timeline
                    if (tl != null) {
                        val t0 = withFrameNanos { it }
                        while (true) {
                            val now = withFrameNanos { it }
                            val t = (now - t0) / 1_000_000L
                            if (t > tl.totalMs) break
                            val (p, i) = tl.poseAt(t)
                            pose = p
                            activeIdx = i
                        }
                    }
                    pose = Pose.REST
                    activeIdx = -1
                }

                onText = { transcript = it }
                onFinal = { phrase -> transcript = phrase; translate(phrase) }
                onStatus = {
                    status = it
                    listening = keepListening
                }

                Surface(modifier = Modifier.fillMaxSize(), color = Pale) {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("LSA 3D app", fontSize = 29.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Text("CODEMARKET", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Teal)
                        Spacer(Modifier.height(22.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("MODO MISA", color = Teal, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(Modifier.height(14.dp))
                                Avatar3D(
                                    pose = pose,
                                    female = avatar == "Femenino",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp)
                                        .background(Color(0xFFE8F1F4), RoundedCornerShape(20.dp))
                                )
                                Text("Arrastrá para girar la vista", color = Muted, fontSize = 11.sp)
                                Spacer(Modifier.height(14.dp))
                                Text("Elegí tu avatar", color = Ink, fontWeight = FontWeight.SemiBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    listOf("Femenino", "Masculino").forEach { option ->
                                        if (avatar == option) {
                                            Button(onClick = { avatar = option }, colors = ButtonDefaults.buttonColors(containerColor = Teal)) { Text(option) }
                                        } else {
                                            OutlinedButton(onClick = { avatar = option }) { Text(option) }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(18.dp))
                                Button(
                                    onClick = {
                                        if (listening) { keepListening = false; stopListening() }
                                        else {
                                            keepListening = true
                                            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startListening()
                                            else microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(54.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = if (listening) Color(0xFFB23A48) else Teal)
                                ) {
                                    Text(if (listening) "DETENER ESCUCHA" else "INICIAR ESCUCHA", fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(10.dp))
                                Text(status, color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center)
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Lo que se escucha", color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    transcript.ifBlank { "La transcripción aparecerá acá cuando el reconocimiento de voz detecte palabras." },
                                    color = if (transcript.isBlank()) Muted else Ink,
                                    fontSize = 15.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Traducción a señas", color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Spacer(Modifier.height(8.dp))
                                if (items.isEmpty()) {
                                    Text("Cuando se escuche una frase, cada palabra aparece acá indicando si tiene seña disponible.", color = Muted, fontSize = 13.sp)
                                } else {
                                    PlanChips(items, activeIdx)
                                }
                                Spacer(Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = typed,
                                    onValueChange = { typed = it },
                                    label = { Text("Probar escribiendo una frase (ej: demo)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(onClick = { transcript = typed; translate(typed) }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Traducir texto")
                                }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4DE))
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Estado de traducción LSA", color = Color(0xFF76551B), fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    "El motor de animación 3D funciona, pero el diccionario todavía no tiene señas LSA validadas por personas sordas. Las palabras sin seña se marcan como tales: la app no inventa señas. Ver docs/AGREGAR_SENAS.md.",
                                    color = Color(0xFF76551B),
                                    fontSize = 13.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(22.dp))
                        Text("CodeMarket · LSA argentina", color = Muted, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            keepListening = false
            onStatus?.invoke("Este teléfono no tiene un servicio de reconocimiento de voz disponible.")
            return
        }
        runCatching {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) { onStatus?.invoke("Escuchando…") }
                    override fun onBeginningOfSpeech() { onStatus?.invoke("Escuchando…") }
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() { onStatus?.invoke("Procesando audio…") }
                    override fun onError(error: Int) {
                        if (keepListening && (error == SpeechRecognizer.ERROR_NO_MATCH ||
                                error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                                error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY)) {
                            restartSoon(); return
                        }
                        keepListening = false
                        onStatus?.invoke(
                            when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH -> "No se reconocieron palabras. Podés volver a intentar."
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No se detectó voz. Volvé a iniciar la escucha."
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Falta permiso de micrófono."
                                else -> "Se interrumpió el reconocimiento. Tocá iniciar para reintentar."
                            }
                        )
                    }
                    override fun onResults(results: Bundle?) {
                        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                        if (!text.isNullOrBlank()) onFinal?.invoke(text)
                        if (keepListening) restartSoon() else onStatus?.invoke("Transcripción recibida")
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                        if (!text.isNullOrBlank()) onText?.invoke(text)
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-AR")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-AR")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
            speechRecognizer?.startListening(intent)
            onStatus?.invoke("Iniciando micrófono…")
        }.onFailure {
            keepListening = false
            onStatus?.invoke("No se pudo iniciar el reconocimiento de voz.")
        }
    }

    private fun restartSoon() {
        onStatus?.invoke("Escuchando…")
        handler.postDelayed({ if (keepListening) startListening() }, 400)
    }

    private fun stopListening() {
        runCatching { speechRecognizer?.stopListening() }
        onStatus?.invoke("Escucha detenida")
    }

    override fun onDestroy() {
        keepListening = false
        handler.removeCallbacksAndMessages(null)
        speechRecognizer?.destroy()
        speechRecognizer = null
        super.onDestroy()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanChips(items: List<PlanItem>, activeIdx: Int) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEachIndexed { i, item ->
            val sign = item.sign
            val (bg, label) = when {
                sign == null -> Color(0xFFE6E9EE) to "${item.word} · sin seña validada"
                sign.validated -> Color(0xFFD8F0DC) to "${item.word} · LSA validada"
                else -> Color(0xFFFFE9B8) to "${item.word} · animación de prueba"
            }
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .background(if (i == activeIdx) Teal else bg, RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(label, fontSize = 12.sp, color = if (i == activeIdx) Color.White else Ink)
            }
        }
    }
}
