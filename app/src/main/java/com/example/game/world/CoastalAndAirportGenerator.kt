package com.example.game.world

import com.example.game.engine.BuildingStyle
import com.example.game.engine.ParkedWorldVehicle
import com.example.game.engine.WorldBuilding
import com.example.game.engine.WorldTree

data class BoatProp(
    val x: Float,
    val z: Float,
    val rotY: Float,
    val hullColor: Int,
    val isYacht: Boolean = false
)

object CoastalAndAirportGenerator {

    fun generate(roads: List<RoadSegment>): Triple<List<WorldBuilding>, List<WorldTree>, Pair<List<BoatProp>, List<ParkedWorldVehicle>>> {
        val buildings = mutableListOf<WorldBuilding>()
        val trees = mutableListOf<WorldTree>()
        val boats = mutableListOf<BoatProp>()
        val parkedVehicles = mutableListOf<ParkedWorldVehicle>()

        val dRes = WorldDistrictId.RESIDENTIAL_REGION.displayName
        val dAir = WorldDistrictId.AIRPORT.displayName
        val dCoast = WorldDistrictId.COASTAL_REGION.displayName

        val candidates = listOf(
            // --- RESIDENTIAL REGION (SW x in [-760..-260], z in [140..530]) ---
            BuildingGenerator.makeVilla("res_my_villa", "My Shinobi Villa (Enterable)", dRes, -420f, 295f, 0xFFFDF4E7.toInt(), 0xFF334155.toInt(), 0xFF22C55E.toInt(), enterable = true),
            WorldBuilding("res_my_garage", "Private Clan Garage", dRes, -448f, 295f, 14f, 12f, 7f, BuildingStyle.GARAGE, 0xFF334155.toInt(), 0xFF0F172A.toInt(), 0xFF00E5FF.toInt()),
            BuildingGenerator.makeVilla("res_itachi_villa", "ItachiX Residence (Enterable)", dRes, -420f, 435f, 0xFFE2E8F0.toInt(), 0xFF7F1D1D.toInt(), 0xFFFF3B5C.toInt(), enterable = true),
            BuildingGenerator.makeVilla("res_sakura_villa", "Sakura Blossom Villa", dRes, -330f, 295f, 0xFFFDF2F8.toInt(), 0xFFBE185D.toInt(), 0xFFF472B6.toInt(), enterable = true),
            BuildingGenerator.makeModernTower("res_heights_1", "Sakura Heights Tower A", dRes, -555f, 210f, 26f, 20f, 28f, 0xFFF1F5F9.toInt(), 0xFF334155.toInt(), 0xFFA855F7.toInt()),
            BuildingGenerator.makeModernTower("res_heights_2", "Sakura Heights Tower B", dRes, -555f, 305f, 26f, 20f, 32f, 0xFFE2E8F0.toInt(), 0xFF1E3A8A.toInt(), 0xFF38BDF8.toInt()),
            BuildingGenerator.makeModernTower("res_apartments_s", "Maple Garden Apartments", dRes, -545f, 415f, 30f, 20f, 22f, 0xFFFDE68A.toInt(), 0xFF78350F.toInt(), 0xFFF59E0B.toInt()),
            BuildingGenerator.makeVilla("res_corner_house", "Naruko Modern House", dRes, -335f, 415f, 0xFFF8FAFC.toInt(), 0xFF1E293B.toInt(), 0xFF00E5FF.toInt(), enterable = false),

            // --- AIRPORT (West x in [-940..-420], z in [-450..-40]) ---
            WorldBuilding("air_terminal", "Indra Skyport Terminal", dAir, -510f, -135f, 44f, 26f, 16f, BuildingStyle.DEALERSHIP, 0xFFE2E8F0.toInt(), 0xFF1E3A8A.toInt(), 0xFF00E5FF.toInt()),
            WorldBuilding("air_atc", "Skyport Control Tower", dAir, -565f, -150f, 12f, 12f, 34f, BuildingStyle.LIGHTHOUSE, 0xFFF8FAFC.toInt(), 0xFF0F172A.toInt(), 0xFF38BDF8.toInt()),
            WorldBuilding("air_hangar_1", "Skyport Hangar Alpha", dAir, -690f, -160f, 42f, 32f, 17f, BuildingStyle.AIRPORT_HANGAR, 0xFF475569.toInt(), 0xFF1E293B.toInt(), 0xFFF59E0B.toInt()),
            WorldBuilding("air_hangar_2", "Skyport Hangar Bravo", dAir, -770f, -160f, 42f, 32f, 17f, BuildingStyle.AIRPORT_HANGAR, 0xFF334155.toInt(), 0xFF0F172A.toInt(), 0xFF00E5FF.toInt()),
            WorldBuilding("air_cargo", "Aero Fuel & Cargo Depot", dAir, -620f, -350f, 24f, 18f, 11f, BuildingStyle.GARAGE, 0xFF64748B.toInt(), 0xFF1E293B.toInt(), 0xFFEF4444.toInt()),

            // --- COASTAL REGION (South z > 540) ---
            WorldBuilding("cst_harbor_club", "Azure Harbor Marina Club", dCoast, 55f, 595f, 28f, 18f, 11f, BuildingStyle.DEALERSHIP, 0xFFF8FAFC.toInt(), 0xFF0284C7.toInt(), 0xFF00E5FF.toInt()),
            WorldBuilding("cst_beach_cafe", "Coral Sunset Beach Bar", dCoast, -85f, 595f, 22f, 16f, 8.5f, BuildingStyle.PLAYER_VILLA, 0xFFFDF4E7.toInt(), 0xFFF97316.toInt(), 0xFFFFD166.toInt()),
            WorldBuilding("cst_lighthouse", "Azure Island Lighthouse", dCoast, -180f, 855f, 12f, 12f, 30f, BuildingStyle.LIGHTHOUSE, 0xFFF8FAFC.toInt(), 0xFFEF4444.toInt(), 0xFF00E5FF.toInt()),
            BuildingGenerator.makeVilla("cst_pearl_resort", "Sunset Pearl Beach Resort", dCoast, 260f, 855f, 0xFFFDF4E7.toInt(), 0xFF0EA5E9.toInt(), 0xFFFFD166.toInt(), enterable = true)
        )

        for (b in candidates) {
            BuildingGenerator.safePlaceBuilding(roads, b)?.let { buildings += it }
        }

        // Residential gardens & sakura trees
        val resTrees = listOf(
            -385f to 295f, -475f to 295f, -385f to 435f, -475f to 435f,
            -600f to 245f, -600f to 335f, -310f to 240f, -310f to 330f
        )
        for ((tx, tz) in resTrees) {
            if (!RoadGenerator.isPointNearRoad(roads, tx, tz, 4f)) {
                trees += WorldTree(tx, tz, 6.8f, isSakura = true)
            }
        }

        // Palm trees lining the entire southern coastline & islands
        for (px in -720..720 step 75) {
            val pz = 605f + ((kotlin.math.abs(px) % 3) * 8f)
            if (!RoadGenerator.isPointNearRoad(roads, px.toFloat(), pz, 5f)) {
                trees += WorldTree(px.toFloat(), pz, 8.5f, isSakura = false, isPalm = true)
            }
        }
        trees += WorldTree(-205f, 835f, 8.8f, isSakura = false, isPalm = true)
        trees += WorldTree(-152f, 845f, 8.2f, isSakura = false, isPalm = true)
        trees += WorldTree(235f, 835f, 9.0f, isSakura = false, isPalm = true)
        trees += WorldTree(290f, 845f, 8.4f, isSakura = false, isPalm = true)

        // Moored Boats & Cruisers in the ocean harbor
        boats += BoatProp(-30f, 705f, 15f, 0xFFF8FAFC.toInt(), isYacht = true)
        boats += BoatProp(32f, 718f, -20f, 0xFFDC2626.toInt(), isYacht = false)
        boats += BoatProp(140f, 745f, 65f, 0xFF00E5FF.toInt(), isYacht = true)
        boats += BoatProp(-260f, 760f, 110f, 0xFFF59E0B.toInt(), isYacht = false)
        boats += BoatProp(340f, 880f, -40f, 0xFFF8FAFC.toInt(), isYacht = true)

        // Parked drivable vehicles distributed across key districts
        parkedVehicles += ParkedWorldVehicle("pv_gtr", "Kitsune GT-R", "supercar", 9f, 30f, 35f, 0xFFE11D48.toInt(), isLocked = false, ownerName = "Your Garage")
        parkedVehicles += ParkedWorldVehicle("pv_moto", "Shinobi R1", "motorcycle", -9f, 30f, -25f, 0xFF00E5FF.toInt(), isLocked = false, ownerName = "Your Garage")
        parkedVehicles += ParkedWorldVehicle("pv_police", "Sakura PD Cruiser", "police", 360f, 92f, 90f, 0xFF1D4ED8.toInt(), isLocked = true, ownerName = "Metro Police")
        parkedVehicles += ParkedWorldVehicle("pv_bazaar_suv", "Akatsuki SUV", "suv", 480f, 215f, 0f, 0xFFF59E0B.toInt(), isLocked = false, ownerName = "Dealership")
        parkedVehicles += ParkedWorldVehicle("pv_villa_gt", "Kitsune GT-R", "supercar", -420f, 330f, 180f, 0xFF22C55E.toInt(), isLocked = false, ownerName = "Your Garage")
        parkedVehicles += ParkedWorldVehicle("pv_heli", "Indra Sky-Hawk", "helicopter", -680f, -340f, 45f, 0xFF334155.toInt(), isLocked = false, ownerName = "Skyport Pad")
        parkedVehicles += ParkedWorldVehicle("pv_coast_buggy", "Dune Ronin Buggy", "buggy", -30f, 585f, 180f, 0xFFF97316.toInt(), isLocked = false, ownerName = "Harbor Rental")

        return Triple(buildings, trees, boats to parkedVehicles)
    }
}
