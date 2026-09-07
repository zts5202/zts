package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MatchHighlight
import com.example.model.RingConfig
import com.example.ui.theme.GlassTheme
import kotlin.math.abs

/**
 * 🌟 原生 Jetpack Compose 打造的 Ramotion Folding Cell (3D 折纸折叠展开卡片)
 * 纯物理模拟：
 * 1. 折叠态：轻巧紧凑的工序卡封面；
 * 2. 展开态：沿 X 轴以 3D 铰链级联翻折 2~3 折，伴随折痕光影渐变 (Lighting & Shadow)，
 *    展现完整的公差蓝图、热工推算与工艺操作按钮；
 * 3. 100% 兼容并保留软件既有功能与交互！
 */
@Composable
fun FoldingCellCard(
    config: RingConfig,
    modifier: Modifier = Modifier,
    highlight: MatchHighlight? = null,
    initiallyExpanded: Boolean = false,
    onEdit: () -> Unit,
    onFullDetail: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }

    // 翻折动画进度 (0f = 完全折叠，1f = 完全展开)
    val foldProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessLow
        ),
        label = "foldProgress"
    )

    val is42 = config.material == "42"
    val isDimMatched = highlight?.isDimensionMatch == true
    val themeColor = if (is42) GlassTheme.IosBlue else Color(0xFFFF9F0A)
    val density = LocalDensity.current.density

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF141417))
            .border(
                width = if (isDimMatched) 1.5.dp else 1.dp,
                color = if (isDimMatched) GlassTheme.IosGreen.copy(alpha = 0.65f)
                else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(1.dp)
    ) {
        // ========================【第 1 折：封面折叠头 (Fold Head)】========================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(
                    if (foldProgress > 0.05f)
                        RoundedCornerShape(topStart = 19.dp, topEnd = 19.dp)
                    else
                        RoundedCornerShape(19.dp)
                )
                .background(
                    Brush.linearGradient(
                        listOf(
                            if (isDimMatched) Color(0xFF1B2B20) else Color(0xFF1E1F24),
                            Color(0xFF17171A)
                        )
                    )
                )
                .clickable { isExpanded = !isExpanded }
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 顶行：型号主标 + 材质 + 3D折叠状态指示器
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 吊具方形徽标
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(themeColor.copy(alpha = 0.18f))
                                .border(1.dp, themeColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (config.hanger == "大") "大" else "小",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColor
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = config.model,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (is42) GlassTheme.Mat42Bg else GlassTheme.Mat50Bg)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${config.material}钢",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (is42) GlassTheme.Mat42Text else GlassTheme.Mat50Text
                                    )
                                }
                                if (config.isCustomized) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(GlassTheme.IosBlue.copy(alpha = 0.2f))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "已改",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = GlassTheme.IosBlue
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "外径 ${config.od.nominal} · 内径 ${config.id.nominal} · 高度 ${config.height.nominal}",
                                fontSize = 11.sp,
                                color = GlassTheme.TextDim
                            )
                        }
                    }

                    // 右侧：Folding Cell 折纸动效指示器
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "孔位 ${config.holePosition}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassTheme.IosGreen
                            )
                            Text(
                                text = if (isExpanded) "折叠合拢" else "折叠翻开",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = themeColor
                            )
                        }

                        // 翻折 3D 旋转小图标
                        val arrowRotation by animateFloatAsState(
                            targetValue = if (isExpanded) 180f else 0f,
                            label = "arrowRotation"
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.06f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "折叠" else "展开",
                                tint = themeColor,
                                modifier = Modifier
                                    .size(18.dp)
                                    .graphicsLayer { rotationZ = arrowRotation }
                            )
                        }
                    }
                }

                // 命中高亮气泡 (如果存在)
                if (highlight != null && highlight.badgeTitle.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isDimMatched) GlassTheme.IosGreen.copy(alpha = 0.15f)
                                else GlassTheme.IosBlue.copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = highlight.badgeTitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDimMatched) GlassTheme.IosGreen else GlassTheme.IosBlue
                        )
                    }
                }
            }
        }

        // ========================【第 2 折：3D 折叠详细公差层 (Fold Part 2)】========================
        if (foldProgress > 0.01f) {
            // 计算第 2 折的 3D 翻转角度 (-90° 到 0°)
            val fold2Progress = (foldProgress * 1.5f).coerceIn(0f, 1f)
            val rotationX2 = (1f - fold2Progress) * -90f
            val shadowAlpha2 = (1f - fold2Progress) * 0.8f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        this.rotationX = rotationX2
                        this.cameraDistance = 16f * density
                        this.transformOrigin = TransformOrigin(0.5f, 0f) // 从顶边缘铰链向下翻开
                    }
                    .background(Color(0xFF19191C))
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.04f),
                        RoundedCornerShape(0.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // 模拟折纸顶部的深色折痕线 (Paper Fold Crease)
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        thickness = 1.dp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📐 机械公差精密蓝图",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = GlassTheme.IosBlue
                        )
                        Text(
                            text = "单位: mm",
                            fontSize = 10.sp,
                            color = GlassTheme.TextDim
                        )
                    }

                    // 三列公差面板：外径、内径、高度
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 外径 OD
                        ToleranceMiniBlock(
                            label = "外径 OD",
                            nominal = config.od.nominal,
                            upper = config.od.upperTol,
                            lower = config.od.lowerTol,
                            modifier = Modifier.weight(1f),
                            tintColor = Color(0xFF64D2FF)
                        )
                        // 内径 ID
                        ToleranceMiniBlock(
                            label = "内径 ID",
                            nominal = config.id.nominal,
                            upper = config.id.upperTol,
                            lower = config.id.lowerTol,
                            modifier = Modifier.weight(1f),
                            tintColor = Color(0xFF30D158)
                        )
                        // 高度 H
                        ToleranceMiniBlock(
                            label = "高度 H",
                            nominal = config.height.nominal,
                            upper = config.height.upperTol,
                            lower = config.height.lowerTol,
                            modifier = Modifier.weight(1f),
                            tintColor = Color(0xFFFF9F0A)
                        )
                    }
                }

                // 折纸背光阴影遮罩 (Fold Lighting Shadow)
                if (shadowAlpha2 > 0.01f) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = shadowAlpha2))
                    )
                }
            }
        }

        // ========================【第 3 折：3D 折叠热工推算与操作层 (Fold Part 3)】========================
        if (foldProgress > 0.4f) {
            // 计算第 3 折的 3D 翻转角度 (-90° 到 0°)
            val fold3Progress = ((foldProgress - 0.4f) / 0.6f).coerceIn(0f, 1f)
            val rotationX3 = (1f - fold3Progress) * -90f
            val shadowAlpha3 = (1f - fold3Progress) * 0.85f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        this.rotationX = rotationX3
                        this.cameraDistance = 16f * density
                        this.transformOrigin = TransformOrigin(0.5f, 0f) // 接着从第 2 折底部边缘翻开
                    }
                    .clip(RoundedCornerShape(bottomStart = 19.dp, bottomEnd = 19.dp))
                    .background(Color(0xFF1E1F24))
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.04f),
                        RoundedCornerShape(bottomStart = 19.dp, bottomEnd = 19.dp)
                    )
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 折痕阴影线
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.06f),
                        thickness = 1.dp
                    )

                    // 热工指标展示条
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.25f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Whatshot,
                                contentDescription = null,
                                tint = Color(0xFFFF453A),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (config.hotSize.isNotBlank()) "热态: ${config.hotSize}" else "装炉: 叠 ${config.quantity} 件 / 单重 ${config.weight}kg",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }

                        Text(
                            text = "${config.hanger}吊具",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = GlassTheme.TextMuted
                        )
                    }

                    // 底部原厂操作按钮 (100% 保持软件功能)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 查看 3D 翻转全息卡片
                        Button(
                            onClick = onFullDetail,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2C2C2E)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Flip,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("3D图纸卡", fontSize = 11.sp, color = Color.White)
                        }

                        // 编辑此规格 (原功能完全不变)
                        Button(
                            onClick = onEdit,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GlassTheme.IosBlue
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.2f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("修改规格", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        // 收起折叠按钮
                        OutlinedButton(
                            onClick = { isExpanded = false },
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.weight(0.9f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("收起折纸", fontSize = 11.sp, color = GlassTheme.TextMuted)
                        }
                    }
                }

                // 阴影遮罩
                if (shadowAlpha3 > 0.01f) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = shadowAlpha3))
                    )
                }
            }
        }
    }
}

/**
 * 公差微卡片单元
 */
@Composable
private fun ToleranceMiniBlock(
    label: String,
    nominal: Any,
    upper: Any?,
    lower: Any?,
    tintColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF242428))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Column {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = GlassTheme.TextMuted
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$nominal",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = tintColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            val upperStr = when {
                upper == null -> "0"
                upper is Number && upper.toDouble() >= 0 -> "+$upper"
                else -> "$upper"
            }
            val lowerStr = when {
                lower == null -> "0"
                lower is Number && lower.toDouble() >= 0 -> "+$lower"
                else -> "$lower"
            }
            Text(
                text = "$upperStr / $lowerStr",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = GlassTheme.TextDim
            )
        }
    }
}
