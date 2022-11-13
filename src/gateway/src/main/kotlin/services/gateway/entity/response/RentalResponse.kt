package services.gateway.entity.response

import java.time.Instant
import java.util.*

class RentalResponse(
    val rentalUid: UUID,
    val status: String,
    val dateFrom: String,
    val dateTo: String,
    val car: CarRentalResponse,
    val payment: PaymentRentalResponse
)

class CarRentalResponse(
    val carUid: UUID,
    val brand: String,
    val model: String,
    val registrationNumber: String
)

class PaymentRentalResponse(
    val paymentUid: UUID,
    val status: String,
    val price: Int
)