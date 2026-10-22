package ru.posttrack.app

import ru.posttrack.app.application.AccessPolicy
import ru.posttrack.app.application.PricingApplicationService
import ru.posttrack.app.application.ShipmentApplicationService
import ru.posttrack.app.application.TrackingApplicationService
import ru.posttrack.app.application.WarehouseApplicationService
import ru.posttrack.postgres.PostgresDatabase
import ru.posttrack.postgres.PostgresDeliveryQuoteRepository
import ru.posttrack.postgres.PostgresPickupCodeRepository
import ru.posttrack.postgres.PostgresShipmentRepository
import ru.posttrack.postgres.PostgresStorageCellRepository
import ru.posttrack.postgres.PostgresTrackingEventRepository
import ru.posttrack.postgres.PostgresUserRepository
import ru.posttrack.pricing.DeliveryQuoteProvider
import ru.posttrack.shared.TrackingNumber
import ru.posttrack.shipment.TrackingNumberGenerator
import ru.posttrack.warehouse.PickupCodeGenerator
import ru.posttrack.warehouse.PickupCodeHasher
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.util.UUID

class AppComponents(
    database: PostgresDatabase,
    quoteProvider: DeliveryQuoteProvider,
    codeHasher: PickupCodeHasher,
    clock: Clock,
) {
    private val access = AccessPolicy(PostgresUserRepository(database))
    private val quotes = PostgresDeliveryQuoteRepository(database)
    private val shipmentRepository = PostgresShipmentRepository(database)
    private val events = PostgresTrackingEventRepository(database)
    private val random = SecureRandom()

    val pricing = PricingApplicationService(quotes, quoteProvider, database, access, clock, Duration.ofMinutes(30))
    val shipments =
        ShipmentApplicationService(
            shipmentRepository,
            quotes,
            events,
            TrackingNumberGenerator { TrackingNumber.of(UUID.randomUUID().toString().replace("-", "")) },
            database,
            access,
            clock,
        )
    val tracking = TrackingApplicationService(shipmentRepository, events, database, access)
    val warehouse =
        WarehouseApplicationService(
            shipmentRepository,
            PostgresStorageCellRepository(database),
            PostgresPickupCodeRepository(database),
            events,
            PickupCodeGenerator { random.nextInt(1_000_000).toString().padStart(6, '0') },
            codeHasher,
            database,
            access,
            clock,
            Duration.ofMinutes(15),
        )
}
