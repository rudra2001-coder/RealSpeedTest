package com.rudra.realspeedtest.data.model

import java.util.UUID

enum class TestMode(
    val label: String,
    val description: String,
    val maxMsPerCdn: Int,
    val minDurationMs: Int,
    val minBytes: Long,
    val accuracyLabel: String
) {
    QUICK("Quick", "Fast check", 3000, 1500, 512_000, "⚡ Fast, lower accuracy"),
    NORMAL("Normal", "Balanced", 6000, 3000, 2_000_000, "⚖️ Good balance"),
    THOROUGH("Thorough", "Max precision", 10000, 5000, 5_000_000, "🎯 High precision")
}

enum class CDNCategory(val label: String) {
    UI_FRAMEWORK("UI Framework"),
    WEB_CORE("Web Core"),
    UTILITIES("Utilities"),
    DATA_LAYER("Data Layer"),
    GAME_ENGINE("Game Engine"),
    UNKNOWN("Other")
}

enum class StabilityGrade(val label: String, val emoji: String) {
    ROCK_SOLID("Rock Solid", "🪨"),
    STABLE("Stable", "✅"),
    MODERATE("Moderate", "⚡"),
    UNSTABLE("Unstable", "⚠️"),
    VERY_UNSTABLE("Very Unstable", "🔴")
}

data class RealWorldScore(
    val streamingScore: Int,
    val gamingScore: Int,
    val browsingScore: Int,
    val streamingLabel: String,
    val gamingLabel: String,
    val browsingLabel: String
)

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
    val category: CDNCategory = CDNCategory.UNKNOWN,
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
    val speedVariationPercent: Double = 0.0,
    val stabilityGrade: StabilityGrade = StabilityGrade.MODERATE,
    val realWorldScore: RealWorldScore? = null
) {
    val streamingStats: Triple<Int, String, String> get() {
        val score = when {
            downloadSpeedMbps >= 50 && jitterMs <= 10 -> 95
            downloadSpeedMbps >= 25 && jitterMs <= 15 -> 85
            downloadSpeedMbps >= 15 && jitterMs <= 20 -> 70
            downloadSpeedMbps >= 8 && jitterMs <= 30 -> 50
            downloadSpeedMbps >= 4 -> 30
            else -> 15
        }
        val label = when {
            score >= 80 -> "Excellent"
            score >= 60 -> "Good"
            score >= 40 -> "Fair"
            else -> "Poor"
        }
        return Triple(score, label, when {
            score >= 80 -> "4K HDR ready"
            score >= 60 -> "1080p stable"
            score >= 40 -> "720p OK"
            else -> "Buffering likely"
        })
    }

    val gamingStats: Triple<Int, String, String> get() {
        val score = when {
            latencyMs <= 10 && jitterMs <= 3 && packetLossPercent <= 0.5 -> 95
            latencyMs <= 20 && jitterMs <= 5 && packetLossPercent <= 1 -> 85
            latencyMs <= 40 && jitterMs <= 10 && packetLossPercent <= 2 -> 70
            latencyMs <= 80 && jitterMs <= 20 && packetLossPercent <= 5 -> 50
            latencyMs <= 150 -> 30
            else -> 15
        }
        val label = when {
            score >= 80 -> "Competitive"
            score >= 60 -> "Good"
            score >= 40 -> "Playable"
            else -> "Laggy"
        }
        return Triple(score, label, when {
            score >= 80 -> "eSports ready"
            score >= 60 -> "Casual gaming OK"
            score >= 40 -> "Single player OK"
            else -> "Not recommended"
        })
    }

    val browsingStats: Triple<Int, String, String> get() {
        val score = when {
            downloadSpeedMbps >= 25 && latencyMs <= 30 -> 90
            downloadSpeedMbps >= 10 && latencyMs <= 60 -> 75
            downloadSpeedMbps >= 5 && latencyMs <= 100 -> 60
            downloadSpeedMbps >= 2 -> 40
            else -> 20
        }
        val label = when {
            score >= 80 -> "Instant"
            score >= 60 -> "Fast"
            score >= 40 -> "Adequate"
            else -> "Slow"
        }
        return Triple(score, label, when {
            score >= 80 -> "Pages load instantly"
            score >= 60 -> "Smooth browsing"
            score >= 40 -> "Occasional delays"
            else -> "Images load slowly"
        })
    }
}

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
    val overallSpeedMbps: Double = 0.0,
    val totalBytesDownloaded: Long = 0L
)

enum class TestPhase {
    IDLE, PING_TEST, DOWNLOAD_TEST, UPLOAD_TEST, JITTER_TEST, COMPLETED
}

data class CdnTestResult(
    val cdnName: String,
    val downloadSpeedMbps: Double,
    val uploadSpeedMbps: Double,
    val latencyMs: Double,
    val bytesDownloaded: Long,
    val bytesUploaded: Long,
    val durationMs: Long,
    val category: CDNCategory = CDNCategory.UNKNOWN
)

data class AggregatedCdnResult(
    val results: List<CdnTestResult>,
    val totalDownloadMbps: Double,
    val totalUploadMbps: Double,
    val avgLatencyMs: Double,
    val totalBytesDownloaded: Long,
    val totalBytesUploaded: Long,
    val totalDurationMs: Long,
    val testFileSizeMB: Int
)

data class NetworkInfo(
    val publicIP: String = "Unknown",
    val ispName: String = "Unknown",
    val connectionType: ConnectionType = ConnectionType.UNKNOWN,
    val city: String = "",
    val country: String = ""
)

