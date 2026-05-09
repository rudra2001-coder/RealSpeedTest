package com.rudra.realspeedtest.data.model

import java.util.UUID

enum class TestMode(val label: String, val description: String, val maxMsPerCdn: Int) {
    QUICK("Quick", "~15s — Standard test", 3000),
    NORMAL("Normal", "~30s — More data per CDN", 6000),
    THOROUGH("Thorough", "~60s — Maximum accuracy", 10000)
}

data class SpeedTestConfig(
    val testMode: TestMode = TestMode.QUICK,
    val downloadFileSizeMB: Int = 10,
    val uploadFileSizeMB: Int = 5,
    val testDurationSeconds: Int = 10,
    val cdnEndpoints: List<String> = listOf(
        "https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css",
        "https://cdnjs.cloudflare.com/ajax/libs/jquery/3.7.1/jquery.min.js",
        "https://ajax.googleapis.com/ajax/libs/jquery/3.7.1/jquery.min.js",
        "https://unpkg.com/react@18/umd/react.production.min.js",
        "https://code.jquery.com/jquery-3.7.1.min.js",
        "https://cdn.jsdelivr.net/npm/vue@3/dist/vue.global.prod.js",
        "https://cdn.jsdelivr.net/npm/lodash@4.17.21/lodash.min.js",
        "https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.29.4/moment.min.js",
        "https://cdn.jsdelivr.net/npm/three@0.160.0/build/three.min.js",
        "https://cdn.jsdelivr.net/npm/axios@1.6.0/dist/axios.min.js"
    ),
    val uploadEndpoints: List<String> = listOf(
        "https://httpbin.org/post",
        "https://httpbin.org/upload"
    ),
    val jitterTestPackets: Int = 20,
    val packetLossTestPackets: Int = 50,
    val multiThreadCount: Int = 4
) {
    val maxMsPerCdn: Int get() = testMode.maxMsPerCdn
}

data class CDNEndpoint(
    val name: String,
    val url: String,
    val status: TestStatus = TestStatus.PENDING,
    val downloadSpeedMbps: Double = 0.0,
    val uploadSpeedMbps: Double = 0.0,
    val latencyMs: Double = 0.0,
    val progress: Float = 0f,
    val jitterMs: Double = 0.0,
    val packetLossPercent: Double = 0.0,
    val threadResults: List<ThreadResult> = emptyList()
)

data class ThreadResult(
    val threadId: Int,
    val speedMbps: Double,
    val bytesDownloaded: Long,
    val durationMs: Long
)

data class SpeedTestResult(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val downloadSpeedMbps: Double = 0.0,
    val uploadSpeedMbps: Double = 0.0,
    val latencyMs: Double = 0.0,
    val jitterMs: Double = 0.0,
    val packetLossPercent: Double = 0.0,
    val ispScore: Int = 0,
    val qualityLabel: QualityLabel = QualityLabel.UNKNOWN,
    val cdnResults: List<CDNEndpoint> = emptyList(),
    val networkInfo: NetworkInfo? = null,
    val isThrottled: Boolean = false,
    val throttledCDN: String? = null,
    val inconsistentEndpoints: List<String> = emptyList(),
    val suspiciousCDNs: List<String> = emptyList(),
    val speedVariationPercent: Double = 0.0 // Coefficient of variation
)

data class NetworkInfo(
    val publicIP: String = "Unknown",
    val ispName: String = "Unknown",
    val connectionType: ConnectionType = ConnectionType.UNKNOWN,
    val city: String = "",
    val country: String = ""
)

enum class ConnectionType {
    WIFI, MOBILE, ETHERNET, UNKNOWN
}

enum class TestStatus {
    PENDING, RUNNING, TESTING, DONE, FAILED
}

enum class QualityLabel {
    EXCELLENT, GOOD, FAIR, POOR, BAD, UNKNOWN
}

data class TestProgress(
    val currentCDN: String = "",
    val currentCDNIndex: Int = 0,
    val totalCDNs: Int = 0,
    val progress: Float = 0f,
    val phase: TestPhase = TestPhase.IDLE,
    val currentSpeedMbps: Double = 0.0,
    val overallSpeedMbps: Double = 0.0
)

enum class TestPhase {
    IDLE, PING_TEST, DOWNLOAD_TEST, UPLOAD_TEST, JITTER_TEST, COMPLETED
}