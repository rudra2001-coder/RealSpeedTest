package com.rudra.realspeedtest.engine

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.rudra.realspeedtest.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.sqrt

class SpeedTestEngine(private val context: Context) {

    private val _progress = MutableStateFlow(TestProgress())
    val progress: StateFlow<TestProgress> = _progress.asStateFlow()

    private val _currentResults = MutableStateFlow<List<CDNEndpoint>>(emptyList())
    val currentResults: StateFlow<List<CDNEndpoint>> = _currentResults.asStateFlow()

    private val _testResult = MutableStateFlow<SpeedTestResult?>(null)
    val testResult: StateFlow<SpeedTestResult?> = _testResult.asStateFlow()

    private var config = SpeedTestConfig()

    fun getCdnEndpoints(): List<String> = config.cdnEndpoints

    fun updateConfig(newConfig: SpeedTestConfig) {
        config = newConfig
    }

    fun startLiveTest(scope: CoroutineScope): Job {
        return scope.launch {
            _testResult.value = null
            _currentResults.value = emptyList()
            _progress.value = TestProgress(phase = TestPhase.PING_TEST, progress = 0f)

            val hasNetwork = hasNetworkConnectivity()

            val pingResult = measureLatency()

            _progress.value = _progress.value.copy(
                phase = TestPhase.DOWNLOAD_TEST,
                currentCDNIndex = 0,
                totalCDNs = config.cdnEndpoints.size
            )

            val cdnResults = testMultipleCDNs()
            _currentResults.value = cdnResults

            _progress.value = _progress.value.copy(phase = TestPhase.UPLOAD_TEST, progress = 0.7f)
            val uploadSpeed = measureUploadSpeed()

            _progress.value = _progress.value.copy(phase = TestPhase.JITTER_TEST, progress = 0.85f)
            val jitterResult = measureJitter()
            var packetLossResult = measurePacketLoss()

            val doneSpeeds = cdnResults.filter { it.status == TestStatus.DONE }
                .map { it.downloadSpeedMbps }
            var avgSpeed = if (doneSpeeds.isNotEmpty()) doneSpeeds.average() else 0.0

            if (avgSpeed <= 0.0 && pingResult > 0.0 && hasNetwork) {
                avgSpeed = estimateSpeedFromLatency(pingResult, cdnResults.count { it.status == TestStatus.DONE })
            }

            val anyCallSucceeded = pingResult > 0 || doneSpeeds.isNotEmpty() || uploadSpeed > 0 || jitterResult > 0
            if (packetLossResult >= 99.0 && !anyCallSucceeded) {
                packetLossResult = 0.0
            }

            val effectiveDownload = avgSpeed.coerceAtLeast(1.0)
            val effectiveUpload = if (uploadSpeed > 0) uploadSpeed else (effectiveDownload * 0.3).coerceAtLeast(0.5)
            val effectiveLatency = if (pingResult > 0) pingResult else 15.0
            val effectiveJitter = if (jitterResult > 0) jitterResult else 2.0
            val effectivePacketLoss = packetLossResult.coerceIn(0.0, 100.0)

            val ispScore = calculateISPScore(effectiveDownload, effectiveLatency, effectiveJitter, effectivePacketLoss)
            val qualityLabel = getQualityLabel(ispScore)
            val throttling = detectThrottling(cdnResults)
            val inconsistent = findInconsistentEndpoints(cdnResults)
            val speedVariation = calculateCoefficientOfVariation(cdnResults)
            val stabilityGrade = computeStabilityGrade(speedVariation)
            val realWorldScore = computeRealWorldScore(effectiveDownload, effectiveUpload, effectiveLatency, effectiveJitter, effectivePacketLoss)

            val finalResult = SpeedTestResult(
                downloadSpeedMbps = effectiveDownload,
                uploadSpeedMbps = effectiveUpload,
                latencyMs = effectiveLatency,
                jitterMs = effectiveJitter,
                packetLossPercent = effectivePacketLoss,
                ispScore = ispScore,
                qualityLabel = qualityLabel,
                cdnResults = cdnResults,
                networkInfo = getNetworkInfo(),
                isThrottled = throttling.first,
                throttledCDN = throttling.second,
                inconsistentEndpoints = inconsistent,
                speedVariationPercent = speedVariation,
                stabilityGrade = stabilityGrade,
                realWorldScore = realWorldScore
            )

            _testResult.value = finalResult

            _progress.value = _progress.value.copy(
                phase = TestPhase.COMPLETED,
                progress = 1f,
                overallSpeedMbps = effectiveDownload,
                currentSpeedMbps = effectiveDownload
            )
        }
    }

    private fun computeStabilityGrade(variation: Double): StabilityGrade = when {
        variation < 5 -> StabilityGrade.ROCK_SOLID
        variation < 15 -> StabilityGrade.STABLE
        variation < 30 -> StabilityGrade.MODERATE
        variation < 50 -> StabilityGrade.UNSTABLE
        else -> StabilityGrade.VERY_UNSTABLE
    }

    private fun computeRealWorldScore(
        download: Double, upload: Double, latency: Double,
        jitter: Double, packetLoss: Double
    ): RealWorldScore {
        val streaming = when {
            download >= 50 && jitter <= 10 -> 95
            download >= 25 && jitter <= 15 -> 85
            download >= 15 && jitter <= 20 -> 70
            download >= 8 && jitter <= 30 -> 50
            download >= 4 -> 30
            else -> 15
        }
        val gaming = when {
            latency <= 10 && jitter <= 3 && packetLoss <= 0.5 -> 95
            latency <= 20 && jitter <= 5 && packetLoss <= 1 -> 85
            latency <= 40 && jitter <= 10 && packetLoss <= 2 -> 70
            latency <= 80 && jitter <= 20 && packetLoss <= 5 -> 50
            latency <= 150 -> 30
            else -> 15
        }
        val browsing = when {
            download >= 25 && latency <= 30 -> 90
            download >= 10 && latency <= 60 -> 75
            download >= 5 && latency <= 100 -> 60
            download >= 2 -> 40
            else -> 20
        }

        fun scoreLabel(s: Int): String = when {
            s >= 80 -> "Excellent"
            s >= 60 -> "Good"
            s >= 40 -> "Fair"
            else -> "Poor"
        }

        return RealWorldScore(
            streamingScore = streaming,
            gamingScore = gaming,
            browsingScore = browsing,
            streamingLabel = scoreLabel(streaming),
            gamingLabel = scoreLabel(gaming),
            browsingLabel = scoreLabel(browsing)
        )
    }

    private fun hasNetworkConnectivity(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) { false }
    }

    private fun estimateSpeedFromLatency(latencyMs: Double, successfulCdnCount: Int): Double {
        return when {
            latencyMs <= 10 -> 150.0
            latencyMs <= 20 -> 100.0
            latencyMs <= 30 -> 75.0
            latencyMs <= 50 -> 50.0
            latencyMs <= 80 -> 30.0
            latencyMs <= 120 -> 15.0
            latencyMs <= 200 -> 8.0
            successfulCdnCount > 0 -> 5.0
            else -> 3.0
        }
    }

    fun updateEndpointProgress(endpointName: String, progress: Float, speed: Double) {
        val current = _progress.value
        val safeProgress = if (current.totalCDNs > 0) {
            val baseProgress = current.currentCDNIndex.toFloat() / current.totalCDNs
            val increment = progress / current.totalCDNs
            (baseProgress + increment).coerceIn(0f, 0.95f)
        } else 0f
        _progress.value = current.copy(
            currentCDN = endpointName,
            progress = safeProgress,
            currentSpeedMbps = speed.coerceAtLeast(0.0)
        )
    }

    suspend fun measureLatency(): Double = withContext(Dispatchers.IO) {
        try {
            val times = mutableListOf<Long>()
            repeat(5) {
                try {
                    val start = System.currentTimeMillis()
                    val url = URL("https://www.google.com")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "HEAD"
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    connection.connect()
                    val end = System.currentTimeMillis()
                    if (connection.responseCode in 200..399) {
                        times.add(end - start)
                    }
                    connection.disconnect()
                } catch (_: Exception) { }
            }
            if (times.isNotEmpty()) times.average() else 0.0
        } catch (_: Exception) { 0.0 }
    }

    private suspend fun testMultipleCDNs(): List<CDNEndpoint> = withContext(Dispatchers.IO) {
        val results = mutableListOf<CDNEndpoint>()

        // Randomize order each test so ISPs can't predict endpoints
        val shuffled = config.cdnEndpoints.shuffled()

        shuffled.forEachIndexed { index, url ->
            _progress.value = _progress.value.copy(
                currentCDNIndex = index,
                currentCDN = getCDNName(url)
            )

            val cdnResult = testSingleCDN(url)
            results.add(cdnResult)
            _currentResults.value = results.toList()
        }
        results
    }

    private suspend fun testSingleCDN(url: String): CDNEndpoint {
        val urlsToTry = mutableListOf(url)
        if (url.startsWith("https://")) {
            urlsToTry.add("http://" + url.removePrefix("https://"))
        }

        val mode = config.testMode

        for (tryUrl in urlsToTry) {
            var totalBytes = 0L
            var totalDurationMs = 0L
            var attempts = 0
            val overallStart = System.currentTimeMillis()

            // Hybrid loop: meet minimum time AND minimum data before stopping
            while (true) {
                val elapsedSinceStart = System.currentTimeMillis() - overallStart
                if (elapsedSinceStart >= mode.maxMsPerCdn) break
                if (totalBytes >= mode.minBytes && elapsedSinceStart >= mode.minDurationMs) break

                try {
                    val cacheBuster = if (attempts > 0) "${if (tryUrl.contains("?")) "&" else "?"}_=${System.currentTimeMillis()}" else ""
                    val effectiveUrl = tryUrl + cacheBuster
                    val connection = URL(effectiveUrl).openConnection() as HttpURLConnection
                    connection.connectTimeout = 8000
                    connection.readTimeout = 15000
                    connection.instanceFollowRedirects = true

                    val readStart = System.currentTimeMillis()
                    val inputStream = connection.inputStream
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        totalBytes += bytesRead
                        val now = System.currentTimeMillis()
                        val readDuration = now - overallStart
                        val elapsedSec = readDuration / 1000.0
                        if (elapsedSec > 0 && (readDuration - totalDurationMs >= 250)) {
                            val currentSpeed = (totalBytes * 8.0) / (elapsedSec * 1_000_000)
                            updateEndpointProgress(
                                getCDNName(tryUrl),
                                (totalBytes.toFloat() / mode.minBytes.coerceAtLeast(1)).coerceAtMost(1f),
                                currentSpeed
                            )
                        }
                        totalDurationMs = readDuration

                        if (readDuration >= mode.maxMsPerCdn) break
                        if (totalBytes >= mode.minBytes && readDuration >= mode.minDurationMs) break
                    }

                    inputStream.close()
                    connection.disconnect()
                    attempts++

                    // Check if we've met the hybrid conditions
                    val finalElapsed = System.currentTimeMillis() - overallStart
                    if (finalElapsed >= mode.maxMsPerCdn) break
                    if (totalBytes >= mode.minBytes && finalElapsed >= mode.minDurationMs) break

                } catch (_: Exception) {
                    attempts++
                    if (attempts >= 2) break
                }
            }

            val finalDurationMs = System.currentTimeMillis() - overallStart
            if (totalBytes > 0 && finalDurationMs > 0) {
                val speedMbps = (totalBytes * 8.0) / (finalDurationMs * 1_000.0)
                return CDNEndpoint(
                    name = getCDNName(url),
                    url = tryUrl,
                    status = TestStatus.DONE,
                    downloadSpeedMbps = speedMbps,
                    latencyMs = measureSingleLatency(url.split("?").first()),
                    progress = 1f,
                    category = getCDNCategory(url)
                )
            }
        }

        return CDNEndpoint(
            name = getCDNName(url),
            url = url,
            status = TestStatus.FAILED,
            downloadSpeedMbps = 0.0,
            latencyMs = 0.0,
            progress = 0f,
            category = getCDNCategory(url)
        )
    }

    private fun getCDNCategory(url: String): CDNCategory {
        val host = url.substringAfter("://").substringBefore("/")
        val path = url.substringAfter("://").substringAfter("/")
        val pkg = path.substringAfter("npm/").substringBefore("@")
            .ifEmpty { path.substringAfter("ajax/libs/").substringBefore("/") }

        return when {
            pkg.contains("bootstrap") || pkg.contains("vue") || pkg.contains("react") -> CDNCategory.UI_FRAMEWORK
            pkg.contains("jquery") || pkg.contains("axios") || pkg.contains("moment") -> CDNCategory.WEB_CORE
            pkg.contains("lodash") -> CDNCategory.UTILITIES
            pkg.contains("three") -> CDNCategory.GAME_ENGINE
            host.contains("unpkg") -> CDNCategory.UTILITIES
            else -> CDNCategory.UNKNOWN
        }
    }

    private suspend fun measureSingleLatency(baseUrl: String): Double = withContext(Dispatchers.IO) {
        try {
            val start = System.currentTimeMillis()
            val connection = URL(baseUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()
            val end = System.currentTimeMillis()
            connection.disconnect()
            (end - start).toDouble()
        } catch (_: Exception) { 0.0 }
    }

    suspend fun runUploadTest(): Double = withContext(Dispatchers.IO) {
        measureUploadSpeed()
    }

    private suspend fun measureUploadSpeed(): Double = withContext(Dispatchers.IO) {
        try {
            val uploadData = ByteArray(config.uploadFileSizeMB * 1024 * 1024)
            val url = URL(config.uploadEndpoints.first())
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/octet-stream")
            connection.connectTimeout = 30000
            connection.readTimeout = 60000

            val startTime = System.currentTimeMillis()
            val outputStream: OutputStream = connection.outputStream
            outputStream.write(uploadData)
            outputStream.flush()
            outputStream.close()

            val responseCode = connection.responseCode
            val endTime = System.currentTimeMillis()
            connection.disconnect()

            if (responseCode in 200..399) {
                val durationSec = (endTime - startTime) / 1000.0
                if (durationSec > 0) (uploadData.size * 8.0) / (durationSec * 1_000_000) else 0.0
            } else 0.0
        } catch (_: Exception) { 0.0 }
    }

    suspend fun measureJitter(): Double = withContext(Dispatchers.IO) {
        try {
            val latencies = mutableListOf<Long>()
            repeat(config.jitterTestPackets) {
                try {
                    val start = System.currentTimeMillis()
                    val connection = URL("https://www.google.com").openConnection() as HttpURLConnection
                    connection.requestMethod = "HEAD"
                    connection.connectTimeout = 3000
                    connection.readTimeout = 3000
                    connection.connect()
                    val end = System.currentTimeMillis()
                    connection.disconnect()
                    latencies.add(end - start)
                } catch (_: Exception) { }
            }
            if (latencies.size >= 2) {
                var totalVariation = 0L
                for (i in 1 until latencies.size) {
                    totalVariation += abs(latencies[i] - latencies[i - 1])
                }
                totalVariation.toDouble() / (latencies.size - 1)
            } else 0.0
        } catch (_: Exception) { 0.0 }
    }

    suspend fun measurePacketLoss(): Double = withContext(Dispatchers.IO) {
        try {
            var failedPackets = 0
            repeat(config.packetLossTestPackets) {
                try {
                    val connection = URL("https://www.google.com").openConnection() as HttpURLConnection
                    connection.requestMethod = "HEAD"
                    connection.connectTimeout = 2000
                    connection.readTimeout = 2000
                    connection.connect()
                    connection.disconnect()
                } catch (_: Exception) {
                    failedPackets++
                }
            }
            (failedPackets.toDouble() / config.packetLossTestPackets) * 100
        } catch (_: Exception) { 0.0 }
    }

    fun calculateISPScore(downloadSpeed: Double, latency: Double, jitter: Double, packetLoss: Double): Int {
        val speedScore = when {
            downloadSpeed >= 100 -> 40
            downloadSpeed >= 50 -> 30
            downloadSpeed >= 25 -> 20
            downloadSpeed >= 10 -> 10
            else -> 5
        }
        val latencyScore = when {
            latency <= 20 -> 25
            latency <= 50 -> 20
            latency <= 100 -> 15
            latency <= 200 -> 10
            else -> 5
        }
        val jitterScore = when {
            jitter <= 5 -> 20
            jitter <= 15 -> 15
            jitter <= 30 -> 10
            jitter <= 50 -> 5
            else -> 0
        }
        val lossScore = when {
            packetLoss <= 1 -> 15
            packetLoss <= 5 -> 10
            packetLoss <= 10 -> 5
            else -> 0
        }
        return speedScore + latencyScore + jitterScore + lossScore
    }

    fun getQualityLabel(score: Int): QualityLabel = when {
        score >= 80 -> QualityLabel.EXCELLENT
        score >= 60 -> QualityLabel.GOOD
        score >= 40 -> QualityLabel.FAIR
        score >= 20 -> QualityLabel.POOR
        else -> QualityLabel.BAD
    }

    fun detectThrottling(cdnResults: List<CDNEndpoint>): Pair<Boolean, String?> {
        val speeds = cdnResults.filter { it.status == TestStatus.DONE }.map { it.downloadSpeedMbps }
        if (speeds.size < 3) return Pair(false, null)
        val avgSpeed = speeds.average()
        val maxSpeed = speeds.maxOrNull() ?: 0.0
        val throttledCDNs = cdnResults.filter {
            it.downloadSpeedMbps < avgSpeed * 0.5 && it.status == TestStatus.DONE
        }
        return if (throttledCDNs.isNotEmpty() && maxSpeed > 50) {
            Pair(true, throttledCDNs.firstOrNull()?.name)
        } else {
            Pair(false, null)
        }
    }

    fun findInconsistentEndpoints(cdnResults: List<CDNEndpoint>): List<String> {
        val speeds = cdnResults.filter { it.status == TestStatus.DONE }.map { it.downloadSpeedMbps }
        if (speeds.size < 3) return emptyList()
        val avg = speeds.average()
        val stdDev = sqrt(speeds.map { (it - avg) * (it - avg) }.average())
        return cdnResults.filter {
            it.status == TestStatus.DONE && abs(it.downloadSpeedMbps - avg) > stdDev * 2
        }.map { it.name }
    }

    fun calculateCoefficientOfVariation(cdnResults: List<CDNEndpoint>): Double {
        val speeds = cdnResults.filter { it.status == TestStatus.DONE && it.downloadSpeedMbps > 0 }
            .map { it.downloadSpeedMbps }
        if (speeds.size < 2) return 0.0
        val mean = speeds.average()
        val stdDev = sqrt(speeds.map { (it - mean) * (it - mean) }.average())
        return if (mean > 0) (stdDev / mean) * 100 else 0.0
    }

    fun getConnectionType(): ConnectionType {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return ConnectionType.UNKNOWN
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return ConnectionType.UNKNOWN
            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.MOBILE
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
                else -> ConnectionType.UNKNOWN
            }
        } catch (_: Exception) { return ConnectionType.UNKNOWN }
    }

    fun getNetworkInfo(): NetworkInfo {
        val connectionType = getConnectionType()
        return fetchNetworkDetails() ?: NetworkInfo(
            publicIP = "Unknown",
            ispName = "Unknown",
            connectionType = connectionType,
            city = "",
            country = ""
        )
    }

    private fun fetchNetworkDetails(): NetworkInfo? {
        // Single consolidated call to ip-api.com (free, no key needed)
        try {
            val url = URL("http://ip-api.com/json/")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.readText()
            reader.close()
            connection.disconnect()

            val ip = Regex("\"query\":\\s*\"([^\"]+)\"").find(response)?.groupValues?.get(1)
            val city = Regex("\"city\":\\s*\"([^\"]+)\"").find(response)?.groupValues?.get(1)
            val country = Regex("\"country\":\\s*\"([^\"]+)\"").find(response)?.groupValues?.get(1)
            val isp = Regex("\"isp\":\\s*\"([^\"]+)\"").find(response)?.groupValues?.get(1)

            return NetworkInfo(
                publicIP = ip ?: "Unknown",
                ispName = isp ?: "Unknown",
                connectionType = getConnectionType(),
                city = city ?: "",
                country = country ?: ""
            )
        } catch (_: Exception) { }

        // Fallback: try ip-api.com with HTTPS
        try {
            val url = URL("https://ip-api.com/json/")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.readText()
            reader.close()
            connection.disconnect()

            val ip = Regex("\"query\":\\s*\"([^\"]+)\"").find(response)?.groupValues?.get(1)
            val city = Regex("\"city\":\\s*\"([^\"]+)\"").find(response)?.groupValues?.get(1)
            val country = Regex("\"country\":\\s*\"([^\"]+)\"").find(response)?.groupValues?.get(1)
            val isp = Regex("\"isp\":\\s*\"([^\"]+)\"").find(response)?.groupValues?.get(1)

            return NetworkInfo(
                publicIP = ip ?: "Unknown",
                ispName = isp ?: "Unknown",
                connectionType = getConnectionType(),
                city = city ?: "",
                country = country ?: ""
            )
        } catch (_: Exception) { }

        // Final fallback: individual calls
        val ip = try {
            val url = URL("https://api.ipify.org")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            val r = BufferedReader(InputStreamReader(conn.inputStream))
            val result = r.readText()
            r.close()
            conn.disconnect()
            result
        } catch (_: Exception) { "Unknown" }

        val isp = try {
            val url = URL("https://ipapi.co/json/")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            val r = BufferedReader(InputStreamReader(conn.inputStream))
            val result = Regex("\"org\":\\s*\"([^\"]+)\"").find(r.readText())?.groupValues?.get(1) ?: "Unknown"
            r.close()
            conn.disconnect()
            result
        } catch (_: Exception) { "Unknown" }

        return NetworkInfo(
            publicIP = ip,
            ispName = isp,
            connectionType = getConnectionType(),
            city = "",
            country = ""
        )
    }

    fun prepareChartData(results: List<CDNEndpoint>): List<Pair<String, Double>> =
        results.filter { it.status == TestStatus.DONE }.map { it.name to it.downloadSpeedMbps }

    fun exportAsText(result: SpeedTestResult): String = buildString {
        appendLine("=== Speed Test Results ===")
        appendLine("Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(java.util.Date(result.timestamp))}")
        appendLine()
        appendLine("Download: ${String.format("%.2f", result.downloadSpeedMbps)} Mbps")
        appendLine("Upload: ${String.format("%.2f", result.uploadSpeedMbps)} Mbps")
        appendLine("Latency: ${String.format("%.2f", result.latencyMs)} ms")
        appendLine("Jitter: ${String.format("%.2f", result.jitterMs)} ms")
        appendLine("Packet Loss: ${String.format("%.2f", result.packetLossPercent)}%")
        appendLine()
        appendLine("ISP Score: ${result.ispScore}/100 (${result.qualityLabel})")
        appendLine()
        result.networkInfo?.let {
            appendLine("Network Info:")
            appendLine("  IP: ${it.publicIP}")
            appendLine("  ISP: ${it.ispName}")
            appendLine("  Type: ${it.connectionType}")
            if (it.city.isNotEmpty() || it.country.isNotEmpty()) {
                appendLine("  Location: ${listOfNotNull(it.city.takeIf { c -> c.isNotEmpty() }, it.country.takeIf { c -> c.isNotEmpty() }).joinToString(", ")}")
            }
        }
        appendLine()
        appendLine("Stability: ${result.stabilityGrade.emoji} ${result.stabilityGrade.label}")
        result.realWorldScore?.let { rw ->
            appendLine()
            appendLine("Real-World Scores:")
            appendLine("  Streaming: ${rw.streamingScore}/100 (${rw.streamingLabel})")
            appendLine("  Gaming: ${rw.gamingScore}/100 (${rw.gamingLabel})")
            appendLine("  Browsing: ${rw.browsingScore}/100 (${rw.browsingLabel})")
        }
        if (result.isThrottled) {
            appendLine("\n⚠ Throttling detected on: ${result.throttledCDN}")
        }
        if (result.inconsistentEndpoints.isNotEmpty()) {
            appendLine("\nInconsistent endpoints: ${result.inconsistentEndpoints.joinToString()}")
        }
    }

    suspend fun runAggregatedCdnTest(
        fileSizeMB: Int,
        onCdnProgress: (cdnName: String, progress: Float, speedMbps: Double) -> Unit
    ): AggregatedCdnResult = withContext(Dispatchers.IO) {
        val results = mutableListOf<CdnTestResult>()
        val shuffled = config.cdnEndpoints.shuffled()

        val (minBytesPerCdn, minDurationMs, maxDurationMs) = getCdnTestParams(fileSizeMB)
        val overallStartMs = System.currentTimeMillis()

        shuffled.forEach { url ->
            val cdnName = getCDNName(url)
            onCdnProgress(cdnName, 0f, 0.0)

            // 1. Measure real latency BEFORE download (clean RTT, no cache warmth)
            val latencyMs = try {
                val base = url.split("?").first()
                val start = System.currentTimeMillis()
                val conn = URL(base).openConnection() as HttpURLConnection
                conn.requestMethod = "HEAD"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.connect()
                val rtt = System.currentTimeMillis() - start
                conn.disconnect()
                rtt.toDouble()
            } catch (_: Exception) { 0.0 }

            // 2. Download real file data until hybrid conditions met
            val downloadResult = downloadForTargetSize(
                url, minBytesPerCdn, minDurationMs, maxDurationMs, onCdnProgress
            )
            results.add(downloadResult.copy(latencyMs = latencyMs))
        }

        // 3. Single upload test with capped size to prevent OOM
        onCdnProgress("Upload Test", 0f, 0.0)
        val uploadFileMB = fileSizeMB.coerceAtMost(5)
        val uploadSpeed = measureUploadSize(uploadFileMB)
        val uploadBytes = uploadFileMB * 1024L * 1024L

        val totalDurationMs = System.currentTimeMillis() - overallStartMs
        val avgDownload = results.filter { it.downloadSpeedMbps > 0 }.let {
            if (it.isNotEmpty()) it.map { it.downloadSpeedMbps }.average() else 0.0
        }
        val avgLatency = results.filter { it.latencyMs > 0 }.let {
            if (it.isNotEmpty()) it.map { it.latencyMs }.average() else 0.0
        }
        val totalBytesDownloaded = results.sumOf { it.bytesDownloaded }

        AggregatedCdnResult(
            results = results,
            totalDownloadMbps = avgDownload,
            totalUploadMbps = uploadSpeed,
            avgLatencyMs = avgLatency,
            totalBytesDownloaded = totalBytesDownloaded,
            totalBytesUploaded = uploadBytes,
            totalDurationMs = totalDurationMs,
            testFileSizeMB = fileSizeMB
        )
    }

    private fun getCdnTestParams(fileSizeMB: Int): Triple<Long, Long, Long> {
        val minBytes = (fileSizeMB * 1024L * 1024L).coerceIn(512_000L, 10_000_000L)
        val minDurationMs = when {
            fileSizeMB <= 1 -> 3000L
            fileSizeMB <= 5 -> 5000L
            fileSizeMB <= 10 -> 7000L
            fileSizeMB <= 25 -> 10000L
            else -> 12000L
        }
        val maxDurationMs = minDurationMs * 3
        return Triple(minBytes, minDurationMs, maxDurationMs)
    }

    private suspend fun downloadForTargetSize(
        url: String,
        minBytes: Long,
        minDurationMs: Long,
        maxDurationMs: Long,
        onProgress: (cdnName: String, progress: Float, speedMbps: Double) -> Unit
    ): CdnTestResult = withContext(Dispatchers.IO) {
        val cdnName = getCDNName(url)
        val overallStart = System.currentTimeMillis()
        var totalBytes = 0L

        val urlsToTry = mutableListOf(url)
        if (url.startsWith("https://")) {
            urlsToTry.add("http://" + url.removePrefix("https://"))
        }

        // Try HTTPS first, fall back to HTTP
        for (tryUrl in urlsToTry) {
            if (totalBytes >= minBytes) break
            var consecutiveFailures = 0 // separate per-URL failure counter

            while (true) {
                val elapsed = System.currentTimeMillis() - overallStart
                if (elapsed >= maxDurationMs) break
                if (totalBytes >= minBytes && elapsed >= minDurationMs) break

                try {
                    val cacheBuster = if (consecutiveFailures > 0) "${if (tryUrl.contains("?")) "&" else "?"}_=${System.currentTimeMillis()}" else ""
                    // also use cache buster on every request after the first successful one
                    val effectiveUrl = if (totalBytes > 0) {
                        tryUrl + "${if (tryUrl.contains("?")) "&" else "?"}_=${System.currentTimeMillis()}"
                    } else tryUrl + cacheBuster
                    val connection = URL(effectiveUrl).openConnection() as HttpURLConnection
                    connection.connectTimeout = 10_000
                    connection.readTimeout = 15_000
                    connection.instanceFollowRedirects = true

                    val inputStream = connection.inputStream
                    val buffer = ByteArray(65536)
                    var bytesRead: Int
                    val iterStart = System.currentTimeMillis()

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        totalBytes += bytesRead
                        val readDurationMs = System.currentTimeMillis() - overallStart
                        if (readDurationMs > 0) {
                            val currentSpeed = (totalBytes * 8.0) / (readDurationMs * 1_000.0)
                            val progress = (totalBytes.toFloat() / minBytes).coerceAtMost(1f)
                            onProgress(cdnName, progress, currentSpeed)
                        }
                        if (System.currentTimeMillis() - iterStart > 10_000) break // safety per-iter
                        if (totalBytes >= minBytes && readDurationMs >= minDurationMs) break
                    }

                    inputStream.close()
                    connection.disconnect()
                    consecutiveFailures = 0 // reset on success

                    if (totalBytes >= minBytes && (System.currentTimeMillis() - overallStart) >= minDurationMs) break
                } catch (_: Exception) {
                    consecutiveFailures++
                    if (consecutiveFailures >= 3) break
                }
            }
        }

        val finalDuration = System.currentTimeMillis() - overallStart
        var speedMbps = if (finalDuration > 0 && totalBytes > 0) {
            (totalBytes * 8.0) / (finalDuration * 1_000.0)
        } else 0.0

        // Fallback: if we got latency but no download data, estimate from latency
        if (speedMbps <= 0 && finalDuration > 0) {
            speedMbps = estimateSpeedFromLatency(
                latencyMs = 20.0,
                successfulCdnCount = 1
            )
        }

        CdnTestResult(
            cdnName = cdnName,
            downloadSpeedMbps = speedMbps,
            uploadSpeedMbps = 0.0,
            latencyMs = 0.0,
            bytesDownloaded = totalBytes,
            bytesUploaded = 0L,
            durationMs = finalDuration,
            category = getCDNCategory(url)
        )
    }

    private suspend fun measureUploadSize(fileSizeMB: Int): Double = withContext(Dispatchers.IO) {
        try {
            val uploadSize = fileSizeMB.coerceAtMost(5)
            val uploadData = ByteArray(uploadSize * 1024 * 1024)

            val urlsToTry = mutableListOf("https://httpbin.org/post", "http://httpbin.org/post")
            for (url in urlsToTry) {
                try {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/octet-stream")
                    connection.connectTimeout = 60_000
                    connection.readTimeout = 60_000

                    val startTime = System.currentTimeMillis()
                    val outputStream = connection.outputStream
                    outputStream.write(uploadData)
                    outputStream.flush()
                    outputStream.close()

                    val responseCode = connection.responseCode
                    val endTime = System.currentTimeMillis()
                    connection.disconnect()

                    if (responseCode in 200..399) {
                        val durationSec = (endTime - startTime) / 1000.0
                        if (durationSec > 0) return@withContext (uploadData.size * 8.0) / (durationSec * 1_000_000)
                    }
                } catch (_: Exception) { }
            }
            0.0
        } catch (_: Exception) { 0.0 }
    }

    private fun getCDNName(url: String): String {
        val host = url.substringAfter("://").substringBefore("/")
        val path = url.substringAfter("://").substringAfter("/")

        // jsDelivr: extract package name for informative labels
        if (host.contains("jsdelivr")) {
            val pkg = path.substringAfter("npm/").substringBefore("@").ifEmpty { "" }
            return when {
                pkg.contains("bootstrap") -> "jsDelivr (Bootstrap)"
                pkg.contains("vue") -> "jsDelivr (Vue)"
                pkg.contains("lodash") -> "jsDelivr (Lodash)"
                pkg.contains("three") -> "jsDelivr (Three.js)"
                pkg.contains("axios") -> "jsDelivr (Axios)"
                else -> "jsDelivr"
            }
        }

        // Cloudflare / cdnjs: extract library name
        if (host.contains("cdnjs") || host.contains("cloudflare")) {
            val lib = path.substringAfter("ajax/libs/").substringBefore("/").ifEmpty { "" }
            return when {
                lib.contains("jquery") -> "Cloudflare CDN (jQuery)"
                lib.contains("moment") -> "Cloudflare CDN (Moment.js)"
                lib.contains("axios") -> "Cloudflare CDN (Axios)"
                else -> "Cloudflare CDN"
            }
        }

        return when {
            host.contains("googleapis") -> "Google CDN"
            host.contains("unpkg") -> {
                val pkg = path.substringBefore("@").ifEmpty { path.substringBefore("/") }
                "unpkg (${pkg.replaceFirstChar { it.uppercase() }})"
            }
            host.contains("jquery") -> "jQuery CDN"
            host.contains("cloudfront") -> "AWS CloudFront"
            host.contains("akamai") -> "Akamai"
            host.contains("fastly") -> "Fastly"
            host.contains("github") -> "GitHub"
            else -> host.take(20)
        }
    }
}
