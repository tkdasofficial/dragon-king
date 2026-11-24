package com.example.dragonking.pixel

/**
 * Immutable compiled pixel-art sprite stored as a flat ARGB IntArray for fast blitting.
 */
class PixelSprite(
    val width: Int,
    val height: Int,
    val pixels: IntArray
) {
    companion object {
        fun compile(
            width: Int,
            height: Int,
            rows: List<String>,
            colorMap: Map<Char, Int>
        ): PixelSprite {
            val data = IntArray(width * height) { PixelPalette.TRANSPARENT }
            val maxRows = minOf(height, rows.size)
            for (y in 0 until maxRows) {
                val row = rows[y]
                val maxCols = minOf(width, row.length)
                for (x in 0 until maxCols) {
                    val ch = row[x]
                    if (ch != '.' && ch != ' ') {
                        data[y * width + x] = colorMap[ch] ?: PixelPalette.TRANSPARENT
                    }
                }
            }
            return PixelSprite(width, height, data)
        }
    }
}

/**
 * Polished 16-bit pixel-art sprites and tiles authored purely in code via shaded pixel matrices.
 */
object SpriteAtlas {
    private val sharedColors = mapOf(
        'K' to PixelPalette.OUTLINE,
        // Dragon King
        'G' to PixelPalette.DRAGON_GOLD_HIGHLIGHT,
        'Y' to PixelPalette.DRAGON_GOLD_LIGHT,
        'O' to PixelPalette.DRAGON_GOLD_DARK,
        'C' to PixelPalette.DRAGON_SCALE_RIM,
        'H' to PixelPalette.DRAGON_SCALE_LIGHT,
        'R' to PixelPalette.DRAGON_SCALE_MID,
        'D' to PixelPalette.DRAGON_SCALE_DARK,
        'v' to PixelPalette.DRAGON_WING_LIGHT,
        'V' to PixelPalette.DRAGON_WING_MEMBRANE,
        'B' to PixelPalette.DRAGON_BELLY_LIGHT,
        'U' to PixelPalette.DRAGON_BELLY_DARK,
        'W' to PixelPalette.DRAGON_HORN,
        't' to PixelPalette.DRAGON_HORN_SHADE,
        'E' to PixelPalette.DRAGON_EYE,
        'F' to PixelPalette.FIRE_YELLOW,
        'A' to PixelPalette.FIRE_ORANGE,
        'x' to PixelPalette.FIRE_WHITE,
        // Terrain & Props
        'u' to PixelPalette.GRASS_RIM,
        'g' to PixelPalette.GRASS_BRIGHT,
        'e' to PixelPalette.GRASS_MID,
        'd' to PixelPalette.GRASS_DARK,
        'k' to PixelPalette.DIRT_HIGHLIGHT,
        'l' to PixelPalette.DIRT_LIGHT,
        'm' to PixelPalette.DIRT_MID,
        'n' to PixelPalette.DIRT_DARK,
        'h' to PixelPalette.STONE_HIGHLIGHT,
        's' to PixelPalette.STONE_LIGHT,
        'S' to PixelPalette.STONE_MID,
        'z' to PixelPalette.STONE_DARK,
        'Z' to PixelPalette.STONE_MORTAR,
        'w' to PixelPalette.WOOD_HIGHLIGHT,
        'c' to PixelPalette.WOOD_LIGHT,
        'p' to PixelPalette.WOOD_MID,
        'b' to PixelPalette.WOOD_DARK,
        'i' to PixelPalette.STEEL_BRIGHT,
        'f' to PixelPalette.STEEL_LIGHT,
        'I' to PixelPalette.STEEL_MID,
        'j' to PixelPalette.STEEL_DARK,
        // Trees & Clouds
        'q' to PixelPalette.TRUNK_HIGHLIGHT,
        'T' to PixelPalette.TRUNK_LIGHT,
        'N' to PixelPalette.TRUNK_DARK,
        '4' to PixelPalette.LEAF_HIGHLIGHT,
        '5' to PixelPalette.LEAF_LIGHT,
        '6' to PixelPalette.LEAF_MID,
        '7' to PixelPalette.LEAF_DARK,
        '0' to PixelPalette.CLOUD_HIGHLIGHT,
        '1' to PixelPalette.CLOUD_LIGHT,
        '2' to PixelPalette.CLOUD_MID,
        '3' to PixelPalette.CLOUD_SHADOW,
        // Gems, Shrine, Enemies
        'y' to PixelPalette.GEM_WHITE,
        'o' to PixelPalette.GEM_LIGHT,
        'a' to PixelPalette.GEM_MID,
        'M' to PixelPalette.GEM_DARK,
        'r' to PixelPalette.RUBY_GLOW,
        'Q' to PixelPalette.SHRINE_GLOW,
        'L' to PixelPalette.SLIME_HIGHLIGHT,
        'P' to PixelPalette.SLIME_LIGHT,
        'J' to PixelPalette.SLIME_MID,
        'X' to PixelPalette.SLIME_DARK
    )

    // ---------------------------------------------------------
    // DRAGON KING SPRITES (28x24 compact, plump & kingly, 3-tone shading)
    // Feet plant cleanly on row 21..22 across grounded frames.
    // ---------------------------------------------------------
    val dragonIdle1 = PixelSprite.compile(
        28, 24,
        listOf(
            "...........KG.KG.KG.........",
            "...........KY.KY.KY.........",
            "...........KYOYrYOK.........",
            "......KKK..KOOOOOOK.........",
            ".....KWWKKKCCCHHHHKK........",
            ".....KttWKCHRRRRRRRHK.......",
            "......KKKCHRRKExKRRCCK......",
            "........KCHRRRRRRRRRRRK.....",
            "........KCHRRRRRRKKKWKK.....",
            ".....KK.KDRRRRRRRKKKKK......",
            "....KvvKKDRRBBBRRRKK........",
            "...KvVVVKDRBUBUBRRHHWK......",
            "...KvVVRKDRRBBBBRRKK........",
            ".KKKHRRKKDRRUBUBRRCCHK......",
            "KYHRRRK.KDRRRRBBBBRKK.......",
            "KFRDDK..KDRRRRRRRRDK........",
            ".KKKK...KDRRRRRRRRDK........",
            "........KDRRRKKKDRDK........",
            "........KDRRK...KDRK........",
            "........KDDDK...KDDK........",
            "........KWWWK...KWWK........",
            "........KKKKK...KKKK........",
            "............................",
            "............................"
        ),
        sharedColors
    )

    val dragonIdle2 = PixelSprite.compile(
        28, 24,
        listOf(
            "............................",
            "...........KG.KG.KG.........",
            "...........KY.KY.KY.........",
            "...........KYOYrYOK.........",
            "......KKK..KOOOOOOK.........",
            ".....KWWKKKCCCHHHHKK........",
            ".....KttWKCHRRRRRRRHK.......",
            "......KKKCHRRKExKRRCCK......",
            "........KCHRRRRRRRRRRRK.....",
            "....KK..KCHRRRRRRKKKWKK.....",
            "...KvvKKKDRRRRRRRKKKKK......",
            "...KvVVVKDRRBBBRRRKK........",
            "..KKvVVRKDRBUBUBRRHHWK......",
            ".KYHRRRKKDRRBBBBRRKK........",
            ".KFRDDK.KDRRUBUBRRCCHK......",
            "..KKKK..KDRRRRBBBBRKK.......",
            "........KDRRRRRRRRDK........",
            "........KDRRRKKKDRDK........",
            "........KDRDK...KDRK........",
            "........KDDDK...KDDK........",
            "........KWWWK...KWWK........",
            "........KKKKK...KKKK........",
            "............................",
            "............................"
        ),
        sharedColors
    )

    // 4-frame clear alternating leg-movement walk cycle
    val dragonWalk1 = PixelSprite.compile(
        28, 24,
        listOf(
            "...........KG.KG.KG.........",
            "...........KY.KY.KY.........",
            "...........KYOYrYOK.........",
            "......KKK..KOOOOOOK.........",
            ".....KWWKKKCCCHHHHKK........",
            ".....KttWKCHRRRRRRRHK.......",
            "......KKKCHRRKExKRRCCK......",
            "........KCHRRRRRRRRRRRK.....",
            "........KCHRRRRRRKKKWKK.....",
            ".....KK.KDRRRRRRRKKKKK......",
            "....KvvKKDRRBBBRRRKK........",
            "...KvVVVKDRBUBUBRRHHWK......",
            "KKKvVVRKKDRRBBBBRRKK........",
            "KYHRRRKK.DRRUBUBRRCCHK......",
            "KFRDDKK..KDRRRBBBBRKK.......",
            ".KKKK....KDRRRRRRRDK........",
            "........KDRRKKKKDRDK........",
            ".......KDRDK.....KDRDK......",
            "......KDRDK.......KDRDK.....",
            ".....KDDDK.........KDDK.....",
            ".....KWWWK.........KWWK.....",
            ".....KKKKK.........KKKK.....",
            "............................",
            "............................"
        ),
        sharedColors
    )

    val dragonWalk2 = PixelSprite.compile(
        28, 24,
        listOf(
            "............................",
            "...........KG.KG.KG.........",
            "...........KY.KY.KY.........",
            "...........KYOYrYOK.........",
            "......KKK..KOOOOOOK.........",
            ".....KWWKKKCCCHHHHKK........",
            ".....KttWKCHRRRRRRRHK.......",
            "......KKKCHRRKExKRRCCK......",
            "........KCHRRRRRRRRRRRK.....",
            ".....KK.KCHRRRRRRKKKWKK.....",
            "....KvvKKDRRRRRRRKKKKK......",
            "...KvVVVKDRRBBBRRRKK........",
            "...KvVVRKDRBUBUBRRHHWK......",
            ".KKKHRRKKDRRBBBBRRKK........",
            "KYHRRRK.KDRRUBUBRRCCHK......",
            "KFRDDK..KDRRRRBBBBRKK.......",
            ".KKKK...KDRRRRRRRRDK........",
            "........KDRRRKKDRDK.........",
            "........KDRDK.KDRK..........",
            "........KDDDK.KWWK..........",
            "........KWWWK.KKKK..........",
            "........KKKKK...............",
            "............................",
            "............................"
        ),
        sharedColors
    )

    val dragonWalk3 = PixelSprite.compile(
        28, 24,
        listOf(
            "...........KG.KG.KG.........",
            "...........KY.KY.KY.........",
            "...........KYOYrYOK.........",
            "......KKK..KOOOOOOK.........",
            ".....KWWKKKCCCHHHHKK........",
            ".....KttWKCHRRRRRRRHK.......",
            "......KKKCHRRKExKRRCCK......",
            "........KCHRRRRRRRRRRRK.....",
            "........KCHRRRRRRKKKWKK.....",
            "....KK..KDRRRRRRRKKKKK......",
            "...KvvK.KDRRBBBRRRKK........",
            "...KvVVVKDRBUBUBRRHHWK......",
            "KKKvVVRKKDRRBBBBRRKK........",
            "KYHRRRKK.DRRUBUBRRCCHK......",
            "KFRDDKK..KDRRRBBBBRKK.......",
            ".KKKK....KDRRRRRRRDK........",
            "........KDRRKKKKDRDK........",
            "........KDRDK..KDRDK........",
            "........KDRDK..KDRDK........",
            "........KDDDK...KDDK........",
            "........KWWWK...KWWK........",
            "........KKKKK...KKKK........",
            "............................",
            "............................"
        ),
        sharedColors
    )

    val dragonWalk4 = PixelSprite.compile(
        28, 24,
        listOf(
            "............................",
            "...........KG.KG.KG.........",
            "...........KY.KY.KY.........",
            "...........KYOYrYOK.........",
            "......KKK..KOOOOOOK.........",
            ".....KWWKKKCCCHHHHKK........",
            ".....KttWKCHRRRRRRRHK.......",
            "......KKKCHRRKExKRRCCK......",
            "........KCHRRRRRRRRRRRK.....",
            ".....KK.KCHRRRRRRKKKWKK.....",
            "....KvvKKDRRRRRRRKKKKK......",
            "...KvVVVKDRRBBBRRRKK........",
            "...KvVVRKDRBUBUBRRHHWK......",
            ".KKKHRRKKDRRBBBBRRKK........",
            "KYHRRRK.KDRRUBUBRRCCHK......",
            "KFRDDK..KDRRRRBBBBRKK.......",
            ".KKKK...KDRRRRRRRRDK........",
            ".........KDRRKKDRDK.........",
            ".........KDRK.KDRDK.........",
            ".........KWWK.KDDDK.........",
            ".........KKKK.KWWWK.........",
            "..............KKKKK.........",
            "............................",
            "............................"
        ),
        sharedColors
    )

    val dragonJump = PixelSprite.compile(
        28, 24,
        listOf(
            "...........KG.KG.KG.........",
            "..KKK......KY.KY.KY.........",
            ".KvvvKK....KYOYrYOK.........",
            "KvvVVVKKK..KOOOOOOK.........",
            "KvVVVVRKWWKKCCCHHHHKK.......",
            ".KvVVRKKttWKCHRRRRRRHK......",
            "..KKKK..KKKCHRRKExKRRCCK....",
            "..........KCHRRRRRRRRRRRK...",
            "..........KCHRRRRRRKKKWKK...",
            "..........KDRRRRRRRKKKKK....",
            "........KKKDRRBBBRRRKK......",
            ".......KHRKDRBUBUBRRHHWK....",
            "......KCHRKDRRBBBBRRKK......",
            "..KKK..KKK.KDRRUBUBRRCCHK...",
            ".KYHRKK....KDRRRRBBBBRKK....",
            ".KFRDDK....KDRRRRRRRRDK.....",
            "..KKKK.....KDRRKKKDRDK......",
            "..........KDRDK...KDRDK.....",
            "..........KDDK.....KDDK.....",
            "..........KWWK.....KWWK.....",
            "..........KKKK.....KKKK.....",
            "............................",
            "............................",
            "............................",
        ),
        sharedColors
    )

    val dragonFall = PixelSprite.compile(
        28, 24,
        listOf(
            ".KKKK......KG.KG.KG.........",
            "KvvvVKK....KY.KY.KY.........",
            "KvvVVVKK...KYOYrYOK.........",
            ".KvVVVKKK..KOOOOOOK.........",
            "..KvVVRKWWKKCCCHHHHKK.......",
            "...KKKKKttWKCHRRRRRRHK......",
            "........KKKCHRRKExKRRCCK....",
            "..........KCHRRRRRRRRRRRK...",
            "..........KCHRRRRRRKKKWKK...",
            "..........KDRRRRRRRKKKKK....",
            "........KKKDRRBBBRRRKK......",
            ".......KHRKDRBUBUBRRHHWK....",
            "......KCHRKDRRBBBBRRKK......",
            ".KKKK..KKK.KDRRUBUBRRCCHK...",
            "KYHRKK.....KDRRRRBBBBRKK....",
            "KFRDDK.....KDRRRRRRRRDK.....",
            ".KKKK......KDRRKKKDRDK......",
            "..........KDRDK...KDRDK.....",
            "..........KDRDK...KDRDK.....",
            "..........KWWWK...KWWWK.....",
            "..........KKKKK...KKKKK.....",
            "............................",
            "............................",
            "............................"
        ),
        sharedColors
    )

    // Snout/Jaw patches (9x4) stamped at local (18, 7) + headBobY when mouth opens
    val dragonMouthAmbientPatch = PixelSprite.compile(
        9, 4,
        listOf(
            "RRRRRRKK.",
            "RRRFFAxK.",
            "KKKFAFK..",
            "KKKKKKKK."
        ),
        sharedColors
    )

    val dragonMouthFirePatch = PixelSprite.compile(
        9, 4,
        listOf(
            "RRRRRRKK.",
            "RRFFAAxxK",
            "KKFAAFxKK",
            "KKKKKKKK."
        ),
        sharedColors
    )

    // ---------------------------------------------------------
    // 16x16 WORLD TILES (Polished 4-tone shading & crisp bevels)
    // ---------------------------------------------------------
    val tileGrassTop = PixelSprite.compile(
        16, 16,
        listOf(
            "uuuguuuuguuuguuu",
            "uggugggugggguggu",
            "gggeggggggegggeg",
            "eedeeeddeeeeedde",
            "ddkddddklnddkddn",
            "kklmmkkllmkklmml",
            "llmmnmmlmmmnllmm",
            "mmnmmmmnmmmmnmmm",
            "mnmmkmmmmkmmmmnm",
            "mmmmlmmnmmmnmmml",
            "lmmnmmmnmmmnmmmm",
            "mmkmmnmmmmkmnmml",
            "mmnmmmmnmmmmmmnm",
            "nmmmmlmmnmmnmmmm",
            "mmnmmmmmmnmmnmml",
            "mnmnnmmmnmnnmmmn"
        ),
        sharedColors
    )

    val tileDirt = PixelSprite.compile(
        16, 16,
        listOf(
            "llmmnmmlmmnmmmml",
            "mmkmmmnmmmmmmnmm",
            "lmmmmlmmmnmmkmmm",
            "mmnmmnmmlmmnmmmm",
            "mnmmmmmmmnmmmmnm",
            "kkmmnmmmlmmnmmml",
            "mmnmmmmnmmmmnmmm",
            "mlmmnmmlmmmlmmnm",
            "nmmmmkmmmnmmnmmm",
            "mmnmmlmmmnmmmmnl",
            "lmmnmmmmnmmmlmmm",
            "mmmmlmmnmmmmnmmm",
            "mnmmnmmmmkmmmmnm",
            "mmnmmmmlmmmnmmml",
            "lmmmnmmmmnmmmmnm",
            "nmmmnmnmmmnnmmnm"
        ),
        sharedColors
    )

    val tileStone = PixelSprite.compile(
        16, 16,
        listOf(
            "ZZZZZZZZZZZZZZZZ",
            "ZhhhhhhZhhhhhhhZ",
            "ZhsssssZhssssssZ",
            "ZhsSSSzZhsSSSzSZ",
            "ZhsSSSSZhsSSSSSZ",
            "ZzzzzzzZzzzzzzzZ",
            "ZZZZZZZZZZZZZZZZ",
            "ZhhhZhhhhhhhZhhZ",
            "ZhssZhssssssZhsZ",
            "ZhsSZhsSSzSSZhsZ",
            "ZhsSZhsSSSSSZhSZ",
            "ZzzzZzzzzzzzZzzZ",
            "ZZZZZZZZZZZZZZZZ",
            "ZhhhhhhZhhhhhhhZ",
            "ZhsSSSSZhsSSSSSZ",
            "ZZZZZZZZZZZZZZZZ"
        ),
        sharedColors
    )

    val tileMossStone = PixelSprite.compile(
        16, 16,
        listOf(
            "uuuguuuuguuuguuu",
            "uggedgggeggedgge",
            "ZdehhhhZdehhhhhZ",
            "ZdsssssZdssssssZ",
            "ZhsSSSzZhsSSSzSZ",
            "ZzzzzzzZzzzzzzzZ",
            "ZZZZZZZZZZZZZZZZ",
            "ZhhhZhhhhhhhZhhZ",
            "ZhssZhssssssZhsZ",
            "ZhsSZhsSSzSSZhsZ",
            "ZzzzZzzzzzzzZzzZ",
            "ZZZZZZZZZZZZZZZZ",
            "ZhhhhhhZhhhhhhhZ",
            "ZhsSSSSZhsSSSSSZ",
            "ZzzzzzzZzzzzzzzZ",
            "ZZZZZZZZZZZZZZZZ"
        ),
        sharedColors
    )

    val tileCrate = PixelSprite.compile(
        16, 16,
        listOf(
            "KKKKKKKKKKKKKKKK",
            "KwwwwwwwwwwwwwwK",
            "KwbbbbbbbbbbbbpK",
            "KwbccccccccbbcpK",
            "KwbwccccccwbbcpK",
            "KwbcwccccwcbbcpK",
            "KwbcwwccwwcbbcpK",
            "KwbccwwwwccbbcpK",
            "KwbccwwwwccbbcpK",
            "KwbcwwccwwcbbcpK",
            "KwbcwccccwcbbcpK",
            "KwbwccccccwbbcpK",
            "KwbccccccccbbcpK",
            "KwbbbbbbbbbbbbpK",
            "KppppppppppppppK",
            "KKKKKKKKKKKKKKKK"
        ),
        sharedColors
    )

    val tileSpike = PixelSprite.compile(
        16, 16,
        listOf(
            "................",
            "................",
            "................",
            "................",
            "..K.....K.....K.",
            "..K....KiK....K.",
            ".KiK...KiK...KiK",
            ".KifK..KifK..Kif",
            ".KifK.KifIK.KifK",
            "KifIK.KifIK.KifI",
            "KifIJKifIIJKifII",
            "KifIJKKfIIJKKfII",
            "KiIIJKfIIIJKfIII",
            "KjjjjKKjjjjKKjjj",
            "KKKKKKKKKKKKKKKK",
            "ZhhhhhhhhhhhhhhZ"
        ),
        sharedColors
    )

    val tileBrazierOff = PixelSprite.compile(
        16, 16,
        listOf(
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "..KKKKKKKKKKKK..",
            "..KhhhhhhhhhhK..",
            "...KssssssssK...",
            "....KzzzzzzK....",
            ".....KSSSSK.....",
            ".....KsszzK.....",
            "....KhhhhhhK....",
            "...KzzzzzzzzK...",
            "...KKKKKKKKKK..."
        ),
        sharedColors
    )

    val tileBrazierOn1 = PixelSprite.compile(
        16, 16,
        listOf(
            ".......A........",
            "......AFA.......",
            ".....AFxFA......",
            "....RAFxFAR.....",
            "....RFFxFFR.....",
            "...KRAFFFARK....",
            "...KRRAAARRK....",
            "..KKKKKKKKKKKK..",
            "..KhhhhhhhhhhK..",
            "...KssssssssK...",
            "....KzzzzzzK....",
            ".....KSSSSK.....",
            ".....KsszzK.....",
            "....KhhhhhhK....",
            "...KzzzzzzzzK...",
            "...KKKKKKKKKK..."
        ),
        sharedColors
    )

    val tileBrazierOn2 = PixelSprite.compile(
        16, 16,
        listOf(
            "........A.......",
            "......AAFA......",
            ".....AFxxFA.....",
            "....RFFxFFR.....",
            "....RAFxFAR.....",
            "...KRAFFFARK....",
            "...KRRAAARRK....",
            "..KKKKKKKKKKKK..",
            "..KhhhhhhhhhhK..",
            "...KssssssssK...",
            "....KzzzzzzK....",
            ".....KSSSSK.....",
            ".....KsszzK.....",
            "....KhhhhhhK....",
            "...KzzzzzzzzK...",
            "...KKKKKKKKKK..."
        ),
        sharedColors
    )

    val tileGem1 = PixelSprite.compile(
        16, 16,
        listOf(
            "................",
            "......KKKK......",
            ".....KyyyyK.....",
            "....KyyoooyK....",
            "...KyyoaaaoaK...",
            "...KyoayyoaoK...",
            "...KyoaoooraK...",
            "...KyoaaaaMaK...",
            "....KoaMMMaK....",
            ".....KaMMMK.....",
            "......KKKK......",
            "................",
            "................",
            "................",
            "................",
            "................"
        ),
        sharedColors
    )

    val tileGem2 = PixelSprite.compile(
        16, 16,
        listOf(
            ".......y........",
            "......KKKK......",
            ".....KyyyyK.....",
            "....KyyoooyK....",
            "...KyyoaaaoaK...",
            "...KyoayyoaoK...",
            "...KyoaoooraK...",
            "...KyoaaaaMaK...",
            "....KoaMMMaK....",
            ".....KaMMMK.....",
            "......KKKK......",
            ".......y........",
            "................",
            "................",
            "................",
            "................"
        ),
        sharedColors
    )

    val tileCheckpointOff = PixelSprite.compile(
        16, 16,
        listOf(
            "......KK........",
            ".....KifK.......",
            ".....KifKsKK....",
            ".....KifKssszzK.",
            ".....KifKSSSzzK.",
            ".....KifKzzKK...",
            ".....KifK.......",
            ".....KifK.......",
            ".....KifK.......",
            ".....KifK.......",
            ".....KifK.......",
            ".....KifK.......",
            ".....KifK.......",
            "....KhhhhK......",
            "...KzzzzzzK.....",
            "...KKKKKKKK....."
        ),
        sharedColors
    )

    val tileCheckpointOn = PixelSprite.compile(
        16, 16,
        listOf(
            "......KK........",
            ".....KGGK.......",
            ".....KifKRHHKK..",
            ".....KifKRRRYRK.",
            ".....KifKRRRHHK.",
            ".....KifKDDKK...",
            ".....KifK.......",
            ".....KifK.......",
            ".....KifK.......",
            ".....KifK.......",
            ".....KifK.......",
            ".....KifK.......",
            ".....KifK.......",
            "....KhhhhK......",
            "...KzzzzzzK.....",
            "...KKKKKKKK....."
        ),
        sharedColors
    )

    val tileGoalShrine = PixelSprite.compile(
        16, 16,
        listOf(
            "....KKKKKKKK....",
            "...KGGGGGGGGK...",
            "..KYOOOOOOOOYK..",
            "..KYKQQQQQQKYK..",
            "..KYKQyQQyQKYK..",
            "..KYKQQyyQQKYK..",
            "..KYKQQQQQQKYK..",
            "..KYKssQQssKYK..",
            "..KYKKKKKKKKYK..",
            "..KhhhhhhhhhhK..",
            "..KsSSzKKzSSsK..",
            "..KsSSK..KSSsK..",
            "..KsSSK..KSSsK..",
            ".KhhhhK..KhhhhK.",
            "KzzzzzzKKzzzzzzK",
            "KKKKKKKKKKKKKKKK"
        ),
        sharedColors
    )

    // ---------------------------------------------------------
    // ENEMY: SHADOW SLIME (16x12)
    // ---------------------------------------------------------
    val slime1 = PixelSprite.compile(
        16, 12,
        listOf(
            ".....KKKKKK.....",
            "...KKLLPPPPKK...",
            "..KLLPPPPPPJLK..",
            ".KLPPKrrPPKrrJK.",
            ".KPPPKrrPPKrrJK.",
            "KLPPPJJJJJJJJJLK",
            "KPPJJJJJJJJJJJJK",
            "KPJJXXXXXXXXXJJk",
            "KXXXXXXXXXXXXXXK",
            ".KKKKKKKKKKKKKK.",
            "................",
            "................"
        ),
        sharedColors
    )

    val slime2 = PixelSprite.compile(
        16, 12,
        listOf(
            "................",
            "....KKKKKKKK....",
            "..KKLLPPPPPPKK..",
            ".KLLPPPPPPPPPJK.",
            "KLPPKrrPPKrrJJJK",
            "KPPPKrrPPKrrJJJK",
            "KPPJJJJJJJJJJJJK",
            "KPJXXXXXXXXXXJJK",
            "KXXXXXXXXXXXXXXK",
            "KKKKKKKKKKKKKKKK",
            "................",
            "................"
        ),
        sharedColors
    )

    // ---------------------------------------------------------
    // BACKGROUND CODE-DRIVEN SCENERY SPRITES
    // ---------------------------------------------------------
    val cloudSmall = PixelSprite.compile(
        26, 11,
        listOf(
            "........0000000...........",
            "......00111111100.........",
            "....001111111111100.......",
            "..001111111111111110......",
            ".01111111111111111111.....",
            "01112221111222111122211...",
            "112222222222222222222222..",
            "2233222233222233222233222.",
            ".33333333333333333333333..",
            "...3333333333333333333....",
            ".........................."
        ),
        sharedColors
    )

    val treePine = PixelSprite.compile(
        16, 26,
        listOf(
            ".......44.......",
            "......4454......",
            ".....445554.....",
            ".....455655.....",
            "....45566654....",
            "......5665......",
            "....44556544....",
            "...4455666554...",
            "..455666766554..",
            "..667777777766..",
            "....45566554....",
            "..445566665544..",
            ".45566666666554.",
            "5566677777766655",
            "6777777777777776",
            "...7777777777...",
            "......qTN.......",
            "......qTN.......",
            "......qTN.......",
            "......qTN.......",
            "......qTN.......",
            ".....qqTNN......",
            ".....qTTNN......",
            "....qqTTNNN.....",
            "................",
            "................"
        ),
        sharedColors
    )

    val treeOak = PixelSprite.compile(
        22, 24,
        listOf(
            "......4444444444......",
            "....44555555555544....",
            "..445554555555565544..",
            ".44555445555566665544.",
            ".45555555556666766554.",
            "4555655556666777666554",
            "5566666666677777766655",
            "5666776666777777776655",
            "6777777777777777777776",
            ".77777777777777777777.",
            "...7777777777777777...",
            ".......qqTTTN.........",
            "........qTTTN.........",
            "........qTTTN.........",
            "........qTTTN.........",
            "........qTTTN.........",
            "........qTTTN.........",
            ".......qqTTTNN........",
            "......qqTTTTNNN.......",
            "......................",
            "......................",
            "......................",
            "......................",
            "......................"
        ),
        sharedColors
    )

    val bush = PixelSprite.compile(
        16, 8,
        listOf(
            "....44444444....",
            "..445555555544..",
            ".44544555565544.",
            "4555555666665554",
            "5566676667776655",
            "7777777777777777",
            ".77777777777777.",
            "................"
        ),
        sharedColors
    )
}
