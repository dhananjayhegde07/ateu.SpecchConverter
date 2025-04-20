package com.ateu.textconverter

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
//import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ateu.textconverter.ui.theme.TextConverterTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.Locale
import android.Manifest
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import com.google.mlkit.nl.translate.TranslateLanguage
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        setContent {
            TextConverterTheme {
                SpeechToTextScreen()
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SpeechToTextScreen() {
    val context = LocalContext.current
    val permissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val translator = remember { Translator() }
    val source = remember { mutableStateOf(IndianLanguage("English", "en-IN", "en")) }
    val target = remember { mutableStateOf(IndianLanguage("Kannada", "kn-IN", "kn")) }
    val speechIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
    }

    val partial = remember { mutableStateOf("") }
    val spokenText = remember { mutableStateOf("") }
    val isListening = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val recognitionListener = remember {
        object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    partial.value = matches[0]
                    scope.launch{
                        translator.translateText(matches[0], { str ->
                            Log.d("TAG", "onResults: $str")
                            spokenText.value = str
                        }, {})
                    }
                }
                isListening.value = false
            }

            override fun onError(error: Int) {
                spokenText.value = "Error: $error"
                isListening.value = false
                partial.value = ""
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                speechRecognizer.stopListening()
                partial.value = ""
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (spokenText.value == matches?.getOrNull(0)) return
                spokenText.value = matches?.getOrNull(0) ?: ""
                partial.value = spokenText.value
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    DisposableEffect(Unit) {
        speechRecognizer.setRecognitionListener(recognitionListener)
        onDispose {
            speechRecognizer.destroy()
        }
    }


        if (permissionState.status.isGranted) {
            SpeechToTextContent(
                partial = partial,
                spokenText = spokenText,
                isListening = isListening,
                speechRecognizer = speechRecognizer,
                speechIntent = speechIntent,
                source = source,
                target = target,
                translator = translator
            )
        } else {
            Button(onClick = { permissionState.launchPermissionRequest() }) {
                Text("Grant Microphone Permission")
            }
        }

}




@Composable
fun SpeechToTextContent(
    partial: MutableState<String>,
    spokenText: MutableState<String>,
    isListening: MutableState<Boolean>,
    speechRecognizer: SpeechRecognizer,
    speechIntent: Intent,
    source: MutableState<IndianLanguage>,
    target: MutableState<IndianLanguage>,
    translator: Translator
) {
    val loading = remember { mutableStateOf(false) }
    val expanded1 = remember { mutableStateOf(false) }
    val expanded2 = remember { mutableStateOf(false) }

    if(loading.value){
        Loading()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(4f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .verticalScroll(rememberScrollState())
                .background(Color(0xFFE0E0E0))
                .padding(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("You said : ", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(8.dp))
                Box {
                    TextButton(onClick = { expanded1.value = true }) {
                        Text(text = source.value.displayName)
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Language"
                        )
                    }
                }
                DropdownMenu(
                    expanded = expanded1.value,
                    onDismissRequest = { expanded1.value = false }
                ) {
                    indianLanguages.forEach { language ->
                        DropdownMenuItem(
                            text = { Text(language.displayName) },
                            onClick = {
                                source.value = language
                                loading.value = true
                                translator.prepareTranslator(
                                    source.value.mlKitTag,target.value.mlKitTag,
                                    {loading.value = false},
                                    {loading.value = false}
                                )
                                expanded1.value = false
                            }
                        )
                    }
                }

            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = partial.value, style = MaterialTheme.typography.bodyLarge)
        }
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .weight(4f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .verticalScroll(rememberScrollState())
                    .background(Color(0xFFE0E0E0))
                    .padding(10.dp)
            ) {
                Row (
                   horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text("Translation :  ", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box {
                        TextButton(onClick = { expanded2.value = true }) {
                            Text(text = target.value.displayName)
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Language"
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = expanded2.value,
                        onDismissRequest = { expanded2.value = false }
                    ) {
                        indianLanguages.forEach { language ->
                            DropdownMenuItem(
                                text = { Text(language.displayName) },
                                onClick = {
                                    target.value = language
                                    loading.value = true
                                    translator.prepareTranslator(
                                        source.value.mlKitTag,target.value.mlKitTag,
                                        {loading.value = false},
                                        {loading.value = false}
                                    )
                                    expanded2.value = false
                                }
                            )
                        }
                    }

                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = spokenText.value, style = MaterialTheme.typography.bodyLarge)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Buttons
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = {
                    if (isListening.value) {
                        speechRecognizer.stopListening()
                    } else {
                        // Dynamically update the language in the intent
                        Log.d("TAG", "SpeechToTextContent: setting ${source.value.speechRecognizerTag}")
                        val updatedIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, source.value.speechRecognizerTag)
                        }
                        speechRecognizer.startListening(updatedIntent)
                    }
                    isListening.value = !isListening.value
                }) {
                    Icon(
                        imageVector = if (!isListening.value) Icons.Default.PlayArrow else Icons.Default.Close,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isListening.value) "Pause" else "Start")
                }
            }
    }
}


@Composable
fun Loading(){
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = {})
            .zIndex(2f)
            .background(Color(0x77595959))
    ) {
        Text(text = "Preparing", style = MaterialTheme.typography.titleLarge)
    }
}
