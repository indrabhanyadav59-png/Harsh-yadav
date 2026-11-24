package com.example.game.world

import com.example.game.engine.BuildingStyle
import com.example.game.engine.WorldBuilding
import com.example.game.engine.WorldTree

data class ParkFountainProp(
    val x: Float,
    val z: Float,
    val radius: Float
)

object CityGenerator {

    fun generate(roads: List<RoadSegment>): Triple<List<WorldBuilding>, List<WorldTree>, Pair<List<DistrictBillboard>, List<ParkFountainProp>>> {
        val buildings = mutableListOf<WorldBuilding>()
        val trees = mutableListOf<WorldTree>()
        val billboards = mutableListOf<DistrictBillboard>()
        val fountains = mutableListOf<ParkFountainProp>()

        val dCity = WorldDistrictId.CENTRAL_CITY.displayName
        val dComm = WorldDistrictId.COMMERCIAL_REGION.displayName

        val candidates = listOf(
            // =========================================================================
            // 1. CENTRAL CIRCULAR ROUNDABOUT PERIMETER (All buildings r >= 76u from origin)
            // =========================================================================
            BuildingGenerator.makeModernTower("c_akiba_tower", "Indra Skyline Cyber Tower", dCity, -86f, -44f, 22f, 18f, 44f, 0xFF1E293B.toInt(), 0xFF0F172A.toInt(), 0xFF00E5FF.toInt()),
            BuildingGenerator.makeModernTower("c_shinjuku_spire", "Surya Neon Spire", dCity, 118f, -112f, 24f, 20f, 52f, 0xFF1E243B.toInt(), 0xFF0F172A.toInt(), 0xFFFF9933.toInt()),
            BuildingGenerator.makeModernTower("c_sky_hotel", "Ashoka Grand Hotel", dCity, 118f, 112f, 24f, 20f, 38f, 0xFFE2E8F0.toInt(), 0xFF1E3A8A.toInt(), 0xFF38BDF8.toInt()),
            WorldBuilding("c_torii_sanctuary", "Grand Heritage Pavilion", dCity, 0f, -94f, 22f, 16f, 14f, BuildingStyle.INDIAN_PALACE, 0xFFFDE68A.toInt(), 0xFFEA580C.toInt(), 0xFFFF9933.toInt()),

            // =========================================================================
            // 2. INNER-WEST RESIDENTIAL BLOCKS (Linked Indian-Anime Apartments & Houses)
            // =========================================================================
            WorldBuilding("c_res_blk_1", "Lotus Courtyard House A", dCity, -138f, -42f, 18f, 16f, 11f, BuildingStyle.PLAYER_VILLA, 0xFFFDF4E7.toInt(), 0xFFB45309.toInt(), 0xFFFF9933.toInt(), enterable = true, interiorType = InteriorType.SMALL_HOUSE),
            BuildingGenerator.makeModernTower("c_res_blk_2", "Yamuna Heights Apartments", dCity, -88f, -124f, 22f, 18f, 26f, 0xFFF1F5F9.toInt(), 0xFF334155.toInt(), 0xFFA855F7.toInt()),
            BuildingGenerator.makeModernTower("c_res_blk_3", "Shibuya Lofts West", dCity, -138f, -124f, 20f, 18f, 24f, 0xFFE2E8F0.toInt(), 0xFF1E3A8A.toInt(), 0xFF38BDF8.toInt()),
            WorldBuilding("c_res_blk_4", "Gulmohar Lane Villa", dCity, -88f, 44f, 18f, 16f, 10f, BuildingStyle.PLAYER_VILLA, 0xFFFED7AA.toInt(), 0xFF9A3412.toInt(), 0xFF22C55E.toInt(), enterable = true, interiorType = InteriorType.SMALL_HOUSE),
            BuildingGenerator.makeModernTower("c_res_blk_5", "Ganga Residency Block B", dCity, -138f, 44f, 20f, 18f, 22f, 0xFFFDE68A.toInt(), 0xFF78350F.toInt(), 0xFFF59E0B.toInt()),
            WorldBuilding("c_res_blk_6", "Kitsune Arcade & Flats", dCity, -88f, 124f, 22f, 18f, 20f, BuildingStyle.JAPANESE_MODERN, 0xFF1E1B4B.toInt(), 0xFF312E81.toInt(), 0xFFA855F7.toInt()),
            BuildingGenerator.makeModernTower("c_res_blk_7", "Kaveri Terrace Flats", dCity, -138f, 124f, 20f, 18f, 25f, 0xFFCBD5E1.toInt(), 0xFF1E293B.toInt(), 0xFF00E5FF.toInt()),
            BuildingGenerator.makeModernTower("c_metro_sw", "Nehru Nagar Towers", dCity, -198f, 96f, 22f, 18f, 28f, 0xFFF1F5F9.toInt(), 0xFF334155.toInt(), 0xFFFF3B5C.toInt()),
            BuildingGenerator.makeModernTower("c_metro_west", "Kurogane Tech Residency", dCity, -198f, -64f, 22f, 20f, 32f, 0xFFCBD5E1.toInt(), 0xFF1E293B.toInt(), 0xFF00E5FF.toInt()),

            // =========================================================================
            // 3. INNER-EAST MIXED RETAIL AVENUE (Shops, Central Forge, Ability Dojo & Bistro)
            // =========================================================================
            WorldBuilding(CityLandmarksAndModules.CENTRAL_UPGRADE_STORE_ID, "Vishwakarma Central Weapon Forge", dCity, 120f, -34f, 22f, 18f, 14f, BuildingStyle.GARAGE, 0xFF1E293B.toInt(), 0xFFEA580C.toInt(), 0xFFFFD166.toInt(), enterable = true, interiorType = InteriorType.UPGRADE_STORE),
            WorldBuilding("c_ramen_bistro", "Ichiraku Masala Bistro", dCity, 120f, 34f, 18f, 15f, 11f, BuildingStyle.JAPANESE_MODERN, 0xFFFDF4E7.toInt(), 0xFFB91C1C.toInt(), 0xFFFFD166.toInt(), enterable = true, interiorType = InteriorType.RESTAURANT),
            WorldBuilding("c_ability_shop", "Indra Chakra Ability Dojo", dCity, 212f, -34f, 22f, 18f, 13f, BuildingStyle.JAPANESE_SHRINE, 0xFFF5EFE6.toInt(), 0xFF312E81.toInt(), 0xFF00E5FF.toInt(), enterable = true, interiorType = InteriorType.ABILITY_SHOP),
            WorldBuilding("c_cloth_1", "Jaipur Couture Clothing Studio", dCity, 212f, 34f, 20f, 16f, 12f, BuildingStyle.INDIAN_PALACE, 0xFFFBCFE8.toInt(), 0xFFDB2777.toInt(), 0xFFFFD166.toInt(), enterable = true, interiorType = InteriorType.CLOTHING_STORE),
            WorldBuilding("c_wpn_shop_1", "Saffron Tactical Armory", dCity, 266f, -34f, 20f, 16f, 12f, BuildingStyle.JAPANESE_MODERN, 0xFF1E243B.toInt(), 0xFF991B1B.toInt(), 0xFFFF9933.toInt(), enterable = true, interiorType = InteriorType.WEAPON_SHOP),
            WorldBuilding("c_bike_show_1", "Shinobi Neon Bike Gallery", dCity, 266f, 34f, 22f, 16f, 11f, BuildingStyle.DEALERSHIP, 0xFF0F172A.toInt(), 0xFF0284C7.toInt(), 0xFF00E5FF.toInt(), enterable = true, interiorType = InteriorType.BIKE_SHOWROOM),
            WorldBuilding("c_cloth_2", "Shibuya Drip Fashion House", dCity, 212f, -110f, 22f, 18f, 15f, BuildingStyle.SHOPPING_MALL, 0xFFFDE68A.toInt(), 0xFFBE123C.toInt(), 0xFFF472B6.toInt(), enterable = true, interiorType = InteriorType.CLOTHING_STORE),
            WorldBuilding("c_car_show_1", "Indraprastha Royal Motors", dCity, 212f, 110f, 24f, 18f, 13f, BuildingStyle.DEALERSHIP, 0xFF1E293B.toInt(), 0xFFDC2626.toInt(), 0xFFFFD166.toInt(), enterable = true, interiorType = InteriorType.CAR_SHOWROOM),

            // Outer City Anchors
            BuildingGenerator.makeModernTower("c_outer_n1", "Midori Office Tower", dCity, -96f, -212f, 24f, 20f, 32f, 0xFF1E293B.toInt(), 0xFF0F172A.toInt(), 0xFF38BDF8.toInt()),
            BuildingGenerator.makeModernTower("c_outer_n2", "Asahi Neon Tower", dCity, 96f, -212f, 26f, 20f, 38f, 0xFF334155.toInt(), 0xFF0F172A.toInt(), 0xFFFF3B5C.toInt()),
            BuildingGenerator.makeModernTower("c_outer_s1", "Bayview Condos West", dCity, -88f, 222f, 24f, 18f, 28f, 0xFFE2E8F0.toInt(), 0xFF1E3A8A.toInt(), 0xFF00E5FF.toInt()),
            BuildingGenerator.makeModernTower("c_outer_s2", "Bayview Condos East", dCity, 88f, 222f, 24f, 18f, 30f, 0xFFCBD5E1.toInt(), 0xFF0F172A.toInt(), 0xFFA855F7.toInt()),

            // =========================================================================
            // 4. COMMERCIAL SHOPPING DISTRICT GRID (East Sector X [320..760], Z [-140..360])
            // =========================================================================
            WorldBuilding("m_hospital", "Indra Central Hospital", dComm, 388f, -38f, 28f, 22f, 22f, BuildingStyle.HOSPITAL, 0xFFF8FAFC.toInt(), 0xFFE11D48.toInt(), 0xFFFF3B5C.toInt()),
            WorldBuilding("m_police", "Metro Police Headquarters", dComm, 388f, 64f, 26f, 20f, 16f, BuildingStyle.POLICE_STATION, 0xFFCBD5E1.toInt(), 0xFF1E293B.toInt(), 0xFF3B82F6.toInt()),
            WorldBuilding("m_mall", "Shibuya-Indra Cyber Mall", dComm, 522f, 56f, 38f, 26f, 24f, BuildingStyle.SHOPPING_MALL, 0xFF1E293B.toInt(), 0xFF334155.toInt(), 0xFFA855F7.toInt(), enterable = true, interiorType = InteriorType.SHOPPING_MALL),
            WorldBuilding("m_dealership", "Kitsune Supercar Dealership", dComm, 522f, 200f, 32f, 22f, 13f, BuildingStyle.DEALERSHIP, 0xFF0F172A.toInt(), 0xFFDC2626.toInt(), 0xFF00E5FF.toInt(), enterable = true, interiorType = InteriorType.CAR_SHOWROOM),
            WorldBuilding("m_car_show_2", "Garuda Hypercar Pavilion", dComm, 522f, -38f, 28f, 20f, 13f, BuildingStyle.DEALERSHIP, 0xFF1E293B.toInt(), 0xFF0284C7.toInt(), 0xFF38BDF8.toInt(), enterable = true, interiorType = InteriorType.CAR_SHOWROOM),
            WorldBuilding("m_car_show_3", "Metro Velocity Motors", dComm, 665f, 200f, 28f, 20f, 12f, BuildingStyle.DEALERSHIP, 0xFF334155.toInt(), 0xFF16A34A.toInt(), 0xFF22C55E.toInt(), enterable = true, interiorType = InteriorType.CAR_SHOWROOM),
            WorldBuilding("m_bike_show_2", "Hayabusa Neon Superbikes", dComm, 388f, 198f, 24f, 18f, 11f, BuildingStyle.DEALERSHIP, 0xFF0F172A.toInt(), 0xFFF97316.toInt(), 0xFFFF9933.toInt(), enterable = true, interiorType = InteriorType.BIKE_SHOWROOM),
            WorldBuilding("m_bike_show_3", "Royal Cyber Cruisers", dComm, 665f, -120f, 24f, 18f, 11f, BuildingStyle.DEALERSHIP, 0xFF1E293B.toInt(), 0xFFB91C1C.toInt(), 0xFFFFD166.toInt(), enterable = true, interiorType = InteriorType.BIKE_SHOWROOM),
            WorldBuilding("m_weapon_shop", "Kurogane Weapon Shop", dComm, 665f, -38f, 22f, 18f, 12f, BuildingStyle.JAPANESE_MODERN, 0xFF1E243B.toInt(), 0xFF991B1B.toInt(), 0xFFFF3B5C.toInt(), enterable = true, interiorType = InteriorType.WEAPON_SHOP),
            WorldBuilding("m_wpn_shop_2", "Chakra Arms & Blades", dComm, 388f, -120f, 22f, 18f, 12f, BuildingStyle.JAPANESE_MODERN, 0xFF312E81.toInt(), 0xFF1E1B4B.toInt(), 0xFF00E5FF.toInt(), enterable = true, interiorType = InteriorType.WEAPON_SHOP),
            WorldBuilding("m_wpn_shop_3", "Bazaar Ordnance Depot", dComm, 522f, -120f, 22f, 18f, 12f, BuildingStyle.JAPANESE_MODERN, 0xFF1F2937.toInt(), 0xFFEA580C.toInt(), 0xFFFACC15.toInt(), enterable = true, interiorType = InteriorType.WEAPON_SHOP),
            WorldBuilding("m_pet_shop", "Spirited Companion Pet Shop", dComm, 665f, 64f, 22f, 18f, 11f, BuildingStyle.JAPANESE_MODERN, 0xFFFDF4E7.toInt(), 0xFFDB2777.toInt(), 0xFFF472B6.toInt()),
            WorldBuilding("m_dhaba_cafe", "Tandoor & Ramen Lounge", dComm, 665f, 282f, 22f, 16f, 10f, BuildingStyle.INDIAN_PALACE, 0xFFFED7AA.toInt(), 0xFFDC2626.toInt(), 0xFFFFD166.toInt(), enterable = true, interiorType = InteriorType.RESTAURANT),
            WorldBuilding("m_indian_palace", "Indraprastha Royal Bazaar", dComm, 522f, 284f, 34f, 24f, 19f, BuildingStyle.INDIAN_PALACE, 0xFFFDE68A.toInt(), 0xFFF59E0B.toInt(), 0xFF06B6D4.toInt()),
            WorldBuilding("m_cloth_3", "Jaipur Silk & Armor Couture", dComm, 388f, 284f, 22f, 16f, 11f, BuildingStyle.INDIAN_PALACE, 0xFFFBCFE8.toInt(), 0xFFDB2777.toInt(), 0xFFFFD166.toInt(), enterable = true, interiorType = InteriorType.CLOTHING_STORE),

            // Outer Expanded City Zone Towers & Buyable Forest Estate
            WorldBuilding("c_forest_villa_buy", "Emerald Pine Villa", WorldDistrictId.FOREST_REGION.displayName, 460f, -240f, 22f, 18f, 12f, BuildingStyle.PLAYER_VILLA, 0xFFE2E8F0.toInt(), 0xFF15803D.toInt(), 0xFF22C55E.toInt(), enterable = true, interiorType = InteriorType.PLAYER_VILLA),
            BuildingGenerator.makeModernTower("c_east_exp_1", "Eastgate Cyber Plaza", dComm, 860f, 64f, 28f, 22f, 40f, 0xFF1E293B.toInt(), 0xFF0F172A.toInt(), 0xFF00E5FF.toInt()),
            BuildingGenerator.makeModernTower("c_east_exp_2", "Kansai Horizon Tower", dComm, 940f, -64f, 26f, 22f, 44f, 0xFF334155.toInt(), 0xFF1E1B4B.toInt(), 0xFFFF9933.toInt()),
            BuildingGenerator.makeModernTower("c_west_exp_1", "Vayu Skyport Lofts", dCity, -860f, 64f, 26f, 22f, 34f, 0xFFE2E8F0.toInt(), 0xFF1E3A8A.toInt(), 0xFF38BDF8.toInt()),
            BuildingGenerator.makeModernTower("c_south_exp_1", "Marina Bay Suites", WorldDistrictId.COASTAL_REGION.displayName, -760f, 480f, 24f, 20f, 28f, 0xFFF1F5F9.toInt(), 0xFF0F766E.toInt(), 0xFF00E5FF.toInt())
        )

        for (b in candidates) {
            BuildingGenerator.safePlaceBuilding(roads, b)?.let { buildings += it }
        }

        // Park Fountains (outside the central pool/roundabout)
        fountains += ParkFountainProp(-74f, 58f, 7.5f)
        fountains += ParkFountainProp(86f, 62f, 6.0f)
        fountains += ParkFountainProp(455f, 220f, 7.5f)

        // Glowing Neon Anime Billboards framing the Roundabout & Shopping Avenues
        billboards += DistrictBillboard(-58f, 15f, -58f, 11f, 5.5f, 45f, 0xFF00E5FF.toInt())
        billboards += DistrictBillboard(58f, 16f, 58f, 12f, 6.0f, -135f, 0xFFFF9933.toInt())
        billboards += DistrictBillboard(142f, 18f, 22f, 13f, 6.2f, -90f, 0xFFA855F7.toInt())
        billboards += DistrictBillboard(475f, 17f, 26f, 14f, 6.5f, 180f, 0xFFFFD166.toInt())
        billboards += DistrictBillboard(-142f, 16f, -24f, 12f, 5.5f, 90f, 0xFF38BDF8.toInt())

        // Trees lining Roundabout Promenade (r ~ 31..34u) & City Parks
        val sakuraGrid = listOf(
            -26f to 22f, 26f to 22f, -22f to -26f, 22f to -26f,
            -32f to -8f, 32f to 8f,
            -62f to 48f, -66f to 70f, 64f to -54f, 72f to 58f,
            -128f to 20f, 128f to -18f, -120f to -142f, 120f to 142f,
            240f to 20f, 240f to -20f, 350f to 20f, 620f to 20f
        )
        for ((tx, tz) in sakuraGrid) {
            if (!RoadGenerator.isPointNearRoad(roads, tx, tz, 4.0f) && !CityLandmarksAndModules.isInCentralPool(tx, tz)) {
                trees += WorldTree(tx, tz, 7.0f, isSakura = true)
            }
        }

        return Triple(buildings, trees, billboards to fountains)
    }
}
