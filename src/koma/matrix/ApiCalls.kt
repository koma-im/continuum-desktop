package matrix

import com.squareup.moshi.Moshi
import domain.*
import koma.matrix.UserId
import koma.matrix.UserIdAdapter
import koma.matrix.pagination.FetchDirection
import koma.matrix.pagination.RoomBatch
import koma.matrix.room.naming.RoomId
import koma.matrix.sync.SyncResponse
import matrix.room.RoomEvent
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import util.saveToken
import java.io.File
import java.io.FileInputStream
import java.net.Proxy
import java.net.SocketTimeoutException
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong



data class CreateRoomSettings(
        val room_alias_name: String,
        val visibility: String)

data class CreateRoomResult(
        val room_id: String)

data class JoinRoomResult(
        val room_id: String)


data class InviteUserData(val user_id: UserId)

class InviteMemResult()

class LeaveRoomResult()

data class MemberBanishment(val user_id: UserId)

class BanRoomResult()

data class ResolveRoomAliasResult(
        val room_id: String,
        val servers: List<String>
)

data class SendMessage(
        val msgtype: String = "m.text",
        val body: String
)

data class SendResult(
        val event_id: String
)

class UpdateAvatarResult()


// interactions using access_token
interface MatrixAccessApi {
    @POST("createRoom")
    fun createRoom(@Query("access_token") token: String,
                   @Body roomSettings: CreateRoomSettings): Call<CreateRoomResult>

    @POST("rooms/{roomIdentifier}/join")
    fun joinRoom(@Path("roomIdentifier") roomIdentifier: String,
                 @Query("access_token") token: String)
            : Call<JoinRoomResult>

    @POST("rooms/{roomId}/leave")
    fun leaveRoom(@Path("roomId") roomId: RoomId,
                  @Query("access_token") token: String)
            : Call<LeaveRoomResult>

    @GET("directory/room/{roomAlias}")
    fun resolveRoomAlias(@Path("roomAlias") roomAlias: String): Call<ResolveRoomAliasResult>

    @PUT("directory/room/{roomAlias}")
    fun putRoomAlias(@Path("roomAlias") roomAlias: String,
                     @Query("access_token") token: String,
                     @Body roomInfo: RoomInfo): Call<EmptyResult>

    @GET("publicRooms")
    fun publicRooms(@Query("since") since: String = "",
                    @Query("limit") limit: Int = 20
    ): Call<RoomBatch<DiscoveredRoom>>


    @GET("rooms/{roomId}/messages")
    fun getMessages(
            @Path("roomId") roomId: RoomId,
            @Query("access_token") token: String,
            @Query("from") from: String,
            @Query("dir") dir: FetchDirection,
            // optional params
            @Query("limit") limit: Int = 3,
            @Query("to") to: String? = null
    ): Call<Chunked<RoomEvent>>

    @POST("rooms/{roomId}/invite")
    fun inviteUser(@Path("roomId") roomId: String,
                   @Query("access_token") token: String,
                   @Body invitation: InviteUserData
    ): Call<InviteMemResult>

    @POST("rooms/{roomId}/ban")
    fun banUser(@Path("roomId") roomId: String,
                @Query("access_token") token: String,
                @Body banishment: MemberBanishment
    ): Call<BanRoomResult>

    @PUT("rooms/{roomId}/send/m.room.message/{txnId}")
    fun sendMessage(@Path("roomId") roomId: RoomId,
                    @Path("txnId") txnId: Long,
                    @Query("access_token") token: String,
                    @Body sendMessage: SendMessage): Call<SendResult>

    @PUT("rooms/{roomId}/send/{eventType}/{txnId}")
    fun sendMessageEvent(
            @Path("roomId") roomId: RoomId,
            @Path("eventType") eventType: String,
            @Path("txnId") txnId: Long,
            @Query("access_token") token: String,
            @Body message: Map<String, String>): Call<SendResult>

    @PUT("rooms/{roomId}/state/m.room.avatar")
    fun setRoomIcon(@Path("roomId") roomId: RoomId,
                    @Query("access_token") token: String,
                    @Body avatar: Map<String, String>): Call<SendResult>

    @PUT("rooms/{roomId}/state/m.room.canonical_alias")
    fun setRoomAlias(@Path("roomId") roomId: RoomId,
                    @Query("access_token") token: String,
                    @Body alias: Map<String, String>): Call<SendResult>


    @GET("sync")
    fun getEvents(@Query("since") from: String? = null,
                  @Query("access_token") token: String,
                  @Query("full_state") full_state: Boolean = false,
                  @Query("timeout") timeout: Int = 50_000,
                  @Query("filter") filter: String? = null)
            : Call<SyncResponse>

   @PUT("profile/{userId}/avatar_url")
    fun updateAvatar(@Path("userId") user_id: UserId,
                     @Query("access_token") token: String,
                     @Body avatarUrl: AvatarUrl): Call<UpdateAvatarResult>

    @PUT("profile/{userId}/displayname")
    fun updateDisplayName(@Path("userId") user_id: UserId,
                     @Query("access_token") token: String,
                     @Body body: Map<String, String>): Call<EmptyResult>

}

interface MatrixMediaApi {
    @POST("upload")
    fun uploadMedia(@Header("Content-Type") type: String,
                    @Query("access_token") token: String,
                    @Body content: RequestBody
    ): Call<UploadResponse>

    @GET("download/{serverName}/{mediaId}")
    fun downloadMedia(@Path("serverName") serverName: String,
                      @Path("mediaId") mediaId: String
    ): Call<ResponseBody>
}

class ApiClient(val baseURL: String, credentials: AuthedUser, proxy: Proxy) {
    val apiURL: String = baseURL + "_matrix/client/r0/"

    val token: String
    val userId: UserId

    val moshi = Moshi.Builder().add(UserIdAdapter()).build()
    val client = OkHttpClient.Builder().proxy(proxy).build()
    val retrofit = Retrofit.Builder()
            .baseUrl(apiURL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    val service = retrofit.create(MatrixAccessApi::class.java)

    val mediaService = Retrofit.Builder().baseUrl(baseURL + "_matrix/media/r0/")
            .addConverterFactory(MoshiConverterFactory.create())
            .client(client)
            .build().create(MatrixMediaApi::class.java)

    fun createRoom(roomname: String, visibility: String): CreateRoomResult? {
        return service.createRoom(token, CreateRoomSettings(roomname, visibility)).execute().body()
    }

    fun getRoomMessages(roomId: RoomId, from: String, direction: FetchDirection, to: String?=null): Call<Chunked<RoomEvent>> {
        return service.getMessages(roomId, token, from, direction, to=to)
    }

    fun joinRoom(roomid: RoomId): Call<JoinRoomResult> {
        return service.joinRoom(roomid.id, token)
    }

    fun uploadMedia(file: String): UploadResponse? {
        val instream = FileInputStream(File(file))
        val buf = instream.readBytes()
        val requestBody = RequestBody.create(MediaType.parse("application/octet-stream"), buf)
        val res = mediaService.uploadMedia("application/octet-stream", token, requestBody).execute()
        if (res.isSuccessful)
            return res.body()
        else {
            println("error uploading $file code ${res.code()}, ${res.errorBody()}, ${res.body()}")
            return null
        }
    }

    fun downloadMedia(url: String): ByteArray? {
        val parts = url.substringAfter("mxc://")
        val server = parts.substringBefore('/')
        val media = parts.substringAfter('/')
        val res = try {
            mediaService.downloadMedia(
                    server, media)
                    .execute()
        } catch (err: java.net.SocketTimeoutException) {
            println("timeout getting $url, with error $err")
            return null
        }
        if (res.isSuccessful) {
            val reb: ResponseBody? = res.body()
            return reb?.bytes()
        }
        else {
            println("error code ${res.code()}, ${res.errorBody()}, ${res.body()}")
            return null
        }
    }

    fun inviteMember(
          room: RoomId,
          memId: UserId): Call<InviteMemResult> =
            service.inviteUser(room.id, token, InviteUserData(memId))

    fun updateAvatar(user_id: UserId, avatarUrl: AvatarUrl): UpdateAvatarResult? {
        println("updating avatar of $user_id to $avatarUrl")
        val call: Call<UpdateAvatarResult> = service.updateAvatar(user_id, token, avatarUrl)
        val resp: Response<UpdateAvatarResult>
        try {
            resp = call.execute()
        } catch(e: Exception) {
            e.printStackTrace()
            return null
        }
        if (resp.isSuccessful) {
            return resp.body()
        } else{
            println("error code ${resp.code()}, ${resp.errorBody()}, ${resp.body()}")
            return null
        }
    }

    fun updateDisplayName(newname: String):EmptyResult? {
        val call: Call<EmptyResult> = service.updateDisplayName(
                this.userId, token,
                mapOf(Pair("displayname", newname)))
        val resp: Response<EmptyResult>
        try {
            resp = call.execute()
        } catch(e: Exception) {
            e.printStackTrace()
            return null
        }
        if (resp.isSuccessful) {
            return resp.body()
        } else{
            println("error code ${resp.code()}, ${resp.errorBody()}, ${resp.body()}")
            return null
        }
    }

    fun uploadRoomIcon(roomId: RoomId, iconUrl: String): SendResult? {
        val call: Call<SendResult> = service.setRoomIcon(roomId, token, mapOf(Pair("url", iconUrl)))
        val resp: Response<SendResult>
        try {
            resp = call.execute()
        } catch(e: Exception) {
            e.printStackTrace()
            return null
        }
        if (resp.isSuccessful) {
            println("set icon of room $roomId to $iconUrl")
            return resp.body()
        } else{
            println("error code ${resp.code()}, ${resp.errorBody()}, ${resp.body()}")
            return null
        }
    }

  fun banMember(
          roomid: RoomId,
          memId: UserId
          ): Call<BanRoomResult> = service.banUser(roomid.id, token, MemberBanishment(memId))

  fun leavingRoom(roomid: RoomId): LeaveRoomResult? {

      println("leaving room with id $roomid")
      val call = service.leaveRoom(roomid, token)
      val resp: Response<LeaveRoomResult>
      try {
          resp = call.execute()
      } catch(e: Exception) {
          e.printStackTrace()
          return null
      }
      if (resp.isSuccessful) {
          return resp.body()
      } else {
          println("error code ${resp.code()}, ${resp.errorBody()}, ${resp.body()}")
          return null
      }
  }

    fun setRoomAlias(roomid: RoomId, alias: String): EmptyResult? {

        val call = service.putRoomAlias(alias, token, RoomInfo(roomid))
        val resp: Response<EmptyResult>
        try {
            resp = call.execute()
        } catch(e: Exception) {
            e.printStackTrace()
            return null
        }
        if (resp.isSuccessful) {
            println("put alias $alias to room $roomid")
            return resp.body()
        } else {
            println("error code ${resp.code()}, ${resp.raw()}")
            return null
        }
    }

    fun setRoomCanonicalAlias(roomid: RoomId, alias: String): SendResult? {

        val call = service.setRoomAlias(roomid, token, mapOf(Pair("alias", alias)))
        val resp: Response<SendResult>
        try {
            resp = call.execute()
        } catch(e: Exception) {
            e.printStackTrace()
            return null
        }
        if (resp.isSuccessful) {
            println("set alias $alias as canonical to room $roomid")
            return resp.body()
        } else {
            println("error code ${resp.code()}, ${resp.raw()}")
            return null
        }
    }

    fun resolveRoomAlias(roomAlias: String): ResolveRoomAliasResult? {
        println("resolving room alias $roomAlias")
        val call: Call<ResolveRoomAliasResult> = service.resolveRoomAlias(roomAlias)
        val resp: Response<ResolveRoomAliasResult>
        try {
            resp = call.execute()
        } catch(e: Exception) {
            e.printStackTrace()
            return null
        }
        if (resp.isSuccessful) {
            return resp.body()
        } else{
            println("error code ${resp.code()}, ${resp.errorBody()}, ${resp.body()}")
            return null
        }
    }

  private var txnIdUnique = AtomicLong()

    fun sendMessage(roomId: RoomId, message: String): Call<SendResult> {
        val txnId = txnIdUnique.addAndGet(1L)
        println("sending message $message to room $roomId ")
        val r = service.sendMessage(roomId, txnId, token, SendMessage(body = message))
        return r
    }

    fun sendImage(roomId: RoomId, imageUrl: String, desc: String): SendResult? {
        val txnId = txnIdUnique.addAndGet(1L)
        val msg = mapOf(
                Pair("msgtype", "m.image"),
                Pair("url", imageUrl),
                Pair("body", desc)
        )
        val call: Call<SendResult> = service.sendMessageEvent(roomId, "m.room.message", txnId, token,
                msg)
        val resp: Response<SendResult>
        try {
            resp = call.execute()
        } catch(e: Exception) {
            e.printStackTrace()
            return null
        }
        if (resp.isSuccessful) {
            println("sent image $imageUrl to room $roomId ")
            return resp.body()
        } else {
            println("error code ${resp.code()}, ${resp.errorBody()}, ${resp.body()}")
            return null
        }
    }

    fun getEvents(from: String?): SyncResponse? {
      val call: Call<SyncResponse> = service.getEvents(from, token)
      val resp: Response<SyncResponse>
      val startime = Instant.now()
      try {
          resp = call.execute()
      } catch (e: SocketTimeoutException) {
          val outime = Instant.now()
          val dur: Duration = Duration.between(startime, outime)
          if (dur.seconds < 8 || dur.seconds > 30)
              println("timeout waiting for events: ${e.message}, time ${dur.seconds}")
          return null
      }
      catch(e: Exception) {
          e.printStackTrace()
          return null
      }
      if (resp.isSuccessful) {
          return resp.body()
      } else{
          println("error code ${resp.code()}, ${resp.errorBody()}, ${resp.body()}")
          return null
      }
  }

    init {
        token = credentials.access_token
        userId = credentials.user_id
    }
}


data class AuthedUser(
        val access_token: String,
        val user_id: UserId)

data class UserPassword(
        val type: String = "m.login.password",
        // name only, without @ or :
        val user: String,
        val password: String
)
interface MatrixLoginApi {
    @POST("_matrix/client/r0/login")
    fun login(@Body userpass: UserPassword): Call<AuthedUser>
}

fun login(serverUrl: String, userpass: UserPassword, proxy: Proxy):
        AuthedUser? {
    val moshi = Moshi.Builder().add(UserIdAdapter()).build()
    val client = OkHttpClient.Builder().proxy(proxy).build()
    val retrofit = Retrofit.Builder()
            .baseUrl(serverUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    val service = retrofit.create(MatrixLoginApi::class.java)
    val auth_call: Call<AuthedUser> = service.login(userpass)
    val authed: retrofit2.Response<AuthedUser> = auth_call.execute()
    if (authed.isSuccessful) {
        println("successful login")
        val user: AuthedUser? = authed.body()
        if (user != null) saveToken(user)
        return user
    } else{
        println("error code ${authed.code()}," +
                "error message ${authed.message()}," +
                "headers ${authed.headers()}, " +
                "raw ${authed.raw()} " +
                "body ${authed.body()}")
        return null
    }
}

data class UserRegistering(
        val username: String,
        val password: String,
        val auth: Map<String, String> = mapOf(Pair("type", "m.login.dummy"))
)

data class RegisterdUser(
        val access_token: String,
        // set by the server admin, not necessarily a valid address
        val home_server: String,
        val user_id: UserId,
        val refresh_token: String? = null
)

interface MatrixRegisterApi {
    @POST("_matrix/client/r0/register")
    fun register(@Body userreg: UserRegistering): Call<RegisterdUser>
}

fun register(serverUrl: String, userregi: UserRegistering, proxy: Proxy):
        RegisterdUser? {
    println("register user $userregi on $serverUrl")
    val moshi = Moshi.Builder().add(UserIdAdapter()).build()
    val client = OkHttpClient.Builder().proxy(proxy).build()
    val retrofit = Retrofit.Builder()
            .baseUrl(serverUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    val service = retrofit.create(MatrixRegisterApi::class.java)
    val auth_call: Call<RegisterdUser> = service.register(userregi)
    val authed: retrofit2.Response<RegisterdUser> = auth_call.execute()
    if (authed.isSuccessful) {
        println("successful registeration")
        val user: RegisterdUser? = authed.body()
        if (user != null) saveToken(
                AuthedUser(user.access_token, user.user_id)
                )
        return user
    } else{
        println("error code ${authed.code()}," +
                "error message ${authed.message()}," +
                "headers ${authed.headers()}, " +
                "raw ${authed.raw()} " +
                "body ${authed.body()}")
        return null
    }
}
