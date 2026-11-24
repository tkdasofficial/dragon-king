package com.example.dragonking.engine

enum class TileType(val solid: Boolean, val burnable: Boolean = false, val lethal: Boolean = false) {
    EMPTY(solid = false),
    GRASS_TOP(solid = true),
    DIRT(solid = true),
    STONE(solid = true),
    MOSS_STONE(solid = true),
    CRATE(solid = true, burnable = true),
    SPIKE(solid = false, lethal = true),
    BRAZIER_OFF(solid = false),
    BRAZIER_ON(solid = false),
    GEM(solid = false),
    CHECKPOINT_OFF(solid = false),
    CHECKPOINT_ON(solid = false),
    GOAL(solid = false)
}

enum class ParticleKind {
    FLAME,
    AMBIENT_SPARK,
    SPARK,
    SMOKE,
    WOOD_ASH
}

data class FireParticle(
    var active: Boolean = false,
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var life: Float = 0f,
    var maxLife: Float = 0.35f,
    var size: Int = 2,
    var kind: ParticleKind = ParticleKind.FLAME,
    var deadlyToEnemies: Boolean = true
)

data class SlimeEnemy(
    val id: Int,
    var x: Float,
    var y: Float,
    val startX: Float,
    val minX: Float,
    val maxX: Float,
    var vx: Float = 28f,
    var alive: Boolean = true
)

data class InputState(
    val moveLeft: Boolean = false,
    val moveRight: Boolean = false,
    val jumpHeld: Boolean = false,
    val jumpJustPressed: Boolean = false,
    val jumpJustReleased: Boolean = false,
    val fireHeld: Boolean = false
)

data class HudSnapshot(
    val hp: Int = GameConstants.MAX_HP,
    val maxHp: Int = GameConstants.MAX_HP,
    val gems: Int = 0,
    val totalGems: Int = 0,
    val cratesBurned: Int = 0,
    val braziersLit: Int = 0,
    val slimesDefeated: Int = 0,
    val isDead: Boolean = false,
    val levelCleared: Boolean = false,
    val distancePercent: Int = 0,
    val elapsedSeconds: Int = 0
)
