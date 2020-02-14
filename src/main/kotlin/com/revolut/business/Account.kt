package com.revolut.business

import java.math.BigDecimal

data class Account(
    val id: Long,
    val name: String,
    val balance: BigDecimal
)