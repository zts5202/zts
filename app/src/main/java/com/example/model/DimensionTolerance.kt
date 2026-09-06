package com.example.model

/**
 * 尺寸与公差模型：对应三一重工 / 索特传动 OP40 辗环工序卡尺寸定义
 */
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
