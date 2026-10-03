package com.example.service

import com.example.db.HouseholdEntity
import com.example.db.PersonEntity
import java.util.UUID

sealed class ImportValidationResult {
    data class Success(val plan: ImportPlan) : ImportValidationResult()
    data class Error(val messages: List<String>) : ImportValidationResult()
}

data class ImportPlan(
    val householdsToCreate: List<HouseholdEntity>,
    val personsToCreate: List<PersonEntity>,
    val conflicts: List<String> = emptyList()
)

class OsmImportService {
    fun validateRawData(
        villageId: String,
        rows: List<Map<String, String>>
    ): ImportValidationResult {
        val errors = mutableListOf<String>()
        val households = mutableListOf<HouseholdEntity>()
        val persons = mutableListOf<PersonEntity>()

        rows.forEachIndexed { index, row ->
            val houseNo = row["houseNo"]
            val firstName = row["firstName"]
            val lastName = row["lastName"]

            if (houseNo.isNullOrBlank()) errors.add("แถวที่ ${index + 1}: ไม่พบเลขที่บ้าน")
            if (firstName.isNullOrBlank()) errors.add("แถวที่ ${index + 1}: ไม่พบชื่อ")

            if (errors.isEmpty()) {
                val houseUuid = UUID.randomUUID().toString()
                households.add(HouseholdEntity(houseUuid, villageId, houseNo!!))
                persons.add(PersonEntity(
                    personUuid = UUID.randomUUID().toString(),
                    householdUuid = houseUuid,
                    title = row["title"] ?: "นาย",
                    firstName = firstName!!,
                    lastName = lastName ?: "",
                    birthDate = 0 // Needs parsing
                ))
            }
        }

        return if (errors.isNotEmpty()) {
            ImportValidationResult.Error(errors)
        } else {
            ImportValidationResult.Success(ImportPlan(households, persons))
        }
    }
}
