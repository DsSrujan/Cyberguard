package com.example.aifraudguard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AIAssistantFragment : Fragment() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageView
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ai_assistant, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatRecyclerView = view.findViewById(R.id.chatRecyclerView)
        messageInput = view.findViewById(R.id.messageInput)
        sendButton = view.findViewById(R.id.sendButton)

        setupChat()
        addWelcomeMessage()
    }

    private fun setupChat() {
        chatAdapter = ChatAdapter(messages)
        chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }

        sendButton.setOnClickListener {
            val message = messageInput.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessage(message)
                messageInput.text.clear()
            }
        }
    }

    private fun addWelcomeMessage() {
        val welcomeMessage = ChatMessage(
            text = "Hello! I'm FraudGuard Assist. I can help you with:\n\n" +
                    "• Identifying scams and fraud\n" +
                    "• Understanding cybercrime tactics\n" +
                    "• Protecting yourself online\n" +
                    "• Reporting suspicious activities\n\n" +
                    "How can I help you today?",
            isUser = false
        )
        chatAdapter.addMessage(welcomeMessage)
    }

    private fun sendMessage(userMessage: String) {
        // Add user message
        val userChatMessage = ChatMessage(text = userMessage, isUser = true)
        chatAdapter.addMessage(userChatMessage)
        chatRecyclerView.scrollToPosition(messages.size - 1)

        // Check if API key is configured
        if (ApiConfig.GEMINI_API_KEY == "YOUR_GEMINI_API_KEY_HERE") {
            val errorMessage = ChatMessage(
                text = "API key not configured. Please add your Gemini API key to use this feature.",
                isUser = false
            )
            chatAdapter.addMessage(errorMessage)
            chatRecyclerView.scrollToPosition(messages.size - 1)
            return
        }

        // Get AI response using direct HTTP API
        lifecycleScope.launch {
            try {
                val aiResponse = getGeminiResponse(userMessage)
                val aiMessage = ChatMessage(text = aiResponse, isUser = false)
                chatAdapter.addMessage(aiMessage)
                chatRecyclerView.scrollToPosition(messages.size - 1)
            } catch (e: Exception) {
                Log.e("AIAssistant", "Error: ${e.message}", e)
                val errorMessage = ChatMessage(
                    text = "Sorry, I encountered an error. Please try again.",
                    isUser = false
                )
                chatAdapter.addMessage(errorMessage)
                chatRecyclerView.scrollToPosition(messages.size - 1)
            }
        }
    }

    private suspend fun getGeminiResponse(userMessage: String): String = withContext(Dispatchers.IO) {
        try {
            // Using v1beta endpoint with gemini-2.5-flash (specifically verified available for this key)
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${ApiConfig.GEMINI_API_KEY}"
            
            Log.d("AIAssistant", "Calling Gemini API...")
            
            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    // System Instruction as a first message part
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "You are FraudGuard Assist, a cybercrime prevention AI. Provide helpful, concise responses about fraud prevention and cybersecurity.")
                            })
                        })
                    })
                    // User Message
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", userMessage)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 2000)
                })
            }

            val requestBody = jsonBody.toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            Log.d("AIAssistant", "Gemini Response code: ${response.code}")
            Log.d("AIAssistant", "Gemini Response body: $responseBody")

            if (response.isSuccessful && responseBody != null) {
                val jsonResponse = JSONObject(responseBody)
                
                // Check if there are candidates
                if (jsonResponse.has("candidates")) {
                    val candidates = jsonResponse.getJSONArray("candidates")
                    if (candidates.length() > 0) {
                        val candidate = candidates.getJSONObject(0)
                        
                        // Check finish reason
                        val finishReason = candidate.optString("finishReason", "")
                        if (finishReason == "MAX_TOKENS") {
                            Log.w("AIAssistant", "Response truncated due to MAX_TOKENS")
                        }
                        
                        // Get content
                        if (candidate.has("content")) {
                            val content = candidate.getJSONObject("content")
                            if (content.has("parts")) {
                                val parts = content.getJSONArray("parts")
                                if (parts.length() > 0) {
                                    val aiText = parts.getJSONObject(0).getString("text")
                                    Log.d("AIAssistant", "Gemini Response: $aiText")
                                    return@withContext aiText
                                }
                            } else {
                                Log.e("AIAssistant", "No parts in content")
                                return@withContext "Sorry, the response was incomplete. Please try asking in a different way."
                            }
                        }
                    }
                }
                
                // If no candidates, check for error
                if (jsonResponse.has("error")) {
                    val error = jsonResponse.getJSONObject("error")
                    val errorMessage = error.optString("message", "Unknown error")
                    Log.e("AIAssistant", "Gemini API Error: $errorMessage")
                    return@withContext "API Error: $errorMessage"
                }
            } else {
                Log.e("AIAssistant", "Gemini HTTP Error: ${response.code} - $responseBody")
                return@withContext "Sorry, I encountered an error (${response.code}). Please try again."
            }
            
            return@withContext "I'm sorry, I couldn't generate a response. Please try again."
        } catch (e: Exception) {
            Log.e("AIAssistant", "Error calling Gemini API: ${e.message}", e)
            return@withContext "Error: ${e.message ?: "Unknown error occurred"}"
        }
    }
}