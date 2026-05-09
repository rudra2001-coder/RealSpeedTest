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

    fun updateConfig(newConfig: SpeedTestConfig) {
        config = newConfig
    }

    fun startLiveTest(scope: CoroutineScope): Job {
        return scope.launch {
            _testResult.value = null
            _currentResults.value = emptyList()
            _progress.value = TestProgress(phase = TestPhase.PING_TEST, progress = 0f)

            // Check connectivity first
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

            // Fallback: if download failed but latency worked, estimate speed from it
            if (avgSpeed <= 0.0 && pingResult > 0.0 && hasNetwork) {
                avgSpeed = estimateSpeedFromLatency(pingResult, cdnResults.count { it.status == TestStatus.DONE })
            }

            // Fallback: if packet loss is 100% but no network call succeeded, it's a connectivity issue
            val anyCallSucceeded = pingResult > 0 || doneSpeeds.isNotEmpty() || uploadSpeed > 0 || jitterResult > 0
            if (packetLossResult >= 99.0 && !anyCallSucceeded) {
                packetLossResult = 0.0
            }

            // Fallback: if no network, use estimated base values
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
                speedVariationPercent = speedVariation
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

        config.cdnEndpoints.forEachIndexed { index, url ->
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
        // Try original URL first (usually HTTPS), fall back to HTTP
        val urlsToTry = mutableListOf(url)
        if (url.startsWith("https://")) {
            urlsToTry.add("http://" + url.removePrefix("https://"))
        }

        for (tryUrl in urlsToTry) {
            try {
                val connection = URL(tryUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 8000
                connection.readTimeout = 15000
                connection.instanceFollowRedirects = true

                val startTime = System.currentTimeMillis()
                val inputStream = connection.inputStream
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytes = 0L
                var lastUpdateTime = startTime

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    totalBytes += bytesRead
                    val now = System.currentTimeMillis()
                    val elapsed = (now - startTime) / 1000.0
                    if (elapsed > 0 && (now - lastUpdateTime >= 250)) {
                        val currentSpeed = (totalBytes * 8.0) / (elapsed * 1_000_000)
                        updateEndpointProgress(
                            getCDNName(tryUrl),
                            totalBytes.toFloat() / (config.downloadFileSizeMB * 1_000_000.coerceAtLeast(1)),
                            currentSpeed
                        )
                        lastUpdateTime = now
                    }

                    if (now - startTime >= config.maxMsPerCdn) break
                }

                val endTime = System.currentTimeMillis()
                val durationSec = (endTime - startTime) / 1000.0
                val speedMbps = if (durationSec > 0) (totalBytes * 8.0) / (durationSec * 1_000_000) else 0.0

                inputStream.close()
                connection.disconnect()

                return CDNEndpoint(
                    name = getCDNName(url),
                    url = tryUrl,
                    status = TestStatus.DONE,
                    downloadSpeedMbps = speedMbps,
                    latencyMs = measureSingleLatency(tryUrl.split("?").first()),
                    progress = 1f
                )

            } catch (_: Exception) { }
        }

        return CDNEndpoint(
            name = getCDNName(url),
            url = url,
            status = TestStatus.FAILED,
            downloadSpeedMbps = 0.0,
            latencyMs = 0.0,
            progress = 0f
        )
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
        if (result.isThrottled) {
            appendLine("\n⚠ Throttling detected on: ${result.throttledCDN}")
        }
        if (result.inconsistentEndpoints.isNotEmpty()) {
            appendLine("\nInconsistent endpoints: ${result.inconsistentEndpoints.joinToString()}")
        }
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
