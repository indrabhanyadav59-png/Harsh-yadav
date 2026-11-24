package com.example.game.world

import com.example.game.engine.BuildingStyle
import com.example.game.engine.WorldBuilding
import com.example.game.engine.WorldTree

data class MountainPeakProp(
    val x: Float,
    val y: Float,
    val z: Float,
    val width: Float,
    val height: Float,
    val depth: Float,
    val colorInt: Int
)

data class BoulderProp(
    val x: Float,
    val z: Float,
    val scale: Float
)

object MountainGenerator {

    fun generate(roads: List<RoadSegment>): Pair<List<WorldBuilding>, Triple<List<MountainPeakProp>, List<BoulderProp>, List<WorldTree>>> {
        val buildings = mutableListOf<WorldBuilding>()
        val peaks = mutableListOf<MountainPeakProp>()
        val boulders = mutableListOf<BoulderProp>()
        val trees = mutableListOf<WorldTree>()
        val dMtn = WorldDistrictId.MOUNTAIN_REGION.displayName

        // Scenic Mountain Viewpoint Lodge & Hidden Summit Shrine
        val candidates = listOf(
            WorldBuilding("mtn_viewpoint_lodge", "Dragon Ridge Overlook", dMtn, 28f, -720f, 16f, 12f, 8f, BuildingStyle.JAPANESE_SHRINE, 0xFFF5EFE6.toInt(), 0xFF1E293B.toInt(), 0xFF38BDF8.toInt()),
            WorldBuilding("mtn_summit_shrine", "Summit Cloud Shrine", dMtn, 240f, -860f, 18f, 14f, 11f, BuildingStyle.JAPANESE_SHRINE, 0xFFFDF4E7.toInt(), 0xFFB91C1C.toInt(), 0xFFFFD166.toInt())
        )
        for (b in candidates) {
            BuildingGenerator.safePlaceBuilding(roads, b)?.let { buildings += it }
        }

        // Towering Dragon Ridge Peaks framing the northern horizon (z in [-950..-680])
        peaks += MountainPeakProp(-360f, 44f, -860f, 220f, 98f, 180f, 0xFF334155.toInt())
        peaks += MountainPeakProp(-170f, 52f, -900f, 190f, 114f, 170f, 0xFF3F4E63.toInt())
        peaks += MountainPeakProp(0f, 56f, -920f, 210f, 122f, 160f, 0xFF475569.toInt())
        peaks += MountainPeakProp(190f, 50f, -895f, 185f, 108f, 165f, 0xFF3F4E63.toInt())
        peaks += MountainPeakProp(390f, 46f, -865f, 210f, 100f, 175f, 0xFF334155.toInt())
        peaks += MountainPeakProp(-510f, 36f, -790f, 150f, 82f, 140f, 0xFF475569.toInt())
        peaks += MountainPeakProp(520f, 38f, -780f, 155f, 84f, 140f, 0xFF475569.toInt())

        // Rocky boulders along switchback turns
        val boulderCoords = listOf(
            -90f to -660f, 85f to -670f, -195f to -720f, 175f to -760f,
            -70f to -785f, 65f to -840f, -280f to -740f, 310f to -790f
        )
        for ((bx, bz) in boulderCoords) {
            if (!RoadGenerator.isPointNearRoad(roads, bx, bz, 5f)) {
                boulders += BoulderProp(bx, bz, 4.5f)
            }
        }

        // Alpine Pines
        for (i in -8..8) {
            val px = i * 54f + 12f
            val pz = -635f - (kotlin.math.abs(i) % 3) * 32f
            if (!RoadGenerator.isPointNearRoad(roads, px, pz, 5f)) {
                trees += WorldTree(px, pz, 10.5f, isSakura = false, isPalm = false)
            }
        }

        return buildings to Triple(peaks, boulders, trees)
    }
}
