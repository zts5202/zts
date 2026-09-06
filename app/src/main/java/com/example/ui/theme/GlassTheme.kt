package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 车间工业毛玻璃 (Industrial Glassmorphism) 设计系统主题常量
 */
object GlassTheme {
    val BackgroundTop = Color(0xFF0A0F1D)
    val BackgroundBottom = Color(0xFF0F172A)
    val DeepNavyBg = Color(0xFF070B14)

    val OrbBlue = Color(0xFF1E40AF).copy(alpha = 0.35f)
    val OrbPurple = Color(0xFF6D28D9).copy(alpha = 0.28f)
    val OrbCyan = Color(0xFF0284C7).copy(alpha = 0.25f)

    val CardBg = Color(0xFF1E293B).copy(alpha = 0.60f)
    val CardBgActive = Color(0xFF334155).copy(alpha = 0.75f)

    val BorderLight = Color(0xFFFFFFFF).copy(alpha = 0.18f)
    val BorderSubtle = Color(0xFFFFFFFF).copy(alpha = 0.08f)
    val BorderHighlight = Color(0xFF38BDF8).copy(alpha = 0.65f)

    val TextWhite = Color(0xFFF8FAFC)
    val TextMuted = Color(0xFF94A3B8)
    val TextDim = Color(0xFF64748B)

    val CyanGlow = Color(0xFF38BDF8)
    val AmberGlow = Color(0xFFFBBF24)
    val EmeraldGlow = Color(0xFF34D399)
    val RoseGlow = Color(0xFFFB7185)

    // 材质专用配色 (42CrMo 科技绿蓝, 50Mn 琥珀熔岩金)
    val Mat42Bg = Color(0xFF064E3B).copy(alpha = 0.45f)
    val Mat42Text = Color(0xFF34D399)
    val Mat50Bg = Color(0xFF78350F).copy(alpha = 0.45f)
    val Mat50Text = Color(0xFFFBBF24)
}
