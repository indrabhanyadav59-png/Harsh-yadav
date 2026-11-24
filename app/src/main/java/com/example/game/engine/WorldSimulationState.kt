package com.example.game.engine

import com.example.data.model.GameCatalog
import com.example.data.model.GraphicsPreset
import com.example.data.model.HeroAppearanceCatalog
import com.example.data.model.HeroExpression
import com.example.data.model.NpcHairSilhouette
import com.example.data.model.PlayerProfile
import com.example.data.model.TimeOfDayMode
import com.example.data.model.WeatherMode
import com.example.game.systems.WorldNavigation
import com.example.game.world.WorldLocation
import com.example.game.world.WorldLocations
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

import com.example.data.model.WeaponSpec
import com.example.data.model.WeaponType

data class EnergyProjectile(
    val id: Long,
    var x: Float,
    var y: Float,
    var z: Float,
    val vx: Float,
    val vy: Float,
    val vz: Float,
    val damage: Float,
    val colorInt: Int,
    var lifeSec: Float = 1.4f
)

data class GroundWeaponPickup(
    val uid: String,
    val weaponId: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val ammoInMag: Int,
    val reserveAmmo: Int,
    var pickupGraceSec: Float = 0f
)

data class ThrownGrenade(
    val id: Long,
    var x: Float,
    var y: Float,
    var z: Float,
    var vx: Float,
    var vy: Float,
    var vz: Float,
    var fuseSec: Float = 1.35f
)

data class ShockwaveEffect(
    val x: Float,
    val y: Float,
    val z: Float,
    val colorInt: Int,
    var radius: Float = 0.8f,
    var lifeSec: Float = 0.55f
)

data class WeatherParticle(
    var x: Float,
    var y: Float,
    var z: Float,
    val speedY: Float,
    val driftX: Float
)

data class DamageFeedbackEvent(
    val damage: Int,
    val targetName: String,
    val isCritical: Boolean = false,
    val seq: Long = 0L
)

data class NearbyInteraction(
    val kind: String, // "VEHICLE", "EXIT_VEH", "NPC", "HOUSE", "EXIT_HOUSE", "ALARM_THEFT", "BUY_PLOT"
    val id: String,
    val label: String,
    val subLabel: String
)

data class ProjectedSignboard(
    val id: String,
    val title: String,
    val subtitle: String,
    val screenX: Float,
    val screenY: Float,
    val distanceMeters: Int,
    val accentHex: Long,
    val isOwnedProperty: Boolean = false
)

data class WorldHudSnapshot(
    val x: Float = WorldLocations.playerSpawn.x,
    val y: Float = WorldLocations.playerSpawn.y,
    val z: Float = WorldLocations.playerSpawn.z,
    val yawDeg: Float = 180f,
    val district: String = WorldLocations.playerSpawn.district.displayName,
    val hp: Float = 200f,
    val maxHp: Float = 200f,
    val ep: Float = 200f,
    val maxEp: Float = 200f,
    val shield: Float = 50f,
    val maxShield: Float = 100f,
    val activeWeaponId: String = "wpn_vortex_ar",
    val weaponType: WeaponType = WeaponType.FIREARM,
    val ownedWeaponIds: List<String> = listOf("wpn_vortex_ar", "wpn_pulse_pistol", "wpn_shadow_katana", "wpn_fists"),
    val ammoInMag: Int = 42,
    val reserveAmmo: Int = 120,
    val reloading: Boolean = false,
    val sprinting: Boolean = false,
    val crouching: Boolean = false,
    val proning: Boolean = false,
    val aiming: Boolean = false,
    val defending: Boolean = false,
    val meleeCooldown: Float = 0f,
    val heavyAttackCooldownSec: Float = 0f,
    val medkitCount: Int = 5,
    val healCooldownSec: Float = 0f,
    val grenadeCount: Int = 3,
    val grenadeCooldownSec: Float = 0f,
    val petSummoned: Boolean = true,
    val isDefeated: Boolean = false,
    val respawnSecondsLeft: Int = 0,
    val drivingVehicleName: String? = null,
    val vehicleSpeedKmh: Int = 0,
    val insideHouseName: String? = null,
    val nearbyInteraction: NearbyInteraction? = null,
    val nearbyVehicleInteraction: NearbyInteraction? = null,
    val nearbyGroundWeapon: GroundWeaponPickup? = null,
    val abilityCooldowns: List<Float> = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
    val inspectedNpc: NpcEntity? = null,
    val lastDamagePopup: DamageFeedbackEvent? = null,
    val gpsDestinationName: String? = null,
    val gpsDistanceMeters: Int = 0,
    val petEvolutionLevel: Int = 1,
    val petRadarActive: Boolean = false,
    val petRadarRadius: Float = 0f,
    val lootRadarActive: Boolean = false,
    val activePetBuffLabel: String? = null,
    val nearbyWildPet: WildWorldPet? = null,
    val crosshairLockedOnEnemy: Boolean = false,
    val crosshairTargetName: String? = null,
    val katanaComboStep: Int = 1,
    val projectedSignboards: List<ProjectedSignboard> = emptyList(),
    val nearbyBuyableListing: com.example.data.model.PropertyMarketListing? = null,
    val houseStashWeapons: List<String> = emptyList(),
    val houseStashMedkits: Int = 0,
    val houseStashGrenades: Int = 0,
    val sessionKills: Int = 0,
    val killBannerText: String? = null,
    val heroOverheadScreenX: Float = 0.44f,
    val heroOverheadScreenY: Float = 0.25f,
    val customObjName: String? = null
)

enum class ShowcaseMode {
    NONE,
    SPLASH_LOADING,
    LOGIN,
    LOBBY,
    CHARACTER_PREVIEW
}

class WorldSimulationState {
    // Player transform & motion (spawns at Sakura Torii Plaza, Central Anime City)
    var playerX = WorldLocations.playerSpawn.x
    var playerY = WorldLocations.playerSpawn.y
    var playerZ = WorldLocations.playerSpawn.z
    var playerRotY = 180f
    var velocityY = 0f
    var stridePhase = 0f
    var crouching = false
    var proning = false
    var sprinting = false
    var aiming = false
    var defending = false
    var recoilKick = 0f

    // Camera orbit
    var cameraYaw = 180f
    var cameraPitch = 7f

    // Player combat stats
    var hp = 200f
    val maxHp = 200f
    var ep = 200f
    val maxEp = 200f
    var shield = 50f
    val maxShield = 100f
    var isDefeated = false
    var respawnTimerSec = 0f
    var meleeCooldown = 0f
    var heavyAttackCooldownSec = 0f
    var medkitCount = 5
    var healCooldownSec = 0f
    var grenadeCount = 3
    var grenadeCooldownSec = 0f
    @Volatile var isFiring = false

    // Weapon state & per-weapon ammo tracking
    val ownedWeaponIds = mutableListOf("wpn_vortex_ar", "wpn_pulse_pistol", "wpn_shadow_katana", "wpn_fists")
    private val weaponMagMap = mutableMapOf(
        "wpn_vortex_ar" to 42,
        "wpn_pulse_pistol" to 15,
        "wpn_dragon_smg" to 50
    )
    private val weaponReserveMap = mutableMapOf(
        "wpn_vortex_ar" to 120,
        "wpn_pulse_pistol" to 90,
        "wpn_dragon_smg" to 200
    )
    var activeWeaponId = "wpn_vortex_ar"
    var ammoInMag = 42
    var reserveAmmo = 120
    var reloadTimer = 0f
    private var lastFireMs = 0L

    // Katana 3-hit combo ("खाच-खाच-खाच"), bare-hands punch & right-side Crosshair target-lock + House stash
    @Volatile var katanaComboStep = 1
    @Volatile var katanaSwingTimer = 0f
    @Volatile var punchAnimTimer = 0f
    @Volatile var punchHandSign = 1f
    @Volatile var crosshairLockedOnEnemy = false
    @Volatile var crosshairTargetName: String? = null
    @Volatile var hasCustomCameraRay = false
    var aimRayOriginX = 0f
    var aimRayOriginY = 0f
    var aimRayOriginZ = 0f
    var aimRayDirX = 0f
    var aimRayDirY = 0f
    var aimRayDirZ = -1f
    private var lastNonFistsWeaponId = "wpn_vortex_ar"
    @Volatile var sessionKills = 0
    @Volatile var killBannerText: String? = null
    private var killBannerTimerSec = 0f
    @Volatile var heroOverheadScreenX = 0.44f
    @Volatile var heroOverheadScreenY = 0.25f
    @Volatile var customObjModel: ParsedObjModel? = null
    @Volatile var customObjUploadSeq = 0L
    @Volatile var projectedSignboards: List<ProjectedSignboard> = emptyList()
    @Volatile var ownedPropertyIds: Set<String> = emptySet()
    val houseStashWeapons = mutableListOf<String>()
    var houseStashMedkits = 0
    var houseStashGrenades = 0

    // Driving & House Interior
    var drivingVehicle: ParkedWorldVehicle? = null
    var vehicleSpeed = 0f
    var vehicleWheelAngle = 0f
    @Volatile var driveThrottle = 0f
    var insideHouse: WorldBuilding? = null

    // Pet companion transform & 50-pet runtime engine
    var petX = WorldLocations.playerSpawn.x + 2.2f
    var petY = 0.2f
    var petZ = WorldLocations.playerSpawn.z + 2.0f
    var petRotY = 180f
    var petType = "Fox"
    var petBobPhase = 0f
    var petSummoned = true
    var petEvolutionLevel = 1
    var petCustomTintHex = ""
    var petAuraStyle = "Classic"
    val petController = PetSimulationController()
    val wildPets = PetSimulationController.createInitialWildPets()

    fun effectiveMaxHp(): Float = maxHp * (1f + petController.cappedPassives.maxHpBonusPct)
    fun effectiveMaxShield(): Float = maxShield * (1f + petController.cappedPassives.maxShieldBonusPct)

    // Modular Hero Appearance state
    var heroOutfitId = "shadow"
    var heroHairId = "black"
    var heroHairCutId = "spiky_hero"
    var heroHairSilhouette = NpcHairSilhouette.SPIKY_CROWN
    var heroAccessoryId = "katana"
    var heroHeadgearId = "gear_none"
    var heroWeaponSkinId = "wskin_crimson"
    var heroExpression = HeroExpression.FIERCE
    var hairColorInt = 0xFF161422.toInt()
    var hairHighlightColorInt = 0xFF2B2840.toInt()
    var coatColorInt = 0xFF16141F.toInt()
    var coatLiningColorInt = 0xFFAE1624.toInt()
    var accentColorInt = 0xFFE2263A.toInt()

    // Environment
    var timeOfDay = TimeOfDayMode.NOON
    var weatherMode = WeatherMode.SAKURA
    var graphicsPreset = GraphicsPreset.HIGH
    var worldTimeSec = 0f

    // Gameplay Control & Sensitivity preferences
    var targetFps = 60
    var cameraSensitivity = 1.0f
    var aimSensitivity = 0.85f
    var scopeSensitivity = 0.65f
    var joystickSensitivity = 1.0f
    var autoSprint = false
    var aimAssist = true
    var cameraDistanceScale = 1.0f
    var vibrationEnabled = true
    var autoPickup = true
    var leftHandedControls = false

    // Multiplayer & PvP tracking
    var localPlayerUid: String = ""
    @Volatile var onlinePlayerTargets: List<PlayerProfile> = emptyList()
    @Volatile var protectedTeammateUids: Set<String> = emptySet()
    @Volatile var activeBountyTargetUids: Set<String> = emptySet()
    private val onlineTargetHp = mutableMapOf<String, Float>()

    // GPS Navigation state
    @Volatile var gpsDestination: WorldLocation? = null
    @Volatile var gpsPath: List<Vec3> = emptyList()

    // Startup / Login / Lobby / Customizer 3D Showcase staging
    @Volatile var showcaseMode = ShowcaseMode.NONE
    @Volatile var showcaseVehicleModel = "Kitsune GT-R"
    @Volatile var showcaseVehicleCategory = "supercar"
    @Volatile var showcaseVehicleColorInt = 0xFFFF1E42.toInt()
    @Volatile var showcaseDragYawOffset = 0f
    @Volatile var previewYawOffset = 0f

    // Virtual Joystick + Keyboard WASD input [-1..1]
    @Volatile var joyX = 0f
    @Volatile var joyY = 0f
    @Volatile var keyX = 0f
    @Volatile var keyY = 0f

    // World entities
    val npcs = NpcAiSystem.createInitialNpcs()
    val traffic = NpcAiSystem.createInitialTraffic()
    val parkedVehicles = WorldMapData.parkedVehicles.toMutableList()
    val projectiles = mutableListOf<EnergyProjectile>()
    val activeGrenades = mutableListOf<ThrownGrenade>()
    val groundWeapons = createInitialGroundWeapons()
    val shockwaves = mutableListOf<ShockwaveEffect>()
    val weatherParticles = MutableList(180) {
        WeatherParticle(
            x = (Random.nextFloat() - 0.5f) * 64f,
            y = Random.nextFloat() * 24f,
            z = (Random.nextFloat() - 0.5f) * 64f,
            speedY = 2.5f + Random.nextFloat() * 3.5f,
            driftX = (Random.nextFloat() - 0.5f) * 2.2f
        )
    }

    // Damage popup feedback
    @Volatile var lastDamageFeedback: DamageFeedbackEvent? = null
    private var damagePopupTimerSec = 0f
    private var damageSeq = 1L

    // Ability cooldowns for up to 8 loadout slots (Character + Pet skills)
    val abilityCooldowns = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)

    // Callbacks into ViewModel / Repository
    var onNpcDefeated: ((NpcEntity) -> Unit)? = null
    var onPlayerDefeated: (() -> Unit)? = null
    var onOnlineTargetHit: ((PlayerProfile, Float, Boolean) -> Unit)? = null
    var onDistrictEntered: ((String) -> Unit)? = null
    var onCombatEventBanner: ((String) -> Unit)? = null
    var onHapticPulse: (() -> Unit)? = null
    var onWildPetAdopted: ((WildWorldPet) -> Unit)? = null
    var onPetBonusCoins: ((Long) -> Unit)? = null

    private var lastDistrict = WorldLocations.playerSpawn.district.displayName
    private var projectileSeq = 1L
    private var groundWeaponSeq = 1L
    private var botAttackAccumulatorSec = 0f
    private var needsGameplayCameraSync = false

    private fun registerEnemyKill() {
        sessionKills += 1
        killBannerText = if (sessionKills == 1) "1 KILL" else "$sessionKills KILLS"
        killBannerTimerSec = 2.8f
        com.example.audio.IndraAudioEngine.playKillConfirm(sessionKills)
    }

    private fun provokeNpcByPlayer(npc: NpcEntity, defeated: Boolean) {
        if (defeated) {
            npc.provokedByPlayer = false
            npc.provokedTimerSec = 0f
            npc.state = NpcBehaviorState.DEFEATED
            npc.statusBubble = "Defeated! Respawning in 5s..."
            registerEnemyKill()
            petController.onEnemyEliminated(this)?.let { onCombatEventBanner?.invoke(it) }
            onNpcDefeated?.invoke(npc)
        } else {
            npc.provokedByPlayer = true
            npc.provokedTimerSec = 25f
            npc.state = NpcBehaviorState.ENGAGE
            npc.pathWaypoints = emptyList()
            npc.targetX = playerX
            npc.targetZ = playerZ
            npc.statusBubble = "Retaliating!"
        }
    }

    fun activeWeaponSpec(): WeaponSpec =
        GameCatalog.weapons.firstOrNull { it.id == activeWeaponId } ?: GameCatalog.weapons.first()

    fun applyHeroAppearance(
        outfitRaw: String?,
        hairRaw: String?,
        accessoryRaw: String?,
        accentHex: String? = null,
        hairCutRaw: String? = heroHairCutId,
        headgearRaw: String? = heroHeadgearId,
        weaponSkinRaw: String? = heroWeaponSkinId,
        expressionRaw: String? = heroExpression.id
    ) {
        val outfit = HeroAppearanceCatalog.resolveOutfit(outfitRaw)
        val hair = HeroAppearanceCatalog.resolveHair(hairRaw)
        val hairCut = HeroAppearanceCatalog.resolveHairCut(hairCutRaw)
        val accessory = HeroAppearanceCatalog.resolveAccessory(accessoryRaw)
        val headgear = HeroAppearanceCatalog.resolveHeadgear(headgearRaw)
        val wskin = HeroAppearanceCatalog.resolveWeaponSkin(weaponSkinRaw)
        val expr = HeroAppearanceCatalog.resolveExpression(expressionRaw)

        heroOutfitId = outfit.id
        heroHairId = hair.id
        heroHairCutId = hairCut.id
        heroHairSilhouette = hairCut.silhouette
        heroAccessoryId = accessory.id
        heroHeadgearId = headgear.id
        heroWeaponSkinId = wskin.id
        heroExpression = expr
        coatColorInt = outfit.coatColorInt
        coatLiningColorInt = outfit.liningColorInt
        hairColorInt = hair.colorInt
        hairHighlightColorInt = hair.highlightColorInt
        accentColorInt = parseAccentColor(accentHex, parseAccentColor(outfit.defaultAccentHex, 0xFFE2263A.toInt()))
    }

    private fun parseAccentColor(hex: String?, fallback: Int): Int {
        if (hex.isNullOrBlank()) return fallback
        return try {
            android.graphics.Color.parseColor(hex)
        } catch (_: Exception) {
            fallback
        }
    }

    fun setGpsTarget(location: WorldLocation?) {
        gpsDestination = location
        gpsPath = if (location != null) {
            WorldNavigation.findPath(playerX, playerZ, location.x, location.z)
        } else {
            emptyList()
        }
    }

    fun switchWeapon(weaponId: String, addToOwnedIfMissing: Boolean = true) {
        val spec = GameCatalog.weapons.firstOrNull { it.id == weaponId } ?: return
        // Save current firearm ammo before switching
        if (activeWeaponSpec().weaponType.isFirearm) {
            weaponMagMap[activeWeaponId] = ammoInMag
            weaponReserveMap[activeWeaponId] = reserveAmmo
        }
        if (spec.id != "wpn_fists") {
            lastNonFistsWeaponId = spec.id
        }
        if (addToOwnedIfMissing && weaponId !in ownedWeaponIds) {
            ownedWeaponIds.add(weaponId)
        }
        activeWeaponId = spec.id
        if (spec.weaponType.isFirearm) {
            ammoInMag = weaponMagMap[spec.id] ?: spec.magSize
            reserveAmmo = weaponReserveMap[spec.id] ?: spec.reserveMax
        } else {
            ammoInMag = 0
            reserveAmmo = 0
            aiming = false
        }
        reloadTimer = 0f
    }

    fun toggleBareHands() {
        if (activeWeaponId != "wpn_fists") {
            switchWeapon("wpn_fists")
        } else {
            val restoreId = if (lastNonFistsWeaponId in ownedWeaponIds) lastNonFistsWeaponId else {
                ownedWeaponIds.firstOrNull { it != "wpn_fists" } ?: "wpn_vortex_ar"
            }
            switchWeapon(restoreId)
        }
    }

    fun dropActiveWeapon(): GroundWeaponPickup? {
        val spec = activeWeaponSpec()
        if (spec.weaponType == WeaponType.FISTS) {
            onCombatEventBanner?.invoke("Cannot drop unarmed fists!")
            return null
        }
        val rad = Math3D.degToRad(playerRotY)
        val dropX = (playerX + sin(rad) * 1.8f).coerceIn(-WorldMapData.WORLD_BOUND, WorldMapData.WORLD_BOUND)
        val dropZ = (playerZ + cos(rad) * 1.8f).coerceIn(-WorldMapData.WORLD_BOUND, WorldMapData.WORLD_BOUND)
        val dropY = if (insideHouse != null) 0f else WorldMapData.terrainHeightAt(dropX, dropZ)
        val pickup = GroundWeaponPickup(
            uid = "drop_${groundWeaponSeq++}",
            weaponId = spec.id,
            x = dropX,
            y = dropY,
            z = dropZ,
            ammoInMag = if (spec.weaponType.isFirearm) ammoInMag else 0,
            reserveAmmo = if (spec.weaponType.isFirearm) reserveAmmo else 0,
            pickupGraceSec = 2.6f
        )
        groundWeapons.add(pickup)
        ownedWeaponIds.removeAll { it == spec.id }
        if ("wpn_fists" !in ownedWeaponIds) ownedWeaponIds.add("wpn_fists")
        val nextId = ownedWeaponIds.firstOrNull { it != "wpn_fists" } ?: "wpn_fists"
        activeWeaponId = nextId
        val nextSpec = activeWeaponSpec()
        if (nextSpec.weaponType.isFirearm) {
            ammoInMag = weaponMagMap[nextId] ?: nextSpec.magSize
            reserveAmmo = weaponReserveMap[nextId] ?: nextSpec.reserveMax
        } else {
            ammoInMag = 0
            reserveAmmo = 0
        }
        aiming = false
        reloadTimer = 0f
        onCombatEventBanner?.invoke("Dropped ${spec.name} • Switched to ${nextSpec.name}")
        return pickup
    }

    fun pickupNearbyWeapon(maxDist: Float = 4.5f): GroundWeaponPickup? {
        val pickup = findNearestGroundWeapon(maxDist, ignoreGrace = true) ?: return null
        val spec = GameCatalog.weapons.firstOrNull { it.id == pickup.weaponId } ?: return null
        groundWeapons.remove(pickup)
        if (spec.id !in ownedWeaponIds) {
            val fistsIdx = ownedWeaponIds.indexOf("wpn_fists")
            if (fistsIdx >= 0) ownedWeaponIds.add(fistsIdx, spec.id) else ownedWeaponIds.add(spec.id)
        }
        if (spec.weaponType.isFirearm) {
            val prevMag = weaponMagMap[spec.id] ?: 0
            val prevRes = weaponReserveMap[spec.id] ?: 0
            weaponMagMap[spec.id] = maxOf(prevMag, pickup.ammoInMag).coerceAtMost(spec.magSize)
            weaponReserveMap[spec.id] = (prevRes + pickup.reserveAmmo).coerceAtLeast(pickup.reserveAmmo)
        }
        switchWeapon(spec.id, addToOwnedIfMissing = false)
        shockwaves.add(ShockwaveEffect(pickup.x, pickup.y + 0.4f, pickup.z, spec.boltColorHex.toInt(), 0.6f, 0.35f))
        com.example.audio.IndraAudioEngine.playPickup()
        onCombatEventBanner?.invoke("Picked up ${spec.name}!")
        return pickup
    }

    fun buyWeaponToBag(weaponId: String) {
        val spec = GameCatalog.weapons.firstOrNull { it.id == weaponId } ?: return
        if (weaponId !in ownedWeaponIds) {
            val fistsIdx = ownedWeaponIds.indexOf("wpn_fists")
            if (fistsIdx >= 0) ownedWeaponIds.add(fistsIdx, weaponId) else ownedWeaponIds.add(weaponId)
        }
        if (spec.weaponType.isFirearm) {
            weaponMagMap[spec.id] = spec.magSize
            weaponReserveMap[spec.id] = spec.reserveMax
        }
        switchWeapon(spec.id, addToOwnedIfMissing = false)
    }

    fun sellWeaponFromBag(weaponId: String): Boolean {
        if (weaponId == "wpn_fists" || weaponId !in ownedWeaponIds) return false
        ownedWeaponIds.removeAll { it == weaponId }
        if ("wpn_fists" !in ownedWeaponIds) ownedWeaponIds.add("wpn_fists")
        if (activeWeaponId == weaponId) {
            val nextId = ownedWeaponIds.firstOrNull { it != "wpn_fists" } ?: "wpn_fists"
            switchWeapon(nextId, addToOwnedIfMissing = false)
        }
        return true
    }

    fun depositWeaponToStash(weaponId: String): Boolean {
        if (weaponId == "wpn_fists" || weaponId !in ownedWeaponIds) return false
        ownedWeaponIds.removeAll { it == weaponId }
        houseStashWeapons.add(weaponId)
        if ("wpn_fists" !in ownedWeaponIds) ownedWeaponIds.add("wpn_fists")
        if (activeWeaponId == weaponId) {
            val nextId = ownedWeaponIds.firstOrNull { it != "wpn_fists" } ?: "wpn_fists"
            switchWeapon(nextId, addToOwnedIfMissing = false)
        }
        com.example.audio.IndraAudioEngine.playPickup()
        return true
    }

    fun withdrawWeaponFromStash(weaponId: String): Boolean {
        val idx = houseStashWeapons.indexOf(weaponId)
        if (idx < 0) return false
        houseStashWeapons.removeAt(idx)
        if (weaponId !in ownedWeaponIds) {
            val fistsIdx = ownedWeaponIds.indexOf("wpn_fists")
            if (fistsIdx >= 0) ownedWeaponIds.add(fistsIdx, weaponId) else ownedWeaponIds.add(weaponId)
        }
        switchWeapon(weaponId, addToOwnedIfMissing = false)
        com.example.audio.IndraAudioEngine.playPickup()
        return true
    }

    fun depositSupplyToStash(isMedkit: Boolean): Boolean {
        return if (isMedkit) {
            if (medkitCount <= 0) return false
            medkitCount -= 1
            houseStashMedkits += 1
            true
        } else {
            if (grenadeCount <= 0) return false
            grenadeCount -= 1
            houseStashGrenades += 1
            true
        }
    }

    fun withdrawSupplyFromStash(isMedkit: Boolean): Boolean {
        return if (isMedkit) {
            if (houseStashMedkits <= 0) return false
            houseStashMedkits -= 1
            medkitCount += 1
            true
        } else {
            if (houseStashGrenades <= 0) return false
            houseStashGrenades -= 1
            grenadeCount += 1
            true
        }
    }

    fun findNearestGroundWeapon(maxDist: Float = 4.5f, ignoreGrace: Boolean = true): GroundWeaponPickup? =
        groundWeapons
            .filter { ignoreGrace || it.pickupGraceSec <= 0f }
            .minByOrNull { (it.x - playerX) * (it.x - playerX) + (it.z - playerZ) * (it.z - playerZ) }
            ?.takeIf {
                val dx = it.x - playerX
                val dz = it.z - playerZ
                dx * dx + dz * dz <= maxDist * maxDist
            }

    fun toggleCrouch() {
        crouching = !crouching
        if (crouching) {
            proning = false
        }
    }

    fun toggleProne() {
        proning = !proning
        if (proning) {
            crouching = false
            sprinting = false
            aiming = false
        }
    }

    fun triggerHeal(): Boolean {
        if (isDefeated || medkitCount <= 0 || healCooldownSec > 0f) return false
        if (hp >= maxHp) {
            onCombatEventBanner?.invoke("HP is already full!")
            return false
        }
        medkitCount -= 1
        healCooldownSec = 4.0f
        hp = (hp + 75f).coerceAtMost(maxHp)
        shockwaves.add(ShockwaveEffect(playerX, playerY + 0.5f, playerZ, 0xFF22C55E.toInt(), 0.9f, 0.55f))
        onCombatEventBanner?.invoke("Medkit used! +75 HP (${medkitCount} left)")
        return true
    }

    fun throwGrenade(): Boolean {
        if (isDefeated || grenadeCount <= 0 || grenadeCooldownSec > 0f || drivingVehicle != null) return false
        grenadeCount -= 1
        grenadeCooldownSec = 2.4f
        val rad = Math3D.degToRad(cameraYaw)
        val dirX = sin(rad)
        val dirZ = cos(rad)
        playerRotY = cameraYaw
        recoilKick = 0.65f
        activeGrenades.add(
            ThrownGrenade(
                id = projectileSeq++,
                x = playerX + dirX * 0.9f,
                y = playerY + 1.55f,
                z = playerZ + dirZ * 0.9f,
                vx = dirX * 19f,
                vy = 7.2f,
                vz = dirZ * 19f,
                fuseSec = 1.25f
            )
        )
        onCombatEventBanner?.invoke("Frag Grenade thrown! (${grenadeCount} left)")
        return true
    }

    fun triggerSwordHeavy(): Boolean {
        if (isDefeated || heavyAttackCooldownSec > 0f || drivingVehicle != null) return false
        val spec = activeWeaponSpec()
        heavyAttackCooldownSec = 3.2f
        recoilKick = 1.0f
        val rad = Math3D.degToRad(cameraYaw)
        val dirX = sin(rad)
        val dirZ = cos(rad)
        playerRotY = cameraYaw
        val (nx, nz) = PhysicsWorld.resolveCapsuleWorld(
            x = playerX + dirX * 4.5f,
            y = playerY,
            z = playerZ + dirZ * 4.5f,
            radius = 0.7f,
            insideHouse = insideHouse,
            parkedVehicles = parkedVehicles,
            traffic = traffic
        )
        playerX = nx
        playerZ = nz
        playerY = if (insideHouse != null) 0f else WorldMapData.terrainHeightAt(nx, nz)
        shockwaves.add(ShockwaveEffect(playerX, playerY + 0.9f, playerZ, spec.boltColorHex.toInt(), 1.5f, 0.5f))
        applyAreaDamage(playerX + dirX * 1.4f, playerZ + dirZ * 1.4f, 6.2f, spec.damage + 28f)
        tryHitOnlinePlayersAlongRay(dirX, dirZ, 7.0f, spec.damage + 22f)
        onHapticPulse?.invoke()
        val moveTitle = if (spec.weaponType == WeaponType.KATANA) "Iaido Sword Dash" else "Chakra Heavy Slam"
        onCombatEventBanner?.invoke("$moveTitle!")
        return true
    }

    fun triggerReload() {
        val spec = activeWeaponSpec()
        if (!spec.weaponType.isFirearm) return
        if (reloadTimer > 0f || ammoInMag >= spec.magSize || reserveAmmo <= 0) return
        reloadTimer = petController.effectiveReloadDuration(1.2f)
        com.example.audio.IndraAudioEngine.playReload()
    }

    fun triggerInstantMagTopUp() {
        val spec = activeWeaponSpec()
        if (!spec.weaponType.isFirearm) return
        val needed = spec.magSize - ammoInMag
        val take = needed.coerceAtMost(reserveAmmo)
        ammoInMag += take
        reserveAmmo -= take
        reloadTimer = 0f
        com.example.audio.IndraAudioEngine.playReload()
    }

    fun triggerJump() {
        if (isDefeated) return
        if (drivingVehicle != null) {
            if (drivingVehicle?.category == "helicopter") {
                playerY = (playerY + 3.0f).coerceAtMost(58f)
            }
            return
        }
        crouching = false
        proning = false
        val groundY = if (insideHouse != null) 0f else WorldMapData.terrainHeightAt(playerX, playerZ)
        if (playerY <= groundY + 0.15f) {
            velocityY = 8.8f
            com.example.audio.IndraAudioEngine.playJump()
        }
    }

    fun triggerMeleeAttack() {
        if (isDefeated || meleeCooldown > 0f || drivingVehicle != null) return
        if (activeWeaponSpec().weaponType == WeaponType.KATANA) {
            performKatanaComboStrike()
            return
        }
        meleeCooldown = 0.42f
        punchAnimTimer = 0.34f
        punchHandSign = -punchHandSign
        recoilKick = 0.9f
        val rad = Math3D.degToRad(cameraYaw)
        val dirX = sin(rad)
        val dirZ = cos(rad)
        playerRotY = cameraYaw
        com.example.audio.IndraAudioEngine.playPunch()

        shockwaves.add(
            ShockwaveEffect(
                x = playerX + dirX * 1.8f,
                y = playerY + 1.0f,
                z = playerZ + dirZ * 1.8f,
                colorInt = accentColorInt,
                radius = 0.9f,
                lifeSec = 0.35f
            )
        )

        val meleeRange = 4.4f
        var hitAny = false
        for (npc in npcs) {
            if (!npc.respawnController.collidersEnabled) continue
            val dx = npc.x - playerX
            val dz = npc.z - playerZ
            val distSq = dx * dx + dz * dz
            if (distSq > meleeRange * meleeRange) continue
            val dist = sqrt(distSq).coerceAtLeast(0.01f)
            val dot = (dx / dist) * dirX + (dz / dist) * dirZ
            if (dot < -0.1f) continue
            if (!PhysicsWorld.hasLineOfSight(playerX, playerY + 1.2f, playerZ, npc.x, npc.y + 1.2f, npc.z, insideHouse)) {
                continue
            }
            val dmg = if (dist < 2.3f) 60f else 42f
            recordDamageFeedback(dmg.toInt(), npc.displayName, isCritical = dist < 2.3f)
            val defeated = npc.respawnController.takeDamage(dmg)
            provokeNpcByPlayer(npc, defeated)
            hitAny = true
            break
        }
        if (!hitAny) {
            hitAny = tryHitOnlinePlayersAlongRay(dirX, dirZ, meleeRange, 40f)
        }
        if (hitAny) {
            com.example.audio.IndraAudioEngine.playHitImpact(isBlade = false)
        }
        onHapticPulse?.invoke()
    }

    private fun performKatanaComboStrike() {
        val spec = activeWeaponSpec()
        katanaComboStep = (katanaComboStep % 3) + 1
        katanaSwingTimer = 0.34f
        meleeCooldown = 0.24f
        recoilKick = 0.85f
        val rad = Math3D.degToRad(cameraYaw)
        val dirX = sin(rad)
        val dirZ = cos(rad)
        playerRotY = cameraYaw

        // Slight anime step-in lunge on swing
        val (nx, nz) = PhysicsWorld.resolveCapsuleWorld(
            x = playerX + dirX * 1.15f,
            y = playerY,
            z = playerZ + dirZ * 1.15f,
            radius = 0.7f,
            insideHouse = insideHouse,
            parkedVehicles = parkedVehicles,
            traffic = traffic
        )
        playerX = nx
        playerZ = nz
        playerY = if (insideHouse != null) 0f else WorldMapData.terrainHeightAt(nx, nz)

        com.example.audio.IndraAudioEngine.playKatanaSlash(katanaComboStep)
        shockwaves.add(
            ShockwaveEffect(
                x = playerX + dirX * 2.0f,
                y = playerY + 1.05f,
                z = playerZ + dirZ * 2.0f,
                colorInt = spec.boltColorHex.toInt(),
                radius = 1.25f + katanaComboStep * 0.25f,
                lifeSec = 0.34f
            )
        )

        val atkMult = petController.effectiveAttackMultiplier()
        val comboBonus = if (katanaComboStep == 3) 1.35f else 1.0f + (katanaComboStep - 1) * 0.12f
        val dmg = spec.damage * atkMult * comboBonus
        val slashRange = 5.6f
        var hitEnemy = false
        for (npc in npcs) {
            if (!npc.respawnController.collidersEnabled) continue
            val dx = npc.x - playerX
            val dz = npc.z - playerZ
            val distSq = dx * dx + dz * dz
            if (distSq > slashRange * slashRange) continue
            val dist = sqrt(distSq).coerceAtLeast(0.01f)
            val dot = (dx / dist) * dirX + (dz / dist) * dirZ
            if (katanaComboStep < 3 && dot < 0.05f && dist > 1.8f) continue
            if (!PhysicsWorld.hasLineOfSight(playerX, playerY + 1.2f, playerZ, npc.x, npc.y + 1.2f, npc.z, insideHouse)) {
                continue
            }
            hitEnemy = true
            recordDamageFeedback(dmg.toInt(), npc.displayName, isCritical = katanaComboStep == 3)
            val defeated = npc.respawnController.takeDamage(dmg)
            provokeNpcByPlayer(npc, defeated)
        }
        if (tryHitOnlinePlayersAlongRay(dirX, dirZ, slashRange, dmg)) {
            hitEnemy = true
        }
        if (hitEnemy) {
            com.example.audio.IndraAudioEngine.playHitImpact(isBlade = true)
        }
        onHapticPulse?.invoke()
    }

    fun fireActiveWeapon() {
        if (isDefeated || reloadTimer > 0f) return
        val spec = activeWeaponSpec()
        val now = System.currentTimeMillis()
        if (now - lastFireMs < spec.fireRateMs) return
        lastFireMs = now

        if (spec.weaponType == WeaponType.KATANA) {
            performKatanaComboStrike()
            return
        }

        if (spec.weaponType == WeaponType.FISTS) {
            triggerMeleeAttack()
            return
        }

        if (ammoInMag <= 0) {
            triggerReload()
            return
        }
        ammoInMag -= 1

        recoilKick = 1f
        val rad = Math3D.degToRad(cameraYaw)
        val fwdX = sin(rad)
        val fwdZ = cos(rad)
        val rightX = cos(rad)
        val rightZ = -sin(rad)
        playerRotY = cameraYaw
        onHapticPulse?.invoke()
        com.example.audio.IndraAudioEngine.playGunshot(spec.id)

        val muzzleX = playerX - rightX * 0.16f + fwdX * 0.95f
        val muzzleY = playerY + (if (proning) 0.55f else if (crouching) 1.05f else 1.40f)
        val muzzleZ = playerZ - rightZ * 0.16f + fwdZ * 0.95f
        val speed = 68f
        val atkMult = petController.effectiveAttackMultiplier()

        val vx: Float
        val vy: Float
        val vz: Float
        if (hasCustomCameraRay) {
            // Aim straight through the right-side crosshair world ray convergence point
            val targetDist = resolveAimConvergenceDistance()
            val targetX = aimRayOriginX + aimRayDirX * targetDist
            val targetY = aimRayOriginY + aimRayDirY * targetDist
            val targetZ = aimRayOriginZ + aimRayDirZ * targetDist
            val dx = targetX - muzzleX
            val dy = targetY - muzzleY
            val dz = targetZ - muzzleZ
            val len = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(0.01f)
            vx = (dx / len) * speed
            vy = (dy / len) * speed
            vz = (dz / len) * speed
        } else {
            val pitchRad = Math3D.degToRad(cameraPitch * 0.25f)
            vx = fwdX * speed
            vy = -sin(pitchRad) * speed * 0.2f
            vz = fwdZ * speed
        }

        projectiles.add(
            EnergyProjectile(
                id = projectileSeq++,
                x = muzzleX,
                y = muzzleY,
                z = muzzleZ,
                vx = vx,
                vy = vy,
                vz = vz,
                damage = spec.damage * atkMult,
                colorInt = spec.boltColorHex.toInt()
            )
        )
    }

    private fun resolveAimConvergenceDistance(): Float {
        var bestDist = 48f
        for (npc in npcs) {
            if (!npc.respawnController.collidersEnabled) continue
            val vx = npc.x - aimRayOriginX
            val vy = (npc.y + 1.05f) - aimRayOriginY
            val vz = npc.z - aimRayOriginZ
            val proj = vx * aimRayDirX + vy * aimRayDirY + vz * aimRayDirZ
            if (proj <= 1.5f || proj >= bestDist) continue
            val px = vx - aimRayDirX * proj
            val py = vy - aimRayDirY * proj
            val pz = vz - aimRayDirZ * proj
            if (px * px + pz * pz <= 1.45f * 1.45f && abs(py) <= 1.85f) {
                bestDist = proj
            }
        }
        return bestDist
    }

    private fun recordDamageFeedback(dmg: Int, targetName: String, isCritical: Boolean) {
        lastDamageFeedback = DamageFeedbackEvent(
            damage = dmg,
            targetName = targetName,
            isCritical = isCritical,
            seq = damageSeq++
        )
        damagePopupTimerSec = 1.6f
    }

    private fun tryHitOnlinePlayersAlongRay(dirX: Float, dirZ: Float, maxRange: Float, dmg: Float): Boolean {
        for (target in onlinePlayerTargets) {
            if (target.userId == localPlayerUid) continue
            if (target.userId in protectedTeammateUids && target.userId !in activeBountyTargetUids) continue
            val dx = target.posX.toFloat() - playerX
            val dz = target.posZ.toFloat() - playerZ
            val distSq = dx * dx + dz * dz
            if (distSq > maxRange * maxRange) continue
            val dist = sqrt(distSq).coerceAtLeast(0.01f)
            val dot = (dx / dist) * dirX + (dz / dist) * dirZ
            if (dot < 0.72f) continue
            if (!PhysicsWorld.hasLineOfSight(playerX, playerY + 1.2f, playerZ, target.posX.toFloat(), target.posY.toFloat() + 1.2f, target.posZ.toFloat(), insideHouse)) {
                continue
            }
            val curHp = onlineTargetHp[target.userId] ?: target.hp.toFloat().coerceAtLeast(100f)
            val nextHp = (curHp - dmg).coerceAtLeast(0f)
            onlineTargetHp[target.userId] = if (nextHp <= 0f) 200f else nextHp
            recordDamageFeedback(dmg.toInt(), target.username, isCritical = dmg >= 50f)
            if (nextHp <= 0f) registerEnemyKill()
            onOnlineTargetHit?.invoke(target, dmg, nextHp <= 0f)
            return true
        }
        return false
    }

    fun castAbility(slotIndex: Int, abilityName: String): Boolean {
        if (isDefeated || slotIndex !in abilityCooldowns.indices) return false
        if (abilityCooldowns[slotIndex] > 0f) return false

        // 1. Check Character / Legacy Abilities
        val charSpec = GameCatalog.abilities.firstOrNull { it.name == abilityName && it.source == "Character" }
        if (charSpec != null) {
            if (ep < charSpec.epCost) {
                onCombatEventBanner?.invoke("Not enough EP (${ep.toInt()}/${charSpec.epCost.toInt()})!")
                return false
            }
            ep = (ep - charSpec.epCost).coerceAtLeast(0f)
            abilityCooldowns[slotIndex] = charSpec.cooldownSec
            shockwaves.add(ShockwaveEffect(playerX, playerY + 0.3f, playerZ, charSpec.colorHex.toInt(), 1.0f, 0.7f))
            val atkMult = petController.effectiveAttackMultiplier()
            when (charSpec.name) {
                "Crimson Slash" -> applyAreaDamage(playerX, playerZ, 12f, 65f * atkMult)
                "Lightning Dash" -> {
                    val rad = Math3D.degToRad(cameraYaw)
                    val (nx, nz) = PhysicsWorld.resolveCapsuleWorld(
                        x = playerX + sin(rad) * 11f,
                        y = playerY,
                        z = playerZ + cos(rad) * 11f,
                        radius = 0.7f,
                        insideHouse = insideHouse,
                        parkedVehicles = parkedVehicles,
                        traffic = traffic
                    )
                    playerX = nx
                    playerZ = nz
                    playerY = if (insideHouse != null) 0f else WorldMapData.terrainHeightAt(nx, nz)
                }
                "Aegis Barrier" -> shield = (shield + 80f).coerceAtMost(effectiveMaxShield())
                "Shadow Clone Burst" -> applyAreaDamage(playerX, playerZ, 15f, 80f * atkMult)
            }
            onCombatEventBanner?.invoke("${charSpec.name} activated!")
            return true
        }

        // 2. Check all 50 Pet Active Abilities from PetCatalog
        val petSpec = com.example.data.model.PetCatalog.resolveByActiveAbilityName(abilityName)
            ?: com.example.data.model.PetCatalog.resolveSpec(petType)
        if (ep < petSpec.epCost) {
            onCombatEventBanner?.invoke("Not enough EP (${ep.toInt()}/${petSpec.epCost.toInt()})!")
            return false
        }
        val ownedMatch = petController.equippedRoster.firstOrNull {
            com.example.data.model.PetCatalog.resolveSpec(it.petType).id == petSpec.id
        }
        val evo = ownedMatch?.effectiveEvolutionLevel() ?: petEvolutionLevel
        ep = (ep - petSpec.epCost).coerceAtLeast(0f)
        abilityCooldowns[slotIndex] = petSpec.activeCooldownSec
        petController.executeActivePetAbility(petSpec, evo, this, onPetBonusCoins)
        com.example.audio.IndraAudioEngine.playPetSound(petSpec.soundProfile, evo)
        onCombatEventBanner?.invoke("${petSpec.name}: ${petSpec.activeName}!")
        return true
    }

    fun applyPetAreaBlast(cx: Float, cz: Float, radius: Float, damage: Float) {
        applyAreaDamage(cx, cz, radius, damage)
    }

    private fun applyAreaDamage(cx: Float, cz: Float, radius: Float, dmg: Float) {
        for (npc in npcs) {
            if (!npc.respawnController.collidersEnabled) continue
            val dx = npc.x - cx
            val dz = npc.z - cz
            if (dx * dx + dz * dz <= radius * radius) {
                if (!PhysicsWorld.hasLineOfSight(cx, playerY + 1.1f, cz, npc.x, npc.y + 1.1f, npc.z, insideHouse)) {
                    continue
                }
                recordDamageFeedback(dmg.toInt(), npc.displayName, isCritical = dmg >= 60f)
                val defeated = npc.respawnController.takeDamage(dmg)
                provokeNpcByPlayer(npc, defeated)
            }
        }
    }

    fun applyDamageToPlayer(rawDmg: Float, attackerName: String = "Rival") {
        if (isDefeated) return
        val guardScale = if (defending) 0.45f else 1f
        val effectiveDmg = rawDmg * guardScale * petController.effectiveDamageTakenMultiplier()
        val shieldAbsorb = minOf(shield, effectiveDmg * 0.6f)
        shield = (shield - shieldAbsorb).coerceAtLeast(0f)
        hp = (hp - (effectiveDmg - shieldAbsorb)).coerceAtLeast(0f)
        if (hp <= 0f) {
            if (petController.cappedPassives.hasEmergencySave && !petController.emergencySaveUsedThisLife) {
                petController.emergencySaveUsedThisLife = true
                hp = 70f.coerceAtMost(effectiveMaxHp())
                shield = (shield + 35f).coerceAtMost(effectiveMaxShield())
                shockwaves.add(ShockwaveEffect(playerX, playerY + 0.5f, playerZ, 0xFFFFD166.toInt(), 2.2f, 0.9f))
                onCombatEventBanner?.invoke("Phoenix Rebirth! Saved you from knockout (+70 HP)!")
                return
            }
            isDefeated = true
            respawnTimerSec = 5.0f
            onPlayerDefeated?.invoke()
            onCombatEventBanner?.invoke("You were defeated by $attackerName! Respawning...")
        }
    }

    fun triggerVehicleInteraction(onVehicleAlarm: (String) -> Unit) {
        if (isDefeated || insideHouse != null) return
        if (drivingVehicle != null) {
            val v = drivingVehicle!!
            val idx = parkedVehicles.indexOfFirst { it.id == v.id }
            val updated = v.copy(x = playerX, z = playerZ, rotY = playerRotY)
            if (idx >= 0) parkedVehicles[idx] = updated else parkedVehicles.add(updated)
            drivingVehicle = null
            vehicleSpeed = 0f
            driveThrottle = 0f
            val rad = Math3D.degToRad(playerRotY + 90f)
            val (ex, ez) = PhysicsWorld.resolveCapsuleWorld(
                x = playerX + sin(rad) * 2.4f,
                y = playerY,
                z = playerZ + cos(rad) * 2.4f,
                radius = 0.7f,
                insideHouse = null,
                parkedVehicles = parkedVehicles,
                traffic = traffic
            )
            playerX = ex
            playerZ = ez
            playerY = WorldMapData.terrainHeightAt(ex, ez)
            onCombatEventBanner?.invoke("Exited ${v.modelName}")
            return
        }

        val nearVeh = findNearestParkedVehicle(6.5f) ?: return
        if (!PhysicsWorld.hasLineOfSight(playerX, playerY + 1.1f, playerZ, nearVeh.x, WorldMapData.terrainHeightAt(nearVeh.x, nearVeh.z) + 0.8f, nearVeh.z, insideHouse)) {
            onCombatEventBanner?.invoke("Obstacle blocking access to ${nearVeh.modelName}")
            return
        }
        if (nearVeh.isLocked) {
            shockwaves.add(ShockwaveEffect(nearVeh.x, playerY + 0.8f, nearVeh.z, 0xFFEF4444.toInt(), 1.5f, 0.8f))
            onVehicleAlarm("ALARM TRIGGERED! ${nearVeh.modelName} is Car-Key locked by ${nearVeh.ownerName}.")
        } else {
            drivingVehicle = nearVeh
            parkedVehicles.removeAll { it.id == nearVeh.id }
            playerX = nearVeh.x
            playerZ = nearVeh.z
            playerY = WorldMapData.terrainHeightAt(nearVeh.x, nearVeh.z)
            playerRotY = nearVeh.rotY
            val actionLabel = if (nearVeh.ownerName == "Your Garage") "Driving" else "Hijacked"
            onCombatEventBanner?.invoke("$actionLabel ${nearVeh.modelName}!")
        }
    }

    fun interactWithNearby(onHouseEnter: (WorldBuilding) -> Unit, onNpcInspect: (NpcEntity) -> Unit, onVehicleAlarm: (String) -> Unit) {
        if (isDefeated) return
        if (insideHouse != null) {
            val exited = insideHouse!!
            insideHouse = null
            playerX = exited.x
            playerZ = exited.z + exited.depth * 0.5f + 4.5f
            playerY = WorldMapData.terrainHeightAt(playerX, playerZ)
            com.example.audio.IndraAudioEngine.playDoorTransition()
            onCombatEventBanner?.invoke("Exited ${exited.name} back into open world")
            return
        }
        if (drivingVehicle != null) {
            triggerVehicleInteraction(onVehicleAlarm)
            return
        }

        val nearVeh = findNearestParkedVehicle(6.5f)
        if (nearVeh != null) {
            triggerVehicleInteraction(onVehicleAlarm)
            return
        }

        val nearHouse = findNearestEnterableHouse(14f)
        if (nearHouse != null) {
            insideHouse = nearHouse
            com.example.audio.IndraAudioEngine.playDoorTransition()
            onHouseEnter(nearHouse)
            return
        }

        val nearWild = findNearestWildPet(6.5f)
        if (nearWild != null) {
            nearWild.adopted = true
            onWildPetAdopted?.invoke(nearWild)
            return
        }

        val nearNpc = findNearestNpc(7.0f)
        if (nearNpc != null) {
            onNpcInspect(nearNpc)
        }
    }

    fun teleportToDistrict(x: Float, z: Float, districtName: String) {
        insideHouse = null
        drivingVehicle = null
        playerX = x.coerceIn(-WorldMapData.WORLD_BOUND, WorldMapData.WORLD_BOUND)
        playerZ = z.coerceIn(-WorldMapData.WORLD_BOUND, WorldMapData.WORLD_BOUND)
        playerY = WorldMapData.terrainHeightAt(playerX, playerZ)
        velocityY = 0f
        petX = playerX + 2.2f
        petZ = playerZ + 2.0f
        petY = playerY
        lastDistrict = districtName
        gpsDestination?.let { dest ->
            gpsPath = WorldNavigation.findPath(playerX, playerZ, dest.x, dest.z)
        }
        onDistrictEntered?.invoke(districtName)
    }

    fun step(dtRaw: Float) {
        val dt = dtRaw.coerceIn(0.001f, 0.05f)
        worldTimeSec += dt
        recoilKick = (recoilKick - dt * 5f).coerceAtLeast(0f)
        if (katanaSwingTimer > 0f) katanaSwingTimer = (katanaSwingTimer - dt).coerceAtLeast(0f)
        if (punchAnimTimer > 0f) punchAnimTimer = (punchAnimTimer - dt).coerceAtLeast(0f)
        if (meleeCooldown > 0f) meleeCooldown = (meleeCooldown - dt).coerceAtLeast(0f)
        if (heavyAttackCooldownSec > 0f) heavyAttackCooldownSec = (heavyAttackCooldownSec - dt).coerceAtLeast(0f)
        if (healCooldownSec > 0f) healCooldownSec = (healCooldownSec - dt).coerceAtLeast(0f)
        if (grenadeCooldownSec > 0f) grenadeCooldownSec = (grenadeCooldownSec - dt).coerceAtLeast(0f)
        if (killBannerTimerSec > 0f) {
            killBannerTimerSec -= dt
            if (killBannerTimerSec <= 0f) killBannerText = null
        }
        if (damagePopupTimerSec > 0f) {
            damagePopupTimerSec -= dt
            if (damagePopupTimerSec <= 0f) lastDamageFeedback = null
        }
        if (!hasCustomCameraRay && showcaseMode == ShowcaseMode.NONE && insideHouse == null && !isDefeated) {
            val yawRad = Math3D.degToRad(cameraYaw)
            updateCrosshairRayTarget(
                playerX,
                playerY + 1.55f,
                playerZ,
                sin(yawRad),
                0f,
                cos(yawRad)
            )
        }

        for (gw in groundWeapons) {
            if (gw.pickupGraceSec > 0f) gw.pickupGraceSec = (gw.pickupGraceSec - dt).coerceAtLeast(0f)
        }

        if (isDefeated) {
            respawnTimerSec -= dt
            if (respawnTimerSec <= 0f) {
                isDefeated = false
                petController.resetOnRespawn()
                hp = effectiveMaxHp()
                ep = maxEp
                shield = 50f
                proning = false
                crouching = false
                for (npc in npcs) {
                    npc.provokedByPlayer = false
                    npc.provokedTimerSec = 0f
                    if (npc.state == NpcBehaviorState.ENGAGE) npc.state = NpcBehaviorState.PATROL
                }
                // Respawn at Plaza while preserving chosen Hero appearance (heroOutfitId/heroHairId/heroAccessoryId)
                teleportToDistrict(WorldLocations.playerSpawn.x, WorldLocations.playerSpawn.z, WorldLocations.playerSpawn.district.displayName)
                onCombatEventBanner?.invoke("Respawned at Sakura Torii Plaza!")
            }
        }

        for (i in abilityCooldowns.indices) {
            if (abilityCooldowns[i] > 0f) {
                abilityCooldowns[i] = (abilityCooldowns[i] - dt).coerceAtLeast(0f)
            }
        }
        if (!isDefeated) {
            ep = (ep + dt * 4.5f).coerceAtMost(maxEp)
            val eMaxHp = effectiveMaxHp()
            if (hp < eMaxHp) hp = (hp + dt * 1.2f).coerceAtMost(eMaxHp)
        }
        petController.stepPassivesAndBuffs(dt, this, worldTimeSec)

        if (reloadTimer > 0f) {
            reloadTimer -= dt
            if (reloadTimer <= 0f) {
                reloadTimer = 0f
                val spec = activeWeaponSpec()
                if (spec.weaponType.isFirearm) {
                    val needed = spec.magSize - ammoInMag
                    val take = needed.coerceAtMost(reserveAmmo)
                    ammoInMag += take
                    reserveAmmo -= take
                }
            }
        }

        if (isFiring && !isDefeated && showcaseMode == ShowcaseMode.NONE) {
            fireActiveWeapon()
        }

        val inputX = (joyX + keyX).coerceIn(-1f, 1f)
        val inputY = (joyY + keyY + driveThrottle).coerceIn(-1f, 1f)

        if (showcaseMode == ShowcaseMode.CHARACTER_PREVIEW) {
            // Preserve exact in-world player coordinates while animating character pose smoothly
            stridePhase = sin(worldTimeSec * 2.0f) * 0.08f
        } else if (showcaseMode != ShowcaseMode.NONE) {
            needsGameplayCameraSync = true
            insideHouse = null
            drivingVehicle = null
            playerX = 0f
            playerY = 0f
            playerZ = 14f
            playerRotY = 22f
            stridePhase = sin(worldTimeSec * 2.2f) * 0.14f
            crouching = false
            cameraPitch = 12f
            cameraYaw = when (showcaseMode) {
                ShowcaseMode.SPLASH_LOADING -> (202f + sin(worldTimeSec * 0.28f) * 26f + 360f) % 360f
                ShowcaseMode.LOGIN -> (192f + sin(worldTimeSec * 0.16f) * 8f + 360f) % 360f
                ShowcaseMode.LOBBY -> (196f + showcaseDragYawOffset + 360f) % 360f
                else -> cameraYaw
            }
        } else if (!isDefeated && drivingVehicle != null) {
            if (needsGameplayCameraSync) {
                needsGameplayCameraSync = false
                playerRotY = 180f
            }
            val v = drivingVehicle!!
            val maxSpd = when (v.category) {
                "supercar" -> 42f
                "motorcycle" -> 36f
                "helicopter" -> 38f
                "buggy" -> 32f
                else -> 30f
            }
            if (abs(inputY) > 0.05f) {
                vehicleSpeed = Math3D.lerp(vehicleSpeed, inputY * maxSpd, dt * 2.8f)
            } else {
                vehicleSpeed = Math3D.lerp(vehicleSpeed, 0f, dt * 2.2f)
            }
            if (abs(vehicleSpeed) > 0.4f || v.category == "helicopter") {
                val steerFactor = if (vehicleSpeed >= -0.1f) 1f else -1f
                playerRotY = (playerRotY - inputX * 115f * dt * steerFactor + 360f) % 360f
            }
            cameraYaw = playerRotY
            cameraPitch = 11f
            val rad = Math3D.degToRad(playerRotY)
            val nx = playerX + sin(rad) * vehicleSpeed * dt
            val nz = playerZ + cos(rad) * vehicleSpeed * dt
            if (v.category == "helicopter") {
                playerX = nx.coerceIn(-WorldMapData.WORLD_BOUND, WorldMapData.WORLD_BOUND)
                playerZ = nz.coerceIn(-WorldMapData.WORLD_BOUND, WorldMapData.WORLD_BOUND)
                val minHover = WorldMapData.terrainHeightAt(playerX, playerZ) + 5.5f
                if (playerY < minHover) playerY = Math3D.lerp(playerY, minHover + 1.5f, dt * 1.6f)
            } else {
                val (rx, rz) = PhysicsWorld.resolveCapsuleWorld(
                    x = nx,
                    y = playerY,
                    z = nz,
                    radius = 1.65f,
                    insideHouse = null,
                    parkedVehicles = parkedVehicles,
                    traffic = traffic,
                    ignoreVehicleId = v.id
                )
                playerX = rx
                playerZ = rz
                val targetGround = WorldMapData.terrainHeightAt(rx, rz).coerceAtLeast(-0.25f)
                playerY = Math3D.lerp(playerY, targetGround, dt * 10f)
            }
            vehicleWheelAngle = (vehicleWheelAngle + vehicleSpeed * dt * 42f) % 360f
        } else if (!isDefeated) {
            if (needsGameplayCameraSync) {
                needsGameplayCameraSync = false
                playerRotY = 180f
            }
            if (abs(inputX) > 0.06f) {
                val turnRateDegPerSec = 130f * joystickSensitivity
                playerRotY = (playerRotY - inputX * turnRateDegPerSec * dt + 360f) % 360f
            }
            // Keep third-person camera locked directly behind hero with straight horizon angle
            cameraYaw = playerRotY
            cameraPitch = 7f

            val rawForward = abs(inputY)
            val groundY = if (insideHouse != null) 0f else WorldMapData.terrainHeightAt(playerX, playerZ).coerceAtLeast(-0.35f)
            val isGrounded = playerY <= groundY + 0.18f

            if (rawForward > 0.06f) {
                val playerRad = Math3D.degToRad(playerRotY)
                val dirSign = if (inputY >= 0f) 1f else -1f
                val dirX = sin(playerRad) * dirSign
                val dirZ = cos(playerRad) * dirSign

                if (sprinting && proning) proning = false
                val activeSprint = inputY > 0f && (sprinting || (autoSprint && rawForward > 0.85f && !crouching && !proning))
                val inWater = insideHouse == null && WorldMapData.isDeepWater(playerX, playerZ)
                val baseSpeed = when {
                    inWater -> 5.0f
                    proning -> 2.3f
                    crouching -> 4.2f
                    activeSprint -> 14.5f
                    else -> 8.5f
                }
                val speedScale = if (inputY >= 0f) rawForward else rawForward * 0.68f
                val moveMag = (speedScale * joystickSensitivity).coerceAtMost(1f)
                val speed = baseSpeed * petController.effectiveSpeedMultiplier(inWater)
                val nx = playerX + dirX * speed * moveMag * dt
                val nz = playerZ + dirZ * speed * moveMag * dt

                val (rx, rz) = PhysicsWorld.resolveCapsuleWorld(
                    x = nx,
                    y = playerY,
                    z = nz,
                    radius = 0.7f,
                    insideHouse = insideHouse,
                    parkedVehicles = parkedVehicles,
                    traffic = traffic
                )
                val (sx, sz) = if (insideHouse == null) {
                    PhysicsWorld.applySlopeConstraint(playerX, playerZ, rx, rz, isGrounded)
                } else {
                    rx to rz
                }
                playerX = sx
                playerZ = sz
                stridePhase += dt * speed * 1.8f
            } else if (abs(inputX) > 0.12f) {
                stridePhase += dt * 4.2f
            } else {
                stridePhase = 0f
            }

            // Smooth terrain height & jump gravity
            val nextGroundY = if (insideHouse != null) 0f else WorldMapData.terrainHeightAt(playerX, playerZ).coerceAtLeast(-0.35f)
            if (playerY > nextGroundY + 0.05f || velocityY > 0f) {
                playerY += velocityY * dt
                velocityY -= 22f * dt
                if (playerY <= nextGroundY) {
                    playerY = nextGroundY
                    velocityY = 0f
                }
            } else {
                playerY = Math3D.lerp(playerY, nextGroundY, dt * 14f)
                velocityY = 0f
            }
        }

        // Pet spring-follow behind player's right shoulder
        if (petSummoned) {
            petBobPhase += dt * 4.5f
            val followRad = Math3D.degToRad(if (showcaseMode != ShowcaseMode.NONE) 118f else playerRotY - 135f)
            val desiredPetX = playerX + sin(followRad) * 2.3f
            val desiredPetZ = playerZ + cos(followRad) * 2.3f
            petX = Math3D.lerp(petX, desiredPetX, dt * 3.8f)
            petZ = Math3D.lerp(petZ, desiredPetZ, dt * 3.8f)
            val motion = com.example.data.model.PetCatalog.resolveSpec(petType).motionType
            petY = when (motion) {
                com.example.data.model.PetMotionType.FLY_HOVER -> playerY + 1.55f + sin(petBobPhase) * 0.25f
                com.example.data.model.PetMotionType.CRYSTAL_FLOAT -> playerY + 0.95f + sin(petBobPhase) * 0.18f
                com.example.data.model.PetMotionType.SWIM_FLOAT -> playerY + 0.45f + sin(petBobPhase) * 0.14f
                com.example.data.model.PetMotionType.HOP -> playerY + abs(sin(petBobPhase * 1.4f)) * 0.30f
                else -> playerY + abs(sin(petBobPhase)) * 0.12f
            }
            petRotY = Math3D.lerpAngleDeg(petRotY, if (showcaseMode != ShowcaseMode.NONE) 195f else playerRotY, dt * 6f)
        }

        // Update NPCs, Traffic, Projectiles, Shockwaves & Weather
        if (insideHouse == null && showcaseMode != ShowcaseMode.CHARACTER_PREVIEW) {
            NpcAiSystem.updateNpcs(
                npcs = npcs,
                playerX = playerX,
                playerZ = playerZ,
                dt = dt,
                aggroRangeScale = (1f - petController.effectiveCamouflageFactor()).coerceIn(0.35f, 1f)
            )
            NpcAiSystem.updateTraffic(traffic, dt)

            // Only NPCs provoked by player attacks may counter-attack in melee
            if (showcaseMode == ShowcaseMode.NONE && !isDefeated && drivingVehicle == null) {
                botAttackAccumulatorSec += dt
                if (botAttackAccumulatorSec >= 1.3f) {
                    botAttackAccumulatorSec = 0f
                    val attacker = npcs.firstOrNull {
                        it.provokedByPlayer &&
                            it.state == NpcBehaviorState.ENGAGE &&
                            it.respawnController.collidersEnabled &&
                            (it.x - playerX) * (it.x - playerX) + (it.z - playerZ) * (it.z - playerZ) <= 5.0f * 5.0f &&
                            PhysicsWorld.hasLineOfSight(it.x, it.y + 1.2f, it.z, playerX, playerY + 1.2f, playerZ, null)
                    }
                    if (attacker != null) {
                        applyDamageToPlayer(14f, attacker.displayName)
                    }
                }
            }
        }

        val projIt = projectiles.iterator()
        while (projIt.hasNext()) {
            val p = projIt.next()
            val nx = p.x + p.vx * dt
            val ny = p.y + p.vy * dt
            val nz = p.z + p.vz * dt
            p.lifeSec -= dt

            // Stop projectile if blocked by building/terrain
            val wallHit = !PhysicsWorld.hasLineOfSight(p.x, p.y, p.z, nx, ny, nz, insideHouse)
            p.x = nx
            p.y = ny
            p.z = nz

            var hit = wallHit
            if (!hit) {
                for (npc in npcs) {
                    if (!npc.respawnController.collidersEnabled) continue
                    val dx = npc.x - p.x
                    val dz = npc.z - p.z
                    if (dx * dx + dz * dz < 1.35f * 1.35f) {
                        hit = true
                        shockwaves.add(ShockwaveEffect(p.x, p.y, p.z, p.colorInt, 0.5f, 0.35f))
                        val isCrit = p.damage >= 45f
                        recordDamageFeedback(p.damage.toInt(), npc.displayName, isCrit)
                        val defeated = npc.respawnController.takeDamage(p.damage)
                        provokeNpcByPlayer(npc, defeated)
                        break
                    }
                }
            } else {
                shockwaves.add(ShockwaveEffect(p.x, p.y, p.z, p.colorInt, 0.35f, 0.2f))
            }

            if (hit || p.lifeSec <= 0f) projIt.remove()
        }

        val grenIt = activeGrenades.iterator()
        while (grenIt.hasNext()) {
            val g = grenIt.next()
            g.vx *= (1f - dt * 0.45f)
            g.vz *= (1f - dt * 0.45f)
            g.vy -= 18f * dt
            g.x += g.vx * dt
            g.y += g.vy * dt
            g.z += g.vz * dt
            val floorY = if (insideHouse != null) 0.15f else WorldMapData.terrainHeightAt(g.x, g.z) + 0.15f
            if (g.y < floorY) {
                g.y = floorY
                g.vy = abs(g.vy) * 0.35f
                g.vx *= 0.65f
                g.vz *= 0.65f
            }
            g.fuseSec -= dt
            if (g.fuseSec <= 0f) {
                shockwaves.add(ShockwaveEffect(g.x, g.y + 0.4f, g.z, 0xFFFF3B5C.toInt(), 2.2f, 0.75f))
                shockwaves.add(ShockwaveEffect(g.x, g.y + 0.6f, g.z, 0xFFFFD166.toInt(), 1.4f, 0.55f))
                com.example.audio.IndraAudioEngine.playExplosion()
                applyAreaDamage(g.x, g.z, 9.5f, 95f)
                grenIt.remove()
            }
        }

        if (autoPickup && !isDefeated && showcaseMode == ShowcaseMode.NONE && drivingVehicle == null) {
            val autoNear = findNearestGroundWeapon(1.65f, ignoreGrace = false)
            if (autoNear != null && autoNear.weaponId !in ownedWeaponIds) {
                pickupNearbyWeapon(1.65f)
            }
        }

        val swIt = shockwaves.iterator()
        while (swIt.hasNext()) {
            val sw = swIt.next()
            sw.radius += dt * 16f
            sw.lifeSec -= dt
            if (sw.lifeSec <= 0f) swIt.remove()
        }

        if (weatherMode != WeatherMode.CLEAR) {
            val activeCount = graphicsPreset.particleCount.coerceAtMost(weatherParticles.size)
            for (i in 0 until activeCount) {
                val wp = weatherParticles[i]
                wp.y -= wp.speedY * dt * (if (weatherMode == WeatherMode.SAKURA) 0.45f else 2.2f)
                wp.x += wp.driftX * dt
                if (wp.y < playerY - 1f) {
                    wp.y = playerY + 18f + Random.nextFloat() * 6f
                    wp.x = playerX + (Random.nextFloat() - 0.5f) * 56f
                    wp.z = playerZ + (Random.nextFloat() - 0.5f) * 56f
                }
            }
        }

        // Check GPS arrival
        gpsDestination?.let { dest ->
            if (hypot(dest.x - playerX, dest.z - playerZ) < 16f) {
                onCombatEventBanner?.invoke("Arrived at ${dest.name}!")
                gpsDestination = null
                gpsPath = emptyList()
            }
        }

        // District change notification
        if (insideHouse == null && showcaseMode == ShowcaseMode.NONE) {
            val distNow = WorldMapData.resolveDistrict(playerX, playerZ)
            if (distNow != lastDistrict) {
                lastDistrict = distNow
                onDistrictEntered?.invoke(distNow)
            }
        }
    }

    fun findNearestParkedVehicle(maxDist: Float): ParkedWorldVehicle? =
        parkedVehicles.minByOrNull { (it.x - playerX) * (it.x - playerX) + (it.z - playerZ) * (it.z - playerZ) }
            ?.takeIf {
                val dx = it.x - playerX
                val dz = it.z - playerZ
                dx * dx + dz * dz <= maxDist * maxDist
            }

    fun findNearestEnterableHouse(maxDist: Float): WorldBuilding? =
        WorldMapData.buildings.filter { it.enterable }
            .minByOrNull { (it.x - playerX) * (it.x - playerX) + (it.z - playerZ) * (it.z - playerZ) }
            ?.takeIf {
                val dx = it.x - playerX
                val dz = it.z - playerZ
                dx * dx + dz * dz <= maxDist * maxDist
            }

    fun findNearestNpc(maxDist: Float): NpcEntity? =
        npcs.filter { it.respawnController.collidersEnabled }
            .minByOrNull { (it.x - playerX) * (it.x - playerX) + (it.z - playerZ) * (it.z - playerZ) }
            ?.takeIf {
                val dx = it.x - playerX
                val dz = it.z - playerZ
                dx * dx + dz * dz <= maxDist * maxDist
            }

    fun findNearestWildPet(maxDist: Float): WildWorldPet? =
        wildPets.filter { !it.adopted }
            .minByOrNull { (it.x - playerX) * (it.x - playerX) + (it.z - playerZ) * (it.z - playerZ) }
            ?.takeIf {
                val dx = it.x - playerX
                val dz = it.z - playerZ
                dx * dx + dz * dz <= maxDist * maxDist
            }

    fun findNearestBuyableListing(maxDist: Float = 16f): com.example.data.model.PropertyMarketListing? =
        GameCatalog.propertyMarketListings
            .filter { it.id !in ownedPropertyIds }
            .minByOrNull { (it.locX - playerX) * (it.locX - playerX) + (it.locZ - playerZ) * (it.locZ - playerZ) }
            ?.takeIf {
                val dx = it.locX - playerX
                val dz = it.locZ - playerZ
                dx * dx + dz * dz <= maxDist * maxDist
            }

    fun updateCrosshairRayTarget(eyeX: Float, eyeY: Float, eyeZ: Float, rayDirX: Float, rayDirY: Float, rayDirZ: Float) {
        aimRayOriginX = eyeX
        aimRayOriginY = eyeY
        aimRayOriginZ = eyeZ
        aimRayDirX = rayDirX
        aimRayDirY = rayDirY
        aimRayDirZ = rayDirZ
        if (isDefeated || insideHouse != null || drivingVehicle != null || showcaseMode != ShowcaseMode.NONE) {
            crosshairLockedOnEnemy = false
            crosshairTargetName = null
            return
        }
        val maxRange = 85f
        var bestDist = maxRange
        var bestName: String? = null

        for (npc in npcs) {
            if (!npc.respawnController.collidersEnabled) continue
            if (npc.role != NpcRole.RIVAL_BOT) continue
            val vx = npc.x - eyeX
            val vy = (npc.y + 1.05f) - eyeY
            val vz = npc.z - eyeZ
            val proj = vx * rayDirX + vy * rayDirY + vz * rayDirZ
            if (proj <= 0.5f || proj >= bestDist) continue
            val perpX = vx - rayDirX * proj
            val perpY = vy - rayDirY * proj
            val perpZ = vz - rayDirZ * proj
            val horizSq = perpX * perpX + perpZ * perpZ
            if (horizSq <= 1.35f * 1.35f && abs(perpY) <= 1.8f) {
                bestDist = proj
                bestName = npc.displayName
            }
        }

        for (target in onlinePlayerTargets) {
            if (target.userId == localPlayerUid) continue
            if (target.userId in protectedTeammateUids && target.userId !in activeBountyTargetUids) continue
            val vx = target.posX.toFloat() - eyeX
            val vy = (target.posY.toFloat() + 1.05f) - eyeY
            val vz = target.posZ.toFloat() - eyeZ
            val proj = vx * rayDirX + vy * rayDirY + vz * rayDirZ
            if (proj <= 0.5f || proj >= bestDist) continue
            val perpX = vx - rayDirX * proj
            val perpY = vy - rayDirY * proj
            val perpZ = vz - rayDirZ * proj
            if (perpX * perpX + perpZ * perpZ <= 1.35f * 1.35f && abs(perpY) <= 1.8f) {
                bestDist = proj
                bestName = target.username
            }
        }

        crosshairLockedOnEnemy = bestName != null
        crosshairTargetName = bestName
    }

    fun snapshotHud(): WorldHudSnapshot {
        val vehInteract: NearbyInteraction? = if (drivingVehicle != null) {
            NearbyInteraction("EXIT_VEH", drivingVehicle!!.id, "Exit ${drivingVehicle!!.modelName}", "Step out")
        } else if (insideHouse == null) {
            findNearestParkedVehicle(6.5f)?.let { veh ->
                NearbyInteraction(
                    kind = if (veh.isLocked) "ALARM_THEFT" else "VEHICLE",
                    id = veh.id,
                    label = if (veh.isLocked) "Unlock" else "Enter ${veh.modelName}",
                    subLabel = if (veh.isLocked) "Locked (${veh.ownerName})" else "Owner: ${veh.ownerName}"
                )
            }
        } else null

        val nearWild = if (insideHouse == null && drivingVehicle == null) findNearestWildPet(6.5f) else null
        val nearPlot = if (insideHouse == null && drivingVehicle == null) findNearestBuyableListing(16f) else null
        val nearby: NearbyInteraction? = when {
            insideHouse != null -> NearbyInteraction("EXIT_HOUSE", insideHouse!!.id, "Exit House", insideHouse!!.name)
            vehInteract != null -> vehInteract
            nearWild != null -> {
                val spec = com.example.data.model.PetCatalog.resolveSpec(nearWild.petId)
                NearbyInteraction("WILD_PET", nearWild.wildId, "Adopt Lv.${nearWild.evolutionLevel} ${spec.name}", nearWild.temperament)
            }
            else -> {
                val house = findNearestEnterableHouse(14.0f)
                val npc = findNearestNpc(7.0f)
                when {
                    house != null -> NearbyInteraction("HOUSE", house.id, "Enter House", house.name)
                    nearPlot != null -> NearbyInteraction("BUY_PLOT", nearPlot.id, "Buy ${nearPlot.title}", "${nearPlot.buyCoins} Coins")
                    npc != null -> NearbyInteraction("NPC", npc.id, "Inspect ${npc.displayName}", npc.statusBubble)
                    else -> null
                }
            }
        }

        val dest = gpsDestination
        val gpsDist = if (dest != null) hypot(dest.x - playerX, dest.z - playerZ).toInt() else 0
        val nearWeapon = if (drivingVehicle == null) findNearestGroundWeapon(4.5f, ignoreGrace = true) else null
        val radarRadius = petController.effectiveEnemyRadarRadius()
        val buffNotes = buildList {
            if (petController.hotRemainingSec > 0f) add("HoT")
            if (petController.attackBuffTimerSec > 0f) add("+ATK")
            if (petController.speedBuffTimerSec > 0f) add("+SPD")
            if (petController.protectionTimerSec > 0f) add("GUARD")
            if (petController.radarPulseTimerSec > 0f) add("RADAR")
            if (petController.camouflageTimerSec > 0f) add("CAMO")
        }

        return WorldHudSnapshot(
            x = playerX,
            y = playerY,
            z = playerZ,
            yawDeg = cameraYaw,
            district = insideHouse?.name ?: WorldMapData.resolveDistrict(playerX, playerZ),
            hp = hp,
            maxHp = effectiveMaxHp(),
            ep = ep,
            maxEp = maxEp,
            shield = shield,
            maxShield = effectiveMaxShield(),
            activeWeaponId = activeWeaponId,
            weaponType = activeWeaponSpec().weaponType,
            ownedWeaponIds = ownedWeaponIds.toList(),
            ammoInMag = ammoInMag,
            reserveAmmo = reserveAmmo,
            reloading = reloadTimer > 0f,
            sprinting = sprinting,
            crouching = crouching,
            proning = proning,
            aiming = aiming,
            defending = defending,
            meleeCooldown = meleeCooldown,
            heavyAttackCooldownSec = heavyAttackCooldownSec,
            medkitCount = medkitCount,
            healCooldownSec = healCooldownSec,
            grenadeCount = grenadeCount,
            grenadeCooldownSec = grenadeCooldownSec,
            petSummoned = petSummoned,
            isDefeated = isDefeated,
            respawnSecondsLeft = respawnTimerSec.toInt().coerceAtLeast(0),
            drivingVehicleName = drivingVehicle?.modelName,
            vehicleSpeedKmh = abs((vehicleSpeed * 4.5f).toInt()),
            insideHouseName = insideHouse?.name,
            nearbyInteraction = nearby,
            nearbyVehicleInteraction = vehInteract,
            nearbyGroundWeapon = nearWeapon,
            abilityCooldowns = abilityCooldowns.toList(),
            lastDamagePopup = lastDamageFeedback,
            gpsDestinationName = dest?.name,
            gpsDistanceMeters = gpsDist,
            petEvolutionLevel = petEvolutionLevel,
            petRadarActive = radarRadius > 0f,
            petRadarRadius = radarRadius,
            lootRadarActive = petController.isLootRadarActive(),
            activePetBuffLabel = buffNotes.takeIf { it.isNotEmpty() }?.joinToString(" • "),
            nearbyWildPet = nearWild,
            crosshairLockedOnEnemy = crosshairLockedOnEnemy,
            crosshairTargetName = crosshairTargetName,
            katanaComboStep = katanaComboStep,
            projectedSignboards = projectedSignboards,
            nearbyBuyableListing = nearPlot,
            houseStashWeapons = houseStashWeapons.toList(),
            houseStashMedkits = houseStashMedkits,
            houseStashGrenades = houseStashGrenades,
            sessionKills = sessionKills,
            killBannerText = killBannerText,
            heroOverheadScreenX = heroOverheadScreenX,
            heroOverheadScreenY = heroOverheadScreenY,
            customObjName = customObjModel?.modelName
        )
    }

    private fun createInitialGroundWeapons(): MutableList<GroundWeaponPickup> {
        val seeds = listOf(
            Triple("wpn_dragon_smg", 4.2f, 21f),
            Triple("wpn_shadow_katana", -4.2f, 21f),
            Triple("wpn_vortex_ar", 42f, -32f),
            Triple("wpn_pulse_pistol", -265f, -342f),
            Triple("wpn_dragon_smg", 510f, 36f),
            Triple("wpn_shadow_katana", -385f, -455f),
            Triple("wpn_vortex_ar", 565f, -335f),
            Triple("wpn_dragon_smg", -515f, -175f)
        )
        return seeds.mapIndexed { idx, (wId, wx, wz) ->
            val spec = GameCatalog.weapons.first { it.id == wId }
            GroundWeaponPickup(
                uid = "world_wpn_$idx",
                weaponId = wId,
                x = wx,
                y = WorldMapData.terrainHeightAt(wx, wz),
                z = wz,
                ammoInMag = spec.magSize,
                reserveAmmo = spec.reserveMax
            )
        }.toMutableList()
    }
}
