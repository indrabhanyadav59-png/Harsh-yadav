package com.example.data.model

import kotlin.math.min

enum class PetCategory(val displayName: String, val badgeHex: Long) {
    LAND("Land Animals", 0xFF22C55E),
    FLYING("Flying Animals", 0xFF38BDF8),
    REPTILE_FANTASY("Reptiles & Fantasy", 0xFFA855F7),
    WATER("Water Animals", 0xFF00E5FF),
    MAGICAL("Magical Animals", 0xFFFFD166)
}

enum class PetMotionType {
    GROUND_WALK,
    HOP,
    FLY_HOVER,
    SLITHER,
    SWIM_FLOAT,
    CRYSTAL_FLOAT
}

enum class PetArchetype {
    FOX_CANINE,
    KITSUNE_NINE,
    FELINE,
    DEER,
    RABBIT,
    BEAR_PANDA,
    AVIAN_RAPTOR,
    AVIAN_SONGBIRD,
    AVIAN_WATERFOWL,
    AVIAN_PHOENIX,
    DRAGON,
    TURTLE,
    SERPENT,
    LIZARD_GECKO,
    CROCODILE,
    DOLPHIN_WHALE_SHARK,
    SEAL_OTTER,
    PENGUIN,
    OCTOPUS,
    JELLYFISH,
    SEAHORSE,
    WATER_SPIRIT,
    UNICORN,
    CRYSTAL_GOLEM,
    CELESTIAL_BEAST
}

enum class PetSoundProfile {
    CHIRP_YIP,
    HOWL_BARK,
    PURR_ROAR,
    AVIAN_SCREECH,
    SONGBIRD_TRILL,
    DRAGON_RUMBLE,
    AQUA_SONAR,
    CRYSTAL_CHIME,
    CELESTIAL_CHORD
}

enum class PetPassiveType {
    HP_ON_KILL,
    EP_ON_KILL,
    HP_AND_EP_ON_KILL,
    DETECT_ENEMIES,
    MOVEMENT_SPEED,
    ATTACK_BONUS,
    PERIODIC_SHIELD,
    PERIODIC_HP,
    PERIODIC_EP,
    MAX_HP_BONUS,
    MAX_SHIELD_BONUS,
    RELOAD_SPEED,
    WATER_SPEED,
    CAMOUFLAGE,
    NIGHT_VISION_CRIT,
    LOOT_DETECTOR,
    PET_XP_BONUS,
    EMERGENCY_SAVE,
    DAMAGE_RESIST,
    OMNI_HARMONY
}

enum class PetActiveType {
    HOT_HP,
    ENEMY_RADAR_PULSE,
    EP_SURGE,
    SPEED_BOOST,
    ATTACK_BOOST,
    SPEED_AND_ATTACK,
    TEMP_SHIELD,
    QUICK_DASH,
    DAMAGE_PROTECTION,
    HP_AND_EP_RECOVER,
    RELOAD_AND_FOCUS,
    CAMOUFLAGE_CLOAK,
    AOE_BLAST,
    TEAM_HEAL_SHIELD,
    LOOT_SCAN_AND_COIN,
    REBIRTH_FLAME
}

data class PetSpec(
    val id: String,
    val number: Int,
    val name: String,
    val category: PetCategory,
    val rarity: String,
    val priceCoins: Long,
    val personality: String,
    val passiveName: String,
    val passiveSummary: String,
    val passiveType: PetPassiveType,
    val passiveBaseValue: Float,
    val activeName: String,
    val activeSummary: String,
    val activeType: PetActiveType,
    val activeBaseValue: Float,
    val activeCooldownSec: Float,
    val epCost: Float,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val accentColorHex: Long,
    val eyeColorHex: Long,
    val archetype: PetArchetype,
    val motionType: PetMotionType,
    val soundProfile: PetSoundProfile
) {
    fun evolutionMultiplier(evolutionLevel: Int): Float = when (evolutionLevel.coerceIn(1, 3)) {
        1 -> 1.0f
        2 -> 1.25f
        else -> 1.55f
    }

    fun evolutionTitle(evolutionLevel: Int): String = when (evolutionLevel.coerceIn(1, 3)) {
        1 -> "Lv.1 Basic"
        2 -> "Lv.2 Advanced"
        else -> "Lv.3 Legendary"
    }

    fun passiveScaledValue(evolutionLevel: Int): Float =
        passiveBaseValue * evolutionMultiplier(evolutionLevel)

    fun activeScaledValue(evolutionLevel: Int): Float =
        activeBaseValue * evolutionMultiplier(evolutionLevel)
}

/**
 * Aggregated passive bonuses from up to 5 equipped pets.
 * Duplicate HP, EP, Shield, Speed, Attack, and Reload bonuses are strictly capped
 * to prevent overpowered stacking.
 */
data class CappedPetPassives(
    val equippedCount: Int = 0,
    val speedBonusPct: Float = 0f,         // cap 0.22f (+22%)
    val maxHpBonusPct: Float = 0f,         // cap 0.18f (+18%)
    val maxShieldBonusPct: Float = 0f,     // cap 0.20f (+20%)
    val attackBonusPct: Float = 0f,        // cap 0.16f (+16%)
    val damageResistPct: Float = 0f,       // cap 0.18f (+18%)
    val reloadSpeedBonusPct: Float = 0f,   // cap 0.22f (+22%)
    val waterSpeedBonusPct: Float = 0f,    // cap 0.35f (+35%)
    val hpOnKill: Float = 0f,              // cap 35f
    val epOnKill: Float = 0f,              // cap 30f
    val periodicHpPer30s: Float = 0f,      // cap 28f
    val periodicShieldPer30s: Float = 0f,  // cap 24f
    val periodicEpPer25s: Float = 0f,      // cap 25f
    val enemyDetectRadius: Float = 0f,     // cap 65f
    val lootDetectRadius: Float = 0f,      // cap 55f
    val camouflageFactor: Float = 0f,      // cap 0.38f (enemy aggro range reduction)
    val nightVisionCritBonus: Float = 0f,  // cap 0.12f
    val petXpMultiplier: Float = 1f,       // cap 1.35f
    val hasEmergencySave: Boolean = false
)

data class WildPetSpawnSeed(
    val wildId: String,
    val petId: String,
    val evolutionLevel: Int,
    val district: String,
    val x: Float,
    val z: Float,
    val temperament: String
)

object PetCatalog {
    const val MAX_EQUIPPED_ROSTER = 5
    const val MAX_ABILITY_SLOTS_CHAR = 3
    const val MAX_ABILITY_SLOTS_PET = 4

    // Strict Anti-Stacking Caps (balanced PvP & open-world progression)
    const val CAP_SPEED_BONUS = 0.22f
    const val CAP_MAX_HP_BONUS = 0.18f
    const val CAP_MAX_SHIELD_BONUS = 0.20f
    const val CAP_ATTACK_BONUS = 0.16f
    const val CAP_DAMAGE_RESIST = 0.18f
    const val CAP_RELOAD_BONUS = 0.22f
    const val CAP_WATER_SPEED_BONUS = 0.35f
    const val CAP_HP_ON_KILL = 35f
    const val CAP_EP_ON_KILL = 30f
    const val CAP_PERIODIC_HP = 28f
    const val CAP_PERIODIC_SHIELD = 24f
    const val CAP_PERIODIC_EP = 25f
    const val CAP_ENEMY_DETECT_RADIUS = 65f
    const val CAP_LOOT_DETECT_RADIUS = 55f
    const val CAP_CAMOUFLAGE = 0.38f

    val allPets: List<PetSpec> = listOf(
        // ==================== 1. LAND ANIMALS (1-10) ====================
        PetSpec(
            id = "pet_anime_fox", number = 1, name = "Fox",
            category = PetCategory.LAND, rarity = "Mythic", priceCoins = 0L,
            personality = "Clever, loyal shrine Trickster who cheers after every takedown.",
            passiveName = "Vulpine Mend", passiveSummary = "Recover 15 HP after an elimination.",
            passiveType = PetPassiveType.HP_ON_KILL, passiveBaseValue = 15f,
            activeName = "Fox Flame", activeSummary = "Recover 30 HP over 5 seconds.",
            activeType = PetActiveType.HOT_HP, activeBaseValue = 30f,
            activeCooldownSec = 9f, epCost = 18f,
            primaryColorHex = 0xFFFFF8F3, secondaryColorHex = 0xFFFF5A36, accentColorHex = 0xFFFFD166, eyeColorHex = 0xFFDC2626,
            archetype = PetArchetype.FOX_CANINE, motionType = PetMotionType.GROUND_WALK, soundProfile = PetSoundProfile.CHIRP_YIP
        ),
        PetSpec(
            id = "pet_02_wolf", number = 2, name = "Wolf",
            category = PetCategory.LAND, rarity = "Legendary", priceCoins = 550L,
            personality = "Alert cyber-pack tracker with razor-sharp battle instincts.",
            passiveName = "Pack Sense", passiveSummary = "Detect nearby enemies within 32m.",
            passiveType = PetPassiveType.DETECT_ENEMIES, passiveBaseValue = 32f,
            activeName = "Hunter Radar", activeSummary = "Enemy detection pulse for 8 seconds.",
            activeType = PetActiveType.ENEMY_RADAR_PULSE, activeBaseValue = 8f,
            activeCooldownSec = 12f, epCost = 20f,
            primaryColorHex = 0xFF1E293B, secondaryColorHex = 0xFF38BDF8, accentColorHex = 0xFF00E5FF, eyeColorHex = 0xFF00E5FF,
            archetype = PetArchetype.FOX_CANINE, motionType = PetMotionType.GROUND_WALK, soundProfile = PetSoundProfile.HOWL_BARK
        ),
        PetSpec(
            id = "pet_03_dog", number = 3, name = "Dog",
            category = PetCategory.LAND, rarity = "Epic", priceCoins = 380L,
            personality = "Cheerful cyber-puppy bursting with boundless energy.",
            passiveName = "Loyal Charge", passiveSummary = "Gain 20 EP after an elimination.",
            passiveType = PetPassiveType.EP_ON_KILL, passiveBaseValue = 20f,
            activeName = "Aegis Pulse", activeSummary = "EP recovery boost (+50 EP & rapid regen).",
            activeType = PetActiveType.EP_SURGE, activeBaseValue = 50f,
            activeCooldownSec = 10f, epCost = 10f,
            primaryColorHex = 0xFFD97706, secondaryColorHex = 0xFFFEF3C7, accentColorHex = 0xFF22C55E, eyeColorHex = 0xFF1E293B,
            archetype = PetArchetype.FOX_CANINE, motionType = PetMotionType.GROUND_WALK, soundProfile = PetSoundProfile.HOWL_BARK
        ),
        PetSpec(
            id = "pet_04_cat", number = 4, name = "Cat",
            category = PetCategory.LAND, rarity = "Rare", priceCoins = 320L,
            personality = "Nimble rooftops neko who sprints ahead cleanly.",
            passiveName = "Feline Stride", passiveSummary = "+8% movement speed bonus.",
            passiveType = PetPassiveType.MOVEMENT_SPEED, passiveBaseValue = 0.08f,
            activeName = "Neko Sprint", activeSummary = "5-second burst speed boost.",
            activeType = PetActiveType.SPEED_BOOST, activeBaseValue = 5f,
            activeCooldownSec = 11f, epCost = 16f,
            primaryColorHex = 0xFFF1F5F9, secondaryColorHex = 0xFFF472B6, accentColorHex = 0xFFFBBF24, eyeColorHex = 0xFF22C55E,
            archetype = PetArchetype.FELINE, motionType = PetMotionType.GROUND_WALK, soundProfile = PetSoundProfile.CHIRP_YIP
        ),
        PetSpec(
            id = "pet_05_lion", number = 5, name = "Lion",
            category = PetCategory.LAND, rarity = "Epic", priceCoins = 480L,
            personality = "Proud golden-maned guardian who inspires fierce strikes.",
            passiveName = "King's Pride", passiveSummary = "+5% attack bonus.",
            passiveType = PetPassiveType.ATTACK_BONUS, passiveBaseValue = 0.05f,
            activeName = "Warlord Roar", activeSummary = "Temporary attack boost (+18% DMG for 7s).",
            activeType = PetActiveType.ATTACK_BOOST, activeBaseValue = 7f,
            activeCooldownSec = 13f, epCost = 22f,
            primaryColorHex = 0xFFF59E0B, secondaryColorHex = 0xFF92400E, accentColorHex = 0xFFFFD166, eyeColorHex = 0xFFB45309,
            archetype = PetArchetype.FELINE, motionType = PetMotionType.GROUND_WALK, soundProfile = PetSoundProfile.PURR_ROAR
        ),
        PetSpec(
            id = "pet_06_tiger", number = 6, name = "Tiger",
            category = PetCategory.LAND, rarity = "Legendary", priceCoins = 640L,
            personality = "Fierce striped brawler thriving in fast close combat.",
            passiveName = "Apex Momentum", passiveSummary = "Recover 25 HP and 10 EP after elimination.",
            passiveType = PetPassiveType.HP_AND_EP_ON_KILL, passiveBaseValue = 25f,
            activeName = "Striped Blitz", activeSummary = "Movement and attack boost for 6 seconds.",
            activeType = PetActiveType.SPEED_AND_ATTACK, activeBaseValue = 6f,
            activeCooldownSec = 14f, epCost = 24f,
            primaryColorHex = 0xFFFB923C, secondaryColorHex = 0xFF1E293B, accentColorHex = 0xFFFFD166, eyeColorHex = 0xFFFACC15,
            archetype = PetArchetype.FELINE, motionType = PetMotionType.GROUND_WALK, soundProfile = PetSoundProfile.PURR_ROAR
        ),
        PetSpec(
            id = "pet_07_deer", number = 7, name = "Deer",
            category = PetCategory.LAND, rarity = "Rare", priceCoins = 360L,
            personality = "Gentle sakura-antlered spirit attuned to barrier energy.",
            passiveName = "Forest Ward", passiveSummary = "Recover 15 Shield every 30 seconds.",
            passiveType = PetPassiveType.PERIODIC_SHIELD, passiveBaseValue = 15f,
            activeName = "Antler Barrier", activeSummary = "Temporary +45 Shield barrier.",
            activeType = PetActiveType.TEMP_SHIELD, activeBaseValue = 45f,
            activeCooldownSec = 13f, epCost = 20f,
            primaryColorHex = 0xFFC08457, secondaryColorHex = 0xFFFEF3C7, accentColorHex = 0xFFF9A8D4, eyeColorHex = 0xFF3B2F2F,
            archetype = PetArchetype.DEER, motionType = PetMotionType.GROUND_WALK, soundProfile = PetSoundProfile.SONGBIRD_TRILL
        ),
        PetSpec(
            id = "pet_08_rabbit", number = 8, name = "Rabbit",
            category = PetCategory.LAND, rarity = "Rare", priceCoins = 340L,
            personality = "Hyper-bouncy mochi bunny who never stands still.",
            passiveName = "Sky Hopper", passiveSummary = "+10% movement speed bonus.",
            passiveType = PetPassiveType.MOVEMENT_SPEED, passiveBaseValue = 0.10f,
            activeName = "Turbo Dash", activeSummary = "Quick forward dash & 3s sprint surge.",
            activeType = PetActiveType.QUICK_DASH, activeBaseValue = 9.5f,
            activeCooldownSec = 8f, epCost = 14f,
            primaryColorHex = 0xFFF8FAFC, secondaryColorHex = 0xFFF9A8D4, accentColorHex = 0xFF38BDF8, eyeColorHex = 0xFFEF4444,
            archetype = PetArchetype.RABBIT, motionType = PetMotionType.HOP, soundProfile = PetSoundProfile.CHIRP_YIP
        ),
        PetSpec(
            id = "pet_09_bear", number = 9, name = "Bear",
            category = PetCategory.LAND, rarity = "Epic", priceCoins = 500L,
            personality = "Warm-hearted tank cub who shields friends from heavy hits.",
            passiveName = "Grizzly Vitality", passiveSummary = "+5% maximum HP bonus.",
            passiveType = PetPassiveType.MAX_HP_BONUS, passiveBaseValue = 0.05f,
            activeName = "Iron Fur Guard", activeSummary = "Temporary 40% damage protection for 6s.",
            activeType = PetActiveType.DAMAGE_PROTECTION, activeBaseValue = 6f,
            activeCooldownSec = 15f, epCost = 24f,
            primaryColorHex = 0xFF78350F, secondaryColorHex = 0xFFD97706, accentColorHex = 0xFF38BDF8, eyeColorHex = 0xFF111827,
            archetype = PetArchetype.BEAR_PANDA, motionType = PetMotionType.GROUND_WALK, soundProfile = PetSoundProfile.PURR_ROAR
        ),
        PetSpec(
            id = "pet_10_panda", number = 10, name = "Panda",
            category = PetCategory.LAND, rarity = "Epic", priceCoins = 520L,
            personality = "Chill bamboo meditator radiating calm restorative chi.",
            passiveName = "Zen Bamboo", passiveSummary = "Recover 20 HP every 40 seconds.",
            passiveType = PetPassiveType.PERIODIC_HP, passiveBaseValue = 20f,
            activeName = "Chi Harmony", activeSummary = "Instant HP (+38) and EP (+35) recovery.",
            activeType = PetActiveType.HP_AND_EP_RECOVER, activeBaseValue = 38f,
            activeCooldownSec = 14f, epCost = 15f,
            primaryColorHex = 0xFFF8FAFC, secondaryColorHex = 0xFF0F172A, accentColorHex = 0xFF22C55E, eyeColorHex = 0xFF0F172A,
            archetype = PetArchetype.BEAR_PANDA, motionType = PetMotionType.GROUND_WALK, soundProfile = PetSoundProfile.CHIRP_YIP
        ),

        // ==================== 2. FLYING ANIMALS (11-20) ====================
        PetSpec(
            id = "pet_11_eagle", number = 11, name = "Eagle",
            category = PetCategory.FLYING, rarity = "Epic", priceCoins = 580L,
            personality = "Regal sky sovereign spotting threats from towering altitudes.",
            passiveName = "Eagle Eye", passiveSummary = "Long-range enemy detection (42m radius).",
            passiveType = PetPassiveType.DETECT_ENEMIES, passiveBaseValue = 42f,
            activeName = "Sky Talon Mark", activeSummary = "8s tactical enemy scan + +12% attack.",
            activeType = PetActiveType.ENEMY_RADAR_PULSE, activeBaseValue = 8f,
            activeCooldownSec = 12f, epCost = 20f,
            primaryColorHex = 0xFF78350F, secondaryColorHex = 0xFFF8FAFC, accentColorHex = 0xFFFACC15, eyeColorHex = 0xFFF59E0B,
            archetype = PetArchetype.AVIAN_RAPTOR, motionType = PetMotionType.FLY_HOVER, soundProfile = PetSoundProfile.AVIAN_SCREECH
        ),
        PetSpec(
            id = "pet_12_owl", number = 12, name = "Owl",
            category = PetCategory.FLYING, rarity = "Rare", priceCoins = 440L,
            personality = "Wise nocturnal sentinel with glowing golden optics.",
            passiveName = "Night Sight", passiveSummary = "+8% critical precision & dusk clarity.",
            passiveType = PetPassiveType.NIGHT_VISION_CRIT, passiveBaseValue = 0.08f,
            activeName = "Moonbeam Pulse", activeSummary = "7s enemy detection + fast focus reload.",
            activeType = PetActiveType.RELOAD_AND_FOCUS, activeBaseValue = 7f,
            activeCooldownSec = 11f, epCost = 18f,
            primaryColorHex = 0xFF94A3B8, secondaryColorHex = 0xFFE2E8F0, accentColorHex = 0xFFFACC15, eyeColorHex = 0xFFFBBF24,
            archetype = PetArchetype.AVIAN_RAPTOR, motionType = PetMotionType.FLY_HOVER, soundProfile = PetSoundProfile.SONGBIRD_TRILL
        ),
        PetSpec(
            id = "pet_13_parrot", number = 13, name = "Parrot",
            category = PetCategory.FLYING, rarity = "Rare", priceCoins = 400L,
            personality = "Colorful chatty pirate buddy obsessed with shiny loot.",
            passiveName = "Treasure Mimic", passiveSummary = "Detect ground weapons & loot within 38m.",
            passiveType = PetPassiveType.LOOT_DETECTOR, passiveBaseValue = 38f,
            activeName = "Loot Chirp", activeSummary = "Reveals loot & awards +35 Coins + speed.",
            activeType = PetActiveType.LOOT_SCAN_AND_COIN, activeBaseValue = 35f,
            activeCooldownSec = 15f, epCost = 15f,
            primaryColorHex = 0xFFEF4444, secondaryColorHex = 0xFF22C55E, accentColorHex = 0xFF38BDF8, eyeColorHex = 0xFFFACC15,
            archetype = PetArchetype.AVIAN_SONGBIRD, motionType = PetMotionType.FLY_HOVER, soundProfile = PetSoundProfile.SONGBIRD_TRILL
        ),
        PetSpec(
            id = "pet_14_peacock", number = 14, name = "Peacock",
            category = PetCategory.FLYING, rarity = "Epic", priceCoins = 620L,
            personality = "Dazzling emerald plume dancer inspiring shield aura.",
            passiveName = "Emerald Plume", passiveSummary = "+7% maximum Shield bonus.",
            passiveType = PetPassiveType.MAX_SHIELD_BONUS, passiveBaseValue = 0.07f,
            activeName = "Radiant Fan", activeSummary = "Restore +40 HP & +40 Shield shockwave.",
            activeType = PetActiveType.TEAM_HEAL_SHIELD, activeBaseValue = 40f,
            activeCooldownSec = 14f, epCost = 22f,
            primaryColorHex = 0xFF0EA5E9, secondaryColorHex = 0xFF10B981, accentColorHex = 0xFFFACC15, eyeColorHex = 0xFF6366F1,
            archetype = PetArchetype.AVIAN_SONGBIRD, motionType = PetMotionType.FLY_HOVER, soundProfile = PetSoundProfile.SONGBIRD_TRILL
        ),
        PetSpec(
            id = "pet_15_falcon", number = 15, name = "Falcon",
            category = PetCategory.FLYING, rarity = "Epic", priceCoins = 590L,
            personality = "Supersonic dive-striker tuned for rapid weapon handling.",
            passiveName = "Quick Talon", passiveSummary = "+12% weapon reload speed bonus.",
            passiveType = PetPassiveType.RELOAD_SPEED, passiveBaseValue = 0.12f,
            activeName = "Gale Dive", activeSummary = "Quick dash + instant weapon reload.",
            activeType = PetActiveType.QUICK_DASH, activeBaseValue = 11f,
            activeCooldownSec = 10f, epCost = 18f,
            primaryColorHex = 0xFF475569, secondaryColorHex = 0xFFE2E8F0, accentColorHex = 0xFF00E5FF, eyeColorHex = 0xFFFACC15,
            archetype = PetArchetype.AVIAN_RAPTOR, motionType = PetMotionType.FLY_HOVER, soundProfile = PetSoundProfile.AVIAN_SCREECH
        ),
        PetSpec(
            id = "pet_16_crow", number = 16, name = "Crow",
            category = PetCategory.FLYING, rarity = "Epic", priceCoins = 540L,
            personality = "Shinobi midnight messenger carrying genjutsu feathers.",
            passiveName = "Shinobi Crows", passiveSummary = "Recover 12 EP after each elimination.",
            passiveType = PetPassiveType.EP_ON_KILL, passiveBaseValue = 12f,
            activeName = "Shadow Flock", activeSummary = "Camouflage cloak + 6s enemy detection.",
            activeType = PetActiveType.CAMOUFLAGE_CLOAK, activeBaseValue = 6f,
            activeCooldownSec = 12f, epCost = 18f,
            primaryColorHex = 0xFF0F172A, secondaryColorHex = 0xFF312E81, accentColorHex = 0xFFA855F7, eyeColorHex = 0xFFEF4444,
            archetype = PetArchetype.AVIAN_RAPTOR, motionType = PetMotionType.FLY_HOVER, soundProfile = PetSoundProfile.AVIAN_SCREECH
        ),
        PetSpec(
            id = "pet_17_swan", number = 17, name = "Swan",
            category = PetCategory.FLYING, rarity = "Rare", priceCoins = 460L,
            personality = "Graceful lotus-lake companion radiating soothing aura.",
            passiveName = "Serene Ripples", passiveSummary = "Recover 14 EP every 25 seconds.",
            passiveType = PetPassiveType.PERIODIC_EP, passiveBaseValue = 14f,
            activeName = "Graceful Ward", activeSummary = "Restore +45 HP & +35 Shield smoothly.",
            activeType = PetActiveType.TEAM_HEAL_SHIELD, activeBaseValue = 45f,
            activeCooldownSec = 13f, epCost = 16f,
            primaryColorHex = 0xFFFFFFFF, secondaryColorHex = 0xFFE0F2FE, accentColorHex = 0xFF38BDF8, eyeColorHex = 0xFF0F172A,
            archetype = PetArchetype.AVIAN_WATERFOWL, motionType = PetMotionType.FLY_HOVER, soundProfile = PetSoundProfile.SONGBIRD_TRILL
        ),
        PetSpec(
            id = "pet_18_duck", number = 18, name = "Duck",
            category = PetCategory.FLYING, rarity = "Common", priceCoins = 260L,
            personality = "Plucky little pond duck splashing happily beside you.",
            passiveName = "Paddle Boost", passiveSummary = "+18% faster movement in water.",
            passiveType = PetPassiveType.WATER_SPEED, passiveBaseValue = 0.18f,
            activeName = "Quack Pep", activeSummary = "Instant +25 HP & +25 EP Cheer!",
            activeType = PetActiveType.HP_AND_EP_RECOVER, activeBaseValue = 25f,
            activeCooldownSec = 10f, epCost = 10f,
            primaryColorHex = 0xFFFEF08A, secondaryColorHex = 0xFFF97316, accentColorHex = 0xFF22C55E, eyeColorHex = 0xFF0F172A,
            archetype = PetArchetype.AVIAN_WATERFOWL, motionType = PetMotionType.GROUND_WALK, soundProfile = PetSoundProfile.CHIRP_YIP
        ),
        PetSpec(
            id = "pet_19_phoenix", number = 19, name = "Phoenix",
            category = PetCategory.FLYING, rarity = "Legendary", priceCoins = 920L,
            personality = "Immortal crimson solar bird who refuses to let heroes fall.",
            passiveName = "Ashes Rebirth", passiveSummary = "Emergency save with 70 HP once per life.",
            passiveType = PetPassiveType.EMERGENCY_SAVE, passiveBaseValue = 70f,
            activeName = "Rebirth Flame", activeSummary = "Restore +60 HP + 70 AoE fire wave.",
            activeType = PetActiveType.REBIRTH_FLAME, activeBaseValue = 70f,
            activeCooldownSec = 16f, epCost = 28f,
            primaryColorHex = 0xFFDC2626, secondaryColorHex = 0xFFFFD166, accentColorHex = 0xFFF97316, eyeColorHex = 0xFFFFD166,
            archetype = PetArchetype.AVIAN_PHOENIX, motionType = PetMotionType.FLY_HOVER, soundProfile = PetSoundProfile.CELESTIAL_CHORD
        ),
        PetSpec(
            id = "pet_20_firebird", number = 20, name = "Firebird",
            category = PetCategory.FLYING, rarity = "Mythic", priceCoins = 860L,
            personality = "Blazing ember sprite leaving warm golden spark trails.",
            passiveName = "Ember Surge", passiveSummary = "+7% attack bonus with fiery strikes.",
            passiveType = PetPassiveType.ATTACK_BONUS, passiveBaseValue = 0.07f,
            activeName = "Crimson Nova", activeSummary = "85 AoE blast + 6s attack buff.",
            activeType = PetActiveType.AOE_BLAST, activeBaseValue = 85f,
            activeCooldownSec = 14f, epCost = 26f,
            primaryColorHex = 0xFFF97316, secondaryColorHex = 0xFFEF4444, accentColorHex = 0xFFFEF08A, eyeColorHex = 0xFFFFFFFF,
            archetype = PetArchetype.AVIAN_PHOENIX, motionType = PetMotionType.FLY_HOVER, soundProfile = PetSoundProfile.AVIAN_SCREECH
        ),

        // ==================== 3. REPTILES & FANTASY (21-30) ====================
        PetSpec(
            id = "pet_21_dragon", number = 21, name = "Dragon",
            category = PetCategory.REPTILE_FANTASY, rarity = "Mythic", priceCoins = 950L,
            personality = "Fierce shadow-violet sky drake loyal to true warriors.",
            passiveName = "Draconic Might", passiveSummary = "+8% attack bonus & fierce intimidation.",
            passiveType = PetPassiveType.ATTACK_BONUS, passiveBaseValue = 0.08f,
            activeName = "Dragon Roar", activeSummary = "Massive 90 AoE knockback wave.",
            activeType = PetActiveType.AOE_BLAST, activeBaseValue = 90f,
            activeCooldownSec = 11f, epCost = 30f,
            primaryColorHex = 0xFF311042, secondaryColorHex = 0xFFA855F7, accentColorHex = 0xFFFFD166, eyeColorHex = 0xFFFF3B5C,
            archetype = PetArchetype.DRAGON, motionType = PetMotionType.FLY_HOVER, soundProfile = PetSoundProfile.DRAGON_RUMBLE
        ),
        PetSpec(
            id = "pet_22_baby_dragon", number = 22, name = "Baby Dragon",
            category = PetCategory.REPTILE_FANTASY, rarity = "Epic", priceCoins = 560L,
            personality = "Adorable oversized-head hatchling sneezing cute sparks.",
            passiveName = "Hatchling Growth", passiveSummary = "+25% bonus Pet XP & +4% attack.",
            passiveType = PetPassiveType.PET_XP_BONUS, passiveBaseValue = 0.25f,
            activeName = "Mini Flame Breath", activeSummary = "60 AoE ember burst + +20 EP.",
            activeType = PetActiveType.AOE_BLAST, activeBaseValue = 60f,
            activeCooldownSec = 9f, epCost = 18f,
            primaryColorHex = 0xFFFB7185, secondaryColorHex = 0xFFFACC15, accentColorHex = 0xFFF97316, eyeColorHex = 0xFF38BDF8,
            archetype = PetArchetype.DRAGON, motionType = PetMotionType.FLY_HOVER, soundProfile = PetSoundProfile.CHIRP_YIP
        ),
        PetSpec(
            id = "pet_23_turtle", number = 23, name = "Turtle",
            category = PetCategory.REPTILE_FANTASY, rarity = "Rare", priceCoins = 420L,
            personality = "Steady jade-shelled bastion who never backs down.",
            passiveName = "Jade Carapace", passiveSummary = "+8% damage resistance & shell defense.",
            passiveType = PetPassiveType.DAMAGE_RESIST, passiveBaseValue = 0.08f,
            activeName = "Iron Shell", activeSummary = "Temporary +65 Shield fortress.",
            activeType = PetActiveType.TEMP_SHIELD, activeBaseValue = 65f,
            activeCooldownSec = 14f, epCost = 20f,
            primaryColorHex = 0xFF15803D, secondaryColorHex = 0xFF86EFAC, accentColorHex = 0xFFFACC15, eyeColorHex = 0xFF0F172A,
            archetype = PetArchetype.TURTLE, motionType = PetMotionType.GROUND_WALK, soundProfile = PetSoundProfile.AQUA_SONAR
        ),
        PetSpec(
            id = "pet_24_snake", number = 24, name = "Snake",
            category = PetCategory.REPTILE_FANTASY, rarity = "Rare", priceCoins = 390L,
            personality = "Sleek emerald ninja serpent gliding quietly beside you.",
            passiveName = "Silent Fang", passiveSummary = "18% stealth camouflage + +5% speed.",
            passiveType = PetPassiveType.CAMOUFLAGE, passiveBaseValue = 0.18f,
            activeName = "Venom Strike", activeSummary = "65 AoE toxic shockwave + 5s speed.",
            activeType = PetActiveType.SPEED_AND_ATTACK, activeBaseValue = 5f,
            activeCooldownSec = 12f, epCost = 20f,
            primaryColorHex = 0xFF10B981, secondaryColorHex = 0xFF064E3B, accentColorHex = 0xFFA3E635, eyeColorHex = 0xFFFACC15,
            archetype = PetArchetype.SERPENT, motionType = PetMotionType.SLITHER, soundProfile = PetSoundProfile.CHIRP_YIP
        ),
        PetSpec(
            id = "pet_25_lizard", number = 25, name = "Lizard",
            category = PetCategory.REPTILE_FANTASY, rarity = "Common", priceCoins = 290L,
            personality = "Zippy sun-basking gecko cousin with lightning reflexes.",
            passiveName = "Quick Reflex", passiveSummary = "+10% faster weapon reload speed.",
            passiveType = PetPassiveType.RELOAD_SPEED, passiveBaseValue = 0.10f,
            activeName = "Tail Whip Dash", activeSummary = "Quick evasive dash + reload assist.",
            activeType = PetActiveType.QUICK_DASH, activeBaseValue = 8.5f,
            activeCooldownSec = 9f, epCost = 14f,
            primaryColorHex = 0xFF84CC16, secondaryColorHex = 0xFFFEF08A, accentColorHex = 0xFF22D3EE, eyeColorHex = 0xFF0F172A,
            archetype = PetArchetype.LIZARD_GECKO, motionType = PetMotionType.GROUND_WALK, soundProfile = PetSoundProfile.CHIRP_YIP
        ),
        PetSpec(
            id = "pet_26_crocodile", number = 26, name = "Crocodile",
            category = PetCategory.REPTILE_FANTASY, rarity = "Epic", priceCoins = 540L,
            personality = "Armored river chomper fiercely guarding its trainer.",
            passiveName = "Armored Scales", passiveSummary = "+7% max HP & sturdy jaw defense.",
            passiveType = PetPassiveType.MAX_HP_BONUS, passiveBaseValue = 0.07f,
            activeName = "Swamp Bastion", activeSummary = "6s damage protection + +12% attack.",
            activeType = PetActiveType.DAMAGE_PROTECTION, activeBaseValue = 6f,
            activeCooldownSec = 14f, epCost = 22f,
            primaryColorHex = 0xFF166534, secondaryColorHex = 0xFFBEF264, accentColorHex = 0xFFF59E0B, eyeColorHex = 0xFFFACC15,
            archetype = PetArchetype.CROCODILE, motionType = PetMotionType.GROUND_WALK, soundProfile = PetSoundProfile.DRAGON_RUMBLE
        ),
        PetSpec(
            id = "pet_27_gecko", number = 27, name = "Gecko",
            category = PetCategory.REPTILE_FANTASY, rarity = "Rare", priceCoins = 360L,
            personality = "Big-eyed pastel neon gecko sticking to any adventure.",
            passiveName = "Neon Grip", passiveSummary = "+7% movement speed + crouch agility.",
            passiveType = PetPassiveType.MOVEMENT_SPEED, passiveBaseValue = 0.07f,
            activeName = "Agile Leap", activeSummary = "Quick dash + +25 HP recover.",
            activeType = PetActiveType.QUICK_DASH, activeBaseValue = 9f,
            activeCooldownSec = 9f, epCost = 14f,
            primaryColorHex = 0xFF2DD4BF, secondaryColorHex = 0xFFF472B6, accentColorHex = 0xFFFEF08A, eyeColorHex = 0xFF1E293B,
            archetype = PetArchetype.LIZARD_GECKO, motionType = PetMotionType.GROUND_WALK, soundProfile = PetSoundProfile.CHIRP_YIP
        ),
        PetSpec(
            id = "pet_28_chameleon", number = 28, name = "Chameleon",
            category = PetCategory.REPTILE_FANTASY, rarity = "Epic", priceCoins = 580L,
            personality = "Shy prism-skinned spy master blending into any district.",
            passiveName = "Prism Camo", passiveSummary = "28% enemy detection reduction.",
            passiveType = PetPassiveType.CAMOUFLAGE, passiveBaseValue = 0.28f,
            activeName = "Optical Cloak", activeSummary = "7s stealth cloak + speed boost.",
            activeType = PetActiveType.CAMOUFLAGE_CLOAK, activeBaseValue = 7f,
            activeCooldownSec = 13f, epCost = 18f,
            primaryColorHex = 0xFF34D399, secondaryColorHex = 0xFFA855F7, accentColorHex = 0xFF00E5FF, eyeColorHex = 0xFFFACC15,
            archetype = PetArchetype.LIZARD_GECKO, motionType = PetMotionType.GROUND_WALK, soundProfile = PetSoundProfile.CRYSTAL_CHIME
        ),
        PetSpec(
            id = "pet_29_sea_serpent", number = 29, name = "Sea Serpent",
            category = PetCategory.REPTILE_FANTASY, rarity = "Legendary", priceCoins = 780L,
            personality = "Leviathan shrine spirit surfing shimmering blue currents.",
            passiveName = "Tidal Scale", passiveSummary = "+22% swim speed + +6% max Shield.",
            passiveType = PetPassiveType.WATER_SPEED, passiveBaseValue = 0.22f,
            activeName = "Tidal Surge", activeSummary = "75 AoE hydro wave + +35 Shield.",
            activeType = PetActiveType.AOE_BLAST, activeBaseValue = 75f,
            activeCooldownSec = 12f, epCost = 24f,
            primaryColorHex = 0xFF0284C7, secondaryColorHex = 0xFF38BDF8, accentColorHex = 0xFF67E8F9, eyeColorHex = 0xFFFEF08A,
            archetype = PetArchetype.SERPENT, motionType = PetMotionType.SLITHER, soundProfile = PetSoundProfile.DRAGON_RUMBLE
        ),
        PetSpec(
            id = "pet_30_crystal_dragon", number = 30, name = "Crystal Dragon",
            category = PetCategory.REPTILE_FANTASY, rarity = "Mythic", priceCoins = 1100L,
            personality = "Luminous diamond-horned dragon refracting chakra light.",
            passiveName = "Prism Aegis", passiveSummary = "+10% maximum Shield bonus.",
            passiveType = PetPassiveType.MAX_SHIELD_BONUS, passiveBaseValue = 0.10f,
            activeName = "Crystal Bastion", activeSummary = "+75 Shield + 95 prism AoE blast.",
            activeType = PetActiveType.TEMP_SHIELD, activeBaseValue = 75f,
            activeCooldownSec = 14f, epCost = 28f,
            primaryColorHex = 0xFFE0F2FE, secondaryColorHex = 0xFF00E5FF, accentColorHex = 0xFFA855F7, eyeColorHex = 0xFF00E5FF,
            archetype = PetArchetype.DRAGON, motionType = PetMotionType.CRYSTAL_FLOAT, soundProfile = PetSoundProfile.CRYSTAL_CHIME
        ),

        // ==================== 4. WATER ANIMALS (31-40) ====================
        PetSpec(
            id = "pet_31_dolphin", number = 31, name = "Dolphin",
            category = PetCategory.WATER, rarity = "Epic", priceCoins = 490L,
            personality = "Playful coastal acrobat emitting friendly sonar rings.",
            passiveName = "Sonar Wave", passiveSummary = "+24% water speed + enemy sonar ping.",
            passiveType = PetPassiveType.WATER_SPEED, passiveBaseValue = 0.24f,
            activeName = "Aqua Sonar", activeSummary = "8s enemy & loot sonar scan + +25 HP.",
            activeType = PetActiveType.ENEMY_RADAR_PULSE, activeBaseValue = 8f,
            activeCooldownSec = 11f, epCost = 16f,
            primaryColorHex = 0xFF38BDF8, secondaryColorHex = 0xFFF0F9FF, accentColorHex = 0xFF00E5FF, eyeColorHex = 0xFF0F172A,
            archetype = PetArchetype.DOLPHIN_WHALE_SHARK, motionType = PetMotionType.SWIM_FLOAT, soundProfile = PetSoundProfile.AQUA_SONAR
        ),
        PetSpec(
            id = "pet_32_whale", number = 32, name = "Whale",
            category = PetCategory.WATER, rarity = "Legendary", priceCoins = 740L,
            personality = "Gentle floating sky-whale humming deep ocean lullabies.",
            passiveName = "Ocean Fortress", passiveSummary = "+9% maximum HP bonus.",
            passiveType = PetPassiveType.MAX_HP_BONUS, passiveBaseValue = 0.09f,
            activeName = "Tidal Sanctuary", activeSummary = "7s damage protection + +45 Shield.",
            activeType = PetActiveType.DAMAGE_PROTECTION, activeBaseValue = 7f,
            activeCooldownSec = 15f, epCost = 24f,
            primaryColorHex = 0xFF1D4ED8, secondaryColorHex = 0xFFBAE6FD, accentColorHex = 0xFF60A5FA, eyeColorHex = 0xFFFEF08A,
            archetype = PetArchetype.DOLPHIN_WHALE_SHARK, motionType = PetMotionType.SWIM_FLOAT, soundProfile = PetSoundProfile.AQUA_SONAR
        ),
        PetSpec(
            id = "pet_33_shark", number = 33, name = "Shark",
            category = PetCategory.WATER, rarity = "Epic", priceCoins = 600L,
            personality = "Bold little chibi shark wearing a fiery determination grin.",
            passiveName = "Riptide Bite", passiveSummary = "+6% attack bonus & swift pursuit.",
            passiveType = PetPassiveType.ATTACK_BONUS, passiveBaseValue = 0.06f,
            activeName = "Feeding Frenzy", activeSummary = "6s speed + attack surge.",
            activeType = PetActiveType.SPEED_AND_ATTACK, activeBaseValue = 6f,
            activeCooldownSec = 13f, epCost = 22f,
            primaryColorHex = 0xFF64748B, secondaryColorHex = 0xFFF8FAFC, accentColorHex = 0xFFFF3B5C, eyeColorHex = 0xFF0F172A,
            archetype = PetArchetype.DOLPHIN_WHALE_SHARK, motionType = PetMotionType.SWIM_FLOAT, soundProfile = PetSoundProfile.PURR_ROAR
        ),
        PetSpec(
            id = "pet_34_seal", number = 34, name = "Seal",
            category = PetCategory.WATER, rarity = "Common", priceCoins = 280L,
            personality = "Fluffy round harbor pup clapping flippers with joy.",
            passiveName = "Cozy Blubber", passiveSummary = "Recover 16 HP every 35 seconds.",
            passiveType = PetPassiveType.PERIODIC_HP, passiveBaseValue = 16f,
            activeName = "Frost Bounce", activeSummary = "+30 HP & +30 EP cheerful bounce!",
            activeType = PetActiveType.HP_AND_EP_RECOVER, activeBaseValue = 30f,
            activeCooldownSec = 12f, epCost = 14f,
            primaryColorHex = 0xFFE2E8F0, secondaryColorHex = 0xFF94A3B8, accentColorHex = 0xFF38BDF8, eyeColorHex = 0xFF0F172A,
            archetype = PetArchetype.SEAL_OTTER, motionType = PetMotionType.SWIM_FLOAT, soundProfile = PetSoundProfile.CHIRP_YIP
        ),
        PetSpec(
            id = "pet_35_otter", number = 35, name = "Otter",
            category = PetCategory.WATER, rarity = "Rare", priceCoins = 380L,
            personality = "Resourceful river collector holding a lucky chakra shell.",
            passiveName = "Pearl Finder", passiveSummary = "Detect ground loot & weapons within 42m.",
            passiveType = PetPassiveType.LOOT_DETECTOR, passiveBaseValue = 42f,
            activeName = "River Blessing", activeSummary = "+40 Coins treasure find + +30 HP.",
            activeType = PetActiveType.LOOT_SCAN_AND_COIN, activeBaseValue = 40f,
            activeCooldownSec = 16f, epCost = 14f,
            primaryColorHex = 0xFF92400E, secondaryColorHex = 0xFFFDE68A, accentColorHex = 0xFF38BDF8, eyeColorHex = 0xFF0F172A,
            archetype = PetArchetype.SEAL_OTTER, motionType = PetMotionType.GROUND_WALK, soundProfile = PetSoundProfile.CHIRP_YIP
        ),
        PetSpec(
            id = "pet_36_penguin", number = 36, name = "Penguin",
            category = PetCategory.WATER, rarity = "Rare", priceCoins = 350L,
            personality = "Dapper tuxedo waddler who loves sliding down slopes.",
            passiveName = "Glacier Slide", passiveSummary = "+7% movement speed + icy poise.",
            passiveType = PetPassiveType.MOVEMENT_SPEED, passiveBaseValue = 0.07f,
            activeName = "Ice Slide Dash", activeSummary = "Quick slide dash + +35 Shield.",
            activeType = PetActiveType.QUICK_DASH, activeBaseValue = 10f,
            activeCooldownSec = 9f, epCost = 15f,
            primaryColorHex = 0xFF0F172A, secondaryColorHex = 0xFFFFFFFF, accentColorHex = 0xFFF97316, eyeColorHex = 0xFF38BDF8,
            archetype = PetArchetype.PENGUIN, motionType = PetMotionType.GROUND_WALK, soundProfile = PetSoundProfile.CHIRP_YIP
        ),
        PetSpec(
            id = "pet_37_octopus", number = 37, name = "Octopus",
            category = PetCategory.WATER, rarity = "Epic", priceCoins = 520L,
            personality = "Eight-armed tactical reload prodigy in a ninja headband.",
            passiveName = "Eight-Arm Loader", passiveSummary = "+15% weapon reload speed bonus.",
            passiveType = PetPassiveType.RELOAD_SPEED, passiveBaseValue = 0.15f,
            activeName = "Ink Veil", activeSummary = "Instant magazine refill + 6s stealth cloak.",
            activeType = PetActiveType.RELOAD_AND_FOCUS, activeBaseValue = 6f,
            activeCooldownSec = 12f, epCost = 18f,
            primaryColorHex = 0xFFEC4899, secondaryColorHex = 0xFFFBCFE8, accentColorHex = 0xFFA855F7, eyeColorHex = 0xFFFEF08A,
            archetype = PetArchetype.OCTOPUS, motionType = PetMotionType.SWIM_FLOAT, soundProfile = PetSoundProfile.AQUA_SONAR
        ),
        PetSpec(
            id = "pet_38_jellyfish", number = 38, name = "Jellyfish",
            category = PetCategory.WATER, rarity = "Rare", priceCoins = 410L,
            personality = "Translucent bioluminescent lantern drifting softly.",
            passiveName = "Bio-Luminescence", passiveSummary = "Recover 16 EP every 25 seconds.",
            passiveType = PetPassiveType.PERIODIC_EP, passiveBaseValue = 16f,
            activeName = "Volt Pulse", activeSummary = "60 AoE shock stun + +45 EP surge.",
            activeType = PetActiveType.EP_SURGE, activeBaseValue = 45f,
            activeCooldownSec = 11f, epCost = 12f,
            primaryColorHex = 0xFF22D3EE, secondaryColorHex = 0xFFC084FC, accentColorHex = 0xFF67E8F9, eyeColorHex = 0xFFFFFFFF,
            archetype = PetArchetype.JELLYFISH, motionType = PetMotionType.SWIM_FLOAT, soundProfile = PetSoundProfile.CRYSTAL_CHIME
        ),
        PetSpec(
            id = "pet_39_seahorse", number = 39, name = "Seahorse",
            category = PetCategory.WATER, rarity = "Rare", priceCoins = 370L,
            personality = "Regal coral steed fluttering golden fin wings.",
            passiveName = "Coral Armor", passiveSummary = "Recover 14 Shield every 30 seconds.",
            passiveType = PetPassiveType.PERIODIC_SHIELD, passiveBaseValue = 14f,
            activeName = "Coral Aura", activeSummary = "Restore +35 HP & +40 Shield cleanly.",
            activeType = PetActiveType.TEAM_HEAL_SHIELD, activeBaseValue = 35f,
            activeCooldownSec = 12f, epCost = 18f,
            primaryColorHex = 0xFFFBBF24, secondaryColorHex = 0xFFFB7185, accentColorHex = 0xFF2DD4BF, eyeColorHex = 0xFF0F172A,
            archetype = PetArchetype.SEAHORSE, motionType = PetMotionType.SWIM_FLOAT, soundProfile = PetSoundProfile.SONGBIRD_TRILL
        ),
        PetSpec(
            id = "pet_40_water_spirit", number = 40, name = "Water Spirit",
            category = PetCategory.WATER, rarity = "Mythic", priceCoins = 890L,
            personality = "Pure undulating aqua nymph crowned with a floating ring.",
            passiveName = "Spring Blessing", passiveSummary = "Recover 22 HP every 35s + swim speed.",
            passiveType = PetPassiveType.PERIODIC_HP, passiveBaseValue = 22f,
            activeName = "Tidal Blessing", activeSummary = "Restore +55 HP & +45 Shield ripple.",
            activeType = PetActiveType.TEAM_HEAL_SHIELD, activeBaseValue = 55f,
            activeCooldownSec = 14f, epCost = 20f,
            primaryColorHex = 0xFF00E5FF, secondaryColorHex = 0xFFE0F2FE, accentColorHex = 0xFF38BDF8, eyeColorHex = 0xFF0284C7,
            archetype = PetArchetype.WATER_SPIRIT, motionType = PetMotionType.CRYSTAL_FLOAT, soundProfile = PetSoundProfile.CELESTIAL_CHORD
        ),

        // ==================== 5. MAGICAL ANIMALS (41-50) ====================
        PetSpec(
            id = "pet_41_unicorn", number = 41, name = "Unicorn",
            category = PetCategory.MAGICAL, rarity = "Legendary", priceCoins = 880L,
            personality = "Starlight-horned celestial foal spreading pastel sparkles.",
            passiveName = "Starlight Aura", passiveSummary = "+8% speed & soothing light.",
            passiveType = PetPassiveType.MOVEMENT_SPEED, passiveBaseValue = 0.08f,
            activeName = "Starlight Blessing", activeSummary = "Restore +45 HP, +35 EP & 5s speed.",
            activeType = PetActiveType.HP_AND_EP_RECOVER, activeBaseValue = 45f,
            activeCooldownSec = 13f, epCost = 20f,
            primaryColorHex = 0xFFFDF2F8, secondaryColorHex = 0xFFF472B6, accentColorHex = 0xFFFFD166, eyeColorHex = 0xFF6366F1,
            archetype = PetArchetype.UNICORN, motionType = PetMotionType.GROUND_WALK, soundProfile = PetSoundProfile.CELESTIAL_CHORD
        ),
        PetSpec(
            id = "pet_42_kitsune", number = 42, name = "Kitsune",
            category = PetCategory.MAGICAL, rarity = "Mythic", priceCoins = 1050L,
            personality = "Sacred multi-tailed shrine guardian unlocking more tails per evolution.",
            passiveName = "Nine-Tail Soul", passiveSummary = "Recover 22 HP after an elimination.",
            passiveType = PetPassiveType.HP_ON_KILL, passiveBaseValue = 22f,
            activeName = "Sacred Foxfire", activeSummary = "Restore 45 HP over 5s + speed burst.",
            activeType = PetActiveType.HOT_HP, activeBaseValue = 45f,
            activeCooldownSec = 10f, epCost = 20f,
            primaryColorHex = 0xFFFFFFFF, secondaryColorHex = 0xFFFF1E42, accentColorHex = 0xFFFFD166, eyeColorHex = 0xFFFF3B5C,
            archetype = PetArchetype.KITSUNE_NINE, motionType = PetMotionType.GROUND_WALK, soundProfile = PetSoundProfile.CELESTIAL_CHORD
        ),
        PetSpec(
            id = "pet_43_spirit_fox", number = 43, name = "Spirit Fox",
            category = PetCategory.MAGICAL, rarity = "Legendary", priceCoins = 820L,
            personality = "Translucent cyan phantom fox phasing swiftly beside its master.",
            passiveName = "Astral Drain", passiveSummary = "Gain 22 EP after an elimination.",
            passiveType = PetPassiveType.EP_ON_KILL, passiveBaseValue = 22f,
            activeName = "Phantom Step", activeSummary = "Quick blink dash + +45 EP surge.",
            activeType = PetActiveType.EP_SURGE, activeBaseValue = 45f,
            activeCooldownSec = 10f, epCost = 12f,
            primaryColorHex = 0xFF67E8F9, secondaryColorHex = 0xFFA855F7, accentColorHex = 0xFF00E5FF, eyeColorHex = 0xFFFFFFFF,
            archetype = PetArchetype.KITSUNE_NINE, motionType = PetMotionType.CRYSTAL_FLOAT, soundProfile = PetSoundProfile.CRYSTAL_CHIME
        ),
        PetSpec(
            id = "pet_44_moon_wolf", number = 44, name = "Moon Wolf",
            category = PetCategory.MAGICAL, rarity = "Mythic", priceCoins = 940L,
            personality = "Silver-crescent alpha wolf who commands nocturnal awareness.",
            passiveName = "Crescent Eye", passiveSummary = "Detect nearby enemies (45m) + night crit.",
            passiveType = PetPassiveType.DETECT_ENEMIES, passiveBaseValue = 45f,
            activeName = "Lunar Howl", activeSummary = "10s enemy detection + +12% attack boost.",
            activeType = PetActiveType.ENEMY_RADAR_PULSE, activeBaseValue = 10f,
            activeCooldownSec = 13f, epCost = 22f,
            primaryColorHex = 0xFFE2E8F0, secondaryColorHex = 0xFF4338CA, accentColorHex = 0xFF60A5FA, eyeColorHex = 0xFF38BDF8,
            archetype = PetArchetype.FOX_CANINE, motionType = PetMotionType.GROUND_WALK, soundProfile = PetSoundProfile.HOWL_BARK
        ),
        PetSpec(
            id = "pet_45_shadow_panther", number = 45, name = "Shadow Panther",
            category = PetCategory.MAGICAL, rarity = "Legendary", priceCoins = 860L,
            personality = "Velvet-black assassin cat stalking silently in neon alleys.",
            passiveName = "Midnight Stalker", passiveSummary = "+9% speed & 20% stealth camo.",
            passiveType = PetPassiveType.MOVEMENT_SPEED, passiveBaseValue = 0.09f,
            activeName = "Shadow Pounce", activeSummary = "Stealth dash + 6s attack boost.",
            activeType = PetActiveType.SPEED_AND_ATTACK, activeBaseValue = 6f,
            activeCooldownSec = 12f, epCost = 20f,
            primaryColorHex = 0xFF0F172A, secondaryColorHex = 0xFF7C3AED, accentColorHex = 0xFFC084FC, eyeColorHex = 0xFFA855F7,
            archetype = PetArchetype.FELINE, motionType = PetMotionType.GROUND_WALK, soundProfile = PetSoundProfile.PURR_ROAR
        ),
        PetSpec(
            id = "pet_46_thunder_tiger", number = 46, name = "Thunder Tiger",
            category = PetCategory.MAGICAL, rarity = "Mythic", priceCoins = 980L,
            personality = "Crackling azure-gold tiger surging with Indra lightning.",
            passiveName = "Storm Reflex", passiveSummary = "+14% reload speed & electric charge.",
            passiveType = PetPassiveType.RELOAD_SPEED, passiveBaseValue = 0.14f,
            activeName = "Thunderclap Blitz", activeSummary = "6s speed + attack boost + shockwave.",
            activeType = PetActiveType.SPEED_AND_ATTACK, activeBaseValue = 6f,
            activeCooldownSec = 13f, epCost = 24f,
            primaryColorHex = 0xFF0EA5E9, secondaryColorHex = 0xFF1E293B, accentColorHex = 0xFFFACC15, eyeColorHex = 0xFFFFD166,
            archetype = PetArchetype.FELINE, motionType = PetMotionType.GROUND_WALK, soundProfile = PetSoundProfile.PURR_ROAR
        ),
        PetSpec(
            id = "pet_47_ice_deer", number = 47, name = "Ice Deer",
            category = PetCategory.MAGICAL, rarity = "Legendary", priceCoins = 790L,
            personality = "Crystalline frost stag conjuring shimmering ice shields.",
            passiveName = "Glacial Grace", passiveSummary = "Recover 20 Shield every 30 seconds.",
            passiveType = PetPassiveType.PERIODIC_SHIELD, passiveBaseValue = 20f,
            activeName = "Frost Sanctuary", activeSummary = "Temporary +75 Shield barrier.",
            activeType = PetActiveType.TEMP_SHIELD, activeBaseValue = 75f,
            activeCooldownSec = 13f, epCost = 22f,
            primaryColorHex = 0xFFE0F2FE, secondaryColorHex = 0xFF38BDF8, accentColorHex = 0xFFA5F3FC, eyeColorHex = 0xFF00E5FF,
            archetype = PetArchetype.DEER, motionType = PetMotionType.CRYSTAL_FLOAT, soundProfile = PetSoundProfile.CRYSTAL_CHIME
        ),
        PetSpec(
            id = "pet_48_flame_lion", number = 48, name = "Flame Lion",
            category = PetCategory.MAGICAL, rarity = "Mythic", priceCoins = 960L,
            personality = "Blazing solar king whose fiery mane illuminates battlefields.",
            passiveName = "Solar Fury", passiveSummary = "+9% attack bonus.",
            passiveType = PetPassiveType.ATTACK_BONUS, passiveBaseValue = 0.09f,
            activeName = "Solar Flare Roar", activeSummary = "7s strong attack boost + fire wave.",
            activeType = PetActiveType.ATTACK_BOOST, activeBaseValue = 7f,
            activeCooldownSec = 13f, epCost = 24f,
            primaryColorHex = 0xFFEF4444, secondaryColorHex = 0xFFF59E0B, accentColorHex = 0xFFFEF08A, eyeColorHex = 0xFFFFD166,
            archetype = PetArchetype.FELINE, motionType = PetMotionType.GROUND_WALK, soundProfile = PetSoundProfile.PURR_ROAR
        ),
        PetSpec(
            id = "pet_49_crystal_golem", number = 49, name = "Crystal Golem",
            category = PetCategory.MAGICAL, rarity = "Legendary", priceCoins = 900L,
            personality = "Adorable floating rune-stone construct protecting allies.",
            passiveName = "Diamond Bulwark", passiveSummary = "+10% damage resistance & shield.",
            passiveType = PetPassiveType.DAMAGE_RESIST, passiveBaseValue = 0.10f,
            activeName = "Runic Fortress", activeSummary = "7s 45% damage shield + +50 Shield.",
            activeType = PetActiveType.DAMAGE_PROTECTION, activeBaseValue = 7f,
            activeCooldownSec = 15f, epCost = 24f,
            primaryColorHex = 0xFF475569, secondaryColorHex = 0xFF00E5FF, accentColorHex = 0xFFA855F7, eyeColorHex = 0xFF00E5FF,
            archetype = PetArchetype.CRYSTAL_GOLEM, motionType = PetMotionType.CRYSTAL_FLOAT, soundProfile = PetSoundProfile.CRYSTAL_CHIME
        ),
        PetSpec(
            id = "pet_50_celestial_beast", number = 50, name = "Celestial Beast",
            category = PetCategory.MAGICAL, rarity = "Mythic", priceCoins = 1400L,
            personality = "Ascended cosmic guardian uniting all five elemental realms.",
            passiveName = "Astral Harmony", passiveSummary = "+5% HP/Shield/Atk/Speed & 25m radar.",
            passiveType = PetPassiveType.OMNI_HARMONY, passiveBaseValue = 0.05f,
            activeName = "Astral Nova", activeSummary = "+50 HP, +50 EP, +50 Shield & AoE blast.",
            activeType = PetActiveType.TEAM_HEAL_SHIELD, activeBaseValue = 50f,
            activeCooldownSec = 15f, epCost = 28f,
            primaryColorHex = 0xFF1E1B4B, secondaryColorHex = 0xFFFFD166, accentColorHex = 0xFF00E5FF, eyeColorHex = 0xFFFFD166,
            archetype = PetArchetype.CELESTIAL_BEAST, motionType = PetMotionType.CRYSTAL_FLOAT, soundProfile = PetSoundProfile.CELESTIAL_CHORD
        )
    )

    val wildWorldSeeds: List<WildPetSpawnSeed> = listOf(
        WildPetSpawnSeed("wild_kitsune_forest", "pet_42_kitsune", 2, "Forest Region", 520f, -310f, "Playful Shrine Spirit"),
        WildPetSpawnSeed("wild_crystal_dragon_mtn", "pet_30_crystal_dragon", 3, "Mountain Region", 24f, -685f, "Ancient Ridge Guardian"),
        WildPetSpawnSeed("wild_dolphin_coast", "pet_31_dolphin", 2, "Coastal Region", -18f, 595f, "Friendly Harbor Acrobat"),
        WildPetSpawnSeed("wild_unicorn_village", "pet_41_unicorn", 2, "Hidden Village", -345f, -360f, "Starlight Stray"),
        WildPetSpawnSeed("wild_phoenix_city", "pet_19_phoenix", 3, "Central Anime City", 34f, -18f, "Solar Shrine Sovereign"),
        WildPetSpawnSeed("wild_panda_residential", "pet_10_panda", 2, "Residential Region", -395f, 325f, "Bamboo Wanderer"),
        WildPetSpawnSeed("wild_moon_wolf_airport", "pet_44_moon_wolf", 2, "Airport", -505f, -180f, "Silver Scout"),
        WildPetSpawnSeed("wild_water_spirit_river", "pet_40_water_spirit", 3, "Forest Region", 585f, -380f, "Spring Nymph")
    )

    fun resolveSpec(idOrName: String?): PetSpec {
        if (idOrName.isNullOrBlank()) return allPets.first()
        val key = idOrName.trim().lowercase()
        // Legacy aliases so starter profiles & old names always resolve to rich 50-pet specs
        when (key) {
            "anime fox", "pet_anime_fox", "fox" -> return allPets[0]
            "cyber wolf", "wolf" -> return allPets[1]
            "robotic dog", "dog" -> return allPets[2]
            "crimson phoenix", "phoenix" -> return allPets[18]
            "shadow dragon", "dragon" -> return allPets[20]
        }
        return allPets.firstOrNull {
            it.id.equals(key, ignoreCase = true) ||
                it.name.equals(key, ignoreCase = true) ||
                it.activeName.equals(key, ignoreCase = true)
        } ?: allPets.firstOrNull {
            key.contains(it.name.lowercase())
        } ?: allPets.first()
    }

    fun resolveByActiveAbilityName(abilityName: String): PetSpec? {
        return allPets.firstOrNull { it.activeName.equals(abilityName, ignoreCase = true) }
    }

    fun findByCategory(category: PetCategory?): List<PetSpec> =
        if (category == null) allPets else allPets.filter { it.category == category }

    /**
     * Computes combined passive bonuses from up to 5 equipped pets,
     * applying strict anti-stacking caps across all stat dimensions.
     */
    fun computeCappedPassives(equippedPets: List<OwnedPet>): CappedPetPassives {
        val activeRoster = equippedPets.filter { it.equipped }.take(MAX_EQUIPPED_ROSTER)
        if (activeRoster.isEmpty()) return CappedPetPassives()

        var speed = 0f
        var maxHp = 0f
        var maxShield = 0f
        var atk = 0f
        var resist = 0f
        var reload = 0f
        var waterSpeed = 0f
        var hpKill = 0f
        var epKill = 0f
        var perHp = 0f
        var perShield = 0f
        var perEp = 0f
        var detectEnemy = 0f
        var detectLoot = 0f
        var camo = 0f
        var nightCrit = 0f
        var petXpBonus = 0f
        var emergencySave = false

        for (pet in activeRoster) {
            val spec = resolveSpec(pet.petType.ifBlank { pet.id })
            val v = spec.passiveScaledValue(pet.effectiveEvolutionLevel())
            when (spec.passiveType) {
                PetPassiveType.HP_ON_KILL -> hpKill += v
                PetPassiveType.EP_ON_KILL -> epKill += v
                PetPassiveType.HP_AND_EP_ON_KILL -> {
                    hpKill += v
                    epKill += v * 0.4f
                }
                PetPassiveType.DETECT_ENEMIES -> detectEnemy = maxOf(detectEnemy, v)
                PetPassiveType.MOVEMENT_SPEED -> speed += v
                PetPassiveType.ATTACK_BONUS -> atk += v
                PetPassiveType.PERIODIC_SHIELD -> perShield += v
                PetPassiveType.PERIODIC_HP -> perHp += v
                PetPassiveType.PERIODIC_EP -> perEp += v
                PetPassiveType.MAX_HP_BONUS -> maxHp += v
                PetPassiveType.MAX_SHIELD_BONUS -> maxShield += v
                PetPassiveType.RELOAD_SPEED -> reload += v
                PetPassiveType.WATER_SPEED -> waterSpeed += v
                PetPassiveType.CAMOUFLAGE -> camo = maxOf(camo, v)
                PetPassiveType.NIGHT_VISION_CRIT -> nightCrit += v
                PetPassiveType.LOOT_DETECTOR -> detectLoot = maxOf(detectLoot, v)
                PetPassiveType.PET_XP_BONUS -> {
                    petXpBonus += v
                    atk += 0.04f
                }
                PetPassiveType.EMERGENCY_SAVE -> emergencySave = true
                PetPassiveType.DAMAGE_RESIST -> resist += v
                PetPassiveType.OMNI_HARMONY -> {
                    speed += v
                    maxHp += v
                    maxShield += v
                    atk += v
                    detectEnemy = maxOf(detectEnemy, 25f)
                }
            }
        }

        return CappedPetPassives(
            equippedCount = activeRoster.size,
            speedBonusPct = min(speed, CAP_SPEED_BONUS),
            maxHpBonusPct = min(maxHp, CAP_MAX_HP_BONUS),
            maxShieldBonusPct = min(maxShield, CAP_MAX_SHIELD_BONUS),
            attackBonusPct = min(atk, CAP_ATTACK_BONUS),
            damageResistPct = min(resist, CAP_DAMAGE_RESIST),
            reloadSpeedBonusPct = min(reload, CAP_RELOAD_BONUS),
            waterSpeedBonusPct = min(waterSpeed, CAP_WATER_SPEED_BONUS),
            hpOnKill = min(hpKill, CAP_HP_ON_KILL),
            epOnKill = min(epKill, CAP_EP_ON_KILL),
            periodicHpPer30s = min(perHp, CAP_PERIODIC_HP),
            periodicShieldPer30s = min(perShield, CAP_PERIODIC_SHIELD),
            periodicEpPer25s = min(perEp, CAP_PERIODIC_EP),
            enemyDetectRadius = min(detectEnemy, CAP_ENEMY_DETECT_RADIUS),
            lootDetectRadius = min(detectLoot, CAP_LOOT_DETECT_RADIUS),
            camouflageFactor = min(camo, CAP_CAMOUFLAGE),
            nightVisionCritBonus = min(nightCrit, 0.12f),
            petXpMultiplier = 1f + min(petXpBonus, 0.35f),
            hasEmergencySave = emergencySave
        )
    }
}
