package services.gateway.controller

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.EMPTY_REQUEST
import okio.use
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import services.gateway.entity.*
import services.gateway.entity.response.*
import services.gateway.utils.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import kotlin.concurrent.thread

class ResponseMessage(val message: String)

@Controller
@RequestMapping("api/v1")
class GatewayController @Autowired constructor(val queueKeeper: QueueKeeper) {

    @Bean
    fun javaTimeModule(): JavaTimeModule? {
        val javaTimeModule = JavaTimeModule()
        val formatter = SimpleDateFormat("yyyy-MM-dd")
        javaTimeModule.addDeserializer(Instant::class.java, object : StdDeserializer<Instant?>(Instant::class.java) {
            @Throws(IOException::class, JsonProcessingException::class)
            override fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext?): Instant? {
                return try {
                    var stringDate = jsonParser.readValueAs(String::class.java)
                    if (stringDate.length > 10) stringDate = stringDate.slice(0 until 10)
                    formatter.parse(stringDate).toInstant()
                } catch (ex: Exception) {
                    jsonParser.readValueAs(StupidInstant::class.java).let { Instant.ofEpochSecond(it.seconds) }
                }
            }
        })
        return javaTimeModule
    }

    private fun getRental(uid: UUID): Rental? {
        val rentalRequest =
            OkHttpKeeper
                .builder
                .url(OkHttpKeeper.RENTAL_URL + "/$uid")
                .get()
                .build()

        return ClientKeeper.client.newCall(rentalRequest).execute().use { response ->
            if (!response.isSuccessful) null
            else {
                val body = response.body!!.string()
                GsonKeeper.gson.fromJson(body, Rental::class.java)
            }
        }
    }

    private fun getCars(showAll: Boolean): ResponseEntity<List<Car>?> {
        if (CircuitBreaker.shouldThrowInternalOnCall(CircuitBreaker.Service.CAR)) {
            return ResponseEntity.internalServerError().build()
        }

        val carRequest =
            OkHttpKeeper
                .builder
                .url(OkHttpKeeper.CARS_URL + "/?showAll=$showAll")
                .get()
                .build()

        return ClientKeeper.client.newCall(carRequest).execute().use { response ->
            if (!response.isSuccessful) {
                if (CircuitBreaker.incrementFailuresAndCheck(CircuitBreaker.Service.CAR)) {
                    CircuitBreaker.serviceFailure(CircuitBreaker.Service.CAR)
                    ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
                } else {
                    getCars(showAll)
                }
            } else {
                CircuitBreaker.serviceIsOk(CircuitBreaker.Service.CAR)
                val typeToken = object : TypeToken<List<Car>>() {}.type
                ResponseEntity.ok(GsonKeeper.gson.fromJson<List<Car>>(response.body!!.string(), typeToken))
            }
        }
    }

    private fun getPayments(): ResponseEntity<List<Payment>?> {
        if (CircuitBreaker.shouldThrowInternalOnCall(CircuitBreaker.Service.PAYMENT)) {
            return ResponseEntity.internalServerError().build()
        }

        val paymentRequest =
            OkHttpKeeper
                .builder
                .url(OkHttpKeeper.PAYMENT_URL + "/")
                .get()
                .build()

        return try {
            ClientKeeper.client.newCall(paymentRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    if (CircuitBreaker.incrementFailuresAndCheck(CircuitBreaker.Service.PAYMENT)) {
                        CircuitBreaker.serviceFailure(CircuitBreaker.Service.PAYMENT)
                        ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
                    } else {
                        getPayments()
                    }
                }
                else {
                    CircuitBreaker.serviceIsOk(CircuitBreaker.Service.PAYMENT)
                    val typeToken = object : TypeToken<List<Payment>>() {}.type
                    ResponseEntity.ok(GsonKeeper.gson.fromJson<List<Payment>>(response.body!!.string(), typeToken))
                }
            }
        } catch (ex: Exception) {
            if (CircuitBreaker.incrementFailuresAndCheck(CircuitBreaker.Service.PAYMENT)) {
                CircuitBreaker.serviceFailure(CircuitBreaker.Service.PAYMENT)
                ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
            } else {
                getPayments()
            }
        }
    }

    @GetMapping("/cars")
    fun getCars(
        @RequestParam("page") page: Int,
        @RequestParam("size") size: Int,
        @RequestParam("showAll", required = false, defaultValue = "false") showAll: Boolean
    ): ResponseEntity<CarsResponse> {
        val cars = getCars(showAll).body ?: return ResponseEntity.internalServerError().build()

        return ResponseEntity.ok(
            CarsResponse(
                page,
                size,
                cars.size,
                cars
                    .slice(size * (page - 1) until if (size * page < cars.size) (size * page) else cars.size)
                    .let {
                        it.map { car ->
                            CarCarsResponse(
                                car.carUid,
                                car.brand,
                                car.model,
                                car.registrationNumber,
                                car.power,
                                car.type,
                                car.price,
                                car.availability
                            )
                        }
                    }
            )
        )
    }

    @GetMapping("/rental")
    fun getRentals(@RequestHeader("X-User-Name") username: String) : ResponseEntity<Any> {

        if (CircuitBreaker.shouldThrowInternalOnCall(CircuitBreaker.Service.RENTAL)) {
            return ResponseEntity("Rental Service unavailable", HttpStatus.SERVICE_UNAVAILABLE)
        }

        val request =
            OkHttpKeeper
                .builder
                .url(OkHttpKeeper.RENTAL_URL + "/")
                .addHeader("User-Name", username)
                .get()
                .build()

        val rentals = ClientKeeper.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return if (CircuitBreaker.incrementFailuresAndCheck(CircuitBreaker.Service.RENTAL)) {
                    CircuitBreaker.serviceFailure(CircuitBreaker.Service.RENTAL)
                    ResponseEntity("Rental Service unavailable", HttpStatus.SERVICE_UNAVAILABLE)
                } else {
                    getRentals(username)
                }
            }
            else {
                CircuitBreaker.serviceIsOk(CircuitBreaker.Service.RENTAL)
                val typeToken = object : TypeToken<List<Rental>>() {}.type
                GsonKeeper.gson.fromJson<List<Rental>>(response.body!!.string(), typeToken)
            }
        }

        val cars = getCars(true).body

        val payments = getPayments().body

        val outputDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault())

        return ResponseEntity.ok(
            rentals.map { rental ->
                RentalResponse(
                    rental.rentalUid,
                    rental.status,
                    outputDateFormatter.format(rental.dateFrom),
                    outputDateFormatter.format(rental.dateTo),
                    cars
                        ?.findLast { car -> car.carUid == rental.carUid }
                        .let {
                            if (it != null) CarRentalResponse(it.carUid, it.brand, it.model, it.registrationNumber)
                            else CarRentalResponse(rental.carUid)
                        },
                    payments
                        ?.findLast { payment -> payment.paymentUid == rental.paymentUid }
                        .let {
                            if (it != null) PaymentRentalResponse(it.paymentUid, it.status, it.price)
                            else PaymentRentalResponse(rental.paymentUid)
                        }
                )
            }
        )
    }

    @PostMapping("/rental", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun reserveRental(
        @RequestHeader("X-User-Name") username: String,
        @RequestBody reservation: RentalReservation
    ) : ResponseEntity<Any> {

        val cars = getCars(true).body ?: return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
        val car = cars.findLast { it.carUid == reservation.carUid } ?: return ResponseEntity.notFound().build()

        val reserveCarRequest =
            OkHttpKeeper
                .builder
                .url(OkHttpKeeper.CARS_URL + "/${car.carUid}/unavailable")
                .patch(EMPTY_REQUEST)
                .build()

        // Better use like a transaction but... You know. I don't give a shit.
        ClientKeeper.client.newCall(reserveCarRequest).execute().use { response ->
            if (!response.isSuccessful) return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
        }

        val rentalPeriodDays = ChronoUnit.DAYS.between(reservation.dateFrom, reservation.dateTo)
        val money = car.price * rentalPeriodDays
        val paymentUid = UUID.randomUUID()

        val rentalToPost = Rental(
            0,
            UUID.randomUUID(),
            username,
            paymentUid,
            car.carUid,
            reservation.dateFrom,
            reservation.dateTo,
            "IN_PROGRESS"
        )

        val rentalRequest =
            OkHttpKeeper
                .builder
                .url(OkHttpKeeper.RENTAL_URL + "/")
                .post(GsonKeeper.gson.toJson(rentalToPost).toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

        ClientKeeper.client.newCall(rentalRequest).execute().use { response ->
            if (!response.isSuccessful)  {
                val restoreRequest = OkHttpKeeper
                    .builder
                    .url(OkHttpKeeper.CARS_URL + "/${car.carUid}/available")
                    .patch(EMPTY_REQUEST)
                    .build()

                ClientKeeper.client.newCall(restoreRequest).execute()

                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
            }
        }

        val paymentToPost = Payment(
            0,
            paymentUid,
            "PAID",
            money.toInt()
        )

        val paymentRequest =
            OkHttpKeeper
                .builder
                .url(OkHttpKeeper.PAYMENT_URL + "/")
                .post(GsonKeeper.gson.toJson(paymentToPost).toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

        try {
            ClientKeeper.client.newCall(paymentRequest).execute().use { response ->
                if (!response.isSuccessful) {

                    val restoreCarRequest = OkHttpKeeper
                        .builder
                        .url(OkHttpKeeper.CARS_URL + "/${car.carUid}/available")
                        .patch(EMPTY_REQUEST)
                        .build()

                    ClientKeeper.client.newCall(restoreCarRequest).execute()

                    val restoreRentalRequest =
                        OkHttpKeeper
                            .builder
                            .url(OkHttpKeeper.RENTAL_URL + "/${rentalToPost.rentalUid}/cancel")
                            .patch(EMPTY_REQUEST)
                            .build()

                    ClientKeeper.client.newCall(restoreRentalRequest).execute()

                    return ResponseEntity
                        .status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(ResponseMessage("Payment Service unavailable"))
                }
            }
        } catch (ex: Exception) {
            return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ResponseMessage("Payment Service unavailable"))
        }
        val outputDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault())

        return ResponseEntity.ok(
            ReservationResponse(
                rentalToPost.rentalUid,
                rentalToPost.status,
                car.carUid,
                outputDateFormatter.format(rentalToPost.dateFrom),
                outputDateFormatter.format(rentalToPost.dateTo),
                paymentToPost
            )
        )
    }

    @GetMapping("/rental/{rentalUid}")
    fun getUsersRental(
        @PathVariable("rentalUid") rentalUid: UUID,
        @RequestHeader("X-User-Name") username: String
    ): ResponseEntity<RentalResponse> {
        val rental = getRental(rentalUid) ?: return ResponseEntity.notFound().build()

        if (username != rental.username) return ResponseEntity.notFound().build()

        val cars = getCars(true).body
        val payments = getPayments().body

        val outputDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault())

        return ResponseEntity.ok(
            RentalResponse(
                rental.rentalUid,
                rental.status,
                outputDateFormatter.format(rental.dateFrom),
                outputDateFormatter.format(rental.dateTo),
                cars
                    ?.findLast { car -> car.carUid == rental.carUid }
                    .let {
                        if (it != null) CarRentalResponse(it.carUid, it.brand, it.model, it.registrationNumber)
                        else CarRentalResponse(rental.carUid)
                    },
                payments
                    ?.findLast { payment -> payment.paymentUid == rental.paymentUid }
                    .let {
                        if (it != null) PaymentRentalResponse(it.paymentUid, it.status, it.price)
                        else BasePaymentResponse()
                    }
            )
        )
    }

    @PostMapping("/rental/{rentalUid}/finish")
    fun finishRental(
        @RequestHeader("X-User-Name") username: String,
        @PathVariable rentalUid: UUID
    ): ResponseEntity<*> {
        val rental = getRental(rentalUid) ?: return ResponseEntity("lol what", HttpStatus.NOT_FOUND)

        if (username != rental.username) return ResponseEntity("lol what", HttpStatus.NOT_FOUND)

        val carAvailableStateRequest =
            OkHttpKeeper
                .builder
                .url(OkHttpKeeper.CARS_URL + "/${rental.carUid}/available")
                .patch(EMPTY_REQUEST)
                .build()

        ClientKeeper.client.newCall(carAvailableStateRequest).execute()

        val rentalFinishRequest =
            OkHttpKeeper
                .builder
                .url(OkHttpKeeper.RENTAL_URL + "/${rental.rentalUid}/finish")
                .patch(EMPTY_REQUEST)
                .build()

        ClientKeeper.client.newCall(rentalFinishRequest).execute()

        return ResponseEntity("...", HttpStatus.NO_CONTENT)
    }

    @DeleteMapping("/rental/{rentalUid}")
    fun cancelRent(
        @RequestHeader("X-User-Name") username: String,
        @PathVariable rentalUid: UUID
    ): ResponseEntity<String> {
        val rental = getRental(rentalUid) ?: return ResponseEntity("lol man", HttpStatus.NOT_FOUND)

        if (rental.username != username) return ResponseEntity("lol man", HttpStatus.NOT_FOUND)

        val carAvailableStateRequest =
            OkHttpKeeper
                .builder
                .url(OkHttpKeeper.CARS_URL + "/${rental.carUid}/available")
                .addHeader("User-Name", username)
                .patch(EMPTY_REQUEST)
                .build()

        ClientKeeper.client.newCall(carAvailableStateRequest).execute().use { response ->
            if (!response.isSuccessful) return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
        }

        val cancelRentalRequest =
            OkHttpKeeper
                .builder
                .url(OkHttpKeeper.RENTAL_URL + "/$rentalUid/cancel")
                .patch(EMPTY_REQUEST)
                .build()

        if (!ClientKeeper.client.newCall(cancelRentalRequest).execute().isSuccessful) {
            queueKeeper.rentalRequestsQueue.add(cancelRentalRequest)
        }

        val cancelPaymentRequest =
            OkHttpKeeper
                .builder
                .url(OkHttpKeeper.PAYMENT_URL + "/${rental.paymentUid}/cancel")
                .patch(EMPTY_REQUEST)
                .build()

        try {
            if (!ClientKeeper.client.newCall(cancelPaymentRequest).execute().isSuccessful) {
                queueKeeper.paymentRequestsQueue.add(cancelPaymentRequest)
            }
        } catch (ex: Exception) {
            queueKeeper.paymentRequestsQueue.add(cancelPaymentRequest)
        }

        return ResponseEntity("...", HttpStatus.NO_CONTENT)
    }

    @GetMapping("/manage/health")
    fun healthCheck(): ResponseEntity<*> = ResponseEntity.ok(null)
}