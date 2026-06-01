package com.example.data.repository

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.ChatDao
import com.example.data.local.ChatMessage
import com.example.data.local.ChatSession
import com.example.data.models.Content
import com.example.data.models.GenerateContentRequest
import com.example.data.models.GenerationConfig
import com.example.data.models.ImageConfig
import com.example.data.models.Part
import com.example.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ChatRepository(
    private val context: Context,
    private val chatDao: ChatDao
) {
    val allSessions: Flow<List<ChatSession>> = chatDao.getAllSessions()

    fun getMessages(sessionId: Long): Flow<List<ChatMessage>> {
        return chatDao.getMessagesForSession(sessionId)
    }

    suspend fun createSession(title: String): Long = withContext(Dispatchers.IO) {
        chatDao.insertSession(ChatSession(title = title))
    }

    suspend fun deleteSession(sessionId: Long) = withContext(Dispatchers.IO) {
        chatDao.deleteMessagesForSession(sessionId)
        chatDao.deleteSession(sessionId)
    }

    suspend fun updateSessionTitle(sessionId: Long, newTitle: String) = withContext(Dispatchers.IO) {
        chatDao.updateSessionTitle(sessionId, newTitle)
    }

    /**
     * Submits a user prompt, retrieves the generated image from Gemini, saves it
     * locally to filesDir, and inserts both user and AI companion messages into Room.
     */
    suspend fun generateImage(
        sessionId: Long,
        prompt: String,
        aspectRatio: String, // "1:1", "16:9", "4:3", "9:16", "3:4"
        imageSize: String = "1K"
    ): Unit = withContext(Dispatchers.IO) {
        // 1. Save user prompt message
        val userMsg = ChatMessage(
            sessionId = sessionId,
            sender = "user",
            text = prompt,
            aspectRatio = aspectRatio
        )
        chatDao.insertMessage(userMsg)

        // 2. Formulate model request.
        // We use gemini-3.1-flash-image-preview as "google nano banana 2"
        val model = "gemini-3.1-flash-image-preview"
        val apiKey = BuildConfig.GEMINI_API_KEY

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            val errorMsg = ChatMessage(
                sessionId = sessionId,
                sender = "ai",
                text = "Shadow AI: API Key is missing. Please enter your GEMINI_API_KEY in the Secrets panel (configured via .env in AI Studio) to generate images.",
                timestamp = System.currentTimeMillis()
            )
            chatDao.insertMessage(errorMsg)
            return@withContext
        }

        // Insert a temporary "generating..." status or proceed directly
        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig(
                responseModalities = listOf("TEXT", "IMAGE"),
                imageConfig = ImageConfig(aspectRatio = aspectRatio, imageSize = imageSize),
                temperature = 0.9f
            ),
            systemInstruction = Content(parts = listOf(Part(text = "You are Shadow AI, a mysterious and elegant dark assistant. In response to image requests, you output high quality generated imagery with descriptions.")))
        )

        try {
            Log.d("ChatRepository", "Sending request with prompt: '$prompt' aspectRatio: '$aspectRatio' to model '$model'")
            val response = RetrofitClient.service.generateContent(model, apiKey, request)
            Log.d("ChatRepository", "Received response from Gemini API")

            val candidate = response.candidates?.firstOrNull()
            val candidateParts = candidate?.content?.parts

            val imagePart = candidateParts?.firstOrNull {
                it.inlineData != null && it.inlineData.mimeType.startsWith("image/")
            }
            val textPart = candidateParts?.firstOrNull { it.text != null }

            if (imagePart != null && imagePart.inlineData != null) {
                val base64Data = imagePart.inlineData.data
                val mimeType = imagePart.inlineData.mimeType
                val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)

                // Save bytes to local file private directory
                val extension = if (mimeType.contains("png")) "png" else "jpg"
                val fileName = "shadow_gen_${System.currentTimeMillis()}.$extension"
                val file = File(context.filesDir, fileName)
                FileOutputStream(file).use { out ->
                    out.write(imageBytes)
                }

                val localFilePath = file.absolutePath
                val aiDescription = textPart?.text ?: "Behold, your creation. Rendered in highest contrast."

                val aiMsg = ChatMessage(
                    sessionId = sessionId,
                    sender = "ai",
                    text = aiDescription,
                    imagePath = localFilePath,
                    aspectRatio = aspectRatio
                )
                chatDao.insertMessage(aiMsg)
                Log.d("ChatRepository", "Saved generated image to: $localFilePath")
            } else {
                // If the model did not generate an image part, display text response
                val aiText = textPart?.text ?: "Shadow AI was unable to render the requested canvas. Select a different descriptive prompt."
                val aiMsg = ChatMessage(
                    sessionId = sessionId,
                    sender = "ai",
                    text = aiText
                )
                chatDao.insertMessage(aiMsg)
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error in generateImage", e)
            val errorMessage = ChatMessage(
                sessionId = sessionId,
                sender = "ai",
                text = "Shadow AI: Render engine encountered an anomaly. Error message: ${e.localizedMessage ?: e.message ?: "Unknown Exception"}. Please check Internet connectivity or API billing quota."
            )
            chatDao.insertMessage(errorMessage)
        }
    }
}
