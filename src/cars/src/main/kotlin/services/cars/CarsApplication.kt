package services.cars

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CarsApplication

fun main(args: Array<String>) {
    runApplication<CarsApplication>(*args)
}
