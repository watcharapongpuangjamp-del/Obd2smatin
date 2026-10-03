package com.example.data

import com.example.model.CasinoInfo
import com.example.model.GameInfo

object MockData {
    val blacklist = listOf(
        CasinoInfo(domain = "scam-slots.com", trustScore = 0, status = "อันตราย", reason = "พบประวัติการโกงและไม่จ่ายเงิน", isBlacklisted = true),
        CasinoInfo(domain = "fake-win.net", trustScore = 10, status = "อันตราย", reason = "เซิร์ฟเวอร์ไม่ได้มาตรฐานสากล", isBlacklisted = true)
    )

    val casinos = listOf(
        CasinoInfo(domain = "trusted-play.com", trustScore = 95, status = "เชื่อถือได้สูง", reason = "มีใบอนุญาตถูกต้อง (MGA)"),
        CasinoInfo(domain = "royal-slot-hub.org", trustScore = 85, status = "ปลอดภัย", reason = "ถอนเงินไว รีวิวเป็นบวก")
    )

    val games = listOf(
        GameInfo(slug = "sweet-bonanza", name = "Sweet Bonanza", rtp = 0.965, volatility = "High", provider = "Pragmatic Play"),
        GameInfo(slug = "gates-of-olympus", name = "Gates of Olympus", rtp = 0.965, volatility = "High", provider = "Pragmatic Play"),
        GameInfo(slug = "fortune-ox", name = "Fortune Ox", rtp = 0.967, volatility = "Medium", provider = "PG Soft"),
        GameInfo(slug = "lucky-neko", name = "Lucky Neko", rtp = 0.967, volatility = "Medium", provider = "PG Soft")
    )
}
