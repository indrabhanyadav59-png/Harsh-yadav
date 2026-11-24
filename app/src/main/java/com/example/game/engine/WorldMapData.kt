package com.example.game.engine

import com.example.game.world.ChunkManager
import com.example.game.world.CityLandmarksAndModules
import com.example.game.world.CityZoneBounds
import com.example.game.world.InteriorType
import com.example.game.world.RoadSegment
import com.example.game.world.WorldGenerator
import com.example.game.world.WorldLocations

enum class BuildingStyle {
    JAPANESE_MODERN,
    JAPANESE_SHRINE,
    INDIAN_PALACE,
    POLICE_STATION,
    HOSPITAL,
    ACADEMY,
    SHOPPING_MALL,
    DEALERSHIP,
    GARAGE,
    PLAYER_VILLA,
    AIRPORT_HANGAR,
    LIGHTHOUSE
}

data class WorldBuilding(
    val id: String,
    val name: String,
    val district: String,
    val x: Float,
    val z: Float,
    val width: Float,
    val depth: Float,
    val height: Float,
    val style: BuildingStyle,
    val wallColor: Int,
    val roofColor: Int,
    val accentColor: Int,
    val enterable: Boolean = false,
    val interiorType: InteriorType = InteriorType.PLAYER_VILLA
) {
    fun toAabb() = Aabb2D(
        minX = x - width * 0.5f,
        maxX = x + width * 0.5f,
        minZ = z - depth * 0.5f,
        maxZ = z + depth * 0.5f,
        height = height,
        label = name
    )
}

data class WorldTree(
    val x: Float,
    val z: Float,
    val height: Float,
    val isSakura: Boolean = true,
    val isPalm: Boolean = false
)

data class ParkedWorldVehicle(
    val id: String,
    val modelName: String,
    val category: String,
    val x: Float,
    val z: Float,
    val rotY: Float,
    val colorInt: Int,
    val isLocked: Boolean,
    val ownerName: String
)

/**
 * Facade over the 2000 x 2000 procedural WorldGenerator & ChunkManager.
 * Provides spatial chunk-accelerated collision resolution, terrain heights, and district lookup.
 */
object WorldMapData {

    const val WORLD_BOUND = WorldLocations.PLAYABLE_BOUND

    val buildings: List<WorldBuilding> get() = WorldGenerator.buildings
    val trees: List<WorldTree> get() = WorldGenerator.trees
    val parkedVehicles: List<ParkedWorldVehicle> get() = WorldGenerator.parkedVehicles
    val roads: List<RoadSegment> get() = WorldGenerator.roads
    val chunkManager: ChunkManager get() = WorldGenerator.chunkManager

    private val scratchAabbs = ArrayList<Aabb2D>(32)

    fun resolveDistrict(x: Float, z: Float): String = WorldLocations.resolveDistrictName(x, z)

    fun resolveCityZone(x: Float, z: Float): CityZoneBounds? = CityLandmarksAndModules.resolveCityZone(x, z)

    fun terrainHeightAt(x: Float, z: Float): Float = WorldGenerator.terrainHeightAt(x, z)

    fun isDeepWater(x: Float, z: Float): Boolean = WorldGenerator.isDeepWater(x, z)

    fun resolveCollision(x: Float, z: Float, radius: Float): Pair<Float, Float> {
        var nx = x.coerceIn(-WORLD_BOUND, WORLD_BOUND)
        var nz = z.coerceIn(-WORLD_BOUND, WORLD_BOUND)

        synchronized(scratchAabbs) {
            chunkManager.collectNearbyAabbs(nx, nz, radius, scratchAabbs)
            for (i in 0 until scratchAabbs.size) {
                val box = scratchAabbs[i]
                if (box.containsCircle(nx, nz, radius)) {
                    val dxLeft = (nx - (box.minX - radius))
                    val dxRight = ((box.maxX + radius) - nx)
                    val dzTop = (nz - (box.minZ - radius))
                    val dzBot = ((box.maxZ + radius) - nz)
                    val minPen = minOf(dxLeft, dxRight, dzTop, dzBot)
                    when (minPen) {
                        dxLeft -> nx = box.minX - radius
                        dxRight -> nx = box.maxX + radius
                        dzTop -> nz = box.minZ - radius
                        else -> nz = box.maxZ + radius
                    }
                }
            }
        }
        return nx to nz
    }
}
