# 🧪 Device Feature Lab

**Device Feature Lab** is a state-of-the-art Android prototype built using **Kotlin, Jetpack Compose, and Material Design 3 (M3)**. It integrates a server-side Gemini AI Assistant (`gemini-3.5-flash`) with physical and virtual device features. 

The application establishes a real-time, bi-directional loop: the AI assistant can proactively interact with local hardware, trigger device actuators, query web services, and analyze raw physical sensor data through standard **Gemini Function Calling (Tool Use)**.

---

## 🚀 Key Capabilities & Features

### 1. 🌐 Geolocation & Free Weather Radar Map
* **Direct Web Service**: Connects to the **Open-Meteo Geocoding and Weather Forecast REST APIs** with **zero API keys** required.
* **Live Interactive Map**: Embeds a fully zoomable OpenStreetMap view utilizing **Leaflet.js** inside a Compose `WebView`. When the AI assistant updates or resolves city weather, the map dynamically re-pans to those exact latitude and longitude coordinates.

### 2. 🎛️ Live Telemetry Dashboard
* **Dynamic Rotating Compass**: A high-performance, custom-drawn `Canvas` compass that rotates in real-time by reading the device's physical magnetometer and accelerometer. Includes a manual simulation override dial to test rotations on non-sensor devices/emulators.
* **Pitch & Roll Level (Gyroscope)**: A 3D circular bubble level displaying physical device tilt and gravity coordinates in real-time, alongside live G-Force calculation.

### 3. 🔌 Hardware Actuators & Diagnostics
* **Vibrant Battery HUD**: Direct state binding to check real-time battery percentage and active charging bolt indicators via Android's `BatteryManager`.
* **Physical Flashlight Actuator**: Direct system toggles for the physical hardware camera LED torch via `CameraManager` (with safe virtual fallbacks).
* **Pulse Haptic Actuation**: Custom vibration pattern triggers for immediate physical tactile feedback upon system events.

### 4. 💬 AI Terminal & Function Calling
* **Bi-directional Tool Use**: Chatting with Gemini doesn't just return text. Gemini reads the device status, controls the torch, and pushes alerts autonomously using JSON-driven function schemas!
* **Quick Commands Console**: Clickable shortcut chips to immediately demonstrate AI-local feature orchestration (e.g. *"Check weather in Tokyo"*, *"Toggle flashlight"*, *"Alert me in 5s"*).

---

## 🛠️ Onboarding & Setup Instructions

To get the project compiled and running locally or in your Android development environment, follow these steps:

### Prerequisite System Declarations
1. **Operating System**: Linux, macOS, or Windows
2. **Android SDK**: API level 24 (Android 7.0) minimum, targeting API level 36 (Android 14+)
3. **Gradle**: Build System configured with Kotlin DSL (`build.gradle.kts`)

### Step 1: Configure Secret Management 🔐
This project utilizes the **Secrets Gradle Plugin** to read environmental keys from a local `.env` file and generate a type-safe `BuildConfig` class at compile time.

1. Locate the file `/.env.example` at the root directory.
2. Create a copy of this file named `/.env` in the root folder:
   ```properties
   # .env
   GEMINI_API_KEY=your_actual_gemini_api_key_here
   ```
3. **AI Studio Security**: When using Google AI Studio Build, configure your Gemini API Key in the **Secrets Panel**. The environment automatically injects this secret into the workspace `.env` file during construction.

> ⚠️ **Security Warning**: Android APKs can be easily decompiled, and secrets stored in strings or exposed via `BuildConfig` can be extracted. **Do not share the generated debug APK file publicly or with unauthorized individuals** to prevent potential misuse or quota theft of your Gemini credentials.

---

## 📡 API Integrations & Tool Use Schemas

The AI Assistant processes natural user queries and automatically delegates tasks to local device controllers. Here are the documented JSON tool schemas and REST endpoints:

### 1. Weather API Integration (Open-Meteo)
* **Geocoding Endpoint**: 
  `GET https://geocoding-api.open-meteo.com/v1/search?name={city_name}&count=1&language=en&format=json`
* **Forecast Endpoint**:
  `GET https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m`

### 2. Gemini Assistant Tool Schemas
Gemini receives a list of tools it can call at any time. When Gemini returns a response, the app intercepts the request, runs the device logic, and replies back to Gemini with the result data:

#### A. `get_weather`
* **Description**: Resolve city coordinates and current forecast conditions.
```json
{
  "name": "get_weather",
  "description": "Get the current weather in a given city or location by geocoding its coordinates.",
  "parameters": {
    "type": "OBJECT",
    "properties": {
      "location": { "type": "STRING", "description": "The city and/or country, e.g. Tokyo" }
    },
    "required": ["location"]
  }
}
```

#### B. `toggle_flashlight`
* **Description**: Control the physical LED camera torch.
```json
{
  "name": "toggle_flashlight",
  "description": "Turn the device physical flashlight on or off.",
  "parameters": {
    "type": "OBJECT",
    "properties": {
      "on": { "type": "BOOLEAN", "description": "Set true to turn flashlight on, false to turn off." }
    },
    "required": ["on"]
  }
}
```

#### C. `schedule_notification`
* **Description**: Deliver a push alert with custom text after a specified delay.
```json
{
  "name": "schedule_notification",
  "description": "Schedule a local push notification on the device with a specific message and a delay in seconds.",
  "parameters": {
    "type": "OBJECT",
    "properties": {
      "message": { "type": "STRING", "description": "Text description to display." },
      "delaySeconds": { "type": "INTEGER", "description": "Delay in seconds before alert displays." }
    },
    "required": ["message", "delaySeconds"]
  }
}
```

#### D. `get_device_status`
* **Description**: Read live physical battery diagnostics and sensory orientations.
```json
{
  "name": "get_device_status",
  "description": "Get the live physical device telemetry logs, including battery, compass orientation, and accelerometer movement.",
  "parameters": {
    "type": "OBJECT",
    "properties": {}
  }
}
```

---

## 📱 Developer Usage Examples

Here are concrete examples of how users and developers can interact with the Device Lab system:

### 🌟 Example A: Natural Language Weather Scanning
1. **User input**: *"What's the weather like in Paris?"*
2. **AI Action**: Triggers `get_weather(location="Paris")`.
3. **Device Action**: Resolves Paris to `(48.8566, 2.3522)`, calls Open-Meteo API, moves the Leaflet WebView Map to Paris, updates the Weather Status HUD with live temperature/humidity/wind speed, and feeds results back to the AI.
4. **AI Output**: *"It is currently overcast and 18°C in Paris, France, with a light wind of 11 km/h. I have adjusted your map coordinates to focus on Paris!"*

### 🌟 Example B: Hardware Command Execution
1. **User input**: *"Turn on my flashlight, it's too dark in here!"*
2. **AI Action**: Triggers `toggle_flashlight(on=true)`.
3. **Device Action**: Executes camera torch mode, triggers haptic vibration, and reports success back to the AI.
4. **AI Output**: *"Flashlight is now active! I've powered on the LED torch and sent a haptic confirmation pulse to your device."*

### 🌟 Example C: Delayed Push Alerts
1. **User input**: *"Set a break alert in 5 seconds."*
2. **AI Action**: Triggers `schedule_notification(message="Time to stretch!", delaySeconds=5)`.
3. **Device Action**: Registers delayed task. After 5 seconds, triggers a haptic buzz and fires a standard Android Push Notification (with dynamic visual screen-banner fallback if system permissions are blocked).
4. **AI Output**: *"Understood! Your stretch alert is scheduled and will trigger on your screen in exactly 5 seconds."*

---

## 🏗️ Architecture Stack Overview
* **UI Layer**: Built entirely in **Jetpack Compose** following M3 guidelines with Edge-to-Edge window insets and responsive canvas gauges.
* **ViewModel Layer**: `DeviceLabViewModel` coordinates UI state, active sensor listeners, hardware integrations, and triggers the AI function-calling loop.
* **Network Layer**: **Retrofit + OkHttp** with custom 60s timeouts and integrated JSON-to-Model serialization converters.
* **Hardware Layer**: Direct Android Framework interactions via `SensorManager`, `CameraManager`, `BatteryManager`, and `NotificationManager`.
