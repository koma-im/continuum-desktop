package koma.concurrency

import javafx.application.Platform
import javafx.concurrent.Task
import koma.matrix.room.naming.RoomId
import matrix.ApiClient
import matrix.BanRoomResult
import matrix.InviteMemResult
import matrix.JoinRoomResult






fun runBanRoomMember(apiClient: ApiClient, room: RoomId, memId: String)  {

    val task = object : Task<BanRoomResult?>() {
        override fun call(): BanRoomResult? {
            val banRoomResult = apiClient.banningMember(room, memId)
            if (banRoomResult == null) {
                return null
            } else {
                return banRoomResult
            }
        }
    }
    Thread(task).start()
}

fun <T> runTask(func: () -> T, cb: (T) -> Unit) {
    val task = object : Task<T>() {
        override fun call(): T {
            return func()
        }
    }
    task.setOnSucceeded {
        Platform.runLater {
            cb(task.value)
        }
    }
    Thread(task).start()
}

