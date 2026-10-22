package ru.posttrack.pricing

import ru.posttrack.shared.QuoteId

interface DeliveryQuoteRepository {
    fun findById(id: QuoteId): DeliveryQuote?

    fun save(quote: DeliveryQuote)
}
