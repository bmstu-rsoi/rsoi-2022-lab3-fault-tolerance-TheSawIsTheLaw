package services.cars.entity

import java.util.UUID

class Car(
    val id: Int,
    val carUid: UUID,
    val brand: String,
    val model: String,
    val registrationNumber: String,
    val price: Int,
    val availability: Boolean,
    val power: Int?,
    val type: String?
)