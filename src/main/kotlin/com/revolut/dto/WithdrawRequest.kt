package com.revolut.dto

data class WithdrawRequest(
    val accountId: Long,
    val amount: Money,
    val idempotencyKey: String
)