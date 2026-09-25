package com.github.sonatadev.sbldb.domain

/** Camera for a joint-action figure. */
enum class FigureView { SIDE, FRONT, TOP }

/**
 * The degree of freedom an animation moves, the views that can show it and the angles that make
 * sense for it (degrees; for scapula moves the number is a small shift rather than an angle).
 */
enum class Dof(val key: String, val views: Set<FigureView>, val min: Float, val max: Float) {
    SHOULDER_FLEX("shoulder_flex", setOf(FigureView.SIDE), -60f, 180f),
    SHOULDER_ABD("shoulder_abd", setOf(FigureView.FRONT), -20f, 180f),
    SHOULDER_HZ("shoulder_hz", setOf(FigureView.TOP), -45f, 135f),
    SHOULDER_ROT("shoulder_rot", setOf(FigureView.TOP), -100f, 100f),
    SCAP_ELEV("scap_elev", setOf(FigureView.FRONT), -30f, 40f),
    SCAP_PROTRACT("scap_protract", setOf(FigureView.TOP), -40f, 40f),
    SCAP_ROT("scap_rot", setOf(FigureView.FRONT), 0f, 60f),
    ELBOW_FLEX("elbow_flex", setOf(FigureView.SIDE), 0f, 150f),
    FOREARM_ROT("forearm_rot", setOf(FigureView.FRONT), -90f, 90f),
    WRIST_FLEX("wrist_flex", setOf(FigureView.SIDE), -80f, 90f),
    SPINE_FLEX("spine_flex", setOf(FigureView.SIDE), -30f, 90f),
    SPINE_LAT("spine_lat", setOf(FigureView.FRONT), -45f, 45f),
    SPINE_ROT("spine_rot", setOf(FigureView.TOP), -60f, 60f),
    HIP_FLEX("hip_flex", setOf(FigureView.SIDE), -30f, 130f),
    HIP_ABD("hip_abd", setOf(FigureView.FRONT), -20f, 60f),
    HIP_ROT("hip_rot", setOf(FigureView.FRONT), -60f, 60f),
    KNEE_FLEX("knee_flex", setOf(FigureView.SIDE), 0f, 150f),
    ANKLE_FLEX("ankle_flex", setOf(FigureView.SIDE), -40f, 60f);

    companion object {
        fun of(key: String): Dof? = entries.firstOrNull { it.key == key }
    }
}

/** How a joint action is animated: [dof] swings from [from] to [to] and back, seen from [view]. */
data class ActionAnimation(val view: FigureView, val dof: Dof, val from: Float, val to: Float) {
    /** Stored in the database as "side|shoulder_flex|0|170". */
    fun encode(): String = "${view.name.lowercase()}|${dof.key}|${fmt(from)}|${fmt(to)}"

    /** Why this animation cannot be shown, or null when it is fine. */
    fun problem(): String? = when {
        view !in dof.views -> "${dof.key} cannot be shown from the ${view.name.lowercase()}"
        from !in dof.min..dof.max || to !in dof.min..dof.max -> "${dof.key} range must stay within ${fmt(dof.min)}..${fmt(dof.max)}"
        from == to -> "${dof.key} does not move"
        else -> null
    }

    companion object {
        fun decode(text: String?): ActionAnimation? {
            val parts = text?.split('|') ?: return null
            if (parts.size != 4) return null
            val view = FigureView.entries.firstOrNull { it.name.equals(parts[0], ignoreCase = true) } ?: return null
            val dof = Dof.of(parts[1]) ?: return null
            val from = parts[2].toFloatOrNull() ?: return null
            val to = parts[3].toFloatOrNull() ?: return null
            return ActionAnimation(view, dof, from, to)
        }

        /** From the YAML map {view, dof, from, to}; throws with a readable message when malformed. */
        fun parse(map: Map<*, *>, owner: String): ActionAnimation {
            val view = (map["view"] as? String)?.let { v -> FigureView.entries.firstOrNull { it.name.equals(v, ignoreCase = true) } }
                ?: throw IllegalArgumentException("$owner: animation view must be side, front or top")
            val dof = (map["dof"] as? String)?.let(Dof::of) ?: throw IllegalArgumentException("$owner: unknown animation dof ${map["dof"]}")
            val from = (map["from"] as? Number)?.toFloat() ?: throw IllegalArgumentException("$owner: animation needs a numeric from")
            val to = (map["to"] as? Number)?.toFloat() ?: throw IllegalArgumentException("$owner: animation needs a numeric to")
            return ActionAnimation(view, dof, from, to)
        }

        private fun fmt(v: Float): String = if (v == v.toLong().toFloat()) v.toLong().toString() else v.toString()
    }
}
