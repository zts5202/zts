package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlassTheme

/**
 * 🌟 Leonxlnx Taste-Skill 原则重构的工业控制台顶部中枢
 * 包含：
 * 1. 顶部极简工业标头 (状态呼吸灯 + 规格计数 + 紧凑集成式快捷设置)
 * 2. 整合式工业遥测控制条：AI识图触发器 + 实时公差/型号混合输入器 + 物理触感搜索
 * 3. 热门规程/实测快查微标签
 */
@Composable
fun IndustrialControlHeader(
    totalSpecsCount: Int,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onOpenAddChoice: () -> Unit,
    onOpenApiKey: () -> Unit,
    onQuickTagSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. 顶栏极简信息头：深色冷调遥测标头
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 工控呼吸状态灯
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(GlassTheme.IosGreen)
                        .drawBehind {
                            drawCircle(
                                color = GlassTheme.IosGreen.copy(alpha = 0.4f),
                                radius = size.minDimension * 0.9f
                            )
                        }
                )

                Spacer(modifier = Modifier.width(9.dp))

                Column {
                    Text(
                        text = "调质炉工艺中枢",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "THERMAL PROCESS ENGINE · $totalSpecsCount 款出厂标准",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.8.sp,
                        color = GlassTheme.TextDim
                    )
                }
            }

            // 右侧紧凑工具微胶囊 (API 密钥状态与配置)
            val keySource = remember { MutableInteractionSource() }
            val isKeyPressed by keySource.collectIsPressedAsState()
            val keyScale by animateFloatAsState(
                targetValue = if (isKeyPressed) 0.94f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "keyScale"
            )

            Box(
                modifier = Modifier
                    .graphicsLayer { scaleX = keyScale; scaleY = keyScale }
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF14161B))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = keySource,
                        indication = null,
                        onClick = onOpenApiKey
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.VpnKey,
                        contentDescription = "API 密钥",
                        tint = GlassTheme.IosTeal,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "API KEY",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = GlassTheme.TextPrimary
                    )
                }
            }
        }

        // 2. 极简玻璃渐变检索与录入中枢 (Glass Gradient Search & Add Bar)
        // 左侧放大镜图标 + 高清晰度全宽搜索框 + 清空按钮 + 右侧高辨识度「+ 添加」玻璃渐变物理触觉按钮
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1B1E26).copy(alpha = 0.85f),
                            Color(0xFF111317).copy(alpha = 0.95f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            if (searchQuery.isNotBlank()) GlassTheme.IosBlue.copy(alpha = 0.7f)
                            else Color.White.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧搜索图标微标
                Box(
                    modifier = Modifier
                        .padding(start = 6.dp, end = 4.dp)
                        .size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索",
                        tint = if (searchQuery.isNotBlank()) GlassTheme.IosBlue else GlassTheme.TextDim,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 中间：高清晰度搜索输入框 (支持型号 / 实测三维尺寸反查)
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    androidx.compose.material3.TextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "搜索型号 (如485) 或实测尺寸...",
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                color = GlassTheme.TextDim
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSearchChange("") },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "清空",
                                        tint = GlassTheme.TextDim,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        ),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onSearch = {
                                keyboardController?.hide()
                                onSearchSubmit()
                            }
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = GlassTheme.IosBlue
                        )
                    )
                }

                // 右侧：优雅的玻璃渐变「添加」按钮 (点击打开实图/手动选择器)
                val addSource = remember { MutableInteractionSource() }
                val isAddPressed by addSource.collectIsPressedAsState()
                val addScale by animateFloatAsState(
                    targetValue = if (isAddPressed) 0.92f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "addScale"
                )

                Box(
                    modifier = Modifier
                        .graphicsLayer { scaleX = addScale; scaleY = addScale }
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    GlassTheme.IosBlue,
                                    Color(0xFF2563EB)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.4f),
                                    GlassTheme.IosBlue.copy(alpha = 0.2f)
                                )
                            ),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable(
                            interactionSource = addSource,
                            indication = null,
                            onClick = onOpenAddChoice
                        )
                        .padding(horizontal = 13.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "添加",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "添加",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 3. 底部车间高频工艺快速索引胶囊 (Quick Telemetry Filters)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "速查:",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = GlassTheme.TextDim
            )

            listOf("485", "858", "42CrMo", "50Mn", "大吊具").forEach { tag ->
                val isSelected = searchQuery == tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isSelected) GlassTheme.IosBlue.copy(alpha = 0.25f)
                            else Color(0xFF17181D)
                        )
                        .border(
                            1.dp,
                            if (isSelected) GlassTheme.IosBlue.copy(alpha = 0.5f)
                            else Color.White.copy(alpha = 0.05f),
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onQuickTagSelect(tag) }
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = tag,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else GlassTheme.TextMuted
                    )
                }
            }
        }
    }
}
