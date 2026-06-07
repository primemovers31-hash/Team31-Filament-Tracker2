package com.bambu.nfc.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Handles Google sign-in using Android Credential Manager + Firebase Auth.
 */
class GoogleSignInHelper(
    private val auth: FirebaseAuth,
    private val webClientId: String
) {
    companion object {
        private const val TAG = "GoogleSignInHelper"
    }

    suspend fun signIn(context: Context): Boolean {
        return try {
            val credentialManager = CredentialManager.create(context)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context, request)
            val credential = result.credential

            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken

            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(firebaseCredential).await()

            Log.d(TAG, "Signed in as: ${auth.currentUser?.displayName}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Google sign-in failed", e)
            false
        }
    }

    fun signOut() {
        auth.signOut()
    }

    fun isSignedIn(): Boolean = auth.currentUser != null

    fun getUserName(): String? = auth.currentUser?.displayName
}
