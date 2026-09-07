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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlassTheme

/**
 * 极简纯净的玻璃渐变风格添加选择弹窗
 * 用户点击添加按钮后展示：
 * 1. 实图添加（拍照或相册 AI 视觉智能提取）
 * 2. 手动添加（手工键入各公差与尺寸参数）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChoiceBottomSheet(
    onDismiss: () -> Unit,
    onVisualAdd: () -> Unit,
    onManualAdd: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = 0.65f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E222B).copy(alpha = 0.95f),
                            Color(0xFF111317).copy(alpha = 0.98f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.25f),
                            GlassTheme.IosBlue.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 顶部标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "添加工艺规格",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "SELECT INPUT MODE · 工艺规格录入",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.8.sp,
                            color = GlassTheme.TextDim
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = GlassTheme.TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // 选项一：实图添加 (AI 视觉图纸识别)
                AddOptionCard(
                    icon = Icons.Default.AutoAwesome,
                    iconBgBrush = Brush.linearGradient(
                        listOf(GlassTheme.IosBlue, Color(0xFF64D2FF))
                    ),
                    title = "实图添加",
                    subtitle = "拍摄或上传工艺卡图纸，AI 自动提取尺寸及吊具推荐",
                    badge = "AI 智能识别",
                    badgeColor = GlassTheme.IosBlue,
                    onClick = {
                        onDismiss()
                        onVisualAdd()
                    }
                )

                // 选项二：手动添加
                AddOptionCard(
                    icon = Icons.Default.Edit,
                    iconBgBrush = Brush.linearGradient(
                        listOf(GlassTheme.IosGreen, Color(0xFF34C759))
                    ),
                    title = "手动添加",
                    subtitle = "自主填写型号、冷态外径、内径、高度及公差范围",
                    badge = "标准录入",
                    badgeColor = GlassTheme.IosGreen,
                    onClick = {
                        onDismiss()
                        onManualAdd()
                    }
                )
            }
        }
    }
}

@Composable
private fun AddOptionCard(
    icon: ImageVector,
    iconBgBrush: Brush,
    title: String,
    subtitle: String,
    badge: String,
    badgeColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "optionScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF232731).copy(alpha = 0.85f),
                        Color(0xFF181B22).copy(alpha = 0.95f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.04f)
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 左侧图标容器
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(iconBgBrush),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // 中间标题与副标题
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeColor.copy(alpha = 0.18f))
                            .border(0.8.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = badgeColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = GlassTheme.TextMuted
                )
            }

            // 右侧箭头
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = GlassTheme.TextDim,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
