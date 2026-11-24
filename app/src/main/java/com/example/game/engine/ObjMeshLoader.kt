package com.example.game.engine

import android.content.Context
import android.net.Uri
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Result of parsing a Wavefront .obj 3D character/prop mesh.
 * [vertices] uses the renderer's interleaved 6-float format: [px, py, pz, nx, ny, nz] per triangle vertex.
 */
data class ParsedObjModel(
    val modelName: String,
    val vertices: FloatArray,
    val triangleCount: Int,
    val heightMeters: Float
)

object ObjMeshLoader {
    private const val MAX_TRIANGLES = 36_000

    fun loadFromUri(context: Context, uri: Uri, displayNameHint: String? = null): Result<ParsedObjModel> {
        return try {
            val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().readText()
            } ?: return Result.failure(IllegalStateException("Could not open selected 3D model file."))
            val name = displayNameHint?.substringBeforeLast('.')?.take(32)?.ifBlank { "Custom 3D Hero" }
                ?: "Custom 3D Hero"
            parseObjText(content, name)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseObjText(
        objText: String,
        modelName: String = "Custom 3D Hero",
        targetHeightMeters: Float = 1.92f
    ): Result<ParsedObjModel> {
        if (objText.isBlank()) {
            return Result.failure(IllegalArgumentException("3D OBJ file is empty."))
        }

        val positions = ArrayList<FloatArray>(1024)
        val normals = ArrayList<FloatArray>(1024)
        val rawVertices = ArrayList<Float>(8192)
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY
        var triCount = 0

        val lines = objText.lineSequence()
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith('#')) continue
            if (line.startsWith("v ")) {
                val parts = line.split(WhitespaceRegex)
                if (parts.size >= 4) {
                    val x = parts[1].toFloatOrNull() ?: continue
                    val y = parts[2].toFloatOrNull() ?: continue
                    val z = parts[3].toFloatOrNull() ?: continue
                    positions.add(floatArrayOf(x, y, z))
                    if (x < minX) minX = x
                    if (y < minY) minY = y
                    if (z < minZ) minZ = z
                    if (x > maxX) maxX = x
                    if (y > maxY) maxY = y
                    if (z > maxZ) maxZ = z
                }
            } else if (line.startsWith("vn ")) {
                val parts = line.split(WhitespaceRegex)
                if (parts.size >= 4) {
                    val nx = parts[1].toFloatOrNull() ?: 0f
                    val ny = parts[2].toFloatOrNull() ?: 1f
                    val nz = parts[3].toFloatOrNull() ?: 0f
                    normals.add(floatArrayOf(nx, ny, nz))
                }
            } else if (line.startsWith("f ")) {
                if (triCount >= MAX_TRIANGLES) break
                val parts = line.split(WhitespaceRegex)
                if (parts.size < 4) continue
                val faceIndices = ArrayList<Pair<Int, Int>>(parts.size - 1)
                for (i in 1 until parts.size) {
                    val token = parts[i]
                    if (token.isBlank()) continue
                    val sub = token.split('/')
                    val rawVi = sub[0].toIntOrNull() ?: continue
                    val vIdx = resolveObjIndex(rawVi, positions.size) ?: continue
                    val rawNi = sub.getOrNull(2)?.toIntOrNull()
                    val nIdx = if (rawNi != null) resolveObjIndex(rawNi, normals.size) ?: -1 else -1
                    faceIndices.add(vIdx to nIdx)
                }
                if (faceIndices.size < 3) continue
                // Fan-triangulate polygons (triangles, quads, n-gons)
                for (k in 1 until faceIndices.size - 1) {
                    if (triCount >= MAX_TRIANGLES) break
                    val (v0, n0) = faceIndices[0]
                    val (v1, n1) = faceIndices[k]
                    val (v2, n2) = faceIndices[k + 1]
                    val p0 = positions[v0]
                    val p1 = positions[v1]
                    val p2 = positions[v2]
                    val fallbackN = if (n0 < 0 || n1 < 0 || n2 < 0) computeFaceNormal(p0, p1, p2) else null

                    appendVertex(rawVertices, p0, if (n0 >= 0) normals[n0] else fallbackN!!)
                    appendVertex(rawVertices, p1, if (n1 >= 0) normals[n1] else fallbackN!!)
                    appendVertex(rawVertices, p2, if (n2 >= 0) normals[n2] else fallbackN!!)
                    triCount++
                }
            }
        }

        if (triCount == 0 || positions.isEmpty()) {
            return Result.failure(IllegalArgumentException("No valid 3D triangles found in OBJ file."))
        }

        // Normalize model: center X/Z at origin, sit feet cleanly on Y=0, scale to hero height (~1.92m)
        val centerX = (minX + maxX) * 0.5f
        val centerZ = (minZ + maxZ) * 0.5f
        val rawHeight = (maxY - minY).takeIf { it > 1e-4f } ?: max(maxX - minX, maxZ - minZ).coerceAtLeast(1f)
        val scale = targetHeightMeters / rawHeight

        val result = FloatArray(rawVertices.size)
        var i = 0
        while (i < rawVertices.size) {
            result[i] = (rawVertices[i] - centerX) * scale
            result[i + 1] = (rawVertices[i + 1] - minY) * scale
            result[i + 2] = (rawVertices[i + 2] - centerZ) * scale
            val nx = rawVertices[i + 3]
            val ny = rawVertices[i + 4]
            val nz = rawVertices[i + 5]
            val nLen = sqrt(nx * nx + ny * ny + nz * nz).takeIf { it > 1e-5f } ?: 1f
            result[i + 3] = nx / nLen
            result[i + 4] = ny / nLen
            result[i + 5] = nz / nLen
            i += 6
        }

        return Result.success(
            ParsedObjModel(
                modelName = modelName,
                vertices = result,
                triangleCount = triCount,
                heightMeters = abs(maxY - minY) * scale
            )
        )
    }

    private fun resolveObjIndex(rawIdx: Int, listSize: Int): Int? {
        val resolved = if (rawIdx > 0) rawIdx - 1 else listSize + rawIdx
        return if (resolved in 0 until listSize) resolved else null
    }

    private fun computeFaceNormal(p0: FloatArray, p1: FloatArray, p2: FloatArray): FloatArray {
        val ux = p1[0] - p0[0]
        val uy = p1[1] - p0[1]
        val uz = p1[2] - p0[2]
        val vx = p2[0] - p0[0]
        val vy = p2[1] - p0[1]
        val vz = p2[2] - p0[2]
        val nx = uy * vz - uz * vy
        val ny = uz * vx - ux * vz
        val nz = ux * vy - uy * vx
        val len = sqrt(nx * nx + ny * ny + nz * nz).takeIf { it > 1e-5f } ?: 1f
        return floatArrayOf(nx / len, ny / len, nz / len)
    }

    private fun appendVertex(out: ArrayList<Float>, p: FloatArray, n: FloatArray) {
        out.add(p[0])
        out.add(p[1])
        out.add(p[2])
        out.add(n[0])
        out.add(n[1])
        out.add(n[2])
    }

    private val WhitespaceRegex = Regex("\\s+")
}
