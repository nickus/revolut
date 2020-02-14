package com.revolut.infrastructure

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.withContext
import java.sql.Connection
import javax.sql.DataSource
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

class Database(private val dataSource: DataSource) {
    val poolSize = 10
    val dbPoolName = "db-thread-pool"
    val dbThreadPool = newFixedThreadPoolContext(nThreads = poolSize, name = dbPoolName)

    suspend fun <T> transaction(
        isolationLevel: IsolationLevel = IsolationLevel.READ_COMMITTED,
        block: suspend Connection.() -> T
    ): T {
        val transaction = coroutineContext[TransactionContext]
        return if (transaction == null) {
            newTransaction(isolationLevel, block)
        } else {
            transaction.connection.block()
        }
    }

    private suspend fun <T> newTransaction(
        isolationLevel: IsolationLevel = IsolationLevel.READ_COMMITTED,
        block: suspend Connection.() -> T
    ): T = coroutineScope {
        val connection = dataSource.connection
        try {
            connection.autoCommit = false
            connection.transactionIsolation = isolationLevel.value
            val transactionContext = TransactionContext(connection)
            val dbThreadPool = dbThreadPool + transactionContext
            val result = withContext(dbThreadPool) { connection.block() }
            connection.commit()
            result
        } catch (e: Throwable) {
            connection.rollback()
            throw e
        } finally {
            connection.close()
        }
    }
}

private class TransactionContext(val connection: Connection) : AbstractCoroutineContextElement(TransactionContext) {
    companion object Key : CoroutineContext.Key<TransactionContext>
}

enum class IsolationLevel(val value: Int) {
    READ_UNCOMMITTED(1),
    READ_COMMITTED(2),
    REPEATABLE_READ(4),
    SERIALIZABLE(8)
}