package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.BorderStroke
import java.util.Locale
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hardware.SensorData
import kotlinx.coroutines.launch

// Color Scheme Constants for Bold Typography Theme
val CyberBg = Color(0xFFF7FBF1)          // Main Page Background
val CyberCard = Color(0xFFFFFFFF)        // Standard Card Background (White)
val CyberGreen = Color(0xFF386A20)       // Vibrant Primary Forest Green
val CyberBlue = Color(0xFFB8F397)        // Bright Lime Highlight Accent
val CyberOrange = Color(0xFFE2E9D8)      // Soft Muted Grayish Green
val CyberRed = Color(0xFFBA1A1A)         // Elegant warning red
val CyberText = Color(0xFF191D17)        // Core Dark Text Color
val CyberMuted = Color(0xFF5A6352)       // Soft Sage-Muted Gray-Green
val CyberBorder = Color(0xFFDDE5D5)      // Soft divider/border line
val CyberHighlightOrange = Color(0xFFB25E00) // Clear text highlight color for tool calls

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceLabScreen(
    viewModel: DeviceLabViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // API Key State (starts with local environment build config)
    var apiKeyInput by remember { mutableStateOf(com.example.BuildConfig.GEMINI_API_KEY) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }

    // State Collection
    val sensorData by viewModel.sensorState.collectAsState()
    
    // Permissions Handling
    val requiredPermissions = remember {
        mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toList()
    }

    var permissionsGranted by remember {
        mutableStateOf(
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
        viewModel.addActivityLog(
            if (permissionsGranted) "All device permissions granted."
            else "Some permissions denied. Fallbacking to virtual state."
        )
    }

    // Auto launch permissions request once
    LaunchedEffect(Unit) {
        if (!permissionsGranted) {
            launcher.launch(requiredPermissions.toTypedArray())
        }
    }

    Scaffold(
        containerColor = CyberBg,
        modifier = Modifier.statusBarsPadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CyberBg)
        ) {
            // Sleek Settings toggle button row at top right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { isSettingsOpen = !isSettingsOpen },
                    modifier = Modifier.background(CyberOrange, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = if (isSettingsOpen) Icons.Default.Close else Icons.Default.Settings,
                        contentDescription = "Config panel",
                        tint = CyberGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Beautiful Bold Header from the design specification HTML
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SYSTEM STATUS: ACTIVE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = CyberGreen
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(CyberGreen, CircleShape))
                        Box(modifier = Modifier.size(8.dp).background(CyberGreen.copy(alpha = 0.3f), CircleShape))
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Device\nFeature Lab",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 40.sp,
                    letterSpacing = (-1).sp,
                    color = CyberText
                )
            }

            // Collapsible Config Panel
            AnimatedVisibility(
                visible = isSettingsOpen,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .border(1.dp, CyberBorder, RoundedCornerShape(28.dp)),
                    colors = CardDefaults.cardColors(containerColor = CyberCard),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "LAB CREDENTIALS CONFIG",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = CyberGreen,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            "The Gemini Assistant requires a valid API key to connect. Edit below if needed.",
                            fontSize = 11.sp,
                            color = CyberText.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            label = { Text("Gemini API Key", color = CyberMuted) },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                color = CyberText
                            ),
                            singleLine = true,
                            visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                    Icon(
                                        imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Key visibility",
                                        tint = CyberMuted
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberGreen,
                                unfocusedBorderColor = CyberBorder,
                                focusedLabelColor = CyberGreen,
                                unfocusedLabelColor = CyberMuted
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("api_key_input"),
                            shape = RoundedCornerShape(16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    apiKeyInput = com.example.BuildConfig.GEMINI_API_KEY
                                    viewModel.addActivityLog("Reset API Key to local secure variables.")
                                }
                            ) {
                                Text("Reset to default", color = CyberMuted, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    viewModel.addActivityLog("Applied customized lab credentials.")
                                    isSettingsOpen = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberGreen),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Apply Settings", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Main Workspace Layout
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                // 1. Diagnostics and Sensors Cards (Grid Style via Row)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Card A: Realtime Compass
                        CompassCard(
                            sensorData = sensorData,
                            modifier = Modifier
                                .weight(1f)
                                .height(170.dp),
                            onManualRotate = { viewModel.simulateSensorTilt(15, 0f) }
                        )

                        // Card B: Tilt / Pitch Level
                        TiltCard(
                            sensorData = sensorData,
                            modifier = Modifier
                                .weight(1f)
                                .height(170.dp),
                            onManualTilt = { viewModel.simulateSensorTilt(0, 5f) }
                        )
                    }
                }

                // 2. Hardware Actuators
                item {
                    HardwareActuatorsCard(
                        batteryPercentage = viewModel.batteryPercentage,
                        isCharging = viewModel.isBatteryCharging,
                        isFlashOn = viewModel.isFlashlightOn,
                        onToggleFlash = { viewModel.toggleFlashlightDirect() },
                        onVibrate = { viewModel.triggerVibration() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }

                // 3. Geolocation & Weather Radar Map Card
                item {
                    GeolocMapCard(
                        coordinates = viewModel.mapCoordinates,
                        cityName = viewModel.selectedCityName,
                        weatherState = viewModel.weatherState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }

                // 4. Lab Activity Log Logs Terminal
                item {
                    ActivityTerminalCard(
                        logs = viewModel.recentActivities,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }

                // Blank bottom padding to ensure clean scrolling before the Assistant drawer
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // AI Assistant Panel (Anchored Bottom)
            AiAssistantPanel(
                messages = viewModel.chatMessages,
                isGenerating = viewModel.isGeneratingChat,
                onSendMessage = { query -> viewModel.sendChatMessage(query, apiKeyInput) },
                onClearChat = { viewModel.clearChat() }
            )
        }
    }
}

// --- TELEMETRY COMPOSE WIDGETS ---

@Composable
fun CompassCard(
    sensorData: SensorData,
    onManualRotate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CyberCard),
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.border(1.dp, CyberBorder, RoundedCornerShape(28.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "COMPASS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyberGreen
                )
                Box(
                    modifier = Modifier
                        .background(
                            if (sensorData.isPhysicalSensor) CyberGreen.copy(alpha = 0.2f) else CyberOrange.copy(alpha = 0.2f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        if (sensorData.isPhysicalSensor) "LIVE" else "MOCK",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (sensorData.isPhysicalSensor) CyberGreen else CyberOrange,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Drawing Rotating Dial
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(80.dp)
                    .clickable { onManualRotate() }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.width / 2

                    // Draw outer ring
                    drawCircle(
                        color = CyberMuted.copy(alpha = 0.4f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Draw Cardinal points
                    val cardinalRadius = radius - 8.dp.toPx()
                    // Rotate the dial based on heading
                    rotate(degrees = -sensorData.compassHeading.toFloat(), pivot = center) {
                        // Lines/Ticks
                        for (angle in 0 until 360 step 30) {
                            val angleRad = Math.toRadians(angle.toDouble())
                            val tickStart = Offset(
                                (center.x + (radius - 4.dp.toPx()) * kotlin.math.cos(angleRad)).toFloat(),
                                (center.y + (radius - 4.dp.toPx()) * kotlin.math.sin(angleRad)).toFloat()
                            )
                            val tickEnd = Offset(
                                (center.x + radius * kotlin.math.cos(angleRad)).toFloat(),
                                (center.y + radius * kotlin.math.sin(angleRad)).toFloat()
                            )
                            drawLine(
                                color = if (angle % 90 == 0) CyberGreen else CyberMuted.copy(alpha = 0.5f),
                                start = tickStart,
                                end = tickEnd,
                                strokeWidth = if (angle % 90 == 0) 2.dp.toPx() else 1.dp.toPx()
                            )
                        }

                        // Draw Needle pointer
                        val needlePath = Path().apply {
                            moveTo(center.x, center.y - radius + 10.dp.toPx()) // North tip
                            lineTo(center.x - 6.dp.toPx(), center.y)
                            lineTo(center.x + 6.dp.toPx(), center.y)
                            close()
                        }
                        drawPath(needlePath, color = CyberRed)

                        val needlePathSouth = Path().apply {
                            moveTo(center.x, center.y + radius - 10.dp.toPx()) // South tip
                            lineTo(center.x - 6.dp.toPx(), center.y)
                            lineTo(center.x + 6.dp.toPx(), center.y)
                            close()
                        }
                        drawPath(needlePathSouth, color = CyberMuted)
                    }
                }
                
                // Realtime digital reading
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${sensorData.compassHeading}°",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = CyberText,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        getHeadingDirection(sensorData.compassHeading),
                        fontSize = 9.sp,
                        color = CyberMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Text(
                "Click to turn simulated dial",
                fontSize = 8.sp,
                color = CyberMuted,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun TiltCard(
    sensorData: SensorData,
    onManualTilt: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CyberCard),
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.border(1.dp, CyberBorder, RoundedCornerShape(28.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "GYROSCOPE / TILT",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyberBlue
                )
                Text(
                    "${sensorData.totalGForce} G",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (sensorData.totalGForce > 1.2f) CyberOrange else CyberBlue,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .background(CyberBlue.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            // 3D Level Bubble Container
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(80.dp)
                    .clickable { onManualTilt() }
                    .background(CyberBg, CircleShape)
                    .border(1.dp, CyberMuted.copy(alpha = 0.3f), CircleShape)
            ) {
                // Crosshair drawing
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    drawLine(
                        color = CyberMuted.copy(alpha = 0.2f),
                        start = Offset(0f, center.y),
                        end = Offset(size.width, center.y)
                    )
                    drawLine(
                        color = CyberMuted.copy(alpha = 0.2f),
                        start = Offset(center.x, 0f),
                        end = Offset(center.x, size.height)
                    )
                    drawCircle(
                        color = CyberMuted.copy(alpha = 0.2f),
                        radius = size.width / 4,
                        center = center,
                        style = Stroke(1f)
                    )
                }

                // Bubble movement based on Pitch (tilt forward/backward) and Roll (tilt left/right)
                val maxOffset = 30f
                val bubbleX = (sensorData.roll / 90f) * maxOffset
                val bubbleY = (sensorData.pitch / 90f) * maxOffset

                Box(
                    modifier = Modifier
                        .offset(x = bubbleX.dp, y = bubbleY.dp)
                        .size(16.dp)
                        .background(
                            Brush.radialGradient(listOf(Color.White, CyberBlue)),
                            CircleShape
                        )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PITCH", fontSize = 8.sp, color = CyberMuted)
                    Text("${sensorData.pitch.toInt()}°", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberText, fontFamily = FontFamily.Monospace)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ROLL", fontSize = 8.sp, color = CyberMuted)
                    Text("${sensorData.roll.toInt()}°", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberText, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun HardwareActuatorsCard(
    batteryPercentage: Int,
    isCharging: Boolean,
    isFlashOn: Boolean,
    onToggleFlash: () -> Unit,
    onVibrate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CyberCard),
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.border(1.dp, CyberBorder, RoundedCornerShape(28.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "HARDWARE DIAGNOSTICS & CONTROLS",
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = CyberMuted,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Battery Gauge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1.1f)
                ) {
                    Icon(
                        imageVector = when {
                            isCharging -> Icons.Default.BatteryChargingFull
                            batteryPercentage < 20 -> Icons.Default.BatteryAlert
                            else -> Icons.Default.BatteryFull
                        },
                        contentDescription = "Battery",
                        tint = when {
                            isCharging -> CyberGreen
                            batteryPercentage < 20 -> CyberRed
                            else -> CyberGreen
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            "BATTERY STATUS",
                            fontSize = 8.sp,
                            color = CyberMuted,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "$batteryPercentage%",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberText,
                                fontFamily = FontFamily.Monospace
                            )
                            if (isCharging) {
                                Text(
                                    " (CHARGING)",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberGreen,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier
                        .height(35.dp)
                        .width(1.dp)
                        .background(CyberBorder)
                )

                // Flashlight Controller
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onToggleFlash() }
                        .padding(horizontal = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "FLASHLIGHT",
                            fontSize = 8.sp,
                            color = CyberMuted,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            if (isFlashOn) "ON" else "OFF",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFlashOn) CyberGreen else CyberText,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    IconButton(
                        onClick = onToggleFlash,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isFlashOn) CyberGreen.copy(alpha = 0.15f) else CyberMuted.copy(alpha = 0.1f),
                                CircleShape
                            )
                            .border(
                                1.dp,
                                if (isFlashOn) CyberGreen else CyberBorder,
                                CircleShape
                            )
                            .testTag("flashlight_toggle")
                    ) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Default.LightMode else Icons.Default.FlashlightOff,
                            contentDescription = "Flashlight",
                            tint = if (isFlashOn) CyberGreen else CyberMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Direct Feedback Activators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onVibrate,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberGreen),
                    border = BorderStroke(1.dp, CyberGreen),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pulse Haptic", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- INTERACTIVE OPENSTREETMAP WEBVIEW WIDGET ---

@Composable
fun GeolocMapCard(
    coordinates: Pair<Double, Double>,
    cityName: String,
    weatherState: WeatherUiState,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CyberCard),
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.border(1.dp, CyberBorder, RoundedCornerShape(28.dp))
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        tint = CyberGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "GEOLOCATION & WEATHER RADAR",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberText
                    )
                }
                Text(
                    cityName.uppercase(),
                    fontSize = 10.sp,
                    color = CyberGreen,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Embedded Live Leaflet WebView Map
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(CyberBg)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            webViewClient = WebViewClient()
                        }
                    },
                    update = { webView ->
                        val html = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                                <style>
                                    body, html, #map { margin: 0; padding: 0; height: 100%; width: 100%; background: #f7fbf1; }
                                    .leaflet-attribution-flag { display: none !important; }
                                    .leaflet-control-attribution { font-size: 8px !important; }
                                </style>
                            </head>
                            <body>
                                <div id="map"></div>
                                <script>
                                    var map = L.map('map', { zoomControl: false }).setView([${coordinates.first}, ${coordinates.second}], 12);
                                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                                        maxZoom: 19,
                                        attribution: '© OSM'
                                    }).addTo(map);
                                    L.marker([${coordinates.first}, ${coordinates.second}]).addTo(map);
                                </script>
                            </body>
                            </html>
                        """.trimIndent()
                        webView.loadDataWithBaseURL("https://openstreetmap.org", html, "text/html", "UTF-8", null)
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Floating Coordinates HUD
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .background(CyberBg.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        "LAT: ${String.format(Locale.US, "%.4f", coordinates.first)}",
                        fontSize = 8.sp,
                        color = CyberText,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "LON: ${String.format(Locale.US, "%.4f", coordinates.second)}",
                        fontSize = 8.sp,
                        color = CyberText,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Weather details footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberBg.copy(alpha = 0.4f))
                    .padding(12.dp)
            ) {
                when (val state = weatherState) {
                    is WeatherUiState.Idle -> {
                        Text(
                            "Ask the AI assistant about city weather to scan conditions.",
                            fontSize = 11.sp,
                            color = CyberMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    is WeatherUiState.Loading -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = CyberGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Querying Open-Meteo Satellite data...", fontSize = 11.sp, color = CyberText, fontFamily = FontFamily.Monospace)
                        }
                    }
                    is WeatherUiState.Success -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = getWeatherIcon(state.code),
                                        contentDescription = null,
                                        tint = CyberGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "${state.temp}°C",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = CyberText,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        getWeatherDescription(state.code),
                                        fontSize = 11.sp,
                                        color = CyberGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text("Satellite Scan for ${state.cityName}", fontSize = 9.sp, color = CyberMuted)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("HUMIDITY", fontSize = 8.sp, color = CyberMuted)
                                    Text("${state.humidity}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberText, fontFamily = FontFamily.Monospace)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("WIND", fontSize = 8.sp, color = CyberMuted)
                                    Text("${state.windSpeed} km/h", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberText, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                    is WeatherUiState.Error -> {
                        Text(
                            "Radar scan failed: ${state.message}",
                            fontSize = 11.sp,
                            color = CyberRed,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// --- ACTIVITY TERMINAL LOGGER WIDGET ---

@Composable
fun ActivityTerminalCard(
    logs: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CyberOrange.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.border(2.dp, CyberBlue, RoundedCornerShape(28.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Biotech Icon and Header Style from HTML Design Spec
            Icon(
                imageVector = Icons.Default.Biotech,
                contentDescription = null,
                tint = CyberGreen.copy(alpha = 0.5f),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "HARDWARE LOGS",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                fontFamily = FontFamily.Monospace,
                color = CyberGreen
            )
            Spacer(modifier = Modifier.height(10.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .border(0.5.dp, CyberBorder, RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                LazyColumn(reverseLayout = false) {
                    items(logs) { log ->
                        Text(
                            "> $log",
                            fontSize = 10.sp,
                            color = when {
                                log.contains("failed", ignoreCase = true) || log.contains("Error", ignoreCase = true) -> CyberRed
                                log.contains("tool call", ignoreCase = true) -> CyberHighlightOrange
                                else -> CyberGreen
                            },
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- BOTTOM ANCHORED AI TERMINAL INTERFACE ---

@Composable
fun AiAssistantPanel(
    messages: List<ChatUiMessage>,
    isGenerating: Boolean,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit
) {
    var queryText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-scroll chat to bottom when new messages arrive
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val quickCommands = listOf(
        "Scan local status",
        "Weather in Tokyo",
        "Toggle Flashlight",
        "Alert me in 5 seconds"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = CyberBorder,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = CyberCard),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
        ) {
            // Header Bar with Clean State Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SmartButton,
                        contentDescription = null,
                        tint = CyberGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "AI HARDWARE ASSISTANT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberText
                    )
                }
                
                IconButton(
                    onClick = onClearChat,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Clear Chat",
                        tint = CyberMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Message Board
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(CyberBg.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(messages) { msg ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            val isUser = msg.sender == "user"
                            val isTool = msg.sender == "tool"

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isUser) 16.dp else 4.dp,
                                                bottomEnd = if (isUser) 4.dp else 16.dp
                                            )
                                        )
                                        .background(
                                            when {
                                                isUser -> CyberBlue
                                                isTool -> CyberOrange
                                                else -> CyberCard
                                            }
                                        )
                                        .border(
                                            1.dp,
                                            when {
                                                isUser -> CyberGreen.copy(alpha = 0.5f)
                                                isTool -> CyberMuted.copy(alpha = 0.5f)
                                                else -> CyberBorder
                                            },
                                            RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isUser) 16.dp else 4.dp,
                                                bottomEnd = if (isUser) 4.dp else 16.dp
                                            )
                                        )
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                        .widthIn(max = 280.dp)
                                ) {
                                    Column {
                                        // Header
                                        Text(
                                            text = when {
                                                isUser -> "YOU"
                                                isTool -> "LOCAL HARDWARE CONTROLLER"
                                                else -> "ASSISTANT"
                                            }.uppercase(),
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                isUser -> CyberGreen
                                                isTool -> CyberHighlightOrange
                                                else -> CyberGreen
                                            },
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        Text(
                                            text = msg.content,
                                            fontSize = 11.sp,
                                            color = CyberText,
                                            fontFamily = if (isTool) FontFamily.Monospace else FontFamily.Default
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isGenerating) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(CyberCard, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(10.dp),
                                            strokeWidth = 1.dp,
                                            color = CyberGreen
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Thinking...",
                                            fontSize = 10.sp,
                                            color = CyberMuted,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick command chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickCommands) { cmd ->
                    Box(
                        modifier = Modifier
                            .background(CyberOrange, RoundedCornerShape(16.dp))
                            .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
                            .clickable {
                                onSendMessage(cmd)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            cmd,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Input TextField Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    placeholder = { Text("Command assistant...", color = CyberMuted, fontSize = 12.sp) },
                    textStyle = androidx.compose.ui.text.TextStyle(color = CyberText, fontSize = 12.sp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (queryText.isNotBlank()) {
                            onSendMessage(queryText)
                            queryText = ""
                        }
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberGreen,
                        unfocusedBorderColor = CyberBorder,
                        cursorColor = CyberGreen
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input"),
                    shape = RoundedCornerShape(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                FloatingActionButton(
                    onClick = {
                        if (queryText.isNotBlank()) {
                            onSendMessage(queryText)
                            queryText = ""
                        }
                    },
                    containerColor = CyberGreen,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("send_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Elegant M3 Bottom Navigation Bar matching the HTML Theme
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = CyberBorder, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Item 1: Lab (Active)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { /* Active screen */ }
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "Lab Screen",
                        tint = CyberGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "LAB",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = CyberGreen
                    )
                }

                // Item 2: Keys
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onSendMessage("Check my environment configuration") }
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "Keys Config",
                        tint = CyberText.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "KEYS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = CyberText.copy(alpha = 0.4f)
                    )
                }

                // Item 3: Config
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onSendMessage("Toggle Flashlight") }
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Diagnostics Settings",
                        tint = CyberText.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "CONFIG",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = CyberText.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

// --- HELPER CONVERSIONS ---

fun getHeadingDirection(heading: Int): String {
    return when (heading) {
        in 338..360, in 0..22 -> "NORTH"
        in 23..67 -> "N-EAST"
        in 68..112 -> "EAST"
        in 113..157 -> "S-EAST"
        in 158..202 -> "SOUTH"
        in 203..247 -> "S-WEST"
        in 248..292 -> "WEST"
        else -> "N-WEST"
    }
}

fun getWeatherIcon(code: Int): androidx.compose.ui.graphics.vector.ImageVector {
    return when (code) {
        0 -> Icons.Default.WbSunny              // Clear sky
        1, 2, 3 -> Icons.Default.CloudQueue     // Mainly clear, partly cloudy
        45, 48 -> Icons.Default.WaterDrop       // Fog and depositing rime fog
        51, 53, 55 -> Icons.Default.Grain       // Drizzle
        61, 63, 65 -> Icons.Default.Thunderstorm // Rain
        71, 73, 75 -> Icons.Default.AcUnit       // Snow fall
        80, 81, 82 -> Icons.Default.Water       // Rain showers
        else -> Icons.Default.WbCloudy
    }
}

fun getWeatherDescription(code: Int): String {
    return when (code) {
        0 -> "Clear Sky"
        1 -> "Mainly Clear"
        2 -> "Partly Cloudy"
        3 -> "Overcast"
        45 -> "Fog"
        48 -> "Depositing Rime Fog"
        51 -> "Light Drizzle"
        53 -> "Moderate Drizzle"
        55 -> "Dense Drizzle"
        61 -> "Slight Rain"
        63 -> "Moderate Rain"
        65 -> "Heavy Rain"
        71 -> "Slight Snow"
        73 -> "Moderate Snow"
        75 -> "Heavy Snow"
        80 -> "Slight Rain Showers"
        81 -> "Moderate Rain Showers"
        82 -> "Violent Rain Showers"
        95, 96, 99 -> "Thunderstorm"
        else -> "Stormy Clouds"
    }
}
