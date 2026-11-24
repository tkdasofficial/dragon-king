package com.example.dragonking.ui

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.dragonking.engine.HudSnapshot
import com.example.ui.theme.DragonAmber
import com.example.ui.theme.DragonCrimson
import com.example.ui.theme.DragonGold
import com.example.ui.theme.DragonIvory
import com.example.ui.theme.DragonObsidian
import kotlinx.coroutines.isActive

@Composable
fun GameScreen(
    gameViewModel: GameViewModel = remember { GameViewModel() }
) {
    val hud by gameViewModel.hudState.collectAsState()
    val isPaused by gameViewModel.isPaused.collectAsState()
    val soundEnabled by gameViewModel.soundEnabled.collectAsState()
    var frameTick by remember { mutableLongStateOf(0L) }
    val focusRequester = remember { FocusRequester() }

    if (isPaused) {
        BackHandler {
            gameViewModel.resumeGame()
        }
    }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { focusRequester.requestFocus() }
        var lastNanos = 0L
        while (isActive) {
            withFrameNanos { frameNanos ->
                val dt = if (lastNanos == 0L) {
                    1f / 60f
                } else {
                    ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                }
                lastNanos = frameNanos
                gameViewModel.tick(dt)
                frameTick = frameNanos
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DragonObsidian)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                val isDown = keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN
                val isUp = keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_UP
                if (!isDown && !isUp) return@onPreviewKeyEvent false
                val pressed = isDown
                when (keyEvent.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_A -> {
                        gameViewModel.keyLeft = pressed
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_D -> {
                        gameViewModel.keyRight = pressed
                        true
                    }
                    KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_Z -> {
                        gameViewModel.keyJump = pressed
                        true
                    }
                    KeyEvent.KEYCODE_X, KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_ENTER -> {
                        gameViewModel.keyFire = pressed
                        true
                    }
                    KeyEvent.KEYCODE_P, KeyEvent.KEYCODE_ESCAPE -> {
                        if (isDown && !hud.isDead && !hud.levelCleared) {
                            gameViewModel.togglePause()
                        }
                        true
                    }
                    KeyEvent.KEYCODE_R -> {
                        if (isDown) gameViewModel.resetLevel()
                        true
                    }
                    else -> false
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("game_canvas")
        ) {
            @Suppress("UNUSED_VARIABLE")
            val tick = frameTick
            val w = size.width.toInt().coerceAtLeast(1)
            val h = size.height.toInt().coerceAtLeast(1)
            drawIntoCanvas { canvas ->
                gameViewModel.renderer.render(
                    engine = gameViewModel.engine,
                    targetCanvas = canvas.nativeCanvas,
                    targetWidth = w,
                    targetHeight = h
                )
            }
        }

        // Top HUD sits snug near the top, left, and right safe-area screen edges
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(start = 12.dp, end = 12.dp, top = 10.dp)
        ) {
            TopHudBar(
                hud = hud,
                onPauseClick = {
                    if (!hud.isDead && !hud.levelCleared) {
                        gameViewModel.togglePause()
                    }
                },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        // Bottom touch controls stay anchored cleanly with comfortable margin
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            GameTouchControls(
                onLeftChanged = { gameViewModel.touchLeft = it },
                onRightChanged = { gameViewModel.touchRight = it },
                onJumpChanged = { gameViewModel.touchJump = it },
                onFireChanged = { gameViewModel.touchFire = it },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        if (isPaused && !hud.isDead && !hud.levelCleared) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x990D0717))
                    .clickable(enabled = false, onClick = {}),
                contentAlignment = Alignment.Center
            ) {
                PauseMenuOverlay(
                    soundEnabled = soundEnabled,
                    onResume = { gameViewModel.resumeGame() },
                    onRestart = { gameViewModel.resetLevel() },
                    onToggleSound = { gameViewModel.toggleSound() }
                )
            }
        }

        if (hud.isDead || hud.levelCleared) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EndBannerOverlay(
                    cleared = hud.levelCleared,
                    hud = hud,
                    onRetry = { gameViewModel.resetLevel() }
                )
            }
        }
    }
}

@Composable
private fun TopHudBar(
    hud: HudSnapshot,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Top-Left Group: Crisp square Pause button ("||") + 3 Life Hearts in a single row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val pauseLabel = stringResource(R.string.action_pause)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .pixelBevelBlock(
                        faceColor = Color(0xCC221833),
                        outerBorder = DragonObsidian,
                        innerBorder = Color(0xFF9C89C2),
                        bevelTopLeft = Color(0xFF9E8BC2),
                        bevelBottomRight = Color(0xFF0E0817)
                    )
                    .clickable(onClick = onPauseClick)
                    .testTag("pause_button")
                    .semantics { contentDescription = pauseLabel },
                contentAlignment = Alignment.Center
            ) {
                PixelHudIcon(
                    kind = PixelHudIconKind.PAUSE,
                    tint = DragonIvory,
                    modifier = Modifier.size(18.dp)
                )
            }

            Row(
                modifier = Modifier
                    .pixelBevelBlock(
                        faceColor = Color(0xCC181026),
                        outerBorder = DragonObsidian,
                        innerBorder = Color(0xFF7B689E),
                        bevelTopLeft = Color(0xFF9E8BC2),
                        bevelBottomRight = Color(0xFF0E0817)
                    )
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                for (i in 1..hud.maxHp) {
                    val filled = i <= hud.hp
                    val heartDesc = if (filled) "Heart full" else "Heart empty"
                    PixelHudIcon(
                        kind = if (filled) PixelHudIconKind.HEART_FULL else PixelHudIconKind.HEART_EMPTY,
                        tint = if (filled) DragonCrimson else Color(0xFF5D4C73),
                        modifier = Modifier
                            .size(16.dp)
                            .semantics { contentDescription = heartDesc }
                    )
                }
            }
        }

        // Top-Right Group: Coin Counter + Compact Run Timer ("00:00") neatly aligned
        Column(
            modifier = Modifier
                .pixelBevelBlock(
                    faceColor = Color(0xCC181026),
                    outerBorder = DragonObsidian,
                    innerBorder = Color(0xFF7B689E),
                    bevelTopLeft = Color(0xFF9E8BC2),
                    bevelBottomRight = Color(0xFF0E0817)
                )
                .padding(horizontal = 11.dp, vertical = 7.dp)
                .testTag("top_right_hud"),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PixelHudIcon(
                    kind = PixelHudIconKind.COIN,
                    tint = DragonGold,
                    modifier = Modifier
                        .size(14.dp)
                        .semantics { contentDescription = "Coins" }
                )
                Text(
                    text = "${hud.gems}/${hud.totalGems}",
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.sp),
                    color = DragonIvory
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PixelHudIcon(
                    kind = PixelHudIconKind.CLOCK,
                    tint = Color(0xFFB5A7D6),
                    modifier = Modifier
                        .size(13.dp)
                        .semantics { contentDescription = "Timer" }
                )
                Text(
                    text = formatRunTimer(hud.elapsedSeconds),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.sp),
                    color = DragonGold
                )
            }
        }
    }
}

@Composable
private fun PauseMenuOverlay(
    soundEnabled: Boolean,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onToggleSound: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(248.dp)
            .pixelBevelBlock(
                faceColor = Color(0xF5181026),
                outerBorder = DragonObsidian,
                innerBorder = DragonGold,
                bevelTopLeft = Color(0xFFFFF59D),
                bevelBottomRight = Color(0xFF5C3D00)
            )
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .testTag("pause_menu_overlay"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.pause_title),
            style = MaterialTheme.typography.titleMedium,
            color = DragonGold
        )
        Spacer(modifier = Modifier.height(2.dp))

        PauseMenuButton(
            label = stringResource(R.string.action_resume),
            faceColor = DragonGold,
            textColor = DragonObsidian,
            bevelLight = Color(0xFFFFF59D),
            bevelDark = Color(0xFF8C5A00),
            innerBorder = DragonIvory,
            tag = "resume_button",
            onClick = onResume
        )

        PauseMenuButton(
            label = stringResource(R.string.action_restart),
            faceColor = Color(0xFF2D2045),
            textColor = DragonIvory,
            bevelLight = Color(0xFF9E8BC2),
            bevelDark = Color(0xFF0E0817),
            innerBorder = Color(0xFF9C89C2),
            tag = "restart_button",
            onClick = onRestart
        )

        PauseMenuButton(
            label = if (soundEnabled) stringResource(R.string.sound_on) else stringResource(R.string.sound_off),
            icon = if (soundEnabled) PixelHudIconKind.SOUND_ON else PixelHudIconKind.SOUND_OFF,
            iconTint = if (soundEnabled) DragonGold else DragonCrimson,
            faceColor = Color(0xFF221833),
            textColor = DragonIvory,
            bevelLight = Color(0xFF9E8BC2),
            bevelDark = Color(0xFF0E0817),
            innerBorder = if (soundEnabled) DragonAmber else Color(0xFF7B689E),
            tag = "sound_toggle_button",
            onClick = onToggleSound
        )
    }
}

@Composable
private fun PauseMenuButton(
    label: String,
    faceColor: Color,
    textColor: Color,
    bevelLight: Color,
    bevelDark: Color,
    innerBorder: Color,
    tag: String,
    onClick: () -> Unit,
    icon: PixelHudIconKind? = null,
    iconTint: Color = textColor
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .pixelBevelBlock(
                faceColor = faceColor,
                outerBorder = DragonObsidian,
                innerBorder = innerBorder,
                bevelTopLeft = bevelLight,
                bevelBottomRight = bevelDark
            )
            .clickable(onClick = onClick)
            .testTag(tag),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            PixelHudIcon(
                kind = icon,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
            color = textColor
        )
    }
}

@Composable
private fun EndBannerOverlay(
    cleared: Boolean,
    hud: HudSnapshot,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = if (cleared) DragonGold else DragonCrimson
    val bevelLight = if (cleared) Color(0xFFFFF59D) else Color(0xFFFF8A80)
    val bevelDark = if (cleared) Color(0xFF8C5A00) else Color(0xFF5C0A0C)

    Column(
        modifier = modifier
            .pixelBevelBlock(
                faceColor = Color(0xF0181026),
                outerBorder = DragonObsidian,
                innerBorder = accentColor,
                bevelTopLeft = bevelLight,
                bevelBottomRight = bevelDark
            )
            .padding(horizontal = 32.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (cleared) stringResource(R.string.banner_victory) else stringResource(R.string.banner_defeat),
            style = MaterialTheme.typography.titleMedium,
            color = accentColor
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "COINS ${hud.gems}/${hud.totalGems}  •  TIME ${formatRunTimer(hud.elapsedSeconds)}",
            style = MaterialTheme.typography.labelSmall,
            color = DragonIvory
        )
        Spacer(modifier = Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .height(48.dp)
                .pixelBevelBlock(
                    faceColor = accentColor,
                    outerBorder = DragonObsidian,
                    innerBorder = DragonIvory,
                    bevelTopLeft = bevelLight,
                    bevelBottomRight = bevelDark
                )
                .clickable(onClick = onRetry)
                .padding(horizontal = 24.dp)
                .testTag("retry_button"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.action_retry),
                style = MaterialTheme.typography.labelLarge,
                color = if (cleared) DragonObsidian else DragonIvory
            )
        }
    }
}

internal fun formatRunTimer(totalSeconds: Int): String {
    val clamped = totalSeconds.coerceAtLeast(0)
    val mins = (clamped / 60).coerceAtMost(99)
    val secs = clamped % 60
    return "%02d:%02d".format(mins, secs)
}
