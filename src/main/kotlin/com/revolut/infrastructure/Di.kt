package com.revolut.infrastructure

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.revolut.api.Api
import com.revolut.api.exceptions.WrongArgsException
import com.revolut.business.exceptions.InsufficientFunds
import com.revolut.business.exceptions.NotFoundException
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.flywaydb.core.Flyway
import javax.inject.Named
import javax.inject.Singleton

object Di : AbstractModule() {
    @Provides
    @Singleton
    @Named("JDBC_URL")
    private fun provideDatabaseUrl(): String {
        return EmbeddedPostgres.start().getJdbcUrl("postgres", "postgres")
    }

    @Provides
    @Singleton
    private fun provideWebServer(api: Api): ApplicationEngine {
        return embeddedServer(Netty, 8080, module = {
            install(ContentNegotiation) {
                jackson {}
            }
            install(StatusPages) {
                exception<WrongArgsException> { cause ->
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to cause.message))
                }
                exception<NotFoundException> { cause ->
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to cause.message))
                }
                exception<InsufficientFunds> { cause ->
                    call.respond(HttpStatusCode.NotAcceptable, mapOf("error" to cause.message))
                }
                exception<JsonProcessingException> {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Could not parse request"))
                }
                exception<MissingKotlinParameterException> { cause ->
                    call.respond(
                        HttpStatusCode.BadRequest, mapOf(
                            "error" to "Could not parse request: Missing parameter ${cause.parameter.name}"
                        )
                    )
                }
            }
            api.registerApi(this)
        })
    }

    @Provides
    @Singleton
    private fun provideFlyway(@Named("JDBC_URL") jdbcUrl: String): Flyway {
        return Flyway.configure()
            .dataSource(jdbcUrl, "postgres", "")
            .load()
    }

    @Provides
    @Singleton
    private fun provideDatabase(@Named("JDBC_URL") jdbcUrl: String): Database {
        val config = HikariConfig().apply {
            setJdbcUrl(jdbcUrl)
            maximumPoolSize = 10
            username = "postgres"
        }
        return Database(HikariDataSource(config))
    }
}