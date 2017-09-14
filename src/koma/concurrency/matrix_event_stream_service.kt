package com.smith.faktor

import controller.guiEvents
import javafx.concurrent.ScheduledService
import javafx.concurrent.Task
import koma.matrix.sync.SyncResponse
import matrix.ApiClient
import rx.Observable
import rx.javafx.kt.addTo

class EventService(val apiClient: ApiClient, var from: String?) : ScheduledService<SyncResponse>() {
    init {
        this.restartOnFailure = true
    }

    override fun createTask(): Task<SyncResponse> {
        return object : Task<SyncResponse>() {
            override fun call(): SyncResponse? {
        val eventResult = apiClient.getEvents(from)
        if (eventResult == null) {
          failed()
          return null
        } else{
          from = eventResult.next_batch
          return eventResult
        }
      }
    }
  }
}
