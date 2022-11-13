package services.rental.data

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object RentalTable: Table() {

    val id = integer("id").autoIncrement()
    val rentalUid = uuid("rental_uid")
    val username = varchar("username", 80)
    val paymentUid = uuid("payment_uid")
    val carUid = uuid("car_uid")
    val dateFrom = timestamp("date_from")
    val dateTo = timestamp("date_to")
    val status = varchar("status", 20).check { it.inList(listOf("IN_PROGRESS", "FINISHED", "CANCELED")) }
}