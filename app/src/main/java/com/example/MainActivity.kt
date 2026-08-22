package com.example

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

// 数据模型：严格对应三一重工 / 索特传动 OP40 辗环工序卡 (文件编号: 0005609439, 版本: G.2)
data class DimensionTolerance(
    val nominal: Int,
    val tolerance: Int,
    val isDiameter: Boolean = true,
    val upperTol: Int? = null,
    val lowerTol: Int? = null
) {
    val min: Int get() = if (lowerTol != null) nominal - lowerTol else nominal - tolerance
    val max: Int get() = if (upperTol != null) nominal + upperTol else nominal + tolerance
    val formatted: String get() {
        val prefix = if (isDiameter) "Φ" else ""
        return if (upperTol != null && lowerTol != null) {
            if (lowerTol == 0) "$prefix$nominal(+$upperTol/-0)" else "$prefix$nominal(+$upperTol/-$lowerTol)"
        } else {
            "$prefix$nominal±$tolerance"
        }
    }
    val rangeText: String get() = "$min ~ $max mm"
    val fullText: String get() = "$formatted ($rangeText)"
}

// 实测尺寸检索命中结果
data class MatchHighlight(
    val matched: Boolean,
    val isDimensionMatch: Boolean = false,
    val isMultiMatch: Boolean = false,
    val matchedType: String = "", // "OD", "ID", "H", "MULTI", "TEXT", "MODEL"
    val badgeTitle: String = "",
    val badgeDetail: String = ""
)

data class RingConfig(
    val model: String,
    val hanger: String, // "大" / "小"
    val holePosition: String,
    val quantity: String,
    val material: String, // "42" (42CrMo) / "50" (50Mn)
    val weight: String,   // 下料重量 kg
    val od: DimensionTolerance, // 冷态外径 (标准通用公差: ±4 mm)
    val id: DimensionTolerance, // 冷态内径 (标准通用公差: ±6 mm)
    val height: DimensionTolerance, // 冷态高度 (标准通用公差: ±2 mm)
    val materialCode: String = "", // 物料编码 (例如 11309665DJ)
    val fullProductName: String = "", // 工序卡产品名称 (例如 SY55外圈)
    val machinedSize: String = "", // 精车尺寸 (例如 Φ852 × Φ765 × 61 mm)
    val hotSize: String = "", // 热态尺寸 (例如 Φ870±2 × Φ764(+3/-0) × 145±2 mm)
    val billetInfo: String = "", // 制坯/整平尺寸
    val notes: String = "-",
    val isCustomized: Boolean = false
) {
    // 综合冷态尺寸字符串
    val coldSize: String get() = "${od.formatted} × ${id.formatted} × ${height.formatted} mm"

    /**
     * 智能尺寸公差反查算法：
     * 支持输入实测冷态外径、内径、高度（如 858, 755, 142, 1690, 1520），
     * 自动校验是否落在 [min ~ max] 公差区间内，秒级反查对应型号
     */
    fun evaluateSearch(rawQuery: String): MatchHighlight {
        val query = rawQuery.trim()
        if (query.isEmpty()) return MatchHighlight(matched = true)

        // 1. 优先检查型号名与前缀模糊匹配
        if (model.contains(query, ignoreCase = true)) {
            return MatchHighlight(matched = true, isDimensionMatch = false, matchedType = "MODEL")
        }
        // 检查物料编码 (如 11309665)
        if (materialCode.contains(query, ignoreCase = true)) {
            return MatchHighlight(matched = true, isDimensionMatch = false, matchedType = "TEXT", badgeTitle = "物料编码匹配", badgeDetail = materialCode)
        }
        // 检查工序卡品名 (如 SY55外圈)
        if (fullProductName.contains(query, ignoreCase = true)) {
            return MatchHighlight(matched = true, isDimensionMatch = false, matchedType = "TEXT", badgeTitle = "工序卡品名", badgeDetail = fullProductName)
        }

        // 2. 解析车间工人输入的数值与测量尺寸 (支持 858, 860 755, 858*755*142, 858/755, 860±4 等)
        val sanitized = query
            .replace("±", " ")
            .replace("+", " ")
            .replace("~", " ")
            .replace("～", " ")
            .replace("×", " ")
            .replace("*", " ")
            .replace("x", " ", ignoreCase = true)
            .replace("X", " ")
            .replace("/", " ")
            .replace(",", " ")
            .replace("，", " ")
            .replace("-", " ")
            .replace("Φ", " ", ignoreCase = true)
            .replace("φ", " ", ignoreCase = true)
            .replace("mm", " ", ignoreCase = true)
            .replace("MM", " ", ignoreCase = true)
            .trim()

        val numbers = sanitized.split("\\s+".toRegex()).mapNotNull { it.toIntOrNull() }

        if (numbers.isEmpty()) {
            // 纯文本备注检索
            if (notes.contains(query, ignoreCase = true)) {
                return MatchHighlight(matched = true, isDimensionMatch = false, matchedType = "TEXT", badgeTitle = "备注匹配", badgeDetail = notes)
            }
            return MatchHighlight(matched = false)
        }

        // A. 单个数值检索 (如工人量出外径 858、内径 755、高度 142、1690、1520 等)
        if (numbers.size == 1) {
            val num = numbers[0]

            // 1. 优先命中冷态外径公差区间 [od.min ~ od.max]
            if (num in od.min..od.max) {
                return MatchHighlight(
                    matched = true,
                    isDimensionMatch = true,
                    matchedType = "OD",
                    badgeTitle = "🎯 命中冷态外径",
                    badgeDetail = "实测 ${num}mm 落在外径 [${od.rangeText}]"
                )
            }
            // 2. 命中冷态内径公差区间 [id.min ~ id.max]
            if (num in id.min..id.max) {
                return MatchHighlight(
                    matched = true,
                    isDimensionMatch = true,
                    matchedType = "ID",
                    badgeTitle = "🎯 命中冷态内径",
                    badgeDetail = "实测 ${num}mm 落在内径 [${id.rangeText}]"
                )
            }
            // 3. 命中冷态高度公差区间 [height.min ~ height.max]
            if (num in height.min..height.max) {
                return MatchHighlight(
                    matched = true,
                    isDimensionMatch = true,
                    matchedType = "H",
                    badgeTitle = "🎯 命中冷态高度",
                    badgeDetail = "实测 ${num}mm 落在高度 [${height.rangeText}]"
                )
            }
            // 4. 公称尺寸字符包含匹配 (如输入 860, 1691, 142)
            if (od.nominal.toString().contains(num.toString())) {
                return MatchHighlight(matched = true, isDimensionMatch = true, matchedType = "OD", badgeTitle = "外径公称匹配", badgeDetail = od.formatted)
            }
            if (id.nominal.toString().contains(num.toString())) {
                return MatchHighlight(matched = true, isDimensionMatch = true, matchedType = "ID", badgeTitle = "内径公称匹配", badgeDetail = id.formatted)
            }
            if (height.nominal.toString().contains(num.toString())) {
                return MatchHighlight(matched = true, isDimensionMatch = true, matchedType = "H", badgeTitle = "高度公称匹配", badgeDetail = height.formatted)
            }
            if (weight.contains(num.toString())) {
                return MatchHighlight(matched = true, isDimensionMatch = false, matchedType = "TEXT", badgeTitle = "下料重量", badgeDetail = weight)
            }
        }

        // B. 多个测量数值组合检索 (如工人同时量了外径和内径 "858 755" 或外径和高度 "858 142" 或三维 "858 755 142")
        var odHit = false
        var idHit = false
        var heightHit = false
        val hitDetails = mutableListOf<String>()

        for (num in numbers) {
            if (!odHit && num in od.min..od.max) {
                odHit = true
                hitDetails.add("外径${num}[${od.rangeText}]")
            } else if (!idHit && num in id.min..id.max) {
                idHit = true
                hitDetails.add("内径${num}[${id.rangeText}]")
            } else if (!heightHit && num in height.min..height.max) {
                heightHit = true
                hitDetails.add("高度${num}[${height.rangeText}]")
            }
        }

        val totalHits = (if (odHit) 1 else 0) + (if (idHit) 1 else 0) + (if (heightHit) 1 else 0)
        if (totalHits >= 2 || (numbers.size <= 2 && totalHits >= 1)) {
            return MatchHighlight(
                matched = true,
                isDimensionMatch = true,
                isMultiMatch = true,
                matchedType = "MULTI",
                badgeTitle = "🎯 实测匹配 ($totalHits 项)",
                badgeDetail = hitDetails.joinToString(" · ")
            )
        }

        return MatchHighlight(matched = false)
    }
}

// 工艺卡数据：100% 严格核对自《索特传动设备有限公司 辗环工序卡 OP40辗环 (0005609439 G.2)》
val defaultSanyConfigs = listOf(
    // ========== 大吊具组 ==========
    RingConfig(
        model = "485外",
        hanger = "大",
        holePosition = "333",
        quantity = "3个",
        material = "42",
        weight = "942 kg",
        od = DimensionTolerance(1691, 4),
        id = DimensionTolerance(1520, 6),
        height = DimensionTolerance(262, 2, isDiameter = false),
        materialCode = "13624156DJ",
        fullProductName = "SY485/SY465外圈",
        machinedSize = "Φ1680 × Φ1531 × 121 mm",
        hotSize = "Φ1710±2 × Φ1538(+3/-0) × 264±2 mm",
        billetInfo = "制坯Φ756 / 整平330±5 mm",
        notes = "特殊孔位 333 (SY485/465外圈共用)"
    ),
    RingConfig(
        model = "465内",
        hanger = "大",
        holePosition = "222",
        quantity = "2个",
        material = "42",
        weight = "1237 kg",
        od = DimensionTolerance(1540, 4),
        id = DimensionTolerance(1319, 6),
        height = DimensionTolerance(306, 2, isDiameter = false),
        materialCode = "13524545DJ",
        fullProductName = "SY465内圈",
        machinedSize = "Φ1530 × Φ1332 × 143 mm",
        hotSize = "Φ1557±2 × Φ1335(+3/-0) × 309±2 mm",
        billetInfo = "制坯Φ786 / 整平380±5 mm"
    ),
    RingConfig(
        model = "485内",
        hanger = "大",
        holePosition = "222",
        quantity = "2个",
        material = "42",
        weight = "1307 kg",
        od = DimensionTolerance(1538, 4),
        id = DimensionTolerance(1303, 6),
        height = DimensionTolerance(306, 2, isDiameter = false),
        materialCode = "13946507DJ",
        fullProductName = "SY485内圈",
        machinedSize = "Φ1528 × Φ1316 × 143 mm",
        hotSize = "Φ1555±2 × Φ1319(+3/-0) × 309±2 mm",
        billetInfo = "制坯Φ805 / 整平380±5 mm"
    ),
    RingConfig(
        model = "365外",
        hanger = "大",
        holePosition = "222",
        quantity = "4个",
        material = "50",
        weight = "765 kg",
        od = DimensionTolerance(1561, 4),
        id = DimensionTolerance(1392, 6),
        height = DimensionTolerance(237, 2, isDiameter = false),
        materialCode = "13484727DJ",
        fullProductName = "SY365外圈 (新365为42CrMo/705kg)",
        machinedSize = "Φ1550 × Φ1406 × 109 mm",
        hotSize = "Φ1578±2 × Φ1411(+3/-0) × 240±2 mm",
        billetInfo = "制坯Φ705 / 整平305±5 mm",
        notes = "普通SY365材质为50Mn，新SY365外圈材质为42CrMo"
    ),
    RingConfig(
        model = "265外",
        hanger = "大",
        holePosition = "222",
        quantity = "4个",
        material = "50",
        weight = "638 kg",
        od = DimensionTolerance(1546, 4),
        id = DimensionTolerance(1392, 6),
        height = DimensionTolerance(216, 2, isDiameter = false),
        materialCode = "12621575DJ",
        fullProductName = "SY265C9外圈",
        machinedSize = "Φ1536 × Φ1407 × 98 mm",
        hotSize = "Φ1568±2 × Φ1408(+3/-0) × 219±2 mm",
        billetInfo = "制坯Φ718 / 整平300±5 mm"
    ),
    RingConfig(
        model = "100塔外",
        hanger = "大",
        holePosition = "222",
        quantity = "3个",
        material = "50",
        weight = "982 kg",
        od = DimensionTolerance(1611, 4),
        id = DimensionTolerance(1387, 6),
        height = DimensionTolerance(222, 2, isDiameter = false),
        materialCode = "12816270DJ",
        fullProductName = "SYT100塔机外圈",
        machinedSize = "Φ1606 × Φ1400 × 101 mm",
        hotSize = "Φ1629±2 × Φ1405(+3/-0) × 225±2 mm",
        billetInfo = "制坯Φ800 / 整平290±5 mm"
    ),
    RingConfig(
        model = "1900",
        hanger = "大",
        holePosition = "222",
        quantity = "3个",
        material = "42",
        weight = "675 kg",
        od = DimensionTolerance(1598, 4),
        id = DimensionTolerance(1389, 6),
        height = DimensionTolerance(164, 2, isDiameter = false),
        materialCode = "HZZ008241900DJ",
        fullProductName = "SSM1400.35BHHII-2 (1400外圈)",
        machinedSize = "Φ1588 × Φ1400 × 72 mm",
        hotSize = "Φ1616±2 × Φ1407(+3/-0) × 167±2 mm",
        billetInfo = "制坯Φ734 / 整平244±5 mm",
        notes = "3件合锻: 下料752kg/冷态高度247±2mm [245~249mm]/热态高度250±2mm"
    ),
    RingConfig(
        model = "1447",
        hanger = "大",
        holePosition = "222",
        quantity = "3个",
        material = "42",
        weight = "553 kg",
        od = DimensionTolerance(1510, 4),
        id = DimensionTolerance(1349, 6),
        height = DimensionTolerance(182, 2, isDiameter = false),
        materialCode = "HZZ005431447DJ",
        fullProductName = "SSM1500.40BHH-1 (1500内圈)",
        machinedSize = "Φ1500 × Φ1360 × 81 mm",
        hotSize = "Φ1525±2 × Φ1367(+3/-0) × 185±2 mm",
        billetInfo = "制坯Φ664 / 整平256±5 mm",
        notes = "3件合锻: 下料677kg/冷态高度273±2mm [271~275mm]/热态高度276±2mm"
    ),
    RingConfig(
        model = "1600",
        hanger = "大",
        holePosition = "222",
        quantity = "3或4个",
        material = "42",
        weight = "590 kg",
        od = DimensionTolerance(1610, 4),
        id = DimensionTolerance(1449, 6),
        height = DimensionTolerance(182, 2, isDiameter = false),
        materialCode = "HZZ005661588DJ",
        fullProductName = "SSM1600.40BHH-1 (1600内圈)",
        machinedSize = "Φ1600 × Φ1460 × 81 mm",
        hotSize = "Φ1628±2 × Φ1468(+3/-0) × 185±2 mm",
        billetInfo = "制坯Φ676 / 整平261±5 mm",
        notes = "3件合锻: 下料691kg/冷态高度273±2mm [271~275mm]/热态高度276±2mm"
    ),
    RingConfig(
        model = "1400外",
        hanger = "大",
        holePosition = "222",
        quantity = "3个",
        material = "42",
        weight = "825 kg",
        od = DimensionTolerance(1618, 4),
        id = DimensionTolerance(1384, 6),
        height = DimensionTolerance(182, 2, isDiameter = false),
        materialCode = "HZZ005008247DJ",
        fullProductName = "1400外圈",
        machinedSize = "Φ1606 × Φ1398 × 81 mm",
        hotSize = "Φ1635±2 × Φ1401(+3/-0) × 185±2 mm",
        billetInfo = "制坯Φ791 / 整平250±5 mm"
    ),
    RingConfig(
        model = "泵送1600",
        hanger = "大",
        holePosition = "222",
        quantity = "4个",
        material = "42",
        weight = "831 kg",
        od = DimensionTolerance(1616, 4),
        id = DimensionTolerance(1420, 6),
        height = DimensionTolerance(222, 2, isDiameter = false),
        materialCode = "12760623DJ",
        fullProductName = "泵送1600/50内圈",
        machinedSize = "Φ1600 × Φ1433 × 101 mm",
        hotSize = "Φ1637±2 × Φ1437(+3/-0) × 225±2 mm",
        billetInfo = "制坯Φ745 / 整平290±5 mm"
    ),
    RingConfig(
        model = "1595",
        hanger = "大",
        holePosition = "222",
        quantity = "2个",
        material = "42",
        weight = "1358 kg",
        od = DimensionTolerance(1603, 4),
        id = DimensionTolerance(1371, 6),
        height = DimensionTolerance(304, 2, isDiameter = false),
        materialCode = "HZZ006129434DJ",
        fullProductName = "SSF1595-60CWH-1",
        machinedSize = "Φ1593 × Φ1382 × 142 mm",
        hotSize = "Φ1621±2 × Φ1389(+3/-0) × 307±2 mm",
        billetInfo = "制坯Φ815 / 整平330±5 mm"
    ),
    RingConfig(
        model = "870内",
        hanger = "大",
        holePosition = "222",
        quantity = "2个",
        material = "42",
        weight = "1434 kg",
        od = DimensionTolerance(1686, 4),
        id = DimensionTolerance(1419, 6),
        height = DimensionTolerance(268, 2, isDiameter = false),
        materialCode = "12873385DJ",
        fullProductName = "SY870内圈",
        machinedSize = "Φ1674 × Φ1430 × 124 mm",
        hotSize = "Φ1705±2 × Φ1436(+3/-0) × 271±2 mm",
        billetInfo = "制坯Φ867 / 整平320±5 mm"
    ),
    RingConfig(
        model = "1588",
        hanger = "大",
        holePosition = "222",
        quantity = "3个",
        material = "42",
        weight = "590 kg",
        od = DimensionTolerance(1610, 4),
        id = DimensionTolerance(1449, 6),
        height = DimensionTolerance(182, 2, isDiameter = false),
        materialCode = "HZZ005661588DJ",
        fullProductName = "SSM1600.40BHH-1",
        machinedSize = "Φ1600 × Φ1460 × 81 mm",
        hotSize = "Φ1628±2 × Φ1468(+3/-0) × 185±2 mm",
        billetInfo = "制坯Φ676 / 整平261±5 mm",
        notes = "3件合锻: 下料691kg/冷态高度273±2mm [271~275mm]"
    ),

    // ========== 小吊具组 ==========
    RingConfig(
        model = "55外",
        hanger = "小",
        holePosition = "223",
        quantity = "7个",
        material = "50",
        weight = "164 kg",
        od = DimensionTolerance(860, 4),
        id = DimensionTolerance(755, 6),
        height = DimensionTolerance(142, 2, isDiameter = false),
        materialCode = "11309665DJ",
        fullProductName = "SY55外圈",
        machinedSize = "Φ852 × Φ765 × 61 mm",
        hotSize = "Φ870±2 × Φ764(+3/-0) × 145±2 mm",
        billetInfo = "制坯Φ414 / 整平200±5 mm"
    ),
    RingConfig(
        model = "65外",
        hanger = "小",
        holePosition = "223",
        quantity = "7个",
        material = "50",
        weight = "164 kg",
        od = DimensionTolerance(819, 4),
        id = DimensionTolerance(721, 6),
        height = DimensionTolerance(152, 2, isDiameter = false),
        materialCode = "11184699DJ",
        fullProductName = "SY65外圈",
        machinedSize = "Φ811 × Φ730 × 66 mm",
        hotSize = "Φ828±2 × Φ729(+3/-0) × 155±2 mm",
        billetInfo = "制坯Φ405 / 整平200±5 mm"
    ),
    RingConfig(
        model = "50C外",
        hanger = "小",
        holePosition = "223",
        quantity = "7个",
        material = "50",
        weight = "164 kg",
        od = DimensionTolerance(848, 4),
        id = DimensionTolerance(740, 6),
        height = DimensionTolerance(141, 2, isDiameter = false),
        materialCode = "12966498DJ",
        fullProductName = "SY50C外",
        machinedSize = "Φ840 × Φ750 × 61 mm",
        hotSize = "Φ858±2 × Φ749(+3/-0) × 144±2 mm",
        billetInfo = "制坯Φ419 / 整平200±5 mm"
    ),
    RingConfig(
        model = "95外",
        hanger = "小",
        holePosition = "333",
        quantity = "6或7个",
        material = "50",
        weight = "239 kg",
        od = DimensionTolerance(924, 4),
        id = DimensionTolerance(790, 6),
        height = DimensionTolerance(157, 2, isDiameter = false),
        materialCode = "11316351DJ",
        fullProductName = "SY95外圈",
        machinedSize = "Φ914 × Φ802 × 69 mm",
        hotSize = "Φ936±2 × Φ800(+3/-0) × 160±2 mm",
        billetInfo = "制坯Φ466 / 整平230±5 mm"
    ),
    RingConfig(
        model = "135外",
        hanger = "小",
        holePosition = "444",
        quantity = "6个",
        material = "50",
        weight = "392 kg",
        od = DimensionTolerance(1132, 4),
        id = DimensionTolerance(988, 6),
        height = DimensionTolerance(198, 2, isDiameter = false),
        materialCode = "10835682DJ",
        fullProductName = "SY135外圈",
        machinedSize = "Φ1122 × Φ1002 × 89 mm",
        hotSize = "Φ1147±2 × Φ1001(+3/-0) × 201±2 mm",
        billetInfo = "制坯Φ533 / 整平270±5 mm"
    ),
    RingConfig(
        model = "135内",
        hanger = "小",
        holePosition = "333",
        quantity = "6个",
        material = "42",
        weight = "470 kg",
        od = DimensionTolerance(1007, 4),
        id = DimensionTolerance(809, 6),
        height = DimensionTolerance(202, 2, isDiameter = false),
        materialCode = "10835681DJ",
        fullProductName = "SY135内圈",
        machinedSize = "Φ998 × Φ822 × 91 mm",
        hotSize = "Φ1019±2 × Φ819(+3/-0) × 205±2 mm",
        billetInfo = "制坯Φ575 / 整平270±5 mm"
    ),
    RingConfig(
        model = "230内",
        hanger = "小",
        holePosition = "555",
        quantity = "6个",
        material = "50",
        weight = "520 kg",
        od = DimensionTolerance(1230, 4),
        id = DimensionTolerance(1059, 6),
        height = DimensionTolerance(202, 2, isDiameter = false),
        materialCode = "11841736DJ",
        fullProductName = "SY230III内圈",
        machinedSize = "Φ1219 × Φ1073 × 91 mm",
        hotSize = "Φ1246±2 × Φ1073(+3/-0) × 205±2 mm",
        billetInfo = "制坯Φ626 / 整平280±5 mm"
    ),
    RingConfig(
        model = "230外",
        hanger = "小",
        holePosition = "666",
        quantity = "6个",
        material = "50",
        weight = "446 kg",
        od = DimensionTolerance(1339, 4),
        id = DimensionTolerance(1208, 6),
        height = DimensionTolerance(202, 2, isDiameter = false),
        materialCode = "11841737DJ",
        fullProductName = "SY230III外圈",
        machinedSize = "Φ1328 × Φ1222 × 91 mm",
        hotSize = "Φ1356±2 × Φ1224(+3/-0) × 205±2 mm",
        billetInfo = "制坯Φ586 / 整平285±5 mm"
    ),
    RingConfig(
        model = "365内",
        hanger = "小",
        holePosition = "666",
        quantity = "3个",
        material = "42",
        weight = "967 kg",
        od = DimensionTolerance(1413, 4),
        id = DimensionTolerance(1225, 6),
        height = DimensionTolerance(302, 2, isDiameter = false),
        materialCode = "13484726DJ",
        fullProductName = "SY365内圈",
        machinedSize = "Φ1403 × Φ1239 × 141 mm",
        hotSize = "Φ1429±2 × Φ1239(+3/-0) × 305±2 mm",
        billetInfo = "制坯Φ709 / 整平390±5 mm"
    ),
    RingConfig(
        model = "80塔内",
        hanger = "小",
        holePosition = "555",
        quantity = "6个",
        material = "50",
        weight = "602 kg",
        od = DimensionTolerance(1257, 4),
        id = DimensionTolerance(1098, 6),
        height = DimensionTolerance(246, 2, isDiameter = false),
        materialCode = "12640482DJ",
        fullProductName = "SYT80塔机内圈",
        machinedSize = "Φ1248 × Φ1108 × 113 mm",
        hotSize = "Φ1274±2 × Φ1112(+3/-0) × 249±2 mm",
        billetInfo = "制坯Φ676 / 整平320±5 mm"
    ),
    RingConfig(
        model = "80塔外",
        hanger = "小",
        holePosition = "666",
        quantity = "4个",
        material = "50",
        weight = "854 kg",
        od = DimensionTolerance(1460, 4),
        id = DimensionTolerance(1239, 6),
        height = DimensionTolerance(222, 2, isDiameter = false),
        materialCode = "12640483DJ",
        fullProductName = "SYT80塔机外圈",
        machinedSize = "Φ1450 × Φ1252 × 101 mm",
        hotSize = "Φ1479±2 × Φ1255(+3/-0) × 225±2 mm",
        billetInfo = "制坯Φ764 / 整平280±5 mm"
    ),
    RingConfig(
        model = "1250内",
        hanger = "小",
        holePosition = "555",
        quantity = "6个",
        material = "42",
        weight = "470 kg",
        od = DimensionTolerance(1260, 4),
        id = DimensionTolerance(1095, 6),
        height = DimensionTolerance(183, 2, isDiameter = false),
        materialCode = "HZZ005362157DJ",
        fullProductName = "1250内圈",
        machinedSize = "Φ1250 × Φ1108 × 81 mm",
        hotSize = "Φ1272±2 × Φ1108(+3/-0) × 186±2 mm",
        billetInfo = "制坯Φ639 / 整平240±5 mm",
        notes = "3件合锻: 下料627kg/冷态高度275±2mm [273~277mm]/热态高度278±2mm"
    ),
    RingConfig(
        model = "5519",
        hanger = "小",
        holePosition = "444",
        quantity = "6个",
        material = "42",
        weight = "415 kg",
        od = DimensionTolerance(1128, 4),
        id = DimensionTolerance(979, 6),
        height = DimensionTolerance(200, 2, isDiameter = false),
        materialCode = "13933519DJ",
        fullProductName = "SSM1120.40BHHIII-1",
        machinedSize = "Φ1118 × Φ990 × 90 mm",
        hotSize = "Φ1140±2 × Φ992(+3/-0) × 203±2 mm",
        billetInfo = "制坯Φ548 / 整平268±5 mm"
    ),
    RingConfig(
        model = "5520",
        hanger = "小",
        holePosition = "555",
        quantity = "5个",
        material = "42",
        weight = "630 kg",
        od = DimensionTolerance(1308, 4),
        id = DimensionTolerance(1111, 6),
        height = DimensionTolerance(200, 2, isDiameter = false),
        materialCode = "13933520DJ",
        fullProductName = "SSM1120.40BHHIII-2",
        machinedSize = "Φ1298 × Φ1122 × 90 mm",
        hotSize = "Φ1322±2 × Φ1125(+3/-0) × 203±2 mm",
        billetInfo = "制坯Φ676 / 整平278±5 mm"
    ),
    RingConfig(
        model = "7873",
        hanger = "小",
        holePosition = "555",
        quantity = "6个",
        material = "42",
        weight = "330 kg",
        od = DimensionTolerance(1058, 4),
        id = DimensionTolerance(919, 6),
        height = DimensionTolerance(180, 2, isDiameter = false),
        materialCode = "11197873DJ",
        fullProductName = "SSM1050.40CHW-1",
        machinedSize = "Φ1048 × Φ930 × 80 mm",
        hotSize = "Φ1070±2 × Φ931(+3/-0) × 183±2 mm",
        billetInfo = "制坯Φ518 / 整平243±5 mm"
    ),
    RingConfig(
        model = "7874",
        hanger = "小",
        holePosition = "555",
        quantity = "6个",
        material = "42",
        weight = "543 kg",
        od = DimensionTolerance(1228, 4),
        id = DimensionTolerance(1041, 6),
        height = DimensionTolerance(196, 2, isDiameter = false),
        materialCode = "11197874DJ",
        fullProductName = "SSM1050.40CHW-2",
        machinedSize = "Φ1218 × Φ1052 × 88 mm",
        hotSize = "Φ1242±2 × Φ1055(+3/-0) × 199±2 mm",
        billetInfo = "制坯Φ613 / 整平270±5 mm"
    ),
    RingConfig(
        model = "1250",
        hanger = "小",
        holePosition = "555",
        quantity = "6个",
        material = "50",
        weight = "447 kg",
        od = DimensionTolerance(1261, 4),
        id = DimensionTolerance(1103, 6),
        height = DimensionTolerance(184, 2, isDiameter = false),
        materialCode = "HZZ005405873DJ",
        fullProductName = "SSF1250.25CWH-1 (1250内圈)",
        machinedSize = "Φ1251 × Φ1114 × 82 mm",
        hotSize = "Φ1277±2 × Φ1118(+3/-0) × 187±2 mm",
        billetInfo = "制坯Φ575 / 整平258±5 mm",
        notes = "3件合锻: 下料627kg/冷态高度275±2mm [273~277mm]/热态高度278±2mm"
    ),
    RingConfig(
        model = "265内",
        hanger = "小",
        holePosition = "666",
        quantity = "4个",
        material = "50",
        weight = "787 kg",
        od = DimensionTolerance(1413, 4),
        id = DimensionTolerance(1223, 6),
        height = DimensionTolerance(242, 2, isDiameter = false),
        materialCode = "12621574DJ",
        fullProductName = "SY265C9内圈",
        machinedSize = "Φ1403 × Φ1236 × 111 mm",
        hotSize = "Φ1432±2 × Φ1238(+3/-0) × 245±2 mm",
        billetInfo = "制坯Φ718 / 整平300±5 mm"
    ),
    RingConfig(
        model = "1250外",
        hanger = "小",
        holePosition = "666",
        quantity = "4个",
        material = "42",
        weight = "715 kg",
        od = DimensionTolerance(1460, 4),
        id = DimensionTolerance(1237, 6),
        height = DimensionTolerance(183, 2, isDiameter = false),
        materialCode = "HZZ005362158DJ",
        fullProductName = "1250外圈",
        machinedSize = "Φ1450 × Φ1250 × 81 mm",
        hotSize = "Φ1476±2 × Φ1251(+3/-0) × 186±2 mm",
        billetInfo = "制坯Φ751 / 整平245±5 mm",
        notes = "另有50Mn 1250/25外圈 (四件合锻下料585kg/单件510kg/高度250±2mm)"
    ),
    RingConfig(
        model = "1400内",
        hanger = "小",
        holePosition = "666",
        quantity = "6个",
        material = "42",
        weight = "533 kg",
        od = DimensionTolerance(1413, 4),
        id = DimensionTolerance(1245, 6),
        height = DimensionTolerance(182, 2, isDiameter = false),
        materialCode = "HZZ005008244DJ",
        fullProductName = "1400内圈",
        machinedSize = "Φ1402 × Φ1258 × 81 mm",
        hotSize = "Φ1427±2 × Φ1260(+3/-0) × 185±2 mm",
        billetInfo = "制坯Φ671 / 整平240±5 mm",
        notes = "3件合锻: 下料658kg/冷态高度247±2mm [245~249mm]/热态高度250±2mm"
    ),
    RingConfig(
        model = "100塔内",
        hanger = "小",
        holePosition = "666",
        quantity = "5个",
        material = "50",
        weight = "626 kg",
        od = DimensionTolerance(1408, 4),
        id = DimensionTolerance(1245, 6),
        height = DimensionTolerance(222, 2, isDiameter = false),
        materialCode = "12816269DJ",
        fullProductName = "SYT100塔机内圈",
        machinedSize = "Φ1397 × Φ1258 × 101 mm",
        hotSize = "Φ1426±2 × Φ1261(+3/-0) × 225±2 mm",
        billetInfo = "制坯Φ655 / 整平300±5 mm"
    ),
    RingConfig(
        model = "1899",
        hanger = "小",
        holePosition = "666",
        quantity = "6个",
        material = "42",
        weight = "472 kg",
        od = DimensionTolerance(1410, 4),
        id = DimensionTolerance(1249, 6),
        height = DimensionTolerance(164, 2, isDiameter = false),
        materialCode = "HZZ008241899DJ",
        fullProductName = "SSM1400.35BHHII-1 (1400内圈)",
        machinedSize = "Φ1400 × Φ1260 × 72 mm",
        hotSize = "Φ1426±2 × Φ1265(+3/-0) × 167±2 mm",
        billetInfo = "制坯Φ645 / 整平235±5 mm",
        notes = "3件合锻: 下料658kg/冷态高度247±2mm [245~249mm]/热态高度250±2mm"
    )
)

// 参数持久化存储管理器 (支持用户修改孔位、叠放个数、内外径与高度尺寸并保存于设备)
object ConfigStorageManager {
    private const val PREFS_NAME = "sany_hanger_configs_v1"
    private const val KEY_CUSTOM_CONFIGS = "custom_configs_json"
    private const val KEY_ADDED_MODELS = "added_models_json"

    fun loadAllConfigs(context: Context): List<RingConfig> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val customMap = loadCustomConfigsMap(prefs)

        val resultList = mutableListOf<RingConfig>()
        val defaultModelsSeen = mutableSetOf<String>()

        // 1. 合并默认 43 种型号与用户的自定义修改
        for (defaultCfg in defaultSanyConfigs) {
            defaultModelsSeen.add(defaultCfg.model)
            val custom = customMap[defaultCfg.model]
            if (custom != null) {
                resultList.add(custom.copy(isCustomized = true))
            } else {
                resultList.add(defaultCfg)
            }
        }

        // 2. 追加用户新增的全新规格型号
        for ((model, customCfg) in customMap) {
            if (!defaultModelsSeen.contains(model)) {
                resultList.add(customCfg.copy(isCustomized = true))
            }
        }

        return resultList
    }

    fun saveConfig(context: Context, updated: RingConfig) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val map = loadCustomConfigsMap(prefs)
        map[updated.model] = updated.copy(isCustomized = true)
        saveCustomConfigsMap(prefs, map)
    }

    fun restoreSingleDefault(context: Context, model: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val map = loadCustomConfigsMap(prefs)
        map.remove(model)
        saveCustomConfigsMap(prefs, map)
    }

    fun resetAllToFactory(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    private fun loadCustomConfigsMap(prefs: android.content.SharedPreferences): MutableMap<String, RingConfig> {
        val map = mutableMapOf<String, RingConfig>()
        val jsonStr = prefs.getString(KEY_CUSTOM_CONFIGS, null) ?: return map
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val cfg = parseRingConfigFromJson(obj)
                map[cfg.model] = cfg.copy(isCustomized = true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

    private fun saveCustomConfigsMap(prefs: android.content.SharedPreferences, map: Map<String, RingConfig>) {
        val array = JSONArray()
        for (cfg in map.values) {
            array.put(ringConfigToJson(cfg))
        }
        prefs.edit().putString(KEY_CUSTOM_CONFIGS, array.toString()).apply()
    }

    private fun ringConfigToJson(cfg: RingConfig): JSONObject {
        val json = JSONObject()
        json.put("model", cfg.model)
        json.put("hanger", cfg.hanger)
        json.put("holePosition", cfg.holePosition)
        json.put("quantity", cfg.quantity)
        json.put("material", cfg.material)
        json.put("weight", cfg.weight)

        val odJson = JSONObject()
        odJson.put("nominal", cfg.od.nominal)
        odJson.put("tolerance", cfg.od.tolerance)
        odJson.put("isDiameter", cfg.od.isDiameter)
        cfg.od.upperTol?.let { odJson.put("upperTol", it) }
        cfg.od.lowerTol?.let { odJson.put("lowerTol", it) }
        json.put("od", odJson)

        val idJson = JSONObject()
        idJson.put("nominal", cfg.id.nominal)
        idJson.put("tolerance", cfg.id.tolerance)
        idJson.put("isDiameter", cfg.id.isDiameter)
        cfg.id.upperTol?.let { idJson.put("upperTol", it) }
        cfg.id.lowerTol?.let { idJson.put("lowerTol", it) }
        json.put("id", idJson)

        val hJson = JSONObject()
        hJson.put("nominal", cfg.height.nominal)
        hJson.put("tolerance", cfg.height.tolerance)
        hJson.put("isDiameter", cfg.height.isDiameter)
        cfg.height.upperTol?.let { hJson.put("upperTol", it) }
        cfg.height.lowerTol?.let { hJson.put("lowerTol", it) }
        json.put("height", hJson)

        json.put("materialCode", cfg.materialCode)
        json.put("fullProductName", cfg.fullProductName)
        json.put("machinedSize", cfg.machinedSize)
        json.put("hotSize", cfg.hotSize)
        json.put("billetInfo", cfg.billetInfo)
        json.put("notes", cfg.notes)
        json.put("isCustomized", true)
        return json
    }

    private fun parseRingConfigFromJson(json: JSONObject): RingConfig {
        fun parseTolerance(key: String, defaultIsDiameter: Boolean): DimensionTolerance {
            val obj = json.optJSONObject(key) ?: return DimensionTolerance(0, 0, defaultIsDiameter)
            val nom = obj.optInt("nominal", 0)
            val tol = obj.optInt("tolerance", 0)
            val isDia = obj.optBoolean("isDiameter", defaultIsDiameter)
            val upper = if (obj.has("upperTol")) obj.getInt("upperTol") else null
            val lower = if (obj.has("lowerTol")) obj.getInt("lowerTol") else null
            return DimensionTolerance(nom, tol, isDia, upper, lower)
        }

        return RingConfig(
            model = json.optString("model", ""),
            hanger = json.optString("hanger", "大"),
            holePosition = json.optString("holePosition", ""),
            quantity = json.optString("quantity", ""),
            material = json.optString("material", "42"),
            weight = json.optString("weight", ""),
            od = parseTolerance("od", true),
            id = parseTolerance("id", true),
            height = parseTolerance("height", false),
            materialCode = json.optString("materialCode", ""),
            fullProductName = json.optString("fullProductName", ""),
            machinedSize = json.optString("machinedSize", ""),
            hotSize = json.optString("hotSize", ""),
            billetInfo = json.optString("billetInfo", ""),
            notes = json.optString("notes", "-"),
            isCustomized = true
        )
    }
}

// 毛玻璃设计配色常量
object GlassTheme {
    val BackgroundTop = Color(0xFF0A0F1D)
    val BackgroundBottom = Color(0xFF0F172A)
    
    val OrbBlue = Color(0xFF1E40AF).copy(alpha = 0.35f)
    val OrbPurple = Color(0xFF6D28D9).copy(alpha = 0.28f)
    val OrbCyan = Color(0xFF0284C7).copy(alpha = 0.25f)
    
    val CardBg = Color(0xFF1E293B).copy(alpha = 0.60f)
    val CardBgActive = Color(0xFF334155).copy(alpha = 0.75f)
    
    val BorderLight = Color(0xFFFFFFFF).copy(alpha = 0.18f)
    val BorderSubtle = Color(0xFFFFFFFF).copy(alpha = 0.08f)
    val BorderHighlight = Color(0xFF38BDF8).copy(alpha = 0.65f)
    
    val TextWhite = Color(0xFFF8FAFC)
    val TextMuted = Color(0xFF94A3B8)
    val TextDim = Color(0xFF64748B)
    
    val CyanGlow = Color(0xFF38BDF8)
    val AmberGlow = Color(0xFFFBBF24)
    val EmeraldGlow = Color(0xFF34D399)
    val RoseGlow = Color(0xFFFB7185)
    
    val Mat42Bg = Color(0xFF064E3B).copy(alpha = 0.45f)
    val Mat42Text = Color(0xFF34D399)
    val Mat50Bg = Color(0xFF78350F).copy(alpha = 0.45f)
    val Mat50Text = Color(0xFFFBBF24)
}

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
                                // 新增型号按键
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(GlassTheme.CyanGlow.copy(alpha = 0.25f))
                                        .border(1.dp, GlassTheme.CyanGlow.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                        .clickable {
                                            isCreatingNew = true
                                            editingConfig = null
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "新增型号",
                                            tint = GlassTheme.CyanGlow,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "新增规格",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
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
    }
}

/**
 * 毛玻璃型号卡片（支持尺寸匹配高亮、用户自定义徽章与快速编辑按钮）
 */
@Composable
fun GlassModelCard(
    config: RingConfig,
    highlight: MatchHighlight? = null,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    val is42 = config.material == "42"
    val isLarge = config.hanger == "大"
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDimMatched = highlight?.isDimensionMatch == true
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        if (isDimMatched) Color(0xFF064E3B).copy(alpha = 0.45f) else Color(0xFF1E293B).copy(alpha = 0.65f),
                        Color(0xFF0F172A).copy(alpha = 0.75f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        if (isDimMatched) GlassTheme.EmeraldGlow.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.22f),
                        if (isDimMatched) GlassTheme.CyanGlow.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.05f)
                    )
                ),
                RoundedCornerShape(18.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = GlassTheme.CyanGlow),
                onClick = onClick
            )
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 顶部：型号名称 + 材质徽标 + 自定义标识
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = config.model,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = GlassTheme.TextWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // 若被用户自定义修改，显示小绿点
                    if (config.isCustomized) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(GlassTheme.EmeraldGlow.copy(alpha = 0.25f))
                                .border(0.8.dp, GlassTheme.EmeraldGlow.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "已自定义",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassTheme.EmeraldGlow
                            )
                        }
                    }

                    // 材质芯片 (毛玻璃微晶)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (is42) GlassTheme.Mat42Bg else GlassTheme.Mat50Bg)
                            .border(
                                1.dp,
                                (if (is42) GlassTheme.Mat42Text else GlassTheme.Mat50Text).copy(alpha = 0.5f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${config.material}钢",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (is42) GlassTheme.Mat42Text else GlassTheme.Mat50Text
                        )
                    }
                }
            }

            // 如果有尺寸命中徽章，高亮提示
            if (highlight != null && highlight.badgeTitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isDimMatched) GlassTheme.EmeraldGlow.copy(alpha = 0.18f)
                            else GlassTheme.CyanGlow.copy(alpha = 0.15f)
                        )
                        .border(
                            1.dp,
                            if (isDimMatched) GlassTheme.EmeraldGlow.copy(alpha = 0.6f)
                            else GlassTheme.CyanGlow.copy(alpha = 0.4f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Column {
                        Text(
                            text = highlight.badgeTitle,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDimMatched) GlassTheme.EmeraldGlow else GlassTheme.CyanGlow
                        )
                        if (highlight.badgeDetail.isNotBlank()) {
                            Text(
                                text = highlight.badgeDetail,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Normal,
                                color = GlassTheme.TextWhite.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 中部：吊具与孔位高亮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isLarge) GlassTheme.CyanGlow else GlassTheme.EmeraldGlow)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${config.hanger}吊具",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLarge) GlassTheme.CyanGlow else GlassTheme.EmeraldGlow
                    )
                }

                // 孔位
                Text(
                    text = "孔位 ${config.holePosition}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = GlassTheme.AmberGlow
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            // 尺寸与公差胶囊
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.75f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Column {
                    Text(
                        text = "外:${config.od.formatted} 内:${config.id.formatted}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTheme.CyanGlow
                    )
                    Text(
                        text = "高:${config.height.formatted} | 放 ${config.quantity}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlassTheme.TextWhite.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 底部：重量与快速编辑按键
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = config.weight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = GlassTheme.TextMuted
                )

                // 快捷编辑按钮
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF334155).copy(alpha = 0.6f))
                        .border(0.8.dp, GlassTheme.CyanGlow.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .clickable { onEdit() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "编辑",
                            tint = GlassTheme.CyanGlow,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "编辑",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlassTheme.CyanGlow
                        )
                    }
                }
            }
        }
    }
}

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

/**
 * 工艺参数编辑与新建弹窗 (可编辑孔位、叠放个数、冷态内外径、冷态高度及公差)
 */
@Composable
fun EditConfigDialog(
    initialConfig: RingConfig? = null,
    isNew: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (RingConfig) -> Unit,
    onRestoreDefault: (() -> Unit)? = null
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
                            label = { Text("型号名称 (如 485外)") },
                            readOnly = !isNew && initialConfig != null,
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = editFieldColors()
                        )
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
                            onValueChange = { materialCode = it },
                            label = { Text("物料编码 (如 13624156DJ)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = editFieldColors()
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
