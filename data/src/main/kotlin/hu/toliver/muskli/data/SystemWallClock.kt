package hu.toliver.muskli.data

import hu.toliver.muskli.domain.repo.Clock

class SystemWallClock : Clock {
    override fun nowEpochMs(): Long = System.currentTimeMillis()
}
