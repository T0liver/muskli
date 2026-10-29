package hu.toliver.muskli.domain.calc

enum class OneRmFormula { EPLEY, BRZYCKI }

/** Returns null for inputs with no meaningful estimate instead of throwing. */
fun estimateOneRepMax(weightKg: Double, reps: Int, formula: OneRmFormula): Double? {
    if (weightKg <= 0.0 || reps < 1) return null
    if (reps == 1) return weightKg
    return when (formula) {
        OneRmFormula.EPLEY -> weightKg * (1 + reps / 30.0)
        // Brzycki divides by zero at 37 reps and goes negative beyond.
        OneRmFormula.BRZYCKI -> if (reps >= 37) null else weightKg * 36.0 / (37 - reps)
    }
}
