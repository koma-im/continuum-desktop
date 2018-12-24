package koma.koma_app

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class NoAlertErrorHandler: Thread.UncaughtExceptionHandler {
        override fun uncaughtException(t: Thread?, e: Throwable?) {
        logger.error { "Uncaught error $e" }
        logger.warn { "Uncaught error stacktrace ${e?.stackTrace}" }
    }
}
