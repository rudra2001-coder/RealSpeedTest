package com.rudra.realspeedtest.ui.screens.speedtest

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rudra.realspeedtest.data.model.*
import com.rudra.realspeedtest.ui.components.*
import com.rudra.realspeedtest.ui.theme.*
import androidx.compose.runtime.collectAsState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedTestScreen(
    viewModel: SpeedTestViewModel,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val testProgress by viewModel.testProgress.collectAsState()
    val isRunning by viewModel.isTestRunning.collectAsState()
    val currentResult by viewModel.currentResult.collectAsState()
    val cdnResults by viewModel.currentResults.collectAsState()
    val testMode by viewModel.testMode.collectAsState()

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
                        Icon(Icons.Default.DarkMode, "Dark Mode", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, "Settings", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Speed gauge (always visible)
            item {
                SpeedGauge(
                    speed = testProgress.overallSpeedMbps,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Test mode selector (only when idle)
            if (!isRunning) {
                item {
                    TestModeSelector(
                        selectedMode = testMode,
                        onModeSelected = { viewModel.setTestMode(it) }
                    )
                }
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

                // Real-World Score
                result.realWorldScore?.let { rw ->
                    item {
                        RealWorldScoreCard(
                            realWorldScore = rw,
                            stabilityGrade = result.stabilityGrade
                        )
                    }
                }

                // CDN Performance Breakdown
                if (result.cdnResults.isNotEmpty()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Dns, null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.padding(6.dp).size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "CDN Performance Breakdown",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
                                    uploadSpeed = cdn.uploadSpeedMbps,
                                    latency = cdn.latencyMs,
                                    category = cdn.category.label
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
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                if (result.inconsistentEndpoints.isNotEmpty()) {
                    item {
                        AlertBanner(
                            title = "Inconsistent Performance",
                            message = "Unstable connection on: ${result.inconsistentEndpoints.joinToString()}",
                            icon = Icons.Default.SignalWifiBad,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                // Network info
                result.networkInfo?.let { network ->
                    item {
                        NetworkInfoCard(
                            publicIP = network.publicIP,
                            ispName = network.ispName,
                            connectionType = network.connectionType.name,
                            city = network.city,
                            country = network.country
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

}

// ---------- Start Button ----------
@Composable
private fun StartTestButton(onClick: () -> Unit, hasPreviousResult: Boolean) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            if (hasPreviousResult) "Run New Test" else "Start Speed Test",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

// ---------- In‑Progress Card with live animation ----------
@Composable
private fun TestInProgressCard(progress: TestProgress, currentSpeed: Double) {
    val infiniteTransition = rememberInfiniteTransition(label = "test_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_bg"
    )

    val animatedSpeed by animateFloatAsState(
        targetValue = currentSpeed.toFloat(),
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "live_speed"
    )

    ModernCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box {
            // Animated background pulse
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha),
                                Color.Transparent,
                                MaterialTheme.colorScheme.secondary.copy(alpha = pulseAlpha * 0.5f)
                            )
                        ),
                        RoundedCornerShape(20.dp)
                    )
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Phase icon ring
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp,
                        progress = { progress.progress }
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = getPhaseText(progress.phase),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                // Speed display
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = String.format("%.1f", animatedSpeed),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Mbps",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                // Progress bar
                LinearProgressIndicator(
                    progress = { progress.progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${(progress.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    if (progress.currentCDN.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = progress.currentCDN,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------- Live CDN Progress (with animated bars) ----------
@Composable
private fun LiveCDNProgressCard(cdnResults: List<CDNEndpoint>) {
    ModernCard(
        modifier = Modifier.fillMaxWidth(),
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
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "${cdnResults.count { it.status == TestStatus.DONE }}/${cdnResults.size}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            cdnResults.take(5).forEach { cdn ->
                LiveCDNItem(cdn)
                if (cdn != cdnResults.take(5).last()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun LiveCDNItem(cdn: CDNEndpoint) {
    val maxSpeed = 100.0
    val isDone = cdn.status == TestStatus.DONE
    val isRunning = cdn.status == TestStatus.RUNNING

    val animatedProgress by animateFloatAsState(
        targetValue = if (isDone) 1f else (cdn.downloadSpeedMbps / maxSpeed).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "cdn_progress"
    )

    val progressColor = when {
        isDone -> MaterialTheme.colorScheme.primary
        isRunning -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isRunning) MaterialTheme.colorScheme.secondary else if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    cdn.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                when (cdn.status) {
                    TestStatus.RUNNING -> "Testing..."
                    TestStatus.DONE -> "${String.format("%.1f", cdn.downloadSpeedMbps)} Mbps"
                    else -> cdn.status.name.lowercase().replaceFirstChar { it.uppercase() }
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = progressColor
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = if (isDone)
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                            else if (isRunning)
                                listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f))
                            else
                                listOf(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.outline)
                        ),
                        shape = RoundedCornerShape(3.dp)
                    )
            )
        }
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
        isThrottled -> Icons.Default.GppBad to MaterialTheme.colorScheme.error
        score >= 80 -> Icons.Default.Verified to MaterialTheme.colorScheme.primary
        score >= 60 -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.tertiary
        else -> Icons.Default.Warning to MaterialTheme.colorScheme.error
    }

    val variationColor = when {
        speedVariation < 10 -> MaterialTheme.colorScheme.primary
        speedVariation < 20 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    ModernCard(
        modifier = Modifier.fillMaxWidth(),
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Speed Stability",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
    latency: Double,
    category: String = ""
) {
    val maxSpeed = 100.0 // visual reference
    val dlProgress = (downloadSpeed / maxSpeed).toFloat().coerceIn(0f, 1f)
    val ulProgress = (uploadSpeed / maxSpeed).toFloat().coerceIn(0f, 1f)

    ModernCard(
        modifier = Modifier.widthIn(min = 160.dp, max = 200.dp),
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
            if (category.isNotEmpty()) {
                Text(
                    category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(12.dp))

            // Download
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Download, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                Text(
                    "${String.format("%.1f", downloadSpeed)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text("Mbps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinearProgressIndicator(
                progress = { dlProgress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(Modifier.height(8.dp))

            // Upload
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Upload, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                Text(
                    "${String.format("%.1f", uploadSpeed)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text("Mbps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinearProgressIndicator(
                progress = { ulProgress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(Modifier.height(8.dp))

            // Latency
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SignalCellularAlt, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    "${latency.toInt()} ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
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
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Upload",
            value = String.format("%.1f", result.uploadSpeedMbps),
            unit = "Mbps",
            icon = Icons.Default.Upload,
            color = MaterialTheme.colorScheme.tertiary,
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
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f)
        )
        DetailedStatCard(
            title = "Jitter",
            value = String.format("%.1f", result.jitterMs),
            unit = "ms",
            subtitle = "Stability",
            icon = Icons.Default.Speed,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f)
        )
        DetailedStatCard(
            title = "Packet Loss",
            value = String.format("%.1f", result.packetLossPercent),
            unit = "%",
            subtitle = "Reliability",
            icon = Icons.Default.Warning,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f)
        )
    }
}

// ---------- Share Actions ----------
@Composable
private fun ShareActionsRow(context: Context, viewModel: SpeedTestViewModel) {
    var showPreview by remember { mutableStateOf(false) }

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
            containerColor = MaterialTheme.colorScheme.secondary
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
            containerColor = MaterialTheme.colorScheme.tertiary
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
            containerColor = MaterialTheme.colorScheme.tertiary
        )
        ActionButton(
            text = "Preview",
            icon = Icons.Default.Visibility,
            onClick = { showPreview = true },
            modifier = Modifier.weight(1f),
            containerColor = MaterialTheme.colorScheme.primary
        )
    }

    viewModel.currentResult.collectAsState().value?.let { result ->
        if (showPreview) {
            PreviewResultDialog(
                result = result,
                onDismiss = { showPreview = false }
            )
        }
    }
}

@Composable
private fun PreviewResultDialog(
    result: SpeedTestResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Speed Test Results Preview")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Download / Upload
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Download",
                        value = String.format("%.1f", result.downloadSpeedMbps),
                        unit = "Mbps",
                        icon = Icons.Default.Download,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Upload",
                        value = String.format("%.1f", result.uploadSpeedMbps),
                        unit = "Mbps",
                        icon = Icons.Default.Upload,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Network quality
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailedStatCard(
                        title = "Latency",
                        value = String.format("%.0f", result.latencyMs),
                        unit = "ms",
                        subtitle = "Ping",
                        icon = Icons.Default.SignalCellularAlt,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                    DetailedStatCard(
                        title = "Jitter",
                        value = String.format("%.1f", result.jitterMs),
                        unit = "ms",
                        subtitle = "Stability",
                        icon = Icons.Default.Speed,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                }

                // ISP score
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Verified, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "ISP Score: ${result.ispScore}/100 (${result.qualityLabel.name})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
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

// ---------- Test Mode Selector ----------
@Composable
private fun TestModeSelector(
    selectedMode: TestMode,
    onModeSelected: (TestMode) -> Unit
) {
    val transition = rememberInfiniteTransition(label = "mode_pulse")
    val pulse by transition.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "pulse"
    )

    ModernCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Test Depth",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TestMode.entries.forEach { mode ->
                    val isSelected = mode == selectedMode
                    val color = when (mode) {
                        TestMode.QUICK -> MaterialTheme.colorScheme.primary
                        TestMode.NORMAL -> MaterialTheme.colorScheme.secondary
                        TestMode.THOROUGH -> MaterialTheme.colorScheme.tertiary
                    }
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0.92f,
                        animationSpec = tween(250),
                        label = "chip_scale"
                    )

                    FilterChip(
                        selected = isSelected,
                        onClick = { onModeSelected(mode) },
                        label = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
                            ) {
                                Text(
                                    mode.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    mode.accuracyLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color.copy(alpha = 0.12f),
                            selectedLabelColor = color,
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) color.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline,
                            selectedBorderColor = color,
                            enabled = true,
                            selected = isSelected
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }
    }
}

// ---------- Real-World Score Card ----------
@Composable
private fun RealWorldScoreCard(
    realWorldScore: RealWorldScore,
    stabilityGrade: StabilityGrade
) {
    ModernCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Dashboard, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "Real-World Experience",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${stabilityGrade.emoji} ${stabilityGrade.label}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = when (stabilityGrade) {
                        StabilityGrade.ROCK_SOLID, StabilityGrade.STABLE -> MaterialTheme.colorScheme.primary
                        StabilityGrade.MODERATE -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExperienceStat(
                    label = "🎬 Streaming",
                    score = realWorldScore.streamingScore,
                    grade = realWorldScore.streamingLabel,
                    modifier = Modifier.weight(1f)
                )
                ExperienceStat(
                    label = "🎮 Gaming",
                    score = realWorldScore.gamingScore,
                    grade = realWorldScore.gamingLabel,
                    modifier = Modifier.weight(1f)
                )
                ExperienceStat(
                    label = "🌐 Browsing",
                    score = realWorldScore.browsingScore,
                    grade = realWorldScore.browsingLabel,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ExperienceStat(
    label: String,
    score: Int,
    grade: String,
    modifier: Modifier = Modifier
) {
    val scoreColor = when {
        score >= 80 -> ExcellentColor
        score >= 60 -> GoodColor
        score >= 40 -> FairColor
        else -> BadColor
    }
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(
                "$score",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = scoreColor
            )
            Spacer(Modifier.height(2.dp))
            Text(
                grade,
                style = MaterialTheme.typography.labelSmall,
                color = scoreColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
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

