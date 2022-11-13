package services.gateway.entity

import java.time.Instant
import java.util.UUID

class Rental(
    val id: Int,
    val rentalUid: UUID,
    val username: String,
    val paymentUid: UUID,
    val carUid: UUID,
    val dateFrom: Instant,
    val dateTo: Instant,
    val status: String
)