package ru.posttrack.app.http

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.UnsupportedMediaTypeException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import ru.posttrack.app.application.AccessDenied
import ru.posttrack.app.application.EntityNotFound
import ru.posttrack.app.infrastructure.QuoteProviderFailure
import ru.posttrack.app.infrastructure.QuoteProviderUnavailable
import java.sql.SQLException
import java.time.format.DateTimeParseException

fun Application.configureApiErrors() {
    val logger = log
    install(StatusPages) {
        exception<BadRequestException> { call, _ ->
            call.problem(HttpStatusCode.BadRequest, "Invalid request body or missing required parameter")
        }
        exception<UnsupportedMediaTypeException> { call, _ ->
            call.problem(HttpStatusCode.UnsupportedMediaType, "Content-Type must be application/json")
        }
        exception<IllegalArgumentException> { call, cause ->
            call.problem(HttpStatusCode.UnprocessableEntity, cause.message ?: "Invalid value")
        }
        exception<DateTimeParseException> { call, _ ->
            call.problem(HttpStatusCode.UnprocessableEntity, "Date must use YYYY-MM-DD format")
        }
        exception<EntityNotFound> { call, cause ->
            call.problem(HttpStatusCode.NotFound, cause.message!!)
        }
        exception<AccessDenied> { call, _ ->
            call.problem(HttpStatusCode.Forbidden, "Access denied")
        }
        exception<QuoteProviderUnavailable> { call, cause ->
            call.problem(HttpStatusCode.ServiceUnavailable, cause.message!!)
        }
        exception<QuoteProviderFailure> { call, cause ->
            logger.warn("Delivery quote provider failed", cause)
            call.problem(HttpStatusCode.BadGateway, cause.message!!)
        }
        exception<SQLException> { call, cause ->
            if (cause.sqlState == "23505") {
                call.problem(HttpStatusCode.Conflict, "An entry with these identifiers already exists")
            } else {
                logger.error("Database operation failed", cause)
                call.problem(HttpStatusCode.InternalServerError, "Internal server error")
            }
        }
        exception<Throwable> { call, cause ->
            if (cause is CancellationException) throw cause
            logger.error("Request failed", cause)
            call.problem(HttpStatusCode.InternalServerError, "Internal server error")
        }
        status(HttpStatusCode.NotFound, HttpStatusCode.MethodNotAllowed) { call, status ->
            call.problem(status, status.description)
        }
    }
}

private suspend fun ApplicationCall.problem(
    status: HttpStatusCode,
    detail: String,
) {
    respond(status, ProblemResponse(status.value, status.description, detail))
}

@Serializable
data class ProblemResponse(
    val status: Int,
    val title: String,
    val detail: String,
)
