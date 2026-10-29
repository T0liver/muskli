package hu.toliver.muskli.domain.calc

const val LB_PER_KG = 2.20462262

fun kgToLb(kg: Double): Double = kg * LB_PER_KG

fun lbToKg(lb: Double): Double = lb / LB_PER_KG
