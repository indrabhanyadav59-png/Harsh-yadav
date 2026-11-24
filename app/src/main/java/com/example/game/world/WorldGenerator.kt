package com.example.game.world

import com.example.game.engine.Aabb2D
import com.example.game.engine.ParkedWorldVehicle
import com.example.game.engine.WorldBuilding
import com.example.game.engine.WorldTree
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

data class WorldChunk(
    val chunkX: Int,
    val chunkZ: Int,
    val centerX: Float,
    val centerZ: Float,
    val buildings: MutableList<WorldBuilding> = mutableListOf(),
    val aabbs: MutableList<Aabb2D> = mutableListOf(),
    val trees: MutableList<WorldTree> = mutableListOf(),
    val boulders: MutableList<BoulderProp> = mutableListOf(),
    val streetLights: MutableList<StreetLightProp> = mutableListOf(),
    val roads: MutableList<RoadSegment> = mutableListOf(),
    val billboards: MutableList<DistrictBillboard> = mutableListOf(),
    val fountains: MutableList<ParkFountainProp> = mutableListOf(),
    val gates: MutableList<ToriiMonumentProp> = mutableListOf(),
    val boats: MutableList<BoatProp> = mutableListOf(),
    val benches: MutableList<CityBenchProp> = mutableListOf(),
    val roadSigns: MutableList<CityRoadSignProp> = mutableListOf(),
    val indianArches: MutableList<IndianArchProp> = mutableListOf()
)

class ChunkManager(
    val gridCount: Int = 10,
    val chunkSize: Float = 240f,
    val originOffset: Float = -1200f
) {
    private val chunks = Array(gridCount * gridCount) { idx ->
        val cx = idx % gridCount
        val cz = idx / gridCount
        WorldChunk(
            chunkX = cx,
            chunkZ = cz,
            centerX = originOffset + (cx + 0.5f) * chunkSize,
            centerZ = originOffset + (cz + 0.5f) * chunkSize
        )
    }

    fun chunkIndexFor(x: Float, z: Float): Pair<Int, Int> {
        val cx = ((x - originOffset) / chunkSize).toInt().coerceIn(0, gridCount - 1)
        val cz = ((z - originOffset) / chunkSize).toInt().coerceIn(0, gridCount - 1)
        return cx to cz
    }

    fun getChunk(cx: Int, cz: Int): WorldChunk = chunks[cz * gridCount + cx]

    fun allChunks(): Array<WorldChunk> = chunks

    fun insertBuilding(b: WorldBuilding) {
        val aabb = b.toAabb()
        val (minCx, minCz) = chunkIndexFor(aabb.minX - 4f, aabb.minZ - 4f)
        val (maxCx, maxCz) = chunkIndexFor(aabb.maxX + 4f, aabb.maxZ + 4f)
        for (cz in minCz..maxCz) {
            for (cx in minCx..maxCx) {
                val c = getChunk(cx, cz)
                c.buildings += b
                c.aabbs += aabb
            }
        }
    }

    fun insertRoad(seg: RoadSegment) {
        val minX = minOf(seg.startX, seg.endX) - seg.width
        val maxX = maxOf(seg.startX, seg.endX) + seg.width
        val minZ = minOf(seg.startZ, seg.endZ) - seg.width
        val maxZ = maxOf(seg.startZ, seg.endZ) + seg.width
        val (minCx, minCz) = chunkIndexFor(minX, minZ)
        val (maxCx, maxCz) = chunkIndexFor(maxX, maxZ)
        for (cz in minCz..maxCz) {
            for (cx in minCx..maxCx) {
                getChunk(cx, cz).roads += seg
            }
        }
    }

    fun insertTree(t: WorldTree) {
        val (cx, cz) = chunkIndexFor(t.x, t.z)
        getChunk(cx, cz).trees += t
    }

    fun insertBoulder(b: BoulderProp) {
        val (cx, cz) = chunkIndexFor(b.x, b.z)
        getChunk(cx, cz).boulders += b
    }

    fun insertStreetLight(l: StreetLightProp) {
        val (cx, cz) = chunkIndexFor(l.x, l.z)
        getChunk(cx, cz).streetLights += l
    }

    fun insertBillboard(b: DistrictBillboard) {
        val (cx, cz) = chunkIndexFor(b.x, b.z)
        getChunk(cx, cz).billboards += b
    }

    fun insertFountain(f: ParkFountainProp) {
        val (cx, cz) = chunkIndexFor(f.x, f.z)
        getChunk(cx, cz).fountains += f
    }

    fun insertGate(g: ToriiMonumentProp) {
        val (cx, cz) = chunkIndexFor(g.x, g.z)
        getChunk(cx, cz).gates += g
    }

    fun insertBoat(b: BoatProp) {
        val (cx, cz) = chunkIndexFor(b.x, b.z)
        getChunk(cx, cz).boats += b
    }

    fun insertBench(b: CityBenchProp) {
        val (cx, cz) = chunkIndexFor(b.x, b.z)
        getChunk(cx, cz).benches += b
    }

    fun insertRoadSign(s: CityRoadSignProp) {
        val (cx, cz) = chunkIndexFor(s.x, s.z)
        getChunk(cx, cz).roadSigns += s
    }

    fun insertIndianArch(a: IndianArchProp) {
        val (cx, cz) = chunkIndexFor(a.x, a.z)
        getChunk(cx, cz).indianArches += a
    }

    /**
     * Collects nearby building AABBs within the 1..4 chunks touching the query circle.
     */
    fun collectNearbyAabbs(x: Float, z: Float, radius: Float, out: MutableList<Aabb2D>) {
        out.clear()
        val (minCx, minCz) = chunkIndexFor(x - radius - 2f, z - radius - 2f)
        val (maxCx, maxCz) = chunkIndexFor(x + radius + 2f, z + radius + 2f)
        for (cz in minCz..maxCz) {
            for (cx in minCx..maxCx) {
                out.addAll(getChunk(cx, cz).aabbs)
            }
        }
    }
}

/**
 * Master procedural generator assembling the entire 2000 x 2000 world once at startup.
 */
object WorldGenerator {

    val roads: List<RoadSegment>
    val intersections: List<RoadIntersection>
    val streetLights: List<StreetLightProp>
    val buildings: List<WorldBuilding>
    val trees: List<WorldTree>
    val billboards: List<DistrictBillboard>
    val fountains: List<ParkFountainProp>
    val gates: List<ToriiMonumentProp>
    val mountainPeaks: List<MountainPeakProp>
    val boulders: List<BoulderProp>
    val boats: List<BoatProp>
    val parkedVehicles: List<ParkedWorldVehicle>
    val benches: List<CityBenchProp>
    val roadSigns: List<CityRoadSignProp>
    val indianArches: List<IndianArchProp>
    val chunkManager = ChunkManager()

    init {
        val (roadSegs, roadInts, lights) = RoadGenerator.buildRoadNetwork()
        roads = roadSegs
        intersections = roadInts
        streetLights = lights

        val (cityB, cityT, cityProps) = CityGenerator.generate(roads)
        val (villB, villT, villGates) = VillageGenerator.generate(roads)
        val (mtnB, mtnAssets) = MountainGenerator.generate(roads)
        val (forB, forT, forRocks) = ForestGenerator.generate(roads)
        val (coastB, coastT, coastAssets) = CoastalAndAirportGenerator.generate(roads)

        buildings = cityB + villB + mtnB + forB + coastB
        trees = cityT + villT + mtnAssets.third + forT + coastT
        billboards = cityProps.first
        fountains = cityProps.second
        gates = villGates + ToriiMonumentProp(0f, -31.5f, 0f, 1.0f)
        mountainPeaks = mtnAssets.first
        boulders = mtnAssets.second + forRocks
        boats = coastAssets.first
        parkedVehicles = coastAssets.second
        benches = CityLandmarksAndModules.cityBenches
        roadSigns = CityLandmarksAndModules.cityRoadSigns
        indianArches = CityLandmarksAndModules.indianArches

        roads.forEach { chunkManager.insertRoad(it) }
        buildings.forEach { chunkManager.insertBuilding(it) }
        trees.forEach { chunkManager.insertTree(it) }
        boulders.forEach { chunkManager.insertBoulder(it) }
        streetLights.forEach { chunkManager.insertStreetLight(it) }
        billboards.forEach { chunkManager.insertBillboard(it) }
        fountains.forEach { chunkManager.insertFountain(it) }
        gates.forEach { chunkManager.insertGate(it) }
        boats.forEach { chunkManager.insertBoat(it) }
        benches.forEach { chunkManager.insertBench(it) }
        roadSigns.forEach { chunkManager.insertRoadSign(it) }
        indianArches.forEach { chunkManager.insertIndianArch(it) }
    }

    /**
     * Smooth deterministic terrain height for the 2000 x 2000 map:
     * - Flat (0f) in city, village, airport, residential, commercial basins
     * - Smooth climb (0f -> 22f) in the Northern Mountain Region (z < -600)
     * - Elevated bridge decks (+1.2f..+1.6f) over forest river & southern ocean
     * - Coastal beach slope down to shallow shoreline (-0.5f) & seabed (-2.2f)
     */
    fun terrainHeightAt(x: Float, z: Float): Float {
        RoadGenerator.getBridgeDeckHeightOrNull(roads, x, z)?.let { return it }
        CityLandmarksAndModules.centralPoolTerrainHeightOrNull(x, z)?.let { return it }

        // Southern Coastal Ocean & Islands
        if (z > 625f) {
            // Azure Lighthouse Island (-180, 850) & Pearl Island (260, 845)
            val dLighthouse = (x + 180f) * (x + 180f) + (z - 850f) * (z - 850f)
            if (dLighthouse < 65f * 65f) return 0.5f
            val dPearl = (x - 260f) * (x - 260f) + (z - 845f) * (z - 845f)
            if (dPearl < 75f * 75f) return 0.5f
            return -1.4f
        }
        if (z in 590f..625f) {
            val t = (z - 590f) / 35f
            return -0.35f * t
        }

        // Northern Mountain Region progressive elevation
        if (z < -600f && abs(x) < 760f) {
            val climb = ((-600f - z) / 340f).coerceIn(0f, 1f)
            val baseClimb = climb * 21.0f
            val subtleWave = (sin(x * 0.018f) * cos(z * 0.018f)) * 1.4f * climb
            return (baseClimb + subtleWave).coerceAtLeast(0f)
        }

        // Gentle rolling hills in outer Forest outskirts
        if (x > 640f && z in -540f..-150f && !ForestGenerator.isInRiver(x, z)) {
            return (sin(x * 0.02f) * cos(z * 0.02f) * 1.2f + 1.0f).coerceAtLeast(0f)
        }

        return 0f
    }

    fun isDeepWater(x: Float, z: Float): Boolean {
        if (RoadGenerator.getBridgeDeckHeightOrNull(roads, x, z) != null) return false
        if (CityLandmarksAndModules.isInCentralPool(x, z)) return true
        if (z > 635f) {
            val dLighthouse = (x + 180f) * (x + 180f) + (z - 850f) * (z - 850f)
            val dPearl = (x - 260f) * (x - 260f) + (z - 845f) * (z - 845f)
            return dLighthouse >= 64f * 64f && dPearl >= 74f * 74f
        }
        return ForestGenerator.isInRiver(x, z)
    }
}
