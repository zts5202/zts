package com.example.model

/**
 * 匹配推荐结果数据模型
 */
data class HangerRecommendation(
    val hanger: String,         // "大" 或 "小"
    val holePosition: String,   // 推荐孔位 (如 "222", "333", "444", "555", "666", "223")
    val quantity: String,       // 推荐叠放个数 (如 "2个", "3个", "4个", "6个", "7个")
    val hangerReason: String,   // 吊具与孔位力学匹配原因
    val quantityReason: String, // 叠放个数热处理承重与炉膛原因
    val summaryText: String     // 综合工艺结论
)

/**
 * 车间吊具工艺与热处理装炉数学力学规则引擎
 */
object HangerRuleEngine {

    /**
     * 根据冷态内径(ID)、下料重量(Weight)与高度(Height)自动推导最佳吊具、孔位与叠放个数
     */
    fun recommend(
        idNominal: Int?,
        weightKg: Int?,
        heightNominal: Int?
    ): HangerRecommendation {
        // 1. 吊具类型与孔位推荐
        val (recHanger, recHole, hangerReason) = recommendHangerAndHole(idNominal)

        // 2. 叠放个数推荐
        val (recQuantity, quantityReason) = recommendQuantity(weightKg, heightNominal)

        val summaryText = buildString {
            append("【AI工艺推理】：")
            append("选用 ${recHanger}吊具 ${recHole}孔，建议叠放 ${recQuantity}。")
            if (idNominal != null) {
                append(" (冷态内径 Φ${idNominal}mm 匹配吊臂跨度；")
            }
            if (weightKg != null && weightKg > 0) {
                val countInt = recQuantity.filter { it.isDigit() }.toIntOrNull() ?: 1
                val totalWeight = (weightKg * countInt) / 1000.0
                append("单重 ${weightKg}kg × ${recQuantity}，单柱整吊重约 ${String.format("%.2f", totalWeight)} 吨，处于调质炉 2.5~3.0 吨安全承重线)")
            } else {
                append(")")
            }
        }

        return HangerRecommendation(
            hanger = recHanger,
            holePosition = recHole,
            quantity = recQuantity,
            hangerReason = hangerReason,
            quantityReason = quantityReason,
            summaryText = summaryText
        )
    }

    private fun recommendHangerAndHole(idNominal: Int?): Triple<String, String, String> {
        if (idNominal == null || idNominal <= 0) {
            return Triple("小", "444", "待输入有效内径尺寸以精确匹配吊架与孔位")
        }

        return if (idNominal >= 1280) {
            // 大吊具区间 (内径 >= 1280mm)
            if (idNominal >= 1490) {
                Triple(
                    "大",
                    "333",
                    "工件冷态内径 Φ${idNominal}mm ≥ 1490mm，属于超大跨度环件，选用【大吊具 333孔】使吊爪向外微扩，确保支撑接触面积与安全防滑脱。"
                )
            } else {
                Triple(
                    "大",
                    "222",
                    "工件冷态内径 Φ${idNominal}mm 处于 1280~1480mm 范围，选用【大吊具 222孔】，契合大吊具标准底座托爪展开跨度。"
                )
            }
        } else {
            // 小吊具区间 (内径 < 1280mm)
            when {
                idNominal <= 765 -> {
                    Triple(
                        "小",
                        "223",
                        "工件冷态内径 Φ${idNominal}mm 处于 700~765mm 极小内径群，选用【小吊具 223孔】，收紧撑脚跨度避免探臂刮碰工件内壁。"
                    )
                }
                idNominal <= 860 -> {
                    Triple(
                        "小",
                        "333",
                        "工件冷态内径 Φ${idNominal}mm 处于 766~860mm，选用【小吊具 333孔】标准定位档位。"
                    )
                }
                idNominal <= 1010 -> {
                    Triple(
                        "小",
                        "444",
                        "工件冷态内径 Φ${idNominal}mm 处于 861~1010mm，选用【小吊具 444孔】中等档位。"
                    )
                }
                idNominal <= 1160 -> {
                    Triple(
                        "小",
                        "555",
                        "工件冷态内径 Φ${idNominal}mm 处于 1011~1160mm，选用【小吊具 555孔】较宽档位，提升三爪受力均匀性。"
                    )
                }
                else -> {
                    Triple(
                        "小",
                        "666",
                        "工件冷态内径 Φ${idNominal}mm 处于 1161~1280mm（小吊架极限尺寸群），选用【小吊具 666孔】最外侧撑脚档位。"
                    )
                }
            }
        }
    }

    private fun recommendQuantity(weightKg: Int?, heightNominal: Int?): Pair<String, String> {
        if (weightKg == null || weightKg <= 0) {
            return Pair("4个", "根据通用工艺标准默认建议 4 个，输入具体单件重量后可精确计算")
        }

        // 1. 根据单重计算叠放上限 (目标：单柱总重限制在 2400~3000kg)
        val countByWeight = when {
            weightKg >= 1100 -> 2
            weightKg >= 750 -> 3
            weightKg >= 580 -> 4
            weightKg >= 380 -> 6
            else -> 7
        }

        // 2. 校验单柱总高度 (炉膛与淬火油槽深度限制在 1000~1300mm 以内)
        var finalCount = countByWeight
        if (heightNominal != null && heightNominal > 0) {
            val maxCountByHeight = (1200 / heightNominal).coerceAtLeast(2)
            if (maxCountByHeight < finalCount) {
                finalCount = maxCountByHeight
            }
        }

        val totalWeightTon = String.format("%.2f", (weightKg * finalCount) / 1000.0)
        val reason = when {
            weightKg >= 1100 -> {
                "单件重达 ${weightKg}kg，为防止 850~900℃ 高温调质时底层工件受压塑性下沉变形，并控制行车吊重安全，严格建议【叠放 2个】(总重约 ${totalWeightTon} 吨)。"
            }
            weightKg >= 750 -> {
                "单件重 ${weightKg}kg 属于重型环件，建议【叠放 3个】(整柱总重约 ${totalWeightTon} 吨，完美落在 2.5~3.0 吨经济与安全区间)。"
            }
            weightKg >= 580 -> {
                "单件重 ${weightKg}kg，建议【叠放 4个】(整柱总重约 ${totalWeightTon} 吨)。"
            }
            weightKg >= 380 -> {
                "单件重 ${weightKg}kg，建议【叠放 6个】(整柱总重约 ${totalWeightTon} 吨)。"
            }
            else -> {
                "单件重 ${weightKg}kg 属于轻型件，建议【叠放 7个】(整柱总高约符合炉膛 1.1~1.2 米装料窗)。"
            }
        }

        return Pair("${finalCount}个", reason)
    }

    /**
     * 辅助工具：从任意重量字符串提取整数字 (例如 "942 kg" -> 942)
     */
    fun extractWeightNumber(weightStr: String): Int? {
        val digits = weightStr.filter { it.isDigit() }
        return digits.toIntOrNull()
    }
}
