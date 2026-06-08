package com.featherframe.app.domain.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.featherframe.app.data.database.SupabaseConfig

/**
 * Google OAuth2 helper for authenticating with Google Drive API.
 * Provides a sign-in intent and manages the authenticated Drive service instance.
 */
class GoogleAuthHelper(private val context: Context) {

    companion object {
        private const val TAG = "GoogleAuthHelper"
        private const val REQUEST_DRIVE_SIGN_IN = 1001

        @Volatile
        private var driveService: Drive? = null
    }

    private val googleSignInClient: GoogleSignInClient by lazy {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(SupabaseConfig.GOOGLE_DRIVE_SCOPE))
            .requestServerAuthCode(SupabaseConfig.GOOGLE_CLIENT_ID)
            .build()

        GoogleSignIn.getClient(context, options)
    }

    /**
     * Get the sign-in intent to launch from an Activity.
     */
    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    /**
     * Handle the sign-in result and build the Drive service.
     */
    suspend fun handleSignInResult(data: Intent?): Boolean {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.result

            if (account != null) {
                buildDriveService(account)
                Log.i(TAG, "Google Drive sign-in successful for: ${account.email}")
                true
            } else {
                Log.w(TAG, "Google sign-in returned null account")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google sign-in failed", e)
            false
        }
    }

    /**
     * Build the Drive service from an authenticated Google account.
     */
    private fun buildDriveService(account: GoogleSignInAccount) {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_FILE)
        ).apply {
            selectedAccount = account.account
            backOff = ExponentialBackOff()
        }

        driveService = Drive.Builder(
            com.google.api.client.googleapis.javanet.GoogleNetHttpTransport.newTrustedTransport(),
            com.google.api.client.json.gson.GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("FeatherFrame")
            .build()

        Log.i(TAG, "Drive service built successfully")
    }

    /**
     * Get the authenticated Drive service.
     */
    fun getDriveService(): Drive? {
        return driveService
    }

    /**
     * Check if the user is already signed in.
     */
    fun isSignedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null && account.scopes.any { it.scopeUri == SupabaseConfig.GOOGLE_DRIVE_SCOPE }
    }

    /**
     * Silent sign-in attempt (no UI).
     */
    fun silentSignIn(onResult: (Boolean) -> Unit) {
        googleSignInClient.silentSignIn()
            .addOnSuccessListener { account ->
                buildDriveService(account)
                Log.i(TAG, "Silent sign-in successful")
                onResult(true)
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Silent sign-in failed", exception)
                onResult(false)
            }
    }

    /**
     * Sign out from Google.
     */
    fun signOut() {
        googleSignInClient.signOut().addOnCompleteListener {
            driveService = null
            Log.i(TAG, "Signed out from Google")
        }
    }
}
