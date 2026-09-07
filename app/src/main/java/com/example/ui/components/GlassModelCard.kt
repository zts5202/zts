package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MatchHighlight
import com.example.model.RingConfig
import com.example.ui.theme.GlassTheme

/**
 * Apple iOS (Cupertino) 极简风格模型卡片
 * 纯粹大圆角、优雅次级深灰、iOS 药丸微标、扁平清晰的尺寸信息排版，告别杂乱厚重边框
 */
@Composable
fun GlassModelCard(
    config: RingConfig,
    highlight: MatchHighlight? = null,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    val is42 = config.material == "42"
    val isLarge = config.hanger == "大"
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDimMatched = highlight?.isDimensionMatch == true

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isDimMatched) Color(0xFF1E2D24)
                else Color(0xFF1C1C1E)
            )
            .border(
                width = if (isDimMatched) 1.5.dp else 1.dp,
                color = if (isDimMatched) GlassTheme.IosGreen.copy(alpha = 0.6f)
                        else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color.White.copy(alpha = 0.15f)),
                onClick = onClick
            )
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 顶行：型号主标题 + 材质胶囊标签
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = config.model,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (config.isCustomized) {
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

                    // iOS 风格极简材质胶囊
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
                }
            }

            // 搜索命中提醒（极简气泡）
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = highlight.badgeTitle,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDimMatched) GlassTheme.IosGreen else GlassTheme.IosBlue
                        )
                        if (highlight.badgeDetail.isNotBlank()) {
                            Text(
                                text = highlight.badgeDetail,
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 第二行：吊具类型与醒目 iOS 药丸孔位
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isLarge) GlassTheme.IosBlue else GlassTheme.IosTeal)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${config.hanger}吊具",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                // 标志性 iOS 药丸孔位徽章 (Pill Badge)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(GlassTheme.IosOrange.copy(alpha = 0.18f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "孔位 ${config.holePosition}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTheme.IosOrange
                    )
                }
            }

            // iOS 极细分隔线
            HorizontalDivider(
                color = Color.White.copy(alpha = 0.06f),
                thickness = 0.8.dp
            )

            // 第三行：整齐紧凑的冷态规格尺寸（采用 Monospace 遥测数字呈现）
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF101115))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "外 Φ${config.od.nominal}",
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "内 Φ${config.id.nominal}",
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "高 ${config.height.nominal}mm",
                        fontSize = 10.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        color = GlassTheme.TextMuted
                    )
                    Text(
                        text = "叠放 ${config.quantity}件",
                        fontSize = 10.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTheme.IosTeal
                    )
                }
            }

            // 底部：重量与轻量编辑触点
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = config.weight,
                    fontSize = 10.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = FontWeight.Normal,
                    color = GlassTheme.TextDim
                )

                // 极简扁平轻量编辑按钮
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .clickable { onEdit() }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "编辑",
                        tint = GlassTheme.TextMuted,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "编辑",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlassTheme.TextMuted
                    )
                }
            }
        }
    }
}
