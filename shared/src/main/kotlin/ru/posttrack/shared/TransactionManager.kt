package ru.posttrack.shared

interface TransactionManager {
    fun <T> execute(block: () -> T): T
}
