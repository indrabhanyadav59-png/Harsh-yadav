package com.example.game.engine

import com.example.data.model.NpcAppearanceGenerator
import com.example.data.model.NpcAppearanceSpec
import com.example.game.systems.WorldNavigation
import com.example.game.world.NpcSpawnPoints
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.random.Random

enum class NpcRole(val label: String) {
    CIVILIAN("Civilian"),
    POLICE("Metro Police"),
    SHOPKEEPER("Shopkeeper"),
    ACADEMY_STUDENT("Academy Student"),
    MISSION_NPC("Mission Handler"),
    RIVAL_BOT("Rogue Shinobi"),
    OFFLINE_PLAYER_PROXY("Offline AI Proxy")
}

enum class NpcBehaviorState {
    PATROL,
    VISIT_SHOP,
    SOCIALIZE,
    ENGAGE,
    DEFEATED,
    RESPAWNING
}

/**
 * Manages authoritative health, collider/navAgent toggling, defeated animation timing,
 * respawn delay, position restoration, and new post-respawn AI activity assignment.
 */
class NpcRespawnController(
    val maxHealth: Float = 100f,
    val respawnDelaySec: Float = 5f
) {
    var health: Float = maxHealth
        private set
    var collidersEnabled: Boolean = true
        private set
    var navAgentEnabled: Boolean = true
        private set
    var animationTrigger: String = "Idle"
        private set
    private var respawnTimerSec: Float = 0f

    fun takeDamage(damage: Float): Boolean {
        if (health <= 0f || !collidersEnabled) return false
        health = (health - damage).coerceAtLeast(0f)
        if (health <= 0f) {
            navAgentEnabled = false
            collidersEnabled = false
            animationTrigger = "Defeated"
            respawnTimerSec = respawnDelaySec
            return true
        }
        return false
    }

    fun tickRespawn(dt: Float): Boolean {
        if (health > 0f) return false
        respawnTimerSec -= dt
        if (respawnTimerSec <= 0f) {
            health = maxHealth
            collidersEnabled = true
            navAgentEnabled = true
            animationTrigger = "Respawn"
            return true
        }
        return false
    }
}

data class NpcEntity(
    val id: String,
    val displayName: String,
    val role: NpcRole,
    val spawnX: Float,
    val spawnZ: Float,
    var x: Float = spawnX,
    var y: Float = 0f,
    var z: Float = spawnZ,
    var rotY: Float = 0f,
    var targetX: Float = spawnX,
    var targetZ: Float = spawnZ,
    var stridePhase: Float = 0f,
    var state: NpcBehaviorState = NpcBehaviorState.PATROL,
    var stateTimerSec: Float = 4f,
    val coatColor: Int,
    val hairColor: Int,
    val accentColor: Int,
    val level: Int = 24,
    val guild: String = "CITIZEN",
    val respawnController: NpcRespawnController = NpcRespawnController(
        maxHealth = if (role == NpcRole.RIVAL_BOT) 120f else 100f,
        respawnDelaySec = 5f
    ),
    var statusBubble: String = "Patrolling Sakura Plaza",
    var pathWaypoints: List<Vec3> = emptyList(),
    var pathIdx: Int = 0,
    var provokedByPlayer: Boolean = false,
    var provokedTimerSec: Float = 0f,
    val appearance: NpcAppearanceSpec = NpcAppearanceGenerator.generate(
        npcId = id,
        roleKey = role.name,
        preferredCoatColor = coatColor,
        preferredHairColor = hairColor,
        preferredAccentColor = accentColor
    )
)

data class TrafficVehicleEntity(
    val id: String,
    val modelName: String,
    val category: String,
    val colorInt: Int,
    val laneAxisZ: Float?,
    val laneAxisX: Float?,
    val minBound: Float,
    val maxBound: Float,
    var x: Float,
    var z: Float,
    var rotY: Float,
    var speed: Float,
    var wheelAngle: Float = 0f
)

object NpcAiSystem {

    fun createInitialNpcs(): MutableList<NpcEntity> = NpcSpawnPoints.createNpcs().also { list ->
        for (npc in list) {
            npc.y = WorldMapData.terrainHeightAt(npc.x, npc.z)
        }
    }

    fun createInitialTraffic(): MutableList<TrafficVehicleEntity> = NpcSpawnPoints.createTraffic()

    fun updateNpcs(
        npcs: List<NpcEntity>,
        playerX: Float,
        playerZ: Float,
        dt: Float,
        aggroRangeScale: Float = 1f
    ) {
        val waypoints = NpcSpawnPoints.roamWaypoints
        val engageRadius = (36f * aggroRangeScale).coerceIn(14f, 42f)
        for (npc in npcs) {
            if (npc.state == NpcBehaviorState.DEFEATED || npc.state == NpcBehaviorState.RESPAWNING) {
                npc.provokedByPlayer = false
                npc.provokedTimerSec = 0f
                val respawned = npc.respawnController.tickRespawn(dt)
                if (respawned) {
                    npc.x = npc.spawnX
                    npc.z = npc.spawnZ
                    npc.y = WorldMapData.terrainHeightAt(npc.x, npc.z)
                    npc.state = NpcBehaviorState.PATROL
                    val wp = waypoints.random()
                    assignPath(npc, wp.x, wp.z)
                    npc.statusBubble = when (npc.role) {
                        NpcRole.RIVAL_BOT -> "Respawned & patrolling!"
                        NpcRole.OFFLINE_PLAYER_PROXY -> "AI Proxy resumed mission"
                        else -> "Heading to ${WorldMapData.resolveDistrict(wp.x, wp.z)}"
                    }
                }
                continue
            }

            if (!npc.respawnController.navAgentEnabled) continue

            npc.stateTimerSec -= dt
            val distToPlayerSq = (playerX - npc.x) * (playerX - npc.x) + (playerZ - npc.z) * (playerZ - npc.z)

            if (npc.provokedByPlayer) {
                npc.provokedTimerSec -= dt
                if (npc.provokedTimerSec <= 0f || distToPlayerSq > engageRadius * engageRadius) {
                    npc.provokedByPlayer = false
                    npc.provokedTimerSec = 0f
                    npc.state = NpcBehaviorState.PATROL
                    npc.stateTimerSec = 0f
                }
            }

            if (npc.provokedByPlayer) {
                npc.state = NpcBehaviorState.ENGAGE
                npc.pathWaypoints = emptyList()
                if (distToPlayerSq > 2.8f * 2.8f) {
                    npc.targetX = playerX
                    npc.targetZ = playerZ
                } else {
                    npc.targetX = npc.x
                    npc.targetZ = npc.z
                }
            } else if (npc.state == NpcBehaviorState.ENGAGE || npc.stateTimerSec <= 0f) {
                // Peaceful roam/visit/socialize cycle until provoked by the player
                val localPool = waypoints.filter {
                    val dx = it.x - npc.spawnX
                    val dz = it.z - npc.spawnZ
                    dx * dx + dz * dz <= 320f * 320f
                }.ifEmpty { waypoints }
                val wp = localPool.random()
                assignPath(npc, wp.x, wp.z)
                npc.state = when (Random.nextInt(3)) {
                    0 -> NpcBehaviorState.VISIT_SHOP
                    1 -> NpcBehaviorState.SOCIALIZE
                    else -> NpcBehaviorState.PATROL
                }
                npc.stateTimerSec = 6f + Random.nextFloat() * 7f
                npc.statusBubble = when (npc.state) {
                    NpcBehaviorState.VISIT_SHOP -> "Visiting ${WorldMapData.resolveDistrict(wp.x, wp.z)}"
                    NpcBehaviorState.SOCIALIZE -> "Chatting in ${WorldMapData.resolveDistrict(npc.x, npc.z)}"
                    else -> "Roaming ${WorldMapData.resolveDistrict(wp.x, wp.z)}"
                }
            }

            if (npc.pathWaypoints.isNotEmpty() && npc.pathIdx < npc.pathWaypoints.size) {
                val curWp = npc.pathWaypoints[npc.pathIdx]
                npc.targetX = curWp.x
                npc.targetZ = curWp.z
                val dSq = (curWp.x - npc.x) * (curWp.x - npc.x) + (curWp.z - npc.z) * (curWp.z - npc.z)
                if (dSq < 3.5f * 3.5f && npc.pathIdx < npc.pathWaypoints.lastIndex) {
                    npc.pathIdx++
                }
            }

            val dx = npc.targetX - npc.x
            val dz = npc.targetZ - npc.z
            val dist = sqrt(dx * dx + dz * dz)
            if (dist > 0.9f) {
                val moveSpeed = if (npc.state == NpcBehaviorState.ENGAGE) 5.2f else 3.2f
                val step = (moveSpeed * dt).coerceAtMost(dist)
                val nx = npc.x + (dx / dist) * step
                val nz = npc.z + (dz / dist) * step
                val (resolvedX, resolvedZ) = WorldMapData.resolveCollision(nx, nz, 0.7f)
                npc.x = resolvedX
                npc.z = resolvedZ
                npc.y = WorldMapData.terrainHeightAt(resolvedX, resolvedZ).coerceAtLeast(-0.15f)
                npc.rotY = Math3D.radToDeg(atan2(dx, dz))
                npc.stridePhase += dt * moveSpeed * 2.2f
            } else {
                npc.stridePhase = 0f
            }
        }
    }

    private fun assignPath(npc: NpcEntity, goalX: Float, goalZ: Float) {
        val dx = goalX - npc.x
        val dz = goalZ - npc.z
        if (dx * dx + dz * dz < 45f * 45f) {
            npc.pathWaypoints = emptyList()
            npc.targetX = goalX
            npc.targetZ = goalZ
        } else {
            npc.pathWaypoints = WorldNavigation.findPath(npc.x, npc.z, goalX, goalZ)
            npc.pathIdx = if (npc.pathWaypoints.size > 1) 1 else 0
        }
    }

    fun updateTraffic(traffic: List<TrafficVehicleEntity>, dt: Float) {
        for (v in traffic) {
            if (v.laneAxisZ != null) {
                val dir = if (v.rotY > 0f) 1f else -1f
                v.x += dir * v.speed * dt
                if (v.x > v.maxBound) {
                    v.x = v.maxBound
                    v.rotY = -90f
                } else if (v.x < v.minBound) {
                    v.x = v.minBound
                    v.rotY = 90f
                }
            } else if (v.laneAxisX != null) {
                val dir = if (cos(Math3D.degToRad(v.rotY)) >= 0f) 1f else -1f
                v.z += dir * v.speed * dt
                if (v.z > v.maxBound) {
                    v.z = v.maxBound
                    v.rotY = 180f
                } else if (v.z < v.minBound) {
                    v.z = v.minBound
                    v.rotY = 0f
                }
            }
            v.wheelAngle = (v.wheelAngle + v.speed * dt * 35f) % 360f
        }
    }
}
