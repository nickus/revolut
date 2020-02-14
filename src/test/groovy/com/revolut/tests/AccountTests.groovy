package com.revolut.tests

import io.ktor.http.HttpStatusCode
import spock.guice.UseModules
import spock.lang.Specification

import javax.inject.Inject

@UseModules(TestModule)
class AccountTests extends Specification {
    @Inject
    Fixtures fixtures

    def "test deposit process"() {
        given: "an account"
        def account = fixtures.createAccount()
        def idempotencyKey = UUID.randomUUID().toString()

        when: "deposit to the nonexistent account"
        def result = fixtures.makeDeposit(account - 1, 1.0)

        then: "account not found"
        result == HttpStatusCode.NotFound

        when: "deposit to the real account"
        result = fixtures.makeDeposit(account, 1.0, idempotencyKey)

        then: "success"
        result == HttpStatusCode.OK

        and: "account balance is increased"
        fixtures.getAccount(account).balance == 1.0

        when: "we use the same idempotency key"
        result = fixtures.makeDeposit(account, 1.0, idempotencyKey)

        then: "it does not change account balance"
        result == HttpStatusCode.OK
        fixtures.getAccount(account).balance == 1.0

        and: "debit equal credit in database"
        fixtures.saldo == 0.0
    }

    def "test deposit negative cases"(){
        given: "an account"
        def account = fixtures.createAccount()

        when: "not acceptable precision"
        def result = fixtures.makeDeposit(account, 1.123456789)

        then: "bad request"
        result == HttpStatusCode.BadRequest

        when: "negative amount"
        result = fixtures.makeDeposit(account, -1.0)

        then: "bad request"
        result == HttpStatusCode.BadRequest
    }

    def "test withdraw process"() {
        given: "an account"
        def account = fixtures.createAccount()
        def idempotencyKey = UUID.randomUUID().toString()

        when: "withdraw from the nonexistent account"
        def result = fixtures.makeDeposit(account + 1, 1.0)

        then: "account not found"
        result == HttpStatusCode.NotFound

        when: "withdraw from the empty real account"
        result = fixtures.makeWithdrawal(account, 1.0)

        then: "insufficient funds"
        result == HttpStatusCode.NotAcceptable

        when: "withdraw more then available"
        fixtures.makeDeposit(account, 2.0)
        result = fixtures.makeWithdrawal(account, 3.0)

        then: "insufficient funds"
        result == HttpStatusCode.NotAcceptable

        when: "withdraw deposited amount"
        result = fixtures.makeWithdrawal(account, 1.0, idempotencyKey)

        then: "success"
        result == HttpStatusCode.OK
        fixtures.getAccount(account).balance == 1.0

        when: "we use the same idempotency key"
        result = fixtures.makeWithdrawal(account, 1.0, idempotencyKey)

        then: "it does not change account balance"
        result == HttpStatusCode.OK
        fixtures.getAccount(account).balance == 1.0

        and: "debit equal credit in database"
        fixtures.saldo == 0.0
    }

    def "test withdrawal negative cases"(){
        given: "an account"
        def account = fixtures.createAccount()

        when: "not acceptable precision"
        def result = fixtures.makeWithdrawal(account, 1.123456789)

        then: "bad request"
        result == HttpStatusCode.BadRequest

        when: "negative amount"
        result = fixtures.makeWithdrawal(account, -1.0)

        then: "bad request"
        result == HttpStatusCode.BadRequest
    }
}
