package com.example.dragonking.levels.level1

import com.example.dragonking.engine.SlimeEnemy
import com.example.dragonking.engine.TileType
import com.example.dragonking.levels.GameLevel
import com.example.dragonking.levels.LevelConfig

/**
 * Initializer module mirroring `levels/level-1/index.ts`.
 */
object Level1 : GameLevel {
    override val config: LevelConfig = Level1Config.config
    override val rows: List<String> = Level1Layout.rows

    override fun createGrid(): Array<Array<TileType>> = Level1Layout.createGrid()

    override fun createSlimes(): MutableList<SlimeEnemy> = Level1Layout.createSlimes()
}
