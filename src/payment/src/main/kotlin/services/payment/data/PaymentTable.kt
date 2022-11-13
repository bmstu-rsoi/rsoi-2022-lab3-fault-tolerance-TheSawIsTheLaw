package services.payment.data

import org.jetbrains.exposed.sql.Table

object PaymentTable: Table() {

    val id = integer("id").autoIncrement()
    val paymentUid = uuid("payment_uid")
    val status = varchar("status", 20).check { it.inList(listOf("PAID", "CANCELED")) }
    val price = integer("price")
}