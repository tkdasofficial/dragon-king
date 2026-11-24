package com.example.dragonking.engine

import com.example.dragonking.levels.LevelRegistry

/**
 * Compatibility facade delegating to [LevelRegistry.defaultLevel] (`levels/level-1/`).
 */
object LevelData {
    val TEST_LEVEL_ROWS: List<String>
        get() = LevelRegistry.defaultLevel.rows

    fun createGrid(): Array<Array<TileType>> = LevelRegistry.defaultLevel.createGrid()

    fun createSlimes(): MutableList<SlimeEnemy> = LevelRegistry.defaultLevel.createSlimes()
}
