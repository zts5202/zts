package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Apple iOS (Cupertino) 工业极简设计系统主题规范
 * 采用原生 iOS 深色模式系统色彩层级：去除了刺眼的霓虹硬框，保留高对比度、大圆角与高级毛玻璃材质
 */
object GlassTheme {
    // iOS 系统深色背景层次 (System Backgrounds) -> Leonxlnx 工业深空背景层
    val BackgroundTop = Color(0xFF0A0B0D)
    val BackgroundBottom = Color(0xFF0F1014)
    val DeepNavyBg = Color(0xFF08090B)

    // Leonxlnx 高级工控微光 (Subtle Telemetry Glow)
    val OrbBlue = Color(0xFF0A84FF).copy(alpha = 0.07f)
    val OrbPurple = Color(0xFF5E5CE6).copy(alpha = 0.05f)
    val OrbCyan = Color(0xFF64D2FF).copy(alpha = 0.07f)

    // 工控物理卡片材质 (Industrial Tactile Surface)
    val CardBg = Color(0xFF131418)
    val CardBgActive = Color(0xFF1A1C22)
    val CardSubtleBg = Color(0xFF16181D)
    val CardElevated = Color(0xFF1F222A)

    // 严苛的工控 1px 晶体微边框 (1.dp Precision Hairlines)
    val BorderLight = Color(0xFFFFFFFF).copy(alpha = 0.08f)
    val BorderSubtle = Color(0xFFFFFFFF).copy(alpha = 0.04f)
    val BorderHighlight = Color(0xFF0A84FF).copy(alpha = 0.45f)
    val BorderCyan = Color(0xFF64D2FF).copy(alpha = 0.35f)

    // 高信息密度文字阶梯 (High-Contrast Typography)
    val TextWhite = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFFE5E7EB)
    val TextMuted = Color(0xFF9CA3AF)
    val TextDim = Color(0xFF6B7280)

    // 语义高亮色 (Leonxlnx Industrial Accents)
    val IosBlue = Color(0xFF0A84FF)
    val IosGreen = Color(0xFF30D158)
    val IosOrange = Color(0xFFFF9F0A)
    val IosIndigo = Color(0xFF5E5CE6)
    val IosTeal = Color(0xFF64D2FF)
    val NeonCyan = Color(0xFF00E5FF)
    val WarningAmber = Color(0xFFFFB300)
    val DangerRed = Color(0xFFFF453A)

    // 兼容原引用命名
    val CyanGlow = Color(0xFF0A84FF)
    val AmberGlow = Color(0xFFFF9F0A)
    val EmeraldGlow = Color(0xFF30D158)
    val RoseGlow = Color(0xFFFF453A)

    // 材质徽标 (42CrMo 极简苹果绿, 50Mn 极简琥珀金)
    val Mat42Bg = Color(0xFF30D158).copy(alpha = 0.15f)
    val Mat42Text = Color(0xFF30D158)
    val Mat50Bg = Color(0xFFFF9F0A).copy(alpha = 0.15f)
    val Mat50Text = Color(0xFFFF9F0A)
}
