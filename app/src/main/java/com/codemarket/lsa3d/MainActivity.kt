package com.codemarket.lsa3d

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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

    private val microphonePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListening()
        else onStatus?.invoke("Se necesita permiso de micrófono para escuchar la misa.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var transcript by remember { mutableStateOf("") }
                var status by remember { mutableStateOf("Listo para comenzar") }
                var listening by remember { mutableStateOf(false) }
                var avatar by remember { mutableStateOf("Femenino") }

                onText = { transcript = it }
                onStatus = {
                    status = it
                    listening = it == "Escuchando…"
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
                                // Marcador visual provisional: no representa una animación ni una seña.
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(210.dp)
                                        .background(Color(0xFFE8F1F4), RoundedCornerShape(20.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        androidx.compose.foundation.layout.Box(
                                            modifier = Modifier
                                                .background(Color(0xFFD3E5E9), CircleShape)
                                                .padding(horizontal = 27.dp, vertical = 18.dp)
                                        ) {
                                            Text(if (avatar == "Femenino") "♀" else "♂", fontSize = 54.sp, color = Teal)
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Text("Avatar $avatar", color = Ink, fontWeight = FontWeight.SemiBold)
                                        Text("Modelo 3D en desarrollo", color = Muted, fontSize = 12.sp)
                                    }
                                }
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
                                        if (listening) stopListening()
                                        else {
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
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4DE))
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Estado de traducción LSA", color = Color(0xFF76551B), fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    "La traducción automática a LSA y las señas animadas todavía no están implementadas. Esta versión solo transcribe el audio; no presenta la transcripción como si fuera una seña.",
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
                        if (!text.isNullOrBlank()) onText?.invoke(text)
                        onStatus?.invoke("Transcripción recibida")
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
            onStatus?.invoke("No se pudo iniciar el reconocimiento de voz.")
        }
    }

    private fun stopListening() {
        runCatching { speechRecognizer?.stopListening() }
        onStatus?.invoke("Escucha detenida")
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        super.onDestroy()
    }
}
