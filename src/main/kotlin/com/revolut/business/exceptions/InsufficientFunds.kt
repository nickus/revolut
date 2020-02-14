package com.revolut.business.exceptions

import com.revolut.business.AccountId

class InsufficientFunds(accountId: AccountId) : RuntimeException("Insufficient funds on account $accountId") {
}