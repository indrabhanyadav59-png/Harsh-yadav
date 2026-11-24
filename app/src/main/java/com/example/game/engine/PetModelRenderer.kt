package com.example.game.engine

import android.graphics.Color
import com.example.data.model.PetArchetype
import com.example.data.model.PetCatalog
import com.example.data.model.PetSpec
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

interface PetPrimitiveDrawer {
    fun petPart(x: Float, y: Float, z: Float, rx: Float, ry: Float, rz: Float, sx: Float, sy: Float, sz: Float, colorInt: Int, emissive: Float = 0f)
    fun petSphere(x: Float, y: Float, z: Float, diameter: Float, colorInt: Int, emissive: Float = 0f)
    fun petDome(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, colorInt: Int, emissive: Float = 0f)
    fun petCyl(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, colorInt: Int, emissive: Float = 0f)
    fun petCone(x: Float, y: Float, z: Float, rx: Float, ry: Float, rz: Float, sx: Float, sy: Float, sz: Float, colorInt: Int, emissive: Float = 0f)
}

/**
 * Procedural Cute Anime 3D Pet Model Renderer covering all 50 pets,
 * Level 1/2/3 evolution forms, custom chroma tints, aura styles,
 * and distinct animations (walk, hop, fly wing-flap, slither, swim bob, crystal float).
 */
object PetModelRenderer {

    fun drawAnimePet(
        drawer: PetPrimitiveDrawer,
        x: Float,
        y: Float,
        z: Float,
        rotY: Float,
        petTypeOrId: String,
        bobPhase: Float,
        evolutionLevel: Int = 1,
        customTintHex: String = "",
        auraStyle: String = "Classic",
        worldTimeSec: Float = 0f,
        isWildBeacon: Boolean = false
    ) {
        val spec = PetCatalog.resolveSpec(petTypeOrId)
        val evo = evolutionLevel.coerceIn(1, 3)
        val scale = when (evo) {
            1 -> 0.88f
            2 -> 1.06f
            else -> 1.24f
        }
        val rad = Math3D.degToRad(rotY)
        val fwdX = sin(rad)
        val fwdZ = cos(rad)
        val rightX = cos(rad)
        val rightZ = -sin(rad)

        val primaryColor = if (customTintHex.isNotBlank()) {
            parseTintOrFallback(customTintHex, spec.primaryColorHex.toInt())
        } else {
            spec.primaryColorHex.toInt()
        }
        val secondaryColor = spec.secondaryColorHex.toInt()
        val accentColor = spec.accentColorHex.toInt()
        val eyeColor = spec.eyeColorHex.toInt()

        val hop = when (spec.archetype) {
            PetArchetype.RABBIT -> abs(sin(bobPhase * 1.4f)) * 0.28f * scale
            PetArchetype.DRAGON, PetArchetype.AVIAN_RAPTOR, PetArchetype.AVIAN_SONGBIRD,
            PetArchetype.AVIAN_WATERFOWL, PetArchetype.AVIAN_PHOENIX -> sin(bobPhase * 1.3f) * 0.16f * scale
            PetArchetype.DOLPHIN_WHALE_SHARK, PetArchetype.JELLYFISH, PetArchetype.WATER_SPIRIT,
            PetArchetype.CRYSTAL_GOLEM, PetArchetype.CELESTIAL_BEAST -> sin(bobPhase * 1.1f) * 0.14f * scale
            else -> abs(sin(bobPhase)) * 0.08f * scale
        }
        val baseY = y + hop

        // Ground / Aura / Wild Adoption Rings
        if (isWildBeacon) {
            val pulse = 1.0f + sin(worldTimeSec * 4.5f) * 0.18f
            drawer.petCyl(x, y + 0.05f, z, 2.2f * pulse, 0.06f, 2.2f * pulse, 0xFFFFD166.toInt(), emissive = 0.9f)
        }
        if (evo >= 2 || auraStyle != "Classic") {
            val ringCol = when (auraStyle) {
                "Sakura" -> 0xFFF472B6.toInt()
                "Thunder" -> 0xFF00E5FF.toInt()
                "Celestial" -> 0xFFFFD166.toInt()
                "Shadow" -> 0xFFA855F7.toInt()
                else -> accentColor
            }
            val ringRad = (1.15f + 0.18f * evo) * scale
            drawer.petCyl(x, y + 0.04f, z, ringRad, 0.04f, ringRad, ringCol, emissive = if (evo >= 3) 0.95f else 0.6f)
        }

        when (spec.archetype) {
            PetArchetype.FOX_CANINE, PetArchetype.KITSUNE_NINE, PetArchetype.FELINE, PetArchetype.DEER,
            PetArchetype.RABBIT, PetArchetype.BEAR_PANDA, PetArchetype.UNICORN ->
                drawQuadrupedAnimePet(
                    drawer, spec, x, baseY, z, rotY, fwdX, fwdZ, rightX, rightZ,
                    scale, bobPhase, evo, primaryColor, secondaryColor, accentColor, eyeColor
                )

            PetArchetype.AVIAN_RAPTOR, PetArchetype.AVIAN_SONGBIRD,
            PetArchetype.AVIAN_WATERFOWL, PetArchetype.AVIAN_PHOENIX ->
                drawAvianAnimePet(
                    drawer, spec, x, baseY, z, rotY, fwdX, fwdZ, rightX, rightZ,
                    scale, bobPhase, evo, primaryColor, secondaryColor, accentColor, eyeColor
                )

            PetArchetype.DRAGON, PetArchetype.CRYSTAL_GOLEM, PetArchetype.CELESTIAL_BEAST ->
                drawDragonOrGuardianPet(
                    drawer, spec, x, baseY, z, rotY, fwdX, fwdZ, rightX, rightZ,
                    scale, bobPhase, evo, primaryColor, secondaryColor, accentColor, eyeColor
                )

            PetArchetype.TURTLE, PetArchetype.CROCODILE, PetArchetype.LIZARD_GECKO ->
                drawReptileAnimePet(
                    drawer, spec, x, baseY, z, rotY, fwdX, fwdZ, rightX, rightZ,
                    scale, bobPhase, evo, primaryColor, secondaryColor, accentColor, eyeColor
                )

            PetArchetype.SERPENT ->
                drawSerpentAnimePet(
                    drawer, x, baseY, z, rotY, fwdX, fwdZ, rightX, rightZ,
                    scale, bobPhase, evo, primaryColor, secondaryColor, accentColor, eyeColor
                )

            PetArchetype.DOLPHIN_WHALE_SHARK, PetArchetype.SEAL_OTTER, PetArchetype.PENGUIN,
            PetArchetype.OCTOPUS, PetArchetype.JELLYFISH, PetArchetype.SEAHORSE, PetArchetype.WATER_SPIRIT ->
                drawAquaticAnimePet(
                    drawer, spec, x, baseY, z, rotY, fwdX, fwdZ, rightX, rightZ,
                    scale, bobPhase, evo, primaryColor, secondaryColor, accentColor, eyeColor
                )
        }

        // Level 3 Legendary Orbiting Halo / Crown Orbs
        if (evo >= 3) {
            val haloY = baseY + 1.25f * scale
            for (k in 0 until 3) {
                val orbAngle = worldTimeSec * 2.6f + k * 2.094f
                val ox = x + cos(orbAngle) * 0.72f * scale
                val oz = z + sin(orbAngle) * 0.72f * scale
                drawer.petSphere(ox, haloY + sin(worldTimeSec * 3f + k) * 0.08f, oz, 0.14f * scale, accentColor, emissive = 0.95f)
            }
        }
    }

    private fun drawAnimeFaceEyes(
        drawer: PetPrimitiveDrawer,
        headX: Float, headY: Float, headZ: Float,
        fwdX: Float, fwdZ: Float, rightX: Float, rightZ: Float,
        headRadius: Float, eyeColor: Int, eyeSpacing: Float = 0.36f
    ) {
        val eyeForward = headRadius * 0.86f
        val side = headRadius * eyeSpacing
        for (dir in listOf(-1f, 1f)) {
            val ex = headX + fwdX * eyeForward + rightX * (side * dir)
            val ez = headZ + fwdZ * eyeForward + rightZ * (side * dir)
            // Outer white sclera + large anime iris + highlight
            drawer.petSphere(ex, headY + headRadius * 0.08f, ez, headRadius * 0.30f, 0xFFFFFFFF.toInt(), emissive = 0.25f)
            drawer.petSphere(
                ex + fwdX * headRadius * 0.08f,
                headY + headRadius * 0.08f,
                ez + fwdZ * headRadius * 0.08f,
                headRadius * 0.20f,
                eyeColor,
                emissive = 0.65f
            )
        }
    }

    private fun drawQuadrupedAnimePet(
        drawer: PetPrimitiveDrawer,
        spec: PetSpec,
        x: Float, y: Float, z: Float, rotY: Float,
        fwdX: Float, fwdZ: Float, rightX: Float, rightZ: Float,
        s: Float, bob: Float, evo: Int,
        primary: Int, secondary: Int, accent: Int, eye: Int
    ) {
        // Rounded chibi body + soft chest bib
        drawer.petPart(x, y + 0.36f * s, z, 0f, rotY, 0f, 0.48f * s, 0.40f * s, 0.78f * s, primary)
        drawer.petSphere(x, y + 0.38f * s, z, 0.56f * s, primary)
        drawer.petSphere(x + fwdX * 0.25f * s, y + 0.35f * s, z + fwdZ * 0.25f * s, 0.42f * s, secondary)

        // 4 animated legs
        val legStride = sin(bob * 1.4f) * 18f
        for (fb in listOf(0.24f, -0.24f)) {
            for (lr in listOf(-0.16f, 0.16f)) {
                val lx = x + fwdX * fb * s + rightX * lr * s
                val lz = z + fwdZ * fb * s + rightZ * lr * s
                val swing = if ((fb > 0) == (lr > 0)) legStride else -legStride
                drawer.petPart(lx, y + 0.16f * s, lz, swing, rotY, 0f, 0.13f * s, 0.30f * s, 0.13f * s, secondary)
            }
        }

        // Cute oversized anime head + muzzle + expressive eyes
        val hx = x + fwdX * 0.40f * s
        val hy = y + 0.68f * s
        val hz = z + fwdZ * 0.40f * s
        val headDia = 0.54f * s
        drawer.petSphere(hx, hy, hz, headDia, primary)
        // Snout / cute nose
        val snoutDia = if (spec.archetype == PetArchetype.BEAR_PANDA) 0.24f * s else 0.18f * s
        drawer.petSphere(hx + fwdX * 0.24f * s, hy - 0.06f * s, hz + fwdZ * 0.24f * s, snoutDia, secondary)
        drawAnimeFaceEyes(drawer, hx, hy, hz, fwdX, fwdZ, rightX, rightZ, headDia * 0.5f, eye)

        // Distinct species ears / horns / mane / antlers
        when (spec.archetype) {
            PetArchetype.RABBIT -> {
                for (lr in listOf(-0.14f, 0.14f)) {
                    drawer.petPart(hx + rightX * lr * s, hy + 0.42f * s, hz + rightZ * lr * s, -12f, rotY, lr * 45f, 0.13f * s, 0.48f * s, 0.09f * s, primary)
                    drawer.petPart(hx + rightX * lr * s, hy + 0.40f * s, hz + rightZ * lr * s, -12f, rotY, lr * 45f, 0.07f * s, 0.36f * s, 0.06f * s, secondary)
                }
                // Fluffy cotton bobtail
                drawer.petSphere(x - fwdX * 0.42f * s, y + 0.38f * s, z - fwdZ * 0.42f * s, 0.22f * s, secondary, emissive = 0.2f)
            }
            PetArchetype.BEAR_PANDA -> {
                for (lr in listOf(-0.20f, 0.20f)) {
                    drawer.petSphere(hx + rightX * lr * s, hy + 0.24f * s, hz + rightZ * lr * s, 0.18f * s, secondary)
                }
            }
            PetArchetype.DEER -> {
                for (lr in listOf(-0.16f, 0.16f)) {
                    drawer.petCone(hx + rightX * lr * s, hy + 0.30f * s, hz + rightZ * lr * s, 0f, rotY, lr * 95f, 0.11f * s, 0.26f * s, 0.11f * s, secondary)
                    // Branching antlers
                    drawer.petPart(hx + rightX * lr * 1.15f * s, hy + 0.48f * s, hz + rightZ * lr * 1.15f * s, 0f, rotY, -lr * 110f, 0.05f * s, 0.34f * s, 0.05f * s, accent, emissive = 0.45f)
                    drawer.petPart(hx + rightX * lr * 1.45f * s, hy + 0.56f * s, hz + rightZ * lr * 1.45f * s, 0f, rotY, lr * 60f, 0.04f * s, 0.18f * s, 0.04f * s, accent, emissive = 0.45f)
                }
            }
            PetArchetype.UNICORN -> {
                for (lr in listOf(-0.16f, 0.16f)) {
                    drawer.petCone(hx + rightX * lr * s, hy + 0.30f * s, hz + rightZ * lr * s, 0f, rotY, 0f, 0.12f * s, 0.22f * s, 0.12f * s, primary)
                }
                // Glowing spiral golden horn + pastel mane
                drawer.petCone(hx + fwdX * 0.14f * s, hy + 0.42f * s, hz + fwdZ * 0.14f * s, 18f, rotY, 0f, 0.09f * s, 0.42f * s, 0.09f * s, accent, emissive = 0.9f)
                drawer.petPart(hx - fwdX * 0.12f * s, hy + 0.06f * s, hz - fwdZ * 0.12f * s, -22f, rotY, 0f, 0.16f * s, 0.40f * s, 0.24f * s, secondary, emissive = 0.4f)
            }
            PetArchetype.FELINE -> {
                for (lr in listOf(-0.17f, 0.17f)) {
                    drawer.petCone(hx + rightX * lr * s, hy + 0.28f * s, hz + rightZ * lr * s, 0f, rotY, 0f, 0.14f * s, 0.22f * s, 0.14f * s, secondary, emissive = 0.3f)
                }
                if (spec.name.contains("Lion")) {
                    drawer.petSphere(hx - fwdX * 0.06f * s, hy - 0.02f * s, hz - fwdZ * 0.06f * s, 0.66f * s, secondary, emissive = if (spec.id.contains("flame")) 0.45f else 0.15f)
                }
            }
            else -> {
                // Fox / Wolf / Dog / Kitsune pointed ears
                for (lr in listOf(-0.18f, 0.18f)) {
                    drawer.petCone(hx + rightX * lr * s, hy + 0.32f * s, hz + rightZ * lr * s, 0f, rotY, 0f, 0.15f * s, 0.28f * s, 0.15f * s, secondary, emissive = 0.45f)
                }
            }
        }

        // Evolution Tier 2+ Radiant Collar
        if (evo >= 2) {
            drawer.petCyl(x + fwdX * 0.28f * s, y + 0.48f * s, z + fwdZ * 0.28f * s, 0.36f * s, 0.08f * s, 0.36f * s, accent, emissive = 0.7f)
        }

        // Tails: Kitsune grows 1 -> 5 -> 9 glowing tails across evolutions!
        val tailCount = when (spec.archetype) {
            PetArchetype.KITSUNE_NINE -> if (evo == 1) 3 else if (evo == 2) 5 else 9
            PetArchetype.FOX_CANINE -> if (evo >= 3) 3 else 1
            PetArchetype.RABBIT -> 0
            else -> 1
        }
        val mid = (tailCount - 1) / 2f
        for (t in 0 until tailCount) {
            val offsetAngle = (t - mid) * (if (tailCount > 5) 18f else 24f)
            val tailYaw = rotY + offsetAngle + sin(bob + t * 0.7f) * 9f
            val tRad = Math3D.degToRad(tailYaw)
            drawer.petPart(
                x - sin(tRad) * 0.48f * s,
                y + 0.50f * s,
                z - cos(tRad) * 0.48f * s,
                34f, tailYaw, 0f,
                0.15f * s, 0.15f * s, 0.68f * s,
                if (t % 2 == 0) secondary else accent,
                emissive = if (evo >= 2 || spec.archetype == PetArchetype.KITSUNE_NINE) 0.7f else 0.25f
            )
        }
    }

    private fun drawAvianAnimePet(
        drawer: PetPrimitiveDrawer,
        spec: PetSpec,
        x: Float, y: Float, z: Float, rotY: Float,
        fwdX: Float, fwdZ: Float, rightX: Float, rightZ: Float,
        s: Float, bob: Float, evo: Int,
        primary: Int, secondary: Int, accent: Int, eye: Int
    ) {
        val hoverY = y + 0.55f * s
        drawer.petSphere(x, hoverY, z, 0.50f * s, primary)
        val headY = hoverY + if (spec.name == "Swan") 0.42f * s else 0.30f * s
        val hx = x + fwdX * 0.20f * s
        val hz = z + fwdZ * 0.20f * s
        if (spec.name == "Swan") {
            drawer.petPart(x + fwdX * 0.14f * s, hoverY + 0.20f * s, z + fwdZ * 0.14f * s, 14f, rotY, 0f, 0.14f * s, 0.38f * s, 0.14f * s, primary)
        }
        drawer.petSphere(hx, headY, hz, 0.38f * s, primary)
        drawAnimeFaceEyes(drawer, hx, headY, hz, fwdX, fwdZ, rightX, rightZ, 0.19f * s, eye)
        // Beak / crest
        drawer.petCone(hx + fwdX * 0.22f * s, headY - 0.02f * s, hz + fwdZ * 0.22f * s, 90f, rotY, 0f, 0.12f * s, 0.18f * s, 0.12f * s, accent, emissive = 0.35f)
        if (spec.name == "Owl") {
            for (lr in listOf(-0.14f, 0.14f)) {
                drawer.petCone(hx + rightX * lr * s, headY + 0.22f * s, hz + rightZ * lr * s, 0f, rotY, lr * 55f, 0.10f * s, 0.18f * s, 0.10f * s, secondary)
            }
        } else if (spec.name in listOf("Phoenix", "Firebird", "Parrot", "Peacock")) {
            drawer.petCone(hx - fwdX * 0.05f * s, headY + 0.26f * s, hz - fwdZ * 0.05f * s, -24f, rotY, 0f, 0.12f * s, 0.28f * s, 0.16f * s, accent, emissive = 0.85f)
        }

        // Flapping wings
        val flap = sin(bob * 2.2f) * 26f
        for (dir in listOf(-1f, 1f)) {
            drawer.petPart(
                x + rightX * (0.48f * dir * s), hoverY + 0.08f * s, z + rightZ * (0.48f * dir * s),
                0f, rotY, flap * dir,
                0.76f * s, 0.07f * s, 0.36f * s,
                secondary,
                emissive = if (spec.archetype == PetArchetype.AVIAN_PHOENIX || evo >= 2) 0.75f else 0.2f
            )
        }

        // Tail plumes (fanned for Peacock / Phoenix)
        val fanCount = if (spec.name == "Peacock" || spec.archetype == PetArchetype.AVIAN_PHOENIX) (2 + evo) else 1
        val mid = (fanCount - 1) / 2f
        for (i in 0 until fanCount) {
            val yaw = rotY + (i - mid) * 22f
            val tRad = Math3D.degToRad(yaw)
            drawer.petPart(
                x - sin(tRad) * 0.44f * s, hoverY + 0.08f * s, z - cos(tRad) * 0.44f * s,
                -22f, yaw, 0f, 0.18f * s, 0.06f * s, 0.62f * s,
                if (i % 2 == 0) accent else secondary,
                emissive = 0.65f
            )
        }
    }

    private fun drawDragonOrGuardianPet(
        drawer: PetPrimitiveDrawer,
        spec: PetSpec,
        x: Float, y: Float, z: Float, rotY: Float,
        fwdX: Float, fwdZ: Float, rightX: Float, rightZ: Float,
        s: Float, bob: Float, evo: Int,
        primary: Int, secondary: Int, accent: Int, eye: Int
    ) {
        val hoverY = y + 0.72f * s
        drawer.petSphere(x, hoverY, z, 0.56f * s, primary)
        drawer.petSphere(x + fwdX * 0.16f * s, hoverY - 0.05f * s, z + fwdZ * 0.16f * s, 0.34f * s, accent, emissive = 0.55f)

        val hx = x + fwdX * 0.34f * s
        val hy = hoverY + 0.34f * s
        val hz = z + fwdZ * 0.34f * s
        val headDia = if (spec.name == "Baby Dragon") 0.56f * s else 0.48f * s
        drawer.petSphere(hx, hy, hz, headDia, primary)
        drawAnimeFaceEyes(drawer, hx, hy, hz, fwdX, fwdZ, rightX, rightZ, headDia * 0.5f, eye)

        // Horns / Crystal Crown
        for (dir in listOf(-1f, 1f)) {
            drawer.petCone(
                hx + rightX * (0.16f * dir * s) - fwdX * 0.06f * s,
                hy + 0.30f * s,
                hz + rightZ * (0.16f * dir * s) - fwdZ * 0.06f * s,
                -18f, rotY, dir * 18f,
                0.12f * s, 0.32f * s, 0.12f * s,
                accent, emissive = 0.85f
            )
        }

        if (spec.archetype == PetArchetype.CRYSTAL_GOLEM) {
            // Orbiting crystal gauntlets
            val swing = sin(bob * 1.5f) * 0.12f * s
            for (dir in listOf(-1f, 1f)) {
                drawer.petCone(x + rightX * (0.50f * dir * s), hoverY + swing * dir, z + rightZ * (0.50f * dir * s), 0f, rotY, dir * 24f, 0.24f * s, 0.42f * s, 0.24f * s, secondary, emissive = 0.8f)
            }
        } else {
            // Dragon / Celestial wings
            val flap = sin(bob * 1.9f) * 24f
            for (dir in listOf(-1f, 1f)) {
                drawer.petPart(
                    x + rightX * (0.58f * dir * s), hoverY + 0.15f * s, z + rightZ * (0.58f * dir * s),
                    0f, rotY, flap * dir,
                    0.96f * s, 0.08f * s, 0.48f * s,
                    secondary, emissive = 0.7f
                )
            }
            // Sweeping dragon tail
            drawer.petPart(x - fwdX * 0.50f * s, hoverY - 0.10f * s, z - fwdZ * 0.50f * s, 26f, rotY, 0f, 0.18f * s, 0.18f * s, 0.72f * s, secondary, emissive = 0.4f)
            if (evo >= 2) {
                drawer.petCone(x - fwdX * 0.85f * s, hoverY - 0.22f * s, z - fwdZ * 0.85f * s, -75f, rotY, 0f, 0.18f * s, 0.26f * s, 0.18f * s, accent, emissive = 0.9f)
            }
        }
    }

    private fun drawReptileAnimePet(
        drawer: PetPrimitiveDrawer,
        spec: PetSpec,
        x: Float, y: Float, z: Float, rotY: Float,
        fwdX: Float, fwdZ: Float, rightX: Float, rightZ: Float,
        s: Float, bob: Float, evo: Int,
        primary: Int, secondary: Int, accent: Int, eye: Int
    ) {
        if (spec.archetype == PetArchetype.TURTLE) {
            drawer.petDome(x, y + 0.22f * s, z, 0.78f * s, 0.48f * s, 0.88f * s, primary)
            drawer.petDome(x, y + 0.42f * s, z, 0.42f * s, 0.24f * s, 0.48f * s, secondary, emissive = if (evo >= 2) 0.5f else 0.15f)
        } else {
            val len = if (spec.archetype == PetArchetype.CROCODILE) 0.96f * s else 0.68f * s
            drawer.petPart(x, y + 0.26f * s, z, 0f, rotY, 0f, 0.40f * s, 0.28f * s, len, primary)
        }
        val hx = x + fwdX * 0.46f * s
        val hy = y + 0.42f * s
        val hz = z + fwdZ * 0.46f * s
        drawer.petSphere(hx, hy, hz, 0.40f * s, secondary)
        drawAnimeFaceEyes(drawer, hx, hy, hz, fwdX, fwdZ, rightX, rightZ, 0.20f * s, eye)
        drawer.petPart(x - fwdX * 0.52f * s, y + 0.26f * s, z - fwdZ * 0.52f * s, 8f, rotY + sin(bob) * 14f, 0f, 0.16f * s, 0.14f * s, 0.56f * s, accent)
    }

    private fun drawSerpentAnimePet(
        drawer: PetPrimitiveDrawer,
        x: Float, y: Float, z: Float, rotY: Float,
        fwdX: Float, fwdZ: Float, rightX: Float, rightZ: Float,
        s: Float, bob: Float, evo: Int,
        primary: Int, secondary: Int, accent: Int, eye: Int
    ) {
        // Coiled undulating anime serpent segments
        for (seg in 0..4) {
            val sway = sin(bob * 1.6f + seg * 0.9f) * 0.18f * s
            val sx = x - fwdX * (seg * 0.22f * s) + rightX * sway
            val sz = z - fwdZ * (seg * 0.22f * s) + rightZ * sway
            val sy = y + (0.28f + seg * 0.04f) * s
            val dia = (0.36f - seg * 0.045f) * s
            drawer.petSphere(sx, sy, sz, dia, if (seg % 2 == 0) primary else secondary, emissive = if (evo >= 2) 0.4f else 0.15f)
        }
        val hx = x + fwdX * 0.22f * s
        val hy = y + 0.62f * s
        val hz = z + fwdZ * 0.22f * s
        drawer.petSphere(hx, hy, hz, 0.42f * s, primary)
        drawAnimeFaceEyes(drawer, hx, hy, hz, fwdX, fwdZ, rightX, rightZ, 0.21f * s, eye)
        drawer.petCone(hx, hy + 0.28f * s, hz, 0f, rotY, 0f, 0.12f * s, 0.22f * s, 0.12f * s, accent, emissive = 0.8f)
    }

    private fun drawAquaticAnimePet(
        drawer: PetPrimitiveDrawer,
        spec: PetSpec,
        x: Float, y: Float, z: Float, rotY: Float,
        fwdX: Float, fwdZ: Float, rightX: Float, rightZ: Float,
        s: Float, bob: Float, evo: Int,
        primary: Int, secondary: Int, accent: Int, eye: Int
    ) {
        val swimY = y + if (spec.archetype == PetArchetype.PENGUIN) 0.40f * s else 0.62f * s
        when (spec.archetype) {
            PetArchetype.JELLYFISH, PetArchetype.WATER_SPIRIT -> {
                drawer.petDome(x, swimY + 0.16f * s, z, 0.60f * s, 0.42f * s, 0.60f * s, primary, emissive = 0.7f)
                drawer.petSphere(x, swimY + 0.08f * s, z, 0.34f * s, secondary, emissive = 0.85f)
                drawAnimeFaceEyes(drawer, x, swimY + 0.12f * s, z, fwdX, fwdZ, rightX, rightZ, 0.24f * s, eye)
                for (t in 0 until 4) {
                    val ang = t * 1.57f + bob * 0.6f
                    drawer.petPart(
                        x + cos(ang) * 0.18f * s, swimY - 0.18f * s, z + sin(ang) * 0.18f * s,
                        sin(bob + t) * 12f, rotY, 0f, 0.08f * s, 0.44f * s, 0.08f * s, accent, emissive = 0.8f
                    )
                }
            }
            PetArchetype.OCTOPUS -> {
                drawer.petSphere(x, swimY + 0.14f * s, z, 0.56f * s, primary)
                drawAnimeFaceEyes(drawer, x, swimY + 0.14f * s, z, fwdX, fwdZ, rightX, rightZ, 0.27f * s, eye)
                for (t in 0 until 6) {
                    val ang = t * 1.047f
                    drawer.petPart(
                        x + cos(ang) * 0.24f * s, swimY - 0.18f * s, z + sin(ang) * 0.24f * s,
                        sin(bob * 1.8f + t) * 20f, rotY, 0f, 0.11f * s, 0.30f * s, 0.11f * s, secondary
                    )
                }
            }
            PetArchetype.PENGUIN -> {
                drawer.petSphere(x, swimY, z, 0.52f * s, primary)
                drawer.petSphere(x + fwdX * 0.12f * s, swimY - 0.02f * s, z + fwdZ * 0.12f * s, 0.38f * s, secondary)
                val hx = x + fwdX * 0.06f * s
                val hy = swimY + 0.34f * s
                val hz = z + fwdZ * 0.06f * s
                drawer.petSphere(hx, hy, hz, 0.40f * s, primary)
                drawAnimeFaceEyes(drawer, hx, hy, hz, fwdX, fwdZ, rightX, rightZ, 0.20f * s, eye)
                drawer.petCone(hx + fwdX * 0.22f * s, hy - 0.03f * s, hz + fwdZ * 0.22f * s, 90f, rotY, 0f, 0.11f * s, 0.16f * s, 0.11f * s, accent)
            }
            else -> {
                // Dolphin / Whale / Shark / Seal / Otter / Seahorse streamlined anime body
                drawer.petSphere(x, swimY, z, 0.56f * s, primary)
                drawer.petSphere(x + fwdX * 0.30f * s, swimY + 0.06f * s, z + fwdZ * 0.30f * s, 0.44f * s, primary)
                drawAnimeFaceEyes(drawer, x + fwdX * 0.30f * s, swimY + 0.06f * s, z + fwdZ * 0.30f * s, fwdX, fwdZ, rightX, rightZ, 0.22f * s, eye)
                // Side flippers
                for (dir in listOf(-1f, 1f)) {
                    drawer.petPart(x + rightX * (0.34f * dir * s), swimY - 0.06f * s, z + rightZ * (0.34f * dir * s), 0f, rotY, 24f * dir, 0.34f * s, 0.06f * s, 0.20f * s, secondary)
                }
                // Dorsal fin for Dolphin/Shark + Tail fluke
                if (spec.name in listOf("Dolphin", "Shark")) {
                    drawer.petCone(x - fwdX * 0.05f * s, swimY + 0.34f * s, z - fwdZ * 0.05f * s, -22f, rotY, 0f, 0.12f * s, 0.30f * s, 0.22f * s, secondary, emissive = 0.4f)
                }
                drawer.petPart(x - fwdX * 0.48f * s, swimY, z - fwdZ * 0.48f * s, sin(bob * 2f) * 20f, rotY, 0f, 0.34f * s, 0.08f * s, 0.28f * s, accent, emissive = if (evo >= 2) 0.5f else 0.15f)
            }
        }
    }

    private fun parseTintOrFallback(hex: String, fallback: Int): Int = try {
        Color.parseColor(hex)
    } catch (_: Exception) {
        fallback
    }
}
