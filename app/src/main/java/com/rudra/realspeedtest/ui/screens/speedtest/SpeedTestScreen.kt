package com.rudra.realspeedtest.ui.screens.speedtest

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rudra.realspeedtest.data.model.*
import com.rudra.realspeedtest.ui.components.*
import com.rudra.realspeedtest.ui.theme.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedTestScreen(
    viewModel: SpeedTestViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val testProgress by viewModel.testProgress.collectAsState()
    val isRunning by viewModel.isTestRunning.collectAsState()
    val currentResult by viewModel.currentResult.collectAsState()
    val cdnResults by viewModel.currentResults.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Speed,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Real Speed Test", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleDarkMode() }) {
                        Icon(Icons.Default.DarkMode, "Dark Mode", tint = Color.White)
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, "Settings", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Green700,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Speed gauge (always visible)
            item {
                SpeedGauge(
                    speed = testProgress.overallSpeedMbps,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Start / Progress toggle
            item {
                AnimatedVisibility(
                    visible = !isRunning,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    StartTestButton(
                        onClick = { viewModel.startTest() },
                        hasPreviousResult = currentResult != null
                    )
                }
                AnimatedVisibility(
                    visible = isRunning,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    TestInProgressCard(
                        progress = testProgress,
                        currentSpeed = testProgress.currentSpeedMbps
                    )
                }
            }

            // Live CDN progress (while test is running)
            if (testProgress.phase != TestPhase.IDLE && isRunning && cdnResults.isNotEmpty()) {
                item {
                    LiveCDNProgressCard(cdnResults = cdnResults)
                }
            }

            // Completed test results
            currentResult?.let { result ->
                // Fairness Score Card
                item {
                    FairnessScoreCard(
                        score = result.ispScore,
                        label = result.qualityLabel.name,
                        isThrottled = result.isThrottled,
                        speedVariation = result.speedVariationPercent
                    )
                }

                // Overall speed stats
                item {
                    SpeedStatsRow(result)
                }

                // Speed comparison vs average
                item {
                    SpeedComparisonCard(
                        currentSpeed = result.downloadSpeedMbps,
                        averageSpeed = viewModel.calculateAverageSpeed()
                    )
                }

                // Network quality (latency, jitter, loss)
                item {
                    NetworkQualityRow(result)
                }

                // CDN Performance Breakdown
                if (result.cdnResults.isNotEmpty()) {
                    item {
                        Text(
                            text = "CDN Performance Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    // Horizontal scrollable cards for CDN results (compact view)
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 0.dp)
                        ) {
                            items(result.cdnResults) { cdn ->
                                CDNPerformanceCard(
                                    name = cdn.name,
                                    downloadSpeed = cdn.downloadSpeedMbps,
                                    uploadSpeed = cdn.uploadSpeedMbps, // needs model support
                                    latency = cdn.latencyMs
                                )
                            }
                        }
                    }

                    // Bandwidth distribution chart
                    if (result.cdnResults.isNotEmpty()) {
                        item {
                            BandwidthDistributionChart(
                                cdnResults = result.cdnResults
                                    .filter { it.status == TestStatus.DONE }
                                    .map { it.name to it.downloadSpeedMbps }
                            )
                        }
                    }
                }

                // Alerts for throttling / inconsistency
                if (result.isThrottled) {
                    item {
                        AlertBanner(
                            title = "ISP Throttling Detected",
                            message = "Your ISP may be limiting speed on ${result.throttledCDN}. " +
                                    "Multiple CDN tests show inconsistent speeds.",
                            icon = Icons.Default.Warning,
                            color = Orange500
                        )
                    }
                }
                if (result.inconsistentEndpoints.isNotEmpty()) {
                    item {
                        AlertBanner(
                            title = "Inconsistent Performance",
                            message = "Unstable connection on: ${result.inconsistentEndpoints.joinToString()}",
                            icon = Icons.Default.SignalWifiBad,
                            color = FairColor
                        )
                    }
                }

                // Network info
                result.networkInfo?.let { network ->
                    item {
                        NetworkInfoCard(
                            publicIP = network.publicIP,
                            ispName = network.ispName,
                            connectionType = network.connectionType.name
                        )
                    }
                }

                // Share / copy actions
                item {
                    ShareActionsRow(context = context, viewModel = viewModel)
                }
            }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettingsDialog = false }
        )
    }
}

// ---------- Start Button (unchanged) ----------
@Composable
private fun StartTestButton(onClick: () -> Unit, hasPreviousResult: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = Green700),
        shape = RoundedCornerShape(20.dp)
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                if (hasPreviousResult) "Run New Test" else "Start Speed Test",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ---------- In‑Progress Card ----------
@Composable
private fun TestInProgressCard(progress: TestProgress, currentSpeed: Double) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = CardLight),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = Green700,
                strokeWidth = 4.dp
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = getPhaseText(progress.phase),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            ProgressIndicator(progress = progress.progress, label = "Overall Progress")
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format("%.1f", currentSpeed),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Green700
                )
                Spacer(Modifier.width(4.dp))
                Text("Mbps", style = MaterialTheme.typography.bodyLarge, color = Gray500)
            }
            if (progress.currentCDN.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Testing: ${progress.currentCDN}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500
                )
            }
        }
    }
}

// ---------- Live CDN Progress (with animated bars) ----------
@Composable
private fun LiveCDNProgressCard(cdnResults: List<CDNEndpoint>) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CardLight),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Live CDN Results",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = Green700.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "${cdnResults.count { it.status == TestStatus.DONE }}/${cdnResults.size}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Green700
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            cdnResults.take(5).forEach { cdn ->
                LiveCDNItem(cdn)
                if (cdn != cdnResults.take(5).last()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Gray200)
                }
            }
        }
    }
}

@Composable
private fun LiveCDNItem(cdn: CDNEndpoint) {
    val maxSpeed = 100.0
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                cdn.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                when (cdn.status) {
                    TestStatus.RUNNING -> "Testing..."
                    TestStatus.DONE -> "${String.format("%.1f", cdn.downloadSpeedMbps)} Mbps"
                    else -> cdn.status.name.lowercase().replaceFirstChar { it.uppercase() }
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = Green700
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = {
                if (cdn.status == TestStatus.DONE) 1f
                else (cdn.downloadSpeedMbps / maxSpeed).toFloat().coerceIn(0f, 1f)
            },
            modifier = Modifier.fillMaxWidth(),
            color = Green700,
            trackColor = Gray200
        )
    }
}

// ---------- Fairness Score Card (NEW) ----------
@Composable
private fun FairnessScoreCard(
    score: Int,
    label: String,
    isThrottled: Boolean,
    speedVariation: Double = 0.0
) {
    val (icon, color) = when {
        isThrottled -> Icons.Default.GppBad to Red500
        score >= 80 -> Icons.Default.Verified to Green700
        score >= 60 -> Icons.Default.CheckCircle to Orange700
        else -> Icons.Default.Warning to Red500
    }

    val variationColor = when {
        speedVariation < 10 -> Green700
        speedVariation < 20 -> Orange700
        else -> Red500
    }

    Card(
        modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = CardLight),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Network Fairness",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isThrottled) "Traffic Shaping Detected" else "Fair & Neutral",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray600
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$score/100",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = color
                    )
                }
            }
            if (speedVariation > 0) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Gray200)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Speed Stability",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Gray600
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "±${String.format("%.1f", speedVariation)}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = variationColor
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = when {
                                speedVariation < 10 -> "Stable"
                                speedVariation < 20 -> "Unstable"
                                else -> "Very Unstable"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = variationColor
                        )
                    }
                }
            }
        }
    }
}

// ---------- CDN Performance Card (Horizontal Scroll) ----------
@Composable
private fun CDNPerformanceCard(
    name: String,
    downloadSpeed: Double,
    uploadSpeed: Double = 0.0,
    latency: Double
) {
    val maxSpeed = 100.0 // visual reference
    val dlProgress = (downloadSpeed / maxSpeed).toFloat().coerceIn(0f, 1f)
    val ulProgress = (uploadSpeed / maxSpeed).toFloat().coerceIn(0f, 1f)

    Card(
        modifier = Modifier.width(180.dp).shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CardLight),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))

            // Download
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Download, null, tint = Blue700, modifier = Modifier.size(16.dp))
                Text(
                    "${String.format("%.1f", downloadSpeed)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Blue700
                )
                Text("Mbps", style = MaterialTheme.typography.labelSmall, color = Gray500)
            }
            LinearProgressIndicator(
                progress = { dlProgress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = Blue700,
                trackColor = Gray200
            )
            Spacer(Modifier.height(8.dp))

            // Upload
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Upload, null, tint = Teal700, modifier = Modifier.size(16.dp))
                Text(
                    "${String.format("%.1f", uploadSpeed)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Teal700
                )
                Text("Mbps", style = MaterialTheme.typography.labelSmall, color = Gray500)
            }
            LinearProgressIndicator(
                progress = { ulProgress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = Teal700,
                trackColor = Gray200
            )
            Spacer(Modifier.height(8.dp))

            // Latency
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SignalCellularAlt, null, tint = Purple700, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    "${latency.toInt()} ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = Purple700
                )
            }
        }
    }
}

// ---------- Speed Stats Row ----------
@Composable
private fun SpeedStatsRow(result: SpeedTestResult) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            title = "Download",
            value = String.format("%.1f", result.downloadSpeedMbps),
            unit = "Mbps",
            icon = Icons.Default.Download,
            color = Blue700,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Upload",
            value = String.format("%.1f", result.uploadSpeedMbps),
            unit = "Mbps",
            icon = Icons.Default.Upload,
            color = Teal700,
            modifier = Modifier.weight(1f)
        )
    }
}

// ---------- Network Quality Row ----------
@Composable
private fun NetworkQualityRow(result: SpeedTestResult) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DetailedStatCard(
            title = "Latency",
            value = String.format("%.0f", result.latencyMs),
            unit = "ms",
            subtitle = "Ping response",
            icon = Icons.Default.SignalCellularAlt,
            color = Purple700,
            modifier = Modifier.weight(1f)
        )
        DetailedStatCard(
            title = "Jitter",
            value = String.format("%.1f", result.jitterMs),
            unit = "ms",
            subtitle = "Stability",
            icon = Icons.Default.Speed,
            color = Orange700,
            modifier = Modifier.weight(1f)
        )
        DetailedStatCard(
            title = "Packet Loss",
            value = String.format("%.1f", result.packetLossPercent),
            unit = "%",
            subtitle = "Reliability",
            icon = Icons.Default.Warning,
            color = Red700,
            modifier = Modifier.weight(1f)
        )
    }
}

// ---------- Share Actions ----------
@Composable
private fun ShareActionsRow(context: Context, viewModel: SpeedTestViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionButton(
            text = "Copy",
            icon = Icons.Default.ContentCopy,
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Speed Test", viewModel.exportAsText()))
            },
            modifier = Modifier.weight(1f),
            containerColor = Blue700
        )
        ActionButton(
            text = "Share",
            icon = Icons.Default.Share,
            onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, viewModel.exportAsText())
                }
                context.startActivity(Intent.createChooser(intent, "Share Results"))
            },
            modifier = Modifier.weight(1f),
            containerColor = Teal700
        )
        ActionButton(
            text = "Image",
            icon = Icons.Default.Image,
            onClick = {
                val result = viewModel.currentResult.value ?: return@ActionButton
                val htmlContent = generateShareImageHtml(result)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/html"
                    putExtra(Intent.EXTRA_TEXT, htmlContent)
                }
                context.startActivity(Intent.createChooser(intent, "Share as Image"))
            },
            modifier = Modifier.weight(1f),
            containerColor = Purple700
        )
    }
}

private fun generateShareImageHtml(result: SpeedTestResult): String {
    val date = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
        .format(java.util.Date(result.timestamp))

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; padding: 20px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 16px; }
                .card { background: white; border-radius: 16px; padding: 24px; box-shadow: 0 10px 40px rgba(0,0,0,0.2); }
                .title { font-size: 18px; font-weight: bold; color: #333; margin-bottom: 16px; }
                .speed { font-size: 48px; font-weight: bold; color: #2E7D32; text-align: center; }
                .unit { font-size: 16px; color: #666; }
                .stats { display: flex; justify-content: space-around; margin-top: 16px; }
                .stat { text-align: center; }
                .stat-value { font-size: 20px; font-weight: bold; color: #1976D2; }
                .stat-label { font-size: 12px; color: #666; }
                .footer { margin-top: 16px; text-align: center; font-size: 12px; color: #999; }
                .score { background: ${getScoreColor(result.ispScore)}; color: white; padding: 4px 12px; border-radius: 12px; font-size: 14px; }
            </style>
        </head>
        <body>
            <div class="card">
                <div class="title">Real Speed Test Results</div>
                <div class="speed">${String.format("%.1f", result.downloadSpeedMbps)} <span class="unit">Mbps</span></div>
                <div style="text-align: center; margin-top: 8px;">
                    <span class="score">ISP Score: ${result.ispScore}/100 (${result.qualityLabel.name})</span>
                </div>
                <div class="stats">
                    <div class="stat">
                        <div class="stat-value">${String.format("%.1f", result.uploadSpeedMbps)}</div>
                        <div class="stat-label">Upload Mbps</div>
                    </div>
                    <div class="stat">
                        <div class="stat-value">${String.format("%.0f", result.latencyMs)}</div>
                        <div class="stat-label">Latency ms</div>
                    </div>
                    <div class="stat">
                        <div class="stat-value">${String.format("%.1f", result.jitterMs)}</div>
                        <div class="stat-label">Jitter ms</div>
                    </div>
                </div>
                <div class="footer">Tested on $date</div>
            </div>
        </body>
        </html>
    """.trimIndent()
}

private fun getScoreColor(score: Int): String = when {
    score >= 80 -> "#22C55E"
    score >= 60 -> "#10B981"
    score >= 40 -> "#F59E0B"
    else -> "#EF4444"
}

// ---------- Helpers ----------
private fun getPhaseText(phase: TestPhase): String = when (phase) {
    TestPhase.IDLE -> "Ready"
    TestPhase.PING_TEST -> "Testing Latency..."
    TestPhase.DOWNLOAD_TEST -> "Testing Download Speed..."
    TestPhase.UPLOAD_TEST -> "Testing Upload Speed..."
    TestPhase.JITTER_TEST -> "Testing Connection Stability..."
    TestPhase.COMPLETED -> "Test Complete!"
}

// ---------- Settings Dialog (unchanged) ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(viewModel: SpeedTestViewModel, onDismiss: () -> Unit) {
    var autoTestEnabled by remember { mutableStateOf(viewModel.isAutoTestEnabled.value) }
    var intervalMinutes by remember { mutableStateOf(viewModel.autoTestIntervalMinutes.value.toFloat()) }
    var speedThreshold by remember { mutableStateOf(viewModel.speedThreshold.value.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Gray100),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Autorenew, null, tint = Green700)
                                Spacer(Modifier.width(8.dp))
                                Text("Auto Test", fontWeight = FontWeight.Medium)
                            }
                            Switch(
                                checked = autoTestEnabled,
                                onCheckedChange = {
                                    autoTestEnabled = it
                                    if (it) viewModel.scheduleAutoTest(intervalMinutes.toInt())
                                    else viewModel.cancelAutoTest()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Green700,
                                    checkedTrackColor = Green200
                                )
                            )
                        }
                        if (autoTestEnabled) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Interval: ${intervalMinutes.toInt()} minutes",
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray600
                            )
                            Slider(
                                value = intervalMinutes,
                                onValueChange = { intervalMinutes = it },
                                valueRange = 5f..120f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Green700,
                                    activeTrackColor = Green700
                                )
                            )
                        }
                    }
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Gray100),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, null, tint = Orange700)
                            Spacer(Modifier.width(8.dp))
                            Text("Speed Alert Threshold", fontWeight = FontWeight.Medium)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${speedThreshold.toInt()} Mbps",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Orange700
                        )
                        Slider(
                            value = speedThreshold,
                            onValueChange = { speedThreshold = it },
                            valueRange = 1f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = Orange700,
                                activeTrackColor = Orange700
                            )
                        )
                        Text(
                            "Get notified when speed drops below this threshold",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray500
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.setSpeedThreshold(speedThreshold.toDouble())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Green700)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Gray600) }
        },
        shape = RoundedCornerShape(20.dp)
    )
}