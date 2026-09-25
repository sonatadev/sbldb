package com.github.sonatadev.sbldb.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.github.sonatadev.sbldb.data.entity.ExerciseJointAction
import com.github.sonatadev.sbldb.data.entity.JointAction
import com.github.sonatadev.sbldb.data.entity.JointActionMuscle
import com.github.sonatadev.sbldb.data.entity.JointActionMuscleRow
import com.github.sonatadev.sbldb.data.entity.JointActionSummary
import com.github.sonatadev.sbldb.data.entity.MuscleActionRow
import com.github.sonatadev.sbldb.data.entity.RatedExercise
import com.github.sonatadev.sbldb.data.entity.RatedJointAction
import kotlinx.coroutines.flow.Flow

@Dao
interface JointActionDAO {
    @Insert
    suspend fun insert(action: JointAction): Long

    @Update
    suspend fun update(action: JointAction)

    @Query("SELECT jointActionId FROM joint_actions WHERE joint = :joint AND name = :name")
    suspend fun findId(joint: String, name: String): Int?

    @Insert
    suspend fun insertMuscle(link: JointActionMuscle)

    @Query("DELETE FROM joint_action_muscles")
    suspend fun deleteAllMuscles()

    @Insert
    suspend fun insertExerciseLink(link: ExerciseJointAction)

    @Query("DELETE FROM exercise_joint_actions WHERE exerciseId = :exerciseId")
    suspend fun deleteExerciseLinks(exerciseId: Int)

    /** Every action with its prime-mover groups, in library order. */
    @Query(
        """
        SELECT ja.jointActionId, ja.joint, ja.name, ja.position,
            (SELECT GROUP_CONCAT(DISTINCT m.muscleGroup) FROM joint_action_muscles jam
             JOIN muscles m ON m.muscleId = jam.muscleId
             WHERE jam.jointActionId = ja.jointActionId AND jam.role = 'PRIMARY') AS primaryGroups,
            ja.whatBasic, ja.whatExpert
        FROM joint_actions ja
        ORDER BY ja.position
        """
    )
    fun getSummaries(): Flow<List<JointActionSummary>>

    @Query("SELECT * FROM joint_actions WHERE jointActionId = :id")
    fun getAction(id: Int): Flow<JointAction?>

    @Query(
        """
        SELECT m.muscleGroup, m.muscleRegion, jam.role, jam.noteBasic, jam.noteExpert FROM joint_action_muscles jam
        JOIN muscles m ON m.muscleId = jam.muscleId
        WHERE jam.jointActionId = :id
        ORDER BY jam.role, m.muscleGroup, m.muscleRegion
        """
    )
    fun getMuscles(id: Int): Flow<List<JointActionMuscleRow>>

    @Query(
        """
        SELECT e.exerciseId, e.name, e.equipment, e.attachment, e.note, eja.rating FROM exercise_joint_actions eja
        JOIN exercises e ON e.exerciseId = eja.exerciseId
        WHERE eja.jointActionId = :id
        ORDER BY eja.rating DESC, e.name
        """
    )
    fun getExercises(id: Int): Flow<List<RatedExercise>>

    @Query(
        """
        SELECT ja.jointActionId, ja.joint, ja.name, eja.rating FROM exercise_joint_actions eja
        JOIN joint_actions ja ON ja.jointActionId = eja.jointActionId
        WHERE eja.exerciseId = :exerciseId
        ORDER BY eja.rating DESC, ja.position
        """
    )
    fun getActionsForExercise(exerciseId: Int): Flow<List<RatedJointAction>>

    /** Joint actions a muscle group takes part in, with its strongest role ('PRIMARY' sorts first). */
    @Query(
        """
        SELECT ja.jointActionId, ja.joint, ja.name, MIN(jam.role) AS role FROM joint_action_muscles jam
        JOIN muscles m ON m.muscleId = jam.muscleId
        JOIN joint_actions ja ON ja.jointActionId = jam.jointActionId
        WHERE m.muscleGroup = :group
        GROUP BY ja.jointActionId
        ORDER BY role, ja.position
        """
    )
    fun getActionsForGroup(group: String): Flow<List<MuscleActionRow>>

    /** Best-rated exercises for the actions where a muscle group is a prime mover. */
    @Query(
        """
        SELECT e.exerciseId, e.name, e.equipment, e.attachment, e.note, MAX(eja.rating) AS rating
        FROM exercise_joint_actions eja
        JOIN joint_action_muscles jam ON jam.jointActionId = eja.jointActionId AND jam.role = 'PRIMARY'
        JOIN muscles m ON m.muscleId = jam.muscleId
        JOIN exercises e ON e.exerciseId = eja.exerciseId
        WHERE m.muscleGroup = :group
        GROUP BY e.exerciseId
        ORDER BY rating DESC, e.name
        LIMIT :limit
        """
    )
    fun getBestExercisesForGroup(group: String, limit: Int): Flow<List<RatedExercise>>

    /** Exercise id → joint, for filtering the exercise picker by joint. */
    @Query(
        """
        SELECT DISTINCT eja.exerciseId, ja.joint AS muscleGroup FROM exercise_joint_actions eja
        JOIN joint_actions ja ON ja.jointActionId = eja.jointActionId
        WHERE eja.rating >= 3
        """
    )
    fun getExerciseJoints(): Flow<List<ExercisePrimaryGroup>>
}
