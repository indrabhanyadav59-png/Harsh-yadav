package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.BountyContract
import com.example.data.model.ChatMessage
import com.example.data.model.GameCatalog
import com.example.data.model.HeroAppearanceCatalog
import com.example.data.model.HudElementConfig
import com.example.data.model.HudElementId
import com.example.data.model.HudIconStyle
import com.example.data.model.PlayerProfile
import com.example.data.model.WeaponType
import com.example.game.engine.NpcEntity
import com.example.game.engine.ProjectedSignboard
import com.example.game.engine.WorldHudSnapshot
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
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun GameHudOverlay(
    viewModel: GameViewModel,
    hud: WorldHudSnapshot,
    playerProfile: PlayerProfile?,
    nearbyPlayersCount: Int,
    activeBountyTargetingMe: BountyContract?,
    modifier: Modifier = Modifier
) {
    val banner by viewModel.bannerMessage.collectAsStateWithLifecycle()
    val audioOn by viewModel.audioEnabled.collectAsStateWithLifecycle()
    val chatOpen by viewModel.chatOpen.collectAsStateWithLifecycle()
    val chatState by viewModel.chatState.collectAsStateWithLifecycle()
    val inspectedNpc by viewModel.inspectedNpc.collectAsStateWithLifecycle()
    val aiDialogueState by viewModel.aiDialogueState.collectAsStateWithLifecycle()
    val hudSettings by viewModel.hudSettingsBundle.collectAsStateWithLifecycle()
    val missionsState by viewModel.missionsState.collectAsStateWithLifecycle()

    val allMissions = (missionsState as? UiState.Success)?.data.orEmpty()
    val activeMission = allMissions.firstOrNull { it.aiGenerated && !it.claimed }
        ?: allMissions.firstOrNull { !it.claimed }
    val activeLayout = hudSettings.activeLayout

    // Bottom hub menu stays HIDDEN by default during combat and opens only on tap
    var menuExpanded by remember { mutableStateOf(false) }
    var houseStashOpen by remember { mutableStateOf(false) }
    var sellHouseConfirmOpen by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        // --- FLOATING OVERHEAD 3D LANDMARK / HOUSE / SHOP SIGNBOARDS ---
        if (hud.projectedSignboards.isNotEmpty() && hud.insideHouseName == null) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val cw = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                val ch = constraints.maxHeight.toFloat().coerceAtLeast(1f)
                ProjectedSignboardsLayer(
                    signboards = hud.projectedSignboards,
                    canvasW = cw,
                    canvasH = ch
                )
            }
        }

        // --- OVERHEAD KILL STREAK POPUP ABOVE PLAYER'S HEAD ("1 KILL", "2 KILLS"...) ---
        val killText = hud.killBannerText
        if (!killText.isNullOrBlank() && hud.drivingVehicleName == null && !hud.isDefeated) {
            val hBias = (hud.heroOverheadScreenX * 2f - 1f).coerceIn(-0.6f, 0.6f)
            val vBias = (hud.heroOverheadScreenY * 2f - 1f).coerceIn(-0.82f, 0.25f)
            Surface(
                color = Color(0xE67F1D1D),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(BiasAlignment(hBias, vBias))
                    .border(1.5.dp, GoldCoin, RoundedCornerShape(12.dp))
                    .testTag("hud_overhead_kill_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = GoldCoin, modifier = Modifier.size(15.dp))
                    Text(
                        text = killText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldCoin
                    )
                }
            }
        }

        // --- RIGHT-SIDE TARGET-LOCK CROSSHAIR CIRCLE (Fixed cleanly to the right of hero; jump-steady) ---
        if (hud.drivingVehicleName == null && !hud.isDefeated) {
            TargetLockCrosshair(
                lockedOnEnemy = hud.crosshairLockedOnEnemy,
                targetName = hud.crosshairTargetName,
                aimingScope = hud.aiming && hud.weaponType.isFirearm,
                weaponType = hud.weaponType,
                modifier = Modifier.align(BiasAlignment(0.16f, -0.10f))
            )
        }

        // --- TOP-LEFT SLIM QUICK TOOLS (4 compact essential pills; Minimap removed cleanly) ---
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 4.dp, top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            HudMiniUtilityChip(
                icon = if (audioOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                contentDescription = "Toggle Audio",
                tag = "hud_audio_button",
                onClick = { viewModel.toggleAudio() }
            )
            HudMiniUtilityChip(
                icon = Icons.AutoMirrored.Filled.Chat,
                contentDescription = "Open World Chat",
                tag = "hud_chat_button",
                tint = ElectricCyan,
                onClick = { viewModel.toggleChat() }
            )
            HudMiniUtilityChip(
                icon = Icons.Filled.Checkroom,
                contentDescription = "Open Style & Clothing Shop",
                tag = "hud_style_button",
                tint = GoldCoin,
                onClick = { viewModel.openPanel(GamePanel.CHARACTER) }
            )
            HudMiniUtilityChip(
                icon = Icons.Filled.Map,
                contentDescription = "Open 2400x2400 World Map",
                tag = "hud_top_map_button",
                tint = CyberBlue,
                onClick = { viewModel.openPanel(GamePanel.MAP) }
            )
        }

        // --- TOP-CENTER COMPASS + DISTRICT + BOUNTY + DAMAGE POPUP ---
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 310.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            CompassBar(
                yawDeg = hud.yawDeg,
                district = hud.district,
                gpsName = hud.gpsDestinationName,
                gpsDistMeters = hud.gpsDistanceMeters
            )

            if (activeBountyTargetingMe != null) {
                Row(
                    modifier = Modifier
                        .background(DangerRed.copy(alpha = 0.9f), RoundedCornerShape(10.dp))
                        .border(1.dp, GoldCoin, RoundedCornerShape(10.dp))
                        .clickable { viewModel.openPanel(GamePanel.FRIENDS) }
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = GoldCoin, modifier = Modifier.size(14.dp))
                    Text(
                        text = "BOUNTY: ${activeBountyTargetingMe.hunterName} hunting you!",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!hud.activePetBuffLabel.isNullOrBlank()) {
                Surface(
                    color = Color(0xBD0D182E),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.border(1.dp, GoldCoin.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = "PET BUFF: ${hud.activePetBuffLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldCoin,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            AnimatedVisibility(visible = banner != null) {
                Surface(
                    color = NavyPanel,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.border(1.dp, ElectricCyan.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                ) {
                    Text(
                        text = banner.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (hud.insideHouseName != null) {
                InsideHouseActionBar(
                    houseName = hud.insideHouseName,
                    onOpenStash = { houseStashOpen = true },
                    onSellHouse = { sellHouseConfirmOpen = true },
                    onExitHouse = { viewModel.exitHouseInterior() }
                )
            }
        }

        // --- TOP-RIGHT COMPACT CURRENCY + PLAYER COUNT + SETTINGS ---
        Column(
            modifier = Modifier.align(Alignment.TopEnd),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .background(Color(0xC2091224), RoundedCornerShape(10.dp))
                    .border(1.dp, ElectricCyan.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                CurrencyTag(Icons.Filled.MonetizationOn, "${playerProfile?.coins ?: 1250}", GoldCoin)
                CurrencyTag(Icons.Filled.Bolt, "${playerProfile?.diamonds ?: 540}", CyberBlue)
                CurrencyTag(Icons.Filled.Group, "${nearbyPlayersCount + 3}", Color.White)
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings & Controls",
                    tint = Color.White,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { viewModel.openPanel(GamePanel.SETTINGS_RANK) }
                        .testTag("hud_settings_button")
                )
            }

            if (hud.drivingVehicleName != null) {
                Surface(
                    color = NavyPanel,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.border(1.dp, GoldCoin.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = "${hud.drivingVehicleName} • ${hud.vehicleSpeedKmh} km/h",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldCoin,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // --- CUSTOMIZABLE POSITIONED HUD LAYER (reads HudLayoutProfile coords & scales) ---
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val canvasW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val canvasH = constraints.maxHeight.toFloat().coerceAtLeast(1f)

            // 1. MAIN MISSION / AI TASK CARD (Left top-below tools; Minimap removed cleanly)
            HudPositionedSlot(activeLayout.element(HudElementId.MISSION_PILL), canvasW, canvasH) { cfg ->
                TopRightMissionPill(
                    mission = activeMission,
                    aiTaskPoints = playerProfile?.upgradePoints ?: 0L,
                    onClick = { viewModel.openPanel(GamePanel.MISSIONS) },
                    modifier = Modifier.alpha(cfg.opacity)
                )
            }

            // 3. LEFT VERTICAL UTILITY RAIL (Bag, Shop, Heal, Map in clean vertical pairs)
            HudPositionedSlot(activeLayout.element(HudElementId.UTILITY_RAIL), canvasW, canvasH) { cfg ->
                Row(
                    modifier = Modifier.alpha(cfg.opacity),
                    horizontalArrangement = Arrangement.spacedBy((4f * cfg.spacingScale).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy((4f * cfg.spacingScale).dp)
                    ) {
                        FuturisticCircleButton(
                            icon = Icons.Filled.Backpack,
                            contentDescription = "Backpack & Weapons",
                            tag = "hud_backpack_button",
                            baseSize = 40.dp,
                            scaleFactor = cfg.sizeScale,
                            opacity = cfg.opacity,
                            iconStyle = cfg.iconStyle,
                            accentColor = ElectricCyan,
                            badgeLabel = "BAG",
                            onClick = { viewModel.openPanel(GamePanel.WEAPONS_SHOP) }
                        )
                        FuturisticCircleButton(
                            icon = Icons.Filled.LocalHospital,
                            contentDescription = "Use Medkit Heal",
                            tag = "hud_heal_button",
                            baseSize = 40.dp,
                            scaleFactor = cfg.sizeScale,
                            opacity = cfg.opacity,
                            iconStyle = cfg.iconStyle,
                            accentColor = EmeraldHp,
                            cooldownSec = hud.healCooldownSec,
                            maxCooldownSec = 4.0f,
                            badgeLabel = "${hud.medkitCount}",
                            vectorArt = { tint -> with(HudVectorArtwork) { drawMedkitIcon(tint) } },
                            onClick = { viewModel.triggerHeal() }
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy((4f * cfg.spacingScale).dp)
                    ) {
                        FuturisticCircleButton(
                            icon = Icons.Filled.ShoppingCart,
                            contentDescription = "Open Shop",
                            tag = "hud_shop_button",
                            baseSize = 40.dp,
                            scaleFactor = cfg.sizeScale,
                            opacity = cfg.opacity,
                            iconStyle = cfg.iconStyle,
                            accentColor = GoldCoin,
                            badgeLabel = "SHOP",
                            onClick = { viewModel.openPanel(GamePanel.WEAPONS_SHOP) }
                        )
                        FuturisticCircleButton(
                            icon = Icons.Filled.Map,
                            contentDescription = "Open World Map",
                            tag = "hud_map_button",
                            baseSize = 40.dp,
                            scaleFactor = cfg.sizeScale,
                            opacity = cfg.opacity,
                            iconStyle = cfg.iconStyle,
                            accentColor = CyberBlue,
                            badgeLabel = "MAP",
                            onClick = { viewModel.openPanel(GamePanel.MAP) }
                        )
                    }
                }
            }

            // 4. MOVEMENT JOYSTICK
            HudPositionedSlot(activeLayout.element(HudElementId.JOYSTICK), canvasW, canvasH) { cfg ->
                VirtualJoystick(
                    sizeDp = (108f * cfg.sizeScale.coerceIn(0.65f, 1.6f)).dp,
                    opacity = cfg.opacity,
                    onMove = { jx, jy ->
                        viewModel.sim.joyX = jx
                        viewModel.sim.joyY = jy
                    }
                )
            }

            // 5. SPRINT TOGGLE
            HudPositionedSlot(activeLayout.element(HudElementId.SPRINT), canvasW, canvasH) { cfg ->
                FuturisticCircleButton(
                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                    contentDescription = "Toggle Sprint",
                    tag = "hud_sprint_button",
                    baseSize = 42.dp,
                    scaleFactor = cfg.sizeScale,
                    opacity = cfg.opacity,
                    iconStyle = cfg.iconStyle,
                    accentColor = GoldCoin,
                    active = hud.sprinting,
                    subLabel = "Sprint",
                    onClick = {
                        viewModel.sim.sprinting = !viewModel.sim.sprinting
                        if (viewModel.sim.sprinting) viewModel.sim.proning = false
                        viewModel.showBanner(if (viewModel.sim.sprinting) "Sprint ON" else "Sprint OFF")
                    }
                )
            }

            // 6. CONTEXTUAL INTERACT (House / Land Plot Buy / NPC / World objects)
            val nonVehicleInteract = hud.nearbyInteraction?.takeIf {
                it.kind != "VEHICLE" && it.kind != "EXIT_VEH" && it.kind != "ALARM_THEFT"
            }
            if (nonVehicleInteract != null) {
                val isBuyPlot = nonVehicleInteract.kind == "BUY_PLOT"
                HudPositionedSlot(activeLayout.element(HudElementId.INTERACT), canvasW, canvasH) { cfg ->
                    FuturisticCircleButton(
                        icon = if (isBuyPlot) Icons.Filled.MonetizationOn else Icons.Filled.TouchApp,
                        contentDescription = nonVehicleInteract.label,
                        tag = "hud_interact_button",
                        baseSize = 46.dp,
                        scaleFactor = cfg.sizeScale,
                        opacity = cfg.opacity,
                        iconStyle = cfg.iconStyle,
                        accentColor = if (isBuyPlot) GoldCoin else ElectricCyan,
                        active = true,
                        badgeLabel = if (isBuyPlot) "BUY" else nonVehicleInteract.kind.take(5),
                        onClick = { viewModel.onInteractPressed() }
                    )
                }
            }

            // 7. COMPACT SLIM HERO PORTRAIT + NAME + HP / EP BARS
            HudPositionedSlot(activeLayout.element(HudElementId.HP_EP_BARS), canvasW, canvasH) { cfg ->
                val outfitId = HeroAppearanceCatalog.resolveOutfit(playerProfile?.outfitStyle).id
                val portraitRes = if (outfitId == "crimson_blox") {
                    R.drawable.char_crimson_blox
                } else {
                    R.drawable.char_main_portrait
                }
                CompactCombatStatsBar(
                    hud = hud,
                    heroName = playerProfile?.username ?: "Indra Hero",
                    avatarResId = portraitRes,
                    scaleFactor = cfg.sizeScale,
                    opacity = cfg.opacity
                )
            }

            // 8. CHARACTER (2..3) + PET (2..4) ABILITY SLOTS + PET BADGE
            HudPositionedSlot(activeLayout.element(HudElementId.ABILITIES), canvasW, canvasH) { cfg ->
                val charSlots = playerProfile?.unlockedCharSlots?.toInt()?.coerceIn(2, 3) ?: 2
                val petSlots = playerProfile?.unlockedPetSlots?.toInt()?.coerceIn(2, 4) ?: 2
                val totalSlots = charSlots + petSlots
                val equippedAbilities = playerProfile?.equippedAbilities
                    ?: listOf("Crimson Slash", "Lightning Dash", "Fox Flame", "Aegis Pulse")
                Row(
                    modifier = Modifier.alpha(cfg.opacity),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((5f * cfg.spacingScale).dp)
                ) {
                    FuturisticCircleButton(
                        icon = Icons.Filled.Pets,
                        contentDescription = "Manage Pets",
                        tag = "hud_pet_badge",
                        baseSize = 36.dp,
                        scaleFactor = cfg.sizeScale,
                        opacity = cfg.opacity,
                        iconStyle = cfg.iconStyle,
                        accentColor = if (hud.petSummoned) GoldCoin else TextSecondary,
                        active = hud.petSummoned,
                        badgeLabel = "Lv.${hud.petEvolutionLevel}",
                        onClick = { viewModel.openPanel(GamePanel.PETS) }
                    )
                    equippedAbilities.take(totalSlots).forEachIndexed { idx, abilityName ->
                        val cd = hud.abilityCooldowns.getOrElse(idx) { 0f }
                        val isCharSkill = idx < charSlots
                        FuturisticCircleButton(
                            icon = Icons.Filled.FlashOn,
                            contentDescription = abilityName,
                            tag = "ability_slot_${abilityName.lowercase().replace(" ", "_")}",
                            baseSize = if (isCharSkill) 38.dp else 34.dp,
                            scaleFactor = cfg.sizeScale,
                            opacity = cfg.opacity,
                            iconStyle = cfg.iconStyle,
                            accentColor = if (isCharSkill) GoldCoin else VioletSkill,
                            cooldownSec = cd,
                            badgeLabel = if (isCharSkill) "C${idx + 1}" else "P${idx - charSlots + 1}",
                            onClick = { viewModel.castEquippedAbility(idx) }
                        )
                    }
                }
            }

            // 9. DYNAMIC WEAPON SWITCH & AMMO / HANDS / DROP CARD
            HudPositionedSlot(activeLayout.element(HudElementId.WEAPON_SWITCH), canvasW, canvasH) { cfg ->
                WeaponHudCard(
                    hud = hud,
                    opacity = cfg.opacity,
                    onSwitchWeapon = { viewModel.switchWeapon(it) },
                    onToggleBareHands = { viewModel.toggleBareHands() },
                    onQuickReload = { viewModel.sim.triggerReload() },
                    onDropWeapon = { viewModel.dropActiveWeapon() }
                )
            }

            // 10. DYNAMIC RELOAD (Firearms) OR PARRY (Katana only; clean HUD when Fists)
            HudPositionedSlot(activeLayout.element(HudElementId.RELOAD), canvasW, canvasH) { cfg ->
                if (hud.weaponType.isFirearm) {
                    FuturisticCircleButton(
                        icon = Icons.Filled.Refresh,
                        contentDescription = "Reload Weapon",
                        tag = "hud_reload_button",
                        baseSize = 42.dp,
                        scaleFactor = cfg.sizeScale,
                        opacity = cfg.opacity,
                        iconStyle = cfg.iconStyle,
                        accentColor = GoldCoin,
                        active = hud.reloading,
                        subLabel = "Reload",
                        onClick = { viewModel.sim.triggerReload() }
                    )
                } else if (hud.weaponType == WeaponType.KATANA) {
                    FuturisticCircleButton(
                        icon = Icons.Filled.Shield,
                        contentDescription = "Guard / Parry",
                        tag = "hud_guard_button",
                        baseSize = 42.dp,
                        scaleFactor = cfg.sizeScale,
                        opacity = cfg.opacity,
                        iconStyle = cfg.iconStyle,
                        accentColor = ElectricCyan,
                        active = hud.defending,
                        subLabel = "Parry",
                        onClick = { viewModel.sim.defending = !viewModel.sim.defending }
                    )
                }
            }

            // 11. DYNAMIC AIM SCOPE (Firearms) OR SWORD HEAVY ATTACK (Katana only; clean single punch button for Fists)
            HudPositionedSlot(activeLayout.element(HudElementId.AIM), canvasW, canvasH) { cfg ->
                if (hud.weaponType.isFirearm) {
                    FuturisticCircleButton(
                        icon = Icons.Filled.GpsFixed,
                        contentDescription = "Toggle Aim Scope",
                        tag = "hud_aim_button",
                        baseSize = 46.dp,
                        scaleFactor = cfg.sizeScale,
                        opacity = cfg.opacity,
                        iconStyle = cfg.iconStyle,
                        accentColor = ElectricCyan,
                        active = hud.aiming,
                        vectorArt = { tint -> with(HudVectorArtwork) { drawScopeAimIcon(tint) } },
                        onClick = { viewModel.sim.aiming = !viewModel.sim.aiming }
                    )
                } else if (hud.weaponType == WeaponType.KATANA) {
                    FuturisticCircleButton(
                        icon = Icons.Filled.Bolt,
                        contentDescription = "Heavy Attack",
                        tag = "hud_sword_heavy_button",
                        baseSize = 46.dp,
                        scaleFactor = cfg.sizeScale,
                        opacity = cfg.opacity,
                        iconStyle = cfg.iconStyle,
                        accentColor = VioletSkill,
                        cooldownSec = hud.heavyAttackCooldownSec,
                        maxCooldownSec = 3.2f,
                        subLabel = "Iaido",
                        vectorArt = { tint ->
                            with(HudVectorArtwork) { drawKatanaSlashIcon(tint) }
                        },
                        onClick = { viewModel.triggerSwordHeavy() }
                    )
                }
            }

            // 12. JUMP BUTTON
            HudPositionedSlot(activeLayout.element(HudElementId.JUMP), canvasW, canvasH) { cfg ->
                FuturisticCircleButton(
                    icon = Icons.Filled.KeyboardArrowUp,
                    contentDescription = "Jump",
                    tag = "hud_jump_button",
                    baseSize = 46.dp,
                    scaleFactor = cfg.sizeScale,
                    opacity = cfg.opacity,
                    iconStyle = cfg.iconStyle,
                    accentColor = ElectricCyan,
                    subLabel = "Jump",
                    vectorArt = { tint -> with(HudVectorArtwork) { drawJumpIcon(tint) } },
                    onClick = { viewModel.sim.triggerJump() }
                )
            }

            // 13. CROUCH BUTTON
            HudPositionedSlot(activeLayout.element(HudElementId.CROUCH), canvasW, canvasH) { cfg ->
                FuturisticCircleButton(
                    icon = Icons.Filled.Shield,
                    contentDescription = "Toggle Crouch",
                    tag = "hud_crouch_button",
                    baseSize = 44.dp,
                    scaleFactor = cfg.sizeScale,
                    opacity = cfg.opacity,
                    iconStyle = cfg.iconStyle,
                    accentColor = CyberBlue,
                    active = hud.crouching,
                    subLabel = "Crouch",
                    vectorArt = { tint -> with(HudVectorArtwork) { drawCrouchIcon(tint) } },
                    onClick = { viewModel.toggleCrouch() }
                )
            }

            // 14. PRONE BUTTON
            HudPositionedSlot(activeLayout.element(HudElementId.PRONE), canvasW, canvasH) { cfg ->
                FuturisticCircleButton(
                    icon = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Toggle Prone",
                    tag = "hud_prone_button",
                    baseSize = 42.dp,
                    scaleFactor = cfg.sizeScale,
                    opacity = cfg.opacity,
                    iconStyle = cfg.iconStyle,
                    accentColor = CyberBlue,
                    active = hud.proning,
                    subLabel = "Prone",
                    vectorArt = { tint -> with(HudVectorArtwork) { drawProneIcon(tint) } },
                    onClick = { viewModel.toggleProne() }
                )
            }

            // 15. GRENADE BUTTON
            HudPositionedSlot(activeLayout.element(HudElementId.GRENADE), canvasW, canvasH) { cfg ->
                FuturisticCircleButton(
                    icon = Icons.Filled.Bolt,
                    contentDescription = "Throw Frag Grenade",
                    tag = "hud_grenade_button",
                    baseSize = 42.dp,
                    scaleFactor = cfg.sizeScale,
                    opacity = cfg.opacity,
                    iconStyle = cfg.iconStyle,
                    accentColor = GoldCoin,
                    cooldownSec = hud.grenadeCooldownSec,
                    maxCooldownSec = 2.4f,
                    badgeLabel = "${hud.grenadeCount}",
                    subLabel = "Grenade",
                    vectorArt = { tint -> with(HudVectorArtwork) { drawGrenadeIcon(tint) } },
                    onClick = { viewModel.throwGrenade() }
                )
            }

            // 16. QUICK MELEE STRIKE BUTTON
            HudPositionedSlot(activeLayout.element(HudElementId.MELEE), canvasW, canvasH) { cfg ->
                FuturisticCircleButton(
                    icon = Icons.Filled.Bolt,
                    contentDescription = "Quick Strike",
                    tag = "hud_melee_button",
                    baseSize = 40.dp,
                    scaleFactor = cfg.sizeScale,
                    opacity = cfg.opacity,
                    iconStyle = cfg.iconStyle,
                    accentColor = CrimsonAccent,
                    cooldownSec = hud.meleeCooldown,
                    maxCooldownSec = 0.45f,
                    badgeLabel = "HIT",
                    onClick = { viewModel.sim.triggerMeleeAttack() }
                )
            }

            // 17. DYNAMIC FIRE / ATTACK BUTTON (Bullet vs Katana Slash vs Fist)
            HudPositionedSlot(activeLayout.element(HudElementId.FIRE), canvasW, canvasH) { cfg ->
                val scaledFire = (64f * cfg.sizeScale.coerceIn(0.6f, 1.75f)).dp
                val fireAccent = when (hud.weaponType) {
                    WeaponType.FIREARM -> ElectricCyan
                    WeaponType.KATANA -> VioletSkill
                    WeaponType.FISTS -> GoldCoin
                }
                val activeFire = viewModel.sim.isFiring
                Box(
                    modifier = Modifier
                        .alpha(cfg.opacity)
                        .minimumInteractiveComponentSize()
                        .size(scaledFire)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                if (activeFire) {
                                    listOf(fireAccent.copy(alpha = 0.85f), CrimsonAccent.copy(alpha = 0.85f))
                                } else {
                                    listOf(Color(0xB810203E), Color(0xD8060B18))
                                }
                            ),
                            CircleShape
                        )
                        .border(2.5.dp, if (activeFire) GoldCoin else fireAccent, CircleShape)
                        .semantics {
                            contentDescription = when (hud.weaponType) {
                                WeaponType.FIREARM -> "Fire Weapon"
                                WeaponType.KATANA -> "Katana Slash"
                                WeaponType.FISTS -> "Punch Attack"
                            }
                            onClick {
                                viewModel.sim.fireActiveWeapon()
                                true
                            }
                        }
                        .pointerInput(hud.weaponType) {
                            detectTapGestures(
                                onPress = {
                                    viewModel.sim.isFiring = true
                                    tryAwaitRelease()
                                    viewModel.sim.isFiring = false
                                    viewModel.sim.fireActiveWeapon()
                                }
                            )
                        }
                        .testTag("hud_fire_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                        drawCircle(
                            color = fireAccent.copy(alpha = 0.35f),
                            style = Stroke(width = 1.6f)
                        )
                    }
                    Canvas(modifier = Modifier.size((scaledFire.value * 0.56f).dp)) {
                        val tint = if (activeFire) Color.White else Color(0xFFF8FAFC)
                        with(HudVectorArtwork) {
                            when (hud.weaponType) {
                                WeaponType.FIREARM -> drawBulletIcon(tint)
                                WeaponType.KATANA -> drawKatanaSlashIcon(tint)
                                WeaponType.FISTS -> drawFistIcon(tint)
                            }
                        }
                    }
                }
            }
        }

        // --- CONTEXTUAL GROUND PICKUP, VEHICLE ENTER / EXIT & DRIVING PEDALS ---
        val groundWeapon = hud.nearbyGroundWeapon
        val vehicleInteract = hud.nearbyVehicleInteraction
        if (groundWeapon != null || vehicleInteract != null || hud.drivingVehicleName != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 74.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (groundWeapon != null) {
                    val pickupSpec = GameCatalog.weapons.firstOrNull { it.id == groundWeapon.weaponId }
                    Surface(
                        color = Color(0xD90A1428),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .border(1.5.dp, GoldCoin, RoundedCornerShape(16.dp))
                            .clickable { viewModel.pickupNearbyWeapon() }
                            .testTag("hud_pickup_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.PanTool, contentDescription = "Pick Up Weapon", tint = GoldCoin, modifier = Modifier.size(16.dp))
                            Column {
                                Text(
                                    text = "Pick Up",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldCoin
                                )
                                Text(
                                    text = pickupSpec?.name ?: groundWeapon.weaponId,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                if (vehicleInteract != null) {
                    Surface(
                        color = if (hud.drivingVehicleName != null) DangerRed else Color(0xD90D162C),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .border(1.5.dp, if (hud.drivingVehicleName != null) GoldCoin else ElectricCyan, RoundedCornerShape(16.dp))
                            .clickable { viewModel.onVehicleInteractPressed() }
                            .testTag("hud_vehicle_action_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (hud.drivingVehicleName != null) Icons.AutoMirrored.Filled.ExitToApp else Icons.Filled.DirectionsCar,
                                contentDescription = vehicleInteract.label,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = vehicleInteract.label,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }

                if (hud.drivingVehicleName != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = Color(0xCC0D162C),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .border(1.5.dp, DangerRed, RoundedCornerShape(12.dp))
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            viewModel.sim.driveThrottle = -1f
                                            tryAwaitRelease()
                                            viewModel.sim.driveThrottle = 0f
                                        }
                                    )
                                }
                                .testTag("hud_drive_brake_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Brake/Reverse", tint = DangerRed, modifier = Modifier.size(18.dp))
                                Text("REV", style = MaterialTheme.typography.labelMedium, color = Color.White)
                            }
                        }

                        Surface(
                            color = Color(0xCC0D162C),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .border(1.5.dp, EmeraldHp, RoundedCornerShape(12.dp))
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            viewModel.sim.driveThrottle = 1f
                                            tryAwaitRelease()
                                            viewModel.sim.driveThrottle = 0f
                                        }
                                    )
                                }
                                .testTag("hud_drive_gas_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Accelerate", tint = EmeraldHp, modifier = Modifier.size(18.dp))
                                Text("GAS", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // --- FLOATING WORLD CHAT DRAWER ---
        AnimatedVisibility(
            visible = chatOpen,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 52.dp)
        ) {
            WorldChatOverlay(
                chatState = chatState,
                onSend = { viewModel.sendChat(it) },
                onClose = { viewModel.toggleChat() }
            )
        }

        // --- COLLAPSIBLE BOTTOM HUB MENU (Hidden by default during combat) ---
        CollapsibleHubMenuBar(
            expanded = menuExpanded,
            onToggleExpanded = { menuExpanded = !menuExpanded },
            onSelectPanel = { panel ->
                menuExpanded = false
                viewModel.openPanel(panel)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
        )

        // --- DEFEAT & RESPAWN COUNTDOWN OVERLAY ---
        if (hud.isDefeated) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x9912050B)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = NavyPanel,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.border(2.dp, CrimsonAccent, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("DEFEATED", style = MaterialTheme.typography.headlineSmall, color = CrimsonAccent)
                        Text(
                            text = "Respawning at Sakura Torii Plaza in ${hud.respawnSecondsLeft}s...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GoldCoin
                        )
                    }
                }
            }
        }
    }

    // --- NPC / OFFLINE AI PROXY INSPECTION DIALOG ---
    if (inspectedNpc != null) {
        NpcInspectionDialog(
            npc = inspectedNpc!!,
            aiDialogueState = aiDialogueState,
            onAskAi = { viewModel.askNpcAiDialogue(inspectedNpc!!) },
            onDismiss = { viewModel.clearInspectedNpc() }
        )
    }

    // --- INSIDE-HOUSE STASH VAULT DIALOG ---
    if (houseStashOpen && hud.insideHouseName != null) {
        HouseStashDialog(
            houseName = hud.insideHouseName,
            hud = hud,
            onStoreWeapon = { viewModel.storeWeaponInHouse(it) },
            onTakeWeapon = { viewModel.takeWeaponFromHouse(it) },
            onStoreSupply = { viewModel.storeSupplyInHouse(it) },
            onTakeSupply = { viewModel.takeSupplyFromHouse(it) },
            onDismiss = { houseStashOpen = false }
        )
    }

    // --- SELL ACTIVE HOUSE CONFIRMATION DIALOG ---
    if (sellHouseConfirmOpen && hud.insideHouseName != null) {
        AlertDialog(
            onDismissRequest = { sellHouseConfirmOpen = false },
            containerColor = NavyCard,
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("Sell ${hud.insideHouseName}?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Do you want to sell this house right now for Coins? All vault weapons & supplies stay safe in your House Stash.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        sellHouseConfirmOpen = false
                        viewModel.sellActiveRoomHouse()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent),
                    modifier = Modifier.testTag("hud_house_sell_confirm_button")
                ) {
                    Text("Sell House", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { sellHouseConfirmOpen = false }) {
                    Text("Cancel", color = ElectricCyan)
                }
            }
        )
    }
}

@Composable
private fun HudPositionedSlot(
    cfg: HudElementConfig,
    canvasW: Float,
    canvasH: Float,
    content: @Composable (HudElementConfig) -> Unit
) {
    if (!cfg.visible) return
    var slotSize by remember { mutableStateOf(IntSize(64, 64)) }
    val rawX = (cfg.normX * canvasW - slotSize.width * 0.5f).roundToInt()
    val rawY = (cfg.normY * canvasH - slotSize.height * 0.5f).roundToInt()
    val maxOffX = (canvasW.roundToInt() - slotSize.width).coerceAtLeast(0)
    val maxOffY = (canvasH.roundToInt() - slotSize.height).coerceAtLeast(0)
    Box(
        modifier = Modifier
            .offset { IntOffset(rawX.coerceIn(0, maxOffX), rawY.coerceIn(0, maxOffY)) }
            .onSizeChanged { slotSize = it }
    ) {
        content(cfg)
    }
}

@Composable
private fun HudMiniUtilityChip(
    icon: ImageVector,
    contentDescription: String,
    tag: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xBA091224),
        shape = CircleShape,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(34.dp)
            .border(1.dp, ElectricCyan.copy(alpha = 0.42f), CircleShape)
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
private fun TargetLockCrosshair(
    lockedOnEnemy: Boolean,
    targetName: String?,
    aimingScope: Boolean,
    weaponType: WeaponType,
    modifier: Modifier = Modifier
) {
    val ringColor = when {
        lockedOnEnemy -> DangerRed
        aimingScope -> ElectricCyan
        weaponType == WeaponType.KATANA -> VioletSkill.copy(alpha = 0.85f)
        else -> Color.White.copy(alpha = 0.78f)
    }
    Column(
        modifier = modifier.testTag("hud_crosshair_circle"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Canvas(modifier = Modifier.size(if (lockedOnEnemy || aimingScope) 44.dp else 34.dp)) {
            val cx = size.width * 0.5f
            val cy = size.height * 0.5f
            val baseRadius = if (lockedOnEnemy) 17f else if (aimingScope) 15f else 12f
            drawCircle(
                color = ringColor,
                radius = baseRadius,
                style = Stroke(width = if (lockedOnEnemy) 3.0f else 1.8f)
            )
            if (lockedOnEnemy) {
                drawCircle(DangerRed.copy(alpha = 0.22f), radius = baseRadius - 2f)
                drawCircle(DangerRed, radius = 3.6f)
                // Four crimson lock-on tick lines
                drawLine(DangerRed, Offset(cx - 22f, cy), Offset(cx - 10f, cy), strokeWidth = 2.6f)
                drawLine(DangerRed, Offset(cx + 10f, cy), Offset(cx + 22f, cy), strokeWidth = 2.6f)
                drawLine(DangerRed, Offset(cx, cy - 22f), Offset(cx, cy - 10f), strokeWidth = 2.6f)
                drawLine(DangerRed, Offset(cx, cy + 10f), Offset(cx, cy + 22f), strokeWidth = 2.6f)
            } else {
                drawCircle(ringColor, radius = 2.3f)
                if (aimingScope) {
                    drawLine(ElectricCyan, Offset(cx - 20f, cy), Offset(cx - 7f, cy), strokeWidth = 2f)
                    drawLine(ElectricCyan, Offset(cx + 7f, cy), Offset(cx + 20f, cy), strokeWidth = 2f)
                    drawLine(ElectricCyan, Offset(cx, cy - 20f), Offset(cx, cy - 7f), strokeWidth = 2f)
                    drawLine(ElectricCyan, Offset(cx, cy + 7f), Offset(cx, cy + 20f), strokeWidth = 2f)
                }
            }
        }
        if (lockedOnEnemy) {
            Text(
                text = "LOCK: ${targetName ?: "ENEMY"}",
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier
                    .background(DangerRed.copy(alpha = 0.92f), RoundedCornerShape(6.dp))
                    .border(1.dp, GoldCoin.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
private fun ProjectedSignboardsLayer(
    signboards: List<ProjectedSignboard>,
    canvasW: Float,
    canvasH: Float
) {
    signboards.take(8).forEach { board ->
        var boardSize by remember(board.id) { mutableStateOf(IntSize(100, 32)) }
        val px = (board.screenX * canvasW - boardSize.width * 0.5f).roundToInt()
            .coerceIn(8, (canvasW.roundToInt() - boardSize.width - 8).coerceAtLeast(8))
        val py = (board.screenY * canvasH - boardSize.height * 0.5f).roundToInt()
            .coerceIn(36, (canvasH.roundToInt() - boardSize.height - 64).coerceAtLeast(36))
        val accent = if (board.isOwnedProperty) EmeraldHp else Color(board.accentHex)
        Surface(
            color = Color(0xBD081122),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .offset { IntOffset(px, py) }
                .onSizeChanged { boardSize = it }
                .border(1.dp, accent.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${board.title} • ${board.distanceMeters}m",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (board.subtitle.isNotBlank()) {
                    Text(
                        text = board.subtitle,
                        fontSize = 8.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun InsideHouseActionBar(
    houseName: String,
    onOpenStash: () -> Unit,
    onSellHouse: () -> Unit,
    onExitHouse: () -> Unit
) {
    Surface(
        color = Color(0xDC081124),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .border(1.2.dp, GoldCoin.copy(alpha = 0.78f), RoundedCornerShape(12.dp))
            .testTag("hud_inside_house_bar")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = houseName,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = GoldCoin,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 110.dp)
            )
            Surface(
                color = ElectricCyan.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .border(1.dp, ElectricCyan, RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenStash)
                    .testTag("hud_house_stash_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(Icons.Filled.Backpack, contentDescription = "Open House Vault", tint = ElectricCyan, modifier = Modifier.size(13.dp))
                    Text("Vault", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Surface(
                color = CrimsonAccent.copy(alpha = 0.24f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .border(1.dp, CrimsonAccent, RoundedCornerShape(8.dp))
                    .clickable(onClick = onSellHouse)
                    .testTag("hud_house_sell_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(Icons.Filled.MonetizationOn, contentDescription = "Sell House", tint = GoldCoin, modifier = Modifier.size(13.dp))
                    Text("Sell House", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GoldCoin)
                }
            }
            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                    .clickable(onClick = onExitHouse)
                    .testTag("hud_house_exit_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Exit House", tint = Color.White, modifier = Modifier.size(13.dp))
                    Text("Exit", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun HouseStashDialog(
    houseName: String,
    hud: WorldHudSnapshot,
    onStoreWeapon: (String) -> Unit,
    onTakeWeapon: (String) -> Unit,
    onStoreSupply: (Boolean) -> Unit,
    onTakeSupply: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavyCard,
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Backpack, contentDescription = null, tint = GoldCoin)
                Column {
                    Text("House Storage Vault", style = MaterialTheme.typography.titleMedium)
                    Text(houseName, style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                }
            }
        },
        text = {
            val storable = hud.ownedWeaponIds.filter { it != "wpn_fists" }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .testTag("house_stash_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "header_supplies") {
                    Text("Supplies Vault", style = MaterialTheme.typography.labelMedium, color = GoldCoin)
                }
                item(key = "row_medkits") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Medkits — Bag: ${hud.medkitCount} | Vault: ${hud.houseStashMedkits}", fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { onStoreSupply(true) }, modifier = Modifier.size(30.dp).testTag("stash_store_medkit")) {
                                Icon(Icons.Filled.Add, contentDescription = "Store Medkit", tint = ElectricCyan, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { onTakeSupply(true) }, modifier = Modifier.size(30.dp).testTag("stash_take_medkit")) {
                                Icon(Icons.Filled.Remove, contentDescription = "Take Medkit", tint = GoldCoin, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                item(key = "row_grenades") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Grenades — Bag: ${hud.grenadeCount} | Vault: ${hud.houseStashGrenades}", fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { onStoreSupply(false) }, modifier = Modifier.size(30.dp).testTag("stash_store_grenade")) {
                                Icon(Icons.Filled.Add, contentDescription = "Store Grenade", tint = ElectricCyan, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { onTakeSupply(false) }, modifier = Modifier.size(30.dp).testTag("stash_take_grenade")) {
                                Icon(Icons.Filled.Remove, contentDescription = "Take Grenade", tint = GoldCoin, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                item(key = "header_stored") {
                    Text("Stored in Vault (${hud.houseStashWeapons.size})", style = MaterialTheme.typography.labelMedium, color = ElectricCyan)
                }
                if (hud.houseStashWeapons.isEmpty()) {
                    item(key = "empty_vault") {
                        Text("No weapons stored inside this house yet.", fontSize = 12.sp, color = TextSecondary)
                    }
                } else {
                    items(hud.houseStashWeapons.withIndex().toList(), key = { "vault_${it.index}_${it.value}" }) { (_, wid) ->
                        val spec = GameCatalog.weapons.firstOrNull { it.id == wid }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(spec?.name ?: wid, fontSize = 12.sp, color = Color.White)
                            TextButton(
                                onClick = { onTakeWeapon(wid) },
                                modifier = Modifier.testTag("stash_take_weapon_$wid")
                            ) {
                                Text("Take", color = GoldCoin, fontSize = 11.sp)
                            }
                        }
                    }
                }
                item(key = "header_bag") {
                    Text("Store From Bag", style = MaterialTheme.typography.labelMedium, color = EmeraldHp)
                }
                if (storable.isEmpty()) {
                    item(key = "empty_bag") {
                        Text("Only unarmed fists left in bag.", fontSize = 12.sp, color = TextSecondary)
                    }
                } else {
                    items(storable, key = { "bag_$it" }) { wid ->
                        val spec = GameCatalog.weapons.firstOrNull { it.id == wid }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(spec?.name ?: wid, fontSize = 12.sp, color = Color.White)
                            TextButton(
                                onClick = { onStoreWeapon(wid) },
                                modifier = Modifier.testTag("stash_store_weapon_$wid")
                            ) {
                                Text("Store", color = ElectricCyan, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = ElectricCyan)
            }
        }
    )
}

@Composable
private fun CompassBar(
    yawDeg: Float,
    district: String,
    gpsName: String? = null,
    gpsDistMeters: Int = 0
) {
    val heading = ((yawDeg % 360f) + 360f) % 360f
    val cardinal = when {
        heading < 22.5f || heading >= 337.5f -> "N"
        heading < 67.5f -> "NE"
        heading < 112.5f -> "E"
        heading < 157.5f -> "SE"
        heading < 202.5f -> "S"
        heading < 247.5f -> "SW"
        heading < 292.5f -> "W"
        else -> "NW"
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            color = Color(0x9E091224),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.border(1.dp, ElectricCyan.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
        ) {
            Text(
                text = "${((heading - 45 + 360) % 360).toInt()}  ${((heading - 20 + 360) % 360).toInt()}  ▾ $cardinal ${heading.toInt()}°  ${((heading + 20) % 360).toInt()}  ${((heading + 45) % 360).toInt()}",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Text(
                text = district,
                fontSize = 10.sp,
                color = GoldCoin,
                modifier = Modifier
                    .background(Color(0x99070B16), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 1.dp)
            )
            if (gpsName != null) {
                Text(
                    text = "GPS: $gpsName (${gpsDistMeters}m)",
                    fontSize = 10.sp,
                    color = ElectricCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .background(Color(0xB3070B16), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun CurrencyTag(icon: ImageVector, value: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

@Composable
private fun WeaponHudCard(
    hud: WorldHudSnapshot,
    opacity: Float,
    onSwitchWeapon: (String) -> Unit,
    onToggleBareHands: () -> Unit,
    onQuickReload: () -> Unit,
    onDropWeapon: () -> Unit
) {
    val activeSpec = GameCatalog.weapons.firstOrNull { it.id == hud.activeWeaponId } ?: GameCatalog.weapons.first()
    val ownedSpecs = hud.ownedWeaponIds.mapNotNull { id -> GameCatalog.weapons.firstOrNull { it.id == id } }
    val slotSpecs = ownedSpecs.ifEmpty { GameCatalog.weapons.take(4) }
    val isBareHands = hud.weaponType == WeaponType.FISTS

    Column(
        modifier = Modifier
            .alpha(opacity.coerceIn(0.2f, 1f))
            .background(Color(0xBD091224), RoundedCornerShape(12.dp))
            .border(1.2.dp, ElectricCyan.copy(alpha = 0.48f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Active Weapon Status & Silhouette + One-Tap Bare Hands Toggle Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                when (hud.weaponType) {
                    WeaponType.FIREARM -> {
                        Text(
                            text = if (hud.reloading) "RELOADING..." else "${hud.ammoInMag}/${hud.reserveAmmo}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (hud.reloading) GoldCoin else Color.White,
                            modifier = Modifier.testTag("hud_weapon_ammo_text")
                        )
                        Text(
                            text = "Quick Reload",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ElectricCyan,
                            modifier = Modifier
                                .clickable(onClick = onQuickReload)
                                .testTag("hud_quick_reload_chip")
                        )
                    }
                    WeaponType.KATANA -> {
                        Text(
                            text = "BLADE",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = VioletSkill
                        )
                        Text(
                            text = "${activeSpec.name} • INF",
                            fontSize = 9.sp,
                            color = ElectricCyan,
                            maxLines = 1
                        )
                    }
                    WeaponType.FISTS -> {
                        Text(
                            text = "BARE HANDS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldCoin
                        )
                        Text(
                            text = "Gun Put Away",
                            fontSize = 9.sp,
                            color = ElectricCyan,
                            maxLines = 1
                        )
                    }
                }
            }

            // Custom Canvas silhouette of held weapon
            Canvas(modifier = Modifier.width(52.dp).height(24.dp)) {
                with(HudVectorArtwork) {
                    drawWeaponSilhouette(hud.weaponType, Color(0xFFF8FAFC))
                }
            }

            // One-tap Hide Gun / Show Gun (Bare Hands Toggle)
            Surface(
                color = if (isBareHands) GoldCoin.copy(alpha = 0.25f) else ElectricCyan.copy(alpha = 0.16f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .border(1.dp, if (isBareHands) GoldCoin else ElectricCyan.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                    .clickable(onClick = onToggleBareHands)
                    .testTag("hud_hands_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PanTool,
                        contentDescription = if (isBareHands) "Equip Gun" else "Hide Gun (Bare Hands)",
                        tint = if (isBareHands) GoldCoin else Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (isBareHands) "Gun" else "Hands",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isBareHands) GoldCoin else Color.White
                    )
                }
            }

            // Drop current weapon (hidden/disabled for unarmed fists)
            if (hud.weaponType != WeaponType.FISTS) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CrimsonAccent.copy(alpha = 0.25f))
                        .border(1.dp, CrimsonAccent.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                        .clickable(onClick = onDropWeapon)
                        .testTag("hud_drop_weapon_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Drop Weapon",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        // Owned Weapon Quick-Switch Bar
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            slotSpecs.take(4).forEachIndexed { i, w ->
                val selected = w.id == hud.activeWeaponId
                Surface(
                    color = if (selected) CrimsonAccent.copy(alpha = 0.85f) else Color(0x9E0A1226),
                    shape = RoundedCornerShape(7.dp),
                    modifier = Modifier
                        .border(1.dp, if (selected) GoldCoin else ElectricCyan.copy(alpha = 0.3f), RoundedCornerShape(7.dp))
                        .clickable { onSwitchWeapon(w.id) }
                        .testTag("hud_weapon_slot_$i")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Canvas(modifier = Modifier.width(18.dp).height(11.dp)) {
                            with(HudVectorArtwork) {
                                drawWeaponSilhouette(w.weaponType, if (selected) Color.White else Color(0xFFCBD5E1))
                            }
                        }
                        Text(
                            text = when (w.id) {
                                "wpn_vortex_ar" -> "AR"
                                "wpn_pulse_pistol" -> "PST"
                                "wpn_shadow_katana" -> "KTN"
                                "wpn_dragon_smg" -> "SMG"
                                else -> "HANDS"
                            },
                            fontSize = 9.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VirtualJoystick(
    sizeDp: androidx.compose.ui.unit.Dp,
    opacity: Float,
    onMove: (Float, Float) -> Unit
) {
    var knobX by remember { mutableFloatStateOf(0f) }
    var knobY by remember { mutableFloatStateOf(0f) }
    val maxRadiusPx = 90f

    Box(
        modifier = Modifier
            .alpha(opacity.coerceIn(0.2f, 1f))
            .size(sizeDp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        knobX = 0f; knobY = 0f
                        onMove(0f, 0f)
                    },
                    onDragCancel = {
                        knobX = 0f; knobY = 0f
                        onMove(0f, 0f)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val nx = knobX + dragAmount.x
                        val ny = knobY + dragAmount.y
                        val dist = sqrt(nx * nx + ny * ny)
                        if (dist > maxRadiusPx) {
                            knobX = (nx / dist) * maxRadiusPx
                            knobY = (ny / dist) * maxRadiusPx
                        } else {
                            knobX = nx
                            knobY = ny
                        }
                        onMove((knobX / maxRadiusPx).coerceIn(-1f, 1f), (-knobY / maxRadiusPx).coerceIn(-1f, 1f))
                    }
                )
            }
            .testTag("hud_joystick"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension * 0.46f
            val cx = size.width * 0.5f
            val cy = size.height * 0.5f
            drawCircle(
                color = Color(0x520D162C),
                radius = r
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.52f),
                radius = r,
                style = Stroke(width = 2.4f)
            )
            drawCircle(
                color = ElectricCyan.copy(alpha = 0.25f),
                radius = r * 0.68f,
                style = Stroke(width = 1.2f)
            )
            // 4-way directional arrowheads (Top, Bottom, Left, Right)
            val arrowDist = r * 0.80f
            val arrowSpan = 5.5f
            val arrowColor = Color.White.copy(alpha = 0.72f)
            // Up
            drawPath(Path().apply {
                moveTo(cx, cy - arrowDist - 4f)
                lineTo(cx - arrowSpan, cy - arrowDist + 3f)
                lineTo(cx + arrowSpan, cy - arrowDist + 3f)
                close()
            }, arrowColor)
            // Down
            drawPath(Path().apply {
                moveTo(cx, cy + arrowDist + 4f)
                lineTo(cx - arrowSpan, cy + arrowDist - 3f)
                lineTo(cx + arrowSpan, cy + arrowDist - 3f)
                close()
            }, arrowColor)
            // Left
            drawPath(Path().apply {
                moveTo(cx - arrowDist - 4f, cy)
                lineTo(cx - arrowDist + 3f, cy - arrowSpan)
                lineTo(cx - arrowDist + 3f, cy + arrowSpan)
                close()
            }, arrowColor)
            // Right
            drawPath(Path().apply {
                moveTo(cx + arrowDist + 4f, cy)
                lineTo(cx + arrowDist - 3f, cy - arrowSpan)
                lineTo(cx + arrowDist - 3f, cy + arrowSpan)
                close()
            }, arrowColor)
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(knobX.roundToInt(), knobY.roundToInt()) }
                .size((sizeDp.value * 0.36f).dp)
                .clip(CircleShape)
                .background(Color(0x9EE2E8F0))
                .border(2.dp, ElectricCyan.copy(alpha = 0.8f), CircleShape)
        )
    }
}

@Composable
private fun WorldChatOverlay(
    chatState: UiState<List<ChatMessage>>,
    onSend: (String) -> Unit,
    onClose: () -> Unit
) {
    var draft by remember { mutableStateOf("") }
    Surface(
        color = Color(0xEB0A1226),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .widthIn(max = 310.dp)
            .height(240.dp)
            .border(1.dp, ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("World Session Chat", style = MaterialTheme.typography.titleSmall, color = ElectricCyan)
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close Chat", tint = Color.White)
                }
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                when (chatState) {
                    is UiState.Success -> {
                        if (chatState.data.isEmpty()) {
                            item {
                                Text(
                                    "Say hi to nearby Shinobi & friends!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        } else {
                            items(chatState.data) { msg ->
                                Text(
                                    text = "${msg.senderName}: ${msg.text}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    is UiState.Error -> {
                        item { Text(chatState.message, color = CrimsonAccent, fontSize = 12.sp) }
                    }
                    else -> {
                        item { Text("Connecting chat...", color = TextSecondary, fontSize = 12.sp) }
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("Message...", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        if (draft.isNotBlank()) {
                            onSend(draft)
                            draft = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(CrimsonAccent, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Chat", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun NpcInspectionDialog(
    npc: NpcEntity,
    aiDialogueState: UiState<String>,
    onAskAi: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavyCard,
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = ElectricCyan)
                Column {
                    Text(npc.displayName, style = MaterialTheme.typography.titleMedium)
                    Text("Lv.${npc.level} • ${npc.guild} • ${npc.role.label}", style = MaterialTheme.typography.labelSmall, color = GoldCoin)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("AI Behavior: ${npc.state.name} — ${npc.statusBubble}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text("HP: ${npc.respawnController.health.toInt()}/${npc.respawnController.maxHealth.toInt()}", style = MaterialTheme.typography.labelMedium, color = EmeraldHp)
                Surface(
                    color = Color(0xFF0A1226),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ElectricCyan.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                ) {
                    Box(modifier = Modifier.padding(10.dp)) {
                        when (aiDialogueState) {
                            is UiState.Loading -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ElectricCyan)
                                Text("AI NPC thinking...", style = MaterialTheme.typography.bodyMedium, color = ElectricCyan)
                            }
                            is UiState.Success -> Text("\"${aiDialogueState.data}\"", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                            is UiState.Error -> Text(aiDialogueState.message, style = MaterialTheme.typography.bodyMedium, color = CrimsonAccent)
                            UiState.Idle -> Text("Tap 'Talk (Gemini AI)' to converse with this character.", color = TextSecondary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAskAi,
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent)
            ) {
                Text("Talk (Gemini AI)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = ElectricCyan)
            }
        }
    )
}
