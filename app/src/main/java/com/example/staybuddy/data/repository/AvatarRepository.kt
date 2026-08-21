package com.example.staybuddy.data.repository

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class AvatarItem(
    val avatarKey: String = "",
    val name: String = "",
    val gender: String = "neutral",
    val imageUrl: String = ""
)

@Singleton
class AvatarRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val _activeAvatars = MutableStateFlow<List<AvatarItem>>(emptyList())
    val activeAvatars: StateFlow<List<AvatarItem>> = _activeAvatars.asStateFlow()

    // Default to FALSE (disabled) so experimental feature is OFF by default until Firestore explicitly enables it
    private val _isFeatureEnabled = MutableStateFlow(false)
    val isFeatureEnabled: StateFlow<Boolean> = _isFeatureEnabled.asStateFlow()

    private var activePackId: String = "default_vectors"

    init {
        listenToAvatarSettings()
    }

    private fun listenToAvatarSettings() {
        firestore.collection("system_config").document("avatar_settings")
            .addSnapshotListener { snapshot: DocumentSnapshot?, error: FirebaseFirestoreException? ->
                if (error != null) {
                    Log.w("AvatarRepository", "Listen to avatar_settings failed: ${error.message}")
                    _isFeatureEnabled.value = false
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val rawEnabled = snapshot.get("feature_avatars_enabled")
                    val enabled = when (rawEnabled) {
                        is Boolean -> rawEnabled
                        is String -> rawEnabled.toBooleanStrictOrNull() ?: false
                        else -> false
                    }
                    
                    val packId = snapshot.getString("activePackId") ?: "default_vectors"
                    _isFeatureEnabled.value = enabled
                    Log.d("AvatarRepository", "Remote config loaded: feature_avatars_enabled=$enabled, packId=$packId")
                    
                    if (enabled && (packId != activePackId || _activeAvatars.value.isEmpty())) {
                        activePackId = packId
                        listenToActivePack(packId)
                    }
                } else {
                    _isFeatureEnabled.value = false
                    Log.d("AvatarRepository", "avatar_settings doc not found. Feature DISABLED.")
                }
            }
    }

    private fun listenToActivePack(packId: String) {
        Log.d("AvatarRepository", "Listening to avatar pack doc: '$packId'")
        firestore.collection("avatar_packs").document(packId)
            .addSnapshotListener { snapshot: DocumentSnapshot?, error: FirebaseFirestoreException? ->
                if (error != null) {
                    Log.w("AvatarRepository", "Listen to pack $packId failed: ${error.message}")
                    fallbackToAnyPack()
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val rawList = snapshot.get("avatars") as? List<*>
                    val items = parseAvatarList(rawList)

                    if (items.isNotEmpty()) {
                        _activeAvatars.value = items
                        Log.d("AvatarRepository", "Successfully loaded ${items.size} avatars from pack '$packId'")
                    } else {
                        fallbackToAnyPack()
                    }
                } else {
                    Log.w("AvatarRepository", "Pack doc '$packId' does not exist, falling back...")
                    fallbackToAnyPack()
                }
            }
    }

    private fun fallbackToAnyPack() {
        firestore.collection("avatar_packs").get()
            .addOnSuccessListener { query ->
                for (doc in query.documents) {
                    val rawList = doc.get("avatars") as? List<*>
                    val items = parseAvatarList(rawList)
                    if (items.isNotEmpty()) {
                        _activeAvatars.value = items
                        Log.d("AvatarRepository", "Fallback loaded ${items.size} avatars from '${doc.id}'")
                        return@addOnSuccessListener
                    }
                }
            }
            .addOnFailureListener { err ->
                Log.e("AvatarRepository", "Fallback to avatar_packs failed", err)
            }
    }

    private fun parseAvatarList(rawList: List<*>?): List<AvatarItem> {
        return rawList?.mapNotNull { item ->
            val map = item as? Map<*, *> ?: return@mapNotNull null
            AvatarItem(
                avatarKey = (map["avatarKey"] as? String) ?: "",
                name = (map["name"] as? String) ?: "",
                gender = (map["gender"] as? String) ?: "neutral",
                imageUrl = (map["imageUrl"] as? String) ?: ""
            )
        } ?: emptyList()
    }

    fun resolveAvatarUrl(photoUrl: String?): String? {
        if (photoUrl == null) return null
        if (!photoUrl.startsWith("avatar:")) return photoUrl

        if (!_isFeatureEnabled.value) return null

        val key = photoUrl.removePrefix("avatar:").trim()
        val found = _activeAvatars.value.find { it.avatarKey == key }
        return found?.imageUrl ?: _activeAvatars.value.firstOrNull()?.imageUrl
    }
}
