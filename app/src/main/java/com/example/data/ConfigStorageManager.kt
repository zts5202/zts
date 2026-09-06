package com.example.data

import android.content.Context
import com.example.model.DimensionTolerance
import com.example.model.RingConfig
import org.json.JSONArray
import org.json.JSONObject

/**
 * 参数持久化存储管理器：支持用户修改孔位、叠放个数、内外径与高度尺寸并保存于设备
 */
object ConfigStorageManager {
    private const val PREFS_NAME = "sany_hanger_configs_v1"
    private const val KEY_CUSTOM_CONFIGS = "custom_configs_json"

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

    /**
     * 批量持久化保存多个规格型号（常用于从工序汇总表/批量试验卡一次性入库）
     */
    fun saveBatchConfigs(context: Context, updatedList: List<RingConfig>) {
        if (updatedList.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val map = loadCustomConfigsMap(prefs)
        for (cfg in updatedList) {
            map[cfg.model] = cfg.copy(isCustomized = true)
        }
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
