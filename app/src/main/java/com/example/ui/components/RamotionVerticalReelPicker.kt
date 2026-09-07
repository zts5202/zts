package com.example.ui.components

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
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
 * 严格复刻 Ramotion Reel-Search 原型效果的纵向 3D 滚轴选择器
 *
 * 特性：
 * 1. 随着用户的上下滑动，文字具有 3D 滚筒视差（距离输入标尺越远，透明度越低、字体越小且发生 X 轴向内翻折）
 * 2. 停留在正中输入横线/光标位置的选项为当前激活选中项
 * 3. 上下滑动带有惯性弹簧吸附回弹（Spring Physics）
 */
@Composable
fun RamotionVerticalReelPicker(
    candidates: List<RingConfig>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onConfirmSelect: (RingConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    if (candidates.isEmpty()) return

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // 滚轴每个单项高度
    val itemHeightDp = 48.dp
    val itemHeightPx = with(density) { itemHeightDp.toPx() }

    // 滚轴偏移量（单位：索引小数）
    val reelOffset = remember { Animatable(selectedIndex.toFloat()) }

    // 当外界候选项改变或选定改变时动画同步
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

    // 可视槽半径（上下各 3 项）
    val visibleRadius = 3
    val defaultReelHeight = itemHeightDp * 3.8f

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = defaultReelHeight)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0C0E14).copy(alpha = 0.65f)) // 通透纯净深色玻璃
            .border(
                1.2.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.15f),
                        GlassTheme.IosBlue.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.08f)
                    )
                ),
                RoundedCornerShape(20.dp)
            )
            // 边缘羽化，营造如 Ramotion 动图中的滚轴隐入上下的立体空间效果
            .drawWithContent {
                drawContent()
                // 顶部渐变遮罩
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0C0E14), Color.Transparent),
                        startY = 0f,
                        endY = size.height * 0.30f
                    )
                )
                // 底部渐变遮罩
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF0C0E14)),
                        startY = size.height * 0.70f,
                        endY = size.height
                    )
                )
            }
            // 手势监听：垂直拖拽滚动
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
        // 中央高亮参考线与游标背景框 (聚焦标尺)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .height(itemHeightDp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            GlassTheme.IosBlue.copy(alpha = 0.22f),
                            GlassTheme.CyanGlow.copy(alpha = 0.12f),
                            GlassTheme.IosBlue.copy(alpha = 0.22f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = GlassTheme.IosBlue.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp)
                )
        )

        // 遍历并在 3D 滚柱上投影条目
        val currentOffsetVal = reelOffset.value
        val centerIndex = currentOffsetVal.roundToInt()
        val minVisible = (centerIndex - visibleRadius - 1).coerceAtLeast(0)
        val maxVisible = (centerIndex + visibleRadius + 1).coerceAtMost(candidates.size - 1)

        for (i in minVisible..maxVisible) {
            val config = candidates[i]
            val delta = i - currentOffsetVal // 相对居中位置的偏差

            if (abs(delta) <= 3.2f) {
                // 3D 柱面数学投影
                val rotationAngleX = (delta * 26f).coerceIn(-70f, 70f)
                val scale = (1f - abs(delta) * 0.10f).coerceIn(0.72f, 1f)
                val alpha = (1f - abs(delta) * 0.28f).coerceIn(0.12f, 1f)
                val translateY = (delta * itemHeightPx * 0.88f)

                val isCurrent = abs(delta) < 0.4f

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
                            this.cameraDistance = 16f * density.density
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
                                if (isCurrent) {
                                    onConfirmSelect(config)
                                }
                            }
                        }
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 型号名称
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = config.model,
                                fontSize = if (isCurrent) 20.sp else 16.sp,
                                fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Medium,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp,
                                color = if (isCurrent) Color.White else GlassTheme.TextMuted
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${config.material}钢",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isCurrent) GlassTheme.IosBlue else GlassTheme.TextDim
                            )
                        }

                        // 规格及尺寸
                        Text(
                            text = "${config.od.nominal} × ${config.id.nominal} × ${config.height.nominal}",
                            fontSize = if (isCurrent) 13.sp else 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (isCurrent) Color.White.copy(alpha = 0.95f) else GlassTheme.TextDim
                        )

                        // 状态与指示
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "孔位 ${config.holePosition}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) GlassTheme.IosGreen else GlassTheme.TextDim
                            )
                            if (isCurrent) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "选定",
                                    tint = GlassTheme.CyanGlow,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
