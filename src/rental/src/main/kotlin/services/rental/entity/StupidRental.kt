package services.rental.entity

import java.util.*

class StupidRental(
    val id: Int,
    val rentalUid: UUID,
    val username: String,
    val paymentUid: UUID,
    val carUid: UUID,
    val dateFrom: StupidInstant,
    val dateTo: StupidInstant,
    val status: String
)