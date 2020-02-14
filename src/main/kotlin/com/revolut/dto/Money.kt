package com.revolut.dto

import com.revolut.api.exceptions.WrongArgsException
import java.math.BigDecimal
import java.math.RoundingMode

data class Money(
    val amount: BigDecimal
) {
    init {
        try {
            amount.setScale(precision, RoundingMode.UNNECESSARY)
        } catch (e: Exception) {
            throw WrongArgsException("amount max precision is $precision. Loss of accuracy")
        }
    }

    @Transient
    val isPositive = amount.signum() > 0

    companion object {
        const val precision = 2
    }
}