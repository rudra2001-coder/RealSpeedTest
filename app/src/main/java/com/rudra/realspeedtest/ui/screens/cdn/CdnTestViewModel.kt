package com.rudra.realspeedtest.ui.screens.cdn

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rudra.realspeedtest.data.model.AggregatedCdnResult
import com.rudra.realspeedtest.data.model.CdnTestResult
import com.rudra.realspeedtest.engine.SpeedTestEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CdnCdnProgress(
    val cdnName: String = "",
    val progress: Float = 0f,
    val speedMbps: Double = 0.0
)

sealed class CdnTestPhase {
    data object Idle : CdnTestPhase()
    data class Testing(val currentCdn: String, val progress: Float, val currentSpeed: Double) : CdnTestPhase()
    data class Completed(val result: AggregatedCdnResult) : CdnTestPhase()
    data class Error(val message: String) : CdnTestPhase()
}

class CdnTestViewModel(context: Context) : ViewModel() {

    private val engine = SpeedTestEngine(context)

    private val _fileSizeMB = MutableStateFlow(5)
    val fileSizeMB: StateFlow<Int> = _fileSizeMB.asStateFlow()

    private val _phase = MutableStateFlow<CdnTestPhase>(CdnTestPhase.Idle)
    val phase: StateFlow<CdnTestPhase> = _phase.asStateFlow()

    private val _cdnResults = MutableStateFlow<List<CdnTestResult>>(emptyList())
    val cdnResults: StateFlow<List<CdnTestResult>> = _cdnResults.asStateFlow()

    private var testJob: Job? = null

    fun setFileSize(mb: Int) {
        _fileSizeMB.value = mb.coerceIn(1, 50)
    }

    fun startTest() {
        if (testJob?.isActive == true) return
        _phase.value = CdnTestPhase.Idle
        _cdnResults.value = emptyList()
        testJob = viewModelScope.launch {
            try {
                val size = _fileSizeMB.value
                val result = engine.runAggregatedCdnTest(size) { cdnName, progress, speed ->
                    _phase.value = CdnTestPhase.Testing(cdnName, progress, speed)
                }
                _cdnResults.value = result.results
                _phase.value = CdnTestPhase.Completed(result)
            } catch (e: Exception) {
                _phase.value = CdnTestPhase.Error(e.message ?: "Test failed")
            }
        }
    }

    fun cancelTest() {
        testJob?.cancel()
        testJob = null
        _phase.value = CdnTestPhase.Idle
    }

    fun reset() {
        cancelTest()
        _cdnResults.value = emptyList()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CdnTestViewModel(context) as T
        }
    }
}
