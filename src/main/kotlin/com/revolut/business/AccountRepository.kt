package com.revolut.business

import com.revolut.business.exceptions.NotFoundException
import com.revolut.infrastructure.Database
import org.intellij.lang.annotations.Language
import javax.inject.Inject

class AccountRepository @Inject constructor(
    private val db: Database
) {
    suspend fun getAccount(id: Long): Account {
        @Language("SQL")
        val sql = """
            SELECT 
                id,
                name,
                (
                    SELECT COALESCE(SUM(CASE WHEN type = 'debit' THEN -amount ELSE amount END), 0) 
                    FROM posting 
                    WHERE account_id=a.id
                ) balance
            FROM account a
            WHERE id=?
        """.trimIndent()

        return db.transaction {
            val rs = prepareStatement(sql).apply {
                setLong(1, id)
            }.executeQuery()

            if (!rs.next()) throw NotFoundException("Account $id not found")
            Account(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getBigDecimal("balance")
            )
        }
    }
}