package com.bambu.nfc.ui

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.bambu.nfc.data.auth.GoogleSignInHelper
import com.bambu.nfc.data.firestore.FirestoreSync
import com.bambu.nfc.ui.navigation.AppNavGraph
import com.bambu.nfc.ui.scan.ScanViewModel
import com.bambu.nfc.ui.theme.BambuNfcTheme
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private val scanViewModel: ScanViewModel by viewModels()

    @Inject lateinit var firestoreSync: FirestoreSync
    @Inject lateinit var firebaseAuth: FirebaseAuth
    @Inject lateinit var googleSignInHelper: GoogleSignInHelper

    private val authState = MutableStateFlow<AuthState>(AuthState.Loading)

    sealed class AuthState {
        data object Loading : AuthState()
        data object SignedOut : AuthState()
        data object SignedIn : AuthState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null) {
            Toast.makeText(this, "This device does not support NFC", Toast.LENGTH_LONG).show()
        }

        setContent {
            BambuNfcTheme {
                val state by authState.collectAsState()
                when (state) {
                    AuthState.Loading -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    AuthState.SignedOut -> {
                        val context = LocalContext.current
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Nfc,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(16.dp))
                            Text("Team31", style = MaterialTheme.typography.headlineLarge)
                            Text(
                                "Filament Tracker",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(32.dp))
                            Button(onClick = { signIn() }) {
                                Text("Sign in with Google")
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Privacy Policy",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    textDecoration = TextDecoration.Underline
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://github.com/primemovers31/Team31-Filament-Tracker/blob/main/privacy-policy.md")
                                        )
                                    )
                                }
                            )
                        }
                    }
                    AuthState.SignedIn -> {
                        AppNavGraph(
                            scanViewModel = scanViewModel,
                            userName = firebaseAuth.currentUser?.displayName,
                            userEmail = firebaseAuth.currentUser?.email,
                            onSignOut = { signOut() },
                            onDeleteAccount = { deleteAccount() }
                        )
                    }
                }
            }
        }

        handleIntent(intent)
        checkAuth()
    }

    private fun checkAuth() {
        val user = firebaseAuth.currentUser
        val isGoogleUser = user?.providerData?.any { it.providerId == "google.com" } == true
        if (isGoogleUser) {
            authState.value = AuthState.SignedIn
            lifecycleScope.launch {
                firestoreSync.pullAll()
            }
        } else {
            // Sign out anonymous user so Google sign-in screen shows
            if (user != null) firebaseAuth.signOut()
            authState.value = AuthState.SignedOut
        }
    }

    private fun signIn() {
        authState.value = AuthState.Loading
        lifecycleScope.launch {
            val success = googleSignInHelper.signIn(this@MainActivity)
            if (success) {
                authState.value = AuthState.SignedIn
                firestoreSync.pushAll()
                firestoreSync.pullAll()
            } else {
                authState.value = AuthState.SignedOut
                Toast.makeText(this@MainActivity, "Sign-in failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun signOut() {
        googleSignInHelper.signOut()
        authState.value = AuthState.SignedOut
        Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show()
    }

    private fun deleteAccount() {
        lifecycleScope.launch {
            firestoreSync.deleteAllCloud()
            googleSignInHelper.signOut()
            authState.value = AuthState.SignedOut
            Toast.makeText(this@MainActivity, "Account data deleted", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        enableNfcForegroundDispatch()
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == NfcAdapter.ACTION_TECH_DISCOVERED ||
            intent.action == NfcAdapter.ACTION_TAG_DISCOVERED
        ) {
            @Suppress("DEPRECATION")
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            if (tag != null) {
                scanViewModel.onTagDiscovered(tag)
            }
        }
    }

    private fun enableNfcForegroundDispatch() {
        val adapter = nfcAdapter ?: return

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )

        val techFilter = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        val techList = arrayOf(
            arrayOf("android.nfc.tech.MifareClassic"),
            arrayOf("android.nfc.tech.NfcA")
        )

        adapter.enableForegroundDispatch(this, pendingIntent, arrayOf(techFilter), techList)
    }
}
