package com.github.sonatadev.sbldb.data.repository

import com.github.sonatadev.sbldb.data.AppDatabase
import com.github.sonatadev.sbldb.data.dao.ExercisePrimaryGroup
import com.github.sonatadev.sbldb.data.entity.GlossaryTerm
import com.github.sonatadev.sbldb.data.entity.JointAction
import com.github.sonatadev.sbldb.data.entity.Muscle
import com.github.sonatadev.sbldb.data.entity.MuscleActionRow
import com.github.sonatadev.sbldb.data.entity.JointActionMuscleRow
import com.github.sonatadev.sbldb.data.entity.JointActionSummary
import com.github.sonatadev.sbldb.data.entity.RatedExercise
import com.github.sonatadev.sbldb.data.entity.RatedJointAction
import kotlinx.coroutines.flow.Flow

class JointActionRepository(db: AppDatabase) {
    private val dao = db.jointActionDAO()
    private val muscleDao = db.muscleDAO()
    private val glossaryDao = db.glossaryDAO()

    val glossary: Flow<List<GlossaryTerm>> = glossaryDao.getAll()

    /** A muscle group's own entry first, then its regions. */
    fun muscleGroup(group: String): Flow<List<Muscle>> = muscleDao.getGroup(group)

    fun actionsForGroup(group: String): Flow<List<MuscleActionRow>> = dao.getActionsForGroup(group)

    fun bestExercisesForGroup(group: String, limit: Int = 8): Flow<List<RatedExercise>> = dao.getBestExercisesForGroup(group, limit)

    val summaries: Flow<List<JointActionSummary>> = dao.getSummaries()

    /** Exercise id → joint it trains meaningfully (rating 3+), for filtering. */
    val exerciseJoints: Flow<List<ExercisePrimaryGroup>> = dao.getExerciseJoints()

    fun action(id: Int): Flow<JointAction?> = dao.getAction(id)

    fun muscles(id: Int): Flow<List<JointActionMuscleRow>> = dao.getMuscles(id)

    fun exercises(id: Int): Flow<List<RatedExercise>> = dao.getExercises(id)

    fun actionsForExercise(exerciseId: Int): Flow<List<RatedJointAction>> = dao.getActionsForExercise(exerciseId)
}
