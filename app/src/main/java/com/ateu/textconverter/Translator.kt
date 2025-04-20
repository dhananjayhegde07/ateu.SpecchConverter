package com.ateu.textconverter

import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

class Translator{
    var translator: Translator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.KANNADA)
            .build()
    )
    fun prepareTranslator(source: String, target: String,onSuccess: (String) -> Unit,onError: (String) -> Unit) {
        this.translator = Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(source)
                .setTargetLanguage(target)
                .build()
        )
        translator.downloadModelIfNeeded().addOnCompleteListener { e->
            onSuccess("Success")
        }.addOnFailureListener { e ->
            Log.d("TAG", "prepareTranslator: ${e.message}")
            onError("Failed")
        }
    }
    fun translateText(
        text: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val translator = translator

        // Download the model if necessary
        translator.downloadModelIfNeeded(DownloadConditions.Builder().build())
            .addOnSuccessListener { e->
                Log.d("TAG", "translateText: ")
                translator.translate(text)
                    .addOnSuccessListener { translatedText ->
                        onSuccess(translatedText)
                    }
                    .addOnFailureListener { e ->
                        onError("Translation failed: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                onError("Model download failed: ${e.message}")
            }
    }
}



data class IndianLanguage(
    val displayName: String,
    val speechRecognizerTag: String, // BCP-47
    val mlKitTag: String             // ML Kit language tag
)

val indianLanguages = listOf(
    IndianLanguage("English", "en-IN", "en"),
    IndianLanguage("Hindi", "hi-IN", "hi"),
    IndianLanguage("Kannada", "kn-IN", "kn"),
    IndianLanguage("Tamil", "ta-IN", "ta"),
    IndianLanguage("Telugu", "te-IN", "te"),
    IndianLanguage("Bengali", "bn-IN", "bn"),
    IndianLanguage("Marathi", "mr-IN", "mr"),
    IndianLanguage("Gujarati", "gu-IN", "gu"),
    IndianLanguage("Malayalam", "ml-IN", "ml"),
    IndianLanguage("Punjabi", "pa-IN", "pa"),
    IndianLanguage("Urdu", "ur-IN", "ur")
)