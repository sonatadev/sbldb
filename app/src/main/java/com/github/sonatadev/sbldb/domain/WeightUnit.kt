package com.github.sonatadev.sbldb.domain

import java.math.BigDecimal
import java.math.RoundingMode

/** [decimals]: kg keeps two for 1.25 kg plates, lb only needs one. */
enum class WeightUnit(val label: String, private val decimals: Int) {
    KG("kg", 2), LB("lb", 1);

    fun fromKg(kg: Double): Double = if (this == KG) kg else kg * LB_PER_KG

    fun toKg(value: Double): Double = if (this == KG) value else value / LB_PER_KG

    /** Formats a weight stored in kg in this unit, e.g. "82.5" or "176.4". */
    fun format(kg: Double): String =
        BigDecimal(fromKg(kg)).setScale(decimals, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

    /** Body measurements follow the weight unit: cm with kg, inches with lb. */
    val lengthLabel: String get() = if (this == KG) "cm" else "in"

    fun fromCm(cm: Double): Double = if (this == KG) cm else cm / CM_PER_INCH

    fun toCm(value: Double): Double = if (this == KG) value else value * CM_PER_INCH

    fun formatLength(cm: Double): String =
        BigDecimal(fromCm(cm)).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

    /** Whole number in this unit, for estimates like e1RM where decimals are false precision. */
    fun formatRounded(kg: Double): String = kotlin.math.round(fromKg(kg)).toLong().toString()

    companion object {
        const val LB_PER_KG = 2.2046226218
        const val CM_PER_INCH = 2.54
    }
}
