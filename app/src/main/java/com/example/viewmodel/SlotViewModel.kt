package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MockData
import com.example.model.*
import com.example.util.SlotEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URL

data class SlotUiState(
    val urlInput: String = "",
    val isLoading: Boolean = false,
    val analysisResult: AnalysisResult? = null,
    val credits: Double = 1000.0,
    val bet: Double = 10.0,
    val rtpSetting: Double = 0.96,
    val lastSpinResult: SpinResult? = null,
    val history: List<Double> = listOf(1000.0),
    val stats: SlotStats = SlotStats()
)

data class SlotStats(
    val spins: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val totalProfit: Double = 0.0
)

class SlotViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SlotUiState())
    val uiState: StateFlow<SlotUiState> = _uiState.asStateFlow()

    fun onUrlChange(newUrl: String) {
        _uiState.value = _uiState.value.copy(urlInput = newUrl)
    }

    fun runAnalysis() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, analysisResult = null)
            delay(1000) // Simulation delay for "realism" as requested

            val input = _uiState.value.urlInput
            try {
                val url = URL(input)
                val domain = url.host
                val pathSegments = url.path.split("/")
                val gameSlug = pathSegments.lastOrNull() ?: ""

                val casino = MockData.blacklist.find { it.domain == domain }
                    ?: MockData.casinos.find { it.domain == domain }
                    ?: CasinoInfo(domain = domain, trustScore = -1, status = "ไม่มีข้อมูล")

                val game = MockData.games.find { it.slug == gameSlug }
                    ?: GameInfo(slug = gameSlug, name = "Unknown", rtp = 0.96, volatility = "Unknown", provider = "Unknown")

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    analysisResult = AnalysisResult(casino, game),
                    rtpSetting = game.rtp
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    analysisResult = null // Should show error in UI
                )
            }
        }
    }

    fun spin() {
        val currentState = _uiState.value
        if (currentState.credits < currentState.bet) return

        val result = SlotEngine.spin(currentState.rtpSetting, currentState.bet)
        val newCredits = currentState.credits - currentState.bet + result.win
        val newHistory = currentState.history + newCredits
        
        val newStats = currentState.stats.copy(
            spins = currentState.stats.spins + 1,
            wins = currentState.stats.wins + (if (result.win > 0) 1 else 0),
            losses = currentState.stats.losses + (if (result.win == 0.0) 1 else 0),
            totalProfit = newCredits - 1000.0
        )

        _uiState.value = currentState.copy(
            credits = newCredits,
            lastSpinResult = result,
            history = newHistory,
            stats = newStats
        )
    }

    fun reset() {
        _uiState.value = _uiState.value.copy(
            credits = 1000.0,
            lastSpinResult = null,
            history = listOf(1000.0),
            stats = SlotStats()
        )
    }

    fun onRtpChange(newRtp: Double) {
        _uiState.value = _uiState.value.copy(rtpSetting = newRtp)
    }
}
