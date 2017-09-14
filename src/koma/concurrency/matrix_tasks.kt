package koma.concurrency

import javafx.application.Platform
import javafx.concurrent.Task
import koma.matrix.room.naming.RoomId
import matrix.ApiClient
import matrix.BanRoomResult
import matrix.InviteMemResult
import matrix.JoinRoomResult

fun run_join_romm(apiClient: ApiClient, room: RoomId) {
    val task =  object : Task<JoinRoomResult?>() {
        override fun call(): JoinRoomResult? {
            val joinResult = apiClient.joiningRoom(room)
            if (joinResult == null) {
                println("failed to join room $room")
                updateMessage("Failed")
                failed()
                return null
            } else {
                println("joined room $room")
                updateMessage("")
                return joinResult
            }
        }
    }
    Thread(task).start()
}


fun runInviteMember(apiClient: ApiClient, room: RoomId, memName: String) {

    val task = object : Task<InviteMemResult?>() {
        override fun call(): InviteMemResult? {
            val inviteResult = apiClient.inviteMember(room, memName)
            if (inviteResult == null) {
                return null
            } else {
                return inviteResult
            }
        }
    }
    Thread(task).start()
}

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

