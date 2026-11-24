package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BountyContract
import com.example.data.model.ChatMessage
import com.example.data.model.Friendship
import com.example.data.model.MissionProgress
import com.example.data.model.OwnedPet
import com.example.data.model.OwnedProperty
import com.example.data.model.OwnedVehicle
import com.example.data.model.PetTradeOffer
import com.example.data.model.PlayerHudSettingsBundle
import com.example.data.model.PlayerProfile
import com.example.security.OperationType
import com.example.security.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class IndraGameRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun unwrapAndLogFlowError(error: Throwable, op: OperationType, path: String, expectedUid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }

    // --- 1. Bootstrap Starter Profile & Collections ---

    suspend fun ensureStarterData(uid: String, displayName: String?): Result<Unit> {
        val playerPath = "players/$uid"
        return try {
            val currentUid = requireUserId()
            check(currentUid == uid) { "Signed-in user mismatch" }

            val docRef = db.collection("players").document(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val safeName = (displayName ?: "Harsh007")
                    .replace(Regex("[^a-zA-Z0-9_\\- ]"), "")
                    .trim()
                    .take(24)
                    .ifBlank { "ShinobiHero" }
                val playerCode = (100000000L + (uid.hashCode().toLong() and 0x7FFFFFFFL) % 899999999L).toString()

                val starter = PlayerProfile(
                    userId = uid,
                    playerCode = playerCode,
                    username = safeName,
                    guild = "SHINOBI",
                    level = 52L,
                    xp = 420L,
                    rankTitle = "Grand Shinobi",
                    coins = 1250L,
                    diamonds = 540L,
                    upgradePoints = 6L,
                    kills = 5L,
                    deaths = 0L
                )
                docRef.set(starter.toCreateMap()).await()

                // Seed starter pet (Fox #1)
                val pet = OwnedPet(
                    userId = uid,
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
                db.collection("users").document(uid).collection("pets")
                    .document("pet_anime_fox").set(pet.toCreateMap()).await()

                // Seed starter vehicles
                val supercar = OwnedVehicle(
                    userId = uid,
                    modelId = "Kitsune GT-R",
                    category = "supercar",
                    paintHex = "#FF1E42",
                    skinName = "Sakura Carbon",
                    locked = true,
                    alarmArmed = true,
                    garageSlot = 1L,
                    upgradeLevel = 2L
                )
                val moto = OwnedVehicle(
                    userId = uid,
                    modelId = "Shinobi R1",
                    category = "motorcycle",
                    paintHex = "#00E5FF",
                    skinName = "Cyber Blue",
                    locked = false,
                    alarmArmed = true,
                    garageSlot = 2L,
                    upgradeLevel = 1L
                )
                db.collection("users").document(uid).collection("vehicles")
                    .document("veh_kitsune_gtr").set(supercar.toCreateMap()).await()
                db.collection("users").document(uid).collection("vehicles")
                    .document("veh_shinobi_moto").set(moto.toCreateMap()).await()

                // New players start with 0 houses — they can explore, locate & buy houses or land plots in-world!

                // Seed starter missions
                val starterMissions = listOf(
                    "mis_daily_patrol" to MissionProgress(
                        userId = uid,
                        title = "Central City Patrol",
                        description = "Defeat 3 rogue cyber-ninjas patrolling Central Anime City.",
                        category = "daily",
                        targetCount = 3L,
                        progressCount = 0L,
                        rewardCoins = 150L,
                        rewardXp = 250L
                    ),
                    "mis_driving_bazaar" to MissionProgress(
                        userId = uid,
                        title = "Commercial Drift Run",
                        description = "Drive any vehicle into the Commercial Region district.",
                        category = "driving",
                        targetCount = 1L,
                        progressCount = 0L,
                        rewardCoins = 120L,
                        rewardXp = 180L
                    ),
                    "mis_pet_bond" to MissionProgress(
                        userId = uid,
                        title = "Companion Synergy",
                        description = "Activate your Pet Ability in open-world combat.",
                        category = "pet",
                        targetCount = 2L,
                        progressCount = 0L,
                        rewardCoins = 100L,
                        rewardXp = 160L
                    )
                )
                for ((id, mission) in starterMissions) {
                    db.collection("users").document(uid).collection("missions")
                        .document(id).set(mission.toCreateMap()).await()
                }
            } else {
                // Reconnect: stop offline AI proxy and mark online
                docRef.update(
                    mapOf(
                        "onlineStatus" to true,
                        "aiProxyActive" to false,
                        "aiProxyLastAction" to "Player reconnected and resumed direct control",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, playerPath)
            Result.failure(e)
        }
    }

    // --- 2. Real-time Observations ---

    fun observePlayerProfile(uid: String): Flow<PlayerProfile?> = flow {
        val currentUid = requireUserId()
        check(currentUid == uid)
        val path = "players/$uid"
        emitAll(
            db.collection("players").document(uid)
                .snapshots()
                .map { it.toObject(PlayerProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    unwrapAndLogFlowError(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    fun observeDiscoverablePlayers(uid: String): Flow<List<PlayerProfile>> = flow {
        requireUserId()
        val path = "players"
        emitAll(
            db.collection("players")
                .whereEqualTo("discoverable", true)
                .limit(25)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PlayerProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.userId != uid }
                }
                .catch { error ->
                    unwrapAndLogFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observePets(uid: String): Flow<List<OwnedPet>> = flow {
        requireUserId()
        val path = "users/$uid/pets"
        emitAll(
            db.collection("users").document(uid).collection("pets")
                .snapshots()
                .map { it.toObjects(OwnedPet::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    unwrapAndLogFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeVehicles(uid: String): Flow<List<OwnedVehicle>> = flow {
        requireUserId()
        val path = "users/$uid/vehicles"
        emitAll(
            db.collection("users").document(uid).collection("vehicles")
                .snapshots()
                .map { it.toObjects(OwnedVehicle::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    unwrapAndLogFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeProperties(uid: String): Flow<List<OwnedProperty>> = flow {
        requireUserId()
        val path = "users/$uid/properties"
        emitAll(
            db.collection("users").document(uid).collection("properties")
                .snapshots()
                .map { it.toObjects(OwnedProperty::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    unwrapAndLogFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeMissions(uid: String): Flow<List<MissionProgress>> = flow {
        requireUserId()
        val path = "users/$uid/missions"
        emitAll(
            db.collection("users").document(uid).collection("missions")
                .snapshots()
                .map { it.toObjects(MissionProgress::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    unwrapAndLogFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeFriendships(uid: String): Flow<List<Friendship>> = flow {
        requireUserId()
        val path = "friendships"
        emitAll(
            db.collection("friendships")
                .whereArrayContains("participantUids", uid)
                .snapshots()
                .map { it.toObjects(Friendship::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    unwrapAndLogFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeBounties(uid: String): Flow<List<BountyContract>> = flow {
        requireUserId()
        val path = "bounties"
        emitAll(
            db.collection("bounties")
                .whereArrayContains("participantUids", uid)
                .snapshots()
                .map { it.toObjects(BountyContract::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    unwrapAndLogFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeWorldChat(uid: String): Flow<List<ChatMessage>> = flow {
        requireUserId()
        val path = "chat_messages"
        emitAll(
            db.collection("chat_messages")
                .whereEqualTo("roomId", "global_world")
                .limit(40)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
                        .takeLast(30)
                }
                .catch { error ->
                    unwrapAndLogFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    // --- 3. Player State & Offline AI Proxy Sync ---

    suspend fun syncPlayerTransformAndStats(
        uid: String,
        district: String,
        posX: Double,
        posY: Double,
        posZ: Double,
        rotY: Double,
        hp: Double,
        ep: Double,
        shield: Double
    ): Result<Unit> {
        val path = "players/$uid"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf(
                    "worldDistrict" to district.take(50),
                    "posX" to posX.coerceIn(-1200.0, 1200.0),
                    "posY" to posY.coerceIn(-50.0, 400.0),
                    "posZ" to posZ.coerceIn(-1200.0, 1200.0),
                    "rotY" to rotY,
                    "hp" to hp.coerceIn(0.0, 1000.0),
                    "ep" to ep.coerceIn(0.0, 1000.0),
                    "shield" to shield.coerceIn(0.0, 1000.0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePlayerLocation(
        uid: String,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Double? = null,
        trackingEnabled: Boolean = true
    ): Result<Unit> {
        val path = "players/$uid"
        return try {
            val currentUid = requireUserId()
            check(currentUid == uid) { "Signed-in user mismatch" }
            require(latitude.isFinite() && latitude in -90.0..90.0) { "Invalid latitude" }
            require(longitude.isFinite() && longitude in -180.0..180.0) { "Invalid longitude" }
            val payload = buildMap<String, Any> {
                put("latitude", latitude)
                put("longitude", longitude)
                accuracyMeters?.takeIf { it.isFinite() }?.let {
                    put("locationAccuracyMeters", it.coerceIn(0.0, 50000.0))
                }
                put("locationTrackingEnabled", trackingEnabled)
                put("updatedAt", FieldValue.serverTimestamp())
            }
            db.document(path).update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun setLocationTrackingEnabled(uid: String, enabled: Boolean): Result<Unit> {
        val path = "players/$uid"
        return try {
            val currentUid = requireUserId()
            check(currentUid == uid) { "Signed-in user mismatch" }
            db.document(path).update(
                mapOf(
                    "locationTrackingEnabled" to enabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleOfflineAiProxyMode(
        uid: String,
        enableProxy: Boolean,
        proxyActionSummary: String
    ): Result<Unit> {
        val path = "players/$uid"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf(
                    "onlineStatus" to !enableProxy,
                    "aiProxyActive" to enableProxy,
                    "aiProxyLastAction" to proxyActionSummary.take(140),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateCustomizationAndLoadout(
        uid: String,
        username: String,
        hairStyle: String,
        outfitStyle: String,
        accessoryStyle: String = "Katana",
        accentColorHex: String,
        equippedAbilities: List<String>,
        activeWeaponId: String,
        activePetId: String,
        activeVehicleId: String
    ): Result<Unit> {
        val path = "players/$uid"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf(
                    "username" to username.trim().take(30).ifBlank { "ShinobiHero" },
                    "hairStyle" to hairStyle.take(40),
                    "outfitStyle" to outfitStyle.take(40),
                    "accessoryStyle" to accessoryStyle.take(40),
                    "accentColorHex" to accentColorHex.take(16),
                    "equippedAbilities" to equippedAbilities.take(8),
                    "activeWeaponId" to activeWeaponId.take(64),
                    "activePetId" to activePetId.take(64),
                    "activeVehicleId" to activeVehicleId.take(64),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun loadHudSettings(uid: String): Result<PlayerHudSettingsBundle?> {
        val path = "users/$uid/hud_settings/default"
        return try {
            requireUserId()
            val snap = db.document(path).get().await()
            if (!snap.exists()) return Result.success(null)
            val payloadJson = snap.getString("payloadJson")
            Result.success(PlayerHudSettingsBundle.fromJsonString(payloadJson, uid))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun saveHudSettings(uid: String, bundle: PlayerHudSettingsBundle): Result<Unit> {
        val path = "users/$uid/hud_settings/default"
        return try {
            requireUserId()
            val ref = db.document(path)
            val existing = ref.get().await()
            if (existing.exists()) {
                ref.update(bundle.toFirestoreUpdateMap()).await()
            } else {
                ref.set(bundle.toFirestoreCreateMap(uid)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun awardEconomyAndCombat(
        uid: String,
        coinsDelta: Long,
        diamondsDelta: Long,
        xpDelta: Long,
        killsDelta: Long,
        upgradePointsDelta: Long = 0L
    ): Result<Unit> {
        val path = "players/$uid"
        return try {
            requireUserId()
            val snap = db.document(path).get().await()
            val currentCoins = (snap.getLong("coins") ?: 0L)
            val currentDiamonds = (snap.getLong("diamonds") ?: 0L)
            val currentXp = (snap.getLong("xp") ?: 0L) + xpDelta.coerceAtLeast(0L)
            val currentLevel = (snap.getLong("level") ?: 1L)
            val levelGain = currentXp / 1000L
            val newLevel = (currentLevel + levelGain).coerceIn(1L, 999L)
            val newXp = currentXp % 1000L
            val newCoins = (currentCoins + coinsDelta).coerceAtLeast(0L)
            val newDiamonds = (currentDiamonds + diamondsDelta).coerceAtLeast(0L)
            val newKills = ((snap.getLong("kills") ?: 0L) + killsDelta).coerceAtLeast(0L)
            val newUp = ((snap.getLong("upgradePoints") ?: 0L) + levelGain + upgradePointsDelta).coerceAtLeast(0L)

            db.document(path).update(
                mapOf(
                    "coins" to newCoins,
                    "diamonds" to newDiamonds,
                    "xp" to newXp,
                    "level" to newLevel,
                    "kills" to newKills,
                    "upgradePoints" to newUp,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- 4. Pets, Vehicles (Car Key System), Properties, Missions ---

    suspend fun unlockOrUpgradePet(
        uid: String,
        petId: String,
        pet: OwnedPet,
        coinCost: Long
    ): Result<Unit> {
        val petPath = "users/$uid/pets/$petId"
        return try {
            requireUserId()
            if (coinCost > 0L) {
                awardEconomyAndCombat(uid, -coinCost, 0L, 40L, 0L).getOrThrow()
            }
            val ref = db.document(petPath)
            val snap = ref.get().await()
            if (snap.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "level" to pet.level.coerceIn(1L, 100L),
                    "xp" to pet.xp.coerceAtLeast(0L),
                    "equipped" to pet.equipped,
                    "evolutionLevel" to pet.evolutionLevel.coerceIn(1L, 3L),
                    "abilityId" to pet.abilityId.take(40),
                    "nickname" to pet.nickname.take(30).ifBlank { pet.petType.take(30) },
                    "auraStyle" to pet.auraStyle.take(24).ifBlank { "Classic" },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (pet.customTintHex.isNotBlank()) updates["customTintHex"] = pet.customTintHex.take(16)
                ref.update(updates).await()
            } else {
                ref.set(pet.copy(userId = uid).toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, petPath)
            Result.failure(e)
        }
    }

    suspend fun customizePet(
        uid: String,
        petId: String,
        nickname: String,
        customTintHex: String,
        auraStyle: String
    ): Result<Unit> {
        val petPath = "users/$uid/pets/$petId"
        return try {
            requireUserId()
            db.document(petPath).update(
                mapOf(
                    "nickname" to nickname.trim().take(30).ifBlank { "Companion" },
                    "customTintHex" to customTintHex.take(16),
                    "auraStyle" to auraStyle.take(24).ifBlank { "Classic" },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, petPath)
            Result.failure(e)
        }
    }

    suspend fun setEquippedPet(uid: String, petId: String, equippedInRoster: Boolean = true, setAsMainFollower: Boolean = true): Result<Unit> {
        val petPath = "users/$uid/pets/$petId"
        return try {
            requireUserId()
            db.document(petPath).update(
                mapOf(
                    "equipped" to equippedInRoster,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (setAsMainFollower) {
                db.document("players/$uid").update(
                    mapOf(
                        "activePetId" to petId.take(64),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, petPath)
            Result.failure(e)
        }
    }

    suspend fun unlockAbilitySlot(
        uid: String,
        nextCharSlots: Long,
        nextPetSlots: Long,
        upgradePointCost: Long,
        coinCost: Long,
        equippedAbilities: List<String>
    ): Result<Unit> {
        val path = "players/$uid"
        return try {
            requireUserId()
            val snap = db.document(path).get().await()
            val curUp = snap.getLong("upgradePoints") ?: 0L
            val curCoins = snap.getLong("coins") ?: 0L
            db.document(path).update(
                mapOf(
                    "unlockedCharSlots" to nextCharSlots.coerceIn(2L, 3L),
                    "unlockedPetSlots" to nextPetSlots.coerceIn(2L, 4L),
                    "upgradePoints" to (curUp - upgradePointCost).coerceAtLeast(0L),
                    "coins" to (curCoins - coinCost).coerceAtLeast(0L),
                    "equippedAbilities" to equippedAbilities.take(8),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observePetTrades(uid: String): Flow<List<PetTradeOffer>> = flow {
        requireUserId()
        val path = "pet_trades"
        emitAll(
            db.collection("pet_trades")
                .whereArrayContains("participantUids", uid)
                .snapshots()
                .map { it.toObjects(PetTradeOffer::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    unwrapAndLogFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun createPetTradeOffer(
        senderUid: String,
        senderName: String,
        receiverUid: String,
        receiverName: String,
        offeredPet: OwnedPet,
        requestedPetType: String
    ): Result<String> {
        val id = "trade_${System.currentTimeMillis()}"
        val path = "pet_trades/$id"
        return try {
            requireUserId()
            val offer = PetTradeOffer(
                senderUid = senderUid,
                senderName = senderName.take(30),
                receiverUid = receiverUid,
                receiverName = receiverName.take(30),
                participantUids = listOf(senderUid, receiverUid),
                offeredPetId = offeredPet.id,
                offeredPetType = offeredPet.petType.take(40),
                offeredEvolutionLevel = offeredPet.evolutionLevel.coerceIn(1L, 3L),
                requestedPetType = requestedPetType.take(40),
                status = "pending"
            )
            db.document(path).set(offer.toCreateMap()).await()
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun respondPetTradeOffer(
        uid: String,
        trade: PetTradeOffer,
        accept: Boolean,
        receivedPetTemplate: OwnedPet?
    ): Result<Unit> {
        val path = "pet_trades/${trade.id}"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf(
                    "status" to if (accept) "accepted" else "declined",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (accept && receivedPetTemplate != null) {
                db.collection("users").document(uid).collection("pets")
                    .document(receivedPetTemplate.id)
                    .set(receivedPetTemplate.copy(userId = uid).toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleVehicleLockOrStealState(
        uid: String,
        vehicleId: String,
        locked: Boolean,
        alarmArmed: Boolean,
        paintHex: String? = null
    ): Result<Unit> {
        val path = "users/$uid/vehicles/$vehicleId"
        return try {
            requireUserId()
            val updates = mutableMapOf<String, Any>(
                "locked" to locked,
                "alarmArmed" to alarmArmed,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (paintHex != null) updates["paintHex"] = paintHex.take(16)
            db.document(path).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun buyVehicle(uid: String, vehicleId: String, vehicle: OwnedVehicle, coinCost: Long): Result<Unit> {
        val path = "users/$uid/vehicles/$vehicleId"
        return try {
            requireUserId()
            if (coinCost > 0L) {
                awardEconomyAndCombat(uid, -coinCost, 0L, 80L, 0L).getOrThrow()
            }
            db.document(path).set(vehicle.copy(userId = uid).toCreateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun buyOrUpgradeProperty(
        uid: String,
        propertyId: String,
        property: OwnedProperty,
        coinCost: Long
    ): Result<Unit> {
        val path = "users/$uid/properties/$propertyId"
        return try {
            requireUserId()
            if (coinCost > 0L) {
                awardEconomyAndCombat(uid, -coinCost, 0L, 120L, 0L).getOrThrow()
            }
            val ref = db.document(path)
            val snap = ref.get().await()
            if (snap.exists()) {
                ref.update(
                    mapOf(
                        "customName" to property.customName.take(50),
                        "upgradeLevel" to property.upgradeLevel.coerceIn(1L, 10L),
                        "decorTheme" to property.decorTheme.take(40),
                        "furnitureCount" to property.furnitureCount.coerceIn(0L, 200L),
                        "garageCapacity" to property.garageCapacity.coerceIn(1L, 20L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(property.copy(userId = uid).toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun sellProperty(
        uid: String,
        propertyId: String,
        refundCoins: Long
    ): Result<Unit> {
        val path = "users/$uid/properties/$propertyId"
        return try {
            requireUserId()
            db.document(path).delete().await()
            if (refundCoins > 0L) {
                awardEconomyAndCombat(uid, refundCoins, 0L, 20L, 0L).getOrThrow()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveMission(uid: String, missionId: String, mission: MissionProgress): Result<Unit> {
        val path = "users/$uid/missions/$missionId"
        return try {
            requireUserId()
            db.document(path).set(mission.copy(userId = uid).toCreateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateMissionProgress(
        uid: String,
        missionId: String,
        progressCount: Long,
        completed: Boolean,
        claimed: Boolean
    ): Result<Unit> {
        val path = "users/$uid/missions/$missionId"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf(
                    "progressCount" to progressCount.coerceAtLeast(0L),
                    "completed" to completed,
                    "claimed" to claimed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- 5. Friendships & PvP Bounty ("Eliminate My Friend") ---

    suspend fun sendFriendRequest(
        requesterUid: String,
        requesterName: String,
        targetUid: String,
        targetName: String
    ): Result<Unit> {
        val id = "${requesterUid.take(20)}_${targetUid.take(20)}"
        val path = "friendships/$id"
        return try {
            requireUserId()
            val friendship = Friendship(
                requesterUid = requesterUid,
                targetUid = targetUid,
                requesterName = requesterName.take(30),
                targetName = targetName.take(30),
                participantUids = listOf(requesterUid, targetUid),
                status = "pending"
            )
            db.document(path).set(friendship.toCreateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun respondFriendRequest(friendshipId: String, accept: Boolean): Result<Unit> {
        val path = "friendships/$friendshipId"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf(
                    "status" to if (accept) "accepted" else "declined",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createBountyMission(
        hunterUid: String,
        hunterName: String,
        targetUid: String,
        targetName: String
    ): Result<String> {
        val id = "bounty_${System.currentTimeMillis()}"
        val path = "bounties/$id"
        return try {
            requireUserId()
            val contract = BountyContract(
                hunterUid = hunterUid,
                hunterName = hunterName.take(30),
                targetUid = targetUid,
                targetName = targetName.take(30),
                participantUids = listOf(hunterUid, targetUid),
                rewardHunter = 50L,
                rewardCounter = 25L,
                status = "active",
                cooldownUntilEpochMs = System.currentTimeMillis() + 180_000L
            )
            db.document(path).set(contract.toCreateMap()).await()
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun resolveBountyMission(
        bountyId: String,
        hunterWon: Boolean,
        winnerUid: String
    ): Result<Unit> {
        val path = "bounties/$bountyId"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf(
                    "status" to if (hunterWon) "hunter_won" else "target_won",
                    "winnerUid" to winnerUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- 6. World Chat with Safety Filter ---

    suspend fun sendChatMessage(uid: String, senderName: String, rawText: String): Result<Unit> {
        val msgId = "msg_${System.currentTimeMillis()}_${uid.take(6)}"
        val path = "chat_messages/$msgId"
        return try {
            requireUserId()
            val cleanText = rawText.trim().take(240)
            require(cleanText.isNotEmpty()) { "Message cannot be empty." }
            val msg = ChatMessage(
                roomId = "global_world",
                senderUid = uid,
                senderName = senderName.take(30),
                text = cleanText,
                messageType = "text"
            )
            db.document(path).set(msg.toCreateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- 7. Guest Session Linking & Progress Migration ---

    suspend fun migrateGuestProgress(
        uid: String,
        displayName: String?,
        snapshot: GuestProgressSnapshot
    ): Result<Unit> {
        val playerPath = "players/$uid"
        return try {
            val currentUid = requireUserId()
            check(currentUid == uid) { "Signed-in user mismatch" }

            val docRef = db.collection("players").document(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val safeName = (displayName ?: snapshot.profile.username)
                    .replace(Regex("[^a-zA-Z0-9_\\- ]"), "")
                    .trim()
                    .take(24)
                    .ifBlank { "ShinobiHero" }
                val playerCode = (100000000L + (uid.hashCode().toLong() and 0x7FFFFFFFL) % 899999999L).toString()

                val migratedProfile = snapshot.profile.copy(
                    userId = uid,
                    playerCode = playerCode,
                    username = safeName,
                    onlineStatus = true,
                    aiProxyActive = false
                )
                docRef.set(migratedProfile.toCreateMap()).await()

                for (pet in snapshot.pets) {
                    val petId = pet.id.ifBlank { "pet_${pet.petType.lowercase().replace(" ", "_")}" }
                    db.collection("users").document(uid).collection("pets")
                        .document(petId).set(pet.copy(userId = uid).toCreateMap()).await()
                }
                for (veh in snapshot.vehicles) {
                    val vehId = veh.id.ifBlank { "veh_${veh.modelId.lowercase().replace(" ", "_").replace("-", "_")}" }
                    db.collection("users").document(uid).collection("vehicles")
                        .document(vehId).set(veh.copy(userId = uid).toCreateMap()).await()
                }
                for (prop in snapshot.properties) {
                    val propId = prop.id.ifBlank { "prop_${prop.propertyType}" }
                    db.collection("users").document(uid).collection("properties")
                        .document(propId).set(prop.copy(userId = uid).toCreateMap()).await()
                }
                for (mission in snapshot.missions) {
                    val mId = mission.id.ifBlank { "mis_${System.currentTimeMillis()}" }
                    db.collection("users").document(uid).collection("missions")
                        .document(mId).set(mission.copy(userId = uid).toCreateMap()).await()
                }
            } else {
                // Merge guest-earned currency/XP safely onto existing cloud profile
                docRef.update(
                    mapOf(
                        "coins" to FieldValue.increment(snapshot.earnedCoinsDelta.coerceAtLeast(0L)),
                        "xp" to FieldValue.increment(snapshot.earnedXpDelta.coerceAtLeast(0L)),
                        "kills" to FieldValue.increment(snapshot.earnedKillsDelta.coerceAtLeast(0L)),
                        "onlineStatus" to true,
                        "aiProxyActive" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, playerPath)
            Result.failure(e)
        }
    }
}

data class GuestProgressSnapshot(
    val profile: PlayerProfile,
    val pets: List<OwnedPet>,
    val vehicles: List<OwnedVehicle>,
    val properties: List<OwnedProperty>,
    val missions: List<MissionProgress>,
    val earnedCoinsDelta: Long,
    val earnedXpDelta: Long,
    val earnedKillsDelta: Long
)

