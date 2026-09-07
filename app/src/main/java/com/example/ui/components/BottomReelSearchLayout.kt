package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
 * 严格按照用户截图要求修正的真实底部布局：
 *
 * 1. 顶部操作栏：「配置 APK」和「+ 添加工艺」固定在状态栏下方，永不位移。
 * 2. 纵向 3D 滚轴列表在屏幕下方展开，紧靠搜索框正上方，预留足够的显示空间（高度自适应，绝不会顶破屏幕到状态栏）。
 * 3. 搜索框始终固定在最底部（或软键盘正上方），绝不上窜到顶部！
 */
@Composable
fun BottomReelSearchLayout(
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
    var selectedReelIndex by remember { mutableStateOf(0) }

    LaunchedEffect(searchResults) {
        selectedReelIndex = 0
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding() // 紧跟输入法，保持在键盘正上方
    ) {
        // ==================== 1. 顶部固定精简操作行（配置 APK + 添加工艺） ====================
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：配置 APK
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
                    .background(Color(0xFF141720).copy(alpha = 0.85f))
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

            // 右侧：添加工艺
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

        // ==================== 2. 中部核心空白区：炫酷调质工艺曲线 SVG/Canvas 动效面板 ====================
        // 当未搜索或仅空闲展示时，完美利用屏幕中央宽敞的空白位置，展示工业级动态调质曲线与实时遥测
        AnimatedVisibility(
            visible = searchQuery.isEmpty(),
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(150)),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .offset(y = (-28).dp) // 略微往上偏一点，与底部搜索框保持舒适负空间
        ) {
            QuenchingTemperingCurveCard(
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ==================== 3. 底部控制区（搜索框固定在底，卷轴稳居其上方） ====================
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 当搜索框有输入且有结果时，在搜索框上方展示紧凑的 Ramotion 3D 卷轴选择器
            AnimatedVisibility(
                visible = searchQuery.isNotEmpty(),
                enter = fadeIn(tween(200)) + expandVertically(tween(250)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    if (searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF10131A).copy(alpha = 0.7f))
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "未匹配到相关工艺型号或公差区间",
                                fontSize = 13.sp,
                                color = GlassTheme.TextMuted
                            )
                        }
                    } else {
                        // 提示小标题
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "上下滑动卷轴定位型号 (${searchResults.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GlassTheme.IosBlue
                            )
                            Text(
                                text = "点击条目直接查看",
                                fontSize = 11.sp,
                                color = GlassTheme.TextDim
                            )
                        }

                        // Ramotion 3D 垂直滚轴选择器（高度严格限制在 190dp，防止挤占空间冲上状态栏）
                        val candidateConfigs = searchResults.map { it.first }
                        RamotionVerticalReelPicker(
                            candidates = candidateConfigs,
                            selectedIndex = selectedReelIndex,
                            onSelectIndex = { index ->
                                selectedReelIndex = index
                            },
                            onConfirmSelect = { config ->
                                onSelectModel(config)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                        )
                    }
                }
            }

            // ==================== 底部固定大搜索框 ====================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        Color(0xFF161A24).copy(alpha = 0.65f)
                    )
                    .border(
                        width = 1.4.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.35f),
                                GlassTheme.IosBlue.copy(alpha = 0.5f),
                                Color.White.copy(alpha = 0.15f)
                            )
                        ),
                        shape = RoundedCornerShape(30.dp)
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
}
