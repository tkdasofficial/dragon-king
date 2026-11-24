package com.example.dragonking.engine

import com.example.dragonking.levels.GameLevel
import com.example.dragonking.levels.LevelRegistry
import com.example.dragonking.models.dragons.DragonRegistry
import com.example.dragonking.models.dragons.reddragon.RedDragonModel
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Pure Kotlin 2D platformer engine with fixed-timestep physics, tight Nintendo-style jump feel,
 * AABB tile collisions, fire breath particle pool, burnable crates/braziers, and slime hazards.
 */
class GameEngine(
    initialLevel: GameLevel = LevelRegistry.defaultLevel
) {
    var currentLevel: GameLevel = initialLevel
        private set

    var grid: Array<Array<TileType>> = currentLevel.createGrid()
        private set

    val levelCols: Int get() = grid[0].size
    val levelRows: Int get() = grid.size
    val worldWidth: Float get() = levelCols * GameConstants.TILE_SIZE.toFloat()
    val worldHeight: Float get() = levelRows * GameConstants.TILE_SIZE.toFloat()

    // Dragon King state
    var dragonX = 32f
        private set
    var dragonY = 140f
        private set
    var dragonVx = 0f
        private set
    var dragonVy = 0f
        private set
    var facingRight = true
        private set
    var onGround = false
        private set
    var coyoteTimer = 0f
        private set
    var jumpBufferTimer = 0f
        private set
    var jumpsRemaining = GameConstants.MAX_JUMPS
        private set
    var doubleJumpWingBoostTimer = 0f
        private set
    private var firstJumpElapsed = 99f
    private var firstJumpUncutVy = 0f
    var fireTimer = 0f
        private set
    var fireSpawnAccum = 0f
        private set
    var ambientMouthOpen = false
        private set
    private var ambientMouthTimer = 0f
    private var ambientSparkAccum = 0f
    var hp = GameConstants.MAX_HP
        private set
    var invulnTimer = 0f
        private set
    var animTime = 0f
        private set
    var runTimeSeconds = 0f
        private set
    var isDead = false
        private set
    var levelCleared = false
        private set

    // Respawn checkpoint
    private var spawnX = 32f
    private var spawnY = 140f

    // Counters
    var gemsCollected = 0
        private set
    var totalGems = 0
        private set
    var cratesBurned = 0
        private set
    var braziersLit = 0
        private set
    var slimesDefeated = 0
        private set

    // Camera (logical pixels)
    var cameraX = 0f
        private set
    var cameraY = 0f
        private set
    var screenShakeTimer = 0f
        private set
    private var smoothedCamLead = 24f

    // Active Dragon Model (extracted from models/dragons/red-dragon/)
    val activeDragonDescriptor = DragonRegistry.RED_DRAGON
    val dragonModel = RedDragonModel()
    private val mouthOffsetScratch = FloatArray(2)

    // Pooled entities (zero per-frame allocations)
    val particles = Array(GameConstants.MAX_PARTICLES) { FireParticle() }
    val slimes = mutableListOf<SlimeEnemy>()

    private var accumulator = 0f
    private val rng = Random(42)

    init {
        resetLevel()
    }

    fun loadLevel(level: GameLevel) {
        currentLevel = level
        resetLevel()
    }

    fun resetLevel() {
        grid = currentLevel.createGrid()
        slimes.clear()
        slimes.addAll(currentLevel.createSlimes())
        for (p in particles) p.active = false

        totalGems = 0
        for (row in grid) {
            for (tile in row) {
                if (tile == TileType.GEM) totalGems++
            }
        }

        val cfg = currentLevel.config
        spawnX = cfg.spawnX
        spawnY = cfg.spawnRow * cfg.tileSize - GameConstants.DRAGON_HEIGHT
        dragonX = spawnX
        dragonY = spawnY
        dragonVx = 0f
        dragonVy = 0f
        facingRight = true
        onGround = false
        coyoteTimer = 0f
        jumpBufferTimer = 0f
        jumpsRemaining = GameConstants.MAX_JUMPS
        doubleJumpWingBoostTimer = 0f
        firstJumpElapsed = 99f
        firstJumpUncutVy = 0f
        fireTimer = 0f
        fireSpawnAccum = 0f
        ambientMouthOpen = false
        ambientMouthTimer = 0f
        ambientSparkAccum = 0f
        hp = GameConstants.MAX_HP
        invulnTimer = 0f
        animTime = 0f
        runTimeSeconds = 0f
        isDead = false
        levelCleared = false
        gemsCollected = 0
        cratesBurned = 0
        braziersLit = 0
        slimesDefeated = 0
        cameraX = 0f
        cameraY = 0f
        smoothedCamLead = 24f
        screenShakeTimer = 0f
        accumulator = 0f
        dragonModel.reset()
    }

    fun stepFrame(rawDeltaSeconds: Float, input: InputState, virtualWidth: Int) {
        val clampedDt = min(rawDeltaSeconds, GameConstants.MAX_FRAME_DELTA)
        accumulator += clampedDt

        var consumedPress = false
        var consumedRelease = false

        while (accumulator >= GameConstants.FIXED_DT) {
            val stepInput = input.copy(
                jumpJustPressed = input.jumpJustPressed && !consumedPress,
                jumpJustReleased = input.jumpJustReleased && !consumedRelease
            )
            consumedPress = true
            consumedRelease = true
            fixedUpdate(GameConstants.FIXED_DT, stepInput, virtualWidth)
            accumulator -= GameConstants.FIXED_DT
        }
    }

    private fun fixedUpdate(dt: Float, input: InputState, virtualWidth: Int) {
        animTime += dt
        if (screenShakeTimer > 0f) {
            screenShakeTimer = max(0f, screenShakeTimer - dt)
        }

        updateParticles(dt)

        if (isDead || levelCleared) {
            return
        }

        runTimeSeconds += dt

        if (invulnTimer > 0f) {
            invulnTimer = max(0f, invulnTimer - dt)
        }
        if (doubleJumpWingBoostTimer > 0f) {
            doubleJumpWingBoostTimer = max(0f, doubleJumpWingBoostTimer - dt)
        }

        // 1. Horizontal Movement (snappy traction, zero slippery ice)
        val moveDir = (if (input.moveRight) 1f else 0f) - (if (input.moveLeft) 1f else 0f)
        if (moveDir != 0f) {
            facingRight = moveDir > 0f
            val accel = if (onGround) GameConstants.MOVE_ACCEL_GROUND else GameConstants.MOVE_ACCEL_AIR
            dragonVx += moveDir * accel * dt
            dragonVx = dragonVx.coerceIn(-GameConstants.MAX_RUN_SPEED, GameConstants.MAX_RUN_SPEED)
        } else {
            val decel = if (onGround) GameConstants.MOVE_DECEL_GROUND else GameConstants.MOVE_DECEL_AIR
            if (abs(dragonVx) <= decel * dt) {
                dragonVx = 0f
            } else {
                dragonVx -= kotlin.math.sign(dragonVx) * decel * dt
            }
        }

        // 2. Coyote Time, Jump Buffer & Capped Double Jump (max 2 jumps before landing)
        if (onGround) {
            coyoteTimer = GameConstants.COYOTE_TIME
            jumpsRemaining = GameConstants.MAX_JUMPS
            firstJumpElapsed = 99f
            firstJumpUncutVy = 0f
        } else {
            if (coyoteTimer > 0f) {
                coyoteTimer = max(0f, coyoteTimer - dt)
            }
            if (firstJumpElapsed < 99f) {
                firstJumpElapsed += dt
            }
            if (firstJumpUncutVy < 0f) {
                firstJumpUncutVy = min(0f, firstJumpUncutVy + GameConstants.GRAVITY_UP * dt)
            }
            // Walking off a ledge leaves at most 1 mid-air jump once coyote time expires
            if (coyoteTimer <= 0f && jumpsRemaining >= GameConstants.MAX_JUMPS) {
                jumpsRemaining = GameConstants.MAX_JUMPS - 1
            }
        }

        if (input.jumpJustPressed) {
            jumpBufferTimer = GameConstants.JUMP_BUFFER_TIME
        } else if (jumpBufferTimer > 0f) {
            jumpBufferTimer = max(0f, jumpBufferTimer - dt)
        }

        if (jumpBufferTimer > 0f && coyoteTimer > 0f) {
            dragonVy = GameConstants.JUMP_VELOCITY
            onGround = false
            coyoteTimer = 0f
            jumpBufferTimer = 0f
            jumpsRemaining = GameConstants.MAX_JUMPS - 1
            firstJumpElapsed = 0f
            firstJumpUncutVy = GameConstants.JUMP_VELOCITY
        } else if (input.jumpJustPressed && !onGround && jumpsRemaining > 0) {
            // Preserve remaining upward momentum (including quick tap-release-tap double taps)
            // so total double-jump reach matches full first + second jump height.
            val remainingUpVy = if (firstJumpElapsed <= 0.22f) {
                min(dragonVy, firstJumpUncutVy)
            } else {
                min(dragonVy, 0f)
            }
            val dj = GameConstants.DOUBLE_JUMP_VELOCITY
            dragonVy = -sqrt(remainingUpVy * remainingUpVy + dj * dj)
            jumpsRemaining = (jumpsRemaining - 1).coerceAtLeast(0)
            coyoteTimer = 0f
            jumpBufferTimer = 0f
            firstJumpElapsed = 99f
            firstJumpUncutVy = 0f
            doubleJumpWingBoostTimer = GameConstants.DOUBLE_JUMP_WING_BOOST_TIME
            spawnBurst(
                cx = dragonX + GameConstants.DRAGON_WIDTH * 0.5f,
                cy = dragonY + GameConstants.DRAGON_HEIGHT - 2f,
                kind = ParticleKind.SPARK,
                count = 7
            )
        }

        // Variable jump height cut when player releases jump early
        if (input.jumpJustReleased && dragonVy < 0f) {
            dragonVy *= GameConstants.JUMP_CUT_MULTIPLIER
        }

        // 3. Gravity (crisp fast-fall)
        val gravity = if (dragonVy < 0f) GameConstants.GRAVITY_UP else GameConstants.GRAVITY_DOWN
        dragonVy = min(GameConstants.MAX_FALL_SPEED, dragonVy + gravity * dt)

        // 4. Move & Resolve X Collisions
        dragonX += dragonVx * dt
        dragonX = dragonX.coerceIn(0f, worldWidth - GameConstants.DRAGON_WIDTH)
        resolveHorizontalCollisions()

        // 5. Move & Resolve Y Collisions
        dragonY += dragonVy * dt
        onGround = false
        resolveVerticalCollisions()

        // 6. Pit Fall Check
        if (dragonY > worldHeight + 8f) {
            damageOrRespawn(fellInPit = true)
            return
        }

        // 7. Fire Breath Handling (Big stream) vs Periodic Ambient Mouth Sparks
        if (input.fireHeld) {
            if (fireTimer <= 0f) {
                dragonModel.fire(GameConstants.FIRE_DURATION)
            }
            fireTimer = GameConstants.FIRE_DURATION
            ambientMouthOpen = true
            fireSpawnAccum += dt
            while (fireSpawnAccum >= GameConstants.FIRE_SPAWN_INTERVAL) {
                fireSpawnAccum -= GameConstants.FIRE_SPAWN_INTERVAL
                spawnFireBreathParticle()
            }
        } else {
            fireSpawnAccum = GameConstants.FIRE_SPAWN_INTERVAL // instant burst on next tap
            if (fireTimer > 0f) {
                fireTimer = max(0f, fireTimer - dt)
            }

            // Periodic breathing mouth open/close cycle (~2.2s period, open for ~0.48s)
            ambientMouthTimer = (ambientMouthTimer + dt) % 2.2f
            ambientMouthOpen = ambientMouthTimer in 1.55f..2.03f
            if (ambientMouthOpen && fireTimer <= 0f) {
                ambientSparkAccum += dt
                if (ambientSparkAccum >= 0.085f) {
                    ambientSparkAccum -= 0.085f
                    spawnAmbientMouthSpark()
                }
            } else {
                ambientSparkAccum = 0.085f
            }
        }

        // 7b. Step RedDragon 3D voxel rig & animations (walk/plant, jump wing-flaps, tail, jaw, fire cubes)
        val isWalking = abs(dragonVx) > 8f && onGround
        val walkSpeed = (abs(dragonVx) / GameConstants.MAX_RUN_SPEED).coerceIn(0.45f, 1.35f)
        val doubleJumpBoost = (doubleJumpWingBoostTimer / GameConstants.DOUBLE_JUMP_WING_BOOST_TIME).coerceIn(0f, 1f)
        dragonModel.step(
            dt = dt,
            walking = isWalking,
            mouthOpen = ambientMouthOpen,
            attacking = fireTimer > 0f,
            walkSpeed = walkSpeed,
            airborne = !onGround,
            doubleJumpBoost = doubleJumpBoost
        )

        // 8. Non-solid tile triggers (Gems, Spikes, Checkpoints, Goal)
        checkTileTriggers()

        // 9. Update Slimes & Enemy Collisions
        updateSlimes(dt)

        // 10. Silky Camera Tracking with Eased Horizontal Look-Ahead
        val desiredLead = if (facingRight) 26f else -22f
        smoothedCamLead += (desiredLead - smoothedCamLead) * min(1f, dt * 4.5f)
        val targetCamX = (dragonX + GameConstants.DRAGON_WIDTH * 0.5f + smoothedCamLead - virtualWidth * 0.42f)
            .coerceIn(0f, max(0f, worldWidth - virtualWidth))
        cameraX += (targetCamX - cameraX) * min(1f, dt * 9.0f)
        cameraY = 0f
    }

    private fun resolveHorizontalCollisions() {
        val ts = GameConstants.TILE_SIZE
        val leftCol = (dragonX / ts).toInt().coerceIn(0, levelCols - 1)
        val rightCol = ((dragonX + GameConstants.DRAGON_WIDTH - 0.01f) / ts).toInt().coerceIn(0, levelCols - 1)
        val topRow = (dragonY / ts).toInt().coerceIn(0, levelRows - 1)
        val bottomRow = ((dragonY + GameConstants.DRAGON_HEIGHT - 0.01f) / ts).toInt().coerceIn(0, levelRows - 1)

        for (r in topRow..bottomRow) {
            for (c in leftCol..rightCol) {
                if (grid[r][c].solid) {
                    if (dragonVx > 0f) {
                        dragonX = c * ts - GameConstants.DRAGON_WIDTH
                        dragonVx = 0f
                    } else if (dragonVx < 0f) {
                        dragonX = (c + 1) * ts.toFloat()
                        dragonVx = 0f
                    }
                }
            }
        }
    }

    private fun resolveVerticalCollisions() {
        val ts = GameConstants.TILE_SIZE
        val leftCol = (dragonX / ts).toInt().coerceIn(0, levelCols - 1)
        val rightCol = ((dragonX + GameConstants.DRAGON_WIDTH - 0.01f) / ts).toInt().coerceIn(0, levelCols - 1)
        val topRow = (dragonY / ts).toInt().coerceIn(0, levelRows - 1)
        val bottomRow = ((dragonY + GameConstants.DRAGON_HEIGHT - 0.01f) / ts).toInt().coerceIn(0, levelRows - 1)

        for (r in topRow..bottomRow) {
            for (c in leftCol..rightCol) {
                if (grid[r][c].solid) {
                    if (dragonVy > 0f) {
                        dragonY = r * ts - GameConstants.DRAGON_HEIGHT
                        dragonVy = 0f
                        onGround = true
                        jumpsRemaining = GameConstants.MAX_JUMPS
                    } else if (dragonVy < 0f) {
                        dragonY = (r + 1) * ts.toFloat()
                        dragonVy = 0f
                        firstJumpUncutVy = 0f
                    }
                }
            }
        }
    }

    private fun checkTileTriggers() {
        val ts = GameConstants.TILE_SIZE
        val leftCol = (dragonX / ts).toInt().coerceIn(0, levelCols - 1)
        val rightCol = ((dragonX + GameConstants.DRAGON_WIDTH - 0.01f) / ts).toInt().coerceIn(0, levelCols - 1)
        val topRow = (dragonY / ts).toInt().coerceIn(0, levelRows - 1)
        val bottomRow = ((dragonY + GameConstants.DRAGON_HEIGHT - 0.01f) / ts).toInt().coerceIn(0, levelRows - 1)

        for (r in topRow..bottomRow) {
            for (c in leftCol..rightCol) {
                when (grid[r][c]) {
                    TileType.GEM -> {
                        grid[r][c] = TileType.EMPTY
                        gemsCollected++
                        spawnBurst(c * ts + 8f, r * ts + 8f, ParticleKind.SPARK, 8)
                    }
                    TileType.CHECKPOINT_OFF -> {
                        grid[r][c] = TileType.CHECKPOINT_ON
                        spawnX = c * ts.toFloat()
                        spawnY = (r + 1) * ts - GameConstants.DRAGON_HEIGHT
                        spawnBurst(c * ts + 8f, r * ts + 4f, ParticleKind.SPARK, 10)
                    }
                    TileType.GOAL -> {
                        levelCleared = true
                        spawnBurst(c * ts + 8f, r * ts + 6f, ParticleKind.SPARK, 22)
                    }
                    TileType.SPIKE -> {
                        // Slightly forgiving spike hitbox (bottom 10px of the 16px tile)
                        val spikeTop = r * ts + 6f
                        if (dragonY + GameConstants.DRAGON_HEIGHT > spikeTop) {
                            damageOrRespawn(fellInPit = false)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun spawnFireBreathParticle() {
        val slot = particles.firstOrNull { !it.active } ?: return
        val dir = if (facingRight) 1f else -1f
        dragonModel.mouthOffsetPx(facingRight, mouthOffsetScratch)
        val mouthX = dragonX + mouthOffsetScratch[0]
        val mouthY = dragonY + mouthOffsetScratch[1] + rng.nextFloat() * 3.0f - 1.5f

        slot.active = true
        slot.x = mouthX
        slot.y = mouthY
        slot.vx = dir * (155f + rng.nextFloat() * 56f) + dragonVx * 0.25f
        slot.vy = (rng.nextFloat() - 0.52f) * 38f
        slot.life = 0f
        slot.maxLife = 0.28f + rng.nextFloat() * 0.14f
        slot.size = if (rng.nextFloat() < 0.5f) 3 else 2
        slot.kind = ParticleKind.FLAME
        slot.deadlyToEnemies = true
    }

    private fun spawnAmbientMouthSpark() {
        val slot = particles.firstOrNull { !it.active } ?: return
        val dir = if (facingRight) 1f else -1f
        dragonModel.mouthOffsetPx(facingRight, mouthOffsetScratch)
        val mouthX = dragonX + mouthOffsetScratch[0]
        val mouthY = dragonY + mouthOffsetScratch[1] + (rng.nextFloat() - 0.5f) * 2f

        slot.active = true
        slot.x = mouthX
        slot.y = mouthY
        slot.vx = dir * (22f + rng.nextFloat() * 18f) + dragonVx * 0.35f
        slot.vy = -14f - rng.nextFloat() * 18f
        slot.life = 0f
        slot.maxLife = 0.24f + rng.nextFloat() * 0.14f
        slot.size = if (rng.nextFloat() < 0.35f) 2 else 1
        slot.kind = ParticleKind.AMBIENT_SPARK
        slot.deadlyToEnemies = false
    }

    private fun spawnBurst(cx: Float, cy: Float, kind: ParticleKind, count: Int) {
        var spawned = 0
        for (p in particles) {
            if (!p.active) {
                p.active = true
                p.x = cx + (rng.nextFloat() - 0.5f) * 6f
                p.y = cy + (rng.nextFloat() - 0.5f) * 6f
                p.vx = (rng.nextFloat() - 0.5f) * 95f
                p.vy = -30f - rng.nextFloat() * 70f
                p.life = 0f
                p.maxLife = 0.3f + rng.nextFloat() * 0.25f
                p.size = if (kind == ParticleKind.WOOD_ASH) 2 else 2
                p.kind = kind
                p.deadlyToEnemies = false
                spawned++
                if (spawned >= count) break
            }
        }
    }

    private fun updateParticles(dt: Float) {
        val ts = GameConstants.TILE_SIZE
        for (p in particles) {
            if (!p.active) continue
            p.life += dt
            if (p.life >= p.maxLife) {
                p.active = false
                continue
            }

            p.x += p.vx * dt
            p.y += p.vy * dt

            when (p.kind) {
                ParticleKind.FLAME -> {
                    p.vy -= 28f * dt // thermal lift
                    p.vx *= 0.985f
                }
                ParticleKind.AMBIENT_SPARK -> {
                    p.vy -= 18f * dt
                    p.vx *= 0.95f
                }
                ParticleKind.WOOD_ASH, ParticleKind.SPARK -> {
                    p.vy += 220f * dt
                }
                ParticleKind.SMOKE -> {
                    p.vy -= 35f * dt
                }
            }

            // Tile interaction for active flame particles
            if (p.kind == ParticleKind.FLAME && p.deadlyToEnemies) {
                val col = ((p.x + p.size * 0.5f) / ts).toInt()
                val row = ((p.y + p.size * 0.5f) / ts).toInt()
                if (row in 0 until levelRows && col in 0 until levelCols) {
                    val tile = grid[row][col]
                    when {
                        tile.burnable -> {
                            grid[row][col] = TileType.EMPTY
                            cratesBurned++
                            screenShakeTimer = 0.08f
                            spawnBurst(col * ts + 8f, row * ts + 8f, ParticleKind.WOOD_ASH, 10)
                            p.active = false
                        }
                        tile == TileType.BRAZIER_OFF -> {
                            grid[row][col] = TileType.BRAZIER_ON
                            braziersLit++
                            spawnBurst(col * ts + 8f, row * ts + 4f, ParticleKind.SPARK, 8)
                        }
                        tile.solid -> {
                            p.kind = ParticleKind.SMOKE
                            p.deadlyToEnemies = false
                            p.vx *= 0.15f
                            p.vy = -22f
                            p.maxLife = min(p.maxLife, p.life + 0.14f)
                        }
                    }
                }
            }
        }
    }

    private fun updateSlimes(dt: Float) {
        val slimeW = 14f
        val slimeH = 10f
        for (slime in slimes) {
            if (!slime.alive) continue

            slime.x += slime.vx * dt
            if (slime.x <= slime.minX) {
                slime.x = slime.minX
                slime.vx = abs(slime.vx)
            } else if (slime.x >= slime.maxX) {
                slime.x = slime.maxX
                slime.vx = -abs(slime.vx)
            }

            // Check fire breath particles hitting slime
            var burned = false
            for (p in particles) {
                if (!p.active || !p.deadlyToEnemies) continue
                if (p.x + p.size >= slime.x && p.x <= slime.x + slimeW &&
                    p.y + p.size >= slime.y && p.y <= slime.y + slimeH
                ) {
                    slime.alive = false
                    slimesDefeated++
                    burned = true
                    screenShakeTimer = 0.08f
                    spawnBurst(slime.x + 7f, slime.y + 5f, ParticleKind.SPARK, 12)
                    break
                }
            }
            if (burned) continue

            // Check Dragon King touching slime
            if (dragonX + GameConstants.DRAGON_WIDTH > slime.x + 2f &&
                dragonX < slime.x + slimeW - 2f &&
                dragonY + GameConstants.DRAGON_HEIGHT > slime.y + 2f &&
                dragonY < slime.y + slimeH
            ) {
                // Stomp from above defeats slime with a crisp bounce and grants 1 mid-air recovery jump
                if (dragonVy > 0f && dragonY + GameConstants.DRAGON_HEIGHT - dragonVy * dt <= slime.y + 5f) {
                    slime.alive = false
                    slimesDefeated++
                    dragonVy = GameConstants.JUMP_VELOCITY * 0.72f
                    jumpsRemaining = max(jumpsRemaining, 1)
                    spawnBurst(slime.x + 7f, slime.y + 5f, ParticleKind.SPARK, 10)
                } else {
                    damageOrRespawn(fellInPit = false)
                }
            }
        }
    }

    private fun damageOrRespawn(fellInPit: Boolean) {
        if (!fellInPit && invulnTimer > 0f) return

        hp = max(0, hp - 1)
        screenShakeTimer = 0.16f

        if (hp <= 0) {
            isDead = true
            spawnBurst(dragonX + 6f, dragonY + 9f, ParticleKind.FLAME, 18)
            return
        }

        if (fellInPit) {
            dragonX = spawnX
            dragonY = spawnY
            dragonVx = 0f
            dragonVy = 0f
            jumpsRemaining = GameConstants.MAX_JUMPS
            doubleJumpWingBoostTimer = 0f
            firstJumpElapsed = 99f
            firstJumpUncutVy = 0f
            invulnTimer = GameConstants.INVULN_TIME
        } else {
            invulnTimer = GameConstants.INVULN_TIME
            dragonVy = -140f
            dragonVx = if (facingRight) -80f else 80f
        }
    }

    fun getHudSnapshot(): HudSnapshot {
        val progress = ((dragonX / (worldWidth - 32f)) * 100f).toInt().coerceIn(0, 100)
        return HudSnapshot(
            hp = hp,
            maxHp = GameConstants.MAX_HP,
            gems = gemsCollected,
            totalGems = totalGems,
            cratesBurned = cratesBurned,
            braziersLit = braziersLit,
            slimesDefeated = slimesDefeated,
            isDead = isDead,
            levelCleared = levelCleared,
            distancePercent = progress,
            elapsedSeconds = runTimeSeconds.toInt()
        )
    }
}
