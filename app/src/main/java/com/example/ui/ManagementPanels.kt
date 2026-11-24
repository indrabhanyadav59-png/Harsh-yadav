package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.GameCatalog
import com.example.data.model.HeroAppearanceCatalog
import com.example.data.model.HeroExpression
import com.example.data.model.OwnedPet
import com.example.data.model.OwnedProperty
import com.example.data.model.OwnedVehicle
import com.example.data.model.PlayerProfile
import com.example.game.world.LocationCategory
import com.example.game.world.WorldDistrictId
import com.example.game.world.WorldLocation
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.CyberBlue
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldHp
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyCardElevated
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletSkill

@Composable
fun PanelShell(
    title: String,
    playerProfile: PlayerProfile?,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    BackHandler { onClose() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg.copy(alpha = 0.88f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = NavyCard,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
                .border(1.5.dp, ElectricCyan.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(42.dp)
                                .background(NavyCardElevated, CircleShape)
                                .testTag("panel_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to 3D World", tint = Color.White)
                        }
                        Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${playerProfile?.coins ?: 0} Coins", style = MaterialTheme.typography.labelMedium, color = GoldCoin)
                        Text("${playerProfile?.diamonds ?: 0} Gems", style = MaterialTheme.typography.labelMedium, color = CyberBlue)
                        Button(
                            onClick = onClose,
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("panel_play_world_button")
                        ) {
                            Text("Play World", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                content()
            }
        }
    }
}

private enum class CustomizerTab(val label: String, val tag: String) {
    OUTFIT("Outfit", "char_tab_outfit"),
    HAIR("Hair", "char_tab_hair"),
    ACCESSORY("Accessory", "char_tab_accessory"),
    PROFILE("Profile", "char_tab_profile")
}

@Composable
fun CharacterPanel(
    viewModel: GameViewModel,
    profile: PlayerProfile?,
    onClose: () -> Unit
) {
    val hudBundle by viewModel.hudSettingsBundle.collectAsStateWithLifecycle()
    val resolvedInitialOutfit = HeroAppearanceCatalog.resolveOutfit(profile?.outfitStyle)
    val resolvedInitialHair = HeroAppearanceCatalog.resolveHair(profile?.hairStyle)
    val resolvedInitialAccessory = HeroAppearanceCatalog.resolveAccessory(profile?.accessoryStyle)

    var username by remember(profile?.username) { mutableStateOf(profile?.username ?: "Harsh007") }
    var outfitStyle by remember(profile?.outfitStyle) { mutableStateOf(resolvedInitialOutfit.displayName) }
    var hairStyle by remember(profile?.hairStyle) { mutableStateOf(resolvedInitialHair.displayName) }
    var hairCutId by remember(hudBundle.selectedHairCutId) { mutableStateOf(hudBundle.selectedHairCutId) }
    var accessoryStyle by remember(profile?.accessoryStyle) { mutableStateOf(resolvedInitialAccessory.displayName) }
    var headgearId by remember(hudBundle.selectedHeadgearId) { mutableStateOf(hudBundle.selectedHeadgearId) }
    var weaponSkinId by remember(hudBundle.selectedWeaponSkinId) { mutableStateOf(hudBundle.selectedWeaponSkinId) }
    var expressionId by remember(hudBundle.selectedExpressionId) { mutableStateOf(hudBundle.selectedExpressionId) }
    var accentHex by remember(profile?.accentColorHex) {
        mutableStateOf(profile?.accentColorHex ?: resolvedInitialOutfit.defaultAccentHex)
    }
    val selectedAbilities = remember(profile?.equippedAbilities) {
        mutableStateListOf<String>().apply {
            addAll(profile?.equippedAbilities ?: listOf("Crimson Slash", "Lightning Dash", "Fox Flame", "Aegis Pulse"))
        }
    }
    var activeTab by remember { mutableStateOf(CustomizerTab.OUTFIT) }
    var savedCurrentSelection by remember { mutableStateOf(true) }

    fun previewCurrentLook() {
        viewModel.previewHeroAppearance(
            outfitStyle = outfitStyle,
            hairStyle = hairStyle,
            accessoryStyle = accessoryStyle,
            accentColorHex = accentHex,
            hairCutId = hairCutId,
            headgearId = headgearId,
            weaponSkinId = weaponSkinId,
            expressionId = expressionId
        )
    }

    LaunchedEffect(Unit) {
        viewModel.sim.previewYawOffset = 0f
        previewCurrentLook()
    }

    val closeWithRestore = {
        if (!savedCurrentSelection) {
            viewModel.restoreSavedHeroAppearance()
        }
        onClose()
    }

    BackHandler { closeWithRestore() }

    val activeOutfitPreset = HeroAppearanceCatalog.resolveOutfit(outfitStyle)
    val activeHairPreset = HeroAppearanceCatalog.resolveHair(hairStyle)
    val activeHairCutPreset = HeroAppearanceCatalog.resolveHairCut(hairCutId)
    val activeAccessoryPreset = HeroAppearanceCatalog.resolveAccessory(accessoryStyle)
    val activeHeadgearPreset = HeroAppearanceCatalog.resolveHeadgear(headgearId)
    val activeWeaponSkinPreset = HeroAppearanceCatalog.resolveWeaponSkin(weaponSkinId)
    val activeExpr = HeroAppearanceCatalog.resolveExpression(expressionId)

    val pendingCost = remember(
        hudBundle.unlockedStyleKeys,
        activeOutfitPreset.id,
        activeHairPreset.id,
        activeHairCutPreset.id,
        activeAccessoryPreset.id,
        activeHeadgearPreset.id,
        activeWeaponSkinPreset.id
    ) {
        viewModel.computePendingStyleCost(
            outfitId = activeOutfitPreset.id,
            hairId = activeHairPreset.id,
            hairCutId = activeHairCutPreset.id,
            accessoryId = activeAccessoryPreset.id,
            headgearId = activeHeadgearPreset.id,
            weaponSkinId = activeWeaponSkinPreset.id
        )
    }

    fun badgeForStyle(category: String, id: String, price: Int, isEquippedOnProfile: Boolean): String {
        val unlocked = viewModel.isStyleUnlocked(category, id) || price == 0
        return when {
            isEquippedOnProfile -> "EQUIPPED"
            unlocked -> "OWNED"
            else -> "₹$price"
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(10.dp)
    ) {
        val isWideStudio = maxWidth >= 600.dp || maxWidth > maxHeight

        val stageContent: @Composable (Modifier) -> Unit = { stageMod ->
            Box(
                modifier = stageMod
                    .clip(RoundedCornerShape(20.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            viewModel.sim.previewYawOffset =
                                (viewModel.sim.previewYawOffset + dragAmount.x * 0.45f) % 360f
                        }
                    }
                    .testTag("character_preview_stage")
            ) {
                // Top gradient header leaving center clear for 3D Anime Hero
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .background(
                            Brush.verticalGradient(
                                listOf(ObsidianBg.copy(alpha = 0.72f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = closeWithRestore,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .background(NavyCardElevated.copy(alpha = 0.85f), CircleShape)
                                .border(1.dp, ElectricCyan.copy(alpha = 0.45f), CircleShape)
                                .testTag("panel_back_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to 3D World",
                                tint = Color.White
                            )
                        }
                        Column {
                            Text(
                                text = "INDRA — ANIME HERO STUDIO",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "Drag hero to 360° spin • Tap expression",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.sim.previewYawOffset = 0f },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .background(NavyCardElevated.copy(alpha = 0.8f), CircleShape)
                            .testTag("character_recenter_camera")
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Reset Camera Angle",
                            tint = GoldCoin
                        )
                    }
                }

                // 4 Anime Expression Portrait Selector Column (left of 3D Stage, with visible names)
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp, top = 28.dp, bottom = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    HeroExpression.entries.forEach { expr ->
                        val exprSelected = activeExpr == expr
                        Surface(
                            color = ObsidianBg.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .width(60.dp)
                                .height(54.dp)
                                .border(
                                    width = if (exprSelected) 2.dp else 1.dp,
                                    color = if (exprSelected) GoldCoin else ElectricCyan.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    expressionId = expr.id
                                    savedCurrentSelection = false
                                    previewCurrentLook()
                                }
                                .testTag("char_expr_${expr.id}")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize().padding(horizontal = 3.dp, vertical = 2.dp)
                            ) {
                                AnimeExpressionCanvas(
                                    expression = expr,
                                    hairColor = Color(activeHairPreset.colorInt),
                                    eyeColor = parseColorSafe(accentHex, CrimsonAccent),
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = expr.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.sp,
                                    fontWeight = if (exprSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (exprSelected) GoldCoin else Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Bottom active loadout readout pill
                Surface(
                    color = ObsidianBg.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(8.dp)
                        .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = "${activeOutfitPreset.displayName} • ${activeHairCutPreset.displayName} (${activeHairPreset.displayName}) • ${activeHeadgearPreset.displayName} • ${activeExpr.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        val customizerCard: @Composable (Modifier) -> Unit = { cardMod ->
            Surface(
                color = NavyCard.copy(alpha = 0.94f),
                shape = RoundedCornerShape(20.dp),
                modifier = cardMod.border(1.5.dp, ElectricCyan.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header currency (₹ Rupees + Gems) & Play World button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "INDRA: STYLE & KAPDE SHOP (₹10+)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Wallet: ₹${profile?.coins ?: 0} • ${profile?.diamonds ?: 0} Gems",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldCoin
                            )
                        }
                        Button(
                            onClick = closeWithRestore,
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("panel_play_world_button")
                        ) {
                            Text("Play World", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    // 4 Studio Tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CustomizerTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            Surface(
                                color = if (selected) CrimsonAccent else NavyCardElevated,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .border(
                                        1.dp,
                                        if (selected) GoldCoin else ElectricCyan.copy(alpha = 0.35f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { activeTab = tab }
                                    .testTag(tab.tag)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Scrollable Tab Content
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("character_options_list"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (activeTab) {
                            CustomizerTab.OUTFIT -> {
                                item(key = "featured_main_indra_card") {
                                    val isMainIndraEquipped = activeOutfitPreset.id == "shadow"
                                    Surface(
                                        color = NavyCardElevated,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(
                                                width = if (isMainIndraEquipped) 2.dp else 1.2.dp,
                                                color = if (isMainIndraEquipped) GoldCoin else ElectricCyan,
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.char_main_indra),
                                                contentDescription = "Main Hero: Indra Shinobi",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .border(1.5.dp, ElectricCyan, RoundedCornerShape(12.dp))
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "MAIN HERO: INDRA SHINOBI",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = ElectricCyan,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "Shadow Blaze Jacket • Spiky Indra Hair • Crimson Katana",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondary,
                                                    maxLines = 2
                                                )
                                            }
                                            Button(
                                                onClick = {
                                                    outfitStyle = "Shadow Blaze"
                                                    hairStyle = "Black"
                                                    hairCutId = "spiky_hero"
                                                    accessoryStyle = "Katana"
                                                    headgearId = "gear_none"
                                                    weaponSkinId = "wskin_crimson"
                                                    expressionId = "fierce"
                                                    accentHex = "#E2263A"
                                                    savedCurrentSelection = true
                                                    viewModel.saveCustomization(
                                                        username = username,
                                                        hairStyle = "Black",
                                                        outfitStyle = "Shadow Blaze",
                                                        accentColorHex = "#E2263A",
                                                        equippedAbilities = selectedAbilities.toList(),
                                                        accessoryStyle = "Katana",
                                                        hairCutId = "spiky_hero",
                                                        headgearId = "gear_none",
                                                        weaponSkinId = "wskin_crimson",
                                                        expressionId = "fierce"
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isMainIndraEquipped) EmeraldHp else ElectricCyan,
                                                    contentColor = Color.Black
                                                ),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                modifier = Modifier
                                                    .minimumInteractiveComponentSize()
                                                    .testTag("equip_main_indra_button")
                                            ) {
                                                Text(
                                                    text = if (isMainIndraEquipped) "EQUIPPED" else "EQUIP HERO",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                                item(key = "featured_crimson_blox_card") {
                                    val isBloxEquipped = activeOutfitPreset.id == "crimson_blox"
                                    Surface(
                                        color = NavyCardElevated,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(
                                                width = if (isBloxEquipped) 2.dp else 1.2.dp,
                                                color = if (isBloxEquipped) GoldCoin else CrimsonAccent,
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.char_crimson_blox),
                                                contentDescription = "Crimson Blox Character",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .border(1.5.dp, GoldCoin, RoundedCornerShape(12.dp))
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "HERO: CRIMSON BLOX",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = GoldCoin,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "Flame Hoodie • Black-Red Spiky Hair • Red Sneakers",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondary,
                                                    maxLines = 2
                                                )
                                            }
                                            Button(
                                                onClick = {
                                                    outfitStyle = "Crimson Blox"
                                                    hairStyle = "Black & Crimson"
                                                    hairCutId = "spiky_hero"
                                                    accessoryStyle = "None"
                                                    headgearId = "gear_none"
                                                    expressionId = "happy"
                                                    accentHex = "#E11D2E"
                                                    savedCurrentSelection = true
                                                    viewModel.saveCustomization(
                                                        username = username,
                                                        hairStyle = "Black & Crimson",
                                                        outfitStyle = "Crimson Blox",
                                                        accentColorHex = "#E11D2E",
                                                        equippedAbilities = selectedAbilities.toList(),
                                                        accessoryStyle = "None",
                                                        hairCutId = "spiky_hero",
                                                        headgearId = "gear_none",
                                                        weaponSkinId = weaponSkinId,
                                                        expressionId = "happy"
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isBloxEquipped) EmeraldHp else CrimsonAccent,
                                                    contentColor = Color.White
                                                ),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                modifier = Modifier
                                                    .minimumInteractiveComponentSize()
                                                    .testTag("equip_crimson_blox_button")
                                            ) {
                                                Text(
                                                    text = if (isBloxEquipped) "EQUIPPED" else "EQUIP FREE",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                                item {
                                    Text(
                                        text = "Outfits & Jackets (Starts ₹10 • Main Hero & Crimson Blox Free)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ElectricCyan
                                    )
                                }
                                items(HeroAppearanceCatalog.outfits.chunked(2)) { pair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        pair.forEach { o ->
                                            val isSelected = activeOutfitPreset.id == o.id
                                            val isEquipped = HeroAppearanceCatalog.resolveOutfit(profile?.outfitStyle).id == o.id
                                            val thumbRes = when (o.id) {
                                                "shadow" -> R.drawable.char_main_indra
                                                "crimson_blox" -> R.drawable.char_crimson_blox
                                                else -> null
                                            }
                                            CustomizerPresetCard(
                                                title = o.displayName,
                                                subtitle = o.subtitle,
                                                swatchColor = Color(o.coatColorInt),
                                                trimColor = Color(o.trimColorInt),
                                                badgeText = badgeForStyle("outfit", o.id, o.priceRupees, isEquipped),
                                                selected = isSelected,
                                                thumbnailResId = thumbRes,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("outfit_option_${o.id}"),
                                                onClick = {
                                                    outfitStyle = o.displayName
                                                    accentHex = o.defaultAccentHex
                                                    savedCurrentSelection = false
                                                    previewCurrentLook()
                                                }
                                            )
                                        }
                                        if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Hero Look & Expression (All Looks Free)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ElectricCyan
                                    )
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(top = 6.dp)
                                    ) {
                                        items(HeroExpression.entries) { expr ->
                                            val exprSelected = activeExpr == expr
                                            Surface(
                                                color = if (exprSelected) NavyCardElevated else Color(0xFF0C142B),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .minimumInteractiveComponentSize()
                                                    .border(
                                                        width = if (exprSelected) 1.8.dp else 1.dp,
                                                        color = if (exprSelected) GoldCoin else ElectricCyan.copy(alpha = 0.35f),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable {
                                                        expressionId = expr.id
                                                        savedCurrentSelection = false
                                                        previewCurrentLook()
                                                    }
                                                    .testTag("char_look_card_${expr.id}")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    AnimeExpressionCanvas(
                                                        expression = expr,
                                                        hairColor = Color(activeHairPreset.colorInt),
                                                        eyeColor = parseColorSafe(accentHex, CrimsonAccent),
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                    Text(
                                                        text = expr.displayName,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = if (exprSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (exprSelected) GoldCoin else Color.White,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Colors Palette (Dress Trim, Eyes & Aura Glow)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ElectricCyan
                                    )
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.padding(top = 6.dp)
                                    ) {
                                        items(HeroAppearanceCatalog.namedColorSwatches) { swatch ->
                                            val col = parseColorSafe(swatch.hex, CrimsonAccent)
                                            val isCur = accentHex.equals(swatch.hex, ignoreCase = true)
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier
                                                    .width(62.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (isCur) NavyCardElevated else Color.Transparent)
                                                    .clickable {
                                                        accentHex = swatch.hex
                                                        savedCurrentSelection = false
                                                        previewCurrentLook()
                                                    }
                                                    .padding(vertical = 4.dp, horizontal = 2.dp)
                                                    .testTag("color_swatch_${swatch.hex.removePrefix("#").lowercase()}")
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(col)
                                                        .border(
                                                            width = if (isCur) 3.dp else 1.5.dp,
                                                            color = if (isCur) Color.White else GoldCoin.copy(alpha = 0.45f),
                                                            shape = CircleShape
                                                        )
                                                )
                                                Text(
                                                    text = swatch.name,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 9.sp,
                                                    fontWeight = if (isCur) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isCur) GoldCoin else TextSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            CustomizerTab.HAIR -> {
                                item {
                                    Text(
                                        text = "Hair Style Silhouette (₹0 – ₹30)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ElectricCyan
                                    )
                                }
                                items(HeroAppearanceCatalog.hairCuts.chunked(2)) { pair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        pair.forEach { hc ->
                                            val isSelected = activeHairCutPreset.id == hc.id
                                            val isEquipped = hudBundle.selectedHairCutId == hc.id
                                            CustomizerPresetCard(
                                                title = hc.displayName,
                                                subtitle = hc.subtitle,
                                                swatchColor = Color(activeHairPreset.colorInt),
                                                trimColor = Color(activeHairPreset.highlightColorInt),
                                                badgeText = badgeForStyle("haircut", hc.id, hc.priceRupees, isEquipped),
                                                selected = isSelected,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("haircut_option_${hc.id}"),
                                                onClick = {
                                                    hairCutId = hc.id
                                                    savedCurrentSelection = false
                                                    previewCurrentLook()
                                                }
                                            )
                                        }
                                        if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Hair Colors (₹0 – ₹30)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ElectricCyan
                                    )
                                }
                                items(HeroAppearanceCatalog.hairs.chunked(2)) { pair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        pair.forEach { h ->
                                            val isSelected = activeHairPreset.id == h.id
                                            val isEquipped = HeroAppearanceCatalog.resolveHair(profile?.hairStyle).id == h.id
                                            CustomizerPresetCard(
                                                title = h.displayName,
                                                subtitle = "${activeHairCutPreset.displayName} Dye",
                                                swatchColor = Color(h.colorInt),
                                                trimColor = Color(h.highlightColorInt),
                                                badgeText = badgeForStyle("hair", h.id, h.priceRupees, isEquipped),
                                                selected = isSelected,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("hair_option_${h.id}"),
                                                onClick = {
                                                    hairStyle = h.displayName
                                                    savedCurrentSelection = false
                                                    previewCurrentLook()
                                                }
                                            )
                                        }
                                        if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }

                            CustomizerTab.ACCESSORY -> {
                                item {
                                    Text(
                                        text = "Back Gear & Ninja Mask (₹0 – ₹30)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ElectricCyan
                                    )
                                }
                                items(HeroAppearanceCatalog.accessories.chunked(2)) { pair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        pair.forEach { a ->
                                            val isSelected = activeAccessoryPreset.id == a.id
                                            val isEquipped = HeroAppearanceCatalog.resolveAccessory(profile?.accessoryStyle).id == a.id
                                            CustomizerPresetCard(
                                                title = a.displayName,
                                                subtitle = a.description,
                                                swatchColor = when (a.id) {
                                                    "katana" -> Color(0xFF94A3B8)
                                                    "mask" -> Color(0xFF334155)
                                                    "wings" -> Color(0xFF00E5FF)
                                                    else -> Color(0xFF1E293B)
                                                },
                                                trimColor = GoldCoin,
                                                badgeText = badgeForStyle("acc", a.id, a.priceRupees, isEquipped),
                                                selected = isSelected,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("accessory_option_${a.id}"),
                                                onClick = {
                                                    accessoryStyle = a.displayName
                                                    savedCurrentSelection = false
                                                    previewCurrentLook()
                                                }
                                            )
                                        }
                                        if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Headgear, Scarf, Glasses & Headphones (₹10 – ₹20)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ElectricCyan
                                    )
                                }
                                items(HeroAppearanceCatalog.headgears.chunked(2)) { pair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        pair.forEach { g ->
                                            val isSelected = activeHeadgearPreset.id == g.id
                                            val isEquipped = hudBundle.selectedHeadgearId == g.id
                                            CustomizerPresetCard(
                                                title = g.displayName,
                                                subtitle = g.description,
                                                swatchColor = Color(g.swatchColorInt),
                                                trimColor = GoldCoin,
                                                badgeText = badgeForStyle("gear", g.id, g.priceRupees, isEquipped),
                                                selected = isSelected,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("headgear_option_${g.id}"),
                                                onClick = {
                                                    headgearId = g.id
                                                    savedCurrentSelection = false
                                                    previewCurrentLook()
                                                }
                                            )
                                        }
                                        if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Katana Weapon Skins (₹0 – ₹40)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ElectricCyan
                                    )
                                }
                                items(HeroAppearanceCatalog.weaponSkins.chunked(2)) { pair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        pair.forEach { ws ->
                                            val isSelected = activeWeaponSkinPreset.id == ws.id
                                            val isEquipped = hudBundle.selectedWeaponSkinId == ws.id
                                            CustomizerPresetCard(
                                                title = ws.displayName,
                                                subtitle = "Katana Blade & Hilt Glow",
                                                swatchColor = Color(ws.scabbardColorInt),
                                                trimColor = Color(ws.bladeColorInt),
                                                badgeText = badgeForStyle("wskin", ws.id, ws.priceRupees, isEquipped),
                                                selected = isSelected,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("weapon_skin_option_${ws.id}"),
                                                onClick = {
                                                    weaponSkinId = ws.id
                                                    savedCurrentSelection = false
                                                    previewCurrentLook()
                                                }
                                            )
                                        }
                                        if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }

                            CustomizerTab.PROFILE -> {
                                item {
                                    val context = LocalContext.current
                                    val hudState by viewModel.hudSnapshot.collectAsStateWithLifecycle()
                                    val objPickerLauncher = rememberLauncherForActivityResult(
                                        contract = ActivityResultContracts.OpenDocument()
                                    ) { uri ->
                                        if (uri != null) {
                                            viewModel.loadCustomObjModel(context, uri, uri.lastPathSegment)
                                        }
                                    }
                                    Surface(
                                        color = NavyCardElevated,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, VioletSkill.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "3D Character Import (.obj)",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = GoldCoin
                                            )
                                            Text(
                                                text = hudState.customObjName?.let {
                                                    "Active Custom 3D Mesh: $it"
                                                } ?: "Import a Wavefront .obj character file from your device (zero storage permission needed).",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = { objPickerLauncher.launch(arrayOf("*/*")) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.Black),
                                                    modifier = Modifier.testTag("import_obj_3d_button")
                                                ) {
                                                    Text("Import .OBJ File", fontWeight = FontWeight.Bold)
                                                }
                                                if (hudState.customObjName != null) {
                                                    OutlinedButton(
                                                        onClick = { viewModel.clearCustomObjModel() },
                                                        modifier = Modifier.testTag("reset_obj_3d_button")
                                                    ) {
                                                        Text("Use Anime Hero", color = GoldCoin)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                item {
                                    Surface(
                                        color = NavyCardElevated,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val heroPortraitRes = if (activeOutfitPreset.id == "crimson_blox") {
                                                R.drawable.char_crimson_blox
                                            } else {
                                                R.drawable.char_main_portrait
                                            }
                                            Image(
                                                painter = painterResource(id = heroPortraitRes),
                                                contentDescription = activeOutfitPreset.displayName,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(60.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .border(2.dp, GoldCoin, RoundedCornerShape(12.dp))
                                            )
                                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text("Name: ${profile?.username ?: username}", style = MaterialTheme.typography.titleSmall, color = Color.White)
                                                Text("Hero Look: ${activeOutfitPreset.displayName} • ${activeHairCutPreset.displayName} • ${activeExpr.displayName}", style = MaterialTheme.typography.labelSmall, color = GoldCoin, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text("Level: ${profile?.level ?: 52} • ${profile?.rankTitle ?: "Grand Shinobi"} • Guild: ${profile?.guild ?: "SHINOBI"}", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    repeat(3) { idx ->
                                                        Icon(Icons.Filled.Star, contentDescription = "Rank Badge", tint = if (idx == 0) GoldCoin else VioletSkill, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                item {
                                    OutlinedTextField(
                                        value = username,
                                        onValueChange = {
                                            username = it.take(24)
                                            savedCurrentSelection = false
                                        },
                                        label = { Text("Character Name") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("character_name_input")
                                    )
                                }

                                item {
                                    val totalCap = ((profile?.unlockedCharSlots ?: 2L) + (profile?.unlockedPetSlots ?: 2L)).toInt().coerceIn(4, 7)
                                    Text("Ability Loadout (${profile?.unlockedCharSlots ?: 2L} Char + ${profile?.unlockedPetSlots ?: 2L} Pet Slots)", style = MaterialTheme.typography.labelMedium, color = GoldCoin)
                                }

                                items(GameCatalog.abilities) { ab ->
                                    val totalCap = ((profile?.unlockedCharSlots ?: 2L) + (profile?.unlockedPetSlots ?: 2L)).toInt().coerceIn(4, 7)
                                    val equipped = ab.name in selectedAbilities
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .minimumInteractiveComponentSize()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (equipped) NavyCardElevated else Color(0xFF0B1328))
                                            .border(1.dp, if (equipped) GoldCoin else ElectricCyan.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                            .clickable {
                                                savedCurrentSelection = false
                                                if (equipped && selectedAbilities.size > 1) {
                                                    selectedAbilities.remove(ab.name)
                                                } else if (!equipped && selectedAbilities.size < totalCap) {
                                                    selectedAbilities.add(ab.name)
                                                }
                                            }
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${ab.name} (${ab.source})", style = MaterialTheme.typography.titleSmall, color = Color.White)
                                            Text("${ab.summary} • EP ${ab.epCost.toInt()} • CD ${ab.cooldownSec.toInt()}s", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                        }
                                        if (equipped) {
                                            Icon(Icons.Filled.Check, contentDescription = "Equipped", tint = EmeraldHp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Fixed Action Row: RESET + (BUY ₹X if unowned) + SAVE CHARACTER
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                outfitStyle = "Shadow Blaze"
                                hairStyle = "Black"
                                hairCutId = "spiky_hero"
                                accessoryStyle = "Katana"
                                headgearId = "gear_none"
                                weaponSkinId = "wskin_crimson"
                                expressionId = "fierce"
                                accentHex = "#E2263A"
                                viewModel.sim.previewYawOffset = 0f
                                savedCurrentSelection = false
                                previewCurrentLook()
                            },
                            modifier = Modifier
                                .weight(0.30f)
                                .height(48.dp)
                                .testTag("character_reset_button")
                        ) {
                            Text("RESET", style = MaterialTheme.typography.labelMedium, color = ElectricCyan)
                        }
                        if (pendingCost > 0) {
                            Button(
                                onClick = {
                                    savedCurrentSelection = true
                                    viewModel.saveCustomization(
                                        username = username,
                                        hairStyle = hairStyle,
                                        outfitStyle = outfitStyle,
                                        accentColorHex = accentHex,
                                        equippedAbilities = selectedAbilities.toList(),
                                        accessoryStyle = accessoryStyle,
                                        hairCutId = hairCutId,
                                        headgearId = headgearId,
                                        weaponSkinId = weaponSkinId,
                                        expressionId = expressionId
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldCoin, contentColor = Color.Black),
                                modifier = Modifier
                                    .weight(0.35f)
                                    .height(48.dp)
                                    .testTag("buy_selected_style_button")
                            ) {
                                Text("BUY ₹$pendingCost", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                        Button(
                            onClick = {
                                savedCurrentSelection = true
                                viewModel.saveCustomization(
                                    username = username,
                                    hairStyle = hairStyle,
                                    outfitStyle = outfitStyle,
                                    accentColorHex = accentHex,
                                    equippedAbilities = selectedAbilities.toList(),
                                    accessoryStyle = accessoryStyle,
                                    hairCutId = hairCutId,
                                    headgearId = headgearId,
                                    weaponSkinId = weaponSkinId,
                                    expressionId = expressionId
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent),
                            modifier = Modifier
                                .weight(if (pendingCost > 0) 0.35f else 0.70f)
                                .height(48.dp)
                                .testTag("save_character_button")
                        ) {
                            Text(
                                text = if (pendingCost > 0) "BUY & SAVE" else "SAVE CHARACTER",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        if (isWideStudio) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                stageContent(Modifier.weight(0.44f).fillMaxHeight())
                customizerCard(Modifier.weight(0.56f).fillMaxHeight())
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                stageContent(Modifier.weight(0.38f).fillMaxWidth())
                customizerCard(Modifier.weight(0.62f).fillMaxWidth())
            }
        }
    }
}

private fun parseColorSafe(hex: String, fallback: Color): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    fallback
}

@Composable
private fun AnimeExpressionCanvas(
    expression: HeroExpression,
    hairColor: Color,
    eyeColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.56f
        val r = w * 0.34f

        // Anime spiky silhouette top hair
        val hairPath = Path().apply {
            moveTo(cx - r * 1.15f, cy + r * 0.1f)
            lineTo(cx - r * 1.2f, cy - r * 0.6f)
            lineTo(cx - r * 0.65f, cy - r * 1.25f)
            lineTo(cx - r * 0.15f, cy - r * 1.05f)
            lineTo(cx + r * 0.25f, cy - r * 1.32f)
            lineTo(cx + r * 0.85f, cy - r * 1.10f)
            lineTo(cx + r * 1.20f, cy - r * 0.55f)
            lineTo(cx + r * 1.12f, cy + r * 0.15f)
            close()
        }
        drawPath(hairPath, hairColor)

        // Face oval + chin
        drawOval(
            color = Color(0xFFF3C3A3),
            topLeft = Offset(cx - r * 0.82f, cy - r * 0.62f),
            size = Size(r * 1.64f, r * 1.56f)
        )

        // Front bangs
        drawLine(hairColor, Offset(cx - r * 0.7f, cy - r * 0.7f), Offset(cx - r * 0.25f, cy - r * 0.15f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
        drawLine(hairColor, Offset(cx + r * 0.1f, cy - r * 0.75f), Offset(cx + r * 0.15f, cy - r * 0.12f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)
        drawLine(hairColor, Offset(cx + r * 0.75f, cy - r * 0.68f), Offset(cx + r * 0.45f, cy - r * 0.15f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)

        // Eyes
        when (expression) {
            HeroExpression.HAPPY -> {
                val eyeStroke = Stroke(width = w * 0.07f, cap = StrokeCap.Round)
                drawArc(Color(0xFF14131D), 190f, 160f, false, Offset(cx - r * 0.52f, cy - r * 0.05f), Size(r * 0.36f, r * 0.26f), style = eyeStroke)
                drawArc(Color(0xFF14131D), 190f, 160f, false, Offset(cx + r * 0.16f, cy - r * 0.05f), Size(r * 0.36f, r * 0.26f), style = eyeStroke)
            }
            HeroExpression.SURPRISED -> {
                drawCircle(Color.White, radius = r * 0.20f, center = Offset(cx - r * 0.34f, cy + r * 0.04f))
                drawCircle(Color.White, radius = r * 0.20f, center = Offset(cx + r * 0.34f, cy + r * 0.04f))
                drawCircle(eyeColor, radius = r * 0.10f, center = Offset(cx - r * 0.34f, cy + r * 0.04f))
                drawCircle(eyeColor, radius = r * 0.10f, center = Offset(cx + r * 0.34f, cy + r * 0.04f))
            }
            else -> {
                drawCircle(Color.White, radius = r * 0.16f, center = Offset(cx - r * 0.34f, cy + r * 0.06f))
                drawCircle(Color.White, radius = r * 0.16f, center = Offset(cx + r * 0.34f, cy + r * 0.06f))
                drawCircle(eyeColor, radius = r * 0.11f, center = Offset(cx - r * 0.34f, cy + r * 0.06f))
                drawCircle(eyeColor, radius = r * 0.11f, center = Offset(cx + r * 0.34f, cy + r * 0.06f))
                // Determined angled brows
                drawLine(Color(0xFF18141E), Offset(cx - r * 0.56f, cy - r * 0.14f), Offset(cx - r * 0.14f, cy - r * 0.04f), strokeWidth = w * 0.06f, cap = StrokeCap.Round)
                drawLine(Color(0xFF18141E), Offset(cx + r * 0.56f, cy - r * 0.14f), Offset(cx + r * 0.14f, cy - r * 0.04f), strokeWidth = w * 0.06f, cap = StrokeCap.Round)
            }
        }

        // Mouth
        when (expression) {
            HeroExpression.FIERCE -> {
                drawLine(Color(0xFF522820), Offset(cx - r * 0.18f, cy + r * 0.45f), Offset(cx + r * 0.20f, cy + r * 0.41f), strokeWidth = w * 0.06f, cap = StrokeCap.Round)
            }
            HeroExpression.HAPPY -> {
                drawArc(Color(0xFF8C2D2A), 10f, 160f, false, Offset(cx - r * 0.22f, cy + r * 0.32f), Size(r * 0.44f, r * 0.26f), style = Stroke(width = w * 0.06f, cap = StrokeCap.Round))
            }
            HeroExpression.RAGE -> {
                drawOval(Color(0xFF7F1D1D), topLeft = Offset(cx - r * 0.24f, cy + r * 0.34f), size = Size(r * 0.48f, r * 0.26f))
                drawLine(Color.White, Offset(cx - r * 0.16f, cy + r * 0.39f), Offset(cx + r * 0.16f, cy + r * 0.39f), strokeWidth = w * 0.045f)
            }
            HeroExpression.SURPRISED -> {
                drawCircle(Color(0xFF7F1D1D), radius = r * 0.13f, center = Offset(cx, cy + r * 0.48f))
            }
        }
    }
}

@Composable
private fun CustomizerPresetCard(
    title: String,
    subtitle: String,
    swatchColor: Color,
    trimColor: Color,
    badgeText: String?,
    selected: Boolean,
    modifier: Modifier = Modifier,
    thumbnailResId: Int? = null,
    onClick: () -> Unit
) {
    val isPriceBadge = badgeText?.startsWith("₹") == true
    val badgeColor = when {
        badgeText == "EQUIPPED" -> EmeraldHp
        badgeText == "OWNED" -> ElectricCyan
        else -> GoldCoin
    }
    Surface(
        color = if (selected) NavyCardElevated else Color(0xFF0C142B),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .border(
                width = if (selected) 1.8.dp else 1.dp,
                color = if (selected) GoldCoin else ElectricCyan.copy(alpha = 0.28f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (thumbnailResId != null) {
                    Image(
                        painter = painterResource(id = thumbnailResId),
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.5.dp, trimColor, RoundedCornerShape(8.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(swatchColor)
                            .border(2.dp, trimColor, CircleShape)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (badgeText != null) {
                        Surface(
                            color = badgeColor.copy(alpha = if (isPriceBadge) 0.24f else 0.16f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.border(1.dp, badgeColor.copy(alpha = 0.72f), RoundedCornerShape(6.dp))
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (selected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Selected",
                            tint = EmeraldHp,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PetsPanel(
    viewModel: GameViewModel,
    profile: PlayerProfile?,
    onClose: () -> Unit
) {
    PetsPanelContent(
        viewModel = viewModel,
        profile = profile,
        onClose = onClose
    )
}

@Composable
fun VehiclesPanel(
    viewModel: GameViewModel,
    profile: PlayerProfile?,
    onClose: () -> Unit
) {
    val vehiclesState by viewModel.vehiclesState.collectAsStateWithLifecycle()
    val ownedVehicles = (vehiclesState as? UiState.Success)?.data ?: emptyList()

    PanelShell(title = "Vehicles & Car Key System", playerProfile = profile, onClose = onClose) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text("Personal Garage (Lock with Car Key to prevent theft!)", style = MaterialTheme.typography.titleSmall, color = GoldCoin)
            }
            items(ownedVehicles) { veh ->
                VehicleCardRow(
                    veh = veh,
                    onDrive = { viewModel.spawnAndDriveOwnedVehicle(veh) },
                    onToggleLock = { viewModel.toggleCarKeyLock(veh) },
                    onRepaint = { hex -> viewModel.repaintVehicle(veh, hex) }
                )
            }
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Indra Dealership & Skyport", style = MaterialTheme.typography.titleSmall, color = ElectricCyan)
            }
            items(GameCatalog.vehicleMarket) { (model, category, cost) ->
                val owned = ownedVehicles.any { it.modelId == model }
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(28.dp))
                            Column {
                                Text(model, style = MaterialTheme.typography.titleMedium, color = Color.White)
                                Text("Class: ${category.uppercase()}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }
                        Button(
                            onClick = { viewModel.buyVehicle(model, category, cost) },
                            enabled = !owned,
                            colors = ButtonDefaults.buttonColors(containerColor = if (owned) Color(0xFF334155) else CrimsonAccent)
                        ) {
                            Text(if (owned) "In Garage" else "$cost Coins")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VehicleCardRow(
    veh: OwnedVehicle,
    onDrive: () -> Unit,
    onToggleLock: () -> Unit,
    onRepaint: (String) -> Unit
) {
    Surface(
        color = NavyCardElevated,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, if (veh.locked) EmeraldHp.copy(alpha = 0.6f) else GoldCoin, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("${veh.modelId} (${veh.category.uppercase()})", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text(
                        text = if (veh.locked) "Car Key: LOCKED • Alarm Armed" else "Car Key: UNLOCKED (Theft Risk!)",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (veh.locked) EmeraldHp else CrimsonAccent
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onToggleLock,
                        modifier = Modifier.size(40.dp).background(Color(0xFF0A1226), CircleShape)
                    ) {
                        Icon(
                            if (veh.locked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                            contentDescription = "Toggle Car Key Lock",
                            tint = if (veh.locked) EmeraldHp else GoldCoin
                        )
                    }
                    Button(onClick = onDrive, colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent)) {
                        Text("Drive")
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Paint:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                listOf("#FF1E42" to Color(0xFFFF1E42), "#00E5FF" to Color(0xFF00E5FF), "#FFD166" to Color(0xFFFFD166), "#A855F7" to Color(0xFFA855F7)).forEach { (hex, col) ->
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(col)
                            .border(1.5.dp, if (veh.paintHex == hex) Color.White else Color.Transparent, CircleShape)
                            .clickable { onRepaint(hex) }
                    )
                }
            }
        }
    }
}

@Composable
fun HousePanel(
    viewModel: GameViewModel,
    profile: PlayerProfile?,
    onClose: () -> Unit
) {
    val propsState by viewModel.propertiesState.collectAsStateWithLifecycle()
    val hud by viewModel.hudSnapshot.collectAsStateWithLifecycle()
    val ownedProps = (propsState as? UiState.Success)?.data ?: emptyList()

    PanelShell(title = "My House, Land & Real Estate", playerProfile = profile, onClose = onClose) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.testTag("house_panel_list")
        ) {
            item {
                Text("Your Owned Houses & Land Plots (अपना घर और ज़मीन)", style = MaterialTheme.typography.titleSmall, color = GoldCoin)
            }
            if (ownedProps.isEmpty()) {
                item {
                    Surface(
                        color = Color(0xFF141E36),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, GoldCoin.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                            .testTag("house_empty_state")
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("No House Owned Yet (नया खिलाड़ी — घर खरीदें)", style = MaterialTheme.typography.titleMedium, color = GoldCoin)
                            Text(
                                text = "New players start with 0 houses! Locate a House or Land Plot (ज़मीन) below with GPS, travel to inspect it, buy it with Coins, store your weapons/medkits inside your room, and sell it back anytime for Coins.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(ownedProps, key = { "owned_${it.id}" }) { prop ->
                    val sellRefund = viewModel.propertySellRefund(prop)
                    Surface(
                        color = NavyCardElevated,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, EmeraldHp.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${prop.customName} (Lv.${prop.upgradeLevel})", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                    Text(
                                        text = "${prop.district} • ${prop.decorTheme} • Vault: ${hud.houseStashWeapons.size} Weapons / ${hud.houseStashMedkits} Meds",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricCyan
                                    )
                                }
                                Icon(Icons.Filled.Home, contentDescription = null, tint = GoldCoin, modifier = Modifier.size(28.dp))
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    Button(
                                        onClick = { viewModel.enterOwnedPropertyInterior(prop) },
                                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent),
                                        modifier = Modifier.testTag("house_enter_${prop.id}")
                                    ) {
                                        Text("Enter Room")
                                    }
                                }
                                item {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.buyOrUpgradeProperty(prop.propertyType, prop.district, 250L, prop)
                                        },
                                        modifier = Modifier.testTag("house_upgrade_${prop.id}")
                                    ) {
                                        Text("Upgrade (250c)", color = GoldCoin)
                                    }
                                }
                                item {
                                    OutlinedButton(onClick = {
                                        val nextTheme = if (prop.decorTheme.contains("Tatami")) "Cyber Neon Loft" else "Neo-Tokyo Tatami"
                                        viewModel.buyOrUpgradeProperty(prop.propertyType, prop.district, 0L, prop, nextTheme)
                                    }) {
                                        Text("Restyle", color = ElectricCyan)
                                    }
                                }
                                item {
                                    Button(
                                        onClick = { viewModel.sellProperty(prop) },
                                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White),
                                        modifier = Modifier.testTag("house_sell_${prop.id}")
                                    ) {
                                        Text("Sell (+${sellRefund}c)", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Real Estate Market — Houses & Land Plots (घर और ज़मीन खरीदें)", style = MaterialTheme.typography.titleSmall, color = ElectricCyan)
            }
            items(GameCatalog.propertyMarketListings, key = { "market_${it.id}" }) { listing ->
                val owned = ownedProps.any { it.id == listing.id }
                Surface(
                    color = NavyCardElevated,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (owned) EmeraldHp.copy(alpha = 0.65f) else if (listing.isLandPlot) GoldCoin.copy(alpha = 0.5f) else ElectricCyan.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(14.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = if (listing.isLandPlot) GoldCoin.copy(alpha = 0.2f) else ElectricCyan.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (listing.isLandPlot) "LAND PLOT" else "HOUSE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (listing.isLandPlot) GoldCoin else ElectricCyan,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(listing.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                                }
                                Text(
                                    text = "${listing.district} (${listing.locX.toInt()}, ${listing.locZ.toInt()}) • Sell Value: +${listing.sellCoins}c",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricCyan
                                )
                                Text(listing.summary, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = {
                                    val districtEnum = WorldDistrictId.entries.firstOrNull { it.displayName == listing.district }
                                        ?: WorldDistrictId.RESIDENTIAL_REGION
                                    viewModel.setGpsDestination(
                                        WorldLocation(
                                            id = "gps_${listing.id}",
                                            name = listing.title,
                                            district = districtEnum,
                                            category = LocationCategory.HOUSING,
                                            x = listing.locX,
                                            y = 0f,
                                            z = listing.locZ,
                                            subtitle = listing.summary
                                        )
                                    )
                                    onClose()
                                },
                                modifier = Modifier.testTag("house_gps_${listing.id}")
                            ) {
                                Text("GPS Locate", color = GoldCoin)
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.fastTravelToDistrict(listing.locX, listing.locZ + 12f, listing.district)
                                },
                                modifier = Modifier.testTag("house_visit_${listing.id}")
                            ) {
                                Text("Visit", color = ElectricCyan)
                            }
                            Button(
                                onClick = { viewModel.buyPropertyListing(listing) },
                                enabled = !owned,
                                colors = ButtonDefaults.buttonColors(containerColor = if (owned) EmeraldHp else CrimsonAccent, contentColor = if (owned) Color.Black else Color.White),
                                modifier = Modifier.testTag("house_buy_${listing.id}")
                            ) {
                                Text(if (owned) "Owned" else "Buy ${listing.buyCoins}c", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChoiceChipBox(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) CrimsonAccent else NavyCardElevated,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .border(1.dp, if (selected) GoldCoin else ElectricCyan.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}
