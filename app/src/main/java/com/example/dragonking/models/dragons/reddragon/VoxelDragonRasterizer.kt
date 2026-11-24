package com.example.dragonking.models.dragons.reddragon

import com.example.dragonking.pixel.PixelPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Zero-allocation orthographic 3D-to-pixel-art voxel rasterizer for [RedDragonModel].
 * Renders the articulated 3D bone hierarchy (`legL`, `legR`, `body`, `wingL`, `wingR`,
 * `armL`, `armR`, 4-segment `tail`, `head`, `jaw`) into a crisp pixel buffer with Z-buffering,
 * flat-shaded dual directional lights (matching `DragonScene.tsx`), emissive eyes,
 * and a 1px dark silhouette outline matching the 16-bit pixel-art world.
 */
class VoxelDragonRasterizer {
    companion object {
        const val SCRATCH_W = 48
        const val SCRATCH_H = 38
        private const val ANCHOR_X = 24f
        private const val ANCHOR_Y = 33f
        private const val VOXEL_TO_PX = 1.24f // 18.2 voxels * 1.24 ≈ 22.6px height

        // Heroic 3/4 side-view camera angles so both eyes, horns, wings & belly plates read clearly
        private const val VIEW_YAW_OFFSET = 0.34f
        private const val VIEW_PITCH = 0.14f

        private val CORNER_X = floatArrayOf(-0.5f, 0.5f, 0.5f, -0.5f, -0.5f, 0.5f, 0.5f, -0.5f)
        private val CORNER_Y = floatArrayOf(-0.5f, -0.5f, 0.5f, 0.5f, -0.5f, -0.5f, 0.5f, 0.5f)
        private val CORNER_Z = floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f, -0.5f, -0.5f, -0.5f, -0.5f)

        // 6 faces in CCW order matching right-hand rule: cross(v1 - v0, v2 - v0) == outward normal
        private val FACE_INDICES = arrayOf(
            intArrayOf(2, 1, 5, 6), // +X
            intArrayOf(7, 4, 0, 3), // -X
            intArrayOf(7, 3, 2, 6), // +Y
            intArrayOf(0, 4, 5, 1), // -Y
            intArrayOf(3, 0, 1, 2), // +Z
            intArrayOf(6, 5, 4, 7)  // -Z
        )
        private val FACE_NX = floatArrayOf(1f, -1f, 0f, 0f, 0f, 0f)
        private val FACE_NY = floatArrayOf(0f, 0f, 1f, -1f, 0f, 0f)
        private val FACE_NZ = floatArrayOf(0f, 0f, 0f, 0f, 1f, -1f)

        // Normalized directional lights mirroring DragonScene.tsx:
        // Key light (5, 8, 4) warm #ffe2c2; Rim light (-5, 3, -4) cool #7aa0ff
        private const val K_LEN = 10.246951f // sqrt(25+64+16)
        private const val K_LX = 5f / K_LEN
        private const val K_LY = 8f / K_LEN
        private const val K_LZ = 4f / K_LEN

        private const val R_LEN = 7.071068f // sqrt(25+9+16)
        private const val R_LX = -5f / R_LEN
        private const val R_LY = 3f / R_LEN
        private const val R_LZ = -4f / R_LEN
    }

    private val scratchPixels = IntArray(SCRATCH_W * SCRATCH_H)
    private val zBuffer = FloatArray(SCRATCH_W * SCRATCH_H)
    private val occupiedMask = BooleanArray(SCRATCH_W * SCRATCH_H)

    // Preallocated matrix stack (no allocations per frame)
    private val matRoot = FloatArray(16)
    private val matLegL = FloatArray(16)
    private val matLegR = FloatArray(16)
    private val matBody = FloatArray(16)
    private val matWingL = FloatArray(16)
    private val matWingR = FloatArray(16)
    private val matArmL = FloatArray(16)
    private val matArmR = FloatArray(16)
    private val matTail = Array(4) { FloatArray(16) }
    private val matTailTip = FloatArray(16)
    private val matHead = FloatArray(16)
    private val matJaw = FloatArray(16)
    private val matBox = FloatArray(16)

    // Projected corner scratch arrays
    private val cornerSx = FloatArray(8)
    private val cornerSy = FloatArray(8)
    private val cornerSz = FloatArray(8)

    fun renderDragon(
        model: RedDragonModel,
        facingRight: Boolean,
        hitboxScreenX: Int,
        hitboxScreenY: Int,
        hitboxWidth: Int,
        hitboxHeight: Int,
        destBuffer: IntArray,
        destWidth: Int,
        destHeight: Int
    ) {
        rasterizeToScratch(model, facingRight)

        // Align dragon feet (ANCHOR_Y) with hitbox bottom, centered horizontally around hitbox
        val drawX = hitboxScreenX + hitboxWidth / 2 - ANCHOR_X.roundToInt()
        val drawY = hitboxScreenY + hitboxHeight - ANCHOR_Y.roundToInt()

        val startY = max(0, -drawY)
        val endY = min(SCRATCH_H, destHeight - drawY)
        val startX = max(0, -drawX)
        val endX = min(SCRATCH_W, destWidth - drawX)
        if (startX >= endX || startY >= endY) return

        for (sy in startY until endY) {
            val dstRow = (drawY + sy) * destWidth + drawX
            val srcRow = sy * SCRATCH_W
            for (sx in startX until endX) {
                val col = scratchPixels[srcRow + sx]
                if (col != 0) {
                    destBuffer[dstRow + sx] = col
                }
            }
        }
    }

    fun rasterizeToScratch(model: RedDragonModel, facingRight: Boolean): IntArray {
        scratchPixels.fill(0)
        zBuffer.fill(Float.NEGATIVE_INFINITY)
        occupiedMask.fill(false)

        val yaw = if (facingRight) {
            (PI.toFloat() * 0.5f) - VIEW_YAW_OFFSET
        } else {
            -(PI.toFloat() * 0.5f) + VIEW_YAW_OFFSET
        }

        // Root camera/model transform (raw voxel units -> camera space)
        setIdentity(matRoot)
        rotateX(matRoot, VIEW_PITCH)
        rotateY(matRoot, yaw)

        // Legs
        copyMat(matRoot, matLegL)
        translate(matLegL, -1.9f, 5f, 0f)
        rotateX(matLegL, model.legLRotX)
        drawBoxes(RedDragonRigData.legBoxes, matLegL)

        copyMat(matRoot, matLegR)
        translate(matLegR, 1.9f, 5f, 0f)
        rotateX(matLegR, model.legRRotX)
        drawBoxes(RedDragonRigData.legBoxes, matLegR)

        // Body
        copyMat(matRoot, matBody)
        translate(matBody, 0f, model.bodyPosY, 0f)
        rotateZ(matBody, model.bodyRotZ)
        drawBoxes(RedDragonRigData.torsoBoxes, matBody)

        // Wings
        copyMat(matBody, matWingL)
        translate(matWingL, -2.8f, 10.6f, -2f)
        rotateY(matWingL, -1.25f)
        rotateZ(matWingL, model.wingRotZ[0])
        drawBoxes(RedDragonRigData.wingLeftBoxes, matWingL)

        copyMat(matBody, matWingR)
        translate(matWingR, 2.8f, 10.6f, -2f)
        rotateY(matWingR, 1.25f)
        rotateZ(matWingR, model.wingRotZ[1])
        drawBoxes(RedDragonRigData.wingRightBoxes, matWingR)

        // Arms
        copyMat(matBody, matArmL)
        translate(matArmL, -3.6f, 9.3f, 1.8f)
        rotateX(matArmL, model.armLRotX)
        drawBoxes(RedDragonRigData.armBoxes, matArmL)

        copyMat(matBody, matArmR)
        translate(matArmR, 3.6f, 9.3f, 1.8f)
        rotateX(matArmR, model.armRRotX)
        drawBoxes(RedDragonRigData.armBoxes, matArmR)

        // 4-segment articulated tail chain + gold spade tip
        var parentTail = matBody
        copyMat(parentTail, matTail[0])
        translate(matTail[0], 0f, 6.8f, -3.4f)
        for (i in 0 until 4) {
            if (i > 0) {
                copyMat(matTail[i - 1], matTail[i])
                val yOff = -0.25f + (if (i == 1) -0.4f else 0.1f)
                translate(matTail[i], 0f, yOff, -2f)
            }
            rotateY(matTail[i], model.tailRotY[i])
            drawBoxes(RedDragonRigData.tailSegmentBoxes[i], matTail[i])
        }
        copyMat(matTail[3], matTailTip)
        translate(matTailTip, 0f, -0.15f, -2f)
        drawBoxes(RedDragonRigData.tailTipBoxes, matTailTip)

        // Head & articulated jaw
        copyMat(matBody, matHead)
        translate(matHead, 0f, 11.6f, 1.6f)
        rotateX(matHead, model.headRotX)
        drawBoxes(RedDragonRigData.headBoxes, matHead)

        copyMat(matHead, matJaw)
        translate(matJaw, 0f, 0.7f, 1.4f)
        rotateX(matJaw, model.jawRotX)
        drawBoxes(RedDragonRigData.jawBoxes, matJaw)

        // Apply crisp 1px dark silhouette outline on empty pixels bordering rendered dragon voxels
        applyPixelOutline()
        return scratchPixels
    }

    private fun drawBoxes(boxes: List<VoxelBox>, parentMat: FloatArray) {
        for (i in boxes.indices) {
            drawBox(boxes[i], parentMat)
        }
    }

    private fun drawBox(box: VoxelBox, parentMat: FloatArray) {
        copyMat(parentMat, matBox)
        translate(matBox, box.px, box.py, box.pz)
        scale(matBox, box.sx, box.sy, box.sz)

        // Project 8 corners of unit cube to scratch screen space
        for (c in 0 until 8) {
            val lx = CORNER_X[c]
            val ly = CORNER_Y[c]
            val lz = CORNER_Z[c]
            val wx = matBox[0] * lx + matBox[4] * ly + matBox[8] * lz + matBox[12]
            val wy = matBox[1] * lx + matBox[5] * ly + matBox[9] * lz + matBox[13]
            val wz = matBox[2] * lx + matBox[6] * ly + matBox[10] * lz + matBox[14]
            cornerSx[c] = ANCHOR_X + wx * VOXEL_TO_PX
            cornerSy[c] = ANCHOR_Y - wy * VOXEL_TO_PX
            cornerSz[c] = wz
        }

        // Rasterize front-facing faces
        for (f in 0 until 6) {
            val nx0 = FACE_NX[f]
            val ny0 = FACE_NY[f]
            val nz0 = FACE_NZ[f]
            // Transform normal using upper 3x3 (orthogonal rotation * uniform/box scale)
            // Inverse-transpose of diagonal scale divides by (sx, sy, sz), so normalize:
            val wnx = matBox[0] * nx0 / box.sx + matBox[4] * ny0 / box.sy + matBox[8] * nz0 / box.sz
            val wny = matBox[1] * nx0 / box.sx + matBox[5] * ny0 / box.sy + matBox[9] * nz0 / box.sz
            val wnz = matBox[2] * nx0 / box.sx + matBox[6] * ny0 / box.sy + matBox[10] * nz0 / box.sz
            if (wnz <= 0.001f) continue // Backface culled

            val nLen = sqrt(wnx * wnx + wny * wny + wnz * wnz).coerceAtLeast(1e-5f)
            val nnx = wnx / nLen
            val nny = wny / nLen
            val nnz = wnz / nLen

            val shadedColor = shadeColor(box.color, box.emissive, nnx, nny, nnz)
            val idx = FACE_INDICES[f]
            val i0 = idx[0]
            val i1 = idx[1]
            val i2 = idx[2]
            val i3 = idx[3]

            var written = rasterizeTriangle(
                cornerSx[i0], cornerSy[i0], cornerSz[i0],
                cornerSx[i1], cornerSy[i1], cornerSz[i1],
                cornerSx[i2], cornerSy[i2], cornerSz[i2],
                shadedColor
            )
            written = rasterizeTriangle(
                cornerSx[i0], cornerSy[i0], cornerSz[i0],
                cornerSx[i2], cornerSy[i2], cornerSz[i2],
                cornerSx[i3], cornerSy[i3], cornerSz[i3],
                shadedColor
            ) || written

            // Ensure tiny sub-pixel features (fangs, pupils, nostrils, claws) never vanish between pixel centers
            if (!written) {
                val cx = ((cornerSx[i0] + cornerSx[i1] + cornerSx[i2] + cornerSx[i3]) * 0.25f).roundToInt()
                val cy = ((cornerSy[i0] + cornerSy[i1] + cornerSy[i2] + cornerSy[i3]) * 0.25f).roundToInt()
                val cz = (cornerSz[i0] + cornerSz[i1] + cornerSz[i2] + cornerSz[i3]) * 0.25f + 0.02f
                plotPixel(cx, cy, cz, shadedColor)
            }
        }
    }

    private fun rasterizeTriangle(
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        color: Int
    ): Boolean {
        val area = (x1 - x0) * (y2 - y0) - (y1 - y0) * (x2 - x0)
        if (kotlin.math.abs(area) < 1e-4f) return false

        val minX = max(0, kotlin.math.floor(min(x0, min(x1, x2))).toInt())
        val maxX = min(SCRATCH_W - 1, kotlin.math.ceil(max(x0, max(x1, x2))).toInt())
        val minY = max(0, kotlin.math.floor(min(y0, min(y1, y2))).toInt())
        val maxY = min(SCRATCH_H - 1, kotlin.math.ceil(max(y0, max(y1, y2))).toInt())
        if (minX > maxX || minY > maxY) return false

        val invArea = 1f / area
        val sign = if (area > 0f) 1f else -1f
        val eps = -0.02f * kotlin.math.abs(area)
        var anyWritten = false

        for (py in minY..maxY) {
            val cy = py + 0.5f
            for (px in minX..maxX) {
                val cx = px + 0.5f
                val w0 = ((x1 - cx) * (y2 - cy) - (y1 - cy) * (x2 - cx)) * sign
                val w1 = ((x2 - cx) * (y0 - cy) - (y2 - cy) * (x0 - cx)) * sign
                val w2 = ((x0 - cx) * (y1 - cy) - (y0 - cy) * (x1 - cx)) * sign
                if (w0 >= eps && w1 >= eps && w2 >= eps) {
                    val b0 = ((x1 - cx) * (y2 - cy) - (y1 - cy) * (x2 - cx)) * invArea
                    val b1 = ((x2 - cx) * (y0 - cy) - (y2 - cy) * (x0 - cx)) * invArea
                    val b2 = 1f - b0 - b1
                    val z = b0 * z0 + b1 * z1 + b2 * z2
                    plotPixel(px, py, z, color)
                    anyWritten = true
                }
            }
        }
        return anyWritten
    }

    private fun plotPixel(px: Int, py: Int, z: Float, color: Int) {
        if (px !in 0 until SCRATCH_W || py !in 0 until SCRATCH_H) return
        val idx = py * SCRATCH_W + px
        if (z >= zBuffer[idx]) {
            zBuffer[idx] = z
            scratchPixels[idx] = color
            occupiedMask[idx] = true
        }
    }

    private fun applyPixelOutline() {
        val outline = PixelPalette.OUTLINE
        for (y in 0 until SCRATCH_H) {
            val row = y * SCRATCH_W
            for (x in 0 until SCRATCH_W) {
                val idx = row + x
                if (occupiedMask[idx]) continue
                val hasNeighbor =
                    (x > 0 && occupiedMask[idx - 1]) ||
                        (x < SCRATCH_W - 1 && occupiedMask[idx + 1]) ||
                        (y > 0 && occupiedMask[idx - SCRATCH_W]) ||
                        (y < SCRATCH_H - 1 && occupiedMask[idx + SCRATCH_W])
                if (hasNeighbor) {
                    scratchPixels[idx] = outline
                }
            }
        }
    }

    private fun shadeColor(baseArgb: Int, emissive: Float, nx: Float, ny: Float, nz: Float): Int {
        if (emissive >= 1f) return baseArgb
        val r = (baseArgb ushr 16) and 0xFF
        val g = (baseArgb ushr 8) and 0xFF
        val b = baseArgb and 0xFF

        val keyDot = (nx * K_LX + ny * K_LY + nz * K_LZ).coerceAtLeast(0f)
        val rimDot = (nx * R_LX + ny * R_LY + nz * R_LZ).coerceAtLeast(0f)

        // Quantized retro brightness steps with warm key tint & subtle cool rim
        val lightR = (0.44f + keyDot * 0.64f + rimDot * 0.14f).coerceIn(0.38f, 1.15f)
        val lightG = (0.42f + keyDot * 0.58f + rimDot * 0.18f).coerceIn(0.36f, 1.12f)
        val lightB = (0.42f + keyDot * 0.50f + rimDot * 0.28f).coerceIn(0.36f, 1.12f)

        val nr = (r * lightR).toInt().coerceIn(0, 255)
        val ng = (g * lightG).toInt().coerceIn(0, 255)
        val nb = (b * lightB).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (nr shl 16) or (ng shl 8) or nb
    }

    // --- Minimal column-major 4x4 matrix operations (zero allocations) ---

    private fun setIdentity(m: FloatArray) {
        m.fill(0f)
        m[0] = 1f
        m[5] = 1f
        m[10] = 1f
        m[15] = 1f
    }

    private fun copyMat(src: FloatArray, dst: FloatArray) {
        src.copyInto(dst)
    }

    private fun translate(m: FloatArray, x: Float, y: Float, z: Float) {
        m[12] += m[0] * x + m[4] * y + m[8] * z
        m[13] += m[1] * x + m[5] * y + m[9] * z
        m[14] += m[2] * x + m[6] * y + m[10] * z
    }

    private fun scale(m: FloatArray, x: Float, y: Float, z: Float) {
        m[0] *= x; m[1] *= x; m[2] *= x
        m[4] *= y; m[5] *= y; m[6] *= y
        m[8] *= z; m[9] *= z; m[10] *= z
    }

    private fun rotateX(m: FloatArray, rad: Float) {
        if (rad == 0f) return
        val c = cos(rad)
        val s = sin(rad)
        val m4 = m[4]; val m5 = m[5]; val m6 = m[6]
        val m8 = m[8]; val m9 = m[9]; val m10 = m[10]
        m[4] = m4 * c + m8 * s
        m[5] = m5 * c + m9 * s
        m[6] = m6 * c + m10 * s
        m[8] = m8 * c - m4 * s
        m[9] = m9 * c - m5 * s
        m[10] = m10 * c - m6 * s
    }

    private fun rotateY(m: FloatArray, rad: Float) {
        if (rad == 0f) return
        val c = cos(rad)
        val s = sin(rad)
        val m0 = m[0]; val m1 = m[1]; val m2 = m[2]
        val m8 = m[8]; val m9 = m[9]; val m10 = m[10]
        m[0] = m0 * c - m8 * s
        m[1] = m1 * c - m9 * s
        m[2] = m2 * c - m10 * s
        m[8] = m0 * s + m8 * c
        m[9] = m1 * s + m9 * c
        m[10] = m2 * s + m10 * c
    }

    private fun rotateZ(m: FloatArray, rad: Float) {
        if (rad == 0f) return
        val c = cos(rad)
        val s = sin(rad)
        val m0 = m[0]; val m1 = m[1]; val m2 = m[2]
        val m4 = m[4]; val m5 = m[5]; val m6 = m[6]
        m[0] = m0 * c + m4 * s
        m[1] = m1 * c + m5 * s
        m[2] = m2 * c + m6 * s
        m[4] = m4 * c - m0 * s
        m[5] = m5 * c - m1 * s
        m[6] = m6 * c - m2 * s
    }
}
