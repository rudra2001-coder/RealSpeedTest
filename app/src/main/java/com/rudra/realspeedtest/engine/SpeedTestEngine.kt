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
import java.net.InetAddress
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.HttpsURLConnection

class SpeedTestEngine(private val context: Context) {

    private val _progress = MutableStateFlow(TestProgress())
    val progress: StateFlow<TestProgress> = _progress.asStateFlow()

    private val _currentResults = MutableStateFlow<List<CDNEndpoint>>(emptyList())
    val currentResults: StateFlow<List<CDNEndpoint>> = _currentResults.asStateFlow()

    private var config = SpeedTestConfig()

    fun updateConfig(newConfig: SpeedTestConfig) {
        config = newConfig
    }

    fun startLiveTest(scope: CoroutineScope): Job {
        return scope.launch {
            _progress.value = TestProgress(phase = TestPhase.PING_TEST)

            val pingResult = measureLatency()
            _progress.value = _progress.value.copy(
                phase = TestPhase.DOWNLOAD_TEST,
                currentCDNIndex = 0,
                totalCDNs = config.cdnEndpoints.size
            )

            val cdnResults = testMultipleCDNs(scope)
            _currentResults.value = cdnResults

            _progress.value = _progress.value.copy(phase = TestPhase.UPLOAD_TEST)
            val uploadSpeed = measureUploadSpeed(scope)

            _progress.value = _progress.value.copy(phase = TestPhase.JITTER_TEST)
            val jitterResult = measureJitter()
            val packetLossResult = measurePacketLoss()

            _progress.value = _progress.value.copy(phase = TestPhase.COMPLETED, progress = 1f)

            val avgSpeed = cdnResults.filter { it.status == TestStatus.DONE }
                .map { it.downloadSpeedMbps }.average().coerceAtLeast(0.0)

            val ispScore = calculateISPScore(avgSpeed, pingResult, jitterResult, packetLossResult)
            val qualityLabel = getQualityLabel(ispScore)

            val throttling = detectThrottling(cdnResults)
            val inconsistent = findInconsistentEndpoints(cdnResults)
            val suspicious = flagSuspiciousCDN(cdnResults)
            val speedVariation = calculateCoefficientOfVariation(cdnResults)

            val finalResult = SpeedTestResult(
                downloadSpeedMbps = avgSpeed,
                uploadSpeedMbps = uploadSpeed,
                latencyMs = pingResult,
                jitterMs = jitterResult,
                packetLossPercent = packetLossResult,
                ispScore = ispScore,
                qualityLabel = qualityLabel,
                cdnResults = cdnResults,
                networkInfo = getNetworkInfo(),
                isThrottled = throttling.first,
                throttledCDN = throttling.second,
                inconsistentEndpoints = inconsistent,
                suspiciousCDNs = suspicious,
                speedVariationPercent = speedVariation
            )

            _progress.value = _progress.value.copy(
                phase = TestPhase.COMPLETED,
                overallSpeedMbps = avgSpeed
            )
        }
    }

    fun updateEndpointProgress(endpointName: String, progress: Float, speed: Double) {
        val current = _progress.value
        _progress.value = current.copy(
            currentCDN = endpointName,
            progress = (current.currentCDNIndex + progress) / current.totalCDNs,
            currentSpeedMbps = speed
        )
    }

    fun observeLiveResults(): StateFlow<List<CDNEndpoint>> = currentResults

    suspend fun measureLatency(): Double = withContext(Dispatchers.IO) {
        try {
            val times = mutableListOf<Long>()
            repeat(5) {
                val start = System.currentTimeMillis()
                val url = URL("https://www.google.com")
                val connection = url.openConnection() as HttpsURLConnection
                connection.requestMethod = "HEAD"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.connect()
                val end = System.currentTimeMillis()
if (connection.responseCode in 200..399) {
                    times.add(end - start)
                }
                connection.disconnect()
            }
            if (times.isNotEmpty()) times.average() else 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    suspend fun runMultiThreadDownload(
        endpoint: String,
        threadCount: Int,
        fileSizeBytes: Long,
        scope: CoroutineScope
    ): List<ThreadResult> = withContext(Dispatchers.IO) {
        val results = ConcurrentHashMap<Int, ThreadResult>()
        val activeThreads = AtomicInteger(0)
        val bytesPerThread = fileSizeBytes / threadCount

        val jobs = (0 until threadCount).map { threadId ->
            scope.async {
                activeThreads.incrementAndGet()
                try {
                    val url = URL("$endpoint&start=${threadId * bytesPerThread}&end=${(threadId + 1) * bytesPerThread - 1}")
                    val connection = url.openConnection() as HttpsURLConnection
                    connection.connectTimeout = 10000
                    connection.readTimeout = 30000
                    connection.setRequestProperty("Range", "bytes=${threadId * bytesPerThread}-${(threadId + 1) * bytesPerThread - 1}")

                    val startTime = System.currentTimeMillis()
                    val inputStream = connection.inputStream
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytes = 0L
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        totalBytes += bytesRead
                    }
                    val endTime = System.currentTimeMillis()

                    val durationSec = (endTime - startTime) / 1000.0
                    val speedMbps = if (durationSec > 0) (totalBytes * 8.0) / (durationSec * 1_000_000) else 0.0

                    results[threadId] = ThreadResult(
                        threadId = threadId,
                        speedMbps = speedMbps,
                        bytesDownloaded = totalBytes,
                        durationMs = endTime - startTime
                    )
                } catch (e: Exception) {
                    results[threadId] = ThreadResult(threadId = threadId, speedMbps = 0.0, bytesDownloaded = 0, durationMs = 0)
                } finally {
                    activeThreads.decrementAndGet()
                }
            }
        }

        jobs.awaitAll()
        results.values.toList()
    }

    fun mergeThreadResults(results: List<ThreadResult>): Double {
        val totalSpeed = results.sumOf { it.speedMbps }
        return totalSpeed
    }

    private suspend fun testMultipleCDNs(scope: CoroutineScope): List<CDNEndpoint> = withContext(Dispatchers.IO) {
        val results = mutableListOf<CDNEndpoint>()

        config.cdnEndpoints.forEachIndexed { index, url ->
            _progress.value = _progress.value.copy(
                currentCDNIndex = index,
                currentCDN = getCDNName(url)
            )

            try {
                val startTime = System.currentTimeMillis()
                val connection = URL(url).openConnection() as HttpsURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 30000

                val inputStream = connection.inputStream
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytes = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    totalBytes += bytesRead
                    val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                    if (elapsed > 0) {
                        val currentSpeed = (totalBytes * 8.0) / (elapsed * 1_000_000)
                        updateEndpointProgress(getCDNName(url), totalBytes.toFloat() / (config.downloadFileSizeMB * 1_000_000), currentSpeed)
                    }
                }

                val endTime = System.currentTimeMillis()
                val durationSec = (endTime - startTime) / 1000.0
                val speedMbps = if (durationSec > 0) (totalBytes * 8.0) / (durationSec * 1_000_000) else 0.0

                results.add(CDNEndpoint(
                    name = getCDNName(url),
                    url = url,
                    status = TestStatus.DONE,
                    downloadSpeedMbps = speedMbps,
                    latencyMs = measureSingleLatency(url),
                    progress = 1f
                ))

                _currentResults.value = results.toList()

            } catch (e: Exception) {
                results.add(CDNEndpoint(
                    name = getCDNName(url),
                    url = url,
                    status = TestStatus.FAILED,
                    downloadSpeedMbps = 0.0,
                    latencyMs = 0.0,
                    progress = 0f
                ))
            }
        }

        results
    }

    private suspend fun measureSingleLatency(url: String): Double = withContext(Dispatchers.IO) {
        try {
            val start = System.currentTimeMillis()
            val connection = URL(url.split("?").first()).openConnection() as HttpsURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()
            val end = System.currentTimeMillis()
            connection.disconnect()
            (end - start).toDouble()
        } catch (e: Exception) {
            0.0
        }
    }

    suspend fun runUploadTest(): Double = withContext(Dispatchers.IO) {
        measureUploadSpeed(CoroutineScope(Dispatchers.IO))
    }

    suspend fun measureUploadSpeed(scope: CoroutineScope): Double = withContext(Dispatchers.IO) {
        try {
            val uploadData = ByteArray(config.uploadFileSizeMB * 1024 * 1024)
            val url = URL(config.uploadEndpoints.first())
            val connection = url.openConnection() as HttpsURLConnection
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
                if (durationSec > 0) {
                    (uploadData.size * 8.0) / (durationSec * 1_000_000)
                } else 0.0
            } else 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    suspend fun measureJitter(): Double = withContext(Dispatchers.IO) {
        try {
            val latencies = mutableListOf<Long>()
            repeat(config.jitterTestPackets) {
                val start = System.currentTimeMillis()
                val connection = URL("https://www.google.com").openConnection() as HttpsURLConnection
                connection.requestMethod = "HEAD"
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                connection.connect()
                val end = System.currentTimeMillis()
                connection.disconnect()
                latencies.add(end - start)
            }

            if (latencies.size >= 2) {
                var totalVariation = 0L
                for (i in 1 until latencies.size) {
                    totalVariation += kotlin.math.abs(latencies[i] - latencies[i-1])
                }
                totalVariation.toDouble() / (latencies.size - 1)
            } else 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    suspend fun measurePacketLoss(): Double = withContext(Dispatchers.IO) {
        try {
            var failedPackets = 0
            repeat(config.packetLossTestPackets) {
                try {
                    val connection = URL("https://www.google.com").openConnection() as HttpsURLConnection
                    connection.requestMethod = "HEAD"
                    connection.connectTimeout = 2000
                    connection.readTimeout = 2000
                    connection.connect()
                    connection.disconnect()
                } catch (e: Exception) {
                    failedPackets++
                }
            }
            (failedPackets.toDouble() / config.packetLossTestPackets) * 100
        } catch (e: Exception) {
            0.0
        }
    }

    fun calculateISPScore(
        downloadSpeed: Double,
        latency: Double,
        jitter: Double,
        packetLoss: Double
    ): Int {
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
        val stdDev = kotlin.math.sqrt(speeds.map { (it - avg) * (it - avg) }.average())

        return cdnResults.filter {
            it.status == TestStatus.DONE && kotlin.math.abs(it.downloadSpeedMbps - avg) > stdDev * 2
        }.map { it.name }
    }

    fun calculateCoefficientOfVariation(cdnResults: List<CDNEndpoint>): Double {
        val speeds = cdnResults.filter { it.status == TestStatus.DONE && it.downloadSpeedMbps > 0 }
            .map { it.downloadSpeedMbps }
        if (speeds.size < 2) return 0.0

        val mean = speeds.average()
        val stdDev = kotlin.math.sqrt(speeds.map { (it - mean) * (it - mean) }.average())

        return if (mean > 0) (stdDev / mean) * 100 else 0.0
    }

    fun flagSuspiciousCDN(cdnResults: List<CDNEndpoint>): List<String> {
        val failedCDNs = cdnResults.filter { it.status == TestStatus.FAILED }
        val zeroSpeedCDNs = cdnResults.filter { it.status == TestStatus.DONE && it.downloadSpeedMbps < 1 }

        return (failedCDNs.map { it.name } + zeroSpeedCDNs.map { it.name }).distinct()
    }

    fun getPublicIP(): String {
        return try {
            val url = URL("https://api.ipify.org")
            val connection = url.openConnection() as HttpsURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val ip = reader.readText()
            reader.close()
            connection.disconnect()
            ip
        } catch (e: Exception) {
            "Unknown"
        }
    }

    fun getISPInfo(): String {
        return try {
            val url = URL("https://ipapi.co/json/")
            val connection = url.openConnection() as HttpsURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.readText()
            reader.close()
            connection.disconnect()
            val isp = Regex("\"org\":\\s*\"([^\"]+)\"").find(response)?.groupValues?.get(1) ?: "Unknown"
            isp
        } catch (e: Exception) {
            "Unknown"
        }
    }

    fun getConnectionType(): ConnectionType {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return ConnectionType.UNKNOWN
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return ConnectionType.UNKNOWN

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.MOBILE
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
            else -> ConnectionType.UNKNOWN
        }
    }

    fun getNetworkInfo(): NetworkInfo {
        val ip = getPublicIP()
        val isp = getISPInfo()
        val connectionType = getConnectionType()

        return NetworkInfo(
            publicIP = ip,
            ispName = isp,
            connectionType = connectionType
        )
    }

    fun prepareChartData(results: List<CDNEndpoint>): List<Pair<String, Double>> {
        return results.filter { it.status == TestStatus.DONE }
            .map { it.name to it.downloadSpeedMbps }
    }

    fun exportAsText(result: SpeedTestResult): String {
        return buildString {
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
            }
            if (result.isThrottled) {
                appendLine("\n⚠️ Throttling detected on: ${result.throttledCDN}")
            }
            if (result.inconsistentEndpoints.isNotEmpty()) {
                appendLine("\nInconsistent endpoints: ${result.inconsistentEndpoints.joinToString()}")
            }
        }
    }

    private fun getCDNName(url: String): String {
        return when {
            url.contains("cloudflare") -> "Cloudflare"
            url.contains("ovh") -> "OVH"
            url.contains("hetzner") -> "Hetzner"
            url.contains("tele2") -> "Tele2"
            url.contains("t-online") -> "T-Online"
            url.contains("lorem") -> "Lorem.ch"
            url.contains("seasonic") -> "Seasonic"
            url.contains("watson") -> "Watson"
            url.contains("packetloss") -> "PacketLoss"
            else -> url.substringAfter("://").substringBefore("/")
        }
    }
}