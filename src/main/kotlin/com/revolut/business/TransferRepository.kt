package com.revolut.business

import com.revolut.business.exceptions.InsufficientFunds
import com.revolut.business.exceptions.NotFoundException
import com.revolut.dto.Money
import com.revolut.infrastructure.Database
import com.revolut.infrastructure.IsolationLevel.READ_COMMITTED
import org.intellij.lang.annotations.Language
import java.math.BigDecimal
import java.sql.Connection
import javax.inject.Inject

class TransferRepository @Inject constructor(
    private val db: Database
) {
    suspend fun transferFromCashBook(to: AccountId, money: Money, idempotencyKey: String) =
        transfer(cashBookAccountId, to, money, idempotencyKey, TransactionType.DEPOSIT)

    suspend fun transferToCashBook(from: AccountId, money: Money, idempotencyKey: String) =
        transfer(from, cashBookAccountId, money, idempotencyKey, TransactionType.WITHDRAWAL)

    suspend fun transfer(
        fromAccount: AccountId,
        toAccount: AccountId,
        money: Money,
        idempotencyKey: String,
        type: TransactionType = TransactionType.TRANSFER
    ) {
        db.transaction(isolationLevel = READ_COMMITTED) {
            if (idempotencyKeyAlreadyExists(idempotencyKey)) return@transaction
            if (!accountExists(toAccount)) throw NotFoundException("Account $toAccount not found")

            //we do not have to block cash book account since its balance can be either negative and positive
            if (fromAccount != cashBookAccountId) {
                if (!blockAccount(fromAccount)) throw NotFoundException("Account $fromAccount not found")
                if (accountBalance(fromAccount) < money.amount) throw InsufficientFunds(fromAccount)
            }

            makeTransfer(fromAccount, toAccount, money.amount, idempotencyKey, type)
        }
    }

    private fun Connection.idempotencyKeyAlreadyExists(key: String): Boolean {
        @Language("SQL")
        val sql = "SELECT * FROM transaction WHERE idempotency_key=?"
        return prepareStatement(sql).apply {
            setString(1, key)
        }.executeQuery().next()
    }

    /**
     * Sets exclusive lock on account
     */
    private fun Connection.blockAccount(account: AccountId): Boolean {
        @Language("SQL")
        val sql = "SELECT * FROM account WHERE id=? FOR UPDATE"
        return prepareStatement(sql).apply {
            setLong(1, account)
        }.executeQuery().next()
    }

    private fun Connection.accountExists(account: AccountId): Boolean {
        @Language("SQL")
        val sql = "SELECT * FROM account WHERE id=?"
        return prepareStatement(sql).apply {
            setLong(1, account)
        }.executeQuery().next()
    }

    private fun Connection.accountBalance(account: AccountId): BigDecimal {
        @Language("SQL")
        val sql = """
            SELECT COALESCE(SUM(CASE WHEN type = 'debit' THEN -amount ELSE amount END), 0) balance
            FROM posting
            WHERE account_id=?
        """.trimIndent()

        val rs = prepareStatement(sql).apply {
            setLong(1, account)
        }.executeQuery()

        rs.next()
        return rs.getBigDecimal("balance")
    }

    private fun Connection.makeTransfer(
        from: AccountId,
        to: AccountId,
        amount: BigDecimal,
        idempotencyKey: String,
        type: TransactionType
    ) {
        @Language("SQL")
        val transactionIdSql = "SELECT nextval('transaction_id') id"

        @Language("SQL")
        val transaction = """
            INSERT INTO transaction(id, type, amount, from_account_id, to_account_id, idempotency_key)
            VALUES (?,?::transaction_type,?,?,?,?)
        """.trimMargin()

        @Language("SQL")
        val posting = """
            INSERT INTO posting(transaction_id, account_id, type, amount) 
            VALUES (?,?,?::posting_type,?),(?,?,?::posting_type,?)
        """.trimIndent()

        val transactionId = prepareStatement(transactionIdSql).executeQuery().also { it.next() }.getLong("id")

        prepareStatement(transaction).apply {
            setLong(1, transactionId)
            setString(2, type.sqlType)
            setBigDecimal(3, amount)
            setLong(4, from)
            setLong(5, to)
            setString(6, idempotencyKey)
        }.executeUpdate()

        prepareStatement(posting).apply {
            setLong(1, transactionId)
            setLong(2, from)
            setString(3, PostingType.DEBIT.sqlType)
            setBigDecimal(4, amount)

            setLong(5, transactionId)
            setLong(6, to)
            setString(7, PostingType.CREDIT.sqlType)
            setBigDecimal(8, amount)
        }.executeUpdate()
    }


    companion object {
        private const val cashBookAccountId = 0L
    }
}