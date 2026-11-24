package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.GameCatalog
import com.example.data.model.GraphicsPreset
import com.example.data.model.HeroAppearanceCatalog
import com.example.data.model.MissionProgress
import com.example.data.model.PlayerProfile
import com.example.data.model.TimeOfDayMode
import com.example.data.model.WeaponType
import com.example.data.model.WeatherMode
import com.example.location.LocationTrackingService
import com.example.game.engine.WorldHudSnapshot
import com.example.game.world.CityLandmarksAndModules
import com.example.game.world.CityZoneId
import com.example.game.world.ForestGenerator
import com.example.game.world.LocationCategory
import com.example.game.world.WorldDistrictId
import com.example.game.world.WorldGenerator
import com.example.game.world.WorldLocation
import com.example.game.world.WorldLocations
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.CyberBlue
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldHp
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyCardElevated
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletSkill
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun FriendsAndBountyPanel(
    viewModel: GameViewModel,
    profile: PlayerProfile?,
    onClose: () -> Unit
) {
    val playersState by viewModel.discoverablePlayersState.collectAsStateWithLifecycle()
    val friendshipsState by viewModel.friendshipsState.collectAsStateWithLifecycle()
    val bountiesState by viewModel.bountiesState.collectAsStateWithLifecycle()
    val inspectedPlayer by viewModel.inspectedPlayer.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    val discoverable = (playersState as? UiState.Success)?.data ?: emptyList()
    val friendships = (friendshipsState as? UiState.Success)?.data ?: emptyList()
    val bounties = (bountiesState as? UiState.Success)?.data ?: emptyList()

    val filteredPlayers = remember(discoverable, searchQuery) {
        if (searchQuery.isBlank()) discoverable
        else discoverable.filter {
            it.username.contains(searchQuery, ignoreCase = true) ||
                it.playerCode.contains(searchQuery, ignoreCase = true)
        }
    }

    PanelShell(title = "Friends, AI Proxy & Bounties", playerProfile = profile, onClose = onClose) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Offline Player AI Proxy Card
            item {
                val proxyOn = profile?.aiProxyActive == true
                Surface(
                    color = NavyCardElevated,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.SmartToy, contentDescription = null, tint = ElectricCyan)
                                Text("Persistent Offline Player AI Proxy", style = MaterialTheme.typography.titleSmall, color = Color.White)
                            }
                            Text(
                                text = profile?.aiProxyLastAction ?: "Saves position, HP, vehicle & protects currencies while disconnected.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = proxyOn,
                            onCheckedChange = { viewModel.toggleOfflineAiProxy(it) }
                        )
                    }
                }
            }

            // Active PvP Bounties ("Eliminate My Friend")
            val activeBounties = bounties.filter { it.status == "active" }
            if (activeBounties.isNotEmpty()) {
                item {
                    Text("Active PvP Bounty Contracts", style = MaterialTheme.typography.titleSmall, color = CrimsonAccent)
                }
                items(activeBounties) { bounty ->
                    val amTarget = bounty.targetUid == viewModel.currentUserId
                    Surface(
                        color = Color(0xFF2B1020),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().border(1.5.dp, GoldCoin, RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.Warning, contentDescription = null, tint = GoldCoin)
                                Text(
                                    text = if (amTarget) "WARNING! ${bounty.hunterName} is targeting you!" else "Eliminate ${bounty.targetName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "Hunter Reward: 50 Credits • Counter-Defend Reward: 25 Credits",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldCoin
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.resolveBountyDuel(bounty, hunterWon = true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent)
                                ) {
                                    Text("Hunter Eliminates (+50c)")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.resolveBountyDuel(bounty, hunterWon = false) }
                                ) {
                                    Text("Target Defends (+25c)", color = ElectricCyan)
                                }
                            }
                        }
                    }
                }
            }

            // Search Players by Username or UID
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Players by Username or UID") },
                    leadingIcon = { Icon(Icons.Filled.PersonSearch, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Pending Friend Requests
            val pending = friendships.filter { it.status == "pending" && it.targetUid == viewModel.currentUserId }
            if (pending.isNotEmpty()) {
                item { Text("Incoming Friend Requests", style = MaterialTheme.typography.titleSmall, color = GoldCoin) }
                items(pending) { req ->
                    Surface(color = NavyCardElevated, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${req.requesterName} wants to squad up", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(onClick = { viewModel.respondFriendRequest(req.id, true) }, colors = ButtonDefaults.buttonColors(containerColor = EmeraldHp, contentColor = Color.Black)) {
                                    Text("Accept")
                                }
                                OutlinedButton(onClick = { viewModel.respondFriendRequest(req.id, false) }) {
                                    Text("Decline", color = CrimsonAccent)
                                }
                            }
                        }
                    }
                }
            }

            // Live Firestore Players Directory
            item {
                Text("Online Players & AI Proxies (${filteredPlayers.size})", style = MaterialTheme.typography.titleSmall, color = ElectricCyan)
            }
            if (filteredPlayers.isEmpty()) {
                item {
                    Surface(color = NavyCardElevated, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Squad Allies in World", style = MaterialTheme.typography.titleSmall, color = GoldCoin)
                            Text("ItachiX (AI Proxy) • NarutoFF (Online) • Riya_Gamer (In Game) are active in Central Anime City. Sign in on a second device to duel live players!", color = TextSecondary)
                        }
                    }
                }
            } else {
                items(filteredPlayers) { other ->
                    Surface(
                        color = NavyCardElevated,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, ElectricCyan.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${other.username} (Lv.${other.level})", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                Text(
                                    text = "UID: ${other.playerCode} • ${if (other.aiProxyActive) "AI Proxy Active" else if (other.onlineStatus) "Online in ${other.worldDistrict}" else "Offline"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricCyan
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick = { viewModel.inspectPlayer(other) }) {
                                    Text("Inspect", color = ElectricCyan)
                                }
                                OutlinedButton(onClick = { viewModel.sendFriendRequest(other) }) {
                                    Text("Add", color = GoldCoin)
                                }
                                Button(
                                    onClick = { viewModel.startBountyAgainstFriend(other.userId, other.username) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent)
                                ) {
                                    Text("Bounty 50c")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (inspectedPlayer != null) {
        val p = inspectedPlayer!!
        AlertDialog(
            onDismissRequest = { viewModel.inspectPlayer(null) },
            containerColor = NavyCard,
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("${p.username} • Profile Inspection") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("UID: ${p.playerCode} • Guild: ${p.guild}", color = ElectricCyan)
                    Text("Level ${p.level} • Rank: ${p.rankTitle}", color = GoldCoin)
                    Text("Outfit: ${p.outfitStyle} • Hair: ${p.hairStyle} • Accessory: ${p.accessoryStyle}")
                    Text("Active Pet: ${p.activePetId} • Weapon: ${p.activeWeaponId}")
                    Text("Abilities: ${p.equippedAbilities.joinToString()}")
                    Text("Combat Record: ${p.kills} Eliminations / ${p.deaths} Respawns")
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.inspectPlayer(null) }) {
                    Text("Close", color = ElectricCyan)
                }
            }
        )
    }
}

@Composable
fun MissionsPanel(
    viewModel: GameViewModel,
    profile: PlayerProfile?,
    onClose: () -> Unit
) {
    val missionsState by viewModel.missionsState.collectAsStateWithLifecycle()
    val aiMissionState by viewModel.aiMissionState.collectAsStateWithLifecycle()
    val aiDoneCount by viewModel.aiTasksCompletedCount.collectAsStateWithLifecycle()
    val missions = (missionsState as? UiState.Success)?.data ?: emptyList()
    val orderedMissions = remember(missions) {
        missions.sortedWith(compareBy<MissionProgress>({ it.claimed }, { !it.aiGenerated }, { !it.completed }))
    }

    PanelShell(title = "Missions & AI Task Master", playerProfile = profile, onClose = onClose) {
        LazyColumn(
            modifier = Modifier.testTag("missions_list"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Surface(
                    color = NavyCardElevated,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, VioletSkill.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = GoldCoin)
                                Text("Sequential AI Task Master", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            }
                            Surface(
                                color = Color(0xFF0F172A),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .border(1.dp, GoldCoin.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                    .testTag("ai_task_points_badge")
                            ) {
                                Text(
                                    text = "AI Points: ${profile?.upgradePoints ?: 0L} • Done: $aiDoneCount",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoldCoin,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        when (val ai = aiMissionState) {
                            is UiState.Loading -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = ElectricCyan)
                                Text("AI assigning next sequential task...", color = ElectricCyan)
                            }
                            is UiState.Success -> Text(ai.data, color = EmeraldHp)
                            is UiState.Error -> Text(ai.message, color = DangerRed)
                            UiState.Idle -> Text("Complete your active AI task to earn AI Points & automatically unlock the next AI task!", color = TextSecondary)
                        }
                        Button(
                            onClick = { viewModel.generateAiMission() },
                            enabled = aiMissionState !is UiState.Loading,
                            colors = ButtonDefaults.buttonColors(containerColor = VioletSkill),
                            modifier = Modifier.testTag("generate_ai_mission_button")
                        ) {
                            Text(if (orderedMissions.any { it.aiGenerated && !it.claimed }) "Request Next AI Task" else "Start AI Task")
                        }
                    }
                }
            }

            items(orderedMissions, key = { it.id }) { mission ->
                val aiPts = if (mission.aiGenerated || mission.category == "ai") 10L + mission.targetCount * 2L else 5L
                Surface(
                    color = NavyCardElevated,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, if (mission.completed) EmeraldHp else if (mission.aiGenerated) VioletSkill else ElectricCyan.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "${if (mission.aiGenerated) "[AI TASK] " else ""}${mission.title} [${mission.category.uppercase()}]",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (mission.aiGenerated) GoldCoin else Color.White
                            )
                            Text(mission.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Text(
                                text = "Progress: ${mission.progressCount}/${mission.targetCount} • Reward: +$aiPts AI Pts + ${mission.rewardCoins}c + ${mission.rewardXp} XP",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan
                            )
                        }
                        if (mission.claimed) {
                            Text("Claimed", style = MaterialTheme.typography.labelMedium, color = EmeraldHp)
                        } else if (mission.completed) {
                            Button(
                                onClick = { viewModel.claimMissionReward(mission) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldHp, contentColor = Color.Black),
                                modifier = Modifier.testTag("claim_mission_${mission.id}")
                            ) {
                                Text("Claim + Next")
                            }
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.advanceMissionDemo(mission) },
                                modifier = Modifier.testTag("advance_mission_${mission.id}")
                            ) {
                                Text("+1 Progress", color = GoldCoin)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArsenalShopPanel(
    viewModel: GameViewModel,
    profile: PlayerProfile?,
    onClose: () -> Unit
) {
    val hud by viewModel.hudSnapshot.collectAsStateWithLifecycle()
    val hudBundle by viewModel.hudSettingsBundle.collectAsStateWithLifecycle()
    var showStyleShop by remember { mutableStateOf(false) }

    PanelShell(title = "Backpack, Weapons & Kapde Shop", playerProfile = profile, onClose = onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { showStyleShop = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!showStyleShop) CrimsonAccent else NavyCardElevated,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("shop_tab_weapons")
                ) {
                    Text("Weapons & Bag", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { showStyleShop = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showStyleShop) CrimsonAccent else NavyCardElevated,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("shop_tab_style")
                ) {
                    Text("Style & Kapde (₹10+)", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { viewModel.openPanel(GamePanel.CHARACTER) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("shop_open_style_studio")
                ) {
                    Text("3D Studio", color = ElectricCyan)
                }
            }

            if (!showStyleShop) {
                Surface(
                    color = NavyCardElevated,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GoldCoin.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Field Supplies: ${hud.medkitCount} Medkits • ${hud.grenadeCount} Grenades",
                                style = MaterialTheme.typography.labelMedium,
                                color = GoldCoin
                            )
                            Text(
                                text = "${hud.ownedWeaponIds.size} Bag Slots",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan
                            )
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                Button(
                                    onClick = { viewModel.buySupplyItem(isMedkit = true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldHp, contentColor = Color.Black),
                                    modifier = Modifier.testTag("shop_buy_medkit")
                                ) {
                                    Text("+Medkit (60c)", fontWeight = FontWeight.Bold)
                                }
                            }
                            item {
                                OutlinedButton(
                                    onClick = { viewModel.sellSupplyItem(isMedkit = true) },
                                    enabled = hud.medkitCount > 0,
                                    modifier = Modifier.testTag("shop_sell_medkit")
                                ) {
                                    Text("Sell Med (+35c)", color = GoldCoin)
                                }
                            }
                            item {
                                Button(
                                    onClick = { viewModel.buySupplyItem(isMedkit = false) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent, contentColor = Color.White),
                                    modifier = Modifier.testTag("shop_buy_grenade")
                                ) {
                                    Text("+Grenade (80c)", fontWeight = FontWeight.Bold)
                                }
                            }
                            item {
                                OutlinedButton(
                                    onClick = { viewModel.sellSupplyItem(isMedkit = false) },
                                    enabled = hud.grenadeCount > 0,
                                    modifier = Modifier.testTag("shop_sell_grenade")
                                ) {
                                    Text("Sell Gren (+45c)", color = GoldCoin)
                                }
                            }
                            item {
                                OutlinedButton(
                                    onClick = { viewModel.toggleBareHands() },
                                    modifier = Modifier.testTag("arsenal_bare_hands_button")
                                ) {
                                    Text(if (hud.weaponType == WeaponType.FISTS) "Re-Equip Gun" else "Bare Hands (Hide Gun)", color = ElectricCyan)
                                }
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.testTag("arsenal_weapons_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(GameCatalog.weapons) { w ->
                        val equipped = hud.activeWeaponId == w.id
                        val ownedInBag = w.id in hud.ownedWeaponIds
                        Surface(
                            color = NavyCardElevated,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = when {
                                        equipped -> GoldCoin
                                        ownedInBag -> ElectricCyan.copy(alpha = 0.55f)
                                        else -> ElectricCyan.copy(alpha = 0.25f)
                                    },
                                    shape = RoundedCornerShape(14.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Canvas(modifier = Modifier.size(44.dp, 26.dp).padding(end = 6.dp)) {
                                    with(HudVectorArtwork) {
                                        drawWeaponSilhouette(w.weaponType, if (equipped) GoldCoin else Color.White)
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "${w.name} • ${w.skinName}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White
                                        )
                                        if (ownedInBag) {
                                            Text(
                                                text = if (equipped) "EQUIPPED" else "IN BAG",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (equipped) GoldCoin else EmeraldHp
                                            )
                                        }
                                    }
                                    val ammoInfo = if (w.weaponType.isFirearm) "Mag ${w.magSize}" else "Infinite"
                                    val priceInfo = if (w.priceCoins > 0L) " • Buy ${w.priceCoins}c / Sell +${w.sellPriceCoins}c" else ""
                                    Text(
                                        text = "${w.category} • DMG ${w.damage.toInt()} • $ammoInfo$priceInfo",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricCyan
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (ownedInBag && w.weaponType != WeaponType.FISTS) {
                                        OutlinedButton(
                                            onClick = { viewModel.sellWeapon(w) },
                                            modifier = Modifier.testTag("arsenal_sell_${w.id}")
                                        ) {
                                            Text("Sell +${w.sellPriceCoins}c", color = GoldCoin)
                                        }
                                    }
                                    if (equipped && w.weaponType != WeaponType.FISTS) {
                                        OutlinedButton(
                                            onClick = { viewModel.dropActiveWeapon() },
                                            modifier = Modifier.testTag("arsenal_drop_${w.id}")
                                        ) {
                                            Text("Drop", color = CrimsonAccent)
                                        }
                                    }
                                    if (ownedInBag || w.priceCoins == 0L) {
                                        Button(
                                            onClick = { viewModel.switchWeapon(w.id) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (equipped) EmeraldHp else ElectricCyan,
                                                contentColor = Color.Black
                                            ),
                                            modifier = Modifier.testTag("arsenal_equip_${w.id}")
                                        ) {
                                            Text(if (equipped) "Equipped" else "Equip", fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Button(
                                            onClick = { viewModel.buyWeapon(w) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CrimsonAccent,
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier.testTag("arsenal_buy_${w.id}")
                                        ) {
                                            Text("Buy ${w.priceCoins}c", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("shop_style_list")
                ) {
                    item {
                        Text(
                            text = "Clothes & Jackets (Starts ₹10 • Instant Equip)",
                            style = MaterialTheme.typography.labelMedium,
                            color = GoldCoin
                        )
                    }
                    items(HeroAppearanceCatalog.outfits) { o ->
                        val equipped = HeroAppearanceCatalog.resolveOutfit(profile?.outfitStyle).id == o.id
                        val unlocked = viewModel.isStyleUnlocked("outfit", o.id) || o.priceRupees == 0
                        StyleShopItemRow(
                            title = o.displayName,
                            subtitle = o.subtitle,
                            swatchColor = Color(o.coatColorInt),
                            trimColor = Color(o.trimColorInt),
                            priceRupees = o.priceRupees,
                            unlocked = unlocked,
                            equipped = equipped,
                            buttonTag = "shop_buy_outfit_${o.id}",
                            thumbnailResId = when (o.id) {
                                "shadow" -> R.drawable.char_main_indra
                                "crimson_blox" -> R.drawable.char_crimson_blox
                                else -> null
                            },
                            onAction = { viewModel.buyAndEquipStyleQuick("outfit", o.id) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Hair Styles & Colors (₹10 – ₹30)",
                            style = MaterialTheme.typography.labelMedium,
                            color = GoldCoin
                        )
                    }
                    items(HeroAppearanceCatalog.hairCuts) { hc ->
                        val equipped = hudBundle.selectedHairCutId == hc.id
                        val unlocked = viewModel.isStyleUnlocked("haircut", hc.id) || hc.priceRupees == 0
                        StyleShopItemRow(
                            title = hc.displayName,
                            subtitle = hc.subtitle,
                            swatchColor = Color(0xFF1E293B),
                            trimColor = ElectricCyan,
                            priceRupees = hc.priceRupees,
                            unlocked = unlocked,
                            equipped = equipped,
                            buttonTag = "shop_buy_haircut_${hc.id}",
                            onAction = { viewModel.buyAndEquipStyleQuick("haircut", hc.id) }
                        )
                    }
                    items(HeroAppearanceCatalog.hairs) { h ->
                        val equipped = HeroAppearanceCatalog.resolveHair(profile?.hairStyle).id == h.id
                        val unlocked = viewModel.isStyleUnlocked("hair", h.id) || h.priceRupees == 0
                        StyleShopItemRow(
                            title = "${h.displayName} Hair Color",
                            subtitle = "Anime multi-spike hair dye",
                            swatchColor = Color(h.colorInt),
                            trimColor = Color(h.highlightColorInt),
                            priceRupees = h.priceRupees,
                            unlocked = unlocked,
                            equipped = equipped,
                            buttonTag = "shop_buy_hair_${h.id}",
                            onAction = { viewModel.buyAndEquipStyleQuick("hair", h.id) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Streetwear Gear, Scarf, Caps & Weapon Skins (₹10 – ₹40)",
                            style = MaterialTheme.typography.labelMedium,
                            color = GoldCoin
                        )
                    }
                    items(HeroAppearanceCatalog.headgears) { g ->
                        val equipped = hudBundle.selectedHeadgearId == g.id
                        val unlocked = viewModel.isStyleUnlocked("gear", g.id) || g.priceRupees == 0
                        StyleShopItemRow(
                            title = g.displayName,
                            subtitle = g.description,
                            swatchColor = Color(g.swatchColorInt),
                            trimColor = GoldCoin,
                            priceRupees = g.priceRupees,
                            unlocked = unlocked,
                            equipped = equipped,
                            buttonTag = "shop_buy_gear_${g.id}",
                            onAction = { viewModel.buyAndEquipStyleQuick("gear", g.id) }
                        )
                    }
                    items(HeroAppearanceCatalog.weaponSkins) { ws ->
                        val equipped = hudBundle.selectedWeaponSkinId == ws.id
                        val unlocked = viewModel.isStyleUnlocked("wskin", ws.id) || ws.priceRupees == 0
                        StyleShopItemRow(
                            title = "Katana: ${ws.displayName}",
                            subtitle = "Aura edge & diamond hilt wrap",
                            swatchColor = Color(ws.scabbardColorInt),
                            trimColor = Color(ws.bladeColorInt),
                            priceRupees = ws.priceRupees,
                            unlocked = unlocked,
                            equipped = equipped,
                            buttonTag = "shop_buy_wskin_${ws.id}",
                            onAction = { viewModel.buyAndEquipStyleQuick("wskin", ws.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StyleShopItemRow(
    title: String,
    subtitle: String,
    swatchColor: Color,
    trimColor: Color,
    priceRupees: Int,
    unlocked: Boolean,
    equipped: Boolean,
    buttonTag: String,
    thumbnailResId: Int? = null,
    onAction: () -> Unit
) {
    Surface(
        color = NavyCardElevated,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (equipped) EmeraldHp else ElectricCyan.copy(alpha = 0.35f),
                shape = RoundedCornerShape(14.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (thumbnailResId != null) {
                Image(
                    painter = painterResource(id = thumbnailResId),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.5.dp, trimColor, RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(swatchColor)
                        .border(2.dp, trimColor, CircleShape)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
            }
            Button(
                onClick = onAction,
                enabled = !equipped,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        equipped -> EmeraldHp
                        unlocked -> ElectricCyan
                        else -> GoldCoin
                    },
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag(buttonTag)
            ) {
                Text(
                    text = when {
                        equipped -> "Equipped"
                        unlocked -> "Equip"
                        else -> "Buy ₹$priceRupees"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun WorldMapPanel(
    viewModel: GameViewModel,
    hud: WorldHudSnapshot,
    profile: PlayerProfile?,
    onClose: () -> Unit
) {
    val playersState by viewModel.discoverablePlayersState.collectAsStateWithLifecycle()
    val activeGps by viewModel.activeGpsDestination.collectAsStateWithLifecycle()
    val friends = (playersState as? UiState.Success)?.data ?: emptyList()

    var zoomFactor by remember { mutableFloatStateOf(1f) }
    var panWorldX by remember { mutableFloatStateOf(0f) }
    var panWorldZ by remember { mutableFloatStateOf(0f) }
    var selectedCategory by remember { mutableStateOf<LocationCategory?>(null) }
    var selectedLocation by remember { mutableStateOf<WorldLocation>(WorldLocations.playerSpawn) }

    val filteredLocations = remember(selectedCategory) {
        if (selectedCategory == null) WorldLocations.allLocations
        else WorldLocations.allLocations.filter { it.category == selectedCategory }
    }

    PanelShell(title = "2400 x 2400 Open World Map & GPS", playerProfile = profile, onClose = onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            // Category Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    ChoiceChipBox(label = "All", selected = selectedCategory == null) {
                        selectedCategory = null
                    }
                }
                items(LocationCategory.entries.filter { it != LocationCategory.SPAWN }) { cat ->
                    ChoiceChipBox(label = cat.label, selected = selectedCategory == cat) {
                        selectedCategory = cat
                    }
                }
            }

            // Interactive 2400 x 2400 Tactical World Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds()
                    .background(Color(0xFF0A1628), RoundedCornerShape(16.dp))
                    .border(1.5.dp, ElectricCyan.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(zoomFactor, panWorldX, panWorldZ, filteredLocations) {
                            detectTapGestures { tap ->
                                val cx = size.width * 0.5f
                                val cy = size.height * 0.5f
                                val baseScale = minOf(size.width, size.height).toFloat() / 2450f
                                val s = baseScale * zoomFactor
                                val wx = panWorldX + (tap.x - cx) / s
                                val wz = panWorldZ + (tap.y - cy) / s
                                val hit = filteredLocations.minByOrNull { hypot(it.x - wx, it.z - wz) }
                                if (hit != null && hypot(hit.x - wx, hit.z - wz) * s <= 42f) {
                                    selectedLocation = hit
                                }
                            }
                        }
                        .pointerInput(zoomFactor) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val baseScale = minOf(size.width, size.height).toFloat() / 2450f
                                val s = baseScale * zoomFactor
                                panWorldX = (panWorldX - dragAmount.x / s).coerceIn(-1050f, 1050f)
                                panWorldZ = (panWorldZ - dragAmount.y / s).coerceIn(-1050f, 1050f)
                            }
                        }
                ) {
                    val cx = size.width * 0.5f
                    val cy = size.height * 0.5f
                    val scale = (size.minDimension / 2450f) * zoomFactor

                    fun toPx(wx: Float, wz: Float): Offset = Offset(
                        cx + (wx - panWorldX) * scale,
                        cy + (wz - panWorldZ) * scale
                    )

                    // District Regional Zones (Expanded 2400 x 2400)
                    // Mountain Region (North)
                    drawRect(
                        color = Color(0xFF334155),
                        topLeft = toPx(-1200f, -1200f),
                        size = Size(2400f * scale, 620f * scale)
                    )
                    // Hidden Village (NW)
                    drawRect(
                        color = Color(0xFF2D4A3E),
                        topLeft = toPx(-620f, -580f),
                        size = Size(480f * scale, 320f * scale)
                    )
                    // Forest Region (NE)
                    drawRect(
                        color = Color(0xFF1A4731),
                        topLeft = toPx(280f, -580f),
                        size = Size(920f * scale, 470f * scale)
                    )
                    // Airport (West)
                    drawRect(
                        color = Color(0xFF1E293B),
                        topLeft = toPx(-1160f, -460f),
                        size = Size(720f * scale, 420f * scale)
                    )
                    // Residential Region (SW)
                    drawRect(
                        color = Color(0xFF274E42),
                        topLeft = toPx(-980f, 120f),
                        size = Size(720f * scale, 420f * scale)
                    )
                    // Commercial Region (East)
                    drawRect(
                        color = Color(0xFF2E3648),
                        topLeft = toPx(260f, -110f),
                        size = Size(840f * scale, 560f * scale)
                    )
                    // Golden Coast Sand Strip & Southern Ocean
                    drawRect(
                        color = Color(0xFFC9AE74),
                        topLeft = toPx(-1200f, 540f),
                        size = Size(2400f * scale, 100f * scale)
                    )
                    drawRect(
                        color = Color(0xFF0369A1),
                        topLeft = toPx(-1200f, 640f),
                        size = Size(2400f * scale, 560f * scale)
                    )
                    // Offshore Islands
                    drawCircle(Color(0xFF2D6A4F), radius = 64f * scale, center = toPx(-180f, 850f))
                    drawCircle(Color(0xFF2D6A4F), radius = 72f * scale, center = toPx(260f, 845f))

                    // Forest River
                    val rNodes = ForestGenerator.riverNodes
                    for (i in 0 until rNodes.lastIndex) {
                        drawLine(
                            color = Color(0xFF0EA5E9),
                            start = toPx(rNodes[i].x, rNodes[i].z),
                            end = toPx(rNodes[i + 1].x, rNodes[i + 1].z),
                            strokeWidth = (18f * scale).coerceAtLeast(3f)
                        )
                    }

                    // 200u Coordinate Grid Lines for 2400x2400 structural world scale
                    for (g in -1000..1000 step 200) {
                        val vx = toPx(g.toFloat(), -1200f)
                        val vxEnd = toPx(g.toFloat(), 1200f)
                        drawLine(Color(0x2294A3B8), start = vx, end = vxEnd, strokeWidth = 1f)
                        val hz = toPx(-1200f, g.toFloat())
                        val hzEnd = toPx(1200f, g.toFloat())
                        drawLine(Color(0x2294A3B8), start = hz, end = hzEnd, strokeWidth = 1f)
                    }

                    // Indian Mega City Authoritative 3D Zone Layout Boundaries
                    val dashedStroke = Stroke(
                        width = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                    )
                    for (zone in CityLandmarksAndModules.cityZones) {
                        val accent = Color(zone.id.accentHex)
                        if (zone.id == CityZoneId.CENTRAL_ROUNDABOUT) {
                            val centerPx = toPx(zone.centerX, zone.centerZ)
                            drawCircle(accent.copy(alpha = 0.12f), radius = zone.radius * scale, center = centerPx)
                            drawCircle(accent.copy(alpha = 0.85f), radius = zone.radius * scale, center = centerPx, style = dashedStroke)
                            // Central Pool Ring & Indian National Flag origin emblem
                            drawCircle(Color(0x880EA5E9), radius = CityLandmarksAndModules.POOL_SHALLOW_OUTER_RADIUS * scale, center = centerPx)
                            drawCircle(Color(0xFFFF9933), radius = (CityLandmarksAndModules.FLAG_ISLAND_RADIUS * scale).coerceAtLeast(4f), center = centerPx)
                        } else {
                            val topLeft = toPx(zone.minX, zone.minZ)
                            val zSize = Size((zone.maxX - zone.minX) * scale, (zone.maxZ - zone.minZ) * scale)
                            drawRect(accent.copy(alpha = 0.10f), topLeft = topLeft, size = zSize)
                            drawRect(accent.copy(alpha = 0.80f), topLeft = topLeft, size = zSize, style = dashedStroke)
                        }
                    }

                    // All Connected Road Segments & Bridges
                    for (seg in WorldGenerator.roads) {
                        drawLine(
                            color = if (seg.isBridge) Color(0xFF38BDF8) else Color(0xFF64748B),
                            start = toPx(seg.startX, seg.startZ),
                            end = toPx(seg.endX, seg.endZ),
                            strokeWidth = (seg.width * scale).coerceAtLeast(2.2f)
                        )
                    }

                    // Active GPS Navigation Route
                    val path = viewModel.sim.gpsPath
                    if (path.size >= 2) {
                        for (i in 0 until path.lastIndex) {
                            drawLine(
                                color = GoldCoin,
                                start = toPx(path[i].x, path[i].z),
                                end = toPx(path[i + 1].x, path[i + 1].z),
                                strokeWidth = 4.5f
                            )
                        }
                    }

                    // Location Registry Markers
                    for (loc in filteredLocations) {
                        val pt = toPx(loc.x, loc.z)
                        val isSelected = loc.id == selectedLocation.id
                        val isGpsTarget = activeGps?.id == loc.id
                        val markerColor = Color(loc.category.colorHex)
                        if (isSelected || isGpsTarget) {
                            drawCircle(
                                color = if (isGpsTarget) GoldCoin else Color.White,
                                radius = 13f,
                                center = pt,
                                style = Stroke(width = 3f)
                            )
                        }
                        drawCircle(markerColor, radius = if (isSelected) 8.5f else 6.5f, center = pt)
                    }

                    // Online Multiplayer Friend Markers
                    for (fr in friends) {
                        val fPt = toPx(fr.posX.toFloat(), fr.posZ.toFloat())
                        drawCircle(ElectricCyan, radius = 9f, center = fPt, style = Stroke(width = 2.5f))
                        drawCircle(EmeraldHp, radius = 5.5f, center = fPt)
                    }

                    // Player Position Chevron
                    val pPt = toPx(hud.x, hud.z)
                    val rad = Math.toRadians(hud.yawDeg.toDouble())
                    val dx = sin(rad).toFloat()
                    val dz = cos(rad).toFloat()
                    val chevron = Path().apply {
                        moveTo(pPt.x + dx * 12f, pPt.y + dz * 12f)
                        lineTo(pPt.x - dz * 7f - dx * 5f, pPt.y + dx * 7f - dz * 5f)
                        lineTo(pPt.x + dz * 7f - dx * 5f, pPt.y - dx * 7f - dz * 5f)
                        close()
                    }
                    drawPath(chevron, color = GoldCoin)
                }

                // Current position badge + Live GPS sync status (top-left)
                val locTracking by viewModel.locationTrackingState.collectAsStateWithLifecycle()
                val mapContext = LocalContext.current
                val mapLocationLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { grants ->
                    val granted = grants[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                        grants[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        LocationTrackingService.hasLocationPermission(mapContext)
                    if (granted) {
                        viewModel.startLiveLocationTracking(mapContext)
                    } else {
                        viewModel.onLocationPermissionDenied()
                    }
                }
                val liveLat = locTracking.latitude ?: profile?.latitude
                val liveLng = locTracking.longitude ?: profile?.longitude

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                ) {
                    Surface(
                        color = Color(0xCC070B16),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${hud.district} (${hud.x.toInt()}, ${hud.z.toInt()}) • ${friends.size + 3} Active",
                            color = GoldCoin,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        color = if (locTracking.isTracking) Color(0xCC064E3B) else Color(0xCC0F172A),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .border(
                                1.dp,
                                if (locTracking.isTracking) EmeraldHp else ElectricCyan.copy(alpha = 0.6f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                if (locTracking.isTracking) {
                                    viewModel.stopLiveLocationTracking(mapContext)
                                } else if (LocationTrackingService.hasLocationPermission(mapContext)) {
                                    viewModel.startLiveLocationTracking(mapContext)
                                } else {
                                    mapLocationLauncher.launch(LocationTrackingService.requiredPermissions())
                                }
                            }
                            .testTag("world_map_live_gps_toggle")
                    ) {
                        val gpsLabel = if (liveLat != null && liveLng != null) {
                            "Device GPS: ${String.format(java.util.Locale.US, "%.4f, %.4f", liveLat, liveLng)}${if (locTracking.isTracking) " • LIVE" else ""}"
                        } else if (locTracking.isTracking) {
                            "Device GPS: Syncing..."
                        } else {
                            "Tap for Live Device GPS Sync"
                        }
                        Text(
                            text = gpsLabel,
                            color = if (locTracking.isTracking) EmeraldHp else ElectricCyan,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).testTag("world_map_live_gps_badge")
                        )
                    }
                }

                // Zoom Controls (top-right)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                ) {
                    IconButton(
                        onClick = { zoomFactor = (zoomFactor + 0.6f).coerceAtMost(3.2f) },
                        modifier = Modifier.size(34.dp).background(Color(0xCC0F172A), RoundedCornerShape(8.dp)).testTag("world_map_zoom_in")
                    ) {
                        Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom In", tint = ElectricCyan, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = {
                            zoomFactor = (zoomFactor - 0.6f).coerceAtLeast(1f)
                            if (zoomFactor == 1f) {
                                panWorldX = 0f
                                panWorldZ = 0f
                            }
                        },
                        modifier = Modifier.size(34.dp).background(Color(0xCC0F172A), RoundedCornerShape(8.dp)).testTag("world_map_zoom_out")
                    ) {
                        Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom Out", tint = ElectricCyan, modifier = Modifier.size(18.dp))
                    }
                }

                // Indian Mega City Zone Scale Legend (bottom-left inside map)
                Surface(
                    color = Color(0xCC070B16),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .testTag("world_map_zone_legend")
                ) {
                    Text(
                        text = "Zones: Roundabout (r=72u) • Shopping (X:260..760) • Residential (X:-680..-75)",
                        color = ElectricCyan,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Selected Location Action Bar (GPS Route + Fast Travel)
            Surface(
                color = NavyCardElevated,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(selectedLocation.category.colorHex).copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(selectedLocation.name, style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Text(
                            text = "${selectedLocation.district.displayName} • ${selectedLocation.subtitle}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (activeGps != null) {
                            IconButton(
                                onClick = { viewModel.clearGpsDestination() },
                                modifier = Modifier.size(36.dp).testTag("world_map_clear_gps")
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear GPS", tint = DangerRed)
                            }
                        }
                        OutlinedButton(
                            onClick = { viewModel.setGpsDestination(selectedLocation) },
                            modifier = Modifier.testTag("world_map_gps_button")
                        ) {
                            Icon(Icons.Filled.Navigation, contentDescription = null, tint = GoldCoin, modifier = Modifier.size(16.dp))
                            Text(" GPS", color = GoldCoin)
                        }
                        Button(
                            onClick = { viewModel.fastTravelToDistrict(selectedLocation.x, selectedLocation.z, selectedLocation.district.displayName) },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.Black),
                            modifier = Modifier.testTag("world_map_fast_travel_selected")
                        ) {
                            Text("Travel")
                        }
                    }
                }
            }

            // 8 District Quick Jump & Focus Strip
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(GameCatalog.districts) { d ->
                    Surface(
                        color = NavyCardElevated,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .border(1.dp, Color(d.accentColor), RoundedCornerShape(12.dp))
                            .clickable {
                                val matched = WorldLocations.allLocations.firstOrNull { it.district.displayName == d.name }
                                if (matched != null) selectedLocation = matched
                                panWorldX = d.spawnX
                                panWorldZ = d.spawnZ
                                if (zoomFactor < 1.8f) zoomFactor = 1.8f
                            }
                            .testTag("fast_travel_${d.name.lowercase().replace(" ", "_")}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Filled.GpsFixed, contentDescription = null, tint = Color(d.accentColor), modifier = Modifier.size(15.dp))
                            Column {
                                Text(d.name, style = MaterialTheme.typography.labelMedium, color = Color.White)
                                Text(d.subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsRankPanel(
    viewModel: GameViewModel,
    profile: PlayerProfile?,
    onBackToLobby: () -> Unit = {},
    onLinkSuccess: (AuthProviderKind) -> Unit = {},
    onSignOutOrLeave: () -> Unit = {},
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val bundle by viewModel.hudSettingsBundle.collectAsStateWithLifecycle()
    val locTracking by viewModel.locationTrackingState.collectAsStateWithLifecycle()
    val gameplay = bundle.gameplay

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            grants[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            LocationTrackingService.hasLocationPermission(context)
        if (granted) {
            viewModel.startLiveLocationTracking(context)
        } else {
            viewModel.onLocationPermissionDenied()
        }
    }

    var graphics by remember(gameplay.graphicsPreset) { mutableStateOf(gameplay.graphicsPreset) }
    var timeMode by remember { mutableStateOf(viewModel.sim.timeOfDay) }
    var weather by remember { mutableStateOf(viewModel.sim.weatherMode) }
    var showLinkDialog by remember { mutableStateOf(false) }

    PanelShell(title = "Settings, Controls & Custom HUD", playerProfile = profile, onClose = onClose) {
        LazyColumn(modifier = Modifier.testTag("settings_list"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Surface(color = NavyCardElevated, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Season Rank: ${profile?.rankTitle ?: "Grand Shinobi"} (Lv.${profile?.level ?: 52})", style = MaterialTheme.typography.titleMedium, color = GoldCoin)
                        Text("Eliminations: ${profile?.kills ?: 0} • XP: ${profile?.xp ?: 0}/1000 • Upgrade Pts: ${profile?.upgradePoints ?: 0}", color = ElectricCyan)
                    }
                }
            }

            // Live Device GPS -> Firestore Profile Coordinate Sync
            item {
                val activeLat = locTracking.latitude ?: profile?.latitude
                val activeLng = locTracking.longitude ?: profile?.longitude
                Surface(
                    color = NavyCardElevated,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (locTracking.isTracking) EmeraldHp else ElectricCyan.copy(alpha = 0.4f),
                            RoundedCornerShape(14.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SettingToggleRow(
                            label = "Live Location Sync (GPS -> Firestore Profile)",
                            checked = locTracking.isTracking,
                            tag = "settings_toggle_location_tracking"
                        ) { wantEnable ->
                            if (wantEnable) {
                                if (LocationTrackingService.hasLocationPermission(context)) {
                                    viewModel.startLiveLocationTracking(context)
                                } else {
                                    locationPermissionLauncher.launch(LocationTrackingService.requiredPermissions())
                                }
                            } else {
                                viewModel.stopLiveLocationTracking(context)
                            }
                        }
                        val coordLine = if (activeLat != null && activeLng != null) {
                            val accText = (locTracking.accuracyMeters?.toDouble() ?: profile?.locationAccuracyMeters)
                                ?.let { " (±${it.toInt()}m)" } ?: ""
                            "Coords: ${String.format(java.util.Locale.US, "%.5f, %.5f", activeLat, activeLng)}$accText • ${locTracking.statusMessage}"
                        } else {
                            locTracking.statusMessage
                        }
                        Text(
                            text = coordLine,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (locTracking.isTracking) EmeraldHp else TextSecondary,
                            modifier = Modifier.testTag("settings_location_status")
                        )
                    }
                }
            }

            // 16. CUSTOMIZE HUD LAUNCHER + ACTIVE PRESET PICKER
            item {
                Surface(
                    color = Color(0xFF142447),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, ElectricCyan, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Custom HUD Layout Editor", style = MaterialTheme.typography.titleMedium, color = WhiteOrGold())
                                Text("Active Preset: ${bundle.activeLayout.name} (5 Save Slots)", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                            }
                            Button(
                                onClick = { viewModel.openHudEditor() },
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent),
                                modifier = Modifier.testTag("settings_customize_hud_button")
                            ) {
                                Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(" CUSTOMIZE HUD")
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(bundle.layouts) { preset ->
                                ChoiceChipBox(
                                    label = preset.name,
                                    selected = bundle.activeLayoutIndex == preset.slotIndex
                                ) {
                                    viewModel.switchHudLayout(preset.slotIndex)
                                }
                            }
                        }
                    }
                }
            }

            // 1. Graphics Quality
            item {
                Text("1. Graphics Quality", style = MaterialTheme.typography.titleSmall, color = ElectricCyan)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    items(GraphicsPreset.entries) { preset ->
                        ChoiceChipBox(label = preset.label, selected = graphics == preset) {
                            graphics = preset
                            viewModel.updateGraphicsAndEnv(preset, timeMode, weather)
                            viewModel.applyGameplaySettings(gameplay.copy(graphicsPreset = preset))
                        }
                    }
                }
            }

            // 2. Frame Rate Limit (30 / 60 / 90)
            item {
                Text("2. Frame Rate Limit", style = MaterialTheme.typography.titleSmall, color = ElectricCyan)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    listOf(30, 60, 90).forEach { fps ->
                        ChoiceChipBox(label = "$fps FPS", selected = gameplay.targetFps == fps) {
                            viewModel.applyGameplaySettings(gameplay.copy(targetFps = fps))
                        }
                    }
                }
            }

            // 3-6 & 9. Sensitivities + Camera Distance Sliders
            item {
                Surface(color = NavyCardElevated, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Camera & Control Sensitivity", style = MaterialTheme.typography.titleSmall, color = GoldCoin)
                        SettingSliderRow("3. Camera Sensitivity", gameplay.cameraSensitivity, 0.3f..2.2f, "${(gameplay.cameraSensitivity * 100).toInt()}%") {
                            viewModel.applyGameplaySettings(gameplay.copy(cameraSensitivity = it))
                        }
                        SettingSliderRow("4. Aim Sensitivity", gameplay.aimSensitivity, 0.3f..2.0f, "${(gameplay.aimSensitivity * 100).toInt()}%") {
                            viewModel.applyGameplaySettings(gameplay.copy(aimSensitivity = it))
                        }
                        SettingSliderRow("5. Scope Sensitivity", gameplay.scopeSensitivity, 0.3f..1.8f, "${(gameplay.scopeSensitivity * 100).toInt()}%") {
                            viewModel.applyGameplaySettings(gameplay.copy(scopeSensitivity = it))
                        }
                        SettingSliderRow("6. Joystick Sensitivity", gameplay.joystickSensitivity, 0.5f..1.8f, "${(gameplay.joystickSensitivity * 100).toInt()}%") {
                            viewModel.applyGameplaySettings(gameplay.copy(joystickSensitivity = it))
                        }
                        SettingSliderRow("9. Camera Distance", gameplay.cameraDistanceScale, 0.7f..1.45f, "${String.format("%.2fx", gameplay.cameraDistanceScale)}") {
                            viewModel.applyGameplaySettings(gameplay.copy(cameraDistanceScale = it))
                        }
                    }
                }
            }

            // 10-12. Audio Volume Mixers
            item {
                Surface(color = NavyCardElevated, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Audio Mixers", style = MaterialTheme.typography.titleSmall, color = GoldCoin)
                        SettingSliderRow("10. Sound Volume (SFX)", gameplay.soundVolume, 0f..1f, "${(gameplay.soundVolume * 100).toInt()}%") {
                            viewModel.applyGameplaySettings(gameplay.copy(soundVolume = it))
                        }
                        SettingSliderRow("11. Music Volume", gameplay.musicVolume, 0f..1f, "${(gameplay.musicVolume * 100).toInt()}%") {
                            viewModel.applyGameplaySettings(gameplay.copy(musicVolume = it))
                        }
                        SettingSliderRow("12. Voice Chat Volume", gameplay.voiceChatVolume, 0f..1f, "${(gameplay.voiceChatVolume * 100).toInt()}%") {
                            viewModel.applyGameplaySettings(gameplay.copy(voiceChatVolume = it))
                        }
                    }
                }
            }

            // 7, 8, 13, 14, 15. Gameplay Toggles
            item {
                Surface(color = NavyCardElevated, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Gameplay & Ergonomic Toggles", style = MaterialTheme.typography.titleSmall, color = GoldCoin)
                        SettingToggleRow("7. Auto Sprint", gameplay.autoSprint, "settings_toggle_auto_sprint") {
                            viewModel.applyGameplaySettings(gameplay.copy(autoSprint = it))
                        }
                        SettingToggleRow("8. Aim Assist", gameplay.aimAssist, "settings_toggle_aim_assist") {
                            viewModel.applyGameplaySettings(gameplay.copy(aimAssist = it))
                        }
                        SettingToggleRow("13. Vibration Feedback", gameplay.vibrationEnabled, "settings_toggle_vibration") {
                            viewModel.applyGameplaySettings(gameplay.copy(vibrationEnabled = it))
                        }
                        SettingToggleRow("14. Auto Pickup", gameplay.autoPickup, "settings_toggle_auto_pickup") {
                            viewModel.applyGameplaySettings(gameplay.copy(autoPickup = it))
                        }
                        SettingToggleRow("15. Left-handed Controls (Mirrors HUD)", gameplay.leftHandedControls, "settings_toggle_left_handed") {
                            viewModel.applyGameplaySettings(gameplay.copy(leftHandedControls = it))
                        }
                    }
                }
            }

            item {
                Text("Dynamic Time of Day", style = MaterialTheme.typography.titleSmall, color = ElectricCyan)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    items(TimeOfDayMode.entries) { t ->
                        ChoiceChipBox(label = t.label, selected = timeMode == t) {
                            timeMode = t
                            viewModel.updateGraphicsAndEnv(graphics, timeMode, weather)
                        }
                    }
                }
            }

            item {
                Text("Dynamic Weather", style = MaterialTheme.typography.titleSmall, color = ElectricCyan)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    items(WeatherMode.entries) { w ->
                        ChoiceChipBox(label = w.label, selected = weather == w) {
                            weather = w
                            viewModel.updateGraphicsAndEnv(graphics, timeMode, weather)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                if (viewModel.isGuestMode) {
                    Button(
                        onClick = { showLinkDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = VioletSkill),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("settings_link_account_button")
                    ) {
                        Text("Link Guest Progress (Google / Facebook)", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                OutlinedButton(
                    onClick = {
                        viewModel.syncTransformNow()
                        onClose()
                        onBackToLobby()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("return_to_lobby_button")
                ) {
                    Text("Return to Main Lobby", color = ElectricCyan, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.syncTransformNow()
                        if (!viewModel.isGuestMode) {
                            signOutUser(credentialManager, scope) {
                                onSignOutOrLeave()
                            }
                        } else {
                            onSignOutOrLeave()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Text(
                        if (viewModel.isGuestMode) "  Exit Guest Session" else "  Sign Out (Save Cloud State)",
                        style = MaterialTheme.typography.titleMedium
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
                onClose()
                onLinkSuccess(provider)
            }
        )
    }
}

@Composable
private fun WhiteOrGold(): Color = GoldCoin

@Composable
private fun SettingSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.White)
            Text(valueLabel, style = MaterialTheme.typography.labelMedium, color = ElectricCyan)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.height(28.dp)
        )
    }
}

@Composable
private fun SettingToggleRow(
    label: String,
    checked: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.White, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}
