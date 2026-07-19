package com.example.hardware

import com.example.data.*
import android.util.Log

class GeminiAssistant {

    fun getTools(): List<Tool> {
        val weatherDecl = FunctionDeclaration(
            name = "get_weather",
            description = "Get the current weather in a given city or location by geocoding its coordinates.",
            parameters = ParameterSchema(
                type = "OBJECT",
                properties = mapOf(
                    "location" to PropertySchema(
                        type = "STRING",
                        description = "The city and/or country, e.g. London, Tokyo, Paris"
                    )
                ),
                required = listOf("location")
            )
        )

        val flashlightDecl = FunctionDeclaration(
            name = "toggle_flashlight",
            description = "Turn the device physical flashlight on or off.",
            parameters = ParameterSchema(
                type = "OBJECT",
                properties = mapOf(
                    "on" to PropertySchema(
                        type = "BOOLEAN",
                        description = "Set to true to turn the flashlight on, or false to turn it off."
                    )
                ),
                required = listOf("on")
            )
        )

        val notificationDecl = FunctionDeclaration(
            name = "schedule_notification",
            description = "Schedule a local push notification on the device with a specific message and a delay in seconds.",
            parameters = ParameterSchema(
                type = "OBJECT",
                properties = mapOf(
                    "message" to PropertySchema(
                        type = "STRING",
                        description = "The text description or message to display in the push notification alert."
                    ),
                    "delaySeconds" to PropertySchema(
                        type = "INTEGER",
                        description = "The delay in seconds before the notification is pushed (e.g., 5, 10, 30)."
                    )
                ),
                required = listOf("message", "delaySeconds")
            )
        )

        val deviceStatusDecl = FunctionDeclaration(
            name = "get_device_status",
            description = "Get the live physical device telemetry logs, including battery, compass orientation, and accelerometer movement.",
            parameters = ParameterSchema(
                type = "OBJECT",
                properties = emptyMap()
            )
        )

        return listOf(Tool(functionDeclarations = listOf(weatherDecl, flashlightDecl, notificationDecl, deviceStatusDecl)))
    }

    fun getSystemInstruction(): Content {
        return Content(
            parts = listOf(
                Part(
                    text = """
                        You are a highly advanced, responsive AI Device Assistant for the 'Device Feature Lab' app. 
                        You can interact with physical and virtual hardware features via local functions.
                        
                        You have access to:
                        1. Weather API (get_weather): Always call this when a user asks about the weather anywhere.
                        2. Flashlight (toggle_flashlight): Controls the physical device torch.
                        3. Local Notifications (schedule_notification): Alerts the user on device.
                        4. Telemetry Status (get_device_status): Reads battery levels, haptics, compass, and tilt metrics.
                        
                        RULES:
                        - Be precise, engaging, and action-oriented.
                        - Proactively trigger appropriate function calls whenever user intent implies them (e.g. if user says "make it dark", turn off flashlight; if they say "alert me in 10s to take a break", schedule a notification).
                        - When calling a function, briefly state what action you are taking.
                    """.trimIndent()
                )
            )
        )
    }

    suspend fun sendChatRequest(apiKey: String, history: List<Content>): GeminiResponse? {
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e("GeminiAssistant", "Invalid API key provided.")
            return null
        }

        val request = GeminiRequest(
            contents = history,
            tools = getTools(),
            systemInstruction = getSystemInstruction(),
            generationConfig = GenerationConfig(
                temperature = 0.5f,
                maxOutputTokens = 1000
            )
        )

        return try {
            RetrofitClient.geminiService.generateContent(apiKey, request)
        } catch (e: Exception) {
            Log.e("GeminiAssistant", "Network error during content generation: ${e.message}")
            null
        }
    }
}
