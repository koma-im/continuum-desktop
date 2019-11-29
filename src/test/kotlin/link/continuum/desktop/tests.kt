package koma.gui.element.emoji.icon.link.continuum.desktop

import io.ktor.util.KtorExperimentalAPI
import koma.Server
import koma.matrix.UserId
import koma.matrix.room.naming.RoomId
import koma.network.client.okhttp.KHttpClient
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import org.junit.Test
import java.util.concurrent.TimeUnit

@KtorExperimentalAPI
internal class CacheKtTest {
    @Test
    fun testServerInstance() {
        val u = HttpUrl.Builder()
                .scheme("http")
                .host("localhost")
                .build()
        val s = Server(u, KHttpClient.client.newBuilder().readTimeout(1, TimeUnit.MILLISECONDS).build())
        val api = s.account(UserId("u"), "token")
        runBlocking {
            api.getRoomName(RoomId("r"))
        }
    }
}