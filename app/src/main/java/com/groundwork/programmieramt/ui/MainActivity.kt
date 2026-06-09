package com.groundwork.programmieramt.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
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
            Timber.i("Sign-in successful: ${account.email}")
            onSignedIn()
        } catch (e: ApiException) {
            Timber.e(e, "Sign-in failed: ${e.statusCode}")
            updateUi(signedIn = false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.btnSignIn.setOnClickListener {
            signInLauncher.launch(signInClient.signInIntent)
        }

        binding.btnSignOut.setOnClickListener {
            signInClient.signOut().addOnCompleteListener {
                updateUi(signedIn = false)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            onSignedIn()
        } else {
            updateUi(signedIn = false)
        }
    }

    private fun onSignedIn() {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        binding.tvStatus.text = "Connected: ${account?.email}"
        updateUi(signedIn = true)
    }

    private fun updateUi(signedIn: Boolean) {
        binding.btnSignIn.visibility = if (signedIn) android.view.View.GONE else android.view.View.VISIBLE
        binding.btnSignOut.visibility = if (signedIn) android.view.View.VISIBLE else android.view.View.GONE
        if (!signedIn) {
            binding.tvStatus.text = "Not connected"
        }
    }
}
