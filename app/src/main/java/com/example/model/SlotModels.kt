package com.example.model

data class CasinoInfo(
    val domain: String,
    val trustScore: Int,
    val status: String,
    val reason: String = "",
    val isBlacklisted: Boolean = false
)

data class GameInfo(
    val slug: String,
    val name: String,
    val rtp: Double,
    val volatility: String,
    val provider: String
)

data class SpinResult(
    val reels: List<String>,
    val win: Double,
    val multiplier: Double
)

data class AnalysisResult(
    val casino: CasinoInfo?,
    val game: GameInfo?
)
