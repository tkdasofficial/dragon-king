package com.example.dragonking.models.dragons.reddragon

import com.example.dragonking.models.dragons.DragonCharacterModel
import com.example.dragonking.models.dragons.DragonDescriptor
import com.example.dragonking.models.dragons.DragonRegistry
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.sin
import kotlin.random.Random

/**
 * Faithful Kotlin port of `models/dragons/red-dragon/RedDragon.tsx`.
 * Preserves exact feel constants, color palette, voxel bone hierarchy,
 * articulated animations (leg walk/plant, arms, body bob/tilt, 4-segment tail wave,
 * wing flap, jaw open/fire pitch), and pixel-snapped fire particle simulation.
 */
object RedDragonConstants {
    const val STEP_RATE = 7f
    const val LEG_SWING = 0.65f
    const val JAW_OPEN = 0.55f
    const val ATTACK_TIME = 1.6f
    const val MODEL_SCALE = 0.13f
    const val PIXEL_SNAP = 0.05f
    const val MAX_PARTICLES = 120
}

object RedDragonColors {
    const val RED = 0xFFC0262D.toInt()
    const val RED_HI = 0xFFE2453C.toInt()
    const val RED_LO = 0xFF8A1620.toInt()
    const val RED_DK = 0xFF5E0E17.toInt()
    const val GOLD = 0xFFF2B632.toInt()
    const val GOLD_HI = 0xFFFFD76A.toInt()
    const val GOLD_LO = 0xFFB97D14.toInt()
    const val BONE = 0xFFF4EAD2.toInt()
    const val EYE = 0xFFFFF35C.toInt()
    const val PUPIL = 0xFF1A0A05.toInt()
    const val MOUTH = 0xFF3A0610.toInt()

    val FIRE_RAMP = intArrayOf(
        0xFFFFFBE0.toInt(),
        0xFFFFE14A.toInt(),
        0xFFFFA21F.toInt(),
        0xFFF2521B.toInt(),
        0xFFA3201A.toInt(),
        0xFF4A3A36.toInt()
    )
}

data class VoxelBox(
    val px: Float,
    val py: Float,
    val pz: Float,
    val sx: Float,
    val sy: Float,
    val sz: Float,
    val color: Int,
    val emissive: Float = 0f
)

data class RedDragonFireCube(
    var active: Boolean = false,
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var vz: Float = 0f,
    var life: Float = 0f,
    var maxLife: Float = 1f,
    var size: Float = 0.1f,
    var big: Boolean = false,
    var color: Int = RedDragonColors.FIRE_RAMP[0]
)

object RedDragonRigData {
    val legBoxes: List<VoxelBox> = buildList {
        add(VoxelBox(0f, -0.8f, 0f, 2.4f, 2f, 2.4f, RedDragonColors.RED))
        add(VoxelBox(0f, -2.6f, 0f, 1.6f, 1.8f, 1.6f, RedDragonColors.RED_LO))
        add(VoxelBox(0f, -4.25f, 0.5f, 2.2f, 1f, 3f, RedDragonColors.RED_DK))
        for (cx in floatArrayOf(-0.7f, 0f, 0.7f)) {
            add(VoxelBox(cx, -4.4f, 2.3f, 0.5f, 0.7f, 0.8f, RedDragonColors.BONE))
        }
    }

    val armBoxes: List<VoxelBox> = listOf(
        VoxelBox(0f, -0.5f, 0.6f, 1.1f, 1.1f, 2f, RedDragonColors.RED_HI),
        VoxelBox(0f, -1f, 1.8f, 1f, 0.6f, 0.8f, RedDragonColors.BONE)
    )

    val torsoBoxes: List<VoxelBox> = buildList {
        // Stepped plump torso
        add(VoxelBox(0f, 8f, 0f, 6f, 6f, 6f, RedDragonColors.RED))
        add(VoxelBox(0f, 8f, 0f, 7f, 4f, 5f, RedDragonColors.RED))
        add(VoxelBox(0f, 7.5f, 0f, 5f, 3.5f, 7f, RedDragonColors.RED))
        add(VoxelBox(0f, 5.6f, 0f, 5f, 1.2f, 5f, RedDragonColors.RED_LO))
        add(VoxelBox(0f, 11.2f, -0.3f, 5f, 0.8f, 5f, RedDragonColors.RED_HI))

        // Belly plates
        val bellyY = floatArrayOf(6.2f, 7.4f, 8.6f, 9.8f)
        for (i in bellyY.indices) {
            val y = bellyY[i]
            val w = if (i % 2 == 1) 3.6f else 3f
            val col = if (i % 2 == 1) RedDragonColors.GOLD else RedDragonColors.GOLD_HI
            add(VoxelBox(0f, y, 3.55f, w, 1f, 0.3f, col))
        }

        // Back spikes
        val backSpikes = arrayOf(
            floatArrayOf(11.8f, -1.5f, 1.2f),
            floatArrayOf(10.8f, -3.2f, 1.0f),
            floatArrayOf(9.4f, -3.9f, 0.9f)
        )
        for (sp in backSpikes) {
            val y = sp[0]
            val z = sp[1]
            val h = sp[2]
            add(VoxelBox(0f, y, z, 0.8f, h, 0.8f, RedDragonColors.GOLD))
            add(VoxelBox(0f, y + h * 0.75f, z, 0.4f, h * 0.5f, 0.4f, RedDragonColors.GOLD_HI))
        }
    }

    fun wingBoxes(sx: Float): List<VoxelBox> = buildList {
        add(VoxelBox(sx * 1.5f, 0.8f, 0f, 3f, 0.8f, 0.8f, RedDragonColors.RED_LO))
        add(VoxelBox(sx * 3.8f, 2.2f, -0.2f, 2.2f, 0.8f, 0.8f, RedDragonColors.RED_LO))
        add(VoxelBox(sx * 5.6f, 3.4f, -0.4f, 1.8f, 0.7f, 0.7f, RedDragonColors.RED_DK))
        add(VoxelBox(sx * 6.8f, 4.2f, -0.4f, 0.7f, 1f, 0.7f, RedDragonColors.GOLD))

        val panels = arrayOf(
            floatArrayOf(1.5f, -0.4f, 2.6f),
            floatArrayOf(3f, 0.2f, 4f),
            floatArrayOf(4.4f, 1f, 4.4f),
            floatArrayOf(5.6f, 1.6f, 4.2f),
            floatArrayOf(6.6f, 2.4f, 3.2f)
        )
        for (i in panels.indices) {
            val x = panels[i][0]
            val y = panels[i][1]
            val h = panels[i][2]
            val col = if (i % 2 == 1) RedDragonColors.GOLD_LO else RedDragonColors.GOLD
            add(VoxelBox(sx * x, y - h / 2f + 0.6f, -0.5f, 1.3f, h, 0.3f, col))
            add(VoxelBox(sx * x, y - h + 0.4f, -0.5f, 0.5f, 0.5f, 0.35f, RedDragonColors.RED_DK))
        }
    }

    val wingLeftBoxes: List<VoxelBox> = wingBoxes(-1f)
    val wingRightBoxes: List<VoxelBox> = wingBoxes(1f)

    val tailSegmentSizes = floatArrayOf(2.8f, 2.2f, 1.6f, 1.1f)
    val tailSegmentBoxes: List<List<VoxelBox>> = tailSegmentSizes.indices.map { i ->
        val sz = tailSegmentSizes[i]
        val col = if (i % 2 == 1) RedDragonColors.RED_LO else RedDragonColors.RED
        listOf(
            VoxelBox(0f, 0f, -1f, sz, sz, 2f, col),
            VoxelBox(0f, sz / 2f + 0.25f, -1f, 0.5f, 0.5f, 0.6f, RedDragonColors.GOLD)
        )
    }

    val tailTipBoxes: List<VoxelBox> = listOf(
        VoxelBox(0f, 0.4f, -0.6f, 1.8f, 0.4f, 1.2f, RedDragonColors.GOLD),
        VoxelBox(0f, 0.4f, -1.4f, 0.8f, 0.4f, 0.6f, RedDragonColors.GOLD_HI)
    )

    val headBoxes: List<VoxelBox> = buildList {
        add(VoxelBox(0f, 2f, 0f, 5f, 4f, 4.6f, RedDragonColors.RED))
        add(VoxelBox(0f, 2f, 0f, 5.6f, 3f, 3.6f, RedDragonColors.RED))
        add(VoxelBox(0f, 4.2f, -0.3f, 4f, 0.6f, 3.6f, RedDragonColors.RED_HI))
        // Brow
        add(VoxelBox(0f, 3.5f, 2.4f, 4.6f, 0.7f, 0.4f, RedDragonColors.RED_DK))
        // Eyes
        for (sx in floatArrayOf(-1f, 1f)) {
            add(VoxelBox(sx * 1.35f, 2.6f, 2.35f, 1.3f, 1f, 0.2f, RedDragonColors.EYE, emissive = 2.2f))
            add(VoxelBox(sx * 1.1f, 2.5f, 2.47f, 0.4f, 0.7f, 0.1f, RedDragonColors.PUPIL))
        }
        // Upper snout + nostrils + upper fangs + mouth roof
        add(VoxelBox(0f, 1.5f, 3.4f, 3.8f, 1.6f, 2.6f, RedDragonColors.RED))
        add(VoxelBox(0f, 2.4f, 3.2f, 3f, 0.4f, 2f, RedDragonColors.RED_HI))
        for (nx in floatArrayOf(-0.9f, 0.9f)) {
            add(VoxelBox(nx, 1.9f, 4.75f, 0.4f, 0.4f, 0.1f, RedDragonColors.RED_DK))
        }
        for (fx in floatArrayOf(-1.4f, -0.5f, 0.5f, 1.4f)) {
            add(VoxelBox(fx, 0.55f, 4.2f, 0.35f, 0.4f, 0.35f, RedDragonColors.BONE))
        }
        add(VoxelBox(0f, 0.9f, 2.6f, 3.4f, 0.4f, 2.6f, RedDragonColors.MOUTH))
        // Horns & ear spikes
        for (sx in floatArrayOf(-1f, 1f)) {
            add(VoxelBox(sx * 2f, 4.5f, -0.8f, 0.9f, 1f, 0.9f, RedDragonColors.GOLD_LO))
            add(VoxelBox(sx * 2.4f, 5.4f, -1.3f, 0.7f, 1f, 0.7f, RedDragonColors.GOLD))
            add(VoxelBox(sx * 2.7f, 6.2f, -1.9f, 0.5f, 0.8f, 0.5f, RedDragonColors.GOLD_HI))
            add(VoxelBox(sx * 3f, 2.2f, -0.5f, 0.6f, 1.2f, 1f, RedDragonColors.GOLD))
        }
        // Crown spikes
        for (x in floatArrayOf(-0.9f, 0f, 0.9f)) {
            val isCenter = x == 0f
            add(
                VoxelBox(
                    px = x,
                    py = if (isCenter) 5.1f else 4.8f,
                    pz = 0.6f,
                    sx = 0.6f,
                    sy = if (isCenter) 1.2f else 0.7f,
                    sz = 0.6f,
                    color = if (isCenter) RedDragonColors.GOLD_HI else RedDragonColors.GOLD
                )
            )
        }
    }

    val jawBoxes: List<VoxelBox> = buildList {
        add(VoxelBox(0f, -0.6f, 1.6f, 3.4f, 1f, 3f, RedDragonColors.RED_LO))
        add(VoxelBox(0f, -0.05f, 1.6f, 2.8f, 0.15f, 2.6f, RedDragonColors.MOUTH))
        for (fx in floatArrayOf(-1.2f, 1.2f)) {
            add(VoxelBox(fx, 0.15f, 2.8f, 0.35f, 0.4f, 0.35f, RedDragonColors.BONE))
        }
        add(VoxelBox(0f, -1.15f, 1.2f, 2.4f, 0.3f, 2f, RedDragonColors.GOLD_LO))
    }
}

class RedDragonModel(
    private val rng: Random = Random(1337)
) : DragonCharacterModel {
    override val descriptor: DragonDescriptor = DragonRegistry.RED_DRAGON

    var phase: Float = 0f
        private set
    var walkAmt: Float = 0f
        private set
    var jaw: Float = 0f
        private set
    var t: Float = 0f
        private set
    var attackUntil: Float = 0f
        private set
    private var spawnAcc: Float = 0f

    // Articulated bone transforms matching RedDragon.tsx useFrame
    var legLRotX: Float = 0f
        private set
    var legRRotX: Float = 0f
        private set
    var armLRotX: Float = 0f
        private set
    var armRRotX: Float = 0f
        private set
    var bodyPosY: Float = 0f
        private set
    var bodyRotZ: Float = 0f
        private set
    val tailRotY = FloatArray(4)
    val wingRotZ = floatArrayOf(-0.35f, 0.35f)
    var headRotX: Float = 0f
        private set
    var jawRotX: Float = 0f
        private set
    var flapPhase: Float = 0f
        private set
    var lastFlapAmplitude: Float = 0.15f
        private set
    var isAttacking: Boolean = false
        private set

    // Pooled 3D pixel-snapped fire cubes (mirrors instancedMesh in RedDragon.tsx)
    val fireCubes = Array(RedDragonConstants.MAX_PARTICLES) { RedDragonFireCube() }

    override fun reset() {
        phase = 0f
        walkAmt = 0f
        jaw = 0f
        t = 0f
        attackUntil = 0f
        spawnAcc = 0f
        legLRotX = 0f
        legRRotX = 0f
        armLRotX = 0f
        armRRotX = 0f
        bodyPosY = 0f
        bodyRotZ = 0f
        tailRotY.fill(0f)
        wingRotZ[0] = -0.35f
        wingRotZ[1] = 0.35f
        headRotX = 0f
        jawRotX = 0f
        flapPhase = 0f
        lastFlapAmplitude = 0.15f
        isAttacking = false
        for (c in fireCubes) c.active = false
    }

    override fun fire(seconds: Float) {
        attackUntil = t + seconds
    }

    override fun step(
        dt: Float,
        walking: Boolean,
        mouthOpen: Boolean,
        attacking: Boolean,
        walkSpeed: Float,
        airborne: Boolean,
        doubleJumpBoost: Float
    ) {
        val clampedDt = dt.coerceIn(0f, 0.05f)
        t += clampedDt
        val activeAttack = attacking || t < attackUntil
        isAttacking = activeAttack

        // Walking leg cycle with smooth exponential blend and crisp idle plant
        val tw = if (walking && !activeAttack && !airborne) 1f else 0f
        walkAmt += (tw - walkAmt) * (1f - exp(-8f * clampedDt))
        if (walkAmt > 0.01f) {
            phase += RedDragonConstants.STEP_RATE * walkSpeed * clampedDt * walkAmt
        } else {
            phase = (round(phase / PI.toFloat()) * PI.toFloat())
        }

        val sw = sin(phase) * RedDragonConstants.LEG_SWING * walkAmt
        val legAirTuck = when {
            doubleJumpBoost > 0f -> -0.32f
            airborne -> -0.18f
            else -> 0f
        }
        legLRotX = sw + legAirTuck
        legRRotX = -sw + legAirTuck
        armLRotX = -sw * 0.5f - (if (activeAttack) 0.6f else if (airborne) 0.25f else 0f)
        armRRotX = sw * 0.5f - (if (activeAttack) 0.6f else if (airborne) 0.25f else 0f)
        bodyPosY = abs(sin(phase)) * 0.35f * walkAmt + sin(t * 2f) * 0.08f
        bodyRotZ = sin(phase) * 0.05f * walkAmt

        for (i in tailRotY.indices) {
            tailRotY[i] = sin(t * 3f - i * 0.7f) * (0.12f + 0.1f * walkAmt)
        }

        // Wing flapping speeds up noticeably during jumps and peaks during double-jump boost
        val flapRate = when {
            doubleJumpBoost > 0f -> 22f
            airborne -> 13f
            activeAttack -> 9f
            else -> 2.5f
        }
        val flapAmp = when {
            doubleJumpBoost > 0f -> 0.72f
            airborne -> 0.52f
            activeAttack -> 0.35f
            else -> 0.15f
        }
        val flapBias = when {
            doubleJumpBoost > 0f -> 0.18f
            airborne -> 0.08f
            else -> 0f
        }
        flapPhase += clampedDt * flapRate
        lastFlapAmplitude = flapAmp
        val flap = sin(flapPhase) * flapAmp + flapBias
        wingRotZ[0] = -0.35f - flap
        wingRotZ[1] = 0.35f + flap

        // Head pitch + articulated jaw
        val open = activeAttack || mouthOpen
        val jt = when {
            activeAttack -> RedDragonConstants.JAW_OPEN * 1.3f
            open -> RedDragonConstants.JAW_OPEN
            else -> 0f
        }
        jaw += (jt - jaw) * (1f - exp(-14f * clampedDt))
        jawRotX = jaw
        headRotX = if (activeAttack) -0.12f else -jaw * 0.2f

        // Pixel-snapped 3D fire particle pool (matches RedDragon.tsx particle physics & color ramp)
        if (jaw > RedDragonConstants.JAW_OPEN * 0.5f) {
            spawnAcc += clampedDt * (if (activeAttack) 90f else 18f)
            while (spawnAcc >= 1f) {
                spawnAcc -= 1f
                spawnCube(activeAttack)
            }
        } else {
            spawnAcc = 0f
        }

        updateFireCubes(clampedDt)
    }

    private fun spawnCube(big: Boolean) {
        val slot = fireCubes.firstOrNull { !it.active } ?: return
        // Mouth local position in scaled units (~1.25 forward, ~1.65 up)
        slot.active = true
        slot.x = 0f
        slot.y = (12.5f + bodyPosY) * RedDragonConstants.MODEL_SCALE
        slot.z = 6.4f * RedDragonConstants.MODEL_SCALE
        val spread = if (big) 0.35f else 0.5f
        val speed = if (big) 4f + rng.nextFloat() * 2.5f else 0.6f + rng.nextFloat() * 0.7f
        slot.vx = (rng.nextFloat() - 0.5f) * spread * speed * 0.6f
        slot.vy = if (big) (rng.nextFloat() - 0.3f) * 0.8f else 0.5f + rng.nextFloat() * 0.6f
        slot.vz = speed + (rng.nextFloat() - 0.5f) * spread * speed * 0.6f
        val maxLife = if (big) 0.7f + rng.nextFloat() * 0.5f else 0.35f + rng.nextFloat() * 0.3f
        slot.maxLife = maxLife
        slot.life = maxLife
        slot.size = if (big) 0.12f + rng.nextFloat() * 0.12f else 0.04f + rng.nextFloat() * 0.04f
        slot.big = big
        slot.color = RedDragonColors.FIRE_RAMP[0]
    }

    private fun updateFireCubes(dt: Float) {
        val ramp = RedDragonColors.FIRE_RAMP
        for (pt in fireCubes) {
            if (!pt.active) continue
            pt.life -= dt
            if (pt.life <= 0f) {
                pt.active = false
                continue
            }
            pt.x += pt.vx * dt
            pt.y += pt.vy * dt
            pt.z += pt.vz * dt
            pt.vy += (if (pt.big) 0.9f else 1.2f) * dt
            val drag = exp(-(if (pt.big) 1.2f else 2f) * dt)
            pt.vx *= drag
            pt.vy *= drag
            pt.vz *= drag

            val r = (1f - (pt.life / pt.maxLife).coerceIn(0f, 1f))
            val f = r * (ramp.size - 1)
            val i0 = floor(f).toInt().coerceIn(0, ramp.size - 1)
            val i1 = (i0 + 1).coerceAtMost(ramp.size - 1)
            pt.color = lerpArgb(ramp[i0], ramp[i1], f - i0)
        }
    }

    override fun mouthOffsetPx(facingRight: Boolean, out: FloatArray) {
        // Scaled mouth offset in 2D platformer logical pixels relative to dragon hitbox top-left (12x18)
        // Hitbox center X is +6f; feet bottom is +18f.
        // Local mouthPt is at (0, 12.5, 6.4) raw voxel units; at ~1.28px/voxel that projects ~8px forward, ~16px up from feet.
        val forwardPx = 8.5f
        out[0] = if (facingRight) 6f + forwardPx else 6f - forwardPx
        out[1] = 3.2f - bodyPosY * 1.1f + jawRotX * 1.8f
    }

    private fun lerpArgb(c0: Int, c1: Int, t: Float): Int {
        val f = t.coerceIn(0f, 1f)
        val r0 = (c0 ushr 16) and 0xFF
        val g0 = (c0 ushr 8) and 0xFF
        val b0 = c0 and 0xFF
        val r1 = (c1 ushr 16) and 0xFF
        val g1 = (c1 ushr 8) and 0xFF
        val b1 = c1 and 0xFF
        val r = (r0 + (r1 - r0) * f).toInt().coerceIn(0, 255)
        val g = (g0 + (g1 - g0) * f).toInt().coerceIn(0, 255)
        val b = (b0 + (b1 - b0) * f).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}
