package com.example.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.db.ObdDatabase
import com.example.model.DtcCode
import com.example.model.ServiceUrgencyStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ServiceIntervalRobolectricTest {

    private lateinit var db: ObdDatabase
    private lateinit var repository: VehicleRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ObdDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = VehicleRepository(context, db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `seed default service intervals creates engine oil, brake, transmission, filters, and ecu cycle`() = runBlocking {
        repository.seedDefaultServiceIntervalsIfEmpty(vehicleId = 1L, currentOdoKm = 120000)
        val intervals = repository.allServiceIntervals.first()

        assertTrue(intervals.size >= 5)
        assertTrue(intervals.any { it.titleTh.contains("น้ำมันเครื่อง") })
        assertTrue(intervals.any { it.titleTh.contains("Drive Cycle") })
    }

    @Test
    fun `record DTC clear event updates DTC clear tracking table and DTC cycle interval`() = runBlocking {
        repository.seedDefaultServiceIntervalsIfEmpty(vehicleId = 1L, currentOdoKm = 120000)

        val codes = listOf(
            DtcCode(code = "P0420", descriptionEn = "Catalyst System Efficiency", descriptionTh = "ประสิทธิภาพแคตตาไลติกต่ำกว่าเกณฑ์", severity = "Medium", recommendedAction = "ตรวจสอบ")
        )

        repository.recordDtcClearEvent(
            vehicleId = 1L,
            odometerKm = 120000,
            clearedCodes = codes,
            notes = "Cleared in test"
        )

        val latestEvent = repository.latestDtcClearEvent.first()
        assertNotNull(latestEvent)
        assertEquals(1, latestEvent?.clearedCount)
        assertEquals(120000, latestEvent?.odometerKm)

        val intervals = repository.allServiceIntervals.first()
        val dtcInterval = intervals.firstOrNull { it.trackDtcClear }
        assertNotNull(dtcInterval)
        assertEquals(120000, dtcInterval?.lastDtcClearKm)
    }

    @Test
    fun `record service completed resets interval and logs maintenance history`() = runBlocking {
        repository.seedDefaultServiceIntervalsIfEmpty(vehicleId = 1L, currentOdoKm = 120000)
        val initialIntervals = repository.allServiceIntervals.first()
        val oilInterval = initialIntervals.first { it.titleTh.contains("น้ำมันเครื่อง") }

        repository.recordServiceCompleted(
            intervalId = oilInterval.id,
            vehicleId = 1L,
            titleTh = oilInterval.titleTh,
            costBaht = 1800.0,
            currentOdoKm = 130000,
            category = oilInterval.category,
            notes = "เปลี่ยนน้ำมันเครื่อง 5W-30"
        )

        val updatedIntervals = repository.allServiceIntervals.first()
        val updatedOil = updatedIntervals.first { it.id == oilInterval.id }
        assertEquals(130000, updatedOil.lastServiceKm)

        val logs = repository.allLogs.first()
        assertTrue(logs.any { it.title.contains("น้ำมันเครื่อง") && it.cost == 1800.0 })
    }
}
