package services.gateway.utils

import com.google.gson.Gson

object GsonKeeper {

    val gson by lazy {
        Gson()
    }
}