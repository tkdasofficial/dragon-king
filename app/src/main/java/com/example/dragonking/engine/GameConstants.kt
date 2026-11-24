package com.example.dragonking.engine

/**
 * Tuned Nintendo-style 2D platformer physics and grid constants.
 * All units are in logical pixels and seconds.
 */
object GameConstants {
    const val TILE_SIZE = 16
    const val VIRTUAL_HEIGHT = 192 // 12 tiles vertically
    const val MIN_VIRTUAL_WIDTH = 288
    const val MAX_VIRTUAL_WIDTH = 440

    const val FIXED_DT = 1f / 60f
    const val MAX_FRAME_DELTA = 0.05f

    // Dragon King hitbox (smaller than 28x24 visual sprite for forgiving platforming)
    const val DRAGON_WIDTH = 12f
    const val DRAGON_HEIGHT = 18f
    const val DRAGON_SPRITE_W = 28
    const val DRAGON_SPRITE_H = 24

    // Movement & Tight Jump Feel
    const val MOVE_ACCEL_GROUND = 1080f
    const val MOVE_DECEL_GROUND = 1350f
    const val MOVE_ACCEL_AIR = 880f
    const val MOVE_DECEL_AIR = 340f
    const val MAX_RUN_SPEED = 124f

    const val GRAVITY_UP = 580f
    const val GRAVITY_DOWN = 880f
    const val MAX_FALL_SPEED = 255f
    const val JUMP_VELOCITY = -338f
    const val DOUBLE_JUMP_VELOCITY = -312f
    const val MAX_JUMPS = 2
    const val DOUBLE_JUMP_WING_BOOST_TIME = 0.42f
    const val JUMP_CUT_MULTIPLIER = 0.45f

    const val COYOTE_TIME = 0.11f
    const val JUMP_BUFFER_TIME = 0.12f

    // Fire Breath & Combat
    const val FIRE_DURATION = 0.24f
    const val FIRE_SPAWN_INTERVAL = 0.022f
    const val FIRE_RANGE_X = 64f
    const val FIRE_RANGE_Y = 20f
    const val MAX_PARTICLES = 110

    const val INVULN_TIME = 1.15f
    const val MAX_HP = 3
}
