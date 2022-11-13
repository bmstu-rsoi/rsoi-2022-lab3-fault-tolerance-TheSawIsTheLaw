package services.gateway.entity

import java.time.Instant
import java.util.*

class RentalReservation(
    val carUid: UUID,
    val dateFrom: Instant,
    val dateTo: Instant
)