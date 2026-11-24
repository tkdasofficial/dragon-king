package com.example.dragonking

import com.example.dragonking.engine.GameConstants
import com.example.dragonking.engine.GameEngine
import com.example.dragonking.engine.InputState
import com.example.dragonking.engine.ParticleKind
import com.example.dragonking.engine.TileType
import com.example.dragonking.pixel.SpriteAtlas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {

    @Test
    fun `dragon settles on grass ground and runs right`() {
        val engine = GameEngine()
        val startX = engine.dragonX

        repeat(10) {
            engine.stepFrame(GameConstants.FIXED_DT, InputState(), 340)
        }
        assertTrue("Dragon should land on ground", engine.onGround)

        repeat(30) {
            engine.stepFrame(GameConstants.FIXED_DT, InputState(moveRight = true), 340)
        }
        assertTrue("Dragon should move right", engine.dragonX > startX + 20f)
        assertTrue("Dragon should face right", engine.facingRight)
    }

    @Test
    fun `dragon jump and variable jump cut behave tightly`() {
        val engine = GameEngine()
        repeat(5) { engine.stepFrame(GameConstants.FIXED_DT, InputState(), 340) }
        val groundY = engine.dragonY

        engine.stepFrame(GameConstants.FIXED_DT, InputState(jumpHeld = true, jumpJustPressed = true), 340)
        assertFalse("Dragon should leave ground on jump", engine.onGround)
        assertTrue("Dragon should move upward", engine.dragonVy < 0f)

        repeat(10) {
            engine.stepFrame(GameConstants.FIXED_DT, InputState(jumpHeld = true), 340)
        }
        assertTrue("Dragon should rise above ground", engine.dragonY < groundY - 15f)
    }

    @Test
    fun `fire breath spawns particles and burns crate`() {
        val engine = GameEngine()
        repeat(5) { engine.stepFrame(GameConstants.FIXED_DT, InputState(), 340) }

        val crateCol = ((engine.dragonX + GameConstants.DRAGON_WIDTH + 16f) / GameConstants.TILE_SIZE).toInt()
        val crateRow = ((engine.dragonY + 6f) / GameConstants.TILE_SIZE).toInt()
        engine.grid[crateRow][crateCol] = TileType.CRATE

        repeat(25) {
            engine.stepFrame(GameConstants.FIXED_DT, InputState(fireHeld = true), 340)
        }

        assertTrue("Fire timer should be active", engine.fireTimer > 0f)
        assertEquals("Crate should be burned to empty", TileType.EMPTY, engine.grid[crateRow][crateCol])
        assertTrue("Crates burned counter should increment", engine.cratesBurned >= 1)
    }

    @Test
    fun `ambient mouth cycle opens periodically with non-lethal sparks and closes again`() {
        val engine = GameEngine()
        repeat(10) { engine.stepFrame(GameConstants.FIXED_DT, InputState(), 340) }
        assertFalse("Mouth starts closed", engine.ambientMouthOpen)

        // Advance to ~1.7s where ambient mouth opens
        repeat(100) { engine.stepFrame(GameConstants.FIXED_DT, InputState(), 340) }
        assertTrue("Mouth should open during cycle", engine.ambientMouthOpen)
        assertTrue(
            "Ambient sparks should emit while mouth is open",
            engine.particles.any { it.active && it.kind == ParticleKind.AMBIENT_SPARK && !it.deadlyToEnemies }
        )

        // Advance past 2.1s where mouth closes again
        repeat(35) { engine.stepFrame(GameConstants.FIXED_DT, InputState(), 340) }
        assertFalse("Mouth should close again after cycle", engine.ambientMouthOpen)
    }

    @Test
    fun `pixel sprites compile to expected grid dimensions`() {
        assertEquals(28 * 24, SpriteAtlas.dragonIdle1.pixels.size)
        assertEquals(28 * 24, SpriteAtlas.dragonWalk1.pixels.size)
        assertEquals(28 * 24, SpriteAtlas.dragonWalk4.pixels.size)
        assertEquals(9 * 4, SpriteAtlas.dragonMouthFirePatch.pixels.size)
        assertEquals(16 * 16, SpriteAtlas.tileGrassTop.pixels.size)
        assertEquals(16 * 16, SpriteAtlas.tileSpike.pixels.size)
    }

    @Test
    fun `pixel renderer and viewmodel execute safely across edge screen dimensions`() {
        val vm = com.example.dragonking.ui.GameViewModel()
        vm.touchRight = true
        vm.touchFire = true
        repeat(45) {
            vm.tick(GameConstants.FIXED_DT)
        }
        val sizes = listOf(
            1 to 1,
            1080 to 1920,
            1280 to 800,
            1920 to 1080,
            2400 to 1080
        )
        for ((w, h) in sizes) {
            val buf = vm.renderer.renderToBuffer(vm.engine, w, h, allocateBitmap = false)
            assertEquals(vm.renderer.virtualWidth * vm.renderer.virtualHeight, buf.size)
            assertTrue("Buffer should contain non-zero sky/scene pixels", buf.any { it != 0 })
        }
        vm.resetLevel()
        assertEquals(0, vm.hudState.value.gems)
    }

    @Test
    fun `red dragon model is registered and animates walk, jaw, wings, tail and fire`() {
        val registry = com.example.dragonking.models.dragons.DragonRegistry
        assertEquals("red-dragon", registry.defaultDragon.id)
        assertEquals("models/dragons/red-dragon", registry.defaultDragon.modelPath)

        val model = com.example.dragonking.models.dragons.reddragon.RedDragonModel()
        // Walk cycle alternates legs smoothly
        repeat(20) {
            model.step(GameConstants.FIXED_DT, walking = true, mouthOpen = false, attacking = false, walkSpeed = 1f)
        }
        assertTrue("walkAmt should blend toward 1 while walking", model.walkAmt > 0.7f)
        assertTrue("Legs should swing in opposite directions", model.legLRotX * model.legRRotX < 0f)

        // Attacking triggers wider jaw, head tilt, and fire cubes
        model.fire()
        repeat(15) {
            model.step(GameConstants.FIXED_DT, walking = false, mouthOpen = false, attacking = false, walkSpeed = 1f)
        }
        assertTrue("Attack should open jaw wide", model.jawRotX > 0.5f)
        assertTrue("Attack should pitch head back", model.headRotX < -0.05f)
        assertTrue("Fire cubes should spawn while attacking", model.fireCubes.any { it.active && it.big })
    }

    @Test
    fun `voxel dragon rasterizer produces crisp directional pixel art for both facings`() {
        val rasterizer = com.example.dragonking.models.dragons.reddragon.VoxelDragonRasterizer()
        val model = com.example.dragonking.models.dragons.reddragon.RedDragonModel()
        repeat(10) {
            model.step(GameConstants.FIXED_DT, walking = true, mouthOpen = true, attacking = false)
        }

        val rightPixels = rasterizer.rasterizeToScratch(model, facingRight = true).copyOf()
        val leftPixels = rasterizer.rasterizeToScratch(model, facingRight = false).copyOf()

        val rightOpaque = rightPixels.count { it != 0 }
        val leftOpaque = leftPixels.count { it != 0 }
        assertTrue("Right-facing dragon should render substantial pixels ($rightOpaque)", rightOpaque > 120)
        assertTrue("Left-facing dragon should render substantial pixels ($leftOpaque)", leftOpaque > 120)
        assertTrue("Facing left and right should produce distinct silhouettes", !rightPixels.contentEquals(leftPixels))
        assertTrue(
            "Rendered Red Dragon should contain bright emissive eye pixels",
            rightPixels.contains(com.example.dragonking.models.dragons.reddragon.RedDragonColors.EYE)
        )
    }

    @Test
    fun `higher jump and capped double jump allow max 2 jumps before landing`() {
        val engine = GameEngine()
        repeat(5) { engine.stepFrame(GameConstants.FIXED_DT, InputState(), 340) }
        assertTrue("Dragon starts on ground", engine.onGround)
        assertEquals(GameConstants.MAX_JUMPS, engine.jumpsRemaining)
        val groundY = engine.dragonY

        // Jump 1 (from ground)
        engine.stepFrame(GameConstants.FIXED_DT, InputState(jumpHeld = true, jumpJustPressed = true), 340)
        assertEquals(1, engine.jumpsRemaining)
        var minYAfterFirstJump = engine.dragonY
        repeat(24) {
            engine.stepFrame(GameConstants.FIXED_DT, InputState(jumpHeld = true), 340)
            minYAfterFirstJump = minOf(minYAfterFirstJump, engine.dragonY)
        }
        val singleJumpHeight = groundY - minYAfterFirstJump
        assertTrue("Single jump should be higher than 48px (was $singleJumpHeight)", singleJumpHeight > 48f)

        // Jump 2 (mid-air double jump)
        engine.stepFrame(GameConstants.FIXED_DT, InputState(jumpHeld = true, jumpJustPressed = true), 340)
        assertEquals(0, engine.jumpsRemaining)
        assertTrue("Double jump should trigger upward impulse", engine.dragonVy < -200f)
        assertTrue("Double jump should trigger wing boost", engine.doubleJumpWingBoostTimer > 0f)

        // Advance until falling downward
        repeat(35) {
            engine.stepFrame(GameConstants.FIXED_DT, InputState(jumpHeld = true), 340)
        }
        assertTrue("Dragon should now be descending", engine.dragonVy > 0f)
        val vyBeforeThirdTry = engine.dragonVy

        // Attempt 3rd mid-air jump -> MUST be prevented
        engine.stepFrame(GameConstants.FIXED_DT, InputState(jumpHeld = true, jumpJustPressed = true), 340)
        assertEquals(0, engine.jumpsRemaining)
        assertTrue("Third mid-air jump must not apply upward velocity", engine.dragonVy > vyBeforeThirdTry)

        // Let dragon land back on ground -> remaining jumps resets to MAX_JUMPS (2)
        repeat(80) {
            engine.stepFrame(GameConstants.FIXED_DT, InputState(), 340)
        }
        assertTrue("Dragon should land back on ground", engine.onGround)
        assertEquals(GameConstants.MAX_JUMPS, engine.jumpsRemaining)
    }

    @Test
    fun `wing flapping accelerates during jump and peaks during double jump`() {
        val engine = GameEngine()
        repeat(5) { engine.stepFrame(GameConstants.FIXED_DT, InputState(), 340) }
        val idleAmp = engine.dragonModel.lastFlapAmplitude

        engine.stepFrame(GameConstants.FIXED_DT, InputState(jumpHeld = true, jumpJustPressed = true), 340)
        val singleJumpAmp = engine.dragonModel.lastFlapAmplitude
        assertTrue("Wing flap amplitude should increase during jump", singleJumpAmp > idleAmp)

        repeat(10) { engine.stepFrame(GameConstants.FIXED_DT, InputState(jumpHeld = true), 340) }
        engine.stepFrame(GameConstants.FIXED_DT, InputState(jumpHeld = true, jumpJustPressed = true), 340)
        val doubleJumpAmp = engine.dragonModel.lastFlapAmplitude
        assertTrue("Wing flap amplitude should peak on double jump", doubleJumpAmp > singleJumpAmp)
    }

    @Test
    fun `floating blocks sit high with head clearance and ravines remain open hazards`() {
        val rows = com.example.dragonking.engine.LevelData.TEST_LEVEL_ROWS
        assertEquals(12, rows.size)
        assertTrue("All level rows should be 110 cols", rows.all { it.length == 110 })

        val grid = com.example.dragonking.engine.LevelData.createGrid()
        // Verify no low floating MOSS_STONE platforms at row 7 or 8 that bump dragon heads
        for (r in 7..8) {
            for (c in grid[r].indices) {
                assertFalse(
                    "Floating MOSS_STONE should not sit low at row $r col $c",
                    grid[r][c] == TileType.MOSS_STONE
                )
            }
        }
        // Verify high floating blocks exist in rows 3..6 with gems placed nearby
        val hasHighPlatforms = (3..6).any { r -> grid[r].any { it == TileType.MOSS_STONE } }
        assertTrue("High floating platforms should exist in rows 3..6", hasHighPlatforms)

        // Verify ravines (empty columns in bottom rows 10..11) exist
        val ravineCols = grid[10].indices.count { c ->
            grid[10][c] == TileType.EMPTY && grid[11][c] == TileType.EMPTY
        }
        assertTrue("Level must retain open ravines ($ravineCols cols)", ravineCols >= 15)
    }

    @Test
    fun `level registry wires level-1 layout and config into game engine`() {
        val level = com.example.dragonking.levels.LevelRegistry.defaultLevel
        assertEquals("level-1", level.config.id)
        assertEquals("levels/level-1", level.config.folderPath)
        assertEquals(12, level.config.rows)
        assertEquals(110, level.config.cols)

        val engine = GameEngine()
        assertEquals("level-1", engine.currentLevel.config.id)
        assertEquals(level.config.spawnX, engine.dragonX)
        assertTrue("Slimes from level-1 should load into engine", engine.slimes.size >= 2)
    }

    @Test
    fun `optimized 1x and 2x jump lengths cross distinct obstacle heights and chasms`() {
        val ts = GameConstants.TILE_SIZE.toFloat()

        // 1) Verify 1x running jump crosses a 2-tile obstacle (height 32px) cleanly
        val engine1x = GameEngine()
        // Clear ground path up to a 2-tile tall test obstacle at col 6
        for (r in 0..9) {
            for (c in 0..10) {
                engine1x.grid[r][c] = TileType.EMPTY
            }
        }
        engine1x.grid[9][6] = TileType.STONE
        engine1x.grid[8][6] = TileType.STONE
        repeat(5) { engine1x.stepFrame(GameConstants.FIXED_DT, InputState(), 340) }
        repeat(28) { engine1x.stepFrame(GameConstants.FIXED_DT, InputState(moveRight = true), 340) }
        engine1x.stepFrame(GameConstants.FIXED_DT, InputState(moveRight = true, jumpJustPressed = true, jumpHeld = true), 340)
        repeat(45) { engine1x.stepFrame(GameConstants.FIXED_DT, InputState(moveRight = true, jumpHeld = true), 340) }
        assertTrue("1x jump should cross 2-tile obstacle cleanly (x=${engine1x.dragonX})", engine1x.dragonX > 7f * ts)

        // 2) Verify 4-tile tall obstacle (64px) needs 2x jump and is crossed cleanly by 2x jump
        val engine2x = GameEngine()
        for (r in 0..9) {
            for (c in 0..12) {
                engine2x.grid[r][c] = TileType.EMPTY
            }
        }
        for (r in 6..9) engine2x.grid[r][6] = TileType.STONE // 4-tile wall (64px)
        repeat(5) { engine2x.stepFrame(GameConstants.FIXED_DT, InputState(), 340) }
        repeat(22) { engine2x.stepFrame(GameConstants.FIXED_DT, InputState(moveRight = true), 340) }
        // First jump
        engine2x.stepFrame(GameConstants.FIXED_DT, InputState(moveRight = true, jumpJustPressed = true, jumpHeld = true), 340)
        repeat(20) { engine2x.stepFrame(GameConstants.FIXED_DT, InputState(moveRight = true, jumpHeld = true), 340) }
        // Second jump (2x jump)
        engine2x.stepFrame(GameConstants.FIXED_DT, InputState(moveRight = true, jumpJustPressed = true, jumpHeld = true), 340)
        repeat(55) { engine2x.stepFrame(GameConstants.FIXED_DT, InputState(moveRight = true, jumpHeld = true), 340) }
        assertTrue("2x jump should cross 4-tile obstacle cleanly (x=${engine2x.dragonX})", engine2x.dragonX > 7f * ts)
    }

    @Test
    fun `run timer advances while active, stops on defeat or victory, and resets on restart`() {
        val engine = GameEngine()
        assertEquals(0, engine.getHudSnapshot().elapsedSeconds)

        // Advance 90 frames (1.5 seconds) -> elapsedSeconds == 1
        repeat(90) {
            engine.stepFrame(GameConstants.FIXED_DT, InputState(), 340)
        }
        assertEquals(1, engine.getHudSnapshot().elapsedSeconds)

        // Advance 35 more frames (past 2.0 seconds) -> elapsedSeconds == 2
        repeat(35) {
            engine.stepFrame(GameConstants.FIXED_DT, InputState(), 340)
        }
        assertEquals(2, engine.getHudSnapshot().elapsedSeconds)

        // Trigger levelCleared via GOAL tile -> timer stops advancing
        val goalCol = ((engine.dragonX + GameConstants.DRAGON_WIDTH) / GameConstants.TILE_SIZE).toInt() + 1
        val goalRow = (engine.dragonY / GameConstants.TILE_SIZE).toInt()
        engine.grid[goalRow][goalCol] = TileType.GOAL
        var safety = 0
        while (!engine.levelCleared && safety++ < 60) {
            engine.stepFrame(GameConstants.FIXED_DT, InputState(moveRight = true), 340)
        }
        assertTrue("Dragon should reach placed GOAL tile", engine.levelCleared)
        val timeAtClear = engine.runTimeSeconds
        repeat(60) {
            engine.stepFrame(GameConstants.FIXED_DT, InputState(), 340)
        }
        assertEquals(timeAtClear, engine.runTimeSeconds, 0.0001f)

        // Reset restores timer to 0
        engine.resetLevel()
        assertEquals(0f, engine.runTimeSeconds, 0.0001f)
        assertEquals(0, engine.getHudSnapshot().elapsedSeconds)
    }

    @Test
    fun `pause freezes gameplay and clears held inputs until resumed or restarted`() {
        val vm = com.example.dragonking.ui.GameViewModel()
        assertFalse(vm.isPaused.value)

        vm.touchRight = true
        repeat(15) { vm.tick(GameConstants.FIXED_DT) }
        val xBeforePause = vm.engine.dragonX
        val timeBeforePause = vm.engine.runTimeSeconds
        assertTrue("Run time should advance before pause", timeBeforePause > 0f)

        vm.togglePause()
        assertTrue("ViewModel should be paused", vm.isPaused.value)
        assertFalse("Pausing should clear held inputs", vm.touchRight)

        // Attempt to tick with held input while paused -> world remains frozen
        vm.touchRight = true
        repeat(30) { vm.tick(GameConstants.FIXED_DT) }
        assertEquals(xBeforePause, vm.engine.dragonX, 0.0001f)
        assertEquals(timeBeforePause, vm.engine.runTimeSeconds, 0.0001f)

        // Resume restores gameplay progression
        vm.resumeGame()
        assertFalse("Resuming should unpause", vm.isPaused.value)
        repeat(15) { vm.tick(GameConstants.FIXED_DT) }
        assertTrue("Dragon should move again after resume", vm.engine.dragonX > xBeforePause)
        assertTrue("Timer should advance again after resume", vm.engine.runTimeSeconds > timeBeforePause)

        // Pause then restart unpauses, clears inputs, and resets timer
        vm.togglePause()
        assertTrue(vm.isPaused.value)
        vm.resetLevel()
        assertFalse("Restarting should unpause", vm.isPaused.value)
        assertEquals(0, vm.hudState.value.elapsedSeconds)
    }

    @Test
    fun `sound toggle flips state safely and timer formatter formats mm_ss`() {
        val vm = com.example.dragonking.ui.GameViewModel()
        assertTrue("Sound defaults to enabled", vm.soundEnabled.value)

        vm.toggleSound()
        assertFalse("Sound should toggle off", vm.soundEnabled.value)

        vm.toggleSound()
        assertTrue("Sound should toggle back on safely on JVM", vm.soundEnabled.value)

        assertEquals("00:00", com.example.dragonking.ui.formatRunTimer(0))
        assertEquals("00:09", com.example.dragonking.ui.formatRunTimer(9))
        assertEquals("01:05", com.example.dragonking.ui.formatRunTimer(65))
        assertEquals("99:59", com.example.dragonking.ui.formatRunTimer(99 * 60 + 59))
    }

    @Test
    fun `tuned single jump adds 20 percent height and double jump preserves momentum across timings`() {
        // 1) Measure full single jump peak height (~95.7px = +20% over previous ~79.2px)
        val singleEngine = GameEngine()
        for (r in 0..9) {
            for (c in 0..10) singleEngine.grid[r][c] = TileType.EMPTY
        }
        repeat(5) { singleEngine.stepFrame(GameConstants.FIXED_DT, InputState(), 340) }
        val groundY = singleEngine.dragonY

        singleEngine.stepFrame(GameConstants.FIXED_DT, InputState(jumpHeld = true, jumpJustPressed = true), 340)
        var minSingleY = singleEngine.dragonY
        repeat(50) {
            singleEngine.stepFrame(GameConstants.FIXED_DT, InputState(jumpHeld = true), 340)
            minSingleY = minOf(minSingleY, singleEngine.dragonY)
        }
        val singleHeight = groundY - minSingleY
        assertTrue("Single jump should be ~95.7px (+20% over 79.2px, was $singleHeight)", singleHeight in 92f..100f)

        // 2) Measure apex double jump total height (~177px)
        val apexEngine = GameEngine()
        for (r in 0..9) {
            for (c in 0..10) apexEngine.grid[r][c] = TileType.EMPTY
        }
        repeat(5) { apexEngine.stepFrame(GameConstants.FIXED_DT, InputState(), 340) }
        apexEngine.stepFrame(GameConstants.FIXED_DT, InputState(jumpHeld = true, jumpJustPressed = true), 340)
        var minApexDoubleY = apexEngine.dragonY
        repeat(34) {
            apexEngine.stepFrame(GameConstants.FIXED_DT, InputState(jumpHeld = true), 340)
            minApexDoubleY = minOf(minApexDoubleY, apexEngine.dragonY)
        }
        apexEngine.stepFrame(GameConstants.FIXED_DT, InputState(jumpHeld = true, jumpJustPressed = true), 340)
        assertEquals(0, apexEngine.jumpsRemaining)
        assertTrue("Double jump must trigger extra wing-flap boost", apexEngine.doubleJumpWingBoostTimer > 0f)
        repeat(45) {
            apexEngine.stepFrame(GameConstants.FIXED_DT, InputState(jumpHeld = true), 340)
            minApexDoubleY = minOf(minApexDoubleY, apexEngine.dragonY)
        }
        val apexDoubleHeight = groundY - minApexDoubleY
        assertTrue(
            "Apex double jump should reach ~170-183px (was $apexDoubleHeight)",
            apexDoubleHeight in 170f..183f
        )

        // 3) Measure quick back-to-back tap-release-tap double jump (tap 2 frames, release 2 frames, tap+hold)
        val quickEngine = GameEngine()
        for (r in 0..9) {
            for (c in 0..10) quickEngine.grid[r][c] = TileType.EMPTY
        }
        repeat(5) { quickEngine.stepFrame(GameConstants.FIXED_DT, InputState(), 340) }
        // Tap 1 (press + hold 2 frames)
        quickEngine.stepFrame(GameConstants.FIXED_DT, InputState(jumpHeld = true, jumpJustPressed = true), 340)
        quickEngine.stepFrame(GameConstants.FIXED_DT, InputState(jumpHeld = true), 340)
        // Brief finger lift between taps
        quickEngine.stepFrame(GameConstants.FIXED_DT, InputState(jumpHeld = false, jumpJustReleased = true), 340)
        quickEngine.stepFrame(GameConstants.FIXED_DT, InputState(jumpHeld = false), 340)
        // Quick Tap 2 (press + hold)
        quickEngine.stepFrame(GameConstants.FIXED_DT, InputState(jumpHeld = true, jumpJustPressed = true), 340)
        assertEquals(0, quickEngine.jumpsRemaining)
        assertTrue("Quick double tap must trigger wing-flap boost", quickEngine.doubleJumpWingBoostTimer > 0f)
        var minQuickDoubleY = quickEngine.dragonY
        repeat(60) {
            quickEngine.stepFrame(GameConstants.FIXED_DT, InputState(jumpHeld = true), 340)
            minQuickDoubleY = minOf(minQuickDoubleY, quickEngine.dragonY)
        }
        val quickDoubleHeight = groundY - minQuickDoubleY
        assertTrue(
            "Quick back-to-back double tap should reach >=165px (was $quickDoubleHeight)",
            quickDoubleHeight >= 165f
        )
    }
}
