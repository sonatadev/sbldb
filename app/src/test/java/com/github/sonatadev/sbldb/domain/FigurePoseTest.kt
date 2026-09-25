package com.github.sonatadev.sbldb.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FigurePoseTest {
    private fun anim(view: FigureView, dof: Dof, to: Float) = ActionAnimation(view, dof, 0f, to)

    @Test
    fun shoulderFlexionAtNinetyPointsTheArmForward() {
        val figure = FigurePose.pose(anim(FigureView.SIDE, Dof.SHOULDER_FLEX, 170f), 90f)
        val arm = figure.bones.filter { it.moving }
        assertEquals(3, arm.size)
        val upper = arm.first()
        assertTrue(upper.b.x - upper.a.x > 0.3f)
        assertEquals(upper.a.y, upper.b.y, 0.01f)
    }

    @Test
    fun abductionMovesBothArmsSymmetrically() {
        val figure = FigurePose.pose(anim(FigureView.FRONT, Dof.SHOULDER_ABD, 170f), 90f)
        val ends = figure.bones.filter { it.moving }.map { it.b.x }
        assertEquals(ends.max(), -ends.min(), 0.001f)
        assertTrue(ends.max() > 0.8f)
    }

    @Test
    fun horizontalAdductionAtNinetyReachesForward() {
        val figure = FigurePose.pose(anim(FigureView.TOP, Dof.SHOULDER_HZ, 100f), 90f)
        figure.bones.filter { it.moving }.forEach { assertTrue(it.b.y < it.a.y) } // forward is up
    }

    @Test
    fun kneeFlexionOnlyMovesShinAndFoot() {
        val figure = FigurePose.pose(anim(FigureView.SIDE, Dof.KNEE_FLEX, 120f), 90f)
        assertEquals(2, figure.bones.count { it.moving })
    }

    @Test
    fun everyDofHasAViewThatDrawsSomethingMoving() {
        Dof.entries.forEach { dof ->
            val figure = FigurePose.pose(ActionAnimation(dof.views.first(), dof, dof.min, dof.max), (dof.min + dof.max) / 2)
            assertTrue(dof.key, figure.bones.any { it.moving } || figure.headMoving)
        }
    }

    @Test
    fun encodeDecodeRoundTripAndValidation() {
        val a = ActionAnimation(FigureView.SIDE, Dof.ELBOW_FLEX, 5f, 145f)
        assertEquals(a, ActionAnimation.decode(a.encode()))
        assertNull(a.problem())
        assertNotNull(a.copy(view = FigureView.TOP).problem())
        assertNotNull(a.copy(to = 200f).problem())
        assertNotNull(a.copy(to = 5f).problem())
        assertNull(ActionAnimation.decode("side|nope|0|1"))
    }
}
