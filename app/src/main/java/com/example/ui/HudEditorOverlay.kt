package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.HudElementConfig
import com.example.data.model.HudElementId
import com.example.data.model.HudIconStyle
import com.example.data.model.HudLayoutPresets
import com.example.data.model.HudSide
import com.example.data.model.PlayerHudSettingsBundle
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.CyberBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldHp
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyPanel
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletSkill
import kotlin.math.roundToInt

@Composable
fun HudEditorOverlay(
    viewModel: GameViewModel,
    isGuestMode: Boolean,
    modifier: Modifier = Modifier
) {
    val persistedBundle by viewModel.hudSettingsBundle.collectAsStateWithLifecycle()
    var draftBundle by remember(persistedBundle) { mutableStateOf(persistedBundle) }
    var selectedId by remember { mutableStateOf(HudElementId.FIRE) }
    var inspectorExpanded by remember { mutableStateOf(true) }
    var renameDialogOpen by remember { mutableStateOf(false) }

    BackHandler { viewModel.closeHudEditor() }

    val activeLayout = draftBundle.activeLayout
    val selectedCfg = activeLayout.element(selectedId)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x36040812))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Subtle tactical alignment grid overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cols = 8
            val rows = 5
            for (c in 1 until cols) {
                val x = size.width * (c / cols.toFloat())
                drawLine(ElectricCyan.copy(alpha = 0.14f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            }
            for (r in 1 until rows) {
                val y = size.height * (r / rows.toFloat())
                drawLine(ElectricCyan.copy(alpha = 0.14f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }
        }

        // Draggable Interactive HUD Canvas
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            val canvasW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val canvasH = constraints.maxHeight.toFloat().coerceAtLeast(1f)

            HudElementId.entries.forEach { id ->
                val cfg = activeLayout.element(id)
                val isSelected = selectedId == id
                var elemSize by remember { mutableStateOf(IntSize(96, 96)) }

                val offsetX = (cfg.normX * canvasW - elemSize.width * 0.5f).roundToInt()
                val offsetY = (cfg.normY * canvasH - elemSize.height * 0.5f).roundToInt()

                Box(
                    modifier = Modifier
                        .offset { IntOffset(offsetX, offsetY) }
                        .onSizeChanged { elemSize = it }
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = when {
                                isSelected -> GoldCoin
                                !cfg.visible -> Color(0x88EF4444)
                                else -> ElectricCyan.copy(alpha = 0.45f)
                            },
                            shape = RoundedCornerShape(14.dp)
                        )
                        .background(
                            if (isSelected) GoldCoin.copy(alpha = 0.14f) else Color.Transparent,
                            RoundedCornerShape(14.dp)
                        )
                        .pointerInput(id, canvasW, canvasH, draftBundle) {
                            detectDragGestures(
                                onDragStart = { selectedId = id },
                                onDrag = { change, drag ->
                                    change.consume()
                                    val latest = draftBundle.activeLayout.element(id)
                                    val nx = (latest.normX + drag.x / canvasW).coerceIn(0.04f, 0.96f)
                                    val ny = (latest.normY + drag.y / canvasH).coerceIn(0.08f, 0.92f)
                                    draftBundle = draftBundle.withUpdatedActiveLayout(
                                        draftBundle.activeLayout.withElement(latest.copy(normX = nx, normY = ny))
                                    )
                                }
                            )
                        }
                        .clickable { selectedId = id }
                        .padding(4.dp)
                        .testTag("hud_editor_item_${id.name.lowercase()}")
                ) {
                    HudEditorElementPreview(cfg = cfg)
                }
            }
        }

        // Top Control Bar: Presets 1-5 + Rename + Save/Cancel/Reset
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            val isWide = maxWidth >= 600.dp
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    color = Color(0xE0091224),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                ) {
                    val presetTabs: @Composable (Modifier) -> Unit = { rowMod ->
                        LazyRow(
                            modifier = rowMod.testTag("hud_editor_presets_row"),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items((0..4).toList()) { idx ->
                                val layout = draftBundle.layouts.getOrElse(idx) { HudLayoutPresets.createPreset(idx) }
                                val selected = draftBundle.activeLayoutIndex == idx
                                Surface(
                                    color = if (selected) CrimsonAccent else Color(0xFF142244),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .border(1.dp, if (selected) GoldCoin else ElectricCyan.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                        .clickable { draftBundle = draftBundle.copy(activeLayoutIndex = idx) }
                                        .testTag("hud_editor_preset_$idx")
                                ) {
                                    Text(
                                        text = layout.name,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }
                            item {
                                IconButton(
                                    onClick = { renameDialogOpen = true },
                                    modifier = Modifier.size(32.dp).testTag("hud_editor_rename")
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Rename Preset", tint = GoldCoin, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    val actionButtons: @Composable () -> Unit = {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = {
                                    val reset = HudLayoutPresets.createPreset(draftBundle.activeLayoutIndex)
                                    draftBundle = draftBundle.withUpdatedActiveLayout(reset)
                                },
                                modifier = Modifier.height(34.dp).testTag("hud_editor_reset")
                            ) {
                                Icon(Icons.Filled.Restore, contentDescription = null, tint = GoldCoin, modifier = Modifier.size(15.dp))
                                Text(" Reset", fontSize = 11.sp, color = GoldCoin)
                            }
                            OutlinedButton(
                                onClick = { viewModel.closeHudEditor() },
                                modifier = Modifier.height(34.dp).testTag("hud_editor_cancel")
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                Text(" Cancel", fontSize = 11.sp, color = Color.White)
                            }
                            Button(
                                onClick = {
                                    viewModel.saveHudSettingsBundle(draftBundle)
                                    viewModel.closeHudEditor()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldHp, contentColor = Color.Black),
                                modifier = Modifier.height(34.dp).testTag("hud_editor_save")
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                                Text(" Save", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (isWide) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            presetTabs(Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(8.dp))
                            actionButtons()
                        }
                    } else {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            presetTabs(Modifier.fillMaxWidth())
                            actionButtons()
                        }
                    }
                }

                if (isGuestMode) {
                    Surface(
                        color = Color(0xDB2E1D07),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .border(1.dp, GoldCoin.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                            .testTag("hud_editor_guest_warning")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = GoldCoin, modifier = Modifier.size(14.dp))
                            Text(
                                text = "Guest Mode: HUD layouts save locally on this device. Link Google or Facebook to cloud sync.",
                                fontSize = 11.sp,
                                color = GoldCoin
                            )
                        }
                    }
                }
            }
        }

        // Bottom Property Inspector Drawer (Size, Opacity, Spacing, Visibility, L/R Swap, Icon Style)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = Color(0xE80A1328),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .border(1.dp, ElectricCyan.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.OpenWith, contentDescription = null, tint = GoldCoin, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Selected: ${selectedId.displayName} (${(selectedCfg.sizeScale * 100).toInt()}% • ${(selectedCfg.opacity * 100).toInt()}%)",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White
                            )
                            if (selectedId.essential) {
                                Text("Essential", fontSize = 10.sp, color = EmeraldHp, modifier = Modifier.background(EmeraldHp.copy(alpha = 0.18f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        TextButton(onClick = { inspectorExpanded = !inspectorExpanded }) {
                            Text(if (inspectorExpanded) "Minimize" else "Customize Button", color = ElectricCyan, fontSize = 11.sp)
                        }
                    }

                    if (inspectorExpanded) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Size ${(selectedCfg.sizeScale * 100).toInt()}%", fontSize = 10.sp, color = TextSecondary)
                                Slider(
                                    value = selectedCfg.sizeScale,
                                    onValueChange = { v ->
                                        draftBundle = draftBundle.withUpdatedActiveLayout(
                                            activeLayout.withElement(selectedCfg.copy(sizeScale = v))
                                        )
                                    },
                                    valueRange = 0.5f..2.0f,
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Opacity ${(selectedCfg.opacity * 100).toInt()}%", fontSize = 10.sp, color = TextSecondary)
                                Slider(
                                    value = selectedCfg.opacity,
                                    onValueChange = { v ->
                                        draftBundle = draftBundle.withUpdatedActiveLayout(
                                            activeLayout.withElement(selectedCfg.copy(opacity = v))
                                        )
                                    },
                                    valueRange = 0.2f..1.0f,
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Spacing ${(selectedCfg.spacingScale * 100).toInt()}%", fontSize = 10.sp, color = TextSecondary)
                                Slider(
                                    value = selectedCfg.spacingScale,
                                    onValueChange = { v ->
                                        draftBundle = draftBundle.withUpdatedActiveLayout(
                                            activeLayout.withElement(selectedCfg.copy(spacingScale = v))
                                        )
                                    },
                                    valueRange = 0.7f..1.6f,
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Icon Style switcher
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                HudIconStyle.entries.forEach { st ->
                                    val chosen = selectedCfg.iconStyle == st
                                    Surface(
                                        color = if (chosen) ElectricCyan else Color(0xFF142244),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.clickable {
                                            draftBundle = draftBundle.withUpdatedActiveLayout(
                                                activeLayout.withElement(selectedCfg.copy(iconStyle = st))
                                            )
                                        }
                                    ) {
                                        Text(
                                            text = st.label,
                                            fontSize = 10.sp,
                                            color = if (chosen) Color.Black else Color.White,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                // Left / Right quick swap button
                                OutlinedButton(
                                    onClick = {
                                        val nextSide = if (selectedCfg.normX < 0.5f) HudSide.RIGHT else HudSide.LEFT
                                        draftBundle = draftBundle.withUpdatedActiveLayout(
                                            activeLayout.withElement(
                                                selectedCfg.copy(
                                                    normX = (1f - selectedCfg.normX).coerceIn(0.05f, 0.95f),
                                                    side = nextSide
                                                )
                                            )
                                        )
                                    },
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Filled.SwapHoriz, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(14.dp))
                                    Text(" Swap L/R", fontSize = 10.sp, color = ElectricCyan)
                                }

                                // Visibility switch (non-essential only)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Show", fontSize = 10.sp, color = TextSecondary)
                                    Switch(
                                        checked = selectedCfg.visible,
                                        enabled = !selectedId.essential,
                                        onCheckedChange = { checked ->
                                            draftBundle = draftBundle.withUpdatedActiveLayout(
                                                activeLayout.withElement(selectedCfg.copy(visible = checked))
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (renameDialogOpen) {
        var newName by remember(activeLayout.name) { mutableStateOf(activeLayout.name) }
        AlertDialog(
            onDismissRequest = { renameDialogOpen = false },
            containerColor = NavyCard,
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("Rename Layout Preset") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it.take(24) },
                    label = { Text("Preset Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = newName.trim().ifBlank { activeLayout.name }
                        val nextLayouts = draftBundle.layouts.mapIndexed { idx, l ->
                            if (idx == draftBundle.activeLayoutIndex) l.copy(name = clean) else l
                        }
                        draftBundle = draftBundle.copy(layouts = nextLayouts)
                        renameDialogOpen = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent)
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameDialogOpen = false }) {
                    Text("Cancel", color = ElectricCyan)
                }
            }
        )
    }
}

@Composable
private fun HudEditorElementPreview(cfg: HudElementConfig) {
    val alpha = if (cfg.visible) cfg.opacity else 0.28f
    when (cfg.id) {
        HudElementId.MINIMAP -> {
            val s = (84f * cfg.sizeScale.coerceIn(0.6f, 1.6f)).dp
            Box(
                modifier = Modifier
                    .alpha(alpha)
                    .size(s)
                    .background(NavyPanel, RoundedCornerShape(12.dp))
                    .border(1.5.dp, ElectricCyan, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Map, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(22.dp))
                    Text("MAP OFF", fontSize = 9.sp, color = Color.White)
                }
            }
        }
        HudElementId.JOYSTICK -> {
            val s = (104f * cfg.sizeScale.coerceIn(0.6f, 1.7f)).dp
            Box(
                modifier = Modifier
                    .alpha(alpha)
                    .size(s)
                    .background(Color(0x660D162C), CircleShape)
                    .border(1.5.dp, ElectricCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size((s.value * 0.42f).dp)
                        .clip(CircleShape)
                        .background(CrimsonAccent.copy(alpha = 0.85f))
                        .border(1.5.dp, Color.White, CircleShape)
                )
            }
        }
        HudElementId.HP_EP_BARS -> {
            val w = (180f * cfg.sizeScale.coerceIn(0.65f, 1.5f)).dp
            Column(
                modifier = Modifier
                    .alpha(alpha)
                    .width(w)
                    .background(Color(0xCC070C1A), RoundedCornerShape(8.dp))
                    .border(1.dp, EmeraldHp, RoundedCornerShape(8.dp))
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(GoldCoin, RoundedCornerShape(2.dp)))
                Box(modifier = Modifier.fillMaxWidth().height(7.dp).background(EmeraldHp, RoundedCornerShape(3.dp)))
                Text("HP / EP BAR", fontSize = 9.sp, color = Color.White)
            }
        }
        HudElementId.ABILITIES -> {
            Row(
                modifier = Modifier.alpha(alpha),
                horizontalArrangement = Arrangement.spacedBy((6f * cfg.spacingScale).dp)
            ) {
                repeat(4) { i ->
                    Box(
                        modifier = Modifier
                            .size((34f * cfg.sizeScale.coerceIn(0.6f, 1.6f)).dp)
                            .background(Color(0xCC0A1226), CircleShape)
                            .border(1.5.dp, if (i < 2) GoldCoin else VioletSkill, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Bolt, contentDescription = null, tint = if (i < 2) GoldCoin else VioletSkill, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        HudElementId.MISSION_PILL -> {
            Surface(
                color = NavyPanel,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .alpha(alpha)
                    .border(1.dp, GoldCoin, RoundedCornerShape(10.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Filled.Flag, contentDescription = null, tint = GoldCoin, modifier = Modifier.size(13.dp))
                    Text("MISSION", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        HudElementId.UTILITY_RAIL -> {
            Column(
                modifier = Modifier.alpha(alpha),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy((4f * cfg.spacingScale).dp)
            ) {
                listOf(
                    Icons.Filled.Backpack to ElectricCyan,
                    Icons.Filled.ShoppingCart to GoldCoin,
                    Icons.Filled.LocalHospital to EmeraldHp,
                    Icons.Filled.Map to CyberBlue
                ).forEach { (ic, tint) ->
                    Box(
                        modifier = Modifier
                            .size((30f * cfg.sizeScale.coerceIn(0.6f, 1.5f)).dp)
                            .background(Color(0xCC0A1226), CircleShape)
                            .border(1.2.dp, tint, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(ic, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
        HudElementId.WEAPON_SWITCH -> {
            Surface(
                color = NavyPanel,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .alpha(alpha)
                    .border(1.dp, ElectricCyan, RoundedCornerShape(10.dp))
            ) {
                Text(
                    text = "AR / PST / KTN / FST",
                    fontSize = 10.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
        else -> {
            val (icon, color) = when (cfg.id) {
                HudElementId.SPRINT -> Icons.AutoMirrored.Filled.DirectionsRun to GoldCoin
                HudElementId.INTERACT -> Icons.Filled.TouchApp to ElectricCyan
                HudElementId.JUMP -> Icons.Filled.KeyboardArrowUp to ElectricCyan
                HudElementId.CROUCH -> Icons.Filled.Shield to CyberBlue
                HudElementId.PRONE -> Icons.Filled.KeyboardArrowDown to CyberBlue
                HudElementId.GRENADE -> Icons.Filled.Bolt to GoldCoin
                HudElementId.AIM -> Icons.Filled.GpsFixed to ElectricCyan
                HudElementId.FIRE -> Icons.Filled.MyLocation to CrimsonAccent
                HudElementId.MELEE -> Icons.Filled.Bolt to CrimsonAccent
                HudElementId.RELOAD -> Icons.Filled.Refresh to GoldCoin
                HudElementId.MINIMAP,
                HudElementId.MISSION_PILL,
                HudElementId.UTILITY_RAIL,
                HudElementId.JOYSTICK,
                HudElementId.HP_EP_BARS,
                HudElementId.ABILITIES,
                HudElementId.WEAPON_SWITCH -> Icons.Filled.TouchApp to ElectricCyan
            }
            val baseDp = if (cfg.id == HudElementId.FIRE) 60.dp else 44.dp
            FuturisticCircleButton(
                icon = icon,
                contentDescription = cfg.id.displayName,
                tag = "preview_${cfg.id.name.lowercase()}",
                baseSize = baseDp,
                scaleFactor = cfg.sizeScale,
                opacity = alpha,
                iconStyle = cfg.iconStyle,
                accentColor = color,
                active = cfg.id == HudElementId.FIRE,
                badgeLabel = cfg.id.name.take(4),
                onClick = {}
            )
        }
    }
}
