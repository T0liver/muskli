package hu.toliver.muskli.domain.repo

fun interface IdGenerator {
    fun newId(): String
}
