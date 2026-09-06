package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.BuildConfig
import com.example.model.HangerRecommendation
import com.example.model.HangerRuleEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * AI 提取出的工艺卡原始信息实体
 */
data class ExtractedProcessInfo(
    val model: String = "",
    val material: String = "42",
    val odNominal: Int = 0,
    val odTolerance: Int = 4,
    val idNominal: Int = 0,
    val idTolerance: Int = 6,
    val heightNominal: Int = 0,
    val heightTolerance: Int = 2,
    val weight: String = "",
    val materialCode: String = "",
    val fullProductName: String = "",
    val machinedSize: String = "",
    val hotSize: String = "",
    val billetInfo: String = "",
    val notes: String = "",
    val recommendation: HangerRecommendation? = null
)

object GeminiAiService {
    private const val PREFS_NAME = "gemini_api_settings"
    private const val KEY_CUSTOM_API_KEY = "custom_gemini_key"
    
    // 候选视觉模型列表：兼顾主流最新与官方高配，遇高峰或限额自动轮换
    private val CANDIDATE_MODELS = listOf(
        "gemini-flash-latest",
        "gemini-2.5-flash",
        "gemini-3.5-flash",
        "gemini-3.1-pro-preview"
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    fun getApiKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedKey = prefs.getString(KEY_CUSTOM_API_KEY, "") ?: ""
        if (savedKey.isNotBlank()) return savedKey.trim()

        return try {
            val buildKey = BuildConfig.GEMINI_API_KEY
            if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") buildKey else ""
        } catch (_: Exception) {
            ""
        }
    }

    fun saveCustomApiKey(context: Context, key: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CUSTOM_API_KEY, key.trim()).apply()
    }

    /**
     * 快速测试指定的 API Key 是否可用
     */
    suspend fun testApiKey(context: Context, testKey: String): Result<String> = withContext(Dispatchers.IO) {
        val key = testKey.trim().ifBlank { getApiKey(context) }
        if (key.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("请输入或粘贴有效的 API Key"))
        }

        for (modelName in CANDIDATE_MODELS) {
            try {
                val pingJson = JSONObject().apply {
                    val contentsArray = JSONArray()
                    val contentObj = JSONObject()
                    val partsArray = JSONArray()
                    partsArray.put(JSONObject().put("text", "ping"))
                    contentObj.put("parts", partsArray)
                    contentsArray.put(contentObj)
                    put("contents", contentsArray)
                }

                val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$key"
                val request = Request.Builder()
                    .url(url)
                    .post(pingJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    return@withContext Result.success("✅ 连接成功！$modelName 响应正常")
                } else if (response.code == 503) {
                    // 当前模型高峰排队，继续测试下一个备选模型
                    continue
                } else if (response.code == 400) {
                    return@withContext Result.failure(Exception("API Key 无效 (HTTP 400): 请检查是否完整复制以 AIza 开头的密钥"))
                } else if (response.code == 403) {
                    return@withContext Result.failure(Exception("权限不足 (HTTP 403): 请检查该 Key 是否具有 Generative Language 权限"))
                } else {
                    return@withContext Result.failure(Exception("测试失败 (HTTP ${response.code}): $body"))
                }
            } catch (e: Exception) {
                // 尝试下一个模型
            }
        }

        Result.failure(Exception("Google 官方服务当前处于突发高峰排队(HTTP 503)，请稍候片刻重试"))
    }

    /**
     * 将从 Uri 或相册选择的图片缩放并转换为 Base64
     */
    fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            bitmapToBase64(originalBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun bitmapToBase64(bitmap: Bitmap): String {
        // 提高最大分辨率至 2560 并以 92% 高画质压缩，确保超宽横向大表格和微小文字清晰可见
        val maxDim = 2560
        val scale = if (bitmap.width > maxDim || bitmap.height > maxDim) {
            val ratio = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).toInt(),
                (bitmap.height * ratio).toInt(),
                true
            )
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        scale.compress(Bitmap.CompressFormat.JPEG, 92, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * 调用 Gemini 视觉模型进行工程图纸 / 工艺试验通知单图文识别
     * 自带多模型自动故障转移（当 3.5-flash 遇 503 高峰时自动切换备选模型）
     */
    suspend fun recognizeProcessImage(
        context: Context,
        imageBase64: String
    ): Result<List<ExtractedProcessInfo>> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(context)
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("未配置 Gemini API Key。请点击右上角设置配置 API Key 或直接粘贴。")
            )
        }

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
            4. 必须将表格中出现的所有零件序号行全部加入 items；不要使用 ```json 包裹，直接输出纯 JSON 字符串。
        """.trimIndent()

        val requestJson = JSONObject().apply {
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()

            // 文本提示
            partsArray.put(JSONObject().put("text", prompt))

            // 图片数据 (InlineData)
            val inlineDataObj = JSONObject().apply {
                put("mimeType", "image/jpeg")
                put("data", imageBase64)
            }
            partsArray.put(JSONObject().put("inlineData", inlineDataObj))

            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            put("contents", contentsArray)

            // 开启 JSON 格式化输出，指定 8192 输出 token 以完整容纳 8 款以上产品的大型汇总表
            val generationConfig = JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.1)
                put("maxOutputTokens", 8192)
            }
            put("generationConfig", generationConfig)
        }

        var lastErrorMsg = ""
        var encountered503 = false

        // 依次尝试候选模型，解决特定模型突发 503 高峰排队问题
        for (modelName in CANDIDATE_MODELS) {
            try {
                val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(endpoint)
                    .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    if (response.code == 503) {
                        encountered503 = true
                        lastErrorMsg = "Google 官方模型 $modelName 处于高峰排队 (HTTP 503)"
                        // 稍作停顿后尝试下一个备用模型
                        delay(600)
                        continue
                    } else if (response.code == 400) {
                        lastErrorMsg = "API Key 无效或格式错误 (HTTP 400)"
                        continue
                    } else if (response.code == 404) {
                        lastErrorMsg = "模型 $modelName 未就绪 (HTTP 404)"
                        continue
                    } else if (response.code == 429) {
                        lastErrorMsg = "请求频次受限 (HTTP 429)"
                        delay(800)
                        continue
                    } else {
                        lastErrorMsg = "HTTP ${response.code}: $responseBody"
                        continue
                    }
                }

                val rootJson = JSONObject(responseBody)
                val candidates = rootJson.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    lastErrorMsg = "Gemini 未返回识别结果"
                    continue
                }

                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val rawText = parts?.optJSONObject(0)?.optString("text") ?: ""

                val jsonList = extractJsonObjects(rawText)
                if (jsonList.isEmpty()) {
                    lastErrorMsg = "未能从响应中解析出零件数据"
                    continue
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

                if (results.isNotEmpty()) {
                    return@withContext Result.success(results)
                }
            } catch (e: Exception) {
                lastErrorMsg = e.message ?: "网络请求异常"
            }
        }

        val finalError = if (encountered503) {
            "Google 官方服务器当前处于全球高峰期排队中(HTTP 503)。请稍候重试。"
        } else {
            "AI 识别请求未成功: $lastErrorMsg"
        }

        Result.failure(Exception(finalError))
    }

    /**
     * 高容错 JSON 零件数据提取器：
     * 1. 优先作为标准 items 数组或 JSON 对象解析；
     * 2. 具备尾部截断修补与独立对象提取能力，即使大型表格因 token 限制发生末尾截断，
     *    也能完整提取出已生成的所有零件项，绝不导致整张图纸识别失败！
     */
    private fun extractJsonObjects(rawText: String): List<JSONObject> {
        val clean = rawText
            .replace("```json", "")
            .replace("```", "")
            .trim()

        // 1. 标准模式：尝试完整解析根 JSONObject
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

        // 2. 尝试直接作为 JSONArray 解析
        try {
            val array = JSONArray(clean)
            val list = mutableListOf<JSONObject>()
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i)
                if (item != null) list.add(item)
            }
            if (list.isNotEmpty()) return list
        } catch (_: Exception) {}

        // 3. 容错抽取：通过花括号配对提取每一个完整闭合的零件项
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
