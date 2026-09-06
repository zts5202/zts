package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AiVisionManager
import com.example.data.ExtractedProcessInfo
import com.example.model.DimensionTolerance
import com.example.model.HangerRuleEngine
import com.example.model.RingConfig
import com.example.ui.theme.GlassTheme

/**
 * AI 图像识别多产品候选卡片弹窗
 * - 支持单项点选载入到当前编辑表单
 * - 支持勾选多项或全选后“一键全部批量入库”
 */
@Composable
fun MultiProductCandidateDialog(
    candidates: List<ExtractedProcessInfo>,
    onDismiss: () -> Unit,
    onSelectOne: (ExtractedProcessInfo) -> Unit,
    onBatchImport: (List<RingConfig>) -> Unit
) {
    // 默认全选所有识别出来的产品
    var selectedIndices by remember(candidates) {
        mutableStateOf(candidates.indices.toSet())
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        1.5.dp,
                        Brush.verticalGradient(
                            listOf(GlassTheme.CyanGlow.copy(alpha = 0.8f), GlassTheme.EmeraldGlow.copy(alpha = 0.4f))
                        ),
                        RoundedCornerShape(20.dp)
                    ),
                color = Color(0xFF070F1E)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // 1. 顶部标题栏
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(GlassTheme.CyanGlow.copy(alpha = 0.2f))
                                    .border(1.dp, GlassTheme.CyanGlow, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = GlassTheme.CyanGlow,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "AI 识别到多款产品",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Black,
                                        color = GlassTheme.TextWhite
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(GlassTheme.EmeraldGlow.copy(alpha = 0.25f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "共 ${candidates.size} 款规格",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GlassTheme.EmeraldGlow
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "点选单个载入编辑，或勾选后一键批量入库",
                                    fontSize = 11.sp,
                                    color = GlassTheme.TextMuted
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = GlassTheme.TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. 批量选择操作工具条
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F1A2E))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selectedIndices.size == candidates.size && candidates.isNotEmpty(),
                                onCheckedChange = { checked ->
                                    selectedIndices = if (checked) candidates.indices.toSet() else emptySet()
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = GlassTheme.CyanGlow,
                                    uncheckedColor = GlassTheme.TextMuted
                                )
                            )
                            Text(
                                text = "全选 (${selectedIndices.size}/${candidates.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassTheme.TextWhite
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // 反选快捷按键
                            TextButton(
                                onClick = {
                                    selectedIndices = candidates.indices.toSet() - selectedIndices
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("反选", fontSize = 11.sp, color = GlassTheme.CyanGlow)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. 候选卡片列表
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(candidates) { index, item ->
                            val isChecked = selectedIndices.contains(index)
                            val rec = item.recommendation ?: HangerRuleEngine.recommend(
                                item.idNominal,
                                HangerRuleEngine.extractWeightNumber(item.weight),
                                item.heightNominal
                            )

                            CandidateCard(
                                index = index,
                                item = item,
                                rec = rec,
                                isChecked = isChecked,
                                onCheckedChange = { checked ->
                                    selectedIndices = if (checked) {
                                        selectedIndices + index
                                    } else {
                                        selectedIndices - index
                                    }
                                },
                                onSelectOne = {
                                    onSelectOne(item)
                                    onDismiss()
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4. 底部批量操作按钮条
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassTheme.TextMuted.copy(alpha = 0.5f))
                        ) {
                            Text("暂不入库", color = GlassTheme.TextMuted, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                val selectedList = selectedIndices.map { candidates[it] }
                                val configs = selectedList.map { candidateToRingConfig(it) }
                                onBatchImport(configs)
                                onDismiss()
                            },
                            enabled = selectedIndices.isNotEmpty(),
                            modifier = Modifier
                                .weight(2f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF059669),
                                disabledContainerColor = Color(0xFF1E293B)
                            )
                        ) {
                            Icon(
                                Icons.Default.Bolt,
                                contentDescription = null,
                                tint = if (selectedIndices.isNotEmpty()) Color.White else GlassTheme.TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "一键批量入库 (${selectedIndices.size} 款)",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedIndices.isNotEmpty()) Color.White else GlassTheme.TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 单个候选产品展示卡片
 */
@Composable
private fun CandidateCard(
    index: Int,
    item: ExtractedProcessInfo,
    rec: com.example.model.HangerRecommendation,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onSelectOne: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0D1B2E))
            .border(
                1.dp,
                if (isChecked) GlassTheme.CyanGlow.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(12.dp)
            )
            .padding(10.dp)
    ) {
        Column {
            // 卡片头部
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = onCheckedChange,
                        modifier = Modifier.size(28.dp),
                        colors = CheckboxDefaults.colors(
                            checkedColor = GlassTheme.CyanGlow,
                            uncheckedColor = GlassTheme.TextMuted
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlassTheme.CyanGlow
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    val displayModel = AiVisionManager.deriveStandardModelName(item.model, item.fullProductName, item.materialCode)
                    Text(
                        text = displayModel,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = GlassTheme.CyanGlow
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // 材质标签
                    val matText = if (item.material == "50") "50Mn" else "42CrMo"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (item.material == "50") Color(0xFFF59E0B).copy(alpha = 0.2f) else Color(0xFF38BDF8).copy(alpha = 0.2f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = matText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.material == "50") GlassTheme.AmberGlow else GlassTheme.CyanGlow
                        )
                    }
                }

                // 单项快速载入按键
                Button(
                    onClick = onSelectOne,
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("单项载入", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 全称与编码
            if (item.fullProductName.isNotBlank() || item.materialCode.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (item.materialCode.isNotBlank()) {
                        Text(
                            text = "编码: ${item.materialCode}",
                            fontSize = 11.sp,
                            color = GlassTheme.TextMuted
                        )
                    }
                    if (item.fullProductName.isNotBlank()) {
                        Text(
                            text = "品名: ${item.fullProductName}",
                            fontSize = 11.sp,
                            color = GlassTheme.TextWhite.copy(alpha = 0.8f),
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 尺寸与参数排布
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF081220))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "冷态: Φ${item.odNominal} × Φ${item.idNominal} × ${item.heightNominal}mm",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = GlassTheme.TextWhite
                )
                Text(
                    text = "单重: ${item.weight.ifBlank { "-" }}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlassTheme.AmberGlow
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 智能力学匹配结果
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "推荐吊具:",
                    fontSize = 10.sp,
                    color = GlassTheme.TextMuted
                )
                Text(
                    text = "${rec.hanger}吊架 ${rec.holePosition}孔",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlassTheme.CyanGlow
                )
                Text(
                    text = "· 叠放: ${rec.quantity}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlassTheme.EmeraldGlow
                )
            }
        }
    }
}

/**
 * 将 AI 提取项转换为持久化 RingConfig
 */
fun candidateToRingConfig(item: ExtractedProcessInfo): RingConfig {
    val standardModel = AiVisionManager.deriveStandardModelName(item.model, item.fullProductName, item.materialCode)
    val rec = item.recommendation ?: HangerRuleEngine.recommend(
        item.idNominal,
        HangerRuleEngine.extractWeightNumber(item.weight),
        item.heightNominal
    )
    return RingConfig(
        model = standardModel,
        hanger = rec.hanger,
        holePosition = rec.holePosition.ifBlank { "待定" },
        quantity = rec.quantity.ifBlank { "1个" },
        material = item.material.ifBlank { "42" },
        weight = item.weight.ifBlank { "-" },
        od = DimensionTolerance(item.odNominal, item.odTolerance, true),
        id = DimensionTolerance(item.idNominal, item.idTolerance, true),
        height = DimensionTolerance(item.heightNominal, item.heightTolerance, false),
        materialCode = item.materialCode,
        fullProductName = item.fullProductName,
        machinedSize = item.machinedSize,
        hotSize = item.hotSize,
        billetInfo = item.billetInfo,
        notes = item.notes.ifBlank { "AI多规格智能入库" },
        isCustomized = true
    )
}
