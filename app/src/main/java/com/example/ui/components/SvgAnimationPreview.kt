package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlassTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 动效预览切换容器：方案 A 与 方案 B
 */
@Composable
fun SvgAnimationPreviewSection(
    modifier: Modifier = Modifier,
    onSelectScheme: ((String) -> Unit)? = null
) {
    var currentScheme by remember { mutableStateOf("A") } // "A" or "B"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF141416))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 顶部切换控制器 (Preview Switcher)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1C1C1E))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 方案 A 切换按钮
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (currentScheme == "A") Color(0xFF2C2C2E) else Color.Transparent)
                    .clickable { currentScheme = "A" }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Radar,
                        contentDescription = null,
                        tint = if (currentScheme == "A") GlassTheme.IosBlue else GlassTheme.TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "方案A · 炉内孔位雷达",
                        fontSize = 11.sp,
                        fontWeight = if (currentScheme == "A") FontWeight.Bold else FontWeight.Normal,
                        color = if (currentScheme == "A") Color.White else GlassTheme.TextMuted
                    )
                }
            }

            // 方案 B 切换按钮
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (currentScheme == "B") Color(0xFF2C2C2E) else Color.Transparent)
                    .clickable { currentScheme = "B" }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Engineering,
                        contentDescription = null,
                        tint = if (currentScheme == "B") GlassTheme.IosGreen else GlassTheme.TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "方案B · 精密图纸蓝图",
                        fontSize = 11.sp,
                        fontWeight = if (currentScheme == "B") FontWeight.Bold else FontWeight.Normal,
                        color = if (currentScheme == "B") Color.White else GlassTheme.TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 核心矢量动画画布舞台
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
            contentAlignment = Alignment.Center
        ) {
            if (currentScheme == "A") {
                FurnaceRadarAnimation()
            } else {
                BlueprintToleranceAnimation()
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 底部动效解析与指引
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (currentScheme == "A")
                    "● 60FPS 调质炉吊具装炉矩阵实时扫描"
                else
                    "● CAD 矢量图纸微米级公差描线流动",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = GlassTheme.TextDim
            )

            // 满意选中按钮
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (currentScheme == "A") GlassTheme.IosBlue.copy(alpha = 0.2f)
                        else GlassTheme.IosGreen.copy(alpha = 0.2f)
                    )
                    .border(
                        0.8.dp,
                        if (currentScheme == "A") GlassTheme.IosBlue.copy(alpha = 0.5f)
                        else GlassTheme.IosGreen.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelectScheme?.invoke(currentScheme) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = if (currentScheme == "A") GlassTheme.IosBlue else GlassTheme.IosGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "选定此方案",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (currentScheme == "A") GlassTheme.IosBlue else GlassTheme.IosGreen
                    )
                }
            }
        }
    }
}

/**
 * 方案 A：调质炉装炉吊具矩阵 · 矢量雷达动效
 */
@Composable
fun FurnaceRadarAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "furnace_radar")

    // 雷达扫描角度旋转 (8秒一圈)
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepAngle"
    )

    // 刻度盘慢速逆时针微转 (30秒一圈)
    val dialAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -360f,
        animationSpec = infiniteRepeatable(
            animation = tween(32000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dialAngle"
    )

    // 孔位脉动微光
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = (minOf(size.width, size.height) / 2f) - 12.dp.toPx()

        // 1. 虚线十字基准线
        val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        drawLine(
            color = Color.White.copy(alpha = 0.08f),
            start = Offset(center.x - maxRadius, center.y),
            end = Offset(center.x + maxRadius, center.y),
            strokeWidth = 1f,
            pathEffect = dashPathEffect
        )
        drawLine(
            color = Color.White.copy(alpha = 0.08f),
            start = Offset(center.x, center.y - maxRadius),
            end = Offset(center.x, center.y + maxRadius),
            strokeWidth = 1f,
            pathEffect = dashPathEffect
        )

        // 2. 调质炉外圈与中圈、内圈同心圆
        drawCircle(
            color = Color(0xFF0A84FF).copy(alpha = 0.25f),
            radius = maxRadius,
            center = center,
            style = Stroke(width = 1.5f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.1f),
            radius = maxRadius * 0.72f,
            center = center,
            style = Stroke(width = 1f)
        )
        drawCircle(
            color = Color(0xFF30D158).copy(alpha = 0.2f),
            radius = maxRadius * 0.44f,
            center = center,
            style = Stroke(width = 1.2f, pathEffect = dashPathEffect)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.15f),
            radius = maxRadius * 0.18f,
            center = center,
            style = Stroke(width = 1f)
        )

        // 3. 旋转刻度环 (Tick Marks)
        rotate(dialAngle, pivot = center) {
            val tickCount = 36
            for (i in 0 until tickCount) {
                val angleRad = (i * 360f / tickCount) * (PI / 180f)
                val isMajor = i % 3 == 0
                val tickLen = if (isMajor) 8.dp.toPx() else 4.dp.toPx()
                val rOuter = maxRadius
                val rInner = maxRadius - tickLen

                val start = Offset(
                    center.x + (rInner * cos(angleRad)).toFloat(),
                    center.y + (rInner * sin(angleRad)).toFloat()
                )
                val end = Offset(
                    center.x + (rOuter * cos(angleRad)).toFloat(),
                    center.y + (rOuter * sin(angleRad)).toFloat()
                )
                drawLine(
                    color = if (isMajor) Color(0xFF0A84FF).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.15f),
                    start = start,
                    end = end,
                    strokeWidth = if (isMajor) 1.5f else 1f
                )
            }
        }

        // 4. 孔位矩阵 (12 个外圈孔位 + 6 个内圈孔位)
        val outerHoleCount = 12
        for (i in 0 until outerHoleCount) {
            val angleRad = (i * 360f / outerHoleCount) * (PI / 180f)
            val hRadius = maxRadius * 0.72f
            val hCenter = Offset(
                center.x + (hRadius * cos(angleRad)).toFloat(),
                center.y + (hRadius * sin(angleRad)).toFloat()
            )

            // 孔位底座
            drawCircle(
                color = Color(0xFF0A84FF).copy(alpha = pulseAlpha * 0.8f),
                radius = 4.5.dp.toPx(),
                center = hCenter,
                style = Stroke(width = 1.5f)
            )
            // 孔位中心光斑
            drawCircle(
                color = Color(0xFF0A84FF).copy(alpha = pulseAlpha * 0.5f),
                radius = 2.dp.toPx(),
                center = hCenter
            )
        }

        // 6 个内圈小吊具孔位
        val innerHoleCount = 6
        for (i in 0 until innerHoleCount) {
            val angleRad = (i * 360f / innerHoleCount + 30f) * (PI / 180f)
            val hRadius = maxRadius * 0.44f
            val hCenter = Offset(
                center.x + (hRadius * cos(angleRad)).toFloat(),
                center.y + (hRadius * sin(angleRad)).toFloat()
            )

            drawCircle(
                color = Color(0xFF30D158).copy(alpha = 0.7f),
                radius = 3.5.dp.toPx(),
                center = hCenter,
                style = Stroke(width = 1.2f)
            )
        }

        // 5. 动态雷达扫描扇形光锥 (Sweep Radar)
        rotate(sweepAngle, pivot = center) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF0A84FF).copy(alpha = 0.01f),
                        Color(0xFF0A84FF).copy(alpha = 0.22f)
                    ),
                    center = center
                ),
                startAngle = -45f,
                sweepAngle = 45f,
                useCenter = true,
                size = Size(maxRadius * 2, maxRadius * 2),
                topLeft = Offset(center.x - maxRadius, center.y - maxRadius)
            )

            // 扫描光束前锋线
            drawLine(
                color = Color(0xFF64D2FF).copy(alpha = 0.85f),
                start = center,
                end = Offset(center.x + maxRadius, center.y),
                strokeWidth = 1.5f,
                cap = StrokeCap.Round
            )
        }

        // 炉心焦点
        drawCircle(
            color = Color.White,
            radius = 2.5.dp.toPx(),
            center = center
        )
    }
}

/**
 * 方案 B：机械图纸公差蓝图 · CAD 流线绘制动效
 */
@Composable
fun BlueprintToleranceAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "cad_blueprint")

    // 线条绘制流光进度 (0f -> 1f 循环)
    val flowProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flowProgress"
    )

    // 公差引线高亮脉动
    val lineAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lineAlpha"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)

        // 1. 绘制极细 CAD 工程网格背景
        val gridStep = 20.dp.toPx()
        val cols = (size.width / gridStep).toInt()
        val rows = (size.height / gridStep).toInt()

        for (c in 0..cols) {
            drawLine(
                color = Color.White.copy(alpha = 0.025f),
                start = Offset(c * gridStep, 0f),
                end = Offset(c * gridStep, size.height),
                strokeWidth = 0.5f
            )
        }
        for (r in 0..rows) {
            drawLine(
                color = Color.White.copy(alpha = 0.025f),
                start = Offset(0f, r * gridStep),
                end = Offset(size.width, r * gridStep),
                strokeWidth = 0.5f
            )
        }

        // 2. 环形工件 3D 透视图形 (外径椭圆与内径椭圆 + 侧壁高度)
        val outerRadiusX = size.width * 0.38f
        val outerRadiusY = size.height * 0.22f
        val ringHeight = 36.dp.toPx()
        val topCenter = Offset(center.x, center.y - ringHeight / 2)
        val bottomCenter = Offset(center.x, center.y + ringHeight / 2)

        val innerRadiusX = outerRadiusX * 0.72f
        val innerRadiusY = outerRadiusY * 0.72f

        // 绘制下底面外轮廓半弧
        drawArc(
            color = Color(0xFF30D158).copy(alpha = 0.35f),
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(bottomCenter.x - outerRadiusX, bottomCenter.y - outerRadiusY),
            size = Size(outerRadiusX * 2, outerRadiusY * 2),
            style = Stroke(width = 1.2f)
        )

        // 侧壁垂直轮廓线 (左 & 右)
        drawLine(
            color = Color(0xFF30D158).copy(alpha = 0.6f),
            start = Offset(topCenter.x - outerRadiusX, topCenter.y),
            end = Offset(bottomCenter.x - outerRadiusX, bottomCenter.y),
            strokeWidth = 1.5f
        )
        drawLine(
            color = Color(0xFF30D158).copy(alpha = 0.6f),
            start = Offset(topCenter.x + outerRadiusX, topCenter.y),
            end = Offset(bottomCenter.x + outerRadiusX, bottomCenter.y),
            strokeWidth = 1.5f
        )

        // 顶面完整外径椭圆 (带流光相位)
        val dashEffect = PathEffect.dashPathEffect(
            floatArrayOf(15f, 10f),
            flowProgress * 50f
        )

        drawOval(
            color = Color(0xFF30D158).copy(alpha = lineAlpha),
            topLeft = Offset(topCenter.x - outerRadiusX, topCenter.y - outerRadiusY),
            size = Size(outerRadiusX * 2, outerRadiusY * 2),
            style = Stroke(width = 1.8f, pathEffect = dashEffect)
        )

        // 顶面内径椭圆
        drawOval(
            color = Color(0xFF64D2FF).copy(alpha = lineAlpha * 0.85f),
            topLeft = Offset(topCenter.x - innerRadiusX, topCenter.y - innerRadiusY),
            size = Size(innerRadiusX * 2, innerRadiusY * 2),
            style = Stroke(width = 1.2f)
        )

        // 3. 机械图纸公差引出标线 (Dimension Arrows)
        // 外径 OD 标注线 (上方)
        val odLineY = topCenter.y - outerRadiusY - 14.dp.toPx()
        drawLine(
            color = Color(0xFF0A84FF).copy(alpha = 0.8f),
            start = Offset(topCenter.x - outerRadiusX, odLineY),
            end = Offset(topCenter.x + outerRadiusX, odLineY),
            strokeWidth = 1.2f
        )
        // OD 左右边界引线
        drawLine(
            color = Color(0xFF0A84FF).copy(alpha = 0.4f),
            start = Offset(topCenter.x - outerRadiusX, topCenter.y),
            end = Offset(topCenter.x - outerRadiusX, odLineY - 4.dp.toPx()),
            strokeWidth = 0.8f
        )
        drawLine(
            color = Color(0xFF0A84FF).copy(alpha = 0.4f),
            start = Offset(topCenter.x + outerRadiusX, topCenter.y),
            end = Offset(topCenter.x + outerRadiusX, odLineY - 4.dp.toPx()),
            strokeWidth = 0.8f
        )

        // 高度 H 标注线 (右侧)
        val hLineX = topCenter.x + outerRadiusX + 16.dp.toPx()
        drawLine(
            color = Color(0xFFFF9F0A).copy(alpha = 0.85f),
            start = Offset(hLineX, topCenter.y),
            end = Offset(hLineX, bottomCenter.y),
            strokeWidth = 1.2f
        )
        // H 上下界限
        drawLine(
            color = Color(0xFFFF9F0A).copy(alpha = 0.4f),
            start = Offset(topCenter.x + outerRadiusX, topCenter.y),
            end = Offset(hLineX + 4.dp.toPx(), topCenter.y),
            strokeWidth = 0.8f
        )
        drawLine(
            color = Color(0xFFFF9F0A).copy(alpha = 0.4f),
            start = Offset(bottomCenter.x + outerRadiusX, bottomCenter.y),
            end = Offset(hLineX + 4.dp.toPx(), bottomCenter.y),
            strokeWidth = 0.8f
        )

        // 4. 沿图纸流动的能量微光点 (Laser particle traveling)
        val angle = flowProgress * 2 * PI
        val particleX = topCenter.x + (outerRadiusX * cos(angle)).toFloat()
        val particleY = topCenter.y + (outerRadiusY * sin(angle)).toFloat()

        drawCircle(
            color = Color.White,
            radius = 3.5.dp.toPx(),
            center = Offset(particleX, particleY)
        )
        drawCircle(
            color = Color(0xFF30D158).copy(alpha = 0.6f),
            radius = 7.dp.toPx(),
            center = Offset(particleX, particleY)
        )
    }
}
