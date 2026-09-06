package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.GlassTheme

/**
 * 分组标题栏
 */
@Composable
fun EditSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = GlassTheme.CyanGlow
    )
}

/**
 * 公差实时计算与输入行
 */
@Composable
fun ToleranceInputField(
    title: String,
    nominal: String,
    onNominalChange: (String) -> Unit,
    tolerance: String,
    onToleranceChange: (String) -> Unit,
    unitPrefix: String,
    color: Color
) {
    val nomVal = nominal.toIntOrNull()
    val tolVal = tolerance.toIntOrNull() ?: 0
    val previewText = if (nomVal != null) {
        val min = nomVal - tolVal
        val max = nomVal + tolVal
        "$unitPrefix$nomVal±$tolVal mm [ $min ~ $max mm ]"
    } else {
        "待输入有效公称尺寸"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E293B).copy(alpha = 0.5f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = previewText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (nomVal != null) GlassTheme.EmeraldGlow else GlassTheme.TextMuted
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = nominal,
                    onValueChange = onNominalChange,
                    label = { Text("公称值 (mm)") },
                    modifier = Modifier.weight(1.3f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = editFieldColors()
                )
                OutlinedTextField(
                    value = tolerance,
                    onValueChange = onToleranceChange,
                    label = { Text("公差 (±mm)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = editFieldColors()
                )
            }
        }
    }
}

/**
 * 重置出厂确认弹窗
 */
@Composable
fun ResetConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F172A))
                .border(1.5.dp, GlassTheme.RoseGlow.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = GlassTheme.RoseGlow,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "确认恢复出厂设置？",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlassTheme.TextWhite
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "这将清除所有自定义修改的孔位、叠放个数及公差尺寸，恢复为标准 43 种 OP40 辗环工序卡数据。",
                    fontSize = 12.sp,
                    color = GlassTheme.TextMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("取消", color = GlassTheme.TextWhite)
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBE123C))
                    ) {
                        Text("确认重置", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun editFieldColors() = TextFieldDefaults.colors(
    focusedTextColor = GlassTheme.TextWhite,
    unfocusedTextColor = GlassTheme.TextWhite,
    focusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.6f),
    unfocusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.4f),
    focusedIndicatorColor = GlassTheme.CyanGlow,
    unfocusedIndicatorColor = Color.White.copy(alpha = 0.2f),
    focusedLabelColor = GlassTheme.CyanGlow,
    unfocusedLabelColor = GlassTheme.TextMuted,
    cursorColor = GlassTheme.CyanGlow
)

/**
 * 尺寸公差范围展示小卡片
 */
@Composable
fun DimensionRangeBox(
    modifier: Modifier = Modifier,
    label: String,
    toleranceStr: String,
    rangeStr: String,
    color: Color,
    isHighlighted: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isHighlighted) GlassTheme.EmeraldGlow.copy(alpha = 0.25f)
                else Color(0xFF1E293B).copy(alpha = 0.7f)
            )
            .border(
                if (isHighlighted) 1.5.dp else 1.dp,
                if (isHighlighted) GlassTheme.EmeraldGlow else color.copy(alpha = 0.3f),
                RoundedCornerShape(10.dp)
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isHighlighted) "🎯 $label" else label,
                fontSize = 10.sp,
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                color = if (isHighlighted) GlassTheme.EmeraldGlow else GlassTheme.TextMuted
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = toleranceStr,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = if (isHighlighted) GlassTheme.EmeraldGlow else color,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = rangeStr,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = GlassTheme.TextWhite.copy(alpha = 0.9f),
                maxLines = 1
            )
        }
    }
}
