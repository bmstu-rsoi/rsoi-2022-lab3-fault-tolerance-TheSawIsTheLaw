package services.gateway.utils

object CircuitBreaker {

    var maxFailures = 3
    var awaitTime = 60_000

    private val states = hashMapOf(
        Service.CAR to State.CLOSED,
        Service.PAYMENT to State.CLOSED,
        Service.RENTAL to State.CLOSED
    )

    private val failuresCounter = hashMapOf(
        Service.CAR to 0,
        Service.PAYMENT to 0,
        Service.RENTAL to 0
    )

    private val timers = hashMapOf(
        Service.CAR to 0L,
        Service.PAYMENT to 0L,
        Service.RENTAL to 0L
    )

    fun shouldThrowInternalOnCall(service: Service) =
        states.getOrDefault(service, State.OPEN) == State.OPEN
                && timers.getOrDefault(service, System.currentTimeMillis() - System.currentTimeMillis()) < awaitTime

    fun incrementFailuresAndCheck(service: Service): Boolean {
        failuresCounter[service] = failuresCounter.getOrDefault(service, 0) + 1
        val isFailuresOverflow = failuresCounter[service]!! >= maxFailures
        if (isFailuresOverflow) failuresCounter[service] = 0
        return isFailuresOverflow
    }

    fun serviceFailure(service: Service) {
        states[service] = State.OPEN
        timers[service] = System.currentTimeMillis()
    }

    fun serviceIsOk(service: Service) {
        states[service] = State.CLOSED
        failuresCounter[service] = 0
    }

    enum class State {
        OPEN, CLOSED, HALF_OPEN
    }

    enum class Service {
        CAR, RENTAL, PAYMENT
    }
}