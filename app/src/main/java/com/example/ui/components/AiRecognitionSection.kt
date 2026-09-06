package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.AiEngine
import com.example.data.AiVisionManager
import com.example.data.DeepSeekAiService
import com.example.data.ExtractedProcessInfo
import com.example.data.GeminiAiService
import com.example.model.HangerRecommendation
import com.example.model.HangerRuleEngine
import com.example.model.RingConfig
import com.example.ui.theme.GlassTheme
import kotlinx.coroutines.launch

/**
 * AI 工艺图纸/工序卡识别与数学力学推荐组件
 */
@Composable
fun AiRecognitionSection(
    currentIdNominal: Int?,
    currentWeightStr: String,
    currentHeightNominal: Int?,
    onApplyExtracted: (ExtractedProcessInfo) -> Unit,
    onQuickApplyRecommendation: (HangerRecommendation) -> Unit,
    onBatchImport: ((List<RingConfig>) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isAnalyzing by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var extractedList by remember { mutableStateOf<List<ExtractedProcessInfo>>(emptyList()) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var showCandidateDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var activeEngine by remember { mutableStateOf(AiVisionManager.getActiveEngine(context)) }

    // 实时计算当前手动输入的推荐（与 AI 规律引擎保持双向互通）
    val liveWeightKg = HangerRuleEngine.extractWeightNumber(currentWeightStr)
    val liveRec = remember(currentIdNominal, liveWeightKg, currentHeightNominal) {
        HangerRuleEngine.recommend(currentIdNominal, liveWeightKg, currentHeightNominal)
    }

    // Android 系统 Photo Picker (零权限需求，符合 Play 安全规范)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isAnalyzing = true
                errorMessage = null
                statusText = "正在读取图片并预处理..."
                try {
                    val stream = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(stream)
                    stream?.close()

                    if (bitmap != null) {
                        previewBitmap = bitmap
                        statusText = "${activeEngine.displayName} (${activeEngine.badgeText}) 正在多角度自适应解析图纸/通知单..."
                        val base64 = GeminiAiService.bitmapToBase64(bitmap)
                        val result = AiVisionManager.recognizeProcessImage(context, base64, activeEngine)

                        result.onSuccess { rawList ->
                            val list = rawList.map { item ->
                                val stdModel = AiVisionManager.deriveStandardModelName(item.model, item.fullProductName, item.materialCode)
                                item.copy(model = stdModel)
                            }
                            extractedList = list
                            selectedIndex = 0
                            if (list.size > 1) {
                                showCandidateDialog = true
                                val first = list[0]
                                onApplyExtracted(first)
                                statusText = "识别成功！检测到包含 ${list.size} 款零件（以物料编码后4位命名），已弹开多产品候选列表"
                                Toast.makeText(context, "AI 识别到 ${list.size} 款零件（型号已规范为物料编码后4位）！已打开候选选择与批量入库面板", Toast.LENGTH_SHORT).show()
                            } else if (list.isNotEmpty()) {
                                val first = list[0]
                                onApplyExtracted(first)
                                statusText = "识别成功！共提取到 ${list.size} 款零件，已推导力学吊具与孔位"
                                Toast.makeText(context, "AI 成功识别！已应用型号【${first.model}】", Toast.LENGTH_SHORT).show()
                            }
                        }.onFailure { err ->
                            errorMessage = err.message ?: "识别失败，请检查网络或 API Key"
                            if (err is IllegalStateException) {
                                showApiKeyDialog = true
                            }
                        }
                    } else {
                        errorMessage = "无法解码所选图片"
                    }
                } catch (e: Exception) {
                    errorMessage = "处理图片异常: ${e.message}"
                } finally {
                    isAnalyzing = false
                }
            }
        }
    }

    // 呼吸发光动画
    val infiniteTransition = rememberInfiniteTransition(label = "ai_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        // 主卡片外框
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0F172A).copy(alpha = 0.9f),
                            Color(0xFF1E1B4B).copy(alpha = 0.7f)
                        )
                    )
                )
                .border(
                    1.5.dp,
                    Brush.horizontalGradient(
                        listOf(
                            GlassTheme.CyanGlow.copy(alpha = glowAlpha),
                            Color(0xFF818CF8).copy(alpha = glowAlpha),
                            GlassTheme.AmberGlow.copy(alpha = glowAlpha * 0.7f)
                        )
                    ),
                    RoundedCornerShape(16.dp)
                )
                .padding(14.dp)
        ) {
            Column {
                // 顶部标题栏 + API Key 状态按键
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(GlassTheme.CyanGlow.copy(alpha = 0.2f))
                                .border(1.dp, GlassTheme.CyanGlow, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = GlassTheme.CyanGlow,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "AI 识图匹配吊具与叠放",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = GlassTheme.TextWhite
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (activeEngine == AiEngine.DEEPSEEK) Color(0xFF4338CA).copy(alpha = 0.5f) else Color(0xFF0284C7).copy(alpha = 0.3f))
                                        .border(0.8.dp, if (activeEngine == AiEngine.DEEPSEEK) Color(0xFF818CF8) else GlassTheme.CyanGlow, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = activeEngine.badgeText,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeEngine == AiEngine.DEEPSEEK) Color(0xFFC7D2FE) else GlassTheme.CyanGlow
                                    )
                                }
                            }
                            Text(
                                text = "上传工序卡/图纸，自动解析尺寸并推导吊具、孔位及叠放",
                                fontSize = 10.sp,
                                color = GlassTheme.TextMuted
                            )
                        }
                    }

                    // 设置 API Key 按键
                    IconButton(
                        onClick = { showApiKeyDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        val hasKey = if (activeEngine == AiEngine.DEEPSEEK) {
                            DeepSeekAiService.getApiKey(context).isNotBlank()
                        } else {
                            GeminiAiService.getApiKey(context).isNotBlank()
                        }
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "API设置",
                            tint = if (hasKey) GlassTheme.EmeraldGlow else GlassTheme.AmberGlow,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 视觉大模型双引擎切换开关 (Google Gemini 与 DeepSeek 视觉)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0A0F1D).copy(alpha = 0.7f))
                        .border(1.dp, GlassTheme.CyanGlow.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AiEngine.values().forEach { engine ->
                        val isSelected = activeEngine == engine
                        val bgBrush = if (engine == AiEngine.DEEPSEEK) {
                            Brush.horizontalGradient(listOf(Color(0xFF4F46E5).copy(alpha = 0.45f), Color(0xFF3730A3).copy(alpha = 0.45f)))
                        } else {
                            Brush.horizontalGradient(listOf(GlassTheme.CyanGlow.copy(alpha = 0.35f), Color(0xFF0284C7).copy(alpha = 0.35f)))
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .then(
                                    if (isSelected) Modifier.background(bgBrush)
                                    else Modifier.background(Color.Transparent)
                                )
                                .border(
                                    if (isSelected) 1.dp else 0.dp,
                                    if (isSelected) (if (engine == AiEngine.DEEPSEEK) Color(0xFFA5B4FC) else GlassTheme.CyanGlow) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    activeEngine = engine
                                    AiVisionManager.setActiveEngine(context, engine)
                                    val keyPresent = if (engine == AiEngine.DEEPSEEK) DeepSeekAiService.getApiKey(context).isNotBlank() else GeminiAiService.getApiKey(context).isNotBlank()
                                    if (!keyPresent) {
                                        Toast.makeText(context, "已切换为【${engine.displayName}】，请先点击右上方齿轮配置 Key", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "已切换为【${engine.displayName}】", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (engine == AiEngine.GEMINI) Icons.Default.AutoAwesome else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = if (isSelected) (if (engine == AiEngine.DEEPSEEK) Color(0xFFC7D2FE) else GlassTheme.CyanGlow) else GlassTheme.TextMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = engine.displayName + " (" + engine.badgeText + ")",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) (if (engine == AiEngine.DEEPSEEK) Color(0xFFC7D2FE) else GlassTheme.CyanGlow) else GlassTheme.TextMuted
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 操作按钮组：上传图片 / 测试工序卡示例
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 上传识别主按钮
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isAnalyzing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0284C7)
                        )
                    ) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAnalyzing) "正在识别..." else "选择工序卡/试验通知单截图",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 快捷载入测试行（包含最新 2 线试验通知单的两款零件与经典卡）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 5790 (原1250外，HZZ0091845790J后四位) 快速测试
                    OutlinedButton(
                        onClick = {
                            val info1250Outer = ExtractedProcessInfo(
                                model = "5790",
                                material = "42",
                                odNominal = 1448,
                                odTolerance = 4,
                                idNominal = 1239,
                                idTolerance = 6,
                                heightNominal = 173,
                                heightTolerance = 2,
                                weight = "642 kg",
                                materialCode = "HZZ0091845790J",
                                fullProductName = "外圈锻坯 SSM1250.35AH11-2Z",
                                machinedSize = "Φ1438 × Φ1250 × 73 mm (粗车)",
                                hotSize = "Φ1462±2 × Φ1251(+3/-6) × 176±2 mm",
                                billetInfo = "下料Φ390×684 / 镦粗280 / 垫平260",
                                notes = "辗环2线新增零件试验(合碾2件/初物2-4个/有效至2026.9.14)",
                                recommendation = HangerRuleEngine.recommend(1239, 642, 173)
                            )
                            extractedList = listOf(info1250Outer)
                            selectedIndex = 0
                            onApplyExtracted(info1250Outer)
                            Toast.makeText(context, "已载入试验通知【5790】：小吊具 666孔 / 叠放 4个", Toast.LENGTH_SHORT).show()
                        },
                        enabled = !isAnalyzing,
                        modifier = Modifier.weight(1f).height(36.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassTheme.CyanGlow.copy(alpha = 0.5f))
                    ) {
                        Text("试验: 5790", fontSize = 11.sp, color = GlassTheme.CyanGlow, fontWeight = FontWeight.Bold)
                    }

                    // 5800 (原1250内，HZZ0091845800J后四位) 快速测试
                    OutlinedButton(
                        onClick = {
                            val info1250Inner = ExtractedProcessInfo(
                                model = "5800",
                                material = "42",
                                odNominal = 1260,
                                odTolerance = 4,
                                idNominal = 1099,
                                idTolerance = 6,
                                heightNominal = 173,
                                heightTolerance = 2,
                                weight = "433 kg",
                                materialCode = "HZZ0091845800J",
                                fullProductName = "内圈锻坯 SSM1250.35AH11-1Z",
                                machinedSize = "Φ1250 × Φ1110 × 73 mm (粗车)",
                                hotSize = "Φ1273±2 × Φ1110(+3/-6) × 176±2 mm",
                                billetInfo = "下料Φ390×462 / 镦粗270 / 垫平250",
                                notes = "辗环2线新增零件试验(合碾2件/初物2-4个/有效至2026.9.14)",
                                recommendation = HangerRuleEngine.recommend(1099, 433, 173)
                            )
                            extractedList = listOf(info1250Inner)
                            selectedIndex = 0
                            onApplyExtracted(info1250Inner)
                            Toast.makeText(context, "已载入试验通知【5800】：小吊具 555孔 / 叠放 6个", Toast.LENGTH_SHORT).show()
                        },
                        enabled = !isAnalyzing,
                        modifier = Modifier.weight(1f).height(36.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassTheme.AmberGlow.copy(alpha = 0.5f))
                    ) {
                        Text("试验: 5800", fontSize = 11.sp, color = GlassTheme.AmberGlow, fontWeight = FontWeight.Bold)
                    }

                    // 4156 (原485外，13624156DJ后四位)
                    OutlinedButton(
                        onClick = {
                            val sampleInfo = ExtractedProcessInfo(
                                model = "4156",
                                material = "42",
                                odNominal = 1690,
                                odTolerance = 4,
                                idNominal = 1520,
                                idTolerance = 6,
                                heightNominal = 142,
                                heightTolerance = 2,
                                weight = "942 kg",
                                materialCode = "13624156DJ",
                                fullProductName = "SY485外圈",
                                notes = "模拟示例：超大跨度环件，依力学规律自动选用大吊架 333 孔与 3个 叠放",
                                recommendation = HangerRuleEngine.recommend(1520, 942, 142)
                            )
                            extractedList = listOf(sampleInfo)
                            selectedIndex = 0
                            onApplyExtracted(sampleInfo)
                            Toast.makeText(context, "已载入【4156】标准工艺卡并完成力学推导！", Toast.LENGTH_SHORT).show()
                        },
                        enabled = !isAnalyzing,
                        modifier = Modifier.weight(0.9f).height(36.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassTheme.EmeraldGlow.copy(alpha = 0.4f))
                    ) {
                        Text("示例: 4156", fontSize = 11.sp, color = GlassTheme.EmeraldGlow)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 真实工序汇总表 8 款规格批量识别测试快捷键（产品名称/型号统一为物料编码后4位）
                OutlinedButton(
                    onClick = {
                        val sampleBatch = listOf(
                            ExtractedProcessInfo(
                                model = "6271",
                                material = "42",
                                odNominal = 1560,
                                odTolerance = 4,
                                idNominal = 1394,
                                idTolerance = 6,
                                heightNominal = 245,
                                heightTolerance = 2,
                                weight = "791 kg",
                                materialCode = "HZZ009986271DJ",
                                fullProductName = "外圈锻坯SSF1405-50CWHV-2M",
                                machinedSize = "Φ1550 × Φ1405 × 108.5",
                                hotSize = "Φ1577 × Φ1409 × 248",
                                billetInfo = "料坯Φ390×844 / 制坯外径691",
                                notes = "汇总表序号1 / 合辗2件",
                                recommendation = HangerRuleEngine.recommend(1394, 791, 245)
                            ),
                            ExtractedProcessInfo(
                                model = "1644",
                                material = "50",
                                odNominal = 1413,
                                odTolerance = 4,
                                idNominal = 1228,
                                idTolerance = 6,
                                heightNominal = 309,
                                heightTolerance = 2,
                                weight = "986 kg",
                                materialCode = "HZZ010671644DJ",
                                fullProductName = "内圈锻坯SSF1405.50CWHVI-1M",
                                machinedSize = "Φ1405 × Φ1240 × 124",
                                hotSize = "Φ1429 × Φ1242 × 312",
                                billetInfo = "料坯Φ390×1052 / 制坯外径661",
                                notes = "汇总表序号2 / 合辗2件",
                                recommendation = HangerRuleEngine.recommend(1228, 986, 309)
                            ),
                            ExtractedProcessInfo(
                                model = "4710",
                                material = "42",
                                odNominal = 1688,
                                odTolerance = 4,
                                idNominal = 1488,
                                idTolerance = 6,
                                heightNominal = 189,
                                heightTolerance = 2,
                                weight = "788 kg",
                                materialCode = "HZZ010624710DJ",
                                fullProductName = "外圈(锻坯)SSM1500.40BHHIV-2M",
                                machinedSize = "Φ1678 × Φ1500 × 74",
                                hotSize = "Φ1705 × Φ1502 × 192",
                                billetInfo = "料坯Φ390×840 / 制坯外径689",
                                notes = "汇总表序号3 / 合辗2件",
                                recommendation = HangerRuleEngine.recommend(1488, 788, 189)
                            ),
                            ExtractedProcessInfo(
                                model = "0692",
                                material = "50",
                                odNominal = 1018,
                                odTolerance = 4,
                                idNominal = 891,
                                idTolerance = 6,
                                heightNominal = 253,
                                heightTolerance = 2,
                                weight = "407 kg",
                                materialCode = "11780692DJ",
                                fullProductName = "外圈锻坯SSN0900/32CHW-2(M)",
                                machinedSize = "Φ1008 × Φ900 × 114",
                                hotSize = "Φ1031 × Φ905 × 256",
                                billetInfo = "料坯Φ310×688 / 制坯外径525",
                                notes = "汇总表序号4 / 合辗2件",
                                recommendation = HangerRuleEngine.recommend(891, 407, 253)
                            ),
                            ExtractedProcessInfo(
                                model = "1916",
                                material = "50",
                                odNominal = 1128,
                                odTolerance = 4,
                                idNominal = 979,
                                idTolerance = 6,
                                heightNominal = 170,
                                heightTolerance = 2,
                                weight = "354 kg",
                                materialCode = "HZZ007001916DJ",
                                fullProductName = "内圈锻坯SSM1120.32BHH-1M",
                                machinedSize = "Φ1120 × Φ990 × 72",
                                hotSize = "Φ1141 × Φ991 × 173",
                                billetInfo = "料坯Φ310×598 / 制坯外径489",
                                notes = "汇总表序号5 / 合辗2件",
                                recommendation = HangerRuleEngine.recommend(979, 354, 170)
                            ),
                            ExtractedProcessInfo(
                                model = "1918",
                                material = "50",
                                odNominal = 1308,
                                odTolerance = 4,
                                idNominal = 1111,
                                idTolerance = 6,
                                heightNominal = 170,
                                heightTolerance = 2,
                                weight = "529 kg",
                                materialCode = "HZZ007001918DJ",
                                fullProductName = "外圈锻坯SSM1120.32BHH-2M",
                                machinedSize = "Φ1298 × Φ1120 × 72",
                                hotSize = "Φ1323 × Φ1122 × 173",
                                billetInfo = "料坯Φ350×701 / 制坯外径589",
                                notes = "汇总表序号6 / 合辗2件",
                                recommendation = HangerRuleEngine.recommend(1111, 529, 170)
                            ),
                            ExtractedProcessInfo(
                                model = "5054",
                                material = "42",
                                odNominal = 1125,
                                odTolerance = 4,
                                idNominal = 972,
                                idTolerance = 6,
                                heightNominal = 218,
                                heightTolerance = 2,
                                weight = "460 kg",
                                materialCode = "11465054DJ",
                                fullProductName = "内圈锻坯SSM1116/45CHH-1M",
                                machinedSize = "Φ1116 × Φ985 × 95",
                                hotSize = "Φ1139 × Φ985 × 221",
                                billetInfo = "料坯Φ350×609 / 制坯外径549",
                                notes = "汇总表序号7 / 合辗2件",
                                recommendation = HangerRuleEngine.recommend(972, 460, 218)
                            ),
                            ExtractedProcessInfo(
                                model = "5055",
                                material = "42",
                                odNominal = 1299,
                                odTolerance = 4,
                                idNominal = 1106,
                                idTolerance = 6,
                                heightNominal = 210,
                                heightTolerance = 2,
                                weight = "644 kg",
                                materialCode = "11465055DJ",
                                fullProductName = "外圈锻坯SSM1116/45CHH-2M",
                                machinedSize = "Φ1289 × Φ1116 × 95",
                                hotSize = "Φ1314 × Φ1118 × 213",
                                billetInfo = "料坯Φ390×686 / 制坯外径633",
                                notes = "汇总表序号8 / 合辗2件",
                                recommendation = HangerRuleEngine.recommend(1106, 644, 210)
                            )
                        )
                        extractedList = sampleBatch
                        selectedIndex = 0
                        onApplyExtracted(sampleBatch.first())
                        showCandidateDialog = true
                        Toast.makeText(context, "已载入 8 款规格汇总表！已弹出候选卡片列表供选择或一键批量入库", Toast.LENGTH_SHORT).show()
                    },
                    enabled = !isAnalyzing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassTheme.CyanGlow.copy(alpha = 0.6f))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Layers, contentDescription = null, tint = GlassTheme.CyanGlow, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("快速体验: 模拟识别多产品汇总表 (8款规格 · 弹候选/批量入库)", fontSize = 11.sp, color = GlassTheme.CyanGlow, fontWeight = FontWeight.Bold)
                    }
                }

                // 正在分析中的动态进度条
                AnimatedVisibility(visible = isAnalyzing) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = GlassTheme.CyanGlow,
                            trackColor = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = GlassTheme.CyanGlow
                        )
                    }
                }

                // 错误提示
                if (errorMessage != null && !isAnalyzing) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(GlassTheme.RoseGlow.copy(alpha = 0.15f))
                            .border(1.dp, GlassTheme.RoseGlow.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = GlassTheme.RoseGlow,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    fontSize = 11.sp,
                                    color = GlassTheme.TextWhite,
                                    lineHeight = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (activeEngine == AiEngine.DEEPSEEK) {
                                    Button(
                                        onClick = {
                                            activeEngine = AiEngine.GEMINI
                                            AiVisionManager.setActiveEngine(context, AiEngine.GEMINI)
                                            errorMessage = null
                                            Toast.makeText(context, "已切换为【Google Gemini】视觉引擎，请重新选择图片进行高保真识图！", Toast.LENGTH_LONG).show()
                                        },
                                        modifier = Modifier.weight(1.2f).height(34.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = GlassTheme.CyanGlow.copy(alpha = 0.4f))
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GlassTheme.CyanGlow, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("切至 Google 视觉识图", fontSize = 11.sp, color = GlassTheme.CyanGlow, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Button(
                                    onClick = { showApiKeyDialog = true },
                                    modifier = Modifier.weight(1f).height(34.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                                ) {
                                    Icon(Icons.Default.Key, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("配置/查看 API Key", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                // 识别成果与力学推导结果展示卡
                if (extractedList.isNotEmpty() && !isAnalyzing) {
                    Spacer(modifier = Modifier.height(10.dp))

                    // 多零件候选卡片托盘展开按钮（当识别到表格包含多个零件时）
                    if (extractedList.size > 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF065F46).copy(alpha = 0.5f), Color(0xFF047857).copy(alpha = 0.35f))
                                    )
                                )
                                .border(1.dp, GlassTheme.EmeraldGlow.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                                .clickable { showCandidateDialog = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Layers, contentDescription = null, tint = GlassTheme.EmeraldGlow, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "已识别 ${extractedList.size} 款产品规格",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GlassTheme.TextWhite
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(点击展开候选列表/批量入库)",
                                        fontSize = 10.sp,
                                        color = GlassTheme.EmeraldGlow
                                    )
                                }
                                Icon(Icons.Default.OpenInNew, contentDescription = null, tint = GlassTheme.EmeraldGlow, modifier = Modifier.size(14.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // 多零件横向滑动快捷切换标签
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "快速切换:",
                                fontSize = 11.sp,
                                color = GlassTheme.TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                            extractedList.forEachIndexed { index, item ->
                                val isSelected = (index == selectedIndex)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) GlassTheme.CyanGlow else Color(0xFF1E293B))
                                        .border(
                                            1.dp,
                                            if (isSelected) GlassTheme.CyanGlow else Color.Gray.copy(alpha = 0.4f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable {
                                            selectedIndex = index
                                            onApplyExtracted(item)
                                            Toast.makeText(context, "已载入【${item.model}】！", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${index + 1}.${item.model}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else GlassTheme.TextWhite
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    val info = extractedList.getOrNull(selectedIndex) ?: extractedList.first()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F2338))
                            .border(1.dp, GlassTheme.EmeraldGlow.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
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
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = GlassTheme.EmeraldGlow,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "已提取: ${info.model} (${info.fullProductName.ifBlank { "标准件" }})",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GlassTheme.TextWhite
                                    )
                                }
                                Text(
                                    text = "已自动同步至下方表单",
                                    fontSize = 10.sp,
                                    color = GlassTheme.EmeraldGlow
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // 关键推荐指示器（三联卡：吊具 / 孔位 / 叠放）
                            val rec = info.recommendation ?: HangerRuleEngine.recommend(
                                info.idNominal,
                                HangerRuleEngine.extractWeightNumber(info.weight),
                                info.heightNominal
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                RecBadgeItem(
                                    modifier = Modifier.weight(1f),
                                    title = "选用吊具",
                                    value = "${rec.hanger}吊具",
                                    color = GlassTheme.CyanGlow
                                )
                                RecBadgeItem(
                                    modifier = Modifier.weight(1f),
                                    title = "推荐孔位",
                                    value = "${rec.holePosition}孔",
                                    color = GlassTheme.AmberGlow
                                )
                                RecBadgeItem(
                                    modifier = Modifier.weight(1f),
                                    title = "建议叠放",
                                    value = rec.quantity,
                                    color = GlassTheme.EmeraldGlow
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // 详细力学推导依据文本
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E293B).copy(alpha = 0.6f))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "📐 吊具孔位依据: ${rec.hangerReason}",
                                    fontSize = 10.sp,
                                    color = GlassTheme.TextWhite.copy(alpha = 0.9f)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "⚖️ 叠放承重依据: ${rec.quantityReason}",
                                    fontSize = 10.sp,
                                    color = GlassTheme.TextWhite.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }

                // 即使没有上传图片，当用户手动修改内径或重量时，也提供即时物理规律推导横幅
                if (extractedList.isEmpty() && (currentIdNominal != null && currentIdNominal > 0)) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E293B).copy(alpha = 0.7f))
                            .border(1.dp, GlassTheme.CyanGlow.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "💡 数学力学推荐: ${liveRec.hanger}吊具 · ${liveRec.holePosition}孔 · 叠放 ${liveRec.quantity}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GlassTheme.CyanGlow
                                )
                                Text(
                                    text = "基于冷态内径 Φ${currentIdNominal}mm 及单重推算",
                                    fontSize = 9.sp,
                                    color = GlassTheme.TextMuted
                                )
                            }
                            Button(
                                onClick = { onQuickApplyRecommendation(liveRec) },
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GlassTheme.CyanGlow.copy(alpha = 0.3f))
                            ) {
                                Text("一键填入", fontSize = 10.sp, color = GlassTheme.CyanGlow, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // API Key 配置对话框
    if (showApiKeyDialog) {
        ApiKeyConfigDialog(
            onDismiss = { showApiKeyDialog = false },
            onKeySaved = {
                showApiKeyDialog = false
                activeEngine = AiVisionManager.getActiveEngine(context)
                Toast.makeText(context, "API 配置已保存生效！当前主力引擎: ${activeEngine.displayName}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 多产品识别候选卡片列表弹窗（支持单项点击载入、一键全部批量入库）
    if (showCandidateDialog && extractedList.size > 1) {
        MultiProductCandidateDialog(
            candidates = extractedList,
            onDismiss = { showCandidateDialog = false },
            onSelectOne = { selectedItem ->
                val idx = extractedList.indexOf(selectedItem).coerceAtLeast(0)
                selectedIndex = idx
                onApplyExtracted(selectedItem)
                showCandidateDialog = false
                Toast.makeText(context, "已载入【${selectedItem.model}】到当前编辑表单！", Toast.LENGTH_SHORT).show()
            },
            onBatchImport = { configs ->
                onBatchImport?.invoke(configs)
                showCandidateDialog = false
            }
        )
    }
}

@Composable
private fun RecBadgeItem(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    color: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E293B).copy(alpha = 0.8f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontSize = 9.sp, color = GlassTheme.TextMuted)
            Spacer(modifier = Modifier.height(1.dp))
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

/**
 * 视觉大模型 API Key 用户设置弹窗
 * 支持 Google Gemini 与 DeepSeek v4-Flash 视觉双引擎配置、一键剪贴板粘贴与在线连通性测试
 */
@Composable
fun ApiKeyConfigDialog(
    onDismiss: () -> Unit,
    onKeySaved: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { 
        mutableStateOf(if (AiVisionManager.getActiveEngine(context) == AiEngine.DEEPSEEK) 1 else 0) 
    }

    // Gemini 状态
    var geminiKeyInput by remember { mutableStateOf(GeminiAiService.getApiKey(context)) }
    var geminiTestStatus by remember { mutableStateOf<String?>(null) }
    var isGeminiTesting by remember { mutableStateOf(false) }
    var geminiTestSuccess by remember { mutableStateOf(false) }

    // DeepSeek 状态
    var deepseekKeyInput by remember { mutableStateOf(DeepSeekAiService.getApiKey(context)) }
    var deepseekEndpointInput by remember { mutableStateOf(DeepSeekAiService.getEndpoint(context)) }
    var deepseekModelInput by remember { mutableStateOf(DeepSeekAiService.getModelName(context)) }
    var deepseekTestStatus by remember { mutableStateOf<String?>(null) }
    var isDeepseekTesting by remember { mutableStateOf(false) }
    var deepseekTestSuccess by remember { mutableStateOf(false) }

    // 是否同时将当前选中的 Tab 设置为活跃引擎
    var setAsActiveEngine by remember { mutableStateOf(true) }

    // 智能粘贴辅助函数
    fun performPasteTo(target: (String) -> Unit) {
        val composeClip = clipboardManager.getText()?.text
        if (!composeClip.isNullOrBlank()) {
            target(composeClip.trim())
            Toast.makeText(context, "已成功从剪贴板粘贴 API Key！", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val sysClipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            val item = sysClipboard?.primaryClip?.getItemAt(0)?.text?.toString()
            if (!item.isNullOrBlank()) {
                target(item.trim())
                Toast.makeText(context, "已成功从系统剪贴板粘贴！", Toast.LENGTH_SHORT).show()
                return
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Toast.makeText(context, "剪贴板中未找到文本，请先复制 API Key (Ctrl+C)", Toast.LENGTH_LONG).show()
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F172A))
                .border(1.5.dp, GlassTheme.CyanGlow.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Column {
                // 弹窗顶部栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = GlassTheme.CyanGlow, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI 视觉模型设置与粘贴",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlassTheme.TextWhite
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", tint = GlassTheme.TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 双引擎 Tab 切换栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E293B).copy(alpha = 0.9f))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 0) Color(0xFF0284C7).copy(alpha = 0.4f) else Color.Transparent)
                            .border(if (selectedTab == 0) 1.dp else 0.dp, if (selectedTab == 0) GlassTheme.CyanGlow else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = if (selectedTab == 0) GlassTheme.CyanGlow else GlassTheme.TextMuted, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Google Gemini", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selectedTab == 0) GlassTheme.CyanGlow else GlassTheme.TextMuted)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 1) Color(0xFF4F46E5).copy(alpha = 0.4f) else Color.Transparent)
                            .border(if (selectedTab == 1) 1.dp else 0.dp, if (selectedTab == 1) Color(0xFFA5B4FC) else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = if (selectedTab == 1) Color(0xFFC7D2FE) else GlassTheme.TextMuted, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("DeepSeek 视觉", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selectedTab == 1) Color(0xFFC7D2FE) else GlassTheme.TextMuted)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (selectedTab == 0) {
                    // Gemini 配置卡片
                    Text(
                        text = "模型：Gemini 3.5 Flash（具备高峰多模型自动故障转移）",
                        fontSize = 11.sp,
                        color = GlassTheme.CyanGlow,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = geminiKeyInput,
                        onValueChange = { 
                            geminiKeyInput = it
                            geminiTestStatus = null
                        },
                        label = { Text("Gemini API Key") },
                        placeholder = { Text("AIzaSy...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (geminiKeyInput.isNotBlank()) {
                                    IconButton(onClick = { geminiKeyInput = ""; geminiTestStatus = null }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Clear, contentDescription = "清空", tint = GlassTheme.TextMuted, modifier = Modifier.size(16.dp))
                                    }
                                }
                                IconButton(onClick = { performPasteTo { geminiKeyInput = it; geminiTestStatus = null } }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "粘贴", tint = GlassTheme.CyanGlow, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        colors = editFieldColors()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { performPasteTo { geminiKeyInput = it; geminiTestStatus = null } },
                            modifier = Modifier.weight(1.3f).height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GlassTheme.CyanGlow.copy(alpha = 0.25f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassTheme.CyanGlow.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, tint = GlassTheme.CyanGlow, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("一键从剪贴板粘贴", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GlassTheme.CyanGlow)
                        }

                        OutlinedButton(
                            onClick = {
                                if (geminiKeyInput.isBlank()) {
                                    geminiTestStatus = "请先输入或粘贴 Gemini Key"
                                    geminiTestSuccess = false
                                    return@OutlinedButton
                                }
                                coroutineScope.launch {
                                    isGeminiTesting = true
                                    geminiTestStatus = "正在测试 Google Gemini 接口握手..."
                                    val res = GeminiAiService.testApiKey(context, geminiKeyInput)
                                    res.onSuccess { msg ->
                                        geminiTestStatus = msg
                                        geminiTestSuccess = true
                                    }
                                    res.onFailure { err ->
                                        geminiTestStatus = err.message ?: "连接失败"
                                        geminiTestSuccess = false
                                    }
                                    isGeminiTesting = false
                                }
                            },
                            enabled = !isGeminiTesting,
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassTheme.AmberGlow.copy(alpha = 0.5f))
                        ) {
                            if (isGeminiTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = GlassTheme.AmberGlow)
                            } else {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = GlassTheme.AmberGlow, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("测试连通性", fontSize = 11.sp, color = GlassTheme.AmberGlow, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (geminiTestStatus != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (geminiTestSuccess) GlassTheme.EmeraldGlow.copy(alpha = 0.15f) else GlassTheme.RoseGlow.copy(alpha = 0.15f))
                                .border(1.dp, if (geminiTestSuccess) GlassTheme.EmeraldGlow.copy(alpha = 0.4f) else GlassTheme.RoseGlow.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (geminiTestSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (geminiTestSuccess) GlassTheme.EmeraldGlow else GlassTheme.RoseGlow,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = geminiTestStatus ?: "",
                                    fontSize = 10.sp,
                                    color = GlassTheme.TextWhite,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                } else {
                    // DeepSeek 视觉模型配置卡片
                    Column {
                        // 预设模型快速选择与说明
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF312E81).copy(alpha = 0.35f))
                                .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "💡 提示：官方 DeepSeek (api.deepseek.com) 仅支持纯文本，测试 Key 会显示正常；若使用图片识图，系统会自动调用 Google Gemini 视觉引擎完成解析。",
                                fontSize = 10.sp,
                                color = Color(0xFFC7D2FE),
                                lineHeight = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "deepseek-v4-flash-vision-exp" to "视觉专版(推荐)",
                                "deepseek-v4-flash" to "Flash",
                                "deepseek-v4-pro" to "Pro"
                            ).forEach { (mod, label) ->
                                val isCur = deepseekModelInput == mod
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isCur) Color(0xFF4F46E5).copy(alpha = 0.6f) else Color(0xFF1E293B))
                                        .border(1.dp, if (isCur) Color(0xFFA5B4FC) else Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                        .clickable { deepseekModelInput = mod }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isCur) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCur) Color.White else GlassTheme.TextMuted
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // DeepSeek Key 输入框
                        OutlinedTextField(
                            value = deepseekKeyInput,
                            onValueChange = { 
                                deepseekKeyInput = it
                                deepseekTestStatus = null
                            },
                            label = { Text("DeepSeek API Key (sk-...)") },
                            placeholder = { Text("sk-...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (deepseekKeyInput.isNotBlank()) {
                                        IconButton(onClick = { deepseekKeyInput = ""; deepseekTestStatus = null }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Clear, contentDescription = "清空", tint = GlassTheme.TextMuted, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    IconButton(onClick = { performPasteTo { deepseekKeyInput = it; deepseekTestStatus = null } }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = "粘贴", tint = Color(0xFFA5B4FC), modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            colors = editFieldColors()
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // DeepSeek 接口地址 (Base URL / Endpoint)
                        OutlinedTextField(
                            value = deepseekEndpointInput,
                            onValueChange = { 
                                deepseekEndpointInput = it
                                deepseekTestStatus = null
                            },
                            label = { Text("API 接口地址 (Endpoint)") },
                            placeholder = { Text(DeepSeekAiService.DEFAULT_ENDPOINT) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = editFieldColors()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { performPasteTo { deepseekKeyInput = it; deepseekTestStatus = null } },
                                modifier = Modifier.weight(1.3f).height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5).copy(alpha = 0.35f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA5B4FC).copy(alpha = 0.6f)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null, tint = Color(0xFFC7D2FE), modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("一键从剪贴板粘贴", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC7D2FE))
                            }

                            OutlinedButton(
                                onClick = {
                                    if (deepseekKeyInput.isBlank()) {
                                        deepseekTestStatus = "请先输入或粘贴 DeepSeek Key"
                                        deepseekTestSuccess = false
                                        return@OutlinedButton
                                    }
                                    coroutineScope.launch {
                                        isDeepseekTesting = true
                                        deepseekTestStatus = "正在向 DeepSeek 视觉接口发送测试握手..."
                                        val res = DeepSeekAiService.testApiKey(
                                            context,
                                            deepseekKeyInput,
                                            deepseekEndpointInput,
                                            deepseekModelInput
                                        )
                                        res.onSuccess { msg ->
                                            deepseekTestStatus = msg
                                            deepseekTestSuccess = true
                                        }
                                        res.onFailure { err ->
                                            deepseekTestStatus = err.message ?: "连接失败"
                                            deepseekTestSuccess = false
                                        }
                                        isDeepseekTesting = false
                                    }
                                },
                                enabled = !isDeepseekTesting,
                                modifier = Modifier.weight(1f).height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GlassTheme.AmberGlow.copy(alpha = 0.5f))
                            ) {
                                if (isDeepseekTesting) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = GlassTheme.AmberGlow)
                                } else {
                                    Icon(Icons.Default.Speed, contentDescription = null, tint = GlassTheme.AmberGlow, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("测试连通性", fontSize = 11.sp, color = GlassTheme.AmberGlow, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (deepseekTestStatus != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (deepseekTestSuccess) GlassTheme.EmeraldGlow.copy(alpha = 0.15f) else GlassTheme.RoseGlow.copy(alpha = 0.15f))
                                    .border(1.dp, if (deepseekTestSuccess) GlassTheme.EmeraldGlow.copy(alpha = 0.4f) else GlassTheme.RoseGlow.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (deepseekTestSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (deepseekTestSuccess) GlassTheme.EmeraldGlow else GlassTheme.RoseGlow,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = deepseekTestStatus ?: "",
                                        fontSize = 10.sp,
                                        color = GlassTheme.TextWhite,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 设为当前默认主力引擎勾选项
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { setAsActiveEngine = !setAsActiveEngine }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = setAsActiveEngine,
                        onCheckedChange = { setAsActiveEngine = it },
                        colors = CheckboxDefaults.colors(checkedColor = GlassTheme.CyanGlow)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "保存并设【${if (selectedTab == 0) "Google Gemini" else "DeepSeek 视觉"}】为当前识图主力引擎",
                        fontSize = 11.sp,
                        color = GlassTheme.TextWhite
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 底部保存与取消按键
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("取消", color = GlassTheme.TextWhite)
                    }
                    Button(
                        onClick = {
                            // 保存 Gemini
                            GeminiAiService.saveCustomApiKey(context, geminiKeyInput)

                            // 保存 DeepSeek
                            DeepSeekAiService.saveApiKey(context, deepseekKeyInput)
                            DeepSeekAiService.saveEndpoint(context, deepseekEndpointInput)
                            DeepSeekAiService.saveModelName(context, deepseekModelInput)

                            // 切换主力引擎
                            if (setAsActiveEngine) {
                                val targetEngine = if (selectedTab == 0) AiEngine.GEMINI else AiEngine.DEEPSEEK
                                AiVisionManager.setActiveEngine(context, targetEngine)
                            }

                            onKeySaved()
                        },
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 0) Color(0xFF0284C7) else Color(0xFF4F46E5)
                        )
                    ) {
                        Text("保存配置", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
