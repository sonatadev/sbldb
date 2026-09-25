package com.github.sonatadev.sbldb.domain

import com.github.sonatadev.sbldb.data.entity.Role
import com.github.sonatadev.sbldb.data.entity.VolumeRow
import java.time.Instant
import java.time.ZoneId

enum class VolumeBand { LOW, OPTIMAL, HIGH }

data class RegionVolume(val region: String, val sets: Double)

data class MuscleGroupVolume(
    val muscleGroup: String,
    val sets: Double,
    val frequency: Int,
    val regions: List<RegionVolume>
) {
    val band: VolumeBand
        get() = when {
            sets < VolumeCalculator.OPTIMAL_MIN_SETS -> VolumeBand.LOW
            sets <= VolumeCalculator.OPTIMAL_MAX_SETS -> VolumeBand.OPTIMAL
            else -> VolumeBand.HIGH
        }
}

/**
 * Weekly fractional volume per muscle:
 * - only hard sets count (completed, not warm-up, RIR ≤ [MAX_HARD_SET_RIR] or not recorded);
 * - a set counts 1 for a PRIMARY muscle and [SECONDARY_WEIGHT] for a SECONDARY one;
 * - at group level each set counts once per group, with the highest role the exercise has on it,
 *   so an exercise listing three triceps heads still adds a single set to "Triceps";
 * - at region level each region is counted on its own; rows without a region only count for the group.
 */
object VolumeCalculator {
    const val PRIMARY_WEIGHT = 1.0
    const val SECONDARY_WEIGHT = 0.5
    const val MAX_HARD_SET_RIR = 4
    const val OPTIMAL_MIN_SETS = 10.0
    const val OPTIMAL_MAX_SETS = 20.0

    fun isHardSet(rir: Int?): Boolean = rir == null || rir <= MAX_HARD_SET_RIR

    fun weight(role: Role): Double = if (role == Role.PRIMARY) PRIMARY_WEIGHT else SECONDARY_WEIGHT

    /**
     * @param allGroups groups to always include, so untrained muscles show up with 0 sets.
     * @return one entry per group, sorted by descending volume and then by name.
     */
    fun calculate(
        rows: List<VolumeRow>,
        allGroups: List<String> = emptyList(),
        zone: ZoneId = ZoneId.systemDefault()
    ): List<MuscleGroupVolume> {
        val hard = rows.filter { isHardSet(it.rir) }

        val groupSets = hard
            .groupBy { it.setId to it.muscleGroup }
            .map { (key, hits) -> key.second to hits.maxOf { weight(it.role) } }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.sum() }

        val regionSets = hard
            .filter { it.muscleRegion != null }
            .groupBy { Triple(it.setId, it.muscleGroup, it.muscleRegion!!) }
            .map { (key, hits) -> (key.second to key.third) to hits.maxOf { weight(it.role) } }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.sum() }

        val frequency = hard
            .groupBy { it.muscleGroup }
            .mapValues { (_, hits) ->
                hits.map { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }.distinct().size
            }

        return (allGroups + groupSets.keys).distinct()
            .map { group ->
                MuscleGroupVolume(
                    muscleGroup = group,
                    sets = groupSets[group] ?: 0.0,
                    frequency = frequency[group] ?: 0,
                    regions = regionSets
                        .filterKeys { it.first == group }
                        .map { (key, sets) -> RegionVolume(key.second, sets) }
                        .sortedByDescending { it.sets }
                )
            }
            .sortedWith(compareByDescending<MuscleGroupVolume> { it.sets }.thenBy { it.muscleGroup })
    }
}
