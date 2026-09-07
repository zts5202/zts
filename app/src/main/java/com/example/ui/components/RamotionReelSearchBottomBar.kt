package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MatchHighlight
import com.example.model.RingConfig
import com.example.ui.theme.GlassTheme
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 严格遵照 Ramotion Reel-Search 演示图效果实现：
 *
 * 搜索输入框本身就是 3D 卷轴的「聚焦窗口」（对焦点）。
 * 滚轴位于搜索框的上下，上下滑动时，候选型号从输入框背后向上/向下旋转翻折滚动！
 *
 * 并且布局严格置底：
 * 搜索框与卷轴固定停留在屏幕底部（距底 24dp，伴随软键盘升降），绝对不会发生“点击搜索框跳到最上面”的混乱现象！
 */
@Composable
fun RamotionReelSearchBottomBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    searchResults: List<Pair<RingConfig, MatchHighlight>>,
    onSelectModel: (RingConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()

    val candidates = remember(searchResults) { searchResults.map { it.first } }
    val itemHeightDp = 52.dp
    val itemHeightPx = with(density) { itemHeightDp.toPx() }

    // 滚轴在候选条目中的偏移索引
    val reelOffset = remember { Animatable(0f) }

    LaunchedEffect(candidates) {
        reelOffset.snapTo(0f)
    }

    val visibleRadius = 3
    val totalWheelHeight = itemHeightDp * 4.8f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 当有搜索结果且用户输入时，展示与搜索框严丝合缝连为一体的 Ramotion 3D 垂直滚轴
        AnimatedVisibility(
            visible = searchQuery.isNotEmpty() && candidates.isNotEmpty(),
            enter = fadeIn(tween(220)) + expandVertically(tween(280)),
            exit = fadeOut(tween(180)) + shrinkVertically(tween(220))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                // 微标签提示
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "上下滑动滚轴挑选型号 (${candidates.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTheme.IosBlue
                    )
                    Text(
                        text = "点击条目直接查看工艺",
                        fontSize = 11.sp,
                        color = GlassTheme.TextDim
                    )
                }

                // 3D 柱面滚轴视口 (Reel Viewport)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(totalWheelHeight)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF10131B).copy(alpha = 0.85f))
                        .border(
                            1.2.dp,
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.12f),
                                    GlassTheme.IosBlue.copy(alpha = 0.40f),
                                    Color.White.copy(alpha = 0.08f)
                                )
                            ),
                            RoundedCornerShape(20.dp)
                        )
                        // 上下两端羽化遮罩（完全匹配 Ramotion 动图上下虚化淡出效果）
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFF10131B), Color.Transparent),
                                    startY = 0f,
                                    endY = size.height * 0.32f
                                )
                            )
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xFF10131B)),
                                    startY = size.height * 0.68f,
                                    endY = size.height
                                )
                            )
                        }
                        .pointerInput(candidates.size) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    val current = reelOffset.value
                                    val targetSnap = current.roundToInt().coerceIn(0, (candidates.size - 1).coerceAtLeast(0))
                                    coroutineScope.launch {
                                        reelOffset.animateTo(
                                            targetValue = targetSnap.toFloat(),
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        )
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
                    // 中央游标指示框（高亮框）
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                            .height(itemHeightDp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GlassTheme.IosBlue.copy(alpha = 0.15f))
                            .border(
                                width = 1.2.dp,
                                color = GlassTheme.IosBlue.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(12.dp)
                            )
                    )

                    // 渲染滚轴条目（应用 3D 柱面 X 轴旋转和视差）
                    val currentOffsetVal = reelOffset.value
                    val centerIndex = currentOffsetVal.roundToInt()
                    val minVisible = (centerIndex - visibleRadius - 1).coerceAtLeast(0)
                    val maxVisible = (centerIndex + visibleRadius + 1).coerceAtMost(candidates.size - 1)

                    for (i in minVisible..maxVisible) {
                        val config = candidates[i]
                        val delta = i - currentOffsetVal
                        if (abs(delta) <= 2.8f) {
                            val rotationAngleX = (delta * 26f).coerceIn(-70f, 70f)
                            val scale = (1f - abs(delta) * 0.10f).coerceIn(0.72f, 1f)
                            val alpha = (1f - abs(delta) * 0.30f).coerceIn(0.12f, 1f)
                            val translateY = delta * itemHeightPx * 0.88f
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
                                            onSelectModel(config)
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
                                    // 型号名称
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = config.model,
                                            fontSize = if (isCurrent) 19.sp else 15.sp,
                                            fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Medium,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (isCurrent) Color.White else GlassTheme.TextMuted
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${config.material}钢",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (isCurrent) GlassTheme.IosBlue else GlassTheme.TextDim
                                        )
                                    }

                                    // 冷态尺寸
                                    Text(
                                        text = "${config.od.nominal} × ${config.id.nominal} × ${config.height.nominal}",
                                        fontSize = if (isCurrent) 13.sp else 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isCurrent) Color.White.copy(alpha = 0.9f) else GlassTheme.TextDim
                                    )

                                    // 孔位
                                    Text(
                                        text = "孔位 ${config.holePosition}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) GlassTheme.IosGreen else GlassTheme.TextDim
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==================== 底置透明大搜索框（始终锁定在底部） ====================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .clip(RoundedCornerShape(31.dp))
                .background(Color(0xFF161A24).copy(alpha = 0.45f)) // 全透明毛玻璃质感
                .border(
                    width = 1.4.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.30f),
                            GlassTheme.IosBlue.copy(alpha = 0.45f),
                            Color.White.copy(alpha = 0.15f)
                        )
                    ),
                    shape = RoundedCornerShape(31.dp)
                )
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "搜索",
                    tint = if (searchQuery.isNotEmpty()) GlassTheme.IosBlue else GlassTheme.TextMuted,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.6.sp
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(GlassTheme.IosBlue),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                            onSearchSubmit()
                        }
                    ),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "搜索型号或尺寸 (如 485 或 858)...",
                                    color = GlassTheme.TextDim.copy(alpha = 0.8f),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchChange("") },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "清除",
                            tint = GlassTheme.TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
