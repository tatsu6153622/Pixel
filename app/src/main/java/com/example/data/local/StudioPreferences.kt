package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aistudio.chronocanvas.shared.codec.WatchConfigCodec
import com.aistudio.chronocanvas.shared.model.WatchHomeConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.studioDataStore by preferencesDataStore(name = "chronocanvas_studio")

class StudioPreferences(private val context: Context) {

  private val keyActiveConfig = stringPreferencesKey("active_config_json")
  private val keyAutoSyncEnabled = booleanPreferencesKey("auto_sync_enabled")

  val activeConfigFlow: Flow<WatchHomeConfig> = context.studioDataStore.data.map { prefs ->
    val raw = prefs[keyActiveConfig]
    if (raw.isNullOrBlank()) {
      WatchHomeConfig()
    } else {
      runCatching { WatchConfigCodec.fromJson(raw) }.getOrDefault(WatchHomeConfig())
    }
  }

  val autoSyncEnabledFlow: Flow<Boolean> = context.studioDataStore.data.map { prefs ->
    prefs[keyAutoSyncEnabled] ?: true
  }

  suspend fun saveActiveConfig(config: WatchHomeConfig) {
    val json = WatchConfigCodec.toJson(config)
    context.studioDataStore.edit { prefs ->
      prefs[keyActiveConfig] = json
    }
  }

  suspend fun setAutoSyncEnabled(enabled: Boolean) {
    context.studioDataStore.edit { prefs ->
      prefs[keyAutoSyncEnabled] = enabled
    }
  }
}
