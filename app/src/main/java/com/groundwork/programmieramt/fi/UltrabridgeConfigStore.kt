package com.groundwork.programmieramt.fi

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UltrabridgeConfigStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "ultrabridge_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun save(config: WebDavConfig) {
        prefs.edit()
            .putString("url", config.url)
            .putString("username", config.username)
            .putString("password", config.password)
            .putBoolean("trustAllCerts", config.trustAllCerts)
            .apply()
    }

    fun get(): WebDavConfig? {
        val url = prefs.getString("url", null) ?: return null
        if (url.isBlank()) return null
        return WebDavConfig(
            url = url,
            username = prefs.getString("username", "") ?: "",
            password = prefs.getString("password", "") ?: "",
            trustAllCerts = prefs.getBoolean("trustAllCerts", false)
        )
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
