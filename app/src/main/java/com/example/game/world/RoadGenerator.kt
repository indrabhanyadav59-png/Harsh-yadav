package com.example.game.world

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

enum class RoadSurfaceType {
    ASPHALT_AVENUE,
    VILLAGE_STONE,
    MOUNTAIN_SWITCHBACK,
    BRIDGE_DECK,
    RUNWAY,
    PARKING_BAY
}

data class RoadSegment(
    val id: String,
    val startX: Float,
    val startZ: Float,
    val endX: Float,
    val endZ: Float,
    val width: Float,
    val surface: RoadSurfaceType = RoadSurfaceType.ASPHALT_AVENUE,
    val hasSidewalks: Boolean = true,
    val hasLaneMarkings: Boolean = true,
    val isBridge: Boolean = false
) {
    val midX: Float get() = (startX + endX) * 0.5f
    val midZ: Float get() = (startZ + endZ) * 0.5f
    val length: Float get() = hypot(endX - startX, endZ - startZ)
    val yawDeg: Float get() = Math.toDegrees(atan2((endX - startX).toDouble(), (endZ - startZ).toDouble())).toFloat()
}

data class RoadIntersection(
    val id: String,
    val x: Float,
    val z: Float,
    val size: Float,
    val hasTrafficSignals: Boolean = true,
    val hasCrosswalks: Boolean = true
)

data class StreetLightProp(
    val x: Float,
    val z: Float,
    val rotY: Float
)

/**
 * Generates a continuous, interconnected 2000 x 2000 road & bridge network linking all 8 districts
 * and establishing the Indian Mega City roundabout, shopping district, and residential blocks.
 */
object RoadGenerator {

    fun buildRoadNetwork(): Triple<List<RoadSegment>, List<RoadIntersection>, List<StreetLightProp>> {
        val segments = ArrayList<RoadSegment>(96)
        val intersections = ArrayList<RoadIntersection>(28)
        val lights = ArrayList<StreetLightProp>(220)

        fun addRoad(
            id: String,
            x1: Float,
            z1: Float,
            x2: Float,
            z2: Float,
            width: Float = 20f,
            surface: RoadSurfaceType = RoadSurfaceType.ASPHALT_AVENUE,
            sidewalks: Boolean = true,
            lanes: Boolean = true,
            bridge: Boolean = false
        ) {
            segments += RoadSegment(id, x1, z1, x2, z2, width, surface, sidewalks, lanes, bridge)
        }

        // --- 1. Indian Mega City Central Circular Roundabout (r = 45u centerline, width 18u) ---
        val rbRadius = CityLandmarksAndModules.ROUNDABOUT_CENTER_RADIUS
        val rbSegments = 24
        for (i in 0 until rbSegments) {
            val a1 = (i.toDouble() / rbSegments) * 2.0 * PI
            val a2 = ((i + 1).toDouble() / rbSegments) * 2.0 * PI
            addRoad(
                id = "rd_roundabout_$i",
                x1 = (cos(a1) * rbRadius).toFloat(),
                z1 = (sin(a1) * rbRadius).toFloat(),
                x2 = (cos(a2) * rbRadius).toFloat(),
                z2 = (sin(a2) * rbRadius).toFloat(),
                width = 18f,
                surface = RoadSurfaceType.ASPHALT_AVENUE,
                sidewalks = true,
                lanes = true
            )
        }

        // Radial Cardinal Avenues outward from Roundabout (stops cleanly outside pool at r = 45u)
        addRoad("rd_ns_spine_n", 0f, -960f, 0f, -180f, 22f)
        addRoad("rd_ns_rad_n", 0f, -45f, 0f, -180f, 20f)
        addRoad("rd_ns_rad_s", 0f, 45f, 0f, 180f, 20f)
        addRoad("rd_ns_spine_s", 0f, 180f, 0f, 660f, 22f)

        addRoad("rd_ew_main_w", -980f, 0f, -180f, 0f, 22f)
        addRoad("rd_ew_rad_w", -45f, 0f, -180f, 0f, 20f)
        addRoad("rd_ew_rad_e", 45f, 0f, 180f, 0f, 20f)
        addRoad("rd_ew_main_e", 180f, 0f, 1020f, 0f, 22f)

        // Central City Ring Boulevards (±160) & Mid-Ring Retail/Residential Connectors
        addRoad("rd_city_ring_n", -180f, -160f, 180f, -160f, 18f)
        addRoad("rd_city_ring_s", -180f, 160f, 180f, 160f, 18f)
        addRoad("rd_city_ring_w", -160f, -180f, -160f, 180f, 18f)
        addRoad("rd_city_ring_e", 160f, -180f, 160f, 180f, 18f)

        // Inner Mixed Avenue & Residential Narrow Lanes (establish city block structural density)
        addRoad("rd_inner_res_lane_n", -160f, -85f, -72f, -85f, 10f)
        addRoad("rd_inner_res_lane_s", -160f, 85f, -72f, 85f, 10f)
        addRoad("rd_inner_res_cross_w", -115f, -160f, -115f, 160f, 10f)
        addRoad("rd_inner_shop_lane_e", 160f, 65f, 320f, 65f, 12f)
        addRoad("rd_inner_shop_lane_n", 160f, -65f, 320f, -65f, 12f)
        addRoad("rd_city_parking_plaza", 88f, -18f, 88f, 18f, 12f, RoadSurfaceType.PARKING_BAY, sidewalks = false, lanes = false)

        // --- 2. Hidden Village Roads (NW) ---
        addRoad("rd_hv_access", -160f, -160f, -260f, -340f, 16f, RoadSurfaceType.VILLAGE_STONE, sidewalks = false, lanes = false)
        addRoad("rd_hv_main", -260f, -340f, -490f, -340f, 16f, RoadSurfaceType.VILLAGE_STONE, sidewalks = false, lanes = false)
        addRoad("rd_hv_academy", -380f, -340f, -380f, -520f, 14f, RoadSurfaceType.VILLAGE_STONE, sidewalks = false, lanes = false)
        addRoad("rd_hv_north_link", -380f, -520f, 0f, -580f, 16f, RoadSurfaceType.ASPHALT_AVENUE)

        // --- 3. Mountain Winding Switchback Roads (North) ---
        addRoad("rd_mtn_climb_1", 0f, -640f, -140f, -730f, 15f, RoadSurfaceType.MOUNTAIN_SWITCHBACK, sidewalks = false)
        addRoad("rd_mtn_climb_2", -140f, -730f, 130f, -810f, 15f, RoadSurfaceType.MOUNTAIN_SWITCHBACK, sidewalks = false)
        addRoad("rd_mtn_climb_3", 130f, -810f, 240f, -840f, 15f, RoadSurfaceType.MOUNTAIN_SWITCHBACK, sidewalks = false)
        addRoad("rd_mtn_overlook", -140f, -730f, 0f, -720f, 16f, RoadSurfaceType.MOUNTAIN_SWITCHBACK, sidewalks = false)

        // --- 4. Forest Region Roads & River Bridge (NE) ---
        addRoad("rd_forest_entry", 160f, -160f, 480f, -340f, 16f)
        addRoad("rd_forest_bridge", 480f, -340f, 640f, -340f, 16f, RoadSurfaceType.BRIDGE_DECK, sidewalks = true, lanes = true, bridge = true)
        addRoad("rd_forest_trail", 640f, -340f, 780f, -460f, 14f, RoadSurfaceType.VILLAGE_STONE, sidewalks = false, lanes = false)
        addRoad("rd_forest_south", 480f, -340f, 480f, -140f, 16f)

        // --- 5. Commercial Shopping District Grid (East X [260..760], Z [-140..360]) ---
        addRoad("rd_comm_ave_1", 320f, -130f, 320f, 340f, 20f)
        addRoad("rd_comm_ave_mid", 455f, -130f, 455f, 340f, 16f)
        addRoad("rd_comm_ave_2", 590f, -130f, 590f, 340f, 20f)
        addRoad("rd_comm_ave_3", 735f, -130f, 735f, 250f, 16f)
        addRoad("rd_comm_cross_n", 320f, -80f, 740f, -80f, 18f)
        addRoad("rd_comm_cross_m", 320f, 135f, 740f, 135f, 18f)
        addRoad("rd_comm_cross_s", 320f, 340f, 620f, 340f, 18f)
        addRoad("rd_comm_to_city_s", 160f, 160f, 320f, 135f, 18f)
        addRoad("rd_comm_parking_bay", 455f, 110f, 515f, 110f, 14f, RoadSurfaceType.PARKING_BAY, sidewalks = false, lanes = false)

        // --- 6. Residential Blocks & Side Lanes (SW X [-680..-240], Z [140..520]) ---
        addRoad("rd_res_boulevard", -160f, 160f, -640f, 260f, 18f)
        addRoad("rd_res_loop_w", -640f, 260f, -640f, 460f, 16f)
        addRoad("rd_res_loop_s", -640f, 460f, -260f, 460f, 16f)
        addRoad("rd_res_loop_e", -260f, 460f, -260f, 160f, 16f)
        addRoad("rd_res_cross", -640f, 360f, -260f, 360f, 15f)
        addRoad("rd_res_lane_central", -485f, 260f, -485f, 460f, 11f)
        // Villa Driveways
        addRoad("rd_res_drive_1", -420f, 360f, -420f, 322f, 8f, RoadSurfaceType.PARKING_BAY, sidewalks = false, lanes = false)
        addRoad("rd_res_drive_2", -420f, 360f, -420f, 418f, 8f, RoadSurfaceType.PARKING_BAY, sidewalks = false, lanes = false)

        // --- 7. Airport Access & Runway (West) ---
        addRoad("rd_air_access", -580f, 0f, -520f, -180f, 18f)
        addRoad("rd_air_apron", -520f, -180f, -680f, -340f, 20f, RoadSurfaceType.PARKING_BAY, sidewalks = false, lanes = false)
        addRoad("rd_air_runway", -880f, -240f, -480f, -240f, 28f, RoadSurfaceType.RUNWAY, sidewalks = false, lanes = true)

        // --- 8. Coastal Highway, Outer Plot Lanes & Ocean Island Bridges ---
        addRoad("rd_coast_hwy_w", -1040f, 560f, 0f, 560f, 20f)
        addRoad("rd_coast_hwy_e", 0f, 560f, 1040f, 560f, 20f)
        addRoad("rd_coast_res_link", -460f, 460f, -460f, 560f, 18f)
        addRoad("rd_res_outer_plot_lane", -640f, 515f, -260f, 515f, 12f)
        addRoad("rd_east_plot_lane", 160f, -260f, 360f, -260f, 14f)
        addRoad("rd_forest_villa_lane", 360f, -240f, 480f, -240f, 12f)
        addRoad("rd_coast_comm_link", 460f, 340f, 460f, 560f, 18f)
        addRoad("rd_harbor_pier", 0f, 560f, 0f, 710f, 16f, RoadSurfaceType.BRIDGE_DECK, sidewalks = true, lanes = false, bridge = true)
        addRoad("rd_bridge_lighthouse", -60f, 640f, -180f, 830f, 15f, RoadSurfaceType.BRIDGE_DECK, sidewalks = true, lanes = true, bridge = true)
        addRoad("rd_bridge_pearl", 60f, 640f, 260f, 825f, 15f, RoadSurfaceType.BRIDGE_DECK, sidewalks = true, lanes = true, bridge = true)

        // Major Intersections with Crosswalks & Traffic Signals
        val signalPoints = listOf(
            "int_rb_n" to (0f to -45f),
            "int_rb_s" to (0f to 45f),
            "int_rb_w" to (-45f to 0f),
            "int_rb_e" to (45f to 0f),
            "int_city_n" to (0f to -160f),
            "int_city_s" to (0f to 160f),
            "int_city_w" to (-160f to 0f),
            "int_city_e" to (160f to 0f),
            "int_city_nw" to (-160f to -160f),
            "int_city_ne" to (160f to -160f),
            "int_city_sw" to (-160f to 160f),
            "int_city_se" to (160f to 160f),
            "int_comm_1" to (320f to 0f),
            "int_comm_mid" to (455f to 0f),
            "int_comm_2" to (590f to 0f),
            "int_comm_s1" to (320f to 135f),
            "int_comm_smid" to (455f to 135f),
            "int_comm_s2" to (590f to 135f),
            "int_res_center" to (-485f to 360f),
            "int_coast_c" to (0f to 560f),
            "int_coast_w" to (-460f to 560f),
            "int_coast_e" to (460f to 560f),
            "int_air_entry" to (-580f to 0f)
        )
        for ((id, pt) in signalPoints) {
            intersections += RoadIntersection(id, pt.first, pt.second, size = 24f)
        }

        // Street lights along main avenues & coastal highway
        for (seg in segments) {
            if (!seg.hasSidewalks || seg.length < 40f) continue
            val steps = (seg.length / 72f).toInt().coerceIn(1, 10)
            val dx = (seg.endX - seg.startX) / seg.length
            val dz = (seg.endZ - seg.startZ) / seg.length
            val perpX = -dz
            val perpZ = dx
            val offset = seg.width * 0.5f + 2.2f
            for (s in 1 until steps) {
                val t = s.toFloat() / steps
                val px = seg.startX + (seg.endX - seg.startX) * t
                val pz = seg.startZ + (seg.endZ - seg.startZ) * t
                val side = if (s % 2 == 0) 1f else -1f
                lights += StreetLightProp(
                    x = px + perpX * offset * side,
                    z = pz + perpZ * offset * side,
                    rotY = seg.yawDeg + if (side > 0f) 90f else -90f
                )
            }
        }

        return Triple(segments, intersections, lights)
    }

    /**
     * Checks if a point (x, z) lies inside or near any road corridor (used to prevent trees/buildings overlapping roads).
     */
    fun isPointNearRoad(segments: List<RoadSegment>, x: Float, z: Float, clearance: Float): Boolean {
        for (seg in segments) {
            val vx = seg.endX - seg.startX
            val vz = seg.endZ - seg.startZ
            val lenSq = vx * vx + vz * vz
            if (lenSq < 1f) continue
            val t = (((x - seg.startX) * vx + (z - seg.startZ) * vz) / lenSq).coerceIn(0f, 1f)
            val projX = seg.startX + vx * t
            val projZ = seg.startZ + vz * t
            val dist = hypot(x - projX, z - projZ)
            if (dist < seg.width * 0.5f + clearance) return true
        }
        return false
    }

    /**
     * Returns bridge elevation if the point (x, z) is on an elevated bridge deck over water.
     */
    fun getBridgeDeckHeightOrNull(segments: List<RoadSegment>, x: Float, z: Float): Float? {
        for (seg in segments) {
            if (!seg.isBridge) continue
            val vx = seg.endX - seg.startX
            val vz = seg.endZ - seg.startZ
            val lenSq = vx * vx + vz * vz
            if (lenSq < 1f) continue
            val t = (((x - seg.startX) * vx + (z - seg.startZ) * vz) / lenSq).coerceIn(0f, 1f)
            val projX = seg.startX + vx * t
            val projZ = seg.startZ + vz * t
            if (hypot(x - projX, z - projZ) <= seg.width * 0.52f) {
                return if (abs(seg.midZ) > 600f) 1.6f else 1.2f
            }
        }
        return null
    }
}
