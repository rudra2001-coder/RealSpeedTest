package com.rudra.realspeedtest.ui.screens.history

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rudra.realspeedtest.data.model.QualityLabel
import com.rudra.realspeedtest.data.model.SpeedTestResult
import com.rudra.realspeedtest.ui.components.*
import com.rudra.realspeedtest.ui.theme.*
import com.rudra.realspeedtest.ui.screens.speedtest.SpeedTestViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: SpeedTestViewModel,
    modifier: Modifier = Modifier
) {
    val history by viewModel.testHistory.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<SpeedTestResult?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Test History",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    if (history.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Clear History",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Blue700,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        if (history.isEmpty()) {
            EmptyHistoryState(
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
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    HistorySummaryCard(history = history)
                }

                items(history) { result ->
                    HistoryCard(
                        result = result,
                        isExpanded = selectedItem == result,
                        onClick = {
                            selectedItem = if (selectedItem == result) null else result
                        }
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    "Clear History",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Are you sure you want to clear all test history? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearHistory()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red500)
                ) {
                    Text("Clear All", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = Gray600)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun EmptyHistoryState(modifier: Modifier = Modifier) {
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
                    .background(Gray100, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = Gray400
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "No Test History",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Run a speed test to see your history here.\nTrack your network performance over time.",
                style = MaterialTheme.typography.bodyMedium,
                color = Gray500,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HistorySummaryCard(history: List<SpeedTestResult>) {
    val avgDownload = if (history.isNotEmpty()) history.map { it.downloadSpeedMbps }.average() else 0.0
    val avgUpload = if (history.isNotEmpty()) history.map { it.uploadSpeedMbps }.average() else 0.0
    val avgLatency = if (history.isNotEmpty()) history.map { it.latencyMs }.average() else 0.0
    val totalTests = history.size

    val excellentCount = history.count { it.qualityLabel == QualityLabel.EXCELLENT }
    val goodCount = history.count { it.qualityLabel == QualityLabel.GOOD }

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
                        text = "Performance Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$totalTests tests recorded",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500
                    )
                }
                Surface(
                    color = Green700.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = GoodColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${excellentCount + goodCount} Good",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Green700
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Gray200)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryItem(
                    label = "Avg Download",
                    value = String.format("%.1f", avgDownload),
                    unit = "Mbps",
                    color = Blue700
                )
                SummaryItem(
                    label = "Avg Upload",
                    value = String.format("%.1f", avgUpload),
                    unit = "Mbps",
                    color = Teal700
                )
                SummaryItem(
                    label = "Avg Latency",
                    value = String.format("%.0f", avgLatency),
                    unit = "ms",
                    color = Purple700
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: String,
    unit: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Gray500
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = " $unit",
                style = MaterialTheme.typography.bodySmall,
                color = Gray500
            )
        }
    }
}

@Composable
private fun HistoryCard(
    result: SpeedTestResult,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())

    val scoreColor = when (result.qualityLabel) {
        QualityLabel.EXCELLENT -> ExcellentColor
        QualityLabel.GOOD -> GoodColor
        QualityLabel.FAIR -> FairColor
        QualityLabel.POOR -> PoorColor
        QualityLabel.BAD -> BadColor
        QualityLabel.UNKNOWN -> Gray400
    }

    ModernCard(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = dateFormat.format(Date(result.timestamp)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = result.networkInfo?.ispName ?: "Unknown ISP",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(scoreColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${result.ispScore}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = scoreColor
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Gray500
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CompactStatItem(
                    icon = Icons.Default.Download,
                    value = String.format("%.1f", result.downloadSpeedMbps),
                    color = Blue700
                )
                CompactStatItem(
                    icon = Icons.Default.Upload,
                    value = String.format("%.1f", result.uploadSpeedMbps),
                    color = Teal700
                )
                CompactStatItem(
                    icon = Icons.Default.NetworkPing,
                    value = String.format("%.0f", result.latencyMs),
                    color = Purple700
                )
                CompactStatItem(
                    icon = Icons.Default.Speed,
                    value = result.qualityLabel.name.lowercase()
                        .replaceFirstChar { it.uppercase() },
                    color = scoreColor
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Gray200)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CompactStatItem(
                        icon = Icons.Default.SignalWifi4Bar,
                        value = String.format("%.1f", result.jitterMs),
                        label = "Jitter",
                        color = Orange700
                    )
                    CompactStatItem(
                        icon = Icons.Default.Warning,
                        value = String.format("%.1f", result.packetLossPercent),
                        label = "Loss",
                        color = Red700
                    )
                }

                if (result.isThrottled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    AlertBanner(
                        title = "Throttling Detected",
                        message = "ISP may be throttling speed on ${result.throttledCDN}",
                        icon = Icons.Default.Warning,
                        color = Orange500
                    )
                }

                result.networkInfo?.let { network ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Public IP",
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray500
                            )
                            Text(
                                text = network.publicIP,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Connection",
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray500
                            )
                            Text(
                                text = network.connectionType.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (network.city.isNotEmpty() || network.country.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Location",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Gray500
                                )
                                Text(
                                    text = buildString {
                                        if (network.city.isNotEmpty()) append(network.city)
                                        if (network.city.isNotEmpty() && network.country.isNotEmpty()) append(", ")
                                        if (network.country.isNotEmpty()) append(network.country)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "ISP",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Gray500
                                )
                                Text(
                                    text = network.ispName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactStatItem(
    icon: ImageVector,
    value: String,
    color: Color,
    label: String? = null
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Gray500
            )
        }
    }
}