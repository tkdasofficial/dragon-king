package com.example.dragonking.ui

import com.example.dragonking.audio.RetroSoundPlayer
import com.example.dragonking.audio.SoundCue
import com.example.dragonking.engine.GameEngine
import com.example.dragonking.engine.HudSnapshot
import com.example.dragonking.engine.InputState
import com.example.dragonking.pixel.PixelRenderer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameViewModel {
    val engine = GameEngine()
    val renderer = PixelRenderer()
    private val soundPlayer = RetroSoundPlayer()

    private val _hudState = MutableStateFlow(engine.getHudSnapshot())
    val hudState: StateFlow<HudSnapshot> = _hudState.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    // Volatile input flags updated by multi-touch controls and keyboard events
    @Volatile var touchLeft = false
    @Volatile var touchRight = false
    @Volatile var touchJump = false
    @Volatile var touchFire = false

    @Volatile var keyLeft = false
    @Volatile var keyRight = false
    @Volatile var keyJump = false
    @Volatile var keyFire = false

    private var prevJumpHeld = false
    private var prevFireHeld = false

    fun tick(deltaSeconds: Float) {
        if (_isPaused.value) {
            return
        }

        val prevJumpsRemaining = engine.jumpsRemaining
        val prevOnGround = engine.onGround
        val prevGems = engine.gemsCollected
        val prevHp = engine.hp
        val prevCleared = engine.levelCleared

        val jumpHeld = touchJump || keyJump
        val fireHeld = touchFire || keyFire
        val input = InputState(
            moveLeft = touchLeft || keyLeft,
            moveRight = touchRight || keyRight,
            jumpHeld = jumpHeld,
            jumpJustPressed = jumpHeld && !prevJumpHeld,
            jumpJustReleased = !jumpHeld && prevJumpHeld,
            fireHeld = fireHeld
        )
        prevJumpHeld = jumpHeld

        engine.stepFrame(deltaSeconds, input, renderer.virtualWidth)

        // Dispatch audio cues when enabled
        if (soundPlayer.enabled) {
            if (engine.gemsCollected > prevGems) {
                soundPlayer.play(SoundCue.COIN)
            }
            if (engine.hp < prevHp) {
                soundPlayer.play(SoundCue.HURT)
            }
            if (engine.levelCleared && !prevCleared) {
                soundPlayer.play(SoundCue.VICTORY)
            }
            if (fireHeld && !prevFireHeld) {
                soundPlayer.play(SoundCue.FIRE)
            }
            if (engine.jumpsRemaining < prevJumpsRemaining) {
                if (prevOnGround) {
                    soundPlayer.play(SoundCue.JUMP)
                } else {
                    soundPlayer.play(SoundCue.DOUBLE_JUMP)
                }
            }
        }
        prevFireHeld = fireHeld

        val latest = engine.getHudSnapshot()
        if (latest != _hudState.value) {
            _hudState.value = latest
        }
    }

    fun togglePause() {
        val next = !_isPaused.value
        _isPaused.value = next
        if (next) {
            clearHeldInputs()
        }
    }

    fun resumeGame() {
        _isPaused.value = false
    }

    fun toggleSound() {
        val next = !_soundEnabled.value
        _soundEnabled.value = next
        soundPlayer.enabled = next
        if (next) {
            soundPlayer.play(SoundCue.COIN)
        }
    }

    fun resetLevel() {
        engine.resetLevel()
        clearHeldInputs()
        _isPaused.value = false
        _hudState.value = engine.getHudSnapshot()
    }

    private fun clearHeldInputs() {
        touchLeft = false
        touchRight = false
        touchJump = false
        touchFire = false
        keyLeft = false
        keyRight = false
        keyJump = false
        keyFire = false
        prevJumpHeld = false
        prevFireHeld = false
    }
}
