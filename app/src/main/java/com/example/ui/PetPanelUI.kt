package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.IndraAudioEngine
import com.example.data.model.CappedPetPassives
import com.example.data.model.GameCatalog
import com.example.data.model.OwnedPet
import com.example.data.model.PetCatalog
import com.example.data.model.PetCategory
import com.example.data.model.PlayerProfile
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldHp
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.NavyCardElevated
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletSkill

private enum class PetHubTab(val label: String, val tag: String) {
    COLLECTION("Collection", "pet_tab_collection"),
    SHOP("50-Pet Shop", "pet_tab_shop"),
    ABILITIES("Abilities", "pet_tab_abilities"),
    CUSTOMIZE("Customize", "pet_tab_customize"),
    TRADE("Trade", "pet_tab_trade")
}

@Composable
fun PetsPanelContent(
    viewModel: GameViewModel,
    profile: PlayerProfile?,
    onClose: () -> Unit
) {
    val petsState by viewModel.petsState.collectAsStateWithLifecycle()
    val hud by viewModel.hudSnapshot.collectAsStateWithLifecycle()
    val tradesState by viewModel.petTradesState.collectAsStateWithLifecycle()
    val playersState by viewModel.discoverablePlayersState.collectAsStateWithLifecycle()

    val ownedPets = (petsState as? UiState.Success)?.data ?: emptyList()
    val capped = remember(ownedPets) { PetCatalog.computeCappedPassives(ownedPets) }
    var activeTab by remember { mutableStateOf(PetHubTab.COLLECTION) }

    PanelShell(title = "Pet Sanctuary", playerProfile = profile, onClose = onClose) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PetSanctuaryHeader(
                activePetType = viewModel.sim.petType,
                evoLevel = hud.petEvolutionLevel,
                petSummoned = hud.petSummoned,
                capped = capped,
                onPlayVoice = {
                    val spec = PetCatalog.resolveSpec(viewModel.sim.petType)
                    IndraAudioEngine.playPetSound(spec.soundProfile, hud.petEvolutionLevel)
                },
                onToggleSummon = { viewModel.togglePetSummoned() }
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(PetHubTab.entries) { tab ->
                    ChoiceChipBox(
                        label = tab.label,
                        selected = activeTab == tab,
                        modifier = Modifier.testTag(tab.tag)
                    ) {
                        activeTab = tab
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (activeTab) {
                    PetHubTab.COLLECTION -> PetCollectionTab(viewModel, ownedPets)
                    PetHubTab.SHOP -> PetShopTab(viewModel, ownedPets)
                    PetHubTab.ABILITIES -> PetAbilitiesTab(viewModel, profile, ownedPets)
                    PetHubTab.CUSTOMIZE -> PetCustomizeTab(viewModel, ownedPets)
                    PetHubTab.TRADE -> PetTradeTab(
                        viewModel = viewModel,
                        ownedPets = ownedPets,
                        trades = (tradesState as? UiState.Success)?.data ?: emptyList(),
                        onlinePlayers = (playersState as? UiState.Success)?.data ?: emptyList()
                    )
                }
            }
        }
    }
}

@Composable
private fun PetSanctuaryHeader(
    activePetType: String,
    evoLevel: Int,
    petSummoned: Boolean,
    capped: CappedPetPassives,
    onPlayVoice: () -> Unit,
    onToggleSummon: () -> Unit
) {
    val spec = PetCatalog.resolveSpec(activePetType)
    Surface(
        color = NavyCardElevated,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.2.dp, GoldCoin.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(spec.primaryColorHex))
                            .border(2.dp, Color(spec.accentColorHex), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Pets, contentDescription = null, tint = Color(spec.secondaryColorHex), modifier = Modifier.size(16.dp))
                    }
                    Column {
                        Text(
                            text = "${spec.name} • ${spec.evolutionTitle(evoLevel)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Roster ${capped.equippedCount}/5 • ${if (petSummoned) "Following in 3D" else "Resting in Capsule"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (petSummoned) EmeraldHp else TextSecondary,
                            maxLines = 1
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPlayVoice,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Play Pet Voice", tint = ElectricCyan, modifier = Modifier.size(18.dp))
                    }
                    Button(
                        onClick = onToggleSummon,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (petSummoned) Color(0xFF334155) else EmeraldHp,
                            contentColor = if (petSummoned) Color.White else Color.Black
                        ),
                        modifier = Modifier.testTag("pet_summon_toggle")
                    ) {
                        Text(if (petSummoned) "Dismiss" else "Summon", fontSize = 12.sp)
                    }
                }
            }

            // Live anti-stacking passive readout bar
            val chips = buildList {
                if (capped.speedBonusPct > 0f) add("SPD +${(capped.speedBonusPct * 100).toInt()}%/22%")
                if (capped.maxHpBonusPct > 0f) add("HP +${(capped.maxHpBonusPct * 100).toInt()}%/18%")
                if (capped.attackBonusPct > 0f) add("ATK +${(capped.attackBonusPct * 100).toInt()}%/16%")
                if (capped.damageResistPct > 0f) add("DEF +${(capped.damageResistPct * 100).toInt()}%/18%")
                if (capped.hpOnKill > 0f) add("+${capped.hpOnKill.toInt()} HP/Kill")
                if (capped.epOnKill > 0f) add("+${capped.epOnKill.toInt()} EP/Kill")
                if (capped.enemyDetectRadius > 0f) add("Radar ${capped.enemyDetectRadius.toInt()}m")
                if (capped.hasEmergencySave) add("Rebirth Save")
            }
            Text(
                text = if (chips.isEmpty()) "Equip up to 5 pets to combine passives (anti-stacking caps active)." else "Passives: ${chips.joinToString(" • ")}",
                style = MaterialTheme.typography.labelSmall,
                color = ElectricCyan,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PetCollectionTab(viewModel: GameViewModel, ownedPets: List<OwnedPet>) {
    val wildAvailable = viewModel.sim.wildPets.filter { !it.adopted }
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("pets_list"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(ownedPets, key = { it.id }) { pet ->
            val spec = PetCatalog.resolveSpec(pet.petType)
            val evo = pet.effectiveEvolutionLevel()
            val isFollower = viewModel.sim.petType.equals(spec.name, ignoreCase = true)
            val upgradeCost = if (evo == 1) 240L else 480L
            Surface(
                color = NavyCardElevated,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, if (isFollower) EmeraldHp else if (pet.equipped) ElectricCyan.copy(alpha = 0.7f) else Color(0xFF334155), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "#${spec.number} ${pet.nickname.ifBlank { spec.name }} (${spec.evolutionTitle(evo)})",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${spec.category.displayName} • ${spec.rarity} • Aura: ${pet.auraStyle}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(spec.category.badgeHex),
                                maxLines = 1
                            )
                        }
                        IconButton(
                            onClick = { IndraAudioEngine.playPetSound(spec.soundProfile, evo) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Play ${spec.name} Sound", tint = GoldCoin, modifier = Modifier.size(18.dp))
                        }
                    }
                    Text("Passive (${spec.passiveName}): ${spec.passiveSummary}", style = MaterialTheme.typography.labelSmall, color = EmeraldHp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Active (${spec.activeName}): ${spec.activeSummary}", style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { viewModel.equipPet(pet) },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("pet_equip_${pet.id}")
                        ) {
                            Text(if (isFollower) "Following" else "Follow", color = if (isFollower) EmeraldHp else ElectricCyan, fontSize = 11.sp, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { viewModel.togglePetRosterSlot(pet) },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("pet_roster_${pet.id}")
                        ) {
                            Text(if (pet.equipped) "Roster ON" else "+ Roster", color = if (pet.equipped) GoldCoin else Color.White, fontSize = 11.sp, maxLines = 1)
                        }
                        Button(
                            onClick = { viewModel.evolvePet(pet) },
                            enabled = evo < 3,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (evo >= 3) Color(0xFF334155) else GoldCoin, contentColor = if (evo >= 3) Color.White else Color.Black),
                            modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("pet_upgrade_${pet.id}")
                        ) {
                            if (evo < 3) Icon(Icons.Filled.Upgrade, contentDescription = null, modifier = Modifier.size(13.dp))
                            Text(if (evo >= 3) "MAX Lv.3" else "Evo ${upgradeCost}c", fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
        if (wildAvailable.isNotEmpty()) {
            item {
                Surface(
                    color = Color(0xFF10203A),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, ElectricCyan.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Wild Rare Lv.2/Lv.3 Pets Roaming World (${wildAvailable.size})", style = MaterialTheme.typography.labelMedium, color = ElectricCyan)
                        Text(
                            text = wildAvailable.take(4).joinToString(" • ") {
                                val s = PetCatalog.resolveSpec(it.petId)
                                "Lv.${it.evolutionLevel} ${s.name} (${PetCatalog.wildWorldSeeds.firstOrNull { seed -> seed.wildId == it.wildId }?.district ?: "Open World"})"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PetShopTab(viewModel: GameViewModel, ownedPets: List<OwnedPet>) {
    var selectedCat by remember { mutableStateOf<PetCategory?>(null) }
    var search by remember { mutableStateOf("") }
    val filtered = remember(selectedCat, search) {
        PetCatalog.findByCategory(selectedCat).filter { spec ->
            search.isBlank() ||
                spec.name.contains(search, ignoreCase = true) ||
                spec.passiveSummary.contains(search, ignoreCase = true) ||
                spec.activeSummary.contains(search, ignoreCase = true)
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("pet_shop_list"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search 50 animals, passives or actives...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search Pets", tint = ElectricCyan) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("pet_shop_search")
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    ChoiceChipBox("All (50)", selected = selectedCat == null, modifier = Modifier.testTag("pet_filter_all")) {
                        selectedCat = null
                    }
                }
                items(PetCategory.entries) { cat ->
                    val shortLabel = when (cat) {
                        PetCategory.LAND -> "Land"
                        PetCategory.FLYING -> "Flying"
                        PetCategory.REPTILE_FANTASY -> "Reptile"
                        PetCategory.WATER -> "Water"
                        PetCategory.MAGICAL -> "Magic"
                    }
                    ChoiceChipBox(shortLabel, selected = selectedCat == cat, modifier = Modifier.testTag("pet_filter_${cat.name}")) {
                        selectedCat = cat
                    }
                }
            }
        }
        items(filtered, key = { it.id }) { spec ->
            val owned = ownedPets.any { it.id == spec.id || it.petType.equals(spec.name, ignoreCase = true) }
            Surface(
                color = NavyCardElevated,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(spec.category.badgeHex).copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("#${spec.number} ${spec.name} • ${spec.rarity}", style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 1)
                            Text(spec.personality, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(
                            onClick = { IndraAudioEngine.playPetSound(spec.soundProfile, 1) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Preview ${spec.name}", tint = ElectricCyan, modifier = Modifier.size(18.dp))
                        }
                    }
                    Text("Passive: ${spec.passiveSummary}", style = MaterialTheme.typography.labelSmall, color = EmeraldHp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Active: ${spec.activeName} — ${spec.activeSummary}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        Button(
                            onClick = { viewModel.buyOrUpgradePet(spec.name, spec.rarity, spec.priceCoins, null) },
                            enabled = !owned,
                            colors = ButtonDefaults.buttonColors(containerColor = if (owned) Color(0xFF334155) else CrimsonAccent),
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("pet_shop_buy_${spec.id}")
                        ) {
                            Text(if (owned) "Owned" else if (spec.priceCoins == 0L) "Adopt" else "${spec.priceCoins}c", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PetAbilitiesTab(viewModel: GameViewModel, profile: PlayerProfile?, ownedPets: List<OwnedPet>) {
    val charSlots = profile?.unlockedCharSlots?.toInt()?.coerceIn(2, 3) ?: 2
    val petSlots = profile?.unlockedPetSlots?.toInt()?.coerceIn(2, 4) ?: 2
    val equipped = profile?.equippedAbilities ?: listOf("Crimson Slash", "Lightning Dash", "Fox Flame", "Aegis Pulse")
    var selectedPetSlotOffset by remember { mutableIntStateOf(0) }
    val ownedActives = remember(ownedPets) { ownedPets.map { PetCatalog.resolveSpec(it.petType) } }
    val allCharSkills = remember { GameCatalog.abilities.filter { it.source == "Character" } }

    LazyColumn(modifier = Modifier.fillMaxSize().testTag("pet_abilities_list"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Surface(
                color = NavyCardElevated,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, GoldCoin.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Character Skill Slots ($charSlots / 3)", style = MaterialTheme.typography.titleSmall, color = GoldCoin)
                        OutlinedButton(
                            onClick = { viewModel.unlockExtraAbilitySlot(forPet = false) },
                            enabled = charSlots < PetCatalog.MAX_ABILITY_SLOTS_CHAR,
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("unlock_char_slot_button")
                        ) {
                            Icon(Icons.Filled.LockOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text(if (charSlots >= 3) "Maxed" else "Unlock +1 (2 UP / 350c)", fontSize = 11.sp)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (c in 0 until charSlots) {
                            val current = equipped.getOrElse(c) { allCharSkills.getOrElse(c) { allCharSkills.first() }.name }
                            Surface(color = Color(0xFF0A1429), shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f).border(1.dp, GoldCoin.copy(alpha = 0.45f), RoundedCornerShape(8.dp))) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    Text("C${c + 1}", fontSize = 10.sp, color = GoldCoin, fontWeight = FontWeight.Bold)
                                    Text(current, fontSize = 11.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
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
                modifier = Modifier.fillMaxWidth().border(1.dp, VioletSkill.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Pet Active Skill Slots ($petSlots / 4)", style = MaterialTheme.typography.titleSmall, color = VioletSkill)
                        Button(
                            onClick = { viewModel.unlockExtraAbilitySlot(forPet = true) },
                            enabled = petSlots < PetCatalog.MAX_ABILITY_SLOTS_PET,
                            colors = ButtonDefaults.buttonColors(containerColor = VioletSkill),
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("unlock_pet_slot_button")
                        ) {
                            Icon(Icons.Filled.LockOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text(if (petSlots >= 4) "Maxed" else "Unlock +1 (2 UP / 350c)", fontSize = 11.sp)
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items((0 until petSlots).toList()) { pIdx ->
                            val curName = equipped.getOrElse(charSlots + pIdx) { "Fox Flame" }
                            ChoiceChipBox(
                                label = "P${pIdx + 1}: $curName",
                                selected = selectedPetSlotOffset == pIdx,
                                modifier = Modifier.testTag("select_pet_slot_$pIdx")
                            ) {
                                selectedPetSlotOffset = pIdx
                            }
                        }
                    }
                    Text("Assign Pet Skill to Slot P${selectedPetSlotOffset + 1}:", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                }
            }
        }

        val candidates = (ownedActives + PetCatalog.allPets).distinctBy { it.activeName }
        items(candidates, key = { "ab_${it.id}" }) { spec ->
            val currentInSlot = equipped.getOrNull(charSlots + selectedPetSlotOffset) == spec.activeName
            val ownedFlag = ownedActives.any { it.id == spec.id }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (currentInSlot) NavyCardElevated else Color(0xFF0A1224))
                    .border(1.dp, if (currentInSlot) GoldCoin else ElectricCyan.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                    .clickable { viewModel.equipPetAbilityToSlot(selectedPetSlotOffset, spec.activeName) }
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${spec.activeName} (${spec.name}${if (ownedFlag) " • Owned" else ""})", style = MaterialTheme.typography.titleSmall, color = Color.White)
                    Text("${spec.activeSummary} • EP ${spec.epCost.toInt()} • CD ${spec.activeCooldownSec.toInt()}s", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
                if (currentInSlot) Icon(Icons.Filled.Check, contentDescription = "Assigned", tint = EmeraldHp)
            }
        }
    }
}
