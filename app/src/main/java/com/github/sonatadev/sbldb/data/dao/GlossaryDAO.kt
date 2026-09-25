package com.github.sonatadev.sbldb.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.github.sonatadev.sbldb.data.entity.GlossaryTerm
import kotlinx.coroutines.flow.Flow

@Dao
interface GlossaryDAO {
    @Insert
    suspend fun insertAll(terms: List<GlossaryTerm>)

    @Query("DELETE FROM glossary")
    suspend fun deleteAll()

    @Query("SELECT * FROM glossary ORDER BY position")
    fun getAll(): Flow<List<GlossaryTerm>>
}
