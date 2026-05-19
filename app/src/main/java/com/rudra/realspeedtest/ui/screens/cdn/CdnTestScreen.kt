package com.rudra.realspeedtest.ui.screens.cdn

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rudra.realspeedtest.data.model.AggregatedCdnResult
import com.rudra.realspeedtest.data.model.CdnTestResult
import com.rudra.realspeedtest.ui.components.SectionHeader
import com.rudra.realspeedtest.ui.theme.*
import com.rudra.realspeedtest.data.model.CDNCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CdnTestScreen(
    viewModel: CdnTestViewModel,
    onBack: () -> Unit
) {
    val phase by viewModel.phase.collectAsState()
    val fileSizeMB by viewModel.fileSizeMB.collectAsState()
    val cdnResults by viewModel.cdnResults.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Dns, null, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("CDN Test", fontWeight = FontWeight.SemiBold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val p = phase) {
                is CdnTestPhase.Idle -> IdleContent(
                    fileSizeMB = fileSizeMB,
                    onFileSizeChange = { viewModel.setFileSize(it) },
                    onStart = { viewModel.startTest() }
                )
                is CdnTestPhase.Testing -> TestingContent(
                    currentCdn = p.currentCdn,
                    currentIndex = p.currentIndex,
                    totalCount = p.totalCount,
                    progress = p.progress,
                    currentSpeed = p.currentSpeed,
                    resultsSoFar = cdnResults,
                    onCancel = { viewModel.cancelTest() }
                )
                is CdnTestPhase.Completed -> CompletedContent(
                    result = p.result,
                    onRestart = { viewModel.reset() },
                    onBack = onBack
                )
                is CdnTestPhase.Error -> ErrorContent(
                    message = p.message,
                    onRetry = { viewModel.reset() }
                )
            }
        }
    }
}

@Composable
private fun IdleContent(
    fileSizeMB: Int,
    onFileSizeChange: (Int) -> Unit,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Dns, null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "CDN Performance Test",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Test all CDN endpoints one by one with a custom file size.\nGet per-CDN and aggregated download/upload/latency results.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Test File Size Per CDN",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "$fileSizeMB MB",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Slider(
                    value = fileSizeMB.toFloat(),
                    onValueChange = { onFileSizeChange((it + 0.5f).toInt()) },
                    valueRange = 1f..50f, steps = 48,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("1 MB", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("25 MB", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("50 MB", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = when {
                        fileSizeMB <= 2 -> "Quick test — ~3s per CDN"
                        fileSizeMB <= 10 -> "Balanced test — ~7s per CDN"
                        else -> "Thorough test — ~12s per CDN"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("Start CDN Test", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun TestingContent(
    currentCdn: String,
    currentIndex: Int,
    totalCount: Int,
    progress: Float,
    currentSpeed: Double,
    resultsSoFar: List<CdnTestResult>,
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(animation = tween(800, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "pulse"
    )

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 3.dp
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            "Testing CDN $currentIndex/$totalCount",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            currentCdn,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "${(progress * 100).toInt()}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (currentSpeed > 0) {
                    Text(
                        "${String.format("%.1f", currentSpeed)} Mbps",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ExcellentColor
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Completed (${resultsSoFar.size})",
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(resultsSoFar) { result -> CdnResultRow(result = result) }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Red500)
        ) {
            Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Cancel Test")
        }
    }
}

@Composable
private fun CompletedContent(
    result: AggregatedCdnResult,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        item(key = "galactic") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                            ),
                            RoundedCornerShape(24.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Dns, null,
                                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "CDN Test Results",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${result.testFileSizeMB}MB target · ${result.results.size} CDNs tested",
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )

                        Spacer(Modifier.height(20.dp))

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            val dlStr = if (result.totalDownloadMbps > 0) String.format("%.1f", result.totalDownloadMbps) else "N/A"
                            val ulStr = if (result.totalUploadMbps > 0) String.format("%.1f", result.totalUploadMbps) else "N/A"
                            val latStr = if (result.avgLatencyMs > 0) String.format("%.0f", result.avgLatencyMs) else "N/A"
                            GalacticStat(label = "Download", value = dlStr, unit = "Mbps")
                            GalacticStat(label = "Upload", value = ulStr, unit = "Mbps")
                            GalacticStat(label = "Latency", value = latStr, unit = "ms")
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            val dataLabel = if (result.totalBytesDownloaded >= 1_000_000) {
                                "${String.format("%.1f", result.totalBytesDownloaded / 1_000_000.0)} MB"
                            } else if (result.totalBytesDownloaded >= 1_000) {
                                "${result.totalBytesDownloaded / 1_000} KB"
                            } else if (result.totalBytesDownloaded > 0) {
                                "${result.totalBytesDownloaded} B"
                            } else "N/A"
                            GalacticStat(label = "Data", value = dataLabel, unit = "")
                            GalacticStat(label = "Duration", value = formatDuration(result.totalDurationMs), unit = "")
                        }
                    }
                }
            }
        }

        item(key = "breakdown_header") {
            SectionHeader(title = "Per-CDN Performance")
        }

        items(result.results, key = { it.cdnName }) { cdnResult ->
            CdnResultRow(result = cdnResult)
        }

        item(key = "actions") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onRestart,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Test Again", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onBack,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Home, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Done", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun GalacticStat(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            if (unit.isNotEmpty()) {
                Spacer(Modifier.width(2.dp))
                Text(
                    unit,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Warning, null, tint = Red500, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            "Test Failed",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Try Again")
        }
    }
}

@Composable
private fun CdnResultRow(result: CdnTestResult) {
    val catColor = when (result.category) {
        CDNCategory.UI_FRAMEWORK -> Green700
        CDNCategory.WEB_CORE -> Blue700
        CDNCategory.UTILITIES -> Orange700
        CDNCategory.DATA_LAYER -> Purple700
        CDNCategory.GAME_ENGINE -> Pink700
        CDNCategory.UNKNOWN -> Gray500
    }

    val hasDownload = result.downloadSpeedMbps > 0
    val hasLatency = result.latencyMs > 0
    val hasData = result.bytesDownloaded > 0

    val speedColor = when {
        result.downloadSpeedMbps >= 50 -> Green700
        result.downloadSpeedMbps >= 20 -> Blue700
        result.downloadSpeedMbps >= 5 -> Orange700
        result.downloadSpeedMbps > 0 -> Red500
        else -> Gray400
    }

    val latencyColor = when {
        result.latencyMs in 1.0..30.0 -> Green700
        result.latencyMs in 30.0..80.0 -> Orange700
        result.latencyMs in 80.0..200.0 -> Red500
        result.latencyMs > 0 -> Purple700
        else -> Gray400
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(catColor))
                Spacer(Modifier.width(8.dp))
                Text(
                    result.cdnName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = catColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        result.category.label,
                        fontSize = 10.sp,
                        color = catColor,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth()) {
                val downloadStr = if (hasDownload) formatSpeed(result.downloadSpeedMbps) else "N/A"
                val latencyStr = if (hasLatency) "${String.format("%.0f", result.latencyMs)} ms" else "N/A"
                val dataStr = if (hasData) formatBytes(result.bytesDownloaded) else "N/A"
                val timeStr = formatDuration(result.durationMs)

                CdnStatItem(label = "Download", value = downloadStr, color = speedColor, modifier = Modifier.weight(1f))
                CdnStatItem(label = "Latency", value = latencyStr, color = latencyColor, modifier = Modifier.weight(1f))
                CdnStatItem(label = "Data", value = dataStr, color = Purple700, modifier = Modifier.weight(1f))
                CdnStatItem(label = "Time", value = timeStr, color = Gray600, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CdnStatItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

private fun formatSpeed(speed: Double): String = when {
    speed >= 1000 -> String.format("%.0f", speed)
    speed >= 100 -> String.format("%.1f", speed)
    speed >= 10 -> String.format("%.1f", speed)
    speed >= 1 -> String.format("%.2f", speed)
    speed > 0 -> String.format("%.2f", speed)
    else -> "0.00"
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> String.format("%.1f GB", bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> String.format("%.1f MB", bytes / 1_000_000.0)
    bytes >= 1_000 -> String.format("%.1f KB", bytes / 1_000.0)
    else -> "$bytes B"
}

private fun formatDuration(ms: Long): String = when {
    ms >= 60_000 -> String.format("%.1f min", ms / 60_000.0)
    ms >= 1_000 -> String.format("%.1f s", ms / 1_000.0)
    else -> "${ms}ms"
}
