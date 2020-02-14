package com.revolut.dto

import com.revolut.business.AccountId

data class TransferRequest(
    val fromAccountId: AccountId,
    val toAccountId: AccountId,
    val amount: Money,
    val idempotencyKey: String
)