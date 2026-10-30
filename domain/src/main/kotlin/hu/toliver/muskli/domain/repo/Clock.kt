package hu.toliver.muskli.domain.repo

/** Wall-clock time. Not `java.time.Clock`: minSdk 24 has no `java.time` without desugaring. */
interface Clock {
    fun nowEpochMs(): Long

    /** The device time zone's offset from UTC at [epochMs], in seconds. */
    fun utcOffsetS(epochMs: Long): Int
}
