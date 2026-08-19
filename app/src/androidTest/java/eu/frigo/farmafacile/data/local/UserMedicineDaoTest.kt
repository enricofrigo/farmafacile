package eu.frigo.farmafacile.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import eu.frigo.farmafacile.data.local.user.DoseLogDao
import eu.frigo.farmafacile.data.local.user.DoseLogEntity
import eu.frigo.farmafacile.data.local.user.MedTrackUserDatabase
import eu.frigo.farmafacile.data.local.user.MedicineListDao
import eu.frigo.farmafacile.data.local.user.MedicineListEntity
import eu.frigo.farmafacile.data.local.user.UserMedicineDao
import eu.frigo.farmafacile.data.local.user.UserMedicineEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserMedicineDaoTest {

    private lateinit var database: MedTrackUserDatabase
    private lateinit var listDao: MedicineListDao
    private lateinit var medicineDao: UserMedicineDao
    private lateinit var doseLogDao: DoseLogDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MedTrackUserDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        listDao = database.medicineListDao()
        medicineDao = database.userMedicineDao()
        doseLogDao = database.doseLogDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun testInsertListAndMedicines() = runBlocking {
        val list = MedicineListEntity(
            id = "list-1",
            name = "Casa",
            description = "Armadietto",
            isShared = false,
            driveFileId = null,
            driveFolderName = null,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        listDao.insertOrUpdate(list)

        val retrievedLists = listDao.getAllLists().first()
        assertEquals(1, retrievedLists.size)
        assertEquals("Casa", retrievedLists[0].name)

        val med1 = UserMedicineEntity(
            id = "med-1",
            listId = "list-1",
            name = "Tachipirina 500",
            activeIngredient = "Paracetamolo",
            aic = "000367045",
            expiryDate = "2026-12-31",
            lotNumber = "LOT123",
            serialNumber = "SN123",
            quantity = 2,
            notes = "Per febbre",
            leafletUrl = null,
            dosageScheduleJson = null,
            isManualEntry = false,
            isDeleted = false,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        medicineDao.insertOrUpdate(med1)

        val activeMeds = medicineDao.getActiveMedicinesByList("list-1").first()
        assertEquals(1, activeMeds.size)
        assertEquals("Tachipirina 500", activeMeds[0].name)
    }

    @Test
    fun testSoftDeleteMedicine() = runBlocking {
        val list = MedicineListEntity(id = "list-1", name = "Test", description = null, isShared = false, driveFileId = null, driveFolderName = null, createdAt = 1000L, updatedAt = 1000L)
        listDao.insertOrUpdate(list)

        val med = UserMedicineEntity(
            id = "med-1",
            listId = "list-1",
            name = "Aspirina",
            activeIngredient = "Acido Acetilsalicilico",
            aic = "000527034",
            expiryDate = "2027-01-01",
            lotNumber = null,
            serialNumber = null,
            quantity = 1,
            notes = null,
            leafletUrl = null,
            dosageScheduleJson = null,
            isManualEntry = false,
            isDeleted = false,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        medicineDao.insertOrUpdate(med)

        // Soft delete
        medicineDao.softDelete("med-1", 2000L)

        val activeMeds = medicineDao.getActiveMedicinesByList("list-1").first()
        assertEquals(0, activeMeds.size)

        val allMedsSync = medicineDao.getAllMedicinesByListSync("list-1")
        assertEquals(1, allMedsSync.size)
        assertTrue(allMedsSync[0].isDeleted)
    }

    @Test
    fun testDoseLogDao() = runBlocking {
        val log = DoseLogEntity(
            id = "log-1",
            medicineId = "med-1",
            medicineName = "Tachipirina",
            scheduledTime = "2026-08-19 08:00",
            actionTime = 1700000000L,
            status = "TAKEN",
            createdAt = 1700000000L
        )
        doseLogDao.insertLog(log)

        val todayLogs = doseLogDao.getLogsForDate("2026-08-19").first()
        assertEquals(1, todayLogs.size)
        assertEquals("TAKEN", todayLogs[0].status)
    }
}
