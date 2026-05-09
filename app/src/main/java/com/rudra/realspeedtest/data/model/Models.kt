package com.rudra.realspeedtest.data.model

import java.util.UUID

data class SpeedTestConfig(
    val downloadFileSizeMB: Int = 10,
    val uploadFileSizeMB: Int = 5,
    val testDurationSeconds: Int = 10,
    val cdnEndpoints: List<String> = listOf(
        "https://speed.cloudflare.com/__down?bytes=10000000",
        "https://proof.ovh.net/files/10Mb.dat",
        "https://speed.hetzner.de/10MB.bin",
        "http://speedtest.tele2.net/10MB.zip",
        "https://speedtest.t-online.de/10000000",
        "https://lorem.ch/data/10mb.bin",
        "https://speed-ovh.com/10MB",
        "http://mirror.seasonic.se/10MB.bin",
        "http://speedtest.watsonbroadband.com/10MB.txt",
        "https://cdnpacketloss-test.example.com/file"
    ),
    val uploadEndpoints: List<String> = listOf(
        "https://httpbin.org/post",
        "https://speed.cloudflare.com/__up",
        "https://httpbin.org/upload"
    ),
    val jitterTestPackets: Int = 20,
    val packetLossTestPackets: Int = 50,
    val multiThreadCount: Int = 4
)

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