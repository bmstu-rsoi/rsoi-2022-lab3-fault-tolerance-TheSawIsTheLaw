package services.gateway.entity.response

import java.util.*

class CarsResponse(
    val page: Int,
    val pageSize: Int,
    val totalElements: Int,
    val items: List<CarCarsResponse>
)

class CarCarsResponse(
    val carUid: UUID,
    val brand: String,
    val model: String,
    val registrationNumber: String,
    val power: Int?,
    val type: String?,
    val price: Int,
    val available: Boolean
)