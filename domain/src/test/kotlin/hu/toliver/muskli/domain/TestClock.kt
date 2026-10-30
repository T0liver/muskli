package hu.toliver.muskli.domain

import hu.toliver.muskli.domain.repo.Clock

class TestClock(var now: Long = 0L, private val offsetS: Int = 0) : Clock {
    override fun nowEpochMs(): Long = now

    override fun utcOffsetS(epochMs: Long): Int = offsetS
}
