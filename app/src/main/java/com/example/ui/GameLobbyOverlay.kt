package com.example.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.audio.IndraAudioEngine
import com.example.data.model.HeroAppearanceCatalog
import com.example.data.model.PlayerProfile
import com.example.data.model.TimeOfDayMode
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.CyberBlue
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldHp
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyPanel
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletSkill

@Composable
fun GameLobbyOverlay(
    viewModel: GameViewModel,
    profile: PlayerProfile?,
    authSession: AuthSession,
    audioOn: Boolean,
    onToggleAudio: () -> Unit,
    onStartGame: () -> Unit,
    onLinkSuccess: (AuthProviderKind) -> Unit,
    onSignOutOrLeave: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLinkDialog by remember { mutableStateOf(false) }

    val pulseScale by rememberInfiniteTransition(label = "startPulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "pulse"
    )

    val isGuest = authSession is AuthSession.Guest
    val providerLabel = when (authSession) {
        is AuthSession.Authenticated -> authSession.provider.name
        is AuthSession.Guest -> "GUEST"
        AuthSession.Unauthenticated -> "OFFLINE"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // --- TOP LOBBY BAR (Profile, Currencies, Account Status, Mute, Sign Out) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Player Profile Chip
            Surface(
                color = NavyPanel.copy(alpha = 0.88f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .border(1.dp, ElectricCyan.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                    .clickable { viewModel.openPanel(GamePanel.CHARACTER) }
                    .testTag("lobby_profile_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val activeOutfit = HeroAppearanceCatalog.resolveOutfit(profile?.outfitStyle)
                    val heroAvatarRes = if (activeOutfit.id == "crimson_blox") {
                        R.drawable.char_crimson_blox
                    } else {
                        R.drawable.char_main_portrait
                    }
                    Image(
                        painter = painterResource(id = heroAvatarRes),
                        contentDescription = activeOutfit.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.4.dp, GoldCoin, RoundedCornerShape(10.dp))
                    )
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = profile?.username ?: "ShinobiHero",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = if (isGuest) GoldCoin.copy(alpha = 0.2f) else EmeraldHp.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(5.dp)
                            ) {
                                Text(
                                    text = providerLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isGuest) GoldCoin else EmeraldHp,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "Lv.${profile?.level ?: 52}  •  ${profile?.rankTitle ?: "Grand Shinobi"}  •  Guild: ${profile?.guild ?: "SHINOBI"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan
                        )
                    }
                }
            }

            // Right Controls: Link Account (if guest), Coins/Diamonds, Mute, Sign Out
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isGuest) {
                    Surface(
                        color = Color(0xCC2E1065),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .border(1.2.dp, GoldCoin, RoundedCornerShape(12.dp))
                            .clickable {
                                IndraAudioEngine.playUiTap()
                                showLinkDialog = true
                            }
                            .testTag("lobby_link_account_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Filled.Link, contentDescription = "Link Guest Account", tint = GoldCoin, modifier = Modifier.size(16.dp))
                            Text("LINK ACCOUNT", style = MaterialTheme.typography.labelMedium, color = GoldCoin, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Surface(
                    color = NavyPanel.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(1.dp, GoldCoin.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${profile?.coins ?: 1250} C", color = GoldCoin, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("${profile?.diamonds ?: 540} D", color = ElectricCyan, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }

                AudioMuteHudButton(
                    audioOn = audioOn,
                    tag = "lobby_mute_button",
                    onToggle = onToggleAudio
                )

                Surface(
                    color = NavyPanel.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .border(1.dp, DangerRed.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                        .testTag("lobby_sign_out_button")
                ) {
                    IconButton(onClick = {
                        IndraAudioEngine.playUiTap()
                        onSignOutOrLeave()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = if (isGuest) "Leave Guest Session" else "Sign Out",
                            tint = DangerRed
                        )
                    }
                }
            }
        }

        // --- LEFT LOBBY SHOWCASE RACK (Live 3D Preview Switchers) ---
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .widthIn(max = 240.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "3D SHOWCASE (DRAG TO ROTATE)",
                style = MaterialTheme.typography.labelSmall,
                color = ElectricCyan,
                letterSpacing = 1.sp
            )
            LobbyShowcaseChip(
                icon = Icons.Filled.Person,
                title = "Hero Outfit",
                value = profile?.outfitStyle ?: "Shadow Blaze",
                accent = CrimsonAccent,
                tag = "lobby_cycle_outfit",
                onClick = { viewModel.cycleLobbyOutfit() }
            )
            LobbyShowcaseChip(
                icon = Icons.Filled.Pets,
                title = "Companion Pet",
                value = viewModel.sim.petType,
                accent = GoldCoin,
                tag = "lobby_cycle_pet",
                onClick = { viewModel.cycleLobbyPet() }
            )
            LobbyShowcaseChip(
                icon = Icons.Filled.DirectionsCar,
                title = "Showcase Ride",
                value = viewModel.sim.showcaseVehicleModel,
                accent = ElectricCyan,
                tag = "lobby_cycle_vehicle",
                onClick = { viewModel.cycleLobbyVehicle() }
            )
            LobbyShowcaseChip(
                icon = Icons.Filled.WbSunny,
                title = "Time Lighting",
                value = viewModel.sim.timeOfDay.label,
                accent = VioletSkill,
                tag = "lobby_cycle_time",
                onClick = {
                    val modes = TimeOfDayMode.entries
                    val next = modes[(modes.indexOf(viewModel.sim.timeOfDay) + 1) % modes.size]
                    viewModel.updateGraphicsAndEnv(viewModel.sim.graphicsPreset, next, viewModel.sim.weatherMode)
                }
            )
        }

        // --- BOTTOM-LEFT GUEST WARNING / MODE PILL ---
        if (isGuest) {
            Surface(
                color = Color(0xCC1E1038),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .widthIn(max = 320.dp)
                    .border(1.dp, GoldCoin.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            ) {
                Text(
                    text = "Guest Mode: Unlinked progress is temporary and may be lost if app data is deleted. Tap LINK ACCOUNT to save permanently.",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldCoin,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        } else {
            Surface(
                color = NavyPanel.copy(alpha = 0.8f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            ) {
                Text(
                    text = "OPEN WORLD • SAKURA TORII PLAZA • MULTIPLAYER SYNCED",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        // --- BOTTOM-RIGHT QUICK ACTIONS & GIANT START GAME CTA ---
        Column(
            modifier = Modifier.align(Alignment.BottomEnd),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LobbyMiniActionTile(Icons.Filled.Person, "Hero", "lobby_open_character") {
                    viewModel.openPanel(GamePanel.CHARACTER)
                }
                LobbyMiniActionTile(Icons.Filled.Group, "Friends", "lobby_open_friends") {
                    viewModel.openPanel(GamePanel.FRIENDS)
                }
                LobbyMiniActionTile(Icons.Filled.Map, "World Map", "lobby_open_map") {
                    viewModel.openPanel(GamePanel.MAP)
                }
                LobbyMiniActionTile(Icons.Filled.Settings, "Settings", "lobby_open_settings") {
                    viewModel.openPanel(GamePanel.SETTINGS_RANK)
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .scale(pulseScale)
                    .width(248.dp)
                    .height(62.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(listOf(ElectricCyan, CyberBlue, VioletSkill))
                    )
                    .border(2.dp, GoldCoin, RoundedCornerShape(16.dp))
                    .clickable {
                        IndraAudioEngine.playStartGameLaunch()
                        onStartGame()
                    }
                    .testTag("start_game_button")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "START GAME",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                }
            }
        }
    }

    if (showLinkDialog) {
        GuestAccountLinkDialog(
            onDismiss = { showLinkDialog = false },
            onLinkSuccess = { provider ->
                showLinkDialog = false
                onLinkSuccess(provider)
            }
        )
    }
}

@Composable
fun GuestAccountLinkDialog(
    onDismiss: () -> Unit,
    onLinkSuccess: (AuthProviderKind) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var busy by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val fbConfigured = remember { FacebookAuthBridge.isConfigured() }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = NavyCard,
        titleContentColor = Color.White,
        textContentColor = TextSecondary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Link, contentDescription = null, tint = GoldCoin)
                Text("Link Guest Progress")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Unlinked guest progress is temporary and may be lost if app data is cleared or the session cannot be recovered.\n\n" +
                        "Link Google or Facebook now to sync your current coins, XP, pets, vehicles, houses, and missions to the cloud."
                )
                if (errorMsg != null) {
                    Text(errorMsg!!, color = DangerRed, style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    onClick = {
                        errorMsg = null
                        busy = true
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                busy = false
                                onLinkSuccess(AuthProviderKind.GOOGLE)
                            },
                            onAuthError = { msg ->
                                busy = false
                                errorMsg = msg
                            },
                            scope = scope,
                            onAuthCancelled = { busy = false }
                        )
                    },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("link_google_button")
                ) {
                    if (busy) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.AccountCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Link with Google")
                    }
                }
                Button(
                    onClick = {
                        if (!fbConfigured) {
                            errorMsg = "Facebook App Credentials are not configured yet in AI Studio Secrets."
                            return@Button
                        }
                        errorMsg = null
                        busy = true
                        FacebookAuthBridge.signInWithFacebook(
                            context = context,
                            scope = scope,
                            onAuthSuccess = {
                                busy = false
                                onLinkSuccess(AuthProviderKind.FACEBOOK)
                            },
                            onAuthCancelled = { busy = false },
                            onAuthError = { msg ->
                                busy = false
                                errorMsg = msg
                            },
                            onNotConfigured = {
                                busy = false
                                errorMsg = "Facebook App Credentials are not configured yet."
                            }
                        )
                    },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("link_facebook_button")
                ) {
                    Icon(Icons.Filled.Shield, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (fbConfigured) "Link with Facebook" else "Link with Facebook (Not Configured)")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text("Keep Playing as Guest", color = ElectricCyan)
            }
        }
    )
}

@Composable
private fun LobbyShowcaseChip(
    icon: ImageVector,
    title: String,
    value: String,
    accent: Color,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        color = NavyPanel.copy(alpha = 0.85f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text(
                    value,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Filled.Bolt, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun LobbyMiniActionTile(
    icon: ImageVector,
    label: String,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        color = NavyPanel.copy(alpha = 0.88f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .border(1.dp, ElectricCyan.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .clickable {
                IndraAudioEngine.playUiTap()
                onClick()
            }
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(icon, contentDescription = label, tint = ElectricCyan, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White)
        }
    }
}
