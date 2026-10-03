package com.example.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.db.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val db = SmartOsmDatabase.getDatabase(context)
    private val dao = db.smartOsmDao()
    private val firestore = FirebaseFirestore.getInstance()

    override suspend fun doWork(): Result {
        return try {
            // 1. Sync Villages
            syncVillages()
            
            // 2. Sync Households
            syncHouseholds()
            
            // 3. Sync Persons
            syncPersons()

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("SyncWorker", "Sync failed", e)
            Result.retry()
        }
    }

    private suspend fun syncVillages() {
        val dirtyVillages = dao.getAllVillages().first().filter { it.isDirty }
        for (village in dirtyVillages) {
            firestore.collection("villages")
                .document(village.villageId)
                .set(village.copy(isDirty = false, lastSync = System.currentTimeMillis()))
                .await()
            dao.insertVillage(village.copy(isDirty = false, lastSync = System.currentTimeMillis()))
        }
    }

    private suspend fun syncHouseholds() {
        val dirtyHouseholds = dao.getAllDirtyHouseholds().first()
        for (household in dirtyHouseholds) {
            firestore.collection("households")
                .document(household.householdUuid)
                .set(household.copy(isDirty = false, lastSync = System.currentTimeMillis()))
                .await()
            dao.insertHousehold(household.copy(isDirty = false, lastSync = System.currentTimeMillis()))
        }
    }

    private suspend fun syncPersons() {
        val dirtyPersons = dao.getAllDirtyPersons().first()
        for (person in dirtyPersons) {
            firestore.collection("persons")
                .document(person.personUuid)
                .set(person.copy(isDirty = false, lastSync = System.currentTimeMillis()))
                .await()
            dao.insertPerson(person.copy(isDirty = false, lastSync = System.currentTimeMillis()))
        }
    }

    private fun scheduleNextSync(context: Context) {
        val syncRequest = androidx.work.PeriodicWorkRequestBuilder<SyncWorker>(15, java.util.concurrent.TimeUnit.MINUTES)
            .setConstraints(
                androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .build()
            )
            .build()
        androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "SmartOsmSync",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
