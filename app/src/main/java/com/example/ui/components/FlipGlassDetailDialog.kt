package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.HangerRuleEngine
import com.example.model.MatchHighlight
import com.example.model.RingConfig
import com.example.ui.theme.GlassTheme
import kotlinx.coroutines.launch

/**
 * 具有 3D 翻转动效的毛玻璃详情弹窗 (3D Card Flip Dialog)
 */
@Composable
fun FlipGlassDetailDialog(
    config: RingConfig,
    highlight: MatchHighlight? = null,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onRestoreDefault: (() -> Unit)? = null
) {
    val is42 = config.material == "42"
    val isLarge = config.hanger == "大"
    val materialFullName = if (is42) "42CrMo (42钢)" else "50Mn (50钢)"
    
    // 3D 翻转动画控制器
    val rotationAnim = remember { Animatable(-90f) }
    val scaleAnim = remember { Animatable(0.75f) }
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(config) {
        launch {
            rotationAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
            )
        }
        launch {
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 280)
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val density = LocalDensity.current.density

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.70f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .padding(horizontal = 16.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            // 3D 翻转的毛玻璃核心卡片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        rotationY = rotationAnim.value
                        scaleX = scaleAnim.value
                        scaleY = scaleAnim.value
                        alpha = alphaAnim.value
                        cameraDistance = 16f * density
                    }
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF1E293B).copy(alpha = 0.94f),
                                Color(0xFF0F172A).copy(alpha = 0.98f)
                            )
                        )
                    )
                    .border(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.35f),
                                GlassTheme.CyanGlow.copy(alpha = 0.45f),
                                Color.White.copy(alpha = 0.1f)
                            )
                        ),
                        RoundedCornerShape(26.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* 阻止冒泡 */ }
                    .padding(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 顶部徽章 + 自定义标记
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(GlassTheme.CyanGlow.copy(alpha = 0.12f))
                                .border(1.dp, GlassTheme.CyanGlow.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(GlassTheme.CyanGlow)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "调质工艺标准 · PROCESS SPEC",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = GlassTheme.CyanGlow
                            )
                        }

                        if (config.isCustomized) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GlassTheme.EmeraldGlow.copy(alpha = 0.2f))
                                    .border(1.dp, GlassTheme.EmeraldGlow.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "✏️ 用户已自定义",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GlassTheme.EmeraldGlow
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 型号大标题
                    Text(
                        text = config.model,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = GlassTheme.TextWhite,
                        textAlign = TextAlign.Center
                    )

                    // 如果是尺寸反查命中的，显示匹配摘要条
                    if (highlight != null && highlight.isDimensionMatch) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(GlassTheme.EmeraldGlow.copy(alpha = 0.2f))
                                .border(1.dp, GlassTheme.EmeraldGlow.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${highlight.badgeTitle}: ${highlight.badgeDetail}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassTheme.EmeraldGlow,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 第一行：材料牌号 & 下料重量
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 材料卡片
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (is42) GlassTheme.Mat42Bg else GlassTheme.Mat50Bg)
                                .border(
                                    1.dp,
                                    (if (is42) GlassTheme.Mat42Text else GlassTheme.Mat50Text).copy(alpha = 0.45f),
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "材料牌号",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (is42) GlassTheme.Mat42Text else GlassTheme.Mat50Text
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = materialFullName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (is42) GlassTheme.Mat42Text else GlassTheme.Mat50Text
                                )
                            }
                        }

                        // 重量卡片
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF334155).copy(alpha = 0.45f))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "下料重量",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = GlassTheme.TextMuted
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = config.weight,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = GlassTheme.TextWhite
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 第二行：匹配吊具 & 孔位设定
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 匹配吊具
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF334155).copy(alpha = 0.45f))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "匹配吊架",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = GlassTheme.TextMuted
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${config.hanger}吊具",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isLarge) GlassTheme.CyanGlow else GlassTheme.EmeraldGlow
                                )
                            }
                        }

                        // 孔位设定
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF334155).copy(alpha = 0.45f))
                                .border(1.dp, GlassTheme.AmberGlow.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "孔位设定",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = GlassTheme.AmberGlow
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = config.holePosition,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = GlassTheme.AmberGlow
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 第三行：三维冷态尺寸与公差范围矩阵 (3D Precision Tolerance Matrix)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                            .border(
                                1.dp,
                                Brush.horizontalGradient(
                                    listOf(
                                        GlassTheme.CyanGlow.copy(alpha = 0.45f),
                                        Color.White.copy(alpha = 0.1f)
                                    )
                                ),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "产品冷态尺寸与公差范围 (冷却后)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GlassTheme.CyanGlow
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(GlassTheme.CyanGlow.copy(alpha = 0.2f))
                                        .border(1.dp, GlassTheme.CyanGlow.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "放 ${config.quantity}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = GlassTheme.TextWhite
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 3 列卡片展示：外径、内径、高度
                            val isOdHit = highlight?.matchedType == "OD" || highlight?.isMultiMatch == true
                            val isIdHit = highlight?.matchedType == "ID" || highlight?.isMultiMatch == true
                            val isHHit = highlight?.matchedType == "H" || highlight?.isMultiMatch == true

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // 外径
                                DimensionRangeBox(
                                    modifier = Modifier.weight(1f),
                                    label = "外径 (OD)",
                                    toleranceStr = config.od.formatted,
                                    rangeStr = config.od.rangeText,
                                    color = if (isOdHit) GlassTheme.EmeraldGlow else GlassTheme.CyanGlow,
                                    isHighlighted = isOdHit
                                )

                                // 内径
                                DimensionRangeBox(
                                    modifier = Modifier.weight(1f),
                                    label = "内径 (ID)",
                                    toleranceStr = config.id.formatted,
                                    rangeStr = config.id.rangeText,
                                    color = if (isIdHit) GlassTheme.EmeraldGlow else GlassTheme.CyanGlow,
                                    isHighlighted = isIdHit
                                )

                                // 高度
                                DimensionRangeBox(
                                    modifier = Modifier.weight(1f),
                                    label = "高度 (H)",
                                    toleranceStr = config.height.formatted,
                                    rangeStr = config.height.rangeText,
                                    color = if (isHHit) GlassTheme.EmeraldGlow else GlassTheme.AmberGlow,
                                    isHighlighted = isHHit
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "落地放置层数 (出炉落地码放)：${config.quantity}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = GlassTheme.TextMuted
                            )
                        }
                    }

                    // 标准调质工艺动效曲线 (版本 F 规范)
                    Spacer(modifier = Modifier.height(10.dp))
                    QuenchingTemperingCurveCard(
                        modifier = Modifier.fillMaxWidth(),
                        isCompact = true
                    )

                    // OP40 工序卡辅助参数 (物料编码 / 精车尺寸 / 热态尺寸)
                    if (config.materialCode.isNotBlank() || config.machinedSize.isNotBlank() || config.hotSize.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B).copy(alpha = 0.5f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "OP40工序卡全称: ${config.fullProductName.ifBlank { config.model }}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GlassTheme.TextWhite
                                    )
                                    if (config.materialCode.isNotBlank()) {
                                        Text(
                                            text = config.materialCode,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = GlassTheme.CyanGlow
                                        )
                                    }
                                }
                                if (config.machinedSize.isNotBlank()) {
                                    Text(
                                        text = "精车尺寸: ${config.machinedSize}",
                                        fontSize = 10.sp,
                                        color = GlassTheme.TextMuted
                                    )
                                }
                                if (config.hotSize.isNotBlank()) {
                                    Text(
                                        text = "热态尺寸: ${config.hotSize}",
                                        fontSize = 10.sp,
                                        color = GlassTheme.TextMuted
                                    )
                                }
                                if (config.billetInfo.isNotBlank()) {
                                    Text(
                                        text = "制坯参考: ${config.billetInfo}",
                                        fontSize = 10.sp,
                                        color = GlassTheme.TextDim
                                    )
                                }
                            }
                        }
                    }

                    // AI 数学与力学规律工艺匹配依据
                    val ruleRec = remember(config) {
                        HangerRuleEngine.recommend(
                            idNominal = config.id.nominal,
                            weightKg = HangerRuleEngine.extractWeightNumber(config.weight),
                            heightNominal = config.height.nominal
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A).copy(alpha = 0.9f))
                            .border(1.dp, GlassTheme.CyanGlow.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = GlassTheme.CyanGlow,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "AI 力学与热处理装炉匹配依据",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GlassTheme.CyanGlow
                                    )
                                }
                                Text(
                                    text = "${ruleRec.hanger}吊具 · ${ruleRec.holePosition}孔 · ${ruleRec.quantity}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GlassTheme.AmberGlow
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "🪝 吊臂跨度依据: ${ruleRec.hangerReason}",
                                fontSize = 10.sp,
                                color = GlassTheme.TextWhite.copy(alpha = 0.85f),
                                lineHeight = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "⚖️ 承重防塌依据: ${ruleRec.quantityReason}",
                                fontSize = 10.sp,
                                color = GlassTheme.TextWhite.copy(alpha = 0.85f),
                                lineHeight = 14.sp
                            )
                        }
                    }

                    // 备注警告区
                    if (config.notes != "-" && config.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF881337).copy(alpha = 0.35f))
                                .border(1.dp, GlassTheme.RoseGlow.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "注意",
                                    tint = GlassTheme.RoseGlow,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "工艺注意事项",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GlassTheme.RoseGlow
                                    )
                                    Text(
                                        text = config.notes,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = GlassTheme.TextWhite
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 核心操作区：编辑参数按钮 + 确认返回按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 编辑工艺参数按钮
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            GlassTheme.AmberGlow.copy(alpha = 0.85f),
                                            Color(0xFFEA580C)
                                        )
                                    )
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                .clickable { onEdit() },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "编辑工艺参数",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // 确认并关闭按钮
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF2563EB),
                                            Color(0xFF0284C7)
                                        )
                                    )
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "确认返回",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
