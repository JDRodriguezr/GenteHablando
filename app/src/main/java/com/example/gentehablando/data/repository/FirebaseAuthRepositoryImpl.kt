package com.example.gentehablando.data.repository

import android.content.Context
import com.example.gentehablando.domain.model.Resource
import com.example.gentehablando.domain.model.AppError
import com.example.gentehablando.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class FirebaseAuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) : AuthRepository {

    private val prefs = context.getSharedPreferences("app_user_prefs", Context.MODE_PRIVATE) // i18n: technical Local identity storage key/prefix; not display content.

    private fun getLocalPersistentUid(): String {
        var localId = prefs.getString("persistent_uid", null) // i18n: technical Local identity storage key/prefix; not display content.
        if (localId == null) {
            localId = "usr_" + UUID.randomUUID().toString().take(12) // i18n: technical Local identity storage key/prefix; not display content.
            prefs.edit().putString("persistent_uid", localId).apply() // i18n: technical Local identity storage key/prefix; not display content.
        }
        return localId
    }

    override suspend fun signInAnonymously(): Resource<String> {
        return try {
            val result = auth.signInAnonymously().await()
            val uid = result.user?.uid
            if (uid != null) {
                prefs.edit().putString("persistent_uid", uid).apply() // i18n: technical Local identity storage key/prefix; not display content.
                Resource.Success(uid)
            } else {
                val fallbackId = getLocalPersistentUid()
                Resource.Success(fallbackId)
            }
        } catch (_: Exception) {
            // Si el proveedor Anónimo no está activo aún en la consola web,
            // garantizamos un ID persistente único e inmutable por dispositivo.
            val fallbackId = getLocalPersistentUid()
            Resource.Success(fallbackId)
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Resource<String> {
        return try {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val uid = result.user?.uid
            if (uid != null) {
                // Borramos el UID anónimo local (ya no lo necesitamos, tenemos cuenta real)
                prefs.edit().putString("persistent_uid", uid).apply() // i18n: technical Local identity storage key/prefix; not display content.
                Resource.Success(uid)
            } else {
                Resource.Error(AppError.InvalidGoogleUser)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error(AppError.GoogleAuthentication, e)
        }
    }

    override fun getCurrentUserId(): String? {
        val firebaseUid = try { auth.currentUser?.uid } catch (_: Exception) { null }
        return firebaseUid ?: prefs.getString("persistent_uid", null) ?: getLocalPersistentUid() // i18n: technical Local identity storage key/prefix; not display content.
    }
    
    override fun isUserLoggedInWithAccount(): Boolean {
        // Un usuario anónimo Firebase o alguien con "usr_..." persistente NO es cuenta real.
        val user = auth.currentUser ?: return false
        return !user.isAnonymous
    }

    override suspend fun signOut() {
        try {
            auth.signOut()
            // i18n: technical Local identity storage key/prefix; not display content.
            prefs.edit().remove("persistent_uid").apply() // Limpiamos el caché
        } catch (_: Exception) {}
    }

    override suspend fun deleteAccount(): Resource<Unit> {
        return try {
            auth.currentUser?.delete()?.await()
            prefs.edit().remove("persistent_uid").apply() // i18n: technical Local identity storage key/prefix; not display content.
            Resource.Success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error(AppError.DeleteAccount, e)
        }
    }

    override fun observeAuthState(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid ?: getLocalPersistentUid()
            trySend(uid)
        }
        try {
            auth.addAuthStateListener(listener)
        } catch (_: Exception) {
            trySend(getLocalPersistentUid())
        }

        awaitClose {
            try {
                auth.removeAuthStateListener(listener)
            } catch (_: Exception) {
                // Limpieza silenciosa
            }
        }
    }
}
