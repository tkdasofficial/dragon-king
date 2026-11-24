package com.example.dragonking.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

enum class SoundCue {
    JUMP,
    DOUBLE_JUMP,
    COIN,
    FIRE,
    HURT,
    VICTORY
}

/**
 * Lightweight, allocation-minimal 8-bit chiptune PCM sound synthesizer.
 * Safe against headless/emulator audio failures and toggled via the Pause menu.
 */
class RetroSoundPlayer {
    @Volatile
    var enabled: Boolean = true

    private val executor by lazy { Executors.newSingleThreadExecutor() }
    private val clips: Map<SoundCue, ShortArray> by lazy { buildClips() }

    fun play(cue: SoundCue) {
        if (!enabled) return
        runCatching {
            val samples = clips[cue] ?: return
            executor.execute {
                playSamples(samples)
            }
        }
    }

    private fun playSamples(samples: ShortArray) {
        runCatching {
            val sampleRate = 22050
            val minBufBytes = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(samples.size * 2)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBufBytes)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(samples, 0, samples.size)
            track.play()
            val durationMs = ((samples.size * 1000L) / sampleRate) + 30L
            Thread.sleep(durationMs)
            track.stop()
            track.release()
        }
    }

    private fun buildClips(): Map<SoundCue, ShortArray> = mapOf(
        SoundCue.JUMP to synthSweep(startHz = 280f, endHz = 560f, durationSec = 0.09f, amp = 0.25f),
        SoundCue.DOUBLE_JUMP to synthSweep(startHz = 420f, endHz = 820f, durationSec = 0.11f, amp = 0.26f),
        SoundCue.COIN to synthTwoTone(hz1 = 880f, hz2 = 1318f, durationSec = 0.12f, amp = 0.24f),
        SoundCue.FIRE to synthNoiseBurst(durationSec = 0.09f, amp = 0.20f),
        SoundCue.HURT to synthSweep(startHz = 260f, endHz = 110f, durationSec = 0.18f, amp = 0.30f),
        SoundCue.VICTORY to synthTwoTone(hz1 = 587f, hz2 = 1175f, durationSec = 0.28f, amp = 0.26f)
    )

    private fun synthSweep(startHz: Float, endHz: Float, durationSec: Float, amp: Float): ShortArray {
        val sr = 22050
        val count = (sr * durationSec).toInt().coerceAtLeast(1)
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toFloat() / count
            val freq = startHz + (endHz - startHz) * t
            phase += 2.0 * PI * freq / sr
            val wave = if (sin(phase) >= 0.0) 1f else -1f
            val env = (1f - t)
            out[i] = (wave * amp * env * 32767f).toInt().toShort()
        }
        return out
    }

    private fun synthTwoTone(hz1: Float, hz2: Float, durationSec: Float, amp: Float): ShortArray {
        val sr = 22050
        val count = (sr * durationSec).toInt().coerceAtLeast(1)
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toFloat() / count
            val freq = if (t < 0.4f) hz1 else hz2
            phase += 2.0 * PI * freq / sr
            val wave = if (sin(phase) >= 0.0) 1f else -1f
            val env = (1f - t * 0.7f)
            out[i] = (wave * amp * env * 32767f).toInt().toShort()
        }
        return out
    }

    private fun synthNoiseBurst(durationSec: Float, amp: Float): ShortArray {
        val sr = 22050
        val count = (sr * durationSec).toInt().coerceAtLeast(1)
        val out = ShortArray(count)
        val rng = Random(7)
        for (i in 0 until count) {
            val t = i.toFloat() / count
            val sample = (rng.nextFloat() * 2f - 1f) * (1f - t) * amp
            out[i] = (sample * 32767f).toInt().toShort()
        }
        return out
    }
}
