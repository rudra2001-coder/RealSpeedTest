package com.rudra.realspeedtest.repository

import android.content.Context
import android.content.SharedPreferences
import com.rudra.realspeedtest.data.model.SpeedTestResult
import org.json.JSONArray
import org.json.JSONObject

class TestHistoryRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("speed_test_history", Context.MODE_PRIVATE)

    fun saveTestResult(result: SpeedTestResult) {
        val history = getTestHistory().toMutableList()
        history.add(0, result)
        if (history.size > 50) {
            history.removeAt(history.lastIndex)
        }
        saveHistory(history)
    }

    fun getTestHistory(): List<SpeedTestResult> {
        val json = prefs.getString("history", null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(json)
            val results = mutableListOf<SpeedTestResult>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                results.add(parseResult(obj))
            }
            results
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearHistory() {
        prefs.edit().remove("history").apply()
    }

    private fun saveHistory(history: List<SpeedTestResult>) {
        val jsonArray = JSONArray()
        history.forEach { result ->
            jsonArray.put(toJson(result))
        }
        prefs.edit().putString("history", jsonArray.toString()).apply()
    }

    private fun toJson(result: SpeedTestResult): JSONObject {
        return JSONObject().apply {
            put("id", result.id)
            put("timestamp", result.timestamp)
            put("downloadSpeedMbps", result.downloadSpeedMbps)
            put("uploadSpeedMbps", result.uploadSpeedMbps)
            put("latencyMs", result.latencyMs)
            put("jitterMs", result.jitterMs)
            put("packetLossPercent", result.packetLossPercent)
            put("ispScore", result.ispScore)
            put("qualityLabel", result.qualityLabel.name)
            put("isThrottled", result.isThrottled)
            put("throttledCDN", result.throttledCDN ?: "")
            put("inconsistentEndpoints", JSONArray(result.inconsistentEndpoints))
            put("suspiciousCDNs", JSONArray(result.suspiciousCDNs))
            result.networkInfo?.let { networkInfo ->
                put("networkInfo", JSONObject().apply {
                    put("publicIP", networkInfo.publicIP)
                    put("ispName", networkInfo.ispName)
                    put("connectionType", networkInfo.connectionType.name)
                    put("city", networkInfo.city)
                    put("country", networkInfo.country)
                })
            }
        }
    }

    private fun parseResult(json: JSONObject): SpeedTestResult {
        val inconsistentArray = json.optJSONArray("inconsistentEndpoints") ?: JSONArray()
        val inconsistent = (0 until inconsistentArray.length()).map { inconsistentArray.getString(it) }

        val suspiciousArray = json.optJSONArray("suspiciousCDNs") ?: JSONArray()
        val suspicious = (0 until suspiciousArray.length()).map { suspiciousArray.getString(it) }

        val networkInfoJson = json.optJSONObject("networkInfo")

        return SpeedTestResult(
            id = json.getString("id"),
            timestamp = json.getLong("timestamp"),
            downloadSpeedMbps = json.getDouble("downloadSpeedMbps"),
            uploadSpeedMbps = json.getDouble("uploadSpeedMbps"),
            latencyMs = json.getDouble("latencyMs"),
            jitterMs = json.getDouble("jitterMs"),
            packetLossPercent = json.getDouble("packetLossPercent"),
            ispScore = json.getInt("ispScore"),
            qualityLabel = com.rudra.realspeedtest.data.model.QualityLabel.valueOf(
                json.optString("qualityLabel", "UNKNOWN")
            ),
            isThrottled = json.getBoolean("isThrottled"),
            throttledCDN = json.optString("throttledCDN").takeIf { it.isNotEmpty() },
            inconsistentEndpoints = inconsistent,
            suspiciousCDNs = suspicious,
            networkInfo = networkInfoJson?.let {
                com.rudra.realspeedtest.data.model.NetworkInfo(
                    publicIP = it.getString("publicIP"),
                    ispName = it.getString("ispName"),
                    connectionType = com.rudra.realspeedtest.data.model.ConnectionType.valueOf(
                        it.optString("connectionType", "UNKNOWN")
                    ),
                    city = it.optString("city", ""),
                    country = it.optString("country", "")
                )
            }
        )
    }
}