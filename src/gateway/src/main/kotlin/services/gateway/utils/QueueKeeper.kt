package services.gateway.utils

import okhttp3.Request
import okio.use
import org.springframework.stereotype.Component
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import kotlin.concurrent.thread

@Component
class QueueKeeper {

    val rentalRequestsQueue: BlockingQueue<Request> = ArrayBlockingQueue(100)

    val paymentRequestsQueue: BlockingQueue<Request> = ArrayBlockingQueue(100)

    init {
        thread {
            while (true) {
                val request = rentalRequestsQueue.poll()
                if (request != null) {
                    try {
                        ClientKeeper.client.newCall(request).execute().use { response ->
                            if (!response.isSuccessful) rentalRequestsQueue.add(request)
                        }
                    } catch (ex: Exception) {
                        rentalRequestsQueue.add(request)
                    }
                }
            }
        }

        thread {
            while (true) {
                val request = paymentRequestsQueue.poll()
                if (request != null) {
                    try {
                        ClientKeeper.client.newCall(request).execute().use { response ->
                            if (!response.isSuccessful) paymentRequestsQueue.add(request)
                        }
                    } catch (ex: Exception) {
                        paymentRequestsQueue.add(request)
                    }
                }
            }
        }
    }
}