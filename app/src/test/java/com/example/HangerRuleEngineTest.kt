package com.example

import com.example.model.HangerRuleEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class HangerRuleEngineTest {

    @Test
    fun testLargeHangerExtraWideRule() {
        // 485外: ID 1520 >= 1490 -> 大吊具 333孔, 单重 942kg -> 3个
        val rec = HangerRuleEngine.recommend(
            idNominal = 1520,
            weightKg = 942,
            heightNominal = 142
        )
        assertEquals("大", rec.hanger)
        assertEquals("333", rec.holePosition)
        assertEquals("3个", rec.quantity)
    }

    @Test
    fun testLargeHangerStandardRule() {
        // 485内: ID 1303 -> 大吊具 222孔, 单重 1307kg (超重) -> 2个
        val rec = HangerRuleEngine.recommend(
            idNominal = 1303,
            weightKg = 1307,
            heightNominal = 188
        )
        assertEquals("大", rec.hanger)
        assertEquals("222", rec.holePosition)
        assertEquals("2个", rec.quantity)
    }

    @Test
    fun testSmallHanger666Hole() {
        // 230外: ID 1208 -> 小吊具 666孔, 单重 747kg -> 4个
        val rec = HangerRuleEngine.recommend(
            idNominal = 1208,
            weightKg = 747,
            heightNominal = 168
        )
        assertEquals("小", rec.hanger)
        assertEquals("666", rec.holePosition)
        assertEquals("4个", rec.quantity)
    }

    @Test
    fun testSmallHanger555Hole() {
        // 230内: ID 1059 -> 小吊具 555孔, 单重 660kg -> 4个
        val rec = HangerRuleEngine.recommend(
            idNominal = 1059,
            weightKg = 660,
            heightNominal = 188
        )
        assertEquals("小", rec.hanger)
        assertEquals("555", rec.holePosition)
        assertEquals("4个", rec.quantity)
    }

    @Test
    fun testSmallHanger444Hole() {
        // 5519: ID 979 -> 小吊具 444孔, 单重 469kg -> 6个
        val rec = HangerRuleEngine.recommend(
            idNominal = 979,
            weightKg = 469,
            heightNominal = 148
        )
        assertEquals("小", rec.hanger)
        assertEquals("444", rec.holePosition)
        assertEquals("6个", rec.quantity)
    }

    @Test
    fun testSmallHanger333Hole() {
        // 95外: ID 790 -> 小吊具 333孔, 单重 270kg -> 7个
        val rec = HangerRuleEngine.recommend(
            idNominal = 790,
            weightKg = 270,
            heightNominal = 126
        )
        assertEquals("小", rec.hanger)
        assertEquals("333", rec.holePosition)
        assertEquals("7个", rec.quantity)
    }

    @Test
    fun testSmallHanger223Hole() {
        // 55外: ID 755 -> 小吊具 223孔, 单重 164kg -> 7个
        val rec = HangerRuleEngine.recommend(
            idNominal = 755,
            weightKg = 164,
            heightNominal = 95
        )
        assertEquals("小", rec.hanger)
        assertEquals("223", rec.holePosition)
        assertEquals("7个", rec.quantity)
    }

    @Test
    fun testNewNotice1250Outer() {
        // 用户最新试验通知: 外圈锻坯 SSM1250.35AH11-2Z
        // 冷态内径 ID 1239 (属于 1161~1280mm 小吊具极限尺寸群) -> 小吊具 666孔
        // 下料重量 642kg -> 建议叠放 4个 (单柱总重 2.57吨，符合 2.5~3.0吨安全载荷)
        val rec = HangerRuleEngine.recommend(
            idNominal = 1239,
            weightKg = 642,
            heightNominal = 173
        )
        assertEquals("小", rec.hanger)
        assertEquals("666", rec.holePosition)
        assertEquals("4个", rec.quantity)
    }

    @Test
    fun testNewNotice1250Inner() {
        // 用户最新试验通知: 内圈锻坯 SSM1250.35AH11-1Z
        // 冷态内径 ID 1099 (属于 1011~1160mm 范围) -> 小吊具 555孔
        // 下料重量 433kg -> 建议叠放 6个 (单柱总重 2.60吨，符合 2.5~3.0吨黄金载荷)
        val rec = HangerRuleEngine.recommend(
            idNominal = 1099,
            weightKg = 433,
            heightNominal = 173
        )
        assertEquals("小", rec.hanger)
        assertEquals("555", rec.holePosition)
        assertEquals("6个", rec.quantity)
    }
}
