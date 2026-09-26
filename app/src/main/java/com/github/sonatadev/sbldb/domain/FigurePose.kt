package com.github.sonatadev.sbldb.domain

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** A point in figure space: x to the right, y down, the standing figure spans roughly -1..1. */
data class P(val x: Float, val y: Float) {
    operator fun plus(o: P) = P(x + o.x, y + o.y)
    operator fun times(k: Float) = P(x * k, y * k)
}

/** One body segment; [moving] segments are the ones the joint action moves (drawn in the accent). */
data class Bone(val a: P, val b: P, val moving: Boolean, val ghost: Boolean = false, val width: Float = 0.04f)

data class Figure(
    val bones: List<Bone>,
    val head: P,
    val headRadius: Float,
    /** Small mark showing which way the head faces (the nose), or null in side and front views. */
    val nose: P? = null,
    val headMoving: Boolean = false,
    /** Seen from above the head sits over the shoulders and hides what is under it. */
    val headOccludes: Boolean = false,
    /** How much to magnify the figure; the top view is small and gets drawn larger. */
    val zoom: Float = 1f,
    /** Figure-space point to centre on when zoomed in. */
    val focus: P = P(0f, 0f),
    /** A word describing the pose right now, for moves that are hard to read from shape alone. */
    val label: PoseLabel? = null
)

/** Hand orientation for forearm rotation, shown as text over the figure. */
enum class PoseLabel { PALM_DOWN, THUMB_UP, PALM_UP }

/**
 * Builds the stick figure for a joint action at one angle. Side view faces right; front view faces
 * the viewer and moves both sides together; top view looks down with the front of the body up.
 */
object FigurePose {
    private const val TORSO = 0.67f
    private const val UPPER_ARM = 0.34f
    private const val FOREARM = 0.3f
    private const val HAND = 0.1f
    private const val THIGH = 0.47f
    private const val SHIN = 0.45f
    private const val FOOT = 0.14f
    private val HIP = P(0f, 0.05f)

    private fun rad(deg: Float) = Math.toRadians(deg.toDouble()).toFloat()

    /** Unit vector [deg] degrees from straight down, turning toward +x. */
    private fun dir(deg: Float) = P(sin(rad(deg)), cos(rad(deg)))

    /** Small joints get a close-up of the limb alone: on the whole body they are a few dots. */
    private val CLOSE_UP = setOf(Dof.WRIST_FLEX, Dof.FOREARM_ROT, Dof.ANKLE_FLEX)

    fun pose(animation: ActionAnimation, value: Float): Figure = when {
        animation.dof in CLOSE_UP -> closeUp(animation.dof, value)
        // Hip extension reads best as a hinge (RDL, hip thrust), not a straight leg swinging back
        animation.dof == Dof.HIP_FLEX && animation.from > animation.to -> hinge(value)
        else -> wholeBody(animation, value)
    }

    private fun wholeBody(animation: ActionAnimation, value: Float): Figure = when (animation.view) {
        FigureView.SIDE -> side(animation.dof, value)
        FigureView.FRONT -> front(animation.dof, value)
        FigureView.TOP -> top(animation.dof, value)
    }

    private fun side(dof: Dof, v: Float): Figure {
        val lean = if (dof == Dof.SPINE_FLEX) v else 0f
        val up = P(sin(rad(lean)), -cos(rad(lean)))
        val neck = HIP + up * TORSO
        val shoulder = HIP + up * (TORSO - 0.04f)
        val head = HIP + up * (TORSO + 0.18f)

        val armAngle = if (dof == Dof.SHOULDER_FLEX) v else 0f
        val elbowAngle = when (dof) {
            Dof.ELBOW_FLEX -> v
            Dof.WRIST_FLEX -> 90f
            else -> 8f
        }
        val handAngle = armAngle + elbowAngle + if (dof == Dof.WRIST_FLEX) v else 0f
        val elbow = shoulder + dir(armAngle) * UPPER_ARM
        val wrist = elbow + dir(armAngle + elbowAngle) * FOREARM
        val fingers = wrist + dir(handAngle) * HAND

        val thighAngle = if (dof == Dof.HIP_FLEX) v else 0f
        // Raising the thigh bends the knee too, like marching
        val kneeBend = when (dof) {
            Dof.KNEE_FLEX -> v
            Dof.HIP_FLEX -> (v * 0.9f).coerceAtLeast(0f)
            else -> 0f
        }
        val shinAngle = thighAngle - kneeBend
        val footAngle = shinAngle + 90f - if (dof == Dof.ANKLE_FLEX) v else 0f
        val knee = HIP + dir(thighAngle) * THIGH
        val ankle = knee + dir(shinAngle) * SHIN
        val toes = ankle + dir(footAngle) * FOOT

        val arm = dof == Dof.SHOULDER_FLEX
        val forearm = arm || dof == Dof.ELBOW_FLEX
        val hand = forearm || dof == Dof.WRIST_FLEX
        val thigh = dof == Dof.HIP_FLEX
        val shin = thigh || dof == Dof.KNEE_FLEX
        val foot = shin || dof == Dof.ANKLE_FLEX
        val torso = dof == Dof.SPINE_FLEX
        // A second, still leg keeps the one-leg moves readable as standing
        val standingLeg = if (thigh || shin) listOf(
            Bone(HIP, HIP + dir(0f) * THIGH, false, ghost = true, width = 0.06f),
            Bone(HIP + dir(0f) * THIGH, HIP + dir(0f) * (THIGH + SHIN), false, ghost = true, width = 0.045f),
            Bone(HIP + dir(0f) * (THIGH + SHIN), HIP + dir(0f) * (THIGH + SHIN) + dir(90f) * FOOT, false, ghost = true, width = 0.03f)
        ) else emptyList()
        return Figure(
            bones = standingLeg + listOf(
                Bone(HIP, neck, torso, width = 0.1f),
                Bone(HIP, knee, thigh, width = 0.065f),
                Bone(knee, ankle, shin, width = 0.048f),
                Bone(ankle, toes, foot, width = 0.03f),
                Bone(shoulder, elbow, arm, width = 0.045f),
                Bone(elbow, wrist, forearm, width = 0.038f),
                Bone(wrist, fingers, hand, width = 0.03f)
            ),
            head = head,
            headRadius = 0.12f,
            headMoving = torso
        )
    }

    private fun front(dof: Dof, v: Float): Figure {
        val tilt = if (dof == Dof.SPINE_LAT) v else 0f
        val up = P(sin(rad(tilt)), -cos(rad(tilt)))
        val across = P(cos(rad(tilt)), sin(rad(tilt)))
        val neck = HIP + up * TORSO
        val lift = when (dof) {
            Dof.SCAP_ELEV -> v / 100f
            Dof.SCAP_ROT -> v / 300f
            else -> 0f
        }
        val girdle = neck + up * (lift - 0.05f)
        val abduction = when (dof) {
            Dof.SHOULDER_ABD -> v
            Dof.SCAP_ROT -> v * 2.6f
            else -> 6f
        }
        val armsMove = dof in setOf(Dof.SHOULDER_ABD, Dof.SCAP_ELEV, Dof.SCAP_ROT, Dof.SPINE_LAT)
        val girdleMoves = dof in setOf(Dof.SCAP_ELEV, Dof.SCAP_ROT, Dof.SPINE_LAT)
        val bones = mutableListOf(Bone(HIP, neck, dof == Dof.SPINE_LAT, width = 0.13f))

        for (side in listOf(1f, -1f)) {
            val shoulder = girdle + across * (0.2f * side)
            bones += Bone(girdle, shoulder, girdleMoves, width = 0.05f)
            val armDir = P(sin(rad(abduction)) * side, cos(rad(abduction)))
            val elbow = shoulder + armDir * UPPER_ARM
            val wrist = elbow + armDir * FOREARM
            bones += Bone(shoulder, elbow, armsMove, width = 0.045f)
            bones += Bone(elbow, wrist, armsMove || dof == Dof.FOREARM_ROT, width = 0.038f)
            if (dof == Dof.FOREARM_ROT) {
                // Palm width as seen from the front, thumb on the side it has turned to
                val half = 0.015f + 0.05f * abs(sin(rad(v)))
                val top = wrist + P(0f, 0.02f)
                bones += Bone(top + P(-half, 0f), top + P(half, 0f), true)
                bones += Bone(top + P(-half, 0f), top + P(-half, HAND), true)
                bones += Bone(top + P(half, 0f), top + P(half, HAND), true)
                val thumbSide = side * sin(rad(v))
                bones += Bone(top + P(thumbSide * (half + 0.035f), 0.01f), top + P(thumbSide * (half + 0.035f), 0.06f), true)
            } else {
                bones += Bone(wrist, wrist + armDir * HAND, armsMove, width = 0.03f)
            }

            val hipPoint = HIP + P((if (dof == Dof.HIP_ROT) 0.16f else 0.1f) * side, 0f)
            bones += Bone(HIP, hipPoint, false, width = 0.07f)
            if (dof == Dof.HIP_ROT) {
                // Seated, thighs toward the viewer: external rotation swings the foot inward
                val knee = hipPoint + P(0f, 0.12f)
                val shinDir = P(-sin(rad(v)) * side, cos(rad(v)))
                val ankle = knee + shinDir * (SHIN * 0.85f)
                bones += Bone(hipPoint, knee, false, width = 0.065f)
                bones += Bone(knee, ankle, true, width = 0.048f)
                bones += Bone(ankle, ankle + P(0.06f * side, 0f) + shinDir * 0.02f, true)
            } else {
                val legAngle = if (dof == Dof.HIP_ABD) v else 3f
                val legDir = P(sin(rad(legAngle)) * side, cos(rad(legAngle)))
                val knee = hipPoint + legDir * THIGH
                val ankle = knee + legDir * SHIN
                val moving = dof == Dof.HIP_ABD
                bones += Bone(hipPoint, knee, moving, width = 0.06f)
                bones += Bone(knee, ankle, moving, width = 0.045f)
                bones += Bone(ankle, ankle + P(0.07f * side, 0f), moving)
            }
        }
        if (dof == Dof.HIP_ROT) bones += Bone(P(-0.45f, 0.19f), P(0.45f, 0.19f), false, ghost = true, width = 0.02f)
        return Figure(bones, head = HIP + up * (TORSO + 0.18f), headRadius = 0.13f, headMoving = dof == Dof.SPINE_LAT)
    }

    private fun top(dof: Dof, v: Float): Figure {
        val center = P(0f, 0.1f)
        val twist = if (dof == Dof.SPINE_ROT) v else 0f
        val across = P(cos(rad(twist)), sin(rad(twist)))
        val forward = P(sin(rad(twist)), -cos(rad(twist)))
        val shift = if (dof == Dof.SCAP_PROTRACT) v / 100f else 0f
        val bones = mutableListOf<Bone>()
        if (dof == Dof.SPINE_ROT) bones += Bone(center + P(-0.2f, 0.04f), center + P(0.2f, 0.04f), false, ghost = true, width = 0.08f)

        for (side in listOf(1f, -1f)) {
            val width = 0.32f - abs(shift) * 0.25f * if (shift > 0) 1f else -1f
            val shoulder = center + across * (width * side) + forward * shift
            val girdleMoves = dof == Dof.SCAP_PROTRACT || dof == Dof.SPINE_ROT
            bones += Bone(center, shoulder, girdleMoves, width = 0.065f)
            when (dof) {
                Dof.SHOULDER_HZ -> {
                    val armDir = across * (cos(rad(v)) * side) + forward * sin(rad(v))
                    val elbow = shoulder + armDir * UPPER_ARM
                    bones += Bone(shoulder, elbow, true, width = 0.05f)
                    bones += Bone(elbow, elbow + armDir * (FOREARM + HAND), true, width = 0.042f)
                }
                Dof.SHOULDER_ROT -> {
                    // Upper arm hangs at the side (seen end-on); the forearm swings like a door
                    val elbow = shoulder + across * (0.03f * side)
                    val forearmDir = across * (sin(rad(v)) * side) + forward * cos(rad(v))
                    bones += Bone(shoulder, elbow, false)
                    bones += Bone(elbow, elbow + forearmDir * (FOREARM + HAND), true, width = 0.042f)
                }
                else -> {
                    // Arms reaching forward, carried by the shoulder blades or the trunk
                    val armDir = (across * (0.15f * side) + forward).let { it * (1f / kotlin.math.hypot(it.x, it.y)) }
                    val moving = dof == Dof.SCAP_PROTRACT || dof == Dof.SPINE_ROT
                    val elbow = shoulder + armDir * UPPER_ARM
                    bones += Bone(shoulder, elbow, moving, width = 0.05f)
                    bones += Bone(elbow, elbow + armDir * (FOREARM * 0.8f), moving, width = 0.042f)
                }
            }
        }
        return Figure(
            bones = bones,
            head = center,
            headRadius = 0.15f,
            nose = center + forward * 0.21f,
            headMoving = dof == Dof.SPINE_ROT,
            headOccludes = true,
            zoom = 1.5f
        )
    }

    /**
     * Hip extension as a hinge from the side: legs planted with soft knees, the trunk swinging up
     * around the hips from [lean] degrees forward, arms hanging with a bar.
     */
    private fun hinge(lean: Float): Figure {
        val angle = lean.coerceAtLeast(0f)
        val up = P(sin(rad(angle)), -cos(rad(angle)))
        val neck = HIP + up * TORSO
        val shoulder = HIP + up * (TORSO - 0.04f)
        val hands = shoulder + dir(0f) * (UPPER_ARM + FOREARM)
        val knee = HIP + dir(6f) * THIGH
        val ankle = knee + dir(-6f) * SHIN
        return Figure(
            bones = listOf(
                Bone(HIP, knee, false, width = 0.065f),
                Bone(knee, ankle, false, width = 0.048f),
                Bone(ankle, ankle + dir(90f) * FOOT, false, width = 0.03f),
                Bone(HIP, neck, true, width = 0.1f),
                Bone(shoulder, hands, true, width = 0.04f),
                // The bar, seen end-on
                Bone(hands + P(0f, 0.03f), hands + P(0f, 0.03f), false, ghost = true, width = 0.09f)
            ),
            head = HIP + up * (TORSO + 0.18f),
            headRadius = 0.12f,
            headMoving = true
        )
    }

    /** A far-away head: close-ups show no head. */
    private val NO_HEAD = P(100f, 100f)
    private const val CLOSE_UP_ZOOM = 1.3f

    private fun closeUp(dof: Dof, v: Float): Figure = when (dof) {
        Dof.WRIST_FLEX -> {
            // Wrist curl from the side: forearm resting on a bench, palm up, hand past the edge
            val wrist = P(0.05f, 0.1f)
            val hand = dir(90f + v)
            val knuckles = wrist + hand * 0.3f
            val fingers = dir(90f + v + 18f)
            Figure(
                bones = listOf(
                    Bone(P(-1.15f, 0.29f), P(0f, 0.29f), false, ghost = true, width = 0.07f),
                    Bone(P(-1.0f, 0.1f), wrist, false, width = 0.1f),
                    Bone(wrist, knuckles, true, width = 0.085f),
                    Bone(knuckles, knuckles + fingers * 0.3f, true, width = 0.045f),
                    Bone(wrist + hand * 0.08f, wrist + hand * 0.08f + dir(v) * -0.14f, true, width = 0.035f)
                ),
                head = NO_HEAD, headRadius = 0f, zoom = CLOSE_UP_ZOOM, focus = P(-0.2f, 0f)
            )
        }
        Dof.FOREARM_ROT -> {
            // Seen from above, forearm pointing away: the hand is wide showing the palm or the back,
            // narrow on its edge, and the thumb swaps sides (right when palm up, for a right arm)
            val open = abs(sin(rad(v)))
            val half = 0.03f + 0.13f * open
            val wrist = P(0f, 0.12f)
            val top = wrist + P(0f, -0.34f)
            val side = if (v >= 0f) 1f else -1f
            val bones = mutableListOf(
                Bone(P(0f, 1.05f), wrist, false, width = 0.11f),
                Bone(wrist, top, true, width = half)
            )
            // Four fingers, spread across the visible width of the hand
            listOf(-0.75f, -0.25f, 0.25f, 0.75f).forEach { f ->
                val x = f * half
                bones += Bone(P(x, top.y), P(x, top.y - 0.26f), true, width = 0.02f + 0.018f * open)
            }
            bones += if (open < 0.25f) {
                // Hand on its edge: the thumb points at us, a short stub on top
                Bone(P(0f, wrist.y - 0.08f), P(0f, wrist.y - 0.16f), true, width = 0.05f)
            } else {
                Bone(P(side * half, wrist.y - 0.06f), P(side * (half + 0.17f), wrist.y - 0.22f), true, width = 0.035f)
            }
            Figure(
                bones = bones,
                head = NO_HEAD, headRadius = 0f, zoom = CLOSE_UP_ZOOM, focus = P(0f, 0.1f),
                label = when {
                    v > 30f -> PoseLabel.PALM_UP
                    v < -30f -> PoseLabel.PALM_DOWN
                    else -> PoseLabel.THUMB_UP
                }
            )
        }
        else -> {
            // Ankle from the side: shin fixed, foot pivoting; the floor shows where flat is
            val ankle = P(-0.15f, 0.2f)
            val footDir = dir(90f - v)
            val down = dir(-v)
            val sole = ankle + down * 0.12f
            Figure(
                bones = listOf(
                    Bone(P(-1f, 0.43f), P(1f, 0.43f), false, ghost = true, width = 0.015f),
                    Bone(P(-0.15f, -1.1f), ankle, false, width = 0.1f),
                    Bone(ankle, sole, true, width = 0.09f),
                    Bone(sole + footDir * -0.2f, sole + footDir * 0.62f, true, width = 0.085f)
                ),
                head = NO_HEAD, headRadius = 0f, zoom = CLOSE_UP_ZOOM
            )
        }
    }
}
