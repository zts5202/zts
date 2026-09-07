package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RingConfig
import com.example.ui.theme.GlassTheme
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 🌟 Ramotion 原生 Reel Search (3D 机械滚轴选择器)
 * 
 * 当搜索框有输入或匹配候选时，以带有 3D 柱面透视、阻尼弹簧回弹与中央游标吸附的 Roll 卷轴呈现。
 * 遵循 Leonxlnx / taste-skill 工控遥测级设计准则 (DESIGN_VARIANCE=8/10, MOTION_INTENSITY=9/10)。
 */
@Composable
fun RamotionReelSearchPicker(
    candidates: List<RingConfig>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onConfirmSelect: (RingConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    if (candidates.isEmpty()) return

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // 滚轴每个条目的固定高度 (dp & px)
    val itemHeightDp = 44.dp
    val itemHeightPx = with(density) { itemHeightDp.toPx() }

    // 连续滚轴偏移量 (以条目索引为单位的浮点值，例如 1.5 代表正好在第 1 和第 2 项中间)
    val reelOffset = remember { Animatable(selectedIndex.toFloat()) }

    // 当外界选定改变时（如搜索过滤），平滑过渡
    LaunchedEffect(selectedIndex, candidates.size) {
        val target = selectedIndex.coerceIn(0, (candidates.size - 1).coerceAtLeast(0)).toFloat()
        if (abs(reelOffset.value - target) > 0.01f) {
            reelOffset.animateTo(
                targetValue = target,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    // 滚筒可视槽深度：同时展示上下各 2 项，共 5 个槽位
    val visibleRadius = 2

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F1115))
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.08f),
                        GlassTheme.IosBlue.copy(alpha = 0.4f),
                        Color.White.copy(alpha = 0.08f)
                    )
                ),
                RoundedCornerShape(16.dp)
            )
            .padding(vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶端刻度仪表标识
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = null,
                        tint = GlassTheme.IosBlue,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "REEL 滚轴微调匹配",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.2.sp,
                        color = GlassTheme.IosBlue
                    )
                }

                Text(
                    text = "${(reelOffset.value.roundToInt() + 1).coerceIn(1, candidates.size)} / ${candidates.size} 候选项",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = GlassTheme.TextDim
                )
            }

            // 核心 3D 滚轴视图区域 (高度等于 5 个 itemHeight)
            val totalReelHeight = itemHeightDp * 4.2f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalReelHeight)
                    // 上下两侧羽化遮罩，营造深度滚入黑暗的柱面视差
                    .drawWithContent {
                        drawContent()
                        // 顶部遮罩渐变
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF0F1115), Color.Transparent),
                                startY = 0f,
                                endY = size.height * 0.28f
                            )
                        )
                        // 底部遮罩渐变
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xFF0F1115)),
                                startY = size.height * 0.72f,
                                endY = size.height
                            )
                        )
                    }
                    // 手势拖拽侦测：上划下划旋转 3D 滚轴
                    .pointerInput(candidates.size) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                val current = reelOffset.value
                                val targetSnap = current.roundToInt().coerceIn(0, candidates.size - 1)
                                coroutineScope.launch {
                                    reelOffset.animateTo(
                                        targetValue = targetSnap.toFloat(),
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                    onSelectIndex(targetSnap)
                                }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                val deltaItems = -dragAmount / itemHeightPx
                                val newOffset = (reelOffset.value + deltaItems)
                                    .coerceIn(-0.3f, (candidates.size - 1) + 0.3f)
                                coroutineScope.launch {
                                    reelOffset.snapTo(newOffset)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // 中央游标指示框 (Cursor Bracket)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                        .height(itemHeightDp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(GlassTheme.IosBlue.copy(alpha = 0.12f))
                        .border(
                            width = 1.2.dp,
                            brush = Brush.horizontalGradient(
                                listOf(
                                    GlassTheme.IosBlue.copy(alpha = 0.8f),
                                    GlassTheme.CyanGlow.copy(alpha = 0.8f)
                                )
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                )

                // 遍历渲染当前可见视锥范围内的条目，应用 3D 柱面翻转与透视
                val currentOffsetVal = reelOffset.value
                val centerIndex = currentOffsetVal.roundToInt()
                val minVisible = (centerIndex - visibleRadius - 1).coerceAtLeast(0)
                val maxVisible = (centerIndex + visibleRadius + 1).coerceAtMost(candidates.size - 1)

                for (i in minVisible..maxVisible) {
                    val config = candidates[i]
                    val delta = i - currentOffsetVal // 距离中心游标的相对偏移 (-2.0 ~ +2.0)

                    if (abs(delta) <= 2.6f) {
                        // 3D 物理滚筒变换参数
                        val rotationAngleX = (delta * 28f).coerceIn(-75f, 75f) // 上凸下凸的弧度
                        val scale = (1f - abs(delta) * 0.12f).coerceIn(0.68f, 1f)
                        val alpha = (1f - abs(delta) * 0.38f).coerceIn(0.08f, 1f)
                        val translateY = (delta * itemHeightPx * 0.88f)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(itemHeightDp)
                                .graphicsLayer {
                                    this.translationY = translateY
                                    this.rotationX = rotationAngleX
                                    this.scaleX = scale
                                    this.scaleY = scale
                                    this.alpha = alpha
                                    this.cameraDistance = 14f * density.density
                                    this.transformOrigin = TransformOrigin(0.5f, 0.5f)
                                }
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    coroutineScope.launch {
                                        reelOffset.animateTo(
                                            i.toFloat(),
                                            spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                        )
                                        onSelectIndex(i)
                                    }
                                }
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 左侧：型号大标与材质
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = config.model,
                                        fontSize = if (abs(delta) < 0.4f) 16.sp else 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (abs(delta) < 0.4f) Color.White else GlassTheme.TextMuted
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${config.material}钢",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (abs(delta) < 0.4f) GlassTheme.IosBlue else GlassTheme.TextDim
                                    )
                                }

                                // 中间：规格尺寸摘要 (外径/内径/高)
                                Text(
                                    text = "Φ${config.od.nominal} × Φ${config.id.nominal} × H${config.height.nominal}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (abs(delta) < 0.4f) Color.White.copy(alpha = 0.9f) else GlassTheme.TextDim
                                )

                                // 右侧：核心装炉指标 (孔位与件数)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (abs(delta) < 0.4f) GlassTheme.IosGreen.copy(alpha = 0.2f)
                                                else Color.White.copy(alpha = 0.05f)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "孔位 ${config.holePosition}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (abs(delta) < 0.4f) GlassTheme.IosGreen else GlassTheme.TextDim
                                        )
                                    }

                                    if (abs(delta) < 0.35f) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "当前选中",
                                            tint = GlassTheme.CyanGlow,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 底部一键装炉确认条 (Quick Confirm Pill)
            val currentSelected = candidates.getOrNull(reelOffset.value.roundToInt().coerceIn(0, candidates.size - 1))
            if (currentSelected != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(GlassTheme.IosBlue)
                        .clickable {
                            onConfirmSelect(currentSelected)
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "选定「${currentSelected.model}」查看装炉全息方案 →",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
            }
        }
    }
}
