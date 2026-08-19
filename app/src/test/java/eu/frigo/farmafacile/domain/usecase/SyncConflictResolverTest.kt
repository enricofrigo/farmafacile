package eu.frigo.farmafacile.domain.usecase

import eu.frigo.farmafacile.domain.model.UserMedicine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncConflictResolverTest {

    private lateinit var resolver: SyncConflictResolver

    @Before
    fun setUp() {
        resolver = SyncConflictResolver()
    }

    @Test
    fun testRemoteIsNewerThanLocalRemoteWins() {
        val local = UserMedicine(
            id = "med-1",
            listId = "list-1",
            name = "Tachipirina 500",
            quantity = 1,
            updatedAt = 1000L
        )
        val remote = UserMedicine(
            id = "med-1",
            listId = "list-1",
            name = "Tachipirina 1000",
            quantity = 2,
            updatedAt = 2000L
        )

        val result = resolver.resolve(local, remote)

        assertTrue(result is MergeResult.ApplyUpdate)
        val applied = result as MergeResult.ApplyUpdate
        assertEquals("Tachipirina 1000", applied.mergedMedicine.name)
        assertEquals(2, applied.mergedMedicine.quantity)
        assertEquals("UPDATED_FROM_REMOTE", applied.syncLog.action)
    }

    @Test
    fun testLocalIsNewerThanRemoteLocalWins() {
        val local = UserMedicine(
            id = "med-1",
            listId = "list-1",
            name = "Tachipirina 1000",
            quantity = 3,
            updatedAt = 3000L
        )
        val remote = UserMedicine(
            id = "med-1",
            listId = "list-1",
            name = "Tachipirina 500",
            quantity = 1,
            updatedAt = 2000L
        )

        val result = resolver.resolve(local, remote)

        assertTrue(result is MergeResult.KeepLocal)
        val kept = result as MergeResult.KeepLocal
        assertEquals(3, kept.localMedicine.quantity)
    }

    @Test
    fun testRemoteSoftDeletedWithNewerTimestampTriggersDeleteLocal() {
        val local = UserMedicine(
            id = "med-1",
            listId = "list-1",
            name = "Aspirina",
            updatedAt = 1000L,
            isDeleted = false
        )
        val remote = UserMedicine(
            id = "med-1",
            listId = "list-1",
            name = "Aspirina",
            updatedAt = 2500L,
            isDeleted = true
        )

        val result = resolver.resolve(local, remote)

        assertTrue(result is MergeResult.DeleteLocal)
        val del = result as MergeResult.DeleteLocal
        assertEquals("med-1", del.id)
        assertEquals("DELETED_FROM_REMOTE", del.syncLog.action)
    }

    @Test
    fun testNewRemoteItemWhenLocalIsNullTriggersApplyUpdate() {
        val remote = UserMedicine(
            id = "med-2",
            listId = "list-1",
            name = "Ibuprofene",
            updatedAt = 1500L
        )

        val result = resolver.resolve(null, remote)

        assertTrue(result is MergeResult.ApplyUpdate)
        val applied = result as MergeResult.ApplyUpdate
        assertEquals("med-2", applied.mergedMedicine.id)
        assertEquals("ADDED_FROM_REMOTE", applied.syncLog.action)
    }

    @Test
    fun testEqualTimestampsResultInNoChange() {
        val local = UserMedicine(id = "med-1", listId = "list-1", name = "Test", updatedAt = 1000L)
        val remote = UserMedicine(id = "med-1", listId = "list-1", name = "Test", updatedAt = 1000L)

        val result = resolver.resolve(local, remote)

        assertEquals(MergeResult.NoChange, result)
    }
}
