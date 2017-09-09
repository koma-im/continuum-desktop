package koma.storage.rooms

import javafx.application.Platform
import javafx.beans.property.SimpleListProperty
import javafx.collections.FXCollections
import model.Room

object RoomStore {
    val roomList = SimpleListProperty(FXCollections.observableArrayList<Room>())

    @Synchronized
    fun getOrCreate(roomId: String): Room {
        val knownRoom = roomList.firstOrNull { it.id == roomId }
        if (knownRoom != null)
            return knownRoom

        val newRoom = Room(roomId)
        Platform.runLater{
            roomList.add(newRoom)
        }
        return newRoom
    }

    inline fun forEach(action: (Room)-> Unit) {
        for (element in roomList) action(element)
    }

    @Synchronized
    fun remove(roomId: String) {
        for ((index, value) in roomList.withIndex()) {
            if (value.id == roomId) {
                roomList.removeAt(index)
                break;
            }
        }
    }
}

