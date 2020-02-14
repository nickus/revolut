package com.revolut.tests

import com.revolut.business.Account
import com.revolut.business.AccountId
import com.revolut.infrastructure.Database
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.DEFAULT
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.atomic.AtomicLong

class Fixtures(val db: Database) {
    val baseUrl = "http://localhost:8080"
    val idempotency = "Idempotency-Key"
    val accountIdSeq = AtomicLong(System.currentTimeMillis())
    val client = HttpClient() {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
        install(JsonFeature) {
            serializer = JacksonSerializer()
        }
    }

    fun createAccount(): Long {
        @Language("SQL")
        val sql = "INSERT INTO account(id, name) VALUES (?,?)"
        val accountId = accountIdSeq.getAndIncrement()
        runBlocking {
            db.transaction {
                prepareStatement(sql).apply {
                    setLong(1, accountId)
                    setString(2, UUID.randomUUID().toString())
                }.executeUpdate()
            }
        }
        return accountId
    }

    fun getSaldo(): BigDecimal {
        @Language("SQL")
        val sql = "SELECT COALESCE(SUM(CASE WHEN type = 'debit' THEN -amount ELSE amount END), 0) saldo FROM posting"
        return runBlocking {
            db.transaction {
                val rs = prepareStatement(sql).executeQuery()
                rs.next()
                rs.getBigDecimal("saldo")
            }
        }
    }

    fun getAccount(id: AccountId): Account {
        return runBlocking {
            client.request<Account>("$baseUrl/accounts/$id")
        }
    }

    @JvmOverloads
    fun makeDeposit(
        accountId: AccountId,
        amount: BigDecimal,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): HttpStatusCode {
        return runBlocking {
            client.post<HttpResponse>("$baseUrl/accounts/$accountId/deposit") {
                contentType(ContentType.Application.Json)
                headers[idempotency] = idempotencyKey
                body = mapOf("amount" to amount)
            }.status
        }
    }

    @JvmOverloads
    fun makeWithdrawal(
        accountId: AccountId,
        amount: BigDecimal,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): HttpStatusCode {
        return runBlocking {
            client.post<HttpResponse>("$baseUrl/accounts/$accountId/withdrawal") {
                contentType(ContentType.Application.Json)
                headers[idempotency] = idempotencyKey
                body = mapOf("amount" to amount)
            }.status
        }
    }

    @JvmOverloads
    fun makeTransfer(
        fromAccountId: AccountId,
        toAccountId: AccountId,
        amount: BigDecimal,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): HttpStatusCode {
        return runBlocking {
            client.post<HttpResponse>("$baseUrl/accounts/$fromAccountId/transfer/$toAccountId") {
                contentType(ContentType.Application.Json)
                headers[idempotency] = idempotencyKey
                body = mapOf("amount" to amount)
            }.status
        }
    }

    fun makeTransfersAsync(
        accounts: List<AccountId>,
        amount: BigDecimal
    ): List<HttpStatusCode> {
        return runBlocking {
            accounts.mapIndexed { index, _ ->
                async {
                    val fromAccountId = accounts[index]
                    val toAccountId = accounts[(index + 1) % accounts.size]
                    client.post<HttpResponse>("$baseUrl/accounts/$fromAccountId/transfer/$toAccountId") {
                        contentType(ContentType.Application.Json)
                        headers[idempotency] = UUID.randomUUID().toString()
                        body = mapOf("amount" to amount)
                    }.status
                }
            }.map { it.await() }
        }
    }

    fun makeTransfersAsync(
        accounts: List<AccountId>,
        amount: BigDecimal,
        oneToMany: Boolean = true
    ): List<HttpStatusCode> {
        val account1 = accounts[0]
        val restAccounts = accounts.subList(1, accounts.size)
        return runBlocking {
            restAccounts.chunked(10).flatMap {
                it.map { account2 ->
                    async {
                        val url = if (oneToMany) {
                            "$baseUrl/accounts/$account1/transfer/$account2"
                        } else {
                            "$baseUrl/accounts/$account2/transfer/$account1"
                        }
                        client.post<HttpResponse>(url) {
                            contentType(ContentType.Application.Json)
                            headers[idempotency] = UUID.randomUUID().toString()
                            body = mapOf("amount" to amount)
                        }.status
                    }
                }.map { it.await() }
            }
        }
    }
}