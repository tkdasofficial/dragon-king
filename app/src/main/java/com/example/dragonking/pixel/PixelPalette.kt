package com.example.dragonking.pixel

/**
 * Rich, cohesive 16-bit indie pixel-art color palette with proper highlight/mid/shadow ramps
 * and glow tints for atmospheric depth.
 */
object PixelPalette {
    const val TRANSPARENT = 0x00000000

    // Twilight-to-Dawn Horizon Sky Ramp (8 cohesive bands)
    val SKY_BANDS = intArrayOf(
        0xFF141842.toInt(), // Deep midnight indigo
        0xFF1C2559.toInt(), // Upper royal twilight
        0xFF273773.toInt(), // Deep azure
        0xFF354C8C.toInt(), // Mid cobalt
        0xFF4B68A6.toInt(), // Soft sky blue
        0xFF6A85BD.toInt(), // Horizon blue-mist
        0xFF939FD1.toInt(), // Warm lavender horizon
        0xFFC2A8C9.toInt()  // Subtle dawn haze
    )

    // Sun & Atmospheric Glow
    const val SUN_CORE = 0xFFFFFCE0.toInt()
    const val SUN_INNER = 0xFFFFE57F.toInt()
    const val SUN_HALO = 0xFFFFB74D.toInt()
    const val SUN_GLOW_WARM = 0xFFFF9800.toInt()

    // Clouds (4-tone shading)
    const val CLOUD_HIGHLIGHT = 0xFFFFFFFF.toInt()
    const val CLOUD_LIGHT = 0xFFEEF2FF.toInt()
    const val CLOUD_MID = 0xFFC5CEE8.toInt()
    const val CLOUD_SHADOW = 0xFF93A1C9.toInt()

    // Parallax Mountains (Lit left slope, shaded right slope, snowy caps)
    const val MTN_FAR_LIT = 0xFF4B5791.toInt()
    const val MTN_FAR_SHADE = 0xFF353E6E.toInt()
    const val MTN_FAR_SNOW_LIT = 0xFFE4EAFF.toInt()
    const val MTN_FAR_SNOW_SHADE = 0xFFAAB8E6.toInt()

    const val MTN_MID_LIT = 0xFF394270.toInt()
    const val MTN_MID_SHADE = 0xFF242A4F.toInt()
    const val MTN_MID_RIM = 0xFF5D6BA8.toInt()

    // Rolling Foreground Hills
    const val HILL_HIGHLIGHT = 0xFF3F8A4D.toInt()
    const val HILL_TOP = 0xFF2C6E42.toInt()
    const val HILL_BODY = 0xFF1D4D33.toInt()
    const val HILL_DEEP = 0xFF153827.toInt()

    // Trees & Foliage (Rich 4-tone canopy & bark)
    const val TRUNK_HIGHLIGHT = 0xFF8C5835.toInt()
    const val TRUNK_LIGHT = 0xFF6B3E23.toInt()
    const val TRUNK_DARK = 0xFF422313.toInt()
    const val LEAF_HIGHLIGHT = 0xFF82D656.toInt()
    const val LEAF_LIGHT = 0xFF4EAD45.toInt()
    const val LEAF_MID = 0xFF2A7A3A.toInt()
    const val LEAF_DARK = 0xFF174A28.toInt()

    // Terrain Tiles
    const val OUTLINE = 0xFF140C1C.toInt()
    const val GRASS_RIM = 0xFFA8F060.toInt()
    const val GRASS_BRIGHT = 0xFF68CE3E.toInt()
    const val GRASS_MID = 0xFF389635.toInt()
    const val GRASS_DARK = 0xFF1E5E27.toInt()

    const val DIRT_HIGHLIGHT = 0xFFB87444.toInt()
    const val DIRT_LIGHT = 0xFF94562E.toInt()
    const val DIRT_MID = 0xFF703D1F.toInt()
    const val DIRT_DARK = 0xFF472312.toInt()

    const val STONE_HIGHLIGHT = 0xFFC2CCE0.toInt()
    const val STONE_LIGHT = 0xFF929EB8.toInt()
    const val STONE_MID = 0xFF626D87.toInt()
    const val STONE_DARK = 0xFF3B4357.toInt()
    const val STONE_MORTAR = 0xFF242938.toInt()

    const val WOOD_HIGHLIGHT = 0xFFF0B465.toInt()
    const val WOOD_LIGHT = 0xFFC9853E.toInt()
    const val WOOD_MID = 0xFF945724.toInt()
    const val WOOD_DARK = 0xFF593013.toInt()

    const val STEEL_BRIGHT = 0xFFFFFFFF.toInt()
    const val STEEL_LIGHT = 0xFFC7D3EB.toInt()
    const val STEEL_MID = 0xFF7C8AA6.toInt()
    const val STEEL_DARK = 0xFF3F485E.toInt()

    // Dragon King Sprite (Rich crimson, sunlit coral rim, deep burgundy shadow, royal gold)
    const val DRAGON_SCALE_RIM = 0xFFFF8575.toInt()
    const val DRAGON_SCALE_LIGHT = 0xFFFF4D42.toInt()
    const val DRAGON_SCALE_MID = 0xFFD92121.toInt()
    const val DRAGON_SCALE_DARK = 0xFF820E19.toInt()
    const val DRAGON_WING_LIGHT = 0xFF9E2448.toInt()
    const val DRAGON_WING_MEMBRANE = 0xFF57122D.toInt()
    const val DRAGON_BELLY_LIGHT = 0xFFFFEEAD.toInt()
    const val DRAGON_BELLY_DARK = 0xFFD69524.toInt()
    const val DRAGON_GOLD_HIGHLIGHT = 0xFFFFF8A6.toInt()
    const val DRAGON_GOLD_LIGHT = 0xFFFFD53D.toInt()
    const val DRAGON_GOLD_DARK = 0xFFC97300.toInt()
    const val DRAGON_HORN = 0xFFFFF6DC.toInt()
    const val DRAGON_HORN_SHADE = 0xFFD4B587.toInt()
    const val DRAGON_EYE = 0xFFFFF475.toInt()

    // Fire Breath, Embers & Glow Tints
    const val FIRE_WHITE = 0xFFFFFDE8.toInt()
    const val FIRE_YELLOW = 0xFFFFEA4D.toInt()
    const val FIRE_ORANGE = 0xFFFF8800.toInt()
    const val FIRE_RED = 0xFFE52800.toInt()
    const val FIRE_CRIMSON = 0xFF991200.toInt()
    const val FIRE_SMOKE = 0xFF47333B.toInt()

    // Collectibles, Shrine, Enemies
    const val GEM_WHITE = 0xFFFFFFFF.toInt()
    const val GEM_LIGHT = 0xFFFFF176.toInt()
    const val GEM_MID = 0xFFFFB300.toInt()
    const val GEM_DARK = 0xFFD85600.toInt()
    const val RUBY_GLOW = 0xFFFF1F4B.toInt()
    const val SHRINE_GLOW = 0xFF40E8FF.toInt()

    const val SLIME_HIGHLIGHT = 0xFFE1BEE7.toInt()
    const val SLIME_LIGHT = 0xFFAB47BC.toInt()
    const val SLIME_MID = 0xFF6A1B9A.toInt()
    const val SLIME_DARK = 0xFF38006B.toInt()
}
