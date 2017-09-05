package com.smith.faktor

import controller.guiEvents
import domain.Chunked
import javafx.concurrent.ScheduledService
import javafx.concurrent.Task
import matrix.ApiClient
import model.Message
import rx.Observable
import rx.javafx.kt.addTo

class EventService(val apiClient: ApiClient, var from: String) : ScheduledService<Chunked<Message>>() {
    init {
        this.restartOnFailure = true
    }

  override fun createTask(): Task<Chunked<Message>>? {
    return object : Task<Chunked<Message>>() {
      override fun call(): Chunked<Message>? {
        val eventResult = apiClient.getEvents(from)
        if (eventResult == null) {
            Observable.just("Events Failed").addTo(guiEvents.statusMessage)
          failed()
          return null
        } else {
          from = eventResult.end
          return eventResult
        }
      }
    }
  }
}
