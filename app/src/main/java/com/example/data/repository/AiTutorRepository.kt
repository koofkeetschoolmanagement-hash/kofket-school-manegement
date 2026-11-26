package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.core.AppLanguage
import com.example.data.model.AiChatMessage
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

enum class StudySupportMode(val promptDirective: String) {
    EXPLAIN("Explain the concept step-by-step with clear definitions and intuitive reasoning."),
    HOMEWORK("Guide the student through solving the problem step-by-step without skipping the reasoning."),
    PRACTICE_QUIZ("Provide 3 concise practice questions with hints and check-your-understanding steps."),
    STUDY_PLAN("Create a focused, actionable study checklist and review strategy for this topic.")
}

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
                    Ati "AI TUTOR Mana Barumsaa Koofkeet" (Koofkeet School Private AI Tutor) dha.
                    Yeroo jalqabaa ykn gaaffii eenyummaa gaafatamtu: "$AI_TUTOR_GREETING" jedhi.
                    Barattoota Mana Barumsaa Koofkeet barnoota isaanii (Herrega, Baayoolojii, Fiiziksii, Keemistirii, Afaan Oromoo, Ingiliffa, Seenaa, fi kkf) akka hubatan gargaari.
                    Deebii kee yeroo hunda afaan barataan filateen (Afaan Oromoo, English, ykn Amharic) qulqulluu fi ifaa ta'een kenni.
                    Hubannoo step-by-step kenni; barataan akka yaadu jajjabeessi.
                    Iccitii barataa fi naamusa mana barumsaa eegi.
                    """.trimIndent()
                )
            }
        )
    }

    fun streamTutorAnswer(
        prompt: String,
        subject: String,
        attachedImage: Bitmap? = null,
        studyMode: StudySupportMode = StudySupportMode.EXPLAIN,
        language: AppLanguage = AppLanguage.AFAAN_OROMOO,
        gradeLevel: String = "",
        recentHistory: List<AiChatMessage> = emptyList()
    ): Flow<AiStreamEvent> = flow {
        val languageInstruction = when (language) {
            AppLanguage.AFAAN_OROMOO -> "Reply clearly in Afaan Oromoo."
            AppLanguage.ENGLISH -> "Reply clearly in English."
            AppLanguage.AMHARIC -> "Reply clearly in Amharic (አማርኛ)."
        }
        val gradeContext = if (gradeLevel.isNotBlank()) "Student Grade: $gradeLevel\n" else ""
        val fullPrompt = buildString {
            appendLine("Subject: $subject")
            if (gradeContext.isNotEmpty()) append(gradeContext)
            appendLine("Study Support Mode: ${studyMode.promptDirective}")
            appendLine("Language Preference: $languageInstruction")
            append("Student Question: $prompt")
        }

        val chatHistory = recentHistory
            .sortedBy { it.createdAt?.seconds ?: 0L }
            .takeLast(6)
            .flatMap { msg ->
                listOf(
                    content("user") { text("[${msg.subject}] ${msg.prompt}") },
                    content("model") { text(msg.response) }
                )
            }

        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                val chat = model.startChat(history = chatHistory)
                val inputContent = content {
                    if (attachedImage != null) {
                        image(attachedImage)
                    }
                    text(fullPrompt)
                }
                chat.sendMessageStream(inputContent).collect { chunk ->
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
