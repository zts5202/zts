package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
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
import com.example.ui.components.AddChoiceBottomSheet
import com.example.ui.components.ApiKeyConfigDialog
import com.example.ui.components.BottomReelSearchLayout
import com.example.ui.components.EditConfigDialog
import com.example.ui.components.FlipGlassDetailDialog
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
    var selectedItem by remember { mutableStateOf<RingConfig?>(null) }
    var editingConfig by remember { mutableStateOf<RingConfig?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var initialShowAiVision by remember { mutableStateOf(false) }
    var showAddChoice by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    // 背景呼吸微动效 (Ambient Breathing Animation)
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_glow")
    val ambientAlpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambientAlpha"
    )

    // 多重智能检索过滤 (支持型号名/物料编码模糊匹配 + 实测冷态外径/内径/高度公差区间智能反查)
    val searchResults = remember(configList, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) {
            emptyList()
        } else {
            configList.mapNotNull { config ->
                val matchResult = config.evaluateSearch(q)
                if (matchResult.matched) {
                    Pair(config, matchResult)
                } else {
                    null
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassTheme.BackgroundTop)
            .drawBehind {
                // 柔和灵动的环境呼吸光晕
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(GlassTheme.IosBlue.copy(alpha = ambientAlpha), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.35f),
                        radius = size.width * 0.85f
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.35f),
                    radius = size.width * 0.85f
                )
            }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // 界面顶部操作栏（配置APK与添加） + 中间留白 + 底部透明大搜索框 + 紧贴上方的 Ramotion 3D 卷轴搜索
                BottomReelSearchLayout(
                    searchQuery = searchQuery,
                    onSearchChange = { newValue ->
                        searchQuery = newValue
                    },
                    onSearchSubmit = {
                        val q = searchQuery.trim()
                        if (q.isNotEmpty()) {
                            val matched = configList.filter { it.evaluateSearch(q).matched }
                            if (matched.size == 1) {
                                selectedItem = matched.first()
                            }
                        }
                    },
                    onOpenAdd = {
                        showAddChoice = true
                    },
                    onOpenApiKey = {
                        showApiKeyDialog = true
                    },
                    searchResults = searchResults,
                    onSelectModel = { config ->
                        selectedItem = config
                    },
                    totalCount = configList.size,
                    modifier = Modifier.fillMaxSize()
                )
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

        // 添加方式选择弹窗（实图添加 vs 手动添加）
        if (showAddChoice) {
            AddChoiceBottomSheet(
                onDismiss = { showAddChoice = false },
                onVisualAdd = {
                    showAddChoice = false
                    editingConfig = null
                    isCreatingNew = true
                    initialShowAiVision = true
                },
                onManualAdd = {
                    showAddChoice = false
                    editingConfig = null
                    isCreatingNew = true
                    initialShowAiVision = false
                }
            )
        }

        // 编辑/新增参数弹窗
        if (isCreatingNew || editingConfig != null) {
            EditConfigDialog(
                initialConfig = editingConfig,
                isNew = isCreatingNew,
                initialShowAiVision = initialShowAiVision,
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
