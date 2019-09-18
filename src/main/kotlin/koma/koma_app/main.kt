package koma.koma_app

import javafx.application.Application
import javafx.application.HostServices
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.image.Image
import javafx.scene.layout.Pane
import javafx.stage.Stage
import koma.Koma
import koma.gui.save_win_geometry
import koma.gui.setSaneStageSize
import koma.gui.view.window.start.StartScreen
import koma.network.client.okhttp.AppHttpClient
import kotlinx.coroutines.*
import link.continuum.database.KDataStore
import link.continuum.desktop.gui.JFX
import link.continuum.desktop.gui.scene.ScalingPane
import link.continuum.desktop.util.disk.path.getConfigDir
import link.continuum.desktop.util.disk.path.loadOptionalCert
import link.continuum.desktop.util.gui.alert
import okhttp3.OkHttpClient
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
    appState.koma.http.client.run {
        dispatcher().executorService().shutdownNow()
    }
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
    private var dataStore: KDataStore? = null
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
    fun startLoad() {
        launch(Dispatchers.IO) {
            try {
                load()
            } catch (e: Exception) {
                alert(Alert.AlertType.ERROR,
                        "couldn't open configuration directory",
                        "$e")
                log.error("can't load data: $e")
                e.printStackTrace()
            }
        }
    }
    private fun load() {
        val (settings, db) = loadSettings(configDir)
        log.debug("Database opened {}", startTime.elapsedNow())
        dataStore = db
        val proxy = settings.proxyList.default()
        val k= Koma(proxy.toJavaNet(), path = configDir.canonicalPath,
                addTrust = loadOptionalCert(configDir))
        appState.koma = k
        val store = AppStore(db, settings, k)
        appState.store = store
        launch(Dispatchers.Main) {
            log.debug("Updating UI with data {}", startTime.elapsedNow())
            startScreen.await().start(store)
            log.debug("UI updated at {}", startTime.elapsedNow())
        }
    }

    override fun stop() {
        super.stop()
        dataStore?.run {
            this.data.close()
            log.info("closing database")
            close()
        }?: log.error("database not initialized")
        val kv = runBlocking { kvStore.await() }
        stage?.let { save_win_geometry(it, kv) }
        kv.close()
    }

    override fun start(stage: Stage) {
        this.stage = stage
        JFX.primaryStage = stage
        launch(Dispatchers.Main) {
            setSaneStageSize(stage, kvStore.await())
            log.debug("stage size set at {}", startTime.elapsedNow())
            val scalingPane = ScalingPane()
            JFX.primaryPane = scalingPane
            stage.scene = Scene(scalingPane.root)
            log.debug("Set the scene of stage at {}", startTime.elapsedNow())
            stage.show()
            log.debug("Called stage.show at {}", startTime.elapsedNow())
            stage.title = "Continuum"
            val s = startScreen.await()
            scalingPane.setChild(s.root)
            log.debug("Root of the scene is set at {}", startTime.elapsedNow())
            s.initialize()
            javaClass.getResourceAsStream("/icon/koma.png")?.let {
                stage.icons.add(Image(it))
            } ?: log.error("Failed to load app icon from resources")
            log.debug("icon loaded {}", startTime.elapsedNow())
            startLoad()
            stage.scene.stylesheets.add("/css/main.css")
        }
        stage.isResizable = true
    }

}
