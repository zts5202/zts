package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ConfigStorageManager
import com.example.model.RingConfig
import com.example.ui.components.ApiKeyConfigDialog
import com.example.ui.components.EditConfigDialog
import com.example.ui.components.FlipGlassDetailDialog
import com.example.ui.components.GlassModelCard
import com.example.ui.components.ResetConfirmDialog
import com.example.ui.theme.GlassTheme
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                HangerApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HangerApp() {
    val context = LocalContext.current
    var configList by remember { mutableStateOf(ConfigStorageManager.loadAllConfigs(context)) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("全部") } // 全部, 大吊具, 小吊具, 42钢, 50钢
    var selectedItem by remember { mutableStateOf<RingConfig?>(null) }
    var editingConfig by remember { mutableStateOf<RingConfig?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    // 多重智能检索过滤 (支持型号名/物料编码模糊匹配 + 实测冷态外径/内径/高度公差区间智能反查)
    val searchResults = remember(configList, searchQuery, selectedCategory) {
        configList.mapNotNull { config ->
            val matchResult = config.evaluateSearch(searchQuery)
            val matchesCategory = when (selectedCategory) {
                "大吊具" -> config.hanger == "大"
                "小吊具" -> config.hanger == "小"
                "42钢" -> config.material == "42"
                "50钢" -> config.material == "50"
                else -> true
            }
            if (matchResult.matched && matchesCategory) {
                Pair(config, matchResult)
            } else {
                null
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(GlassTheme.BackgroundTop, GlassTheme.BackgroundBottom)
                )
            )
            .drawBehind {
                // 毛玻璃背后的梦幻光晕球 (Atmospheric Glowing Mesh Orbs)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(GlassTheme.OrbBlue, Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.12f),
                        radius = size.width * 0.7f
                    ),
                    center = Offset(size.width * 0.85f, size.height * 0.12f),
                    radius = size.width * 0.7f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(GlassTheme.OrbPurple, Color.Transparent),
                        center = Offset(size.width * 0.1f, size.height * 0.45f),
                        radius = size.width * 0.65f
                    ),
                    center = Offset(size.width * 0.1f, size.height * 0.45f),
                    radius = size.width * 0.65f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(GlassTheme.OrbCyan, Color.Transparent),
                        center = Offset(size.width * 0.6f, size.height * 0.85f),
                        radius = size.width * 0.6f
                    ),
                    center = Offset(size.width * 0.6f, size.height * 0.85f),
                    radius = size.width * 0.6f
                )
            }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 毛玻璃风格顶部栏
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1E293B).copy(alpha = 0.55f))
                            .border(
                                1.dp,
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.25f),
                                        Color.White.copy(alpha = 0.05f),
                                        GlassTheme.CyanGlow.copy(alpha = 0.35f)
                                    )
                                ),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(vertical = 10.dp, horizontal = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "调质炉查询专用",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp,
                                    color = GlassTheme.TextWhite
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = "HEAT TREATMENT & SOP FORGING MATRIX",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.2.sp,
                                    color = GlassTheme.CyanGlow.copy(alpha = 0.9f)
                                )
                            }

                            // 顶部快捷功能操作组 (新增型号 / 恢复出厂)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 新增规格与AI识图按键
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    GlassTheme.CyanGlow.copy(alpha = 0.35f),
                                                    Color(0xFF6366F1).copy(alpha = 0.35f)
                                                )
                                            )
                                        )
                                        .border(1.dp, GlassTheme.CyanGlow.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                                        .clickable {
                                            isCreatingNew = true
                                            editingConfig = null
                                        }
                                        .padding(horizontal = 9.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = "新增规格/AI识图",
                                            tint = GlassTheme.CyanGlow,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "AI识图/新增",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = GlassTheme.TextWhite
                                        )
                                    }
                                }

                                // Gemini Key 设置按键
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF0F172A).copy(alpha = 0.7f))
                                        .border(1.dp, GlassTheme.CyanGlow.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                        .clickable { showApiKeyDialog = true }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.VpnKey,
                                            contentDescription = "API设置",
                                            tint = GlassTheme.CyanGlow,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "AI模型/Key",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = GlassTheme.CyanGlow
                                        )
                                    }
                                }

                                // 恢复默认按键
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF334155).copy(alpha = 0.5f))
                                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                        .clickable { showResetConfirmDialog = true }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = "重置数据",
                                            tint = GlassTheme.TextMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "出厂重置",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = GlassTheme.TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // 毛玻璃超宽智能搜索栏
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.65f))
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.25f),
                                    GlassTheme.CyanGlow.copy(alpha = 0.4f)
                                )
                            ),
                            RoundedCornerShape(18.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 2.dp)
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { newValue ->
                            searchQuery = newValue
                            val query = newValue.trim()
                            if (query.isNotEmpty()) {
                                val currentMatched = configList.filter { it.evaluateSearch(query).matched }
                                if (currentMatched.size == 1) {
                                    selectedItem = currentMatched.first()
                                    keyboardController?.hide()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "输入型号(485)或量得尺寸(如858, 755, 142)...",
                                fontSize = 14.sp,
                                color = GlassTheme.TextDim
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "搜索",
                                modifier = Modifier.size(24.dp),
                                tint = GlassTheme.CyanGlow
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    selectedItem = null
                                }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "清除",
                                        modifier = Modifier.size(22.dp),
                                        tint = GlassTheme.TextMuted
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlassTheme.TextWhite
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { keyboardController?.hide() }
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = GlassTheme.CyanGlow
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 尺寸反查辅助快捷按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "实测反查:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTheme.CyanGlow
                    )
                    listOf("858", "755", "142", "1690", "1520").forEach { sampleVal ->
                        val isCurrent = searchQuery == sampleVal
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isCurrent) GlassTheme.CyanGlow.copy(alpha = 0.35f)
                                    else Color(0xFF1E293B).copy(alpha = 0.5f)
                                )
                                .border(
                                    1.dp,
                                    if (isCurrent) GlassTheme.CyanGlow else Color.White.copy(alpha = 0.1f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    searchQuery = if (isCurrent) "" else sampleVal
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${sampleVal}mm",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isCurrent) GlassTheme.CyanGlow else GlassTheme.TextWhite.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 分类筛选胶囊按钮 (Capsule Filter Row)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val categories = listOf("全部", "大吊具", "小吊具", "42钢", "50钢")
                    categories.forEach { category ->
                        val isSelected = selectedCategory == category
                        val catColor = when (category) {
                            "大吊具" -> GlassTheme.CyanGlow
                            "小吊具" -> GlassTheme.EmeraldGlow
                            "42钢" -> GlassTheme.Mat42Text
                            "50钢" -> GlassTheme.Mat50Text
                            else -> GlassTheme.CyanGlow
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) catColor.copy(alpha = 0.25f)
                                    else Color(0xFF1E293B).copy(alpha = 0.45f)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) catColor.copy(alpha = 0.8f)
                                    else Color.White.copy(alpha = 0.08f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedCategory = category }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = category,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) Color.White else GlassTheme.TextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 计数与提示
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank() && searchResults.any { it.second.isDimensionMatch })
                            "🎯 尺寸公差命中 ${searchResults.size} 个型号"
                        else
                            "共找到 ${searchResults.size} 种规格",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (searchResults.any { it.second.isDimensionMatch }) GlassTheme.EmeraldGlow else GlassTheme.TextMuted
                    )
                    Text(
                        text = "点击卡片查看/编辑参数",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlassTheme.CyanGlow.copy(alpha = 0.85f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 2列网格卡片
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(searchResults, key = { it.first.model }) { (config, highlight) ->
                        GlassModelCard(
                            config = config,
                            highlight = highlight,
                            onClick = {
                                selectedItem = config
                            },
                            onEdit = {
                                editingConfig = config
                                isCreatingNew = false
                            }
                        )
                    }
                }
            }
        }

        // 3D 翻转弹出的工艺参数卡片
        selectedItem?.let { config ->
            val highlight = searchResults.find { it.first.model == config.model }?.second
            FlipGlassDetailDialog(
                config = config,
                highlight = highlight,
                onDismiss = { selectedItem = null },
                onEdit = {
                    editingConfig = config
                    isCreatingNew = false
                },
                onRestoreDefault = if (config.isCustomized) {
                    {
                        ConfigStorageManager.restoreSingleDefault(context, config.model)
                        configList = ConfigStorageManager.loadAllConfigs(context)
                        selectedItem = configList.find { it.model == config.model }
                    }
                } else null
            )
        }

        // 编辑/新增参数弹窗
        if (isCreatingNew || editingConfig != null) {
            EditConfigDialog(
                initialConfig = editingConfig,
                isNew = isCreatingNew,
                onDismiss = {
                    isCreatingNew = false
                    editingConfig = null
                },
                onSave = { updated ->
                    ConfigStorageManager.saveConfig(context, updated)
                    configList = ConfigStorageManager.loadAllConfigs(context)
                    if (selectedItem?.model == updated.model) {
                        selectedItem = updated
                    }
                    isCreatingNew = false
                    editingConfig = null
                },
                onBatchSave = { configs ->
                    ConfigStorageManager.saveBatchConfigs(context, configs)
                    configList = ConfigStorageManager.loadAllConfigs(context)
                    if (configs.isNotEmpty()) {
                        selectedItem = configList.find { it.model == configs.first().model } ?: configs.first()
                    }
                    isCreatingNew = false
                    editingConfig = null
                    Toast.makeText(context, "已成功批量入库 ${configs.size} 款产品规格！", Toast.LENGTH_SHORT).show()
                },
                onRestoreDefault = if (editingConfig?.isCustomized == true) {
                    {
                        editingConfig?.model?.let { m ->
                            ConfigStorageManager.restoreSingleDefault(context, m)
                            configList = ConfigStorageManager.loadAllConfigs(context)
                            if (selectedItem?.model == m) {
                                selectedItem = configList.find { it.model == m }
                            }
                        }
                        isCreatingNew = false
                        editingConfig = null
                    }
                } else null
            )
        }

        // 重置所有确认弹窗
        if (showResetConfirmDialog) {
            ResetConfirmDialog(
                onDismiss = { showResetConfirmDialog = false },
                onConfirm = {
                    ConfigStorageManager.resetAllToFactory(context)
                    configList = ConfigStorageManager.loadAllConfigs(context)
                    if (selectedItem != null) {
                        selectedItem = configList.find { it.model == selectedItem?.model }
                    }
                    showResetConfirmDialog = false
                    Toast.makeText(context, "已恢复全部 43 种出厂 OP40 辗环工艺卡标准参数！", Toast.LENGTH_LONG).show()
                }
            )
        }

        // API Key 配置弹窗
        if (showApiKeyDialog) {
            ApiKeyConfigDialog(
                onDismiss = { showApiKeyDialog = false },
                onKeySaved = {
                    showApiKeyDialog = false
                    Toast.makeText(context, "API Key 已成功保存并立即生效！", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}
