package com.example.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.IndraAudioEngine
import com.example.data.model.BountyContract
import com.example.data.model.ChatMessage
import com.example.data.model.Friendship
import com.example.data.model.GameCatalog
import com.example.data.model.GameplaySettingsConfig
import com.example.data.model.GraphicsPreset
import com.example.data.model.HeroAppearanceCatalog
import com.example.data.model.HudLayoutPresets
import com.example.data.model.HudLayoutProfile
import com.example.data.model.MissionProgress
import com.example.data.model.OwnedPet
import com.example.data.model.OwnedProperty
import com.example.data.model.OwnedVehicle
import com.example.data.model.PetCatalog
import com.example.data.model.PetTradeOffer
import com.example.data.model.PlayerHudSettingsBundle
import com.example.data.model.PlayerProfile
import com.example.data.model.PropertyMarketListing
import com.example.data.model.TimeOfDayMode
import com.example.data.model.WeaponSpec
import com.example.data.model.WeaponType
import com.example.data.model.WeatherMode
import com.example.data.repository.GuestProgressSnapshot
import com.example.data.repository.IndraAiRepository
import com.example.data.repository.IndraGameRepository
import com.example.data.repository.LocalHudSettingsStore
import com.example.game.engine.BuildingStyle
import com.example.game.engine.NpcEntity
import com.example.game.engine.NpcRole
import com.example.game.engine.ObjMeshLoader
import com.example.game.engine.WorldBuilding
import com.example.game.engine.WorldHudSnapshot
import com.example.game.engine.WorldMapData
import com.example.game.engine.WorldSimulationState
import com.example.game.world.InteriorType
import com.example.game.world.WorldLocation
import com.example.game.world.WorldLocations
import com.example.location.LocationTrackingService
import com.example.location.LocationTrackingSnapshot
import com.example.location.LocationTrackingState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class GamePanel {
    NONE,
    CHARACTER,
    PETS,
    VEHICLES,
    HOUSE,
    FRIENDS,
    MISSIONS,
    WEAPONS_SHOP,
    MAP,
    SETTINGS_RANK
}

class GameViewModel(
    private val gameRepo: IndraGameRepository,
    private val aiRepo: IndraAiRepository,
    val currentUserId: String,
    initialDisplayName: String?,
    val isGuestMode: Boolean = false,
    private val pendingGuestMigration: GuestProgressSnapshot? = null,
    private val localHudStore: LocalHudSettingsStore? = null
) : ViewModel() {

    val sim = WorldSimulationState()

    private val _hudSnapshot = MutableStateFlow(sim.snapshotHud())
    val hudSnapshot: StateFlow<WorldHudSnapshot> = _hudSnapshot.asStateFlow()

    private val _activePanel = MutableStateFlow(GamePanel.NONE)
    val activePanel: StateFlow<GamePanel> = _activePanel.asStateFlow()

    private val _hudEditorOpen = MutableStateFlow(false)
    val hudEditorOpen: StateFlow<Boolean> = _hudEditorOpen.asStateFlow()

    private val _hudSettingsBundle = MutableStateFlow(
        localHudStore?.load(currentUserId) ?: PlayerHudSettingsBundle.defaultForUser(currentUserId)
    )
    val hudSettingsBundle: StateFlow<PlayerHudSettingsBundle> = _hudSettingsBundle.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    private val _inspectedNpc = MutableStateFlow<NpcEntity?>(null)
    val inspectedNpc: StateFlow<NpcEntity?> = _inspectedNpc.asStateFlow()

    private val _inspectedPlayer = MutableStateFlow<PlayerProfile?>(null)
    val inspectedPlayer: StateFlow<PlayerProfile?> = _inspectedPlayer.asStateFlow()

    private val _chatOpen = MutableStateFlow(false)
    val chatOpen: StateFlow<Boolean> = _chatOpen.asStateFlow()

    val audioEnabled: StateFlow<Boolean> = IndraAudioEngine.isMuted
        .map { !it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, !IndraAudioEngine.isMuted.value)

    private val _aiMissionState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val aiMissionState: StateFlow<UiState<String>> = _aiMissionState.asStateFlow()

    private val _aiTasksCompletedCount = MutableStateFlow(0)
    val aiTasksCompletedCount: StateFlow<Int> = _aiTasksCompletedCount.asStateFlow()

    private val _aiDialogueState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val aiDialogueState: StateFlow<UiState<String>> = _aiDialogueState.asStateFlow()

    private val _syncNotice = MutableStateFlow<String?>(null)
    val syncNotice: StateFlow<String?> = _syncNotice.asStateFlow()

    private val _activeGpsDestination = MutableStateFlow<WorldLocation?>(null)
    val activeGpsDestination: StateFlow<WorldLocation?> = _activeGpsDestination.asStateFlow()

    val locationTrackingState: StateFlow<LocationTrackingSnapshot> = LocationTrackingState.state

    private var initialAiTaskEnsured = false

    // --- Local In-Memory Guest Sandbox Flows (used ONLY when isGuestMode == true) ---
    private val initialGuestCoins = 1250L
    private val initialGuestXp = 420L
    private val initialGuestKills = 5L
    private val guestProfileFlow = MutableStateFlow<UiState<PlayerProfile?>>(
        UiState.Success(createStarterGuestProfile(currentUserId, initialDisplayName))
    )
    private val guestPetsFlow = MutableStateFlow<UiState<List<OwnedPet>>>(
        UiState.Success(createStarterGuestPets(currentUserId))
    )
    private val guestVehiclesFlow = MutableStateFlow<UiState<List<OwnedVehicle>>>(
        UiState.Success(createStarterGuestVehicles(currentUserId))
    )
    private val guestPropertiesFlow = MutableStateFlow<UiState<List<OwnedProperty>>>(
        UiState.Success(createStarterGuestProperties(currentUserId))
    )
    private val guestMissionsFlow = MutableStateFlow<UiState<List<MissionProgress>>>(
        UiState.Success(createStarterGuestMissions(currentUserId))
    )
    private val guestPlayersFlow = MutableStateFlow<UiState<List<PlayerProfile>>>(UiState.Success(emptyList()))
    private val guestFriendshipsFlow = MutableStateFlow<UiState<List<Friendship>>>(UiState.Success(emptyList()))
    private val guestBountiesFlow = MutableStateFlow<UiState<List<BountyContract>>>(UiState.Success(emptyList()))
    private val guestChatFlow = MutableStateFlow<UiState<List<ChatMessage>>>(
        UiState.Success(
            listOf(
                ChatMessage(
                    id = "guest_sys_1",
                    roomId = "global_world",
                    senderUid = "system",
                    senderName = "Indra Guide",
                    text = "Guest Session active. Link Google or Facebook anytime to save progress to cloud multiplayer.",
                    messageType = "alert"
                )
            )
        )
    )
    private val guestPetTradesFlow = MutableStateFlow<UiState<List<PetTradeOffer>>>(UiState.Success(emptyList()))

    private fun <T> Flow<T>.toSafeUiState(label: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing $label", root)
                emit(UiState.Error(root.message ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    val playerState: StateFlow<UiState<PlayerProfile?>> =
        if (isGuestMode) guestProfileFlow else gameRepo.observePlayerProfile(currentUserId).toSafeUiState("player profile")

    val discoverablePlayersState: StateFlow<UiState<List<PlayerProfile>>> =
        if (isGuestMode) guestPlayersFlow else gameRepo.observeDiscoverablePlayers(currentUserId).toSafeUiState("nearby players")

    val petsState: StateFlow<UiState<List<OwnedPet>>> =
        if (isGuestMode) guestPetsFlow else gameRepo.observePets(currentUserId).toSafeUiState("pets")

    val vehiclesState: StateFlow<UiState<List<OwnedVehicle>>> =
        if (isGuestMode) guestVehiclesFlow else gameRepo.observeVehicles(currentUserId).toSafeUiState("vehicles")

    val propertiesState: StateFlow<UiState<List<OwnedProperty>>> =
        if (isGuestMode) guestPropertiesFlow else gameRepo.observeProperties(currentUserId).toSafeUiState("properties")

    val missionsState: StateFlow<UiState<List<MissionProgress>>> =
        if (isGuestMode) guestMissionsFlow else gameRepo.observeMissions(currentUserId).toSafeUiState("missions")

    val friendshipsState: StateFlow<UiState<List<Friendship>>> =
        if (isGuestMode) guestFriendshipsFlow else gameRepo.observeFriendships(currentUserId).toSafeUiState("friendships")

    val bountiesState: StateFlow<UiState<List<BountyContract>>> =
        if (isGuestMode) guestBountiesFlow else gameRepo.observeBounties(currentUserId).toSafeUiState("bounties")

    val chatState: StateFlow<UiState<List<ChatMessage>>> =
        if (isGuestMode) guestChatFlow else gameRepo.observeWorldChat(currentUserId).toSafeUiState("world chat")

    val petTradesState: StateFlow<UiState<List<PetTradeOffer>>> =
        if (isGuestMode) guestPetTradesFlow else gameRepo.observePetTrades(currentUserId).toSafeUiState("pet trades")

    init {
        sim.localPlayerUid = currentUserId
        applyGameplaySettingsToEngine(_hudSettingsBundle.value.gameplay)

        if (!isGuestMode) {
            viewModelScope.launch {
                val res = if (pendingGuestMigration != null) {
                    gameRepo.migrateGuestProgress(currentUserId, initialDisplayName, pendingGuestMigration)
                } else {
                    gameRepo.ensureStarterData(currentUserId, initialDisplayName)
                }
                if (res.isFailure) {
                    _syncNotice.value = res.exceptionOrNull()?.localizedMessage ?: "Cloud sync warning"
                } else if (pendingGuestMigration != null) {
                    showBanner("Account linked! Guest progress synced to cloud.")
                }

                // Load cloud HUD & Control settings (or upload local if none in cloud yet)
                val cloudHud = gameRepo.loadHudSettings(currentUserId).getOrNull()
                if (cloudHud != null) {
                    _hudSettingsBundle.value = cloudHud
                    localHudStore?.save(currentUserId, cloudHud)
                    applyGameplaySettingsToEngine(cloudHud.gameplay)
                    (playerState.value as? UiState.Success)?.data?.let { p ->
                        sim.applyHeroAppearance(
                            outfitRaw = p.outfitStyle,
                            hairRaw = p.hairStyle,
                            accessoryRaw = p.accessoryStyle,
                            accentHex = p.accentColorHex,
                            hairCutRaw = cloudHud.selectedHairCutId,
                            headgearRaw = cloudHud.selectedHeadgearId,
                            weaponSkinRaw = cloudHud.selectedWeaponSkinId,
                            expressionRaw = cloudHud.selectedExpressionId
                        )
                    }
                } else if (localHudStore?.hasSavedLayout(currentUserId) == true) {
                    gameRepo.saveHudSettings(currentUserId, _hudSettingsBundle.value)
                }
            }
        }

        // Apply loaded player profile visuals + HUD style choices into 3D simulation
        viewModelScope.launch {
            playerState.collect { st ->
                if (st is UiState.Success && st.data != null) {
                    val p = st.data
                    val b = _hudSettingsBundle.value
                    sim.applyHeroAppearance(
                        outfitRaw = p.outfitStyle,
                        hairRaw = p.hairStyle,
                        accessoryRaw = p.accessoryStyle,
                        accentHex = p.accentColorHex,
                        hairCutRaw = b.selectedHairCutId,
                        headgearRaw = b.selectedHeadgearId,
                        weaponSkinRaw = b.selectedWeaponSkinId,
                        expressionRaw = b.selectedExpressionId
                    )
                }
            }
        }

        // Sync discoverable online players, accepted teammates & active bounties into 3D sim combat checks
        viewModelScope.launch {
            discoverablePlayersState.collect { st ->
                if (st is UiState.Success) {
                    sim.onlinePlayerTargets = st.data
                }
            }
        }
        viewModelScope.launch {
            friendshipsState.collect { st ->
                if (st is UiState.Success) {
                    sim.protectedTeammateUids = st.data
                        .filter { it.status == "accepted" }
                        .flatMap { it.participantUids }
                        .filter { it != currentUserId }
                        .toSet()
                }
            }
        }
        viewModelScope.launch {
            bountiesState.collect { st ->
                if (st is UiState.Success) {
                    sim.activeBountyTargetUids = st.data
                        .filter { it.status == "active" }
                        .flatMap { it.participantUids }
                        .filter { it != currentUserId }
                        .toSet()
                }
            }
        }

        // Sync owned/equipped 5-pet roster and main follower into 3D simulation + passives engine
        viewModelScope.launch {
            petsState.collect { st ->
                if (st is UiState.Success) {
                    sim.petController.updateRoster(st.data)
                    val activePetId = (playerState.value as? UiState.Success)?.data?.activePetId
                    val activePet = st.data.firstOrNull { it.id == activePetId }
                        ?: st.data.firstOrNull { it.equipped }
                        ?: st.data.firstOrNull()
                    if (activePet != null) {
                        sim.petType = PetCatalog.resolveSpec(activePet.petType).name
                        sim.petEvolutionLevel = activePet.effectiveEvolutionLevel()
                        sim.petCustomTintHex = activePet.customTintHex
                        sim.petAuraStyle = activePet.auraStyle
                    }
                }
            }
        }

        // Sync first unlocked/active vehicle into Lobby 3D showcase
        viewModelScope.launch {
            vehiclesState.collect { st ->
                if (st is UiState.Success && st.data.isNotEmpty()) {
                    val v = st.data.first()
                    sim.showcaseVehicleModel = v.modelId
                    sim.showcaseVehicleCategory = v.category
                    sim.showcaseVehicleColorInt = parseHexColor(v.paintHex, 0xFFFF1E42.toInt())
                }
            }
        }

        // Sync owned property IDs into 3D simulation (for ground plot prompts & overhead badges)
        viewModelScope.launch {
            propertiesState.collect { st ->
                if (st is UiState.Success) {
                    sim.ownedPropertyIds = st.data.map { it.id }.toSet()
                }
            }
        }

        // Wire simulation callbacks
        sim.onNpcDefeated = { npc ->
            viewModelScope.launch {
                val coinGain = if (npc.role == NpcRole.RIVAL_BOT) 45L else 15L
                val xpGain = if (npc.role == NpcRole.RIVAL_BOT) 80L else 25L
                if (isGuestMode) {
                    updateGuestProfile {
                        it.copy(
                            coins = it.coins + coinGain,
                            xp = it.xp + xpGain,
                            kills = it.kills + 1L
                        )
                    }
                } else {
                    gameRepo.awardEconomyAndCombat(currentUserId, coinGain, 0L, xpGain, 1L)
                }
                incrementMissionsByCategory("daily", 1L)
                showBanner("Defeated ${npc.displayName}! +$coinGain Coins, +$xpGain XP (Respawns in 5s)")
            }
        }

        sim.onPlayerDefeated = {
            viewModelScope.launch {
                if (isGuestMode) {
                    updateGuestProfile { it.copy(deaths = it.deaths + 1L) }
                }
            }
        }

        sim.onOnlineTargetHit = { target, dmg, defeated ->
            viewModelScope.launch {
                showBanner("Hit ${target.username} (-${dmg.toInt()} HP)")
                if (defeated) {
                    val activeBounty = (bountiesState.value as? UiState.Success)?.data?.firstOrNull {
                        it.status == "active" && target.userId in it.participantUids
                    }
                    if (activeBounty != null) {
                        val hunterWon = activeBounty.hunterUid == currentUserId
                        resolveBountyDuel(activeBounty, hunterWon = hunterWon)
                    } else if (!isGuestMode) {
                        gameRepo.awardEconomyAndCombat(currentUserId, 25L, 0L, 60L, 1L)
                    }
                }
            }
        }

        sim.onDistrictEntered = { district ->
            viewModelScope.launch {
                showBanner("Entered $district")
                incrementMissionsByCategory("exploration", 1L)
                if (district == "Commercial Region") {
                    incrementMissionsByCategory("driving", 1L)
                }
                syncTransformNow()
            }
        }

        sim.onCombatEventBanner = { msg ->
            showBanner(msg)
        }

        sim.onWildPetAdopted = { wildPet ->
            adoptWildPetFromWorld(wildPet)
        }

        sim.onPetBonusCoins = { bonusCoins ->
            viewModelScope.launch {
                if (isGuestMode) {
                    updateGuestProfile { it.copy(coins = it.coins + bonusCoins) }
                } else {
                    gameRepo.awardEconomyAndCombat(currentUserId, bonusCoins, 0L, 0L, 0L)
                }
                showBanner("Treasure Pet found +$bonusCoins Coins!")
            }
        }

        // Periodic cloud transform save every 18s while active
        if (!isGuestMode) {
            viewModelScope.launch {
                while (true) {
                    delay(18_000L)
                    syncTransformNow()
                }
            }
        } else {
            viewModelScope.launch {
                locationTrackingState.collect { loc ->
                    if (loc.latitude != null && loc.longitude != null) {
                        updateGuestProfile {
                            it.copy(
                                latitude = loc.latitude,
                                longitude = loc.longitude,
                                locationAccuracyMeters = loc.accuracyMeters?.toDouble(),
                                locationTrackingEnabled = loc.isTracking
                            )
                        }
                    }
                }
            }
        }
    }

    fun exportGuestProgressSnapshot(): GuestProgressSnapshot? {
        if (!isGuestMode) return null
        val p = (guestProfileFlow.value as? UiState.Success)?.data ?: return null
        val pets = (guestPetsFlow.value as? UiState.Success)?.data ?: emptyList()
        val vehs = (guestVehiclesFlow.value as? UiState.Success)?.data ?: emptyList()
        val props = (guestPropertiesFlow.value as? UiState.Success)?.data ?: emptyList()
        val missions = (guestMissionsFlow.value as? UiState.Success)?.data ?: emptyList()
        return GuestProgressSnapshot(
            profile = p.copy(
                worldDistrict = sim.insideHouse?.name ?: WorldMapData.resolveDistrict(sim.playerX, sim.playerZ),
                posX = sim.playerX.toDouble(),
                posY = sim.playerY.toDouble(),
                posZ = sim.playerZ.toDouble(),
                rotY = sim.playerRotY.toDouble(),
                hp = sim.hp.toDouble(),
                ep = sim.ep.toDouble(),
                shield = sim.shield.toDouble()
            ),
            pets = pets,
            vehicles = vehs,
            properties = props,
            missions = missions,
            earnedCoinsDelta = (p.coins - initialGuestCoins).coerceAtLeast(0L),
            earnedXpDelta = (p.xp - initialGuestXp).coerceAtLeast(0L),
            earnedKillsDelta = (p.kills - initialGuestKills).coerceAtLeast(0L)
        )
    }

    fun previewHeroAppearance(
        outfitStyle: String,
        hairStyle: String,
        accessoryStyle: String,
        accentColorHex: String? = null,
        hairCutId: String = _hudSettingsBundle.value.selectedHairCutId,
        headgearId: String = _hudSettingsBundle.value.selectedHeadgearId,
        weaponSkinId: String = _hudSettingsBundle.value.selectedWeaponSkinId,
        expressionId: String = _hudSettingsBundle.value.selectedExpressionId
    ) {
        sim.applyHeroAppearance(
            outfitRaw = outfitStyle,
            hairRaw = hairStyle,
            accessoryRaw = accessoryStyle,
            accentHex = accentColorHex,
            hairCutRaw = hairCutId,
            headgearRaw = headgearId,
            weaponSkinRaw = weaponSkinId,
            expressionRaw = expressionId
        )
    }

    fun restoreSavedHeroAppearance() {
        val p = (playerState.value as? UiState.Success)?.data
        val b = _hudSettingsBundle.value
        if (p != null) {
            sim.applyHeroAppearance(
                outfitRaw = p.outfitStyle,
                hairRaw = p.hairStyle,
                accessoryRaw = p.accessoryStyle,
                accentHex = p.accentColorHex,
                hairCutRaw = b.selectedHairCutId,
                headgearRaw = b.selectedHeadgearId,
                weaponSkinRaw = b.selectedWeaponSkinId,
                expressionRaw = b.selectedExpressionId
            )
        } else {
            sim.applyHeroAppearance(
                outfitRaw = "Shadow Blaze",
                hairRaw = "Black",
                accessoryRaw = "Katana",
                accentHex = "#E2263A",
                hairCutRaw = b.selectedHairCutId,
                headgearRaw = b.selectedHeadgearId,
                weaponSkinRaw = b.selectedWeaponSkinId,
                expressionRaw = b.selectedExpressionId
            )
        }
    }

    fun isStyleUnlocked(category: String, id: String): Boolean {
        val key = HeroAppearanceCatalog.styleKey(category, id)
        return key in _hudSettingsBundle.value.unlockedStyleKeys || HeroAppearanceCatalog.priceForKey(key) == 0
    }

    fun computePendingStyleCost(
        outfitId: String,
        hairId: String,
        hairCutId: String,
        accessoryId: String,
        headgearId: String,
        weaponSkinId: String
    ): Int {
        val keys = listOf(
            HeroAppearanceCatalog.styleKey("outfit", outfitId),
            HeroAppearanceCatalog.styleKey("hair", hairId),
            HeroAppearanceCatalog.styleKey("haircut", hairCutId),
            HeroAppearanceCatalog.styleKey("acc", accessoryId),
            HeroAppearanceCatalog.styleKey("gear", headgearId),
            HeroAppearanceCatalog.styleKey("wskin", weaponSkinId)
        )
        return keys.distinct()
            .filter { it !in _hudSettingsBundle.value.unlockedStyleKeys }
            .sumOf { HeroAppearanceCatalog.priceForKey(it) }
    }

    fun buyHeroStyleItem(category: String, id: String, onPurchased: (() -> Unit)? = null) {
        val key = HeroAppearanceCatalog.styleKey(category, id)
        if (isStyleUnlocked(category, id)) {
            onPurchased?.invoke()
            return
        }
        val price = HeroAppearanceCatalog.priceForKey(key).toLong()
        val profile = (playerState.value as? UiState.Success)?.data
        val coins = profile?.coins ?: 0L
        if (coins < price) {
            showBanner("Need ₹$price to unlock! (Wallet: ₹$coins)")
            return
        }
        val itemName = styleDisplayName(category, id)
        viewModelScope.launch {
            if (isGuestMode) {
                updateGuestProfile { it.copy(coins = (it.coins - price).coerceAtLeast(0L)) }
            } else if (price > 0L) {
                val res = gameRepo.awardEconomyAndCombat(currentUserId, -price, 0L, 0L, 0L)
                if (res.isFailure) {
                    showBanner("Could not purchase $itemName: ${res.exceptionOrNull()?.localizedMessage}")
                    return@launch
                }
            }
            val updatedBundle = _hudSettingsBundle.value.copy(
                unlockedStyleKeys = _hudSettingsBundle.value.unlockedStyleKeys + key
            )
            _hudSettingsBundle.value = updatedBundle
            localHudStore?.save(currentUserId, updatedBundle)
            if (!isGuestMode) {
                gameRepo.saveHudSettings(currentUserId, updatedBundle)
            }
            IndraAudioEngine.playUiTap()
            showBanner("Bought $itemName for ₹$price!")
            onPurchased?.invoke()
        }
    }

    fun buyAndEquipStyleQuick(category: String, id: String) {
        val profile = (playerState.value as? UiState.Success)?.data ?: return
        buyHeroStyleItem(category, id) {
            val curBundle = _hudSettingsBundle.value
            val nextOutfit = if (category == "outfit") HeroAppearanceCatalog.resolveOutfit(id).displayName else profile.outfitStyle
            val nextHair = if (category == "hair") HeroAppearanceCatalog.resolveHair(id).displayName else profile.hairStyle
            val nextAcc = if (category == "acc") HeroAppearanceCatalog.resolveAccessory(id).displayName else profile.accessoryStyle
            val nextHairCut = if (category == "haircut") HeroAppearanceCatalog.resolveHairCut(id).id else curBundle.selectedHairCutId
            val nextGear = if (category == "gear") HeroAppearanceCatalog.resolveHeadgear(id).id else curBundle.selectedHeadgearId
            val nextWskin = if (category == "wskin") HeroAppearanceCatalog.resolveWeaponSkin(id).id else curBundle.selectedWeaponSkinId
            val nextAccent = if (category == "outfit") HeroAppearanceCatalog.resolveOutfit(id).defaultAccentHex else profile.accentColorHex
            saveCustomization(
                username = profile.username,
                hairStyle = nextHair,
                outfitStyle = nextOutfit,
                accentColorHex = nextAccent,
                equippedAbilities = profile.equippedAbilities,
                accessoryStyle = nextAcc,
                hairCutId = nextHairCut,
                headgearId = nextGear,
                weaponSkinId = nextWskin,
                expressionId = curBundle.selectedExpressionId
            )
        }
    }

    private fun styleDisplayName(category: String, id: String): String = when (category) {
        "outfit" -> HeroAppearanceCatalog.resolveOutfit(id).displayName
        "hair" -> "${HeroAppearanceCatalog.resolveHair(id).displayName} Hair Color"
        "haircut" -> HeroAppearanceCatalog.resolveHairCut(id).displayName
        "acc" -> HeroAppearanceCatalog.resolveAccessory(id).displayName
        "gear" -> HeroAppearanceCatalog.resolveHeadgear(id).displayName
        "wskin" -> HeroAppearanceCatalog.resolveWeaponSkin(id).displayName
        else -> id
    }

    fun cycleLobbyOutfit() {
        IndraAudioEngine.playUiTap()
        val p = (playerState.value as? UiState.Success)?.data ?: return
        val catalogOutfits = HeroAppearanceCatalog.outfits
        val unlockedOutfits = catalogOutfits.filter { isStyleUnlocked("outfit", it.id) }
        val pool = if (unlockedOutfits.size > 1) unlockedOutfits else catalogOutfits
        val currentOutfit = HeroAppearanceCatalog.resolveOutfit(p.outfitStyle)
        val idx = pool.indexOfFirst { it.id == currentOutfit.id }.coerceAtLeast(0)
        val nextOutfit = pool[(idx + 1) % pool.size]
        if (isStyleUnlocked("outfit", nextOutfit.id)) {
            saveCustomization(
                username = p.username,
                hairStyle = p.hairStyle,
                outfitStyle = nextOutfit.displayName,
                accentColorHex = nextOutfit.defaultAccentHex,
                equippedAbilities = p.equippedAbilities,
                accessoryStyle = p.accessoryStyle
            )
        } else {
            previewHeroAppearance(nextOutfit.displayName, p.hairStyle, p.accessoryStyle, nextOutfit.defaultAccentHex)
            showBanner("Previewing ${nextOutfit.displayName} (₹${nextOutfit.priceRupees} in Style Studio)")
        }
    }

    fun cycleLobbyPet() {
        IndraAudioEngine.playUiTap()
        val list = (petsState.value as? UiState.Success)?.data ?: return
        if (list.isEmpty()) return
        val curIdx = list.indexOfFirst { it.petType == sim.petType }.coerceAtLeast(0)
        val nextPet = list[(curIdx + 1) % list.size]
        sim.petType = nextPet.petType
        showBanner("Showcase Pet: ${nextPet.petType}")
    }

    fun cycleLobbyVehicle() {
        IndraAudioEngine.playUiTap()
        val list = (vehiclesState.value as? UiState.Success)?.data ?: return
        if (list.isEmpty()) return
        val curIdx = list.indexOfFirst { it.modelId == sim.showcaseVehicleModel }.coerceAtLeast(0)
        val nextVeh = list[(curIdx + 1) % list.size]
        sim.showcaseVehicleModel = nextVeh.modelId
        sim.showcaseVehicleCategory = nextVeh.category
        sim.showcaseVehicleColorInt = parseHexColor(nextVeh.paintHex, 0xFFFF1E42.toInt())
        showBanner("Showcase Vehicle: ${nextVeh.modelId}")
    }

    fun onHudTick(snapshot: WorldHudSnapshot) {
        _hudSnapshot.value = snapshot
    }

    fun openPanel(panel: GamePanel) {
        IndraAudioEngine.playUiTap()
        _activePanel.value = panel
    }

    fun toggleChat() {
        IndraAudioEngine.playUiTap()
        _chatOpen.value = !_chatOpen.value
    }

    fun toggleAudio() {
        val muted = IndraAudioEngine.toggleMute()
        showBanner(if (!muted) "Game Audio & SFX Enabled" else "Game Audio Muted")
    }

    fun showBanner(message: String) {
        _bannerMessage.value = message
        viewModelScope.launch {
            delay(3200L)
            if (_bannerMessage.value == message) {
                _bannerMessage.value = null
            }
        }
    }

    fun clearInspectedNpc() {
        _inspectedNpc.value = null
        _aiDialogueState.value = UiState.Idle
    }

    fun inspectPlayer(player: PlayerProfile?) {
        _inspectedPlayer.value = player
    }

    fun onVehicleInteractPressed() {
        IndraAudioEngine.playUiTap()
        sim.triggerVehicleInteraction { alarmMsg -> showBanner(alarmMsg) }
    }

    fun onInteractPressed() {
        val canCheckWorldPlot = sim.insideHouse == null &&
            sim.drivingVehicle == null &&
            sim.findNearestParkedVehicle(6.5f) == null &&
            sim.findNearestEnterableHouse(14f) == null &&
            sim.findNearestWildPet(6.5f) == null
        val nearPlot = if (canCheckWorldPlot) sim.findNearestBuyableListing(16f) else null
        if (nearPlot != null) {
            buyPropertyListing(nearPlot)
            return
        }
        sim.interactWithNearby(
            onHouseEnter = { house ->
                sim.playerX = 0f
                sim.playerZ = 3.5f
                sim.petX = 1.8f
                sim.petZ = 4.2f
                showBanner("Entered ${house.name}")
            },
            onNpcInspect = { npc ->
                _inspectedNpc.value = npc
                askNpcAiDialogue(npc)
            },
            onVehicleAlarm = { alarmMsg ->
                showBanner(alarmMsg)
            }
        )
    }

    fun castEquippedAbility(slotIdx: Int) {
        val profile = (playerState.value as? UiState.Success)?.data
        val abilities = profile?.equippedAbilities ?: GameCatalog.abilities.take(4).map { it.name }
        val abilityName = abilities.getOrNull(slotIdx) ?: return
        if (sim.castAbility(slotIdx, abilityName)) {
            if (slotIdx >= 2) {
                viewModelScope.launch { incrementMissionsByCategory("pet", 1L) }
            }
        }
    }

    fun switchWeapon(weaponId: String) {
        IndraAudioEngine.playUiTap()
        sim.switchWeapon(weaponId)
        _hudSnapshot.value = sim.snapshotHud()
        val spec = GameCatalog.weapons.firstOrNull { it.id == weaponId }
        if (spec != null) showBanner("Equipped ${spec.name}")
    }

    fun toggleBareHands() {
        IndraAudioEngine.playUiTap()
        sim.toggleBareHands()
        _hudSnapshot.value = sim.snapshotHud()
        val spec = sim.activeWeaponSpec()
        if (spec.id == "wpn_fists") {
            showBanner("Gun Put Away • Bare Hands")
        } else {
            showBanner("Equipped ${spec.name}")
        }
    }

    fun loadCustomObjModel(context: Context, uri: Uri, fileName: String?) {
        val result = ObjMeshLoader.loadFromUri(context, uri, fileName)
        result.onSuccess { parsed ->
            sim.customObjModel = parsed
            sim.customObjUploadSeq += 1L
            _hudSnapshot.value = sim.snapshotHud()
            IndraAudioEngine.playPickup()
            showBanner("Loaded 3D Character: ${parsed.modelName} (${parsed.triangleCount} tris)")
        }.onFailure { err ->
            showBanner(err.localizedMessage ?: "Could not load .obj model. Ensure it is a valid Wavefront .obj mesh.")
        }
    }

    fun clearCustomObjModel() {
        sim.customObjModel = null
        sim.customObjUploadSeq += 1L
        _hudSnapshot.value = sim.snapshotHud()
        IndraAudioEngine.playUiTap()
        showBanner("Restored Modular Anime Hero")
    }

    fun buyWeapon(spec: WeaponSpec) {
        if (spec.id in sim.ownedWeaponIds) {
            switchWeapon(spec.id)
            return
        }
        val cost = spec.priceCoins.coerceAtLeast(0L)
        viewModelScope.launch {
            val coins = (playerState.value as? UiState.Success)?.data?.coins ?: 0L
            if (coins < cost) {
                showBanner("Need $cost Coins to buy ${spec.name} (You have $coins)")
                return@launch
            }
            if (isGuestMode) {
                updateGuestProfile { it.copy(coins = (it.coins - cost).coerceAtLeast(0L)) }
            } else if (cost > 0L) {
                val res = gameRepo.awardEconomyAndCombat(currentUserId, -cost, 0L, 25L, 0L)
                if (res.isFailure) {
                    showBanner("Failed to buy ${spec.name}: ${res.exceptionOrNull()?.localizedMessage}")
                    return@launch
                }
            }
            sim.buyWeaponToBag(spec.id)
            _hudSnapshot.value = sim.snapshotHud()
            IndraAudioEngine.playCoinTransaction(isSell = false)
            showBanner("Bought ${spec.name} for $cost Coins!")
        }
    }

    fun sellWeapon(spec: WeaponSpec) {
        if (spec.weaponType == WeaponType.FISTS || spec.id !in sim.ownedWeaponIds) {
            showBanner("Cannot sell unarmed fists or unowned weapon!")
            return
        }
        val refund = spec.sellPriceCoins.coerceAtLeast(50L)
        viewModelScope.launch {
            if (isGuestMode) {
                updateGuestProfile { it.copy(coins = it.coins + refund) }
            } else {
                val res = gameRepo.awardEconomyAndCombat(currentUserId, refund, 0L, 10L, 0L)
                if (res.isFailure) {
                    showBanner("Failed to sell ${spec.name}: ${res.exceptionOrNull()?.localizedMessage}")
                    return@launch
                }
            }
            sim.sellWeaponFromBag(spec.id)
            _hudSnapshot.value = sim.snapshotHud()
            IndraAudioEngine.playCoinTransaction(isSell = true)
            showBanner("Sold ${spec.name} for +$refund Coins!")
        }
    }

    fun buySupplyItem(isMedkit: Boolean) {
        val cost = if (isMedkit) 60L else 80L
        val label = if (isMedkit) "Medkit" else "Frag Grenade"
        viewModelScope.launch {
            val coins = (playerState.value as? UiState.Success)?.data?.coins ?: 0L
            if (coins < cost) {
                showBanner("Need $cost Coins for $label!")
                return@launch
            }
            if (isGuestMode) {
                updateGuestProfile { it.copy(coins = (it.coins - cost).coerceAtLeast(0L)) }
            } else {
                val res = gameRepo.awardEconomyAndCombat(currentUserId, -cost, 0L, 5L, 0L)
                if (res.isFailure) return@launch
            }
            if (isMedkit) sim.medkitCount += 1 else sim.grenadeCount += 1
            _hudSnapshot.value = sim.snapshotHud()
            IndraAudioEngine.playCoinTransaction(isSell = false)
            showBanner("Bought 1 $label for $cost Coins!")
        }
    }

    fun sellSupplyItem(isMedkit: Boolean) {
        val currentCount = if (isMedkit) sim.medkitCount else sim.grenadeCount
        val label = if (isMedkit) "Medkit" else "Frag Grenade"
        if (currentCount <= 0) {
            showBanner("No $label left to sell!")
            return
        }
        val refund = if (isMedkit) 35L else 45L
        viewModelScope.launch {
            if (isGuestMode) {
                updateGuestProfile { it.copy(coins = it.coins + refund) }
            } else {
                val res = gameRepo.awardEconomyAndCombat(currentUserId, refund, 0L, 0L, 0L)
                if (res.isFailure) return@launch
            }
            if (isMedkit) sim.medkitCount -= 1 else sim.grenadeCount -= 1
            _hudSnapshot.value = sim.snapshotHud()
            IndraAudioEngine.playCoinTransaction(isSell = true)
            showBanner("Sold 1 $label for +$refund Coins!")
        }
    }

    fun storeWeaponInHouse(weaponId: String) {
        if (sim.depositWeaponToStash(weaponId)) {
            _hudSnapshot.value = sim.snapshotHud()
            val name = GameCatalog.weapons.firstOrNull { it.id == weaponId }?.name ?: weaponId
            showBanner("Stored $name inside House Vault!")
        } else {
            showBanner("Cannot store unarmed fists!")
        }
    }

    fun takeWeaponFromHouse(weaponId: String) {
        if (sim.withdrawWeaponFromStash(weaponId)) {
            _hudSnapshot.value = sim.snapshotHud()
            val name = GameCatalog.weapons.firstOrNull { it.id == weaponId }?.name ?: weaponId
            showBanner("Equipped $name from House Vault!")
        }
    }

    fun storeSupplyInHouse(isMedkit: Boolean) {
        if (sim.depositSupplyToStash(isMedkit)) {
            _hudSnapshot.value = sim.snapshotHud()
            IndraAudioEngine.playPickup()
            showBanner(if (isMedkit) "Stored Medkit in Vault" else "Stored Grenade in Vault")
        }
    }

    fun takeSupplyFromHouse(isMedkit: Boolean) {
        if (sim.withdrawSupplyFromStash(isMedkit)) {
            _hudSnapshot.value = sim.snapshotHud()
            IndraAudioEngine.playPickup()
            showBanner(if (isMedkit) "Took Medkit from Vault" else "Took Grenade from Vault")
        }
    }

    fun pickupNearbyWeapon() {
        IndraAudioEngine.playUiTap()
        val picked = sim.pickupNearbyWeapon()
        _hudSnapshot.value = sim.snapshotHud()
        if (picked == null) {
            showBanner("No weapon on ground nearby")
        }
    }

    fun dropActiveWeapon() {
        IndraAudioEngine.playUiTap()
        sim.dropActiveWeapon()
        _hudSnapshot.value = sim.snapshotHud()
    }

    fun triggerHeal() {
        IndraAudioEngine.playUiTap()
        sim.triggerHeal()
        _hudSnapshot.value = sim.snapshotHud()
    }

    fun throwGrenade() {
        IndraAudioEngine.playUiTap()
        sim.throwGrenade()
        _hudSnapshot.value = sim.snapshotHud()
    }

    fun triggerSwordHeavy() {
        IndraAudioEngine.playUiTap()
        sim.triggerSwordHeavy()
        _hudSnapshot.value = sim.snapshotHud()
    }

    fun toggleCrouch() {
        IndraAudioEngine.playUiTap()
        sim.toggleCrouch()
        _hudSnapshot.value = sim.snapshotHud()
    }

    fun toggleProne() {
        IndraAudioEngine.playUiTap()
        sim.toggleProne()
        _hudSnapshot.value = sim.snapshotHud()
    }

    fun setGpsDestination(location: WorldLocation) {
        IndraAudioEngine.playUiTap()
        _activeGpsDestination.value = location
        sim.setGpsTarget(location)
        showBanner("GPS Route set: ${location.name} (${location.district.displayName})")
    }

    fun clearGpsDestination() {
        _activeGpsDestination.value = null
        sim.setGpsTarget(null)
        showBanner("GPS Navigation cleared")
    }

    fun fastTravelToDistrict(x: Float, z: Float, districtName: String) {
        sim.teleportToDistrict(x, z, districtName)
        _activePanel.value = GamePanel.NONE
        viewModelScope.launch { syncTransformNow() }
    }

    fun enterHouseInterior(house: WorldBuilding? = null) {
        val target = house ?: WorldMapData.buildings.firstOrNull { it.enterable } ?: return
        sim.insideHouse = target
        sim.drivingVehicle = null
        sim.playerX = 0f
        sim.playerY = 0f
        sim.playerZ = 3.5f
        sim.petX = 1.8f
        sim.petZ = 4.2f
        IndraAudioEngine.playDoorTransition()
        _activePanel.value = GamePanel.NONE
        _hudSnapshot.value = sim.snapshotHud()
        showBanner("Entered ${target.name} Interior")
    }

    fun enterOwnedPropertyInterior(prop: OwnedProperty) {
        val listing = GameCatalog.propertyMarketListings.firstOrNull { it.id == prop.id }
        val building = WorldBuilding(
            id = prop.id,
            name = prop.customName,
            district = prop.district,
            x = listing?.locX ?: -420f,
            z = listing?.locZ ?: 310f,
            width = 18f,
            depth = 16f,
            height = 9f,
            style = BuildingStyle.PLAYER_VILLA,
            wallColor = 0xFFFDF4E7.toInt(),
            roofColor = 0xFF334155.toInt(),
            accentColor = 0xFFFFD166.toInt(),
            enterable = true,
            interiorType = InteriorType.PLAYER_VILLA
        )
        enterHouseInterior(building)
    }

    fun exitHouseInterior() {
        val exited = sim.insideHouse ?: return
        sim.insideHouse = null
        sim.playerX = exited.x
        sim.playerZ = exited.z + exited.depth * 0.5f + 4.5f
        sim.playerY = WorldMapData.terrainHeightAt(sim.playerX, sim.playerZ)
        sim.petX = sim.playerX + 1.8f
        sim.petZ = sim.playerZ + 1.2f
        IndraAudioEngine.playDoorTransition()
        _hudSnapshot.value = sim.snapshotHud()
        showBanner("Exited ${exited.name} back into open world")
    }

    fun spawnAndDriveOwnedVehicle(vehicle: OwnedVehicle) {
        if (vehicle.locked) {
            showBanner("${vehicle.modelId} is locked! Unlock with Car Key first.")
            return
        }
        val parked = com.example.game.engine.ParkedWorldVehicle(
            id = vehicle.id,
            modelName = vehicle.modelId,
            category = vehicle.category,
            x = sim.playerX,
            z = sim.playerZ,
            rotY = sim.playerRotY,
            colorInt = parseHexColor(vehicle.paintHex, 0xFFFF3B5C.toInt()),
            isLocked = false,
            ownerName = "Your Garage"
        )
        sim.insideHouse = null
        sim.drivingVehicle = parked
        _activePanel.value = GamePanel.NONE
        viewModelScope.launch { incrementMissionsByCategory("driving", 1L) }
        showBanner("Spawned & driving ${vehicle.modelId}!")
    }

    fun syncTransformNow() {
        if (isGuestMode) return
        viewModelScope.launch {
            gameRepo.syncPlayerTransformAndStats(
                uid = currentUserId,
                district = sim.insideHouse?.name ?: WorldMapData.resolveDistrict(sim.playerX, sim.playerZ),
                posX = sim.playerX.toDouble(),
                posY = sim.playerY.toDouble(),
                posZ = sim.playerZ.toDouble(),
                rotY = sim.playerRotY.toDouble(),
                hp = sim.hp.toDouble(),
                ep = sim.ep.toDouble(),
                shield = sim.shield.toDouble()
            )
        }
    }

    fun startLiveLocationTracking(context: Context) {
        if (!LocationTrackingService.hasLocationPermission(context)) {
            onLocationPermissionDenied()
            return
        }
        LocationTrackingService.startTracking(context, if (isGuestMode) null else currentUserId)
        if (isGuestMode) {
            updateGuestProfile { it.copy(locationTrackingEnabled = true) }
        } else {
            viewModelScope.launch {
                gameRepo.setLocationTrackingEnabled(currentUserId, true)
            }
        }
        IndraAudioEngine.playUiTap()
        showBanner("Live GPS Sync started • Updating player coordinates")
    }

    fun stopLiveLocationTracking(context: Context) {
        LocationTrackingService.stopTracking(context)
        if (isGuestMode) {
            updateGuestProfile { it.copy(locationTrackingEnabled = false) }
        } else {
            viewModelScope.launch {
                gameRepo.setLocationTrackingEnabled(currentUserId, false)
            }
        }
        IndraAudioEngine.playUiTap()
        showBanner("Live GPS Sync stopped")
    }

    fun onLocationPermissionDenied() {
        LocationTrackingState.setStatus(false, "Location permission denied")
        showBanner("Location permission is required to sync GPS coordinates to your profile.")
    }

    fun toggleOfflineAiProxy(enable: Boolean) {
        if (isGuestMode) {
            showBanner("Link Google or Facebook to activate Cloud Offline AI Proxy.")
            return
        }
        viewModelScope.launch {
            syncTransformNow()
            val action = if (enable) {
                "AI Proxy patrolling ${WorldMapData.resolveDistrict(sim.playerX, sim.playerZ)} (currencies protected)"
            } else {
                "Player reconnected; AI Proxy stopped"
            }
            val res = gameRepo.toggleOfflineAiProxyMode(currentUserId, enable, action)
            if (res.isSuccess) {
                showBanner(if (enable) "Offline AI Proxy Activated!" else "Direct Player Control Restored")
            } else {
                _syncNotice.value = res.exceptionOrNull()?.localizedMessage
            }
        }
    }

    fun saveCustomization(
        username: String,
        hairStyle: String,
        outfitStyle: String,
        accentColorHex: String,
        equippedAbilities: List<String>,
        accessoryStyle: String = (playerState.value as? UiState.Success)?.data?.accessoryStyle ?: "Katana",
        hairCutId: String = _hudSettingsBundle.value.selectedHairCutId,
        headgearId: String = _hudSettingsBundle.value.selectedHeadgearId,
        weaponSkinId: String = _hudSettingsBundle.value.selectedWeaponSkinId,
        expressionId: String = _hudSettingsBundle.value.selectedExpressionId
    ) {
        val outfitPreset = HeroAppearanceCatalog.resolveOutfit(outfitStyle)
        val hairPreset = HeroAppearanceCatalog.resolveHair(hairStyle)
        val hairCutPreset = HeroAppearanceCatalog.resolveHairCut(hairCutId)
        val accessoryPreset = HeroAppearanceCatalog.resolveAccessory(accessoryStyle)
        val headgearPreset = HeroAppearanceCatalog.resolveHeadgear(headgearId)
        val weaponSkinPreset = HeroAppearanceCatalog.resolveWeaponSkin(weaponSkinId)
        val exprPreset = HeroAppearanceCatalog.resolveExpression(expressionId)

        val resolvedOutfit = outfitPreset.displayName
        val resolvedHair = hairPreset.displayName
        val resolvedAccessory = accessoryPreset.displayName

        val missingKeys = listOf(
            HeroAppearanceCatalog.styleKey("outfit", outfitPreset.id),
            HeroAppearanceCatalog.styleKey("hair", hairPreset.id),
            HeroAppearanceCatalog.styleKey("haircut", hairCutPreset.id),
            HeroAppearanceCatalog.styleKey("acc", accessoryPreset.id),
            HeroAppearanceCatalog.styleKey("gear", headgearPreset.id),
            HeroAppearanceCatalog.styleKey("wskin", weaponSkinPreset.id)
        ).distinct().filter {
            it !in _hudSettingsBundle.value.unlockedStyleKeys && HeroAppearanceCatalog.priceForKey(it) > 0
        }
        val purchaseCost = missingKeys.sumOf { HeroAppearanceCatalog.priceForKey(it) }.toLong()

        viewModelScope.launch {
            val profile = (playerState.value as? UiState.Success)?.data ?: return@launch
            if (purchaseCost > 0L && profile.coins < purchaseCost) {
                restoreSavedHeroAppearance()
                showBanner("Need ₹$purchaseCost to buy selected styles (You have ₹${profile.coins})")
                return@launch
            }

            if (purchaseCost > 0L && !isGuestMode) {
                val payRes = gameRepo.awardEconomyAndCombat(currentUserId, -purchaseCost, 0L, 0L, 0L)
                if (payRes.isFailure) {
                    showBanner("Style purchase failed: ${payRes.exceptionOrNull()?.localizedMessage}")
                    return@launch
                }
            }

            val updatedBundle = _hudSettingsBundle.value.copy(
                unlockedStyleKeys = _hudSettingsBundle.value.unlockedStyleKeys + missingKeys,
                selectedHairCutId = hairCutPreset.id,
                selectedHeadgearId = headgearPreset.id,
                selectedWeaponSkinId = weaponSkinPreset.id,
                selectedExpressionId = exprPreset.id
            )
            _hudSettingsBundle.value = updatedBundle
            localHudStore?.save(currentUserId, updatedBundle)
            if (!isGuestMode) {
                gameRepo.saveHudSettings(currentUserId, updatedBundle)
            }

            sim.applyHeroAppearance(
                outfitRaw = resolvedOutfit,
                hairRaw = resolvedHair,
                accessoryRaw = resolvedAccessory,
                accentHex = accentColorHex,
                hairCutRaw = hairCutPreset.id,
                headgearRaw = headgearPreset.id,
                weaponSkinRaw = weaponSkinPreset.id,
                expressionRaw = exprPreset.id
            )

            val totalCap = (profile.unlockedCharSlots + profile.unlockedPetSlots).toInt().coerceIn(4, 8)
            if (isGuestMode) {
                updateGuestProfile {
                    it.copy(
                        username = username.trim().take(24).ifBlank { it.username },
                        coins = (it.coins - purchaseCost).coerceAtLeast(0L),
                        hairStyle = resolvedHair,
                        outfitStyle = resolvedOutfit,
                        accessoryStyle = resolvedAccessory,
                        accentColorHex = accentColorHex,
                        equippedAbilities = equippedAbilities.take(totalCap),
                        activeWeaponId = sim.activeWeaponId
                    )
                }
                val suffix = if (purchaseCost > 0L) " (Bought ₹$purchaseCost)" else ""
                showBanner("Character & Style saved$suffix!")
                return@launch
            }
            val res = gameRepo.updateCustomizationAndLoadout(
                uid = currentUserId,
                username = username,
                hairStyle = resolvedHair,
                outfitStyle = resolvedOutfit,
                accessoryStyle = resolvedAccessory,
                accentColorHex = accentColorHex,
                equippedAbilities = equippedAbilities.take(totalCap),
                activeWeaponId = sim.activeWeaponId,
                activePetId = profile.activePetId,
                activeVehicleId = profile.activeVehicleId
            )
            if (res.isSuccess) {
                val suffix = if (purchaseCost > 0L) " & Unlocked ₹$purchaseCost styles" else ""
                showBanner("Character & Style saved to cloud$suffix!")
            }
        }
    }

    fun buyOrUpgradePet(petType: String, rarity: String, cost: Long, existingPet: OwnedPet? = null) {
        viewModelScope.launch {
            val coins = (playerState.value as? UiState.Success)?.data?.coins ?: 0L
            if (coins < cost) {
                showBanner("Not enough Coins ($coins/$cost)")
                return@launch
            }
            val spec = PetCatalog.resolveSpec(petType)
            val currentList = (petsState.value as? UiState.Success)?.data ?: emptyList()
            val petId = existingPet?.id ?: spec.id
            val nextEvo = if (existingPet != null) {
                (existingPet.effectiveEvolutionLevel() + 1).coerceAtMost(3)
            } else 1
            val updated = if (existingPet != null) {
                existingPet.copy(
                    level = (existingPet.level + 1L).coerceAtMost(100L),
                    xp = existingPet.xp + 120L,
                    evolutionLevel = nextEvo.toLong(),
                    equipped = existingPet.equipped
                )
            } else {
                val canAutoEquip = currentList.count { it.equipped } < PetCatalog.MAX_EQUIPPED_ROSTER
                OwnedPet(
                    id = petId,
                    userId = currentUserId,
                    petType = spec.name,
                    nickname = spec.name,
                    rarity = spec.rarity.ifBlank { rarity },
                    level = 1L,
                    xp = 0L,
                    skinId = "prime_${spec.name.lowercase().replace(" ", "_")}",
                    abilityId = spec.activeName,
                    equipped = canAutoEquip,
                    evolutionLevel = 1L,
                    customTintHex = "",
                    auraStyle = "Classic"
                )
            }
            if (isGuestMode) {
                updateGuestProfile { it.copy(coins = (it.coins - cost).coerceAtLeast(0L)) }
                val current = currentList.toMutableList()
                val idx = current.indexOfFirst { it.id == petId }
                if (idx >= 0) current[idx] = updated else current.add(updated)
                guestPetsFlow.value = UiState.Success(current)
                applyPetToSimulation(updated)
                if (existingPet != null) {
                    IndraAudioEngine.playPetEvolutionFanfare(nextEvo)
                    showBanner("${updated.petType} evolved to ${spec.evolutionTitle(nextEvo)}!")
                } else {
                    IndraAudioEngine.playPetSound(spec.soundProfile, 1)
                    showBanner("Adopted ${spec.name} (${spec.passiveName})!")
                }
                return@launch
            }
            val res = gameRepo.unlockOrUpgradePet(currentUserId, petId, updated, cost)
            if (res.isSuccess) {
                applyPetToSimulation(updated)
                if (existingPet != null) {
                    IndraAudioEngine.playPetEvolutionFanfare(nextEvo)
                    showBanner("${updated.petType} evolved to ${spec.evolutionTitle(nextEvo)}!")
                } else {
                    IndraAudioEngine.playPetSound(spec.soundProfile, 1)
                    showBanner("Adopted ${spec.name}!")
                }
            }
        }
    }

    fun evolvePet(pet: OwnedPet) {
        val currentEvo = pet.effectiveEvolutionLevel()
        if (currentEvo >= 3) {
            showBanner("${pet.petType} is already at Max Lv.3 Legendary!")
            return
        }
        val cost = if (currentEvo == 1) 240L else 480L
        buyOrUpgradePet(pet.petType, pet.rarity, cost, pet)
    }

    fun adoptWildPetFromWorld(wildPet: com.example.game.engine.WildWorldPet) {
        val spec = PetCatalog.resolveSpec(wildPet.petId)
        viewModelScope.launch {
            val currentList = (petsState.value as? UiState.Success)?.data ?: emptyList()
            val canAutoEquip = currentList.count { it.equipped } < PetCatalog.MAX_EQUIPPED_ROSTER
            val adoptedPet = OwnedPet(
                id = spec.id,
                userId = currentUserId,
                petType = spec.name,
                nickname = "Wild ${spec.name}",
                rarity = spec.rarity,
                level = (wildPet.evolutionLevel * 5L),
                xp = 200L,
                skinId = "wild_${spec.name.lowercase().replace(" ", "_")}",
                abilityId = spec.activeName,
                equipped = canAutoEquip,
                evolutionLevel = wildPet.evolutionLevel.toLong().coerceIn(1L, 3L),
                customTintHex = "",
                auraStyle = if (wildPet.evolutionLevel >= 3) "Celestial" else "Classic"
            )
            if (isGuestMode) {
                val next = currentList.toMutableList()
                val idx = next.indexOfFirst { it.id == adoptedPet.id }
                if (idx >= 0) next[idx] = adoptedPet else next.add(adoptedPet)
                guestPetsFlow.value = UiState.Success(next)
            } else {
                gameRepo.unlockOrUpgradePet(currentUserId, adoptedPet.id, adoptedPet, 0L)
            }
            applyPetToSimulation(adoptedPet)
            IndraAudioEngine.playPetEvolutionFanfare(wildPet.evolutionLevel)
            showBanner("Tamed Wild Lv.${wildPet.evolutionLevel} ${spec.name}! Added to your collection.")
        }
    }

    fun togglePetRosterSlot(pet: OwnedPet) {
        val currentList = (petsState.value as? UiState.Success)?.data ?: return
        val nextEquipped = !pet.equipped
        if (nextEquipped && currentList.count { it.equipped } >= PetCatalog.MAX_EQUIPPED_ROSTER) {
            showBanner("Max 5 pets can be equipped in active roster at once!")
            return
        }
        IndraAudioEngine.playUiTap()
        viewModelScope.launch {
            if (isGuestMode) {
                val updated = currentList.map { if (it.id == pet.id) it.copy(equipped = nextEquipped) else it }
                guestPetsFlow.value = UiState.Success(updated)
                sim.petController.updateRoster(updated)
            } else {
                gameRepo.setEquippedPet(currentUserId, pet.id, equippedInRoster = nextEquipped, setAsMainFollower = false)
            }
            showBanner(if (nextEquipped) "${pet.petType} added to 5-Pet Roster!" else "${pet.petType} moved to Reserve.")
        }
    }

    fun equipPet(pet: OwnedPet) {
        val spec = PetCatalog.resolveSpec(pet.petType)
        IndraAudioEngine.playPetSound(spec.soundProfile, pet.effectiveEvolutionLevel())
        val currentList = (petsState.value as? UiState.Success)?.data ?: emptyList()
        val activeRosterCount = currentList.count { it.equipped }
        val shouldEquipInRoster = pet.equipped || activeRosterCount < PetCatalog.MAX_EQUIPPED_ROSTER
        applyPetToSimulation(pet)
        viewModelScope.launch {
            if (isGuestMode) {
                val nextList = currentList.map {
                    if (it.id == pet.id) it.copy(equipped = shouldEquipInRoster) else it
                }
                guestPetsFlow.value = UiState.Success(nextList)
                sim.petController.updateRoster(nextList)
                updateGuestProfile { it.copy(activePetId = pet.id) }
            } else {
                gameRepo.setEquippedPet(currentUserId, pet.id, equippedInRoster = shouldEquipInRoster, setAsMainFollower = true)
            }
            showBanner("${pet.nickname.ifBlank { pet.petType }} following you in 3D!")
        }
    }

    fun customizePet(pet: OwnedPet, nickname: String, customTintHex: String, auraStyle: String) {
        IndraAudioEngine.playUiTap()
        val cleanNick = nickname.trim().take(30).ifBlank { pet.petType }
        val updated = pet.copy(nickname = cleanNick, customTintHex = customTintHex, auraStyle = auraStyle)
        if (sim.petType.equals(PetCatalog.resolveSpec(pet.petType).name, ignoreCase = true)) {
            sim.petCustomTintHex = customTintHex
            sim.petAuraStyle = auraStyle
        }
        viewModelScope.launch {
            if (isGuestMode) {
                val list = (guestPetsFlow.value as? UiState.Success)?.data ?: emptyList()
                guestPetsFlow.value = UiState.Success(list.map { if (it.id == pet.id) updated else it })
            } else {
                gameRepo.customizePet(currentUserId, pet.id, cleanNick, customTintHex, auraStyle)
            }
            showBanner("Saved customization for $cleanNick!")
        }
    }

    private fun applyPetToSimulation(pet: OwnedPet) {
        val spec = PetCatalog.resolveSpec(pet.petType)
        sim.petType = spec.name
        sim.petEvolutionLevel = pet.effectiveEvolutionLevel()
        sim.petCustomTintHex = pet.customTintHex
        sim.petAuraStyle = pet.auraStyle
        sim.petSummoned = true
    }

    fun togglePetSummoned() {
        IndraAudioEngine.playUiTap()
        sim.petSummoned = !sim.petSummoned
        showBanner(if (sim.petSummoned) "${sim.petType} summoned!" else "${sim.petType} dismissed!")
    }

    fun equipPetAbilityToSlot(petSlotOffset: Int, abilityName: String) {
        val profile = (playerState.value as? UiState.Success)?.data ?: return
        val charSlots = profile.unlockedCharSlots.toInt().coerceIn(2, 3)
        val petSlots = profile.unlockedPetSlots.toInt().coerceIn(2, 4)
        val totalSlots = charSlots + petSlots
        // Support both raw absolute slot indices (legacy callers passing 2 or 3) and pet-relative indices
        val absIndex = if (petSlotOffset >= charSlots) {
            petSlotOffset.coerceIn(charSlots, totalSlots - 1)
        } else {
            (charSlots + petSlotOffset).coerceIn(charSlots, totalSlots - 1)
        }
        val next = profile.equippedAbilities.toMutableList()
        while (next.size < totalSlots) next.add("Fox Flame")
        next[absIndex] = abilityName
        saveCustomization(
            username = profile.username,
            hairStyle = profile.hairStyle,
            outfitStyle = profile.outfitStyle,
            accentColorHex = profile.accentColorHex,
            equippedAbilities = next,
            accessoryStyle = profile.accessoryStyle
        )
    }

    fun unlockExtraAbilitySlot(forPet: Boolean) {
        val profile = (playerState.value as? UiState.Success)?.data ?: return
        val curChar = profile.unlockedCharSlots.toInt().coerceIn(2, 3)
        val curPet = profile.unlockedPetSlots.toInt().coerceIn(2, 4)
        if (forPet && curPet >= PetCatalog.MAX_ABILITY_SLOTS_PET) {
            showBanner("All 4 Pet Ability Slots unlocked!")
            return
        }
        if (!forPet && curChar >= PetCatalog.MAX_ABILITY_SLOTS_CHAR) {
            showBanner("All 3 Character Ability Slots unlocked!")
            return
        }
        val useUpgradePoints = profile.upgradePoints >= 2L
        val upCost = if (useUpgradePoints) 2L else 0L
        val coinCost = if (useUpgradePoints) 0L else 350L
        if (!useUpgradePoints && profile.coins < coinCost) {
            showBanner("Need 2 Upgrade Points or 350 Coins to unlock slot!")
            return
        }

        val nextChar = if (!forPet) (curChar + 1).toLong() else curChar.toLong()
        val nextPet = if (forPet) (curPet + 1).toLong() else curPet.toLong()
        val nextAbilities = profile.equippedAbilities.toMutableList()
        val targetTotal = (nextChar + nextPet).toInt()
        if (!forPet) {
            val extraChar = GameCatalog.abilities.firstOrNull { it.source == "Character" && it.name !in nextAbilities }?.name ?: "Aegis Barrier"
            nextAbilities.add(curChar.coerceAtMost(nextAbilities.size), extraChar)
        } else {
            val equippedPetSpec = sim.petController.equippedRoster
                .map { PetCatalog.resolveSpec(it.petType) }
                .firstOrNull { it.activeName !in nextAbilities }
                ?: PetCatalog.allPets.firstOrNull { it.activeName !in nextAbilities }
                ?: PetCatalog.allPets.first()
            while (nextAbilities.size < targetTotal - 1) nextAbilities.add("Fox Flame")
            nextAbilities.add(equippedPetSpec.activeName)
        }
        IndraAudioEngine.playPetEvolutionFanfare(3)
        viewModelScope.launch {
            if (isGuestMode) {
                updateGuestProfile {
                    it.copy(
                        unlockedCharSlots = nextChar,
                        unlockedPetSlots = nextPet,
                        upgradePoints = (it.upgradePoints - upCost).coerceAtLeast(0L),
                        coins = (it.coins - coinCost).coerceAtLeast(0L),
                        equippedAbilities = nextAbilities.take(8)
                    )
                }
            } else {
                gameRepo.unlockAbilitySlot(currentUserId, nextChar, nextPet, upCost, coinCost, nextAbilities.take(8))
            }
            showBanner(if (forPet) "Unlocked Pet Ability Slot #$nextPet!" else "Unlocked Character Ability Slot #$nextChar!")
        }
    }

    fun sendPetTradeOffer(targetPlayer: PlayerProfile?, offeredPet: OwnedPet, requestedSpec: com.example.data.model.PetSpec) {
        val me = (playerState.value as? UiState.Success)?.data ?: return
        val myPets = (petsState.value as? UiState.Success)?.data ?: return
        if (myPets.size <= 1) {
            showBanner("Adopt at least 2 pets before trading away your companion!")
            return
        }
        if (targetPlayer == null || isGuestMode) {
            // Instant confirmed Pet Exchange with Sakura Guild Trader (or local simulation preview)
            val updatedList = myPets.filterNot { it.id == offeredPet.id }.toMutableList()
            val canEquip = updatedList.count { it.equipped } < PetCatalog.MAX_EQUIPPED_ROSTER
            val received = OwnedPet(
                id = requestedSpec.id,
                userId = currentUserId,
                petType = requestedSpec.name,
                nickname = requestedSpec.name,
                rarity = requestedSpec.rarity,
                level = offeredPet.level.coerceAtLeast(1L),
                xp = 80L,
                skinId = "trade_${requestedSpec.name.lowercase().replace(" ", "_")}",
                abilityId = requestedSpec.activeName,
                equipped = canEquip,
                evolutionLevel = offeredPet.evolutionLevel,
                customTintHex = "",
                auraStyle = offeredPet.auraStyle
            )
            if (updatedList.none { it.id == received.id }) updatedList.add(received)
            viewModelScope.launch {
                if (isGuestMode) {
                    guestPetsFlow.value = UiState.Success(updatedList)
                } else {
                    gameRepo.unlockOrUpgradePet(currentUserId, received.id, received, 0L)
                }
                applyPetToSimulation(received)
                IndraAudioEngine.playPetEvolutionFanfare(received.effectiveEvolutionLevel())
                showBanner("Trade Complete! Traded ${offeredPet.petType} for ${requestedSpec.name}!")
            }
            return
        }
        viewModelScope.launch {
            val res = gameRepo.createPetTradeOffer(
                senderUid = currentUserId,
                senderName = me.username,
                receiverUid = targetPlayer.userId,
                receiverName = targetPlayer.username,
                offeredPet = offeredPet,
                requestedPetType = requestedSpec.name
            )
            if (res.isSuccess) {
                showBanner("Pet Trade Offer sent to ${targetPlayer.username}!")
            }
        }
    }

    fun respondPetTrade(trade: PetTradeOffer, accept: Boolean) {
        if (isGuestMode) return
        viewModelScope.launch {
            val spec = PetCatalog.resolveSpec(trade.offeredPetType)
            val received = if (accept) {
                OwnedPet(
                    id = spec.id,
                    userId = currentUserId,
                    petType = spec.name,
                    nickname = spec.name,
                    rarity = spec.rarity,
                    level = 3L,
                    xp = 60L,
                    skinId = "trade_${spec.name.lowercase().replace(" ", "_")}",
                    abilityId = spec.activeName,
                    equipped = true,
                    evolutionLevel = trade.offeredEvolutionLevel
                )
            } else null
            val res = gameRepo.respondPetTradeOffer(currentUserId, trade, accept, received)
            if (res.isSuccess) {
                if (received != null) {
                    applyPetToSimulation(received)
                    IndraAudioEngine.playPetEvolutionFanfare(received.effectiveEvolutionLevel())
                }
                showBanner(if (accept) "Accepted Trade! Received Lv.${trade.offeredEvolutionLevel} ${spec.name}!" else "Declined Pet Trade.")
            }
        }
    }

    fun toggleCarKeyLock(vehicle: OwnedVehicle) {
        viewModelScope.launch {
            val nextLock = !vehicle.locked
            if (isGuestMode) {
                val list = (guestVehiclesFlow.value as? UiState.Success)?.data ?: emptyList()
                guestVehiclesFlow.value = UiState.Success(
                    list.map { if (it.id == vehicle.id) it.copy(locked = nextLock, alarmArmed = nextLock) else it }
                )
            } else {
                gameRepo.toggleVehicleLockOrStealState(
                    uid = currentUserId,
                    vehicleId = vehicle.id,
                    locked = nextLock,
                    alarmArmed = nextLock
                )
            }
            showBanner(if (nextLock) "Car Key: ${vehicle.modelId} LOCKED & Alarm Armed" else "Car Key: ${vehicle.modelId} UNLOCKED")
        }
    }

    fun repaintVehicle(vehicle: OwnedVehicle, paintHex: String) {
        viewModelScope.launch {
            if (isGuestMode) {
                val list = (guestVehiclesFlow.value as? UiState.Success)?.data ?: emptyList()
                guestVehiclesFlow.value = UiState.Success(
                    list.map { if (it.id == vehicle.id) it.copy(paintHex = paintHex) else it }
                )
            } else {
                gameRepo.toggleVehicleLockOrStealState(
                    uid = currentUserId,
                    vehicleId = vehicle.id,
                    locked = vehicle.locked,
                    alarmArmed = vehicle.alarmArmed,
                    paintHex = paintHex
                )
            }
            showBanner("Repainted ${vehicle.modelId}!")
        }
    }

    fun buyVehicle(modelId: String, category: String, cost: Long) {
        viewModelScope.launch {
            val coins = (playerState.value as? UiState.Success)?.data?.coins ?: 0L
            if (coins < cost) {
                showBanner("Need $cost Coins (You have $coins)")
                return@launch
            }
            val vehId = "veh_${modelId.lowercase().replace(" ", "_").replace("-", "_")}"
            val veh = OwnedVehicle(
                id = vehId,
                userId = currentUserId,
                modelId = modelId,
                category = category,
                paintHex = "#00E5FF",
                skinName = "Indra Custom",
                locked = true,
                alarmArmed = true,
                garageSlot = 3L,
                upgradeLevel = 1L
            )
            if (isGuestMode) {
                updateGuestProfile { it.copy(coins = (it.coins - cost).coerceAtLeast(0L)) }
                val list = (guestVehiclesFlow.value as? UiState.Success)?.data?.toMutableList() ?: mutableListOf()
                val idx = list.indexOfFirst { it.id == vehId }
                if (idx >= 0) list[idx] = veh else list.add(veh)
                guestVehiclesFlow.value = UiState.Success(list)
                showBanner("Purchased $modelId!")
                return@launch
            }
            val res = gameRepo.buyVehicle(currentUserId, vehId, veh, cost)
            if (res.isSuccess) showBanner("Purchased $modelId!")
        }
    }

    fun buyOrUpgradeProperty(
        propertyType: String,
        district: String,
        cost: Long,
        existing: OwnedProperty? = null,
        newTheme: String? = null
    ) {
        viewModelScope.launch {
            val coins = (playerState.value as? UiState.Success)?.data?.coins ?: 0L
            if (coins < cost) {
                showBanner("Need $cost Coins (You have $coins)")
                return@launch
            }
            val propId = existing?.id ?: "prop_${propertyType}"
            val updated = if (existing != null) {
                existing.copy(
                    upgradeLevel = (existing.upgradeLevel + 1L).coerceAtMost(10L),
                    decorTheme = newTheme ?: existing.decorTheme,
                    furnitureCount = (existing.furnitureCount + 3L).coerceAtMost(200L),
                    garageCapacity = (existing.garageCapacity + 1L).coerceAtMost(20L)
                )
            } else {
                OwnedProperty(
                    id = propId,
                    userId = currentUserId,
                    propertyType = propertyType,
                    district = district,
                    customName = "${district.split(" ").first()} Residence",
                    upgradeLevel = 1L,
                    decorTheme = "Neo-Tokyo Tatami",
                    furnitureCount = 8L,
                    petAreaEnabled = true,
                    garageCapacity = 2L
                )
            }
            if (isGuestMode) {
                updateGuestProfile { it.copy(coins = (it.coins - cost).coerceAtLeast(0L)) }
                val list = (guestPropertiesFlow.value as? UiState.Success)?.data?.toMutableList() ?: mutableListOf()
                val idx = list.indexOfFirst { it.id == propId }
                if (idx >= 0) list[idx] = updated else list.add(updated)
                guestPropertiesFlow.value = UiState.Success(list)
                sim.ownedPropertyIds = list.map { it.id }.toSet()
                IndraAudioEngine.playCoinTransaction(isSell = false)
                showBanner(if (existing != null) "Upgraded ${updated.customName} to Lv.${updated.upgradeLevel}!" else "Purchased $propertyType in $district!")
                return@launch
            }
            val res = gameRepo.buyOrUpgradeProperty(currentUserId, propId, updated, cost)
            if (res.isSuccess) {
                sim.ownedPropertyIds = sim.ownedPropertyIds + propId
                IndraAudioEngine.playCoinTransaction(isSell = false)
                showBanner(if (existing != null) "Upgraded ${updated.customName} to Lv.${updated.upgradeLevel}!" else "Purchased $propertyType in $district!")
            }
        }
    }

    fun buyPropertyListing(listing: PropertyMarketListing) {
        val currentProps = (propertiesState.value as? UiState.Success)?.data ?: emptyList()
        if (currentProps.any { it.id == listing.id }) {
            showBanner("You already own ${listing.title}!")
            return
        }
        viewModelScope.launch {
            val coins = (playerState.value as? UiState.Success)?.data?.coins ?: 0L
            if (coins < listing.buyCoins) {
                showBanner("Need ${listing.buyCoins} Coins to buy ${listing.title} (Wallet: $coins)")
                return@launch
            }
            val purchased = OwnedProperty(
                id = listing.id,
                userId = currentUserId,
                propertyType = listing.propertyType,
                district = listing.district,
                customName = listing.title.take(48),
                upgradeLevel = 1L,
                decorTheme = if (listing.isLandPlot) "Private Plot Pavilion" else "Neo-Tokyo Tatami",
                furnitureCount = if (listing.isLandPlot) 4L else 8L,
                petAreaEnabled = true,
                garageCapacity = listing.garageCapacity
            )
            if (isGuestMode) {
                updateGuestProfile { it.copy(coins = (it.coins - listing.buyCoins).coerceAtLeast(0L)) }
                val next = currentProps + purchased
                guestPropertiesFlow.value = UiState.Success(next)
                sim.ownedPropertyIds = next.map { it.id }.toSet()
                _hudSnapshot.value = sim.snapshotHud()
                IndraAudioEngine.playCoinTransaction(isSell = false)
                showBanner("Purchased ${listing.title} for ${listing.buyCoins} Coins!")
                return@launch
            }
            val res = gameRepo.buyOrUpgradeProperty(currentUserId, listing.id, purchased, listing.buyCoins)
            if (res.isSuccess) {
                sim.ownedPropertyIds = sim.ownedPropertyIds + listing.id
                _hudSnapshot.value = sim.snapshotHud()
                IndraAudioEngine.playCoinTransaction(isSell = false)
                showBanner("Purchased ${listing.title} for ${listing.buyCoins} Coins!")
            } else {
                showBanner("Could not buy ${listing.title}: ${res.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    fun propertySellRefund(prop: OwnedProperty): Long {
        val listing = GameCatalog.propertyMarketListings.firstOrNull { it.id == prop.id }
        val base = listing?.sellCoins ?: when (prop.propertyType) {
            "villa" -> 1150L
            "luxury_house" -> 760L
            "apartment" -> 460L
            else -> 320L
        }
        return base + ((prop.upgradeLevel - 1L).coerceAtLeast(0L) * 140L)
    }

    fun sellProperty(prop: OwnedProperty) {
        val refund = propertySellRefund(prop)
        viewModelScope.launch {
            if (isGuestMode) {
                val list = (guestPropertiesFlow.value as? UiState.Success)?.data ?: emptyList()
                val remaining = list.filterNot { it.id == prop.id }
                guestPropertiesFlow.value = UiState.Success(remaining)
                updateGuestProfile { it.copy(coins = it.coins + refund) }
                sim.ownedPropertyIds = remaining.map { it.id }.toSet()
            } else {
                val res = gameRepo.sellProperty(currentUserId, prop.id, refund)
                if (res.isFailure) {
                    showBanner("Failed to sell ${prop.customName}: ${res.exceptionOrNull()?.localizedMessage}")
                    return@launch
                }
                sim.ownedPropertyIds = sim.ownedPropertyIds - prop.id
            }
            if (sim.insideHouse?.id == prop.id || sim.insideHouse?.name == prop.customName) {
                exitHouseInterior()
            }
            _hudSnapshot.value = sim.snapshotHud()
            IndraAudioEngine.playCoinTransaction(isSell = true)
            showBanner("Sold ${prop.customName} for +$refund Coins!")
        }
    }

    fun sellActiveRoomHouse() {
        val owned = (propertiesState.value as? UiState.Success)?.data ?: emptyList()
        val roomId = sim.insideHouse?.id
        val roomName = sim.insideHouse?.name
        val matched = owned.firstOrNull { it.id == roomId || it.customName == roomName }
            ?: owned.firstOrNull()
        if (matched == null) {
            showBanner("Public building cannot be sold! Buy a House or Land Plot (ज़मीन) first.")
            return
        }
        sellProperty(matched)
    }

    fun sendFriendRequest(target: PlayerProfile) {
        if (isGuestMode) {
            showBanner("Link Google or Facebook to send multiplayer friend requests.")
            return
        }
        viewModelScope.launch {
            val me = (playerState.value as? UiState.Success)?.data ?: return@launch
            val res = gameRepo.sendFriendRequest(currentUserId, me.username, target.userId, target.username)
            if (res.isSuccess) showBanner("Friend request sent to ${target.username}!")
        }
    }

    fun respondFriendRequest(friendshipId: String, accept: Boolean) {
        if (isGuestMode) return
        viewModelScope.launch {
            gameRepo.respondFriendRequest(friendshipId, accept)
            showBanner(if (accept) "Friend request accepted!" else "Friend request declined.")
        }
    }

    fun startBountyAgainstFriend(targetUid: String, targetName: String) {
        if (isGuestMode) {
            showBanner("Link Google or Facebook to launch multiplayer bounties.")
            return
        }
        viewModelScope.launch {
            val me = (playerState.value as? UiState.Success)?.data ?: return@launch
            val existingBounties = (bountiesState.value as? UiState.Success)?.data ?: emptyList()
            val now = System.currentTimeMillis()
            val blocked = existingBounties.any {
                targetUid in it.participantUids && (it.status == "active" || it.cooldownUntilEpochMs > now)
            }
            if (blocked) {
                showBanner("Bounty cooldown active for $targetName (prevents farming).")
                return@launch
            }
            val res = gameRepo.createBountyMission(currentUserId, me.username, targetUid, targetName)
            if (res.isSuccess) {
                showBanner("BOUNTY STARTED! Eliminate $targetName for 50 Credits!")
            }
        }
    }

    fun resolveBountyDuel(bounty: BountyContract, hunterWon: Boolean) {
        if (isGuestMode) return
        viewModelScope.launch {
            val winnerUid = if (hunterWon) bounty.hunterUid else bounty.targetUid
            val res = gameRepo.resolveBountyMission(bounty.id, hunterWon, winnerUid)
            if (res.isSuccess) {
                val reward = if (winnerUid == currentUserId) {
                    if (hunterWon) 50L else 25L
                } else 0L
                if (reward > 0L) {
                    gameRepo.awardEconomyAndCombat(currentUserId, 0L, reward, 150L, 1L)
                    showBanner("Bounty Victory! Earned +$reward Credits & +150 XP!")
                } else {
                    showBanner("Bounty ended: ${if (hunterWon) bounty.hunterName else bounty.targetName} prevailed.")
                }
            }
        }
    }

    fun onGameplayEntered() {
        if (initialAiTaskEnsured) return
        initialAiTaskEnsured = true
        val existing = (missionsState.value as? UiState.Success)?.data.orEmpty()
        if (existing.none { it.aiGenerated && !it.claimed } && _aiMissionState.value !is UiState.Loading) {
            generateAiMission()
        }
    }

    fun generateAiMission() {
        viewModelScope.launch {
            _aiMissionState.value = UiState.Loading
            val me = (playerState.value as? UiState.Success)?.data
            val res = aiRepo.generateDynamicMission(
                playerName = me?.username ?: "Guest_Shinobi",
                level = me?.level ?: 52L,
                district = _hudSnapshot.value.district,
                activePet = sim.petType
            )
            if (res.isSuccess) {
                val spec = res.getOrThrow()
                val missionId = "ai_${System.currentTimeMillis()}"
                val mission = MissionProgress(
                    id = missionId,
                    userId = currentUserId,
                    title = spec.title,
                    description = spec.description,
                    category = spec.category,
                    targetCount = spec.targetCount,
                    progressCount = 0L,
                    rewardCoins = spec.rewardCoins,
                    rewardXp = spec.rewardXp,
                    aiGenerated = true
                )
                if (isGuestMode) {
                    val list = (guestMissionsFlow.value as? UiState.Success)?.data?.toMutableList() ?: mutableListOf()
                    list.add(0, mission)
                    guestMissionsFlow.value = UiState.Success(list)
                } else {
                    gameRepo.saveMission(currentUserId, missionId, mission)
                }
                _aiMissionState.value = UiState.Success("Active AI Task: ${spec.title}")
            } else {
                _aiMissionState.value = UiState.Error(
                    res.exceptionOrNull()?.localizedMessage ?: "AI mission generation failed."
                )
            }
        }
    }

    fun claimMissionReward(mission: MissionProgress) {
        if (!mission.completed || mission.claimed) return
        val aiPointsGain = if (mission.aiGenerated || mission.category == "ai") {
            10L + mission.targetCount * 2L
        } else {
            5L
        }
        viewModelScope.launch {
            if (isGuestMode) {
                val list = (guestMissionsFlow.value as? UiState.Success)?.data ?: emptyList()
                guestMissionsFlow.value = UiState.Success(
                    list.map { if (it.id == mission.id) it.copy(claimed = true) else it }
                )
                updateGuestProfile {
                    it.copy(
                        coins = it.coins + mission.rewardCoins,
                        diamonds = it.diamonds + 15L,
                        xp = it.xp + mission.rewardXp,
                        upgradePoints = it.upgradePoints + aiPointsGain
                    )
                }
            } else {
                gameRepo.updateMissionProgress(currentUserId, mission.id, mission.progressCount, true, true)
                gameRepo.awardEconomyAndCombat(
                    uid = currentUserId,
                    coinsDelta = mission.rewardCoins,
                    diamondsDelta = 15L,
                    xpDelta = mission.rewardXp,
                    killsDelta = 0L,
                    upgradePointsDelta = aiPointsGain
                )
            }
            _aiTasksCompletedCount.value += 1
            IndraAudioEngine.playPickup()
            showBanner("Task Done! +$aiPointsGain AI Pts • +${mission.rewardCoins} Coins • +${mission.rewardXp} XP!")
            if (mission.aiGenerated) {
                generateAiMission()
            }
        }
    }

    fun advanceMissionDemo(mission: MissionProgress) {
        if (mission.claimed) return
        viewModelScope.launch {
            val next = (mission.progressCount + 1L).coerceAtMost(mission.targetCount)
            val done = next >= mission.targetCount
            if (isGuestMode) {
                val list = (guestMissionsFlow.value as? UiState.Success)?.data ?: emptyList()
                guestMissionsFlow.value = UiState.Success(
                    list.map { if (it.id == mission.id) it.copy(progressCount = next, completed = done) else it }
                )
            } else {
                gameRepo.updateMissionProgress(currentUserId, mission.id, next, done, false)
            }
        }
    }

    fun askNpcAiDialogue(npc: NpcEntity) {
        viewModelScope.launch {
            _aiDialogueState.value = UiState.Loading
            val res = aiRepo.generateNpcOrProxyDialogue(
                speakerRole = npc.role.label,
                district = WorldMapData.resolveDistrict(npc.x, npc.z),
                playerActionContext = "Player approached ${npc.displayName} while carrying weapon ${sim.activeWeaponId}"
            )
            _aiDialogueState.value = if (res.isSuccess) {
                UiState.Success(res.getOrThrow())
            } else {
                UiState.Error(res.exceptionOrNull()?.localizedMessage ?: "AI NPC unavailable.")
            }
        }
    }

    fun sendChat(text: String) {
        val me = (playerState.value as? UiState.Success)?.data
        val filtered = text.replace(Regex("(?i)(hack|cheat|exploit)"), "***")
        if (isGuestMode) {
            val list = (guestChatFlow.value as? UiState.Success)?.data?.toMutableList() ?: mutableListOf()
            list.add(
                ChatMessage(
                    id = "guest_msg_${System.currentTimeMillis()}",
                    roomId = "global_world",
                    senderUid = currentUserId,
                    senderName = me?.username ?: "Guest_Shinobi",
                    text = filtered.trim().take(240),
                    messageType = "text"
                )
            )
            guestChatFlow.value = UiState.Success(list.takeLast(30))
            return
        }
        viewModelScope.launch {
            gameRepo.sendChatMessage(currentUserId, me?.username ?: "Harsh007", filtered)
        }
    }

    fun updateGraphicsAndEnv(preset: GraphicsPreset, time: TimeOfDayMode, weather: WeatherMode) {
        sim.graphicsPreset = preset
        sim.timeOfDay = time
        sim.weatherMode = weather
        val cur = _hudSettingsBundle.value
        val updatedGameplay = cur.gameplay.copy(graphicsPreset = preset)
        _hudSettingsBundle.value = cur.copy(gameplay = updatedGameplay)
        localHudStore?.save(currentUserId, _hudSettingsBundle.value)
        showBanner("Applied ${preset.label} Graphics • ${time.label} • ${weather.label}")
    }

    // --- HUD Customization & Complete Control Settings ---

    fun openHudEditor() {
        IndraAudioEngine.playUiTap()
        _activePanel.value = GamePanel.NONE
        _hudEditorOpen.value = true
    }

    fun closeHudEditor() {
        IndraAudioEngine.playUiTap()
        _hudEditorOpen.value = false
    }

    fun saveHudSettingsBundle(bundle: PlayerHudSettingsBundle) {
        val clean = bundle.copy(userId = currentUserId, gameplay = bundle.gameplay.sanitized())
        _hudSettingsBundle.value = clean
        applyGameplaySettingsToEngine(clean.gameplay)
        localHudStore?.save(currentUserId, clean)
        if (!isGuestMode) {
            viewModelScope.launch { gameRepo.saveHudSettings(currentUserId, clean) }
        }
        showBanner("HUD Layout '${clean.activeLayout.name}' & Controls Saved!")
    }

    fun applyGameplaySettings(gameplay: GameplaySettingsConfig) {
        val cur = _hudSettingsBundle.value
        val clean = gameplay.sanitized()
        val prevLeftHanded = cur.gameplay.leftHandedControls
        val nextLayouts = if (clean.leftHandedControls != prevLeftHanded) {
            cur.layouts.map { it.mirrorLeftRight() }
        } else {
            cur.layouts
        }
        val nextBundle = cur.copy(gameplay = clean, layouts = nextLayouts)
        _hudSettingsBundle.value = nextBundle
        applyGameplaySettingsToEngine(clean)
        localHudStore?.save(currentUserId, nextBundle)
        if (!isGuestMode) {
            viewModelScope.launch { gameRepo.saveHudSettings(currentUserId, nextBundle) }
        }
    }

    fun switchHudLayout(slotIndex: Int) {
        val next = _hudSettingsBundle.value.copy(activeLayoutIndex = slotIndex.coerceIn(0, 4))
        saveHudSettingsBundle(next)
    }

    fun renameHudLayout(slotIndex: Int, newName: String) {
        val idx = slotIndex.coerceIn(0, 4)
        val cleanName = newName.trim().take(24).ifBlank { return }
        val cur = _hudSettingsBundle.value
        val updatedLayouts = cur.layouts.mapIndexed { i, layout ->
            if (i == idx) layout.copy(name = cleanName) else layout
        }
        saveHudSettingsBundle(cur.copy(layouts = updatedLayouts))
    }

    fun resetActiveHudLayoutToDefault(): HudLayoutProfile {
        val cur = _hudSettingsBundle.value
        val idx = cur.activeLayoutIndex.coerceIn(0, 4)
        val resetLayout = HudLayoutPresets.createPreset(idx)
        saveHudSettingsBundle(cur.withUpdatedActiveLayout(resetLayout))
        return resetLayout
    }

    private fun applyGameplaySettingsToEngine(g: GameplaySettingsConfig) {
        sim.graphicsPreset = g.graphicsPreset
        sim.targetFps = g.targetFps
        sim.cameraSensitivity = g.cameraSensitivity
        sim.aimSensitivity = g.aimSensitivity
        sim.scopeSensitivity = g.scopeSensitivity
        sim.joystickSensitivity = g.joystickSensitivity
        sim.autoSprint = g.autoSprint
        sim.aimAssist = g.aimAssist
        sim.cameraDistanceScale = g.cameraDistanceScale
        sim.vibrationEnabled = g.vibrationEnabled
        sim.autoPickup = g.autoPickup
        sim.leftHandedControls = g.leftHandedControls
        IndraAudioEngine.setVolumes(g.musicVolume, g.soundVolume, g.voiceChatVolume)
    }

    private suspend fun incrementMissionsByCategory(category: String, delta: Long) {
        val list = (missionsState.value as? UiState.Success)?.data ?: return
        if (isGuestMode) {
            guestMissionsFlow.value = UiState.Success(
                list.map { m ->
                    if (!m.completed && (m.category == category || m.category == "ai")) {
                        val next = (m.progressCount + delta).coerceAtMost(m.targetCount)
                        m.copy(progressCount = next, completed = next >= m.targetCount)
                    } else m
                }
            )
            return
        }
        for (m in list) {
            if (!m.completed && (m.category == category || m.category == "ai")) {
                val next = (m.progressCount + delta).coerceAtMost(m.targetCount)
                gameRepo.updateMissionProgress(currentUserId, m.id, next, next >= m.targetCount, false)
            }
        }
    }

    private inline fun updateGuestProfile(transform: (PlayerProfile) -> PlayerProfile) {
        val cur = (guestProfileFlow.value as? UiState.Success)?.data ?: return
        guestProfileFlow.value = UiState.Success(transform(cur))
    }

    private fun parseHexColor(hex: String, fallback: Int): Int =
        try {
            android.graphics.Color.parseColor(hex)
        } catch (_: Exception) {
            fallback
        }

    companion object {
        private const val TAG = "GameVM"

        fun createStarterGuestProfile(guestId: String, displayName: String?): PlayerProfile {
            val shortTag = guestId.takeLast(4).uppercase()
            val name = (displayName ?: "Guest_$shortTag").take(24)
            return PlayerProfile(
                userId = guestId,
                playerCode = "GST-$shortTag",
                username = name,
                guild = "GUEST",
                level = 52L,
                xp = 420L,
                rankTitle = "Guest Shinobi",
                coins = 1250L,
                diamonds = 540L,
                upgradePoints = 6L,
                kills = 5L,
                deaths = 0L,
                onlineStatus = false,
                discoverable = false
            )
        }

        fun createStarterGuestPets(guestId: String): List<OwnedPet> = listOf(
            OwnedPet(
                id = "pet_anime_fox",
                userId = guestId,
                petType = "Fox",
                nickname = "Kurama-Chan",
                rarity = "Mythic",
                level = 5L,
                xp = 120L,
                skinId = "nine_tails_crimson",
                abilityId = "Fox Flame",
                equipped = true,
                evolutionLevel = 1L,
                customTintHex = "",
                auraStyle = "Classic"
            )
        )

        fun createStarterGuestVehicles(guestId: String): List<OwnedVehicle> = listOf(
            OwnedVehicle(
                id = "veh_kitsune_gtr",
                userId = guestId,
                modelId = "Kitsune GT-R",
                category = "supercar",
                paintHex = "#FF1E42",
                skinName = "Sakura Carbon",
                locked = false, // Unlocked by default so guest can test driving immediately
                alarmArmed = false,
                garageSlot = 1L,
                upgradeLevel = 2L
            ),
            OwnedVehicle(
                id = "veh_shinobi_moto",
                userId = guestId,
                modelId = "Shinobi R1",
                category = "motorcycle",
                paintHex = "#00E5FF",
                skinName = "Cyber Blue",
                locked = false,
                alarmArmed = true,
                garageSlot = 2L,
                upgradeLevel = 1L
            )
        )

        fun createStarterGuestProperties(guestId: String): List<OwnedProperty> = emptyList()

        fun createStarterGuestMissions(guestId: String): List<MissionProgress> = listOf(
            MissionProgress(
                id = "mis_daily_patrol",
                userId = guestId,
                title = "Central City Patrol",
                description = "Defeat 3 rogue cyber-ninjas patrolling Central Anime City.",
                category = "daily",
                targetCount = 3L,
                progressCount = 0L,
                rewardCoins = 150L,
                rewardXp = 250L
            ),
            MissionProgress(
                id = "mis_driving_bazaar",
                userId = guestId,
                title = "Commercial Drift Run",
                description = "Drive any vehicle into the Commercial Region district.",
                category = "driving",
                targetCount = 1L,
                progressCount = 0L,
                rewardCoins = 120L,
                rewardXp = 180L
            ),
            MissionProgress(
                id = "mis_pet_bond",
                userId = guestId,
                title = "Companion Synergy",
                description = "Activate your Pet Ability in open-world combat.",
                category = "pet",
                targetCount = 2L,
                progressCount = 0L,
                rewardCoins = 100L,
                rewardXp = 160L
            )
        )
    }
}
