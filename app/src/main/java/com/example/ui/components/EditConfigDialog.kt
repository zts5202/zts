package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AiVisionManager
import com.example.data.ConfigStorageManager
import com.example.model.DimensionTolerance
import com.example.model.RingConfig
import com.example.ui.theme.GlassTheme

/**
 * 工艺参数编辑与新建弹窗 (可编辑孔位、叠放个数、冷态内外径、冷态高度及公差)
 */
@Composable
fun EditConfigDialog(
    initialConfig: RingConfig? = null,
    isNew: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (RingConfig) -> Unit,
    onRestoreDefault: (() -> Unit)? = null,
    onBatchSave: ((List<RingConfig>) -> Unit)? = null
) {
    val context = LocalContext.current
    var model by remember { mutableStateOf(initialConfig?.model ?: "") }
    var hanger by remember { mutableStateOf(initialConfig?.hanger ?: "大") }
    var material by remember { mutableStateOf(initialConfig?.material ?: "42") }
    var holePosition by remember { mutableStateOf(initialConfig?.holePosition ?: "") }
    var quantity by remember { mutableStateOf(initialConfig?.quantity ?: "") }
    
    // 冷态外径 OD
    var odNominal by remember { mutableStateOf(initialConfig?.od?.nominal?.toString() ?: "") }
    var odTolerance by remember { mutableStateOf(initialConfig?.od?.tolerance?.toString() ?: "4") }
    
    // 冷态内径 ID
    var idNominal by remember { mutableStateOf(initialConfig?.id?.nominal?.toString() ?: "") }
    var idTolerance by remember { mutableStateOf(initialConfig?.id?.tolerance?.toString() ?: "6") }
    
    // 冷态高度 H
    var heightNominal by remember { mutableStateOf(initialConfig?.height?.nominal?.toString() ?: "") }
    var heightTolerance by remember { mutableStateOf(initialConfig?.height?.tolerance?.toString() ?: "2") }
    
    // 辅助参数
    var weight by remember { mutableStateOf(initialConfig?.weight ?: "") }
    var fullProductName by remember { mutableStateOf(initialConfig?.fullProductName ?: "") }
    var materialCode by remember { mutableStateOf(initialConfig?.materialCode ?: "") }
    var machinedSize by remember { mutableStateOf(initialConfig?.machinedSize ?: "") }
    var hotSize by remember { mutableStateOf(initialConfig?.hotSize ?: "") }
    var billetInfo by remember { mutableStateOf(initialConfig?.billetInfo ?: "") }
    var notes by remember { mutableStateOf(initialConfig?.notes ?: "-") }

    val scrollState = rememberScrollState()

    // 智能提取物料编码数字后四位（支持如 11197873DJ -> 7873, HZZ008241899DJ -> 1899）
    val suggestedCodeLast4 = remember(materialCode) {
        val digits = materialCode.filter { it.isDigit() }
        if (digits.length >= 4) digits.takeLast(4) else null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(horizontal = 14.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0F172A))
                    .border(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(
                                GlassTheme.CyanGlow.copy(alpha = 0.7f),
                                Color.White.copy(alpha = 0.2f),
                                GlassTheme.AmberGlow.copy(alpha = 0.5f)
                            )
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    // 弹窗头部
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isNew) Icons.Default.Add else Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = GlassTheme.CyanGlow,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isNew) "新增型号规格参数" else "编辑工艺参数 · ${initialConfig?.model}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = GlassTheme.TextWhite
                                )
                            }
                            Text(
                                text = "支持随时更新孔位、叠放个数、冷态内外径与高度尺寸",
                                fontSize = 11.sp,
                                color = GlassTheme.TextMuted
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = GlassTheme.TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // AI 智能图纸识别与力学匹配卡片
                    AiRecognitionSection(
                        currentIdNominal = idNominal.toIntOrNull(),
                        currentWeightStr = weight,
                        currentHeightNominal = heightNominal.toIntOrNull(),
                        onApplyExtracted = { info ->
                            val stdModel = AiVisionManager.deriveStandardModelName(info.model, info.fullProductName, info.materialCode)
                            if (stdModel.isNotBlank()) model = stdModel
                            if (info.material.isNotBlank()) material = info.material
                            if (info.odNominal > 0) {
                                odNominal = info.odNominal.toString()
                                odTolerance = info.odTolerance.toString()
                            }
                            if (info.idNominal > 0) {
                                idNominal = info.idNominal.toString()
                                idTolerance = info.idTolerance.toString()
                            }
                            if (info.heightNominal > 0) {
                                heightNominal = info.heightNominal.toString()
                                heightTolerance = info.heightTolerance.toString()
                            }
                            if (info.weight.isNotBlank()) weight = info.weight
                            if (info.materialCode.isNotBlank()) materialCode = info.materialCode
                            if (info.fullProductName.isNotBlank()) fullProductName = info.fullProductName
                            if (info.machinedSize.isNotBlank()) machinedSize = info.machinedSize
                            if (info.hotSize.isNotBlank()) hotSize = info.hotSize
                            if (info.billetInfo.isNotBlank()) billetInfo = info.billetInfo
                            if (info.notes.isNotBlank() && info.notes != "-") notes = info.notes

                            // 自动应用 AI 匹配推荐的吊具、孔位和叠放个数
                            val rec = info.recommendation ?: com.example.model.HangerRuleEngine.recommend(
                                info.idNominal,
                                com.example.model.HangerRuleEngine.extractWeightNumber(info.weight),
                                info.heightNominal
                            )
                            hanger = rec.hanger
                            holePosition = rec.holePosition
                            quantity = rec.quantity
                        },
                        onQuickApplyRecommendation = { rec ->
                            hanger = rec.hanger
                            holePosition = rec.holePosition
                            quantity = rec.quantity
                        },
                        onBatchImport = { configs ->
                            if (onBatchSave != null) {
                                onBatchSave(configs)
                            } else {
                                ConfigStorageManager.saveBatchConfigs(context, configs)
                                Toast.makeText(context, "已成功批量入库 ${configs.size} 款产品规格！", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. 基本信息
                    EditSectionHeader(title = "1. 基本规格与材质")
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = model,
                            onValueChange = { if (isNew || initialConfig == null) model = it },
                            label = { Text("型号名称 (如 485外 或 7873)") },
                            readOnly = !isNew && initialConfig != null,
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = editFieldColors()
                        )
                    }

                    // 针对物料编码数字后四位命名的智能快捷标签 (如 11197873DJ -> 7873, HZZ008241899DJ -> 1899)
                    if ((isNew || initialConfig == null) && suggestedCodeLast4 != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "编码后4位推荐:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassTheme.CyanGlow
                            )
                            listOf(
                                suggestedCodeLast4,
                                "${suggestedCodeLast4}内",
                                "${suggestedCodeLast4}外"
                            ).forEach { candidate ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(GlassTheme.CyanGlow.copy(alpha = 0.2f))
                                        .border(1.dp, GlassTheme.CyanGlow.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                        .clickable { model = candidate }
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "+ $candidate",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = GlassTheme.CyanGlow
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 吊具选择 + 材质选择
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 吊具
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "选用吊架", fontSize = 11.sp, color = GlassTheme.TextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("大", "小").forEach { h ->
                                    val selected = hanger == h
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (selected) GlassTheme.CyanGlow.copy(alpha = 0.3f)
                                                else Color(0xFF1E293B)
                                            )
                                            .border(
                                                1.dp,
                                                if (selected) GlassTheme.CyanGlow else Color.White.copy(alpha = 0.1f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { hanger = h }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${h}吊具",
                                            fontSize = 12.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selected) GlassTheme.CyanGlow else GlassTheme.TextWhite
                                        )
                                    }
                                }
                            }
                        }

                        // 材质
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "材质牌号", fontSize = 11.sp, color = GlassTheme.TextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("42" to "42CrMo", "50" to "50Mn").forEach { (mat, label) ->
                                    val selected = material == mat
                                    val col = if (mat == "42") GlassTheme.Mat42Text else GlassTheme.Mat50Text
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (selected) col.copy(alpha = 0.25f)
                                                else Color(0xFF1E293B)
                                            )
                                            .border(
                                                1.dp,
                                                if (selected) col else Color.White.copy(alpha = 0.1f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { material = mat }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${mat}钢",
                                            fontSize = 12.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selected) col else GlassTheme.TextWhite
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. 现场吊挂参数 (核心修改项)
                    EditSectionHeader(title = "2. 现场吊具参数 (推荐孔位 & 叠放个数)")
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = holePosition,
                            onValueChange = { holePosition = it },
                            label = { Text("推荐孔位 (如 333, 4孔, 13孔)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = editFieldColors()
                        )
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            label = { Text("叠放个数 (如 3个, 8个, 10个)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = editFieldColors()
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. 冷态尺寸与公差 (核心修改项)
                    EditSectionHeader(title = "3. 冷态尺寸与公差范围 (外径 / 内径 / 高度)")
                    Spacer(modifier = Modifier.height(6.dp))

                    // 冷态外径 OD
                    ToleranceInputField(
                        title = "冷态外径 (OD)",
                        nominal = odNominal,
                        onNominalChange = { odNominal = it },
                        tolerance = odTolerance,
                        onToleranceChange = { odTolerance = it },
                        unitPrefix = "Φ",
                        color = GlassTheme.CyanGlow
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 冷态内径 ID
                    ToleranceInputField(
                        title = "冷态内径 (ID)",
                        nominal = idNominal,
                        onNominalChange = { idNominal = it },
                        tolerance = idTolerance,
                        onToleranceChange = { idTolerance = it },
                        unitPrefix = "Φ",
                        color = GlassTheme.CyanGlow
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 冷态高度 H
                    ToleranceInputField(
                        title = "冷态高度 (H)",
                        nominal = heightNominal,
                        onNominalChange = { heightNominal = it },
                        tolerance = heightTolerance,
                        onToleranceChange = { heightTolerance = it },
                        unitPrefix = "",
                        color = GlassTheme.AmberGlow
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. 辅助工艺参数 (可选)
                    EditSectionHeader(title = "4. 辅助工艺参数与工序卡信息 (可选)")
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { weight = it },
                            label = { Text("下料重量 (如 942 kg)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = editFieldColors()
                        )
                        OutlinedTextField(
                            value = materialCode,
                            onValueChange = { 
                                materialCode = it
                                // 若新建且当前型号名称尚为空，自动同步后四位作为默认型号名
                                if ((isNew || initialConfig == null) && model.isBlank()) {
                                    val digits = it.filter { ch -> ch.isDigit() }
                                    if (digits.length >= 4) {
                                        model = digits.takeLast(4)
                                    }
                                }
                            },
                            label = { Text("物料编码 (如 13624156DJ)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = editFieldColors()
                        )
                    }

                    if (suggestedCodeLast4 != null && (isNew || initialConfig == null)) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "💡 提示: 已检测到数字末4位【$suggestedCodeLast4】，可在上方一键选为型号名称",
                            fontSize = 11.sp,
                            color = GlassTheme.CyanGlow
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = fullProductName,
                        onValueChange = { fullProductName = it },
                        label = { Text("OP40 工序卡品名全称 (如 SY485/SY465外圈)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = editFieldColors()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("工艺注意事项 / 现场备注") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3,
                        colors = editFieldColors()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // 底部操作按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (onRestoreDefault != null && initialConfig?.isCustomized == true) {
                            OutlinedButton(
                                onClick = {
                                    onRestoreDefault()
                                    Toast.makeText(context, "已恢复【${initialConfig.model}】的出厂工艺参数", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GlassTheme.AmberGlow.copy(alpha = 0.6f))
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = GlassTheme.AmberGlow, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("恢复默认", color = GlassTheme.AmberGlow, fontSize = 13.sp)
                            }
                        }

                        Button(
                            onClick = {
                                val cleanModel = model.trim()
                                if (cleanModel.isEmpty()) {
                                    Toast.makeText(context, "请输入型号名称", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val odNom = odNominal.toIntOrNull() ?: 0
                                val odTol = odTolerance.toIntOrNull() ?: 4
                                val idNom = idNominal.toIntOrNull() ?: 0
                                val idTol = idTolerance.toIntOrNull() ?: 6
                                val hNom = heightNominal.toIntOrNull() ?: 0
                                val hTol = heightTolerance.toIntOrNull() ?: 2

                                val newConfig = RingConfig(
                                    model = cleanModel,
                                    hanger = hanger,
                                    holePosition = holePosition.ifBlank { "待定" },
                                    quantity = quantity.ifBlank { "1个" },
                                    material = material,
                                    weight = weight.ifBlank { "-" },
                                    od = DimensionTolerance(odNom, odTol, true),
                                    id = DimensionTolerance(idNom, idTol, true),
                                    height = DimensionTolerance(hNom, hTol, false),
                                    materialCode = materialCode,
                                    fullProductName = fullProductName,
                                    machinedSize = machinedSize,
                                    hotSize = hotSize,
                                    billetInfo = billetInfo,
                                    notes = notes.ifBlank { "-" },
                                    isCustomized = true
                                )
                                onSave(newConfig)
                                Toast.makeText(context, "已成功保存【$cleanModel】工艺参数！", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(if (onRestoreDefault != null && initialConfig?.isCustomized == true) 1.5f else 1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("保存生效", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}
