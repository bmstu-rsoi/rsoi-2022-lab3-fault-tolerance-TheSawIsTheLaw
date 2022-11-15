package services.gateway.entity.response

import java.util.*

class RentalResponse(
    val rentalUid: UUID,
    val status: String,
    val dateFrom: String,
    val dateTo: String,
    val car: CarRentalResponse,
    val payment: BasePaymentResponse
)

class CarRentalResponse(
    val carUid: UUID,
    val brand: String = "",
    val model: String = "",
    val registrationNumber: String = ""
)

open class BasePaymentResponse
class PaymentRentalResponse(
    val paymentUid: UUID,
    val status: String = "",
    val price: Int = 0
): BasePaymentResponse()