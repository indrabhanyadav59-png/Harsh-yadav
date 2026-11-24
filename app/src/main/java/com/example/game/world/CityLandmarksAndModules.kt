package com.example.game.world

import kotlin.math.abs
import kotlin.math.hypot

/**
 * Modular Open-World Region Architecture for INDRA: ANIME LIFE RP.
 * Establishes the 2000 x 2000 3D coordinate-based layout map for the Indian Mega City
 * (Central Circular Roundabout, Commercial Shopping District, and Residential Blocks)
 * while keeping outer regions modular for incremental expansion.
 */
enum class WorldModuleStatus(val label: String) {
    ACTIVE_MODULE_01("Module 01: Indian Mega City"),
    EXPANSION_RESERVED("Reserved Expansion Module")
}

data class WorldModuleSpec(
    val district: WorldDistrictId,
    val status: WorldModuleStatus,
    val centerX: Float,
    val centerZ: Float,
    val radius: Float,
    val hasShopsAllowed: Boolean
)

/**
 * Primary structural layout zones defining the Indian Mega City 3D scale.
 */
enum class CityZoneId(val displayName: String, val accentHex: Long) {
    CENTRAL_ROUNDABOUT("Central Circular Roundabout", 0xFF00E5FF),
    COMMERCIAL_SHOPPING_DISTRICT("Commercial Shopping District", 0xFFF59E0B),
    RESIDENTIAL_BLOCKS("Residential Blocks", 0xFFA855F7),
    INNER_MIXED_AVENUE("Inner City Mixed Avenue", 0xFF38BDF8)
}

data class CityZoneBounds(
    val id: CityZoneId,
    val minX: Float,
    val maxX: Float,
    val minZ: Float,
    val maxZ: Float,
    val centerX: Float,
    val centerY: Float,
    val centerZ: Float,
    val radius: Float,
    val floorElevation: Float,
    val roadWidth: Float,
    val summary: String
) {
    fun contains(x: Float, z: Float): Boolean =
        if (id == CityZoneId.CENTRAL_ROUNDABOUT) {
            hypot(x - centerX, z - centerZ) <= radius
        } else {
            x in minX..maxX && z in minZ..maxZ
        }
}

/**
 * Architectural & functional role for buildings across the Indian Mega City zones.
 */
enum class InteriorType(val label: String, val shopTitle: String) {
    PLAYER_VILLA("Residence", "Home Sanctuary"),
    WEAPON_SHOP("Weapon Shop", "Armory & Weapons Counter"),
    CAR_SHOWROOM("Car Showroom", "Supercar & Cruiser Showroom"),
    BIKE_SHOWROOM("Bike Showroom", "Superbike & Moto Gallery"),
    CLOTHING_STORE("Clothing Store", "Anime & Indian Couture"),
    ABILITY_SHOP("Ability Dojo", "Indra Chakra & Skill Shop"),
    UPGRADE_STORE("Central Weapon Forge", "Central Weapon Upgrade Store"),
    RESTAURANT("Bistro & Cafe", "City Dining & Recovery"),
    SHOPPING_MALL("Cyber Mall", "Indra Mega Mall Directory"),
    SMALL_HOUSE("City House", "Apartment & Townhouse Interior")
}

data class CityBenchProp(
    val x: Float,
    val z: Float,
    val rotY: Float
)

data class CityRoadSignProp(
    val x: Float,
    val z: Float,
    val rotY: Float,
    val accentColor: Int
)

data class IndianArchProp(
    val x: Float,
    val z: Float,
    val rotY: Float,
    val spanWidth: Float = 14f,
    val height: Float = 9.5f,
    val stoneColor: Int = 0xFFE6C79C.toInt(),
    val accentColor: Int = 0xFFFF9933.toInt()
)

/**
 * Authoritative 3D Coordinate-Based Layout Map for Module 01: Indian Mega City.
 * Coordinate System: X = East(+) / West(-), Y = Elevation(+Up), Z = South(+) / North(-)
 * Total Open-World Scale: 2000 x 2000 units ([-1000, +1000]).
 */
object CityLandmarksAndModules {

    const val WORLD_SCALE_UNITS = 2000f

    // Indian National Flag Monument at Roundabout Origin (0, 0, 0)
    const val FLAG_POLE_X = 0f
    const val FLAG_POLE_Z = 0f
    const val FLAG_POLE_HEIGHT = 23.5f
    const val FLAG_WIDTH = 7.2f
    const val FLAG_HEIGHT = 4.8f
    const val FLAG_SAFFRON_HEX = 0xFFFF9933L
    const val FLAG_WHITE_HEX = 0xFFFFFFFFL
    const val FLAG_GREEN_HEX = 0xFF138808L
    const val FLAG_CHAKRA_NAVY_HEX = 0xFF000080L
    const val FLAG_ASHOKA_CHAKRA_SPOKES = 24

    // Central Circular Roundabout & Landmark Plinth Radii (centered at (0, 0, 0))
    const val FLAG_ISLAND_RADIUS = 5.5f
    const val POOL_DEEP_OUTER_RADIUS = 21.0f
    const val POOL_SHALLOW_OUTER_RADIUS = 28.0f
    const val PROMENADE_OUTER_RADIUS = 36.0f
    const val ROUNDABOUT_INNER_RADIUS = 36.0f
    const val ROUNDABOUT_CENTER_RADIUS = 45.0f
    const val ROUNDABOUT_OUTER_RADIUS = 54.0f
    const val ROUNDABOUT_PLAZA_SETBACK_RADIUS = 72.0f

    const val CENTRAL_UPGRADE_STORE_ID = "c_central_upgrade_forge"

    /**
     * Authoritative 3D Zone Definitions establishing structural scale for the Indian Mega City:
     * 1. Central Circular Roundabout: (0, 0, 0), radius 72u (inner island -> water ring -> promenade -> 18u circular roadway -> radial avenues)
     * 2. Commercial Shopping District (East Sector): X [260..760], Z [-140..360] (avenue grid, malls, showrooms, shops, bazaar)
     * 3. Inner Commercial Retail Strip (Mid-East): X [75..259], Z [-130..130] (weapon/clothing/forge corridor)
     * 4. Residential Blocks (SW Sector): X [-680..-240], Z [140..520] (villas, heights apartments, garden lanes)
     * 5. Inner City Residential Quarters (NW & SE Mid-Ring): closely connected townhouses, narrow lanes & pocket parks
     */
    val cityZones: List<CityZoneBounds> = listOf(
        CityZoneBounds(
            id = CityZoneId.CENTRAL_ROUNDABOUT,
            minX = -72f, maxX = 72f,
            minZ = -72f, maxZ = 72f,
            centerX = 0f, centerY = 0f, centerZ = 0f,
            radius = ROUNDABOUT_PLAZA_SETBACK_RADIUS,
            floorElevation = 0f,
            roadWidth = 18f,
            summary = "Central Flag Island (r=5.5u), Water Ring (r=5.5..28u), Promenade (r=28..36u), Roundabout Road (r=36..54u)"
        ),
        CityZoneBounds(
            id = CityZoneId.COMMERCIAL_SHOPPING_DISTRICT,
            minX = 260f, maxX = 760f,
            minZ = -140f, maxZ = 360f,
            centerX = 510f, centerY = 0f, centerZ = 110f,
            radius = 250f,
            floorElevation = 0f,
            roadWidth = 20f,
            summary = "East Mega Shopping Grid: Cyber Mall, Car/Bike Showrooms, Weapon Shops, Bazaar & Municipal Services"
        ),
        CityZoneBounds(
            id = CityZoneId.INNER_MIXED_AVENUE,
            minX = 75f, maxX = 259f,
            minZ = -140f, maxZ = 140f,
            centerX = 165f, centerY = 0f, centerZ = 0f,
            radius = 120f,
            floorElevation = 0f,
            roadWidth = 14f,
            summary = "Roundabout-to-Commercial Retail Corridor: Boutique Stores, Central Forge, Ability Dojo & Showrooms"
        ),
        CityZoneBounds(
            id = CityZoneId.RESIDENTIAL_BLOCKS,
            minX = -680f, maxX = -240f,
            minZ = 140f, maxZ = 520f,
            centerX = -460f, centerY = 0f, centerZ = 330f,
            radius = 220f,
            floorElevation = 0f,
            roadWidth = 14f,
            summary = "SW Residential Quarter: Linked Villas, Sakura Heights Towers, Pocket Parks & Narrow Side Lanes"
        ),
        CityZoneBounds(
            id = CityZoneId.RESIDENTIAL_BLOCKS,
            minX = -239f, maxX = -75f,
            minZ = -240f, maxZ = 240f,
            centerX = -155f, centerY = 0f, centerZ = 0f,
            radius = 140f,
            floorElevation = 0f,
            roadWidth = 10f,
            summary = "West Inner-City Residential Blocks: Closely Connected Townhouses, Corner Shops & Narrow Lanes"
        )
    )

    val worldModules: List<WorldModuleSpec> = listOf(
        WorldModuleSpec(WorldDistrictId.CENTRAL_CITY, WorldModuleStatus.ACTIVE_MODULE_01, 0f, 0f, 300f, hasShopsAllowed = true),
        WorldModuleSpec(WorldDistrictId.COMMERCIAL_REGION, WorldModuleStatus.ACTIVE_MODULE_01, 510f, 110f, 260f, hasShopsAllowed = true),
        WorldModuleSpec(WorldDistrictId.RESIDENTIAL_REGION, WorldModuleStatus.ACTIVE_MODULE_01, -460f, 330f, 220f, hasShopsAllowed = false),
        WorldModuleSpec(WorldDistrictId.FOREST_REGION, WorldModuleStatus.EXPANSION_RESERVED, 560f, -340f, 260f, hasShopsAllowed = false),
        WorldModuleSpec(WorldDistrictId.HIDDEN_VILLAGE, WorldModuleStatus.EXPANSION_RESERVED, -380f, -390f, 220f, hasShopsAllowed = false),
        WorldModuleSpec(WorldDistrictId.MOUNTAIN_REGION, WorldModuleStatus.EXPANSION_RESERVED, 0f, -720f, 260f, hasShopsAllowed = false),
        WorldModuleSpec(WorldDistrictId.COASTAL_REGION, WorldModuleStatus.EXPANSION_RESERVED, 0f, 680f, 300f, hasShopsAllowed = false),
        WorldModuleSpec(WorldDistrictId.AIRPORT, WorldModuleStatus.EXPANSION_RESERVED, -580f, -200f, 220f, hasShopsAllowed = false)
    )

    fun resolveCityZone(x: Float, z: Float): CityZoneBounds? =
        cityZones.firstOrNull { it.contains(x, z) }

    /**
     * Southern Ceremonial Marble Viewing Pier leading to (0, 14) player spawn.
     * Keeps (0, 14) dry at elevation 0f for Lobby, Login, Character Preview & spawn.
     */
    fun isOnSouthernCausewayPier(x: Float, z: Float): Boolean =
        abs(x) <= 6.5f && z in 4.8f..34.0f

    fun isOnFlagIslandPlinth(x: Float, z: Float): Boolean =
        hypot(x, z) <= FLAG_ISLAND_RADIUS

    fun isInCentralPool(x: Float, z: Float): Boolean {
        if (isOnSouthernCausewayPier(x, z)) return false
        val r = hypot(x, z)
        return r > FLAG_ISLAND_RADIUS && r <= POOL_SHALLOW_OUTER_RADIUS
    }

    fun centralPoolTerrainHeightOrNull(x: Float, z: Float): Float? {
        if (isOnFlagIslandPlinth(x, z)) return 0.35f
        if (isOnSouthernCausewayPier(x, z)) return 0f
        val r = hypot(x, z)
        if (r > POOL_SHALLOW_OUTER_RADIUS) return null
        return when {
            r <= POOL_DEEP_OUTER_RADIUS -> -2.35f
            else -> {
                val t = (POOL_SHALLOW_OUTER_RADIUS - r) / (POOL_SHALLOW_OUTER_RADIUS - POOL_DEEP_OUTER_RADIUS)
                -0.45f - t * 1.45f
            }
        }
    }

    val cityBenches: List<CityBenchProp> = listOf(
        CityBenchProp(-22f, 24f, 45f),
        CityBenchProp(22f, 24f, -45f),
        CityBenchProp(-24f, -22f, 135f),
        CityBenchProp(24f, -22f, -135f),
        CityBenchProp(-32f, 4f, 90f),
        CityBenchProp(32f, 4f, -90f),
        CityBenchProp(-66f, 52f, 0f),
        CityBenchProp(-82f, 64f, 90f),
        CityBenchProp(78f, -34f, 180f),
        CityBenchProp(452f, 232f, 0f)
    )

    val cityRoadSigns: List<CityRoadSignProp> = listOf(
        CityRoadSignProp(-38f, 38f, 45f, 0xFFFF9933.toInt()),
        CityRoadSignProp(38f, 38f, -45f, 0xFF138808.toInt()),
        CityRoadSignProp(-38f, -38f, 135f, 0xFF00E5FF.toInt()),
        CityRoadSignProp(38f, -38f, -135f, 0xFFFF3B5C.toInt()),
        CityRoadSignProp(306f, 16f, -90f, 0xFFF59E0B.toInt()),
        CityRoadSignProp(576f, -16f, 90f, 0xFF22C55E.toInt()),
        CityRoadSignProp(-276f, 174f, 45f, 0xFFA855F7.toInt())
    )

    val indianArches: List<IndianArchProp> = listOf(
        IndianArchProp(0f, 58f, 0f, spanWidth = 18f, height = 10.5f, stoneColor = 0xFFE6C79C.toInt(), accentColor = 0xFFFF9933.toInt()),
        IndianArchProp(58f, 0f, 90f, spanWidth = 18f, height = 10.5f, stoneColor = 0xFFE6C79C.toInt(), accentColor = 0xFF138808.toInt()),
        IndianArchProp(-58f, 0f, 90f, spanWidth = 18f, height = 10.5f, stoneColor = 0xFFE6C79C.toInt(), accentColor = 0xFF00E5FF.toInt()),
        IndianArchProp(426f, 250f, 90f, spanWidth = 16f, height = 10f, stoneColor = 0xFFFDE68A.toInt(), accentColor = 0xFFEA580C.toInt())
    )
}
