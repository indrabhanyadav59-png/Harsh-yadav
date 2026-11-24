package com.example.game.engine

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sqrt

object PhysicsWorld {

    private const val HOUSE_HALF_X = 8.6f
    private const val HOUSE_HALF_Z = 7.6f

    private val interiorObstacles = listOf(
        Aabb2D(-1.7f, 1.7f, -2.2f, 0.2f, 0.9f, "Table"),
        Aabb2D(-6.9f, -3.1f, -3.9f, -2.1f, 1.1f, "Sofa"),
        Aabb2D(4.3f, 6.7f, -5.3f, -3.7f, 1.3f, "Cabinet")
    )

    fun resolveCapsuleWorld(
        x: Float,
        y: Float,
        z: Float,
        radius: Float,
        insideHouse: WorldBuilding?,
        parkedVehicles: List<ParkedWorldVehicle> = emptyList(),
        traffic: List<TrafficVehicleEntity> = emptyList(),
        ignoreVehicleId: String? = null
    ): Pair<Float, Float> {
        if (insideHouse != null) {
            var nx = x.coerceIn(-HOUSE_HALF_X + radius, HOUSE_HALF_X - radius)
            var nz = z.coerceIn(-HOUSE_HALF_Z + radius, HOUSE_HALF_Z - radius)
            for (box in interiorObstacles) {
                if (box.containsCircle(nx, nz, radius)) {
                    val dxLeft = nx - (box.minX - radius)
                    val dxRight = (box.maxX + radius) - nx
                    val dzTop = nz - (box.minZ - radius)
                    val dzBot = (box.maxZ + radius) - nz
                    when (minOf(dxLeft, dxRight, dzTop, dzBot)) {
                        dxLeft -> nx = box.minX - radius
                        dxRight -> nx = box.maxX + radius
                        dzTop -> nz = box.minZ - radius
                        else -> nz = box.maxZ + radius
                    }
                }
            }
            return nx to nz
        }

        var (nx, nz) = WorldMapData.resolveCollision(x, z, radius)

        // Push out of parked vehicles (unless currently driving that vehicle)
        val vehRadius = radius + 1.45f
        for (pv in parkedVehicles) {
            if (pv.id == ignoreVehicleId) continue
            val dx = nx - pv.x
            val dz = nz - pv.z
            val dSq = dx * dx + dz * dz
            if (dSq < vehRadius * vehRadius) {
                val dist = sqrt(dSq).coerceAtLeast(0.01f)
                nx = pv.x + (dx / dist) * vehRadius
                nz = pv.z + (dz / dist) * vehRadius
            }
        }

        // Push out of moving traffic vehicles
        for (tv in traffic) {
            val dx = nx - tv.x
            val dz = nz - tv.z
            val dSq = dx * dx + dz * dz
            if (dSq < vehRadius * vehRadius) {
                val dist = sqrt(dSq).coerceAtLeast(0.01f)
                nx = tv.x + (dx / dist) * vehRadius
                nz = tv.z + (dz / dist) * vehRadius
            }
        }

        return nx.coerceIn(-WorldMapData.WORLD_BOUND, WorldMapData.WORLD_BOUND) to
            nz.coerceIn(-WorldMapData.WORLD_BOUND, WorldMapData.WORLD_BOUND)
    }

    fun applySlopeConstraint(
        prevX: Float,
        prevZ: Float,
        nextX: Float,
        nextZ: Float,
        isGrounded: Boolean
    ): Pair<Float, Float> {
        if (!isGrounded) return nextX to nextZ
        val horizDist = hypot(nextX - prevX, nextZ - prevZ)
        if (horizDist < 0.01f) return nextX to nextZ
        val prevY = WorldMapData.terrainHeightAt(prevX, prevZ)
        val nextY = WorldMapData.terrainHeightAt(nextX, nextZ)
        val climb = nextY - prevY
        return if (climb > horizDist * 1.45f && climb > 0.6f) {
            prevX to prevZ
        } else {
            nextX to nextZ
        }
    }

    fun hasLineOfSight(
        x1: Float,
        y1: Float,
        z1: Float,
        x2: Float,
        y2: Float,
        z2: Float,
        insideHouse: WorldBuilding?
    ): Boolean {
        val dist = hypot(x2 - x1, z2 - z1)
        if (dist < 0.25f) return true
        val steps = (dist / 1.4f).toInt().coerceIn(4, 28)

        if (insideHouse != null) {
            for (s in 1 until steps) {
                val t = s.toFloat() / steps
                val px = x1 + (x2 - x1) * t
                val pz = z1 + (z2 - z1) * t
                if (abs(px) >= HOUSE_HALF_X || abs(pz) >= HOUSE_HALF_Z) return false
            }
            return true
        }

        val nearby = ArrayList<Aabb2D>(16)
        for (s in 1 until steps) {
            val t = s.toFloat() / steps
            val px = x1 + (x2 - x1) * t
            val py = y1 + (y2 - y1) * t
            val pz = z1 + (z2 - z1) * t
            nearby.clear()
            WorldMapData.chunkManager.collectNearbyAabbs(px, pz, 0.25f, nearby)
            for (i in 0 until nearby.size) {
                val box = nearby[i]
                if (box.containsCircle(px, pz, 0.15f)) {
                    val ground = WorldMapData.terrainHeightAt((box.minX + box.maxX) * 0.5f, (box.minZ + box.maxZ) * 0.5f)
                    if (py <= ground + box.height + 0.15f) {
                        return false
                    }
                }
            }
        }
        return true
    }

    fun resolveCameraPosition(
        pivotX: Float,
        pivotY: Float,
        pivotZ: Float,
        desiredX: Float,
        desiredY: Float,
        desiredZ: Float,
        insideHouse: WorldBuilding?
    ): Vec3 {
        if (insideHouse != null) {
            return Vec3(
                x = desiredX.coerceIn(-HOUSE_HALF_X + 0.4f, HOUSE_HALF_X - 0.4f),
                y = desiredY.coerceIn(0.8f, 4.5f),
                z = desiredZ.coerceIn(-HOUSE_HALF_Z + 0.4f, HOUSE_HALF_Z - 0.4f)
            )
        }

        val steps = 14
        var safeX = desiredX
        var safeY = desiredY
        var safeZ = desiredZ
        val nearby = ArrayList<Aabb2D>(16)

        for (i in 1..steps) {
            val t = i.toFloat() / steps
            val cx = pivotX + (desiredX - pivotX) * t
            val cy = pivotY + (desiredY - pivotY) * t
            val cz = pivotZ + (desiredZ - pivotZ) * t

            var blocked = false
            nearby.clear()
            WorldMapData.chunkManager.collectNearbyAabbs(cx, cz, 0.35f, nearby)
            for (j in 0 until nearby.size) {
                val box = nearby[j]
                if (box.containsCircle(cx, cz, 0.3f)) {
                    val ground = WorldMapData.terrainHeightAt((box.minX + box.maxX) * 0.5f, (box.minZ + box.maxZ) * 0.5f)
                    if (cy <= ground + box.height + 0.4f) {
                        blocked = true
                        break
                    }
                }
            }
            if (blocked) {
                val prevT = ((i - 1).coerceAtLeast(1).toFloat() / steps)
                safeX = pivotX + (desiredX - pivotX) * prevT
                safeY = pivotY + (desiredY - pivotY) * prevT
                safeZ = pivotZ + (desiredZ - pivotZ) * prevT
                break
            }
        }

        val minCamY = WorldMapData.terrainHeightAt(safeX, safeZ) + 0.65f
        return Vec3(safeX, safeY.coerceAtLeast(minCamY), safeZ)
    }
}
