package com.rudra.realspeedtest.ui.screens.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rudra.realspeedtest.data.model.CDNEndpoint
import com.rudra.realspeedtest.data.model.SpeedTestResult
import com.rudra.realspeedtest.data.model.TestStatus
import com.rudra.realspeedtest.ui.components.*
import com.rudra.realspeedtest.ui.theme.*
import com.rudra.realspeedtest.ui.screens.speedtest.SpeedTestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: SpeedTestViewModel,
    modifier: Modifier = Modifier
) {
    val history by viewModel.testHistory.collectAsState()
    val currentResults by viewModel.currentResults.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Analytics",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        if (currentResults.isEmpty() && history.size < 2) {
            EmptyAnalyticsState(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (currentResults.isNotEmpty()) {
                    item {
                        CDNPerformanceOverview(cdnResults = currentResults)
                    }

                    item {
                        SectionHeader(title = "CDN Performance Details")
                    }

                    items(currentResults) { cdn ->
                        CDNPerformanceCard(cdn = cdn)
                    }
                }

                if (history.size >= 2) {
                    item {
                        SectionHeader(title = "Speed Over Time")
                    }

                    item {
                        SpeedHistoryChart(history = history.take(10).reversed())
                    }

                    item {
                        StatisticsCard(history = history)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAnalyticsState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "No Analytics Yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Run at least 2 speed tests to see\ndetailed analytics and trends.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CDNPerformanceOverview(cdnResults: List<CDNEndpoint>) {
    val avgSpeed = cdnResults.filter { it.status == TestStatus.DONE }
        .map { it.downloadSpeedMbps }.average().coerceAtLeast(0.0)
    val maxSpeed = cdnResults.filter { it.status == TestStatus.DONE }
        .maxOfOrNull { it.downloadSpeedMbps } ?: 0.0
    val minSpeed = cdnResults.filter { it.status == TestStatus.DONE }
        .minOfOrNull { it.downloadSpeedMbps } ?: 0.0
    val avgLatency = cdnResults.filter { it.status == TestStatus.DONE }
        .map { it.latencyMs }.average().coerceAtLeast(0.0)
    val successRate = (cdnResults.count { it.status == TestStatus.DONE }.toDouble() / cdnResults.size) * 100

    ModernCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Current Test Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${cdnResults.size} CDN endpoints tested",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${successRate.toInt()}% Success",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SpeedGauge(
                speed = avgSpeed,
                size = 160.dp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OverviewStatItem(
                    label = "Max Speed",
                    value = String.format("%.1f", maxSpeed),
                    color = ExcellentColor
                )
                OverviewStatItem(
                    label = "Min Speed",
                    value = String.format("%.1f", minSpeed),
                    color = PoorColor
                )
                OverviewStatItem(
                    label = "Avg Latency",
                    value = String.format("%.0f", avgLatency),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun OverviewStatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = if (label.contains("Latency")) "ms" else "Mbps",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CDNPerformanceCard(cdn: CDNEndpoint) {
    val statusColor = when (cdn.status) {
        TestStatus.DONE -> ExcellentColor
        TestStatus.RUNNING, TestStatus.TESTING -> FairColor
        TestStatus.FAILED -> BadColor
        TestStatus.PENDING -> Gray400
    }

    val speedColor = when {
        cdn.downloadSpeedMbps >= 80 -> ExcellentColor
        cdn.downloadSpeedMbps >= 40 -> GoodColor
        cdn.downloadSpeedMbps >= 20 -> FairColor
        cdn.downloadSpeedMbps >= 10 -> PoorColor
        else -> BadColor
    }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(statusColor, RoundedCornerShape(6.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = cdn.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when (cdn.status) {
                                TestStatus.DONE -> "Completed successfully"
                                TestStatus.RUNNING, TestStatus.TESTING -> "Testing in progress..."
                                TestStatus.FAILED -> "Test failed"
                                TestStatus.PENDING -> "Pending"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor
                        )
                    }
                }
                if (cdn.status == TestStatus.DONE) {
                    MiniSpeedGauge(speed = cdn.downloadSpeedMbps, size = 60.dp)
                }
            }

            if (cdn.status == TestStatus.DONE) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(5.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((cdn.downloadSpeedMbps / 100).toFloat().coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(speedColor.copy(alpha = 0.7f), speedColor)
                                ),
                                shape = RoundedCornerShape(5.dp)
                            )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Speed: ${String.format("%.1f", cdn.downloadSpeedMbps)} Mbps",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Latency: ${String.format("%.0f", cdn.latencyMs)} ms",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedHistoryChart(history: List<SpeedTestResult>) {
    if (history.isEmpty()) return

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
                Column {
                    Text(
                        text = "Download Speed Trend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Last ${history.size} tests",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ShowChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val primaryColor = MaterialTheme.colorScheme.primary
            val surfaceColor = MaterialTheme.colorScheme.surface

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val maxSpeed = history.maxOfOrNull { it.downloadSpeedMbps } ?: 100.0
                val minSpeed = history.minOfOrNull { it.downloadSpeedMbps } ?: 0.0
                val range = (maxSpeed - minSpeed).coerceAtLeast(1.0)

                val stepX = size.width / (history.size - 1).coerceAtLeast(1)

                val gradient = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.3f),
                        primaryColor.copy(alpha = 0f)
                    )
                )

                val fillPath = Path().apply {
                    moveTo(0f, size.height)
                    history.forEachIndexed { index, result ->
                        val x = index * stepX
                        val y = size.height - ((result.downloadSpeedMbps - minSpeed) / range * size.height).toFloat()
                        lineTo(x, y)
                    }
                    lineTo(size.width, size.height)
                    close()
                }
                drawPath(path = fillPath, brush = gradient)

                val linePath = Path()
                history.forEachIndexed { index, result ->
                    val x = index * stepX
                    val y = size.height - ((result.downloadSpeedMbps - minSpeed) / range * size.height).toFloat()
                    if (index == 0) linePath.moveTo(x, y)
                    else linePath.lineTo(x, y)
                }
                drawPath(
                    path = linePath,
                    color = primaryColor,
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )

                history.forEachIndexed { index, result ->
                    val x = index * stepX
                    val y = size.height - ((result.downloadSpeedMbps - minSpeed) / range * size.height).toFloat()
                    drawCircle(
                        color = primaryColor,
                        radius = 8f,
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = surfaceColor,
                        radius = 4f,
                        center = Offset(x, y)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Oldest",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Latest",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatisticsCard(history: List<SpeedTestResult>) {
    val avgDownload = history.map { it.downloadSpeedMbps }.average()
    val avgUpload = history.map { it.uploadSpeedMbps }.average()
    val avgLatency = history.map { it.latencyMs }.average()
    val avgJitter = history.map { it.jitterMs }.average()
    val avgPacketLoss = history.map { it.packetLossPercent }.average()
    val maxDownload = history.maxOfOrNull { it.downloadSpeedMbps } ?: 0.0
    val minDownload = history.minOfOrNull { it.downloadSpeedMbps } ?: 0.0

    ModernCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatBox(
                    label = "Avg Download",
                    value = String.format("%.1f", avgDownload),
                    unit = "Mbps",
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    label = "Avg Upload",
                    value = String.format("%.1f", avgUpload),
                    unit = "Mbps",
                    color = ExcellentColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatBox(
                    label = "Avg Latency",
                    value = String.format("%.0f", avgLatency),
                    unit = "ms",
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    label = "Avg Jitter",
                    value = String.format("%.1f", avgJitter),
                    unit = "ms",
                    color = Orange700,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatBox(
                    label = "Max Speed",
                    value = String.format("%.1f", maxDownload),
                    unit = "Mbps",
                    color = ExcellentColor,
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    label = "Min Speed",
                    value = String.format("%.1f", minDownload),
                    unit = "Mbps",
                    color = PoorColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatBox(
                    label = "Avg Packet Loss",
                    value = String.format("%.2f", avgPacketLoss),
                    unit = "%",
                    color = Red700,
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    label = "Total Tests",
                    value = "${history.size}",
                    unit = "tests",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    ModernCard(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
