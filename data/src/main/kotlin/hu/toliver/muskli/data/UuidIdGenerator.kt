package hu.toliver.muskli.data

import hu.toliver.muskli.domain.repo.IdGenerator
import java.util.UUID

class UuidIdGenerator : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}
