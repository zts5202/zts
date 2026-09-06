package com.example.model

/**
 * 智能实测尺寸检索命中结果
 */
data class MatchHighlight(
    val matched: Boolean,
    val isDimensionMatch: Boolean = false,
    val isMultiMatch: Boolean = false,
    val matchedType: String = "", // "OD", "ID", "H", "MULTI", "TEXT", "MODEL"
    val badgeTitle: String = "",
    val badgeDetail: String = ""
)
