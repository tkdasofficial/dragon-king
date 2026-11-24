package com.example.dragonking.pixel

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.example.dragonking.engine.GameConstants
import com.example.dragonking.engine.GameEngine
import com.example.dragonking.engine.ParticleKind
import com.example.dragonking.engine.TileType
import com.example.dragonking.models.dragons.reddragon.VoxelDragonRasterizer
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Zero-allocation software pixel buffer renderer with rich multi-tone shading,
 * layered parallax silhouettes, and stepped pixel-grid glow halos.
 */
class PixelRenderer {
    var virtualWidth: Int = 340
        private set
    val virtualHeight: Int = GameConstants.VIRTUAL_HEIGHT

    private var pixelBuffer = IntArray(virtualWidth * virtualHeight)
    private var bitmapA: Bitmap? = null
    private var bitmapB: Bitmap? = null
    private var useBufferA = true
    private val srcRect by lazy { Rect(0, 0, virtualWidth, virtualHeight) }
    private val dstRect by lazy { Rect() }
    private val dragonRasterizer = VoxelDragonRasterizer()
    private val mouthOffsetScratch = FloatArray(2)
    private val nearestPaint by lazy {
        Paint().apply {
            isAntiAlias = false
            isFilterBitmap = false
            isDither = false
        }
    }

    fun updateVirtualWidth(canvasWidth: Int, canvasHeight: Int, allocateBitmap: Boolean = true) {
        if (canvasHeight <= 0 || canvasWidth <= 0) return
        val aspect = canvasWidth.toFloat() / canvasHeight.toFloat()
        val desiredW = (virtualHeight * aspect).roundToInt()
            .coerceIn(GameConstants.MIN_VIRTUAL_WIDTH, GameConstants.MAX_VIRTUAL_WIDTH)
        if (desiredW != virtualWidth || (allocateBitmap && (bitmapA == null || bitmapB == null))) {
            virtualWidth = desiredW
            if (pixelBuffer.size != virtualWidth * virtualHeight) {
                pixelBuffer = IntArray(virtualWidth * virtualHeight)
            }
            if (allocateBitmap) {
                bitmapA = Bitmap.createBitmap(virtualWidth, virtualHeight, Bitmap.Config.ARGB_8888)
                bitmapB = Bitmap.createBitmap(virtualWidth, virtualHeight, Bitmap.Config.ARGB_8888)
                srcRect.set(0, 0, virtualWidth, virtualHeight)
            }
        }
    }

    fun renderToBuffer(engine: GameEngine, targetWidth: Int, targetHeight: Int, allocateBitmap: Boolean = false): IntArray {
        updateVirtualWidth(targetWidth, targetHeight, allocateBitmap = allocateBitmap)

        val shakeX = if (engine.screenShakeTimer > 0f) {
            ((engine.animTime * 90f).toInt() % 3) - 1
        } else 0
        val camX = engine.cameraX.roundToInt() + shakeX
        val camY = engine.cameraY.roundToInt()

        // 1. Stepped Retro Sky + Twinkling Stars + Atmospheric Sun Glow
        drawSkyAndSun(camX, engine.animTime)

        // 2. Parallax Clouds & Jagged Shaded Far Mountains (0.16x)
        drawClouds(camX, engine.animTime)
        drawJaggedMountains(
            scrollX = (camX * 0.16f).roundToInt(),
            baseY = 150,
            period = 96,
            baseHeight = 56,
            litColor = PixelPalette.MTN_FAR_LIT,
            shadeColor = PixelPalette.MTN_FAR_SHADE,
            snowLit = PixelPalette.MTN_FAR_SNOW_LIT,
            snowShade = PixelPalette.MTN_FAR_SNOW_SHADE,
            snowThreshold = 15,
            rimHighlight = false
        )

        // 3. Midground Jagged Shaded Peaks (0.34x)
        drawJaggedMountains(
            scrollX = (camX * 0.34f).roundToInt(),
            baseY = 166,
            period = 72,
            baseHeight = 40,
            litColor = PixelPalette.MTN_MID_LIT,
            shadeColor = PixelPalette.MTN_MID_SHADE,
            snowLit = PixelPalette.MTN_MID_RIM,
            snowShade = PixelPalette.MTN_MID_SHADE,
            snowThreshold = 3,
            rimHighlight = true
        )

        // 4. Rolling Foreground Hills + Pixel Trees & Bushes (0.60x)
        drawHillsAndTrees(scrollX = (camX * 0.60f).roundToInt())

        // 5. Collectible, Brazier & Shrine Ambient Pixel Glows (behind tiles)
        drawWorldGlows(engine, camX, camY)

        // 6. Tilemap World Layer (1.0x)
        drawTiles(engine, camX, camY)

        // 7. Shadow Slimes
        drawSlimes(engine, camX, camY)

        // 8. Dragon King Character + Subtle Ground Contact Shadow + Mouth Fire Glow
        drawDragonKing(engine, camX, camY)

        // 9. Pixel Fire Breath Glow + Embers, Flames & Smoke Particles
        drawParticles(engine, camX, camY)

        return pixelBuffer
    }

    fun render(engine: GameEngine, targetCanvas: Canvas, targetWidth: Int, targetHeight: Int) {
        if (targetWidth <= 0 || targetHeight <= 0) return
        renderToBuffer(engine, targetWidth, targetHeight, allocateBitmap = true)

        // 10. Blit low-res buffer with Nearest-Neighbor filtering using double-buffered Bitmaps
        val bmp = if (useBufferA) {
            bitmapA ?: Bitmap.createBitmap(virtualWidth, virtualHeight, Bitmap.Config.ARGB_8888).also {
                bitmapA = it
            }
        } else {
            bitmapB ?: Bitmap.createBitmap(virtualWidth, virtualHeight, Bitmap.Config.ARGB_8888).also {
                bitmapB = it
            }
        }
        useBufferA = !useBufferA
        bmp.setPixels(pixelBuffer, 0, virtualWidth, 0, 0, virtualWidth, virtualHeight)
        srcRect.set(0, 0, virtualWidth, virtualHeight)
        dstRect.set(0, 0, targetWidth, targetHeight)
        targetCanvas.drawBitmap(bmp, srcRect, dstRect, nearestPaint)
    }

    private fun drawSkyAndSun(camX: Int, animTime: Float) {
        val bands = PixelPalette.SKY_BANDS
        val bandH = (virtualHeight / bands.size).coerceAtLeast(1)
        for (y in 0 until virtualHeight) {
            val idx = (y / bandH).coerceAtMost(bands.lastIndex)
            val nextIdx = (idx + 1).coerceAtMost(bands.lastIndex)
            val localY = y % bandH
            val rowOffset = y * virtualWidth
            val baseColor = bands[idx]
            if (localY >= bandH - 2 && idx != nextIdx) {
                val nextColor = bands[nextIdx]
                for (x in 0 until virtualWidth) {
                    pixelBuffer[rowOffset + x] = if (((x + y) and 1) == 0) nextColor else baseColor
                }
            } else {
                pixelBuffer.fill(baseColor, rowOffset, rowOffset + virtualWidth)
            }
        }

        // Subtle twinkling pixel stars in deep upper sky
        val starScroll = (camX * 0.03f).roundToInt()
        for (i in 0 until 16) {
            val sx = Math.floorMod(i * 47 + 19 - starScroll, virtualWidth)
            val sy = (i * 23 + 7) % 52
            val twinkle = sin(animTime * 2.5f + i * 1.7f)
            if (twinkle > 0.1f) {
                val alpha = if (twinkle > 0.65f) 210 else 110
                blendPixel(sx, sy, PixelPalette.CLOUD_HIGHLIGHT, alpha)
            }
        }

        // Warm atmospheric radial pixel glow around the sun
        val sunCx = virtualWidth - 64 - ((camX * 0.04f).roundToInt() % 36)
        val sunCy = 32
        drawRadialPixelGlow(sunCx, sunCy, radius = 32, glowColor = PixelPalette.SUN_GLOW_WARM, maxAlpha = 88)
        drawRadialPixelGlow(sunCx, sunCy, radius = 18, glowColor = PixelPalette.SUN_INNER, maxAlpha = 115)

        // Crisp 3-tone blocky sun disc
        for (dy in -10..10) {
            for (dx in -10..10) {
                val d2 = dx * dx + dy * dy
                if (d2 <= 104) {
                    val col = when {
                        d2 <= 38 -> PixelPalette.SUN_CORE
                        d2 <= 74 -> PixelPalette.SUN_INNER
                        else -> PixelPalette.SUN_HALO
                    }
                    setPixel(sunCx + dx, sunCy + dy, col)
                }
            }
        }
    }

    private fun drawClouds(camX: Int, animTime: Float) {
        val drift = (animTime * 5f).roundToInt()
        val scroll = (camX * 0.11f).roundToInt() + drift
        val span = virtualWidth + 96
        val positions = intArrayOf(24, 136, 252, 358)
        val heights = intArrayOf(18, 36, 14, 28)
        for (i in positions.indices) {
            val sx = Math.floorMod(positions[i] - scroll, span) - 36
            blitSprite(SpriteAtlas.cloudSmall, sx, heights[i], flipX = (i % 2 == 1))
        }
    }

    private fun drawJaggedMountains(
        scrollX: Int,
        baseY: Int,
        period: Int,
        baseHeight: Int,
        litColor: Int,
        shadeColor: Int,
        snowLit: Int,
        snowShade: Int,
        snowThreshold: Int,
        rimHighlight: Boolean
    ) {
        val half = period / 2
        for (x in 0 until virtualWidth) {
            val wx = x + scrollX
            val peakIdx = Math.floorDiv(wx, period)
            val phase = Math.floorMod(wx, period)
            // Deterministic varied peak height per mountain + subtle crag modulation
            val peakVar = ((peakIdx * 37) and 0x7) * 3 - 8
            val peakH = (baseHeight + peakVar).coerceAtLeast(22)
            val distFromPeak = abs(phase - half)

            // Secondary jagged ridge notches along slope
            val crag = (((wx * 13 + peakIdx * 7) and 3) - 1)
            val slopeHeight = peakH - (distFromPeak * peakH) / half + crag
            val h = slopeHeight.coerceIn(6, peakH + 4)

            val topY = (baseY - h).coerceAtLeast(16)
            val bottomY = baseY.coerceAtMost(virtualHeight - 1)
            val isLitSide = phase <= half

            for (y in topY..bottomY) {
                val depthFromTop = y - topY
                val snowJitter = if ((wx and 3) == 0) 2 else 0
                val color = when {
                    rimHighlight && depthFromTop < 2 && isLitSide -> snowLit
                    !rimHighlight && depthFromTop < snowThreshold + snowJitter -> {
                        if (isLitSide) snowLit else snowShade
                    }
                    isLitSide -> litColor
                    else -> shadeColor
                }
                pixelBuffer[y * virtualWidth + x] = color
            }
        }
    }

    private fun drawHillsAndTrees(scrollX: Int) {
        val hillPeriod = 128
        for (x in 0 until virtualWidth) {
            val wx = x + scrollX
            val hillIdx = Math.floorDiv(wx, hillPeriod)
            val phase = Math.floorMod(wx, hillPeriod)
            val norm = (phase - hillPeriod / 2).toFloat() / (hillPeriod / 2f)
            val amp = 18f + (Math.floorMod(hillIdx * 5, 3) * 4f)
            val h = ((1f - norm * norm) * amp).toInt()
            val topY = (166 - h).coerceIn(128, virtualHeight - 1)
            for (y in topY until virtualHeight) {
                val depth = y - topY
                pixelBuffer[y * virtualWidth + x] = when {
                    depth == 0 -> PixelPalette.HILL_HIGHLIGHT
                    depth < 4 -> PixelPalette.HILL_TOP
                    depth < 14 -> PixelPalette.HILL_BODY
                    else -> PixelPalette.HILL_DEEP
                }
            }
        }

        // Code-driven trees & bushes anchored cleanly to hill contour
        val treeStep = 68
        val firstTree = ((scrollX - 28) / treeStep) * treeStep
        var tx = firstTree
        while (tx < scrollX + virtualWidth + 28) {
            val screenX = tx - scrollX
            val idx = Math.floorDiv(tx, treeStep)
            val hillIdx = Math.floorDiv(tx + 8, hillPeriod)
            val phase = Math.floorMod(tx + 8, hillPeriod)
            val norm = (phase - hillPeriod / 2).toFloat() / (hillPeriod / 2f)
            val amp = 18f + (Math.floorMod(hillIdx * 5, 3) * 4f)
            val hillTopY = (166 - ((1f - norm * norm) * amp).toInt()).coerceIn(128, virtualHeight - 1)

            when (Math.floorMod(idx, 4)) {
                0 -> blitSprite(SpriteAtlas.treePine, screenX, hillTopY - 23)
                1 -> blitSprite(SpriteAtlas.treeOak, screenX, hillTopY - 19)
                2 -> blitSprite(SpriteAtlas.bush, screenX, hillTopY - 6)
                else -> blitSprite(SpriteAtlas.treePine, screenX, hillTopY - 22, flipX = true)
            }
            tx += treeStep
        }
    }

    private fun drawWorldGlows(engine: GameEngine, camX: Int, camY: Int) {
        val ts = GameConstants.TILE_SIZE
        val startCol = (camX / ts - 1).coerceAtLeast(0)
        val endCol = ((camX + virtualWidth) / ts + 1).coerceAtMost(engine.levelCols - 1)

        for (r in 0 until engine.levelRows) {
            val cy = r * ts + 8 - camY
            for (c in startCol..endCol) {
                val tile = engine.grid[r][c]
                val cx = c * ts + 8 - camX
                if (tile == TileType.GEM) {
                    val pulse = 0.78f + 0.22f * sin(engine.animTime * 4.5f + c * 0.9f)
                    drawRadialPixelGlow(cx, cy - 2, 11, PixelPalette.GEM_LIGHT, (68 * pulse).toInt())
                } else if (tile == TileType.BRAZIER_ON) {
                    val flicker = 0.8f + 0.2f * sin(engine.animTime * 16f + c)
                    drawRadialPixelGlow(cx, cy - 5, 18, PixelPalette.FIRE_ORANGE, (96 * flicker).toInt())
                } else if (tile == TileType.GOAL) {
                    val pulse = 0.75f + 0.25f * sin(engine.animTime * 3.5f)
                    drawRadialPixelGlow(cx, cy - 3, 20, PixelPalette.SHRINE_GLOW, (95 * pulse).toInt())
                }
            }
        }
    }

    private fun drawTiles(engine: GameEngine, camX: Int, camY: Int) {
        val ts = GameConstants.TILE_SIZE
        val startCol = (camX / ts - 1).coerceAtLeast(0)
        val endCol = ((camX + virtualWidth) / ts + 1).coerceAtMost(engine.levelCols - 1)
        val frameTick = (engine.animTime * 7f).toInt()

        for (r in 0 until engine.levelRows) {
            val sy = r * ts - camY
            for (c in startCol..endCol) {
                val tile = engine.grid[r][c]
                if (tile == TileType.EMPTY) continue
                val sx = c * ts - camX
                val sprite = when (tile) {
                    TileType.GRASS_TOP -> SpriteAtlas.tileGrassTop
                    TileType.DIRT -> SpriteAtlas.tileDirt
                    TileType.STONE -> SpriteAtlas.tileStone
                    TileType.MOSS_STONE -> SpriteAtlas.tileMossStone
                    TileType.CRATE -> SpriteAtlas.tileCrate
                    TileType.SPIKE -> SpriteAtlas.tileSpike
                    TileType.BRAZIER_OFF -> SpriteAtlas.tileBrazierOff
                    TileType.BRAZIER_ON -> if ((frameTick + c) % 2 == 0) SpriteAtlas.tileBrazierOn1 else SpriteAtlas.tileBrazierOn2
                    TileType.GEM -> if ((frameTick + c) % 3 == 0) SpriteAtlas.tileGem2 else SpriteAtlas.tileGem1
                    TileType.CHECKPOINT_OFF -> SpriteAtlas.tileCheckpointOff
                    TileType.CHECKPOINT_ON -> SpriteAtlas.tileCheckpointOn
                    TileType.GOAL -> SpriteAtlas.tileGoalShrine
                    TileType.EMPTY -> null
                }
                if (sprite != null) {
                    blitSprite(sprite, sx, sy)
                }
            }
        }
    }

    private fun drawSlimes(engine: GameEngine, camX: Int, camY: Int) {
        val frame = ((engine.animTime * 5f).toInt() % 2)
        val sprite = if (frame == 0) SpriteAtlas.slime1 else SpriteAtlas.slime2
        for (slime in engine.slimes) {
            if (!slime.alive) continue
            val sx = slime.x.roundToInt() - camX - 1
            val sy = slime.y.roundToInt() - camY
            blitSprite(sprite, sx, sy, flipX = slime.vx < 0f)
        }
    }

    private fun drawDragonKing(engine: GameEngine, camX: Int, camY: Int) {
        if (engine.isDead) return

        // Ground shadow directly below Red Dragon when grounded
        if (engine.onGround) {
            val shadowX = engine.dragonX.roundToInt() - camX + 1
            val shadowY = (engine.dragonY + GameConstants.DRAGON_HEIGHT).roundToInt() - camY
            for (dx in 0 until 10) {
                blendPixel(shadowX + dx, shadowY, PixelPalette.OUTLINE, 95)
            }
        }

        if (engine.invulnTimer > 0f && ((engine.animTime * 18f).toInt() % 2 == 0)) {
            return
        }

        val isFireBlast = engine.fireTimer > 0f
        val isMouthOpen = isFireBlast || engine.ambientMouthOpen
        engine.dragonModel.mouthOffsetPx(engine.facingRight, mouthOffsetScratch)
        val mouthScreenX = (engine.dragonX + mouthOffsetScratch[0]).roundToInt() - camX
        val mouthScreenY = (engine.dragonY + mouthOffsetScratch[1]).roundToInt() - camY

        // Subtle mouth glow when ambient mouth opens; strong fiery glow when blasting Fire
        if (isFireBlast) {
            drawRadialPixelGlow(mouthScreenX, mouthScreenY, 17, PixelPalette.FIRE_ORANGE, 102)
        } else if (isMouthOpen) {
            drawRadialPixelGlow(mouthScreenX, mouthScreenY, 8, PixelPalette.FIRE_YELLOW, 52)
        }

        // Render articulated 3D Red Dragon voxel rig quantized cleanly into the 16-bit pixel buffer
        dragonRasterizer.renderDragon(
            model = engine.dragonModel,
            facingRight = engine.facingRight,
            hitboxScreenX = engine.dragonX.roundToInt() - camX,
            hitboxScreenY = engine.dragonY.roundToInt() - camY,
            hitboxWidth = GameConstants.DRAGON_WIDTH.roundToInt(),
            hitboxHeight = GameConstants.DRAGON_HEIGHT.roundToInt(),
            destBuffer = pixelBuffer,
            destWidth = virtualWidth,
            destHeight = virtualHeight
        )
    }

    private fun drawParticles(engine: GameEngine, camX: Int, camY: Int) {
        // Pass 1: Soft pixel glow behind active flame & ambient mouth sparks
        for (i in engine.particles.indices) {
            val p = engine.particles[i]
            if (!p.active) continue
            if (p.kind == ParticleKind.FLAME && (i and 1) == 0) {
                val px = p.x.roundToInt() - camX
                val py = p.y.roundToInt() - camY
                val progress = (p.life / p.maxLife).coerceIn(0f, 1f)
                val alpha = ((1f - progress) * 64f).toInt().coerceIn(0, 68)
                if (alpha > 8) {
                    drawRadialPixelGlow(px + 1, py + 1, 8, PixelPalette.FIRE_ORANGE, alpha)
                }
            } else if (p.kind == ParticleKind.AMBIENT_SPARK) {
                val px = p.x.roundToInt() - camX
                val py = p.y.roundToInt() - camY
                val progress = (p.life / p.maxLife).coerceIn(0f, 1f)
                val alpha = ((1f - progress) * 48f).toInt().coerceIn(0, 50)
                if (alpha > 6) {
                    drawRadialPixelGlow(px, py, 5, PixelPalette.FIRE_YELLOW, alpha)
                }
            }
        }

        // Pass 2: Crisp blocky pixel particles
        for (p in engine.particles) {
            if (!p.active) continue
            val progress = (p.life / p.maxLife).coerceIn(0f, 1f)
            val px = p.x.roundToInt() - camX
            val py = p.y.roundToInt() - camY
            if (p.kind == ParticleKind.SMOKE) {
                blendRect(px, py, p.size, p.size, PixelPalette.FIRE_SMOKE, ((1f - progress) * 145).toInt())
            } else {
                val color = when (p.kind) {
                    ParticleKind.FLAME -> when {
                        progress < 0.16f -> PixelPalette.FIRE_WHITE
                        progress < 0.38f -> PixelPalette.FIRE_YELLOW
                        progress < 0.68f -> PixelPalette.FIRE_ORANGE
                        progress < 0.86f -> PixelPalette.FIRE_RED
                        else -> PixelPalette.FIRE_CRIMSON
                    }
                    ParticleKind.AMBIENT_SPARK -> when {
                        progress < 0.35f -> PixelPalette.FIRE_WHITE
                        progress < 0.72f -> PixelPalette.FIRE_YELLOW
                        else -> PixelPalette.FIRE_ORANGE
                    }
                    ParticleKind.SPARK -> if (progress < 0.5f) PixelPalette.FIRE_YELLOW else PixelPalette.FIRE_ORANGE
                    ParticleKind.WOOD_ASH -> if (progress < 0.45f) PixelPalette.WOOD_LIGHT else PixelPalette.WOOD_DARK
                    ParticleKind.SMOKE -> PixelPalette.FIRE_SMOKE
                }
                fillRect(px, py, p.size, p.size, color)
            }
        }
    }

    private fun drawRadialPixelGlow(cx: Int, cy: Int, radius: Int, glowColor: Int, maxAlpha: Int) {
        if (maxAlpha <= 0 || radius <= 0) return
        val rSq = radius * radius
        val minY = maxOf(0, cy - radius)
        val maxY = minOf(virtualHeight - 1, cy + radius)
        val minX = maxOf(0, cx - radius)
        val maxX = minOf(virtualWidth - 1, cx + radius)
        if (minX > maxX || minY > maxY) return

        val gr = (glowColor ushr 16) and 0xFF
        val gg = (glowColor ushr 8) and 0xFF
        val gb = glowColor and 0xFF

        for (y in minY..maxY) {
            val dy = y - cy
            val dy2 = dy * dy
            val rowOffset = y * virtualWidth
            for (x in minX..maxX) {
                val dx = x - cx
                val d2 = dx * dx + dy2
                if (d2 >= rSq) continue
                // Quantized concentric ring falloff for authentic indie pixel glow
                val norm = 1f - (d2.toFloat() / rSq.toFloat())
                val quantized = ((norm * norm * 5f).toInt() / 5f)
                val alpha = (maxAlpha * quantized).toInt()
                if (alpha <= 2) continue

                val dst = pixelBuffer[rowOffset + x]
                val dr = (dst ushr 16) and 0xFF
                val dg = (dst ushr 8) and 0xFF
                val db = dst and 0xFF
                val inv = 255 - alpha
                val nr = ((dr * inv + gr * alpha) ushr 8).coerceAtMost(255)
                val ng = ((dg * inv + gg * alpha) ushr 8).coerceAtMost(255)
                val nb = ((db * inv + gb * alpha) ushr 8).coerceAtMost(255)
                pixelBuffer[rowOffset + x] = (0xFF shl 24) or (nr shl 16) or (ng shl 8) or nb
            }
        }
    }

    private fun blitSprite(sprite: PixelSprite, destX: Int, destY: Int, flipX: Boolean = false) {
        if (destX + sprite.width <= 0 || destX >= virtualWidth || destY + sprite.height <= 0 || destY >= virtualHeight) {
            return
        }
        val startY = maxOf(0, -destY)
        val endY = minOf(sprite.height, virtualHeight - destY)
        val startX = maxOf(0, -destX)
        val endX = minOf(sprite.width, virtualWidth - destX)

        val srcPixels = sprite.pixels
        val sw = sprite.width
        for (sy in startY until endY) {
            val dstRow = (destY + sy) * virtualWidth
            val srcRow = sy * sw
            for (sx in startX until endX) {
                val srcCol = if (flipX) (sw - 1 - sx) else sx
                val color = srcPixels[srcRow + srcCol]
                if (color != PixelPalette.TRANSPARENT) {
                    pixelBuffer[dstRow + destX + sx] = color
                }
            }
        }
    }

    private fun fillRect(x: Int, y: Int, w: Int, h: Int, color: Int) {
        val startX = maxOf(0, x)
        val endX = minOf(virtualWidth, x + w)
        val startY = maxOf(0, y)
        val endY = minOf(virtualHeight, y + h)
        if (startX >= endX || startY >= endY) return
        for (py in startY until endY) {
            pixelBuffer.fill(color, py * virtualWidth + startX, py * virtualWidth + endX)
        }
    }

    private fun blendRect(x: Int, y: Int, w: Int, h: Int, color: Int, alpha: Int) {
        if (alpha <= 0) return
        val startX = maxOf(0, x)
        val endX = minOf(virtualWidth, x + w)
        val startY = maxOf(0, y)
        val endY = minOf(virtualHeight, y + h)
        for (py in startY until endY) {
            for (px in startX until endX) {
                blendPixel(px, py, color, alpha)
            }
        }
    }

    private fun blendPixel(x: Int, y: Int, color: Int, alpha: Int) {
        if (x !in 0 until virtualWidth || y !in 0 until virtualHeight || alpha <= 0) return
        val a = alpha.coerceIn(0, 255)
        val inv = 255 - a
        val idx = y * virtualWidth + x
        val dst = pixelBuffer[idx]
        val sr = (color ushr 16) and 0xFF
        val sg = (color ushr 8) and 0xFF
        val sb = color and 0xFF
        val dr = (dst ushr 16) and 0xFF
        val dg = (dst ushr 8) and 0xFF
        val db = dst and 0xFF
        val nr = ((dr * inv + sr * a) ushr 8).coerceAtMost(255)
        val ng = ((dg * inv + sg * a) ushr 8).coerceAtMost(255)
        val nb = ((db * inv + sb * a) ushr 8).coerceAtMost(255)
        pixelBuffer[idx] = (0xFF shl 24) or (nr shl 16) or (ng shl 8) or nb
    }

    private fun setPixel(x: Int, y: Int, color: Int) {
        if (x in 0 until virtualWidth && y in 0 until virtualHeight) {
            pixelBuffer[y * virtualWidth + x] = color
        }
    }
}
