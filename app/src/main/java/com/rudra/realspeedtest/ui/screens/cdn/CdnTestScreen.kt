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
                        Icon(Icons.Default.Dns, null, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("CDN Test", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Indigo700,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(LightBackground)
        ) {
            when (phase) {
                is CdnTestPhase.Idle -> IdleContent(
                    fileSizeMB = fileSizeMB,
                    onFileSizeChange = { viewModel.setFileSize(it) },
                    onStart = { viewModel.startTest() }
                )
                is CdnTestPhase.Testing -> {
                    val testing = phase as CdnTestPhase.Testing
                    TestingContent(
                        currentCdn = testing.currentCdn,
                        progress = testing.progress,
                        currentSpeed = testing.currentSpeed,
                        resultsSoFar = cdnResults,
                        onCancel = { viewModel.cancelTest() }
                    )
                }
                is CdnTestPhase.Completed -> {
                    val result = (phase as CdnTestPhase.Completed).result
                    CompletedContent(
                        result = result,
                        onRestart = { viewModel.reset() },
                        onBack = onBack
                    )
                }
                is CdnTestPhase.Error -> {
                    val error = phase as CdnTestPhase.Error
                    ErrorContent(
                        message = error.message,
                        onRetry = { viewModel.reset() }
                    )
                }
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
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Indigo700.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Dns,
                        null,
                        tint = Indigo700,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "CDN Performance Test",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Test all CDN endpoints one by one with a custom file size.\nGet per-CDN and aggregated download/upload/latency results.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gray500,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Test File Size",
                    fontWeight = FontWeight.SemiBold,
                    color = Gray600,
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "$fileSizeMB MB",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Indigo700,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Slider(
                    value = fileSizeMB.toFloat(),
                        onValueChange = { onFileSizeChange((it + 0.5f).toInt()) },
                    valueRange = 1f..50f,
                    steps = 48,
                    colors = SliderDefaults.colors(
                        thumbColor = Indigo700,
                        activeTrackColor = Indigo700,
                        inactiveTrackColor = Gray200
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1 MB", fontSize = 12.sp, color = Gray500)
                    Text("25 MB", fontSize = 12.sp, color = Gray500)
                    Text("50 MB", fontSize = 12.sp, color = Gray500)
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Larger files give more accurate results but take longer to test.",
                    fontSize = 12.sp,
                    color = Gray400,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Indigo700)
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
    progress: Float,
    currentSpeed: Double,
    resultsSoFar: List<CdnTestResult>,
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Indigo700.copy(alpha = 0.08f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Indigo700.copy(alpha = pulseAlpha)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color.White,
                        strokeWidth = 3.dp
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Testing CDN...",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = currentCdn,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Indigo700
                )

                Spacer(Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Indigo700,
                    trackColor = Gray200
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Gray600
                )

                if (currentSpeed > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${String.format("%.1f", currentSpeed)} Mbps",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Green700
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Completed (${resultsSoFar.size})",
            fontWeight = FontWeight.SemiBold,
            color = Gray600,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(resultsSoFar) { result ->
                CdnResultRow(result = result)
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            text = "CDN Test Results",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Aggregated Results",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.DarkGray
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "File size: ${result.testFileSizeMB} MB | ${result.results.size} CDNs tested",
                    fontSize = 13.sp,
                    color = Gray500
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AggregatedStat(
                        label = "Total Download",
                        value = String.format("%.1f", result.totalDownloadMbps),
                        unit = "Mbps",
                        color = Green700
                    )
                    AggregatedStat(
                        label = "Upload",
                        value = String.format("%.1f", result.totalUploadMbps),
                        unit = "Mbps",
                        color = Blue700
                    )
                    AggregatedStat(
                        label = "Avg Latency",
                        value = String.format("%.0f", result.avgLatencyMs),
                        unit = "ms",
                        color = Orange700
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AggregatedStat(
                        label = "Data Downloaded",
                        value = formatBytes(result.totalBytesDownloaded),
                        unit = "",
                        color = Purple700
                    )
                    AggregatedStat(
                        label = "Total Time",
                        value = formatDuration(result.totalDurationMs),
                        unit = "",
                        color = Gray600
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Per-CDN Performance",
            fontWeight = FontWeight.SemiBold,
            color = Gray600,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(result.results) { cdnResult ->
                CdnResultRow(result = cdnResult)
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRestart,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Indigo700)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Test Again")
            }

            Button(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo700)
            ) {
                Icon(Icons.Default.Home, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Done")
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Warning,
            null,
            tint = Red500,
            modifier = Modifier.size(64.dp)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Test Failed",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Gray500,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Indigo700)
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

    val speedColor = when {
        result.downloadSpeedMbps >= 50 -> Green700
        result.downloadSpeedMbps >= 20 -> Blue700
        result.downloadSpeedMbps >= 5 -> Orange700
        result.downloadSpeedMbps > 0 -> Red500
        else -> Gray400
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(catColor)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = result.cdnName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.weight(1f)
                )
                if (result.category != CDNCategory.UNKNOWN) {
                    Surface(
                        color = catColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = result.category.label,
                            fontSize = 10.sp,
                            color = catColor,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CdnStatItem(label = "DL", value = "${String.format("%.1f", result.downloadSpeedMbps)} Mbps", color = speedColor)
                CdnStatItem(label = "Latency", value = "${String.format("%.1f", result.latencyMs)} ms", color = if (result.latencyMs in 1.0..50.0) Green700 else if (result.latencyMs in 50.0..150.0) Orange700 else Gray500)
                CdnStatItem(label = "Data", value = formatBytes(result.bytesDownloaded), color = Purple700)
                CdnStatItem(label = "Time", value = formatDuration(result.durationMs), color = Gray600)
            }
        }
    }
}

@Composable
private fun CdnStatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Gray500,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun AggregatedStat(
    label: String,
    value: String,
    unit: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Gray500,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            if (unit.isNotEmpty()) {
                Spacer(Modifier.width(2.dp))
                Text(
                    text = unit,
                    fontSize = 12.sp,
                    color = Gray500,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
    }
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


