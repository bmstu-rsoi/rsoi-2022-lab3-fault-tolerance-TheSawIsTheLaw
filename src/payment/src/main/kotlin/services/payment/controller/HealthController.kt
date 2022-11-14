package services.payment.controller

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@Controller
@RequestMapping("/")
class HealthController {

    @GetMapping("/manage/health")
    fun healthCheck(): ResponseEntity<*> = ResponseEntity.ok(null)
}
