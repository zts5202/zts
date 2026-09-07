package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlassTheme
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 调质工序卡实时解析数据节点
 */
data class QuenchingTemperingTelemetry(
    val stageIndex: Int,
    val stageName: String,
    val stageSubDesc: String,
    val tempDisplay: String,
    val timeDisplay: String,
    val targetEquip: String,
    val isSoak: Boolean,
    val accentColor: Color
)

/**
 * 炫酷交互式调质工艺曲线 SVG/Canvas 动态仪表盘
 *
 * 核心交互升级（完美满足用户需求）：
 * 1. 【点击/拖动定位】：点击画布任意位置或直接拖拽，脉冲光斑直接定格在此处，实时遥测显示该位置的真实工艺温度与保温时间！
 * 2. 【暂停 / 自动巡检双模式】：点击中央光斑或控制按钮可暂停/继续自动巡检（Auto Scan）；
 * 3. 【高精度横向工艺滑块 (Time Slider)】：卡片内置横向工艺滑尺，随手指滑动精确控制时间进度，实时锁定质检工序卡（版本F）上的对应数据！
 */
@Composable
fun QuenchingTemperingCurveCard(
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    val textMeasurer = rememberTextMeasurer()
    val coroutineScope = rememberCoroutineScope()

    // 是否处于用户手动定格模式（点击或滑动滑块后暂停自动播放，定格在特定工艺点）
    var isManualMode by remember { mutableStateOf(false) }

    // 工艺时间进度驱动 (0f ~ 1f)
    val progressAnimatable = remember { Animatable(0.28f) }

    // 自动巡航协程循环：在非手动模式下以 7000ms 循环平滑前进
    LaunchedEffect(isManualMode) {
        if (!isManualMode) {
            while (isActive) {
                val current = progressAnimatable.value
                val remainingTime = ((1f - current) * 7000).toInt().coerceAtLeast(200)
                progressAnimatable.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = remainingTime, easing = LinearEasing)
                )
                progressAnimatable.snapTo(0f)
            }
        }
    }

    val currentProgress = progressAnimatable.value

    // 呼吸辉光脉冲动画
    val infiniteTransition = rememberInfiniteTransition(label = "heatTreatmentGlow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    // 根据工序卡工艺实测数据计算遥测信息
    val telemetry = remember(currentProgress) {
        deriveTelemetry(currentProgress)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF131722).copy(alpha = 0.92f),
                        Color(0xFF0C0F17).copy(alpha = 0.96f)
                    )
                )
            )
            .border(
                1.2.dp,
                Brush.horizontalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.16f),
                        telemetry.accentColor.copy(alpha = 0.55f),
                        GlassTheme.CyanGlow.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.10f)
                    )
                ),
                RoundedCornerShape(22.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column {
            // ==================== 1. 顶部状态栏与模式控制 ====================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(telemetry.accentColor)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = "调质工序卡标准曲线 (版本 F)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.4.sp
                    )
                }

                // 播放/定格模式切换胶囊
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1F2432).copy(alpha = 0.85f))
                        .border(
                            1.dp,
                            if (isManualMode) GlassTheme.IosOrange.copy(alpha = 0.6f) else GlassTheme.CyanGlow.copy(alpha = 0.4f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            isManualMode = !isManualMode
                        }
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isManualMode) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isManualMode) "继续巡航" else "定格分析",
                            tint = if (isManualMode) GlassTheme.IosOrange else GlassTheme.CyanGlow,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isManualMode) "已定格 (点此巡航)" else "巡航中 (点此定格)",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isManualMode) GlassTheme.IosOrange else GlassTheme.CyanGlow
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ==================== 2. 当前定格/巡检位置的工艺数据实时看板 ====================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF10141D).copy(alpha = 0.75f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = telemetry.stageName,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = telemetry.stageSubDesc,
                        fontSize = 10.5.sp,
                        color = GlassTheme.TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // 核心温度与时间大指标
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = telemetry.tempDisplay,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = telemetry.accentColor
                        )
                        Text(
                            text = telemetry.timeDisplay,
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = GlassTheme.CyanGlow
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ==================== 3. 互动 SVG/Canvas 调质曲线图（支持点击定格/拖拽） ====================
            val canvasHeight = if (isCompact) 130.dp else 155.dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(canvasHeight)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        // 交互 1：点击曲线任意位置，脉冲光斑瞬间定格，显示该处数据！
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                isManualMode = true
                                val paddingLeft = 46f
                                val paddingRight = 24f
                                val chartW = (size.width - paddingLeft - paddingRight).coerceAtLeast(1f)
                                val tappedProgress = ((offset.x - paddingLeft) / chartW).coerceIn(0f, 1f)
                                coroutineScope.launch {
                                    progressAnimatable.snapTo(tappedProgress)
                                }
                            }
                        }
                        // 交互 2：手指在图表上直接横向拖动，光斑实时紧随手指移动！
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { isManualMode = true },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val paddingLeft = 46f
                                    val paddingRight = 24f
                                    val chartW = (size.width - paddingLeft - paddingRight).coerceAtLeast(1f)
                                    val draggedProgress = ((change.position.x - paddingLeft) / chartW).coerceIn(0f, 1f)
                                    coroutineScope.launch {
                                        progressAnimatable.snapTo(draggedProgress)
                                    }
                                }
                            )
                        }
                ) {
                    val width = size.width
                    val height = size.height

                    val paddingLeft = 46f
                    val paddingRight = 24f
                    val paddingTop = 26f
                    val paddingBottom = 30f

                    val chartW = width - paddingLeft - paddingRight
                    val chartH = height - paddingTop - paddingBottom

                    // 纵轴刻度基准
                    val yQuench = paddingTop + chartH * 0.12f // 850~860℃
                    val yTemper = paddingTop + chartH * 0.42f // 595~630℃
                    val yCool = paddingTop + chartH * 0.88f   // <150℃
                    val yBottom = paddingTop + chartH        // 0℃/轴底

                    // 横轴时间节点划分
                    val x0 = paddingLeft
                    val x1 = paddingLeft + chartW * 0.18f
                    val x2 = paddingLeft + chartW * 0.40f
                    val x3 = paddingLeft + chartW * 0.48f
                    val x4 = paddingLeft + chartW * 0.54f
                    val x5 = paddingLeft + chartW * 0.65f
                    val x6 = paddingLeft + chartW * 0.86f
                    val x7 = paddingLeft + chartW

                    val gridColor = Color.White.copy(alpha = 0.06f)
                    val axisColor = Color.White.copy(alpha = 0.28f)

                    // 1. 水平刻度与坐标轴
                    drawLine(gridColor, Offset(paddingLeft, yQuench), Offset(paddingLeft + chartW, yQuench), strokeWidth = 1f)
                    drawLine(gridColor, Offset(paddingLeft, yTemper), Offset(paddingLeft + chartW, yTemper), strokeWidth = 1f)
                    drawLine(gridColor, Offset(paddingLeft, yCool), Offset(paddingLeft + chartW, yCool), strokeWidth = 1f)
                    drawLine(axisColor, Offset(paddingLeft, yBottom), Offset(paddingLeft + chartW, yBottom), strokeWidth = 1.5f)

                    // 纵坐标轴线 + 箭头
                    drawLine(axisColor, Offset(paddingLeft, yBottom), Offset(paddingLeft, paddingTop - 12f), strokeWidth = 1.5f)
                    drawLine(axisColor, Offset(paddingLeft, paddingTop - 12f), Offset(paddingLeft - 4f, paddingTop - 4f), strokeWidth = 1.5f)
                    drawLine(axisColor, Offset(paddingLeft, paddingTop - 12f), Offset(paddingLeft + 4f, paddingTop - 4f), strokeWidth = 1.5f)

                    // 纵轴温度刻度文字
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "T(℃)",
                        style = TextStyle(color = GlassTheme.TextDim, fontSize = 9.sp, fontFamily = FontFamily.Monospace),
                        topLeft = Offset(8f, paddingTop - 18f)
                    )
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "860°",
                        style = TextStyle(color = GlassTheme.IosOrange, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                        topLeft = Offset(12f, yQuench - 8f)
                    )
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "630°",
                        style = TextStyle(color = GlassTheme.IosBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                        topLeft = Offset(12f, yTemper - 8f)
                    )
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "<150°",
                        style = TextStyle(color = GlassTheme.CyanGlow, fontSize = 9.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace),
                        topLeft = Offset(8f, yCool - 8f)
                    )

                    // 2. 调质折线路径
                    val curvePath = Path().apply {
                        moveTo(x0, paddingTop + chartH * 0.75f)
                        lineTo(x1, yQuench)
                        lineTo(x2, yQuench)
                        lineTo(x3, yCool)
                        lineTo(x4, yCool)
                        lineTo(x5, yTemper)
                        lineTo(x6, yTemper)
                        lineTo(x7, yBottom)
                    }

                    // 渐变填充阴影区
                    val fillPath = Path().apply {
                        addPath(curvePath)
                        lineTo(x7, yBottom)
                        lineTo(x0, yBottom)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                GlassTheme.IosOrange.copy(alpha = 0.25f * glowPulse),
                                GlassTheme.IosBlue.copy(alpha = 0.15f),
                                GlassTheme.IosIndigo.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            startY = yQuench,
                            endY = yBottom
                        )
                    )

                    // 虚线阶段分区
                    val stageSplitStyle = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    drawLine(Color.White.copy(alpha = 0.16f), Offset(x1, yQuench), Offset(x1, yBottom), strokeWidth = 1f, pathEffect = stageSplitStyle)
                    drawLine(Color.White.copy(alpha = 0.16f), Offset(x2, yQuench), Offset(x2, yBottom), strokeWidth = 1f, pathEffect = stageSplitStyle)
                    drawLine(Color.White.copy(alpha = 0.16f), Offset(x3, yCool), Offset(x3, yBottom), strokeWidth = 1f, pathEffect = stageSplitStyle)
                    drawLine(Color.White.copy(alpha = 0.16f), Offset(x4, yCool), Offset(x4, yBottom), strokeWidth = 1f, pathEffect = stageSplitStyle)
                    drawLine(Color.White.copy(alpha = 0.16f), Offset(x5, yTemper), Offset(x5, yBottom), strokeWidth = 1f, pathEffect = stageSplitStyle)
                    drawLine(Color.White.copy(alpha = 0.16f), Offset(x6, yTemper), Offset(x6, yBottom), strokeWidth = 1f, pathEffect = stageSplitStyle)

                    // 3. 曲线主体（霓虹能量描边）
                    drawPath(
                        path = curvePath,
                        brush = Brush.horizontalGradient(
                            listOf(
                                GlassTheme.IosOrange.copy(alpha = 0.45f * glowPulse),
                                GlassTheme.IosOrange,
                                GlassTheme.CyanGlow,
                                GlassTheme.IosBlue,
                                GlassTheme.IosBlue.copy(alpha = 0.45f)
                            )
                        ),
                        style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = curvePath,
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color(0xFFFFB300),
                                Color(0xFFFF5252),
                                GlassTheme.CyanGlow,
                                GlassTheme.IosBlue,
                                Color(0xFF81D4FA)
                            )
                        ),
                        style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // 4. 定位光斑与激光垂直基准线
                    val currentScanX = paddingLeft + chartW * currentProgress
                    val currentScanY = calculateCurveY(
                        x = currentScanX,
                        x0 = x0, x1 = x1, x2 = x2, x3 = x3, x4 = x4, x5 = x5, x6 = x6, x7 = x7,
                        yQuench = yQuench, yTemper = yTemper, yCool = yCool,
                        yStart = paddingTop + chartH * 0.75f, yBottom = yBottom
                    )

                    // 激光定位线
                    drawLine(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, telemetry.accentColor.copy(alpha = 0.85f), Color.Transparent),
                            startY = paddingTop,
                            endY = yBottom
                        ),
                        start = Offset(currentScanX, paddingTop),
                        end = Offset(currentScanX, yBottom),
                        strokeWidth = if (isManualMode) 2.dp.toPx() else 1.5.dp.toPx()
                    )

                    // 脉冲光斑核心（外环呼吸 + 核心白光球）
                    drawCircle(
                        color = telemetry.accentColor.copy(alpha = 0.35f * glowPulse),
                        radius = (if (isManualMode) 15.dp else 12.dp).toPx(),
                        center = Offset(currentScanX, currentScanY)
                    )
                    drawCircle(
                        color = telemetry.accentColor,
                        radius = 6.dp.toPx(),
                        center = Offset(currentScanX, currentScanY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.8.dp.toPx(),
                        center = Offset(currentScanX, currentScanY)
                    )

                    // 若处于手动模式，在光斑上方浮动展示“已定格”微标签
                    if (isManualMode) {
                        drawText(
                            textMeasurer = textMeasurer,
                            text = "📍定格",
                            style = TextStyle(color = Color.White, fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
                            topLeft = Offset(currentScanX - 12.dp.toPx(), (currentScanY - 20.dp.toPx()).coerceAtLeast(paddingTop))
                        )
                    }

                    // 5. 核心工艺标注文字
                    val quenchCenterX = (x1 + x2) / 2f
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "保温",
                        style = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        topLeft = Offset(quenchCenterX - 10.dp.toPx(), yQuench + 6.dp.toPx())
                    )
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "165~220min",
                        style = TextStyle(color = GlassTheme.IosOrange, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                        topLeft = Offset(quenchCenterX - 24.dp.toPx(), yQuench + 19.dp.toPx())
                    )

                    val temperCenterX = (x5 + x6) / 2f
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "保温",
                        style = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        topLeft = Offset(temperCenterX - 10.dp.toPx(), yTemper + 6.dp.toPx())
                    )
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "≥345min",
                        style = TextStyle(color = GlassTheme.IosBlue, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                        topLeft = Offset(temperCenterX - 18.dp.toPx(), yTemper + 19.dp.toPx())
                    )

                    drawText(
                        textMeasurer = textMeasurer,
                        text = "时间 (t)",
                        style = TextStyle(color = GlassTheme.TextDim, fontSize = 9.sp, fontFamily = FontFamily.Monospace),
                        topLeft = Offset(chartW + 2.dp.toPx(), yBottom - 14.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ==================== 4. 用户要求的横向工艺进度滑块 (Time Slider) ====================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF161922).copy(alpha = 0.85f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = GlassTheme.IosTeal,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "横向滑块调温定格",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlassTheme.TextWhite
                        )
                    }

                    // 进度百分比与阶段标识
                    Text(
                        text = "${(currentProgress * 100).toInt()}% · ${telemetry.targetEquip}",
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = telemetry.accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = currentProgress,
                    onValueChange = { newValue ->
                        isManualMode = true
                        coroutineScope.launch {
                            progressAnimatable.snapTo(newValue)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = telemetry.accentColor,
                        activeTrackColor = telemetry.accentColor,
                        inactiveTrackColor = Color.White.copy(alpha = 0.12f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ==================== 5. 工序卡实测工艺规程参数卡片 (淬火 & 回火) ====================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 淬火指标
                val isQuenchingActive = currentProgress in 0.18f..0.54f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isQuenchingActive) GlassTheme.IosOrange.copy(alpha = 0.15f) else Color(0xFF1B1E28).copy(alpha = 0.7f))
                        .border(
                            1.dp,
                            if (isQuenchingActive) GlassTheme.IosOrange else GlassTheme.IosOrange.copy(alpha = 0.35f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            isManualMode = true
                            coroutineScope.launch {
                                progressAnimatable.animateTo(0.28f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(GlassTheme.IosOrange)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "淬火保温指标 (11小车)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassTheme.IosOrange
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "温度: (850~860)±10℃",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                        Text(
                            text = "保温: 165~220 min",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = GlassTheme.TextMuted
                        )
                        Text(
                            text = "冷却: 6±1 min (<150℃)",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = GlassTheme.CyanGlow
                        )
                    }
                }

                // 回火指标
                val isTemperingActive = currentProgress in 0.54f..0.88f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isTemperingActive) GlassTheme.IosBlue.copy(alpha = 0.15f) else Color(0xFF1B1E28).copy(alpha = 0.7f))
                        .border(
                            1.dp,
                            if (isTemperingActive) GlassTheme.IosBlue else GlassTheme.IosBlue.copy(alpha = 0.35f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            isManualMode = true
                            coroutineScope.launch {
                                progressAnimatable.animateTo(0.72f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(GlassTheme.IosBlue)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "回火保温指标 (23小车)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassTheme.IosBlue
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "温度: (595~630)±10℃",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                        Text(
                            text = "保温: ≥345 min",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = GlassTheme.TextMuted
                        )
                        Text(
                            text = "出炉: 空冷降温",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = GlassTheme.TextDim
                        )
                    }
                }
            }
        }
    }
}

/**
 * 辅助函数：根据工艺进度精准推导工序卡实测工艺数据
 */
private fun deriveTelemetry(progress: Float): QuenchingTemperingTelemetry {
    return when (progress) {
        in 0.00f..0.18f -> {
            val p = (progress / 0.18f).coerceIn(0f, 1f)
            val currentTemp = (200 + p * 655).toInt()
            val minutes = (p * 80).toInt()
            QuenchingTemperingTelemetry(
                stageIndex = 1,
                stageName = "① 功率升温阶段 (<850℃)",
                stageSubDesc = "设备全功率平稳升温中，避免温差内应力",
                tempDisplay = "$currentTemp ℃",
                timeDisplay = "升温时长约 ${minutes}m",
                targetEquip = "升温区",
                isSoak = false,
                accentColor = Color(0xFFFF9800)
            )
        }
        in 0.18f..0.40f -> {
            val p = ((progress - 0.18f) / 0.22f).coerceIn(0f, 1f)
            val elapsedMinutes = (165 + p * (220 - 165)).toInt()
            QuenchingTemperingTelemetry(
                stageIndex = 2,
                stageName = "② 淬火高温保温阶段 (11小车)",
                stageSubDesc = "奥氏体完全均匀化，保温严格控制 165~220min",
                tempDisplay = "855 ℃ (±10)",
                timeDisplay = "已保温 ${elapsedMinutes} / 220min",
                targetEquip = "11小车",
                isSoak = true,
                accentColor = Color(0xFFFF5722)
            )
        }
        in 0.40f..0.48f -> {
            val p = ((progress - 0.40f) / 0.08f).coerceIn(0f, 1f)
            val currentTemp = (855 - p * 710).toInt()
            val quenchSeconds = (p * 6 * 60).toInt()
            QuenchingTemperingTelemetry(
                stageIndex = 3,
                stageName = "③ 淬火急速冷却 (急冷水冷)",
                stageSubDesc = "急剧降温获取硬化马氏体，工序冷却时间 6±1min",
                tempDisplay = "$currentTemp ℃ (<150℃)",
                timeDisplay = "冷却进行 ${quenchSeconds / 60}m ${quenchSeconds % 60}s",
                targetEquip = "淬火水槽",
                isSoak = false,
                accentColor = GlassTheme.CyanGlow
            )
        }
        in 0.48f..0.54f -> {
            val p = ((progress - 0.48f) / 0.06f).coerceIn(0f, 1f)
            val waitMinutes = (p * 240).toInt()
            QuenchingTemperingTelemetry(
                stageIndex = 4,
                stageName = "④ 淬火后中转停留 (<240min)",
                stageSubDesc = "冷却至<150℃，准备转入回火炉",
                tempDisplay = "< 145 ℃",
                timeDisplay = "停留时间 ${waitMinutes} / 240min",
                targetEquip = "转运区",
                isSoak = false,
                accentColor = Color(0xFF26C6DA)
            )
        }
        in 0.54f..0.65f -> {
            val p = ((progress - 0.54f) / 0.11f).coerceIn(0f, 1f)
            val currentTemp = (145 + p * 467).toInt()
            QuenchingTemperingTelemetry(
                stageIndex = 5,
                stageName = "⑤ 回火二次升温 (目标595~630℃)",
                stageSubDesc = "升温消除淬火脆性残余应力",
                tempDisplay = "$currentTemp ℃",
                timeDisplay = "二次升温中",
                targetEquip = "回火炉",
                isSoak = false,
                accentColor = GlassTheme.IosBlue
            )
        }
        in 0.65f..0.86f -> {
            val p = ((progress - 0.65f) / 0.21f).coerceIn(0f, 1f)
            val elapsedMinutes = (p * 345).toInt()
            QuenchingTemperingTelemetry(
                stageIndex = 6,
                stageName = "⑥ 回火长时保温阶段 (23小车)",
                stageSubDesc = "获得回火索氏体综合力学韧性，保温 ≥345min",
                tempDisplay = "612 ℃ (±10)",
                timeDisplay = "已保温 ${elapsedMinutes} / ≥345min",
                targetEquip = "23小车",
                isSoak = true,
                accentColor = GlassTheme.IosBlue
            )
        }
        else -> {
            val p = ((progress - 0.86f) / 0.14f).coerceIn(0f, 1f)
            val currentTemp = (612 - p * 580).toInt()
            QuenchingTemperingTelemetry(
                stageIndex = 7,
                stageName = "⑦ 出炉空冷硬化完成",
                stageSubDesc = "自然空冷降至常温，调质工序圆满达标",
                tempDisplay = "$currentTemp ℃",
                timeDisplay = "空冷硬化降温中",
                targetEquip = "冷却场",
                isSoak = false,
                accentColor = Color(0xFF78909C)
            )
        }
    }
}

/**
 * 辅助函数：根据当前 X 坐标插值计算对应温度曲线的 Y 坐标
 */
private fun calculateCurveY(
    x: Float,
    x0: Float, x1: Float, x2: Float, x3: Float, x4: Float, x5: Float, x6: Float, x7: Float,
    yQuench: Float, yTemper: Float, yCool: Float, yStart: Float, yBottom: Float
): Float {
    return when {
        x <= x0 -> yStart
        x <= x1 -> {
            val ratio = (x - x0) / (x1 - x0)
            yStart + (yQuench - yStart) * ratio
        }
        x <= x2 -> yQuench
        x <= x3 -> {
            val ratio = (x - x2) / (x3 - x2)
            yQuench + (yCool - yQuench) * ratio
        }
        x <= x4 -> yCool
        x <= x5 -> {
            val ratio = (x - x4) / (x5 - x4)
            yCool + (yTemper - yCool) * ratio
        }
        x <= x6 -> yTemper
        x <= x7 -> {
            val ratio = (x - x6) / (x7 - x6)
            yTemper + (yBottom - yTemper) * ratio
        }
        else -> yBottom
    }
}
