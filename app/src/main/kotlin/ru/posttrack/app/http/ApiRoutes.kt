package ru.posttrack.app.http

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.posttrack.app.AppComponents
import ru.posttrack.shared.PhoneNumber
import ru.posttrack.shared.PostalCode
import ru.posttrack.shared.ShipmentId
import ru.posttrack.shared.StorageCellId
import ru.posttrack.shared.TrackingNumber
import ru.posttrack.shared.UserId
import ru.posttrack.shared.WeightGrams
import java.time.LocalDate
import java.util.UUID

fun Route.apiRoutes(services: AppComponents) {
    route("/api/v1") {
        post("/quotes") {
            val userId = call.userId()
            val request = call.receive<CalculateQuoteRequest>().toDomain()
            val quote = withContext(Dispatchers.IO) { services.pricing.calculate(userId, request) }
            call.respond(HttpStatusCode.Created, quote.toResponse())
        }
        route("/shipments") {
            post {
                val userId = call.userId()
                val command = call.receive<CreateShipmentRequest>().toCommand()
                val shipment = withContext(Dispatchers.IO) { services.shipments.create(userId, command) }
                call.respond(HttpStatusCode.Created, shipment.toResponse())
            }
            get {
                val userId = call.userId()
                val phone = PhoneNumber.of(call.request.queryParameters["phone"] ?: throw BadRequestException("phone is required"))
                val shipments = withContext(Dispatchers.IO) { services.tracking.findByRecipientPhone(userId, phone) }
                call.respond(shipments.map { it.toResponse() })
            }
            route("/{id}") {
                post("/accept") {
                    val userId = call.userId()
                    val id = call.shipmentId()
                    val request = call.receive<AcceptShipmentRequest>()
                    val weight = WeightGrams(request.weightGrams)
                    val shipment = withContext(Dispatchers.IO) { services.shipments.accept(userId, id, weight, request.location) }
                    call.respond(shipment.toResponse())
                }
                post("/send") {
                    val userId = call.userId()
                    val id = call.shipmentId()
                    val request = call.receive<SendShipmentRequest>()
                    val shipment = withContext(Dispatchers.IO) { services.shipments.send(userId, id, request.location) }
                    call.respond(shipment.toResponse())
                }
                patch("/expected-delivery") {
                    val userId = call.userId()
                    val id = call.shipmentId()
                    val request = call.receive<ChangeDeliveryRequest>()
                    val date = LocalDate.parse(request.date)
                    val shipment =
                        withContext(Dispatchers.IO) {
                            services.shipments.changeExpectedDelivery(userId, id, date, request.location)
                        }
                    call.respond(shipment.toResponse())
                }
                get("/storage-cell") {
                    val userId = call.userId()
                    val id = call.shipmentId()
                    val cell = withContext(Dispatchers.IO) { services.warehouse.findCell(userId, id) }
                    call.respond(cell.toResponse())
                }
                put("/storage-cell") {
                    val userId = call.userId()
                    val id = call.shipmentId()
                    val cellId = StorageCellId(parseUuid(call.receive<PlaceShipmentRequest>().cellId))
                    val shipment = withContext(Dispatchers.IO) { services.warehouse.place(userId, id, cellId) }
                    call.respond(shipment.toResponse())
                }
                post("/pickup-code") {
                    val userId = call.userId()
                    val id = call.shipmentId()
                    val code = withContext(Dispatchers.IO) { services.warehouse.issuePickupCode(userId, id) }
                    call.respond(HttpStatusCode.Created, PickupCodeDto(code))
                }
                post("/deliver") {
                    val userId = call.userId()
                    val id = call.shipmentId()
                    val request = call.receive<PickupCodeDto>()
                    val delivered = withContext(Dispatchers.IO) { services.warehouse.deliver(userId, id, request.code) }
                    call.respond(
                        if (delivered) HttpStatusCode.OK else HttpStatusCode.UnprocessableEntity,
                        DeliveryResponse(delivered),
                    )
                }
            }
        }
        route("/tracking/{trackingNumber}") {
            get {
                val userId = call.userId()
                val number = TrackingNumber.of(call.parameters["trackingNumber"]!!)
                val shipment = withContext(Dispatchers.IO) { services.tracking.findShipment(userId, number) }
                call.respond(shipment.toResponse())
            }
            get("/events") {
                val userId = call.userId()
                val number = TrackingNumber.of(call.parameters["trackingNumber"]!!)
                val events = withContext(Dispatchers.IO) { services.tracking.history(userId, number) }
                call.respond(events.map { it.toResponse() })
            }
        }
        post("/storage-cells") {
            val userId = call.userId()
            val request = call.receive<CreateCellRequest>()
            val postOffice = PostalCode(request.postOffice)
            val cell =
                withContext(Dispatchers.IO) {
                    services.warehouse.createCell(userId, postOffice, request.rack, request.shelf, request.cell)
                }
            call.respond(HttpStatusCode.Created, cell.toResponse())
        }
    }
}

private fun ApplicationCall.userId(): UserId =
    UserId(parseUuid(request.headers["X-User-Id"] ?: throw BadRequestException("X-User-Id is required")))

private fun ApplicationCall.shipmentId(): ShipmentId = ShipmentId(parseUuid(parameters["id"]!!))

internal fun parseUuid(value: String): UUID {
    val uuid = UUID.fromString(value)
    require(uuid.toString().equals(value, ignoreCase = true)) { "Invalid UUID" }
    return uuid
}
