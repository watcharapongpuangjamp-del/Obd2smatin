package com.example.util

import com.example.model.SpinResult
import kotlin.math.floor

object SlotEngine {
    private val symbols = listOf("🍒", "🍋", "💎", "7️⃣", "⭐", "🍀")

    fun spin(rtp: Double, bet: Double): SpinResult {
        // Random spin
        val reels = listOf(
            symbols.random(),
            symbols.random(),
            symbols.random()
        )

        var win = 0.0
        var multiplier = 0.0

        // Check for wins
        if (reels[0] == reels[1] && reels[1] == reels[2]) {
            // 3 matching
            val mults = mapOf(
                "🍒" to 2.0,
                "🍋" to 3.0,
                "💎" to 5.0,
                "7️⃣" to 10.0,
                "⭐" to 20.0,
                "🍀" to 50.0
            )
            multiplier = mults[reels[0]] ?: 2.0
            win = bet * multiplier
        } else if (reels[0] == reels[1] || reels[1] == reels[2]) {
            // 2 adjacent matching
            multiplier = 1.5
            win = bet * multiplier
        }

        // Adjust by RTP factor (Simplified logic as requested)
        // User logic: win = win * (rtp / 0.96)
        val rtpFactor = rtp / 0.96
        win *= rtpFactor

        return SpinResult(reels, win, multiplier)
    }
}
