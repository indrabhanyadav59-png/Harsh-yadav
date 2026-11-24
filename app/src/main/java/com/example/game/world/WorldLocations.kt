package com.example.game.world

enum class WorldDistrictId(val displayName: String, val accentColor: Long) {
    CENTRAL_CITY("Central Anime City", 0xFFFF3B5C),
    HIDDEN_VILLAGE("Hidden Village", 0xFFF97316),
    MOUNTAIN_REGION("Mountain Region", 0xFF38BDF8),
    FOREST_REGION("Forest Region", 0xFF22C55E),
    COASTAL_REGION("Coastal Region", 0xFF00E5FF),
    RESIDENTIAL_REGION("Residential Region", 0xFFA855F7),
    COMMERCIAL_REGION("Commercial Region", 0xFFF59E0B),
    AIRPORT("Airport", 0xFF60A5FA)
}

enum class LocationCategory(val label: String, val colorHex: Long) {
    SPAWN("Spawn", 0xFFFFD166),
    SERVICE("Services", 0xFFEF4444),
    SHOP("Shops", 0xFFF59E0B),
    HOUSING("Housing", 0xFF22C55E),
    VILLAGE("Village", 0xFFF97316),
    AIRPORT("Airport", 0xFF60A5FA),
    LANDMARK("Landmarks", 0xFF00E5FF),
    MISSION("Missions", 0xFFA855F7)
}

data class WorldLocation(
    val id: String,
    val name: String,
    val district: WorldDistrictId,
    val category: LocationCategory,
    val x: Float,
    val y: Float,
    val z: Float,
    val subtitle: String
)

/**
 * Single authoritative World Coordinate Registry for the 2400 x 2400 open world (x, z in [-1200, +1200]).
 * Used by Player Spawn, Fast Travel, GPS Navigation, Shops, Missions, Housing, and World Map markers.
 */
object WorldLocations {

    const val WORLD_MIN = -1200f
    const val WORLD_MAX = 1200f
    const val PLAYABLE_BOUND = 1185f

    val playerSpawn = WorldLocation(
        id = "loc_spawn_central",
        name = "Sakura Torii Plaza",
        district = WorldDistrictId.CENTRAL_CITY,
        category = LocationCategory.SPAWN,
        x = 0f, y = 0f, z = 14f,
        subtitle = "Central Anime City heart, fountain & main hub"
    )

    val allLocations: List<WorldLocation> = listOf(
        playerSpawn,
        WorldLocation(
            id = "loc_indian_flag_monument",
            name = "Indian National Flag Monument",
            district = WorldDistrictId.CENTRAL_CITY,
            category = LocationCategory.LANDMARK,
            x = 0f, y = 0.35f, z = 0f,
            subtitle = "Central circular roundabout monument & recreational pool ring"
        ),
        WorldLocation(
            id = "loc_central_upgrade_forge",
            name = "Vishwakarma Central Weapon Forge",
            district = WorldDistrictId.CENTRAL_CITY,
            category = LocationCategory.SHOP,
            x = 120f, y = 0f, z = -34f,
            subtitle = "Inner Mixed Avenue upgrade forge & anime retail strip"
        ),
        WorldLocation(
            id = "loc_inner_residential_blocks",
            name = "Gulmohar Inner Residential Blocks",
            district = WorldDistrictId.CENTRAL_CITY,
            category = LocationCategory.HOUSING,
            x = -115f, y = 0f, z = 44f,
            subtitle = "Closely linked courtyard houses, apartments & narrow lanes"
        ),
        WorldLocation(
            id = "loc_central_park",
            name = "Sakura Fountain Park",
            district = WorldDistrictId.CENTRAL_CITY,
            category = LocationCategory.LANDMARK,
            x = -74f, y = 0f, z = 58f,
            subtitle = "Central plaza park with glowing fountain"
        ),
        WorldLocation(
            id = "loc_ramen_street",
            name = "Ichiraku Neon Bistro",
            district = WorldDistrictId.CENTRAL_CITY,
            category = LocationCategory.SHOP,
            x = 52f, y = 0f, z = -42f,
            subtitle = "Anime ramen bar & glowing neon restaurant row"
        ),
        WorldLocation(
            id = "loc_hidden_gate",
            name = "Hidden Village Great Gate",
            district = WorldDistrictId.HIDDEN_VILLAGE,
            category = LocationCategory.VILLAGE,
            x = -260f, y = 0f, z = -340f,
            subtitle = "Ceremonial crimson village entrance"
        ),
        WorldLocation(
            id = "loc_shinobi_academy",
            name = "Shinobi Training Academy",
            district = WorldDistrictId.HIDDEN_VILLAGE,
            category = LocationCategory.VILLAGE,
            x = -380f, y = 0f, z = -460f,
            subtitle = "Academy campus, sparring yard & monument"
        ),
        WorldLocation(
            id = "loc_training_grounds",
            name = "Kunai Training Grounds",
            district = WorldDistrictId.HIDDEN_VILLAGE,
            category = LocationCategory.MISSION,
            x = -470f, y = 0f, z = -380f,
            subtitle = "Combat trial arena & obstacle course"
        ),
        WorldLocation(
            id = "loc_dragon_viewpoint",
            name = "Dragon Ridge Viewpoint",
            district = WorldDistrictId.MOUNTAIN_REGION,
            category = LocationCategory.LANDMARK,
            x = 0f, y = 12f, z = -720f,
            subtitle = "Scenic overlook facing the 4 Guardian Stone Statues"
        ),
        WorldLocation(
            id = "loc_mountain_shrine",
            name = "Summit Cloud Shrine",
            district = WorldDistrictId.MOUNTAIN_REGION,
            category = LocationCategory.MISSION,
            x = 240f, y = 18f, z = -830f,
            subtitle = "Hidden high-altitude shrine at mountain peak"
        ),
        WorldLocation(
            id = "loc_forest_bridge",
            name = "Emerald River Crossing",
            district = WorldDistrictId.FOREST_REGION,
            category = LocationCategory.LANDMARK,
            x = 560f, y = 1.2f, z = -340f,
            subtitle = "Scenic wooden & stone bridge over winding forest river"
        ),
        WorldLocation(
            id = "loc_forest_camp",
            name = "Whispering Pine Trailhead",
            district = WorldDistrictId.FOREST_REGION,
            category = LocationCategory.MISSION,
            x = 760f, y = 0f, z = -450f,
            subtitle = "Dense wildlife forest trails & ancient clearing"
        ),
        WorldLocation(
            id = "loc_hospital",
            name = "Indra Central Hospital",
            district = WorldDistrictId.COMMERCIAL_REGION,
            category = LocationCategory.SERVICE,
            x = 380f, y = 0f, z = -20f,
            subtitle = "Medical recovery center & trauma helipad"
        ),
        WorldLocation(
            id = "loc_police_hq",
            name = "Metro Police Headquarters",
            district = WorldDistrictId.COMMERCIAL_REGION,
            category = LocationCategory.SERVICE,
            x = 380f, y = 0f, z = 110f,
            subtitle = "Anti-theft task force & cruiser dispatch"
        ),
        WorldLocation(
            id = "loc_shopping_mall",
            name = "Shibuya Cyber Mall",
            district = WorldDistrictId.COMMERCIAL_REGION,
            category = LocationCategory.SHOP,
            x = 520f, y = 0f, z = 40f,
            subtitle = "Multi-level neon mall & Indraprastha Bazaar plaza"
        ),
        WorldLocation(
            id = "loc_dealership",
            name = "Kitsune Supercar Dealership",
            district = WorldDistrictId.COMMERCIAL_REGION,
            category = LocationCategory.SHOP,
            x = 520f, y = 0f, z = 190f,
            subtitle = "Exotic supercars, bikes & custom neon paint"
        ),
        WorldLocation(
            id = "loc_main_garage",
            name = "Apex Tuners Main Garage",
            district = WorldDistrictId.COMMERCIAL_REGION,
            category = LocationCategory.SERVICE,
            x = 660f, y = 0f, z = 190f,
            subtitle = "Vehicle repair, upgrades & Car-Key security"
        ),
        WorldLocation(
            id = "loc_weapon_shop",
            name = "Kurogane Arsenal & Skins",
            district = WorldDistrictId.COMMERCIAL_REGION,
            category = LocationCategory.SHOP,
            x = 660f, y = 0f, z = -20f,
            subtitle = "Rifles, katanas, SMGs & energy skins"
        ),
        WorldLocation(
            id = "loc_pet_shop",
            name = "Spirited Companion Pet Shop",
            district = WorldDistrictId.COMMERCIAL_REGION,
            category = LocationCategory.SHOP,
            x = 660f, y = 0f, z = 80f,
            subtitle = "Nine-Tails Foxes, Dragons, Cyber Wolves & Phoenixes"
        ),
        WorldLocation(
            id = "loc_indian_palace",
            name = "Indraprastha Royal Bazaar",
            district = WorldDistrictId.COMMERCIAL_REGION,
            category = LocationCategory.LANDMARK,
            x = 460f, y = 0f, z = 300f,
            subtitle = "Sandstone domes, silk arcade & festival plaza"
        ),
        WorldLocation(
            id = "loc_player_villa",
            name = "My Shinobi Villa (Enterable)",
            district = WorldDistrictId.RESIDENTIAL_REGION,
            category = LocationCategory.HOUSING,
            x = -420f, y = 0f, z = 310f,
            subtitle = "Player-owned villa with tatami interior & garage"
        ),
        WorldLocation(
            id = "loc_residential_apts",
            name = "Sakura Heights Apartments",
            district = WorldDistrictId.RESIDENTIAL_REGION,
            category = LocationCategory.HOUSING,
            x = -560f, y = 0f, z = 240f,
            subtitle = "Modern anime apartment towers & courtyard gardens"
        ),
        WorldLocation(
            id = "loc_friend_villa",
            name = "ItachiX Clan Estate",
            district = WorldDistrictId.RESIDENTIAL_REGION,
            category = LocationCategory.HOUSING,
            x = -420f, y = 0f, z = 430f,
            subtitle = "Enterable neighbor villa & garden driveway"
        ),
        WorldLocation(
            id = "loc_airport_terminal",
            name = "Indra Skyport Terminal",
            district = WorldDistrictId.AIRPORT,
            category = LocationCategory.AIRPORT,
            x = -520f, y = 0f, z = -180f,
            subtitle = "International terminal, hangars & ATC tower"
        ),
        WorldLocation(
            id = "loc_helipad",
            name = "Skyport Helicopter Pad",
            district = WorldDistrictId.AIRPORT,
            category = LocationCategory.AIRPORT,
            x = -680f, y = 0f, z = -340f,
            subtitle = "Helicopter landing zone & Sky-Hawk launchpad"
        ),
        WorldLocation(
            id = "loc_coastal_harbor",
            name = "Azure Bay Harbor & Docks",
            district = WorldDistrictId.COASTAL_REGION,
            category = LocationCategory.LANDMARK,
            x = 0f, y = 0.6f, z = 680f,
            subtitle = "Waterfront promenade, piers & moored boats"
        ),
        WorldLocation(
            id = "loc_lighthouse_island",
            name = "Azure Island Lighthouse",
            district = WorldDistrictId.COASTAL_REGION,
            category = LocationCategory.MISSION,
            x = -180f, y = 0.5f, z = 840f,
            subtitle = "Ocean island connected by southern suspension bridge"
        ),
        WorldLocation(
            id = "loc_pearl_island",
            name = "Sunset Pearl Resort",
            district = WorldDistrictId.COASTAL_REGION,
            category = LocationCategory.LANDMARK,
            x = 260f, y = 0.5f, z = 830f,
            subtitle = "Tropical beach island, coastal highway & boats"
        ),
        WorldLocation(
            id = "loc_plot_saffron",
            name = "Saffron Land Plot #1 (ज़मीन)",
            district = WorldDistrictId.RESIDENTIAL_REGION,
            category = LocationCategory.HOUSING,
            x = -340f, y = 0f, z = 515f,
            subtitle = "Open purchasable plot near Residential Blvd"
        ),
        WorldLocation(
            id = "loc_plot_cyber",
            name = "Cyber Skyline Plot #2 (ज़मीन)",
            district = WorldDistrictId.CENTRAL_CITY,
            category = LocationCategory.HOUSING,
            x = 260f, y = 0f, z = -260f,
            subtitle = "Prime central avenue land plot for building & storage"
        ),
        WorldLocation(
            id = "loc_plot_sunrise",
            name = "Coastal Sunrise Land #3 (ज़मीन)",
            district = WorldDistrictId.COASTAL_REGION,
            category = LocationCategory.HOUSING,
            x = -360f, y = 0f, z = 600f,
            subtitle = "Sea-breeze beachfront plot with boat access"
        ),
        WorldLocation(
            id = "loc_house_gulmohar",
            name = "Gulmohar Lane Villa (For Sale)",
            district = WorldDistrictId.CENTRAL_CITY,
            category = LocationCategory.HOUSING,
            x = -88f, y = 0f, z = 44f,
            subtitle = "Search & buy this enterable downtown courtyard villa"
        ),
        WorldLocation(
            id = "loc_house_forest_estate",
            name = "Emerald Pine Villa (For Sale)",
            district = WorldDistrictId.FOREST_REGION,
            category = LocationCategory.HOUSING,
            x = 460f, y = 0f, z = -240f,
            subtitle = "Scenic forest luxury house with stash room"
        )
    )

    fun resolveDistrictId(x: Float, z: Float): WorldDistrictId = when {
        z <= -580f -> WorldDistrictId.MOUNTAIN_REGION
        z >= 540f -> WorldDistrictId.COASTAL_REGION
        x <= -420f && z in -460f..-40f -> WorldDistrictId.AIRPORT
        x <= -140f && z in -640f..-260f -> WorldDistrictId.HIDDEN_VILLAGE
        x >= 280f && z < -110f -> WorldDistrictId.FOREST_REGION
        x >= 260f && z in -110f..460f -> WorldDistrictId.COMMERCIAL_REGION
        x <= -240f && z in 120f..539f -> WorldDistrictId.RESIDENTIAL_REGION
        else -> WorldDistrictId.CENTRAL_CITY
    }

    fun resolveDistrictName(x: Float, z: Float): String = resolveDistrictId(x, z).displayName

    fun findById(id: String): WorldLocation? = allLocations.firstOrNull { it.id == id }
}
