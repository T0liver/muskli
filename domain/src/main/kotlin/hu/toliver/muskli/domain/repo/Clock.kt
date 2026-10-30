package hu.toliver.muskli.domain.repo

/** Wall-clock time. Not `java.time.Clock`: minSdk 24 has no `java.time` without desugaring. */
fun interface Clock {
    fun nowEpochMs(): Long
}
