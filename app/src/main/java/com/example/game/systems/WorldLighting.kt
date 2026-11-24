package com.example.game.systems

import com.example.data.model.TimeOfDayMode
import com.example.data.model.WeatherMode

data class LightingFrameConfig(
    val skyR: Float,
    val skyG: Float,
    val skyB: Float,
    val fogR: Float,
    val fogG: Float,
    val fogB: Float,
    val sunDirX: Float,
    val sunDirY: Float,
    val sunDirZ: Float,
    val ambientStrength: Float,
    val sunStrength: Float,
    val isNight: Boolean
)

/**
 * Dynamic Skybox, Directional Sunlight, Atmospheric Fog & Weather lighting controller.
 */
object WorldLighting {

    fun computeLighting(timeOfDay: TimeOfDayMode, weather: WeatherMode): LightingFrameConfig {
        val base = when (timeOfDay) {
            TimeOfDayMode.DAWN -> LightingFrameConfig(
                skyR = 0.88f, skyG = 0.56f, skyB = 0.65f,
                fogR = 0.86f, fogG = 0.58f, fogB = 0.66f,
                sunDirX = 0.62f, sunDirY = 0.45f, sunDirZ = 0.48f,
                ambientStrength = 0.44f, sunStrength = 0.72f, isNight = false
            )
            TimeOfDayMode.NOON -> LightingFrameConfig(
                skyR = 0.42f, skyG = 0.72f, skyB = 0.96f,
                fogR = 0.56f, fogG = 0.80f, fogB = 0.98f,
                sunDirX = 0.42f, sunDirY = 0.92f, sunDirZ = 0.38f,
                ambientStrength = 0.48f, sunStrength = 0.88f, isNight = false
            )
            TimeOfDayMode.SUNSET -> LightingFrameConfig(
                skyR = 0.92f, skyG = 0.44f, skyB = 0.34f,
                fogR = 0.88f, fogG = 0.46f, fogB = 0.38f,
                sunDirX = -0.65f, sunDirY = 0.36f, sunDirZ = 0.44f,
                ambientStrength = 0.40f, sunStrength = 0.76f, isNight = false
            )
            TimeOfDayMode.NIGHT -> LightingFrameConfig(
                skyR = 0.05f, skyG = 0.08f, skyB = 0.18f,
                fogR = 0.06f, fogG = 0.10f, fogB = 0.22f,
                sunDirX = 0.35f, sunDirY = 0.65f, sunDirZ = -0.45f,
                ambientStrength = 0.32f, sunStrength = 0.42f, isNight = true
            )
        }

        // Weather atmospheric modulation (rain/storm overcasts sky & deepens fog slightly)
        return when (weather) {
            WeatherMode.RAIN -> base.copy(
                skyR = base.skyR * 0.78f,
                skyG = base.skyG * 0.82f,
                skyB = (base.skyB * 0.92f).coerceAtMost(1f),
                fogR = base.fogR * 0.76f,
                fogG = base.fogG * 0.80f,
                fogB = base.fogB * 0.90f,
                sunStrength = base.sunStrength * 0.72f
            )
            WeatherMode.STORM -> base.copy(
                skyR = base.skyR * 0.56f,
                skyG = base.skyG * 0.58f,
                skyB = base.skyB * 0.74f,
                fogR = base.fogR * 0.54f,
                fogG = base.fogG * 0.56f,
                fogB = base.fogB * 0.70f,
                sunStrength = base.sunStrength * 0.55f
            )
            else -> base
        }
    }
}
