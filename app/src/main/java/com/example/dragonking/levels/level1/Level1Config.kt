package com.example.dragonking.levels.level1

import com.example.dragonking.engine.GameConstants
import com.example.dragonking.levels.LevelConfig

/**
 * Kotlin counterpart of `levels/level-1/config.ts`.
 */
object Level1Config {
    val config = LevelConfig(
        id = "level-1",
        name = "Emerald Foothills",
        folderPath = "levels/level-1",
        tileSize = GameConstants.TILE_SIZE,
        rows = 12,
        cols = 110,
        spawnX = 28f,
        spawnRow = 10,
        floatingPlatformMinRow = 4,
        floatingPlatformMaxRow = 6,
        defaultDragonId = "red-dragon"
    )
}
