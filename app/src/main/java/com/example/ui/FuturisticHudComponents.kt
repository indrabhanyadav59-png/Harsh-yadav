package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HudIconStyle
import com.example.data.model.MissionProgress
import com.example.data.model.WeaponType
import com.example.game.engine.DamageFeedbackEvent
import com.example.game.engine.WorldHudSnapshot
import com.example.ui.theme.AmberEp
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.CyberBlue
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldHp
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.NavyPanel
import com.example.ui.theme.VioletSkill

@Composable
fun FuturisticCircleButton(
    icon: ImageVector,
    contentDescription: String,
    tag: String,
    modifier: Modifier = Modifier,
    baseSize: Dp = 50.dp,
    scaleFactor: Float = 1f,
    opacity: Float = 0.92f,
    iconStyle: HudIconStyle = HudIconStyle.NEON_GLASS,
    accentColor: Color = ElectricCyan,
    active: Boolean = false,
    cooldownSec: Float = 0f,
    maxCooldownSec: Float = 6f,
    badgeLabel: String? = null,
    subLabel: String? = null,
    vectorArt: (DrawScope.(Color) -> Unit)? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "btn_scale"
    )
    val scaledSize = (baseSize.value * scaleFactor.coerceIn(0.5f, 2.0f)).dp

    val bgBrush = when {
        active -> Brush.radialGradient(listOf(accentColor.copy(alpha = 0.72f), Color(0xD80A1428)))
        iconStyle == HudIconStyle.ANIME_CREST -> Brush.radialGradient(listOf(Color(0xB81B1236), Color(0xCC070B18)))
        iconStyle == HudIconStyle.TACTICAL_RING -> Brush.radialGradient(listOf(Color(0xA60F172A), Color(0xC8050A14)))
        else -> Brush.radialGradient(listOf(Color(0x9E10203D), Color(0xBD060A16)))
    }
    val rimColor = when {
        cooldownSec > 0f -> Color(0xFF64748B)
        active -> GoldCoin
        iconStyle == HudIconStyle.ANIME_CREST -> VioletSkill
        iconStyle == HudIconStyle.TACTICAL_RING -> CyberBlue
        else -> accentColor
    }

    Column(
        modifier = modifier.alpha(opacity.coerceIn(0.2f, 1f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .scale(pressScale)
                .minimumInteractiveComponentSize()
                .size(scaledSize)
                .clip(CircleShape)
                .background(bgBrush, CircleShape)
                .border(
                    width = if (active || iconStyle == HudIconStyle.TACTICAL_RING) 2.dp else 1.6.dp,
                    color = rimColor.copy(alpha = if (active) 0.96f else 0.82f),
                    shape = CircleShape
                )
                .semantics { this.contentDescription = contentDescription }
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                .testTag(tag),
            contentAlignment = Alignment.Center
        ) {
            // Inner neon ring & cooldown sweep
            Canvas(modifier = Modifier.fillMaxSize().padding(3.dp)) {
                drawCircle(
                    color = rimColor.copy(alpha = if (active) 0.45f else 0.22f),
                    style = Stroke(width = 1.4f)
                )
                if (iconStyle == HudIconStyle.ANIME_CREST) {
                    drawCircle(
                        color = GoldCoin.copy(alpha = 0.38f),
                        radius = size.minDimension * 0.43f,
                        style = Stroke(width = 1.2f)
                    )
                }
                if (cooldownSec > 0f && maxCooldownSec > 0f) {
                    val frac = (cooldownSec / maxCooldownSec).coerceIn(0f, 1f)
                    drawArc(
                        color = GoldCoin,
                        startAngle = -90f,
                        sweepAngle = 360f * frac,
                        useCenter = false,
                        style = Stroke(width = 3.2f, cap = StrokeCap.Round)
                    )
                }
            }

            if (cooldownSec > 0f) {
                Text(
                    text = "${cooldownSec.toInt() + 1}s",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = GoldCoin
                )
            } else if (vectorArt != null) {
                val artTint = if (active) Color.White else Color(0xFFF8FAFC)
                Canvas(
                    modifier = Modifier
                        .size((scaledSize.value * 0.56f).coerceIn(18f, 46f).dp)
                ) {
                    vectorArt(artTint)
                }
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = if (active) Color.White else Color(0xFFF8FAFC),
                    modifier = Modifier.size((scaledSize.value * 0.46f).coerceIn(14f, 40f).dp)
                )
            }

            if (!badgeLabel.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 3.dp, bottom = 2.dp)
                        .background(Color(0xD9070B16), RoundedCornerShape(6.dp))
                        .border(0.8.dp, rimColor.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 4.dp, vertical = 0.5.dp)
                ) {
                    Text(
                        text = badgeLabel,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldCoin
                    )
                }
            }
        }

        if (!subLabel.isNullOrBlank()) {
            Text(
                text = subLabel,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF1F5F9),
                modifier = Modifier
                    .background(Color(0x88060B18), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 0.5.dp)
            )
        }
    }
}

object HudVectorArtwork {

    fun DrawScope.drawBulletIcon(tint: Color) {
        rotate(-42f) {
            val w = size.width
            val h = size.height
            val body = Path().apply {
                moveTo(w * 0.34f, h * 0.85f)
                lineTo(w * 0.34f, h * 0.38f)
                quadraticTo(w * 0.50f, h * 0.08f, w * 0.66f, h * 0.38f)
                lineTo(w * 0.66f, h * 0.85f)
                close()
            }
            drawPath(body, color = tint)
            drawLine(
                color = Color(0xFF0A1326),
                start = Offset(w * 0.34f, h * 0.68f),
                end = Offset(w * 0.66f, h * 0.68f),
                strokeWidth = 2.4f
            )
            // Speed trails
            drawLine(tint.copy(alpha = 0.75f), Offset(w * 0.24f, h * 0.78f), Offset(w * 0.24f, h * 0.94f), strokeWidth = 2f, cap = StrokeCap.Round)
            drawLine(tint.copy(alpha = 0.75f), Offset(w * 0.76f, h * 0.78f), Offset(w * 0.76f, h * 0.94f), strokeWidth = 2f, cap = StrokeCap.Round)
        }
    }

    fun DrawScope.drawKatanaSlashIcon(tint: Color) {
        val w = size.width
        val h = size.height
        // Curved energy slash arc
        val arc = Path().apply {
            moveTo(w * 0.14f, h * 0.78f)
            quadraticTo(w * 0.52f, h * 0.12f, w * 0.88f, h * 0.22f)
            quadraticTo(w * 0.58f, h * 0.34f, w * 0.24f, h * 0.82f)
            close()
        }
        drawPath(arc, color = tint)
        // Katana hilt & crossguard
        drawLine(GoldCoin, Offset(w * 0.14f, h * 0.86f), Offset(w * 0.30f, h * 0.70f), strokeWidth = 3.2f, cap = StrokeCap.Round)
        drawLine(GoldCoin, Offset(w * 0.22f, h * 0.65f), Offset(w * 0.35f, h * 0.78f), strokeWidth = 2.6f, cap = StrokeCap.Round)
    }

    fun DrawScope.drawFistIcon(tint: Color) {
        val w = size.width
        val h = size.height
        // Knuckles & fist block
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.24f, h * 0.26f),
            size = Size(w * 0.52f, h * 0.42f),
            cornerRadius = CornerRadius(8f, 8f)
        )
        // Wrist cuff
        drawRoundRect(
            color = GoldCoin,
            topLeft = Offset(w * 0.30f, h * 0.71f),
            size = Size(w * 0.40f, h * 0.14f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        // Knuckle ridges
        for (i in 1..3) {
            val x = w * (0.24f + i * 0.13f)
            drawLine(Color(0xFF0A1326), Offset(x, h * 0.26f), Offset(x, h * 0.48f), strokeWidth = 2f)
        }
    }

    fun DrawScope.drawScopeAimIcon(tint: Color) {
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        val r = size.minDimension * 0.34f
        drawCircle(tint, radius = r, style = Stroke(width = 2.4f))
        drawCircle(tint, radius = r * 0.22f)
        drawLine(tint, Offset(cx - r * 1.38f, cy), Offset(cx - r * 0.55f, cy), strokeWidth = 2.4f, cap = StrokeCap.Round)
        drawLine(tint, Offset(cx + r * 0.55f, cy), Offset(cx + r * 1.38f, cy), strokeWidth = 2.4f, cap = StrokeCap.Round)
        drawLine(tint, Offset(cx, cy - r * 1.38f), Offset(cx, cy - r * 0.55f), strokeWidth = 2.4f, cap = StrokeCap.Round)
        drawLine(tint, Offset(cx, cy + r * 0.55f), Offset(cx, cy + r * 1.38f), strokeWidth = 2.4f, cap = StrokeCap.Round)
    }

    fun DrawScope.drawJumpIcon(tint: Color) {
        val w = size.width
        val h = size.height
        // Head
        drawCircle(tint, radius = w * 0.10f, center = Offset(w * 0.54f, h * 0.22f))
        // Dynamic leaping figure
        drawLine(tint, Offset(w * 0.52f, h * 0.34f), Offset(w * 0.44f, h * 0.56f), strokeWidth = 3.2f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.44f, h * 0.56f), Offset(w * 0.28f, h * 0.72f), strokeWidth = 2.8f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.44f, h * 0.56f), Offset(w * 0.66f, h * 0.68f), strokeWidth = 2.8f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.50f, h * 0.40f), Offset(w * 0.72f, h * 0.30f), strokeWidth = 2.6f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.50f, h * 0.40f), Offset(w * 0.26f, h * 0.46f), strokeWidth = 2.6f, cap = StrokeCap.Round)
        // Upward arrow arc
        drawLine(GoldCoin, Offset(w * 0.76f, h * 0.82f), Offset(w * 0.76f, h * 0.48f), strokeWidth = 2.2f, cap = StrokeCap.Round)
        drawLine(GoldCoin, Offset(w * 0.68f, h * 0.56f), Offset(w * 0.76f, h * 0.48f), strokeWidth = 2.2f, cap = StrokeCap.Round)
        drawLine(GoldCoin, Offset(w * 0.84f, h * 0.56f), Offset(w * 0.76f, h * 0.48f), strokeWidth = 2.2f, cap = StrokeCap.Round)
    }

    fun DrawScope.drawCrouchIcon(tint: Color) {
        val w = size.width
        val h = size.height
        drawCircle(tint, radius = w * 0.10f, center = Offset(w * 0.48f, h * 0.28f))
        drawLine(tint, Offset(w * 0.46f, h * 0.40f), Offset(w * 0.38f, h * 0.62f), strokeWidth = 3.2f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.38f, h * 0.62f), Offset(w * 0.60f, h * 0.66f), strokeWidth = 2.8f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.60f, h * 0.66f), Offset(w * 0.58f, h * 0.84f), strokeWidth = 2.8f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.38f, h * 0.62f), Offset(w * 0.28f, h * 0.82f), strokeWidth = 2.8f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.44f, h * 0.46f), Offset(w * 0.72f, h * 0.46f), strokeWidth = 2.5f, cap = StrokeCap.Round)
    }

    fun DrawScope.drawProneIcon(tint: Color) {
        val w = size.width
        val h = size.height
        // Flat prone operator silhouette
        drawCircle(tint, radius = w * 0.09f, center = Offset(w * 0.70f, h * 0.50f))
        drawLine(tint, Offset(w * 0.18f, h * 0.68f), Offset(w * 0.62f, h * 0.60f), strokeWidth = 3.4f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.62f, h * 0.60f), Offset(w * 0.82f, h * 0.64f), strokeWidth = 2.6f, cap = StrokeCap.Round)
        drawLine(tint.copy(alpha = 0.6f), Offset(w * 0.14f, h * 0.78f), Offset(w * 0.86f, h * 0.78f), strokeWidth = 2f, cap = StrokeCap.Round)
    }

    fun DrawScope.drawGrenadeIcon(tint: Color) {
        val w = size.width
        val h = size.height
        // Fuse pin & lever
        drawLine(GoldCoin, Offset(w * 0.44f, h * 0.20f), Offset(w * 0.58f, h * 0.20f), strokeWidth = 2.5f, cap = StrokeCap.Round)
        drawCircle(GoldCoin, radius = w * 0.08f, center = Offset(w * 0.67f, h * 0.24f), style = Stroke(width = 2f))
        // Body
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.30f, h * 0.32f),
            size = Size(w * 0.40f, h * 0.50f),
            cornerRadius = CornerRadius(14f, 14f)
        )
        drawLine(Color(0xFF0A1326), Offset(w * 0.30f, h * 0.48f), Offset(w * 0.70f, h * 0.48f), strokeWidth = 2f)
        drawLine(Color(0xFF0A1326), Offset(w * 0.30f, h * 0.64f), Offset(w * 0.70f, h * 0.64f), strokeWidth = 2f)
        drawLine(Color(0xFF0A1326), Offset(w * 0.50f, h * 0.32f), Offset(w * 0.50f, h * 0.82f), strokeWidth = 2f)
    }

    fun DrawScope.drawMedkitIcon(tint: Color) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.18f, h * 0.28f),
            size = Size(w * 0.64f, h * 0.52f),
            cornerRadius = CornerRadius(6f, 6f),
            style = Stroke(width = 2.6f)
        )
        drawLine(tint, Offset(w * 0.38f, h * 0.28f), Offset(w * 0.38f, h * 0.18f), strokeWidth = 2.4f)
        drawLine(tint, Offset(w * 0.62f, h * 0.28f), Offset(w * 0.62f, h * 0.18f), strokeWidth = 2.4f)
        drawLine(tint, Offset(w * 0.38f, h * 0.18f), Offset(w * 0.62f, h * 0.18f), strokeWidth = 2.4f)
        // Cross
        drawLine(EmeraldHp, Offset(w * 0.50f, h * 0.38f), Offset(w * 0.50f, h * 0.70f), strokeWidth = 4f, cap = StrokeCap.Round)
        drawLine(EmeraldHp, Offset(w * 0.34f, h * 0.54f), Offset(w * 0.66f, h * 0.54f), strokeWidth = 4f, cap = StrokeCap.Round)
    }

    fun DrawScope.drawWeaponSilhouette(weaponType: WeaponType, tint: Color) {
        val w = size.width
        val h = size.height
        when (weaponType) {
            WeaponType.FIREARM -> {
                // Sleek rifle/carbine silhouette
                drawRect(tint, topLeft = Offset(w * 0.14f, h * 0.38f), size = Size(w * 0.62f, h * 0.16f))
                drawRect(tint, topLeft = Offset(w * 0.76f, h * 0.42f), size = Size(w * 0.16f, h * 0.06f)) // barrel
                drawRect(tint, topLeft = Offset(w * 0.06f, h * 0.42f), size = Size(w * 0.12f, h * 0.18f)) // stock
                drawRect(tint, topLeft = Offset(w * 0.45f, h * 0.54f), size = Size(w * 0.10f, h * 0.22f)) // mag
                drawRect(tint, topLeft = Offset(w * 0.26f, h * 0.54f), size = Size(w * 0.09f, h * 0.16f)) // grip
                drawRect(tint.copy(alpha = 0.75f), topLeft = Offset(w * 0.32f, h * 0.28f), size = Size(w * 0.22f, h * 0.08f)) // rail sight
            }
            WeaponType.KATANA -> {
                // Angled katana blade silhouette
                drawLine(tint, Offset(w * 0.12f, h * 0.76f), Offset(w * 0.88f, h * 0.20f), strokeWidth = 3.5f, cap = StrokeCap.Round)
                drawLine(GoldCoin, Offset(w * 0.24f, h * 0.55f), Offset(w * 0.36f, h * 0.72f), strokeWidth = 2.8f, cap = StrokeCap.Round)
            }
            WeaponType.FISTS -> {
                drawFistIcon(tint)
            }
        }
    }

    fun DrawScope.drawArmorCrestIcon(tint: Color) {
        val w = size.width
        val h = size.height
        val crest = Path().apply {
            moveTo(w * 0.22f, h * 0.20f)
            lineTo(w * 0.36f, h * 0.14f)
            lineTo(w * 0.64f, h * 0.14f)
            lineTo(w * 0.78f, h * 0.20f)
            lineTo(w * 0.84f, h * 0.44f)
            lineTo(w * 0.74f, h * 0.48f)
            lineTo(w * 0.72f, h * 0.86f)
            lineTo(w * 0.28f, h * 0.86f)
            lineTo(w * 0.26f, h * 0.48f)
            lineTo(w * 0.16f, h * 0.44f)
            close()
        }
        drawPath(crest, color = tint)
        drawPath(crest, color = Color(0xFF0A1326), style = Stroke(width = 1.4f, join = StrokeJoin.Round))
    }
}

@Composable
fun CompactCombatStatsBar(
    hud: WorldHudSnapshot,
    modifier: Modifier = Modifier,
    heroName: String = "Indra Hero",
    avatarResId: Int? = null,
    scaleFactor: Float = 1f,
    opacity: Float = 0.94f
) {
    val width = (232f * scaleFactor.coerceIn(0.65f, 1.5f)).dp
    Row(
        modifier = modifier
            .alpha(opacity.coerceIn(0.2f, 1f))
            .widthIn(max = width)
            .fillMaxWidth()
            .background(Color(0xAD060B18), RoundedCornerShape(12.dp))
            .border(1.dp, ElectricCyan.copy(alpha = 0.38f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .testTag("hud_compact_stats_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Hero Portrait / Armor Crest Badge
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xCC121B30))
                .border(1.4.dp, if (hud.defending) ElectricCyan else GoldCoin.copy(alpha = 0.9f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (avatarResId != null) {
                Image(
                    painter = painterResource(id = avatarResId),
                    contentDescription = heroName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Canvas(modifier = Modifier.size(18.dp)) {
                    with(HudVectorArtwork) {
                        drawArmorCrestIcon(if (hud.defending) ElectricCyan else GoldCoin)
                    }
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = heroName,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = GoldCoin,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // HP Bar Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text("HP", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = EmeraldHp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1E293B))
                ) {
                    val hpFrac = (hud.hp / hud.maxHp).coerceIn(0f, 1f)
                    val hpColor = if (hpFrac < 0.3f) DangerRed else EmeraldHp
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(hpFrac)
                            .height(7.dp)
                            .background(Brush.horizontalGradient(listOf(hpColor, Color(0xFF34D399))))
                    )
                }
                Text(
                    text = if (hud.defending) "SHD ${hud.hp.toInt()}" else "${hud.hp.toInt()}/${hud.maxHp.toInt()}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (hud.defending) ElectricCyan else Color.White
                )
            }

            // EP Bar Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text("EP", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = GoldCoin)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF1E293B))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((hud.ep / hud.maxEp).coerceIn(0f, 1f))
                            .height(5.dp)
                            .background(Brush.horizontalGradient(listOf(AmberEp, GoldCoin)))
                    )
                }
                Text(
                    text = "${hud.ep.toInt()}/${hud.maxEp.toInt()}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GoldCoin
                )
            }
        }
    }
}

@Composable
fun FloatingDamageBadge(
    event: DamageFeedbackEvent?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = event != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        if (event != null) {
            Surface(
                color = if (event.isCritical) CrimsonAccent.copy(alpha = 0.92f) else Color(0xCC0D162C),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .border(1.dp, if (event.isCritical) GoldCoin else ElectricCyan, RoundedCornerShape(8.dp))
                    .testTag("hud_damage_popup")
            ) {
                Text(
                    text = if (event.isCritical) "CRIT -${event.damage} • ${event.targetName}" else "-${event.damage} • ${event.targetName}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (event.isCritical) GoldCoin else Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
fun TopRightMissionPill(
    mission: MissionProgress?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    aiTaskPoints: Long = 0L
) {
    val isAi = mission?.aiGenerated == true
    Surface(
        color = Color(0xBA091224),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .widthIn(max = 176.dp)
            .border(1.dp, if (isAi) ElectricCyan.copy(alpha = 0.72f) else GoldCoin.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag("hud_mission_pill")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Filled.Flag, contentDescription = "Active Task", tint = if (isAi) ElectricCyan else GoldCoin, modifier = Modifier.size(12.dp))
                Text(
                    text = if (isAi) "AI TASK • Pts: $aiTaskPoints" else "Main Mission • Pts: $aiTaskPoints",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isAi) ElectricCyan else GoldCoin,
                    maxLines = 1
                )
            }
            Text(
                text = if (mission != null) {
                    "${mission.title} (${mission.progressCount}/${mission.targetCount})"
                } else {
                    "Tap for Next AI Task"
                },
                fontSize = 10.sp,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CollapsibleHubMenuBar(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onSelectPanel: (GamePanel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NavyPanel, RoundedCornerShape(14.dp))
                    .border(1.dp, ElectricCyan.copy(alpha = 0.42f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                val navItems = listOf(
                    Triple(GamePanel.CHARACTER, Icons.Filled.Person, "Character"),
                    Triple(GamePanel.PETS, Icons.Filled.Pets, "Pets"),
                    Triple(GamePanel.VEHICLES, Icons.Filled.DirectionsCar, "Vehicles"),
                    Triple(GamePanel.HOUSE, Icons.Filled.Home, "House"),
                    Triple(GamePanel.FRIENDS, Icons.Filled.Group, "Friends"),
                    Triple(GamePanel.MISSIONS, Icons.Filled.Bolt, "Missions"),
                    Triple(GamePanel.WEAPONS_SHOP, Icons.Filled.ShoppingCart, "Arsenal"),
                    Triple(GamePanel.MAP, Icons.Filled.Map, "Map"),
                    Triple(GamePanel.SETTINGS_RANK, Icons.Filled.MilitaryTech, "Rank")
                )
                items(navItems) { (panel, icon, label) ->
                    Row(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF142244))
                            .border(1.dp, ElectricCyan.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .clickable { onSelectPanel(panel) }
                            .padding(horizontal = 10.dp)
                            .testTag("hub_tab_${label.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(icon, contentDescription = label, tint = ElectricCyan, modifier = Modifier.size(15.dp))
                        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White)
                    }
                }
            }
        }

        // Compact pill button — hidden menu stays unobtrusive during combat until requested
        Surface(
            color = if (expanded) CrimsonAccent.copy(alpha = 0.92f) else Color(0xBA091224),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .border(1.dp, if (expanded) GoldCoin else ElectricCyan.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                .clickable(onClick = onToggleExpanded)
                .testTag("hud_menu_toggle")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Filled.Close else Icons.Filled.Apps,
                    contentDescription = if (expanded) "Hide Menu" else "Open Hub Menu",
                    tint = if (expanded) Color.White else ElectricCyan,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = if (expanded) "Hide" else "Menu",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
    }
}
