package com.example.game.world

import com.example.game.engine.NpcEntity
import com.example.game.engine.NpcRole
import com.example.game.engine.TrafficVehicleEntity
import com.example.game.engine.Vec3

/**
 * Provides NPC spawn anchors, patrol waypoints, and vehicle traffic corridors spanning all 8 districts.
 */
object NpcSpawnPoints {

    val roamWaypoints: List<Vec3> = listOf(
        // Central Anime City (Dry Roundabout Promenade, Viewing Pier & Mixed Avenues)
        Vec3(-28f, 0f, -22f), Vec3(28f, 0f, -22f), Vec3(-26f, 0f, 24f), Vec3(26f, 0f, 24f),
        Vec3(0f, 0f, -45f), Vec3(-46f, 0f, 44f), Vec3(48f, 0f, -24f), Vec3(-115f, 0f, 44f), Vec3(120f, 0f, -34f),
        // Hidden Village
        Vec3(-260f, 0f, -340f), Vec3(-380f, 0f, -340f), Vec3(-380f, 0f, -450f), Vec3(-470f, 0f, -340f),
        // Mountain Region
        Vec3(0f, 4f, -640f), Vec3(-120f, 10f, -725f), Vec3(0f, 12f, -720f), Vec3(180f, 16f, -825f),
        // Forest Region
        Vec3(480f, 0f, -340f), Vec3(640f, 0f, -340f), Vec3(740f, 0f, -440f),
        // Commercial Region
        Vec3(320f, 0f, 0f), Vec3(320f, 0f, 135f), Vec3(510f, 0f, 135f), Vec3(590f, 0f, 0f), Vec3(456f, 0f, 250f),
        // Residential Region
        Vec3(-420f, 0f, 360f), Vec3(-560f, 0f, 260f), Vec3(-320f, 0f, 460f),
        // Airport
        Vec3(-520f, 0f, -180f), Vec3(-620f, 0f, -280f),
        // Coastal Region
        Vec3(0f, 0f, 560f), Vec3(-180f, 0.5f, 825f), Vec3(260f, 0.5f, 820f)
    )

    fun createNpcs(): MutableList<NpcEntity> = mutableListOf(
        // Central Anime City Core
        NpcEntity(
            id = "npc_itachix_proxy",
            displayName = "ItachiX [AI Proxy]",
            role = NpcRole.OFFLINE_PLAYER_PROXY,
            spawnX = -4.2f, spawnZ = 19f,
            coatColor = 0xFF141824.toInt(), hairColor = 0xFF0F172A.toInt(), accentColor = 0xFFFF3B5C.toInt(),
            level = 48, guild = "AKATSUKI",
            statusBubble = "Offline AI Proxy: Guarding Flag Pier"
        ),
        NpcEntity(
            id = "npc_naruto_friend",
            displayName = "NarutoFF [AI Squad]",
            role = NpcRole.OFFLINE_PLAYER_PROXY,
            spawnX = 4.2f, spawnZ = 19f,
            coatColor = 0xFFF97316.toInt(), hairColor = 0xFFFACC15.toInt(), accentColor = 0xFF00E5FF.toInt(),
            level = 50, guild = "SHINOBI",
            statusBubble = "Running Masala Ramen Delivery"
        ),
        NpcEntity(
            id = "npc_blox_flame",
            displayName = "Crimson Blox",
            role = NpcRole.OFFLINE_PLAYER_PROXY,
            spawnX = 7.4f, spawnZ = 18.2f,
            coatColor = 0xFF141418.toInt(), hairColor = 0xFF15141B.toInt(), accentColor = 0xFFE11D2E.toInt(),
            level = 52, guild = "BLOX",
            statusBubble = "Crimson Flame Hero — Flag Plaza!"
        ),
        NpcEntity(
            id = "npc_riya_friend",
            displayName = "Riya_Gamer",
            role = NpcRole.CIVILIAN,
            spawnX = -22f, spawnZ = 32f,
            coatColor = 0xFF1E3A8A.toInt(), hairColor = 0xFF311042.toInt(), accentColor = 0xFFA855F7.toInt(),
            level = 41, guild = "SAKURA",
            statusBubble = "Strolling Roundabout Promenade"
        ),
        NpcEntity(
            id = "npc_rogue_1",
            displayName = "Shadow Ronin #1",
            role = NpcRole.RIVAL_BOT,
            spawnX = 18f, spawnZ = -18f,
            coatColor = 0xFF31102F.toInt(), hairColor = 0xFFE2E8F0.toInt(), accentColor = 0xFFFF3B5C.toInt(),
            level = 38, guild = "ROGUE",
            statusBubble = "Flag Monument Intruder — Combat Ready!"
        ),
        NpcEntity(
            id = "npc_rogue_2",
            displayName = "Cyber Raider #2",
            role = NpcRole.RIVAL_BOT,
            spawnX = -18f, spawnZ = 18f,
            coatColor = 0xFF3F1D2B.toInt(), hairColor = 0xFF00E5FF.toInt(), accentColor = 0xFFEF4444.toInt(),
            level = 40, guild = "ROGUE",
            statusBubble = "Patrolling Central Flag Plaza!"
        ),
        NpcEntity(
            id = "npc_flag_rogue_3",
            displayName = "Flag Plaza Striker #3",
            role = NpcRole.RIVAL_BOT,
            spawnX = 0f, spawnZ = -23f,
            coatColor = 0xFF450A0A.toInt(), hairColor = 0xFFFACC15.toInt(), accentColor = 0xFFFF3B5C.toInt(),
            level = 42, guild = "ROGUE",
            statusBubble = "Guarding Central Flag North Rim!"
        ),
        NpcEntity(
            id = "npc_flag_rogue_4",
            displayName = "Bazaar Katana Rogue #4",
            role = NpcRole.RIVAL_BOT,
            spawnX = 18f, spawnZ = 18f,
            coatColor = 0xFF2E1065.toInt(), hairColor = 0xFF38BDF8.toInt(), accentColor = 0xFFA855F7.toInt(),
            level = 43, guild = "ROGUE",
            statusBubble = "Hostile Fighter near Indian Flag!"
        ),
        NpcEntity(
            id = "npc_flag_rogue_5",
            displayName = "Neon Oni #5",
            role = NpcRole.RIVAL_BOT,
            spawnX = -20f, spawnZ = -18f,
            coatColor = 0xFF1F2937.toInt(), hairColor = 0xFFFF3B5C.toInt(), accentColor = 0xFFFF9933.toInt(),
            level = 45, guild = "ROGUE",
            statusBubble = "Hostile Target by Tiranga Monument!"
        ),
        // Hidden Village
        NpcEntity(
            id = "npc_hv_sensei",
            displayName = "Sensei Kakashi [AI]",
            role = NpcRole.OFFLINE_PLAYER_PROXY,
            spawnX = -380f, spawnZ = -440f,
            coatColor = 0xFF1E293B.toInt(), hairColor = 0xFFE2E8F0.toInt(), accentColor = 0xFF00E5FF.toInt(),
            level = 65, guild = "ANBU",
            statusBubble = "Supervising Shinobi Academy Trials"
        ),
        NpcEntity(
            id = "npc_hv_rogue",
            displayName = "Mist Assassin",
            role = NpcRole.RIVAL_BOT,
            spawnX = -460f, spawnZ = -350f,
            coatColor = 0xFF1E1B4B.toInt(), hairColor = 0xFFF43F5E.toInt(), accentColor = 0xFFA855F7.toInt(),
            level = 46, guild = "ROGUE",
            statusBubble = "Infiltrating Training Grounds!"
        ),
        // Commercial Region
        NpcEntity(
            id = "npc_officer_kuro",
            displayName = "Officer Kuro",
            role = NpcRole.POLICE,
            spawnX = 355f, spawnZ = 85f,
            coatColor = 0xFF1E40AF.toInt(), hairColor = 0xFF1E293B.toInt(), accentColor = 0xFF38BDF8.toInt(),
            level = 45, guild = "METRO PD",
            statusBubble = "Scanning traffic & anti-theft grid"
        ),
        NpcEntity(
            id = "npc_bazaar_merchant",
            displayName = "Aarav Merchant",
            role = NpcRole.SHOPKEEPER,
            spawnX = 456f, spawnZ = 248f,
            coatColor = 0xFFD97706.toInt(), hairColor = 0xFF1C1917.toInt(), accentColor = 0xFFFFD166.toInt(),
            level = 30, guild = "BAZAAR",
            statusBubble = "Selling exotic skins & spices"
        ),
        NpcEntity(
            id = "npc_pet_keeper",
            displayName = "Hana Pet Tamer",
            role = NpcRole.SHOPKEEPER,
            spawnX = 640f, spawnZ = 75f,
            coatColor = 0xFFDB2777.toInt(), hairColor = 0xFFFDE047.toInt(), accentColor = 0xFFF472B6.toInt(),
            level = 34, guild = "TAMERS",
            statusBubble = "Adopting Nine-Tails & Cyber Wolves"
        ),
        NpcEntity(
            id = "npc_comm_rogue",
            displayName = "Vortex Raider #3",
            role = NpcRole.RIVAL_BOT,
            spawnX = 520f, spawnZ = 140f,
            coatColor = 0xFF1E1B4B.toInt(), hairColor = 0xFFF43F5E.toInt(), accentColor = 0xFFA855F7.toInt(),
            level = 44, guild = "ROGUE",
            statusBubble = "Raiding Dealership Avenue!"
        ),
        // Residential Region
        NpcEntity(
            id = "npc_res_neighbor",
            displayName = "Ren Takahashi",
            role = NpcRole.CIVILIAN,
            spawnX = -420f, spawnZ = 345f,
            coatColor = 0xFF0F766E.toInt(), hairColor = 0xFF0F172A.toInt(), accentColor = 0xFF22C55E.toInt(),
            level = 28, guild = "VILLAS",
            statusBubble = "Enjoying Residential Garden Blvd"
        ),
        // Airport
        NpcEntity(
            id = "npc_air_captain",
            displayName = "Captain Akira",
            role = NpcRole.POLICE,
            spawnX = -530f, spawnZ = -170f,
            coatColor = 0xFF1E293B.toInt(), hairColor = 0xFF94A3B8.toInt(), accentColor = 0xFF38BDF8.toInt(),
            level = 42, guild = "SKYPORT",
            statusBubble = "Clearing runway & helipad traffic"
        ),
        // Forest & Mountain
        NpcEntity(
            id = "npc_forest_ranger",
            displayName = "Ranger Kaito",
            role = NpcRole.CIVILIAN,
            spawnX = 620f, spawnZ = -345f,
            coatColor = 0xFF14532D.toInt(), hairColor = 0xFF78350F.toInt(), accentColor = 0xFF22C55E.toInt(),
            level = 36, guild = "RANGERS",
            statusBubble = "Patrolling Emerald River Bridge"
        ),
        NpcEntity(
            id = "npc_mtn_rogue",
            displayName = "Summit Ronin",
            role = NpcRole.RIVAL_BOT,
            spawnX = 120f, spawnZ = -795f,
            coatColor = 0xFF31102F.toInt(), hairColor = 0xFFE2E8F0.toInt(), accentColor = 0xFF38BDF8.toInt(),
            level = 52, guild = "ROGUE",
            statusBubble = "Guarding the Mountain Switchback!"
        ),
        // Hidden Village Academy Students & Mission Handlers
        NpcEntity(
            id = "npc_hv_student_1",
            displayName = "Sora Academy",
            role = NpcRole.ACADEMY_STUDENT,
            spawnX = -370f, spawnZ = -428f,
            coatColor = 0xFF1D4ED8.toInt(), hairColor = 0xFF0F172A.toInt(), accentColor = 0xFF38BDF8.toInt(),
            level = 22, guild = "ACADEMY",
            statusBubble = "Practicing chakra kunai drills"
        ),
        NpcEntity(
            id = "npc_hv_student_2",
            displayName = "Yuki Trainee",
            role = NpcRole.ACADEMY_STUDENT,
            spawnX = -392f, spawnZ = -432f,
            coatColor = 0xFF0F766E.toInt(), hairColor = 0xFFF472B6.toInt(), accentColor = 0xFF2DD4BF.toInt(),
            level = 20, guild = "ACADEMY",
            statusBubble = "Studying Shinobi Scrolls"
        ),
        NpcEntity(
            id = "npc_city_mission_handler",
            displayName = "Commander Kenji",
            role = NpcRole.MISSION_NPC,
            spawnX = -4.5f, spawnZ = 26f,
            coatColor = 0xFF134E4A.toInt(), hairColor = 0xFFE2E8F0.toInt(), accentColor = 0xFF2DD4BF.toInt(),
            level = 58, guild = "HQ",
            statusBubble = "Assigning District Patrol Bounties"
        ),
        NpcEntity(
            id = "npc_comm_mission_handler",
            displayName = "Agent Meera",
            role = NpcRole.MISSION_NPC,
            spawnX = 336f, spawnZ = 22f,
            coatColor = 0xFF334155.toInt(), hairColor = 0xFF1C1917.toInt(), accentColor = 0xFFF59E0B.toInt(),
            level = 47, guild = "SYNDICATE",
            statusBubble = "Coordinating Commercial District Contracts"
        ),
        // Coastal Harbor
        NpcEntity(
            id = "npc_coast_surfer",
            displayName = "Mei Harbor Guide",
            role = NpcRole.CIVILIAN,
            spawnX = 12f, spawnZ = 585f,
            coatColor = 0xFF0284C7.toInt(), hairColor = 0xFFF472B6.toInt(), accentColor = 0xFF00E5FF.toInt(),
            level = 29, guild = "HARBOR",
            statusBubble = "Boats ready at Azure Bay Docks"
        )
    )

    fun createTraffic(): MutableList<TrafficVehicleEntity> = mutableListOf(
        TrafficVehicleEntity("tv_1", "Neon Coupe", "supercar", 0xFF0EA5E9.toInt(), laneAxisZ = 5f, laneAxisX = null, minBound = -560f, maxBound = 720f, x = -25f, z = 5f, rotY = 90f, speed = 13.5f),
        TrafficVehicleEntity("tv_2", "Bazaar Runner", "suv", 0xFFF59E0B.toInt(), laneAxisZ = -5f, laneAxisX = null, minBound = -560f, maxBound = 720f, x = 220f, z = -5f, rotY = -90f, speed = 11.5f),
        TrafficVehicleEntity("tv_3", "Metro Patrol", "police", 0xFF2563EB.toInt(), laneAxisZ = null, laneAxisX = 5f, minBound = -600f, maxBound = 630f, x = 5f, z = -120f, rotY = 0f, speed = 14.0f),
        TrafficVehicleEntity("tv_4", "Sakura Scooter", "motorcycle", 0xFFEC4899.toInt(), laneAxisZ = null, laneAxisX = -5f, minBound = -600f, maxBound = 630f, x = -5f, z = 140f, rotY = 180f, speed = 15.0f),
        TrafficVehicleEntity("tv_5", "Coastal Cruiser", "supercar", 0xFF22C55E.toInt(), laneAxisZ = 556f, laneAxisX = null, minBound = -700f, maxBound = 700f, x = -180f, z = 556f, rotY = 90f, speed = 14.5f),
        TrafficVehicleEntity("tv_6", "Commercial Express", "suv", 0xFFA855F7.toInt(), laneAxisZ = null, laneAxisX = 315f, minBound = -110f, maxBound = 330f, x = 315f, z = 60f, rotY = 0f, speed = 11.0f),
        TrafficVehicleEntity("tv_7", "Villa Sedan", "supercar", 0xFFE11D48.toInt(), laneAxisZ = 356f, laneAxisX = null, minBound = -620f, maxBound = -280f, x = -440f, z = 356f, rotY = 90f, speed = 10.0f)
    )
}
