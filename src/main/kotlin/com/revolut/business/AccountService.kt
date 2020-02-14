package com.revolut.business

import com.revolut.api.exceptions.WrongArgsException
import com.revolut.business.exceptions.NotFoundException
import com.revolut.dto.DepositRequest
import com.revolut.dto.TransferRequest
import com.revolut.dto.WithdrawRequest
import org.slf4j.LoggerFactory
import javax.inject.Inject

class AccountService @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transferRepository: TransferRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun getAccount(id: AccountId): Account {
        if (id <= 0) throw NotFoundException("Account $id not found")
        return accountRepository.getAccount(id)
    }

    suspend fun deposit(req: DepositRequest) {
        val (accountId, amount, idempotencyKey) = req
        if (!amount.isPositive) throw WrongArgsException("amount is not positive")
        if (accountId <= 0) throw NotFoundException("Account $accountId not found")

        log.info("Depositing $amount to account $accountId")
        transferRepository.transferFromCashBook(
            to = accountId,
            money = amount,
            idempotencyKey = idempotencyKey
        )
    }

    suspend fun withdraw(req: WithdrawRequest) {
        val (accountId, amount, idempotencyKey) = req
        if (!amount.isPositive) throw WrongArgsException("amount is not positive")
        if (accountId <= 0) throw NotFoundException("Account $accountId not found")

        log.info("Withdrawing $amount from account $accountId")
        transferRepository.transferToCashBook(
            from = accountId,
            money = amount,
            idempotencyKey = idempotencyKey
        )
    }

    suspend fun transfer(req: TransferRequest) {
        val (fromAccount, toAccount, amount, idempotencyKey) = req
        if (fromAccount <= 0) throw NotFoundException("Account $fromAccount not found")
        if (toAccount <= 0) throw NotFoundException("Account $toAccount not found")
        if (fromAccount == toAccount) throw WrongArgsException("Accounts are the same")
        if (!amount.isPositive) throw WrongArgsException("amount is not positive")

        log.info("Creating transfer from $fromAccount to $toAccount for $amount")
        transferRepository.transfer(
            fromAccount = fromAccount,
            toAccount = toAccount,
            money = amount,
            idempotencyKey = idempotencyKey
        )
    }
}