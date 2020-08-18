package koma.koma_app

import io.requery.Persistable
import io.requery.sql.KotlinEntityDataStore
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.stage.Stage
import koma.gui.setSaneStageSize
import koma.gui.view.window.start.StartScreen
import koma.matrix.UserId
import koma.network.client.okhttp.KHttpClient
import koma.storage.config.server.cert_trust.sslConfFromStream
import koma.storage.persistence.settings.AppSettings
import koma.util.given
import kotlinx.coroutines.*
import link.continuum.database.loadDesktopDatabase
import link.continuum.desktop.action.startChat
import link.continuum.desktop.database.KeyValueStore
import link.continuum.desktop.gui.CatchingGroup
import link.continuum.desktop.gui.JFX
import link.continuum.desktop.gui.scene.ScalingPane
import link.continuum.desktop.util.disk.path.getConfigDir
import link.continuum.desktop.util.disk.path.loadOptionalCert
import link.continuum.desktop.util.gui.alert
import okhttp3.Cache

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import org.h2.mvstore.MVStore
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.invoke.MethodHandles
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource.Monotonic

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
    appState.job.cancel()
    // should work because instances all share the same dispatcher
    KHttpClient.client.dispatcher.executorService.shutdownNow()
}


@ExperimentalTime
@ExperimentalCoroutinesApi
class KomaApp : Application(), CoroutineScope by CoroutineScope(Dispatchers.Default) {
    private val log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    private var stage: Stage? = null
    private val startScreen = CompletableDeferred<StartScreen>()
    private val configDir: File
    private val keyValueStore = CompletableDeferred<KeyValueStore>()
    private val database = CompletableDeferred<KotlinEntityDataStore<Persistable>>()
    private val httpClient = CompletableDeferred<OkHttpClient>()
    private val appData = CompletableDeferred<AppData>()
    val startTime = Monotonic.markNow()
    init {
        JFX.application = this
        val arg = System.getenv()["CONTINUUM_DIR"]
        val data_dir = arg ?: getConfigDir()
        log.info("data dir set to {}", data_dir)
        configDir = File(data_dir)
        configDir.mkdirs()
        Thread.setDefaultUncaughtExceptionHandler(NoAlertErrorHandler())
        launch(Dispatchers.IO) {
            try {
                val mv = MVStore.open(configDir.resolve("kvStore").toString())
                val kv = KeyValueStore(mv)
                log.debug("KeyValueStore opened at {}", startTime.elapsedNow())
                keyValueStore.complete(kv)
                val db = loadDesktopDatabase(configDir)
                log.debug("Database opened {}", startTime.elapsedNow())
                database.complete(db)
                appData.complete(load(db, kv))
            }  catch (e: Exception) {
                launch(Dispatchers.Main) {
                    alert(Alert.AlertType.ERROR,
                            "couldn't open configuration directory",
                            "$e")
                }
                log.error("can't load data: $e")
                e.printStackTrace()
            }
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

    private fun load(db: KotlinEntityDataStore<Persistable>,
                     kvStore: KeyValueStore): AppData {
        val settings = AppSettings()
        val proxy = kvStore.proxyList.default()
        val cacheDir = getHttpCacheDir(configDir.canonicalPath)
        val addTrust = loadOptionalCert(configDir)
        val client = KHttpClient.client.newBuilder()
                .proxy(proxy)
                .given(cacheDir) { cache(Cache(it, 800*1024*1024))}
                .given(addTrust) {
                    val (s, m) = sslConfFromStream(it)
                    sslSocketFactory(s.socketFactory, m)
                }
                .build()
        Globals.httpClient = client
        httpClient.complete(client)
        val store = AppStore(db, kvStore, settings)
        appState.store = store
        return store
    }

    override fun stop() {
        super.stop()
        if (appData.isCompleted && !appData.isCancelled) {
            log.info("closing database")
            appData.getCompleted().database.close()
        }
        if (keyValueStore.isCompleted && !keyValueStore.isCancelled) {
            val kv =  keyValueStore.getCompleted()
            stage?.let {
                kv.saveStageSize(it)
            }
            kv.close()
        }
    }

    override fun start(stage: Stage) {
        this.stage = stage
        JFX.primaryStage = stage
        launch(Dispatchers.Main) {
            val keyValueStore = keyValueStore.await()
            setSaneStageSize(stage, keyValueStore)
            log.debug("stage size set at {}", startTime.elapsedNow())
            val scalingPane = ScalingPane()
            JFX.primaryPane = scalingPane
            stage.scene = Scene(scalingPane.root).apply {
                this.fill = Color.WHITE
            }
            log.debug("Set the scene of stage at {}", startTime.elapsedNow())
            stage.show()
            log.debug("Called stage.show at {}", startTime.elapsedNow())
            val acc = keyValueStore.activeAccount.getOrNull()
            if (acc == null || !loadSignedIn(scalingPane, acc, keyValueStore)) {
                val s = startScreen.await()
                scalingPane.setChild(s.root)
                log.debug("Root of the scene is set at {}", startTime.elapsedNow())
                s.initialize(keyValueStore, appData)
                launch(Dispatchers.Main) {
                    log.debug("Updating UI with data {}", startTime.elapsedNow())
                    startScreen.await().login.await().start(httpClient.await())
                    log.debug("UI updated at {}", startTime.elapsedNow())
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

    private suspend fun loadSignedIn(pane: ScalingPane, user: String, kvs: KeyValueStore): Boolean {
        pane.setChild(Text("Continuum").apply {
            fill = Color.GRAY
            font = Font.font(48.0)
        })
        val u = UserId(user)
        val a = kvs.serverToAddress.get(u.server) ?: return false
        val s = a.toHttpUrlOrNull() ?: return false
        val t =kvs.userToToken.get(u.full)?: return false
        startChat(httpClient.await(), u, t, s, kvs, appData)
        return true
    }
}

object Globals {
    internal lateinit var httpClient: OkHttpClient
    internal lateinit var buggyParent: CatchingGroup
}

private fun getHttpCacheDir(dir: String): File? {
    val p = File(dir).resolve("koma").resolve("cache").resolve("http")
    return if (p.exists()) {
        if (p.isDirectory) p else null
    } else if(p.mkdirs()) {
        p
    } else null
}