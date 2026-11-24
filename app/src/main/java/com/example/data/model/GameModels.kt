package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class PlayerProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val playerCode: String = "",
    val username: String = "ShinobiHero",
    val guild: String = "SHINOBI",
    val level: Long = 1L,
    val xp: Long = 0L,
    val rankTitle: String = "Initiate",
    val coins: Long = 1250L,
    val diamonds: Long = 540L,
    val upgradePoints: Long = 5L,
    val kills: Long = 0L,
    val deaths: Long = 0L,
    val worldDistrict: String = "Central Anime City",
    val posX: Double = 0.0,
    val posY: Double = 0.0,
    val posZ: Double = 14.0,
    val rotY: Double = 180.0,
    val hp: Double = 200.0,
    val ep: Double = 200.0,
    val shield: Double = 50.0,
    val onlineStatus: Boolean = true,
    val aiProxyActive: Boolean = false,
    val aiProxyLastAction: String = "Patrolling Sakura Torii Plaza",
    val discoverable: Boolean = true,
    val activeVehicleId: String = "veh_kitsune_gtr",
    val activePetId: String = "pet_anime_fox",
    val activeWeaponId: String = "wpn_vortex_ar",
    val hairStyle: String = "Black",
    val outfitStyle: String = "Shadow Blaze",
    val accessoryStyle: String = "Katana",
    val accentColorHex: String = "#FF3B5C",
    val equippedAbilities: List<String> = listOf(
        "Crimson Slash",
        "Lightning Dash",
        "Fox Flame",
        "Aegis Pulse"
    ),
    val unlockedWeapons: List<String> = listOf(
        "wpn_vortex_ar",
        "wpn_pulse_pistol",
        "wpn_shadow_katana"
    ),
    val unlockedVehicles: List<String> = listOf(
        "veh_kitsune_gtr",
        "veh_shinobi_moto"
    ),
    val unlockedProperties: List<String> = emptyList(),
    val unlockedCharSlots: Long = 2L,
    val unlockedPetSlots: Long = 2L,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationAccuracyMeters: Double? = null,
    val locationTrackingEnabled: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("playerCode", playerCode)
        put("username", username.take(30).ifBlank { "ShinobiHero" })
        put("guild", guild.take(24).ifBlank { "SHINOBI" })
        put("level", level.coerceIn(1L, 999L))
        put("xp", xp.coerceAtLeast(0L))
        put("rankTitle", rankTitle.take(40).ifBlank { "Initiate" })
        put("coins", coins.coerceAtLeast(0L))
        put("diamonds", diamonds.coerceAtLeast(0L))
        put("upgradePoints", upgradePoints.coerceAtLeast(0L))
        put("kills", kills.coerceAtLeast(0L))
        put("deaths", deaths.coerceAtLeast(0L))
        put("worldDistrict", worldDistrict.take(50).ifBlank { "Central Anime City" })
        put("posX", posX.coerceIn(-1200.0, 1200.0))
        put("posY", posY.coerceIn(-50.0, 400.0))
        put("posZ", posZ.coerceIn(-1200.0, 1200.0))
        put("rotY", rotY)
        put("hp", hp.coerceIn(0.0, 1000.0))
        put("ep", ep.coerceIn(0.0, 1000.0))
        put("shield", shield.coerceIn(0.0, 1000.0))
        put("onlineStatus", onlineStatus)
        put("aiProxyActive", aiProxyActive)
        put("aiProxyLastAction", aiProxyLastAction.take(140))
        put("discoverable", discoverable)
        put("activeVehicleId", activeVehicleId.take(64))
        put("activePetId", activePetId.take(64))
        put("activeWeaponId", activeWeaponId.take(64).ifBlank { "wpn_vortex_ar" })
        put("hairStyle", hairStyle.take(40).ifBlank { "Black" })
        put("outfitStyle", outfitStyle.take(40).ifBlank { "Shadow Blaze" })
        put("accessoryStyle", accessoryStyle.take(40).ifBlank { "Katana" })
        put("accentColorHex", accentColorHex.take(16).ifBlank { "#FF3B5C" })
        put("equippedAbilities", equippedAbilities.take(8))
        put("unlockedWeapons", unlockedWeapons.take(16))
        put("unlockedVehicles", unlockedVehicles.take(16))
        put("unlockedProperties", unlockedProperties.take(16))
        put("unlockedCharSlots", unlockedCharSlots.coerceIn(2L, 3L))
        put("unlockedPetSlots", unlockedPetSlots.coerceIn(2L, 4L))
        latitude?.takeIf { it.isFinite() }?.let { put("latitude", it.coerceIn(-90.0, 90.0)) }
        longitude?.takeIf { it.isFinite() }?.let { put("longitude", it.coerceIn(-180.0, 180.0)) }
        locationAccuracyMeters?.takeIf { it.isFinite() }?.let { put("locationAccuracyMeters", it.coerceIn(0.0, 50000.0)) }
        put("locationTrackingEnabled", locationTrackingEnabled)
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class OwnedPet(
    @DocumentId val id: String = "",
    val userId: String = "",
    val petType: String = "Fox",
    val nickname: String = "Kitsune",
    val rarity: String = "Mythic",
    val level: Long = 1L,
    val xp: Long = 0L,
    val skinId: String = "nine_tails_crimson",
    val abilityId: String = "Fox Flame",
    val equipped: Boolean = true,
    val evolutionLevel: Long = 1L,
    val customTintHex: String = "",
    val auraStyle: String = "Classic",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun effectiveEvolutionLevel(): Int = evolutionLevel.toInt().coerceIn(1, 3)

    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("petType", petType.take(40))
        put("nickname", nickname.take(30).ifBlank { petType.take(30) })
        put("rarity", rarity.take(20))
        put("level", level.coerceIn(1L, 100L))
        put("xp", xp.coerceAtLeast(0L))
        put("skinId", skinId.take(40))
        put("abilityId", abilityId.take(40))
        put("equipped", equipped)
        put("evolutionLevel", evolutionLevel.coerceIn(1L, 3L))
        if (customTintHex.isNotBlank()) put("customTintHex", customTintHex.take(16))
        put("auraStyle", auraStyle.take(24).ifBlank { "Classic" })
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class PetTradeOffer(
    @DocumentId val id: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val receiverUid: String = "",
    val receiverName: String = "",
    val participantUids: List<String> = emptyList(),
    val offeredPetId: String = "",
    val offeredPetType: String = "",
    val offeredEvolutionLevel: Long = 1L,
    val requestedPetType: String = "",
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "senderUid" to senderUid,
        "senderName" to senderName.take(30),
        "receiverUid" to receiverUid,
        "receiverName" to receiverName.take(30),
        "participantUids" to participantUids,
        "offeredPetId" to offeredPetId.take(64),
        "offeredPetType" to offeredPetType.take(40),
        "offeredEvolutionLevel" to offeredEvolutionLevel.coerceIn(1L, 3L),
        "requestedPetType" to requestedPetType.take(40),
        "status" to "pending",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class OwnedVehicle(
    @DocumentId val id: String = "",
    val userId: String = "",
    val modelId: String = "Kitsune GT-R",
    val category: String = "supercar",
    val paintHex: String = "#FF1E42",
    val skinName: String = "Sakura Carbon",
    val locked: Boolean = true,
    val alarmArmed: Boolean = true,
    val stolenByUid: String? = null,
    val garageSlot: Long = 1L,
    val upgradeLevel: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @Suppress("UNCHECKED_CAST")
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "modelId" to modelId.take(50),
        "category" to category,
        "paintHex" to paintHex.take(16),
        "skinName" to skinName.take(40),
        "locked" to locked,
        "alarmArmed" to alarmArmed,
        "stolenByUid" to stolenByUid,
        "garageSlot" to garageSlot.coerceIn(0L, 50L),
        "upgradeLevel" to upgradeLevel.coerceIn(1L, 20L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null } as Map<String, Any>
}

@Keep
@IgnoreExtraProperties
data class OwnedProperty(
    @DocumentId val id: String = "",
    val userId: String = "",
    val propertyType: String = "small_house",
    val district: String = "Residential Region",
    val customName: String = "Sakura Clan Residence",
    val upgradeLevel: Long = 1L,
    val decorTheme: String = "Neo-Tokyo Tatami",
    val furnitureCount: Long = 8L,
    val petAreaEnabled: Boolean = true,
    val garageCapacity: Long = 2L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "propertyType" to propertyType,
        "district" to district.take(50),
        "customName" to customName.take(50),
        "upgradeLevel" to upgradeLevel.coerceIn(1L, 10L),
        "decorTheme" to decorTheme.take(40),
        "furnitureCount" to furnitureCount.coerceIn(0L, 200L),
        "petAreaEnabled" to petAreaEnabled,
        "garageCapacity" to garageCapacity.coerceIn(1L, 20L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class MissionProgress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "daily",
    val targetCount: Long = 3L,
    val progressCount: Long = 0L,
    val rewardCoins: Long = 120L,
    val rewardXp: Long = 200L,
    val completed: Boolean = false,
    val claimed: Boolean = false,
    val aiGenerated: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "title" to title.take(80).ifBlank { "World Patrol" },
        "description" to description.take(240).ifBlank { "Explore the district." },
        "category" to category,
        "targetCount" to targetCount.coerceIn(1L, 1000L),
        "progressCount" to progressCount.coerceIn(0L, targetCount.coerceIn(1L, 1000L)),
        "rewardCoins" to rewardCoins.coerceIn(0L, 5000L),
        "rewardXp" to rewardXp.coerceIn(0L, 5000L),
        "completed" to completed,
        "claimed" to claimed,
        "aiGenerated" to aiGenerated,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class Friendship(
    @DocumentId val id: String = "",
    val requesterUid: String = "",
    val targetUid: String = "",
    val requesterName: String = "",
    val targetName: String = "",
    val participantUids: List<String> = emptyList(),
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "requesterUid" to requesterUid,
        "targetUid" to targetUid,
        "requesterName" to requesterName.take(30),
        "targetName" to targetName.take(30),
        "participantUids" to participantUids,
        "status" to status,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class BountyContract(
    @DocumentId val id: String = "",
    val hunterUid: String = "",
    val hunterName: String = "",
    val targetUid: String = "",
    val targetName: String = "",
    val participantUids: List<String> = emptyList(),
    val rewardHunter: Long = 50L,
    val rewardCounter: Long = 25L,
    val status: String = "active",
    val winnerUid: String? = null,
    val cooldownUntilEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @Suppress("UNCHECKED_CAST")
    fun toCreateMap(): Map<String, Any> = mapOf(
        "hunterUid" to hunterUid,
        "hunterName" to hunterName.take(30),
        "targetUid" to targetUid,
        "targetName" to targetName.take(30),
        "participantUids" to participantUids,
        "rewardHunter" to 50L,
        "rewardCounter" to 25L,
        "status" to "active",
        "winnerUid" to winnerUid,
        "cooldownUntilEpochMs" to cooldownUntilEpochMs.coerceAtLeast(0L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null } as Map<String, Any>
}

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val roomId: String = "global_world",
    val senderUid: String = "",
    val senderName: String = "",
    val text: String = "",
    val messageType: String = "text",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "roomId" to roomId.take(64),
        "senderUid" to senderUid,
        "senderName" to senderName.take(30),
        "text" to text.take(240),
        "messageType" to messageType,
        "createdAt" to FieldValue.serverTimestamp()
    )
}

// --- Static Game Catalogs & Enums ---

enum class GraphicsPreset(
    val label: String,
    val particleCount: Int,
    val drawDistance: Float,
    val npcDetailLevel: Int
) {
    LOW("Low", 40, 140f, 1),
    MEDIUM("Medium", 90, 210f, 2),
    HIGH("High", 150, 290f, 3),
    ULTRA("Ultra", 220, 380f, 4)
}

enum class TimeOfDayMode(val label: String, val sunAngle: Float) {
    DAWN("Dawn", 0.2f),
    NOON("Daylight", 0.5f),
    SUNSET("Golden Dusk", 0.78f),
    NIGHT("Neon Night", 0.05f)
}

enum class WeatherMode(val label: String) {
    CLEAR("Clear Sky"),
    SAKURA("Sakura Petals"),
    RAIN("Cyber Rain"),
    STORM("Indra Storm")
}

enum class WeaponType {
    FIREARM,
    KATANA,
    FISTS;

    val isFirearm: Boolean get() = this == FIREARM
}

data class WeaponSpec(
    val id: String,
    val name: String,
    val category: String,
    val weaponType: WeaponType = WeaponType.FIREARM,
    val damage: Float,
    val magSize: Int,
    val reserveMax: Int,
    val fireRateMs: Long,
    val range: Float,
    val skinName: String,
    val priceCoins: Long,
    val sellPriceCoins: Long = 0L,
    val boltColorHex: Long
)

data class PropertyMarketListing(
    val id: String,
    val propertyType: String, // Must be in Firestore allowlist: small_house, apartment, luxury_house, villa, penthouse
    val title: String,
    val district: String,
    val buyCoins: Long,
    val sellCoins: Long,
    val locX: Float,
    val locZ: Float,
    val isLandPlot: Boolean = false,
    val garageCapacity: Long = 2L,
    val summary: String
)

data class AbilitySpec(
    val id: String,
    val name: String,
    val source: String, // "Character" or "Pet"
    val epCost: Float,
    val cooldownSec: Float,
    val summary: String,
    val colorHex: Long
)

data class DistrictInfo(
    val name: String,
    val subtitle: String,
    val spawnX: Float,
    val spawnZ: Float,
    val accentColor: Long
)

object GameCatalog {
    val weapons = listOf(
        WeaponSpec(
            id = "wpn_vortex_ar",
            name = "Vortex AR-9",
            category = "Assault Rifle",
            weaponType = WeaponType.FIREARM,
            damage = 28f,
            magSize = 42,
            reserveMax = 160,
            fireRateMs = 150L,
            range = 75f,
            skinName = "Shinobi Neon",
            priceCoins = 450L,
            sellPriceCoins = 260L,
            boltColorHex = 0xFF00E5FF
        ),
        WeaponSpec(
            id = "wpn_pulse_pistol",
            name = "Indra Pulse-7",
            category = "Sidearm",
            weaponType = WeaponType.FIREARM,
            damage = 22f,
            magSize = 15,
            reserveMax = 90,
            fireRateMs = 240L,
            range = 55f,
            skinName = "Crimson Edge",
            priceCoins = 220L,
            sellPriceCoins = 130L,
            boltColorHex = 0xFFFF3B5C
        ),
        WeaponSpec(
            id = "wpn_shadow_katana",
            name = "Kurogane Katana",
            category = "Melee / Energy Arc",
            weaponType = WeaponType.KATANA,
            damage = 55f,
            magSize = 0,
            reserveMax = 0,
            fireRateMs = 340L,
            range = 6.5f,
            skinName = "Amaterasu Violet",
            priceCoins = 380L,
            sellPriceCoins = 220L,
            boltColorHex = 0xFFA855F7
        ),
        WeaponSpec(
            id = "wpn_fists",
            name = "Shinobi Fists",
            category = "Unarmed Combat",
            weaponType = WeaponType.FISTS,
            damage = 34f,
            magSize = 0,
            reserveMax = 0,
            fireRateMs = 280L,
            range = 4.2f,
            skinName = "Chakra Strike",
            priceCoins = 0L,
            sellPriceCoins = 0L,
            boltColorHex = 0xFFFFD166
        ),
        WeaponSpec(
            id = "wpn_dragon_smg",
            name = "Ryujin Vector SMG",
            category = "SMG",
            weaponType = WeaponType.FIREARM,
            damage = 20f,
            magSize = 50,
            reserveMax = 200,
            fireRateMs = 95L,
            range = 52f,
            skinName = "Golden Dragon",
            priceCoins = 850L,
            sellPriceCoins = 480L,
            boltColorHex = 0xFFFFD166
        )
    )

    val abilities = listOf(
        AbilitySpec("Crimson Slash", "Crimson Slash", "Character", 30f, 6f, "Unleash a 360° crimson shockwave.", 0xFFFF3B5C),
        AbilitySpec("Lightning Dash", "Lightning Dash", "Character", 25f, 5f, "Blink forward with Indra thunder speed.", 0xFF00E5FF),
        AbilitySpec("Aegis Barrier", "Aegis Barrier", "Character", 35f, 10f, "Restore +80 Shield instantly.", 0xFF38BDF8),
        AbilitySpec("Shadow Clone Burst", "Shadow Clone Burst", "Character", 40f, 12f, "Stun & damage nearby hostile NPCs.", 0xFFA855F7),
        AbilitySpec("Fox Flame", "Fox Flame", "Pet", 20f, 8f, "Pet restores +50 HP and boosts speed.", 0xFFFF6B35),
        AbilitySpec("Aegis Pulse", "Aegis Pulse", "Pet", 15f, 9f, "Pet restores +60 EP and scans enemies.", 0xFF22C55E),
        AbilitySpec("Dragon Roar", "Dragon Roar", "Pet", 30f, 11f, "Massive AoE knockback wave.", 0xFFF59E0B)
    )

    val petMarket = listOf(
        Triple("Anime Fox", "Mythic", 0L),
        Triple("Cyber Wolf", "Legendary", 600L),
        Triple("Shadow Dragon", "Mythic", 950L),
        Triple("Robotic Dog", "Epic", 450L),
        Triple("Crimson Phoenix", "Legendary", 800L)
    )

    val vehicleMarket = listOf(
        Triple("Kitsune GT-R", "supercar", 0L),
        Triple("Shinobi R1", "motorcycle", 0L),
        Triple("Akatsuki SUV", "suv", 550L),
        Triple("Dune Ronin Buggy", "buggy", 480L),
        Triple("Sakura PD Cruiser", "police", 750L),
        Triple("Indra Sky-Hawk", "helicopter", 1200L)
    )

    val propertyMarket = listOf(
        Triple("small_house", "Residential Region", 400L),
        Triple("apartment", "Central Anime City", 650L),
        Triple("luxury_house", "Coastal Region", 1100L),
        Triple("villa", "Mountain Region", 1800L)
    )

    val propertyMarketListings = listOf(
        PropertyMarketListing(
            id = "prop_plot_saffron",
            propertyType = "small_house",
            title = "Saffron Land Plot #1 (ज़मीन)",
            district = "Residential Region",
            buyCoins = 320L,
            sellCoins = 220L,
            locX = -340f,
            locZ = 515f,
            isLandPlot = true,
            garageCapacity = 2L,
            summary = "Open residential land plot (ज़मीन) with private stash & tent room"
        ),
        PropertyMarketListing(
            id = "prop_sakura_house",
            propertyType = "small_house",
            title = "Sakura Clan Cottage",
            district = "Residential Region",
            buyCoins = 480L,
            sellCoins = 320L,
            locX = -420f,
            locZ = 310f,
            isLandPlot = false,
            garageCapacity = 2L,
            summary = "Starter-friendly villa with tatami room, garage & gear stash"
        ),
        PropertyMarketListing(
            id = "prop_gulmohar_villa",
            propertyType = "apartment",
            title = "Gulmohar Downtown House",
            district = "Central Anime City",
            buyCoins = 720L,
            sellCoins = 490L,
            locX = -88f,
            locZ = 44f,
            isLandPlot = false,
            garageCapacity = 3L,
            summary = "Steps from Indian Flag Monument — private stash & skyline balcony"
        ),
        PropertyMarketListing(
            id = "prop_plot_cyber",
            propertyType = "apartment",
            title = "Cyber Skyline Plot #2 (ज़मीन)",
            district = "Central Anime City",
            buyCoins = 550L,
            sellCoins = 380L,
            locX = 260f,
            locZ = -260f,
            isLandPlot = true,
            garageCapacity = 2L,
            summary = "Prime city land plot (ज़मीन) near Weapon Shops & Commercial Plaza"
        ),
        PropertyMarketListing(
            id = "prop_coastal_house",
            propertyType = "luxury_house",
            title = "Azure Sunrise Sea Villa",
            district = "Coastal Region",
            buyCoins = 1100L,
            sellCoins = 760L,
            locX = -360f,
            locZ = 600f,
            isLandPlot = false,
            garageCapacity = 4L,
            summary = "Beachfront luxury house with 4-car garage & large armor vault"
        ),
        PropertyMarketListing(
            id = "prop_emerald_estate",
            propertyType = "villa",
            title = "Emerald Pine Forest Estate",
            district = "Forest Region",
            buyCoins = 1650L,
            sellCoins = 1150L,
            locX = 460f,
            locZ = -240f,
            isLandPlot = false,
            garageCapacity = 6L,
            summary = "Peaceful forest retreat with 6-slot garage & full stash vault"
        )
    )

    val districts = listOf(
        DistrictInfo("Central Anime City", "Torii Plaza, Neon Towers, Park & Bistro", 0f, 14f, 0xFFFF3B5C),
        DistrictInfo("Hidden Village", "Traditional Houses, Academy & Monument", -380f, -390f, 0xFFF97316),
        DistrictInfo("Mountain Region", "Dragon Ridge Peaks, Switchbacks & Shrine", 0f, -720f, 0xFF38BDF8),
        DistrictInfo("Forest Region", "Dense Pines, Winding River & Trails", 560f, -340f, 0xFF22C55E),
        DistrictInfo("Coastal Region", "Harbor, Beaches, Bridges & Lighthouse Island", 0f, 620f, 0xFF00E5FF),
        DistrictInfo("Residential Region", "Luxury Player Villas, Apartments & Garages", -420f, 340f, 0xFFA855F7),
        DistrictInfo("Commercial Region", "Mall, Dealership, Shops, Hospital, Police & Bazaar", 510f, 120f, 0xFFF59E0B),
        DistrictInfo("Airport", "Runway, Skyport Hangars, Terminal & Helipad", -540f, -200f, 0xFF60A5FA)
    )
}
