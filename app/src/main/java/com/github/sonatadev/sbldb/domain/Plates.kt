package com.github.sonatadev.sbldb.domain

/** Plates to load on each side of a barbell for a total weight, in the user's unit. */
object Plates {
    private val KG_PLATES = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
    private val LB_PLATES = listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5)

    fun bar(unit: WeightUnit): Double = if (unit == WeightUnit.KG) 20.0 else 45.0

    /**
     * Greedy split, heaviest plates first. Returns null when the load is not above the bar or cannot
     * be made exactly with standard plates (then a list would be misleading).
     */
    fun perSide(totalKg: Double, unit: WeightUnit): List<Double>? {
        val total = unit.fromKg(totalKg)
        var side = (total - bar(unit)) / 2
        if (side <= 1e-6) return null
        val plates = mutableListOf<Double>()
        for (plate in if (unit == WeightUnit.KG) KG_PLATES else LB_PLATES) {
            while (side >= plate - 1e-6) {
                plates += plate
                side -= plate
            }
        }
        return plates.takeIf { side < 0.05 }
    }
}
