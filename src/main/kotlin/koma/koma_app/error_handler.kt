package koma.koma_app

import mu.KotlinLogging
import java.io.PrintStream



private val logger = KotlinLogging.logger {}

class NoAlertErrorHandler: Thread.UncaughtExceptionHandler {
        override fun uncaughtException(t: Thread?, e: Throwable?) {
            logger.error { "Uncaught error $e" }
            e?.let { printStackTrace(e) }

    }
}

private fun printStackTrace(e: Throwable, s: PrintStream = System.err) {

    for (stackTraceElement in e.stackTrace) {
        s.println("\tat $stackTraceElement")
    }
    for (throwable in e.suppressed) {
        s.println("Suppressed: \t $throwable")
    }
    e.cause?.let {
        s.println("Cause: \t ${e.cause}")
    }
}
