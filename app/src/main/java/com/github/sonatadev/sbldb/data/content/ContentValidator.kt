package com.github.sonatadev.sbldb.data.content

import com.github.sonatadev.sbldb.data.ExerciseSeed
import com.github.sonatadev.sbldb.data.GlossarySeed
import com.github.sonatadev.sbldb.data.JointActionSeed
import com.github.sonatadev.sbldb.data.MuscleDerivation
import com.github.sonatadev.sbldb.data.MuscleRef
import com.github.sonatadev.sbldb.data.MuscleSeed
import com.github.sonatadev.sbldb.data.SeedParser

/** Parsed content, ready to be checked and written to the database. */
data class ParsedContent(
    val muscles: List<MuscleSeed>,
    val actions: List<JointActionSeed>,
    val exercises: List<ExerciseSeed>,
    val glossary: List<GlossarySeed>
)

/**
 * The same consistency checks the unit tests run, available at runtime so content downloaded
 * from GitHub is only applied when it is sound.
 */
object ContentValidator {

    /** Parses and validates; returns the content, or the list of problems found. */
    fun check(files: ContentFiles): Result {
        if (files.format !in 1..ContentSource.SUPPORTED_FORMAT) {
            return Result.Invalid(listOf("Unsupported content format ${files.format}"))
        }
        val parsed = try {
            ParsedContent(
                SeedParser.parseMuscles(files.open("muscles.yaml")),
                SeedParser.parseJointActions(files.open("joint_actions.yaml")),
                SeedParser.parseExercises(files.open("exercises.yaml")),
                SeedParser.parseGlossary(files.open("glossary.yaml"))
            )
        } catch (e: Exception) {
            return Result.Invalid(listOf("Could not read content: ${e.message}"))
        }
        val problems = problems(parsed)
        return if (problems.isEmpty()) Result.Valid(parsed) else Result.Invalid(problems)
    }

    fun problems(content: ParsedContent): List<String> {
        val problems = mutableListOf<String>()
        val known = content.muscles.flatMap { m -> m.regions.map { MuscleRef(m.muscleGroup, it) } }.toSet()
        val actions = content.actions.associateBy { it.key }

        if (content.muscles.isEmpty() || content.actions.isEmpty() || content.exercises.isEmpty()) problems += "Content is empty"
        if (actions.size != content.actions.size) problems += "Duplicate joint actions"
        if (content.exercises.map { it.name }.toSet().size != content.exercises.size) problems += "Duplicate exercise names"
        if (content.glossary.map { it.term.lowercase() }.toSet().size != content.glossary.size) problems += "Duplicate glossary terms"

        content.actions.forEach { a ->
            if (a.primary.isEmpty()) problems += "${a.key} has no prime mover"
            a.animation?.problem()?.let { problems += "${a.key}: $it" }
            (a.primary + a.secondary).filter { it !in known }.forEach { problems += "${a.key}: unknown muscle $it" }
        }
        content.exercises.forEach { e ->
            e.actions.forEach { (key, rating) ->
                if (key !in actions) problems += "${e.name}: unknown action $key"
                if (rating !in 1..5) problems += "${e.name}: rating $rating for $key"
            }
            if (e.actions.keys.all { it in actions }) {
                MuscleDerivation.derive(e, actions).keys.filter { it !in known }.forEach { problems += "${e.name}: unknown muscle $it" }
            }
        }
        return problems
    }

    sealed interface Result {
        data class Valid(val content: ParsedContent) : Result
        data class Invalid(val problems: List<String>) : Result
    }
}
