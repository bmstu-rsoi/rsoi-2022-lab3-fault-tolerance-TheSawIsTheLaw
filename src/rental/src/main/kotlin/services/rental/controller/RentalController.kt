package services.rental.controller

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import services.rental.entity.Rental
import services.rental.entity.StupidInstant
import services.rental.entity.StupidRental
import services.rental.repository.RentalRepository
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

@Controller
@RequestMapping("/api/v1/rental")
class RentalController {

    @Bean
    fun javaTimeModule(): JavaTimeModule? {
        val javaTimeModule = JavaTimeModule()
        javaTimeModule.addDeserializer(Instant::class.java, object : StdDeserializer<Instant?>(Instant::class.java) {
            @Throws(IOException::class, JsonProcessingException::class)
            override fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext?): Instant? {
                return jsonParser.readValueAs(StupidInstant::class.java).let { Instant.ofEpochSecond(it.seconds) }
//                return LocalDateTime.parse(jsonParser.readValueAs(String::class.java), formatter).toInstant(ZoneOffset.MIN)
            }
        })
        return javaTimeModule
    }

    @GetMapping("/")
    fun getRentals(@RequestHeader("User-Name") username: String): ResponseEntity<Array<StupidRental>> =
        ResponseEntity.ok(RentalRepository.get(username))

    @GetMapping("/{rentalUid}")
    fun getRental(@PathVariable rentalUid: UUID): ResponseEntity<StupidRental> =
        ResponseEntity.ok(RentalRepository.get(rentalUid))

    @PostMapping("/")
    fun addRental(@RequestBody rental: Rental): ResponseEntity<Int> =
        ResponseEntity.ok(RentalRepository.add(rental))

    @PatchMapping("/{rentalUid}/finish")
    fun finishRental(@PathVariable rentalUid: UUID): ResponseEntity<*> =
        ResponseEntity("...", HttpStatus.OK).apply { RentalRepository.setStatus(rentalUid, "FINISHED") }

    @PatchMapping("/{rentalUid}/cancel")
    fun cancelRental(@PathVariable rentalUid: UUID): ResponseEntity<*> =
        ResponseEntity("...", HttpStatus.OK).apply { RentalRepository.setStatus(rentalUid, "CANCELED") }
}