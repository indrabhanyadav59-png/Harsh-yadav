package com.example.game.world

import com.example.game.engine.BuildingStyle
import com.example.game.engine.WorldBuilding
import com.example.game.engine.WorldTree

data class ToriiMonumentProp(
    val x: Float,
    val z: Float,
    val rotY: Float,
    val scale: Float = 1f
)

object VillageGenerator {

    fun generate(roads: List<RoadSegment>): Triple<List<WorldBuilding>, List<WorldTree>, List<ToriiMonumentProp>> {
        val buildings = mutableListOf<WorldBuilding>()
        val trees = mutableListOf<WorldTree>()
        val gates = mutableListOf<ToriiMonumentProp>()
        val dVillage = WorldDistrictId.HIDDEN_VILLAGE.displayName

        val candidates = listOf(
            WorldBuilding("hv_academy", "Shinobi Training Academy", dVillage, -380f, -470f, 32f, 24f, 15f, BuildingStyle.ACADEMY, 0xFFE2E8F0.toInt(), 0xFF1E3A8A.toInt(), 0xFF00E5FF.toInt()),
            WorldBuilding("hv_monument", "Founder Spirit Monument", dVillage, -380f, -395f, 12f, 12f, 24f, BuildingStyle.JAPANESE_SHRINE, 0xFF94A3B8.toInt(), 0xFFDC2626.toInt(), 0xFFFFD166.toInt()),
            WorldBuilding("hv_dojo", "Kensei Sparring Dojo", dVillage, -480f, -410f, 24f, 18f, 10f, BuildingStyle.JAPANESE_SHRINE, 0xFFFDF4E7.toInt(), 0xFF7F1D1D.toInt(), 0xFFF59E0B.toInt()),
            WorldBuilding("hv_house_1", "Elder Konoha Teahouse", dVillage, -305f, -305f, 16f, 14f, 8.5f, BuildingStyle.PLAYER_VILLA, 0xFFF5EFE6.toInt(), 0xFF334155.toInt(), 0xFFF97316.toInt(), enterable = true),
            WorldBuilding("hv_house_2", "Lantern Forge Workshop", dVillage, -440f, -305f, 16f, 14f, 8.5f, BuildingStyle.PLAYER_VILLA, 0xFFE2E8F0.toInt(), 0xFF7F1D1D.toInt(), 0xFFFFD166.toInt()),
            WorldBuilding("hv_house_3", "Scroll Archive Hall", dVillage, -315f, -415f, 18f, 14f, 10f, BuildingStyle.JAPANESE_SHRINE, 0xFFFDF4E7.toInt(), 0xFF1E293B.toInt(), 0xFF38BDF8.toInt()),
            WorldBuilding("hv_watchtower", "Village Bamboo Watchtower", dVillage, -530f, -340f, 10f, 10f, 19f, BuildingStyle.JAPANESE_SHRINE, 0xFF78350F.toInt(), 0xFF991B1B.toInt(), 0xFFFACC15.toInt())
        )

        for (b in candidates) {
            BuildingGenerator.safePlaceBuilding(roads, b)?.let { buildings += it }
        }

        // Ceremonial Gates
        gates += ToriiMonumentProp(-245f, -320f, 38f, 1.15f)
        gates += ToriiMonumentProp(-380f, -432f, 0f, 0.95f)

        // Village Sakura & Pine grove
        val treeCoords = listOf(
            -275f to -375f, -325f to -370f, -415f to -315f, -505f to -305f,
            -440f to -470f, -320f to -495f, -240f to -390f, -525f to -440f
        )
        for ((idx, pt) in treeCoords.withIndex()) {
            if (!RoadGenerator.isPointNearRoad(roads, pt.first, pt.second, 4f)) {
                trees += WorldTree(pt.first, pt.second, 7.2f, isSakura = idx % 2 == 0)
            }
        }

        return Triple(buildings, trees, gates)
    }
}
