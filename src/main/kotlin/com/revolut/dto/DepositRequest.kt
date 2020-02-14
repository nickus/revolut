package com.revolut.dto

data class DepositRequest(
    val accountId: Long,
    val amount: Money,
    val idempotencyKey: String
)