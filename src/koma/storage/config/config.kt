package util

import com.moandjiezana.toml.Toml
import com.moandjiezana.toml.TomlWriter
import koma.matrix.UserId
import koma.matrix.user.identity.UserId_new
import koma.storage.config.config_paths
import matrix.AuthedUser
import java.io.File

/**
 * Created by developer on 2017/7/8.
 */
fun getRecentUsers(): List<UserId> {
    val users: MutableList<UserId> = mutableListOf()
    val authdir = config_paths.profile_dir
    if (authdir == null) {
        return listOf()
    }
    val last_used = authdir + File.separator + "last.toml"
    val file = File(last_used)
    if (file.isFile()) {
        val toml = Toml().read(file)
        val userids: List<String>? = toml.getList("userids")
        if (userids != null)
            users.addAll(userids.map { UserId_new(it) }.filterNotNull())
    }
    return users
}

fun saveToken(userid: UserId, token: String, dir: String) {
    val data = mapOf(Pair("userid", userid.toString()), Pair("token", token))
    val tokenpath = dir + File.separator + "${userid.user}.toml"
    try {
        val tokenfile = File(tokenpath)
        val tomlWriter = TomlWriter()
        tomlWriter.write(data, tokenfile)
        println("saved token $data in $tokenpath")
    } catch (e: java.io.FileNotFoundException) {
        e.printStackTrace()
        println("failed to save token")
    }
}

fun getConfigDir(): String {
    val env = System.getenv()
    val config_home: String = env.get("XDG_CONFIG_HOME") ?: (System.getProperty("user.home") + File.separator + ".config")
    val config_dir = config_home + File.separator + "koma"
    val dir: File = File(config_dir)
    if (!dir.isDirectory()) {
        dir.mkdir()
    }
    return config_dir
}


fun getToken(userId: UserId, dir: String): AuthedUser? {
    val file = File(dir).resolve("${userId.user}.toml")
    if (!file.isFile()) {
        return null
    }
    val toml = Toml().read(file)
    val userid = UserId_new(toml.getString("userid"))
    val token = toml.getString("token")
    println("loaded user $userid token $token")
    if (userid == null)
        return null
    else
        return AuthedUser(token, userid)
}
