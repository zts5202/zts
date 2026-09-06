package com.example.model

/**
 * 辗环工艺配置模型：严格对应三一重工 / 索特传动 OP40 辗环工序卡 (文件编号: 0005609439, 版本: G.2)
 */
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
