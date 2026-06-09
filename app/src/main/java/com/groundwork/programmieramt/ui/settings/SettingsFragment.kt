package com.groundwork.programmieramt.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.groundwork.programmieramt.R
import com.groundwork.programmieramt.databinding.FragmentSettingsBinding
import com.groundwork.programmieramt.fi.SyncManager
import com.groundwork.programmieramt.fi.WebDavConfig
import com.groundwork.programmieramt.fi.WebDavClient
import com.groundwork.programmieramt.fi.WebDavConfigStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    @Inject lateinit var configStore: WebDavConfigStore
    @Inject lateinit var client: WebDavClient
    @Inject lateinit var syncManager: SyncManager

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configStore.get()?.let { config ->
            binding.etUrl.setText(config.url)
            binding.etUsername.setText(config.username)
            binding.etPassword.setText(config.password)
            binding.cbTrustAll.isChecked = config.trustAllCerts
        }

        binding.btnSave.setOnClickListener { saveConfig() }
        binding.btnTest.setOnClickListener { testConnection() }
        binding.btnSyncNow.setOnClickListener { syncNow() }
    }

    private fun currentConfig() = WebDavConfig(
        url = binding.etUrl.text?.toString()?.trim() ?: "",
        username = binding.etUsername.text?.toString()?.trim() ?: "",
        password = binding.etPassword.text?.toString() ?: "",
        trustAllCerts = binding.cbTrustAll.isChecked
    )

    private fun saveConfig() {
        val config = currentConfig()
        if (config.url.isBlank()) {
            binding.tvStatus.text = "URL darf nicht leer sein"
            return
        }
        configStore.save(config)
        Toast.makeText(requireContext(), R.string.settings_saved, Toast.LENGTH_SHORT).show()
        binding.tvStatus.text = getString(R.string.settings_saved)
    }

    private fun testConnection() {
        val config = currentConfig()
        configStore.save(config)
        binding.tvStatus.text = "Verbindung wird getestet…"
        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { client.testConnection() }
            binding.tvStatus.text = if (result.isSuccess) "✓ Verbindung erfolgreich" else "✗ Fehler: ${result.exceptionOrNull()?.message}"
        }
    }

    private fun syncNow() {
        binding.tvStatus.text = "Synchronisierung läuft…"
        binding.btnSyncNow.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val result = syncManager.syncAll()
            binding.btnSyncNow.isEnabled = true
            binding.tvStatus.text = if (result.isSuccess) "✓ Synchronisierung abgeschlossen" else "✗ Fehler: ${result.exceptionOrNull()?.message}"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
