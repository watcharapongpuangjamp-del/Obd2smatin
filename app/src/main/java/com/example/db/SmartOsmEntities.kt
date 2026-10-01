package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "villages")
data class VillageEntity(
    @PrimaryKey val villageId: String,
    val name: String,
    val description: String = ""
)

@Entity(
    tableName = "households",
    foreignKeys = [
        ForeignKey(
            entity = VillageEntity::class,
            parentColumns = ["villageId"],
            childColumns = ["villageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["villageId"])]
)
data class HouseholdEntity(
    @PrimaryKey val householdUuid: String,
    val villageId: String,
    val houseNo: String, // Note: houseNo is NOT the identity, householdUuid is.
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "persons",
    foreignKeys = [
        ForeignKey(
            entity = HouseholdEntity::class,
            parentColumns = ["householdUuid"],
            childColumns = ["householdUuid"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["householdUuid"])]
)
data class PersonEntity(
    @PrimaryKey val personUuid: String,
    val householdUuid: String,
    val title: String,
    val firstName: String,
    val lastName: String,
    val birthDate: Long,
    val lastUpdated: Long = System.currentTimeMillis()
)
