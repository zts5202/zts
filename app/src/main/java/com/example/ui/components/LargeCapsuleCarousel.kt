package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MatchHighlight
import com.example.model.RingConfig
import com.example.ui.theme.GlassTheme

/**
 * 灵感源自 XAnimation/Lottie 弹性动画的特大胶囊型型号选择组件
 * 位于半透明搜索框底部，支持高弹性回弹、水波涟漪与水平滑动自由挑选
 */
@Composable
fun LargeCapsuleCarousel(
    items: List<Pair<RingConfig, MatchHighlight>>,
    onSelect: (RingConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // 顶部微标签提示栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(GlassTheme.IosBlue)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "滑动胶囊快速定位型号 (${items.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GlassTheme.IosBlue
                )
            }

            Text(
                text = "左右滑动挑选 · 点击开启详情",
                fontSize = 11.sp,
                color = GlassTheme.TextDim
            )
        }

        // 横向高质感特大胶囊滑动区
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(items, key = { _, item -> item.first.model }) { index, (config, highlight) ->
                LargeCapsuleItem(
                    config = config,
                    highlight = highlight,
                    index = index,
                    onClick = { onSelect(config) }
                )
            }
        }
    }
}

/**
 * 单个特大胶囊（Pill / Capsule）：
 * 具备物理按压缩放、动态呼吸光感边框、材质徽标、孔位以及实测尺寸匹配高亮
 */
@Composable
private fun LargeCapsuleItem(
    config: RingConfig,
    highlight: MatchHighlight,
    index: Int,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 弹性进入与按压物理弹簧 (Spring physics)
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    )

    // 胶囊进入动画：根据 index 产生错落进入微动效
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 350, delayMillis = (index * 40).coerceAtMost(300)),
        label = "animatedAlpha"
    )

    val isDimensionMatch = highlight.isDimensionMatch
    val is42 = config.material.contains("42")

    // 胶囊边框与渐变背景
    val borderBrush = if (isDimensionMatch) {
        Brush.linearGradient(
            listOf(
                GlassTheme.IosGreen.copy(alpha = 0.8f),
                GlassTheme.IosTeal.copy(alpha = 0.6f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.05f)
            )
        )
    }

    val capsuleBackground = if (isDimensionMatch) {
        Brush.horizontalGradient(
            listOf(
                Color(0xFF132A1C).copy(alpha = 0.92f),
                Color(0xFF0F1A15).copy(alpha = 0.96f)
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                Color(0xFF1C2028).copy(alpha = 0.88f),
                Color(0xFF14171E).copy(alpha = 0.94f)
            )
        )
    }

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                alpha = animatedAlpha
            }
            .clip(RoundedCornerShape(32.dp)) // 经典大胶囊圆角
            .background(capsuleBackground)
            .border(
                width = if (isDimensionMatch) 1.8.dp else 1.2.dp,
                brush = borderBrush,
                shape = RoundedCornerShape(32.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .widthIn(min = 210.dp, max = 260.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 第一行：特大型号文本 + 材质小胶囊
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = config.model,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (config.isCustomized) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(GlassTheme.IosBlue.copy(alpha = 0.25f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "已调",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassTheme.IosBlue
                            )
                        }
                    }
                }

                // 材质标识徽标
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (is42) GlassTheme.Mat42Bg else GlassTheme.Mat50Bg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${config.material}钢",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (is42) GlassTheme.Mat42Text else GlassTheme.Mat50Text
                    )
                }
            }

            // 第二行：孔位参数与工序卡品名
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "孔位 ${config.holePosition} · ${config.hanger}吊",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = GlassTheme.TextPrimary
                )

                if (config.weight.isNotBlank()) {
                    Text(
                        text = "${config.weight}kg",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = GlassTheme.TextMuted
                    )
                }
            }

            // 第三行：冷态尺寸规格或匹配高亮指示条
            if (isDimensionMatch && highlight.badgeDetail.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(GlassTheme.IosGreen.copy(alpha = 0.18f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Icon(
                        Icons.Default.Straighten,
                        contentDescription = null,
                        tint = GlassTheme.IosGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = highlight.badgeDetail,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTheme.IosGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    text = "${config.od.nominal} × ${config.id.nominal} × ${config.height.nominal} mm",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = GlassTheme.TextDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
