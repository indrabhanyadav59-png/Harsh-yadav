package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.audio.IndraAudioEngine
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.CyberBlue
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyPanel
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletSkill

@Composable
fun AnimeAtmosphericOverlay(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "atmos")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drift"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Upper atmospheric glow & bottom tactical readability vignette (keeps center 3D scene open)
        drawRect(
            brush = Brush.verticalGradient(
                0.0f to Color(0x66040712),
                0.28f to Color.Transparent,
                0.68f to Color.Transparent,
                1.0f to Color(0xCC040712)
            )
        )

        // Subtle animated diagonal anime wind streaks
        for (i in 0 until 10) {
            val phase = (drift + i * 0.13f) % 1f
            val startX = -0.15f * w + phase * (w * 1.35f)
            val startY = h * (0.12f + (i % 5) * 0.16f)
            val streakLen = w * (0.08f + (i % 3) * 0.04f)
            drawLine(
                color = if (i % 2 == 0) ElectricCyan.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.12f),
                start = Offset(startX, startY),
                end = Offset(startX + streakLen, startY - streakLen * 0.14f),
                strokeWidth = 2.2f,
                cap = StrokeCap.Round
            )
        }

        // Floating glowing cyan/gold/violet spirit particles
        for (i in 0 until 18) {
            val pPhase = (drift * (1f + (i % 3) * 0.35f) + i * 0.17f) % 1f
            val px = ((i * 97f + pPhase * 240f) % 1000f) / 1000f * w
            val py = h * (1f - pPhase)
            val radius = 2.5f + (i % 4) * 1.8f
            val particleColor = when (i % 3) {
                0 -> ElectricCyan.copy(alpha = 0.45f)
                1 -> GoldCoin.copy(alpha = 0.40f)
                else -> VioletSkill.copy(alpha = 0.38f)
            }
            drawCircle(color = particleColor, radius = radius, center = Offset(px, py))
        }
    }
}

@Composable
fun IndraGameLogo(
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val pulseTransition = rememberInfiniteTransition(label = "logoPulse")
    val glowAlpha by pulseTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(if (compact) 34.dp else 44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(listOf(ElectricCyan, VioletSkill, CrimsonAccent))
                    )
                    .border(1.5.dp, GoldCoin.copy(alpha = glowAlpha), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(if (compact) 22.dp else 28.dp)
                )
            }
            Column {
                Text(
                    text = "INDRA",
                    style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "ANIME LIFE RP",
                    style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
                    color = ElectricCyan.copy(alpha = glowAlpha),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
            }
        }
        Row(
            modifier = Modifier
                .background(Color(0x99060A16), RoundedCornerShape(8.dp))
                .border(1.dp, ElectricCyan.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Public, contentDescription = null, tint = GoldCoin, modifier = Modifier.size(13.dp))
            Text(
                text = "OPEN WORLD  •  MULTIPLAYER  •  AI POWERED",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}

@Composable
fun AudioMuteHudButton(
    audioOn: Boolean,
    tag: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NavyPanel.copy(alpha = 0.85f),
        shape = CircleShape,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .border(1.dp, ElectricCyan.copy(alpha = 0.5f), CircleShape)
            .testTag(tag)
    ) {
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (audioOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                contentDescription = if (audioOn) "Mute Audio" else "Unmute Audio",
                tint = if (audioOn) ElectricCyan else TextSecondary
            )
        }
    }
}

@Composable
fun GameLoadingOverlay(
    loadState: StartupLoadState,
    audioOn: Boolean,
    onToggleAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (loadState.progressPercent / 100f).coerceIn(0f, 1f),
        animationSpec = tween(240),
        label = "loadProgress"
    )
    val pulseScale by rememberInfiniteTransition(label = "logoScale").animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Top Bar: Logo + Audio Mute & Network Badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            IndraGameLogo(
                compact = false,
                modifier = Modifier.scale(pulseScale)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = NavyPanel.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.border(
                        1.dp,
                        if (loadState.isOnline) ElectricCyan.copy(alpha = 0.45f) else GoldCoin.copy(alpha = 0.5f),
                        RoundedCornerShape(10.dp)
                    )
                ) {
                    Text(
                        text = if (loadState.isOnline) "SERVER ONLINE" else "OFFLINE / LOCAL READY",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (loadState.isOnline) ElectricCyan else GoldCoin,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                AudioMuteHudButton(
                    audioOn = audioOn,
                    tag = "loading_mute_button",
                    onToggle = onToggleAudio
                )
            }
        }

        // Bottom Tactical Loading Progress Strip (NO card box — direct HUD strip over 3D world)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = loadState.statusMessage,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    AnimatedContent(
                        targetState = StartupTips.tips[loadState.tipIndex % StartupTips.tips.size],
                        label = "tipAnim"
                    ) { tip ->
                        Text(
                            text = tip,
                            style = MaterialTheme.typography.bodySmall,
                            color = ElectricCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = "${loadState.progressPercent}%",
                    style = MaterialTheme.typography.headlineSmall,
                    color = GoldCoin,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.testTag("loading_percentage_text")
                )
            }

            // Glowing AAA Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xBB070B18))
                    .border(1.dp, ElectricCyan.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress.coerceAtLeast(0.02f))
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(CyberBlue, ElectricCyan, VioletSkill, GoldCoin)
                            )
                        )
                )
            }
        }
    }
}

@Composable
fun CinematicLoginOverlay(
    audioOn: Boolean,
    isOnline: Boolean,
    onToggleAudio: () -> Unit,
    onAuthenticated: (AuthProviderKind) -> Unit,
    onEnterAsGuest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var isBusy by remember { mutableStateOf(false) }
    var busyProvider by remember { mutableStateOf<AuthProviderKind?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lastFailedProvider by remember { mutableStateOf<AuthProviderKind?>(null) }
    var showFacebookConfigDialog by remember { mutableStateOf(false) }

    val fbConfigured = remember { FacebookAuthBridge.isConfigured() }

    val startGoogleLogin = {
        IndraAudioEngine.playUiTap()
        errorMessage = null
        lastFailedProvider = null
        isBusy = true
        busyProvider = AuthProviderKind.GOOGLE
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isBusy = false
                busyProvider = null
                onAuthenticated(AuthProviderKind.GOOGLE)
            },
            onAuthError = { msg ->
                isBusy = false
                busyProvider = null
                lastFailedProvider = AuthProviderKind.GOOGLE
                errorMessage = msg
            },
            scope = scope,
            onAuthCancelled = {
                isBusy = false
                busyProvider = null
            }
        )
    }

    val startFacebookLogin = {
        IndraAudioEngine.playUiTap()
        errorMessage = null
        lastFailedProvider = null
        if (!fbConfigured) {
            showFacebookConfigDialog = true
        } else {
            isBusy = true
            busyProvider = AuthProviderKind.FACEBOOK
            FacebookAuthBridge.signInWithFacebook(
                context = context,
                scope = scope,
                onAuthSuccess = {
                    isBusy = false
                    busyProvider = null
                    onAuthenticated(AuthProviderKind.FACEBOOK)
                },
                onAuthCancelled = {
                    isBusy = false
                    busyProvider = null
                },
                onAuthError = { msg ->
                    isBusy = false
                    busyProvider = null
                    lastFailedProvider = AuthProviderKind.FACEBOOK
                    errorMessage = msg
                },
                onNotConfigured = {
                    isBusy = false
                    busyProvider = null
                    showFacebookConfigDialog = true
                }
            )
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    0.0f to Color.Transparent,
                    0.48f to Color.Transparent,
                    0.72f to Color(0x99060916),
                    1.0f to Color(0xDD040712)
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        val compactHeight = maxHeight < 420.dp

        // Top-left Title Crest & World Tags (leaving left/center open for 3D anime hero, pet & supercar)
        Column(
            modifier = Modifier.align(Alignment.TopStart),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IndraGameLogo(compact = compactHeight)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TacticalBadge("3D OPEN WORLD", ElectricCyan)
                TacticalBadge("CAR KEY & PETS", GoldCoin)
                TacticalBadge("AI NPCs", VioletSkill)
            }
        }

        // Top-right Audio Mute & Network Indicator
        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = NavyPanel.copy(alpha = 0.85f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.border(
                    1.dp,
                    if (isOnline) ElectricCyan.copy(alpha = 0.45f) else GoldCoin.copy(alpha = 0.55f),
                    RoundedCornerShape(10.dp)
                )
            ) {
                Text(
                    text = if (isOnline) "ASIA • MULTIPLAYER READY" else "OFFLINE • GUEST READY",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isOnline) ElectricCyan else GoldCoin,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            AudioMuteHudButton(
                audioOn = audioOn,
                tag = "login_mute_button",
                onToggle = onToggleAudio
            )
        }

        // Bottom-left Subtle Cinematic Caption over 3D Hero
        Surface(
            color = Color(0x99050914),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .border(1.dp, ElectricCyan.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
        ) {
            Text(
                text = "SAKURA TORII PLAZA  •  DRAGON RIDGE  •  LIVE 3D ENGINE",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // Right-side Full-Screen AAA Login Action Rail (NO website card!)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .widthIn(min = 280.dp, max = 360.dp),
            verticalArrangement = Arrangement.spacedBy(if (compactHeight) 8.dp else 10.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "SELECT ACCOUNT TO ENTER WORLD",
                style = MaterialTheme.typography.labelLarge,
                color = ElectricCyan,
                letterSpacing = 1.5.sp
            )

            // Auth Error Strip with Retry + Guest fallback
            if (errorMessage != null) {
                Surface(
                    color = Color(0xE6260B18),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DangerRed.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = {
                                    if (lastFailedProvider == AuthProviderKind.FACEBOOK) startFacebookLogin()
                                    else startGoogleLogin()
                                },
                                modifier = Modifier.testTag("auth_retry_button")
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(15.dp))
                                Text(" Retry", color = ElectricCyan, style = MaterialTheme.typography.labelLarge)
                            }
                            TextButton(
                                onClick = {
                                    IndraAudioEngine.playLoginSuccessChime()
                                    onEnterAsGuest()
                                },
                                modifier = Modifier.testTag("auth_error_guest_button")
                            ) {
                                Text("Play as Guest Instead", color = GoldCoin, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            // 1. CONTINUE WITH GOOGLE
            AaaLoginButton(
                label = "CONTINUE WITH GOOGLE",
                subtitle = "Cloud Sync • Multiplayer • Friends",
                icon = Icons.Filled.AccountCircle,
                gradientColors = listOf(CrimsonAccent, Color(0xFF991B1B)),
                borderColor = ElectricCyan.copy(alpha = 0.75f),
                loading = isBusy && busyProvider == AuthProviderKind.GOOGLE,
                enabled = !isBusy,
                tag = "google_sign_in_button",
                onClick = startGoogleLogin
            )

            // 2. CONTINUE WITH FACEBOOK
            AaaLoginButton(
                label = "CONTINUE WITH FACEBOOK",
                subtitle = if (fbConfigured) "Official Facebook OAuth" else "App Credentials Not Configured",
                badgeText = if (fbConfigured) null else "SETUP NEEDED",
                icon = Icons.Filled.Shield,
                gradientColors = listOf(Color(0xFF1D4ED8), Color(0xFF1E3A8A)),
                borderColor = CyberBlue.copy(alpha = 0.85f),
                loading = isBusy && busyProvider == AuthProviderKind.FACEBOOK,
                enabled = !isBusy,
                tag = "facebook_sign_in_button",
                onClick = startFacebookLogin
            )

            // 3. PLAY AS GUEST
            AaaLoginButton(
                label = "PLAY AS GUEST",
                subtitle = "Instant 3D World • Vehicles • Pets • NPCs",
                badgeText = "TEMP ID",
                icon = Icons.Filled.PersonOutline,
                gradientColors = listOf(Color(0xFF2E1065), Color(0xFF0F172A)),
                borderColor = VioletSkill.copy(alpha = 0.85f),
                loading = false,
                enabled = !isBusy,
                tag = "guest_play_button",
                onClick = {
                    IndraAudioEngine.playLoginSuccessChime()
                    onEnterAsGuest()
                }
            )

            Text(
                text = "Guest progress is temporary on this device and may be lost if app data is deleted. Link Google or Facebook in the Lobby anytime.",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showFacebookConfigDialog) {
        AlertDialog(
            onDismissRequest = { showFacebookConfigDialog = false },
            containerColor = NavyCard,
            titleContentColor = Color.White,
            textContentColor = TextSecondary,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = ElectricCyan)
                    Text("Facebook OAuth Configuration Needed")
                }
            },
            text = {
                Text(
                    "Facebook Login uses the official Facebook SDK + Firebase Auth, but FACEBOOK_APP_ID and FACEBOOK_CLIENT_TOKEN are still placeholders.\n\n" +
                        "To activate Facebook Login:\n" +
                        "1. Add FACEBOOK_APP_ID and FACEBOOK_CLIENT_TOKEN in the AI Studio Secrets panel.\n" +
                        "2. Enable Facebook Sign-In in Firebase Authentication Console.\n\n" +
                        "You can continue right now using Google Login or Play as Guest."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFacebookConfigDialog = false
                        startGoogleLogin()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent)
                ) {
                    Text("Continue with Google")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = {
                        showFacebookConfigDialog = false
                        IndraAudioEngine.playLoginSuccessChime()
                        onEnterAsGuest()
                    }) {
                        Text("Play as Guest", color = GoldCoin)
                    }
                    TextButton(onClick = { showFacebookConfigDialog = false }) {
                        Text("Close", color = TextSecondary)
                    }
                }
            }
        )
    }
}

@Composable
private fun TacticalBadge(text: String, accent: Color) {
    Surface(
        color = Color(0xAA080D1D),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.border(1.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun AaaLoginButton(
    label: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    borderColor: Color,
    loading: Boolean,
    enabled: Boolean,
    tag: String,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(gradientColors))
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled) { onClick() }
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (badgeText != null) {
                Surface(
                    color = ObsidianBg.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.border(1.dp, GoldCoin.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldCoin,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
