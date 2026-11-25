package com.example.launchertlphoneenfant.data.remote

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class FamilyRemoteState(
    val familyCode: String = "",
    val dailyLimitMinutes: Int = 60,
    val bonusMinutesToday: Int = 0,
    val hiddenPackages: List<String> = emptyList(),
    val selectedTheme: String = "SHONEN",
    val parentPin: String = "0000",
    val usedMinutesToday: Int = 0
)

class FirebaseSyncRepository {

    private val databaseUrl = "https://launcher-enfant-default-rtdb.firebaseio.com"

    private val realtimeDatabase: FirebaseDatabase by lazy {
        try {
            FirebaseDatabase.getInstance(databaseUrl)
        } catch (e: Exception) {
            FirebaseDatabase.getInstance()
        }
    }

    fun observeFamilyState(familyCode: String): Flow<FamilyRemoteState?> = callbackFlow {
        if (familyCode.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val ref = realtimeDatabase.getReference("families").child(familyCode)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    trySend(null)
                    return
                }

                val dailyLimit = (snapshot.child("dailyLimitMinutes").value as? Long)?.toInt() ?: 60
                val bonus = (snapshot.child("bonusMinutesToday").value as? Long)?.toInt() ?: 0
                val hidden = (snapshot.child("hiddenPackages").value as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                val theme = (snapshot.child("selectedTheme").value as? String) ?: "SHONEN"
                val pin = (snapshot.child("parentPin").value as? String) ?: "0000"
                val used = (snapshot.child("usedMinutesToday").value as? Long)?.toInt() ?: 0

                val state = FamilyRemoteState(
                    familyCode = familyCode,
                    dailyLimitMinutes = dailyLimit,
                    bonusMinutesToday = bonus,
                    hiddenPackages = hidden,
                    selectedTheme = theme,
                    parentPin = pin,
                    usedMinutesToday = used
                )
                trySend(state)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(null)
            }
        }

        ref.addValueEventListener(listener)

        awaitClose {
            ref.removeEventListener(listener)
        }
    }

    suspend fun updateRemoteSettings(familyCode: String, updates: Map<String, Any>) {
        if (familyCode.isBlank()) return
        try {
            realtimeDatabase.getReference("families").child(familyCode)
                .updateChildren(updates)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun reportUsageToCloud(familyCode: String, usedMinutes: Int) {
        if (familyCode.isBlank()) return
        try {
            realtimeDatabase.getReference("families").child(familyCode)
                .child("usedMinutesToday")
                .setValue(usedMinutes)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
