package com.rudra.realspeedtest.ui.screens.features

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rudra.realspeedtest.ui.theme.*
import com.rudra.realspeedtest.ui.screens.speedtest.SpeedTestViewModel

data class FeatureItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val action: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureHubScreen(
    viewModel: SpeedTestViewModel,
    onNavigateToTab: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCdnTest: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Apps, null, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("All Features", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, "Settings", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        FeatureGrid(
            onNavigateToTab = onNavigateToTab,
            onOpenSettings = onOpenSettings,
            onOpenCdnTest = onOpenCdnTest,
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

@Composable
private fun FeatureGrid(
    onNavigateToTab: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCdnTest: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val features = listOf(
        FeatureItem(
            "speed_test", "Speed Test",
            "Full network speed analysis with download, upload, ping & jitter",
            Icons.Default.Speed, Green700
        ) { onNavigateToTab(0) },
        FeatureItem(
            "history", "Test History",
            "Browse past results with detailed performance breakdowns",
            Icons.Default.History, Blue700
        ) { onNavigateToTab(1) },
        FeatureItem(
            "analytics", "Analytics",
            "Track trends with charts, statistics and CDN performance data",
            Icons.Default.Analytics, Purple700
        ) { onNavigateToTab(2) },
        FeatureItem(
            "cdn", "CDN Breakdown",
            "Compare speed across multiple CDN endpoints worldwide",
            Icons.Default.Dns, Orange700
        ) { onOpenCdnTest() },
        FeatureItem(
            "network", "Network Info",
            "View public IP, ISP details and connection type",
            Icons.Default.Wifi, Teal700
        ) { onNavigateToTab(0) },
        FeatureItem(
            "fairness", "ISP Score",
            "Detect throttling and measure network fairness score",
            Icons.Default.Verified, Cyan700
        ) { onNavigateToTab(0) },
        FeatureItem(
            "share", "Share Results",
            "Export test data as text, image or share with friends",
            Icons.Default.Share, Pink700
        ) { onNavigateToTab(0) },
        FeatureItem(
            "auto_test", "Auto Test",
            "Schedule automatic speed tests at custom intervals",
            Icons.Default.Autorenew, Green500
        ) { onOpenSettings() },
        FeatureItem(
            "alerts", "Speed Alerts",
            "Get notified when your speed drops below a threshold",
            Icons.Default.NotificationsActive, Red700
        ) { onOpenSettings() },
        FeatureItem(
            "settings", "Settings",
            "Customize appearance, notifications and test preferences",
            Icons.Default.Settings, Gray600
        ) { onOpenSettings() }
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(features) { feature ->
            FeatureGridCard(feature = feature)
        }
    }
}

@Composable
private fun FeatureGridCard(feature: FeatureItem) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = tween(100, easing = FastOutSlowInEasing),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    pressed = true
                    feature.action()
                }
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(feature.color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    feature.icon,
                    contentDescription = null,
                    tint = feature.color,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                feature.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                feature.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp,
                maxLines = 2
            )
        }
    }
}
