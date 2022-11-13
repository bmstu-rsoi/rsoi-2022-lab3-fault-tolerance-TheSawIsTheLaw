package services.gateway.utils

import okhttp3.OkHttpClient

object ClientKeeper {

    val client by lazy { OkHttpClient() }
}