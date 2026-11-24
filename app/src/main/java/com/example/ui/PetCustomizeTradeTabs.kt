package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.IndraAudioEngine
import com.example.data.model.OwnedPet
import com.example.data.model.PetCatalog
import com.example.data.model.PetTradeOffer
import com.example.data.model.PlayerProfile
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldHp
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyCardElevated
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletSkill

private val petTintSwatches = listOf(
    "" to ("Natural" to Color(0xFF94A3B8)),
    "#FF6B9E" to ("Sakura" to Color(0xFFFF6B9E)),
    "#00E5FF" to ("Cyber" to Color(0xFF00E5FF)),
    "#FFD166" to ("Solar" to Color(0xFFFFD166)),
    "#FF3B5C" to ("Crimson" to Color(0xFFFF3B5C)),
    "#A855F7" to ("Void" to Color(0xFFA855F7)),
    "#22C55E" to ("Jade" to Color(0xFF22C55E))
)

private val petAuraStyles = listOf("Classic", "Sakura", "Thunder", "Celestial", "Shadow")

@Composable
internal fun PetCustomizeTab(viewModel: GameViewModel, ownedPets: List<OwnedPet>) {
    var selectedId by remember(ownedPets) { mutableStateOf(ownedPets.firstOrNull()?.id ?: "") }
    val targetPet = ownedPets.firstOrNull { it.id == selectedId } ?: ownedPets.firstOrNull()
    var nickname by remember(targetPet?.id) { mutableStateOf(targetPet?.nickname ?: "") }
    var tintHex by remember(targetPet?.id) { mutableStateOf(targetPet?.customTintHex ?: "") }
    var aura by remember(targetPet?.id) { mutableStateOf(targetPet?.auraStyle ?: "Classic") }

    if (targetPet == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Adopt a pet first to customize it!", color = TextSecondary)
        }
        return
    }
    val spec = PetCatalog.resolveSpec(targetPet.petType)
    LazyColumn(modifier = Modifier.fillMaxSize().testTag("pet_customize_list"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Select Owned Pet to Style", style = MaterialTheme.typography.labelMedium, color = ElectricCyan)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                items(ownedPets, key = { it.id }) { p ->
                    ChoiceChipBox(
                        label = p.nickname.ifBlank { p.petType },
                        selected = p.id == targetPet.id,
                        modifier = Modifier.testTag("pet_cust_select_${p.id}")
                    ) {
                        selectedId = p.id
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it.take(30) },
                label = { Text("Custom Pet Nickname") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("pet_nickname_input")
            )
        }
        item {
            Text("Chroma Coat Tint", style = MaterialTheme.typography.labelMedium, color = GoldCoin)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                items(petTintSwatches) { (hex, pair) ->
                    val (label, col) = pair
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(2.5.dp, if (tintHex == hex) Color.White else Color.Transparent, CircleShape)
                                .clickable { tintHex = hex }
                                .testTag("pet_tint_${label.lowercase()}")
                        )
                        Text(label, fontSize = 10.sp, color = TextSecondary)
                    }
                }
            }
        }
        item {
            Text("Evolution Aura Ring Effect", style = MaterialTheme.typography.labelMedium, color = VioletSkill)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                items(petAuraStyles) { style ->
                    ChoiceChipBox(
                        label = style,
                        selected = aura == style,
                        modifier = Modifier.testTag("pet_aura_${style.lowercase()}")
                    ) {
                        aura = style
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { IndraAudioEngine.playPetSound(spec.soundProfile, targetPet.effectiveEvolutionLevel()) },
                    modifier = Modifier.weight(0.4f).minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(" Test Sound", fontSize = 12.sp)
                }
                Button(
                    onClick = { viewModel.customizePet(targetPet, nickname, tintHex, aura) },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent),
                    modifier = Modifier.weight(0.6f).minimumInteractiveComponentSize().testTag("pet_customize_save")
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(" Save Pet Look", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
internal fun PetTradeTab(
    viewModel: GameViewModel,
    ownedPets: List<OwnedPet>,
    trades: List<PetTradeOffer>,
    onlinePlayers: List<PlayerProfile>
) {
    var selectedOfferPetId by remember(ownedPets) { mutableStateOf(ownedPets.firstOrNull()?.id ?: "") }
    var selectedTargetUid by remember { mutableStateOf<String?>(null) }
    var selectedRequestSpec by remember { mutableStateOf(PetCatalog.allPets[4]) }
    var showConfirmModal by remember { mutableStateOf(false) }

    val offeredPet = ownedPets.firstOrNull { it.id == selectedOfferPetId } ?: ownedPets.firstOrNull()
    val partnerPlayer = onlinePlayers.firstOrNull { it.userId == selectedTargetUid }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("pet_trade_list"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (trades.isNotEmpty()) {
            item { Text("Active Trade Offers", style = MaterialTheme.typography.titleSmall, color = GoldCoin) }
            items(trades, key = { it.id }) { tr ->
                Surface(color = NavyCardElevated, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().border(1.dp, GoldCoin.copy(alpha = 0.5f), RoundedCornerShape(12.dp))) {
                    Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${tr.senderName} offers Lv.${tr.offeredEvolutionLevel} ${tr.offeredPetType}", style = MaterialTheme.typography.titleSmall, color = Color.White)
                            Text("Requesting: ${tr.requestedPetType} • Status: ${tr.status}", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                        }
                        if (tr.receiverUid == viewModel.currentUserId && tr.status == "pending") {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(onClick = { viewModel.respondPetTrade(tr, true) }, colors = ButtonDefaults.buttonColors(containerColor = EmeraldHp, contentColor = Color.Black)) {
                                    Text("Accept", fontSize = 11.sp)
                                }
                                OutlinedButton(onClick = { viewModel.respondPetTrade(tr, false) }) {
                                    Text("Decline", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("Create Pet Trade (2-Step Confirmed Exchange)", style = MaterialTheme.typography.titleSmall, color = ElectricCyan)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("1. Trade Partner:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        ChoiceChipBox("Sakura Guild Trader", selected = selectedTargetUid == null) { selectedTargetUid = null }
                    }
                    items(onlinePlayers, key = { it.userId }) { pl ->
                        ChoiceChipBox(pl.username, selected = selectedTargetUid == pl.userId) { selectedTargetUid = pl.userId }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("2. Pet You Offer (${ownedPets.size} owned):", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ownedPets, key = { "tr_${it.id}" }) { p ->
                        ChoiceChipBox(
                            label = "${p.petType} Lv.${p.effectiveEvolutionLevel()}",
                            selected = p.id == offeredPet?.id,
                            modifier = Modifier.testTag("pet_trade_offer_${p.id}")
                        ) { selectedOfferPetId = p.id }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("3. Pet Requested in Return:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(PetCatalog.allPets, key = { "req_${it.id}" }) { sp ->
                        ChoiceChipBox(
                            label = "#${sp.number} ${sp.name}",
                            selected = selectedRequestSpec.id == sp.id,
                            modifier = Modifier.testTag("pet_trade_req_${sp.id}")
                        ) { selectedRequestSpec = sp }
                    }
                }
            }
        }
        item {
            Button(
                onClick = { showConfirmModal = true },
                enabled = offeredPet != null,
                colors = ButtonDefaults.buttonColors(containerColor = GoldCoin, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("pet_trade_review_button")
            ) {
                Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" Review & Confirm Trade", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showConfirmModal && offeredPet != null) {
        AlertDialog(
            onDismissRequest = { showConfirmModal = false },
            containerColor = NavyCard,
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("Confirm Pet Trade Exchange") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Partner: ${partnerPlayer?.username ?: "Sakura Guild Trader"}", color = ElectricCyan)
                    Text("Giving: Lv.${offeredPet.effectiveEvolutionLevel()} ${offeredPet.petType} (${offeredPet.rarity})", color = CrimsonAccent)
                    Text("Receiving: ${selectedRequestSpec.name} (${selectedRequestSpec.rarity})", color = EmeraldHp)
                    Text("Confirming transfers your offered companion immediately.", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmModal = false
                        viewModel.sendPetTradeOffer(partnerPlayer, offeredPet, selectedRequestSpec)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldHp, contentColor = Color.Black),
                    modifier = Modifier.testTag("pet_trade_confirm_button")
                ) {
                    Text("Confirm Trade")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmModal = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
