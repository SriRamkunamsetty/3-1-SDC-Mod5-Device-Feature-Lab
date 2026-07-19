package com.example.ui

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.hardware.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatUiMessage(
    val sender: String,      // "user", "assistant", "system", "tool"
    val content: String,
    val toolName: String? = null,
    val isPending: Boolean = false
)

sealed interface WeatherUiState {
    object Idle : WeatherUiState
    object Loading : WeatherUiState
    data class Success(val cityName: String, val temp: Double, val humidity: Double, val windSpeed: Double, val code: Int) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

class DeviceLabViewModel(application: Application) : AndroidViewModel(application) {

    // Helper controllers
    private val sensorManager = DeviceSensorManager(application)
    private val hardwareController = HardwareController(application)
    private val notificationHelper = PushNotificationHelper(application)
    private val geminiAssistant = GeminiAssistant()

    // Exposed Sensor Flow
    val sensorState: StateFlow<SensorData> = sensorManager.sensorData
        .stateIn(viewModelScope, SharingStarted.Lazily, SensorData())

    // UI States
    var chatMessages by mutableStateOf<List<ChatUiMessage>>(listOf(
        ChatUiMessage("assistant", "Hello! I am your AI Device Controller. You can chat with me, or ask me to do things like check weather in Tokyo, toggle your flashlight, or schedule notifications!")
    ))
        private set

    var weatherState by mutableStateOf<WeatherUiState>(WeatherUiState.Idle)
        private set

    var isFlashlightOn by mutableStateOf(false)
        private set

    var batteryPercentage by mutableStateOf(hardwareController.getBatteryPercentage())
        private set

    var isBatteryCharging by mutableStateOf(hardwareController.isBatteryCharging())
        private set

    var mapCoordinates by mutableStateOf(Pair(48.8566, 2.3522)) // Default Paris
        private set

    var selectedCityName by mutableStateOf("Paris, France")
        private set

    var isGeneratingChat by mutableStateOf(false)
        private set

    var recentActivities by mutableStateOf<List<String>>(listOf(
        "Lab initialized at Paris coordinates."
    ))
        private set

    // Raw Gemini History (keeps the official role-based parts for subsequent turns)
    private var geminiHistory = mutableListOf<Content>()

    init {
        sensorManager.startListening()
        // Periodically refresh battery status
        viewModelScope.launch {
            while (true) {
                batteryPercentage = hardwareController.getBatteryPercentage()
                isBatteryCharging = hardwareController.isBatteryCharging()
                delay(10000)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager.stopListening()
    }

    fun addActivityLog(log: String) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val time = sdf.format(Date())
        recentActivities = listOf("[$time] $log") + recentActivities.take(29)
    }

    fun simulateSensorTilt(headingDelta: Int, tiltDelta: Float) {
        sensorManager.simulateSensorChange(headingDelta, tiltDelta)
    }

    fun triggerVibration() {
        hardwareController.triggerVibration(150)
        addActivityLog("Haptic vibration feedback executed.")
    }

    fun toggleFlashlightDirect() {
        val target = !isFlashlightOn
        val success = hardwareController.toggleFlashlight(target)
        isFlashlightOn = target
        val type = if (success) "Physical" else "Virtual"
        addActivityLog("Flashlight toggled ${if (target) "ON" else "OFF"} ($type).")
        triggerVibration()
    }

    fun scheduleNotificationDirect(message: String, delaySeconds: Int) {
        viewModelScope.launch {
            addActivityLog("Notification scheduled: '$message' in $delaySeconds seconds.")
            delay(delaySeconds * 1000L)
            triggerVibration()
            val posted = notificationHelper.showNotification("Device Lab Alert", message)
            if (posted) {
                addActivityLog("Push notification delivered: '$message'")
            } else {
                addActivityLog("Fallback visual notification delivered: '$message'")
            }
        }
    }

    fun sendChatMessage(text: String, apiKey: String) {
        if (text.isBlank()) return

        // 1. Add user message to UI state
        chatMessages = chatMessages + ChatUiMessage("user", text)
        addActivityLog("User sent chat command: \"$text\"")

        // 2. Add to Gemini history
        val userContent = Content(role = "user", parts = listOf(Part(text = text)))
        geminiHistory.add(userContent)

        viewModelScope.launch {
            isGeneratingChat = true
            try {
                // 3. Make Gemini request
                val response = geminiAssistant.sendChatRequest(apiKey, geminiHistory)
                if (response != null && response.candidates != null && response.candidates.isNotEmpty()) {
                    val candidate = response.candidates[0]
                    val modelContent = candidate.content
                    geminiHistory.add(modelContent)

                    // 4. Inspect parts for text and/or functionCall
                    var textResponse = ""
                    var functionCallPart: FunctionCall? = null

                    for (part in modelContent.parts) {
                        if (part.text != null) {
                            textResponse += part.text
                        }
                        if (part.functionCall != null) {
                            functionCallPart = part.functionCall
                        }
                    }

                    if (textResponse.isNotEmpty()) {
                        chatMessages = chatMessages + ChatUiMessage("assistant", textResponse)
                    }

                    if (functionCallPart != null) {
                        // Handle function execution!
                        executeFunctionCall(apiKey, functionCallPart)
                    }
                } else {
                    chatMessages = chatMessages + ChatUiMessage("assistant", "No response received. Please check your Gemini API key in the configuration panel.")
                }
            } catch (e: Exception) {
                chatMessages = chatMessages + ChatUiMessage("assistant", "Error contacting assistant: ${e.message}")
            } finally {
                isGeneratingChat = false
            }
        }
    }

    private suspend fun executeFunctionCall(apiKey: String, call: FunctionCall) {
        val functionName = call.name
        val args = call.args ?: emptyMap()
        addActivityLog("Assistant requested tool call: $functionName")

        val resultData = mutableMapOf<String, Any>()

        when (functionName) {
            "get_weather" -> {
                val location = args["location"]?.toString() ?: "Paris"
                weatherState = WeatherUiState.Loading
                try {
                    addActivityLog("Searching location: '$location'...")
                    val geo = RetrofitClient.openMeteoService.geocodeCity(location)
                    val result = geo.results?.firstOrNull()
                    if (result != null) {
                        selectedCityName = "${result.name}, ${result.country ?: ""}"
                        mapCoordinates = Pair(result.latitude, result.longitude)
                        
                        addActivityLog("Coordinates found: (${result.latitude}, ${result.longitude}). Checking forecast...")
                        val weather = RetrofitClient.openMeteoService.getWeather(result.latitude, result.longitude)
                        val cur = weather.current
                        if (cur != null) {
                            weatherState = WeatherUiState.Success(
                                cityName = result.name,
                                temp = cur.temperature,
                                humidity = cur.humidity,
                                windSpeed = cur.windSpeed,
                                code = cur.weatherCode
                            )
                            resultData["success"] = true
                            resultData["location"] = result.name
                            resultData["temperature"] = cur.temperature
                            resultData["humidity"] = cur.humidity
                            resultData["wind_speed"] = cur.windSpeed
                            resultData["weather_code"] = cur.weatherCode
                            addActivityLog("Weather for $location: ${cur.temperature}°C, Code ${cur.weatherCode}")
                        } else {
                            weatherState = WeatherUiState.Error("No current weather data returned.")
                            resultData["success"] = false
                            resultData["error"] = "No current weather details."
                        }
                    } else {
                        weatherState = WeatherUiState.Error("City '$location' not found.")
                        resultData["success"] = false
                        resultData["error"] = "City geocoding failed."
                        addActivityLog("Geocoding failed for city '$location'")
                    }
                } catch (e: Exception) {
                    weatherState = WeatherUiState.Error("Error: ${e.message}")
                    resultData["success"] = false
                    resultData["error"] = e.message ?: "Unknown weather error"
                    addActivityLog("Weather request failed: ${e.message}")
                }
            }

            "toggle_flashlight" -> {
                val on = args["on"]?.toString()?.toBoolean() ?: false
                val success = hardwareController.toggleFlashlight(on)
                isFlashlightOn = on
                triggerVibration()
                resultData["success"] = true
                resultData["flashlight_state"] = if (on) "ON" else "OFF"
                resultData["physical_hardware_activated"] = success
                addActivityLog("Assistant toggled Flashlight ${if (on) "ON" else "OFF"}.")
            }

            "schedule_notification" -> {
                val message = args["message"]?.toString() ?: "Attention required!"
                val delaySec = (args["delaySeconds"] as? Number)?.toInt() ?: 5
                scheduleNotificationDirect(message, delaySec)
                resultData["success"] = true
                resultData["delay_seconds"] = delaySec
                resultData["message"] = message
                addActivityLog("Assistant scheduled notification: \"$message\" in ${delaySec}s.")
            }

            "get_device_status" -> {
                triggerVibration()
                val currentSensors = sensorState.value
                resultData["success"] = true
                resultData["battery_percentage"] = batteryPercentage
                resultData["is_charging"] = isBatteryCharging
                resultData["compass_heading_degrees"] = currentSensors.compassHeading
                resultData["pitch"] = currentSensors.pitch
                resultData["roll"] = currentSensors.roll
                resultData["total_g_force"] = currentSensors.totalGForce
                resultData["using_physical_sensors"] = currentSensors.isPhysicalSensor
                addActivityLog("Assistant checked device telemetry logs.")
            }

            else -> {
                resultData["success"] = false
                resultData["error"] = "Unknown function call"
            }
        }

        // 5. Send function execution results back to Gemini for final commentary
        val functionResponseContent = Content(
            role = "function",
            parts = listOf(
                Part(
                    functionResponse = FunctionResponse(
                        name = functionName,
                        response = resultData
                    )
                )
            )
        )
        geminiHistory.add(functionResponseContent)

        // Show tool execution status visually in chat list
        chatMessages = chatMessages + ChatUiMessage(
            sender = "tool",
            content = "Executed tool: $functionName(${args.entries.joinToString { "${it.key}=${it.value}" }}) -> Result: ${if (resultData["success"] == true) "Success" else "Failed"}",
            toolName = functionName
        )

        // Request final commentary from assistant
        isGeneratingChat = true
        try {
            val finalResponse = geminiAssistant.sendChatRequest(apiKey, geminiHistory)
            if (finalResponse != null && finalResponse.candidates != null && finalResponse.candidates.isNotEmpty()) {
                val candidate = finalResponse.candidates[0]
                geminiHistory.add(candidate.content)
                val text = candidate.content.parts.firstOrNull()?.text ?: ""
                if (text.isNotEmpty()) {
                    chatMessages = chatMessages + ChatUiMessage("assistant", text)
                }
            }
        } catch (e: Exception) {
            chatMessages = chatMessages + ChatUiMessage("assistant", "Executed function, but failed to fetch summary: ${e.message}")
        } finally {
            isGeneratingChat = false
        }
    }

    fun clearChat() {
        chatMessages = listOf(
            ChatUiMessage("assistant", "Chat history reset. How can I assist you with device features now?")
        )
        geminiHistory.clear()
        addActivityLog("Chat history cleared.")
        triggerVibration()
    }
}
