package com.example.game.engine

import android.opengl.Matrix
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Vec3(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)
    fun length(): Float = sqrt(x * x + y * y + z * z)
    fun lengthXZ(): Float = sqrt(x * x + z * z)
    fun normalized(): Vec3 {
        val len = length()
        return if (len > 1e-5f) Vec3(x / len, y / len, z / len) else Vec3(0f, 0f, 1f)
    }
}

data class Aabb2D(
    val minX: Float,
    val maxX: Float,
    val minZ: Float,
    val maxZ: Float,
    val height: Float,
    val label: String = ""
) {
    fun containsCircle(cx: Float, cz: Float, radius: Float): Boolean {
        val closestX = cx.coerceIn(minX, maxX)
        val closestZ = cz.coerceIn(minZ, maxZ)
        val dx = cx - closestX
        val dz = cz - closestZ
        return (dx * dx + dz * dz) < (radius * radius)
    }
}

object Math3D {
    fun degToRad(deg: Float): Float = deg * (PI.toFloat() / 180f)
    fun radToDeg(rad: Float): Float = rad * (180f / PI.toFloat())

    fun sinDeg(deg: Float): Float = sin(degToRad(deg))
    fun cosDeg(deg: Float): Float = cos(degToRad(deg))

    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t.coerceIn(0f, 1f)

    fun lerpAngleDeg(a: Float, b: Float, t: Float): Float {
        var diff = ((b - a + 540f) % 360f) - 180f
        return (a + diff * t.coerceIn(0f, 1f) + 360f) % 360f
    }

    fun buildModelMatrix(
        out: FloatArray,
        tx: Float,
        ty: Float,
        tz: Float,
        rx: Float = 0f,
        ry: Float = 0f,
        rz: Float = 0f,
        sx: Float = 1f,
        sy: Float = 1f,
        sz: Float = 1f
    ) {
        Matrix.setIdentityM(out, 0)
        Matrix.translateM(out, 0, tx, ty, tz)
        if (ry != 0f) Matrix.rotateM(out, 0, ry, 0f, 1f, 0f)
        if (rx != 0f) Matrix.rotateM(out, 0, rx, 1f, 0f, 0f)
        if (rz != 0f) Matrix.rotateM(out, 0, rz, 0f, 0f, 1f)
        Matrix.scaleM(out, 0, sx, sy, sz)
    }
}
