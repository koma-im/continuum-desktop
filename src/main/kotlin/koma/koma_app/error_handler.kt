package koma.koma_app

import mu.KotlinLogging
import java.io.PrintStream
import java.util.concurrent.ConcurrentLinkedDeque


private val logger = KotlinLogging.logger {}

class NoAlertErrorHandler: Thread.UncaughtExceptionHandler {
    private val recent = ConcurrentLinkedDeque<Pair<Long, Throwable?>>()
    override fun uncaughtException(t: Thread?, e: Throwable?) {
        try {
            recent.add(System.nanoTime() to e)
            logger.error { "uncaught error $e" }
            e?.printStackTrace()
            checkAmount()
        } catch (e: Exception) {
            logger.error { "got error handling uncaught exception: $e" }
        }
    }
    private fun checkAmount() {
        val now = System.nanoTime()
        val n = recent.descendingIterator().asSequence().takeWhile { (now - it.first) < 1e9 }.count()
        if (n > 9) {
            logger.error { "$n errors in 1s, shutting down" }
            Runtime.getRuntime().exit(5)
        }
        recent.removeIf { (now - it.first) > 1e9 }
    }
}
