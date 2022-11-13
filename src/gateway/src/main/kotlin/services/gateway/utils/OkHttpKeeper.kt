package services.gateway.utils

import okhttp3.Request

object OkHttpKeeper {

    const val CARS_URL = "http://localhost:8070/api/v1/cars"
    const val RENTAL_URL = "http://localhost:8060/api/v1/rental"
    const val PAYMENT_URL = "http://localhost:8050/api/v1/payment"

    val builder by lazy { Request.Builder() }

}