package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// --- GEMINI API MODELS ---

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<Content>,
    val tools: List<Tool>? = null,
    val systemInstruction: Content? = null,
    val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val role: String? = null, // "user", "model", or "function"
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val functionCall: FunctionCall? = null,
    val functionResponse: FunctionResponse? = null
)

@JsonClass(generateAdapter = true)
data class FunctionCall(
    val name: String,
    val args: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class FunctionResponse(
    val name: String,
    val response: Map<String, Any>
)

@JsonClass(generateAdapter = true)
data class Tool(
    val functionDeclarations: List<FunctionDeclaration>
)

@JsonClass(generateAdapter = true)
data class FunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: ParameterSchema
)

@JsonClass(generateAdapter = true)
data class ParameterSchema(
    val type: String, // "OBJECT"
    val properties: Map<String, PropertySchema>,
    val required: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class PropertySchema(
    val type: String, // "STRING", "NUMBER", "INTEGER", "BOOLEAN"
    val description: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val maxOutputTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content,
    val finishReason: String? = null
)

// --- OPEN-METEO WEATHER MODELS ---

@JsonClass(generateAdapter = true)
data class GeocodingResponse(
    val results: List<GeocodingResult>? = null
)

@JsonClass(generateAdapter = true)
data class GeocodingResult(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    @Json(name = "country") val country: String? = null,
    @Json(name = "admin1") val admin1: String? = null
)

@JsonClass(generateAdapter = true)
data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    val current: CurrentWeather? = null
)

@JsonClass(generateAdapter = true)
data class CurrentWeather(
    val time: String,
    @Json(name = "temperature_2m") val temperature: Double,
    @Json(name = "relative_humidity_2m") val humidity: Double,
    @Json(name = "weather_code") val weatherCode: Int,
    @Json(name = "wind_speed_10m") val windSpeed: Double
)
