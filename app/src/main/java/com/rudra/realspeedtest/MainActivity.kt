package com.rudra.realspeedtest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.rudra.realspeedtest.ui.theme.*
import com.rudra.realspeedtest.ui.screens.speedtest.SpeedTestViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var isDarkMode by remember { mutableStateOf(false) }

            RealSpeedTestTheme(darkTheme = isDarkMode) {
                MainScreen(
                    onToggleDarkMode = { isDarkMode = !isDarkMode }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onToggleDarkMode: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel: SpeedTestViewModel = viewModel(factory = SpeedTestViewModel.Factory(context))
    var selectedTab by remember { mutableIntStateOf(0) }

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
        )
    )

    val navBarColor = when (selectedTab) {
        0 -> Green700
        1 -> Blue700
        2 -> Purple700
        else -> Green700
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = CardLight,
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
                            unselectedIconColor = Gray500,
                            unselectedTextColor = Gray500
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
                0 -> SpeedTestScreen(viewModel = viewModel)
                1 -> HistoryScreen(viewModel = viewModel)
                2 -> AnalyticsScreen(viewModel = viewModel)
            }
        }
    }
}

private data class NavigationItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)