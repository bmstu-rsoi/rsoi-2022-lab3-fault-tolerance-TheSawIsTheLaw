package services.gateway.entity

import java.util.UUID

class Payment(
    val id: Int,
    val paymentUid: UUID,
    val status: String,
    val price: Int
)