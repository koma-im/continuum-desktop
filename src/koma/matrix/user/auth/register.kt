package koma.matrix.user.auth

import com.github.kittinunf.result.Result
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import koma.matrix.UserId
import koma.network.client.okhttp.AppHttpClient
import koma.storage.config.server.ServerConf
import koma.storage.config.server.getAddress
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST


sealed class RegisterData() {
    // Use empty request to get auth types
    class Query(): RegisterData()
    data class Password(
            val username: String,
            val password: String,
            val auth: Map<String, String> = mapOf(Pair("type", "m.login.dummy"))
    ): RegisterData()
    // resubmit a request with an auth dict with just the session ID
    class Finalize(
            val session: String
    ): RegisterData()
}

data class RegisterdUser(
        val access_token: String,
        // set by the server admin, not necessarily a valid address
        val home_server: String,
        val user_id: UserId,
        val refresh_token: String? = null
)

interface MatrixRegisterApi {
    @POST("_matrix/client/r0/register")
    fun register(@Body data: RegisterData): Call<RegisterdUser>
}

class Register(val serverConf: ServerConf) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val client = AppHttpClient.builderForServer(serverConf).build()
    private val retrofit = Retrofit.Builder()
            .baseUrl(serverConf.getAddress())
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    private val service = retrofit.create(MatrixRegisterApi::class.java)
    private var session: String? = null

    suspend fun getFlows(): Result<Unauthorized, Exception> {
        val data = RegisterData.Query()
        val result = service.register(data).awaitMatrixAuth()
        if (result is Result.Failure) {
            val ex = result.error
            if (ex is AuthException.AuthFail) {
                session = ex.status.session // Save identifier for future requests
                return Result.of(ex.status)
            }
            return Result.error(ex)
        }
        return Result.error(Exception("Unexpected"))
    }
    suspend fun finishStage(): Result<RegisterdUser, Exception> {
        val s = session
        s ?: return Result.error(Exception("Session is null"))
        val d = service.register(RegisterData.Finalize(s))
        return d.awaitMatrixAuth()
    }

    fun registerByPassword(username: String, password: String):
            Call<RegisterdUser> {
        val data = RegisterData.Password(username, password)
        println("register user $username on ${serverConf.servername}")
        return service.register(data)
    }
}

