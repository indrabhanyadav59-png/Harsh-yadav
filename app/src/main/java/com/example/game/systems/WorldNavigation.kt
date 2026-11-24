package com.example.game.systems

import com.example.game.engine.Vec3
import com.example.game.engine.WorldMapData
import com.example.game.world.RoadGenerator
import com.example.game.world.WorldGenerator
import com.example.game.world.WorldLocations
import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.math.hypot

/**
 * 2D Grid A* Navigation & Obstacle-Avoidance system covering the 2000 x 2000 world.
 * Used by both Player GPS Route guidance and AI NPC pathfinding.
 */
object WorldNavigation {

    private const val GRID_SIZE = 50
    private const val CELL_SIZE = 40f // 50 * 40 = 2000 units [-1000..+1000]
    private const val ORIGIN = -1000f

    // Lazy precomputed walkability grid: 0 = walkable, 1 = blocked (building / deep water)
    private val blockedGrid: BooleanArray by lazy {
        val grid = BooleanArray(GRID_SIZE * GRID_SIZE)
        for (gz in 0 until GRID_SIZE) {
            for (gx in 0 until GRID_SIZE) {
                val wx = ORIGIN + (gx + 0.5f) * CELL_SIZE
                val wz = ORIGIN + (gz + 0.5f) * CELL_SIZE
                val nearRoad = RoadGenerator.isPointNearRoad(WorldGenerator.roads, wx, wz, 8f)
                if (nearRoad) {
                    grid[gz * GRID_SIZE + gx] = false
                    continue
                }
                val inWater = WorldGenerator.isDeepWater(wx, wz)
                val inBuilding = WorldGenerator.buildings.any { b ->
                    abs(wx - b.x) < (b.width * 0.5f + 4f) &&
                        abs(wz - b.z) < (b.depth * 0.5f + 4f)
                }
                grid[gz * GRID_SIZE + gx] = inWater || inBuilding
            }
        }
        grid
    }

    private fun toCell(wx: Float, wz: Float): Pair<Int, Int> {
        val gx = ((wx - ORIGIN) / CELL_SIZE).toInt().coerceIn(0, GRID_SIZE - 1)
        val gz = ((wz - ORIGIN) / CELL_SIZE).toInt().coerceIn(0, GRID_SIZE - 1)
        return gx to gz
    }

    private fun toWorld(gx: Int, gz: Int): Vec3 {
        val wx = ORIGIN + (gx + 0.5f) * CELL_SIZE
        val wz = ORIGIN + (gz + 0.5f) * CELL_SIZE
        return Vec3(wx, WorldGenerator.terrainHeightAt(wx, wz), wz)
    }

    fun isWalkableCell(gx: Int, gz: Int): Boolean {
        if (gx !in 0 until GRID_SIZE || gz !in 0 until GRID_SIZE) return false
        return !blockedGrid[gz * GRID_SIZE + gx]
    }

    private fun nearestWalkableCell(startGx: Int, startGz: Int): Pair<Int, Int> {
        if (isWalkableCell(startGx, startGz)) return startGx to startGz
        for (r in 1..6) {
            for (dz in -r..r) {
                for (dx in -r..r) {
                    val nx = startGx + dx
                    val nz = startGz + dz
                    if (isWalkableCell(nx, nz)) return nx to nz
                }
            }
        }
        return startGx to startGz
    }

    /**
     * Computes an obstacle-free A* path from (startX, startZ) to (goalX, goalZ).
     */
    fun findPath(startX: Float, startZ: Float, goalX: Float, goalZ: Float): List<Vec3> {
        val sx0 = startX.coerceIn(WorldLocations.WORLD_MIN + 1f, WorldLocations.WORLD_MAX - 1f)
        val sz0 = startZ.coerceIn(WorldLocations.WORLD_MIN + 1f, WorldLocations.WORLD_MAX - 1f)
        val gx0 = goalX.coerceIn(WorldLocations.WORLD_MIN + 1f, WorldLocations.WORLD_MAX - 1f)
        val gz0 = goalZ.coerceIn(WorldLocations.WORLD_MIN + 1f, WorldLocations.WORLD_MAX - 1f)

        val (rawSx, rawSz) = toCell(sx0, sz0)
        val (rawGx, rawGz) = toCell(gx0, gz0)
        val (sx, sz) = nearestWalkableCell(rawSx, rawSz)
        val (tx, tz) = nearestWalkableCell(rawGx, rawGz)

        if (sx == tx && sz == tz) {
            return listOf(
                Vec3(sx0, WorldMapData.terrainHeightAt(sx0, sz0), sz0),
                Vec3(gx0, WorldMapData.terrainHeightAt(gx0, gz0), gz0)
            )
        }

        val total = GRID_SIZE * GRID_SIZE
        val gScore = FloatArray(total) { Float.POSITIVE_INFINITY }
        val cameFrom = IntArray(total) { -1 }
        val closed = BooleanArray(total)

        data class Node(val idx: Int, val f: Float)
        val pq = PriorityQueue<Node>(compareBy { it.f })

        val startIdx = sz * GRID_SIZE + sx
        val goalIdx = tz * GRID_SIZE + tx
        gScore[startIdx] = 0f
        pq.add(Node(startIdx, hypot((tx - sx).toFloat(), (tz - sz).toFloat())))

        val dirs = arrayOf(
            intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1),
            intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(-1, -1)
        )

        var found = false
        while (pq.isNotEmpty()) {
            val cur = pq.poll() ?: break
            if (cur.idx == goalIdx) {
                found = true
                break
            }
            if (closed[cur.idx]) continue
            closed[cur.idx] = true

            val cx = cur.idx % GRID_SIZE
            val cz = cur.idx / GRID_SIZE

            for (d in dirs) {
                val nx = cx + d[0]
                val nz = cz + d[1]
                if (!isWalkableCell(nx, nz)) continue
                val nIdx = nz * GRID_SIZE + nx
                if (closed[nIdx]) continue

                val stepCost = if (d[0] != 0 && d[1] != 0) 1.414f else 1.0f
                val tentativeG = gScore[cur.idx] + stepCost
                if (tentativeG < gScore[nIdx]) {
                    cameFrom[nIdx] = cur.idx
                    gScore[nIdx] = tentativeG
                    val h = hypot((tx - nx).toFloat(), (tz - nz).toFloat())
                    pq.add(Node(nIdx, tentativeG + h))
                }
            }
        }

        if (!found) {
            return listOf(
                Vec3(sx0, WorldMapData.terrainHeightAt(sx0, sz0), sz0),
                Vec3(gx0, WorldMapData.terrainHeightAt(gx0, gz0), gz0)
            )
        }

        val reversed = ArrayList<Vec3>()
        var trace = goalIdx
        while (trace != -1) {
            val cx = trace % GRID_SIZE
            val cz = trace / GRID_SIZE
            reversed.add(toWorld(cx, cz))
            trace = cameFrom[trace]
        }
        reversed.reverse()
        if (reversed.isNotEmpty()) {
            reversed[0] = Vec3(sx0, WorldMapData.terrainHeightAt(sx0, sz0), sz0)
            reversed[reversed.lastIndex] = Vec3(gx0, WorldMapData.terrainHeightAt(gx0, gz0), gz0)
        }
        return reversed
    }
}
