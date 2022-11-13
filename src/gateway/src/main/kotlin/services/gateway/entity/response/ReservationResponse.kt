package services.gateway.entity.response

import services.gateway.entity.Payment
import java.time.Instant
import java.util.*

class ReservationResponse(
    val rentalUid: UUID,
    val status: String,
    val carUid: UUID,
    val dateFrom: String,
    val dateTo: String,
    val payment: Payment
)