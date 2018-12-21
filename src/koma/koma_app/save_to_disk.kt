package koma.koma_app

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

internal object SaveToDiskTasks {

    val jobs = mutableListOf<()->Unit>()

    fun addJob(job: ()->Unit) {
        jobs.add(job)
    }

    fun saveToDisk() {
        logger.debug { "saving ${jobs.size} items before shutdown" }
        jobs.forEach { it() }
        logger.debug { "saved ${jobs.size} items now shuting down" }
    }
}
