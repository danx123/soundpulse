package com.example.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class AuthUser(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?
)

sealed interface AuthState {
    /** google-services.json / Web Client ID belum ada. */
    data object Unconfigured : AuthState
    data object SignedOut : AuthState
    data object SigningIn : AuthState
    data class SignedIn(val user: AuthUser) : AuthState
}

/**
 * Login Google lewat Credential Manager + Firebase Auth. Tidak butuh API key dari pengguna.
 */
class GoogleAuthManager(context: Context) {

    private val appContext = context.applicationContext
    private val _state = MutableStateFlow<AuthState>(AuthState.Unconfigured)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val firebaseReady: Boolean
        get() = try {
            FirebaseApp.getApps(appContext).isNotEmpty()
        } catch (e: Throwable) {
            false
        }

    /** default_web_client_id dibuat plugin google-services; dicari lewat nama agar build tetap jalan tanpa file itu. */
    private fun webClientId(): String? {
        return try {
            val id = appContext.resources.getIdentifier("default_web_client_id", "string", appContext.packageName)
            if (id != 0) appContext.getString(id) else null
        } catch (e: Throwable) {
            null
        }
    }

    val isConfigured: Boolean
        get() = firebaseReady && webClientId() != null

    init {
        refreshConfig()
    }

    fun refreshConfig() {
        try {
            if (isConfigured) {
                val auth = FirebaseAuth.getInstance()
                if (_state.value is AuthState.Unconfigured || _state.value is AuthState.SignedOut) {
                    _state.value = auth.currentUser?.let { AuthState.SignedIn(it.toAuthUser()) } ?: AuthState.SignedOut
                }
                auth.addAuthStateListener { a ->
                    val user = a.currentUser
                    if (user != null) {
                        _state.value = AuthState.SignedIn(user.toAuthUser())
                    } else if (_state.value !is AuthState.SigningIn) {
                        _state.value = AuthState.SignedOut
                    }
                }
            } else {
                _state.value = AuthState.Unconfigured
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Gagal menginisialisasi Auth: ${e.message}")
        }
    }

    /**
     * @param activityContext harus Context Activity (dibutuhkan Credential Manager).
     * @return pesan error untuk ditampilkan, atau null bila sukses / dibatalkan pengguna.
     */
    suspend fun signIn(activityContext: Context): String? {
        refreshConfig()
        val clientId = webClientId()
        if (!firebaseReady || clientId == null) {
            _state.value = AuthState.Unconfigured
            return "Login Google belum dikonfigurasi: google-services.json tidak ditemukan."
        }

        _state.value = AuthState.SigningIn
        return try {
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(GetSignInWithGoogleOption.Builder(clientId).build())
                .build()
            val result = CredentialManager.create(activityContext).getCredential(activityContext, request)
            val credential = result.credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
                FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).awaitTask()
                null // state diperbarui oleh AuthStateListener
            } else {
                restoreState()
                "Jenis kredensial tidak didukung."
            }
        } catch (e: CancellationException) {
            restoreState()
            throw e
        } catch (e: GetCredentialCancellationException) {
            restoreState()
            null // pengguna menutup dialog
        } catch (e: NoCredentialException) {
            restoreState()
            "Tidak ada akun Google di perangkat ini. Tambahkan akun Google lalu coba lagi."
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Credential Manager gagal", e)
            restoreState()
            "Login Google gagal: ${e.message ?: e.javaClass.simpleName}"
        } catch (e: Exception) {
            Log.w(TAG, "Login gagal", e)
            restoreState()
            "Login gagal: ${e.localizedMessage ?: e.javaClass.simpleName}"
        }
    }

    suspend fun signOut() {
        if (!firebaseReady) return
        FirebaseAuth.getInstance().signOut()
        try {
            CredentialManager.create(appContext).clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w(TAG, "clearCredentialState gagal", e)
        }
        _state.value = AuthState.SignedOut
    }

    private fun restoreState() {
        val user = if (firebaseReady) FirebaseAuth.getInstance().currentUser else null
        _state.value = if (user != null) AuthState.SignedIn(user.toAuthUser()) else AuthState.SignedOut
    }

    private fun FirebaseUser.toAuthUser() = AuthUser(
        uid = uid,
        displayName = displayName,
        email = email,
        photoUrl = photoUrl?.toString()
    )

    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
        addOnCanceledListener { cont.cancel() }
    }

    private companion object {
        const val TAG = "GoogleAuthManager"
    }
}
