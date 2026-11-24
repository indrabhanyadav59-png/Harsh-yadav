package com.example.ui

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.BuildConfig
import com.example.R
import com.example.audio.IndraAudioEngine
import com.example.data.model.GameCatalog
import com.example.data.repository.IndraAiRepository
import com.example.game.engine.NpcAiSystem
import com.example.game.engine.WorldMapData
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

enum class StartupStage {
    LOADING,
    LOGIN,
    LOBBY,
    IN_GAME
}

enum class AuthProviderKind {
    GOOGLE,
    FACEBOOK,
    GUEST
}

sealed interface AuthSession {
    data object Unauthenticated : AuthSession
    data class Authenticated(
        val uid: String,
        val displayName: String?,
        val provider: AuthProviderKind
    ) : AuthSession

    data class Guest(
        val guestUid: String,
        val displayName: String,
        val createdAtMs: Long = System.currentTimeMillis()
    ) : AuthSession
}

data class StartupLoadState(
    val progressPercent: Int = 0,
    val statusMessage: String = "Loading Open World...",
    val tipIndex: Int = 0,
    val isOnline: Boolean = true
)

object StartupTips {
    val tips = listOf(
        "Tip: Lock your supercar with the Car Key to block rivals from hijacking it.",
        "Tip: Swipe across the 3D Lobby to inspect your anime hero, pet, and supercar.",
        "Tip: Open the World Map to set GPS routes or fast-travel across all 8 open-world districts.",
        "Tip: Offline AI Proxy keeps patrolling when you step away without spending your coins.",
        "Tip: Equip your Kitsune Fox or Cyber Dragon to unleash elemental Pet Abilities in battle."
    )
}

object StartupInitializer {
    suspend fun runRealInitialization(
        context: Context,
        credentialManager: CredentialManager,
        surfaceReadySignal: CompletableDeferred<Unit>,
        onProgress: (StartupLoadState) -> Unit,
        onComplete: (authenticatedNow: Boolean) -> Unit
    ) {
        val online = isNetworkConnected(context)

        // 1. Open World Terrain, Districts & Colliders
        onProgress(StartupLoadState(10, "Loading Open World...", 0, online))
        val (districtCount, buildingCount) = withContext(Dispatchers.Default) {
            GameCatalog.districts.size to WorldMapData.buildings.size
        }
        Log.d("StartupInit", "Loaded $districtCount districts & $buildingCount buildings")
        onProgress(StartupLoadState(22, "Loading Open World... ($districtCount Districts Ready)", 0, online))

        // 2. OpenGL ES 3.0 Shaders, Meshes & AI NPC Spawns
        onProgress(StartupLoadState(30, "Initializing Anime Characters...", 1, online))
        val initialNpcs = withContext(Dispatchers.Default) {
            NpcAiSystem.createInitialNpcs().size
        }
        withTimeoutOrNull(1800L) {
            surfaceReadySignal.await()
        }
        onProgress(StartupLoadState(46, "Initializing Anime Characters ($initialNpcs AI Entities)...", 1, online))

        // 3. Audio Synth & Effects Pipeline
        onProgress(StartupLoadState(56, "Preparing Audio & Effects...", 2, online))
        IndraAudioEngine.startBgmIfActive()
        delay(120L)

        // 4. AI World & App Check Warm-up
        onProgress(StartupLoadState(70, "Connecting to AI World...", 3, online))
        withContext(Dispatchers.Default) {
            try {
                IndraAiRepository()
            } catch (e: Exception) {
                Log.w("StartupInit", "AI warm-up deferred: ${e.message}")
            }
        }

        // 5. Multiplayer & Provider Inspection
        onProgress(StartupLoadState(84, "Loading Multiplayer...", 4, online))
        val fbReady = FacebookAuthBridge.isConfigured()
        Log.d("StartupInit", "Multiplayer online=$online, facebookConfigured=$fbReady")

        // 6. Synchronizing Existing Player Session via Credential Manager
        onProgress(StartupLoadState(92, "Synchronizing Player Data...", 0, online))
        val signedIn = attemptAutoSignInSuspend(context, credentialManager)
        onProgress(StartupLoadState(100, "World Synchronized! Ready to Launch.", 0, online))
        delay(160L)
        onComplete(signedIn)
    }

    fun isNetworkConnected(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val net = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(net) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }
}

suspend fun attemptAutoSignInSuspend(
    context: Context,
    credentialManager: CredentialManager
): Boolean {
    if (Firebase.auth.currentUser != null) return true
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        return false
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    return try {
        val cred = credentialManager.getCredential(context, request).credential
        if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
            Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
            true
        } else {
            false
        }
    } catch (_: Exception) {
        false
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val activity = context as? Activity
                ?: throw IllegalStateException("Activity context required for Google Sign-In.")
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                IndraAudioEngine.playLoginSuccessChime()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected Google credential response.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Google authentication failed. You can retry or play as Guest.")
        }
    }
}

object FacebookAuthBridge {
    private val callbackManager: CallbackManager by lazy { CallbackManager.Factory.create() }
    @Volatile private var sdkInitialized = false

    fun isConfigured(): Boolean {
        val appId = BuildConfig.FACEBOOK_APP_ID.trim()
        val clientToken = BuildConfig.FACEBOOK_CLIENT_TOKEN.trim()
        return appId.isNotEmpty() &&
            clientToken.isNotEmpty() &&
            !appId.startsWith("YOUR_") &&
            !clientToken.startsWith("YOUR_")
    }

    fun signInWithFacebook(
        context: Context,
        scope: CoroutineScope,
        onAuthSuccess: () -> Unit,
        onAuthCancelled: () -> Unit,
        onAuthError: (String) -> Unit,
        onNotConfigured: () -> Unit
    ) {
        if (!isConfigured()) {
            onNotConfigured()
            return
        }
        val registryOwner = context as? ActivityResultRegistryOwner
        if (registryOwner == null) {
            onAuthError("ActivityResultRegistryOwner required for Facebook Login.")
            return
        }
        try {
            if (!sdkInitialized || !FacebookSdk.isFullyInitialized()) {
                FacebookSdk.setApplicationId(BuildConfig.FACEBOOK_APP_ID.trim())
                FacebookSdk.setClientToken(BuildConfig.FACEBOOK_CLIENT_TOKEN.trim())
                FacebookSdk.setAutoInitEnabled(true)
                FacebookSdk.setAutoLogAppEventsEnabled(false)
                FacebookSdk.setAdvertiserIDCollectionEnabled(false)
                @Suppress("DEPRECATION")
                FacebookSdk.sdkInitialize(context.applicationContext)
                FacebookSdk.fullyInitialize()
                sdkInitialized = FacebookSdk.isFullyInitialized()
            }
            val loginManager = LoginManager.getInstance()
            loginManager.registerCallback(
                callbackManager,
                object : FacebookCallback<LoginResult> {
                    override fun onSuccess(result: LoginResult) {
                        scope.launch {
                            try {
                                val credential = FacebookAuthProvider.getCredential(result.accessToken.token)
                                Firebase.auth.signInWithCredential(credential).await()
                                IndraAudioEngine.playLoginSuccessChime()
                                onAuthSuccess()
                            } catch (e: Exception) {
                                Log.e("Auth", "Firebase Facebook credential exchange failed", e)
                                onAuthError(e.localizedMessage ?: "Facebook Firebase authentication failed.")
                            }
                        }
                    }

                    override fun onCancel() {
                        Log.w("Auth", "Facebook Sign-In cancelled by user")
                        onAuthCancelled()
                    }

                    override fun onError(error: FacebookException) {
                        Log.e("Auth", "Facebook Login error", error)
                        onAuthError(error.localizedMessage ?: "Facebook OAuth error.")
                    }
                }
            )
            loginManager.logInWithReadPermissions(
                registryOwner,
                callbackManager,
                listOf("email", "public_profile")
            )
        } catch (e: Exception) {
            Log.e("Auth", "Failed to start Facebook Login", e)
            onAuthError(e.localizedMessage ?: "Facebook SDK unavailable.")
        }
    }

    fun safeLogOut() {
        if (sdkInitialized && FacebookSdk.isFullyInitialized()) {
            try {
                LoginManager.getInstance().logOut()
            } catch (_: Exception) {
            }
        }
    }
}

fun signOutUser(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                FacebookAuthBridge.safeLogOut()
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}
