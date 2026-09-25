package com.github.sonatadev.sbldb.data

import com.github.sonatadev.sbldb.data.entity.Role
import org.junit.Assert.assertEquals
import org.junit.Test

class MuscleDerivationTest {
    private val chest = MuscleRef("Chest", "Sternocostal Head")
    private val frontDelt = MuscleRef("Shoulders", "Anterior Head")
    private val triceps = MuscleRef("Triceps", "Lateral Head")

    private val text = Explained("basic", "expert")

    private fun action(joint: String, name: String, primary: List<MuscleRef>, secondary: List<MuscleRef> = emptyList()) =
        JointActionSeed(
            joint, name, text, text, text,
            primary.map { ActionMuscleSeed(it, Role.PRIMARY, text) } + secondary.map { ActionMuscleSeed(it, Role.SECONDARY, text) }
        )

    private val actions = listOf(
        action("Shoulder", "Horizontal Adduction", primary = listOf(chest), secondary = listOf(frontDelt)),
        action("Shoulder", "Flexion", primary = listOf(frontDelt)),
        action("Elbow", "Extension", primary = listOf(triceps))
    ).associateBy { it.key }

    private fun exercise(vararg actions: Pair<String, Int>, override: Map<MuscleRef, Role>? = null) =
        ExerciseSeed("Test", "Barbell", null, null, actions.toMap(), override)

    @Test
    fun `prime movers of directly trained actions are primary`() {
        val roles = MuscleDerivation.derive(exercise("Shoulder / Horizontal Adduction" to 4, "Elbow / Extension" to 3), actions)
        assertEquals(Role.PRIMARY, roles[chest])
        assertEquals(Role.SECONDARY, roles[triceps])
        assertEquals(Role.SECONDARY, roles[frontDelt])
    }

    @Test
    fun `the highest role wins across actions`() {
        val roles = MuscleDerivation.derive(exercise("Shoulder / Horizontal Adduction" to 5, "Shoulder / Flexion" to 4), actions)
        assertEquals(Role.PRIMARY, roles[frontDelt])
    }

    @Test
    fun `an override replaces the derivation`() {
        val override = mapOf(triceps to Role.PRIMARY)
        assertEquals(override, MuscleDerivation.derive(exercise("Shoulder / Horizontal Adduction" to 5, override = override), actions))
    }

    @Test
    fun `muscle references parse with and without region`() {
        assertEquals(MuscleRef("Lats", null), MuscleRef.parse("Lats"))
        assertEquals(MuscleRef("Rotator Cuff", "Infraspinatus/Teres Minor"), MuscleRef.parse("Rotator Cuff / Infraspinatus/Teres Minor"))
    }
}
