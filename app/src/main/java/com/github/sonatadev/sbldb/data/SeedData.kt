package com.github.sonatadev.sbldb.data

import android.content.Context
import androidx.room.withTransaction
import com.github.sonatadev.sbldb.data.entity.Exercise
import com.github.sonatadev.sbldb.data.entity.ExerciseJointAction
import com.github.sonatadev.sbldb.data.entity.ExerciseMuscle
import com.github.sonatadev.sbldb.data.entity.GlossaryTerm
import com.github.sonatadev.sbldb.data.entity.JointAction
import com.github.sonatadev.sbldb.data.entity.JointActionMuscle
import com.github.sonatadev.sbldb.data.entity.Muscle
import com.github.sonatadev.sbldb.data.entity.Role
import com.github.sonatadev.sbldb.data.repository.SettingsRepository
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import java.io.InputStream
import java.security.MessageDigest

/** "Group" or "Group / Region", as written in the YAML files. */
data class MuscleRef(val group: String, val region: String?) {
    override fun toString() = region?.let { "$group / $it" } ?: group

    companion object {
        fun parse(text: String): MuscleRef {
            val parts = text.split(" / ", limit = 2).map { it.trim() }
            return MuscleRef(parts[0], parts.getOrNull(1))
        }
    }
}

/** A text written twice: in plain language and for experienced lifters. */
data class Explained(val basic: String, val expert: String)

data class RegionSeed(val name: String?, val text: Explained)

data class MuscleSeed(val muscleGroup: String, val text: Explained, val regionSeeds: List<RegionSeed>) {
    /** Region names including the region-less group entry (null), which always comes first. */
    val regions: List<String?> get() = listOf(null) + regionSeeds.map { it.name }
}

data class ActionMuscleSeed(val muscle: MuscleRef, val role: Role, val text: Explained)

data class JointActionSeed(
    val joint: String,
    val name: String,
    val what: Explained,
    val why: Explained,
    val feel: Explained,
    val muscles: List<ActionMuscleSeed>
) {
    /** How exercises refer to this action: "Shoulder / Horizontal Adduction". */
    val key: String get() = "$joint / $name"
    val primary: List<MuscleRef> get() = muscles.filter { it.role == Role.PRIMARY }.map { it.muscle }
    val secondary: List<MuscleRef> get() = muscles.filter { it.role == Role.SECONDARY }.map { it.muscle }
}

data class ExerciseSeed(
    val name: String,
    val equipment: String,
    val attachment: String?,
    val note: String?,
    /** Joint action key → rating 1–5, in file order. */
    val actions: Map<String, Int>,
    /** Explicit muscle roles replacing the derivation, when present. */
    val muscleOverride: Map<MuscleRef, Role>?
)

data class GlossarySeed(val term: String, val text: Explained)

/** Parses the YAML files in assets/. Independent of Android so it can be unit tested. */
object SeedParser {

    fun parseMuscles(input: InputStream): List<MuscleSeed> =
        loadList(input).map { entry ->
            val group = entry.requireString("muscleGroup")
            MuscleSeed(
                muscleGroup = group,
                text = entry.explained(group),
                regionSeeds = (entry["regions"] as? List<*>).orEmpty().map { raw ->
                    val region = raw as Map<*, *>
                    val name = region.requireString("name")
                    RegionSeed(name, region.explained("$group / $name"))
                }
            )
        }

    fun parseJointActions(input: InputStream): List<JointActionSeed> =
        loadList(input).map { entry ->
            val joint = entry.requireString("joint")
            val name = entry.requireString("name")
            val key = "$joint / $name"
            JointActionSeed(
                joint = joint,
                name = name,
                what = (entry["what"] as Map<*, *>).explained("$key what"),
                why = (entry["why"] as Map<*, *>).explained("$key why"),
                feel = (entry["feel"] as Map<*, *>).explained("$key feel"),
                muscles = (entry["muscles"] as? List<*>).orEmpty().map { raw ->
                    val muscle = raw as Map<*, *>
                    val ref = muscle.requireString("muscle")
                    ActionMuscleSeed(MuscleRef.parse(ref), Role.valueOf(muscle.requireString("role")), muscle.explained("$key $ref"))
                }
            )
        }

    fun parseExercises(input: InputStream): List<ExerciseSeed> =
        loadList(input).map { entry ->
            val name = entry.requireString("name")
            val actions = (entry["actions"] as? Map<*, *>).orEmpty()
                .map { (key, rating) -> key as String to (rating as Number).toInt() }
                .toMap()
            require(actions.isNotEmpty()) { "Exercise '$name' has no joint actions" }
            val muscles = entry["muscles"] as? Map<*, *>
            ExerciseSeed(
                name = name,
                equipment = entry.requireString("equipment"),
                attachment = entry["attachment"] as String?,
                note = entry["note"] as String?,
                actions = actions,
                muscleOverride = muscles?.let {
                    it.muscleList("primary").associateWith { Role.PRIMARY } +
                        it.muscleList("secondary").associateWith { Role.SECONDARY }
                }
            )
        }

    fun parseGlossary(input: InputStream): List<GlossarySeed> =
        loadList(input).map { entry ->
            val term = entry.requireString("term")
            GlossarySeed(term, entry.explained(term))
        }

    private fun loadList(input: InputStream): List<Map<*, *>> =
        input.use { stream ->
            val yaml = Yaml(SafeConstructor(LoaderOptions()))
            (yaml.load<Any?>(stream) as List<*>).map { it as Map<*, *> }
        }

    private fun Map<*, *>.requireString(key: String): String =
        requireNotNull(this[key] as String?) { "Missing '$key' in $this" }

    private fun Map<*, *>.explained(owner: String): Explained {
        val basic = (this["basic"] as String?)?.trim()
        val expert = (this["expert"] as String?)?.trim()
        require(!basic.isNullOrEmpty() && !expert.isNullOrEmpty()) { "$owner needs both a basic and an expert text" }
        return Explained(basic, expert)
    }

    private fun Map<*, *>.muscleList(key: String): List<MuscleRef> =
        (this[key] as? List<*>).orEmpty().map { MuscleRef.parse(it as String) }
}

object MuscleDerivation {
    /** Rating from which an exercise counts as directly training an action. */
    const val DIRECT_RATING = 4

    /**
     * Muscle roles of an exercise, derived from its joint actions: a muscle is PRIMARY when it is a
     * prime mover of an action rated [DIRECT_RATING] or more, otherwise SECONDARY. When a muscle
     * appears in several actions the highest role wins. An explicit override replaces all of this.
     */
    fun derive(exercise: ExerciseSeed, actions: Map<String, JointActionSeed>): Map<MuscleRef, Role> {
        exercise.muscleOverride?.let { return it }
        val roles = LinkedHashMap<MuscleRef, Role>()
        for ((key, rating) in exercise.actions) {
            val action = requireNotNull(actions[key]) { "Exercise '${exercise.name}' references unknown action '$key'" }
            fun add(muscle: MuscleRef, role: Role) {
                if (roles[muscle] != Role.PRIMARY) roles[muscle] = role
            }
            action.primary.forEach { add(it, if (rating >= DIRECT_RATING) Role.PRIMARY else Role.SECONDARY) }
            action.secondary.forEach { add(it, Role.SECONDARY) }
        }
        return roles
    }
}

object SeedData {
    private val files = listOf("muscles.yaml", "joint_actions.yaml", "exercises.yaml", "glossary.yaml")

    /**
     * Brings the reference tables in line with assets/ whenever the files change (or the database
     * was recreated). Rows are matched by natural key, so logged workouts keep their exercises;
     * exercises removed from the files are kept for history.
     */
    suspend fun sync(context: Context, db: AppDatabase, settings: SettingsRepository) {
        val hash = contentHash(context)
        if (hash == settings.contentHash() && db.muscleDAO().count() > 0) return

        val muscles = SeedParser.parseMuscles(context.assets.open(files[0]))
        val actions = SeedParser.parseJointActions(context.assets.open(files[1]))
        val exercises = SeedParser.parseExercises(context.assets.open(files[2]))
        val glossary = SeedParser.parseGlossary(context.assets.open(files[3]))

        db.withTransaction { apply(db, muscles, actions, exercises, glossary) }
        settings.setContentHash(hash)
    }

    private suspend fun apply(
        db: AppDatabase,
        muscles: List<MuscleSeed>,
        actions: List<JointActionSeed>,
        exercises: List<ExerciseSeed>,
        glossary: List<GlossarySeed>
    ) {
        val muscleDao = db.muscleDAO()
        val muscleIds = HashMap<MuscleRef, Int>()
        for (muscle in muscles) {
            val entries = listOf(RegionSeed(null, muscle.text)) + muscle.regionSeeds
            for (region in entries) {
                val existing = muscleDao.findId(muscle.muscleGroup, region.name)
                val id = if (existing != null) {
                    muscleDao.updateInfo(existing, region.text.basic, region.text.expert)
                    existing
                } else {
                    muscleDao.insertMuscle(Muscle(0, muscle.muscleGroup, region.name, region.text.basic, region.text.expert)).toInt()
                }
                muscleIds[MuscleRef(muscle.muscleGroup, region.name)] = id
            }
        }
        fun muscleId(ref: MuscleRef, owner: String) =
            requireNotNull(muscleIds[ref]) { "$owner references unknown muscle $ref" }

        val actionDao = db.jointActionDAO()
        val actionIds = HashMap<String, Int>()
        actionDao.deleteAllMuscles()
        actions.forEachIndexed { position, seed ->
            val existing = actionDao.findId(seed.joint, seed.name)
            val row = JointAction(
                jointActionId = existing ?: 0,
                joint = seed.joint,
                name = seed.name,
                description = seed.what.basic,
                position = position,
                whatBasic = seed.what.basic,
                whatExpert = seed.what.expert,
                whyBasic = seed.why.basic,
                whyExpert = seed.why.expert,
                feelBasic = seed.feel.basic,
                feelExpert = seed.feel.expert
            )
            val id = if (existing != null) existing.also { actionDao.update(row) } else actionDao.insert(row).toInt()
            actionIds[seed.key] = id
            seed.muscles.forEach { m ->
                actionDao.insertMuscle(JointActionMuscle(id, muscleId(m.muscle, seed.key), m.role, m.text.basic, m.text.expert))
            }
        }

        val exerciseDao = db.exerciseDAO()
        val actionsByKey = actions.associateBy { it.key }
        for (seed in exercises) {
            val existing = exerciseDao.findId(seed.name)
            val id = if (existing != null) {
                exerciseDao.updateExercise(Exercise(existing, seed.name, seed.equipment, seed.attachment, seed.note))
                existing
            } else {
                exerciseDao.insertExercise(Exercise(0, seed.name, seed.equipment, seed.attachment, seed.note)).toInt()
            }
            actionDao.deleteExerciseLinks(id)
            seed.actions.forEach { (key, rating) ->
                val actionId = requireNotNull(actionIds[key]) { "Exercise '${seed.name}' references unknown action '$key'" }
                actionDao.insertExerciseLink(ExerciseJointAction(id, actionId, rating))
            }
            db.exerciseMuscleDAO().deleteForExercise(id)
            MuscleDerivation.derive(seed, actionsByKey).forEach { (ref, role) ->
                db.exerciseMuscleDAO().insertExerciseMuscle(ExerciseMuscle(id, muscleId(ref, seed.name), role))
            }
        }

        val glossaryDao = db.glossaryDAO()
        glossaryDao.deleteAll()
        glossaryDao.insertAll(glossary.mapIndexed { i, g -> GlossaryTerm(g.term, g.text.basic, g.text.expert, i) })
    }

    private fun contentHash(context: Context): String {
        val digest = MessageDigest.getInstance("SHA-256")
        files.forEach { name -> context.assets.open(name).use { digest.update(it.readBytes()) } }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
