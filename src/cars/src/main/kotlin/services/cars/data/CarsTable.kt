package services.cars.data

import org.jetbrains.exposed.sql.Table

object CarsTable: Table() {

    val id = integer("id").autoIncrement()
    val carUid = uuid("car_uid")
    val brand = varchar("brand", 80)
    val model = varchar("model", 80)
    val registrationNumber = varchar("registration_number", 20)
    val power = integer("power").nullable()
    val price = integer("price")
    val type =
        varchar("type", 20)
            .check { it.inList(listOf("SEDAN", "SUV", "MINIVAN", "ROADSTER")) }
            .nullable()
    val availability = bool("availability")
}