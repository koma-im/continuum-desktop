package util

import com.moandjiezana.toml.Toml
import com.moandjiezana.toml.TomlWriter
import koma.matrix.UserId
import koma.matrix.user.identity.UserId_new
import koma.storage.config.config_paths
import koma.storage.save_server_address
import matrix.AuthedUser
import java.io.File
import java.io.IOException

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


fun saveLastUsed(user: UserId, server: String) {
    val authdir = config_paths.profile_dir
    println("saving lastest profile $user, $server in $authdir")
    if (authdir != null) {
        val users = mutableListOf(user)

        val last_used = authdir + File.separator + "last.toml"
        val file = File(last_used)
        if (file.isFile) {
            val tomlread = Toml().read(file)
            val oldusers: List<String>? = tomlread.getList("userids")
            if (oldusers != null) {
                users.addAll(oldusers.map { UserId_new(it) }.filterNotNull())
            }
        }

        val data = mapOf(Pair("userids", users.distinct().take(10).map { it.toString() }))
        val tomlWriter = TomlWriter()
        try {
            tomlWriter.write(data, file)
        } catch (e: IOException) {
            println("failed to save last used profile $data")
        }
    } else {
        println("failed to save latest use")
    }
    save_server_address(user.server, server)
}

fun saveToken(creds: AuthedUser) {
    val userid = creds.user_id
    val token = creds.access_token
    val data = mapOf(Pair("userid", userid.toString()), Pair("token", token))
    val authdir = config_paths.profile_dir
    if (authdir == null) {
        println("failed to create authdir")
        return
    }
    val tokenpath = authdir + File.separator + "${userid.user}.toml"
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


fun getToken(userId: UserId): AuthedUser? {
    val file = config_paths.getCreateProfileDir( userId.server, create = false)
            ?.let { File(it) }
            ?.let { it.resolve("${userId.user}.toml") }
    if (file == null || (!file.isFile())) {
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
