package controller

object events {
    var beforeShutdownHook: MutableList<() -> Unit> = mutableListOf()
    init {
    }
}
