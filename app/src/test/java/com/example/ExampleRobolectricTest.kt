package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.IndraAiRepository
import com.example.data.repository.IndraGameRepository
import com.example.ui.AuthSession
import com.example.ui.CinematicLoginOverlay
import com.example.ui.GameLoadingOverlay
import com.example.ui.GameLobbyOverlay
import com.example.ui.GameViewModel
import com.example.ui.StartupLoadState
import com.example.ui.UiState
import com.example.ui.theme.IndraAnimeTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
    }

    @Test
    fun `read string from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Indra: Anime Life RP", appName)
    }

    @Test
    fun loadingOverlay_displaysLogoProgressTipsAndMuteToggle() {
        var muteClicked = false
        composeRule.setContent {
            IndraAnimeTheme {
                GameLoadingOverlay(
                    loadState = StartupLoadState(
                        progressPercent = 42,
                        statusMessage = "Initializing Anime Characters...",
                        tipIndex = 0,
                        isOnline = true
                    ),
                    audioOn = true,
                    onToggleAudio = { muteClicked = true }
                )
            }
        }

        composeRule.onNodeWithText("INDRA").assertIsDisplayed()
        composeRule.onNodeWithText("ANIME LIFE RP").assertIsDisplayed()
        composeRule.onNodeWithTag("loading_percentage_text").assertIsDisplayed()
        composeRule.onNodeWithText("Initializing Anime Characters...").assertIsDisplayed()
        composeRule.onNodeWithTag("loading_mute_button").performClick()
        assertTrue(muteClicked)
    }

    @Test
    fun loginOverlay_showsAllThreeAuthButtonsAndHandlesGuestAndUnconfiguredFacebook() {
        var enteredGuest = false
        var muteToggled = false
        composeRule.setContent {
            IndraAnimeTheme {
                CinematicLoginOverlay(
                    audioOn = true,
                    isOnline = true,
                    onToggleAudio = { muteToggled = true },
                    onAuthenticated = {},
                    onEnterAsGuest = { enteredGuest = true }
                )
            }
        }

        composeRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
        composeRule.onNodeWithTag("facebook_sign_in_button").assertIsDisplayed()
        composeRule.onNodeWithTag("guest_play_button").assertIsDisplayed()

        // Tapping Mute works
        composeRule.onNodeWithTag("login_mute_button").performClick()
        assertTrue(muteToggled)

        // Tapping unconfigured Facebook shows honest configuration dialog instead of fake login
        composeRule.onNodeWithTag("facebook_sign_in_button").performClick()
        composeRule.onNodeWithText("Facebook OAuth Configuration Needed").assertIsDisplayed()
        composeRule.onNodeWithText("Close").performClick()

        // Tapping Play as Guest triggers guest entry
        composeRule.onNodeWithTag("guest_play_button").performClick()
        assertTrue(enteredGuest)
    }

    @Test
    fun lobbyOverlay_guestModeCyclesShowcaseAndStartsGame() {
        val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
        val vm = GameViewModel(
            gameRepo = IndraGameRepository(db),
            aiRepo = IndraAiRepository(),
            currentUserId = "guest_TEST01",
            initialDisplayName = "Guest_TEST01",
            isGuestMode = true
        )
        val guestProfile = (vm.playerState.value as UiState.Success).data
        var started = false

        composeRule.setContent {
            IndraAnimeTheme {
                GameLobbyOverlay(
                    viewModel = vm,
                    profile = guestProfile,
                    authSession = AuthSession.Guest("guest_TEST01", "Guest_TEST01"),
                    audioOn = true,
                    onToggleAudio = {},
                    onStartGame = { started = true },
                    onLinkSuccess = {},
                    onSignOutOrLeave = {}
                )
            }
        }

        composeRule.onNodeWithTag("lobby_link_account_button").assertIsDisplayed()
        composeRule.onNodeWithTag("lobby_cycle_outfit").performClick()
        composeRule.onNodeWithTag("lobby_cycle_pet").performClick()
        composeRule.onNodeWithTag("lobby_cycle_vehicle").performClick()

        composeRule.onNodeWithTag("start_game_button").assertIsDisplayed().performClick()
        assertTrue(started)
    }

    @Test
    fun worldMapPanel_displaysDistrictsSetsGpsAndFastTravels() {
        val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
        val vm = GameViewModel(
            gameRepo = IndraGameRepository(db),
            aiRepo = IndraAiRepository(),
            currentUserId = "guest_MAP01",
            initialDisplayName = "Guest_MAP01",
            isGuestMode = true
        )
        val profile = (vm.playerState.value as UiState.Success).data
        var closed = false

        composeRule.setContent {
            IndraAnimeTheme {
                com.example.ui.WorldMapPanel(
                    viewModel = vm,
                    hud = vm.sim.snapshotHud(),
                    profile = profile,
                    onClose = { closed = true }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("2400 x 2400 Open World Map & GPS").assertIsDisplayed()
        composeRule.onNodeWithTag("world_map_zoom_in").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("fast_travel_hidden_village").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("world_map_gps_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals("Hidden Village Great Gate", vm.activeGpsDestination.value?.name)

        composeRule.onNodeWithTag("world_map_fast_travel_selected").performClick()
        composeRule.waitForIdle()
        assertEquals("Hidden Village", vm.sim.snapshotHud().district)
        assertTrue(!closed)
    }

    @Test
    fun gameHudOverlay_keepsMenuHiddenByDefaultAndSupportsCombatAndVehicleControls() {
        val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
        val vm = GameViewModel(
            gameRepo = IndraGameRepository(db),
            aiRepo = IndraAiRepository(),
            currentUserId = "guest_HUD01",
            initialDisplayName = "Guest_HUD01",
            isGuestMode = true
        )
        val profile = (vm.playerState.value as UiState.Success).data
        val starterCar = vm.sim.parkedVehicles.first { !it.isLocked }
        vm.sim.playerX = starterCar.x
        vm.sim.playerZ = starterCar.z + 2.0f

        composeRule.setContent {
            IndraAnimeTheme {
                com.example.ui.GameHudOverlay(
                    viewModel = vm,
                    hud = vm.sim.snapshotHud(),
                    playerProfile = profile,
                    nearbyPlayersCount = 2,
                    activeBountyTargetingMe = null
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("hud_compact_stats_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("hud_crosshair_circle").assertIsDisplayed()
        composeRule.onNodeWithTag("hud_top_map_button").assertIsDisplayed()
        composeRule.onNodeWithTag("hud_minimap").assertDoesNotExist()
        composeRule.onNodeWithTag("hud_fire_button").assertIsDisplayed()
        composeRule.onNodeWithTag("hud_vehicle_action_button").assertIsDisplayed()

        // Collapsible bottom hub menu is hidden until tapped
        composeRule.onNodeWithTag("hub_tab_character").assertDoesNotExist()
        composeRule.onNodeWithTag("hud_menu_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("hub_tab_character").assertIsDisplayed()
    }

    @Test
    fun settingsAndHudEditor_opensCustomHudSwitchesPresetsAndSaves() {
        val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
        val store = com.example.data.repository.LocalHudSettingsStore(context)
        val vm = GameViewModel(
            gameRepo = IndraGameRepository(db),
            aiRepo = IndraAiRepository(),
            currentUserId = "guest_EDIT01",
            initialDisplayName = "Guest_EDIT01",
            isGuestMode = true,
            localHudStore = store
        )
        val profile = (vm.playerState.value as UiState.Success).data

        composeRule.setContent {
            IndraAnimeTheme {
                val editorOpen by vm.hudEditorOpen.collectAsStateWithLifecycle()
                if (editorOpen) {
                    com.example.ui.HudEditorOverlay(
                        viewModel = vm,
                        isGuestMode = true
                    )
                } else {
                    com.example.ui.SettingsRankPanel(
                        viewModel = vm,
                        profile = profile,
                        onClose = {}
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_customize_hud_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertTrue(vm.hudEditorOpen.value)

        composeRule.onNodeWithTag("hud_editor_guest_warning").assertIsDisplayed()
        composeRule.onNodeWithTag("hud_editor_presets_row").performScrollToNode(hasTestTag("hud_editor_preset_2"))
        composeRule.onNodeWithTag("hud_editor_preset_2").performClick()
        composeRule.onNodeWithTag("hud_editor_save").performClick()
        composeRule.waitForIdle()
        assertEquals(2, vm.hudSettingsBundle.value.activeLayoutIndex)
        assertTrue(!vm.hudEditorOpen.value)

        // Verify JSON + SharedPreferences persistence roundtrip under Android runtime
        val reloaded = store.load("guest_EDIT01")
        assertEquals(2, reloaded.activeLayoutIndex)
        assertEquals(17, reloaded.activeLayout.elements.size)
        assertTrue(reloaded.toJsonString().length < 31500)
    }

    @Test
    fun petsPanel_togglesSummonAndEquipsPet() {
        val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
        val vm = GameViewModel(
            gameRepo = IndraGameRepository(db),
            aiRepo = IndraAiRepository(),
            currentUserId = "guest_PET01",
            initialDisplayName = "Guest_PET01",
            isGuestMode = true
        )
        val profile = (vm.playerState.value as UiState.Success).data

        composeRule.setContent {
            IndraAnimeTheme {
                com.example.ui.PetsPanel(
                    viewModel = vm,
                    profile = profile,
                    onClose = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pet_summon_toggle").assertIsDisplayed().performClick()
        assertTrue(!vm.sim.petSummoned)
        composeRule.onNodeWithTag("pets_list").performScrollToNode(hasTestTag("pet_equip_pet_anime_fox"))
        composeRule.onNodeWithTag("pet_equip_pet_anime_fox").assertIsDisplayed().performClick()
        assertTrue(vm.sim.petSummoned)
    }

    @Test
    fun characterPanel_customizesOutfitHairAccessoryResetsAndSaves() {
        val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
        val vm = GameViewModel(
            gameRepo = IndraGameRepository(db),
            aiRepo = IndraAiRepository(),
            currentUserId = "guest_CHAR01",
            initialDisplayName = "Guest_CHAR01",
            isGuestMode = true
        )
        val profile = (vm.playerState.value as UiState.Success).data

        composeRule.setContent {
            IndraAnimeTheme {
                val liveState by vm.playerState.collectAsStateWithLifecycle()
                val liveProfile = (liveState as? UiState.Success)?.data ?: profile
                com.example.ui.CharacterPanel(
                    viewModel = vm,
                    profile = liveProfile,
                    onClose = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("character_preview_stage").assertIsDisplayed()

        // 1. Pick Cyber Hunter outfit
        composeRule.onNodeWithTag("character_options_list").performScrollToNode(hasTestTag("outfit_option_cyber"))
        composeRule.onNodeWithTag("outfit_option_cyber").performClick()
        composeRule.waitForIdle()
        assertEquals("cyber", vm.sim.heroOutfitId)

        // 2. Switch to Hair tab & pick Crimson
        composeRule.onNodeWithTag("char_tab_hair").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("character_options_list").performScrollToNode(hasTestTag("hair_option_red"))
        composeRule.onNodeWithTag("hair_option_red").performClick()
        composeRule.waitForIdle()
        assertEquals("red", vm.sim.heroHairId)

        // 3. Switch to Accessory tab & pick Energy Wings
        composeRule.onNodeWithTag("char_tab_accessory").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("character_options_list").performScrollToNode(hasTestTag("accessory_option_wings"))
        composeRule.onNodeWithTag("accessory_option_wings").performClick()
        composeRule.waitForIdle()
        assertEquals("wings", vm.sim.heroAccessoryId)

        // 4. Reset restores default Shadow Blaze + Black + Katana
        composeRule.onNodeWithTag("character_reset_button").performClick()
        composeRule.waitForIdle()
        assertEquals("shadow", vm.sim.heroOutfitId)
        assertEquals("black", vm.sim.heroHairId)
        assertEquals("katana", vm.sim.heroAccessoryId)

        // 5. Switch back to Outfit, pick White Storm, and Save Character
        composeRule.onNodeWithTag("char_tab_outfit").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("character_options_list").performScrollToNode(hasTestTag("outfit_option_white"))
        composeRule.onNodeWithTag("outfit_option_white").performClick()
        composeRule.onNodeWithTag("save_character_button").performClick()
        composeRule.waitForIdle()

        val savedProfile = (vm.playerState.value as? UiState.Success)?.data
        assertEquals("White Storm", savedProfile?.outfitStyle)
        assertEquals("white", vm.sim.heroOutfitId)

        // 6. Verify visible Look & Color names + one-tap Equip Main Hero (Indra Shinobi) and Crimson Blox
        composeRule.onNodeWithTag("character_options_list").performScrollToNode(hasTestTag("char_look_card_happy"))
        composeRule.onNodeWithTag("char_look_card_happy").assertIsDisplayed()
        composeRule.onNodeWithTag("character_options_list").performScrollToNode(hasTestTag("color_swatch_e2263a"))
        composeRule.onNodeWithTag("color_swatch_e2263a").assertIsDisplayed()
        composeRule.onNodeWithText("Crimson Red").assertIsDisplayed()

        composeRule.onNodeWithTag("character_options_list").performScrollToNode(hasTestTag("equip_crimson_blox_button"))
        composeRule.onNodeWithTag("equip_crimson_blox_button").performClick()
        composeRule.waitForIdle()
        val bloxProfile = (vm.playerState.value as? UiState.Success)?.data
        assertEquals("Crimson Blox", bloxProfile?.outfitStyle)
        assertEquals("crimson_blox", vm.sim.heroOutfitId)
        assertEquals("crimson_tips", vm.sim.heroHairId)

        composeRule.onNodeWithTag("character_options_list").performScrollToNode(hasTestTag("equip_main_indra_button"))
        composeRule.onNodeWithTag("equip_main_indra_button").performClick()
        composeRule.waitForIdle()
        val indraProfile = (vm.playerState.value as? UiState.Success)?.data
        assertEquals("Shadow Blaze", indraProfile?.outfitStyle)
        assertEquals("shadow", vm.sim.heroOutfitId)
    }

    @Test
    fun gameHudOverlay_adaptsControlsForFirearmKatanaFistsAndGroundPickup() {
        val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
        val vm = GameViewModel(
            gameRepo = IndraGameRepository(db),
            aiRepo = IndraAiRepository(),
            currentUserId = "guest_DYN_HUD",
            initialDisplayName = "Guest_DYNHUD",
            isGuestMode = true
        )
        val profile = (vm.playerState.value as? UiState.Success)?.data
        vm.sim.hp = 120f
        vm.onHudTick(vm.sim.snapshotHud())

        composeRule.setContent {
            IndraAnimeTheme {
                val hud by vm.hudSnapshot.collectAsStateWithLifecycle()
                com.example.ui.GameHudOverlay(
                    viewModel = vm,
                    hud = hud,
                    playerProfile = profile,
                    nearbyPlayersCount = 4,
                    activeBountyTargetingMe = null
                )
            }
        }

        composeRule.waitForIdle()
        // 1. Default Firearm shows crosshair circle, aim, reload, ammo counter, left utility rail, and hides clutter (minimap/prone/grenade) by default
        composeRule.onNodeWithTag("hud_crosshair_circle").assertIsDisplayed()
        composeRule.onNodeWithTag("hud_aim_button").assertIsDisplayed()
        composeRule.onNodeWithTag("hud_reload_button").assertIsDisplayed()
        composeRule.onNodeWithTag("hud_weapon_ammo_text").assertIsDisplayed()
        composeRule.onNodeWithTag("hud_backpack_button").assertIsDisplayed()
        composeRule.onNodeWithTag("hud_shop_button").assertIsDisplayed()
        composeRule.onNodeWithTag("hud_heal_button").assertIsDisplayed()
        composeRule.onNodeWithTag("hud_map_button").assertIsDisplayed()
        composeRule.onNodeWithTag("hud_minimap").assertDoesNotExist()
        composeRule.onNodeWithTag("hud_prone_button").assertDoesNotExist()
        composeRule.onNodeWithTag("hud_grenade_button").assertDoesNotExist()

        // 2. Switch to Katana (slot 2) -> firearm Aim/Reload/Ammo disappear, Sword Heavy & Guard appear
        composeRule.onNodeWithTag("hud_weapon_slot_2").performClick()
        composeRule.waitForIdle()
        assertEquals("wpn_shadow_katana", vm.sim.activeWeaponId)
        composeRule.onNodeWithTag("hud_aim_button").assertDoesNotExist()
        composeRule.onNodeWithTag("hud_reload_button").assertDoesNotExist()
        composeRule.onNodeWithTag("hud_weapon_ammo_text").assertDoesNotExist()
        composeRule.onNodeWithTag("hud_sword_heavy_button").assertIsDisplayed()
        composeRule.onNodeWithTag("hud_guard_button").assertIsDisplayed()

        // 3. Drop Katana onto ground -> Pick Up button appears; tap Pick Up -> Katana restored
        composeRule.onNodeWithTag("hud_drop_weapon_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("hud_pickup_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals("wpn_shadow_katana", vm.sim.activeWeaponId)
        composeRule.onNodeWithTag("hud_pickup_button").assertDoesNotExist()

        // 4. Heal trigger updates simulation state cleanly
        val initialMeds = vm.sim.medkitCount
        composeRule.onNodeWithTag("hud_heal_button").performClick()
        composeRule.waitForIdle()
        assertEquals(initialMeds - 1, vm.sim.medkitCount)
        assertEquals(195f, vm.sim.hp, 0.1f)
    }

    @Test
    fun petSanctuary_supports50PetShopEvolutionAbilitySlotsCustomizationAndTrade() {
        val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
        val vm = GameViewModel(
            gameRepo = IndraGameRepository(db),
            aiRepo = IndraAiRepository(),
            currentUserId = "guest_PET50",
            initialDisplayName = "Guest_PET50",
            isGuestMode = true
        )

        composeRule.setContent {
            IndraAnimeTheme {
                val pState by vm.playerState.collectAsStateWithLifecycle()
                val liveProfile = (pState as? UiState.Success)?.data
                com.example.ui.PetsPanel(
                    viewModel = vm,
                    profile = liveProfile,
                    onClose = {}
                )
            }
        }

        composeRule.waitForIdle()
        // 1. Evolve starter Fox Lv.1 -> Lv.2
        composeRule.onNodeWithTag("pets_list").performScrollToNode(hasTestTag("pet_upgrade_pet_anime_fox"))
        composeRule.onNodeWithTag("pet_upgrade_pet_anime_fox").performClick()
        composeRule.waitForIdle()
        val afterEvo = (vm.petsState.value as UiState.Success).data.first { it.id == "pet_anime_fox" }
        assertEquals(2L, afterEvo.evolutionLevel)

        // 2. Open 50-Pet Shop, filter Water Animals, buy Dolphin
        composeRule.onNodeWithTag("pet_tab_shop").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pet_filter_WATER").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pet_shop_list").performScrollToNode(hasTestTag("pet_shop_buy_pet_31_dolphin"))
        composeRule.onNodeWithTag("pet_shop_buy_pet_31_dolphin").performClick()
        composeRule.waitForIdle()
        val ownedAfterBuy = (vm.petsState.value as UiState.Success).data
        assertTrue(ownedAfterBuy.any { it.id == "pet_31_dolphin" })

        // 3. Unlock extra Pet Ability Slot (2 -> 3)
        composeRule.onNodeWithTag("pet_tab_abilities").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pet_abilities_list").performScrollToNode(hasTestTag("unlock_pet_slot_button"))
        composeRule.onNodeWithTag("unlock_pet_slot_button").performClick()
        composeRule.waitForIdle()
        val updatedProfile = (vm.playerState.value as UiState.Success).data!!
        assertEquals(3L, updatedProfile.unlockedPetSlots)
        assertTrue(updatedProfile.equippedAbilities.size >= 5)

        // 4. Customize Aura & Save
        composeRule.onNodeWithTag("pet_tab_customize").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pet_customize_list").performScrollToNode(hasTestTag("pet_aura_celestial"))
        composeRule.onNodeWithTag("pet_aura_celestial").performClick()
        composeRule.onNodeWithTag("pet_customize_list").performScrollToNode(hasTestTag("pet_customize_save"))
        composeRule.onNodeWithTag("pet_customize_save").performClick()
        composeRule.waitForIdle()
        val styledFox = (vm.petsState.value as UiState.Success).data.first { it.id == "pet_anime_fox" }
        assertEquals("Celestial", styledFox.auraStyle)

        // 5. Pet Trade with 2-step Confirmation Modal
        composeRule.onNodeWithTag("pet_tab_trade").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pet_trade_list").performScrollToNode(hasTestTag("pet_trade_review_button"))
        composeRule.onNodeWithTag("pet_trade_review_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pet_trade_confirm_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        val afterTradeCount = (vm.petsState.value as UiState.Success).data.size
        assertTrue(afterTradeCount >= 2)
    }

    @Test
    fun styleShopAndCharacterStudio_buyClothesHairAndGearInRupeesAnytime() {
        val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
        val store = com.example.data.repository.LocalHudSettingsStore(context)
        val vm = GameViewModel(
            gameRepo = IndraGameRepository(db),
            aiRepo = IndraAiRepository(),
            currentUserId = "guest_STYLE_RUPEE",
            initialDisplayName = "Guest_STYLE",
            isGuestMode = true,
            localHudStore = store
        )

        var showShopInsteadOfStudio = false
        val switcher = androidx.compose.runtime.mutableStateOf(false)

        composeRule.setContent {
            IndraAnimeTheme {
                val pState by vm.playerState.collectAsStateWithLifecycle()
                val liveProfile = (pState as? UiState.Success)?.data
                if (switcher.value) {
                    com.example.ui.ArsenalShopPanel(
                        viewModel = vm,
                        profile = liveProfile,
                        onClose = {}
                    )
                } else {
                    com.example.ui.CharacterPanel(
                        viewModel = vm,
                        profile = liveProfile,
                        onClose = {}
                    )
                }
            }
        }

        composeRule.waitForIdle()
        val startCoins = (vm.playerState.value as UiState.Success).data!!.coins
        assertEquals(1250L, startCoins)

        // 1. Test Expression Portrait toggle on Character Stage
        composeRule.onNodeWithTag("char_expr_happy").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(com.example.data.model.HeroExpression.HAPPY, vm.sim.heroExpression)

        // 2. Pick ₹10 Haircut (Wolf Cut) + ₹10 Crimson Scarf -> verify pending BUY ₹20 appears and buys cleanly
        composeRule.onNodeWithTag("char_tab_hair").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("character_options_list").performScrollToNode(hasTestTag("haircut_option_wolf_cut"))
        composeRule.onNodeWithTag("haircut_option_wolf_cut").performClick()
        composeRule.waitForIdle()
        assertEquals("wolf_cut", vm.sim.heroHairCutId)

        composeRule.onNodeWithTag("char_tab_accessory").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("character_options_list").performScrollToNode(hasTestTag("headgear_option_gear_scarf"))
        composeRule.onNodeWithTag("headgear_option_gear_scarf").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("buy_selected_style_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        val afterStudioCoins = (vm.playerState.value as UiState.Success).data!!.coins
        assertEquals(startCoins - 20L, afterStudioCoins)
        assertTrue(vm.isStyleUnlocked("haircut", "wolf_cut"))
        assertTrue(vm.isStyleUnlocked("gear", "gear_scarf"))
        assertEquals("wolf_cut", store.load("guest_STYLE_RUPEE").selectedHairCutId)
        assertEquals("gear_scarf", store.load("guest_STYLE_RUPEE").selectedHeadgearId)

        // 3. Open in-game Backpack / Shop anytime and buy ₹10 White Storm jacket directly
        switcher.value = true
        showShopInsteadOfStudio = true
        assertTrue(showShopInsteadOfStudio)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("shop_tab_style").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("shop_style_list").performScrollToNode(hasTestTag("shop_buy_outfit_white"))
        composeRule.onNodeWithTag("shop_buy_outfit_white").performClick()
        composeRule.waitForIdle()

        val finalProfile = (vm.playerState.value as? UiState.Success)?.data!!
        assertEquals(afterStudioCoins - 10L, finalProfile.coins)
        assertEquals("White Storm", finalProfile.outfitStyle)
        assertTrue(vm.isStyleUnlocked("outfit", "white"))
    }

    @Test
    fun housingMarketAndRoomVault_startsEmptyBuysPlotVaultsGearAndSellsFromRoom() {
        val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
        val vm = GameViewModel(
            gameRepo = IndraGameRepository(db),
            aiRepo = IndraAiRepository(),
            currentUserId = "guest_HOUSE01",
            initialDisplayName = "Guest_HOUSE",
            isGuestMode = true
        )
        val showHud = androidx.compose.runtime.mutableStateOf(false)

        composeRule.setContent {
            IndraAnimeTheme {
                val pState by vm.playerState.collectAsStateWithLifecycle()
                val liveProfile = (pState as? UiState.Success)?.data
                val hud by vm.hudSnapshot.collectAsStateWithLifecycle()
                if (showHud.value) {
                    com.example.ui.GameHudOverlay(
                        viewModel = vm,
                        hud = hud,
                        playerProfile = liveProfile,
                        nearbyPlayersCount = 2,
                        activeBountyTargetingMe = null
                    )
                } else {
                    com.example.ui.HousePanel(
                        viewModel = vm,
                        profile = liveProfile,
                        onClose = {}
                    )
                }
            }
        }

        composeRule.waitForIdle()
        // 1. Starts with 0 owned houses
        composeRule.onNodeWithTag("house_empty_state").assertIsDisplayed()
        assertEquals(1250L, (vm.playerState.value as UiState.Success).data!!.coins)

        // 2. Buy Land Plot #1 (320 Coins)
        composeRule.onNodeWithTag("house_panel_list").performScrollToNode(hasTestTag("house_buy_prop_plot_saffron"))
        composeRule.onNodeWithTag("house_buy_prop_plot_saffron").performClick()
        composeRule.waitForIdle()
        assertEquals(930L, (vm.playerState.value as UiState.Success).data!!.coins)
        val ownedProp = (vm.propertiesState.value as UiState.Success).data.first { it.id == "prop_plot_saffron" }

        // 3. Enter purchased house room & switch to HUD to test Stash Vault and In-Room Sell House
        composeRule.onNodeWithTag("house_panel_list").performScrollToNode(hasTestTag("house_enter_${ownedProp.id}"))
        composeRule.onNodeWithTag("house_enter_${ownedProp.id}").performClick()
        showHud.value = true
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("hud_inside_house_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("hud_house_stash_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("house_stash_list").performScrollToNode(hasTestTag("stash_store_weapon_wpn_vortex_ar"))
        composeRule.onNodeWithTag("stash_store_weapon_wpn_vortex_ar").performClick()
        composeRule.waitForIdle()
        assertTrue("wpn_vortex_ar" in vm.sim.houseStashWeapons)
        composeRule.onNodeWithText("Done").performClick()
        composeRule.waitForIdle()

        // 4. Sell House right inside the room -> refunds +220 Coins & exits back outside
        composeRule.onNodeWithTag("hud_house_sell_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("hud_house_sell_confirm_button").performClick()
        composeRule.waitForIdle()

        assertEquals(1150L, (vm.playerState.value as UiState.Success).data!!.coins)
        assertTrue((vm.propertiesState.value as UiState.Success).data.isEmpty())
        assertEquals(null, vm.sim.insideHouse)
    }

    @Test
    fun arsenalShop_supportsBuyingAndSellingWeaponsAndSupplies() {
        val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
        val vm = GameViewModel(
            gameRepo = IndraGameRepository(db),
            aiRepo = IndraAiRepository(),
            currentUserId = "guest_SHOP01",
            initialDisplayName = "Guest_SHOP",
            isGuestMode = true
        )

        composeRule.setContent {
            IndraAnimeTheme {
                val pState by vm.playerState.collectAsStateWithLifecycle()
                val liveProfile = (pState as? UiState.Success)?.data
                com.example.ui.ArsenalShopPanel(
                    viewModel = vm,
                    profile = liveProfile,
                    onClose = {}
                )
            }
        }

        composeRule.waitForIdle()
        assertEquals(1250L, (vm.playerState.value as UiState.Success).data!!.coins)

        // Buy Ryujin Vector SMG (850 Coins) then Sell it back (+480 Coins)
        composeRule.onNodeWithTag("arsenal_weapons_list").performScrollToNode(hasTestTag("arsenal_buy_wpn_dragon_smg"))
        composeRule.onNodeWithTag("arsenal_buy_wpn_dragon_smg").performClick()
        composeRule.waitForIdle()
        assertEquals(400L, (vm.playerState.value as UiState.Success).data!!.coins)
        assertTrue("wpn_dragon_smg" in vm.sim.ownedWeaponIds)

        composeRule.onNodeWithTag("arsenal_weapons_list").performScrollToNode(hasTestTag("arsenal_sell_wpn_dragon_smg"))
        composeRule.onNodeWithTag("arsenal_sell_wpn_dragon_smg").performClick()
        composeRule.waitForIdle()
        assertEquals(880L, (vm.playerState.value as UiState.Success).data!!.coins)
        assertTrue("wpn_dragon_smg" !in vm.sim.ownedWeaponIds)

        // Buy Medkit (-60c) then Sell Medkit (+35c)
        val medsBefore = vm.sim.medkitCount
        composeRule.onNodeWithTag("shop_buy_medkit").performClick()
        composeRule.waitForIdle()
        assertEquals(medsBefore + 1, vm.sim.medkitCount)
        assertEquals(820L, (vm.playerState.value as UiState.Success).data!!.coins)

        composeRule.onNodeWithTag("shop_sell_medkit").performClick()
        composeRule.waitForIdle()
        assertEquals(medsBefore, vm.sim.medkitCount)
        assertEquals(855L, (vm.playerState.value as UiState.Success).data!!.coins)
    }

    @Test
    fun facebookManifestOptOutAndSafeLogOut_disablesAutoInitAndProviders() {
        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(context.packageName, android.content.pm.PackageManager.GET_META_DATA)
        val meta = appInfo.metaData!!
        assertEquals(false, meta.getBoolean("com.facebook.sdk.AutoInitEnabled", true))
        assertEquals(false, meta.getBoolean("com.facebook.sdk.AutoLogAppEventsEnabled", true))
        assertEquals(false, meta.getBoolean("com.facebook.sdk.AdvertiserIDCollectionEnabled", true))
        assertEquals(false, meta.getBoolean("com.facebook.sdk.MonitorEnabled", true))
        assertEquals(false, meta.getBoolean("com.facebook.sdk.CodelessDebugLogEnabled", true))

        val pkgInfo = pm.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_PROVIDERS)
        val providerNames = pkgInfo.providers?.map { it.name } ?: emptyList()
        assertTrue(providerNames.none { it == "com.facebook.internal.FacebookInitProvider" })

        com.example.ui.FacebookAuthBridge.safeLogOut()
    }

    @Test
    fun hud_showsOverheadKillBadgeHidesBlueDamagePopupAndTogglesBareHands() {
        val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
        val vm = GameViewModel(
            gameRepo = IndraGameRepository(db),
            aiRepo = IndraAiRepository(),
            currentUserId = "guest_KILL_HANDS",
            initialDisplayName = "Guest_KH",
            isGuestMode = true
        )
        val profile = (vm.playerState.value as UiState.Success).data
        val enemy = vm.sim.npcs.first { it.role == com.example.game.engine.NpcRole.RIVAL_BOT }
        enemy.respawnController.takeDamage(enemy.respawnController.health - 5f)
        enemy.x = vm.sim.playerX
        enemy.z = vm.sim.playerZ + 1.8f
        enemy.y = com.example.game.engine.WorldMapData.terrainHeightAt(enemy.x, enemy.z)
        vm.sim.cameraYaw = 0f
        vm.sim.triggerMeleeAttack()
        vm.onHudTick(vm.sim.snapshotHud())

        composeRule.setContent {
            IndraAnimeTheme {
                val hud by vm.hudSnapshot.collectAsStateWithLifecycle()
                com.example.ui.GameHudOverlay(
                    viewModel = vm,
                    hud = hud,
                    playerProfile = profile,
                    nearbyPlayersCount = 1,
                    activeBountyTargetingMe = null
                )
            }
        }

        composeRule.waitForIdle()
        // Blue damage popup stays removed even after melee damage event; overhead "1 KILL" badge appears
        composeRule.onNodeWithTag("hud_damage_popup").assertDoesNotExist()
        composeRule.onNodeWithTag("hud_overhead_kill_badge").assertIsDisplayed()
        composeRule.onNodeWithText("1 KILL").assertIsDisplayed()

        // One-tap Hands toggle hides gun & hides extra attack/guard buttons, then second tap brings gun back
        composeRule.onNodeWithTag("hud_hands_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals("wpn_fists", vm.sim.activeWeaponId)
        composeRule.onNodeWithTag("hud_sword_heavy_button").assertDoesNotExist()
        composeRule.onNodeWithTag("hud_guard_button").assertDoesNotExist()
        composeRule.onNodeWithTag("hud_fire_button").assertIsDisplayed()

        composeRule.onNodeWithTag("hud_hands_button").performClick()
        composeRule.waitForIdle()
        assertEquals("wpn_vortex_ar", vm.sim.activeWeaponId)
    }

    @Test
    fun missionsPanel_displaysAiPointsAndAwardsPointsOnTaskClaim() {
        val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
        val vm = GameViewModel(
            gameRepo = IndraGameRepository(db),
            aiRepo = IndraAiRepository(),
            currentUserId = "guest_AI_PTS",
            initialDisplayName = "Guest_AI",
            isGuestMode = true
        )

        composeRule.setContent {
            IndraAnimeTheme {
                val pState by vm.playerState.collectAsStateWithLifecycle()
                val liveProfile = (pState as? UiState.Success)?.data
                com.example.ui.MissionsPanel(
                    viewModel = vm,
                    profile = liveProfile,
                    onClose = {}
                )
            }
        }

        composeRule.waitForIdle()
        val initialPts = (vm.playerState.value as UiState.Success).data!!.upgradePoints
        composeRule.onNodeWithTag("ai_task_points_badge").assertIsDisplayed()

        // Complete starter mission via +1 Progress buttons and Claim -> awards AI Points
        val firstMission = (vm.missionsState.value as UiState.Success).data.first()
        repeat(firstMission.targetCount.toInt()) {
            composeRule.onNodeWithTag("advance_mission_${firstMission.id}").performClick()
            composeRule.waitForIdle()
        }
        composeRule.onNodeWithTag("claim_mission_${firstMission.id}").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        val updatedPts = (vm.playerState.value as UiState.Success).data!!.upgradePoints
        assertTrue(updatedPts > initialPts)
        assertEquals(1, vm.aiTasksCompletedCount.value)
    }

    @Test
    fun locationTrackingService_declaresForegroundLocationAndUpdatesCoordinates() {
        val pm = context.packageManager
        val pkgInfo = pm.getPackageInfo(
            context.packageName,
            android.content.pm.PackageManager.GET_PERMISSIONS or android.content.pm.PackageManager.GET_SERVICES
        )
        val perms = pkgInfo.requestedPermissions?.toSet() ?: emptySet()
        assertTrue(android.Manifest.permission.ACCESS_COARSE_LOCATION in perms)
        assertTrue(android.Manifest.permission.ACCESS_FINE_LOCATION in perms)
        assertTrue(android.Manifest.permission.FOREGROUND_SERVICE_LOCATION in perms)
        val serviceInfo = pkgInfo.services?.firstOrNull {
            it.name == com.example.location.LocationTrackingService::class.java.name
        }
        assertTrue(serviceInfo != null)
        assertEquals(
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            serviceInfo!!.foregroundServiceType and android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )

        com.example.location.LocationTrackingState.reset()
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        org.robolectric.Shadows.shadowOf(app).grantPermissions(
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )

        val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
        val vm = GameViewModel(
            gameRepo = IndraGameRepository(db),
            aiRepo = IndraAiRepository(),
            currentUserId = "guest_LOC",
            initialDisplayName = "Guest_LOC",
            isGuestMode = true
        )

        composeRule.setContent {
            IndraAnimeTheme {
                val pState by vm.playerState.collectAsStateWithLifecycle()
                val liveProfile = (pState as? UiState.Success)?.data
                com.example.ui.SettingsRankPanel(
                    viewModel = vm,
                    profile = liveProfile,
                    onClose = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_toggle_location_tracking").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(true, (vm.playerState.value as UiState.Success).data!!.locationTrackingEnabled)

        val controller = org.robolectric.Robolectric.buildService(com.example.location.LocationTrackingService::class.java)
        val service = controller.create().get()
        val mockLocation = android.location.Location("fused").apply {
            latitude = 19.0760
            longitude = 72.8777
            accuracy = 8.5f
        }
        service.processLocation(mockLocation)
        composeRule.waitForIdle()

        val updated = (vm.playerState.value as UiState.Success).data!!
        assertEquals(19.0760, updated.latitude!!, 0.0001)
        assertEquals(72.8777, updated.longitude!!, 0.0001)
        composeRule.onNodeWithTag("settings_location_status").assertIsDisplayed()

        composeRule.onNodeWithTag("settings_toggle_location_tracking").performClick()
        composeRule.waitForIdle()
        assertEquals(false, (vm.playerState.value as UiState.Success).data!!.locationTrackingEnabled)
    }
}
