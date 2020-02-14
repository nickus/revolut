package com.revolut

import com.google.common.annotations.VisibleForTesting
import com.google.inject.Guice
import com.google.inject.Injector
import com.revolut.infrastructure.Di
import io.ktor.server.engine.ApplicationEngine
import org.flywaydb.core.Flyway

class Application {
    companion object {
        @JvmStatic
        @VisibleForTesting
        fun run(): Injector {
            val injector = Guice.createInjector(Di)
            injector.getInstance(Flyway::class.java).migrate()
            injector.getInstance(ApplicationEngine::class.java).start()
            return injector
        }
    }
}

fun main() {
    Application.run()
}