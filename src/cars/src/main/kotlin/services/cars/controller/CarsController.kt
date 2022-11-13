package services.cars.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import services.cars.entity.Car
import services.cars.repository.CarsRepository
import java.util.UUID

@Controller
@RequestMapping("/api/v1/cars")
class CarsController {

    @GetMapping("/")
    fun getCars(@RequestParam("showAll") showAll: Boolean): ResponseEntity<Array<Car>> =
        ResponseEntity.ok(CarsRepository.get(showAll))

    @GetMapping("/{carUid}")
    fun getCar(@PathVariable carUid: UUID): ResponseEntity<Car> =
        ResponseEntity.ok(CarsRepository.get(carUid))

    @PatchMapping("/{carUid}/available")
    fun availableCar(@PathVariable carUid: UUID): ResponseEntity<*> =
        ResponseEntity("...", HttpStatus.OK).apply { CarsRepository.changeAvailability(carUid, true) }

    @PatchMapping("/{carUid}/unavailable")
    fun unavailableCar(@PathVariable carUid: UUID): ResponseEntity<*> =
        ResponseEntity("...", HttpStatus.OK).apply { CarsRepository.changeAvailability(carUid, false) }
}