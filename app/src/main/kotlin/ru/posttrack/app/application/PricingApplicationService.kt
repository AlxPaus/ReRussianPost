package ru.posttrack.app.application

import ru.posttrack.identity.UserRole
import ru.posttrack.pricing.DeliveryQuote
import ru.posttrack.pricing.DeliveryQuoteProvider
import ru.posttrack.pricing.DeliveryQuoteRepository
import ru.posttrack.pricing.QuoteRequest
import ru.posttrack.shared.QuoteId
import ru.posttrack.shared.TransactionManager
import ru.posttrack.shared.UserId
import java.time.Clock
import java.time.Duration

class PricingApplicationService(
    private val quotes: DeliveryQuoteRepository,
    private val provider: DeliveryQuoteProvider,
    private val transactions: TransactionManager,
    private val access: AccessPolicy,
    private val clock: Clock,
    private val quoteLifetime: Duration,
) {
    init {
        require(!quoteLifetime.isNegative && !quoteLifetime.isZero) { "Quote lifetime must be positive" }
    }

    suspend fun calculate(
        userId: UserId,
        request: QuoteRequest,
    ): DeliveryQuote {
        transactions.execute { access.requireRole(userId, UserRole.CLIENT) }
        val calculation = provider.calculate(request)
        return transactions.execute {
            access.requireRole(userId, UserRole.CLIENT)
            val now = clock.instant()
            val quote = DeliveryQuote.create(QuoteId.random(), userId, request, calculation, now, now.plus(quoteLifetime))
            quotes.save(quote)
            quote
        }
    }
}
