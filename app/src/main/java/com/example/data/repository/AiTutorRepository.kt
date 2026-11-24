package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val AI_TAG = "AiTutorRepo"
const val AI_TUTOR_GREETING =
    "Ani AI TUTOR Mana Barumsaa Koofkeetin jedhama. Maal Isin gargaaru?"

sealed interface AiStreamEvent {
    data class Chunk(val accumulatedText: String) : AiStreamEvent
    data class Completed(val finalText: String) : AiStreamEvent
    data class Error(val message: String) : AiStreamEvent
}

class AiTutorRepository {

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    """
                    Ati "AI TUTOR Mana Barumsaa Koofkeet" (Koofkeet School AI Tutor) dha.
                    Yeroo jalqabaa ykn gaaffii eenyummaa gaafatamtu: "$AI_TUTOR_GREETING" jedhi.
                    Barattoota Mana Barumsaa Koofkeet barnoota isaanii (Herrega, Baayoolojii, Fiiziksii, Keemistirii, Afaan Oromoo, Ingiliffa, Seenaa, fi kkf) akka hubatan gargaari.
                    Deebii kee yeroo baay'ee Afaan Oromoo qulqulluu fi ifaa ta'een kenni (barataan yoo Afaan Ingiliffaan gaafate Ingiliffaan ykn wal-makaa ibsa gahaa kenni).
                    Hubannoo step-by-step kenni malee qormaata gatii hin qabne kophaatti hin gowwoomsin; barataan akka yaadu jajjabeessi.
                    Iccitii fi naamusa mana barumsaa eegi.
                    """.trimIndent()
                )
            }
        )
    }

    fun streamTutorAnswer(
        prompt: String,
        subject: String,
        attachedImage: Bitmap? = null
    ): Flow<AiStreamEvent> = flow {
        val fullPrompt = "Gosa Barumsaa (Subject): $subject\nGaaffii Barataa: $prompt"
        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                val inputContent = content {
                    if (attachedImage != null) {
                        image(attachedImage)
                    }
                    text(fullPrompt)
                }
                model.generateContentStream(inputContent).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(AiStreamEvent.Chunk(accumulated.toString()))
                    }
                }
            }
            val resultText = accumulated.toString().trim()
            if (resultText.isNotEmpty()) {
                emit(AiStreamEvent.Completed(resultText))
            } else {
                emit(AiStreamEvent.Error("Deebiin AI duwwaa ta'e. Maaloo irra deebi'aa yaalaa."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(AI_TAG, "AI Tutor generation timed out", e)
            emit(AiStreamEvent.Error("Yeroon deebii AI (60s) xumurameera. Maaloo irra deebi'aa yaalaa."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AI_TAG, "Firebase AI generation failed", e)
            emit(
                AiStreamEvent.Error(
                    e.localizedMessage ?: "Gargaaraan AI yeroo ammaa hin milkoofne."
                )
            )
        }
    }
}
