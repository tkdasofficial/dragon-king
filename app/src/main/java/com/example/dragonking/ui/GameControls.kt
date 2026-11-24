package com.example.dragonking.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DragonAmber
import com.example.ui.theme.DragonCrimson
import com.example.ui.theme.DragonGold
import com.example.ui.theme.DragonIvory
import com.example.ui.theme.DragonObsidian

/**
 * 100% pure blocky, sharp-edged (0dp radius) Nintendo-style pixel-art touch controls.
 * Left/Right rocker on bottom-left; FIRE and JUMP aligned cleanly on bottom-right.
 */
@Composable
fun GameTouchControls(
    onLeftChanged: (Boolean) -> Unit,
    onRightChanged: (Boolean) -> Unit,
    onJumpChanged: (Boolean) -> Unit,
    onFireChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        // Bottom-left: < and > cleanly side-by-side with slide-across tracking
        HorizontalDPad(
            onLeftChanged = onLeftChanged,
            onRightChanged = onRightChanged
        )

        // Bottom-right: FIRE and JUMP aligned cleanly on the same baseline
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            PixelActionButton(
                label = "FIRE",
                kind = PixelIconKind.FIRE,
                faceColor = Color(0xCC9E1B1E),
                pressedFaceColor = Color(0xEECC2828),
                bevelLight = Color(0xFFFF8A65),
                bevelDark = Color(0xFF4A0A0D),
                accentBorder = DragonAmber,
                contentDesc = "Breathe Fire",
                tag = "fire_button",
                onPressedChange = onFireChanged
            )
            PixelActionButton(
                label = "JUMP",
                kind = PixelIconKind.JUMP,
                faceColor = Color(0xCC1E6B30),
                pressedFaceColor = Color(0xEE2E8F43),
                bevelLight = Color(0xFF81E075),
                bevelDark = Color(0xFF0E3818),
                accentBorder = DragonGold,
                contentDesc = "Jump",
                tag = "jump_button",
                onPressedChange = onJumpChanged
            )
        }
    }
}

private enum class PixelIconKind {
    LEFT, RIGHT, FIRE, JUMP
}

@Composable
private fun HorizontalDPad(
    onLeftChanged: (Boolean) -> Unit,
    onRightChanged: (Boolean) -> Unit
) {
    var leftActive by remember { mutableStateOf(false) }
    var rightActive by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .testTag("dpad_pad")
            .semantics { contentDescription = "Move Left and Right Pad" }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val halfWidth = size.width / 2f

                    fun updateDir(x: Float, active: Boolean) {
                        val isLeft = active && x < halfWidth
                        val isRight = active && x >= halfWidth
                        if (isLeft != leftActive) {
                            leftActive = isLeft
                            onLeftChanged(isLeft)
                        }
                        if (isRight != rightActive) {
                            rightActive = isRight
                            onRightChanged(isRight)
                        }
                    }

                    updateDir(down.position.x, true)

                    while (true) {
                        val event = awaitPointerEvent()
                        val tracked = event.changes.firstOrNull { it.id == down.id }
                        if (tracked == null || !tracked.pressed) {
                            updateDir(0f, false)
                            break
                        }
                        updateDir(tracked.position.x, true)
                        tracked.consume()
                    }
                }
            },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        DPadDirectionBlock(
            kind = PixelIconKind.LEFT,
            pressed = leftActive,
            contentDesc = "Move Left",
            tag = "dpad_left"
        )
        DPadDirectionBlock(
            kind = PixelIconKind.RIGHT,
            pressed = rightActive,
            contentDesc = "Move Right",
            tag = "dpad_right"
        )
    }
}

@Composable
private fun DPadDirectionBlock(
    kind: PixelIconKind,
    pressed: Boolean,
    contentDesc: String,
    tag: String
) {
    val faceColor = if (pressed) Color(0xE63D2B59) else Color(0xCC221833)
    val bevelLight = if (pressed) DragonGold else Color(0xFF7B689E)
    val bevelDark = Color(0xFF0E0817)

    Box(
        modifier = Modifier
            .size(width = 68.dp, height = 64.dp)
            .offset { IntOffset(0, if (pressed) 3 else 0) }
            .pixelBevelBlock(
                faceColor = faceColor,
                outerBorder = DragonObsidian,
                innerBorder = if (pressed) DragonGold else Color(0xFF9C89C2),
                bevelTopLeft = if (pressed) bevelDark else bevelLight,
                bevelBottomRight = if (pressed) bevelLight else bevelDark
            )
            .testTag(tag)
            .semantics { contentDescription = contentDesc },
        contentAlignment = Alignment.Center
    ) {
        PixelGlyphIcon(
            kind = kind,
            primaryColor = if (pressed) DragonGold else DragonIvory,
            secondaryColor = DragonAmber,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun PixelActionButton(
    label: String,
    kind: PixelIconKind,
    faceColor: Color,
    pressedFaceColor: Color,
    bevelLight: Color,
    bevelDark: Color,
    accentBorder: Color,
    contentDesc: String,
    tag: String,
    onPressedChange: (Boolean) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(width = 70.dp, height = 64.dp)
            .offset { IntOffset(0, if (pressed) 3 else 0) }
            .pixelBevelBlock(
                faceColor = if (pressed) pressedFaceColor else faceColor,
                outerBorder = DragonObsidian,
                innerBorder = if (pressed) DragonIvory else accentBorder,
                bevelTopLeft = if (pressed) bevelDark else bevelLight,
                bevelBottomRight = if (pressed) bevelLight else bevelDark
            )
            .testTag(tag)
            .semantics { contentDescription = contentDesc }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    onPressedChange(true)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val tracked = event.changes.firstOrNull { it.id == down.id }
                        if (tracked == null || !tracked.pressed) {
                            pressed = false
                            onPressedChange(false)
                            break
                        }
                        tracked.consume()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PixelGlyphIcon(
                kind = kind,
                primaryColor = DragonIvory,
                secondaryColor = DragonGold,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = if (pressed) DragonGold else DragonIvory
            )
        }
    }
}

/**
 * Pure 90-degree sharp-edged pixel-art double border + 3D hardware bevel modifier (border-radius: 0).
 */
fun Modifier.pixelBevelBlock(
    faceColor: Color,
    outerBorder: Color = DragonObsidian,
    innerBorder: Color = Color(0xFF7B689E),
    bevelTopLeft: Color = Color(0xFF9E8BC2),
    bevelBottomRight: Color = Color(0xFF100A1A)
): Modifier = this
    .background(faceColor)
    .drawBehind {
        val w = size.width
        val h = size.height
        val p = 2.dp.toPx()
        if (w <= p * 4f || h <= p * 4f) return@drawBehind

        // Top-left bright bevel strip (inside outer border)
        drawRect(color = bevelTopLeft, topLeft = Offset(p * 2, p * 2), size = Size(w - p * 4, p))
        drawRect(color = bevelTopLeft, topLeft = Offset(p * 2, p * 2), size = Size(p, h - p * 4))

        // Bottom-right dark shadow bevel strip
        drawRect(color = bevelBottomRight, topLeft = Offset(p * 2, h - p * 3), size = Size(w - p * 4, p))
        drawRect(color = bevelBottomRight, topLeft = Offset(w - p * 3, p * 2), size = Size(p, h - p * 4))

        // Inner accent frame
        drawRect(color = innerBorder, topLeft = Offset(p, p), size = Size(w - p * 2, p))
        drawRect(color = innerBorder, topLeft = Offset(p, h - p * 2), size = Size(w - p * 2, p))
        drawRect(color = innerBorder, topLeft = Offset(p, p), size = Size(p, h - p * 2))
        drawRect(color = innerBorder, topLeft = Offset(w - p * 2, p), size = Size(p, h - p * 2))

        // Outer crisp 90-degree dark obsidian frame
        drawRect(color = outerBorder, topLeft = Offset(0f, 0f), size = Size(w, p))
        drawRect(color = outerBorder, topLeft = Offset(0f, h - p), size = Size(w, p))
        drawRect(color = outerBorder, topLeft = Offset(0f, 0f), size = Size(p, h))
        drawRect(color = outerBorder, topLeft = Offset(w - p, 0f), size = Size(p, h))
    }

enum class PixelHudIconKind {
    HEART_FULL, HEART_EMPTY, GEM, COIN, CLOCK, PAUSE, SOUND_ON, SOUND_OFF, FLAME, RESET
}

@Composable
fun PixelHudIcon(
    kind: PixelHudIconKind,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val grid = 9
        val cell = size.width / grid
        fun px(gx: Int, gy: Int, w: Int = 1, h: Int = 1, color: Color = tint) {
            drawRect(
                color = color,
                topLeft = Offset(gx * cell, gy * cell),
                size = Size(w * cell, h * cell)
            )
        }

        when (kind) {
            PixelHudIconKind.HEART_FULL -> {
                px(1, 1, 3, 1)
                px(5, 1, 3, 1)
                px(0, 2, 4, 2)
                px(5, 2, 4, 2)
                px(4, 3, 1, 1)
                px(1, 2, 1, 1, DragonIvory)
                px(0, 4, 9, 1)
                px(1, 5, 7, 1)
                px(2, 6, 5, 1)
                px(3, 7, 3, 1)
                px(4, 8, 1, 1)
            }
            PixelHudIconKind.HEART_EMPTY -> {
                px(1, 1, 3, 1)
                px(5, 1, 3, 1)
                px(0, 2, 1, 3)
                px(8, 2, 1, 3)
                px(4, 2, 1, 1)
                px(1, 5, 1, 1)
                px(7, 5, 1, 1)
                px(2, 6, 1, 1)
                px(6, 6, 1, 1)
                px(3, 7, 1, 1)
                px(5, 7, 1, 1)
                px(4, 8, 1, 1)
            }
            PixelHudIconKind.GEM -> {
                px(3, 0, 3, 1, DragonIvory)
                px(2, 1, 5, 1, tint)
                px(1, 2, 7, 2, tint)
                px(2, 2, 2, 1, DragonIvory)
                px(0, 4, 9, 1, tint)
                px(1, 5, 7, 1, DragonAmber)
                px(2, 6, 5, 1, DragonAmber)
                px(3, 7, 3, 1, tint)
                px(4, 8, 1, 1, DragonAmber)
            }
            PixelHudIconKind.COIN -> {
                // Crisp circular pixel coin with bright specular edge & inner vertical ridge
                px(2, 0, 5, 1, DragonAmber)
                px(1, 1, 7, 1, tint)
                px(0, 2, 9, 5, tint)
                px(2, 1, 2, 1, DragonIvory)
                px(1, 2, 1, 3, DragonIvory)
                px(4, 2, 1, 5, DragonAmber)
                px(1, 7, 7, 1, DragonAmber)
                px(2, 8, 5, 1, Color(0xFF9C5900))
            }
            PixelHudIconKind.CLOCK -> {
                // Compact pixel stopwatch with crown button & L-shaped hands
                px(3, 0, 3, 1, DragonGold)
                px(2, 1, 5, 1, tint)
                px(1, 2, 1, 5, tint)
                px(7, 2, 1, 5, tint)
                px(2, 7, 5, 1, tint)
                px(2, 2, 5, 5, Color(0xFF1D142B))
                px(4, 2, 1, 3, DragonIvory)
                px(4, 4, 2, 1, DragonGold)
            }
            PixelHudIconKind.PAUSE -> {
                // Sharp pixel "||" pause bars with subtle top highlight
                px(2, 1, 2, 7, tint)
                px(5, 1, 2, 7, tint)
                px(2, 1, 2, 1, DragonGold)
                px(5, 1, 2, 1, DragonGold)
            }
            PixelHudIconKind.SOUND_ON -> {
                px(1, 3, 2, 3, tint)
                px(3, 2, 1, 5, tint)
                px(4, 1, 1, 7, tint)
                px(6, 3, 1, 3, DragonGold)
                px(8, 2, 1, 5, DragonGold)
            }
            PixelHudIconKind.SOUND_OFF -> {
                px(1, 3, 2, 3, tint)
                px(3, 2, 1, 5, tint)
                px(4, 1, 1, 7, tint)
                px(6, 2, 1, 1, DragonCrimson)
                px(8, 2, 1, 1, DragonCrimson)
                px(7, 3, 1, 3, DragonCrimson)
                px(6, 6, 1, 1, DragonCrimson)
                px(8, 6, 1, 1, DragonCrimson)
            }
            PixelHudIconKind.FLAME -> {
                px(4, 0, 1, 2, DragonGold)
                px(3, 2, 3, 1, DragonGold)
                px(2, 3, 5, 2, tint)
                px(1, 5, 7, 3, tint)
                px(3, 4, 3, 3, DragonIvory)
                px(2, 8, 5, 1, DragonCrimson)
            }
            PixelHudIconKind.RESET -> {
                px(2, 1, 5, 1)
                px(1, 2, 2, 1)
                px(6, 2, 2, 1)
                px(6, 0, 1, 3)
                px(5, 3, 3, 1)
                px(1, 3, 1, 3)
                px(1, 6, 2, 1)
                px(2, 7, 5, 1)
                px(6, 6, 2, 1)
                px(7, 5, 1, 1)
            }
        }
    }
}

@Composable
private fun PixelGlyphIcon(
    kind: PixelIconKind,
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val grid = 9
        val cell = size.width / grid
        fun px(gx: Int, gy: Int, w: Int = 1, h: Int = 1, color: Color = primaryColor) {
            drawRect(
                color = color,
                topLeft = Offset(gx * cell, gy * cell),
                size = Size(w * cell, h * cell)
            )
        }

        when (kind) {
            PixelIconKind.LEFT -> {
                // Sharp blocky left chevron "<"
                px(5, 1, 2, 1)
                px(4, 2, 2, 1)
                px(3, 3, 2, 1)
                px(2, 4, 2, 1, secondaryColor)
                px(3, 5, 2, 1)
                px(4, 6, 2, 1)
                px(5, 7, 2, 1)
            }
            PixelIconKind.RIGHT -> {
                // Sharp blocky right chevron ">"
                px(2, 1, 2, 1)
                px(3, 2, 2, 1)
                px(4, 3, 2, 1)
                px(5, 4, 2, 1, secondaryColor)
                px(4, 5, 2, 1)
                px(3, 6, 2, 1)
                px(2, 7, 2, 1)
            }
            PixelIconKind.JUMP -> {
                // Sharp blocky upward arrow
                px(4, 1, 1, 1, secondaryColor)
                px(3, 2, 3, 1)
                px(2, 3, 5, 1)
                px(1, 4, 7, 1)
                px(3, 5, 3, 3)
            }
            PixelIconKind.FIRE -> {
                // Sharp multi-tone blocky pixel flame
                px(4, 0, 1, 2, secondaryColor)
                px(3, 2, 3, 1, secondaryColor)
                px(2, 3, 5, 2, DragonAmber)
                px(1, 5, 7, 3, DragonAmber)
                px(3, 4, 3, 3, primaryColor)
                px(4, 6, 1, 2, secondaryColor)
                px(2, 8, 5, 1, DragonCrimson)
            }
        }
    }
}
