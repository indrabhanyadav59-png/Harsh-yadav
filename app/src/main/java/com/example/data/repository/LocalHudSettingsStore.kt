package com.example.data.repository

import android.content.Context
import com.example.data.model.PlayerHudSettingsBundle

class LocalHudSettingsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        "indra_hud_settings_v2",
        Context.MODE_PRIVATE
    )

    fun hasSavedLayout(uid: String): Boolean = prefs.contains(keyFor(uid))

    fun load(uid: String): PlayerHudSettingsBundle {
        val raw = prefs.getString(keyFor(uid), null)
        return PlayerHudSettingsBundle.fromJsonString(raw, uid)
    }

    fun save(uid: String, bundle: PlayerHudSettingsBundle) {
        prefs.edit().putString(keyFor(uid), bundle.toJsonString()).apply()
    }

    private fun keyFor(uid: String): String = "hud_bundle_${uid.ifBlank { "default" }}"
}
