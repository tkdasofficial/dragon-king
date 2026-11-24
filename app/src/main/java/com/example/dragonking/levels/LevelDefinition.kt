package com.example.dragonking.levels

import com.example.dragonking.engine.SlimeEnemy
import com.example.dragonking.engine.TileType
import com.example.dragonking.levels.level1.Level1

/**
 * Shared configuration & contract for levels organized under `levels/level-<n>/`.
 * Mirrors `/levels/index.ts` so additional levels can be registered cleanly.
 */
data class LevelConfig(
    val id: String,
    val name: String,
    val folderPath: String,
    val tileSize: Int,
    val rows: Int,
    val cols: Int,
    val spawnX: Float,
    val spawnRow: Int,
    val floatingPlatformMinRow: Int,
    val floatingPlatformMaxRow: Int,
    val defaultDragonId: String = "red-dragon"
)

interface GameLevel {
    val config: LevelConfig
    val rows: List<String>

    fun createGrid(): Array<Array<TileType>>
    fun createSlimes(): MutableList<SlimeEnemy>
}

object LevelRegistry {
    val LEVEL_1: GameLevel = Level1

    val all: List<GameLevel> = listOf(
        LEVEL_1
    )

    val defaultLevel: GameLevel = LEVEL_1

    fun getById(id: String): GameLevel = all.firstOrNull { it.config.id == id } ?: defaultLevel
}
