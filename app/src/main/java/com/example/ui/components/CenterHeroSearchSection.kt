package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
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

/**
 * 屏幕中央特大高透玻璃搜索核心区：
 * 包含：
 * 1. 顶部操作栏（添加按钮 + 配置 APK 密钥按钮）
 * 2. 居中大号全透明/毛玻璃大搜索框
 * 3. 搜索框底部的横向可滑动特大胶囊型型号选择展示区 (XAnimation 弹性动画胶囊)
 */
@Composable
fun CenterHeroSearchSection(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onOpenAdd: () -> Unit,
    onOpenApiKey: () -> Unit,
    searchResults: List<Pair<RingConfig, MatchHighlight>>,
    onSelectModel: (RingConfig) -> Unit,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ==================== 顶部精简操作行（仅保留「+ 添加」与「配置 APK」按键） ====================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：配置 APK 密钥微胶囊按钮
            val keySource = remember { MutableInteractionSource() }
            val isKeyPressed by keySource.collectIsPressedAsState()
            val keyScale by animateFloatAsState(
                targetValue = if (isKeyPressed) 0.93f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "keyScale"
            )

            Box(
                modifier = Modifier
                    .graphicsLayer { scaleX = keyScale; scaleY = keyScale }
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF141720).copy(alpha = 0.75f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                    .clickable(
                        interactionSource = keySource,
                        indication = null,
                        onClick = onOpenApiKey
                    )
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.VpnKey,
                        contentDescription = "配置 APK",
                        tint = GlassTheme.IosTeal,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = "配置 APK",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // 右侧：大添加按钮（高质感晶体渐变）
            val addSource = remember { MutableInteractionSource() }
            val isAddPressed by addSource.collectIsPressedAsState()
            val addScale by animateFloatAsState(
                targetValue = if (isAddPressed) 0.93f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "addScale"
            )

            Box(
                modifier = Modifier
                    .graphicsLayer { scaleX = addScale; scaleY = addScale }
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                GlassTheme.IosBlue.copy(alpha = 0.85f),
                                GlassTheme.IosIndigo.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .border(1.2.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .clickable(
                        interactionSource = addSource,
                        indication = null,
                        onClick = onOpenAdd
                    )
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "添加",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "添加工艺",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.15f))

        // ==================== 屏幕中央大号通透玻璃搜索框 ====================
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            // 大大通透的搜索输入框 (Transparent Glass Search Box)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Color(0xFF161A24).copy(alpha = 0.45f) // 大面积通透玻璃质感
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.28f),
                                GlassTheme.IosBlue.copy(alpha = 0.35f),
                                Color.White.copy(alpha = 0.12f)
                            )
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧大号放大镜图标
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "搜索",
                        tint = if (searchQuery.isNotEmpty()) GlassTheme.IosBlue else GlassTheme.TextMuted,
                        modifier = Modifier.size(26.dp)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    // 大号输入框
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.8.sp
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
                                        color = GlassTheme.TextDim.copy(alpha = 0.85f),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    // 清空按钮
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchChange("") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "清除",
                                tint = GlassTheme.TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 搜索状态微提示
            Text(
                text = if (searchQuery.isEmpty()) {
                    "输入任意型号或实测冷态尺寸 · 库内收录 $totalCount 种标准工序"
                } else {
                    "模糊查询匹配中 · 正在筛选包含「$searchQuery」的工序型号"
                },
                fontSize = 12.sp,
                color = GlassTheme.TextDim,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ==================== 搜索框底部：特大胶囊型型号选择展示区 ====================
        // 模糊搜索输入后，下方展示大胶囊，像滑动胶囊似的供自由横向滑动挑选
        AnimatedVisibility(
            visible = searchQuery.isNotEmpty(),
            enter = fadeIn(tween(250)) + expandVertically(tween(300)),
            exit = fadeOut(tween(200)) + shrinkVertically(tween(250))
        ) {
            if (searchResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "未匹配到相关型号或公差区间",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = GlassTheme.TextMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "可尝试输入数字(如 858)或点击右上角添加新工艺",
                            fontSize = 11.sp,
                            color = GlassTheme.TextDim
                        )
                    }
                }
            } else {
                LargeCapsuleCarousel(
                    items = searchResults,
                    onSelect = onSelectModel,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.85f))
    }
}
