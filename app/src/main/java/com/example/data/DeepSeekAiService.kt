package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.model.HangerRuleEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object DeepSeekAiService {
    private const val PREFS_NAME = "deepseek_api_settings"
    private const val KEY_API_KEY = "deepseek_api_key"
    private const val KEY_BASE_URL = "deepseek_base_url"
    private const val KEY_MODEL_NAME = "deepseek_model_name"

    const val DEFAULT_ENDPOINT = "https://api.deepseek.com/chat/completions"
    const val DEFAULT_MODEL = "deepseek-v4-flash-vision-exp"
    const val BUILTIN_KEY = "sk-6ddfcc708e114cc6a02ce727b2b8e5c7"

    val POPULAR_MODELS = listOf(
        "deepseek-v4-flash-vision-exp",
        "deepseek-v4-flash",
        "deepseek-v4-pro"
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun normalizeKey(key: String): String {
        val trimmed = key.trim()
        return if (trimmed.startsWith("SK-", ignoreCase = true)) {
            "sk-" + trimmed.substring(3)
        } else {
            trimmed
        }
    }

    fun getApiKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_API_KEY, "")?.trim() ?: ""
        return if (stored.isNotBlank()) normalizeKey(stored) else BUILTIN_KEY
    }

    fun saveApiKey(context: Context, key: String) {
        val normalized = normalizeKey(key)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_API_KEY, normalized).apply()
    }

    fun getEndpoint(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_BASE_URL, "")?.trim() ?: ""
        return if (stored.isNotBlank()) stored else DEFAULT_ENDPOINT
    }

    fun saveEndpoint(context: Context, endpoint: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BASE_URL, endpoint.trim()).apply()
    }

    fun getModelName(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_MODEL_NAME, "")?.trim() ?: ""
        // 若之前保存过无效的 deepseek-v4-vision 或 deepseek-chat，自动迁移到官方支持的视觉模型 deepseek-v4-flash-vision-exp
        if (stored.isBlank() || stored == "deepseek-v4-vision" || stored == "deepseek-chat") {
            return DEFAULT_MODEL
        }
        return stored
    }

    fun saveModelName(context: Context, model: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_MODEL_NAME, model.trim()).apply()
    }

    /**
     * 测试 DeepSeek 连通性
     */
    suspend fun testApiKey(
        context: Context,
        testKey: String,
        endpoint: String = getEndpoint(context),
        modelName: String = getModelName(context)
    ): Result<String> = withContext(Dispatchers.IO) {
        val key = testKey.trim().ifBlank { getApiKey(context) }
        if (key.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("请输入或粘贴有效的 DeepSeek API Key"))
        }

        try {
            val pingJson = JSONObject().apply {
                put("model", modelName)
                val messages = JSONArray().apply {
                    val userMsg = JSONObject().apply {
                        put("role", "user")
                        put("content", "ping")
                    }
                    put(userMsg)
                }
                put("messages", messages)
                put("max_tokens", 5)
            }

            val request = Request.Builder()
                .url(endpoint.ifBlank { DEFAULT_ENDPOINT })
                .addHeader("Authorization", "Bearer $key")
                .addHeader("Content-Type", "application/json")
                .post(pingJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                Result.success("✅ DeepSeek 视觉接口响应正常！模型: $modelName")
            } else if (response.code == 401) {
                Result.failure(Exception("DeepSeek Key 鉴权失败 (HTTP 401): 请检查密钥是否正确输入"))
            } else if (response.code == 404) {
                Result.failure(Exception("未找到服务或模型 (HTTP 404): 请检查模型名 [$modelName] 或接口地址"))
            } else {
                Result.failure(Exception("DeepSeek 测试失败 (HTTP ${response.code}): $body"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("网络连接异常: ${e.localizedMessage}"))
        }
    }

    /**
     * 调用 DeepSeek 视觉模型识别工艺试验通知单/图纸
     */
    suspend fun recognizeProcessImage(
        context: Context,
        imageBase64: String
    ): Result<List<ExtractedProcessInfo>> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(context)
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("未配置 DeepSeek API Key。请在设置中粘贴您的 DeepSeek Key。")
            )
        }

        val endpoint = getEndpoint(context)
        val modelName = getModelName(context)

        val prompt = """
            你是一位重型工程机械精密锻造与热处理（三一/索特车间 OP40 辗环工序）工艺专家。
            用户上传了一张工艺卡、工艺试验通知单、图纸或多产品技术参数汇总表格（图片可能为横向宽表拍摄、逆时针旋转或竖向拍摄，请全方位自适应阅读）。
            
            【至关重要要求】：
            如果图片中是一个包含多行零件的汇总表格（例如包含 序号1 到 序号8 等多行记录）：
            你必须逐行完整提取表格中的【每一个零件序号行】，依次填入 items 列表中！绝对不能只返回第 1 行或提前截断，必须将表中的全部产品完整输出！
            
            请以严格的标准 JSON 格式输出：
            {
              "items": [
                {
                  "model": "核心型号/产品名称。统一必须以【物料编码的后4位纯数字】作为标准命名！例如：物料编码为 HZZ009986271DJ 则命名为 6271；物料编码为 HZZ010671644DJ 则命名为 1644；物料编码为 HZZ010624710DJ 则命名为 4710；物料编码为 11780692DJ 则命名为 0692；物料编码为 HZZ007001916DJ 则命名为 1916；物料编码为 11465054DJ 则命名为 5054；物料编码为 13624156DJ 则命名为 4156；若物料编码为 HZZ0091845790J 则命名为 5790 等。只有实在无物料编码时才填写零件简称。",
                  "material": "材质代号：42CrMo 填 42，50Mn 填 50，如未标明则填 42",
                  "odNominal": 1448,
                  "odTolerance": 4,
                  "idNominal": 1239,
                  "idTolerance": 6,
                  "heightNominal": 173,
                  "heightTolerance": 2,
                  "weight": "下料重量字符串(KG)，如 791 kg 或 986 kg",
                  "materialCode": "物料编码，如 HZZ009986271DJ 或 11780692DJ",
                  "fullProductName": "图号或零件全称，如 外圈锻坯SSF1405-50CWHV-2M 或 SSM1120.32BHH-1M",
                  "machinedSize": "粗车尺寸，如 Φ1550 × Φ1405 × 108.5",
                  "hotSize": "辗环热尺寸，如 Φ1577 × Φ1409 × 248",
                  "billetInfo": "下料或制坯参数，如 料坯Φ390×844 / 制坯外径691",
                  "notes": "工艺要求，如 试验通知/合辗2件"
                }
              ]
            }
            
            识别规则注意：
            1. 尺寸提取优先级最高：优先读取表头为【锻件尺寸(冷态)】或带“冷态”列的 外径Φ、内径Φ、高度；
            2. 若外径公差未注明默认 4，内径公差默认 6，高度公差默认 2；
            3. 下料参数中的重量即为单件下料重量（如 791、986、788、407、354、529、460、644 等）；
            4. 必须将表格中出现的所有零件序号行全部加入 items；严格只输出纯 JSON，不要包含任何 markdown 或解释文字。
        """.trimIndent()

        try {
            val contentArray = JSONArray().apply {
                // 文本 prompt
                put(JSONObject().apply {
                    put("type", "text")
                    put("text", prompt)
                })
                // 图片 url (Base64 Data URI)
                put(JSONObject().apply {
                    put("type", "image_url")
                    val imgObj = JSONObject().apply {
                        put("url", "data:image/jpeg;base64,$imageBase64")
                    }
                    put("image_url", imgObj)
                })
            }

            val requestJson = JSONObject().apply {
                put("model", modelName)
                val messages = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", contentArray)
                    })
                }
                put("messages", messages)
                put("temperature", 0.1)
                put("max_tokens", 8192)
            }

            val request = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = if (response.code == 400) {
                    "DeepSeek 官方 API (api.deepseek.com) 目前仅支持文本，尚未开放公网图片视觉接口 (HTTP 400)。建议在设置中切换使用「Google Gemini」视觉模型进行识图。"
                } else {
                    "DeepSeek 识别请求失败 (HTTP ${response.code}): $responseBody"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val rootJson = JSONObject(responseBody)
            val choices = rootJson.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                return@withContext Result.failure(Exception("DeepSeek 未返回识别内容"))
            }

            val firstChoice = choices.getJSONObject(0)
            val messageObj = firstChoice.optJSONObject("message")
            val rawContent = messageObj?.optString("content") ?: ""

            val jsonList = extractJsonObjects(rawContent)
            if (jsonList.isEmpty()) {
                val previewTip = if (rawContent.length > 60) rawContent.take(60) + "..." else rawContent
                val errorMsg = "DeepSeek 官方 API 仅支持文本，暂不具备图片视觉识别能力（模型回复: \"$previewTip\"）。请点击下方切换为【Google Gemini】视觉引擎！"
                return@withContext Result.failure(Exception(errorMsg))
            }

            val results = jsonList.map { item ->
                val rawModel = item.optString("model", "")
                val materialCode = item.optString("materialCode", "")
                val fullProductName = item.optString("fullProductName", "")
                val model = AiVisionManager.deriveStandardModelName(rawModel, fullProductName, materialCode)
                val material = item.optString("material", "42")
                val odNom = item.optInt("odNominal", 0)
                val odTol = item.optInt("odTolerance", 4)
                val idNom = item.optInt("idNominal", 0)
                val idTol = item.optInt("idTolerance", 6)
                val hNom = item.optInt("heightNominal", 0)
                val hTol = item.optInt("heightTolerance", 2)
                val weight = item.optString("weight", "")
                val machinedSize = item.optString("machinedSize", "")
                val hotSize = item.optString("hotSize", "")
                val billetInfo = item.optString("billetInfo", "")
                val notes = item.optString("notes", "-")

                val weightKg = HangerRuleEngine.extractWeightNumber(weight)
                val recommendation = HangerRuleEngine.recommend(
                    idNominal = if (idNom > 0) idNom else null,
                    weightKg = weightKg,
                    heightNominal = if (hNom > 0) hNom else null
                )

                ExtractedProcessInfo(
                    model = model,
                    material = material,
                    odNominal = odNom,
                    odTolerance = odTol,
                    idNominal = idNom,
                    idTolerance = idTol,
                    heightNominal = hNom,
                    heightTolerance = hTol,
                    weight = weight,
                    materialCode = materialCode,
                    fullProductName = fullProductName,
                    machinedSize = machinedSize,
                    hotSize = hotSize,
                    billetInfo = billetInfo,
                    notes = notes,
                    recommendation = recommendation
                )
            }

            if (results.isEmpty()) {
                Result.failure(Exception("未能从图片中解析出有效零件参数"))
            } else {
                Result.success(results)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractJsonObjects(rawText: String): List<JSONObject> {
        val clean = rawText
            .replace("```json", "")
            .replace("```", "")
            .trim()

        try {
            val root = JSONObject(clean)
            val items = root.optJSONArray("items")
            if (items != null && items.length() > 0) {
                val list = mutableListOf<JSONObject>()
                for (i in 0 until items.length()) {
                    val item = items.optJSONObject(i)
                    if (item != null) list.add(item)
                }
                if (list.isNotEmpty()) return list
            } else if (root.has("model") || root.has("odNominal") || root.has("materialCode")) {
                return listOf(root)
            }
        } catch (_: Exception) {}

        try {
            val array = JSONArray(clean)
            val list = mutableListOf<JSONObject>()
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i)
                if (item != null) list.add(item)
            }
            if (list.isNotEmpty()) return list
        } catch (_: Exception) {}

        val list = mutableListOf<JSONObject>()
        val startBracket = clean.indexOf('[')
        val searchScope = if (startBracket != -1) clean.substring(startBracket) else clean

        var depth = 0
        var itemStart = -1
        for (i in searchScope.indices) {
            val ch = searchScope[i]
            if (ch == '{') {
                if (depth == 0) itemStart = i
                depth++
            } else if (ch == '}') {
                depth--
                if (depth == 0 && itemStart != -1) {
                    val objStr = searchScope.substring(itemStart, i + 1)
                    try {
                        val obj = JSONObject(objStr)
                        if (obj.has("model") || obj.has("materialCode") || obj.has("odNominal") || obj.has("fullProductName")) {
                            list.add(obj)
                        }
                    } catch (_: Exception) {}
                    itemStart = -1
                }
            }
        }

        return list
    }
}
