package com.example.game.engine

import android.graphics.Color
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.data.model.CoatSilhouette
import com.example.data.model.GameCatalog
import com.example.data.model.HeroAppearanceCatalog
import com.example.data.model.HeroExpression
import com.example.data.model.HeroWeaponSkinPreset
import com.example.data.model.NpcBackAccessory
import com.example.data.model.NpcHairSilhouette
import com.example.data.model.TimeOfDayMode
import com.example.data.model.WeatherMode
import com.example.data.model.WeaponType
import com.example.game.systems.LightingFrameConfig
import com.example.game.systems.WorldLighting
import com.example.game.world.CityBenchProp
import com.example.game.world.CityLandmarksAndModules
import com.example.game.world.CityRoadSignProp
import com.example.game.world.DistrictBillboard
import com.example.game.world.ForestGenerator
import com.example.game.world.IndianArchProp
import com.example.game.world.ParkFountainProp
import com.example.game.world.RoadIntersection
import com.example.game.world.RoadSegment
import com.example.game.world.RoadSurfaceType
import com.example.game.world.ToriiMonumentProp
import com.example.game.world.WorldDistrictId
import com.example.game.world.WorldGenerator
import com.example.game.world.WorldLocations
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class IndraGlRenderer(
    private val sim: WorldSimulationState,
    private val onHudTick: (WorldHudSnapshot) -> Unit,
    private val onSurfaceReady: (() -> Unit)? = null
) : GLSurfaceView.Renderer, PetPrimitiveDrawer {

    // Standard Shader Program
    private var program = 0
    private var uMvpLoc = 0
    private var uModelLoc = 0
    private var uColorLoc = 0
    private var uLightDirLoc = 0
    private var uSkyColorLoc = 0
    private var uFogColorLoc = 0
    private var uFogDistLoc = 0
    private var uEmissiveLoc = 0
    private var uTexMixLoc = 0
    private var uDetailTexLoc = 0
    private var aPosLoc = 0
    private var aNormLoc = 0

    // Instanced Shader Program (for trees, poles, lane dashes, boulders)
    private var instProgram = 0
    private var iVpLoc = 0
    private var iLightDirLoc = 0
    private var iSkyColorLoc = 0
    private var iFogColorLoc = 0
    private var iFogDistLoc = 0
    private var iDetailTexLoc = 0
    private var iPosLoc = 0
    private var iNormLoc = 0
    private var iModelLoc = 2
    private var iColorLoc = 6

    private lateinit var cubeMesh: Mesh
    private lateinit var cylMesh: Mesh
    private lateinit var coneMesh: Mesh
    private lateinit var domeMesh: Mesh
    private lateinit var sphereMesh: Mesh
    private lateinit var capsuleMesh: Mesh
    private lateinit var taperedTorsoMesh: Mesh
    private lateinit var beveledBoxMesh: Mesh

    private lateinit var trunkBatch: InstancedMeshBatch
    private lateinit var pineBatch: InstancedMeshBatch
    private lateinit var sakuraBatch: InstancedMeshBatch
    private lateinit var boxPropBatch: InstancedMeshBatch
    private val textureAtlas = WorldTextureAtlas()

    // Pre-allocated zero-GC scratch matrices
    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val invVpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var customHeroMesh: Mesh? = null
    private var uploadedObjSeq = 0L
    private var smoothedCamGroundY = sim.playerY
    private var lastFrameNs = System.nanoTime()
    private var hudAccumulatorSec = 0f
    private var firstFrameCompleted = false

    // Camera view-cone frustum culling values updated each frame
    private var camEyeX = 0f
    private var camEyeZ = 0f
    private var camFwdX = 0f
    private var camFwdZ = 1f
    private var viewportLandscape = true
    private val scratchVec4 = FloatArray(4)
    private val scratchProjVec = FloatArray(4)
    private val rayNearVec = FloatArray(4)
    private val rayFarVec = FloatArray(4)

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_BACK)

        program = createProgram(VERT_SHADER, FRAG_SHADER)
        aPosLoc = GLES30.glGetAttribLocation(program, "aPos")
        aNormLoc = GLES30.glGetAttribLocation(program, "aNorm")
        uMvpLoc = GLES30.glGetUniformLocation(program, "uMvp")
        uModelLoc = GLES30.glGetUniformLocation(program, "uModel")
        uColorLoc = GLES30.glGetUniformLocation(program, "uColor")
        uLightDirLoc = GLES30.glGetUniformLocation(program, "uLightDir")
        uSkyColorLoc = GLES30.glGetUniformLocation(program, "uSkyColor")
        uFogColorLoc = GLES30.glGetUniformLocation(program, "uFogColor")
        uFogDistLoc = GLES30.glGetUniformLocation(program, "uFogDist")
        uEmissiveLoc = GLES30.glGetUniformLocation(program, "uEmissive")
        uTexMixLoc = GLES30.glGetUniformLocation(program, "uTexMix")
        uDetailTexLoc = GLES30.glGetUniformLocation(program, "uDetailTex")

        instProgram = createProgram(INST_VERT_SHADER, INST_FRAG_SHADER)
        iPosLoc = GLES30.glGetAttribLocation(instProgram, "aPos")
        iNormLoc = GLES30.glGetAttribLocation(instProgram, "aNorm")
        iModelLoc = GLES30.glGetAttribLocation(instProgram, "aInstModel")
        iColorLoc = GLES30.glGetAttribLocation(instProgram, "aInstColor")
        iVpLoc = GLES30.glGetUniformLocation(instProgram, "uVp")
        iLightDirLoc = GLES30.glGetUniformLocation(instProgram, "uLightDir")
        iSkyColorLoc = GLES30.glGetUniformLocation(instProgram, "uSkyColor")
        iFogColorLoc = GLES30.glGetUniformLocation(instProgram, "uFogColor")
        iFogDistLoc = GLES30.glGetUniformLocation(instProgram, "uFogDist")
        iDetailTexLoc = GLES30.glGetUniformLocation(instProgram, "uDetailTex")

        cubeMesh = MeshPrimitives.createUnitCube()
        cylMesh = MeshPrimitives.createCylinder(12)
        coneMesh = MeshPrimitives.createCone(12)
        domeMesh = MeshPrimitives.createDome(6, 12)
        sphereMesh = MeshPrimitives.createSphere(8, 14)
        capsuleMesh = MeshPrimitives.createCapsule(12, 3)
        taperedTorsoMesh = MeshPrimitives.createTaperedTorso()
        beveledBoxMesh = MeshPrimitives.createBeveledBox(0.18f)

        trunkBatch = InstancedMeshBatch(512)
        pineBatch = InstancedMeshBatch(512)
        sakuraBatch = InstancedMeshBatch(512)
        boxPropBatch = InstancedMeshBatch(512)
        textureAtlas.init()
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        viewportLandscape = width >= height
        val aspect = width.toFloat() / height.coerceAtLeast(1).toFloat()
        Matrix.perspectiveM(projMatrix, 0, 56f, aspect, 0.3f, 680f)
    }

    override fun onDrawFrame(unused: GL10?) {
        val nowNs = System.nanoTime()
        val dt = ((nowNs - lastFrameNs) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        lastFrameNs = nowNs

        sim.step(dt)
        val objModel = sim.customObjModel
        if (objModel != null && sim.customObjUploadSeq != uploadedObjSeq) {
            customHeroMesh?.release()
            customHeroMesh = Mesh(objModel.vertices)
            uploadedObjSeq = sim.customObjUploadSeq
        } else if (objModel == null && customHeroMesh != null) {
            customHeroMesh?.release()
            customHeroMesh = null
        }

        hudAccumulatorSec += dt
        if (hudAccumulatorSec >= 0.08f) {
            hudAccumulatorSec = 0f
            onHudTick(sim.snapshotHud())
        }

        val lighting = WorldLighting.computeLighting(sim.timeOfDay, sim.weatherMode)
        GLES30.glClearColor(lighting.skyR, lighting.skyG, lighting.skyB, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        val drawDist = sim.graphicsPreset.drawDistance * 1.6f
        textureAtlas.bind(0)

        GLES30.glUseProgram(program)
        GLES30.glUniform1i(uDetailTexLoc, 0)
        GLES30.glUniform3f(uLightDirLoc, lighting.sunDirX, lighting.sunDirY, lighting.sunDirZ)
        GLES30.glUniform3f(uSkyColorLoc, lighting.skyR, lighting.skyG, lighting.skyB)
        GLES30.glUniform3f(uFogColorLoc, lighting.fogR, lighting.fogG, lighting.fogB)
        GLES30.glUniform1f(uFogDistLoc, drawDist)
        GLES30.glUniform1f(uTexMixLoc, 0.16f)

        val groundY = if (sim.insideHouse != null) 0f else WorldMapData.terrainHeightAt(sim.playerX, sim.playerZ)
        if (sim.showcaseMode != ShowcaseMode.NONE || sim.drivingVehicle != null || abs(groundY - smoothedCamGroundY) > 4.0f) {
            smoothedCamGroundY = sim.playerY
        } else {
            val isGrounded = sim.playerY <= groundY + 0.14f && abs(sim.velocityY) <= 0.35f
            val targetGround = if (isGrounded) sim.playerY else smoothedCamGroundY
            smoothedCamGroundY += (targetGround - smoothedCamGroundY) * minOf(1f, dt * 10f)
        }
        val camAnchorY = if (sim.showcaseMode == ShowcaseMode.NONE && sim.drivingVehicle == null) smoothedCamGroundY else sim.playerY

        val eyeX: Float
        val eyeY: Float
        val eyeZ: Float
        val lookX: Float
        val lookY: Float
        val lookZ: Float

        if (sim.showcaseMode == ShowcaseMode.CHARACTER_PREVIEW) {
            // Studio framing on Sakura Plaza with unobstructed view tailored to portrait/landscape split
            val studioX = 0f
            val studioY = 0f
            val studioZ = 14f
            val sideOffsetX = if (viewportLandscape) 1.05f else 0f
            val focusY = if (viewportLandscape) 1.05f else 0.45f
            eyeX = studioX + sideOffsetX
            eyeY = if (viewportLandscape) 1.48f else 1.25f
            eyeZ = studioZ + 4.4f
            lookX = studioX + sideOffsetX
            lookY = studioY + focusY
            lookZ = studioZ
            camFwdX = 0f
            camFwdZ = -1f
        } else {
            // Third-person over-right-shoulder follow camera or Splash/Login/Lobby Showcase camera
            val inShowcase = sim.showcaseMode != ShowcaseMode.NONE
            val baseDist = when {
                inShowcase && sim.showcaseMode == ShowcaseMode.SPLASH_LOADING -> 11.4f
                inShowcase -> 8.8f
                sim.drivingVehicle != null -> 8.6f
                sim.aiming -> 4.1f
                else -> 5.4f * sim.cameraDistanceScale.coerceIn(0.8f, 1.25f)
            }
            val camHeightOffset = when {
                sim.drivingVehicle != null -> 2.2f
                inShowcase -> 1.9f
                else -> 1.28f
            }
            val effectiveYaw = if (sim.showcaseMode == ShowcaseMode.NONE) sim.playerRotY else sim.cameraYaw
            val effectivePitch = when {
                inShowcase -> sim.cameraPitch
                sim.drivingVehicle != null -> 11f
                else -> 7f
            }
            val yawRad = Math3D.degToRad(effectiveYaw)
            val pitchRad = Math3D.degToRad(effectivePitch)
            val horizDist = baseDist * cos(pitchRad)
            val vertDist = baseDist * sin(pitchRad)
            val shoulderCamShift = if (sim.showcaseMode == ShowcaseMode.NONE && sim.drivingVehicle == null) {
                if (sim.aiming) 0.34f else 0.44f
            } else {
                0f
            }
            // Viewer screen-right vector = (-cos(yaw), 0, sin(yaw))
            val scrRightX = -cos(yawRad)
            val scrRightZ = sin(yawRad)
            val desiredEyeX = sim.playerX - sin(yawRad) * horizDist + scrRightX * shoulderCamShift
            val desiredEyeY = (camAnchorY + camHeightOffset + vertDist).coerceAtLeast(camAnchorY + 1.1f)
            val desiredEyeZ = sim.playerZ - cos(yawRad) * horizDist + scrRightZ * shoulderCamShift

            if (sim.showcaseMode == ShowcaseMode.NONE) {
                val safeEye = PhysicsWorld.resolveCameraPosition(
                    pivotX = sim.playerX,
                    pivotY = camAnchorY + 1.35f,
                    pivotZ = sim.playerZ,
                    desiredX = desiredEyeX,
                    desiredY = desiredEyeY,
                    desiredZ = desiredEyeZ,
                    insideHouse = sim.insideHouse
                )
                eyeX = safeEye.x
                eyeY = safeEye.y
                eyeZ = safeEye.z
            } else {
                eyeX = desiredEyeX
                eyeY = desiredEyeY
                eyeZ = desiredEyeZ
            }

            val sideShift = if (sim.showcaseMode == ShowcaseMode.LOGIN) -2.1f else if (sim.showcaseMode == ShowcaseMode.LOBBY) -0.9f else 0f
            val rightLookShift = if (sim.showcaseMode == ShowcaseMode.NONE && sim.drivingVehicle == null) {
                if (sim.aiming) 0.52f else 0.72f
            } else {
                0f
            }
            val lookAheadDist = if (inShowcase) 3.5f else 9.0f
            lookX = sim.playerX + sin(yawRad) * lookAheadDist + cos(yawRad) * sideShift + scrRightX * rightLookShift
            lookY = camAnchorY + when {
                sim.drivingVehicle != null -> 1.75f
                inShowcase -> 1.55f
                else -> 1.62f
            }
            lookZ = sim.playerZ + cos(yawRad) * lookAheadDist - sin(yawRad) * sideShift + scrRightZ * rightLookShift
            camFwdX = sin(yawRad)
            camFwdZ = cos(yawRad)
        }

        camEyeX = eyeX
        camEyeZ = eyeZ

        Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, 0f, 1f, 0f)
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        // Project hero head screen coordinates for overhead kill-streak badge
        if (sim.showcaseMode == ShowcaseMode.NONE && !sim.isDefeated) {
            scratchVec4[0] = sim.playerX
            scratchVec4[1] = sim.playerY + 2.38f
            scratchVec4[2] = sim.playerZ
            scratchVec4[3] = 1f
            Matrix.multiplyMV(scratchProjVec, 0, vpMatrix, 0, scratchVec4, 0)
            val clipW = scratchProjVec[3]
            if (clipW > 0.2f) {
                val sx = ((scratchProjVec[0] / clipW) + 1f) * 0.5f
                val sy = (1f - (scratchProjVec[1] / clipW)) * 0.5f
                sim.heroOverheadScreenX = sx.coerceIn(0.18f, 0.82f)
                sim.heroOverheadScreenY = sy.coerceIn(0.10f, 0.62f)
            }
        }

        // Unproject right-side crosshair screen (0.58f, 0.45f) -> NDC (+0.16f, +0.10f) into 3D aim ray
        val rdx = lookX - eyeX
        val rdy = lookY - eyeY
        val rdz = lookZ - eyeZ
        val rLen = sqrt(rdx * rdx + rdy * rdy + rdz * rdz).coerceAtLeast(1e-4f)
        var rayOrigX = eyeX
        var rayOrigY = eyeY
        var rayOrigZ = eyeZ
        var rayDirX = rdx / rLen
        var rayDirY = rdy / rLen
        var rayDirZ = rdz / rLen
        if (sim.showcaseMode == ShowcaseMode.NONE && sim.drivingVehicle == null && Matrix.invertM(invVpMatrix, 0, vpMatrix, 0)) {
            scratchVec4[0] = 0.16f
            scratchVec4[1] = 0.10f
            scratchVec4[2] = -1f
            scratchVec4[3] = 1f
            Matrix.multiplyMV(rayNearVec, 0, invVpMatrix, 0, scratchVec4, 0)
            scratchVec4[2] = 1f
            Matrix.multiplyMV(rayFarVec, 0, invVpMatrix, 0, scratchVec4, 0)
            val nw = rayNearVec[3]
            val fw = rayFarVec[3]
            if (abs(nw) > 1e-5f && abs(fw) > 1e-5f) {
                val nx = rayNearVec[0] / nw
                val ny = rayNearVec[1] / nw
                val nz = rayNearVec[2] / nw
                val fx = rayFarVec[0] / fw
                val fy = rayFarVec[1] / fw
                val fz = rayFarVec[2] / fw
                val ux = fx - nx
                val uy = fy - ny
                val uz = fz - nz
                val uLen = sqrt(ux * ux + uy * uy + uz * uz).coerceAtLeast(1e-4f)
                rayOrigX = nx
                rayOrigY = ny
                rayOrigZ = nz
                rayDirX = ux / uLen
                rayDirY = uy / uLen
                rayDirZ = uz / uLen
            }
        }
        sim.hasCustomCameraRay = true
        sim.updateCrosshairRayTarget(rayOrigX, rayOrigY, rayOrigZ, rayDirX, rayDirY, rayDirZ)
        updateProjectedSignboards()

        if (!firstFrameCompleted) {
            if (sim.insideHouse == null) {
                renderTerrainAndWater(minOf(drawDist, 160f))
                drawHeroCharacter(
                    x = sim.playerX,
                    y = sim.playerY,
                    z = sim.playerZ,
                    rotY = sim.playerRotY,
                    stride = sim.stridePhase,
                    crouching = sim.crouching,
                    proning = sim.proning,
                    recoil = sim.recoilKick
                )
            } else {
                renderHouseInterior(sim.insideHouse!!)
            }
            firstFrameCompleted = true
            onSurfaceReady?.invoke()
            return
        }

        if (sim.insideHouse != null && sim.showcaseMode != ShowcaseMode.CHARACTER_PREVIEW) {
            renderHouseInterior(sim.insideHouse!!)
        } else {
            renderOpenWorld(lighting, drawDist)
        }
    }

    private fun updateProjectedSignboards() {
        if (sim.showcaseMode != ShowcaseMode.NONE || sim.insideHouse != null) {
            sim.projectedSignboards = emptyList()
            return
        }
        val list = ArrayList<ProjectedSignboard>(8)
        data class AnchorItem(val id: String, val title: String, val subtitle: String, val x: Float, val y: Float, val z: Float, val accent: Long, val owned: Boolean, val maxDist: Float)
        val candidates = ArrayList<AnchorItem>(20)
        candidates += AnchorItem(
            "sign_indian_flag",
            "Indian National Flag",
            "Central Monument",
            CityLandmarksAndModules.FLAG_POLE_X,
            19.5f,
            CityLandmarksAndModules.FLAG_POLE_Z,
            0xFFFF9933L,
            false,
            150f
        )
        for (listing in GameCatalog.propertyMarketListings) {
            val isOwned = listing.id in sim.ownedPropertyIds
            candidates += AnchorItem(
                id = "sign_${listing.id}",
                title = listing.title,
                subtitle = if (isOwned) "OWNED • Enter / Stash" else "Buy ${listing.buyCoins} Coins",
                x = listing.locX,
                y = if (listing.isLandPlot) 5.5f else 13.5f,
                z = listing.locZ,
                accent = if (isOwned) 0xFF22C55EL else 0xFFFFD166L,
                owned = isOwned,
                maxDist = 115f
            )
        }
        for (loc in WorldLocations.allLocations) {
            if (loc.id == "loc_indian_flag_monument" || loc.id == "loc_spawn_central") continue
            if (loc.category == com.example.game.world.LocationCategory.SHOP || loc.category == com.example.game.world.LocationCategory.SERVICE) {
                candidates += AnchorItem(
                    id = "sign_${loc.id}",
                    title = loc.name,
                    subtitle = loc.category.label,
                    x = loc.x,
                    y = 14.5f,
                    z = loc.z,
                    accent = loc.category.colorHex,
                    owned = false,
                    maxDist = 105f
                )
            }
        }

        val sortedLandmarks = candidates.mapNotNull { item ->
            val dx = item.x - sim.playerX
            val dz = item.z - sim.playerZ
            val dist = sqrt(dx * dx + dz * dz)
            if (dist > item.maxDist) null else item to dist
        }.sortedBy { it.second }

        var landmarkCount = 0
        for ((item, dist) in sortedLandmarks) {
            if (landmarkCount >= 4) break
            scratchVec4[0] = item.x
            scratchVec4[1] = item.y
            scratchVec4[2] = item.z
            scratchVec4[3] = 1f
            Matrix.multiplyMV(scratchProjVec, 0, vpMatrix, 0, scratchVec4, 0)
            val w = scratchProjVec[3]
            if (w <= 0.35f) continue
            val ndcX = scratchProjVec[0] / w
            val ndcY = scratchProjVec[1] / w
            if (ndcX !in -0.88f..0.88f || ndcY !in -0.55f..0.88f) continue
            val sx = ((ndcX + 1f) * 0.5f).coerceIn(0.08f, 0.92f)
            val sy = ((1f - ndcY) * 0.5f).coerceIn(0.10f, 0.72f)
            list += ProjectedSignboard(
                id = item.id,
                title = item.title,
                subtitle = item.subtitle,
                screenX = sx,
                screenY = sy,
                distanceMeters = dist.toInt(),
                accentHex = item.accent,
                isOwnedProperty = item.owned
            )
            landmarkCount++
        }

        val sortedNpcs = sim.npcs.mapNotNull { npc ->
            if (!npc.respawnController.collidersEnabled) return@mapNotNull null
            val dx = npc.x - sim.playerX
            val dz = npc.z - sim.playerZ
            val dist = sqrt(dx * dx + dz * dz)
            if (dist > 34f) null else npc to dist
        }.sortedBy { it.second }

        var npcNameCount = 0
        for ((npc, dist) in sortedNpcs) {
            if (npcNameCount >= 4) break
            scratchVec4[0] = npc.x
            scratchVec4[1] = npc.y + 2.28f
            scratchVec4[2] = npc.z
            scratchVec4[3] = 1f
            Matrix.multiplyMV(scratchProjVec, 0, vpMatrix, 0, scratchVec4, 0)
            val w = scratchProjVec[3]
            if (w <= 0.6f) continue
            val ndcX = scratchProjVec[0] / w
            val ndcY = scratchProjVec[1] / w
            if (ndcX !in -0.85f..0.85f || ndcY !in -0.55f..0.88f) continue
            val sx = ((ndcX + 1f) * 0.5f).coerceIn(0.08f, 0.92f)
            val sy = ((1f - ndcY) * 0.5f).coerceIn(0.08f, 0.76f)
            list += ProjectedSignboard(
                id = "npc_${npc.id}",
                title = npc.displayName,
                subtitle = if (npc.provokedByPlayer) "RETALIATING!" else npc.role.label,
                screenX = sx,
                screenY = sy,
                distanceMeters = dist.toInt(),
                accentHex = if (npc.provokedByPlayer) 0xFFEF4444L else 0xFF38BDF8L,
                isOwnedProperty = false
            )
            npcNameCount++
        }

        sim.projectedSignboards = list
    }

    private fun isInFrustumRange(wx: Float, wz: Float, radius: Float, maxDist: Float): Boolean {
        val dx = wx - camEyeX
        val dz = wz - camEyeZ
        val distSq = dx * dx + dz * dz
        val limit = maxDist + radius
        if (distSq > limit * limit) return false
        if (distSq < (radius + 26f) * (radius + 26f)) return true
        val invDist = 1f / sqrt(distSq)
        val dot = (dx * camFwdX + dz * camFwdZ) * invDist
        return dot > -0.35f
    }

    private fun renderOpenWorld(lighting: LightingFrameConfig, drawDist: Float) {
        val isNight = lighting.isNight

        // 1. Chunked 2000 x 2000 Terrain Base + District Ground Transitions + Ocean & River
        renderTerrainAndWater(drawDist)

        // 2. Mountain Ridge Peaks & 4 Carved Guardian Statues overlooking the Valley
        renderMountainLandmarks(drawDist)

        // 3. Connected Roads, Bridges, Crosswalks, Traffic Signals & Plaza Ring
        renderRoadsAndIntersections(drawDist, isNight)

        // 4. Central Roundabout Indian National Flag Monument, Land Plot Pads, Torii Gates, Fountains & Props
        if (isInFrustumRange(CityLandmarksAndModules.FLAG_POLE_X, CityLandmarksAndModules.FLAG_POLE_Z, 26f, drawDist + 60f)) {
            drawIndianNationalFlagMonument(lighting)
        }
        drawPropertyPlotMarkersAndBoards(drawDist, isNight)

        val chunks = WorldMapData.chunkManager.allChunks()
        trunkBatch.clear()
        pineBatch.clear()
        sakuraBatch.clear()
        boxPropBatch.clear()

        for (c in chunks) {
            if (!isInFrustumRange(c.centerX, c.centerZ, 155f, drawDist + 140f)) continue

            // Buildings with 3-tier Distance LOD
            for (b in c.buildings) {
                val dx = b.x - sim.playerX
                val dz = b.z - sim.playerZ
                val dSq = dx * dx + dz * dz
                if (dSq > drawDist * drawDist) continue
                if (!isInFrustumRange(b.x, b.z, maxOf(b.width, b.depth), drawDist)) continue
                val tier = when {
                    dSq < 135f * 135f -> 2
                    dSq < 280f * 280f -> 1
                    else -> 0
                }
                drawBuildingLod(b, isNight, tier)
            }

            // Ceremonial Torii Gates
            for (g in c.gates) {
                if (isInFrustumRange(g.x, g.z, 18f, drawDist)) {
                    drawToriiGate(g)
                }
            }

            // Indian Heritage Arches
            for (arch in c.indianArches) {
                if (isInFrustumRange(arch.x, arch.z, arch.spanWidth, drawDist)) {
                    drawIndianArch(arch)
                }
            }

            // Park Fountains
            for (f in c.fountains) {
                if (isInFrustumRange(f.x, f.z, f.radius + 4f, drawDist)) {
                    drawFountain(f)
                }
            }

            // City Benches & Road Signs
            for (bench in c.benches) {
                if (isInFrustumRange(bench.x, bench.z, 6f, drawDist)) {
                    drawBench(bench)
                }
            }
            for (sign in c.roadSigns) {
                if (isInFrustumRange(sign.x, sign.z, 6f, drawDist)) {
                    drawRoadSign(sign, isNight)
                }
            }

            // Neon Billboards
            for (bb in c.billboards) {
                if (isInFrustumRange(bb.x, bb.z, 16f, drawDist)) {
                    drawBillboard(bb)
                }
            }

            // Moored Harbor Boats
            for (boat in c.boats) {
                if (isInFrustumRange(boat.x, boat.z, 14f, drawDist)) {
                    drawBoat(boat.x, boat.z, boat.rotY, boat.hullColor, boat.isYacht)
                }
            }

            // Queue Instanced Trees
            for (t in c.trees) {
                if (!isInFrustumRange(t.x, t.z, 8f, drawDist)) continue
                val groundY = WorldMapData.terrainHeightAt(t.x, t.z)
                val sway = sin(sim.worldTimeSec * 1.8f + t.x * 0.25f) * 0.22f
                Math3D.buildModelMatrix(modelMatrix, t.x, groundY + t.height * 0.35f, t.z, 0f, 0f, 0f, 0.8f, t.height * 0.7f, 0.8f)
                addInstanceColor(trunkBatch, 0xFF5D4037.toInt(), 0f)
                when {
                    t.isSakura -> {
                        Math3D.buildModelMatrix(modelMatrix, t.x + sway, groundY + t.height * 0.78f, t.z, 0f, 0f, 0f, 4.8f, 4.2f, 4.8f)
                        addInstanceColor(sakuraBatch, 0xFFF472B6.toInt(), 0.05f)
                    }
                    t.isPalm -> {
                        Math3D.buildModelMatrix(modelMatrix, t.x + sway, groundY + t.height * 0.82f, t.z, 0f, 0f, 0f, 5.0f, 0.55f, 1.3f)
                        addInstanceColor(boxPropBatch, 0xFF16A34A.toInt(), 0f)
                        Math3D.buildModelMatrix(modelMatrix, t.x + sway, groundY + t.height * 0.82f, t.z, 0f, 0f, 0f, 1.3f, 0.55f, 5.0f)
                        addInstanceColor(boxPropBatch, 0xFF22C55E.toInt(), 0f)
                    }
                    else -> {
                        Math3D.buildModelMatrix(modelMatrix, t.x + sway * 0.6f, groundY + t.height * 0.78f, t.z, 0f, 0f, 0f, 4.4f, 6.0f, 4.4f)
                        addInstanceColor(pineBatch, 0xFF14532D.toInt(), 0f)
                    }
                }
            }

            // Queue Instanced Boulders
            for (r in c.boulders) {
                if (!isInFrustumRange(r.x, r.z, r.scale * 2f, drawDist)) continue
                val gy = WorldMapData.terrainHeightAt(r.x, r.z)
                Math3D.buildModelMatrix(modelMatrix, r.x, gy + r.scale * 0.35f, r.z, 0f, (r.x % 90f), 0f, r.scale * 1.4f, r.scale, r.scale * 1.2f)
                addInstanceColor(sakuraBatch, 0xFF64748B.toInt(), 0f)
            }

            // Queue Instanced Streetlights
            for (l in c.streetLights) {
                if (!isInFrustumRange(l.x, l.z, 4f, drawDist)) continue
                val gy = WorldMapData.terrainHeightAt(l.x, l.z)
                Math3D.buildModelMatrix(modelMatrix, l.x, gy + 3.4f, l.z, 0f, 0f, 0f, 0.32f, 6.8f, 0.32f)
                addInstanceColor(trunkBatch, 0xFF334155.toInt(), 0f)
                Math3D.buildModelMatrix(modelMatrix, l.x, gy + 6.9f, l.z, 0f, l.rotY, 0f, 1.4f, 0.3f, 0.45f)
                addInstanceColor(boxPropBatch, 0xFFFEF08A.toInt(), if (isNight) 0.95f else 0.35f)
            }
        }

        // Flush all instanced GPU batches cleanly
        flushInstancedBatches(lighting, drawDist)

        // Switch back to main shader for dynamic vehicles, NPCs, GPS beacons & hero
        GLES30.glUseProgram(program)
        GLES30.glUniform1f(uTexMixLoc, 0.08f)

        // 5. GPS 3D Waypoint Beacons along active path
        if (sim.gpsPath.size >= 2) {
            for (idx in 1 until sim.gpsPath.size) {
                val pt = sim.gpsPath[idx]
                if (!isInFrustumRange(pt.x, pt.z, 6f, drawDist)) continue
                val py = WorldMapData.terrainHeightAt(pt.x, pt.z)
                val pulse = 1.0f + sin(sim.worldTimeSec * 4f + idx) * 0.25f
                drawCylinder(pt.x, py + 0.12f, pt.z, 2.2f * pulse, 0.14f, 2.2f * pulse, 0xFF00E5FF.toInt(), emissive = 0.85f)
            }
            sim.gpsDestination?.let { dest ->
                val dy = WorldMapData.terrainHeightAt(dest.x, dest.z)
                drawCylinder(dest.x, dy + 22f, dest.z, 1.2f, 44f, 1.2f, 0xFFFFD166.toInt(), emissive = 0.95f)
            }
        }

        // 6. Parked & Traffic Vehicles + Ground Shadows
        for (pv in sim.parkedVehicles) {
            if (!isInFrustumRange(pv.x, pv.z, 6f, drawDist)) continue
            val py = WorldMapData.terrainHeightAt(pv.x, pv.z)
            drawGroundShadow(pv.x, py, pv.z, 2.6f, lighting)
            drawVehicle(pv.x, py, pv.z, pv.rotY, pv.category, pv.colorInt, 0f, isNight)
        }
        for (tv in sim.traffic) {
            if (!isInFrustumRange(tv.x, tv.z, 6f, drawDist)) continue
            val ty = WorldMapData.terrainHeightAt(tv.x, tv.z)
            drawGroundShadow(tv.x, ty, tv.z, 2.5f, lighting)
            drawVehicle(tv.x, ty, tv.z, tv.rotY, tv.category, tv.colorInt, tv.wheelAngle, isNight)
        }

        // 7. AI NPCs & Offline Player Proxies (skip nearby plaza crowd only during focused studio preview)
        if (sim.showcaseMode != ShowcaseMode.CHARACTER_PREVIEW) {
            for (npc in sim.npcs) {
                if (!isInFrustumRange(npc.x, npc.z, 4f, drawDist)) continue
                val isDefeated = npc.state == NpcBehaviorState.DEFEATED || npc.state == NpcBehaviorState.RESPAWNING
                if (!isDefeated) {
                    drawGroundShadow(npc.x, npc.y, npc.z, 0.95f * npc.appearance.buildScale, lighting)
                }
                drawNpcCharacter(npc, isDefeated)
            }
        }

        // 8. Showcase / Player / Driving Vehicle / Pet
        if (sim.showcaseMode == ShowcaseMode.CHARACTER_PREVIEW) {
            drawGroundShadow(0f, 0f, 14f, 1.05f, lighting)
            drawHeroCharacter(
                x = 0f,
                y = 0f,
                z = 14f,
                rotY = (sim.previewYawOffset + 360f) % 360f,
                stride = sim.stridePhase,
                crouching = false,
                recoil = 0f
            )
        } else {
            if (sim.showcaseMode != ShowcaseMode.NONE) {
                drawVehicle(
                    x = -4.2f,
                    y = if (sim.showcaseVehicleCategory == "helicopter") 0.3f else 0f,
                    z = 12.2f,
                    rotY = 36f,
                    category = sim.showcaseVehicleCategory,
                    colorInt = sim.showcaseVehicleColorInt,
                    wheelAngle = 0f,
                    isNight = isNight
                )
                drawSquadShowcaseCharacter(
                    x = 4.6f, z = 11.2f, rotY = 340f,
                    stride = sin(sim.worldTimeSec * 1.9f) * 0.1f,
                    coatSilhouette = CoatSilhouette.LONG_TACTICAL_COAT,
                    coatColor = 0xFFE2E8F0.toInt(), liningColor = 0xFF0284C7.toInt(),
                    hairSilhouette = NpcHairSilhouette.SWEPT_SIDE, hairColor = 0xFFE2E8F0.toInt(),
                    accentColor = 0xFF00E5FF.toInt(), weaponId = "wpn_shadow_katana",
                    backAccessory = NpcBackAccessory.KATANA
                )
                drawSquadShowcaseCharacter(
                    x = -2.8f, z = 7.4f, rotY = 18f,
                    stride = sin(sim.worldTimeSec * 2.1f + 1f) * 0.1f,
                    coatSilhouette = CoatSilhouette.SHORT_JACKET,
                    coatColor = 0xFFF97316.toInt(), liningColor = 0xFF1E293B.toInt(),
                    hairSilhouette = NpcHairSilhouette.SPIKY_CROWN, hairColor = 0xFFFACC15.toInt(),
                    accentColor = 0xFF7C3AED.toInt(), weaponId = "wpn_vortex_ar",
                    backAccessory = NpcBackAccessory.NONE
                )
                val heliAngle = sim.worldTimeSec * 0.35f
                drawVehicle(
                    x = sin(heliAngle) * 38f,
                    y = 22f + sin(sim.worldTimeSec * 1.5f) * 0.8f,
                    z = -24f + cos(heliAngle) * 18f,
                    rotY = (Math3D.radToDeg(heliAngle) + 90f) % 360f,
                    category = "helicopter",
                    colorInt = 0xFF1E293B.toInt(),
                    wheelAngle = 0f,
                    isNight = isNight
                )
            }

            drawGroundShadow(sim.playerX, WorldMapData.terrainHeightAt(sim.playerX, sim.playerZ), sim.playerZ, if (sim.drivingVehicle != null) 2.7f else 1.0f, lighting)
            if (sim.drivingVehicle != null) {
                val dv = sim.drivingVehicle!!
                drawVehicle(sim.playerX, sim.playerY, sim.playerZ, sim.playerRotY, dv.category, dv.colorInt, sim.vehicleWheelAngle, isNight)
            } else {
                drawHeroCharacter(
                    x = sim.playerX,
                    y = sim.playerY,
                    z = sim.playerZ,
                    rotY = sim.playerRotY,
                    stride = sim.stridePhase,
                    crouching = sim.crouching,
                    proning = sim.proning,
                    recoil = sim.recoilKick
                )
            }
            if (sim.petSummoned) {
                drawPet(sim.petX, sim.petY, sim.petZ, sim.petRotY, sim.petType, sim.petBobPhase)
            }
        }

        // 8b. Ground Weapon Pickups, Wild Rare Pets & Multiplayer Player Companions
        if (sim.showcaseMode == ShowcaseMode.NONE) {
            for (gw in sim.groundWeapons) {
                if (!isInFrustumRange(gw.x, gw.z, 4f, drawDist)) continue
                drawGroundWeaponPickup(gw)
            }
            for (wp in sim.wildPets) {
                if (wp.adopted || !isInFrustumRange(wp.x, wp.z, 6f, drawDist)) continue
                PetModelRenderer.drawAnimePet(
                    drawer = this,
                    x = wp.x, y = wp.y, z = wp.z, rotY = wp.rotY,
                    petTypeOrId = wp.petId, bobPhase = sim.worldTimeSec * 3.5f,
                    evolutionLevel = wp.evolutionLevel, worldTimeSec = sim.worldTimeSec,
                    isWildBeacon = true
                )
            }
            for (target in sim.onlinePlayerTargets) {
                if (target.userId == sim.localPlayerUid || target.activePetId.isBlank()) continue
                val tx = target.posX.toFloat()
                val tz = target.posZ.toFloat()
                if (!isInFrustumRange(tx, tz, 6f, drawDist)) continue
                val ty = target.posY.toFloat()
                PetModelRenderer.drawAnimePet(
                    drawer = this,
                    x = tx + 1.8f, y = ty, z = tz + 1.6f, rotY = target.rotY.toFloat(),
                    petTypeOrId = target.activePetId, bobPhase = sim.worldTimeSec * 4.0f,
                    evolutionLevel = 2, worldTimeSec = sim.worldTimeSec
                )
            }
        }

        // 9. Animated Clouds, Projectiles, Grenades, Shockwaves & Weather Particles
        drawAnimatedClouds(lighting)
        for (p in sim.projectiles) {
            drawSphere(p.x, p.y, p.z, 0.38f, p.colorInt, emissive = 1.0f)
        }
        for (g in sim.activeGrenades) {
            drawSphere(g.x, g.y, g.z, 0.36f, 0xFFEF4444.toInt(), emissive = 0.9f)
            drawSphere(g.x, g.y + 0.16f, g.z, 0.18f, 0xFFFFD166.toInt(), emissive = 1.0f)
        }
        for (sw in sim.shockwaves) {
            drawCylinder(sw.x, sw.y, sw.z, sw.radius * 2f, 0.18f, sw.radius * 2f, sw.colorInt, emissive = 0.85f)
        }
        if (sim.weatherMode != WeatherMode.CLEAR) {
            val pColor = when (sim.weatherMode) {
                WeatherMode.SAKURA -> 0xFFF9A8D4.toInt()
                WeatherMode.RAIN -> 0xFF38BDF8.toInt()
                WeatherMode.STORM -> 0xFFA855F7.toInt()
                else -> 0xFFFFFFFF.toInt()
            }
            val pScaleY = if (sim.weatherMode == WeatherMode.SAKURA) 0.18f else 0.75f
            val count = sim.graphicsPreset.particleCount.coerceAtMost(sim.weatherParticles.size)
            for (i in 0 until count) {
                val wp = sim.weatherParticles[i]
                drawBox(wp.x, wp.y, wp.z, 0.14f, pScaleY, 0.14f, pColor, emissive = 0.65f)
            }
        }
        for (i in 0 until 12) {
            val a = sim.worldTimeSec * 0.7f + i * 0.9f
            val fx = sim.playerX + sin(a + i) * 14f
            val fz = sim.playerZ + cos(a * 0.8f + i) * 14f
            val fy = sim.playerY + 1.1f + abs(sin(sim.worldTimeSec * 1.4f + i)) * 2.5f
            val fColor = if (i % 2 == 0) 0xFF00E5FF.toInt() else 0xFFFFD166.toInt()
            drawSphere(fx, fy, fz, 0.22f, fColor, emissive = 0.95f)
        }
    }

    private fun renderTerrainAndWater(drawDist: Float) {
        GLES30.glUniform1f(uTexMixLoc, 0.24f)
        val chunks = WorldMapData.chunkManager.allChunks()
        for (c in chunks) {
            if (!isInFrustumRange(c.centerX, c.centerZ, 165f, drawDist + 160f)) continue
            val dId = WorldLocations.resolveDistrictId(c.centerX, c.centerZ)
            val groundColor = when (dId) {
                WorldDistrictId.CENTRAL_CITY -> 0xFF2B3548.toInt()
                WorldDistrictId.HIDDEN_VILLAGE -> 0xFF36533A.toInt()
                WorldDistrictId.MOUNTAIN_REGION -> 0xFF475569.toInt()
                WorldDistrictId.FOREST_REGION -> 0xFF1F4E32.toInt()
                WorldDistrictId.COASTAL_REGION -> if (c.centerZ > 640f) 0xFF0369A1.toInt() else 0xFFE8D19A.toInt()
                WorldDistrictId.RESIDENTIAL_REGION -> 0xFF2D6A4F.toInt()
                WorldDistrictId.COMMERCIAL_REGION -> 0xFF3B4254.toInt()
                WorldDistrictId.AIRPORT -> 0xFF334155.toInt()
            }
            val elev = if (dId == WorldDistrictId.MOUNTAIN_REGION && c.centerZ < -640f) {
                ((-620f - c.centerZ) / 280f).coerceIn(0f, 1f) * 14f
            } else if (dId == WorldDistrictId.COASTAL_REGION && c.centerZ > 640f) {
                -1.2f
            } else {
                0f
            }
            val emissive = if (dId == WorldDistrictId.COASTAL_REGION && c.centerZ > 640f) 0.14f else 0f
            val chunkTile = WorldMapData.chunkManager.chunkSize
            drawBox(c.centerX, elev - 0.5f, c.centerZ, chunkTile, 1.0f, chunkTile, groundColor, emissive = emissive)
        }

        // District surface details (Indian Bazaar Sandstone Plaza, Coastal Beach Strip, Island Discs, Forest River)
        if (isInFrustumRange(456f, 260f, 120f, drawDist)) {
            drawBox(456f, -0.02f, 265f, 170f, 0.24f, 140f, 0xFFD6B98C.toInt())
        }
        if (isInFrustumRange(-660f, -240f, 240f, drawDist)) {
            drawBox(-660f, -0.04f, -240f, 440f, 0.2f, 310f, 0xFF3F4B5E.toInt())
        }
        if (isInFrustumRange(0f, 595f, 550f, drawDist)) {
            drawBox(0f, -0.06f, 602f, 2400f, 0.28f, 68f, 0xFFE8D19A.toInt())
        }
        // Azure Lighthouse Island & Pearl Resort Island
        if (isInFrustumRange(-180f, 850f, 80f, drawDist)) {
            drawCylinder(-180f, 0.1f, 850f, 125f, 0.8f, 125f, 0xFFE8D19A.toInt())
            drawCylinder(-180f, 0.28f, 850f, 85f, 0.6f, 85f, 0xFF2D6A4F.toInt())
        }
        if (isInFrustumRange(260f, 845f, 90f, drawDist)) {
            drawCylinder(260f, 0.1f, 845f, 140f, 0.8f, 140f, 0xFFE8D19A.toInt())
            drawCylinder(260f, 0.28f, 845f, 95f, 0.6f, 95f, 0xFF2D6A4F.toInt())
        }
        // Forest River Water Ribbon
        val rNodes = ForestGenerator.riverNodes
        for (i in 0 until rNodes.lastIndex) {
            val a = rNodes[i]
            val b = rNodes[i + 1]
            val mx = (a.x + b.x) * 0.5f
            val mz = (a.z + b.z) * 0.5f
            if (!isInFrustumRange(mx, mz, 90f, drawDist)) continue
            val len = kotlin.math.hypot(b.x - a.x, b.z - a.z) + 12f
            val yaw = Math3D.radToDeg(kotlin.math.atan2(b.x - a.x, b.z - a.z))
            drawPart(mx, 0.05f, mz, 0f, yaw, 0f, 26f, 0.18f, len, 0xFF0EA5E9.toInt(), emissive = 0.22f)
        }
        // Central City Park Lawn
        if (isInFrustumRange(-74f, 58f, 40f, drawDist)) {
            drawBox(-74f, 0.03f, 58f, 42f, 0.16f, 38f, 0xFF22543D.toInt())
        }

        // Indian Mega City Zone Scale Pads (Residential Blocks & Commercial Shopping District)
        if (isInFrustumRange(-155f, 0f, 240f, drawDist)) {
            drawBox(-155f, -0.02f, 0f, 160f, 0.08f, 380f, 0xFF2A3B47.toInt())
        }
        if (isInFrustumRange(510f, 110f, 300f, drawDist)) {
            drawBox(510f, -0.02f, 110f, 480f, 0.08f, 480f, 0xFF333C4D.toInt())
        }

        // Central Circular Roundabout Pool Ring, Flag Island & Southern Viewing Pier
        if (isInFrustumRange(0f, 0f, 76f, drawDist)) {
            val promDiam = CityLandmarksAndModules.PROMENADE_OUTER_RADIUS * 2f
            val copingDiam = CityLandmarksAndModules.POOL_SHALLOW_OUTER_RADIUS * 2f + 2.4f
            val poolDiam = CityLandmarksAndModules.POOL_SHALLOW_OUTER_RADIUS * 2f
            val islandDiam = CityLandmarksAndModules.FLAG_ISLAND_RADIUS * 2f
            // Stone promenade sidewalk ring (r = 28..36u)
            drawCylinder(0f, 0.01f, 0f, promDiam, 0.08f, promDiam, 0xFFCBD5E1.toInt())
            // Sandstone pool coping rim
            drawCylinder(0f, 0.06f, 0f, copingDiam, 0.16f, copingDiam, 0xFFE6C79C.toInt())
            // Sunken shimmering aqua pool surface (r = 5.5..28u)
            drawCylinder(0f, -0.18f, 0f, poolDiam, 0.16f, poolDiam, 0xFF0EA5E9.toInt(), emissive = 0.25f)
            // Flag Monument Island Plinth (r <= 5.5u)
            drawCylinder(0f, 0.18f, 0f, islandDiam, 0.56f, islandDiam, 0xFFE2E8F0.toInt())
            // Southern Ceremonial Marble Viewing Pier keeping (0, 14) dry at y = 0f
            drawBox(0f, 0.04f, 19.4f, 13.0f, 0.22f, 29.2f, 0xFFF1F5F9.toInt())
            drawBox(-6.2f, 0.24f, 19.4f, 0.45f, 0.36f, 29.2f, 0xFFFF9933.toInt(), emissive = 0.25f)
            drawBox(6.2f, 0.24f, 19.4f, 0.45f, 0.36f, 29.2f, 0xFF138808.toInt(), emissive = 0.25f)
        }
    }

    private fun renderMountainLandmarks(drawDist: Float) {
        GLES30.glUniform1f(uTexMixLoc, 0.22f)
        for (pk in WorldGenerator.mountainPeaks) {
            if (!isInFrustumRange(pk.x, pk.z, pk.width, drawDist + 240f)) continue
            drawCone(pk.x, pk.y, pk.z, pk.width, pk.height, pk.depth, pk.colorInt)
        }
        // 4 Monumental Guardian Stone Faces overlooking Sakura Plaza
        if (isInFrustumRange(0f, -890f, 240f, drawDist + 320f)) {
            val faceXs = floatArrayOf(-110f, -38f, 38f, 110f)
            for (fx in faceXs) {
                drawBox(fx, 44f, -850f, 26f, 34f, 14f, 0xFF94A3B8.toInt())
                drawCone(fx, 66f, -850f, 32f, 16f, 18f, 0xFF475569.toInt())
                drawBox(fx, 50f, -842f, 20f, 4f, 3f, 0xFF475569.toInt())
                drawBox(fx, 42f, -841f, 5f, 9f, 4f, 0xFFCBD5E1.toInt())
            }
        }
    }

    private fun renderRoadsAndIntersections(drawDist: Float, isNight: Boolean) {
        GLES30.glUniform1f(uTexMixLoc, 0.15f)
        // Roundabout Inner Promenade Neon Curb Accent Markers (r = 35.8u, preserving sunken pool inside r <= 28u)
        if (isInFrustumRange(0f, 0f, 64f, drawDist)) {
            for (k in 0 until 24) {
                val ang = (k / 24.0) * 2.0 * kotlin.math.PI
                val bx = (cos(ang) * 35.6f).toFloat()
                val bz = (sin(ang) * 35.6f).toFloat()
                val accent = when (k % 3) {
                    0 -> 0xFFFF9933.toInt()
                    1 -> 0xFF00E5FF.toInt()
                    else -> 0xFF138808.toInt()
                }
                drawBox(bx, 0.14f, bz, 1.3f, 0.22f, 1.3f, accent, emissive = if (isNight) 0.85f else 0.35f)
            }
        }

        for (seg in WorldGenerator.roads) {
            if (!isInFrustumRange(seg.midX, seg.midZ, seg.length * 0.55f + 20f, drawDist)) continue
            val yBase = if (seg.isBridge) {
                if (abs(seg.midZ) > 600f) 1.45f else 1.05f
            } else {
                WorldMapData.terrainHeightAt(seg.midX, seg.midZ)
            }
            val roadColor = when (seg.surface) {
                RoadSurfaceType.VILLAGE_STONE -> 0xFF6B5B4B.toInt()
                RoadSurfaceType.MOUNTAIN_SWITCHBACK -> 0xFF334155.toInt()
                RoadSurfaceType.BRIDGE_DECK -> 0xFF475569.toInt()
                RoadSurfaceType.RUNWAY -> 0xFF111827.toInt()
                RoadSurfaceType.PARKING_BAY -> 0xFF283447.toInt()
                RoadSurfaceType.ASPHALT_AVENUE -> 0xFF1F2937.toInt()
            }
            val len = seg.length + seg.width * 0.4f
            // Sidewalk raised curb
            if (seg.hasSidewalks) {
                drawPart(seg.midX, yBase + 0.04f, seg.midZ, 0f, seg.yawDeg, 0f, seg.width + 5f, 0.18f, len, 0xFF64748B.toInt())
            }
            // Bridge guardrails & piers
            if (seg.isBridge) {
                drawPart(seg.midX, yBase - 1.2f, seg.midZ, 0f, seg.yawDeg, 0f, 3.2f, 2.4f, 4.0f, 0xFF334155.toInt())
                drawPart(seg.midX, yBase + 0.55f, seg.midZ, 0f, seg.yawDeg, 0f, seg.width + 1.2f, 0.45f, len, 0xFF00E5FF.toInt(), emissive = 0.25f)
            }
            // Road deck
            drawPart(seg.midX, yBase + 0.08f, seg.midZ, 0f, seg.yawDeg, 0f, seg.width, 0.16f, len, roadColor)

            // Queue instanced lane dashes
            if (seg.hasLaneMarkings && seg.length > 35f) {
                val dashes = (seg.length / 18f).toInt().coerceIn(2, 32)
                for (i in 1 until dashes) {
                    val t = i.toFloat() / dashes
                    val dx = seg.startX + (seg.endX - seg.startX) * t
                    val dz = seg.startZ + (seg.endZ - seg.startZ) * t
                    val dy = if (seg.isBridge) yBase + 0.18f else WorldMapData.terrainHeightAt(dx, dz) + 0.18f
                    Math3D.buildModelMatrix(modelMatrix, dx, dy, dz, 0f, seg.yawDeg, 0f, 0.55f, 0.05f, 5.5f)
                    addInstanceColor(boxPropBatch, if (seg.surface == RoadSurfaceType.RUNWAY) 0xFFFACC15.toInt() else 0xFFF8FAFC.toInt(), 0.15f)
                }
            }
        }

        // Crosswalks & Traffic Signals at Intersections
        val signalPhase = (sim.worldTimeSec * 0.25f).toInt() % 3
        for (inter in WorldGenerator.intersections) {
            if (!isInFrustumRange(inter.x, inter.z, 25f, drawDist)) continue
            if (inter.hasCrosswalks) {
                for (stripe in -2..2) {
                    val off = stripe * 3.2f
                    Math3D.buildModelMatrix(modelMatrix, inter.x + off, 0.18f, inter.z - 12f, 0f, 0f, 0f, 1.3f, 0.04f, 4.2f)
                    addInstanceColor(boxPropBatch, 0xFFF8FAFC.toInt(), 0.1f)
                    Math3D.buildModelMatrix(modelMatrix, inter.x + off, 0.18f, inter.z + 12f, 0f, 0f, 0f, 1.3f, 0.04f, 4.2f)
                    addInstanceColor(boxPropBatch, 0xFFF8FAFC.toInt(), 0.1f)
                }
            }
            if (inter.hasTrafficSignals && isInFrustumRange(inter.x, inter.z, 18f, minOf(drawDist, 220f))) {
                drawTrafficSignal(inter.x + 12f, inter.z + 12f, signalPhase, isNight)
            }
        }
    }

    private fun drawTrafficSignal(x: Float, z: Float, phase: Int, isNight: Boolean) {
        drawCylinder(x, 3.0f, z, 0.3f, 6.0f, 0.3f, 0xFF1E293B.toInt())
        drawBox(x - 1.2f, 5.8f, z, 2.8f, 0.9f, 0.7f, 0xFF0F172A.toInt())
        val sigColor = when (phase) {
            0 -> 0xFF22C55E.toInt()
            1 -> 0xFFFACC15.toInt()
            else -> 0xFFEF4444.toInt()
        }
        drawSphere(x - 1.6f, 5.8f, z + 0.45f, 0.55f, sigColor, emissive = if (isNight) 1.0f else 0.85f)
    }

    private fun drawBuildingLod(b: WorldBuilding, isNight: Boolean, lodTier: Int) {
        val emissiveWin = if (isNight) 0.88f else 0.25f
        val groundY = WorldMapData.terrainHeightAt(b.x, b.z)

        // Tier 0 (Far): Silhouette box + roof cap
        drawBox(b.x, groundY + b.height * 0.5f, b.z, b.width, b.height, b.depth, b.wallColor)
        if (lodTier == 0) {
            drawBox(b.x, groundY + b.height + 0.6f, b.z, b.width + 0.8f, 1.2f, b.depth + 0.8f, b.roofColor)
            return
        }

        // Base granite plinth
        drawBox(b.x, groundY + 0.45f, b.z, b.width + 1.0f, 0.9f, b.depth + 1.0f, 0xFF1E293B.toInt())

        when (b.style) {
            BuildingStyle.INDIAN_PALACE -> {
                drawDome(b.x, groundY + b.height, b.z, b.width * 0.55f, b.height * 0.55f, b.width * 0.55f, b.roofColor)
                drawCone(b.x, groundY + b.height + b.height * 0.35f, b.z, 1.8f, 3.6f, 1.8f, b.accentColor, emissive = 0.4f)
                drawDome(b.x - b.width * 0.32f, groundY + b.height, b.z - b.depth * 0.32f, 5f, 5f, 5f, b.accentColor)
                drawDome(b.x + b.width * 0.32f, groundY + b.height, b.z + b.depth * 0.32f, 5f, 5f, 5f, b.accentColor)
            }
            BuildingStyle.LIGHTHOUSE -> {
                drawCylinder(b.x, groundY + b.height * 0.5f, b.z, b.width * 0.85f, b.height, b.width * 0.85f, b.wallColor)
                drawCylinder(b.x, groundY + b.height + 1.6f, b.z, b.width * 0.6f, 3.2f, b.width * 0.6f, b.accentColor, emissive = 1.0f)
                drawCone(b.x, groundY + b.height + 4.8f, b.z, b.width * 0.72f, 4.0f, b.width * 0.72f, b.roofColor)
            }
            BuildingStyle.HOSPITAL -> {
                drawBox(b.x, groundY + b.height + 0.7f, b.z, b.width + 1.4f, 1.4f, b.depth + 1.4f, b.roofColor)
                drawBox(b.x, groundY + b.height * 0.65f, b.z + b.depth * 0.5f + 0.2f, 5.2f, 1.6f, 0.3f, 0xFFEF4444.toInt(), emissive = 0.65f)
                drawBox(b.x, groundY + b.height * 0.65f, b.z + b.depth * 0.5f + 0.2f, 1.6f, 5.2f, 0.3f, 0xFFEF4444.toInt(), emissive = 0.65f)
            }
            BuildingStyle.PLAYER_VILLA, BuildingStyle.JAPANESE_SHRINE -> {
                drawBox(b.x, groundY + b.height + 0.5f, b.z, b.width + 2.6f, 1.0f, b.depth + 2.6f, b.roofColor)
                drawBox(b.x, groundY + b.height + 1.5f, b.z, b.width * 0.72f, 1.1f, b.depth * 0.72f, b.roofColor)
                // Entrance door glow
                drawBox(b.x, groundY + 1.8f, b.z + b.depth * 0.5f + 0.14f, 3.4f, 3.4f, 0.28f, b.accentColor, emissive = 0.5f)
            }
            BuildingStyle.AIRPORT_HANGAR -> {
                drawDome(b.x, groundY + b.height * 0.7f, b.z, b.width * 1.02f, b.height * 0.7f, b.depth * 1.02f, b.roofColor)
                drawBox(b.x, groundY + b.height * 0.4f, b.z + b.depth * 0.5f + 0.15f, b.width * 0.65f, b.height * 0.7f, 0.3f, 0xFF0F172A.toInt())
            }
            else -> {
                drawBox(b.x, groundY + b.height + 0.6f, b.z, b.width + 1.2f, 1.2f, b.depth + 1.2f, b.roofColor)
                drawBox(b.x, groundY + b.height * 0.88f, b.z, b.width + 0.35f, 0.75f, b.depth + 0.35f, b.accentColor, emissive = 0.82f)
            }
        }

        // Illuminated window strips
        if (b.height >= 9f) {
            val floors = (b.height / 4.5f).toInt().coerceIn(1, 6)
            for (f in 1..floors) {
                drawBox(b.x, groundY + f * 3.9f, b.z, b.width + 0.18f, 1.0f, b.depth + 0.18f, 0xFFBAE6FD.toInt(), emissive = emissiveWin)
            }
        }

        // High-LOD (lodTier == 2): Front doors, awnings, balconies & roof antenna
        if (lodTier == 2) {
            drawBox(b.x, groundY + 1.8f, b.z + b.depth * 0.5f + 0.14f, 3.6f, 3.4f, 0.28f, 0xFF0F172A.toInt())
            drawBox(b.x, groundY + 3.8f, b.z + b.depth * 0.5f + 0.9f, 5.2f, 0.45f, 1.8f, b.accentColor, emissive = 0.35f)
            if (b.height >= 20f) {
                drawBox(b.x, groundY + b.height * 0.52f, b.z + b.depth * 0.5f + 0.85f, b.width * 0.48f, 0.4f, 1.6f, 0xFF475569.toInt())
                drawCylinder(b.x, groundY + b.height + 3.5f, b.z, 0.35f, 6.0f, 0.35f, b.accentColor, emissive = 0.7f)
            }
        }
    }

    private fun drawIndianNationalFlagMonument(lighting: LightingFrameConfig) {
        val fx = CityLandmarksAndModules.FLAG_POLE_X
        val fz = CityLandmarksAndModules.FLAG_POLE_Z
        val baseTopY = 2.4f
        // Stepped marble & brass Ashoka pedestal
        drawCylinder(fx, 0.65f, fz, 7.6f, 0.8f, 7.6f, 0xFFE2E8F0.toInt())
        drawCylinder(fx, 1.35f, fz, 5.2f, 0.7f, 5.2f, 0xFFF8FAFC.toInt())
        drawCylinder(fx, 2.05f, fz, 2.6f, 0.7f, 2.6f, 0xFFD97706.toInt(), emissive = 0.15f)
        // High flagpole & golden finial sphere
        val poleH = CityLandmarksAndModules.FLAG_POLE_HEIGHT
        drawCylinder(fx, baseTopY + poleH * 0.5f, fz, 0.42f, poleH, 0.42f, 0xFFF1F5F9.toInt())
        drawSphere(fx, baseTopY + poleH + 0.35f, fz, 0.75f, 0xFFFFD166.toInt(), emissive = 0.5f)

        // Animated 3-band Indian National Flag (Saffron, White, Green) + Navy Blue 24-spoke Ashoka Chakra
        val flagW = CityLandmarksAndModules.FLAG_WIDTH
        val bandH = CityLandmarksAndModules.FLAG_HEIGHT / 3f
        val flagTopY = baseTopY + poleH - 0.5f
        val flagXCenter = fx + flagW * 0.5f + 0.25f
        val waveYaw = sin(sim.worldTimeSec * 2.8f) * 6.5f
        val flagEmissive = if (lighting.isNight) 0.42f else 0.18f

        drawPart(flagXCenter, flagTopY - bandH * 0.5f, fz, 0f, waveYaw, 0f, flagW, bandH, 0.14f, CityLandmarksAndModules.FLAG_SAFFRON_HEX.toInt(), emissive = flagEmissive)
        drawPart(flagXCenter, flagTopY - bandH * 1.5f, fz, 0f, waveYaw, 0f, flagW, bandH, 0.14f, CityLandmarksAndModules.FLAG_WHITE_HEX.toInt(), emissive = flagEmissive)
        drawPart(flagXCenter, flagTopY - bandH * 2.5f, fz, 0f, waveYaw, 0f, flagW, bandH, 0.14f, CityLandmarksAndModules.FLAG_GREEN_HEX.toInt(), emissive = flagEmissive)

        // 24-spoke Ashoka Chakra on both front & back of the white band
        val chakraY = flagTopY - bandH * 1.5f
        val chakraR = bandH * 0.40f
        val chakraNavy = CityLandmarksAndModules.FLAG_CHAKRA_NAVY_HEX.toInt()
        for (sideZ in floatArrayOf(0.09f, -0.09f)) {
            drawCylinder(flagXCenter, chakraY, fz + sideZ, chakraR * 0.45f, 0.12f, chakraR * 0.45f, chakraNavy, emissive = 0.2f)
            for (s in 0 until CityLandmarksAndModules.FLAG_ASHOKA_CHAKRA_SPOKES) {
                val deg = (s * 360f) / CityLandmarksAndModules.FLAG_ASHOKA_CHAKRA_SPOKES
                drawPart(flagXCenter, chakraY, fz + sideZ, 0f, 0f, deg, chakraR * 1.85f, 0.045f, 0.04f, chakraNavy, emissive = 0.18f)
            }
        }
    }

    private fun drawPropertyPlotMarkersAndBoards(drawDist: Float, isNight: Boolean) {
        val boardGlow = if (isNight) 0.72f else 0.35f
        for (listing in GameCatalog.propertyMarketListings) {
            if (!isInFrustumRange(listing.locX, listing.locZ, 20f, drawDist)) continue
            val isOwned = listing.id in sim.ownedPropertyIds
            val markerColor = if (isOwned) 0xFF22C55E.toInt() else 0xFFFFD166.toInt()
            val gy = WorldMapData.terrainHeightAt(listing.locX, listing.locZ)
            if (listing.isLandPlot) {
                // Concrete foundation slab + glowing corner survey stakes + overhead For Sale / Owned Signboard
                drawBox(listing.locX, gy + 0.06f, listing.locZ, 18f, 0.12f, 18f, 0xFF334155.toInt())
                for (sx in floatArrayOf(-8.5f, 8.5f)) {
                    for (sz in floatArrayOf(-8.5f, 8.5f)) {
                        drawCylinder(listing.locX + sx, gy + 0.9f, listing.locZ + sz, 0.35f, 1.8f, 0.35f, markerColor, emissive = 0.6f)
                    }
                }
            }
            // Overhead entrance signboard above plot/house
            drawCylinder(listing.locX - 2.6f, gy + 3.2f, listing.locZ + 9.2f, 0.24f, 6.4f, 0.24f, 0xFF475569.toInt())
            drawCylinder(listing.locX + 2.6f, gy + 3.2f, listing.locZ + 9.2f, 0.24f, 6.4f, 0.24f, 0xFF475569.toInt())
            drawBox(listing.locX, gy + 6.1f, listing.locZ + 9.2f, 6.4f, 1.6f, 0.42f, 0xFF0F172A.toInt())
            drawBox(listing.locX, gy + 6.1f, listing.locZ + 9.2f, 5.9f, 1.15f, 0.48f, markerColor, emissive = boardGlow)
        }
    }

    private fun drawBench(b: CityBenchProp) {
        val gy = WorldMapData.terrainHeightAt(b.x, b.z)
        drawPart(b.x, gy + 0.55f, b.z, 0f, b.rotY, 0f, 2.4f, 0.16f, 0.85f, 0xFF92400E.toInt())
        drawPart(b.x, gy + 0.95f, b.z - 0.3f, 0f, b.rotY, 0f, 2.4f, 0.65f, 0.14f, 0xFF78350F.toInt())
        drawPart(b.x, gy + 0.25f, b.z, 0f, b.rotY, 0f, 2.1f, 0.45f, 0.65f, 0xFF1E293B.toInt())
    }

    private fun drawRoadSign(s: CityRoadSignProp, isNight: Boolean) {
        val gy = WorldMapData.terrainHeightAt(s.x, s.z)
        drawCylinder(s.x, gy + 2.2f, s.z, 0.22f, 4.4f, 0.22f, 0xFF475569.toInt())
        drawPart(s.x, gy + 4.3f, s.z, 0f, s.rotY, 0f, 2.6f, 1.3f, 0.18f, 0xFF0F172A.toInt())
        drawPart(s.x, gy + 4.3f, s.z, 0f, s.rotY, 0f, 2.3f, 1.0f, 0.22f, s.accentColor, emissive = if (isNight) 0.85f else 0.4f)
    }

    private fun drawIndianArch(arch: IndianArchProp) {
        val gy = WorldMapData.terrainHeightAt(arch.x, arch.z)
        val rad = Math3D.degToRad(arch.rotY)
        val halfSpan = arch.spanWidth * 0.5f
        val px = cos(rad) * halfSpan
        val pz = -sin(rad) * halfSpan
        drawBox(arch.x - px, gy + arch.height * 0.45f, arch.z - pz, 2.0f, arch.height * 0.9f, 2.0f, arch.stoneColor)
        drawBox(arch.x + px, gy + arch.height * 0.45f, arch.z + pz, 2.0f, arch.height * 0.9f, 2.0f, arch.stoneColor)
        drawPart(arch.x, gy + arch.height * 0.88f, arch.z, 0f, arch.rotY, 0f, arch.spanWidth + 2.6f, 1.5f, 2.2f, arch.stoneColor)
        drawPart(arch.x, gy + arch.height * 0.99f, arch.z, 0f, arch.rotY, 0f, arch.spanWidth * 0.72f, 0.7f, 2.4f, arch.accentColor, emissive = 0.4f)
        drawDome(arch.x, gy + arch.height + 0.6f, arch.z, 3.4f, 2.2f, 3.4f, arch.accentColor, emissive = 0.25f)
    }

    private fun drawToriiGate(g: ToriiMonumentProp) {
        val gy = WorldMapData.terrainHeightAt(g.x, g.z)
        val s = g.scale
        val red = 0xFFDC2626.toInt()
        val dark = 0xFF1E1B18.toInt()
        val rad = Math3D.degToRad(g.rotY)
        val rx = cos(rad) * 4.8f * s
        val rz = -sin(rad) * 4.8f * s
        drawCylinder(g.x - rx, gy + 4.5f * s, g.z - rz, 1.1f * s, 9f * s, 1.1f * s, red)
        drawCylinder(g.x + rx, gy + 4.5f * s, g.z + rz, 1.1f * s, 9f * s, 1.1f * s, red)
        drawPart(g.x, gy + 8.8f * s, g.z, 0f, g.rotY, 0f, 13.5f * s, 1.0f * s, 1.3f * s, red)
        drawPart(g.x, gy + 9.5f * s, g.z, 0f, g.rotY, 0f, 14.5f * s, 0.65f * s, 1.5f * s, dark)
        drawPart(g.x, gy + 7.0f * s, g.z, 0f, g.rotY, 0f, 10.8f * s, 0.65f * s, 0.9f * s, red)
        drawPart(g.x, gy + 8.0f * s, g.z, 0f, g.rotY, 0f, 1.4f * s, 1.4f * s, 1.1f * s, 0xFFFFD166.toInt(), emissive = 0.5f)
    }

    private fun drawFountain(f: ParkFountainProp) {
        drawCylinder(f.x, 0.45f, f.z, f.radius * 2f, 0.9f, f.radius * 2f, 0xFFCBD5E1.toInt())
        drawCylinder(f.x, 0.72f, f.z, f.radius * 1.7f, 0.55f, f.radius * 1.7f, 0xFF0EA5E9.toInt(), emissive = 0.35f)
        val plumeH = 2.6f + sin(sim.worldTimeSec * 4.2f) * 0.45f
        drawCylinder(f.x, 1.6f, f.z, 1.2f, plumeH, 1.2f, 0xFF38BDF8.toInt(), emissive = 0.75f)
    }

    private fun drawBillboard(bb: DistrictBillboard) {
        drawCylinder(bb.x, bb.y * 0.5f, bb.z, 0.7f, bb.y, 0.7f, 0xFF1E293B.toInt())
        drawPart(bb.x, bb.y, bb.z, 0f, bb.rotY, 0f, bb.width + 0.8f, bb.height + 0.8f, 0.7f, 0xFF0F172A.toInt())
        drawPart(bb.x, bb.y, bb.z, 0f, bb.rotY, 0f, bb.width, bb.height, 0.8f, bb.accentColor, emissive = 0.9f)
    }

    private fun drawBoat(x: Float, z: Float, rotY: Float, hullColor: Int, isYacht: Boolean) {
        val bob = sin(sim.worldTimeSec * 1.8f + x * 0.05f) * 0.2f
        val len = if (isYacht) 13f else 7.5f
        val wid = if (isYacht) 4.2f else 2.6f
        drawPart(x, -0.35f + bob, z, 0f, rotY, 0f, wid, 1.4f, len, hullColor)
        drawPart(x, 0.85f + bob, z, 0f, rotY, 0f, wid * 0.65f, 1.2f, len * 0.45f, 0xFF0F172A.toInt())
        drawPart(x, 1.55f + bob, z, 0f, rotY, 0f, wid * 0.5f, 0.25f, len * 0.35f, 0xFF00E5FF.toInt(), emissive = 0.45f)
    }

    private fun drawGroundShadow(x: Float, groundY: Float, z: Float, diameter: Float, lighting: LightingFrameConfig) {
        val shiftX = -lighting.sunDirX * 0.4f
        val shiftZ = -lighting.sunDirZ * 0.4f
        drawCylinder(x + shiftX, groundY + 0.04f, z + shiftZ, diameter, 0.03f, diameter, 0xFF0B1120.toInt())
    }

    private fun addInstanceColor(batch: InstancedMeshBatch, colorInt: Int, emissive: Float) {
        batch.addInstance(
            modelMatrix = modelMatrix,
            r = Color.red(colorInt) / 255f,
            g = Color.green(colorInt) / 255f,
            b = Color.blue(colorInt) / 255f,
            emissive = emissive
        )
    }

    private fun flushInstancedBatches(lighting: LightingFrameConfig, drawDist: Float) {
        GLES30.glUseProgram(instProgram)
        GLES30.glUniform1i(iDetailTexLoc, 0)
        GLES30.glUniformMatrix4fv(iVpLoc, 1, false, vpMatrix, 0)
        GLES30.glUniform3f(iLightDirLoc, lighting.sunDirX, lighting.sunDirY, lighting.sunDirZ)
        GLES30.glUniform3f(iSkyColorLoc, lighting.skyR, lighting.skyG, lighting.skyB)
        GLES30.glUniform3f(iFogColorLoc, lighting.fogR, lighting.fogG, lighting.fogB)
        GLES30.glUniform1f(iFogDistLoc, drawDist)

        trunkBatch.flush(cylMesh, iPosLoc, iNormLoc, iModelLoc, iColorLoc)
        pineBatch.flush(coneMesh, iPosLoc, iNormLoc, iModelLoc, iColorLoc)
        sakuraBatch.flush(sphereMesh, iPosLoc, iNormLoc, iModelLoc, iColorLoc)
        boxPropBatch.flush(cubeMesh, iPosLoc, iNormLoc, iModelLoc, iColorLoc)
    }

    private fun drawAnimatedClouds(lighting: LightingFrameConfig) {
        val cloudColor = when (sim.timeOfDay) {
            TimeOfDayMode.NIGHT -> 0xFF1E293B.toInt()
            TimeOfDayMode.SUNSET -> 0xFFFCA5A5.toInt()
            TimeOfDayMode.DAWN -> 0xFFFDE68A.toInt()
            TimeOfDayMode.NOON -> 0xFFF8FAFC.toInt()
        }
        val baseCenterX = sim.playerX
        val baseCenterZ = sim.playerZ
        for (i in 0 until 8) {
            val offsetX = -220f + ((i * 64f + sim.worldTimeSec * (3.2f + (i % 3) * 0.8f)) % 440f)
            val cz = baseCenterZ - 120f + (i % 4) * 75f
            val cx = baseCenterX + offsetX
            val cy = 58f + (i % 3) * 7f
            drawSphere(cx, cy, cz, 14.5f, cloudColor, emissive = if (lighting.isNight) 0.1f else 0.25f)
            drawSphere(cx + 8.5f, cy - 1.0f, cz + 1.5f, 10.5f, cloudColor, emissive = if (lighting.isNight) 0.08f else 0.2f)
        }
    }

    private fun renderHouseInterior(house: WorldBuilding) {
        drawBox(0f, -0.2f, 0f, 18f, 0.4f, 16f, 0xFF855E42.toInt())
        drawBox(0f, 0.02f, 0f, 10f, 0.06f, 8f, 0xFFC2C58F.toInt())
        drawBox(0f, 2.5f, -7.8f, 18f, 5f, 0.4f, 0xFFF5EFE6.toInt())
        drawBox(-8.8f, 2.5f, 0f, 0.4f, 5f, 16f, 0xFFE2E8F0.toInt())
        drawBox(8.8f, 2.5f, 0f, 0.4f, 5f, 16f, 0xFFE2E8F0.toInt())
        drawBox(0f, 2.5f, -7.5f, 7f, 3.6f, 0.15f, house.accentColor, emissive = 0.45f)
        drawBox(0f, 0.55f, -1f, 3.2f, 0.2f, 2.2f, 0xFF3E2723.toInt())
        drawBox(0f, 0.25f, -1f, 2.8f, 0.4f, 1.8f, 0xFF991B1B.toInt())
        drawBox(-5f, 0.6f, -3f, 3.6f, 1.0f, 1.6f, 0xFF1E293B.toInt())
        drawBox(5.5f, 0.65f, -4.5f, 2.2f, 1.2f, 1.4f, 0xFFB45309.toInt())
        drawBox(5.5f, 1.2f, 2.5f, 0.4f, 2.2f, 2.4f, 0xFF111827.toInt())
        drawBox(5.2f, 1.7f, 2.5f, 0.15f, 0.15f, 2.2f, 0xFF00E5FF.toInt(), emissive = 0.85f)
        drawCylinder(-4.8f, 0.18f, 3.5f, 2.0f, 0.3f, 2.0f, 0xFFFF3B5C.toInt())

        drawHeroCharacter(
            x = sim.playerX, y = sim.playerY, z = sim.playerZ, rotY = sim.playerRotY,
            stride = sim.stridePhase, crouching = sim.crouching, proning = sim.proning, recoil = sim.recoilKick
        )
        if (sim.petSummoned) {
            drawPet(sim.petX, sim.petY, sim.petZ, sim.petRotY, sim.petType, sim.petBobPhase)
        }
    }

    private fun drawGroundWeaponPickup(gw: GroundWeaponPickup) {
        val spec = GameCatalog.weapons.firstOrNull { it.id == gw.weaponId } ?: return
        val accent = spec.boltColorHex.toInt()
        val spin = (sim.worldTimeSec * 90f) % 360f
        val bobY = gw.y + 0.42f + sin(sim.worldTimeSec * 3.2f + gw.x) * 0.10f
        // Ground pickup halo disc
        drawCylinder(gw.x, gw.y + 0.06f, gw.z, 1.35f, 0.06f, 1.35f, accent, emissive = 0.85f)
        when (spec.weaponType) {
            WeaponType.KATANA -> {
                drawPart(gw.x, bobY, gw.z, 15f, spin, 72f, 0.07f, 1.35f, 0.09f, accent, emissive = 0.95f)
                drawPart(gw.x, bobY - 0.12f, gw.z, 15f, spin, 72f, 0.16f, 0.06f, 0.16f, 0xFFFFD166.toInt(), emissive = 0.6f)
            }
            else -> {
                drawPart(gw.x, bobY, gw.z, 0f, spin, 0f, 0.12f, 0.20f, 0.85f, 0xFF334155.toInt(), emissive = 0.2f)
                drawPart(gw.x, bobY + 0.04f, gw.z, 0f, spin, 0f, 0.08f, 0.08f, 0.95f, accent, emissive = 0.9f)
            }
        }
    }

    private fun drawHeroCharacter(
        x: Float,
        y: Float,
        z: Float,
        rotY: Float,
        stride: Float,
        crouching: Boolean,
        proning: Boolean = false,
        recoil: Float
    ) {
        val outfit = HeroAppearanceCatalog.resolveOutfit(sim.heroOutfitId)
        val hair = HeroAppearanceCatalog.resolveHair(sim.heroHairId)
        val accessory = HeroAppearanceCatalog.resolveAccessory(sim.heroAccessoryId)
        val weaponSkin = HeroAppearanceCatalog.resolveWeaponSkin(sim.heroWeaponSkinId)
        val backAccessory = when (accessory.id) {
            "katana" -> NpcBackAccessory.KATANA
            else -> NpcBackAccessory.NONE
        }
        drawModularAnimeCharacter(
            x = x,
            y = y,
            z = z,
            rotY = rotY,
            stride = stride,
            crouching = crouching,
            proning = proning,
            isDefeated = sim.isDefeated,
            coatSilhouette = outfit.silhouette,
            coatColor = outfit.coatColorInt,
            liningColor = outfit.liningColorInt,
            trimColor = outfit.trimColorInt,
            chestPanelColor = outfit.chestPanelColorInt,
            beltColor = outfit.beltColorInt,
            buckleColor = outfit.buckleColorInt,
            pantsColor = outfit.pantsColorInt,
            bootColor = outfit.bootColorInt,
            skinColor = if (outfit.silhouette == CoatSilhouette.BLOX_FLAME_HOODIE) 0xFFF6C69C.toInt() else 0xFFF3C6A8.toInt(),
            eyeColor = sim.accentColorInt,
            hairSilhouette = sim.heroHairSilhouette,
            hairColor = hair.colorInt,
            hairHighlightColor = hair.highlightColorInt,
            accentColor = sim.accentColorInt,
            weaponId = if (sim.showcaseMode == ShowcaseMode.CHARACTER_PREVIEW) "" else sim.activeWeaponId,
            recoil = recoil,
            backAccessory = backAccessory,
            hasFaceMask = accessory.id == "mask",
            hasEnergyWings = accessory.id == "wings",
            heightScale = 1.03f,
            buildScale = 1.0f,
            isPlayerHero = true,
            headgearId = sim.heroHeadgearId,
            weaponSkin = weaponSkin,
            expression = sim.heroExpression
        )
    }

    private fun drawNpcCharacter(npc: NpcEntity, isDefeated: Boolean) {
        val spec = npc.appearance
        drawModularAnimeCharacter(
            x = npc.x,
            y = npc.y,
            z = npc.z,
            rotY = npc.rotY,
            stride = npc.stridePhase,
            crouching = false,
            isDefeated = isDefeated,
            coatSilhouette = spec.coatSilhouette,
            coatColor = spec.coatColorInt,
            liningColor = spec.liningColorInt,
            trimColor = spec.accentColorInt,
            chestPanelColor = spec.coatColorInt,
            beltColor = 0xFF1F2433.toInt(),
            buckleColor = spec.accentColorInt,
            pantsColor = spec.pantsColorInt,
            bootColor = spec.bootColorInt,
            skinColor = spec.skinColorInt,
            eyeColor = spec.eyeColorInt,
            hairSilhouette = spec.hairStyle,
            hairColor = spec.hairColorInt,
            hairHighlightColor = if (npc.id == "npc_blox_flame") 0xFFE11D2E.toInt() else spec.hairColorInt,
            accentColor = spec.accentColorInt,
            weaponId = when {
                npc.id == "npc_blox_flame" -> ""
                npc.role == NpcRole.RIVAL_BOT -> "wpn_shadow_katana"
                npc.role == NpcRole.POLICE -> "wpn_pulse_pistol"
                npc.role == NpcRole.OFFLINE_PLAYER_PROXY -> "wpn_vortex_ar"
                else -> ""
            },
            recoil = 0f,
            backAccessory = spec.backAccessory,
            hasFaceMask = spec.hasFaceMask,
            hasEnergyWings = false, // Energy wings remain player-exclusive
            heightScale = spec.heightScale,
            buildScale = spec.buildScale,
            isPlayerHero = false,
            expression = if (npc.id == "npc_blox_flame") HeroExpression.HAPPY else HeroExpression.FIERCE
        )
    }

    private fun drawSquadShowcaseCharacter(
        x: Float,
        z: Float,
        rotY: Float,
        stride: Float,
        coatSilhouette: CoatSilhouette,
        coatColor: Int,
        liningColor: Int,
        hairSilhouette: NpcHairSilhouette,
        hairColor: Int,
        accentColor: Int,
        weaponId: String,
        backAccessory: NpcBackAccessory
    ) {
        drawModularAnimeCharacter(
            x = x,
            y = 0f,
            z = z,
            rotY = rotY,
            stride = stride,
            crouching = false,
            isDefeated = false,
            coatSilhouette = coatSilhouette,
            coatColor = coatColor,
            liningColor = liningColor,
            trimColor = accentColor,
            chestPanelColor = coatColor,
            beltColor = 0xFF1F2433.toInt(),
            buckleColor = 0xFFFFD166.toInt(),
            pantsColor = 0xFF0F172A.toInt(),
            bootColor = 0xFF090D16.toInt(),
            skinColor = 0xFFEEC09E.toInt(),
            eyeColor = accentColor,
            hairSilhouette = hairSilhouette,
            hairColor = hairColor,
            hairHighlightColor = hairColor,
            accentColor = accentColor,
            weaponId = weaponId,
            recoil = 0f,
            backAccessory = backAccessory,
            hasFaceMask = false,
            hasEnergyWings = false,
            heightScale = 1.0f,
            buildScale = 0.98f,
            isPlayerHero = false
        )
    }

    private fun drawModularAnimeCharacter(
        x: Float,
        y: Float,
        z: Float,
        rotY: Float,
        stride: Float,
        crouching: Boolean,
        proning: Boolean = false,
        isDefeated: Boolean,
        coatSilhouette: CoatSilhouette,
        coatColor: Int,
        liningColor: Int,
        trimColor: Int,
        chestPanelColor: Int,
        beltColor: Int,
        buckleColor: Int,
        pantsColor: Int,
        bootColor: Int,
        skinColor: Int,
        eyeColor: Int,
        hairSilhouette: NpcHairSilhouette,
        hairColor: Int,
        hairHighlightColor: Int,
        accentColor: Int,
        weaponId: String,
        recoil: Float,
        backAccessory: NpcBackAccessory,
        hasFaceMask: Boolean,
        hasEnergyWings: Boolean,
        heightScale: Float,
        buildScale: Float,
        isPlayerHero: Boolean = false,
        headgearId: String = "gear_none",
        weaponSkin: HeroWeaponSkinPreset = HeroAppearanceCatalog.weaponSkins.first(),
        expression: HeroExpression = HeroExpression.FIERCE
    ) {
        val rad = Math3D.degToRad(rotY)
        val fwdX = sin(rad)
        val fwdZ = cos(rad)
        val rightX = cos(rad)
        val rightZ = -sin(rad)

        val hS = if (proning) 0.42f * heightScale else if (crouching) 0.78f * heightScale else heightScale
        val bS = if (proning) buildScale * 1.12f else buildScale

        // Custom Imported .obj 3D Hero Mesh (replaces procedural rig while keeping ground aura & active firearm)
        if (isPlayerHero && customHeroMesh != null && !isDefeated) {
            val pulse = 1.0f + sin(sim.worldTimeSec * 3.2f) * 0.04f
            drawCylinder(x, y + 0.025f, z, 1.48f * pulse, 0.02f, 1.48f * pulse, accentColor, emissive = 0.85f)
            drawCylinder(x, y + 0.032f, z, 1.32f * pulse, 0.025f, 1.32f * pulse, 0xFF0A0D18.toInt())
            Math3D.buildModelMatrix(modelMatrix, x, y, z, 0f, rotY, 0f, 1f, 1f, 1f)
            drawMesh(customHeroMesh!!, coatColor, 0.10f)
            if (weaponId == "wpn_vortex_ar" || weaponId == "wpn_pulse_pistol" || weaponId == "wpn_dragon_smg") {
                val kick = recoil * 0.06f
                drawBeveled(x - rightX * 0.16f + fwdX * (0.46f - kick), y + 1.18f, z - rightZ * 0.16f + fwdZ * (0.46f - kick), 0f, rotY, 0f, 0.085f, 0.12f, 0.68f, 0xFF1E293B.toInt())
                drawCapsule(x - rightX * 0.16f + fwdX * (0.64f - kick), y + 1.21f, z - rightZ * 0.16f + fwdZ * (0.64f - kick), 90f, rotY, 0f, 0.036f, 0.44f, 0.036f, accentColor, emissive = 0.9f)
            }
            return
        }

        // Defeated character rests horizontally grounded on terrain
        if (isDefeated) {
            drawCapsule(x, y + 0.14f, z, 88f, rotY, 0f, 0.46f * bS, 0.92f * hS, 0.28f, coatColor)
            drawOrientedSphere(x + fwdX * 0.56f, y + 0.17f, z + fwdZ * 0.56f, 0f, rotY, 0f, 0.34f, 0.28f, 0.34f, skinColor)
            drawOrientedSphere(x + fwdX * 0.62f, y + 0.23f, z + fwdZ * 0.62f, 0f, rotY, 0f, 0.37f, 0.24f, 0.37f, hairColor)
            return
        }

        // Ground glow rings beneath Hero in Lobby / Studio / Combat (like Indra key visual)
        if (isPlayerHero) {
            val pulse = 1.0f + sin(sim.worldTimeSec * 3.2f) * 0.04f
            drawCylinder(x, y + 0.025f, z, 1.48f * pulse, 0.02f, 1.48f * pulse, accentColor, emissive = 0.85f)
            drawCylinder(x, y + 0.032f, z, 1.32f * pulse, 0.025f, 1.32f * pulse, 0xFF0A0D18.toInt())
            drawCylinder(x, y + 0.038f, z, 1.06f * pulse, 0.022f, 1.06f * pulse, accentColor, emissive = 0.65f)
            drawCylinder(x, y + 0.044f, z, 0.94f * pulse, 0.026f, 0.94f * pulse, 0xFF0A0D18.toInt())
        }

        val groundAtChar = if (sim.insideHouse != null) 0f else WorldMapData.terrainHeightAt(x, z)
        val isAirborne = isPlayerHero && !crouching && !proning && y > groundAtChar + 0.16f
        val breatheY = if (isPlayerHero && abs(stride) < 0.02f && !crouching && !proning && !isAirborne) {
            sin(sim.worldTimeSec * 2.4f) * 0.015f
        } else {
            0f
        }

        val legSwing = if (isAirborne) -26f else sin(stride) * 24f
        val rLegSwing = if (isAirborne) 22f else -sin(stride) * 24f
        val armSwing = if (isAirborne) 32f else -sin(stride) * 20f
        val legRad = Math3D.degToRad(legSwing)
        val rLegRad = Math3D.degToRad(rLegSwing)
        val lOffZ = if (isAirborne) 0.12f else sin(legRad) * 0.16f
        val rOffZ = if (isAirborne) -0.10f else sin(rLegRad) * 0.16f

        val isBloxStyle = coatSilhouette == CoatSilhouette.BLOX_FLAME_HOODIE

        // 1. SCULPTED ANIME HIGH-TOP SNEAKERS & CARGO LEGS
        fun drawSneakerAndLeg(sideSign: Float, swingDeg: Float, offZ: Float) {
            val sxOff = rightX * (if (isBloxStyle) 0.165f else 0.145f) * bS * sideSign
            val szOff = rightZ * (if (isBloxStyle) 0.165f else 0.145f) * bS * sideSign
            val footX = x + sxOff + fwdX * offZ
            val footZ = z + szOff + fwdZ * offZ

            if (isBloxStyle) {
                // Chunky white rubber sneaker sole + bold red upper + tilted white square emblem on toe
                drawBeveled(footX + fwdX * 0.03f, y + 0.042f * hS, footZ + fwdZ * 0.03f, swingDeg * 0.25f, rotY, 0f, 0.22f * bS, 0.085f * hS, 0.34f * bS, 0xFFF8FAFC.toInt())
                drawBeveled(footX + fwdX * 0.02f, y + 0.125f * hS, footZ + fwdZ * 0.02f, swingDeg * 0.35f, rotY, 0f, 0.21f * bS, 0.115f * hS, 0.31f * bS, bootColor)
                drawBeveled(footX + fwdX * 0.13f, y + 0.145f * hS, footZ + fwdZ * 0.13f, -22f + swingDeg * 0.35f, rotY, 45f, 0.08f * bS, 0.08f * hS, 0.035f, 0xFFF8FAFC.toInt(), emissive = 0.25f)
                drawBeveled(footX + fwdX * 0.14f, y + 0.145f * hS, footZ + fwdZ * 0.14f, -22f + swingDeg * 0.35f, rotY, 45f, 0.032f * bS, 0.032f * hS, 0.038f, bootColor)
                // Black cargo pants + prominent crimson thigh pocket flap
                val legX = x + sxOff + fwdX * offZ * 0.52f
                val legZ = z + szOff + fwdZ * offZ * 0.52f
                drawBeveled(legX, y + 0.50f * hS, legZ, swingDeg * 0.85f, rotY, 0f, 0.22f * bS, 0.66f * hS, 0.24f * bS, pantsColor)
                drawBeveled(legX + rightX * sideSign * 0.11f * bS, y + 0.58f * hS, legZ + rightZ * sideSign * 0.11f * bS, swingDeg * 0.85f, rotY, 0f, 0.055f * bS, 0.19f * hS, 0.16f * bS, liningColor, emissive = 0.18f)
            } else {
                // Clean white rubber sneaker sole + rounded toe vamp
                drawBeveled(footX + fwdX * 0.03f, y + 0.038f * hS, footZ + fwdZ * 0.03f, swingDeg * 0.25f, rotY, 0f, 0.175f * bS, 0.075f * hS, 0.29f * bS, 0xFFF8FAFC.toInt())
                // Shoe body upper + crimson sneaker highlights
                drawBeveled(footX + fwdX * 0.02f, y + 0.105f * hS, footZ + fwdZ * 0.02f, swingDeg * 0.35f, rotY, 0f, 0.165f * bS, 0.095f * hS, 0.25f * bS, bootColor)
                drawOrientedSphere(footX + fwdX * 0.11f, y + 0.092f * hS, footZ + fwdZ * 0.11f, swingDeg * 0.35f, rotY, 0f, 0.155f * bS, 0.095f * hS, 0.14f * bS, bootColor)
                // White laces/tongue patch + ankle cuff collar
                drawCapsule(footX + fwdX * 0.055f, y + 0.15f * hS, footZ + fwdZ * 0.055f, -22f + swingDeg * 0.3f, rotY, 0f, 0.11f * bS, 0.11f * hS, 0.08f * bS, 0xFFF8FAFC.toInt())
                drawCapsule(footX + fwdX * 0.005f, y + 0.21f * hS, footZ + fwdZ * 0.005f, swingDeg * 0.3f, rotY, 0f, 0.175f * bS, 0.075f * hS, 0.185f * bS, 0xFF0E1018.toInt())
                // Red sneaker stripe trim
                drawBeveled(footX + fwdX * 0.02f, y + 0.225f * hS, footZ + fwdZ * 0.02f, swingDeg * 0.3f, rotY, 0f, 0.182f * bS, 0.035f * hS, 0.195f * bS, trimColor, emissive = 0.25f)

                // Tapered calf & muscular anime cargo trouser thigh (2-segment capsule leg)
                val calfX = x + sxOff + fwdX * offZ * 0.72f
                val calfZ = z + szOff + fwdZ * offZ * 0.72f
                val thighX = x + sxOff + fwdX * offZ * 0.32f
                val thighZ = z + szOff + fwdZ * offZ * 0.32f
                drawCapsule(calfX, y + 0.40f * hS, calfZ, swingDeg * 0.85f, rotY, 0f, 0.165f * bS, 0.34f * hS, 0.175f * bS, pantsColor)
                drawCapsule(thighX, y + 0.67f * hS, thighZ, swingDeg, rotY, 0f, 0.19f * bS, 0.34f * hS, 0.20f * bS, pantsColor)
                // Outer cargo pocket detail + red tactical strap
                drawBeveled(thighX + rightX * sideSign * 0.085f * bS, y + 0.62f * hS, thighZ + rightZ * sideSign * 0.085f * bS, swingDeg, rotY, 0f, 0.055f * bS, 0.15f * hS, 0.13f * bS, 0xFF1F2433.toInt())
                drawBeveled(thighX + rightX * sideSign * 0.09f * bS, y + 0.70f * hS, thighZ + rightZ * sideSign * 0.09f * bS, swingDeg, rotY, 0f, 0.06f * bS, 0.03f * hS, 0.135f * bS, trimColor, emissive = 0.2f)
            }
        }
        drawSneakerAndLeg(1f, legSwing, lOffZ)
        drawSneakerAndLeg(-1f, rLegSwing, rOffZ)

        // 2. PELVIS, BELT, METALLIC BUCKLE, HIP CHAIN & ANIMATED CRIMSON WAIST RIBBONS
        if (isBloxStyle) {
            drawBeveled(x, y + 0.84f * hS, z, 0f, rotY, 0f, 0.46f * bS, 0.12f * hS, 0.26f * bS, beltColor)
        } else {
            drawCapsule(x, y + 0.84f * hS, z, 0f, rotY, 90f, 0.24f * bS, 0.42f * bS, 0.24f * bS, beltColor)
            drawBeveled(x + fwdX * 0.125f * bS, y + 0.84f * hS, z + fwdZ * 0.125f * bS, 0f, rotY, 0f, 0.105f * bS, 0.08f * hS, 0.055f, buckleColor, emissive = 0.35f)
        }

        // Metallic hip chain curve + flowing crimson waist ribbons (matching hero visual!)
        if (!isBloxStyle && (isPlayerHero || coatSilhouette == CoatSilhouette.LONG_TACTICAL_COAT)) {
            for (cIdx in 0..4) {
                val t = cIdx / 4f
                val cx = x + rightX * (0.18f - t * 0.11f) * bS + fwdX * (0.11f - abs(t - 0.5f) * 0.06f) * bS
                val cz = z + rightZ * (0.18f - t * 0.11f) * bS + fwdZ * (0.11f - abs(t - 0.5f) * 0.06f) * bS
                val cy = y + (0.81f - sin(t * kotlin.math.PI.toFloat()) * 0.11f) * hS
                drawSphere(cx, cy, cz, 0.032f * bS, 0xFFD8DEE9.toInt(), emissive = 0.3f)
            }
            val ribWave = sin(sim.worldTimeSec * 4.5f + stride) * 10f
            drawCapsule(
                x - rightX * 0.21f * bS - fwdX * 0.13f * bS, y + 0.58f * hS, z - rightZ * 0.21f * bS - fwdZ * 0.13f * bS,
                16f + ribWave, rotY, -16f, 0.055f * bS, 0.54f * hS, 0.025f, trimColor, emissive = 0.35f
            )
            drawCapsule(
                x + rightX * 0.21f * bS - fwdX * 0.13f * bS, y + 0.60f * hS, z + rightZ * 0.21f * bS - fwdZ * 0.13f * bS,
                14f - ribWave, rotY, 16f, 0.055f * bS, 0.48f * hS, 0.025f, liningColor, emissive = 0.3f
            )
        }

        // 3. SCULPTED TORSO & OUTFIT SILHOUETTE
        val torsoY = y + 1.15f * hS + breatheY
        if (!isBloxStyle) {
            // V-tapered athletic chest shirt / tee (white graphic inner tee on Shadow Blaze)
            drawTaperedTorso(x, torsoY, z, 0f, rotY, 0f, 0.43f * bS, 0.56f * hS, 0.255f * bS, chestPanelColor)
            // Front graphic chest emblem (crimson crest on inner shirt)
            drawBeveled(
                x + fwdX * 0.125f * bS, torsoY + 0.06f * hS, z + fwdZ * 0.125f * bS,
                0f, rotY, 45f, 0.11f * bS, 0.11f * hS, 0.03f, liningColor, emissive = 0.24f
            )
        }

        when (coatSilhouette) {
            CoatSilhouette.LONG_TACTICAL_COAT -> {
                // Sculpted back jacket shell + open contoured jacket lapels framing inner white tee
                drawTaperedTorso(x - fwdX * 0.035f * bS, torsoY + 0.01f * hS, z - fwdZ * 0.035f * bS, 0f, rotY, 0f, 0.50f * bS, 0.54f * hS, 0.22f * bS, coatColor)
                // Contoured left & right cropped/mid jacket flaps
                drawCapsule(x + rightX * 0.175f * bS + fwdX * 0.06f * bS, torsoY + 0.01f * hS, z + rightZ * 0.175f * bS + fwdZ * 0.06f * bS, 0f, rotY, -4f, 0.155f * bS, 0.52f * hS, 0.17f * bS, coatColor)
                drawCapsule(x - rightX * 0.175f * bS + fwdX * 0.06f * bS, torsoY + 0.01f * hS, z - rightZ * 0.175f * bS + fwdZ * 0.06f * bS, 0f, rotY, 4f, 0.155f * bS, 0.52f * hS, 0.17f * bS, coatColor)
                // Crimson jacket inner zipper/trim edges
                drawCapsule(x + rightX * 0.12f * bS + fwdX * 0.125f * bS, torsoY + 0.02f * hS, z + rightZ * 0.12f * bS + fwdZ * 0.125f * bS, 0f, rotY, -4f, 0.03f * bS, 0.48f * hS, 0.03f, trimColor, emissive = 0.28f)
                drawCapsule(x - rightX * 0.12f * bS + fwdX * 0.125f * bS, torsoY + 0.02f * hS, z - rightZ * 0.12f * bS + fwdZ * 0.125f * bS, 0f, rotY, 4f, 0.03f * bS, 0.48f * hS, 0.03f, trimColor, emissive = 0.28f)
                // Waist ribbed bomber hem + crimson accent
                drawCapsule(x - fwdX * 0.02f * bS, torsoY - 0.24f * hS, z - fwdZ * 0.02f * bS, 0f, rotY, 90f, 0.24f * bS, 0.47f * bS, 0.25f * bS, trimColor, emissive = 0.15f)
                // Red cloud motif emblems on back & shoulders (Indra Akatsuki street style!)
                drawOrientedSphere(x - fwdX * 0.13f * bS, torsoY + 0.06f * hS, z - fwdZ * 0.13f * bS, 0f, rotY, 0f, 0.16f * bS, 0.09f * hS, 0.04f, trimColor, emissive = 0.35f)
                drawOrientedSphere(x + rightX * 0.05f * bS - fwdX * 0.13f * bS, torsoY + 0.11f * hS, z + rightZ * 0.05f * bS - fwdZ * 0.13f * bS, 0f, rotY, 0f, 0.09f * bS, 0.07f * hS, 0.04f, trimColor, emissive = 0.35f)

                // Flared dual coattails behind hips (billowing cleanly on sprint)
                val billow = abs(sin(stride)) * 9f + 7f
                drawCapsule(x + rightX * 0.165f * bS - fwdX * 0.10f * bS, y + 0.56f * hS, z + rightZ * 0.165f * bS - fwdZ * 0.10f * bS, billow, rotY, 6f, 0.20f * bS, 0.48f * hS, 0.10f * bS, coatColor)
                drawCapsule(x - rightX * 0.165f * bS - fwdX * 0.10f * bS, y + 0.56f * hS, z - rightZ * 0.165f * bS - fwdZ * 0.10f * bS, billow, rotY, -6f, 0.20f * bS, 0.48f * hS, 0.10f * bS, coatColor)
                // Inner crimson coat lining
                drawCapsule(x - fwdX * 0.135f * bS, y + 0.54f * hS, z - fwdZ * 0.135f * bS, billow, rotY, 0f, 0.34f * bS, 0.44f * hS, 0.05f, liningColor)

                // High anime collar hugging neck with crimson inside trim
                drawCapsule(x - fwdX * 0.03f * bS, torsoY + 0.29f * hS, z - fwdZ * 0.03f * bS, 0f, rotY, 90f, 0.26f * bS, 0.42f * bS, 0.26f * bS, coatColor)
                drawCapsule(x, torsoY + 0.31f * hS, z, 0f, rotY, 90f, 0.24f * bS, 0.39f * bS, 0.24f * bS, trimColor, emissive = 0.28f)
            }
            CoatSilhouette.SHORT_JACKET -> {
                drawTaperedTorso(x, torsoY + 0.03f * hS, z, 0f, rotY, 0f, 0.51f * bS, 0.50f * hS, 0.28f * bS, coatColor)
                drawCapsule(x + fwdX * 0.135f * bS, torsoY + 0.04f * hS, z + fwdZ * 0.135f * bS, 0f, rotY, 0f, 0.08f * bS, 0.42f * hS, 0.045f, trimColor, emissive = 0.35f)
                drawCapsule(x - fwdX * 0.02f * bS, torsoY + 0.27f * hS, z - fwdZ * 0.02f * bS, 0f, rotY, 90f, 0.24f * bS, 0.38f * bS, 0.24f * bS, liningColor)
            }
            CoatSilhouette.VEST_APRON -> {
                drawTaperedTorso(x, torsoY + 0.02f * hS, z, 0f, rotY, 0f, 0.49f * bS, 0.52f * hS, 0.27f * bS, coatColor)
                drawBeveled(x + fwdX * 0.08f * bS, y + 0.66f * hS, z + fwdZ * 0.08f * bS, 0f, rotY, 0f, 0.34f * bS, 0.26f * hS, 0.10f * bS, liningColor)
            }
            CoatSilhouette.UNIFORM_BLAZER -> {
                drawTaperedTorso(x, torsoY + 0.02f * hS, z, 0f, rotY, 0f, 0.49f * bS, 0.54f * hS, 0.27f * bS, coatColor)
                drawBeveled(x + fwdX * 0.135f * bS, torsoY + 0.08f * hS, z + fwdZ * 0.135f * bS, 0f, rotY, 0f, 0.07f * bS, 0.34f * hS, 0.03f, trimColor, emissive = 0.25f)
            }
            CoatSilhouette.SHINOBI_CLOAK -> {
                drawTaperedTorso(x, torsoY + 0.02f * hS, z, 0f, rotY, 0f, 0.53f * bS, 0.54f * hS, 0.29f * bS, coatColor)
                drawCapsule(x - fwdX * 0.14f * bS, y + 0.58f * hS, z - fwdZ * 0.14f * bS, 10f, rotY, 0f, 0.48f * bS, 0.56f * hS, 0.09f * bS, coatColor)
                drawCapsule(x, torsoY + 0.28f * hS, z, 0f, rotY, 90f, 0.26f * bS, 0.42f * bS, 0.26f * bS, liningColor, emissive = 0.2f)
            }
            CoatSilhouette.BLOX_FLAME_HOODIE -> {
                // Stylized blocky black pullover hoodie shell
                drawBeveled(x, torsoY + 0.01f * hS, z, 0f, rotY, 0f, 0.54f * bS, 0.58f * hS, 0.30f * bS, coatColor)
                // Bright crimson folded hood collar & back drape
                drawCapsule(x - fwdX * 0.02f * bS, torsoY + 0.30f * hS, z - fwdZ * 0.02f * bS, 0f, rotY, 90f, 0.28f * bS, 0.48f * bS, 0.28f * bS, liningColor, emissive = 0.18f)
                drawBeveled(x - fwdX * 0.15f * bS, torsoY + 0.20f * hS, z - fwdZ * 0.15f * bS, 14f, rotY, 0f, 0.38f * bS, 0.20f * hS, 0.12f * bS, liningColor, emissive = 0.18f)
                // Hanging crimson drawstrings on front chest
                drawCapsule(x + rightX * 0.085f * bS + fwdX * 0.16f * bS, torsoY + 0.15f * hS, z + rightZ * 0.085f * bS + fwdZ * 0.16f * bS, 0f, rotY, 0f, 0.028f * bS, 0.24f * hS, 0.028f, liningColor, emissive = 0.22f)
                drawCapsule(x - rightX * 0.085f * bS + fwdX * 0.16f * bS, torsoY + 0.15f * hS, z - rightZ * 0.085f * bS + fwdZ * 0.16f * bS, 0f, rotY, 0f, 0.028f * bS, 0.24f * hS, 0.028f, liningColor, emissive = 0.22f)
                // Tilted white diamond-square emblem on chest & back (with dark center cutout)
                drawBeveled(x + fwdX * 0.158f * bS, torsoY + 0.03f * hS, z + fwdZ * 0.158f * bS, 0f, rotY, 32f, 0.17f * bS, 0.17f * hS, 0.03f, 0xFFF8FAFC.toInt(), emissive = 0.28f)
                drawBeveled(x + fwdX * 0.166f * bS, torsoY + 0.03f * hS, z + fwdZ * 0.166f * bS, 0f, rotY, 32f, 0.065f * bS, 0.065f * hS, 0.032f, coatColor)
                drawBeveled(x - fwdX * 0.158f * bS, torsoY + 0.03f * hS, z - fwdZ * 0.158f * bS, 0f, rotY, 32f, 0.16f * bS, 0.16f * hS, 0.03f, 0xFFF8FAFC.toInt(), emissive = 0.22f)
                drawBeveled(x - fwdX * 0.166f * bS, torsoY + 0.03f * hS, z - fwdZ * 0.166f * bS, 0f, rotY, 32f, 0.06f * bS, 0.06f * hS, 0.032f, coatColor)
                // Red flame spikes rising on both lower sides of hoodie
                for (side in floatArrayOf(1f, -1f)) {
                    drawOrientedCone(x + rightX * 0.23f * side * bS + fwdX * 0.08f, torsoY - 0.14f * hS, z + rightZ * 0.23f * side * bS + fwdZ * 0.08f, 0f, rotY, -12f * side, 0.10f * bS, 0.26f * hS, 0.10f * bS, liningColor, emissive = 0.35f)
                    drawOrientedCone(x + rightX * 0.19f * side * bS - fwdX * 0.06f, torsoY - 0.16f * hS, z + rightZ * 0.19f * side * bS - fwdZ * 0.06f, 0f, rotY, -6f * side, 0.08f * bS, 0.22f * hS, 0.08f * bS, 0xFFFF3B5C.toInt(), emissive = 0.35f)
                }
            }
        }

        // Optional Crimson Neck Scarf Gear
        if (headgearId == "gear_scarf") {
            drawCapsule(x, torsoY + 0.32f * hS, z, 0f, rotY, 90f, 0.28f * bS, 0.42f * bS, 0.28f * bS, 0xFFE2263A.toInt(), emissive = 0.25f)
            drawCapsule(x - rightX * 0.12f * bS - fwdX * 0.18f * bS, torsoY + 0.06f * hS, z - rightZ * 0.12f * bS - fwdZ * 0.18f * bS, 24f, rotY, -12f, 0.11f * bS, 0.48f * hS, 0.045f, 0xFFDC2626.toInt())
        }

        // Rounded / Hoodie Shoulder Deltoids
        if (!isBloxStyle) {
            drawOrientedSphere(x + rightX * 0.27f * bS, torsoY + 0.21f * hS, z + rightZ * 0.27f * bS, 0f, rotY, -12f, 0.19f * bS, 0.16f * hS, 0.20f * bS, coatColor)
            drawOrientedSphere(x - rightX * 0.27f * bS, torsoY + 0.21f * hS, z - rightZ * 0.27f * bS, 0f, rotY, 12f, 0.19f * bS, 0.16f * hS, 0.20f * bS, coatColor)
            drawOrientedSphere(x + rightX * 0.28f * bS, torsoY + 0.25f * hS, z + rightZ * 0.28f * bS, 0f, rotY, -12f, 0.16f * bS, 0.07f * hS, 0.17f * bS, trimColor, emissive = 0.32f)
            drawOrientedSphere(x - rightX * 0.28f * bS, torsoY + 0.25f * hS, z - rightZ * 0.28f * bS, 0f, rotY, 12f, 0.16f * bS, 0.07f * hS, 0.17f * bS, trimColor, emissive = 0.32f)
        }

        // Sculpted Capsule Arms / Blocky Hoodie Sleeves & Two-Handed Weapon / Punch Poses
        val sleeveColor = if (coatSilhouette == CoatSilhouette.VEST_APRON) liningColor else coatColor
        val isFirearm = weaponId == "wpn_vortex_ar" || weaponId == "wpn_pulse_pistol" || weaponId == "wpn_dragon_smg"
        val katanaSwingNorm = if (isPlayerHero && weaponId == "wpn_shadow_katana") (sim.katanaSwingTimer / 0.34f).coerceIn(0f, 1f) else 0f
        val swingProgress = 1f - katanaSwingNorm
        val punchProgress = if (isPlayerHero && weaponId == "wpn_fists" && sim.punchAnimTimer > 0f) {
            sin((sim.punchAnimTimer / 0.34f).coerceIn(0f, 1f) * kotlin.math.PI.toFloat())
        } else {
            0f
        }
        val kickBack = recoil * 0.055f

        if (isFirearm) {
            // Two-handed tactical aim grip: Right arm (-rightX) grips trigger/pistol handle; Left arm (+rightX) reaches across to cradle foregrip
            val gripX = x - rightX * 0.17f * bS + fwdX * (0.28f - kickBack)
            val gripY = torsoY + 0.05f * hS + recoil * 0.02f
            val gripZ = z - rightZ * 0.17f * bS + fwdZ * (0.28f - kickBack)

            val foreX = x - rightX * 0.12f * bS + fwdX * (0.55f - kickBack)
            val foreY = torsoY + 0.07f * hS + recoil * 0.025f
            val foreZ = z - rightZ * 0.12f * bS + fwdZ * (0.55f - kickBack)

            // Right upper arm + elbow + forearm gripping pistol handle
            val rUpperX = x - rightX * 0.28f * bS + fwdX * 0.10f
            val rUpperZ = z - rightZ * 0.28f * bS + fwdZ * 0.10f
            val rForeX = (rUpperX + gripX) * 0.5f - rightX * 0.03f
            val rForeZ = (rUpperZ + gripZ) * 0.5f - rightZ * 0.03f

            // Left upper arm + cross-chest forearm gripping barrel handguard
            val lUpperX = x + rightX * 0.27f * bS + fwdX * 0.15f
            val lUpperZ = z + rightZ * 0.27f * bS + fwdZ * 0.15f
            val lForeX = (lUpperX + foreX) * 0.5f + rightX * 0.02f
            val lForeZ = (lUpperZ + foreZ) * 0.5f + rightZ * 0.02f

            if (isBloxStyle) {
                drawBeveled(rUpperX, torsoY + 0.12f * hS, rUpperZ, -48f, rotY, -8f, 0.18f * bS, 0.26f * hS, 0.19f * bS, sleeveColor)
                drawBeveled(lUpperX, torsoY + 0.12f * hS, lUpperZ, -52f, rotY - 26f, 12f, 0.18f * bS, 0.26f * hS, 0.19f * bS, sleeveColor)
                drawBeveled(rForeX, torsoY + 0.06f * hS, rForeZ, -84f, rotY + 14f, 0f, 0.15f * bS, 0.24f * hS, 0.15f * bS, skinColor)
                drawBeveled(lForeX, torsoY + 0.06f * hS, lForeZ, -86f, rotY - 40f, 0f, 0.15f * bS, 0.28f * hS, 0.15f * bS, skinColor)
            } else {
                drawCapsule(rUpperX, torsoY + 0.12f * hS, rUpperZ, -48f, rotY, -8f, 0.14f * bS, 0.25f * hS, 0.14f * bS, sleeveColor)
                drawCapsule(lUpperX, torsoY + 0.12f * hS, lUpperZ, -52f, rotY - 28f, 12f, 0.14f * bS, 0.26f * hS, 0.14f * bS, sleeveColor)
                drawCapsule(rForeX, torsoY + 0.06f * hS, rForeZ, -84f, rotY + 14f, 0f, 0.115f * bS, 0.23f * hS, 0.115f * bS, skinColor)
                drawCapsule(lForeX, torsoY + 0.06f * hS, lForeZ, -86f, rotY - 42f, 0f, 0.115f * bS, 0.28f * hS, 0.115f * bS, skinColor)
                drawOrientedSphere(gripX, gripY, gripZ, 0f, rotY, 0f, 0.11f * bS, 0.11f * bS, 0.11f * bS, 0xFF141722.toInt())
                drawOrientedSphere(foreX, foreY, foreZ, 0f, rotY, 0f, 0.11f * bS, 0.11f * bS, 0.11f * bS, 0xFF141722.toInt())
            }

            // Detailed Firearm held symmetrically between Right Hand (grip) & Left Hand (foregrip)
            val gunCenterX = x - rightX * 0.145f * bS + fwdX * (0.44f - kickBack)
            val gunCenterY = torsoY + 0.12f * hS + recoil * 0.025f
            val gunCenterZ = z - rightZ * 0.145f * bS + fwdZ * (0.44f - kickBack)
            val isPistol = weaponId == "wpn_pulse_pistol"
            val gunLen = if (isPistol) 0.42f else 0.72f
            val barrelLen = if (isPistol) 0.28f else 0.48f

            // Receiver body + glowing energy barrel + pistol grip + magazine + optic sight
            drawBeveled(gunCenterX, gunCenterY, gunCenterZ, 0f, rotY, 0f, 0.085f, 0.115f, gunLen, 0xFF1E293B.toInt())
            drawCapsule(
                gunCenterX + fwdX * (gunLen * 0.34f), gunCenterY + 0.02f, gunCenterZ + fwdZ * (gunLen * 0.34f),
                90f, rotY, 0f, 0.038f, barrelLen, 0.038f, accentColor, emissive = 0.85f
            )
            drawBeveled(gripX, gripY - 0.02f, gripZ, 12f, rotY, 0f, 0.055f, 0.14f, 0.07f, 0xFF0F172A.toInt())
            if (!isPistol) {
                drawBeveled(foreX, foreY + 0.01f, foreZ, -6f, rotY, 0f, 0.055f, 0.11f, 0.07f, 0xFF0F172A.toInt())
                drawBeveled(gunCenterX, gunCenterY + 0.08f, gunCenterZ, 0f, rotY, 0f, 0.05f, 0.055f, 0.16f, 0xFF334155.toInt())
            }
            if (recoil > 0.62f) {
                val muzzDist = gunLen * 0.72f
                drawSphere(
                    gunCenterX + fwdX * muzzDist,
                    gunCenterY + 0.02f,
                    gunCenterZ + fwdZ * muzzDist,
                    0.18f,
                    0xFFFFD166.toInt(),
                    emissive = 1.0f
                )
            }
        } else {
            val punchLeftBoost = if (punchProgress > 0.02f && sim.punchHandSign < 0f) punchProgress else 0f
            val punchRightBoost = if (punchProgress > 0.02f && sim.punchHandSign > 0f) punchProgress else 0f

            val leftArmPitch = if (punchLeftBoost > 0f) -86f * punchLeftBoost else armSwing
            val rightArmPitch = when {
                katanaSwingNorm > 0.01f -> when (sim.katanaComboStep) {
                    1 -> -65f + swingProgress * 135f
                    2 -> 60f - swingProgress * 130f
                    else -> -25f + sin(swingProgress * kotlin.math.PI.toFloat()) * 45f
                }
                punchRightBoost > 0f -> -88f * punchRightBoost
                else -> -armSwing - recoil * 28f
            }
            val rightArmYaw = if (katanaSwingNorm > 0.01f) {
                when (sim.katanaComboStep) {
                    1 -> rotY + (-45f + swingProgress * 95f)
                    2 -> rotY + (45f - swingProgress * 95f)
                    else -> rotY + (-110f + swingProgress * 220f)
                }
            } else {
                rotY
            }

            val lPunchFwd = punchLeftBoost * 0.44f
            val rPunchFwd = punchRightBoost * 0.44f
            val lPunchLift = punchLeftBoost * 0.24f
            val rPunchLift = punchRightBoost * 0.24f

            if (isBloxStyle) {
                drawBeveled(x + rightX * 0.36f * bS + fwdX * lPunchFwd * 0.35f, torsoY + (0.09f + lPunchLift * 0.4f) * hS, z + rightZ * 0.36f * bS + fwdZ * lPunchFwd * 0.35f, leftArmPitch * 0.8f, rotY, 8f, 0.19f * bS, 0.30f * hS, 0.20f * bS, sleeveColor)
                drawBeveled(x - rightX * 0.36f * bS + fwdX * rPunchFwd * 0.35f, torsoY + (0.09f + rPunchLift * 0.4f) * hS, z - rightZ * 0.36f * bS + fwdZ * rPunchFwd * 0.35f, rightArmPitch * 0.85f, rightArmYaw, -8f, 0.19f * bS, 0.30f * hS, 0.20f * bS, sleeveColor)
                drawBeveled(x + rightX * 0.37f * bS + fwdX * lPunchFwd * 0.7f, torsoY + (-0.05f + lPunchLift * 0.7f) * hS, z + rightZ * 0.37f * bS + fwdZ * lPunchFwd * 0.7f, leftArmPitch * 0.85f, rotY, 6f, 0.198f * bS, 0.06f * hS, 0.208f * bS, liningColor, emissive = 0.2f)
                drawBeveled(x - rightX * 0.37f * bS + fwdX * rPunchFwd * 0.7f, torsoY + (-0.05f + rPunchLift * 0.7f) * hS, z - rightZ * 0.37f * bS + fwdZ * rPunchFwd * 0.7f, rightArmPitch * 0.85f, rightArmYaw, -6f, 0.198f * bS, 0.06f * hS, 0.208f * bS, liningColor, emissive = 0.2f)
                drawBeveled(x + rightX * 0.38f * bS + fwdX * lPunchFwd, torsoY + (-0.20f + lPunchLift) * hS, z + rightZ * 0.38f * bS + fwdZ * lPunchFwd, leftArmPitch, rotY, 4f, 0.17f * bS, 0.26f * hS, 0.17f * bS, skinColor)
                drawBeveled(x - rightX * 0.38f * bS + fwdX * rPunchFwd, torsoY + (-0.20f + rPunchLift) * hS, z - rightZ * 0.38f * bS + fwdZ * rPunchFwd, rightArmPitch, rightArmYaw, -4f, 0.17f * bS, 0.26f * hS, 0.17f * bS, skinColor)
            } else {
                // Upper sleeves
                drawCapsule(x + rightX * 0.31f * bS + fwdX * lPunchFwd * 0.3f, torsoY + (0.06f + lPunchLift * 0.35f) * hS, z + rightZ * 0.31f * bS + fwdZ * lPunchFwd * 0.3f, leftArmPitch * 0.8f, rotY, 8f, 0.145f * bS, 0.28f * hS, 0.145f * bS, sleeveColor)
                drawCapsule(x - rightX * 0.31f * bS + fwdX * rPunchFwd * 0.3f, torsoY + (0.06f + rPunchLift * 0.35f) * hS, z - rightZ * 0.31f * bS + fwdZ * rPunchFwd * 0.3f, rightArmPitch * 0.85f, rightArmYaw, -8f, 0.145f * bS, 0.28f * hS, 0.145f * bS, sleeveColor)
                // Forearms + wrist cuffs
                drawCapsule(x + rightX * 0.34f * bS + fwdX * lPunchFwd * 0.75f, torsoY + (-0.13f + lPunchLift * 0.8f) * hS, z + rightZ * 0.34f * bS + fwdZ * lPunchFwd * 0.75f, leftArmPitch, rotY, 4f, 0.12f * bS, 0.24f * hS, 0.12f * bS, skinColor)
                drawCapsule(x - rightX * 0.34f * bS + fwdX * rPunchFwd * 0.75f, torsoY + (-0.13f + rPunchLift * 0.8f) * hS, z - rightZ * 0.34f * bS + fwdZ * rPunchFwd * 0.75f, rightArmPitch, rightArmYaw, -4f, 0.12f * bS, 0.24f * hS, 0.12f * bS, skinColor)
                // Wristband + Natural Fingerless Combat Gloves / Clean Bare Hands
                drawCapsule(x + rightX * 0.35f * bS + fwdX * lPunchFwd * 0.88f, torsoY + (-0.21f + lPunchLift * 0.9f) * hS, z + rightZ * 0.35f * bS + fwdZ * lPunchFwd * 0.88f, leftArmPitch, rotY, 4f, 0.13f * bS, 0.06f * hS, 0.13f * bS, trimColor, emissive = 0.25f)
                drawCapsule(x - rightX * 0.35f * bS + fwdX * rPunchFwd * 0.88f, torsoY + (-0.21f + rPunchLift * 0.9f) * hS, z - rightZ * 0.35f * bS + fwdZ * rPunchFwd * 0.88f, rightArmPitch, rightArmYaw, -4f, 0.13f * bS, 0.06f * hS, 0.13f * bS, trimColor, emissive = 0.25f)
                drawOrientedSphere(x + rightX * 0.36f * bS + fwdX * lPunchFwd, torsoY + (-0.28f + lPunchLift) * hS, z + rightZ * 0.36f * bS + fwdZ * lPunchFwd, leftArmPitch, rotY, 0f, 0.115f * bS, 0.125f * hS, 0.115f * bS, if (weaponId == "wpn_fists") skinColor else 0xFF141722.toInt())
                drawOrientedSphere(x - rightX * 0.36f * bS + fwdX * rPunchFwd, torsoY + (-0.28f + rPunchLift) * hS, z - rightZ * 0.36f * bS + fwdZ * rPunchFwd, rightArmPitch, rightArmYaw, 0f, 0.115f * bS, 0.125f * hS, 0.115f * bS, if (weaponId == "wpn_fists") skinColor else 0xFF141722.toInt())
            }

            // Active Katana blade or brief punch impact aura ONLY during punch swing
            if (weaponId == "wpn_fists" && punchProgress > 0.25f) {
                val handSide = if (sim.punchHandSign > 0f) -1f else 1f
                val pFwd = maxOf(lPunchFwd, rPunchFwd)
                val pLift = maxOf(lPunchLift, rPunchLift)
                drawSphere(
                    x + rightX * 0.36f * handSide * bS + fwdX * (pFwd + 0.12f),
                    torsoY + (-0.28f + pLift) * hS,
                    z + rightZ * 0.36f * handSide * bS + fwdZ * (pFwd + 0.12f),
                    0.22f * punchProgress,
                    accentColor,
                    emissive = 0.9f
                )
            } else if (weaponId == "wpn_shadow_katana") {
                val bladeSwingRad = Math3D.degToRad(rightArmYaw)
                val bFwdX = sin(bladeSwingRad)
                val bFwdZ = cos(bladeSwingRad)
                val bladeTilt = if (katanaSwingNorm > 0.01f) {
                    when (sim.katanaComboStep) {
                        1 -> 62f
                        2 -> 42f
                        else -> 88f
                    }
                } else {
                    38f
                }
                val bladeRoll = if (katanaSwingNorm > 0.01f) {
                    when (sim.katanaComboStep) {
                        1 -> -32f
                        2 -> 32f
                        else -> 82f
                    }
                } else {
                    0f
                }
                drawCapsule(x - rightX * 0.34f * bS + bFwdX * 0.56f, torsoY - 0.02f * hS, z - rightZ * 0.34f * bS + bFwdZ * 0.56f, bladeTilt, rightArmYaw, bladeRoll, 0.045f, 1.32f, 0.068f, weaponSkin.bladeColorInt, emissive = 0.94f)
                drawCylinder(x - rightX * 0.35f * bS + bFwdX * 0.14f, torsoY - 0.22f * hS, z - rightZ * 0.35f * bS + bFwdZ * 0.14f, 0.12f, 0.03f, 0.12f, 0xFFE0AE42.toInt(), emissive = 0.4f)
                // 3D Crescent Katana Energy Slash Trail during 3-hit combo ("खाच-खाच-खाच")
                if (katanaSwingNorm > 0.05f) {
                    val arcColor = weaponSkin.bladeColorInt
                    for (seg in -3..3) {
                        val arcYaw = rightArmYaw + seg * 18f
                        val arcRad = Math3D.degToRad(arcYaw)
                        val arcRadius = 1.9f + swingProgress * 0.5f
                        val ax = x + sin(arcRad) * arcRadius
                        val az = z + cos(arcRad) * arcRadius
                        val ay = torsoY - 0.05f + when (sim.katanaComboStep) {
                            1 -> -seg * 0.12f
                            2 -> seg * 0.12f
                            else -> 0f
                        }
                        drawPart(ax, ay, az, 0f, arcYaw, 0f, 0.55f, 0.09f, 0.22f, arcColor, emissive = 1.0f)
                        drawPart(ax, ay, az, 0f, arcYaw, 0f, 0.42f, 0.045f, 0.12f, 0xFFFFFFFF.toInt(), emissive = 1.0f)
                    }
                }
            }
        }

        // 4. BACK ACCESSORIES (Detailed Diamond-Wrapped Katana, Staff, Backpack, Energy Wings)
        if (hasEnergyWings) {
            val wingFlap = sin(sim.worldTimeSec * 3.4f) * 8f
            drawCapsule(x + rightX * 0.46f * bS - fwdX * 0.26f, torsoY + 0.20f * hS, z + rightZ * 0.46f * bS - fwdZ * 0.26f, 12f, rotY - 18f, 24f + wingFlap, 0.16f, 0.92f, 0.055f, accentColor, emissive = 0.95f)
            drawCapsule(x + rightX * 0.64f * bS - fwdX * 0.32f, torsoY + 0.38f * hS, z + rightZ * 0.64f * bS - fwdZ * 0.32f, 12f, rotY - 22f, 36f + wingFlap, 0.13f, 1.08f, 0.05f, 0xFF38BDF8.toInt(), emissive = 0.95f)
            drawCapsule(x - rightX * 0.46f * bS - fwdX * 0.26f, torsoY + 0.20f * hS, z - rightZ * 0.46f * bS - fwdZ * 0.26f, 12f, rotY + 18f, -24f - wingFlap, 0.16f, 0.92f, 0.055f, accentColor, emissive = 0.95f)
            drawCapsule(x - rightX * 0.64f * bS - fwdX * 0.32f, torsoY + 0.38f * hS, z - rightZ * 0.64f * bS - fwdZ * 0.32f, 12f, rotY + 22f, -36f - wingFlap, 0.13f, 1.08f, 0.05f, 0xFF38BDF8.toInt(), emissive = 0.95f)
        } else {
            when (backAccessory) {
                NpcBackAccessory.KATANA -> {
                    // Empty scabbard when blade is unsheathed in hand; full scabbard + hilt when holstered
                    drawCapsule(x - fwdX * 0.19f * bS, torsoY + 0.08f * hS, z - fwdZ * 0.19f * bS, 0f, rotY, -34f, 0.062f, 1.24f * hS, 0.068f, weaponSkin.scabbardColorInt)
                    if (weaponId != "wpn_shadow_katana") {
                        drawCapsule(x - fwdX * 0.21f * bS, torsoY + 0.08f * hS, z - fwdZ * 0.21f * bS, 0f, rotY, -34f, 0.025f, 1.12f * hS, 0.03f, weaponSkin.bladeColorInt, emissive = 0.65f)
                        drawBeveled(x + rightX * 0.23f * bS - fwdX * 0.19f * bS, torsoY + 0.42f * hS, z + rightZ * 0.23f * bS - fwdZ * 0.19f * bS, 0f, rotY, -34f, 0.13f, 0.038f, 0.13f, 0xFFE0AE42.toInt(), emissive = 0.45f)
                        drawCapsule(x + rightX * 0.31f * bS - fwdX * 0.19f * bS, torsoY + 0.56f * hS, z + rightZ * 0.31f * bS - fwdZ * 0.19f * bS, 0f, rotY, -34f, 0.055f, 0.28f, 0.06f, 0xFF11131C.toInt())
                        for (d in -1..1) {
                            drawBeveled(
                                x + rightX * (0.31f + d * 0.045f) * bS - fwdX * 0.19f * bS,
                                torsoY + (0.56f + d * 0.062f) * hS,
                                z + rightZ * (0.31f + d * 0.045f) * bS - fwdZ * 0.19f * bS,
                                0f, rotY, -34f + 45f, 0.042f, 0.042f, 0.068f, weaponSkin.wrapColorInt, emissive = 0.5f
                            )
                        }
                    }
                }
                NpcBackAccessory.SHEATHED_SWORD -> {
                    drawCapsule(x - fwdX * 0.19f * bS, torsoY + 0.04f * hS, z - fwdZ * 0.19f * bS, 0f, rotY, 26f, 0.075f, 1.12f * hS, 0.06f, 0xFF334155.toInt())
                    drawBeveled(x - rightX * 0.20f * bS - fwdX * 0.19f * bS, torsoY + 0.48f * hS, z - rightZ * 0.20f * bS - fwdZ * 0.19f * bS, 0f, rotY, 26f, 0.16f, 0.05f, 0.09f, 0xFFFACC15.toInt(), emissive = 0.3f)
                }
                NpcBackAccessory.STAFF -> {
                    drawCapsule(x - fwdX * 0.20f * bS, torsoY + 0.12f * hS, z - fwdZ * 0.20f * bS, 0f, rotY, -22f, 0.055f, 1.48f * hS, 0.055f, 0xFF78350F.toInt())
                    drawSphere(x + rightX * 0.26f * bS - fwdX * 0.20f * bS, torsoY + 0.78f * hS, z + rightZ * 0.26f * bS - fwdZ * 0.20f * bS, 0.15f, accentColor, emissive = 0.85f)
                }
                NpcBackAccessory.BACKPACK -> {
                    drawBeveled(x - fwdX * 0.21f * bS, torsoY + 0.04f * hS, z - fwdZ * 0.21f * bS, 0f, rotY, 0f, 0.34f * bS, 0.42f * hS, 0.20f * bS, 0xFF334155.toInt())
                    drawBeveled(x - fwdX * 0.31f * bS, torsoY + 0.02f * hS, z - fwdZ * 0.31f * bS, 0f, rotY, 0f, 0.26f * bS, 0.16f * hS, 0.06f, accentColor, emissive = 0.25f)
                }
                NpcBackAccessory.NONE -> Unit
            }
        }

        // 5. HEAD & EXPRESSIVE FACE
        val neckY = y + 1.48f * hS
        val headY = y + 1.67f * hS
        val eyeY = headY + if (isBloxStyle) 0.03f else 0.012f
        drawCapsule(x, neckY, z, 0f, rotY, 0f, 0.135f * bS, 0.16f * hS, 0.135f * bS, skinColor)
        if (isBloxStyle) {
            // Stylized slightly-rounded cube head + glossy oval black cartoon eyes + cheerful smiling open mouth
            drawBeveled(x, headY + 0.01f, z, 0f, rotY, 0f, 0.38f, 0.36f, 0.35f, skinColor)
            val eyeZ = 0.175f
            for (side in floatArrayOf(1f, -1f)) {
                val ex = x + fwdX * eyeZ + rightX * 0.072f * side
                val ez = z + fwdZ * eyeZ + rightZ * 0.072f * side
                drawOrientedSphere(ex, eyeY, ez, 0f, rotY, 0f, 0.056f, 0.088f, 0.024f, 0xFF101016.toInt())
                drawSphere(ex + fwdX * 0.014f + rightX * 0.012f, eyeY + 0.022f, ez + fwdZ * 0.014f + rightZ * 0.012f, 0.020f, 0xFFFFFFFF.toInt(), emissive = 0.95f)
                drawCapsule(ex + fwdX * 0.004f, eyeY + 0.064f, ez + fwdZ * 0.004f, 0f, rotY, 90f - 10f * side, 0.020f, 0.085f, 0.02f, 0xFF15141B.toInt())
            }
            if (!hasFaceMask) {
                drawOrientedSphere(x + fwdX * 0.174f, headY - 0.075f, z + fwdZ * 0.174f, 0f, rotY, 0f, 0.105f, 0.065f, 0.024f, 0xFF7F1D1D.toInt())
                drawOrientedSphere(x + fwdX * 0.177f, headY - 0.092f, z + fwdZ * 0.177f, 0f, rotY, 0f, 0.072f, 0.032f, 0.024f, 0xFFEF4444.toInt(), emissive = 0.15f)
            }
        } else {
            // Smooth rounded cranium + tapered anime jawline/chin + ears
            drawOrientedSphere(x, headY + 0.015f, z, 0f, rotY, 0f, 0.345f, 0.355f, 0.325f, skinColor)
            drawOrientedSphere(x + fwdX * 0.032f, headY - 0.105f, z + fwdZ * 0.032f, 0f, rotY, 0f, 0.215f, 0.145f, 0.215f, skinColor)
            drawOrientedSphere(x + rightX * 0.165f, headY - 0.01f, z + rightZ * 0.165f, 0f, rotY, 0f, 0.055f, 0.095f, 0.065f, skinColor)
            drawOrientedSphere(x - rightX * 0.165f, headY - 0.01f, z - rightZ * 0.165f, 0f, rotY, 0f, 0.055f, 0.095f, 0.065f, skinColor)

            // Expressive anime eyes + irises + white catchlights + dynamic eyebrows/mouth expression
            val eyeZ = 0.155f
            val eyeScaleY = when (expression) {
                HeroExpression.HAPPY -> 0.026f
                HeroExpression.SURPRISED -> 0.072f
                else -> 0.058f
            }
            val browTilt = when (expression) {
                HeroExpression.FIERCE -> -14f
                HeroExpression.RAGE -> -24f
                HeroExpression.HAPPY -> 10f
                HeroExpression.SURPRISED -> 14f
            }
            val browLift = if (expression == HeroExpression.SURPRISED) 0.062f else 0.048f

            // Left & right eye sclera, colored iris, pupil & white anime specular highlight glint
            for (side in floatArrayOf(1f, -1f)) {
                val ex = x + fwdX * eyeZ + rightX * 0.072f * side
                val ez = z + fwdZ * eyeZ + rightZ * 0.072f * side
                drawOrientedSphere(ex, eyeY, ez, 0f, rotY, 0f, 0.078f, eyeScaleY, 0.032f, 0xFFF8FAFC.toInt(), emissive = 0.22f)
                if (expression != HeroExpression.HAPPY) {
                    drawOrientedSphere(
                        ex + fwdX * 0.012f, eyeY - 0.002f, ez + fwdZ * 0.012f,
                        0f, rotY, 0f, 0.048f, eyeScaleY * 0.92f, 0.024f, eyeColor, emissive = 0.72f
                    )
                    drawSphere(ex + fwdX * 0.020f + rightX * 0.012f, eyeY + 0.010f, ez + fwdZ * 0.020f + rightZ * 0.012f, 0.018f, 0xFFFFFFFF.toInt(), emissive = 0.95f)
                }
                // Expressive eyebrows
                drawCapsule(ex + fwdX * 0.004f, eyeY + browLift, ez + fwdZ * 0.004f, 0f, rotY, 90f + browTilt * side, 0.018f, 0.095f, 0.02f, hairColor)
            }
            // Subtle nose shadow + mouth expression
            drawOrientedSphere(x + fwdX * 0.168f, headY - 0.035f, z + fwdZ * 0.168f, 0f, rotY, 0f, 0.022f, 0.026f, 0.02f, 0xFFD2956C.toInt())
            if (!hasFaceMask) {
                when (expression) {
                    HeroExpression.FIERCE -> drawCapsule(x + fwdX * 0.155f, headY - 0.088f, z + fwdZ * 0.155f, 0f, rotY, 90f, 0.014f, 0.068f, 0.014f, 0xFF7C2D12.toInt())
                    HeroExpression.HAPPY -> drawCapsule(x + fwdX * 0.155f, headY - 0.086f, z + fwdZ * 0.155f, 0f, rotY, 90f, 0.028f, 0.085f, 0.02f, 0xFFEF4444.toInt(), emissive = 0.15f)
                    HeroExpression.RAGE -> drawOrientedSphere(x + fwdX * 0.156f, headY - 0.090f, z + fwdZ * 0.156f, 0f, rotY, 0f, 0.072f, 0.036f, 0.024f, 0xFF991B1B.toInt())
                    HeroExpression.SURPRISED -> drawOrientedSphere(x + fwdX * 0.158f, headY - 0.092f, z + fwdZ * 0.158f, 0f, rotY, 0f, 0.042f, 0.045f, 0.024f, 0xFF991B1B.toInt())
                }
            }
        }

        // Optional lower-face Ninja Respirator Mask
        if (hasFaceMask) {
            drawOrientedSphere(x + fwdX * 0.135f, headY - 0.082f, z + fwdZ * 0.135f, 0f, rotY, 0f, 0.29f, 0.155f, 0.14f, 0xFF0E1320.toInt())
            drawBeveled(x + fwdX * 0.195f, headY - 0.082f, z + fwdZ * 0.195f, 0f, rotY, 0f, 0.105f, 0.032f, 0.022f, accentColor, emissive = 0.78f)
        }

        // Optional Streetwear Headgear (Glasses / Headphones / Cap)
        when (headgearId) {
            "gear_glasses" -> {
                for (side in floatArrayOf(1f, -1f)) {
                    drawBeveled(x + fwdX * 0.175f + rightX * 0.072f * side, eyeY, z + fwdZ * 0.175f + rightZ * 0.072f * side, 0f, rotY, 0f, 0.095f, 0.055f, 0.018f, 0xFF0F172A.toInt())
                    drawBeveled(x + fwdX * 0.180f + rightX * 0.072f * side, eyeY, z + fwdZ * 0.180f + rightZ * 0.072f * side, 0f, rotY, 0f, 0.078f, 0.038f, 0.016f, accentColor, emissive = 0.55f)
                }
                drawCapsule(x + fwdX * 0.178f, eyeY + 0.01f, z + fwdZ * 0.178f, 0f, rotY, 90f, 0.015f, 0.06f, 0.015f, 0xFF1E293B.toInt())
            }
            "gear_headphones" -> {
                drawOrientedSphere(x, headY + 0.13f, z, 0f, rotY, 0f, 0.39f, 0.16f, 0.06f, 0xFF18181B.toInt())
                for (side in floatArrayOf(1f, -1f)) {
                    drawOrientedSphere(x + rightX * 0.19f * side, headY + 0.01f, z + rightZ * 0.19f * side, 0f, rotY, 0f, 0.06f, 0.14f, 0.14f, 0xFF18181B.toInt())
                    drawOrientedSphere(x + rightX * 0.21f * side, headY + 0.01f, z + rightZ * 0.21f * side, 0f, rotY, 0f, 0.03f, 0.09f, 0.09f, accentColor, emissive = 0.9f)
                }
            }
            "gear_cap" -> {
                drawDome(x, headY + 0.09f, z, 0.37f, 0.24f, 0.37f, 0xFF18181B.toInt())
                drawBeveled(x + fwdX * 0.18f, headY + 0.105f, z + fwdZ * 0.18f, 12f, rotY, 0f, 0.32f, 0.032f, 0.19f, 0xFFE2263A.toInt(), emissive = 0.2f)
            }
        }

        // 6. SCULPTED ANIME HAIR SILHOUETTE (Zero blocky cubes!)
        drawAnimeHair(
            x = x, headY = headY, z = z, rotY = rotY,
            fwdX = fwdX, fwdZ = fwdZ, rightX = rightX, rightZ = rightZ,
            silhouette = hairSilhouette,
            hairColor = hairColor,
            highlightColor = hairHighlightColor,
            accentColor = accentColor
        )
    }

    private fun drawAnimeHair(
        x: Float,
        headY: Float,
        z: Float,
        rotY: Float,
        fwdX: Float,
        fwdZ: Float,
        rightX: Float,
        rightZ: Float,
        silhouette: NpcHairSilhouette,
        hairColor: Int,
        highlightColor: Int,
        accentColor: Int
    ) {
        // Smooth rounded hair cap hugging skull + layered nape hair
        drawOrientedSphere(x - fwdX * 0.025f, headY + 0.115f, z - fwdZ * 0.025f, 0f, rotY, 0f, 0.37f, 0.22f, 0.37f, hairColor)
        drawCapsule(x - fwdX * 0.145f, headY + 0.015f, z - fwdZ * 0.145f, -12f, rotY, 0f, 0.32f, 0.26f, 0.14f, hairColor)

        when (silhouette) {
            NpcHairSilhouette.SPIKY_CROWN -> {
                // Iconic multi-layered tousled anime spikes (front bangs + temple side-locks + crown tufts + nape spikes)
                // Central crown & backward tousled tufts
                drawOrientedCone(x - fwdX * 0.02f, headY + 0.29f, z - fwdZ * 0.02f, -18f, rotY, 0f, 0.22f, 0.28f, 0.22f, hairColor)
                drawOrientedCone(x + fwdX * 0.06f + rightX * 0.08f, headY + 0.26f, z + fwdZ * 0.06f + rightZ * 0.08f, 18f, rotY, -22f, 0.14f, 0.25f, 0.14f, highlightColor)
                drawOrientedCone(x + fwdX * 0.06f - rightX * 0.08f, headY + 0.26f, z + fwdZ * 0.06f - rightZ * 0.08f, 18f, rotY, 22f, 0.14f, 0.25f, 0.14f, hairColor)
                // Wispy tousled bangs fanning downward across forehead
                drawOrientedCone(x + fwdX * 0.155f, headY + 0.095f, z + fwdZ * 0.155f, 152f, rotY, 0f, 0.11f, 0.19f, 0.09f, hairColor)
                drawOrientedCone(x + fwdX * 0.145f + rightX * 0.085f, headY + 0.08f, z + fwdZ * 0.145f + rightZ * 0.085f, 148f, rotY, -18f, 0.10f, 0.20f, 0.09f, highlightColor)
                drawOrientedCone(x + fwdX * 0.145f - rightX * 0.085f, headY + 0.08f, z + fwdZ * 0.145f - rightZ * 0.085f, 148f, rotY, 18f, 0.10f, 0.20f, 0.09f, hairColor)
                // Side temple locks framing anime cheeks
                drawOrientedCone(x + rightX * 0.175f + fwdX * 0.05f, headY + 0.03f, z + rightZ * 0.175f + fwdZ * 0.05f, 162f, rotY, -16f, 0.095f, 0.23f, 0.095f, hairColor)
                drawOrientedCone(x - rightX * 0.175f + fwdX * 0.05f, headY + 0.03f, z - rightZ * 0.175f + fwdZ * 0.05f, 162f, rotY, 16f, 0.095f, 0.23f, 0.095f, highlightColor)
                // Lateral & back messy anime spikes
                drawOrientedCone(x + rightX * 0.18f, headY + 0.20f, z + rightZ * 0.18f, 0f, rotY, -52f, 0.13f, 0.24f, 0.13f, hairColor)
                drawOrientedCone(x - rightX * 0.18f, headY + 0.20f, z - rightZ * 0.18f, 0f, rotY, 52f, 0.13f, 0.24f, 0.13f, highlightColor)
                drawOrientedCone(x - fwdX * 0.17f + rightX * 0.10f, headY + 0.20f, z - fwdZ * 0.17f + rightZ * 0.10f, -48f, rotY, -20f, 0.14f, 0.26f, 0.14f, hairColor)
                drawOrientedCone(x - fwdX * 0.17f - rightX * 0.10f, headY + 0.20f, z - fwdZ * 0.17f - rightZ * 0.10f, -48f, rotY, 20f, 0.14f, 0.26f, 0.14f, hairColor)
            }
            NpcHairSilhouette.SWEPT_SIDE -> {
                // Sleek asymmetrical wolf-cut bangs + sweeping locks
                drawOrientedCone(x + fwdX * 0.145f + rightX * 0.06f, headY + 0.09f, z + fwdZ * 0.145f + rightZ * 0.06f, 145f, rotY, -28f, 0.14f, 0.24f, 0.09f, highlightColor)
                drawOrientedCone(x + fwdX * 0.15f - rightX * 0.07f, headY + 0.07f, z + fwdZ * 0.15f - rightZ * 0.07f, 150f, rotY, 24f, 0.12f, 0.23f, 0.09f, hairColor)
                drawOrientedCone(x + rightX * 0.17f, headY + 0.02f, z + rightZ * 0.17f, 165f, rotY, -14f, 0.10f, 0.25f, 0.10f, hairColor)
                drawOrientedCone(x - fwdX * 0.19f, headY + 0.12f, z - fwdZ * 0.19f, -55f, rotY, 0f, 0.18f, 0.28f, 0.14f, hairColor)
            }
            NpcHairSilhouette.SHORT_TACTICAL -> {
                drawOrientedSphere(x, headY + 0.16f, z, 0f, rotY, 0f, 0.34f, 0.16f, 0.34f, highlightColor)
                drawOrientedCone(x + fwdX * 0.14f, headY + 0.10f, z + fwdZ * 0.14f, 138f, rotY, 0f, 0.18f, 0.15f, 0.09f, hairColor)
                drawOrientedCone(x + rightX * 0.15f, headY + 0.06f, z + rightZ * 0.15f, 165f, rotY, -12f, 0.08f, 0.16f, 0.08f, hairColor)
                drawOrientedCone(x - rightX * 0.15f, headY + 0.06f, z - rightZ * 0.15f, 165f, rotY, 12f, 0.08f, 0.16f, 0.08f, hairColor)
            }
            NpcHairSilhouette.TOP_KNOT -> {
                drawOrientedCone(x - fwdX * 0.06f, headY + 0.30f, z - fwdZ * 0.06f, -15f, rotY, 0f, 0.15f, 0.22f, 0.15f, hairColor)
                drawCylinder(x, headY + 0.15f, z, 0.37f, 0.045f, 0.37f, accentColor, emissive = 0.25f)
            }
            NpcHairSilhouette.LONG_LAYERED -> {
                // Smooth shoulder-length layered anime hair curtain
                drawCapsule(x - fwdX * 0.17f, headY - 0.11f, z - fwdZ * 0.17f, 8f, rotY, 0f, 0.36f, 0.48f, 0.12f, hairColor)
                drawCapsule(x + rightX * 0.17f + fwdX * 0.03f, headY - 0.04f, z + rightZ * 0.17f + fwdZ * 0.03f, 6f, rotY, -8f, 0.09f, 0.36f, 0.13f, highlightColor)
                drawCapsule(x - rightX * 0.17f + fwdX * 0.03f, headY - 0.04f, z - rightZ * 0.17f + fwdZ * 0.03f, 6f, rotY, 8f, 0.09f, 0.36f, 0.13f, highlightColor)
                drawOrientedCone(x + fwdX * 0.15f, headY + 0.08f, z + fwdZ * 0.15f, 150f, rotY, 0f, 0.12f, 0.18f, 0.08f, hairColor)
            }
        }
    }

    private fun drawVehicle(
        x: Float,
        y: Float,
        z: Float,
        rotY: Float,
        category: String,
        colorInt: Int,
        wheelAngle: Float,
        isNight: Boolean
    ) {
        val rad = Math3D.degToRad(rotY)
        val fwdX = sin(rad)
        val fwdZ = cos(rad)
        val rightX = cos(rad)
        val rightZ = -sin(rad)
        val headGlow = if (isNight) 1.0f else 0.55f

        when (category) {
            "helicopter" -> {
                drawPart(x, y + 1.4f, z, 0f, rotY, 0f, 2.0f, 1.6f, 4.6f, colorInt)
                drawPart(x - fwdX * 3.0f, y + 1.5f, z - fwdZ * 3.0f, 0f, rotY, 0f, 0.5f, 0.5f, 3.2f, 0xFF1E293B.toInt())
                drawPart(x + fwdX * 1.2f, y + 1.5f, z + fwdZ * 1.2f, 0f, rotY, 0f, 1.7f, 0.9f, 1.8f, 0xFF38BDF8.toInt(), emissive = 0.3f)
                val rotorRot = (sim.worldTimeSec * 480f) % 360f
                drawPart(x, y + 2.45f, z, 0f, rotorRot, 0f, 7.6f, 0.1f, 0.35f, 0xFFCBD5E1.toInt())
                drawPart(x, y + 2.45f, z, 0f, rotorRot + 90f, 0f, 7.6f, 0.1f, 0.35f, 0xFFCBD5E1.toInt())
            }
            "motorcycle" -> {
                drawPart(x, y + 0.72f, z, 0f, rotY, 0f, 0.65f, 0.62f, 2.2f, colorInt)
                drawPart(x + fwdX * 0.5f, y + 1.05f, z + fwdZ * 0.5f, 0f, rotY, 0f, 0.9f, 0.2f, 0.25f, 0xFF00E5FF.toInt(), emissive = 0.6f)
                drawWheel(x + fwdX * 0.85f, y + 0.36f, z + fwdZ * 0.85f, rotY, wheelAngle, 0.72f)
                drawWheel(x - fwdX * 0.85f, y + 0.36f, z - fwdZ * 0.85f, rotY, wheelAngle, 0.72f)
            }
            else -> {
                val isSuv = category == "suv" || category == "police"
                val bodyH = if (isSuv) 0.95f else 0.68f
                val bodyW = if (isSuv) 2.2f else 2.15f
                val bodyL = 4.4f
                drawPart(x, y + 0.65f, z, 0f, rotY, 0f, bodyW, bodyH, bodyL, colorInt)
                drawPart(x - fwdX * 0.25f, y + 0.65f + bodyH * 0.75f, z - fwdZ * 0.25f, 0f, rotY, 0f, bodyW * 0.82f, 0.68f, 2.2f, 0xFF0F172A.toInt())
                if (category == "supercar") {
                    drawPart(x - fwdX * 1.9f, y + 1.25f, z - fwdZ * 1.9f, 0f, rotY, 0f, bodyW, 0.14f, 0.45f, 0xFF111827.toInt())
                }
                if (category == "police") {
                    val flashRed = ((sim.worldTimeSec * 6f).toInt() % 2 == 0)
                    drawPart(x + rightX * 0.4f, y + 1.78f, z + rightZ * 0.4f, 0f, rotY, 0f, 0.6f, 0.22f, 0.3f, 0xFFEF4444.toInt(), emissive = if (flashRed) 1f else 0.2f)
                    drawPart(x - rightX * 0.4f, y + 1.78f, z - rightZ * 0.4f, 0f, rotY, 0f, 0.6f, 0.22f, 0.3f, 0xFF3B82F6.toInt(), emissive = if (!flashRed) 1f else 0.2f)
                }
                drawPart(x + fwdX * 2.22f + rightX * 0.7f, y + 0.68f, z + fwdZ * 2.22f + rightZ * 0.7f, 0f, rotY, 0f, 0.36f, 0.2f, 0.1f, 0xFFFEF08A.toInt(), emissive = headGlow)
                drawPart(x + fwdX * 2.22f - rightX * 0.7f, y + 0.68f, z + fwdZ * 2.22f - rightZ * 0.7f, 0f, rotY, 0f, 0.36f, 0.2f, 0.1f, 0xFFFEF08A.toInt(), emissive = headGlow)
                drawPart(x - fwdX * 2.22f, y + 0.72f, z - fwdZ * 2.22f, 0f, rotY, 0f, 1.7f, 0.16f, 0.1f, 0xFFFF1E42.toInt(), emissive = 0.85f)
                drawWheel(x + fwdX * 1.35f + rightX * 1.05f, y + 0.38f, z + fwdZ * 1.35f + rightZ * 1.05f, rotY, wheelAngle, 0.76f)
                drawWheel(x + fwdX * 1.35f - rightX * 1.05f, y + 0.38f, z + fwdZ * 1.35f - rightZ * 1.05f, rotY, wheelAngle, 0.76f)
                drawWheel(x - fwdX * 1.35f + rightX * 1.05f, y + 0.38f, z - fwdZ * 1.35f + rightZ * 1.05f, rotY, wheelAngle, 0.76f)
                drawWheel(x - fwdX * 1.35f - rightX * 1.05f, y + 0.38f, z - fwdZ * 1.35f - rightZ * 1.05f, rotY, wheelAngle, 0.76f)
            }
        }
    }

    private fun drawWheel(x: Float, y: Float, z: Float, rotY: Float, spinDeg: Float, size: Float) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.rotateM(modelMatrix, 0, rotY, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, 90f, 0f, 0f, 1f)
        Matrix.rotateM(modelMatrix, 0, spinDeg, 0f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, size, 0.28f, size)
        drawMesh(cylMesh, 0xFF0F172A.toInt(), 0f)
    }

    override fun petPart(x: Float, y: Float, z: Float, rx: Float, ry: Float, rz: Float, sx: Float, sy: Float, sz: Float, colorInt: Int, emissive: Float) {
        drawPart(x, y, z, rx, ry, rz, sx, sy, sz, colorInt, emissive)
    }

    override fun petSphere(x: Float, y: Float, z: Float, diameter: Float, colorInt: Int, emissive: Float) {
        drawSphere(x, y, z, diameter, colorInt, emissive)
    }

    override fun petDome(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, colorInt: Int, emissive: Float) {
        drawDome(x, y, z, sx, sy, sz, colorInt, emissive)
    }

    override fun petCyl(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, colorInt: Int, emissive: Float) {
        drawCylinder(x, y, z, sx, sy, sz, colorInt, emissive)
    }

    override fun petCone(x: Float, y: Float, z: Float, rx: Float, ry: Float, rz: Float, sx: Float, sy: Float, sz: Float, colorInt: Int, emissive: Float) {
        drawOrientedCone(x, y, z, rx, ry, rz, sx, sy, sz, colorInt, emissive)
    }

    private fun drawPet(x: Float, y: Float, z: Float, rotY: Float, petType: String, bobPhase: Float) {
        PetModelRenderer.drawAnimePet(
            drawer = this,
            x = x,
            y = y,
            z = z,
            rotY = rotY,
            petTypeOrId = petType,
            bobPhase = bobPhase,
            evolutionLevel = sim.petEvolutionLevel,
            customTintHex = sim.petCustomTintHex,
            auraStyle = sim.petAuraStyle,
            worldTimeSec = sim.worldTimeSec
        )
    }

    private fun drawBox(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, colorInt: Int, emissive: Float = 0f) {
        Math3D.buildModelMatrix(modelMatrix, x, y, z, 0f, 0f, 0f, sx, sy, sz)
        drawMesh(cubeMesh, colorInt, emissive)
    }

    private fun drawPart(x: Float, y: Float, z: Float, rx: Float, ry: Float, rz: Float, sx: Float, sy: Float, sz: Float, colorInt: Int, emissive: Float = 0f) {
        Math3D.buildModelMatrix(modelMatrix, x, y, z, rx, ry, rz, sx, sy, sz)
        drawMesh(cubeMesh, colorInt, emissive)
    }

    private fun drawCylinder(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, colorInt: Int, emissive: Float = 0f) {
        Math3D.buildModelMatrix(modelMatrix, x, y, z, 0f, 0f, 0f, sx, sy, sz)
        drawMesh(cylMesh, colorInt, emissive)
    }

    private fun drawCone(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, colorInt: Int, emissive: Float = 0f) {
        Math3D.buildModelMatrix(modelMatrix, x, y, z, 0f, 0f, 0f, sx, sy, sz)
        drawMesh(coneMesh, colorInt, emissive)
    }

    private fun drawOrientedCone(x: Float, y: Float, z: Float, rx: Float, ry: Float, rz: Float, sx: Float, sy: Float, sz: Float, colorInt: Int, emissive: Float = 0f) {
        Math3D.buildModelMatrix(modelMatrix, x, y, z, rx, ry, rz, sx, sy, sz)
        drawMesh(coneMesh, colorInt, emissive)
    }

    private fun drawDome(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, colorInt: Int, emissive: Float = 0f) {
        Math3D.buildModelMatrix(modelMatrix, x, y, z, 0f, 0f, 0f, sx, sy, sz)
        drawMesh(domeMesh, colorInt, emissive)
    }

    private fun drawSphere(x: Float, y: Float, z: Float, diameter: Float, colorInt: Int, emissive: Float = 0f) {
        Math3D.buildModelMatrix(modelMatrix, x, y, z, 0f, 0f, 0f, diameter, diameter, diameter)
        drawMesh(sphereMesh, colorInt, emissive)
    }

    private fun drawOrientedSphere(x: Float, y: Float, z: Float, rx: Float, ry: Float, rz: Float, sx: Float, sy: Float, sz: Float, colorInt: Int, emissive: Float = 0f) {
        Math3D.buildModelMatrix(modelMatrix, x, y, z, rx, ry, rz, sx, sy, sz)
        drawMesh(sphereMesh, colorInt, emissive)
    }

    private fun drawCapsule(x: Float, y: Float, z: Float, rx: Float, ry: Float, rz: Float, sx: Float, sy: Float, sz: Float, colorInt: Int, emissive: Float = 0f) {
        Math3D.buildModelMatrix(modelMatrix, x, y, z, rx, ry, rz, sx, sy, sz)
        drawMesh(capsuleMesh, colorInt, emissive)
    }

    private fun drawTaperedTorso(x: Float, y: Float, z: Float, rx: Float, ry: Float, rz: Float, sx: Float, sy: Float, sz: Float, colorInt: Int, emissive: Float = 0f) {
        Math3D.buildModelMatrix(modelMatrix, x, y, z, rx, ry, rz, sx, sy, sz)
        drawMesh(taperedTorsoMesh, colorInt, emissive)
    }

    private fun drawBeveled(x: Float, y: Float, z: Float, rx: Float, ry: Float, rz: Float, sx: Float, sy: Float, sz: Float, colorInt: Int, emissive: Float = 0f) {
        Math3D.buildModelMatrix(modelMatrix, x, y, z, rx, ry, rz, sx, sy, sz)
        drawMesh(beveledBoxMesh, colorInt, emissive)
    }

    private fun drawMesh(mesh: Mesh, colorInt: Int, emissive: Float) {
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(uMvpLoc, 1, false, mvpMatrix, 0)
        GLES30.glUniformMatrix4fv(uModelLoc, 1, false, modelMatrix, 0)
        GLES30.glUniform3f(
            uColorLoc,
            Color.red(colorInt) / 255f,
            Color.green(colorInt) / 255f,
            Color.blue(colorInt) / 255f
        )
        GLES30.glUniform1f(uEmissiveLoc, emissive)
        mesh.draw(aPosLoc, aNormLoc)
    }

    fun release() {
        customHeroMesh?.release()
        customHeroMesh = null
        if (::cubeMesh.isInitialized) cubeMesh.release()
        if (::cylMesh.isInitialized) cylMesh.release()
        if (::coneMesh.isInitialized) coneMesh.release()
        if (::domeMesh.isInitialized) domeMesh.release()
        if (::sphereMesh.isInitialized) sphereMesh.release()
        if (::capsuleMesh.isInitialized) capsuleMesh.release()
        if (::taperedTorsoMesh.isInitialized) taperedTorsoMesh.release()
        if (::beveledBoxMesh.isInitialized) beveledBoxMesh.release()
        if (::trunkBatch.isInitialized) trunkBatch.release()
        if (::pineBatch.isInitialized) pineBatch.release()
        if (::sakuraBatch.isInitialized) sakuraBatch.release()
        if (::boxPropBatch.isInitialized) boxPropBatch.release()
        textureAtlas.release()
        if (program != 0) {
            GLES30.glDeleteProgram(program)
            program = 0
        }
        if (instProgram != 0) {
            GLES30.glDeleteProgram(instProgram)
            instProgram = 0
        }
    }

    private fun createProgram(vs: String, fs: String): Int {
        val vShader = compileShader(GLES30.GL_VERTEX_SHADER, vs)
        val fShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fs)
        val prog = GLES30.glCreateProgram()
        GLES30.glAttachShader(prog, vShader)
        GLES30.glAttachShader(prog, fShader)
        GLES30.glLinkProgram(prog)
        GLES30.glDeleteShader(vShader)
        GLES30.glDeleteShader(fShader)
        return prog
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, src)
        GLES30.glCompileShader(shader)
        return shader
    }

    companion object {
        private const val VERT_SHADER = """#version 300 es
            layout(location = 0) in vec3 aPos;
            layout(location = 1) in vec3 aNorm;
            uniform mat4 uMvp;
            uniform mat4 uModel;
            out vec3 vWorldNorm;
            out vec3 vWorldPos;
            out float vDepth;
            void main() {
                vec4 worldPos = uModel * vec4(aPos, 1.0);
                vWorldPos = worldPos.xyz;
                vWorldNorm = normalize(mat3(uModel) * aNorm);
                vec4 clipPos = uMvp * vec4(aPos, 1.0);
                vDepth = clipPos.w;
                gl_Position = clipPos;
            }
        """

        private const val FRAG_SHADER = """#version 300 es
            precision mediump float;
            in vec3 vWorldNorm;
            in vec3 vWorldPos;
            in float vDepth;
            uniform vec3 uColor;
            uniform vec3 uLightDir;
            uniform vec3 uSkyColor;
            uniform vec3 uFogColor;
            uniform float uFogDist;
            uniform float uEmissive;
            uniform float uTexMix;
            uniform sampler2D uDetailTex;
            out vec4 fragColor;
            void main() {
                vec3 n = normalize(vWorldNorm);
                vec3 l = normalize(uLightDir);
                float diff = max(dot(n, l), 0.0);
                float hemi = 0.38 + 0.25 * n.y;
                vec2 uv = (abs(n.y) > 0.5) ? vWorldPos.xz * 0.045 : vec2(vWorldPos.x + vWorldPos.z, vWorldPos.y) * 0.08;
                float detail = texture(uDetailTex, uv).r;
                vec3 baseCol = uColor * mix(1.0, detail * 1.15, uTexMix);
                vec3 lit = baseCol * (hemi + diff * 0.78 + uSkyColor * 0.14);
                vec3 finalRgb = mix(lit, baseCol * 1.25, uEmissive);
                float fogFactor = clamp(vDepth / max(uFogDist, 80.0), 0.0, 0.88);
                finalRgb = mix(finalRgb, uFogColor, fogFactor * fogFactor);
                fragColor = vec4(finalRgb, 1.0);
            }
        """

        private const val INST_VERT_SHADER = """#version 300 es
            layout(location = 0) in vec3 aPos;
            layout(location = 1) in vec3 aNorm;
            layout(location = 2) in mat4 aInstModel;
            layout(location = 6) in vec4 aInstColor;
            uniform mat4 uVp;
            out vec3 vWorldNorm;
            out vec3 vWorldPos;
            out vec4 vInstColor;
            out float vDepth;
            void main() {
                vec4 worldPos = aInstModel * vec4(aPos, 1.0);
                vWorldPos = worldPos.xyz;
                vWorldNorm = normalize(mat3(aInstModel) * aNorm);
                vInstColor = aInstColor;
                vec4 clipPos = uVp * worldPos;
                vDepth = clipPos.w;
                gl_Position = clipPos;
            }
        """

        private const val INST_FRAG_SHADER = """#version 300 es
            precision mediump float;
            in vec3 vWorldNorm;
            in vec3 vWorldPos;
            in vec4 vInstColor;
            in float vDepth;
            uniform vec3 uLightDir;
            uniform vec3 uSkyColor;
            uniform vec3 uFogColor;
            uniform float uFogDist;
            uniform sampler2D uDetailTex;
            out vec4 fragColor;
            void main() {
                vec3 n = normalize(vWorldNorm);
                vec3 l = normalize(uLightDir);
                float diff = max(dot(n, l), 0.0);
                float hemi = 0.38 + 0.25 * n.y;
                float detail = texture(uDetailTex, vWorldPos.xz * 0.06).r;
                vec3 baseCol = vInstColor.rgb * mix(1.0, detail * 1.1, 0.12);
                vec3 lit = baseCol * (hemi + diff * 0.78 + uSkyColor * 0.14);
                vec3 finalRgb = mix(lit, baseCol * 1.25, vInstColor.a);
                float fogFactor = clamp(vDepth / max(uFogDist, 80.0), 0.0, 0.88);
                finalRgb = mix(finalRgb, uFogColor, fogFactor * fogFactor);
                fragColor = vec4(finalRgb, 1.0);
            }
        """
    }
}
