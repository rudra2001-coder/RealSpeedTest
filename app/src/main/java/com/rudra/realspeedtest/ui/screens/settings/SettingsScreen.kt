package com.rudra.realspeedtest.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rudra.realspeedtest.ui.theme.*
import com.rudra.realspeedtest.ui.screens.speedtest.SpeedTestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SpeedTestViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var autoTestEnabled by remember { mutableStateOf(viewModel.isAutoTestEnabled.value) }
    var intervalMinutes by remember { mutableStateOf(viewModel.autoTestIntervalMinutes.value.toFloat()) }
    var speedThreshold by remember { mutableStateOf(viewModel.speedThreshold.value.toFloat()) }
    var isDarkMode by remember { mutableStateOf(viewModel.isDarkMode.value) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, null, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Settings", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Gray800,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Appearance Section
            SectionHeader("Appearance", Icons.Default.Palette, Purple700)

            SettingsCard {
                SwitchSetting(
                    icon = Icons.Default.DarkMode,
                    iconColor = Indigo700,
                    title = "Dark Mode",
                    subtitle = "Switch between light and dark theme",
                    checked = isDarkMode,
                    onCheckedChange = { enabled ->
                        isDarkMode = enabled
                        viewModel.setDarkMode(enabled)
                    }
                )
            }

            // Test Settings Section
            SectionHeader("Test Settings", Icons.Default.Speed, Green700)

            SettingsCard {
                SwitchSetting(
                    icon = Icons.Default.Autorenew,
                    iconColor = Green700,
                    title = "Auto Test",
                    subtitle = "Automatically run tests at intervals",
                    checked = autoTestEnabled,
                    onCheckedChange = { enabled ->
                        autoTestEnabled = enabled
                        if (enabled) viewModel.scheduleAutoTest(intervalMinutes.toInt())
                        else viewModel.cancelAutoTest()
                    }
                )

                if (autoTestEnabled) {
                    HorizontalDivider(color = Gray100, modifier = Modifier.padding(vertical = 8.dp))
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Interval", style = MaterialTheme.typography.bodyMedium, color = Gray600)
                            Text(
                                "${intervalMinutes.toInt()} minutes",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Green700
                            )
                        }
                        Slider(
                            value = intervalMinutes,
                            onValueChange = { intervalMinutes = it },
                            valueRange = 5f..120f,
                            steps = 22,
                            colors = SliderDefaults.colors(
                                thumbColor = Green700,
                                activeTrackColor = Green700,
                                inactiveTrackColor = Gray200
                            )
                        )
                    }
                }
            }

            // Alerts Section
            SectionHeader("Notifications", Icons.Default.Notifications, Orange700)

            SettingsCard {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Orange700.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Speed, null,
                                    tint = Orange700,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(
                                    "Speed Alert Threshold",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Notify when speed drops below",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Gray500
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${speedThreshold.toInt()} Mbps", fontWeight = FontWeight.Bold, color = Orange700)
                        if (speedThreshold > 0) {
                            TextButton(onClick = { speedThreshold = 0f; viewModel.setSpeedThreshold(0.0) }) {
                                Text("Disable", color = Gray500)
                            }
                        }
                    }
                    Slider(
                        value = speedThreshold,
                        onValueChange = { speedThreshold = it },
                        valueRange = 1f..100f,
                        steps = 98,
                        colors = SliderDefaults.colors(
                            thumbColor = Orange700,
                            activeTrackColor = Orange700,
                            inactiveTrackColor = Gray200
                        )
                    )
                }
            }

            // About Section
            SectionHeader("About", Icons.Default.Info, Blue700)

            SettingsCard {
                Column {
                    AboutRow("App Name", "Real Speed Test")
                    HorizontalDivider(color = Gray100, modifier = Modifier.padding(vertical = 6.dp))
                    AboutRow("Version", "1.0.0")
                    HorizontalDivider(color = Gray100, modifier = Modifier.padding(vertical = 6.dp))
                    AboutRow("Developer", "Rudra")
                    HorizontalDivider(color = Gray100, modifier = Modifier.padding(vertical = 6.dp))
                    AboutRow("Network Tests", "10 CDN endpoints across global locations")
                    HorizontalDivider(color = Gray100, modifier = Modifier.padding(vertical = 6.dp))
                    AboutRow("Data Storage", "Local only — your data stays on device")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Save button
            Button(
                onClick = {
                    viewModel.setSpeedThreshold(speedThreshold.toDouble())
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green700),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("Save Settings", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun SwitchSetting(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Gray500)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = iconColor,
                checkedTrackColor = iconColor.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Gray600)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
