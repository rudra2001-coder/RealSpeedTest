package com.rudra.realspeedtest.ui.screens.speedtest

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rudra.realspeedtest.MainActivity
import com.rudra.realspeedtest.R
import com.rudra.realspeedtest.data.PreferencesManager
import com.rudra.realspeedtest.data.model.CDNEndpoint
import com.rudra.realspeedtest.data.model.ConnectionType
import com.rudra.realspeedtest.data.model.NetworkInfo
import com.rudra.realspeedtest.data.model.QualityLabel
import com.rudra.realspeedtest.data.model.SpeedTestConfig
import com.rudra.realspeedtest.data.model.SpeedTestResult
import com.rudra.realspeedtest.data.model.TestMode
import com.rudra.realspeedtest.data.model.TestProgress
import com.rudra.realspeedtest.engine.SpeedTestEngine
import com.rudra.realspeedtest.repository.TestHistoryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask

class SpeedTestViewModel(private val context: Context) : ViewModel() {

    private val engine = SpeedTestEngine(context)
    private val historyRepository = TestHistoryRepository(context)
    private val preferencesManager = PreferencesManager(context)
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private val _isTestRunning = MutableStateFlow(false)
    val isTestRunning: StateFlow<Boolean> = _isTestRunning.asStateFlow()

    private val _testProgress = MutableStateFlow(TestProgress())
    val testProgress: StateFlow<TestProgress> = _testProgress

    private val _currentResults = MutableStateFlow<List<CDNEndpoint>>(emptyList())
    val currentResults: StateFlow<List<CDNEndpoint>> = _currentResults

    private val _currentResult = MutableStateFlow<SpeedTestResult?>(null)
    val currentResult: StateFlow<SpeedTestResult?> = _currentResult.asStateFlow()

    private val _testHistory = MutableStateFlow<List<SpeedTestResult>>(emptyList())
    val testHistory: StateFlow<List<SpeedTestResult>> = _testHistory.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _isDarkMode = MutableStateFlow<Boolean?>(null)
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode.asStateFlow()

    private val _darkModeFollowSystem = MutableStateFlow<Boolean?>(null)
    val darkModeFollowSystem: StateFlow<Boolean?> = _darkModeFollowSystem.asStateFlow()

    private val _isAutoTestEnabled = MutableStateFlow(false)
    val isAutoTestEnabled: StateFlow<Boolean> = _isAutoTestEnabled.asStateFlow()

    private val _autoTestIntervalMinutes = MutableStateFlow(30)
    val autoTestIntervalMinutes: StateFlow<Int> = _autoTestIntervalMinutes.asStateFlow()

    private val _speedThreshold = MutableStateFlow(10.0)
    val speedThreshold: StateFlow<Double> = _speedThreshold.asStateFlow()

    private val _testMode = MutableStateFlow(TestMode.QUICK)
    val testMode: StateFlow<TestMode> = _testMode.asStateFlow()

    private var autoTestJob: Job? = null
    private var autoTestTimer: Timer? = null

    init {
        loadHistory()
        // Listen to engine progress updates
        viewModelScope.launch {
            engine.progress.collect { progress ->
                _testProgress.value = progress
            }
        }
        // Listen to engine live CDN results
        viewModelScope.launch {
            engine.currentResults.collect { results ->
                _currentResults.value = results
            }
        }
        // Listen to engine final test result
        viewModelScope.launch {
            engine.testResult.collect { result ->
                if (result != null) {
                    _currentResult.value = result
                    historyRepository.saveTestResult(result)
                    loadHistory()
                    if (result.isThrottled) triggerThrottlingAlert()
                    if (_isAutoTestEnabled.value) checkSpeedThreshold(result)
                    triggerHapticFeedback()
                    _isTestRunning.value = false
                }
            }
        }
        // Read persisted preferences
        viewModelScope.launch {
            preferencesManager.darkMode.collect { isDark ->
                _isDarkMode.value = isDark
            }
        }
        viewModelScope.launch {
            preferencesManager.darkModeFollowSystem.collect { follow ->
                _darkModeFollowSystem.value = follow
            }
        }
        // Migration: existing users with a stored dark_mode preference should not auto-follow system
        viewModelScope.launch {
            val hasManualPref = preferencesManager.darkMode.first() != null
            val hasFollowPref = preferencesManager.darkModeFollowSystem.first() != null
            if (hasManualPref && !hasFollowPref) {
                preferencesManager.setDarkModeFollowSystem(false)
            }
        }
    }

    fun triggerHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        } catch (_: Exception) { }
    }

    fun triggerThrottlingAlert() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 100, 100), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 100, 100, 100), -1)
            }
        } catch (_: Exception) { }
    }

    fun setTestMode(mode: TestMode) {
        _testMode.value = mode
        engine.updateConfig(SpeedTestConfig(testMode = mode))
    }

    fun startTest() {
        if (_isTestRunning.value) return

        _isTestRunning.value = true
        _currentResult.value = null
        _currentResults.value = emptyList()

        engine.startLiveTest(viewModelScope)
    }

    fun startLiveTest() = startTest()

    fun updateEndpointProgress(endpointName: String, progress: Float, speed: Double) {
        engine.updateEndpointProgress(endpointName, progress, speed)
    }

    fun observeLiveResults(): StateFlow<List<CDNEndpoint>> = _currentResults

    fun runUploadTest() {
        viewModelScope.launch { engine.runUploadTest() }
    }

    fun measureJitter() {
        viewModelScope.launch { engine.measureJitter() }
    }

    fun measurePacketLoss() {
        viewModelScope.launch { engine.measurePacketLoss() }
    }

    fun saveTestResult(result: SpeedTestResult) {
        historyRepository.saveTestResult(result)
        loadHistory()
    }

    fun getTestHistory(): List<SpeedTestResult> = historyRepository.getTestHistory()

    fun clearHistory() {
        historyRepository.clearHistory()
        loadHistory()
    }

    fun exportHistoryAsCsv(): String {
        val history = _testHistory.value
        if (history.isEmpty()) return ""

        return buildString {
            appendLine("Date,Download (Mbps),Upload (Mbps),Latency (ms),Jitter (ms),Packet Loss (%),ISP Score,Quality")
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            history.forEach { result ->
                appendLine("${dateFormat.format(java.util.Date(result.timestamp))},${result.downloadSpeedMbps},${result.uploadSpeedMbps},${result.latencyMs},${result.jitterMs},${result.packetLossPercent},${result.ispScore},${result.qualityLabel}")
            }
        }
    }

    private fun loadHistory() {
        _testHistory.value = historyRepository.getTestHistory()
    }

    fun prepareChartData(): List<Pair<String, Double>> {
        return engine.prepareChartData(_currentResults.value)
    }

    fun getHistoryForChart(): List<Pair<Long, Double>> {
        return _testHistory.value
            .sortedBy { it.timestamp }
            .takeLast(30)
            .map { it.timestamp to it.downloadSpeedMbps }
    }

    fun getUploadHistoryForChart(): List<Pair<Long, Double>> {
        return _testHistory.value
            .sortedBy { it.timestamp }
            .takeLast(30)
            .map { it.timestamp to it.uploadSpeedMbps }
    }

    fun getLatencyHistoryForChart(): List<Pair<Long, Double>> {
        return _testHistory.value
            .sortedBy { it.timestamp }
            .takeLast(30)
            .map { it.timestamp to it.latencyMs }
    }

    fun calculateAverageSpeed(): Double {
        val history = _testHistory.value.takeLast(30)
        return if (history.isNotEmpty()) history.map { it.downloadSpeedMbps }.average() else 0.0
    }

    fun calculateISPScore(downloadSpeed: Double, latency: Double, jitter: Double, packetLoss: Double): Int {
        return engine.calculateISPScore(downloadSpeed, latency, jitter, packetLoss)
    }

    fun getQualityLabel(score: Int): QualityLabel = engine.getQualityLabel(score)

    fun detectThrottling(): Pair<Boolean, String?> = engine.detectThrottling(_currentResults.value)

    fun findInconsistentEndpoints(): List<String> = engine.findInconsistentEndpoints(_currentResults.value)

    fun exportAsText(): String {
        return _currentResult.value?.let { engine.exportAsText(it) } ?: ""
    }

    fun getNetworkInfo(): NetworkInfo = engine.getNetworkInfo()

    fun getPublicIP(): String = engine.getNetworkInfo().publicIP

    fun getISPInfo(): String = engine.getNetworkInfo().ispName

    fun getConnectionType(): ConnectionType = engine.getNetworkInfo().connectionType

    fun scheduleAutoTest(intervalMinutes: Int) {
        _isAutoTestEnabled.value = true
        _autoTestIntervalMinutes.value = intervalMinutes
        autoTestTimer?.cancel()
        autoTestTimer = Timer()
        autoTestTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (_isAutoTestEnabled.value) {
                    viewModelScope.launch { startTest() }
                }
            }
        }, intervalMinutes * 60 * 1000L, intervalMinutes * 60 * 1000L)
    }

    fun cancelAutoTest() {
        _isAutoTestEnabled.value = false
        autoTestTimer?.cancel()
        autoTestTimer = null
    }

    fun checkSpeedThreshold(result: SpeedTestResult) {
        if (result.downloadSpeedMbps < _speedThreshold.value && _speedThreshold.value > 0) {
            // Alert handled by notification system
        }
    }

    fun sendAlertNotification(context: Context, currentSpeed: Double) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "speed_alerts",
                "Speed Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when speed drops below threshold"
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, "speed_alerts")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Speed Alert")
            .setContentText("Current speed ${String.format("%.1f", currentSpeed)} Mbps is below threshold ${_speedThreshold.value} Mbps")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(1, notification)
    }

    fun setTab(index: Int) {
        _selectedTab.value = index
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            val followSystem = _darkModeFollowSystem.value ?: (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            if (followSystem) {
                preferencesManager.setDarkModeFollowSystem(false)
                _darkModeFollowSystem.value = false
                val mode = context.resources.configuration.uiMode
                val isSystemDark = (mode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                        android.content.res.Configuration.UI_MODE_NIGHT_YES
                preferencesManager.setDarkMode(!isSystemDark)
                _isDarkMode.value = !isSystemDark
            } else {
                val newValue = !(_isDarkMode.value ?: false)
                preferencesManager.setDarkMode(newValue)
                _isDarkMode.value = newValue
            }
        }
    }

    fun setDarkMode(enabled: Boolean?) {
        viewModelScope.launch {
            preferencesManager.setDarkMode(enabled)
            _isDarkMode.value = enabled
        }
    }

    fun setDarkModeFollowSystem(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setDarkModeFollowSystem(enabled)
            _darkModeFollowSystem.value = enabled
        }
    }

    fun setSpeedThreshold(threshold: Double) {
        viewModelScope.launch {
            preferencesManager.setSpeedThreshold(threshold)
            _speedThreshold.value = threshold
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SpeedTestViewModel(context) as T
        }
    }
}
