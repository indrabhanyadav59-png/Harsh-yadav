package com.example.data.model

import kotlin.math.abs

enum class CoatSilhouette {
    LONG_TACTICAL_COAT,
    SHORT_JACKET,
    VEST_APRON,
    UNIFORM_BLAZER,
    SHINOBI_CLOAK,
    BLOX_FLAME_HOODIE
}

enum class NpcHairSilhouette {
    SPIKY_CROWN,
    SWEPT_SIDE,
    SHORT_TACTICAL,
    TOP_KNOT,
    LONG_LAYERED
}

enum class NpcBackAccessory {
    KATANA,
    SHEATHED_SWORD,
    STAFF,
    BACKPACK,
    NONE
}

data class HeroOutfitPreset(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val silhouette: CoatSilhouette,
    val coatColorInt: Int,
    val liningColorInt: Int,
    val trimColorInt: Int,
    val chestPanelColorInt: Int,
    val beltColorInt: Int,
    val buckleColorInt: Int,
    val pantsColorInt: Int,
    val bootColorInt: Int,
    val defaultAccentHex: String,
    val isRarePlayerExclusive: Boolean = false,
    val priceRupees: Int = 0
)

data class HeroHairPreset(
    val id: String,
    val displayName: String,
    val colorInt: Int,
    val highlightColorInt: Int,
    val priceRupees: Int = 0
)

data class HeroHairCutPreset(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val silhouette: NpcHairSilhouette,
    val priceRupees: Int = 0
)

data class HeroAccessoryPreset(
    val id: String,
    val displayName: String,
    val description: String,
    val isRarePlayerExclusive: Boolean = false,
    val priceRupees: Int = 0
)

data class HeroHeadgearPreset(
    val id: String,
    val displayName: String,
    val description: String,
    val swatchColorInt: Int,
    val priceRupees: Int = 0
)

data class HeroWeaponSkinPreset(
    val id: String,
    val displayName: String,
    val bladeColorInt: Int,
    val scabbardColorInt: Int,
    val wrapColorInt: Int,
    val priceRupees: Int = 0
)

enum class HeroExpression(val id: String, val displayName: String) {
    FIERCE("fierce", "Fierce"),
    HAPPY("happy", "Happy"),
    RAGE("rage", "Battle Rage"),
    SURPRISED("surprised", "Surprised")
}

data class HeroColorSwatch(
    val hex: String,
    val name: String
)

object HeroAppearanceCatalog {

    val outfits: List<HeroOutfitPreset> = listOf(
        HeroOutfitPreset(
            id = "shadow",
            displayName = "Shadow Blaze",
            subtitle = "Indra Signature Black-Crimson Street Jacket & White Tee",
            silhouette = CoatSilhouette.LONG_TACTICAL_COAT,
            coatColorInt = 0xFF16141F.toInt(),
            liningColorInt = 0xFFAE1624.toInt(),
            trimColorInt = 0xFFE2263A.toInt(),
            chestPanelColorInt = 0xFFF4F6FA.toInt(),
            beltColorInt = 0xFF1F2433.toInt(),
            buckleColorInt = 0xFFE2E8F0.toInt(),
            pantsColorInt = 0xFF141722.toInt(),
            bootColorInt = 0xFFDC2626.toInt(),
            defaultAccentHex = "#E2263A",
            isRarePlayerExclusive = false,
            priceRupees = 0
        ),
        HeroOutfitPreset(
            id = "white",
            displayName = "White Storm",
            subtitle = "Arctic Ivory Street Coat & Cobalt Trim",
            silhouette = CoatSilhouette.LONG_TACTICAL_COAT,
            coatColorInt = 0xFFE2E8F0.toInt(),
            liningColorInt = 0xFF0284C7.toInt(),
            trimColorInt = 0xFF00E5FF.toInt(),
            chestPanelColorInt = 0xFF1E293B.toInt(),
            beltColorInt = 0xFF1E293B.toInt(),
            buckleColorInt = 0xFF38BDF8.toInt(),
            pantsColorInt = 0xFF1E293B.toInt(),
            bootColorInt = 0xFF0284C7.toInt(),
            defaultAccentHex = "#00E5FF",
            isRarePlayerExclusive = false,
            priceRupees = 10
        ),
        HeroOutfitPreset(
            id = "cyber",
            displayName = "Cyber Hunter",
            subtitle = "Neon-Vein Combat Armor & Reactive Chest Core",
            silhouette = CoatSilhouette.SHORT_JACKET,
            coatColorInt = 0xFF0F172A.toInt(),
            liningColorInt = 0xFF00E5FF.toInt(),
            trimColorInt = 0xFF38BDF8.toInt(),
            chestPanelColorInt = 0xFF1E293B.toInt(),
            beltColorInt = 0xFF0284C7.toInt(),
            buckleColorInt = 0xFF00E5FF.toInt(),
            pantsColorInt = 0xFF090F1E.toInt(),
            bootColorInt = 0xFF1E293B.toInt(),
            defaultAccentHex = "#00E5FF",
            isRarePlayerExclusive = true,
            priceRupees = 40
        ),
        HeroOutfitPreset(
            id = "gold",
            displayName = "Golden King",
            subtitle = "Sovereign Gold Pauldrons & Obsidian Coat",
            silhouette = CoatSilhouette.LONG_TACTICAL_COAT,
            coatColorInt = 0xFF1C1917.toInt(),
            liningColorInt = 0xFFD97706.toInt(),
            trimColorInt = 0xFFFACC15.toInt(),
            chestPanelColorInt = 0xFFF59E0B.toInt(),
            beltColorInt = 0xFF78350F.toInt(),
            buckleColorInt = 0xFFFFE066.toInt(),
            pantsColorInt = 0xFF171412.toInt(),
            bootColorInt = 0xFFD97706.toInt(),
            defaultAccentHex = "#FFD166",
            isRarePlayerExclusive = true,
            priceRupees = 20
        ),
        HeroOutfitPreset(
            id = "purple",
            displayName = "Dark Phantom",
            subtitle = "Amaterasu Violet Operative Mantle",
            silhouette = CoatSilhouette.SHINOBI_CLOAK,
            coatColorInt = 0xFF1A1230.toInt(),
            liningColorInt = 0xFF7C3AED.toInt(),
            trimColorInt = 0xFFA855F7.toInt(),
            chestPanelColorInt = 0xFF2E1065.toInt(),
            beltColorInt = 0xFF311042.toInt(),
            buckleColorInt = 0xFFC084FC.toInt(),
            pantsColorInt = 0xFF110D1D.toInt(),
            bootColorInt = 0xFF6D28D9.toInt(),
            defaultAccentHex = "#A855F7",
            isRarePlayerExclusive = true,
            priceRupees = 30
        ),
        HeroOutfitPreset(
            id = "ninja",
            displayName = "Red Ninja",
            subtitle = "Crimson Shinobi Vest & Shadow Wraps",
            silhouette = CoatSilhouette.SHORT_JACKET,
            coatColorInt = 0xFF991B1B.toInt(),
            liningColorInt = 0xFF18181B.toInt(),
            trimColorInt = 0xFFEF4444.toInt(),
            chestPanelColorInt = 0xFF141824.toInt(),
            beltColorInt = 0xFF0F172A.toInt(),
            buckleColorInt = 0xFFF59E0B.toInt(),
            pantsColorInt = 0xFF141824.toInt(),
            bootColorInt = 0xFF991B1B.toInt(),
            defaultAccentHex = "#FF3B5C",
            isRarePlayerExclusive = false,
            priceRupees = 50
        ),
        HeroOutfitPreset(
            id = "crimson_blox",
            displayName = "Crimson Blox",
            subtitle = "Block-Hero Red Flame Hoodie, Cargo & Red Sneakers",
            silhouette = CoatSilhouette.BLOX_FLAME_HOODIE,
            coatColorInt = 0xFF141418.toInt(),
            liningColorInt = 0xFFE11D2E.toInt(),
            trimColorInt = 0xFFF8FAFC.toInt(),
            chestPanelColorInt = 0xFFE11D2E.toInt(),
            beltColorInt = 0xFF18181F.toInt(),
            buckleColorInt = 0xFFF8FAFC.toInt(),
            pantsColorInt = 0xFF111218.toInt(),
            bootColorInt = 0xFFDC2626.toInt(),
            defaultAccentHex = "#E11D2E",
            isRarePlayerExclusive = false,
            priceRupees = 0
        )
    )

    val hairs: List<HeroHairPreset> = listOf(
        HeroHairPreset(
            id = "black",
            displayName = "Black",
            colorInt = 0xFF161422.toInt(),
            highlightColorInt = 0xFF2B2840.toInt(),
            priceRupees = 0
        ),
        HeroHairPreset(
            id = "white",
            displayName = "White",
            colorInt = 0xFFDCE2EF.toInt(),
            highlightColorInt = 0xFFF8FAFC.toInt(),
            priceRupees = 10
        ),
        HeroHairPreset(
            id = "red",
            displayName = "Crimson",
            colorInt = 0xFFB91C1C.toInt(),
            highlightColorInt = 0xFFEF4444.toInt(),
            priceRupees = 20
        ),
        HeroHairPreset(
            id = "blue",
            displayName = "Azure",
            colorInt = 0xFF0284C7.toInt(),
            highlightColorInt = 0xFF38BDF8.toInt(),
            priceRupees = 30
        ),
        HeroHairPreset(
            id = "crimson_tips",
            displayName = "Black & Crimson",
            colorInt = 0xFF15141B.toInt(),
            highlightColorInt = 0xFFE11D2E.toInt(),
            priceRupees = 0
        )
    )

    val hairCuts: List<HeroHairCutPreset> = listOf(
        HeroHairCutPreset(
            id = "spiky_hero",
            displayName = "Spiky Indra",
            subtitle = "Signature Tousled Anime Spikes",
            silhouette = NpcHairSilhouette.SPIKY_CROWN,
            priceRupees = 0
        ),
        HeroHairCutPreset(
            id = "wolf_cut",
            displayName = "Wolf Cut",
            subtitle = "Side-Swept Rogue Locks",
            silhouette = NpcHairSilhouette.SWEPT_SIDE,
            priceRupees = 10
        ),
        HeroHairCutPreset(
            id = "shinobi_long",
            displayName = "Shinobi Flow",
            subtitle = "Layered Shoulder Curtain",
            silhouette = NpcHairSilhouette.LONG_LAYERED,
            priceRupees = 20
        ),
        HeroHairCutPreset(
            id = "short_crop",
            displayName = "Academy Crop",
            subtitle = "Clean Tactical Cut",
            silhouette = NpcHairSilhouette.SHORT_TACTICAL,
            priceRupees = 30
        )
    )

    val accessories: List<HeroAccessoryPreset> = listOf(
        HeroAccessoryPreset(
            id = "katana",
            displayName = "Katana",
            description = "Back-mounted tactical blade with diamond hilt wrap",
            isRarePlayerExclusive = false,
            priceRupees = 0
        ),
        HeroAccessoryPreset(
            id = "mask",
            displayName = "Ninja Mask",
            description = "Lower-face tactical shinobi respirator mask",
            isRarePlayerExclusive = false,
            priceRupees = 10
        ),
        HeroAccessoryPreset(
            id = "wings",
            displayName = "Energy Wings",
            description = "Twin radiant aura wing blades on back",
            isRarePlayerExclusive = true,
            priceRupees = 30
        ),
        HeroAccessoryPreset(
            id = "none",
            displayName = "None",
            description = "Clean coat silhouette with no back gear",
            isRarePlayerExclusive = false,
            priceRupees = 0
        )
    )

    val headgears: List<HeroHeadgearPreset> = listOf(
        HeroHeadgearPreset(
            id = "gear_none",
            displayName = "No Headgear",
            description = "Open face & tousled bangs",
            swatchColorInt = 0xFF334155.toInt(),
            priceRupees = 0
        ),
        HeroHeadgearPreset(
            id = "gear_scarf",
            displayName = "Crimson Scarf",
            description = "Flowing red neck scarf & trailing tails",
            swatchColorInt = 0xFFE2263A.toInt(),
            priceRupees = 10
        ),
        HeroHeadgearPreset(
            id = "gear_glasses",
            displayName = "Stylish Glasses",
            description = "Sharp anime combat visor frames",
            swatchColorInt = 0xFF38BDF8.toInt(),
            priceRupees = 15
        ),
        HeroHeadgearPreset(
            id = "gear_cap",
            displayName = "Red-Black Cap",
            description = "Streetwear snapback with crimson brim",
            swatchColorInt = 0xFFDC2626.toInt(),
            priceRupees = 15
        ),
        HeroHeadgearPreset(
            id = "gear_headphones",
            displayName = "Cyber Headphones",
            description = "Neon-ring acoustic headset over hair",
            swatchColorInt = 0xFFA855F7.toInt(),
            priceRupees = 20
        )
    )

    val weaponSkins: List<HeroWeaponSkinPreset> = listOf(
        HeroWeaponSkinPreset(
            id = "wskin_crimson",
            displayName = "Crimson Demon",
            bladeColorInt = 0xFFE2263A.toInt(),
            scabbardColorInt = 0xFF14131C.toInt(),
            wrapColorInt = 0xFFEF4444.toInt(),
            priceRupees = 0
        ),
        HeroWeaponSkinPreset(
            id = "wskin_azure",
            displayName = "Azure Lightning",
            bladeColorInt = 0xFF00E5FF.toInt(),
            scabbardColorInt = 0xFF0F172A.toInt(),
            wrapColorInt = 0xFF38BDF8.toInt(),
            priceRupees = 10
        ),
        HeroWeaponSkinPreset(
            id = "wskin_amethyst",
            displayName = "Amethyst Shadow",
            bladeColorInt = 0xFFA855F7.toInt(),
            scabbardColorInt = 0xFF1B102E.toInt(),
            wrapColorInt = 0xFFC084FC.toInt(),
            priceRupees = 20
        ),
        HeroWeaponSkinPreset(
            id = "wskin_golden",
            displayName = "Golden Emperor",
            bladeColorInt = 0xFFFFD166.toInt(),
            scabbardColorInt = 0xFF2B1C08.toInt(),
            wrapColorInt = 0xFFFACC15.toInt(),
            priceRupees = 30
        ),
        HeroWeaponSkinPreset(
            id = "wskin_solar",
            displayName = "Solar Flame",
            bladeColorInt = 0xFFFF6B35.toInt(),
            scabbardColorInt = 0xFF28120B.toInt(),
            wrapColorInt = 0xFFFB923C.toInt(),
            priceRupees = 40
        )
    )

    val namedColorSwatches: List<HeroColorSwatch> = listOf(
        HeroColorSwatch("#E2263A", "Crimson Red"),
        HeroColorSwatch("#0088FF", "Azure Blue"),
        HeroColorSwatch("#9333EA", "Royal Purple"),
        HeroColorSwatch("#F8FAFC", "Arctic White"),
        HeroColorSwatch("#111827", "Shadow Black"),
        HeroColorSwatch("#FACC15", "Solar Gold"),
        HeroColorSwatch("#F472B6", "Sakura Pink"),
        HeroColorSwatch("#FF3B5C", "Neon Rose"),
        HeroColorSwatch("#00E5FF", "Electric Cyan"),
        HeroColorSwatch("#A855F7", "Amethyst"),
        HeroColorSwatch("#6366F1", "Indigo Aura"),
        HeroColorSwatch("#FB7185", "Coral Blaze"),
        HeroColorSwatch("#38BDF8", "Sky Ice"),
        HeroColorSwatch("#FDE047", "Lightning Yellow"),
        HeroColorSwatch("#334155", "Steel Slate")
    )

    val colorSwatches: List<String> = namedColorSwatches.map { it.hex }

    fun styleKey(category: String, id: String): String = "$category:$id"

    fun starterUnlockedKeys(): Set<String> = setOf(
        styleKey("outfit", "shadow"),
        styleKey("outfit", "crimson_blox"),
        styleKey("hair", "black"),
        styleKey("hair", "crimson_tips"),
        styleKey("haircut", "spiky_hero"),
        styleKey("acc", "katana"),
        styleKey("acc", "none"),
        styleKey("gear", "gear_none"),
        styleKey("wskin", "wskin_crimson")
    )

    fun priceForKey(key: String): Int {
        val parts = key.split(":", limit = 2)
        if (parts.size != 2) return 0
        val (cat, id) = parts
        return when (cat) {
            "outfit" -> outfits.firstOrNull { it.id == id }?.priceRupees ?: 0
            "hair" -> hairs.firstOrNull { it.id == id }?.priceRupees ?: 0
            "haircut" -> hairCuts.firstOrNull { it.id == id }?.priceRupees ?: 0
            "acc" -> accessories.firstOrNull { it.id == id }?.priceRupees ?: 0
            "gear" -> headgears.firstOrNull { it.id == id }?.priceRupees ?: 0
            "wskin" -> weaponSkins.firstOrNull { it.id == id }?.priceRupees ?: 0
            else -> 0
        }
    }

    fun resolveOutfit(raw: String?): HeroOutfitPreset {
        val key = raw?.trim()?.lowercase().orEmpty()
        return outfits.firstOrNull {
            it.id == key || it.displayName.lowercase() == key
        } ?: when {
            key.contains("blox") || key.contains("flame hoodie") -> outfits.first { it.id == "crimson_blox" }
            key.contains("white") || key.contains("frost") -> outfits.first { it.id == "white" }
            key.contains("cyber") -> outfits.first { it.id == "cyber" }
            key.contains("gold") || key.contains("king") -> outfits.first { it.id == "gold" }
            key.contains("purple") || key.contains("phantom") -> outfits.first { it.id == "purple" }
            key.contains("ninja") || key.contains("red") || key.contains("orange") -> outfits.first { it.id == "ninja" }
            else -> outfits.first() // Shadow Blaze default
        }
    }

    fun resolveHair(raw: String?): HeroHairPreset {
        val key = raw?.trim()?.lowercase().orEmpty()
        return hairs.firstOrNull {
            it.id == key || it.displayName.lowercase() == key
        } ?: when {
            key.contains("tips") || key.contains("blox") || key.contains("black & crimson") -> hairs.first { it.id == "crimson_tips" }
            key.contains("white") || key.contains("silver") -> hairs.first { it.id == "white" }
            key.contains("red") || key.contains("crimson") -> hairs.first { it.id == "red" }
            key.contains("blue") || key.contains("azure") || key.contains("cyan") -> hairs.first { it.id == "blue" }
            else -> hairs.first() // Black default
        }
    }

    fun resolveHairCut(raw: String?): HeroHairCutPreset {
        val key = raw?.trim()?.lowercase().orEmpty()
        return hairCuts.firstOrNull {
            it.id == key || it.displayName.lowercase() == key
        } ?: hairCuts.first()
    }

    fun resolveAccessory(raw: String?): HeroAccessoryPreset {
        val key = raw?.trim()?.lowercase().orEmpty()
        return accessories.firstOrNull {
            it.id == key || it.displayName.lowercase() == key
        } ?: when {
            key.contains("mask") -> accessories.first { it.id == "mask" }
            key.contains("wing") -> accessories.first { it.id == "wings" }
            key.contains("none") -> accessories.first { it.id == "none" }
            else -> accessories.first() // Katana default
        }
    }

    fun resolveHeadgear(raw: String?): HeroHeadgearPreset {
        val key = raw?.trim()?.lowercase().orEmpty()
        return headgears.firstOrNull {
            it.id == key || it.displayName.lowercase() == key
        } ?: headgears.first()
    }

    fun resolveWeaponSkin(raw: String?): HeroWeaponSkinPreset {
        val key = raw?.trim()?.lowercase().orEmpty()
        return weaponSkins.firstOrNull {
            it.id == key || it.displayName.lowercase() == key
        } ?: weaponSkins.first()
    }

    fun resolveExpression(raw: String?): HeroExpression {
        val key = raw?.trim()?.lowercase().orEmpty()
        return HeroExpression.entries.firstOrNull {
            it.id == key || it.displayName.lowercase() == key
        } ?: HeroExpression.FIERCE
    }
}

data class NpcAppearanceSpec(
    val seed: Long,
    val hairStyle: NpcHairSilhouette,
    val hairColorInt: Int,
    val skinColorInt: Int,
    val eyeColorInt: Int,
    val coatColorInt: Int,
    val liningColorInt: Int,
    val pantsColorInt: Int,
    val bootColorInt: Int,
    val accentColorInt: Int,
    val coatSilhouette: CoatSilhouette,
    val backAccessory: NpcBackAccessory,
    val hasFaceMask: Boolean,
    val heightScale: Float,
    val buildScale: Float,
    val factionPresetName: String
)

object NpcAppearanceGenerator {

    private val skinTones = intArrayOf(
        0xFFF6C6A5.toInt(),
        0xFFEEC09E.toInt(),
        0xFFE4AD88.toInt(),
        0xFFD2956C.toInt(),
        0xFFF9D5BE.toInt()
    )

    private val eyePalette = intArrayOf(
        0xFF38BDF8.toInt(),
        0xFFEF4444.toInt(),
        0xFF22C55E.toInt(),
        0xFFF59E0B.toInt(),
        0xFFA855F7.toInt(),
        0xFF1E293B.toInt()
    )

    private val hairColorsCommon = intArrayOf(
        0xFF161422.toInt(),
        0xFF3F2318.toInt(),
        0xFF78350F.toInt(),
        0xFF334155.toInt(),
        0xFFE2E8F0.toInt(),
        0xFFDC2626.toInt(),
        0xFF0284C7.toInt(),
        0xFFF472B6.toInt(),
        0xFFFACC15.toInt()
    )

    fun generate(
        npcId: String,
        roleKey: String,
        preferredCoatColor: Int? = null,
        preferredHairColor: Int? = null,
        preferredAccentColor: Int? = null
    ): NpcAppearanceSpec {
        val rawSeed = computeDeterministicSeed(npcId)
        val h1 = mixHash(rawSeed)
        val h2 = mixHash(h1)
        val h3 = mixHash(h2)
        val h4 = mixHash(h3)

        val skin = skinTones[idx(h1, skinTones.size)]
        val eye = preferredAccentColor ?: eyePalette[idx(h2, eyePalette.size)]
        val hairCol = preferredHairColor ?: hairColorsCommon[idx(h3, hairColorsCommon.size)]

        // Stylized human height [0.91..1.09] and build [0.91..1.10]
        val heightScale = 0.91f + ((h2 ushr 8) and 0xFF) / 255f * 0.18f
        val buildScale = 0.91f + ((h3 ushr 8) and 0xFF) / 255f * 0.19f

        if (npcId == "npc_blox_flame") {
            return NpcAppearanceSpec(
                seed = rawSeed,
                hairStyle = NpcHairSilhouette.SPIKY_CROWN,
                hairColorInt = 0xFF15141B.toInt(),
                skinColorInt = 0xFFF6C69C.toInt(),
                eyeColorInt = 0xFF111116.toInt(),
                coatColorInt = 0xFF141418.toInt(),
                liningColorInt = 0xFFE11D2E.toInt(),
                pantsColorInt = 0xFF111218.toInt(),
                bootColorInt = 0xFFDC2626.toInt(),
                accentColorInt = 0xFFE11D2E.toInt(),
                coatSilhouette = CoatSilhouette.BLOX_FLAME_HOODIE,
                backAccessory = NpcBackAccessory.NONE,
                hasFaceMask = false,
                heightScale = 1.02f,
                buildScale = 1.0f,
                factionPresetName = "Crimson Blox"
            )
        }

        return when (roleKey) {
            "POLICE" -> NpcAppearanceSpec(
                seed = rawSeed,
                hairStyle = if (idx(h1, 2) == 0) NpcHairSilhouette.SHORT_TACTICAL else NpcHairSilhouette.SWEPT_SIDE,
                hairColorInt = hairCol,
                skinColorInt = skin,
                eyeColorInt = 0xFF38BDF8.toInt(),
                coatColorInt = preferredCoatColor ?: 0xFF1E3A8A.toInt(),
                liningColorInt = 0xFF0F172A.toInt(),
                pantsColorInt = 0xFF0F172A.toInt(),
                bootColorInt = 0xFF090D16.toInt(),
                accentColorInt = 0xFF38BDF8.toInt(),
                coatSilhouette = CoatSilhouette.UNIFORM_BLAZER,
                backAccessory = if (idx(h4, 2) == 0) NpcBackAccessory.BACKPACK else NpcBackAccessory.NONE,
                hasFaceMask = idx(h4, 4) == 0,
                heightScale = heightScale.coerceAtLeast(0.98f),
                buildScale = buildScale.coerceAtLeast(0.98f),
                factionPresetName = "Metro Police Uniform"
            )
            "SHOPKEEPER" -> NpcAppearanceSpec(
                seed = rawSeed,
                hairStyle = if (idx(h1, 2) == 0) NpcHairSilhouette.TOP_KNOT else NpcHairSilhouette.LONG_LAYERED,
                hairColorInt = hairCol,
                skinColorInt = skin,
                eyeColorInt = eye,
                coatColorInt = preferredCoatColor ?: 0xFFB45309.toInt(),
                liningColorInt = 0xFFFEF3C7.toInt(),
                pantsColorInt = 0xFF1C1917.toInt(),
                bootColorInt = 0xFF292524.toInt(),
                accentColorInt = preferredAccentColor ?: 0xFFF59E0B.toInt(),
                coatSilhouette = CoatSilhouette.VEST_APRON,
                backAccessory = if (idx(h4, 3) == 0) NpcBackAccessory.BACKPACK else NpcBackAccessory.NONE,
                hasFaceMask = false,
                heightScale = heightScale,
                buildScale = buildScale,
                factionPresetName = "District Merchant"
            )
            "ACADEMY_STUDENT" -> {
                val blazers = intArrayOf(0xFF1D4ED8.toInt(), 0xFF0F766E.toInt(), 0xFF4338CA.toInt(), 0xFF334155.toInt())
                NpcAppearanceSpec(
                    seed = rawSeed,
                    hairStyle = NpcHairSilhouette.entries[idx(h1, NpcHairSilhouette.entries.size)],
                    hairColorInt = hairCol,
                    skinColorInt = skin,
                    eyeColorInt = eye,
                    coatColorInt = preferredCoatColor ?: blazers[idx(h2, blazers.size)],
                    liningColorInt = 0xFFF8FAFC.toInt(),
                    pantsColorInt = 0xFF1E293B.toInt(),
                    bootColorInt = 0xFF0F172A.toInt(),
                    accentColorInt = preferredAccentColor ?: 0xFF38BDF8.toInt(),
                    coatSilhouette = CoatSilhouette.UNIFORM_BLAZER,
                    backAccessory = when (idx(h4, 3)) {
                        0 -> NpcBackAccessory.BACKPACK
                        1 -> NpcBackAccessory.STAFF
                        else -> NpcBackAccessory.NONE
                    },
                    hasFaceMask = false,
                    heightScale = heightScale.coerceAtMost(1.01f),
                    buildScale = buildScale.coerceAtMost(1.00f),
                    factionPresetName = "Shinobi Academy Student"
                )
            }
            "MISSION_NPC" -> NpcAppearanceSpec(
                seed = rawSeed,
                hairStyle = if (idx(h1, 2) == 0) NpcHairSilhouette.SWEPT_SIDE else NpcHairSilhouette.TOP_KNOT,
                hairColorInt = hairCol,
                skinColorInt = skin,
                eyeColorInt = 0xFF00E5FF.toInt(),
                coatColorInt = preferredCoatColor ?: 0xFF134E4A.toInt(),
                liningColorInt = 0xFFD97706.toInt(),
                pantsColorInt = 0xFF0F172A.toInt(),
                bootColorInt = 0xFF0B1120.toInt(),
                accentColorInt = preferredAccentColor ?: 0xFF2DD4BF.toInt(),
                coatSilhouette = CoatSilhouette.LONG_TACTICAL_COAT,
                backAccessory = if (idx(h4, 2) == 0) NpcBackAccessory.STAFF else NpcBackAccessory.SHEATHED_SWORD,
                hasFaceMask = false,
                heightScale = heightScale,
                buildScale = buildScale,
                factionPresetName = "Mission Liaison"
            )
            "RIVAL_BOT" -> {
                val rivalCoats = intArrayOf(0xFF31102F.toInt(), 0xFF3F1D2B.toInt(), 0xFF1E1B4B.toInt(), 0xFF27272A.toInt())
                val rivalAccents = intArrayOf(0xFFFF3B5C.toInt(), 0xFFEF4444.toInt(), 0xFFA855F7.toInt())
                NpcAppearanceSpec(
                    seed = rawSeed,
                    hairStyle = if (idx(h1, 2) == 0) NpcHairSilhouette.SPIKY_CROWN else NpcHairSilhouette.SWEPT_SIDE,
                    hairColorInt = hairCol,
                    skinColorInt = skin,
                    eyeColorInt = 0xFFEF4444.toInt(),
                    coatColorInt = preferredCoatColor ?: rivalCoats[idx(h2, rivalCoats.size)],
                    liningColorInt = 0xFF991B1B.toInt(),
                    pantsColorInt = 0xFF090D16.toInt(),
                    bootColorInt = 0xFF05070C.toInt(),
                    accentColorInt = preferredAccentColor ?: rivalAccents[idx(h3, rivalAccents.size)],
                    coatSilhouette = CoatSilhouette.SHINOBI_CLOAK,
                    backAccessory = if (idx(h4, 2) == 0) NpcBackAccessory.KATANA else NpcBackAccessory.SHEATHED_SWORD,
                    hasFaceMask = idx(h1, 2) == 0,
                    heightScale = heightScale,
                    buildScale = buildScale,
                    factionPresetName = "Rogue Rival"
                )
            }
            "OFFLINE_PLAYER_PROXY" -> NpcAppearanceSpec(
                seed = rawSeed,
                hairStyle = if (idx(h1, 2) == 0) NpcHairSilhouette.SPIKY_CROWN else NpcHairSilhouette.SWEPT_SIDE,
                hairColorInt = hairCol,
                skinColorInt = skin,
                eyeColorInt = eye,
                coatColorInt = preferredCoatColor ?: 0xFF1E293B.toInt(),
                liningColorInt = preferredAccentColor ?: 0xFF38BDF8.toInt(),
                pantsColorInt = 0xFF0F172A.toInt(),
                bootColorInt = 0xFF0B1120.toInt(),
                accentColorInt = preferredAccentColor ?: 0xFF00E5FF.toInt(),
                coatSilhouette = CoatSilhouette.LONG_TACTICAL_COAT,
                backAccessory = when (idx(h4, 3)) {
                    0 -> NpcBackAccessory.KATANA
                    1 -> NpcBackAccessory.SHEATHED_SWORD
                    else -> NpcBackAccessory.STAFF
                },
                hasFaceMask = idx(h3, 3) == 0,
                heightScale = heightScale,
                buildScale = buildScale,
                factionPresetName = "Squad Proxy"
            )
            else -> {
                // Civilian — everyday anime streetwear/jackets, never player-exclusive rare skins or energy wings
                val casualCoats = intArrayOf(
                    0xFF1E3A8A.toInt(),
                    0xFF0F766E.toInt(),
                    0xFF14532D.toInt(),
                    0xFF0284C7.toInt(),
                    0xFF475569.toInt(),
                    0xFF7C2D12.toInt()
                )
                NpcAppearanceSpec(
                    seed = rawSeed,
                    hairStyle = NpcHairSilhouette.entries[idx(h1, NpcHairSilhouette.entries.size)],
                    hairColorInt = hairCol,
                    skinColorInt = skin,
                    eyeColorInt = eye,
                    coatColorInt = preferredCoatColor ?: casualCoats[idx(h2, casualCoats.size)],
                    liningColorInt = 0xFFCBD5E1.toInt(),
                    pantsColorInt = 0xFF1E293B.toInt(),
                    bootColorInt = 0xFF111827.toInt(),
                    accentColorInt = preferredAccentColor ?: eyePalette[idx(h3, eyePalette.size)],
                    coatSilhouette = if (idx(h2, 2) == 0) CoatSilhouette.SHORT_JACKET else CoatSilhouette.VEST_APRON,
                    backAccessory = if (idx(h4, 3) == 0) NpcBackAccessory.BACKPACK else NpcBackAccessory.NONE,
                    hasFaceMask = false,
                    heightScale = heightScale,
                    buildScale = buildScale,
                    factionPresetName = "Citizen"
                )
            }
        }
    }

    private fun computeDeterministicSeed(id: String): Long {
        var h = -0x340d631b7bdddcdbL
        for (ch in id) {
            h = h xor ch.code.toLong()
            h *= 0x100000001b3L
        }
        return h
    }

    private fun mixHash(x: Long): Long {
        var z = x + 0x9e3779b97f4a7c15uL.toLong()
        z = (z xor (z ushr 30)) * 0xbf58476d1ce4e5b9uL.toLong()
        z = (z xor (z ushr 27)) * 0x94d049bb133111ebuL.toLong()
        return z xor (z ushr 31)
    }

    private fun idx(hash: Long, bound: Int): Int = (abs(hash) % bound).toInt()
}
