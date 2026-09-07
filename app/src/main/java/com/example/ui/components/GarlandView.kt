package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RingConfig
import com.example.ui.theme.GlassTheme
import kotlin.math.abs

/**
 * 工艺组数据结构
 */
data class GarlandCategory(
    val title: String,
    val subtitle: String,
    val hangerType: String,
    val badgeColor: Color,
    val configs: List<RingConfig>
)

/**
 * 🌟 原生 Jetpack Compose 打造的 Ramotion Garland View (花环级联交错流)
 * 核心原理：
 * 1. 外层水平轴 (HorizontalPager) 管理工艺系列卡牌轮播；
 * 2. 内层垂直轴 (LazyColumn) 管理子规格卡片；
 * 3. 核心动效：基于 Pager 滑动偏移与子项 Index 计算的「多米诺时序差平移 + 微旋转 + 深度缩放」。
 */
@Composable
fun GarlandProcessView(
    allConfigs: List<RingConfig>,
    modifier: Modifier = Modifier,
    onSelectConfig: (RingConfig) -> Unit,
    onEditConfig: (RingConfig) -> Unit
) {
    // 将 38 种工艺规格智能整理为 4 个富有车间工业意义的系列卡组
    val categories = remember(allConfigs) {
        listOf(
            GarlandCategory(
                title = "大吊具 · 42CrMo 系列",
                subtitle = "核心重载调质规格 · 高频排产",
                hangerType = "大",
                badgeColor = GlassTheme.IosBlue,
                configs = allConfigs.filter { it.hanger == "大" && it.material == "42" }
            ),
            GarlandCategory(
                title = "大吊具 · 50Mn 系列",
                subtitle = "中碳调质辗环件 · 标准矩阵",
                hangerType = "大",
                badgeColor = Color(0xFFFF9F0A),
                configs = allConfigs.filter { it.hanger == "大" && it.material == "50" }
            ),
            GarlandCategory(
                title = "小吊具 · 精密装炉系列",
                subtitle = "紧凑间隙炉膛 · 专用吊具",
                hangerType = "小",
                badgeColor = GlassTheme.IosGreen,
                configs = allConfigs.filter { it.hanger == "小" }
            ),
            GarlandCategory(
                title = "全部工艺全息矩阵",
                subtitle = "车间完整 38 款工艺库速查",
                hangerType = "全",
                badgeColor = Color(0xFFBF5AF2),
                configs = allConfigs
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { categories.size })

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF131316))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .padding(top = 16.dp, bottom = 12.dp)
    ) {
        // 顶部 Garland 栏标与分页指示器
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(categories[pagerState.currentPage].badgeColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GARLAND 工艺花环流",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    color = Color.White
                )
            }

            // iOS 风格滑动小圆点指示器
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                categories.forEachIndexed { index, cat ->
                    val isSelected = pagerState.currentPage == index
                    val dotWidth by animateFloatAsState(
                        targetValue = if (isSelected) 18f else 6f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "dotWidth"
                    )
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(dotWidth.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (isSelected) cat.badgeColor
                                else Color.White.copy(alpha = 0.2f)
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 外层水平 Pager (Garland Horizontal Axis)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp
        ) { pageIndex ->
            val category = categories[pageIndex]

            // 计算当前页相对于视口的滑动偏移量 (-1f 到 1f 之间)
            val pageOffset = (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1C1C1E))
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                    .padding(12.dp)
            ) {
                // 系列头部大卡片 (Category Header Card)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    category.badgeColor.copy(alpha = 0.25f),
                                    Color(0xFF242428)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            category.badgeColor.copy(alpha = 0.4f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = category.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = category.subtitle,
                                fontSize = 11.sp,
                                color = GlassTheme.TextMuted
                            )
                        }

                        // 数量胶囊
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(category.badgeColor.copy(alpha = 0.3f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${category.configs.size} 款",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = category.badgeColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 内层垂直列表 (Garland Vertical Axis)
                // 核心：基于 pageOffset 和 index 产生的 Staggered Cascade 多米诺级联滑动！
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    itemsIndexed(category.configs, key = { _, item -> item.model }) { index, config ->
                        // 🌟 Ramotion Garland 算法核心实现：
                        // 随着 index 递增，平移延迟系数递增，产生波浪般的花环脱离动画
                        val staggerFactor = (index * 0.12f).coerceAtMost(0.8f)
                        val staggerOffset = pageOffset * (1f + staggerFactor) * 160f
                        val staggerRotation = pageOffset * (4f + index * 1.5f).coerceAtMost(18f)
                        val staggerAlpha = (1f - abs(pageOffset) * (0.3f + index * 0.08f)).coerceIn(0.2f, 1f)
                        val staggerScale = (1f - abs(pageOffset) * (0.05f + index * 0.02f)).coerceIn(0.85f, 1f)

                        GarlandItemCard(
                            config = config,
                            categoryColor = category.badgeColor,
                            modifier = Modifier
                                .graphicsLayer {
                                    translationX = -staggerOffset // 横向多米诺时差滑动
                                    rotationZ = -staggerRotation   // 3D 纸牌微倾斜
                                    alpha = staggerAlpha           // 优雅渐隐渐显
                                    scaleX = staggerScale
                                    scaleY = staggerScale
                                },
                            onClick = { onSelectConfig(config) },
                            onEdit = { onEditConfig(config) }
                        )
                    }
                }
            }
        }

        // 底部滑动指引微提示
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Swipe,
                contentDescription = null,
                tint = GlassTheme.TextDim,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "左右滑动切换系列 · 体验多米诺级联折纸交互",
                fontSize = 11.sp,
                color = GlassTheme.TextDim
            )
        }
    }
}

/**
 * Garland 每一行动态多米诺子卡片 (Garland Cascade Card)
 */
@Composable
fun GarlandItemCard(
    config: RingConfig,
    categoryColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF242428))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：型号大标与材质
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(categoryColor.copy(alpha = 0.18f))
                        .border(1.dp, categoryColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (config.hanger == "大") "大" else "小",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = categoryColor
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = config.model,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${config.material}钢",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = GlassTheme.TextMuted
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "外 Φ${config.od.nominal} · 内 Φ${config.id.nominal} · 高 ${config.height.nominal}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = GlassTheme.TextDim
                    )
                }
            }

            // 右侧：车间核心装炉指标 (孔位与叠层)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    // 孔位
                    Text(
                        text = "孔位 ${config.holePosition}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = GlassTheme.IosGreen
                    )
                    Text(
                        text = "装炉 ${config.quantity}件 · ${config.weight}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = GlassTheme.TextDim
                    )
                }

                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "查看详情",
                    tint = GlassTheme.TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
