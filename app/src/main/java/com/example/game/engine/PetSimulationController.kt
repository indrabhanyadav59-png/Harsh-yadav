package com.example.game.engine

import com.example.data.model.CappedPetPassives
import com.example.data.model.OwnedPet
import com.example.data.model.PetActiveType
import com.example.data.model.PetCatalog
import com.example.data.model.PetSpec
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

data class WildWorldPet(
    val wildId: String,
    val petId: String,
    var x: Float,
    var y: Float,
    var z: Float,
    val baseX: Float,
    val baseZ: Float,
    var rotY: Float = 0f,
    val evolutionLevel: Int = 2,
    val temperament: String = "Curious",
    var adopted: Boolean = false
)

/**
 * Modular runtime engine for all 50 pets:
 * - Evaluates capped passives across up to 5 equipped roster pets (anti-stacking enforced)
 * - Runs periodic passive ticks (HP/Shield/EP) and active buff timers
 * - Executes all 50 active & passive pet abilities with real world combat/physics effects
 * - Simulates wild Lv.2/Lv.3 adoptable world pets
 */
class PetSimulationController {
    var equippedRoster: List<OwnedPet> = emptyList()
        private set
    var cappedPassives: CappedPetPassives = CappedPetPassives()
        private set

    // Timed active buffs
    var speedBuffTimerSec = 0f
    var speedBuffMultiplier = 1f
    var attackBuffTimerSec = 0f
    var attackBuffBonusPct = 0f
    var protectionTimerSec = 0f
    var protectionReductionPct = 0f
    var radarPulseTimerSec = 0f
    var lootScanTimerSec = 0f
    var camouflageTimerSec = 0f

    // Heal-over-time buffer (e.g., Fox Flame: 30 HP over 5s)
    var hotHpPerSec = 0f
    var hotRemainingSec = 0f

    // Periodic passive accumulators
    private var periodicHpClock = 0f
    private var periodicShieldClock = 0f
    private var periodicEpClock = 0f

    // Phoenix emergency knockdown save (once per life)
    var emergencySaveUsedThisLife = false

    fun updateRoster(pets: List<OwnedPet>) {
        equippedRoster = pets.filter { it.equipped }.take(PetCatalog.MAX_EQUIPPED_ROSTER)
        cappedPassives = PetCatalog.computeCappedPassives(equippedRoster)
    }

    fun resetOnRespawn() {
        emergencySaveUsedThisLife = false
        speedBuffTimerSec = 0f
        attackBuffTimerSec = 0f
        protectionTimerSec = 0f
        camouflageTimerSec = 0f
        hotRemainingSec = 0f
    }

    fun effectiveSpeedMultiplier(inWater: Boolean): Float {
        val basePassive = 1f + cappedPassives.speedBonusPct + if (inWater) cappedPassives.waterSpeedBonusPct else 0f
        val activeBonus = if (speedBuffTimerSec > 0f) speedBuffMultiplier else 1f
        return (basePassive * activeBonus).coerceAtMost(1.55f)
    }

    fun effectiveAttackMultiplier(): Float {
        val passiveAtk = 1f + cappedPassives.attackBonusPct + cappedPassives.nightVisionCritBonus * 0.5f
        val activeAtk = if (attackBuffTimerSec > 0f) (1f + attackBuffBonusPct) else 1f
        return (passiveAtk * activeAtk).coerceAtMost(1.45f)
    }

    fun effectiveDamageTakenMultiplier(): Float {
        val passiveMit = (1f - cappedPassives.damageResistPct).coerceIn(0.70f, 1f)
        val activeMit = if (protectionTimerSec > 0f) (1f - protectionReductionPct).coerceIn(0.45f, 1f) else 1f
        return (passiveMit * activeMit).coerceAtLeast(0.40f)
    }

    fun effectiveReloadDuration(baseReloadSec: Float): Float =
        (baseReloadSec * (1f - cappedPassives.reloadSpeedBonusPct)).coerceAtLeast(0.45f)

    fun effectiveEnemyRadarRadius(): Float =
        if (radarPulseTimerSec > 0f) 78f else cappedPassives.enemyDetectRadius

    fun isLootRadarActive(): Boolean =
        lootScanTimerSec > 0f || cappedPassives.lootDetectRadius > 0f

    fun effectiveCamouflageFactor(): Float =
        if (camouflageTimerSec > 0f) 0.65f else cappedPassives.camouflageFactor

    fun stepPassivesAndBuffs(
        dt: Float,
        sim: WorldSimulationState,
        worldTimeSec: Float
    ) {
        if (speedBuffTimerSec > 0f) speedBuffTimerSec = (speedBuffTimerSec - dt).coerceAtLeast(0f)
        if (attackBuffTimerSec > 0f) attackBuffTimerSec = (attackBuffTimerSec - dt).coerceAtLeast(0f)
        if (protectionTimerSec > 0f) protectionTimerSec = (protectionTimerSec - dt).coerceAtLeast(0f)
        if (radarPulseTimerSec > 0f) radarPulseTimerSec = (radarPulseTimerSec - dt).coerceAtLeast(0f)
        if (lootScanTimerSec > 0f) lootScanTimerSec = (lootScanTimerSec - dt).coerceAtLeast(0f)
        if (camouflageTimerSec > 0f) camouflageTimerSec = (camouflageTimerSec - dt).coerceAtLeast(0f)

        // Heal-over-time (e.g., Fox Flame 30 HP / 5s)
        if (hotRemainingSec > 0f && !sim.isDefeated) {
            hotRemainingSec = (hotRemainingSec - dt).coerceAtLeast(0f)
            sim.hp = (sim.hp + hotHpPerSec * dt).coerceAtMost(sim.effectiveMaxHp())
        }

        // Periodic passive ticks (only while alive & outside loading)
        if (!sim.isDefeated && sim.showcaseMode == ShowcaseMode.NONE) {
            if (cappedPassives.periodicHpPer30s > 0f) {
                periodicHpClock += dt
                if (periodicHpClock >= 35f) {
                    periodicHpClock = 0f
                    sim.hp = (sim.hp + cappedPassives.periodicHpPer30s).coerceAtMost(sim.effectiveMaxHp())
                }
            }
            if (cappedPassives.periodicShieldPer30s > 0f) {
                periodicShieldClock += dt
                if (periodicShieldClock >= 30f) {
                    periodicShieldClock = 0f
                    sim.shield = (sim.shield + cappedPassives.periodicShieldPer30s).coerceAtMost(sim.effectiveMaxShield())
                }
            }
            if (cappedPassives.periodicEpPer25s > 0f) {
                periodicEpClock += dt
                if (periodicEpClock >= 25f) {
                    periodicEpClock = 0f
                    sim.ep = (sim.ep + cappedPassives.periodicEpPer25s).coerceAtMost(sim.maxEp)
                }
            }
        }

        // Gentle roaming animation for unadopted Wild Lv.2/Lv.3 rare pets
        for ((idx, wp) in sim.wildPets.withIndex()) {
            if (wp.adopted) continue
            val angle = worldTimeSec * 0.55f + idx * 1.1f
            wp.x = wp.baseX + cos(angle) * 4.2f
            wp.z = wp.baseZ + sin(angle) * 4.2f
            wp.y = WorldMapData.terrainHeightAt(wp.x, wp.z)
            wp.rotY = ((angle * 57.2958f) + 90f) % 360f
        }
    }

    fun onEnemyEliminated(sim: WorldSimulationState): String? {
        if (cappedPassives.hpOnKill <= 0f && cappedPassives.epOnKill <= 0f) return null
        if (cappedPassives.hpOnKill > 0f) {
            sim.hp = (sim.hp + cappedPassives.hpOnKill).coerceAtMost(sim.effectiveMaxHp())
        }
        if (cappedPassives.epOnKill > 0f) {
            sim.ep = (sim.ep + cappedPassives.epOnKill).coerceAtMost(sim.maxEp)
        }
        val parts = mutableListOf<String>()
        if (cappedPassives.hpOnKill > 0f) parts.add("+${cappedPassives.hpOnKill.toInt()} HP")
        if (cappedPassives.epOnKill > 0f) parts.add("+${cappedPassives.epOnKill.toInt()} EP")
        return "Pet Passive: ${parts.joinToString(" & ")}"
    }

    fun executeActivePetAbility(
        spec: PetSpec,
        evolutionLevel: Int,
        sim: WorldSimulationState,
        onCoinBonus: ((Long) -> Unit)? = null
    ) {
        val scaled = spec.activeScaledValue(evolutionLevel)
        val effMaxHp = sim.effectiveMaxHp()
        val effMaxShield = sim.effectiveMaxShield()
        val colorInt = spec.accentColorHex.toInt()
        sim.shockwaves.add(ShockwaveEffect(sim.petX, sim.petY + 0.5f, sim.petZ, colorInt, 1.2f, 0.75f))

        when (spec.activeType) {
            PetActiveType.HOT_HP -> {
                hotHpPerSec = scaled / 5f
                hotRemainingSec = 5f
                sim.hp = (sim.hp + scaled * 0.25f).coerceAtMost(effMaxHp)
            }
            PetActiveType.ENEMY_RADAR_PULSE -> {
                radarPulseTimerSec = scaled.coerceAtLeast(8f)
                lootScanTimerSec = 6f
                sim.hp = (sim.hp + 15f).coerceAtMost(effMaxHp)
            }
            PetActiveType.EP_SURGE -> {
                sim.ep = (sim.ep + scaled).coerceAtMost(sim.maxEp)
                radarPulseTimerSec = maxOf(radarPulseTimerSec, 5f)
            }
            PetActiveType.SPEED_BOOST -> {
                speedBuffMultiplier = 1.26f
                speedBuffTimerSec = scaled.coerceAtLeast(5f)
                sim.sprinting = true
            }
            PetActiveType.ATTACK_BOOST -> {
                attackBuffBonusPct = 0.18f * (1f + (evolutionLevel - 1) * 0.15f)
                attackBuffTimerSec = scaled.coerceAtLeast(6f)
            }
            PetActiveType.SPEED_AND_ATTACK -> {
                speedBuffMultiplier = 1.22f
                speedBuffTimerSec = scaled.coerceAtLeast(5f)
                attackBuffBonusPct = 0.16f * (1f + (evolutionLevel - 1) * 0.15f)
                attackBuffTimerSec = scaled.coerceAtLeast(5f)
            }
            PetActiveType.TEMP_SHIELD -> {
                sim.shield = (sim.shield + scaled).coerceAtMost(effMaxShield)
            }
            PetActiveType.QUICK_DASH -> {
                val rad = Math3D.degToRad(sim.cameraYaw)
                val (nx, nz) = PhysicsWorld.resolveCapsuleWorld(
                    x = sim.playerX + sin(rad) * scaled,
                    y = sim.playerY,
                    z = sim.playerZ + cos(rad) * scaled,
                    radius = 0.7f,
                    insideHouse = sim.insideHouse,
                    parkedVehicles = sim.parkedVehicles,
                    traffic = sim.traffic
                )
                sim.playerX = nx
                sim.playerZ = nz
                sim.playerY = if (sim.insideHouse != null) 0f else WorldMapData.terrainHeightAt(nx, nz)
                speedBuffMultiplier = 1.20f
                speedBuffTimerSec = 3.5f
            }
            PetActiveType.DAMAGE_PROTECTION -> {
                protectionReductionPct = (0.35f + (evolutionLevel - 1) * 0.06f).coerceAtMost(0.50f)
                protectionTimerSec = scaled.coerceAtLeast(6f)
                sim.shield = (sim.shield + 35f).coerceAtMost(effMaxShield)
            }
            PetActiveType.HP_AND_EP_RECOVER -> {
                sim.hp = (sim.hp + scaled).coerceAtMost(effMaxHp)
                sim.ep = (sim.ep + scaled * 0.85f).coerceAtMost(sim.maxEp)
            }
            PetActiveType.RELOAD_AND_FOCUS -> {
                sim.triggerInstantMagTopUp()
                radarPulseTimerSec = scaled.coerceAtLeast(6f)
                camouflageTimerSec = 5f
            }
            PetActiveType.CAMOUFLAGE_CLOAK -> {
                camouflageTimerSec = scaled.coerceAtLeast(6f)
                speedBuffMultiplier = 1.18f
                speedBuffTimerSec = 5f
            }
            PetActiveType.AOE_BLAST -> {
                sim.applyPetAreaBlast(sim.playerX, sim.playerZ, radius = 15f, damage = scaled)
                attackBuffBonusPct = 0.12f
                attackBuffTimerSec = 5f
            }
            PetActiveType.TEAM_HEAL_SHIELD -> {
                sim.hp = (sim.hp + scaled).coerceAtMost(effMaxHp)
                sim.shield = (sim.shield + scaled * 0.85f).coerceAtMost(effMaxShield)
                sim.ep = (sim.ep + 25f).coerceAtMost(sim.maxEp)
            }
            PetActiveType.LOOT_SCAN_AND_COIN -> {
                lootScanTimerSec = 12f
                sim.hp = (sim.hp + 25f).coerceAtMost(effMaxHp)
                onCoinBonus?.invoke(scaled.toLong().coerceAtLeast(25L))
            }
            PetActiveType.REBIRTH_FLAME -> {
                sim.hp = (sim.hp + scaled).coerceAtMost(effMaxHp)
                sim.applyPetAreaBlast(sim.playerX, sim.playerZ, radius = 13f, damage = scaled)
            }
        }
    }

    companion object {
        fun createInitialWildPets(): MutableList<WildWorldPet> =
            PetCatalog.wildWorldSeeds.map { seed ->
                WildWorldPet(
                    wildId = seed.wildId,
                    petId = seed.petId,
                    x = seed.x,
                    y = WorldMapData.terrainHeightAt(seed.x, seed.z),
                    z = seed.z,
                    baseX = seed.x,
                    baseZ = seed.z,
                    rotY = abs(seed.x * 13f) % 360f,
                    evolutionLevel = seed.evolutionLevel,
                    temperament = seed.temperament
                )
            }.toMutableList()
    }
}
