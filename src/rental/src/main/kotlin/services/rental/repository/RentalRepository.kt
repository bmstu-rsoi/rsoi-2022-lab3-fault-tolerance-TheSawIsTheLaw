package services.rental.repository

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import services.rental.data.RentalTable
import services.rental.data.RentalTable.carUid
import services.rental.data.RentalTable.dateFrom
import services.rental.data.RentalTable.dateTo
import services.rental.data.RentalTable.paymentUid
import services.rental.data.RentalTable.rentalUid
import services.rental.data.RentalTable.status
import services.rental.entity.Rental
import services.rental.entity.StupidInstant
import services.rental.entity.StupidRental
import services.rental.insecure.Config
import java.util.UUID

object RentalRepository {

    private val db by lazy {
        Database.connect(
            Config.POSTGRES_DB_ADDRESS,
            "org.postgresql.Driver",
            Config.POSTGRES_USER,
            Config.POSTGRES_PASSWORD
        )
    }

    fun get(username: String): Array<StupidRental> =
        transaction(db) {
            RentalTable
                .select(RentalTable.username eq username)
                .map { rental ->
                    StupidRental(
                        rental[RentalTable.id],
                        rental[rentalUid],
                        rental[RentalTable.username],
                        rental[paymentUid],
                        rental[carUid],
                        StupidInstant(rental[dateFrom].epochSecond, 0),
                        StupidInstant(rental[dateTo].epochSecond, 0),
                        rental[status]
                    )
                }
                .toTypedArray()
        }

    fun get(rentalUid: UUID): StupidRental? =
        transaction(db) {
            RentalTable
                .select(RentalTable.rentalUid eq rentalUid)
                .map { rental ->
                    StupidRental(
                        rental[RentalTable.id],
                        rental[RentalTable.rentalUid],
                        rental[RentalTable.username],
                        rental[paymentUid],
                        rental[carUid],
                        StupidInstant(rental[dateFrom].epochSecond, 0),
                        StupidInstant(rental[dateTo].epochSecond, 0),
                        rental[status]
                    )
                }
                .firstOrNull()
        }

    fun add(rental: Rental) =
        transaction(db) {
            RentalTable
                .insert {
                    it[rentalUid] = rental.rentalUid
                    it[username] = rental.username
                    it[paymentUid] = rental.paymentUid
                    it[carUid] = rental.carUid
                    it[dateFrom] = rental.dateFrom
                    it[dateTo] = rental.dateTo
                    it[status] = rental.status
                }.resultedValues!!.first()[RentalTable.id]
        }

    fun setStatus(uid: UUID, newStatus: String) =
        transaction(db) {
            RentalTable
                .update({ rentalUid eq uid }) {
                    it[status] = newStatus
                }
        }
}