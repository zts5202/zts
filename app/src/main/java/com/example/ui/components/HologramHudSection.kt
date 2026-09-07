package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlassTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * ⚡ 款式 4：工控全息 HUD 环 (大尺寸·旗舰级工业视觉中枢)
 * 采用原生硬件加速 Canvas 与 60FPS 矢量动画引擎渲染
 */
@Composable
fun HologramHudSection(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hud_infinite")

    // 六边形外环逆向自转 (32秒一圈，平缓深沉)
    val hexRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -360f,
        animationSpec = infiniteRepeatable(
            animation = tween(32000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hexRotation"
    )

    // 正向刻度微环转动 (20秒一圈)
    val dialRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dialRotation"
    )

    // 正弦热工波形横向流动相位
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    // 次级波形相位 (稍快一点产生波形差)
    val subWavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "subWavePhase"
    )

    // 全息扫描光晕呼吸
    val ringScanAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringScanAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF131417),
                        Color(0xFF0D0E10)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(26.dp))
            .padding(vertical = 20.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 大尺寸核心 HUD 画布 (占据主要视界)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val baseRadius = minOf(size.width, size.height) * 0.46f

                    // 1. 四角工控准星定位 HUD 标线
                    val bracketLen = 16.dp.toPx()
                    val pad = 8.dp.toPx()
                    val bracketColor = Color.White.copy(alpha = 0.22f)
                    val strokeW = 1.5f

                    // 左上
                    drawLine(bracketColor, Offset(pad, pad + bracketLen), Offset(pad, pad), strokeW)
                    drawLine(bracketColor, Offset(pad, pad), Offset(pad + bracketLen, pad), strokeW)
                    // 右上
                    drawLine(bracketColor, Offset(size.width - pad - bracketLen, pad), Offset(size.width - pad, pad), strokeW)
                    drawLine(bracketColor, Offset(size.width - pad, pad), Offset(size.width - pad, pad + bracketLen), strokeW)
                    // 左下
                    drawLine(bracketColor, Offset(pad, size.height - pad - bracketLen), Offset(pad, size.height - pad), strokeW)
                    drawLine(bracketColor, Offset(pad, size.height - pad), Offset(pad + bracketLen, size.height - pad), strokeW)
                    // 右下
                    drawLine(bracketColor, Offset(size.width - pad - bracketLen, size.height - pad), Offset(size.width - pad, size.height - pad), strokeW)
                    drawLine(bracketColor, Offset(size.width - pad, size.height - pad - bracketLen), Offset(size.width - pad, size.height - pad), strokeW)

                    // 2. 六边形外环自转 (带虚线科技边缘)
                    rotate(hexRotation, pivot = center) {
                        val hexRadius = baseRadius * 0.98f
                        val hexPath = Path()
                        for (i in 0..5) {
                            val angleRad = (i * 60f) * (PI / 180f)
                            val x = center.x + (hexRadius * cos(angleRad)).toFloat()
                            val y = center.y + (hexRadius * sin(angleRad)).toFloat()
                            if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
                        }
                        hexPath.close()

                        drawPath(
                            path = hexPath,
                            color = Color(0xFF64D2FF).copy(alpha = 0.25f),
                            style = Stroke(
                                width = 1.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                            )
                        )

                        // 六边形顶点光标
                        for (i in 0..5) {
                            val angleRad = (i * 60f) * (PI / 180f)
                            val px = center.x + (hexRadius * cos(angleRad)).toFloat()
                            val py = center.y + (hexRadius * sin(angleRad)).toFloat()
                            drawCircle(
                                color = Color(0xFF64D2FF).copy(alpha = 0.8f),
                                radius = 2.5.dp.toPx(),
                                center = Offset(px, py)
                            )
                        }
                    }

                    // 3. 顺时针旋转刻度环 (Tick Marks)
                    rotate(dialRotation, pivot = center) {
                        val dialR = baseRadius * 0.84f
                        val tickCount = 48
                        for (i in 0 until tickCount) {
                            val angleRad = (i * 360f / tickCount) * (PI / 180f)
                            val isMajor = i % 4 == 0
                            val tickLen = if (isMajor) 6.dp.toPx() else 3.dp.toPx()
                            val rIn = dialR - tickLen
                            val rOut = dialR

                            val start = Offset(
                                center.x + (rIn * cos(angleRad)).toFloat(),
                                center.y + (rIn * sin(angleRad)).toFloat()
                            )
                            val end = Offset(
                                center.x + (rOut * cos(angleRad)).toFloat(),
                                center.y + (rOut * sin(angleRad)).toFloat()
                            )
                            drawLine(
                                color = if (isMajor) Color(0xFF0A84FF).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.12f),
                                start = start,
                                end = end,
                                strokeWidth = if (isMajor) 1.5f else 1f
                            )
                        }
                    }

                    // 4. 全息同心镜圈
                    drawCircle(
                        color = Color.White.copy(alpha = 0.08f),
                        radius = baseRadius * 0.75f,
                        center = center,
                        style = Stroke(width = 1f)
                    )
                    drawCircle(
                        color = Color(0xFF0A84FF).copy(alpha = ringScanAlpha * 0.9f),
                        radius = baseRadius * 0.65f,
                        center = center,
                        style = Stroke(width = 2.2f)
                    )

                    // 5. 中心全息透镜：裁剪圆形范围并绘制动态热工波形
                    val lensRadius = baseRadius * 0.63f

                    // 镜面背景柔光
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF0A84FF).copy(alpha = 0.16f),
                                Color(0xFF30D158).copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = lensRadius
                        ),
                        radius = lensRadius,
                        center = center
                    )

                    // 波形绘制 (带圆形裁剪)
                    val circleClipPath = Path().apply {
                        addOval(
                            androidx.compose.ui.geometry.Rect(
                                center.x - lensRadius,
                                center.y - lensRadius,
                                center.x + lensRadius,
                                center.y + lensRadius
                            )
                        )
                    }

                    drawContext.canvas.save()
                    drawContext.canvas.clipPath(circleClipPath)

                    // 主热工波形 (绿色 30D158)
                    val wavePath1 = Path()
                    val waveAmp1 = 14.dp.toPx()
                    val waveFreq1 = 0.025f
                    val startX = center.x - lensRadius
                    val endX = center.x + lensRadius

                    var firstPoint = true
                    var x = startX
                    while (x <= endX) {
                        val y = center.y + (waveAmp1 * sin((x - startX) * waveFreq1 + wavePhase)).toFloat()
                        if (firstPoint) {
                            wavePath1.moveTo(x, y)
                            firstPoint = false
                        } else {
                            wavePath1.lineTo(x, y)
                        }
                        x += 3f
                    }

                    drawPath(
                        path = wavePath1,
                        color = Color(0xFF30D158).copy(alpha = 0.85f),
                        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                    )

                    // 次级流波 (琥珀金 FF9F0A)
                    val wavePath2 = Path()
                    val waveAmp2 = 10.dp.toPx()
                    val waveFreq2 = 0.035f
                    var firstPoint2 = true
                    var x2 = startX
                    while (x2 <= endX) {
                        val y2 = center.y + (waveAmp2 * cos((x2 - startX) * waveFreq2 + subWavePhase)).toFloat()
                        if (firstPoint2) {
                            wavePath2.moveTo(x2, y2)
                            firstPoint2 = false
                        } else {
                            wavePath2.lineTo(x2, y2)
                        }
                        x2 += 3f
                    }

                    drawPath(
                        path = wavePath2,
                        color = Color(0xFFFF9F0A).copy(alpha = 0.65f),
                        style = Stroke(width = 1.8f, cap = StrokeCap.Round)
                    )

                    drawContext.canvas.restore()
                }

                // 核心数字全息徽标文本 (浮在中央波形之上)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "AI OPTIMAL",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "PROCESS HUD ACTIVE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF64D2FF).copy(alpha = 0.85f),
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 底部工业遥测状态标牌
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(Color(0xFF30D158))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "REAL-TIME SENSOR TELEMETRY / PASS · 调质工艺参数智能待命",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Normal,
                    color = GlassTheme.TextMuted
                )
            }
        }
    }
}
