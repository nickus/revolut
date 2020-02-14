package com.revolut.api

import com.revolut.api.exceptions.WrongArgsException
import com.revolut.business.AccountService
import com.revolut.dto.DepositRequest
import com.revolut.dto.Money
import com.revolut.dto.TransferRequest
import com.revolut.dto.WithdrawRequest
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import javax.inject.Inject


class Api @Inject constructor(
    private val service: AccountService
) {
    val key = "Idempotency-Key"

    fun registerApi(netty: Application) {
        netty.routing {
            get("/accounts/{account_id}") {
                val accountId = call.parameters["account_id"]?.toLongOrNull() ?: throw WrongArgsException("account_id")
                call.respond(service.getAccount(accountId))
            }
            post("/accounts/{account_id}/deposit") {
                val accountId = call.parameters["account_id"]?.toLongOrNull() ?: throw WrongArgsException("account_id")
                val amount = call.receive<Money>()
                val idempotencyKey = call.request.headers[key] ?: throw WrongArgsException(key)
                service.deposit(DepositRequest(accountId, amount, idempotencyKey))
                call.respond(HttpStatusCode.OK)
            }
            post("/accounts/{account_id}/withdrawal") {
                val accountId = call.parameters["account_id"]?.toLongOrNull() ?: throw WrongArgsException("account_id")
                val amount = call.receive<Money>()
                val idempotencyKey = call.request.headers[key] ?: throw WrongArgsException(key)
                service.withdraw(WithdrawRequest(accountId, amount, idempotencyKey))
                call.respond(HttpStatusCode.OK)
            }
            post("/accounts/{from_account_id}/transfer/{to_account_id}") {
                val fromAccountId = call.parameters["from_account_id"]?.toLongOrNull()
                    ?: throw WrongArgsException("from_account_id")
                val toAccountId = call.parameters["to_account_id"]?.toLongOrNull()
                    ?: throw WrongArgsException("to_account_id")

                val amount = call.receive<Money>()
                val idempotencyKey = call.request.headers[key] ?: throw WrongArgsException(key)
                service.transfer(TransferRequest(fromAccountId, toAccountId, amount, idempotencyKey))
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}


