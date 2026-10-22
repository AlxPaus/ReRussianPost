package ru.posttrack.app

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import ru.posttrack.app.http.apiRoutes
import ru.posttrack.app.http.configureApiErrors
import ru.posttrack.app.infrastructure.HmacPickupCodeHasher
import ru.posttrack.app.infrastructure.RussianPostQuoteProvider
import ru.posttrack.app.infrastructure.russianPostClient
import ru.posttrack.postgres.DatabaseConfig
import ru.posttrack.postgres.DatabaseHealth
import ru.posttrack.postgres.PostgresDatabase
import java.time.Clock

fun main() {
    val environment = System.getenv()
    val clock = Clock.systemUTC()
    val codeHasher = HmacPickupCodeHasher(requireNotNull(environment["PICKUP_CODE_SECRET"]) { "PICKUP_CODE_SECRET is required" })
    PostgresDatabase.connect(DatabaseConfig.fromEnvironment(environment)).use { database ->
        database.migrate()
        russianPostClient().use { client ->
            val provider = RussianPostQuoteProvider(client, environment["RUSSIAN_POST_TOKEN"], environment["RUSSIAN_POST_USER_KEY"], clock)
            val components = AppComponents(database, provider, codeHasher, clock)
            embeddedServer(
                factory = CIO,
                host = environment["APP_HOST"] ?: "0.0.0.0",
                port = environment["APP_PORT"]?.toIntOrNull() ?: 8080,
                module = {
                    postTrackModule(database, components)
                },
            ).start(wait = true)
        }
    }
}

fun Application.postTrackModule(
    databaseHealth: DatabaseHealth,
    components: AppComponents,
) {
    install(CallLogging) {
        level = Level.INFO
        filter { call -> !call.request.path().startsWith("/health") }
        format { call -> "${call.request.local.method.value} ${call.request.path()} ${call.response.status()?.value}" }
    }
    install(ContentNegotiation) {
        json(
            Json {
                explicitNulls = false
            },
        )
    }
    configureApiErrors()

    routing {
        apiRoutes(components)
        get("/health/live") {
            call.respond(HealthResponse(status = "UP"))
        }
        get("/health/ready") {
            if (withContext(Dispatchers.IO) { databaseHealth.isReady() }) {
                call.respond(HealthResponse(status = "UP"))
            } else {
                call.respond(HttpStatusCode.ServiceUnavailable, HealthResponse(status = "DOWN"))
            }
        }
    }
}

@Serializable
data class HealthResponse(
    val status: String,
)
