package koma.koma_app

import java.util.logging.Level
import java.util.logging.Logger

class NoAlertErrorHandler: Thread.UncaughtExceptionHandler {
    val log = Logger.getLogger("ErrorHandler")

    override fun uncaughtException(t: Thread?, e: Throwable?) {
        log.log(Level.SEVERE, "Uncaught error", e)
        e?.printStackTrace()
    }
}
