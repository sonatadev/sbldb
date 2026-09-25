package com.github.sonatadev.sbldb.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.github.sonatadev.sbldb.data.entity.Muscle
import kotlinx.coroutines.flow.Flow

@Dao
interface MuscleDAO {
    @Insert
    suspend fun insertMuscle(muscle: Muscle): Long

    @Query("SELECT * FROM muscles")
    fun getAllMuscle(): Flow<List<Muscle>>

    @Query("SELECT DISTINCT muscleGroup FROM muscles ORDER BY muscleGroup")
    fun getMuscleGroups(): Flow<List<String>>

    @Query("SELECT muscleId FROM muscles WHERE muscleGroup = :group AND muscleRegion IS :region")
    suspend fun findId(group: String, region: String?): Int?

    @Query("UPDATE muscles SET infoBasic = :basic, infoExpert = :expert WHERE muscleId = :id")
    suspend fun updateInfo(id: Int, basic: String?, expert: String?)

    /** The group entry first, then its regions. */
    @Query("SELECT * FROM muscles WHERE muscleGroup = :group ORDER BY muscleRegion IS NOT NULL, muscleId")
    fun getGroup(group: String): Flow<List<Muscle>>

    @Query("SELECT COUNT(*) FROM muscles")
    suspend fun count(): Int

    @Delete
    suspend fun deleteMuscle(muscle: Muscle)
}
