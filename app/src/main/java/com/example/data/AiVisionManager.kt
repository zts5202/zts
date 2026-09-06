package com.example.data

import android.content.Context

enum class AiEngine(val id: String, val displayName: String, val badgeText: String) {
    GEMINI("gemini", "Google Gemini", "推荐·原生视觉"),
    DEEPSEEK("deepseek", "DeepSeek", "仅文本/需视觉网关")
}

object AiVisionManager {
    private const val PREFS_NAME = "ai_vision_preferences"
    private const val KEY_ACTIVE_ENGINE = "active_ai_engine"

    fun getActiveEngine(context: Context): AiEngine {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val id = prefs.getString(KEY_ACTIVE_ENGINE, AiEngine.GEMINI.id)
        return AiEngine.values().find { it.id == id } ?: AiEngine.GEMINI
    }

    fun setActiveEngine(context: Context, engine: AiEngine) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ACTIVE_ENGINE, engine.id).apply()
    }

    suspend fun recognizeProcessImage(
        context: Context,
        imageBase64: String,
        targetEngine: AiEngine = getActiveEngine(context)
    ): Result<List<ExtractedProcessInfo>> {
        val geminiKey = GeminiAiService.getApiKey(context)
        val deepseekKey = DeepSeekAiService.getApiKey(context)

        return when (targetEngine) {
            AiEngine.GEMINI -> {
                val geminiResult = GeminiAiService.recognizeProcessImage(context, imageBase64)
                if (geminiResult.isSuccess) {
                    geminiResult
                } else if (deepseekKey.isNotBlank()) {
                    // Gemini 失败且配置了 DeepSeek 时尝试备用引擎
                    val fallback = DeepSeekAiService.recognizeProcessImage(context, imageBase64)
                    if (fallback.isSuccess) fallback else geminiResult
                } else {
                    geminiResult
                }
            }
            AiEngine.DEEPSEEK -> {
                val deepseekResult = DeepSeekAiService.recognizeProcessImage(context, imageBase64)
                if (deepseekResult.isSuccess) {
                    deepseekResult
                } else if (geminiKey.isNotBlank()) {
                    // DeepSeek 失败（如官方接口不支持多模态图片）且配置了 Gemini 时，自动无缝切换到 Gemini 视觉引擎
                    val geminiFallback = GeminiAiService.recognizeProcessImage(context, imageBase64)
                    if (geminiFallback.isSuccess) geminiFallback else deepseekResult
                } else {
                    deepseekResult
                }
            }
        }
    }

    /**
     * 根据用户最新标准：
     * 只要是通过 AI 识图识别的图片，新产品名称、型号统一更改为【物料编码的后四位纯数字】作为标准。
     * 例如：
     * HZZ009986271DJ -> 6271
     * HZZ010671644DJ -> 1644
     * HZZ010624710DJ -> 4710
     * 11780692DJ -> 0692
     * HZZ007001916DJ -> 1916
     * HZZ007001918DJ -> 1918
     * 11465054DJ -> 5054
     * 11465055DJ -> 5055
     * HZZ0091845790J -> 5790
     * HZZ0091845800J -> 5800
     * 13624156DJ -> 4156
     * 
     * 若物料编码无至少4位数字，则尝试从型号或全称中提取4位数字；若仍无则兜底回退。
     */
    fun deriveStandardModelName(
        rawModel: String,
        fullProductName: String,
        materialCode: String
    ): String {
        val trimmedCode = materialCode.trim()
        val trimmedModel = rawModel.trim()
        val trimmedFull = fullProductName.trim()

        // 1. 核心标准：只要存在物料编码，优先提取物料编码中的纯数字后4位
        val codeDigits = trimmedCode.filter { it.isDigit() }
        if (codeDigits.length >= 4) {
            return codeDigits.takeLast(4)
        }

        // 2. 次选：若原始 model 已经是 4 位纯数字（例如 AI 直接提炼了后四位）
        if (trimmedModel.matches(Regex("""^\d{4}$"""))) {
            return trimmedModel
        }

        // 3. 次选：从零件全称或图号中寻找连续物料编码（如 11780692DJ 或 HZZ...）
        val codeInFullMatch = Regex("""(?:HZZ\d+|[0-9]{7,10}[A-Z]*)""").find(trimmedFull)
        if (codeInFullMatch != null) {
            val digits = codeInFullMatch.value.filter { it.isDigit() }
            if (digits.length >= 4) {
                return digits.takeLast(4)
            }
        }

        // 4. 次选：从零件全称或图号提取后4位数字
        val fullDigits = trimmedFull.filter { it.isDigit() }
        if (fullDigits.length >= 4) {
            return fullDigits.takeLast(4)
        }

        // 5. 兜底：若原始 model 不为空
        if (trimmedModel.isNotBlank() && trimmedModel !in listOf("新规格", "外圈锻坯", "内圈锻坯", "外圈", "内圈")) {
            return trimmedModel
        }

        return "新规格"
    }
}
