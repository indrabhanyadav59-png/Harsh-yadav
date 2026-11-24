package com.example.game.world

import com.example.game.engine.BuildingStyle
import com.example.game.engine.WorldBuilding

data class DistrictBillboard(
    val x: Float,
    val y: Float,
    val z: Float,
    val width: Float,
    val height: Float,
    val rotY: Float,
    val accentColor: Int
)

/**
 * Architectural factory producing detailed buildings (with collision margins verified against roads).
 */
object BuildingGenerator {

    fun safePlaceBuilding(
        roads: List<RoadSegment>,
        building: WorldBuilding
    ): WorldBuilding? {
        val halfDiag = maxOf(building.width, building.depth) * 0.45f
        return if (RoadGenerator.isPointNearRoad(roads, building.x, building.z, halfDiag + 1.5f)) {
            null
        } else {
            building
        }
    }

    fun makeModernTower(
        id: String,
        name: String,
        district: String,
        x: Float,
        z: Float,
        w: Float,
        d: Float,
        h: Float,
        wallColor: Int,
        roofColor: Int,
        accentColor: Int
    ) = WorldBuilding(
        id = id, name = name, district = district,
        x = x, z = z, width = w, depth = d, height = h,
        style = BuildingStyle.JAPANESE_MODERN,
        wallColor = wallColor, roofColor = roofColor, accentColor = accentColor
    )

    fun makeVilla(
        id: String,
        name: String,
        district: String,
        x: Float,
        z: Float,
        wallColor: Int = 0xFFFDF4E7.toInt(),
        roofColor: Int = 0xFF334155.toInt(),
        accentColor: Int = 0xFF22C55E.toInt(),
        enterable: Boolean = true
    ) = WorldBuilding(
        id = id, name = name, district = district,
        x = x, z = z, width = 18f, depth = 14f, height = 10f,
        style = BuildingStyle.PLAYER_VILLA,
        wallColor = wallColor, roofColor = roofColor, accentColor = accentColor,
        enterable = enterable
    )
}
