package com.revolut.tests

import io.ktor.http.HttpStatusCode
import spock.guice.UseModules
import spock.lang.Specification

import javax.inject.Inject

@UseModules(TestModule)
class TransfersTests extends Specification {
    @Inject
    Fixtures fixtures

    def "should have idempotent rest api"() {
        given: "accounts"
        def account1 = fixtures.createAccount()
        def account2 = fixtures.createAccount()
        fixtures.makeDeposit(account1, 2.0)
        fixtures.makeDeposit(account2, 2.0)
        def idempotencyKey = UUID.randomUUID().toString()

        when: "we make transfer"
        def result = fixtures.makeTransfer(account1, account2, 1.0, idempotencyKey)

        then: "it executes this transfer successfully"
        result == HttpStatusCode.OK
        fixtures.getAccount(account1).balance == 1.0
        fixtures.getAccount(account2).balance == 3.0

        when: "we use the same idempotency key"
        result = fixtures.makeTransfer(account1, account2, 1.0, idempotencyKey)

        then: "it does not change balances"
        result == HttpStatusCode.OK
        fixtures.getAccount(account1).balance == 1.0
        fixtures.getAccount(account2).balance == 3.0

        and: "debit equal credit in database"
        fixtures.saldo == 0.0
    }

    def "test validations and negative cases"() {
        given: "accounts"
        def account1 = fixtures.createAccount()
        def account2 = fixtures.createAccount()

        when: "transfer to the nonexistent account"
        def result = fixtures.makeTransfer(account1, account2 + 1, 1.0)

        then: "destination account not found"
        result == HttpStatusCode.NotFound

        when: "transfer from the nonexistent account"
        result = fixtures.makeTransfer(account2 + 1, account1, 1.0)

        then: "source account not found"
        result == HttpStatusCode.NotFound

        when: "transfer to the same account"
        result = fixtures.makeTransfer(account1, account1, 1.0)

        then: "bad request"
        result == HttpStatusCode.BadRequest

        when: "make transfer to the cash book"
        result = fixtures.makeTransfer(account1, 0, 1.0)

        then: "destination account not found"
        result == HttpStatusCode.NotFound

        when: "make transfer from the cash book"
        result = fixtures.makeTransfer(0, account2, 1.0)

        then: "source account not found"
        result == HttpStatusCode.NotFound

        when: "transfer negative amount"
        result = fixtures.makeTransfer(account1, account2, -1.0)

        then: "bad request"
        result == HttpStatusCode.BadRequest

        when: "transfer more than available"
        result = fixtures.makeTransfer(account1, account2, 1.0)

        then: "insufficient funds"
        result == HttpStatusCode.NotAcceptable
    }

    def "test multiple accounts"() {
        given: "many accounts"
        def accounts = new IntRange(1, 10).collect { fixtures.createAccount() }
        accounts.forEach { fixtures.makeDeposit(it, 1000000.0) }

        when: "make many transfers concurrently"
        def result = fixtures.makeTransfersAsync(accounts, 0.1)

        then: "all of them are executed successfully"
        result.every { it == HttpStatusCode.OK }

        and: "debit equal credit in database"
        fixtures.saldo == 0.0
    }

    def "test concurrent transfers from many accounts to one single account"() {
        given: "many accounts"
        def accounts = new IntRange(1, 1000).collect { fixtures.createAccount() }
        accounts.forEach { fixtures.makeDeposit(it, 1000000.0) }

        when: "make many-to-one transfers"
        def result = fixtures.makeTransfersAsync(accounts, 0.1, false)

        then: "all of them are executed successfully"
        result.every { it == HttpStatusCode.OK }
        fixtures.getAccount(accounts[0]).balance == 1000000.0 + (accounts.size() - 1) * 0.1

        and: "debit equal credit in database"
        fixtures.saldo == 0.0
    }

    def "test concurrent transfers from one account to many"() {
        given: "many accounts"
        def accounts = new IntRange(1, 1000).collect { fixtures.createAccount() }
        accounts.forEach { fixtures.makeDeposit(it, 1000000.0) }

        when: "make one-to-many transfers"
        def result = fixtures.makeTransfersAsync(accounts, 0.1, true)

        then: "all of them are executed successfully"
        result.every { it == HttpStatusCode.OK }
        fixtures.getAccount(accounts[0]).balance == 1000000.0 - (accounts.size() - 1) * 0.1

        and: "debit equal credit in database"
        fixtures.saldo == 0.0
    }
}
