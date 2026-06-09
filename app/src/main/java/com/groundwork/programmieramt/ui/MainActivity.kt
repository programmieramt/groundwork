package com.groundwork.programmieramt.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.groundwork.programmieramt.R
import com.groundwork.programmieramt.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var signInClient: GoogleSignInClient

    private lateinit var binding: ActivityMainBinding

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            Timber.i("Drive sign-in successful: ${account.email}")
            invalidateOptionsMenu()
        } catch (e: ApiException) {
            Timber.e(e, "Drive sign-in failed: ${e.statusCode}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        binding.bottomNavigation.setupWithNavController(navHost.navController)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            menu.add(0, MENU_DRIVE_DISCONNECT, 0, getString(R.string.drive_disconnected))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        } else {
            menu.add(0, MENU_DRIVE_CONNECT, 0, getString(R.string.drive_connect))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            MENU_DRIVE_CONNECT -> {
                signInLauncher.launch(signInClient.signInIntent)
                true
            }
            MENU_DRIVE_DISCONNECT -> {
                signInClient.signOut().addOnCompleteListener { invalidateOptionsMenu() }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private const val MENU_DRIVE_CONNECT = 1
        private const val MENU_DRIVE_DISCONNECT = 2
    }
}
