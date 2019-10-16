package koma.koma_app

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.effect.DropShadow
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.stage.Stage
import koma.gui.save_win_geometry
import koma.gui.setSaneStageSize
import koma.gui.view.window.start.StartScreen
import koma.matrix.UserId
import koma.network.client.okhttp.KHttpClient
import koma.storage.config.getHttpCacheDir
import koma.storage.config.server.cert_trust.sslConfFromStream
import koma.util.given
import kotlinx.coroutines.*
import link.continuum.database.models.getServerAddrs
import link.continuum.database.models.getToken
import link.continuum.desktop.action.startChat
import link.continuum.desktop.gui.CatchingGroup
import link.continuum.desktop.gui.JFX
import link.continuum.desktop.gui.scene.ScalingPane
import link.continuum.desktop.util.disk.path.getConfigDir
import link.continuum.desktop.util.disk.path.loadOptionalCert
import link.continuum.desktop.util.gui.alert
import okhttp3.Cache
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import org.h2.mvstore.MVMap
import org.h2.mvstore.MVStore
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.invoke.MethodHandles
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.time.ExperimentalTime
import kotlin.time.MonoClock

@ExperimentalTime
fun main(args: Array<String>) {
    val lvl = System.getenv()["LOG_LEVEL"]?.also {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", it)
    }
    val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    if (lvl != null) {
        logger.info("log level set to {}", lvl)
    }
    Logger.getLogger(OkHttpClient::class.java.name).level = Level.FINE

    Application.launch(KomaApp::class.java, *args)
    appState.coroutineScope.cancel()
    // should work because instances all share the same dispatcher
    KHttpClient.client.dispatcher().executorService().shutdownNow()
}


@ExperimentalTime
@ExperimentalCoroutinesApi
class KomaApp : Application(), CoroutineScope by CoroutineScope(Dispatchers.Default) {
    private val log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    private var stage: Stage? = null
    private val startScreen = CompletableDeferred<StartScreen>()
    private val configDir: File
    private val kvStore = CompletableDeferred<MVStore>()
    val startTime = MonoClock.markNow()
    init {
        JFX.application = this
        val arg = System.getenv()["CONTINUUM_DIR"]
        val data_dir = arg ?: getConfigDir()
        log.info("data dir set to {}", data_dir)
        configDir = File(data_dir)
        configDir.mkdirs()
        Thread.setDefaultUncaughtExceptionHandler(NoAlertErrorHandler())
        launch(Dispatchers.IO) {
            kvStore.complete(MVStore.open(configDir.resolve("kvStore").toString()))
        }
        launch(Dispatchers.Default) {
            try {
                startScreen.complete(StartScreen(startTime))
            } catch (e: KotlinNullPointerException) {
                e.printStackTrace()
            }
            log.debug("StartScreen created at {}", startTime.elapsedNow())
        }
    }
    private val appStorage = async<AppStore?>(start = CoroutineStart.LAZY, context = Dispatchers.IO) {
        log.info("loading database")
        try {
            load()
        } catch (e: Exception) {
            launch(Dispatchers.Main) {
                alert(Alert.AlertType.ERROR,
                        "couldn't open configuration directory",
                        "$e")
            }
            log.error("can't load data: $e")
            e.printStackTrace()
            null
        }
    }

    private fun load(): AppStore {
        val (settings, db) = loadSettings(configDir)
        log.debug("Database opened {}", startTime.elapsedNow())
        val proxy = settings.proxyList.default()
        val cacheDir = getHttpCacheDir(configDir.canonicalPath)
        val addTrust = loadOptionalCert(configDir)
        Globals.httpClient = KHttpClient.client.newBuilder()
                .proxy(proxy.toJavaNet())
                .given(cacheDir) { cache(Cache(it, 800*1024*1024))}
                .given(addTrust) {
                    val (s, m) = sslConfFromStream(it)
                    sslSocketFactory(s.socketFactory, m)
                }
                .build()
        val store = AppStore(db, settings)
        appState.store = store
        return store
    }

    override fun stop() {
        super.stop()
        runBlocking {
            appStorage.await()
        }?.database?.run {
            log.info("closing database")
            close()
        }
        val kv = runBlocking { kvStore.await() }
        stage?.let { save_win_geometry(it, kv) }
        kv.close()
    }

    override fun start(stage: Stage) {
        this.stage = stage
        JFX.primaryStage = stage
        launch(Dispatchers.Main) {
            val kvs = kvStore.await()
            setSaneStageSize(stage, kvs)
            log.debug("stage size set at {}", startTime.elapsedNow())
            val scalingPane = ScalingPane()
            JFX.primaryPane = scalingPane
            stage.scene = Scene(scalingPane.root).apply {
                this.fill = Color.WHITE
            }
            log.debug("Set the scene of stage at {}", startTime.elapsedNow())
            stage.show()
            log.debug("Called stage.show at {}", startTime.elapsedNow())
            val map = kvs.openMap<String, String>("strings")
            val acc = map["active-account"]
            if (acc == null || !loadSignedIn(scalingPane, acc, map)) {
                val s = startScreen.await()
                scalingPane.setChild(s.root)
                log.debug("Root of the scene is set at {}", startTime.elapsedNow())
                s.initialize(map)
                appStorage.await()?.let {
                    launch(Dispatchers.Main) {
                        log.debug("Updating UI with data {}", startTime.elapsedNow())
                        startScreen.await().start(it, Globals.httpClient)
                        log.debug("UI updated at {}", startTime.elapsedNow())
                    }
                }
            }
            stage.title = "Continuum"
            javaClass.getResourceAsStream("/icon/koma.png")?.let {
                stage.icons.add(Image(it))
            } ?: log.error("Failed to load app icon from resources")
            log.debug("icon loaded {}", startTime.elapsedNow())
            stage.scene.stylesheets.add("/css/main.css")
        }
        stage.isResizable = true
    }

    private suspend fun loadSignedIn(pane: ScalingPane, user: String, map: MVMap<String, String>): Boolean {
        pane.setChild(Text("Continuum").apply {
            fill = Color.GRAY
            font = Font.font(48.0)
        })
        val store = appStorage.await() ?: return false
        val db = store.database
        val u = UserId(user)
        val a = getServerAddrs(db, u.server).firstOrNull()?: return false
        val s = HttpUrl.parse(a) ?: return false
        val t = getToken(db, u)?: return false
        store.settings
        startChat(Globals.httpClient, u, t, s, map, store)
        return true
    }
}

object Globals {
    internal lateinit var httpClient: OkHttpClient
    internal lateinit var buggyParent: CatchingGroup
}
