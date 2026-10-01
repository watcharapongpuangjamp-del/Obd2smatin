package com.example.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SmartOsmDao {
    @Query("SELECT * FROM villages")
    fun getAllVillages(): Flow<List<VillageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVillage(village: VillageEntity)

    @Query("SELECT * FROM households WHERE villageId = :villageId")
    fun getHouseholdsInVillage(villageId: String): Flow<List<HouseholdEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHousehold(household: HouseholdEntity)

    @Query("SELECT * FROM persons WHERE householdUuid = :householdUuid")
    fun getPersonsInHousehold(householdUuid: String): Flow<List<PersonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: PersonEntity)

    @Transaction
    @Query("SELECT * FROM persons WHERE personUuid = :personUuid")
    suspend fun getPersonWithDetails(personUuid: String): PersonEntity?
}
