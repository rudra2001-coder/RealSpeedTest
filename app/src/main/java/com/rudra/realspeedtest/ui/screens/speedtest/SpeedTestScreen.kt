package com.rudra.realspeedtest.ui.screens.speedtest

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rudra.realspeedtest.data.model.*
import com.rudra.realspeedtest.ui.components.*
import com.rudra.realspeedtest.ui.theme.*

@Composable
fun SpeedTestScreen(
    viewModel: SpeedTestViewModel,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val testProgress by viewModel.testProgress.collectAsState()
    val isRunning by viewModel.isTestRunning.collectAsState()
    val currentResult by viewModel.currentResult.collectAsState()
    val testMode by viewModel.testMode.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
        ) {
            // Top Header Info
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Speed, null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Speed Test",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                "Tap to start a new test",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { viewModel.toggleDarkMode() },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                                .size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.DarkMode, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                                .size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Settings, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Main Speed Gauge Card
            item {
                ModernCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SpeedGauge(
                            speed = if (isRunning) testProgress.currentSpeedMbps else (currentResult?.downloadSpeedMbps ?: 0.0),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                title = "Download",
                                value = "%.1f".format(if (isRunning) testProgress.overallSpeedMbps else (currentResult?.downloadSpeedMbps ?: 0.0)),
                                unit = "Mbps",
                                icon = Icons.Default.Download,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Upload",
                                value = "%.1f".format(currentResult?.uploadSpeedMbps ?: 0.0),
                                unit = "Mbps",
                                icon = Icons.Default.Upload,
                                color = ExcellentColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Test depth
            if (!isRunning) {
                item {
                    SectionHeader("Test depth")
                    TestModeSelector(
                        selectedMode = testMode
                    ) { viewModel.setTestMode(it) }
                }
            }

            // Run button / In Progress
            item {
                if (isRunning) {
                    TestInProgressCard(progress = testProgress)
                } else {
                    Button(
                        onClick = { viewModel.startTest() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (currentResult == null) "Start speed test" else "Run new test",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // Results Section
            currentResult?.let { result ->
                // Network fairness
                item {
                    SectionHeader("Network fairness")
                    FairnessScoreCard(
                        score = result.ispScore,
                        isThrottled = result.isThrottled,
                        speedVariation = result.speedVariationPercent
                    )
                }

                // Connection quality
                item {
                    SectionHeader("Connection quality")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DetailedStatCard(
                            title = "Latency",
                            value = result.latencyMs.toInt().toString(),
                            unit = "ms",
                            icon = Icons.Default.NetworkCheck,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                        DetailedStatCard(
                            title = "Jitter",
                            value = "%.1f".format(result.jitterMs),
                            unit = "ms",
                            icon = Icons.Default.Speed,
                            color = Orange700,
                            modifier = Modifier.weight(1f)
                        )
                        DetailedStatCard(
                            title = "Pkt loss",
                            value = "%.1f".format(result.packetLossPercent),
                            unit = "%",
                            icon = Icons.Default.ErrorOutline,
                            color = Red500,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // CDN performance
                if (result.cdnResults.isNotEmpty()) {
                    item {
                        SectionHeader("CDN performance")
                        ModernCard {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Multi-endpoint results",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            "${result.cdnResults.size}/${result.cdnResults.size} done",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                                result.cdnResults.forEach { cdn ->
                                    CDNPerformanceItem(cdn)
                                    if (cdn != result.cdnResults.last()) {
                                        Spacer(Modifier.height(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Real-world experience
                result.realWorldScore?.let { rw ->
                    item {
                        SectionHeader("Real-world experience")
                        RealWorldScoreCard(realWorldScore = rw, stabilityGrade = result.stabilityGrade)
                    }
                }

                // Network information
                item {
                    SectionHeader("Network information")
                    NetworkInfoCard(
                        publicIP = result.networkInfo?.publicIP ?: "0.0.0.0",
                        ispName = result.networkInfo?.ispName ?: "Unknown",
                        connectionType = result.networkInfo?.connectionType?.name ?: "Unknown",
                        city = result.networkInfo?.city,
                        country = result.networkInfo?.country
                    )
                }

                // Share results
                item {
                    SectionHeader("Share results")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                            containerColor = ExcellentColor
                        )
                        ActionButton(
                            text = "Image",
                            icon = Icons.Default.Image,
                            onClick = { },
                            modifier = Modifier.weight(1f),
                            containerColor = Orange700
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TestModeSelector(selectedMode: TestMode, onModeSelected: (TestMode) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TestMode.entries.forEach { mode ->
            val isSelected = mode == selectedMode
            Surface(
                onClick = { onModeSelected(mode) },
                modifier = Modifier.weight(1f).height(52.dp),
                color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
                tonalElevation = if (isSelected) 0.dp else 1.dp
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        mode.label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "~${if (mode == TestMode.QUICK) "15" else if (mode == TestMode.NORMAL) "30" else "60"} sec",
                        fontSize = 9.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun TestInProgressCard(progress: TestProgress) {
    ModernCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    getPhaseText(progress.phase),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(16.dp))
            ProgressIndicator(progress = progress.progress, label = "Overall Progress")
            if (progress.currentCDN.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Testing: ${progress.currentCDN}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun CDNPerformanceItem(cdn: CDNEndpoint) {
    val maxSpeed = 100.0
    val progress = (cdn.downloadSpeedMbps / maxSpeed).toFloat().coerceIn(0f, 1f)

    Column {
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
                        .background(MaterialTheme.colorScheme.secondary)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    cdn.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                "%.1f Mbps".format(cdn.downloadSpeedMbps),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Spacer(Modifier.height(8.dp))
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
                    .fillMaxWidth(progress)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary)
                        ),
                        shape = RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}

@Composable
private fun FairnessScoreCard(
    score: Int,
    isThrottled: Boolean,
    speedVariation: Double
) {
    ModernCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        if (isThrottled) Icons.Default.GppBad else Icons.Default.Verified,
                        null,
                        tint = if (isThrottled) Red500 else ExcellentColor,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Fair & Neutral",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        if (isThrottled) "Throttling detected" else "No throttling detected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Normal
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        score.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (score >= 80) ExcellentColor else if (score >= 60) FairColor else Red500
                    )
                    Text(
                        "/ 100",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Speed stability",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "±%.1f%% stable".format(speedVariation),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ExcellentColor
                )
            }
        }
    }
}

@Composable
private fun RealWorldScoreCard(realWorldScore: RealWorldScore, stabilityGrade: StabilityGrade) {
    ModernCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Dashboard, null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Usage scores",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        stabilityGrade.label,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExperienceItem("Streaming", realWorldScore.streamingScore, realWorldScore.streamingLabel, Icons.Default.PlayCircle, modifier = Modifier.weight(1f))
                ExperienceItem("Gaming", realWorldScore.gamingScore, realWorldScore.gamingLabel, Icons.Default.Gamepad, modifier = Modifier.weight(1f))
                ExperienceItem("Browsing", realWorldScore.browsingScore, realWorldScore.browsingLabel, Icons.Default.Public, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ExperienceItem(label: String, score: Int, grade: String, icon: ImageVector, modifier: Modifier = Modifier) {
    val color = if (score >= 80) ExcellentColor else if (score >= 60) FairColor else Red500
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon, null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                score.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                grade,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

private fun getPhaseText(phase: TestPhase): String = when (phase) {
    TestPhase.IDLE -> "Ready"
    TestPhase.PING_TEST -> "Testing Latency..."
    TestPhase.DOWNLOAD_TEST -> "Testing Download..."
    TestPhase.UPLOAD_TEST -> "Testing Upload..."
    TestPhase.JITTER_TEST -> "Testing Stability..."
    TestPhase.COMPLETED -> "Test Complete!"
}
