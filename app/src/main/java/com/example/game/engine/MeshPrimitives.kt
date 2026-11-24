package com.example.game.engine

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class Mesh(vertices: FloatArray) {
    val vertexCount: Int = vertices.size / 6
    private var vboId: Int

    init {
        val ids = IntArray(1)
        GLES30.glGenBuffers(1, ids, 0)
        vboId = ids[0]
        val buffer: FloatBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        buffer.position(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.size * 4, buffer, GLES30.GL_STATIC_DRAW)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    fun draw(posLoc: Int, normLoc: Int) {
        if (vboId == 0) return
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        val stride = 6 * 4
        GLES30.glEnableVertexAttribArray(posLoc)
        GLES30.glVertexAttribPointer(posLoc, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(normLoc)
        GLES30.glVertexAttribPointer(normLoc, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, vertexCount)
    }

    fun drawInstanced(
        posLoc: Int,
        normLoc: Int,
        instanceModelLoc: Int,
        instanceColorLoc: Int,
        instanceVboId: Int,
        instanceCount: Int
    ) {
        if (vboId == 0 || instanceVboId == 0 || instanceCount <= 0) return
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        val vertStride = 6 * 4
        GLES30.glEnableVertexAttribArray(posLoc)
        GLES30.glVertexAttribPointer(posLoc, 3, GLES30.GL_FLOAT, false, vertStride, 0)
        GLES30.glVertexAttribDivisor(posLoc, 0)
        GLES30.glEnableVertexAttribArray(normLoc)
        GLES30.glVertexAttribPointer(normLoc, 3, GLES30.GL_FLOAT, false, vertStride, 3 * 4)
        GLES30.glVertexAttribDivisor(normLoc, 0)

        // Instance buffer layout: mat4 (16 floats) + vec4 (r, g, b, emissive) = 20 floats = 80 bytes
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, instanceVboId)
        val instStride = 20 * 4
        for (col in 0 until 4) {
            val loc = instanceModelLoc + col
            GLES30.glEnableVertexAttribArray(loc)
            GLES30.glVertexAttribPointer(loc, 4, GLES30.GL_FLOAT, false, instStride, col * 16)
            GLES30.glVertexAttribDivisor(loc, 1)
        }
        GLES30.glEnableVertexAttribArray(instanceColorLoc)
        GLES30.glVertexAttribPointer(instanceColorLoc, 4, GLES30.GL_FLOAT, false, instStride, 16 * 4)
        GLES30.glVertexAttribDivisor(instanceColorLoc, 1)

        GLES30.glDrawArraysInstanced(GLES30.GL_TRIANGLES, 0, vertexCount, instanceCount)

        // Reset divisors cleanly so regular non-instanced draws stay intact
        for (col in 0 until 4) {
            GLES30.glDisableVertexAttribArray(instanceModelLoc + col)
            GLES30.glVertexAttribDivisor(instanceModelLoc + col, 0)
        }
        GLES30.glDisableVertexAttribArray(instanceColorLoc)
        GLES30.glVertexAttribDivisor(instanceColorLoc, 0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    fun release() {
        if (vboId != 0) {
            GLES30.glDeleteBuffers(1, intArrayOf(vboId), 0)
            vboId = 0
        }
    }
}

/**
 * Reusable pre-allocated GPU instance buffer for instanced rendering (trees, streetlights, dashes, rocks).
 * Avoids any per-frame heap allocation.
 */
class InstancedMeshBatch(private val maxInstances: Int = 512) {
    private val data = FloatArray(maxInstances * 20)
    private val floatBuf: FloatBuffer = ByteBuffer.allocateDirect(data.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
    private var instanceVboId: Int = 0
    var count: Int = 0
        private set

    init {
        val ids = IntArray(1)
        GLES30.glGenBuffers(1, ids, 0)
        instanceVboId = ids[0]
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, instanceVboId)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, data.size * 4, null, GLES30.GL_DYNAMIC_DRAW)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    fun clear() {
        count = 0
    }

    fun addInstance(modelMatrix: FloatArray, r: Float, g: Float, b: Float, emissive: Float) {
        if (count >= maxInstances) return
        val base = count * 20
        System.arraycopy(modelMatrix, 0, data, base, 16)
        data[base + 16] = r
        data[base + 17] = g
        data[base + 18] = b
        data[base + 19] = emissive
        count++
    }

    fun flush(
        mesh: Mesh,
        posLoc: Int,
        normLoc: Int,
        instanceModelLoc: Int,
        instanceColorLoc: Int
    ) {
        if (count <= 0 || instanceVboId == 0) return
        floatBuf.position(0)
        floatBuf.put(data, 0, count * 20)
        floatBuf.position(0)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, instanceVboId)
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, count * 20 * 4, floatBuf)
        mesh.drawInstanced(posLoc, normLoc, instanceModelLoc, instanceColorLoc, instanceVboId, count)
        count = 0
    }

    fun release() {
        if (instanceVboId != 0) {
            GLES30.glDeleteBuffers(1, intArrayOf(instanceVboId), 0)
            instanceVboId = 0
        }
    }
}

/**
 * Generates procedural tileable 64x64 OpenGL textures for surface detail variation (no external image I/O stalls).
 */
class WorldTextureAtlas {
    var detailTexId: Int = 0
        private set

    fun init() {
        val ids = IntArray(1)
        GLES30.glGenTextures(1, ids, 0)
        detailTexId = ids[0]

        val size = 64
        val pixels = ByteBuffer.allocateDirect(size * size * 4)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val hash = ((x * 73856093) xor (y * 19349663)) and 0xFF
                val checker = ((x / 8 + y / 8) and 1) * 14
                val shade = (212 + (hash % 30) - checker).coerceIn(170, 255).toByte()
                val gridLine = if (x % 16 == 0 || y % 16 == 0) (200).toByte() else shade
                pixels.put(gridLine)
                pixels.put(shade)
                pixels.put(shade)
                pixels.put(255.toByte())
            }
        }
        pixels.position(0)

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, detailTexId)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT)
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, size, size, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, pixels)
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
    }

    fun bind(unit: Int = 0) {
        if (detailTexId == 0) return
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + unit)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, detailTexId)
    }

    fun release() {
        if (detailTexId != 0) {
            GLES30.glDeleteTextures(1, intArrayOf(detailTexId), 0)
            detailTexId = 0
        }
    }
}

object MeshPrimitives {

    fun createUnitCube(): Mesh {
        val data = floatArrayOf(
            // Front (+Z)
            -0.5f, -0.5f,  0.5f,  0f, 0f, 1f,
             0.5f, -0.5f,  0.5f,  0f, 0f, 1f,
             0.5f,  0.5f,  0.5f,  0f, 0f, 1f,
            -0.5f, -0.5f,  0.5f,  0f, 0f, 1f,
             0.5f,  0.5f,  0.5f,  0f, 0f, 1f,
            -0.5f,  0.5f,  0.5f,  0f, 0f, 1f,
            // Back (-Z)
             0.5f, -0.5f, -0.5f,  0f, 0f, -1f,
            -0.5f, -0.5f, -0.5f,  0f, 0f, -1f,
            -0.5f,  0.5f, -0.5f,  0f, 0f, -1f,
             0.5f, -0.5f, -0.5f,  0f, 0f, -1f,
            -0.5f,  0.5f, -0.5f,  0f, 0f, -1f,
             0.5f,  0.5f, -0.5f,  0f, 0f, -1f,
            // Left (-X)
            -0.5f, -0.5f, -0.5f, -1f, 0f, 0f,
            -0.5f, -0.5f,  0.5f, -1f, 0f, 0f,
            -0.5f,  0.5f,  0.5f, -1f, 0f, 0f,
            -0.5f, -0.5f, -0.5f, -1f, 0f, 0f,
            -0.5f,  0.5f,  0.5f, -1f, 0f, 0f,
            -0.5f,  0.5f, -0.5f, -1f, 0f, 0f,
            // Right (+X)
             0.5f, -0.5f,  0.5f,  1f, 0f, 0f,
             0.5f, -0.5f, -0.5f,  1f, 0f, 0f,
             0.5f,  0.5f, -0.5f,  1f, 0f, 0f,
             0.5f, -0.5f,  0.5f,  1f, 0f, 0f,
             0.5f,  0.5f, -0.5f,  1f, 0f, 0f,
             0.5f,  0.5f,  0.5f,  1f, 0f, 0f,
            // Top (+Y)
            -0.5f,  0.5f,  0.5f,  0f, 1f, 0f,
             0.5f,  0.5f,  0.5f,  0f, 1f, 0f,
             0.5f,  0.5f, -0.5f,  0f, 1f, 0f,
            -0.5f,  0.5f,  0.5f,  0f, 1f, 0f,
             0.5f,  0.5f, -0.5f,  0f, 1f, 0f,
            -0.5f,  0.5f, -0.5f,  0f, 1f, 0f,
            // Bottom (-Y)
            -0.5f, -0.5f, -0.5f,  0f, -1f, 0f,
             0.5f, -0.5f, -0.5f,  0f, -1f, 0f,
             0.5f, -0.5f,  0.5f,  0f, -1f, 0f,
            -0.5f, -0.5f, -0.5f,  0f, -1f, 0f,
             0.5f, -0.5f,  0.5f,  0f, -1f, 0f,
            -0.5f, -0.5f,  0.5f,  0f, -1f, 0f
        )
        return Mesh(data)
    }

    fun createCylinder(segments: Int = 14): Mesh {
        val verts = ArrayList<Float>(segments * 12 * 6)
        val step = (2.0 * PI / segments).toFloat()
        for (i in 0 until segments) {
            val a0 = i * step
            val a1 = (i + 1) * step
            val x0 = 0.5f * cos(a0)
            val z0 = 0.5f * sin(a0)
            val x1 = 0.5f * cos(a1)
            val z1 = 0.5f * sin(a1)
            val nx0 = cos(a0)
            val nz0 = sin(a0)
            val nx1 = cos(a1)
            val nz1 = sin(a1)

            addVert(verts, x0, -0.5f, z0, nx0, 0f, nz0)
            addVert(verts, x1, -0.5f, z1, nx1, 0f, nz1)
            addVert(verts, x1,  0.5f, z1, nx1, 0f, nz1)

            addVert(verts, x0, -0.5f, z0, nx0, 0f, nz0)
            addVert(verts, x1,  0.5f, z1, nx1, 0f, nz1)
            addVert(verts, x0,  0.5f, z0, nx0, 0f, nz0)

            addVert(verts, 0f, 0.5f, 0f, 0f, 1f, 0f)
            addVert(verts, x0, 0.5f, z0, 0f, 1f, 0f)
            addVert(verts, x1, 0.5f, z1, 0f, 1f, 0f)

            addVert(verts, 0f, -0.5f, 0f, 0f, -1f, 0f)
            addVert(verts, x1, -0.5f, z1, 0f, -1f, 0f)
            addVert(verts, x0, -0.5f, z0, 0f, -1f, 0f)
        }
        return Mesh(verts.toFloatArray())
    }

    fun createCone(segments: Int = 12): Mesh {
        val verts = ArrayList<Float>(segments * 6 * 6)
        val step = (2.0 * PI / segments).toFloat()
        for (i in 0 until segments) {
            val a0 = i * step
            val a1 = (i + 1) * step
            val x0 = 0.5f * cos(a0)
            val z0 = 0.5f * sin(a0)
            val x1 = 0.5f * cos(a1)
            val z1 = 0.5f * sin(a1)
            val nx = cos((a0 + a1) * 0.5f)
            val nz = sin((a0 + a1) * 0.5f)

            addVert(verts, 0f, 0.5f, 0f, nx, 0.5f, nz)
            addVert(verts, x1, -0.5f, z1, nx, 0.5f, nz)
            addVert(verts, x0, -0.5f, z0, nx, 0.5f, nz)

            addVert(verts, 0f, -0.5f, 0f, 0f, -1f, 0f)
            addVert(verts, x0, -0.5f, z0, 0f, -1f, 0f)
            addVert(verts, x1, -0.5f, z1, 0f, -1f, 0f)
        }
        return Mesh(verts.toFloatArray())
    }

    fun createDome(stacks: Int = 6, slices: Int = 12): Mesh {
        val verts = ArrayList<Float>()
        for (i in 0 until stacks) {
            val phi0 = (PI * 0.5 * i / stacks).toFloat()
            val phi1 = (PI * 0.5 * (i + 1) / stacks).toFloat()
            for (j in 0 until slices) {
                val th0 = (2.0 * PI * j / slices).toFloat()
                val th1 = (2.0 * PI * (j + 1) / slices).toFloat()

                val p00 = spherePt(phi0, th0)
                val p10 = spherePt(phi1, th0)
                val p11 = spherePt(phi1, th1)
                val p01 = spherePt(phi0, th1)

                addVert(verts, p00.x * 0.5f, p00.y * 0.5f, p00.z * 0.5f, p00.x, p00.y, p00.z)
                addVert(verts, p10.x * 0.5f, p10.y * 0.5f, p10.z * 0.5f, p10.x, p10.y, p10.z)
                addVert(verts, p11.x * 0.5f, p11.y * 0.5f, p11.z * 0.5f, p11.x, p11.y, p11.z)

                addVert(verts, p00.x * 0.5f, p00.y * 0.5f, p00.z * 0.5f, p00.x, p00.y, p00.z)
                addVert(verts, p11.x * 0.5f, p11.y * 0.5f, p11.z * 0.5f, p11.x, p11.y, p11.z)
                addVert(verts, p01.x * 0.5f, p01.y * 0.5f, p01.z * 0.5f, p01.x, p01.y, p01.z)
            }
        }
        return Mesh(verts.toFloatArray())
    }

    fun createSphere(stacks: Int = 8, slices: Int = 12): Mesh {
        val verts = ArrayList<Float>()
        for (i in 0 until stacks) {
            val phi0 = (-PI * 0.5 + PI * i / stacks).toFloat()
            val phi1 = (-PI * 0.5 + PI * (i + 1) / stacks).toFloat()
            for (j in 0 until slices) {
                val th0 = (2.0 * PI * j / slices).toFloat()
                val th1 = (2.0 * PI * (j + 1) / slices).toFloat()
                val p00 = spherePt(phi0, th0)
                val p10 = spherePt(phi1, th0)
                val p11 = spherePt(phi1, th1)
                val p01 = spherePt(phi0, th1)

                addVert(verts, p00.x * 0.5f, p00.y * 0.5f, p00.z * 0.5f, p00.x, p00.y, p00.z)
                addVert(verts, p10.x * 0.5f, p10.y * 0.5f, p10.z * 0.5f, p10.x, p10.y, p10.z)
                addVert(verts, p11.x * 0.5f, p11.y * 0.5f, p11.z * 0.5f, p11.x, p11.y, p11.z)

                addVert(verts, p00.x * 0.5f, p00.y * 0.5f, p00.z * 0.5f, p00.x, p00.y, p00.z)
                addVert(verts, p11.x * 0.5f, p11.y * 0.5f, p11.z * 0.5f, p11.x, p11.y, p11.z)
                addVert(verts, p01.x * 0.5f, p01.y * 0.5f, p01.z * 0.5f, p01.x, p01.y, p01.z)
            }
        }
        return Mesh(verts.toFloatArray())
    }

    /**
     * Smooth unit capsule spanning y in [-0.5, +0.5] and x,z radius 0.5.
     * Eliminates blocky limbs, arms, legs, sleeves, fingers, and ribbons.
     */
    fun createCapsule(slices: Int = 12, capRings: Int = 3): Mesh {
        val verts = ArrayList<Float>()
        val halfCyl = 0.30f
        val capH = 0.20f
        // Build vertical profile rings from bottom (-0.5) to top (+0.5)
        data class Ring(val y: Float, val r: Float, val ny: Float)
        val rings = ArrayList<Ring>()
        for (i in 0..capRings) {
            val a = (-PI * 0.5 + (PI * 0.5 * i / capRings)).toFloat()
            rings.add(Ring(-halfCyl + sin(a) * capH, cos(a) * 0.5f, sin(a)))
        }
        for (i in 1..capRings) {
            val a = (PI * 0.5 * i / capRings).toFloat()
            rings.add(Ring(halfCyl + sin(a) * capH, cos(a) * 0.5f, sin(a)))
        }

        val step = (2.0 * PI / slices).toFloat()
        for (rIdx in 0 until rings.size - 1) {
            val r0 = rings[rIdx]
            val r1 = rings[rIdx + 1]
            for (s in 0 until slices) {
                val th0 = s * step
                val th1 = (s + 1) * step
                val c0 = cos(th0); val s0 = sin(th0)
                val c1 = cos(th1); val s1 = sin(th1)

                val horiz0 = r0.r * 2f
                val horiz1 = r1.r * 2f
                val n0 = Vec3(c0 * horiz0, r0.ny, s0 * horiz0).normalized()
                val n1 = Vec3(c1 * horiz0, r0.ny, s1 * horiz0).normalized()
                val n2 = Vec3(c1 * horiz1, r1.ny, s1 * horiz1).normalized()
                val n3 = Vec3(c0 * horiz1, r1.ny, s0 * horiz1).normalized()

                addVert(verts, c0 * r0.r, r0.y, s0 * r0.r, n0.x, n0.y, n0.z)
                addVert(verts, c0 * r1.r, r1.y, s0 * r1.r, n3.x, n3.y, n3.z)
                addVert(verts, c1 * r1.r, r1.y, s1 * r1.r, n2.x, n2.y, n2.z)

                addVert(verts, c0 * r0.r, r0.y, s0 * r0.r, n0.x, n0.y, n0.z)
                addVert(verts, c1 * r1.r, r1.y, s1 * r1.r, n2.x, n2.y, n2.z)
                addVert(verts, c1 * r0.r, r0.y, s1 * r0.r, n1.x, n1.y, n1.z)
            }
        }
        return Mesh(verts.toFloatArray())
    }

    /**
     * Tapered 3-ring anime torso/jacket body (y in [-0.5, +0.5], waist narrower than chest/shoulders).
     */
    fun createTaperedTorso(bottomRadius: Float = 0.37f, midRadius: Float = 0.45f, topRadius: Float = 0.50f, slices: Int = 12): Mesh {
        val verts = ArrayList<Float>()
        val yVals = floatArrayOf(-0.5f, -0.04f, 0.38f, 0.50f)
        val rVals = floatArrayOf(bottomRadius, midRadius, topRadius, topRadius * 0.88f)
        val nyVals = floatArrayOf(-0.18f, 0.0f, 0.14f, 0.45f)
        val step = (2.0 * PI / slices).toFloat()

        for (seg in 0 until yVals.size - 1) {
            val y0 = yVals[seg]; val y1 = yVals[seg + 1]
            val r0 = rVals[seg]; val r1 = rVals[seg + 1]
            val ny0 = nyVals[seg]; val ny1 = nyVals[seg + 1]
            for (s in 0 until slices) {
                val a0 = s * step
                val a1 = (s + 1) * step
                val c0 = cos(a0); val s0 = sin(a0)
                val c1 = cos(a1); val s1 = sin(a1)
                addVert(verts, c0 * r0, y0, s0 * r0, c0, ny0, s0)
                addVert(verts, c0 * r1, y1, s0 * r1, c0, ny1, s0)
                addVert(verts, c1 * r1, y1, s1 * r1, c1, ny1, s1)

                addVert(verts, c0 * r0, y0, s0 * r0, c0, ny0, s0)
                addVert(verts, c1 * r1, y1, s1 * r1, c1, ny1, s1)
                addVert(verts, c1 * r0, y0, s1 * r0, c1, ny0, s1)
            }
        }
        // Top & bottom caps (CCW outward)
        for (s in 0 until slices) {
            val a0 = s * step; val a1 = (s + 1) * step
            addVert(verts, 0f, 0.5f, 0f, 0f, 1f, 0f)
            addVert(verts, cos(a1) * rVals.last(), 0.5f, sin(a1) * rVals.last(), 0f, 1f, 0f)
            addVert(verts, cos(a0) * rVals.last(), 0.5f, sin(a0) * rVals.last(), 0f, 1f, 0f)

            addVert(verts, 0f, -0.5f, 0f, 0f, -1f, 0f)
            addVert(verts, cos(a0) * bottomRadius, -0.5f, sin(a0) * bottomRadius, 0f, -1f, 0f)
            addVert(verts, cos(a1) * bottomRadius, -0.5f, sin(a1) * bottomRadius, 0f, -1f, 0f)
        }
        return Mesh(verts.toFloatArray())
    }

    /**
     * Chamfered 8-sided prism (unit bounds [-0.5, 0.5]) for sleek anime sneakers, collars, cargo pockets, and belts.
     */
    fun createBeveledBox(chamfer: Float = 0.16f): Mesh {
        val verts = ArrayList<Float>()
        val c = (0.5f - chamfer).coerceIn(0.2f, 0.48f)
        val pts = arrayOf(
            0.5f to c,
            c to 0.5f,
            -c to 0.5f,
            -0.5f to c,
            -0.5f to -c,
            -c to -0.5f,
            c to -0.5f,
            0.5f to -c
        )
        for (i in pts.indices) {
            val (x0, z0) = pts[i]
            val (x1, z1) = pts[(i + 1) % pts.size]
            val nx = ((x0 + x1) * 0.5f)
            val nz = ((z0 + z1) * 0.5f)
            val nLen = kotlin.math.sqrt(nx * nx + nz * nz).coerceAtLeast(1e-4f)
            val unx = nx / nLen
            val unz = nz / nLen

            // Outward winding CCW viewed from outside
            addVert(verts, x0, -0.5f, z0, unx, 0f, unz)
            addVert(verts, x0, 0.5f, z0, unx, 0f, unz)
            addVert(verts, x1, 0.5f, z1, unx, 0f, unz)

            addVert(verts, x0, -0.5f, z0, unx, 0f, unz)
            addVert(verts, x1, 0.5f, z1, unx, 0f, unz)
            addVert(verts, x1, -0.5f, z1, unx, 0f, unz)

            // Top & bottom triangles
            addVert(verts, 0f, 0.5f, 0f, 0f, 1f, 0f)
            addVert(verts, x1, 0.5f, z1, 0f, 1f, 0f)
            addVert(verts, x0, 0.5f, z0, 0f, 1f, 0f)

            addVert(verts, 0f, -0.5f, 0f, 0f, -1f, 0f)
            addVert(verts, x0, -0.5f, z0, 0f, -1f, 0f)
            addVert(verts, x1, -0.5f, z1, 0f, -1f, 0f)
        }
        return Mesh(verts.toFloatArray())
    }

    private fun spherePt(phi: Float, theta: Float): Vec3 {
        val cPhi = cos(phi)
        return Vec3(cPhi * cos(theta), sin(phi), cPhi * sin(theta))
    }

    private fun addVert(list: MutableList<Float>, x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float) {
        list.add(x); list.add(y); list.add(z)
        list.add(nx); list.add(ny); list.add(nz)
    }
}
