package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.HouseholdEntity
import com.example.db.PersonEntity
import com.example.db.SmartOsmDatabase
import com.example.db.VillageEntity
import com.example.db.VillageMembershipEntity
import com.example.model.AuthState
import com.example.repository.AuthRepository
import com.example.service.SyncWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class OsmViewModel(application: Application) : AndroidViewModel(application) {
    private val db = SmartOsmDatabase.getDatabase(application)
    private val dao = db.smartOsmDao()
    private val authRepository = AuthRepository()

    // Current user's assigned villages
    val myVillages: StateFlow<List<VillageEntity>> = authRepository.authState
        .flatMapLatest { state ->
            if (state is com.example.model.AuthState.Authenticated) {
                dao.getMyVillages(state.user.uid)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedVillage = MutableStateFlow<VillageEntity?>(null)
    val selectedVillage = _selectedVillage.asStateFlow()

    val householdsInSelectedVillage: StateFlow<List<HouseholdEntity>> = _selectedVillage
        .flatMapLatest { village ->
            if (village != null) {
                dao.getHouseholdsInVillage(village.villageId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedHousehold = MutableStateFlow<HouseholdEntity?>(null)
    val selectedHousehold = _selectedHousehold.asStateFlow()

    val personsInSelectedHousehold: StateFlow<List<PersonEntity>> = _selectedHousehold
        .flatMapLatest { household ->
            if (household != null) {
                dao.getPersonsInHousehold(household.householdUuid)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectVillage(village: VillageEntity) {
        _selectedVillage.value = village
        _selectedHousehold.value = null
    }

    fun selectHousehold(household: HouseholdEntity) {
        _selectedHousehold.value = household
    }

    fun triggerSync() {
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
        WorkManager.getInstance(getApplication()).enqueue(syncRequest)
    }

    private val _aiHealthReport = MutableStateFlow<String?>(null)
    val aiHealthReport = _aiHealthReport.asStateFlow()

    fun requestAiConsult() {
        viewModelScope.launch {
            val villageName = _selectedVillage.value?.name ?: "หมู่บ้านของคุณ"
            // Get all persons in the village for analysis
            // Simplified for now
            val report = AuthRepository().let { auth ->
                // This would call VehicleRepository's analyzeCommunityHealth
                "ผลวิเคราะห์ AI: ระบบกำลังประมวลผลข้อมูลสุขภาพชุมชนเชิงรุก..."
            }
            _aiHealthReport.value = report
        }
    }

    fun commitImportPlan(plan: com.example.service.ImportPlan) {
        viewModelScope.launch {
            plan.householdsToCreate.forEach { dao.insertHousehold(it) }
            plan.personsToCreate.forEach { dao.insertPerson(it) }
            triggerSync()
        }
    }

    fun addVillage(id: String, name: String, desc: String) {
        viewModelScope.launch {
            val village = VillageEntity(id, name, desc)
            dao.insertVillage(village)
            // Auto grant membership to current user for local testing
            authRepository.getCurrentUser()?.let { user ->
                dao.insertMembership(VillageMembershipEntity(user.uid, id))
            }
        }
    }

    fun addHousehold(houseNo: String) {
        val villageId = _selectedVillage.value?.villageId ?: return
        viewModelScope.launch {
            val household = HouseholdEntity(
                householdUuid = UUID.randomUUID().toString(),
                villageId = villageId,
                houseNo = houseNo
            )
            dao.insertHousehold(household)
        }
    }

    fun addPerson(title: String, firstName: String, lastName: String, birthDate: Long) {
        val householdUuid = _selectedHousehold.value?.householdUuid ?: return
        viewModelScope.launch {
            val person = PersonEntity(
                personUuid = UUID.randomUUID().toString(),
                householdUuid = householdUuid,
                title = title,
                firstName = firstName,
                lastName = lastName,
                birthDate = birthDate
            )
            dao.insertPerson(person)
        }
    }
}
