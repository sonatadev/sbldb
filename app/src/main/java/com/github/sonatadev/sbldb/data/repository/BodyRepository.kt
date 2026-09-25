package com.github.sonatadev.sbldb.data.repository

import com.github.sonatadev.sbldb.data.AppDatabase
import com.github.sonatadev.sbldb.data.entity.BodyEntry
import kotlinx.coroutines.flow.Flow

class BodyRepository(db: AppDatabase) {
    private val dao = db.userDataDAO()

    /** Newest first. */
    val entries: Flow<List<BodyEntry>> = dao.bodyEntries()

    /** One entry per day: a second save on the same day fills in or overwrites that day's values. */
    suspend fun save(entry: BodyEntry) {
        val existing = dao.bodyEntryOn(entry.date)
        if (existing == null) {
            dao.insertBodyEntry(entry)
        } else {
            dao.updateBodyEntry(
                existing.copy(
                    weightKg = entry.weightKg ?: existing.weightKg,
                    waistCm = entry.waistCm ?: existing.waistCm,
                    chestCm = entry.chestCm ?: existing.chestCm,
                    armCm = entry.armCm ?: existing.armCm,
                    thighCm = entry.thighCm ?: existing.thighCm
                )
            )
        }
    }

    suspend fun delete(entry: BodyEntry) = dao.deleteBodyEntry(entry.id)
}
