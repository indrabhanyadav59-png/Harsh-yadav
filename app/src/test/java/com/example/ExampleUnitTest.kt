package com.example

import com.example.game.engine.NpcRespawnController
import com.example.game.engine.ShowcaseMode
import com.example.game.engine.WorldMapData
import com.example.game.engine.WorldSimulationState
import com.example.ui.FacebookAuthBridge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun npcRespawn_disablesOnDefeatAndRestoresAfterDelay() {
        val ctrl = NpcRespawnController(maxHealth = 100f, respawnDelaySec = 5f)
        assertFalse(ctrl.takeDamage(40f))
        assertEquals(60f, ctrl.health, 0.01f)
        assertTrue(ctrl.collidersEnabled)

        assertTrue(ctrl.takeDamage(60f))
        assertEquals(0f, ctrl.health, 0.01f)
        assertFalse(ctrl.collidersEnabled)
        assertFalse(ctrl.navAgentEnabled)
        assertEquals("Defeated", ctrl.animationTrigger)

        assertFalse(ctrl.tickRespawn(3.0f))
        assertTrue(ctrl.tickRespawn(2.1f))
        assertEquals(100f, ctrl.health, 0.01f)
        assertTrue(ctrl.collidersEnabled)
        assertTrue(ctrl.navAgentEnabled)
        assertEquals("Respawn", ctrl.animationTrigger)
    }

    @Test
    fun worldMap_resolvesAll8DistrictsAndCollisions() {
        assertEquals("Central Anime City", WorldMapData.resolveDistrict(0f, 14f))
        assertEquals("Hidden Village", WorldMapData.resolveDistrict(-380f, -460f))
        assertEquals("Mountain Region", WorldMapData.resolveDistrict(0f, -720f))
        assertEquals("Forest Region", WorldMapData.resolveDistrict(560f, -340f))
        assertEquals("Coastal Region", WorldMapData.resolveDistrict(0f, 680f))
        assertEquals("Residential Region", WorldMapData.resolveDistrict(-420f, 310f))
        assertEquals("Commercial Region", WorldMapData.resolveDistrict(520f, 40f))
        assertEquals("Airport", WorldMapData.resolveDistrict(-520f, -180f))

        // Inside any generated building center, chunk-accelerated collision pushes player outward
        val sampleBuilding = WorldMapData.buildings.first()
        val (rx, rz) = WorldMapData.resolveCollision(sampleBuilding.x, sampleBuilding.z, 0.7f)
        assertTrue(rx != sampleBuilding.x || rz != sampleBuilding.z)
    }

    @Test
    fun worldRegistryAndTerrain_coverAll8DistrictsAndElevations() {
        val districtsInRegistry = com.example.game.world.WorldLocations.allLocations
            .map { it.district }
            .toSet()
        assertEquals(8, districtsInRegistry.size)

        // Central plaza is flat ground
        assertEquals(0f, WorldMapData.terrainHeightAt(0f, 14f), 0.1f)
        // Northern Mountain Region rises significantly above sea level
        assertTrue(WorldMapData.terrainHeightAt(0f, -850f) > 5f)
        // Southern Ocean away from islands and bridges is below water level
        assertTrue(WorldMapData.isDeepWater(0f, 820f))
        // Coastal bridge deck at x=260? Let's verify forest river bridge or road bridge keeps dry elevation
        val bridgeSeg = com.example.game.world.WorldGenerator.roads.first { it.isBridge }
        val bridgeY = WorldMapData.terrainHeightAt(bridgeSeg.midX, bridgeSeg.midZ)
        assertTrue(bridgeY > 0.5f)
    }

    @Test
    fun worldNavigation_findsObstacleFreePathAndChunkManagerIndexes() {
        val path = com.example.game.systems.WorldNavigation.findPath(0f, 14f, 520f, 40f)
        assertTrue(path.size >= 2)
        assertEquals(0f, path.first().x, 1f)
        assertEquals(520f, path.last().x, 1f)

        val chunkMgr = com.example.game.world.WorldGenerator.chunkManager
        assertEquals(100, chunkMgr.allChunks().size)
        val nearby = mutableListOf<com.example.game.engine.Aabb2D>()
        chunkMgr.collectNearbyAabbs(0f, 14f, 60f, nearby)
        assertTrue(nearby.isNotEmpty())
    }

    @Test
    fun simulation_abilityAndWeaponFireUpdateState() {
        val sim = WorldSimulationState()
        val initialAmmo = sim.ammoInMag
        sim.fireActiveWeapon()
        assertEquals(initialAmmo - 1, sim.ammoInMag)
        assertTrue(sim.projectiles.isNotEmpty())

        assertTrue(sim.castAbility(0, "Crimson Slash"))
        assertTrue(sim.abilityCooldowns[0] > 0f)
        assertFalse(sim.castAbility(0, "Crimson Slash"))
    }

    @Test
    fun showcaseMode_stagesHeroAndAllowsLobbyOrbit() {
        val sim = WorldSimulationState()
        sim.showcaseMode = ShowcaseMode.LOBBY
        sim.showcaseDragYawOffset = 35f
        sim.step(0.03f)

        assertEquals(0f, sim.playerX, 0.01f)
        assertEquals(14f, sim.playerZ, 0.01f)
        assertEquals((196f + 35f) % 360f, sim.cameraYaw, 0.1f)

        // Switching to NONE hands full movement control back to player
        sim.showcaseMode = ShowcaseMode.NONE
        sim.joyY = 1f
        sim.step(0.05f)
        assertTrue(sim.playerX != 0f || sim.playerZ != 14f)
    }

    @Test
    fun facebookAuthBridge_detectsPlaceholderCredentialsSafely() {
        assertFalse(FacebookAuthBridge.isConfigured())
    }

    @Test
    fun physicsWorld_blocksBuildingsVehiclesInteriorAndCameraClipping() {
        val b = WorldMapData.buildings.first()
        val (bx, bz) = com.example.game.engine.PhysicsWorld.resolveCapsuleWorld(
            x = b.x,
            y = 0f,
            z = b.z,
            radius = 0.7f,
            insideHouse = null
        )
        assertTrue(bx != b.x || bz != b.z)

        // House interior clamps walls & pushes out of center coffee table
        val (ix, iz) = com.example.game.engine.PhysicsWorld.resolveCapsuleWorld(
            x = 0f,
            y = 0f,
            z = -1.0f,
            radius = 0.7f,
            insideHouse = b
        )
        assertTrue(ix != 0f || iz != -1.0f)

        // Camera raycast pulls camera forward if desired point is inside a building
        val camPos = com.example.game.engine.PhysicsWorld.resolveCameraPosition(
            pivotX = b.x - b.width - 5f,
            pivotY = 1.6f,
            pivotZ = b.z,
            desiredX = b.x,
            desiredY = 1.6f,
            desiredZ = b.z,
            insideHouse = null
        )
        assertTrue(camPos.x < b.x)
    }

    @Test
    fun combatAndVehicleSystem_handlesMeleeWallLosAndEnterExit() {
        val sim = WorldSimulationState()
        val enemy = sim.npcs.first()
        enemy.x = sim.playerX
        enemy.z = sim.playerZ + 2.5f
        enemy.y = WorldMapData.terrainHeightAt(enemy.x, enemy.z)
        sim.cameraYaw = 0f // facing +Z toward enemy
        val hpBefore = enemy.respawnController.health
        sim.triggerMeleeAttack()
        assertTrue(enemy.respawnController.health < hpBefore)
        assertTrue(sim.lastDamageFeedback != null)

        // Accessible starter vehicle near plaza can be entered, driven, and exited
        val starterCar = sim.parkedVehicles.first { !it.isLocked }
        sim.playerX = starterCar.x
        sim.playerZ = starterCar.z + 2.2f
        sim.playerY = WorldMapData.terrainHeightAt(sim.playerX, sim.playerZ)
        var alarmTriggered = false
        sim.triggerVehicleInteraction { alarmTriggered = true }
        assertFalse(alarmTriggered)
        repeat(15) { sim.step(0.04f) }
        assertEquals(starterCar.modelName, sim.drivingVehicle?.modelName)

        // Exit vehicle restores parked vehicle safely beside player
        sim.triggerVehicleInteraction {}
        repeat(15) { sim.step(0.04f) }
        assertEquals(null, sim.drivingVehicle)
    }

    @Test
    fun hudCustomizationModels_validatesFivePresetsClampingAndMirror() {
        val uid = "user_hud_test"
        val defaultBundle = com.example.data.model.PlayerHudSettingsBundle.defaultForUser(uid)
        assertEquals(5, defaultBundle.layouts.size)

        // Essential buttons cannot be hidden and scale/opacity clamp within safe bounds
        val unsafeFire = defaultBundle.activeLayout.element(com.example.data.model.HudElementId.FIRE)
            .copy(visible = false, sizeScale = 5.0f, opacity = 0.05f)
        val sanitizedFire = unsafeFire.sanitized()
        assertTrue(sanitizedFire.visible)
        assertEquals(2.0f, sanitizedFire.sizeScale, 0.001f)
        assertEquals(0.2f, sanitizedFire.opacity, 0.001f)

        val originalFireX = defaultBundle.activeLayout.element(com.example.data.model.HudElementId.FIRE).normX
        val mirrored = defaultBundle.activeLayout.mirrorLeftRight()
        assertEquals(1f - originalFireX, mirrored.element(com.example.data.model.HudElementId.FIRE).normX, 0.01f)
    }

    @Test
    fun heroAppearanceCatalog_resolvesAllOutfitsHairsAccessoriesAndLegacyNames() {
        val catalog = com.example.data.model.HeroAppearanceCatalog
        assertEquals(7, catalog.outfits.size)
        assertEquals(5, catalog.hairs.size)
        assertEquals(4, catalog.accessories.size)

        // Exact presets
        assertEquals("shadow", catalog.resolveOutfit("Shadow Blaze").id)
        assertEquals("white", catalog.resolveOutfit("White Storm").id)
        assertEquals("cyber", catalog.resolveOutfit("Cyber Hunter").id)
        assertEquals("gold", catalog.resolveOutfit("Golden King").id)
        assertEquals("purple", catalog.resolveOutfit("Dark Phantom").id)
        assertEquals("ninja", catalog.resolveOutfit("Red Ninja").id)
        assertEquals("crimson_blox", catalog.resolveOutfit("Crimson Blox").id)
        assertEquals("crimson_tips", catalog.resolveHair("Black & Crimson").id)

        // Legacy names backward compatibility
        assertEquals("shadow", catalog.resolveOutfit("Shadow Akatsuki Coat").id)
        assertEquals("white", catalog.resolveHair("Silver Hokage").id)
        assertEquals("red", catalog.resolveHair("Spiky Crimson").id)
        assertEquals("wings", catalog.resolveAccessory("Energy Wings").id)
        assertEquals("none", catalog.resolveAccessory("None").id)
    }

    @Test
    fun npcAppearanceGenerator_isDeterministicVariedAndExcludesPlayerExclusiveRareSkins() {
        val gen = com.example.data.model.NpcAppearanceGenerator
        val first = gen.generate("npc_plaza_01", "CIVILIAN")
        val duplicate = gen.generate("npc_plaza_01", "CIVILIAN")
        assertEquals(first, duplicate)

        val sim = WorldSimulationState()
        val npcSignatures = sim.npcs.map {
            "${it.appearance.hairStyle}:${it.appearance.hairColorInt}:${it.appearance.coatSilhouette}:${it.appearance.coatColorInt}:${it.appearance.backAccessory}"
        }.toSet()
        // At least 6 distinct visual appearances across the open-world NPCs
        assertTrue(npcSignatures.size >= 6)

        // Proportion bounds check
        sim.npcs.forEach { npc ->
            assertTrue(npc.appearance.heightScale in 0.90f..1.10f)
            assertTrue(npc.appearance.buildScale in 0.90f..1.10f)
        }

        // Civilian & Academy NPCs never receive player-exclusive rare Cyber Hunter / Golden King colors or Energy Wings
        val civilianNpcColors = sim.npcs
            .filter { it.role == com.example.game.engine.NpcRole.CIVILIAN }
            .map { it.appearance.coatColorInt }
        val exclusiveGoldenChest = com.example.data.model.HeroAppearanceCatalog.resolveOutfit("gold").chestPanelColorInt
        assertFalse(civilianNpcColors.contains(exclusiveGoldenChest))
    }

    @Test
    fun worldSimulation_preservesHeroAndNpcAppearanceAcrossRespawn() {
        val sim = WorldSimulationState()
        sim.applyHeroAppearance("Golden King", "Azure", "Energy Wings", "#FFD166")
        assertEquals("gold", sim.heroOutfitId)
        assertEquals("blue", sim.heroHairId)
        assertEquals("wings", sim.heroAccessoryId)

        // Defeat player and tick past 5s respawn delay (step() clamps dt to 0.05f max)
        sim.applyDamageToPlayer(500f, "Test Rival")
        assertTrue(sim.isDefeated)
        repeat(120) { sim.step(0.05f) }
        assertFalse(sim.isDefeated)
        assertEquals("gold", sim.heroOutfitId)
        assertEquals("blue", sim.heroHairId)
        assertEquals("wings", sim.heroAccessoryId)

        // Defeat NPC and verify its deterministic appearance seed/spec persists after respawn
        val targetNpc = sim.npcs.first()
        val originalSeed = targetNpc.appearance.seed
        val originalAppearance = targetNpc.appearance
        if (targetNpc.respawnController.takeDamage(999f)) {
            targetNpc.state = com.example.game.engine.NpcBehaviorState.DEFEATED
        }
        assertFalse(targetNpc.respawnController.collidersEnabled)
        repeat(120) { sim.step(0.05f) }
        assertTrue(targetNpc.respawnController.collidersEnabled)
        assertEquals(originalSeed, targetNpc.appearance.seed)
        assertEquals(originalAppearance, targetNpc.appearance)
    }

    @Test
    fun dynamicWeaponSystem_handlesDropPickupAmmoPersistenceHealGrenadeAndProne() {
        val sim = WorldSimulationState()
        assertEquals(com.example.data.model.WeaponType.FIREARM, sim.activeWeaponSpec().weaponType)
        sim.aiming = true
        sim.fireActiveWeapon()
        val remainingArAmmo = sim.ammoInMag

        // Switch to Katana -> disables firearm aim & ammo
        sim.switchWeapon("wpn_shadow_katana")
        assertEquals(com.example.data.model.WeaponType.KATANA, sim.activeWeaponSpec().weaponType)
        assertFalse(sim.aiming)
        assertEquals(0, sim.ammoInMag)

        // Switch back to AR -> preserves saved AR ammo
        sim.switchWeapon("wpn_vortex_ar")
        assertEquals(remainingArAmmo, sim.ammoInMag)

        // Drop weapons down to unarmed fists; fists cannot be dropped
        sim.dropActiveWeapon() // drops AR
        assertFalse("wpn_vortex_ar" in sim.ownedWeaponIds)
        assertTrue(sim.findNearestGroundWeapon(ignoreGrace = true) != null)
        sim.dropActiveWeapon() // drops Pistol
        sim.dropActiveWeapon() // drops Katana -> falls back to fists
        assertEquals("wpn_fists", sim.activeWeaponId)
        assertEquals(null, sim.dropActiveWeapon())

        // Pick up nearest dropped weapon
        val picked = sim.pickupNearbyWeapon(maxDist = 6.0f)
        assertTrue(picked != null)
        assertTrue(sim.activeWeaponId != "wpn_fists")

        // Prone, Sprint, Jump transitions
        sim.sprinting = true
        sim.toggleProne()
        assertTrue(sim.proning)
        assertFalse(sim.sprinting)
        assertFalse(sim.crouching)
        sim.triggerJump()
        assertFalse(sim.proning)

        // Medkit heal & Frag grenade throw
        sim.hp = 100f
        val initialMeds = sim.medkitCount
        assertTrue(sim.triggerHeal())
        assertEquals(initialMeds - 1, sim.medkitCount)
        assertEquals(175f, sim.hp, 0.1f)

        val initialGrenades = sim.grenadeCount
        assertTrue(sim.throwGrenade())
        assertEquals(initialGrenades - 1, sim.grenadeCount)
        assertTrue(sim.activeGrenades.isNotEmpty())
        repeat(30) { sim.step(0.05f) }
        assertTrue(sim.activeGrenades.isEmpty())

        // All 17 HUD elements & presets initialize cleanly and support L/R mirroring
        val bundle = com.example.data.model.PlayerHudSettingsBundle.defaultForUser("hud_17_test")
        assertEquals(17, bundle.activeLayout.elements.size)
        val mirrored = bundle.activeLayout.mirrorLeftRight()
        assertEquals(17, mirrored.elements.size)
    }

    @Test
    fun petCatalog_containsAll50UniqueAnimalsAcrossFiveCategories() {
        val pets = com.example.data.model.PetCatalog.allPets
        assertEquals(50, pets.size)
        assertEquals(50, pets.map { it.id }.toSet().size)
        assertEquals(50, pets.map { it.name }.toSet().size)
        assertEquals((1..50).toList(), pets.map { it.number })

        com.example.data.model.PetCategory.entries.forEach { cat ->
            assertEquals(10, com.example.data.model.PetCatalog.findByCategory(cat).size)
        }
        pets.forEach { spec ->
            assertTrue(spec.passiveName.isNotBlank())
            assertTrue(spec.passiveSummary.isNotBlank())
            assertTrue(spec.activeName.isNotBlank())
            assertTrue(spec.activeSummary.isNotBlank())
            assertTrue(spec.personality.isNotBlank())
            assertTrue(spec.evolutionMultiplier(3) > spec.evolutionMultiplier(2))
            assertTrue(spec.evolutionMultiplier(2) > spec.evolutionMultiplier(1))
        }

        // Wild Lv.2/Lv.3 seeds resolve cleanly
        assertTrue(com.example.data.model.PetCatalog.wildWorldSeeds.size >= 8)
        com.example.data.model.PetCatalog.wildWorldSeeds.forEach { seed ->
            assertTrue(seed.evolutionLevel in 2..3)
            assertEquals(seed.petId, com.example.data.model.PetCatalog.resolveSpec(seed.petId).id)
        }
    }

    @Test
    fun petSimulation_enforcesAntiStackingCapsActiveSkillsAndPhoenixSave() {
        val roster = listOf(
            com.example.data.model.OwnedPet(id = "pet_04_cat", petType = "Cat", equipped = true, evolutionLevel = 3L),
            com.example.data.model.OwnedPet(id = "pet_08_rabbit", petType = "Rabbit", equipped = true, evolutionLevel = 3L),
            com.example.data.model.OwnedPet(id = "pet_22_griffin", petType = "Griffin", equipped = true, evolutionLevel = 3L),
            com.example.data.model.OwnedPet(id = "pet_19_phoenix", petType = "Phoenix", equipped = true, evolutionLevel = 3L),
            com.example.data.model.OwnedPet(id = "pet_50_celestial_beast", petType = "Celestial Beast", equipped = true, evolutionLevel = 3L),
            // 6th equipped pet should be ignored beyond MAX_EQUIPPED_ROSTER (5)
            com.example.data.model.OwnedPet(id = "pet_09_bear", petType = "Bear", equipped = true, evolutionLevel = 3L)
        )
        val capped = com.example.data.model.PetCatalog.computeCappedPassives(roster)
        assertEquals(5, capped.equippedCount)
        assertTrue(capped.speedBonusPct <= com.example.data.model.PetCatalog.CAP_SPEED_BONUS + 0.0001f)
        assertTrue(capped.maxHpBonusPct <= com.example.data.model.PetCatalog.CAP_MAX_HP_BONUS + 0.0001f)
        assertTrue(capped.attackBonusPct <= com.example.data.model.PetCatalog.CAP_ATTACK_BONUS + 0.0001f)
        assertTrue(capped.hasEmergencySave)

        // Phoenix emergency save protects against fatal damage once per life
        val sim = WorldSimulationState()
        sim.petController.updateRoster(roster)
        sim.shield = 0f
        sim.hp = 50f
        sim.applyDamageToPlayer(300f, "Boss")
        assertFalse(sim.isDefeated)
        assertTrue(sim.hp > 0f)
        assertTrue(sim.petController.emergencySaveUsedThisLife)

        // Second fatal blow in same life defeats player
        sim.applyDamageToPlayer(500f, "Boss")
        assertTrue(sim.isDefeated)

        // Active Pet skill execution: Wolf Enemy Radar + Fox HoT + Dash
        val wolfSpec = com.example.data.model.PetCatalog.resolveSpec("Wolf")
        sim.petController.executeActivePetAbility(wolfSpec, 2, sim)
        assertTrue(sim.petController.effectiveEnemyRadarRadius() >= 65f)
    }

    @Test
    fun indianMegaCityLayout_definesRoundaboutCommercialResidentialZonesAndNonOverlappingScale() {
        val zones = com.example.game.world.CityLandmarksAndModules.cityZones
        val zoneIds = zones.map { it.id }.toSet()
        assertTrue(com.example.game.world.CityZoneId.CENTRAL_ROUNDABOUT in zoneIds)
        assertTrue(com.example.game.world.CityZoneId.COMMERCIAL_SHOPPING_DISTRICT in zoneIds)
        assertTrue(com.example.game.world.CityZoneId.RESIDENTIAL_BLOCKS in zoneIds)

        // Coordinate resolution checks across all primary layout zones
        assertEquals(
            com.example.game.world.CityZoneId.CENTRAL_ROUNDABOUT,
            WorldMapData.resolveCityZone(0f, 0f)?.id
        )
        assertEquals(
            com.example.game.world.CityZoneId.COMMERCIAL_SHOPPING_DISTRICT,
            WorldMapData.resolveCityZone(522f, 56f)?.id
        )
        assertEquals(
            com.example.game.world.CityZoneId.RESIDENTIAL_BLOCKS,
            WorldMapData.resolveCityZone(-420f, 310f)?.id
        )
        assertEquals(
            com.example.game.world.CityZoneId.RESIDENTIAL_BLOCKS,
            WorldMapData.resolveCityZone(-115f, 44f)?.id
        )

        // Southern viewing pier keeps player spawn (0, 14) dry at 0f, while pool basin is sunken deep water
        assertEquals(0f, WorldMapData.terrainHeightAt(0f, 14f), 0.01f)
        assertFalse(WorldMapData.isDeepWater(0f, 14f))
        assertTrue(WorldMapData.isDeepWater(15f, 0f))
        assertTrue(WorldMapData.terrainHeightAt(15f, 0f) < -1.0f)

        // Indian National Flag Monument constants
        assertEquals(24, com.example.game.world.CityLandmarksAndModules.FLAG_ASHOKA_CHAKRA_SPOKES)
        assertEquals(0xFFFF9933L, com.example.game.world.CityLandmarksAndModules.FLAG_SAFFRON_HEX)
        assertEquals(0xFFFFFFFFL, com.example.game.world.CityLandmarksAndModules.FLAG_WHITE_HEX)
        assertEquals(0xFF138808L, com.example.game.world.CityLandmarksAndModules.FLAG_GREEN_HEX)
        assertEquals(0xFF000080L, com.example.game.world.CityLandmarksAndModules.FLAG_CHAKRA_NAVY_HEX)

        // Roundabout buildings maintain plaza setback (r >= 72u) and core district buildings placed cleanly
        val generatedIds = com.example.game.world.WorldGenerator.buildings.map { it.id }.toSet()
        assertTrue("m_mall" in generatedIds)
        assertTrue("m_weapon_shop" in generatedIds)
        assertTrue("m_dealership" in generatedIds)
        assertTrue(com.example.game.world.CityLandmarksAndModules.CENTRAL_UPGRADE_STORE_ID in generatedIds)
        assertTrue("res_my_villa" in generatedIds)
        assertTrue("res_heights_1" in generatedIds)
        assertTrue("c_res_blk_1" in generatedIds)
        assertTrue("c_res_blk_4" in generatedIds)

        val centralBuildings = com.example.game.world.WorldGenerator.buildings.filter {
            kotlin.math.hypot(it.x, it.z) < com.example.game.world.CityLandmarksAndModules.ROUNDABOUT_PLAZA_SETBACK_RADIUS
        }
        assertTrue("No buildings should overlap the 72u central roundabout plaza", centralBuildings.isEmpty())
    }

    @Test
    fun stylePricingAndCustomizationCatalog_validStarterFreeAndRupeeTiers() {
        val cat = com.example.data.model.HeroAppearanceCatalog
        assertEquals(4, cat.hairCuts.size)
        assertEquals(5, cat.headgears.size)
        assertEquals(5, cat.weaponSkins.size)
        assertEquals(15, cat.colorSwatches.size)
        assertEquals(4, com.example.data.model.HeroExpression.entries.size)

        // Starter look matching the reference photo is free (₹0) & pre-unlocked
        val starters = cat.starterUnlockedKeys()
        starters.forEach { key -> assertEquals(0, cat.priceForKey(key)) }

        // Paid clothes, hair cuts, hair dyes, gear, and weapon skins start at ₹10+
        val paidOutfitPrices = cat.outfits.map { it.priceRupees }.filter { it > 0 }
        assertTrue(paidOutfitPrices.all { it >= 10 })
        assertEquals(10, paidOutfitPrices.min())
        assertTrue(cat.hairCuts.drop(1).all { it.priceRupees >= 10 })
        val paidHairPrices = cat.hairs.map { it.priceRupees }.filter { it > 0 }
        assertTrue(paidHairPrices.all { it >= 10 })
        assertTrue(cat.headgears.drop(1).all { it.priceRupees >= 10 })
        assertTrue(cat.weaponSkins.drop(1).all { it.priceRupees >= 10 })

        // Crimson Blox character & Central Plaza companion NPC exist with BLOX_FLAME_HOODIE
        val sim = WorldSimulationState()
        assertTrue(sim.npcs.any {
            it.id == "npc_blox_flame" &&
                it.appearance.coatSilhouette == com.example.data.model.CoatSilhouette.BLOX_FLAME_HOODIE
        })
        sim.applyHeroAppearance(
            outfitRaw = "White Storm",
            hairRaw = "Crimson",
            accessoryRaw = "Ninja Mask",
            accentHex = "#00E5FF",
            hairCutRaw = "wolf_cut",
            headgearRaw = "gear_headphones",
            weaponSkinRaw = "wskin_azure",
            expressionRaw = "rage"
        )
        assertEquals("white", sim.heroOutfitId)
        assertEquals("wolf_cut", sim.heroHairCutId)
        assertEquals(com.example.data.model.NpcHairSilhouette.SWEPT_SIDE, sim.heroHairSilhouette)
        assertEquals("gear_headphones", sim.heroHeadgearId)
        assertEquals("wskin_azure", sim.heroWeaponSkinId)
        assertEquals(com.example.data.model.HeroExpression.RAGE, sim.heroExpression)
    }

    @Test
    fun housingAndEconomy_startsWithZeroHousesSupportsBuySellAndVaultStorage() {
        // Every new player starts with 0 owned properties
        assertTrue(com.example.data.model.PlayerProfile().unlockedProperties.isEmpty())
        assertTrue(com.example.ui.GameViewModel.createStarterGuestProperties("guest_zero").isEmpty())

        // Market includes both Land Plots (ज़मीन) and Buyable Villas within 2400x2400 bounds
        val listings = com.example.data.model.GameCatalog.propertyMarketListings
        assertTrue(listings.size >= 6)
        assertTrue(listings.any { it.isLandPlot })
        assertTrue(listings.any { !it.isLandPlot })
        listings.forEach { l ->
            assertTrue(l.sellCoins in 1L until l.buyCoins)
            assertTrue(l.locX in -1200f..1200f)
            assertTrue(l.locZ in -1200f..1200f)
        }
        assertTrue(com.example.game.world.WorldLocations.allLocations.count { it.id.startsWith("loc_plot_") } >= 3)

        // House Vault storage & Weapon Buy/Sell inside WorldSimulationState
        val sim = WorldSimulationState()
        assertFalse(sim.depositWeaponToStash("wpn_fists"))
        assertTrue(sim.depositWeaponToStash("wpn_vortex_ar"))
        assertFalse("wpn_vortex_ar" in sim.ownedWeaponIds)
        assertTrue("wpn_vortex_ar" in sim.houseStashWeapons)
        assertTrue(sim.withdrawWeaponFromStash("wpn_vortex_ar"))
        assertTrue("wpn_vortex_ar" in sim.ownedWeaponIds)

        val initMeds = sim.medkitCount
        assertTrue(sim.depositSupplyToStash(isMedkit = true))
        assertEquals(initMeds - 1, sim.medkitCount)
        assertEquals(1, sim.houseStashMedkits)
        assertTrue(sim.withdrawSupplyFromStash(isMedkit = true))
        assertEquals(initMeds, sim.medkitCount)
        assertEquals(0, sim.houseStashMedkits)

        sim.buyWeaponToBag("wpn_dragon_smg")
        assertTrue("wpn_dragon_smg" in sim.ownedWeaponIds)
        assertEquals("wpn_dragon_smg", sim.activeWeaponId)
        assertTrue(sim.sellWeaponFromBag("wpn_dragon_smg"))
        assertFalse("wpn_dragon_smg" in sim.ownedWeaponIds)
        assertFalse(sim.sellWeaponFromBag("wpn_fists"))
    }

    @Test
    fun expandedWorldAndCombat_covers2400MapKatanaComboAndRedTargetLock() {
        assertEquals(1185f, WorldMapData.WORLD_BOUND, 0.1f)
        assertEquals(-1200f, com.example.game.world.WorldLocations.WORLD_MIN, 0.1f)
        assertEquals(1200f, com.example.game.world.WorldLocations.WORLD_MAX, 0.1f)
        assertEquals(100, com.example.game.world.WorldGenerator.chunkManager.allChunks().size)

        // Indian Flag Monument & nearby Rival Patrols exist at (0, 0, 0)
        val sim = WorldSimulationState()
        val botsNearFlag = sim.npcs.filter {
            it.role == com.example.game.engine.NpcRole.RIVAL_BOT &&
                kotlin.math.hypot(it.x, it.z) < 75f
        }
        assertTrue("Expected Rival Bots patrolling near Central Flag Monument", botsNearFlag.size >= 3)

        // Katana 3-hit combo never fires bullets & cycles 1 -> 2 -> 3
        sim.switchWeapon("wpn_shadow_katana")
        val targetBot = botsNearFlag.first()
        targetBot.x = sim.playerX
        targetBot.z = sim.playerZ + 2.4f
        targetBot.y = WorldMapData.terrainHeightAt(targetBot.x, targetBot.z)
        sim.cameraYaw = 0f // facing +Z toward targetBot
        val hpStart = targetBot.respawnController.health

        sim.fireActiveWeapon()
        assertEquals(2, sim.katanaComboStep)
        assertTrue(sim.projectiles.isEmpty())
        assertTrue(targetBot.respawnController.health < hpStart)

        // Advance simulation & wait past fireRateMs cooldown to chain combo hits 2 -> 3 -> 1
        Thread.sleep(360L)
        repeat(10) { sim.step(0.05f) }
        sim.fireActiveWeapon()
        assertEquals(3, sim.katanaComboStep)
        assertTrue(sim.projectiles.isEmpty())

        Thread.sleep(360L)
        repeat(10) { sim.step(0.05f) }
        sim.fireActiveWeapon()
        assertEquals(1, sim.katanaComboStep)

        // Crosshair target-lock turns red when ray aims at live Rival Bot and clears when pointing at empty sky
        val lockBot = botsNearFlag[1]
        lockBot.x = sim.playerX
        lockBot.z = sim.playerZ + 4.5f
        lockBot.y = WorldMapData.terrainHeightAt(lockBot.x, lockBot.z)
        val camY = sim.playerY + 1.55f
        val botEyeY = lockBot.y + 1.05f
        val dy = botEyeY - camY
        val dz = 4.5f
        val len = kotlin.math.sqrt(dy * dy + dz * dz)
        sim.updateCrosshairRayTarget(
            eyeX = sim.playerX,
            eyeY = camY,
            eyeZ = sim.playerZ,
            rayDirX = 0f,
            rayDirY = dy / len,
            rayDirZ = dz / len
        )
        assertTrue(sim.crosshairLockedOnEnemy)
        assertEquals(lockBot.displayName, sim.crosshairTargetName)

        sim.updateCrosshairRayTarget(
            eyeX = sim.playerX,
            eyeY = camY,
            eyeZ = sim.playerZ,
            rayDirX = 0f,
            rayDirY = 1f,
            rayDirZ = 0f
        )
        assertFalse(sim.crosshairLockedOnEnemy)
        assertEquals(null, sim.crosshairTargetName)
    }

    @Test
    fun npcAi_neverAttacksUnprovokedAndRetaliatesAfterPlayerAttack() {
        val sim = WorldSimulationState()
        val rival = sim.npcs.first { it.role == com.example.game.engine.NpcRole.RIVAL_BOT }
        rival.x = sim.playerX
        rival.z = sim.playerZ + 3.2f
        rival.y = WorldMapData.terrainHeightAt(rival.x, rival.z)

        // Move through multiple attack windows without attacking first: player must stay unharmed
        repeat(40) { sim.step(0.05f) }
        assertFalse(rival.provokedByPlayer)
        assertFalse(rival.state == com.example.game.engine.NpcBehaviorState.ENGAGE)
        assertEquals(sim.maxHp, sim.hp, 0.1f)
        assertEquals(50f, sim.shield, 0.1f)

        // Attack the rival directly: now the rival becomes provoked and counter-attacks
        rival.x = sim.playerX
        rival.z = sim.playerZ + 2.4f
        rival.y = WorldMapData.terrainHeightAt(rival.x, rival.z)
        sim.cameraYaw = 0f
        sim.triggerMeleeAttack()
        assertTrue(rival.provokedByPlayer)
        assertEquals(com.example.game.engine.NpcBehaviorState.ENGAGE, rival.state)

        repeat(40) { sim.step(0.05f) }
        assertTrue("Provoked rival should damage player shield or HP", sim.shield < 50f || sim.hp < sim.maxHp)
    }

    @Test
    fun cameraLockAndNamedSwatches_locksYawToHeroResetsFromLobbyAndNamesEveryColor() {
        val sim = WorldSimulationState()
        sim.showcaseMode = ShowcaseMode.LOBBY
        sim.step(0.04f)
        assertEquals(22f, sim.playerRotY, 0.1f)

        // Entering world from lobby snaps camera & hero straight behind with pitch = 7 deg
        sim.showcaseMode = ShowcaseMode.NONE
        sim.step(0.04f)
        assertEquals(180f, sim.playerRotY, 0.1f)
        assertEquals(180f, sim.cameraYaw, 0.1f)
        assertEquals(7f, sim.cameraPitch, 0.1f)

        // Turning hero via joystick rotates cameraYaw in lockstep with playerRotY
        sim.joyX = 1f
        repeat(10) { sim.step(0.05f) }
        assertTrue(sim.playerRotY != 180f)
        assertEquals(sim.playerRotY, sim.cameraYaw, 0.01f)
        assertEquals(7f, sim.cameraPitch, 0.01f)

        val catalog = com.example.data.model.HeroAppearanceCatalog
        assertEquals(catalog.colorSwatches.size, catalog.namedColorSwatches.size)
        assertEquals(catalog.namedColorSwatches.size, catalog.namedColorSwatches.map { it.name }.toSet().size)
        assertTrue(catalog.namedColorSwatches.all { it.name.isNotBlank() })
    }

    @Test
    fun objMeshLoader_parsesQuadsNormalizesHeightAndRejectsEmptyInput() {
        val cubeObj = """
            v -1.0 2.0 -1.0
            v  1.0 2.0 -1.0
            v  1.0 6.0 -1.0
            v -1.0 6.0 -1.0
            f 1 2 3 4
        """.trimIndent()
        val parsedResult = com.example.game.engine.ObjMeshLoader.parseObjText(cubeObj, "custom_hero.obj")
        assertTrue(parsedResult.isSuccess)
        val parsed = parsedResult.getOrNull()!!
        assertEquals(2, parsed.triangleCount)
        assertEquals(2 * 3 * 6, parsed.vertices.size)
        assertEquals(1.92f, parsed.heightMeters, 0.05f)
        // Lowest vertex Y should be normalized to ground (0f)
        val minY = (0 until parsed.vertices.size / 6).minOf { parsed.vertices[it * 6 + 1] }
        assertEquals(0f, minY, 0.01f)

        assertTrue(com.example.game.engine.ObjMeshLoader.parseObjText("# empty obj", "empty.obj").isFailure)
    }

    @Test
    fun bareHandsToggle_airborneRightSideAimAndKillBannerWorkProperly() {
        val sim = WorldSimulationState()
        assertEquals("wpn_vortex_ar", sim.activeWeaponId)

        // Toggle bare hands puts gun away, second toggle restores previous gun
        sim.toggleBareHands()
        assertEquals("wpn_fists", sim.activeWeaponId)
        sim.toggleBareHands()
        assertEquals("wpn_vortex_ar", sim.activeWeaponId)

        // While airborne (jumping), shooting still fires through custom aim ray toward enemy
        sim.triggerJump()
        repeat(4) { sim.step(0.05f) }
        assertTrue(sim.playerY > 0.2f)
        sim.hasCustomCameraRay = true
        sim.updateCrosshairRayTarget(0f, 1.5f, 12f, 0.08f, -0.02f, -0.99f)
        val enemy = sim.npcs.first { it.role == com.example.game.engine.NpcRole.RIVAL_BOT }
        enemy.x = 1.6f
        enemy.y = 0f
        enemy.z = -8f
        enemy.respawnController.takeDamage(enemy.respawnController.health - 10f)
        sim.fireActiveWeapon()
        assertTrue(sim.projectiles.isNotEmpty())
        val bolt = sim.projectiles.last()
        assertTrue(bolt.vx > 0f) // Aimed right toward right-side ray convergence

        // Defeating 1st & 2nd enemy increments sessionKills and sets overhead kill banner text
        sim.switchWeapon("wpn_fists")
        enemy.x = sim.playerX
        enemy.z = sim.playerZ + 1.8f
        enemy.y = WorldMapData.terrainHeightAt(enemy.x, enemy.z)
        sim.cameraYaw = 0f
        sim.triggerMeleeAttack()
        assertTrue(sim.punchAnimTimer > 0f)
        assertEquals(1, sim.sessionKills)
        assertEquals("1 KILL", sim.killBannerText)

        repeat(10) { sim.step(0.05f) }
        val enemy2 = sim.npcs.first { it.id != enemy.id && it.role == com.example.game.engine.NpcRole.RIVAL_BOT }
        enemy2.respawnController.takeDamage(enemy2.respawnController.health - 5f)
        enemy2.x = sim.playerX
        enemy2.z = sim.playerZ + 1.8f
        enemy2.y = WorldMapData.terrainHeightAt(enemy2.x, enemy2.z)
        sim.triggerMeleeAttack()
        assertEquals(2, sim.sessionKills)
        assertEquals("2 KILLS", sim.killBannerText)
    }

    @Test
    fun playerProfile_includesValidatedGpsCoordinatesInCreateMap() {
        val withoutGps = com.example.data.model.PlayerProfile(
            userId = "u_1",
            playerCode = "123456789"
        ).toCreateMap()
        assertFalse(withoutGps.containsKey("latitude"))
        assertFalse(withoutGps.containsKey("longitude"))
        assertEquals(false, withoutGps["locationTrackingEnabled"])

        val withGps = com.example.data.model.PlayerProfile(
            userId = "u_2",
            playerCode = "987654321",
            latitude = 28.6139,
            longitude = 77.2090,
            locationAccuracyMeters = 14.5,
            locationTrackingEnabled = true
        ).toCreateMap()
        assertEquals(28.6139, withGps["latitude"])
        assertEquals(77.2090, withGps["longitude"])
        assertEquals(14.5, withGps["locationAccuracyMeters"])
        assertEquals(true, withGps["locationTrackingEnabled"])
    }
}
