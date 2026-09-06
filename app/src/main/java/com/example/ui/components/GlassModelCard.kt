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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
 * 带有毛玻璃折射与高亮外边框的模型卡片 (Glassmorphism Grid Item Card)
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
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        if (isDimMatched) Color(0xFF064E3B).copy(alpha = 0.45f) else Color(0xFF1E293B).copy(alpha = 0.65f),
                        Color(0xFF0F172A).copy(alpha = 0.75f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        if (isDimMatched) GlassTheme.EmeraldGlow.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.22f),
                        if (isDimMatched) GlassTheme.CyanGlow.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.05f)
                    )
                ),
                RoundedCornerShape(18.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = GlassTheme.CyanGlow),
                onClick = onClick
            )
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 顶部：型号名称 + 材质徽标 + 自定义标识
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = config.model,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = GlassTheme.TextWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // 若被用户自定义修改，显示小绿点
                    if (config.isCustomized) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(GlassTheme.EmeraldGlow.copy(alpha = 0.25f))
                                .border(0.8.dp, GlassTheme.EmeraldGlow.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "已自定义",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassTheme.EmeraldGlow
                            )
                        }
                    }

                    // 材质芯片 (毛玻璃微晶)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (is42) GlassTheme.Mat42Bg else GlassTheme.Mat50Bg)
                            .border(
                                1.dp,
                                (if (is42) GlassTheme.Mat42Text else GlassTheme.Mat50Text).copy(alpha = 0.5f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${config.material}钢",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (is42) GlassTheme.Mat42Text else GlassTheme.Mat50Text
                        )
                    }
                }
            }

            // 如果有尺寸命中徽章，高亮提示
            if (highlight != null && highlight.badgeTitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isDimMatched) GlassTheme.EmeraldGlow.copy(alpha = 0.18f)
                            else GlassTheme.CyanGlow.copy(alpha = 0.15f)
                        )
                        .border(
                            1.dp,
                            if (isDimMatched) GlassTheme.EmeraldGlow.copy(alpha = 0.6f)
                            else GlassTheme.CyanGlow.copy(alpha = 0.4f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Column {
                        Text(
                            text = highlight.badgeTitle,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDimMatched) GlassTheme.EmeraldGlow else GlassTheme.CyanGlow
                        )
                        if (highlight.badgeDetail.isNotBlank()) {
                            Text(
                                text = highlight.badgeDetail,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Normal,
                                color = GlassTheme.TextWhite.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 中部：吊具与孔位高亮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isLarge) GlassTheme.CyanGlow else GlassTheme.EmeraldGlow)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${config.hanger}吊具",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLarge) GlassTheme.CyanGlow else GlassTheme.EmeraldGlow
                    )
                }

                // 孔位
                Text(
                    text = "孔位 ${config.holePosition}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = GlassTheme.AmberGlow
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            // 尺寸与公差胶囊
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.75f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Column {
                    Text(
                        text = "外:${config.od.formatted} 内:${config.id.formatted}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTheme.CyanGlow
                    )
                    Text(
                        text = "高:${config.height.formatted} | 放 ${config.quantity}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlassTheme.TextWhite.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 底部：重量与快速编辑按键
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = config.weight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = GlassTheme.TextMuted
                )

                // 快捷编辑按钮
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF334155).copy(alpha = 0.6f))
                        .border(0.8.dp, GlassTheme.CyanGlow.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .clickable { onEdit() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "编辑",
                            tint = GlassTheme.CyanGlow,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "编辑",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlassTheme.CyanGlow
                        )
                    }
                }
            }
        }
    }
}
