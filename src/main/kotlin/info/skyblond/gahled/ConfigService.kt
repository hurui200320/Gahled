package info.skyblond.gahled

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.telegram.telegrambots.bots.DefaultBotOptions
import java.io.File
import java.net.Proxy
import java.util.*

object ConfigService {
    fun setConfigPath(path: String) {
        val file = File(path)
        if (file.exists()) {
            require(!file.isDirectory) { "Config file is a directory: ${file.absolutePath}" }
            require(file.canRead()) { "Config file cannot be read: ${file.absolutePath}" }
            loadFromFile(file)
        }
        writeToFile(file)
    }

    private fun loadFromFile(file: File) {
        val properties = Properties(listOfConfigs.size)
        file.inputStream().use { properties.load(it) }
        listOfConfigs.forEach {
            if (properties.containsKey(it)) {
                configMap[it] = properties.getProperty(it)
            }
        }
    }

    private fun writeToFile(file: File) {
        val properties = Properties(listOfConfigs.size)
        properties.putAll(configMap)
        file.outputStream().use { properties.store(it, "Gahled config") }
    }

    private fun getConfig(key: String): String = configMap[key]
        ?: throw IllegalStateException("Requested key is not valid: $key")


    private const val JDBC_CONNECT_STRING = "jdbc_connect_string"
    private const val JDBC_USERNAME = "jdbc_username"
    private const val JDBC_PASSWORD = "jdbc_password"

    fun getJdbcDataSource(): HikariDataSource {
        val config = HikariConfig()
        config.jdbcUrl = getConfig(JDBC_CONNECT_STRING)
        config.username = getConfig(JDBC_USERNAME)
        config.password = getConfig(JDBC_PASSWORD)
        return HikariDataSource(config)
    }

    private const val TELEGRAM_BOT_TOKEN = "telegram_bot_token"

    fun getTelegramBotToken(): String = getConfig(TELEGRAM_BOT_TOKEN)

    private const val TELEGRAM_BOT_USERNAME = "telegram_bot_username"

    fun getTelegramBotUsername(): String = getConfig(TELEGRAM_BOT_USERNAME)

    private const val TELEGRAM_BOT_PROXY_TYPE = "telegram_bot_proxy_type"
    private const val TELEGRAM_BOT_PROXY_HOST = "telegram_bot_proxy_host"
    private const val TELEGRAM_BOT_PROXY_PORT = "telegram_bot_proxy_port"

    fun setupTelegramBotOption(option: DefaultBotOptions) {
        val proxyType = DefaultBotOptions.ProxyType.valueOf(getConfig(TELEGRAM_BOT_PROXY_TYPE))
        option.proxyType = proxyType
        if (proxyType != DefaultBotOptions.ProxyType.NO_PROXY) {
            option.proxyHost = getConfig(TELEGRAM_BOT_PROXY_HOST)
            option.proxyPort = getConfig(TELEGRAM_BOT_PROXY_PORT).toInt()
        }
    }

    private val listOfConfigs = listOf(
        JDBC_CONNECT_STRING, JDBC_USERNAME, JDBC_USERNAME,
        TELEGRAM_BOT_TOKEN, TELEGRAM_BOT_USERNAME,
        TELEGRAM_BOT_PROXY_TYPE,TELEGRAM_BOT_PROXY_HOST,TELEGRAM_BOT_PROXY_PORT
    )

    private val configMap = mutableMapOf(
        JDBC_CONNECT_STRING to "",
        JDBC_USERNAME to "",
        JDBC_PASSWORD to "",
        TELEGRAM_BOT_TOKEN to "",
        TELEGRAM_BOT_USERNAME to "",
        TELEGRAM_BOT_PROXY_TYPE to DefaultBotOptions.ProxyType.NO_PROXY.name,
        TELEGRAM_BOT_PROXY_HOST to "",
        TELEGRAM_BOT_PROXY_PORT to ""
    )

}
