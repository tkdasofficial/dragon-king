package com.example.dragonking.levels.level1

import com.example.dragonking.engine.GameConstants
import com.example.dragonking.engine.SlimeEnemy
import com.example.dragonking.engine.TileType

/**
 * Kotlin counterpart of `levels/level-1/layout.ts`.
 * Defines Level 1 platforms, high floating blocks (rows 3..6), coins, obstacles, and ravines.
 */
object Level1Layout {
    val rows: List<String> = listOf(
        "..............................................................................................................",
        "..............................................................................................................",
        "..............................**.....................*........................................................",
        ".............................*..*...................*.*...............*.*.....................................",
        "......***........**..........MMMM...................MMM......**.......MMM........**.................***.......",
        "......MMM.......MMMM..................**..........*..........MM.................MMMM......**........MMM.......",
        ".......................**.............MM..........S.........*...*.........................MM*.................",
        "............*.....................................S.........................C.....................S.....F.....",
        "................................S...........C.....S..................S......C.....................S....SSS....",
        "...B...S.........E..........^^..S...........C.B...S.....P...........SS......C....E.....B........B.S...SDDD....",
        "GGGGGGGGGGG...GGGGGGGG....GGGGGGGGGG......GGGGGGGGGGGGGGGGG.......GGGG^^^GGGGGGGGGGGGGGGG.....GGGGGGGGGGGGGGGG",
        "DDDDDDDDDDD...DDDDDDDD....DDDDDDDDDD......DDDDDDDDDDDDDDDDD.......DDDDDDDDDDDDDDDDDDDDDDD.....DDDDDDDDDDDDDDDD"
    )

    fun createGrid(): Array<Array<TileType>> {
        val height = rows.size
        val width = rows.maxOf { it.length }
        return Array(height) { row ->
            val line = rows[row]
            Array(width) { col ->
                val ch = if (col < line.length) line[col] else '.'
                when (ch) {
                    'G' -> TileType.GRASS_TOP
                    'D' -> TileType.DIRT
                    'S' -> TileType.STONE
                    'M' -> TileType.MOSS_STONE
                    'C' -> TileType.CRATE
                    '^' -> TileType.SPIKE
                    'B' -> TileType.BRAZIER_OFF
                    '*' -> TileType.GEM
                    'P' -> TileType.CHECKPOINT_OFF
                    'F' -> TileType.GOAL
                    else -> TileType.EMPTY
                }
            }
        }
    }

    fun createSlimes(): MutableList<SlimeEnemy> {
        val slimes = mutableListOf<SlimeEnemy>()
        var nextId = 1
        val ts = GameConstants.TILE_SIZE.toFloat()
        for ((rowIdx, line) in rows.withIndex()) {
            for (colIdx in line.indices) {
                if (line[colIdx] == 'E') {
                    val sx = colIdx * ts
                    val sy = (rowIdx + 1) * ts - 10f
                    val patrolSpan = if (colIdx < 40) {
                        Pair(14 * ts, 20 * ts)
                    } else {
                        Pair(78 * ts, 86 * ts)
                    }
                    slimes.add(
                        SlimeEnemy(
                            id = nextId++,
                            x = sx,
                            y = sy,
                            startX = sx,
                            minX = patrolSpan.first,
                            maxX = patrolSpan.second,
                            vx = 28f
                        )
                    )
                }
            }
        }
        return slimes
    }
}
