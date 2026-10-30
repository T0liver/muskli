package hu.toliver.muskli.data

import hu.toliver.muskli.domain.repo.Clock
import java.util.TimeZone

class SystemWallClock : Clock {
    override fun nowEpochMs(): Long = System.currentTimeMillis()

    override fun utcOffsetS(epochMs: Long): Int = TimeZone.getDefault().getOffset(epochMs) / 1_000
}
