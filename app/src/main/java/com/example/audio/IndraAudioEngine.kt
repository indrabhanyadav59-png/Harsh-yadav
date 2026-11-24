package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Lightweight zero-asset-bloat anime-cyber BGM synthesizer & UI SFX player.
 * Thread-safe and gracefully no-ops on environments without audio hardware (e.g. Robolectric).
 */
object IndraAudioEngine {
    private const val SAMPLE_RATE = 22050
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private var bgmJob: Job? = null
    @Volatile private var isForeground = true
    @Volatile private var musicVolume = 0.7f
    @Volatile private var sfxVolume = 0.85f
    @Volatile private var voiceVolume = 0.8f

    fun setVolumes(music: Float, sfx: Float, voice: Float) {
        musicVolume = music.coerceIn(0f, 1f)
        sfxVolume = sfx.coerceIn(0f, 1f)
        voiceVolume = voice.coerceIn(0f, 1f)
    }

    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
        if (muted) {
            stopBgmLoop()
        } else if (isForeground) {
            startBgmIfActive()
        }
    }

    fun toggleMute(): Boolean {
        val next = !_isMuted.value
        setMuted(next)
        if (!next) playUiTap()
        return next
    }

    fun onAppForegroundChanged(foreground: Boolean) {
        isForeground = foreground
        if (!foreground) {
            stopBgmLoop()
        } else if (!_isMuted.value) {
            startBgmIfActive()
        }
    }

    fun startBgmIfActive() {
        if (_isMuted.value || !isForeground) return
        if (bgmJob?.isActive == true) return
        bgmJob = scope.launch {
            var track: AudioTrack? = null
            try {
                val minBuf = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                if (minBuf <= 0) return@launch
                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
                track.play()

                // Japanese pentatonic ambient synth chord progression (A minor / F / G / E)
                val chords = arrayOf(
                    doubleArrayOf(220.0, 261.63, 329.63, 440.0),
                    doubleArrayOf(174.61, 220.0, 261.63, 392.0),
                    doubleArrayOf(196.0, 246.94, 293.66, 392.0),
                    doubleArrayOf(164.81, 246.94, 329.63, 493.88)
                )
                val bufSamples = 1024
                val pcm = ShortArray(bufSamples)
                var sampleIdx = 0L

                while (isActive && !_isMuted.value && isForeground) {
                    val chordIdx = ((sampleIdx / (SAMPLE_RATE * 3L)) % chords.size).toInt()
                    val notes = chords[chordIdx]
                    val beatFrac = (sampleIdx % (SAMPLE_RATE / 2L)).toDouble() / (SAMPLE_RATE / 2.0)
                    val pulseEnv = exp(-beatFrac * 3.5)

                    for (i in 0 until bufSamples) {
                        val t = (sampleIdx + i).toDouble() / SAMPLE_RATE
                        var sum = 0.0
                        for (n in notes) {
                            sum += sin(2.0 * PI * n * t) * 0.14
                        }
                        // Soft sub-bass shimmer
                        sum += sin(2.0 * PI * (notes[0] * 0.5) * t) * 0.22 * (0.4 + 0.6 * pulseEnv)
                        pcm[i] = (sum.coerceIn(-1.0, 1.0) * 3200.0 * musicVolume).toInt().toShort()
                    }
                    sampleIdx += bufSamples
                    track.write(pcm, 0, pcm.size)
                }
            } catch (_: Throwable) {
                // Gracefully ignored in unit tests / devices without audio mixer
            } finally {
                try {
                    track?.stop()
                    track?.release()
                } catch (_: Throwable) {
                }
            }
        }
    }

    private fun stopBgmLoop() {
        bgmJob?.cancel()
        bgmJob = null
    }

    fun playUiTap() {
        playToneBurst(freqStart = 760.0, freqEnd = 980.0, durationMs = 55, gain = 2600.0)
    }

    fun playLoginSuccessChime() {
        playToneBurst(freqStart = 523.25, freqEnd = 783.99, durationMs = 180, gain = 3600.0)
    }

    fun playStartGameLaunch() {
        playToneBurst(freqStart = 392.0, freqEnd = 1046.5, durationMs = 260, gain = 4400.0)
    }

    fun playPetSound(profile: com.example.data.model.PetSoundProfile, evolutionLevel: Int = 1) {
        val pitchScale = when (evolutionLevel.coerceIn(1, 3)) {
            1 -> 1.12
            2 -> 1.0
            else -> 0.88
        }
        when (profile) {
            com.example.data.model.PetSoundProfile.CHIRP_YIP ->
                playToneBurst(freqStart = 880.0 * pitchScale, freqEnd = 1320.0 * pitchScale, durationMs = 95, gain = 3000.0)
            com.example.data.model.PetSoundProfile.HOWL_BARK ->
                playToneBurst(freqStart = 420.0 * pitchScale, freqEnd = 680.0 * pitchScale, durationMs = 190, gain = 3400.0)
            com.example.data.model.PetSoundProfile.PURR_ROAR ->
                playToneBurst(freqStart = 210.0 * pitchScale, freqEnd = 135.0 * pitchScale, durationMs = 240, gain = 3800.0)
            com.example.data.model.PetSoundProfile.AVIAN_SCREECH ->
                playToneBurst(freqStart = 1480.0 * pitchScale, freqEnd = 960.0 * pitchScale, durationMs = 150, gain = 3100.0)
            com.example.data.model.PetSoundProfile.SONGBIRD_TRILL ->
                playToneBurst(freqStart = 980.0 * pitchScale, freqEnd = 1560.0 * pitchScale, durationMs = 140, gain = 2900.0)
            com.example.data.model.PetSoundProfile.DRAGON_RUMBLE ->
                playToneBurst(freqStart = 165.0 * pitchScale, freqEnd = 95.0 * pitchScale, durationMs = 290, gain = 4200.0)
            com.example.data.model.PetSoundProfile.AQUA_SONAR ->
                playToneBurst(freqStart = 720.0 * pitchScale, freqEnd = 1240.0 * pitchScale, durationMs = 180, gain = 3000.0)
            com.example.data.model.PetSoundProfile.CRYSTAL_CHIME ->
                playToneBurst(freqStart = 1174.66 * pitchScale, freqEnd = 1567.98 * pitchScale, durationMs = 210, gain = 3200.0)
            com.example.data.model.PetSoundProfile.CELESTIAL_CHORD ->
                playToneBurst(freqStart = 587.33 * pitchScale, freqEnd = 1174.66 * pitchScale, durationMs = 280, gain = 3600.0)
        }
    }

    fun playPetEvolutionFanfare(targetLevel: Int) {
        val base = if (targetLevel >= 3) 659.25 else 523.25
        playToneBurst(freqStart = base, freqEnd = base * 2.0, durationMs = 340, gain = 4400.0)
    }

    fun playGunshot(weaponId: String) {
        val isHeavy = weaponId == "wpn_vortex_ar"
        val isSmg = weaponId == "wpn_dragon_smg"
        val durationMs = when {
            isHeavy -> 205
            isSmg -> 150
            else -> 175
        }
        val punchSweepStart = when {
            isHeavy -> 118.0
            isSmg -> 136.0
            else -> 128.0
        }
        val punchSweepEnd = when {
            isHeavy -> 48.0
            isSmg -> 62.0
            else -> 54.0
        }
        var boomPhase = 0.0
        playSynthesizedEffect(durationMs) { progress, phaseIdx ->
            val t = phaseIdx.toDouble() / SAMPLE_RATE
            val boomFreq = punchSweepStart + (punchSweepEnd - punchSweepStart) * (progress * 0.85).coerceAtMost(1.0)
            boomPhase += 2.0 * PI * boomFreq / SAMPLE_RATE
            // Heavy "Dhoom-Dhoom" chest sub-bass boom + harmonic overtone + crisp supersonic snap
            val subBoom = sin(boomPhase) * exp(-progress * 6.8) * 1.15
            val chestBody = sin(boomPhase * 2.05) * exp(-progress * 10.5) * 0.52
            val gunCrack = deterministicNoise(phaseIdx) * exp(-progress * 21.0) * 0.92
            val boltEcho = deterministicNoise(phaseIdx + 97) * exp(-progress * 7.5) * 0.24
            val rawMix = (subBoom + chestBody + gunCrack + boltEcho)
            kotlin.math.tanh(rawMix * 1.55) * 9200.0
        }
    }

    fun playKillConfirm(killCount: Int) {
        val pitchBoost = 1.0 + (killCount.coerceIn(1, 8) - 1) * 0.045
        playSynthesizedEffect(230) { progress, phaseIdx ->
            val t = phaseIdx.toDouble() / SAMPLE_RATE
            val chime1 = if (progress < 0.42) {
                sin(2.0 * PI * 740.0 * pitchBoost * t) * exp(-progress * 7.0)
            } else 0.0
            val chime2 = if (progress >= 0.35) {
                sin(2.0 * PI * 1108.73 * pitchBoost * t) * exp(-(progress - 0.35) * 5.2)
            } else 0.0
            val bassPop = sin(2.0 * PI * 96.0 * t) * exp(-progress * 16.0) * 0.65
            (chime1 + chime2 + bassPop) * 5200.0
        }
    }

    fun playReload() {
        playSynthesizedEffect(260) { progress, phaseIdx ->
            val t = phaseIdx.toDouble() / SAMPLE_RATE
            val click1 = if (progress < 0.28) exp(-progress * 24.0) * sin(2.0 * PI * 920.0 * t) else 0.0
            val slide = if (progress in 0.34..0.68) sin(2.0 * PI * 540.0 * t) * exp(-(progress - 0.34) * 14.0) * 0.7 else 0.0
            val lock = if (progress > 0.70) sin(2.0 * PI * 1240.0 * t) * exp(-(progress - 0.70) * 26.0) else 0.0
            (click1 + slide + lock) * 4100.0
        }
    }

    /**
     * Three-step katana slash sound ("खाच-खाच-खाच") with rising metallic ring + air whoosh.
     */
    fun playKatanaSlash(comboStep: Int) {
        val step = comboStep.coerceIn(1, 3)
        val durationMs = if (step == 3) 210 else 155
        val whooshStart = when (step) {
            1 -> 1350.0
            2 -> 1620.0
            else -> 1950.0
        }
        val steelRing = when (step) {
            1 -> 2180.0
            2 -> 2490.0
            else -> 2860.0
        }
        playSynthesizedEffect(durationMs) { progress, phaseIdx ->
            val t = phaseIdx.toDouble() / SAMPLE_RATE
            val env = sin(PI * progress) * exp(-progress * 2.8)
            val cutFreq = whooshStart * (1.0 - 0.58 * progress)
            val airCut = deterministicNoise(phaseIdx) * exp(-progress * 6.5) * 0.62
            val sweep = sin(2.0 * PI * cutFreq * t) * env * 0.55
            val bladeShing = sin(2.0 * PI * steelRing * t) * exp(-progress * 8.5) * 0.42
            val subSnap = sin(2.0 * PI * 155.0 * t) * exp(-progress * 18.0) * 0.38
            (airCut + sweep + bladeShing + subSnap) * 5200.0
        }
    }

    fun playHitImpact(isBlade: Boolean) {
        val dur = if (isBlade) 130 else 120
        playSynthesizedEffect(dur) { progress, phaseIdx ->
            val t = phaseIdx.toDouble() / SAMPLE_RATE
            val thud = sin(2.0 * PI * (if (isBlade) 280.0 else 108.0) * t) * exp(-progress * 11.0)
            val crunch = deterministicNoise(phaseIdx) * exp(-progress * 18.0) * (if (isBlade) 0.78 else 0.62)
            val ring = if (isBlade) sin(2.0 * PI * 1850.0 * t) * exp(-progress * 11.0) * 0.45 else 0.0
            kotlin.math.tanh((thud + crunch + ring) * 1.25) * 6200.0
        }
    }

    fun playPunch() {
        playSynthesizedEffect(140) { progress, phaseIdx ->
            val t = phaseIdx.toDouble() / SAMPLE_RATE
            val whoosh = deterministicNoise(phaseIdx) * exp(-progress * 12.0) * 0.62
            val thump = sin(2.0 * PI * (155.0 - 85.0 * progress) * t) * exp(-progress * 9.5) * 1.05
            kotlin.math.tanh((whoosh + thump) * 1.3) * 6000.0
        }
    }

    fun playExplosion() {
        playSynthesizedEffect(480) { progress, phaseIdx ->
            val t = phaseIdx.toDouble() / SAMPLE_RATE
            val rumble = sin(2.0 * PI * (68.0 - 36.0 * progress) * t) * exp(-progress * 4.2) * 1.15
            val blast = deterministicNoise(phaseIdx) * exp(-progress * 4.8) * 0.92
            kotlin.math.tanh((rumble + blast) * 1.4) * 8400.0
        }
    }

    fun playJump() {
        playToneBurst(freqStart = 210.0, freqEnd = 480.0, durationMs = 110, gain = 3600.0)
    }

    fun playPickup() {
        playSynthesizedEffect(180) { progress, phaseIdx ->
            val t = phaseIdx.toDouble() / SAMPLE_RATE
            val f = if (progress < 0.48) 660.0 else 990.0
            sin(2.0 * PI * f * t) * exp(-((progress % 0.5) * 6.0)) * 3800.0
        }
    }

    fun playDoorTransition() {
        playSynthesizedEffect(220) { progress, phaseIdx ->
            val t = phaseIdx.toDouble() / SAMPLE_RATE
            val f = if (progress < 0.5) 330.0 else 440.0
            sin(2.0 * PI * f * t) * sin(PI * progress) * 3400.0
        }
    }

    fun playCoinTransaction(isSell: Boolean) {
        val f1 = if (isSell) 783.99 else 587.33
        val f2 = if (isSell) 1174.66 else 880.0
        playSynthesizedEffect(230) { progress, phaseIdx ->
            val t = phaseIdx.toDouble() / SAMPLE_RATE
            val f = if (progress < 0.45) f1 else f2
            (sin(2.0 * PI * f * t) + sin(4.0 * PI * f * t) * 0.3) * sin(PI * progress) * 3900.0
        }
    }

    private fun deterministicNoise(idx: Int): Double {
        var x = idx * 1103515245 + 12345
        x = (x shl 13) xor x
        val n = 1.0 - ((x * (x * x * 15731 + 789221) + 1376312589) and 0x7fffffff) / 1073741824.0
        return n.coerceIn(-1.0, 1.0)
    }

    private fun playSynthesizedEffect(durationMs: Int, sampleGenerator: (progress: Double, idx: Int) -> Double) {
        if (_isMuted.value || !isForeground) return
        scope.launch {
            var track: AudioTrack? = null
            try {
                val totalSamples = (SAMPLE_RATE * durationMs) / 1000
                if (totalSamples <= 0) return@launch
                val pcm = ShortArray(totalSamples)
                for (i in 0 until totalSamples) {
                    val progress = i.toDouble() / totalSamples
                    val sample = sampleGenerator(progress, i) * sfxVolume
                    pcm[i] = sample.coerceIn(-32000.0, 32000.0).toInt().toShort()
                }
                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(totalSamples * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.play()
                kotlinx.coroutines.delay(durationMs.toLong() + 45L)
            } catch (_: Throwable) {
            } finally {
                try {
                    track?.stop()
                    track?.release()
                } catch (_: Throwable) {
                }
            }
        }
    }

    private fun playToneBurst(freqStart: Double, freqEnd: Double, durationMs: Int, gain: Double) {
        if (_isMuted.value || !isForeground) return
        scope.launch {
            var track: AudioTrack? = null
            try {
                val totalSamples = (SAMPLE_RATE * durationMs) / 1000
                val pcm = ShortArray(totalSamples)
                var phase = 0.0
                for (i in 0 until totalSamples) {
                    val progress = i.toDouble() / totalSamples
                    val freq = freqStart + (freqEnd - freqStart) * progress
                    phase += 2.0 * PI * freq / SAMPLE_RATE
                    val env = sin(PI * progress)
                    pcm[i] = (sin(phase) * env * gain * sfxVolume).toInt().toShort()
                }
                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(totalSamples * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.play()
                kotlinx.coroutines.delay(durationMs.toLong() + 40L)
            } catch (_: Throwable) {
            } finally {
                try {
                    track?.stop()
                    track?.release()
                } catch (_: Throwable) {
                }
            }
        }
    }
}
