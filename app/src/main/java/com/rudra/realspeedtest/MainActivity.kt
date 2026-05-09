package com.rudra.realspeedtest

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rudra.realspeedtest.ui.screens.analytics.AnalyticsScreen
import com.rudra.realspeedtest.ui.screens.history.HistoryScreen
import com.rudra.realspeedtest.ui.screens.speedtest.SpeedTestScreen
import com.rudra.realspeedtest.ui.screens.cdn.CdnTestScreen
import com.rudra.realspeedtest.ui.screens.cdn.CdnTestViewModel
import com.rudra.realspeedtest.ui.screens.features.FeatureHubScreen
import com.rudra.realspeedtest.ui.screens.settings.SettingsScreen
import com.rudra.realspeedtest.ui.theme.*
import com.rudra.realspeedtest.ui.screens.speedtest.SpeedTestViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val viewModel: SpeedTestViewModel = viewModel(factory = SpeedTestViewModel.Factory(context))
            val darkModePref by viewModel.isDarkMode.collectAsState()
            val followSystemPref by viewModel.darkModeFollowSystem.collectAsState()

            val followSystem = followSystemPref ?: (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            val manualDark = darkModePref ?: false
            val isDarkMode = if (followSystem) isSystemInDarkTheme() else manualDark

            RealSpeedTestTheme(darkTheme = isDarkMode) {
                MainScreen(
                    viewModel = viewModel,
                    isDarkMode = isDarkMode
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: SpeedTestViewModel,
    isDarkMode: Boolean
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var showCdnTest by remember { mutableStateOf(false) }

    val tabs = listOf(
        NavigationItem(
            title = "Speed Test",
            selectedIcon = Icons.Filled.Speed,
            unselectedIcon = Icons.Outlined.Speed
        ),
        NavigationItem(
            title = "History",
            selectedIcon = Icons.Filled.History,
            unselectedIcon = Icons.Outlined.History
        ),
        NavigationItem(
            title = "Analytics",
            selectedIcon = Icons.Filled.Analytics,
            unselectedIcon = Icons.Outlined.Analytics
        ),
        NavigationItem(
            title = "Features",
            selectedIcon = Icons.Filled.Apps,
            unselectedIcon = Icons.Outlined.Apps
        )
    )

    val navBarColor = when (selectedTab) {
        0 -> MaterialTheme.colorScheme.primary
        1 -> MaterialTheme.colorScheme.secondary
        2 -> MaterialTheme.colorScheme.tertiary
        3 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary
    }

    // Show CdnTestScreen overlay
    if (showCdnTest) {
        val cdnViewModel: CdnTestViewModel = viewModel(factory = CdnTestViewModel.Factory(context))
        CdnTestScreen(
            viewModel = cdnViewModel,
            onBack = { showCdnTest = false }
        )
        return
    }

    // Show SettingsScreen as a full overlay when showSettings is true
    if (showSettings) {
        SettingsScreen(
            viewModel = viewModel,
            onBack = { showSettings = false }
        )
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                tabs.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == index) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        },
                        label = {
                            Text(
                                text = item.title,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = navBarColor,
                            selectedTextColor = navBarColor,
                            indicatorColor = navBarColor.copy(alpha = 0.1f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (selectedTab) {
                0 -> SpeedTestScreen(
                    viewModel = viewModel,
                    onOpenSettings = { showSettings = true }
                )
                1 -> HistoryScreen(viewModel = viewModel)
                2 -> AnalyticsScreen(viewModel = viewModel)
                3 -> FeatureHubScreen(
                    viewModel = viewModel,
                    onNavigateToTab = { tab -> selectedTab = tab },
                    onOpenSettings = { showSettings = true },
                    onOpenCdnTest = { showCdnTest = true }
                )
            }
        }
    }
}

private data class NavigationItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)
