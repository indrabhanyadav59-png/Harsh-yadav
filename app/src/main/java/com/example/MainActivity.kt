package com.example

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.audio.IndraAudioEngine
import com.example.data.repository.GuestProgressSnapshot
import com.example.data.repository.IndraAiRepository
import com.example.data.repository.IndraGameRepository
import com.example.data.repository.LocalHudSettingsStore
import com.example.game.engine.IndraWorldSurface
import com.example.game.engine.ShowcaseMode
import com.example.game.engine.WorldMapData
import com.example.game.engine.WorldSimulationState
import com.example.location.LocationTrackingService
import com.example.security.configureAppCheck
import com.example.ui.AnimeAtmosphericOverlay
import com.example.ui.ArsenalShopPanel
import com.example.ui.AuthProviderKind
import com.example.ui.AuthSession
import com.example.ui.CharacterPanel
import com.example.ui.CinematicLoginOverlay
import com.example.ui.FriendsAndBountyPanel
import com.example.ui.GameHudOverlay
import com.example.ui.GameLoadingOverlay
import com.example.ui.GameLobbyOverlay
import com.example.ui.GamePanel
import com.example.ui.GameViewModel
import com.example.ui.HousePanel
import com.example.ui.HudEditorOverlay
import com.example.ui.MissionsPanel
import com.example.ui.PetsPanel
import com.example.ui.SettingsRankPanel
import com.example.ui.StartupInitializer
import com.example.ui.StartupLoadState
import com.example.ui.StartupStage
import com.example.ui.StartupTips
import com.example.ui.UiState
import com.example.ui.VehiclesPanel
import com.example.ui.WorldMapPanel
import com.example.ui.signOutUser
import com.example.ui.theme.IndraAnimeTheme
import com.example.ui.theme.ObsidianBg
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandatory: Configure App Check with launch intent BEFORE setContent or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            IndraAnimeTheme {
                IndraAppNavigation()
            }
        }
    }
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun IndraAppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val credentialManager = remember { CredentialManager.create(context) }

    val currentUser by auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val audioMuted by IndraAudioEngine.isMuted.collectAsStateWithLifecycle()
    val audioOn = !audioMuted

    var stage by remember { mutableStateOf(StartupStage.LOADING) }
    var loadState by remember { mutableStateOf(StartupLoadState()) }
    var guestSession by remember { mutableStateOf<AuthSession.Guest?>(null) }
    var preferredProvider by remember { mutableStateOf(AuthProviderKind.GOOGLE) }
    var pendingMigration by remember { mutableStateOf<GuestProgressSnapshot?>(null) }

    // Continuous 3D showcase surface prepared off the Main Thread after the initial UI frame commits
    var showcaseSim by remember { mutableStateOf<WorldSimulationState?>(null) }
    val surfaceReadySignal = remember { CompletableDeferred<Unit>() }

    // Pause / resume background music with Activity lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> IndraAudioEngine.onAppForegroundChanged(true)
                Lifecycle.Event.ON_PAUSE -> IndraAudioEngine.onAppForegroundChanged(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Keep ShowcaseMode synchronized with current stage
    LaunchedEffect(stage, showcaseSim) {
        showcaseSim?.showcaseMode = when (stage) {
            StartupStage.LOADING -> ShowcaseMode.SPLASH_LOADING
            StartupStage.LOGIN -> ShowcaseMode.LOGIN
            StartupStage.LOBBY -> ShowcaseMode.LOBBY
            StartupStage.IN_GAME -> ShowcaseMode.NONE
        }
    }

    // Rotate gameplay tips smoothly while loading
    LaunchedEffect(stage) {
        if (stage == StartupStage.LOADING) {
            var idx = 0
            while (stage == StartupStage.LOADING) {
                delay(2000L)
                idx = (idx + 1) % StartupTips.tips.size
                loadState = loadState.copy(tipIndex = idx)
            }
        }
    }

    // Yield first frame so WindowManager's initial SurfaceSyncGroup completes immediately,
    // then generate the procedural world off the UI thread before attaching GLSurfaceView.
    LaunchedEffect(Unit) {
        withFrameNanos { }
        showcaseSim = withContext(Dispatchers.Default) {
            WorldMapData.buildings.size
            WorldSimulationState().apply {
                showcaseMode = when (stage) {
                    StartupStage.LOADING -> ShowcaseMode.SPLASH_LOADING
                    StartupStage.LOGIN -> ShowcaseMode.LOGIN
                    StartupStage.LOBBY -> ShowcaseMode.LOBBY
                    StartupStage.IN_GAME -> ShowcaseMode.NONE
                }
            }
        }

        StartupInitializer.runRealInitialization(
            context = context,
            credentialManager = credentialManager,
            surfaceReadySignal = surfaceReadySignal,
            onProgress = { progress ->
                loadState = progress.copy(tipIndex = loadState.tipIndex)
            },
            onComplete = { authenticatedNow ->
                stage = if (authenticatedNow || auth.currentUser != null) {
                    StartupStage.LOBBY
                } else {
                    StartupStage.LOGIN
                }
            }
        )
    }

    val activeSession: AuthSession = when {
        currentUser != null -> AuthSession.Authenticated(
            uid = currentUser!!.uid,
            displayName = currentUser!!.displayName,
            provider = preferredProvider
        )
        guestSession != null -> guestSession!!
        else -> AuthSession.Unauthenticated
    }

    // Safety guard: if an authenticated user signs out externally while in Lobby/InGame, return to Login
    LaunchedEffect(activeSession) {
        if (stage != StartupStage.LOADING && activeSession is AuthSession.Unauthenticated) {
            stage = StartupStage.LOGIN
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(ObsidianBg)) {
        // 1. Always-live 3D world when not in an isolated per-user IN_GAME/LOBBY session
        val activeShowcaseSim = showcaseSim
        if (stage == StartupStage.LOADING || stage == StartupStage.LOGIN || activeSession is AuthSession.Unauthenticated) {
            if (activeShowcaseSim != null) {
                IndraWorldSurface(
                    simulationState = activeShowcaseSim,
                    onHudTick = {},
                    onSurfaceReady = { surfaceReadySignal.complete(Unit) },
                    modifier = Modifier.fillMaxSize()
                )
            }
            AnimeAtmosphericOverlay()
        }

        when {
            stage == StartupStage.LOADING -> {
                GameLoadingOverlay(
                    loadState = loadState,
                    audioOn = audioOn,
                    onToggleAudio = { IndraAudioEngine.toggleMute() }
                )
            }

            stage == StartupStage.LOGIN || activeSession is AuthSession.Unauthenticated -> {
                CinematicLoginOverlay(
                    audioOn = audioOn,
                    isOnline = loadState.isOnline,
                    onToggleAudio = { IndraAudioEngine.toggleMute() },
                    onAuthenticated = { provider ->
                        preferredProvider = provider
                        guestSession = null
                        stage = StartupStage.LOBBY
                    },
                    onEnterAsGuest = {
                        val tag = UUID.randomUUID().toString().replace("-", "").take(6).uppercase()
                        guestSession = AuthSession.Guest(
                            guestUid = "guest_$tag",
                            displayName = "Guest_$tag"
                        )
                        preferredProvider = AuthProviderKind.GUEST
                        stage = StartupStage.LOBBY
                    }
                )
            }

            else -> {
                val sessionUid = when (activeSession) {
                    is AuthSession.Authenticated -> activeSession.uid
                    is AuthSession.Guest -> activeSession.guestUid
                    AuthSession.Unauthenticated -> ""
                }
                val sessionName = when (activeSession) {
                    is AuthSession.Authenticated -> activeSession.displayName
                    is AuthSession.Guest -> activeSession.displayName
                    AuthSession.Unauthenticated -> null
                }
                val isGuest = activeSession is AuthSession.Guest

                key(sessionUid) {
                    IndraSessionContainer(
                        currentUserId = sessionUid,
                        displayName = sessionName,
                        isGuestMode = isGuest,
                        authSession = activeSession,
                        stage = stage,
                        audioOn = audioOn,
                        pendingMigration = pendingMigration,
                        onMigrationConsumed = { pendingMigration = null },
                        onToggleAudio = { IndraAudioEngine.toggleMute() },
                        onStartGame = { stage = StartupStage.IN_GAME },
                        onBackToLobby = { stage = StartupStage.LOBBY },
                        onLinkAccountWithSnapshot = { provider, snapshot ->
                            preferredProvider = provider
                            pendingMigration = snapshot
                            guestSession = null
                            stage = StartupStage.LOBBY
                        },
                        onSignOutOrLeave = {
                            LocationTrackingService.stopTracking(context)
                            if (isGuest) {
                                guestSession = null
                                stage = StartupStage.LOGIN
                            } else {
                                signOutUser(credentialManager, scope) {
                                    guestSession = null
                                    stage = StartupStage.LOGIN
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun IndraSessionContainer(
    currentUserId: String,
    displayName: String?,
    isGuestMode: Boolean,
    authSession: AuthSession,
    stage: StartupStage,
    audioOn: Boolean,
    pendingMigration: GuestProgressSnapshot?,
    onMigrationConsumed: () -> Unit,
    onToggleAudio: () -> Unit,
    onStartGame: () -> Unit,
    onBackToLobby: () -> Unit,
    onLinkAccountWithSnapshot: (AuthProviderKind, GuestProgressSnapshot?) -> Unit,
    onSignOutOrLeave: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: GameViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                GameViewModel(
                    gameRepo = IndraGameRepository(db),
                    aiRepo = IndraAiRepository(),
                    currentUserId = currentUserId,
                    initialDisplayName = displayName,
                    isGuestMode = isGuestMode,
                    pendingGuestMigration = pendingMigration,
                    localHudStore = LocalHudSettingsStore(app)
                )
            }
        }
    )

    // Wire combat & UI haptic feedback pulses respecting user vibration setting
    DisposableEffect(viewModel, context) {
        viewModel.sim.onHapticPulse = {
            if (viewModel.sim.vibrationEnabled) {
                runCatching {
                    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
                    } else {
                        @Suppress("DEPRECATION")
                        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createOneShot(18L, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(18L)
                    }
                }
            }
        }
        onDispose { viewModel.sim.onHapticPulse = null }
    }

    LaunchedEffect(pendingMigration) {
        if (pendingMigration != null) onMigrationConsumed()
    }

    val hud by viewModel.hudSnapshot.collectAsStateWithLifecycle()
    val activePanel by viewModel.activePanel.collectAsStateWithLifecycle()
    val hudEditorOpen by viewModel.hudEditorOpen.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val discoverableState by viewModel.discoverablePlayersState.collectAsStateWithLifecycle()
    val bountiesState by viewModel.bountiesState.collectAsStateWithLifecycle()

    LaunchedEffect(stage, activePanel) {
        viewModel.sim.showcaseMode = when {
            activePanel == GamePanel.CHARACTER -> ShowcaseMode.CHARACTER_PREVIEW
            stage == StartupStage.LOBBY -> ShowcaseMode.LOBBY
            else -> ShowcaseMode.NONE
        }
        if (stage == StartupStage.IN_GAME) {
            viewModel.onGameplayEntered()
        }
    }

    val profile = (playerState as? UiState.Success)?.data
    val nearbyCount = (discoverableState as? UiState.Success)?.data?.size ?: 0
    val activeBountyTargetingMe = (bountiesState as? UiState.Success)?.data?.firstOrNull {
        it.status == "active" && it.targetUid == currentUserId
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Real 60fps OpenGL ES 3.0 3D Open World (drives both Lobby 3D Stage & Playable World)
        IndraWorldSurface(
            simulationState = viewModel.sim,
            onHudTick = { viewModel.onHudTick(it) },
            touchEnabled = !hudEditorOpen && activePanel == GamePanel.NONE,
            modifier = Modifier.fillMaxSize()
        )

        if (stage == StartupStage.LOBBY) {
            AnimeAtmosphericOverlay()
            GameLobbyOverlay(
                viewModel = viewModel,
                profile = profile,
                authSession = authSession,
                audioOn = audioOn,
                onToggleAudio = onToggleAudio,
                onStartGame = onStartGame,
                onLinkSuccess = { provider ->
                    onLinkAccountWithSnapshot(provider, viewModel.exportGuestProgressSnapshot())
                },
                onSignOutOrLeave = onSignOutOrLeave
            )
        } else if (hudEditorOpen) {
            HudEditorOverlay(
                viewModel = viewModel,
                isGuestMode = isGuestMode
            )
        } else {
            AnimatedVisibility(
                visible = stage == StartupStage.IN_GAME,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                GameHudOverlay(
                    viewModel = viewModel,
                    hud = hud,
                    playerProfile = profile,
                    nearbyPlayersCount = nearbyCount,
                    activeBountyTargetingMe = activeBountyTargetingMe
                )
            }
        }

        // Full Interactive Management & Multiplayer Panels (usable from BOTH Lobby & In-Game!)
        when (activePanel) {
            GamePanel.NONE -> Unit
            GamePanel.CHARACTER -> CharacterPanel(viewModel, profile) { viewModel.openPanel(GamePanel.NONE) }
            GamePanel.PETS -> PetsPanel(viewModel, profile) { viewModel.openPanel(GamePanel.NONE) }
            GamePanel.VEHICLES -> VehiclesPanel(viewModel, profile) { viewModel.openPanel(GamePanel.NONE) }
            GamePanel.HOUSE -> HousePanel(viewModel, profile) { viewModel.openPanel(GamePanel.NONE) }
            GamePanel.FRIENDS -> FriendsAndBountyPanel(viewModel, profile) { viewModel.openPanel(GamePanel.NONE) }
            GamePanel.MISSIONS -> MissionsPanel(viewModel, profile) { viewModel.openPanel(GamePanel.NONE) }
            GamePanel.WEAPONS_SHOP -> ArsenalShopPanel(viewModel, profile) { viewModel.openPanel(GamePanel.NONE) }
            GamePanel.MAP -> WorldMapPanel(viewModel, hud, profile) { viewModel.openPanel(GamePanel.NONE) }
            GamePanel.SETTINGS_RANK -> SettingsRankPanel(
                viewModel = viewModel,
                profile = profile,
                onBackToLobby = onBackToLobby,
                onLinkSuccess = { provider ->
                    onLinkAccountWithSnapshot(provider, viewModel.exportGuestProgressSnapshot())
                },
                onSignOutOrLeave = onSignOutOrLeave,
                onClose = { viewModel.openPanel(GamePanel.NONE) }
            )
        }
    }
}
