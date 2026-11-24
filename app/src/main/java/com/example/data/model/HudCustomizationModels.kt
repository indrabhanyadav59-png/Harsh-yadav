package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

enum class HudIconStyle(val label: String) {
    NEON_GLASS("Neon Glass"),
    TACTICAL_RING("Tactical Ring"),
    ANIME_CREST("Anime Crest")
}

enum class HudSide {
    LEFT,
    RIGHT,
    CENTER
}

enum class HudElementId(val displayName: String, val essential: Boolean) {
    MINIMAP("Minimap (Legacy)", false),
    MISSION_PILL("Mission Card", false),
    UTILITY_RAIL("Bag / Shop / Heal / Map", false),
    JOYSTICK("Joystick", true),
    SPRINT("Sprint", false),
    INTERACT("Interact", false),
    JUMP("Jump", true),
    CROUCH("Crouch / Guard", false),
    PRONE("Prone", false),
    GRENADE("Grenade", false),
    AIM("Aim / Heavy", false),
    FIRE("Fire / Attack", true),
    MELEE("Quick Strike", false),
    RELOAD("Reload / Parry", false),
    WEAPON_SWITCH("Weapon & Ammo", true),
    HP_EP_BARS("HP / EP Bars", true),
    ABILITIES("Skills & Pet", false)
}

data class HudElementConfig(
    val id: HudElementId,
    val normX: Float,
    val normY: Float,
    val sizeScale: Float = 1f,
    val opacity: Float = 0.92f,
    val spacingScale: Float = 1f,
    val visible: Boolean = true,
    val side: HudSide = HudSide.RIGHT,
    val iconStyle: HudIconStyle = HudIconStyle.NEON_GLASS
) {
    fun sanitized(): HudElementConfig = copy(
        normX = normX.coerceIn(0.04f, 0.96f),
        normY = normY.coerceIn(0.08f, 0.92f),
        sizeScale = sizeScale.coerceIn(0.5f, 2.0f),
        opacity = opacity.coerceIn(0.2f, 1.0f),
        spacingScale = spacingScale.coerceIn(0.7f, 1.6f),
        visible = if (id.essential) true else visible
    )

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id.name)
        put("normX", normX.toDouble())
        put("normY", normY.toDouble())
        put("sizeScale", sizeScale.toDouble())
        put("opacity", opacity.toDouble())
        put("spacingScale", spacingScale.toDouble())
        put("visible", visible)
        put("side", side.name)
        put("iconStyle", iconStyle.name)
    }

    companion object {
        fun fromJson(obj: JSONObject, fallback: HudElementConfig): HudElementConfig {
            val styleName = obj.optString("iconStyle", fallback.iconStyle.name)
            val sideName = obj.optString("side", fallback.side.name)
            return HudElementConfig(
                id = fallback.id,
                normX = obj.optDouble("normX", fallback.normX.toDouble()).toFloat(),
                normY = obj.optDouble("normY", fallback.normY.toDouble()).toFloat(),
                sizeScale = obj.optDouble("sizeScale", fallback.sizeScale.toDouble()).toFloat(),
                opacity = obj.optDouble("opacity", fallback.opacity.toDouble()).toFloat(),
                spacingScale = obj.optDouble("spacingScale", fallback.spacingScale.toDouble()).toFloat(),
                visible = obj.optBoolean("visible", fallback.visible),
                side = HudSide.entries.firstOrNull { it.name == sideName } ?: fallback.side,
                iconStyle = HudIconStyle.entries.firstOrNull { it.name == styleName } ?: fallback.iconStyle
            ).sanitized()
        }
    }
}

data class HudLayoutProfile(
    val slotIndex: Int,
    val name: String,
    val elements: List<HudElementConfig>
) {
    fun element(id: HudElementId): HudElementConfig =
        elements.firstOrNull { it.id == id } ?: HudLayoutPresets.defaultElement(id)

    fun withElement(updated: HudElementConfig): HudLayoutProfile {
        val clean = updated.sanitized()
        val hasExisting = elements.any { it.id == clean.id }
        val next = if (hasExisting) {
            elements.map { if (it.id == clean.id) clean else it }
        } else {
            elements + clean
        }
        return copy(elements = next)
    }

    fun mirrorLeftRight(): HudLayoutProfile = copy(
        elements = elements.map { el ->
            val mirroredX = (1f - el.normX).coerceIn(0.04f, 0.96f)
            val mirroredSide = when {
                el.side == HudSide.LEFT -> HudSide.RIGHT
                el.side == HudSide.RIGHT -> HudSide.LEFT
                else -> HudSide.CENTER
            }
            el.copy(normX = mirroredX, side = mirroredSide).sanitized()
        }
    )

    fun toJson(): JSONObject = JSONObject().apply {
        put("slotIndex", slotIndex)
        put("name", name.take(24).ifBlank { "Preset ${slotIndex + 1}" })
        val arr = JSONArray()
        elements.forEach { arr.put(it.toJson()) }
        put("elements", arr)
    }

    companion object {
        fun fromJson(obj: JSONObject, fallbackSlot: Int): HudLayoutProfile {
            val base = HudLayoutPresets.createPreset(fallbackSlot)
            val name = obj.optString("name", base.name).take(24).ifBlank { base.name }
            val arr = obj.optJSONArray("elements") ?: return base.copy(name = name)
            val parsedById = mutableMapOf<HudElementId, HudElementConfig>()
            for (i in 0 until arr.length()) {
                val elObj = arr.optJSONObject(i) ?: continue
                val idName = elObj.optString("id")
                val id = HudElementId.entries.firstOrNull { it.name == idName } ?: continue
                parsedById[id] = HudElementConfig.fromJson(elObj, base.element(id))
            }
            val merged = HudElementId.entries.map { id ->
                parsedById[id] ?: base.element(id)
            }
            return HudLayoutProfile(
                slotIndex = fallbackSlot,
                name = name,
                elements = merged
            )
        }
    }
}

object HudLayoutPresets {

    private fun baseElements(
        iconStyle: HudIconStyle = HudIconStyle.NEON_GLASS,
        scaleBoost: Float = 1f,
        firePos: Pair<Float, Float> = 0.855f to 0.695f,
        aimPos: Pair<Float, Float> = 0.945f to 0.455f,
        leftHanded: Boolean = false
    ): List<HudElementConfig> {
        val list = listOf(
            HudElementConfig(HudElementId.MINIMAP, 0.075f, 0.130f, 1.00f, 0.94f, 1f, false, HudSide.LEFT, iconStyle),
            HudElementConfig(HudElementId.MISSION_PILL, 0.085f, 0.150f, 1.0f, 0.92f, 1f, true, HudSide.LEFT, iconStyle),
            HudElementConfig(HudElementId.UTILITY_RAIL, 0.072f, 0.395f, 1.0f * scaleBoost, 0.92f, 1f, true, HudSide.LEFT, iconStyle),
            HudElementConfig(HudElementId.SPRINT, 0.220f, 0.520f, 1.0f * scaleBoost, 0.92f, 1f, true, HudSide.LEFT, iconStyle),
            HudElementConfig(HudElementId.JOYSTICK, 0.130f, 0.805f, 1.05f * scaleBoost, 0.88f, 1f, true, HudSide.LEFT, iconStyle),
            HudElementConfig(HudElementId.HP_EP_BARS, 0.415f, 0.865f, 1.0f, 0.94f, 1f, true, HudSide.CENTER, iconStyle),
            HudElementConfig(HudElementId.ABILITIES, 0.630f, 0.865f, 0.96f * scaleBoost, 0.94f, 1f, true, HudSide.CENTER, iconStyle),
            HudElementConfig(HudElementId.WEAPON_SWITCH, 0.895f, 0.210f, 1.0f, 0.94f, 1f, true, HudSide.RIGHT, iconStyle),
            HudElementConfig(HudElementId.INTERACT, 0.855f, 0.470f, 1.0f * scaleBoost, 0.94f, 1f, true, HudSide.RIGHT, iconStyle),
            HudElementConfig(HudElementId.AIM, aimPos.first, aimPos.second, 1.0f * scaleBoost, 0.92f, 1f, true, HudSide.RIGHT, iconStyle),
            HudElementConfig(HudElementId.JUMP, 0.945f, 0.615f, 1.0f * scaleBoost, 0.92f, 1f, true, HudSide.RIGHT, iconStyle),
            HudElementConfig(HudElementId.CROUCH, 0.945f, 0.760f, 1.0f * scaleBoost, 0.92f, 1f, true, HudSide.RIGHT, iconStyle),
            HudElementConfig(HudElementId.PRONE, 0.875f, 0.865f, 0.95f * scaleBoost, 0.92f, 1f, false, HudSide.RIGHT, iconStyle),
            HudElementConfig(HudElementId.FIRE, firePos.first, firePos.second, 1.06f * scaleBoost, 0.96f, 1f, true, HudSide.RIGHT, iconStyle),
            HudElementConfig(HudElementId.GRENADE, 0.775f, 0.825f, 0.95f * scaleBoost, 0.92f, 1f, false, HudSide.RIGHT, iconStyle),
            HudElementConfig(HudElementId.RELOAD, 0.765f, 0.635f, 0.95f * scaleBoost, 0.92f, 1f, true, HudSide.RIGHT, iconStyle),
            HudElementConfig(HudElementId.MELEE, 0.765f, 0.495f, 0.92f * scaleBoost, 0.92f, 1f, false, HudSide.RIGHT, iconStyle)
        ).map { it.sanitized() }
        return if (leftHanded) {
            HudLayoutProfile(0, "Left", list).mirrorLeftRight().elements
        } else {
            list
        }
    }

    fun defaultElement(id: HudElementId): HudElementConfig =
        baseElements().first { it.id == id }

    fun createPreset(slotIndex: Int): HudLayoutProfile = when (slotIndex.coerceIn(0, 4)) {
        0 -> HudLayoutProfile(0, "Tactical FF", baseElements())
        1 -> HudLayoutProfile(
            1,
            "Claw 4-Finger",
            baseElements(
                iconStyle = HudIconStyle.TACTICAL_RING,
                scaleBoost = 1.05f,
                firePos = 0.82f to 0.38f,
                aimPos = 0.92f to 0.42f
            )
        )
        2 -> HudLayoutProfile(
            2,
            "Anime Crest",
            baseElements(
                iconStyle = HudIconStyle.ANIME_CREST,
                scaleBoost = 0.92f
            )
        )
        3 -> HudLayoutProfile(
            3,
            "Two-Thumb Pro",
            baseElements(
                iconStyle = HudIconStyle.NEON_GLASS,
                scaleBoost = 1.12f
            )
        )
        else -> HudLayoutProfile(
            4,
            "Left-Handed",
            baseElements(
                iconStyle = HudIconStyle.NEON_GLASS,
                leftHanded = true
            )
        )
    }

    fun defaultFiveLayouts(): List<HudLayoutProfile> = (0..4).map { createPreset(it) }
}

data class GameplaySettingsConfig(
    val graphicsPreset: GraphicsPreset = GraphicsPreset.HIGH,
    val targetFps: Int = 60,
    val cameraSensitivity: Float = 1.0f,
    val aimSensitivity: Float = 0.85f,
    val scopeSensitivity: Float = 0.65f,
    val joystickSensitivity: Float = 1.0f,
    val autoSprint: Boolean = false,
    val aimAssist: Boolean = true,
    val cameraDistanceScale: Float = 1.0f,
    val musicVolume: Float = 0.7f,
    val soundVolume: Float = 0.85f,
    val voiceChatVolume: Float = 0.8f,
    val vibrationEnabled: Boolean = true,
    val autoPickup: Boolean = true,
    val leftHandedControls: Boolean = false
) {
    fun sanitized(): GameplaySettingsConfig = copy(
        targetFps = if (targetFps in listOf(30, 60, 90)) targetFps else 60,
        cameraSensitivity = cameraSensitivity.coerceIn(0.3f, 2.2f),
        aimSensitivity = aimSensitivity.coerceIn(0.3f, 2.0f),
        scopeSensitivity = scopeSensitivity.coerceIn(0.3f, 1.8f),
        joystickSensitivity = joystickSensitivity.coerceIn(0.5f, 1.8f),
        cameraDistanceScale = cameraDistanceScale.coerceIn(0.7f, 1.45f),
        musicVolume = musicVolume.coerceIn(0f, 1f),
        soundVolume = soundVolume.coerceIn(0f, 1f),
        voiceChatVolume = voiceChatVolume.coerceIn(0f, 1f)
    )

    fun toJson(): JSONObject = JSONObject().apply {
        put("graphicsPreset", graphicsPreset.name)
        put("targetFps", targetFps)
        put("cameraSensitivity", cameraSensitivity.toDouble())
        put("aimSensitivity", aimSensitivity.toDouble())
        put("scopeSensitivity", scopeSensitivity.toDouble())
        put("joystickSensitivity", joystickSensitivity.toDouble())
        put("autoSprint", autoSprint)
        put("aimAssist", aimAssist)
        put("cameraDistanceScale", cameraDistanceScale.toDouble())
        put("musicVolume", musicVolume.toDouble())
        put("soundVolume", soundVolume.toDouble())
        put("voiceChatVolume", voiceChatVolume.toDouble())
        put("vibrationEnabled", vibrationEnabled)
        put("autoPickup", autoPickup)
        put("leftHandedControls", leftHandedControls)
    }

    companion object {
        fun fromJson(obj: JSONObject?): GameplaySettingsConfig {
            if (obj == null) return GameplaySettingsConfig()
            val gfxName = obj.optString("graphicsPreset", GraphicsPreset.HIGH.name)
            return GameplaySettingsConfig(
                graphicsPreset = GraphicsPreset.entries.firstOrNull { it.name == gfxName } ?: GraphicsPreset.HIGH,
                targetFps = obj.optInt("targetFps", 60),
                cameraSensitivity = obj.optDouble("cameraSensitivity", 1.0).toFloat(),
                aimSensitivity = obj.optDouble("aimSensitivity", 0.85).toFloat(),
                scopeSensitivity = obj.optDouble("scopeSensitivity", 0.65).toFloat(),
                joystickSensitivity = obj.optDouble("joystickSensitivity", 1.0).toFloat(),
                autoSprint = obj.optBoolean("autoSprint", false),
                aimAssist = obj.optBoolean("aimAssist", true),
                cameraDistanceScale = obj.optDouble("cameraDistanceScale", 1.0).toFloat(),
                musicVolume = obj.optDouble("musicVolume", 0.7).toFloat(),
                soundVolume = obj.optDouble("soundVolume", 0.85).toFloat(),
                voiceChatVolume = obj.optDouble("voiceChatVolume", 0.8).toFloat(),
                vibrationEnabled = obj.optBoolean("vibrationEnabled", true),
                autoPickup = obj.optBoolean("autoPickup", true),
                leftHandedControls = obj.optBoolean("leftHandedControls", false)
            ).sanitized()
        }
    }
}

data class PlayerHudSettingsBundle(
    val userId: String,
    val activeLayoutIndex: Int = 0,
    val layouts: List<HudLayoutProfile> = HudLayoutPresets.defaultFiveLayouts(),
    val gameplay: GameplaySettingsConfig = GameplaySettingsConfig(),
    val unlockedStyleKeys: Set<String> = HeroAppearanceCatalog.starterUnlockedKeys(),
    val selectedHairCutId: String = "spiky_hero",
    val selectedHeadgearId: String = "gear_none",
    val selectedWeaponSkinId: String = "wskin_crimson",
    val selectedExpressionId: String = "fierce"
) {
    val activeLayout: HudLayoutProfile
        get() = layouts.getOrElse(activeLayoutIndex.coerceIn(0, 4)) { HudLayoutPresets.createPreset(0) }

    fun withUpdatedActiveLayout(layout: HudLayoutProfile): PlayerHudSettingsBundle {
        val idx = activeLayoutIndex.coerceIn(0, 4)
        val next = layouts.toMutableList()
        while (next.size < 5) next.add(HudLayoutPresets.createPreset(next.size))
        next[idx] = layout.copy(slotIndex = idx)
        return copy(layouts = next)
    }

    fun toJsonString(): String = JSONObject().apply {
        put("activeLayoutIndex", activeLayoutIndex.coerceIn(0, 4))
        val layoutsArr = JSONArray()
        (0..4).forEach { i ->
            val l = layouts.getOrElse(i) { HudLayoutPresets.createPreset(i) }
            layoutsArr.put(l.toJson())
        }
        put("layouts", layoutsArr)
        put("gameplay", gameplay.toJson())
        val unlockedArr = JSONArray()
        (unlockedStyleKeys + HeroAppearanceCatalog.starterUnlockedKeys()).sorted().forEach {
            unlockedArr.put(it)
        }
        put("unlockedStyleKeys", unlockedArr)
        put("selectedHairCutId", selectedHairCutId)
        put("selectedHeadgearId", selectedHeadgearId)
        put("selectedWeaponSkinId", selectedWeaponSkinId)
        put("selectedExpressionId", selectedExpressionId)
    }.toString()

    fun toFirestoreCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "activeLayoutIndex" to activeLayoutIndex.coerceIn(0, 4).toLong(),
        "payloadJson" to toJsonString().take(31500),
        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
    )

    fun toFirestoreUpdateMap(): Map<String, Any> = mapOf(
        "activeLayoutIndex" to activeLayoutIndex.coerceIn(0, 4).toLong(),
        "payloadJson" to toJsonString().take(31500),
        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
    )

    companion object {
        fun defaultForUser(uid: String): PlayerHudSettingsBundle = PlayerHudSettingsBundle(
            userId = uid,
            activeLayoutIndex = 0,
            layouts = HudLayoutPresets.defaultFiveLayouts(),
            gameplay = GameplaySettingsConfig(),
            unlockedStyleKeys = HeroAppearanceCatalog.starterUnlockedKeys()
        )

        fun fromJsonString(raw: String?, uid: String): PlayerHudSettingsBundle {
            if (raw.isNullOrBlank()) return defaultForUser(uid)
            return try {
                val root = JSONObject(raw)
                val activeIdx = root.optInt("activeLayoutIndex", 0).coerceIn(0, 4)
                val arr = root.optJSONArray("layouts")
                val layouts = (0..4).map { idx ->
                    val obj = arr?.optJSONObject(idx)
                    if (obj != null) HudLayoutProfile.fromJson(obj, idx)
                    else HudLayoutPresets.createPreset(idx)
                }
                val gameplay = GameplaySettingsConfig.fromJson(root.optJSONObject("gameplay"))
                val unlockedArr = root.optJSONArray("unlockedStyleKeys")
                val unlockedSet = HeroAppearanceCatalog.starterUnlockedKeys().toMutableSet()
                if (unlockedArr != null) {
                    for (i in 0 until unlockedArr.length()) {
                        val key = unlockedArr.optString(i)
                        if (key.isNotBlank()) unlockedSet.add(key)
                    }
                }
                PlayerHudSettingsBundle(
                    userId = uid,
                    activeLayoutIndex = activeIdx,
                    layouts = layouts,
                    gameplay = gameplay,
                    unlockedStyleKeys = unlockedSet,
                    selectedHairCutId = root.optString("selectedHairCutId", "spiky_hero").ifBlank { "spiky_hero" },
                    selectedHeadgearId = root.optString("selectedHeadgearId", "gear_none").ifBlank { "gear_none" },
                    selectedWeaponSkinId = root.optString("selectedWeaponSkinId", "wskin_crimson").ifBlank { "wskin_crimson" },
                    selectedExpressionId = root.optString("selectedExpressionId", "fierce").ifBlank { "fierce" }
                )
            } catch (_: Exception) {
                defaultForUser(uid)
            }
        }
    }
}
