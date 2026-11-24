package com.example.game.world

import com.example.game.engine.BuildingStyle
import com.example.game.engine.WorldBuilding
import com.example.game.engine.WorldTree

data class RiverNode(
    val x: Float,
    val z: Float,
    val width: Float
)

object ForestGenerator {

    val riverNodes = listOf(
        RiverNode(530f, -590f, 28f),
        RiverNode(555f, -490f, 30f),
        RiverNode(560f, -390f, 30f),
        RiverNode(560f, -300f, 30f),
        RiverNode(590f, -210f, 32f),
        RiverNode(640f, -135f, 28f)
    )

    fun isInRiver(x: Float, z: Float): Boolean {
        if (x < 480f || x > 700f || z > -110f || z < -620f) return false
        for (i in 0 until riverNodes.lastIndex) {
            val a = riverNodes[i]
            val b = riverNodes[i + 1]
            val vx = b.x - a.x
            val vz = b.z - a.z
            val lenSq = vx * vx + vz * vz
            if (lenSq < 1f) continue
            val t = (((x - a.x) * vx + (z - a.z) * vz) / lenSq).coerceIn(0f, 1f)
            val px = a.x + vx * t
            val pz = a.z + vz * t
            val distSq = (x - px) * (x - px) + (z - pz) * (z - pz)
            val halfW = (a.width + b.width) * 0.25f
            if (distSq <= halfW * halfW) return true
        }
        return false
    }

    fun generate(roads: List<RoadSegment>): Triple<List<WorldBuilding>, List<WorldTree>, List<BoulderProp>> {
        val buildings = mutableListOf<WorldBuilding>()
        val trees = mutableListOf<WorldTree>()
        val rocks = mutableListOf<BoulderProp>()
        val dForest = WorldDistrictId.FOREST_REGION.displayName

        val candidates = listOf(
            WorldBuilding("for_ranger_outpost", "Emerald Forest Ranger Lodge", dForest, 760f, -470f, 18f, 14f, 9f, BuildingStyle.PLAYER_VILLA, 0xFFF5EFE6.toInt(), 0xFF14532D.toInt(), 0xFF22C55E.toInt(), enterable = true),
            WorldBuilding("for_river_shrine", "Mossy River Shrine", dForest, 430f, -430f, 14f, 12f, 9f, BuildingStyle.JAPANESE_SHRINE, 0xFFE2E8F0.toInt(), 0xFF166534.toInt(), 0xFFFFD166.toInt())
        )
        for (b in candidates) {
            BuildingGenerator.safePlaceBuilding(roads, b)?.let { buildings += it }
        }

        // Dense forest grid across x in [310..920], z in [-550..-140]
        var idx = 0
        for (gx in 320..900 step 52) {
            for (gz in -540..-150 step 50) {
                idx++
                val jx = gx + ((idx * 17) % 21) - 10f
                val jz = gz + ((idx * 23) % 19) - 9f
                if (isInRiver(jx, jz)) continue
                if (RoadGenerator.isPointNearRoad(roads, jx, jz, 6f)) continue
                val tooCloseToBldg = buildings.any { kotlin.math.abs(it.x - jx) < 18f && kotlin.math.abs(it.z - jz) < 18f }
                if (tooCloseToBldg) continue

                if (idx % 7 == 0) {
                    rocks += BoulderProp(jx, jz, 3.4f + (idx % 3) * 0.8f)
                } else {
                    trees += WorldTree(
                        x = jx,
                        z = jz,
                        height = 8.5f + (idx % 4) * 1.4f,
                        isSakura = idx % 9 == 0,
                        isPalm = false
                    )
                }
            }
        }

        return Triple(buildings, trees, rocks)
    }
}
