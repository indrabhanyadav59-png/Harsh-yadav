package com.example.game.engine

import android.annotation.SuppressLint
import android.opengl.GLSurfaceView
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@SuppressLint("ClickableViewAccessibility")
@Composable
fun IndraWorldSurface(
    simulationState: WorldSimulationState,
    onHudTick: (WorldHudSnapshot) -> Unit,
    onSurfaceReady: () -> Unit = {},
    touchEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentTouchEnabled by rememberUpdatedState(touchEnabled)
    val renderer = remember(simulationState) {
        IndraGlRenderer(simulationState, onHudTick, onSurfaceReady)
    }

    val glView = remember(simulationState, renderer) {
        var lastTouchX = 0f
        var lastTouchY = 0f

        GLSurfaceView(context).apply {
            setEGLContextClientVersion(3)
            preserveEGLContextOnPause = true
            isFocusable = true
            isFocusableInTouchMode = true
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

            setOnKeyListener { _, keyCode, event ->
                val isDown = event.action == KeyEvent.ACTION_DOWN
                val isUp = event.action == KeyEvent.ACTION_UP
                if (!isDown && !isUp) return@setOnKeyListener false
                val mag = if (isDown) 1f else 0f
                when (keyCode) {
                    KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_DPAD_UP -> {
                        simulationState.keyY = mag; true
                    }
                    KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_DPAD_DOWN -> {
                        simulationState.keyY = -mag; true
                    }
                    KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_DPAD_LEFT -> {
                        simulationState.keyX = -mag; true
                    }
                    KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        simulationState.keyX = mag; true
                    }
                    KeyEvent.KEYCODE_SPACE -> {
                        if (isDown) simulationState.triggerJump(); true
                    }
                    KeyEvent.KEYCODE_SHIFT_LEFT -> {
                        simulationState.sprinting = isDown; true
                    }
                    else -> false
                }
            }

            setOnTouchListener { _, event ->
                if (!currentTouchEnabled) return@setOnTouchListener false
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        lastTouchX = event.x
                        lastTouchY = event.y
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.x - lastTouchX
                        val dy = event.y - lastTouchY
                        lastTouchX = event.x
                        lastTouchY = event.y
                        when (simulationState.showcaseMode) {
                            ShowcaseMode.LOBBY -> {
                                simulationState.showcaseDragYawOffset =
                                    (simulationState.showcaseDragYawOffset - dx * 0.25f) % 360f
                            }
                            ShowcaseMode.CHARACTER_PREVIEW -> {
                                simulationState.previewYawOffset =
                                    (simulationState.previewYawOffset + dx * 0.45f) % 360f
                            }
                            ShowcaseMode.NONE -> {
                                val sens = if (simulationState.aiming) {
                                    simulationState.aimSensitivity
                                } else {
                                    simulationState.cameraSensitivity
                                }
                                val nextYaw =
                                    (simulationState.playerRotY - dx * 0.24f * sens + 360f) % 360f
                                simulationState.playerRotY = nextYaw
                                simulationState.cameraYaw = nextYaw
                                simulationState.cameraPitch =
                                    if (simulationState.drivingVehicle != null) 11f else 7f
                            }
                            else -> Unit
                        }
                        true
                    }
                    else -> false
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner, glView, renderer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> glView.onResume()
                Lifecycle.Event.ON_PAUSE -> glView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            glView.queueEvent { renderer.release() }
            glView.onPause()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { glView }
    )
}
