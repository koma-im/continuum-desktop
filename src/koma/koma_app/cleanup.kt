package koma.koma_app

object SaveJobs {

    val jobs = mutableListOf<()->Unit>()

    fun addJob(job: ()->Unit) {
        jobs.add(job)
    }

    fun finishUp() {
        println("running ${jobs.size} jobs before shutdown")
        jobs.forEach { it() }
        println("finished ${jobs.size} remaining clean up tasks")
    }
}
