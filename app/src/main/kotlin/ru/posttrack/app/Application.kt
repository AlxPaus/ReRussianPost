package ru.posttrack.app

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import ru.posttrack.postgres.DatabaseConfig
import ru.posttrack.postgres.DatabaseHealth
import ru.posttrack.postgres.PostgresDatabase

fun main() {
    val environment = System.getenv()
    PostgresDatabase.connect(DatabaseConfig.fromEnvironment(environment)).use { database ->
        database.migrate()

        embeddedServer(
            factory = CIO,
            host = environment["APP_HOST"] ?: "0.0.0.0",
            port = environment["APP_PORT"]?.toIntOrNull() ?: 8080,
            module = {
                postTrackModule(database)
            },
        ).start(wait = true)
    }
}

fun Application.postTrackModule(databaseHealth: DatabaseHealth) {
    install(CallLogging) {
        level = Level.INFO
        filter { call -> !call.request.path().startsWith("/health") }
    }
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = false
                ignoreUnknownKeys = false
                explicitNulls = false
            },
        )
    }
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.UnprocessableEntity,
                ProblemResponse(
                    type = "https://posttrack.local/problems/invalid-value",
                    title = "Invalid value",
                    status = HttpStatusCode.UnprocessableEntity.value,
                    detail = cause.message ?: "A value is invalid",
                ),
            )
        }
    }

    routing {
        get("/health/live") {
            call.respond(HealthResponse(status = "UP"))
        }
        get("/health/ready") {
            if (databaseHealth.isReady()) {
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

@Serializable
data class ProblemResponse(
    val type: String,
    val title: String,
    val status: Int,
    val detail: String,
)
