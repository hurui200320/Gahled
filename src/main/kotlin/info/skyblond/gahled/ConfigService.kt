package info.skyblond.gahled

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging
import org.telegram.telegrambots.bots.DefaultBotOptions
import java.io.File
import java.util.*

object ConfigService {
    private val logger = KotlinLogging.logger { }

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
        val properties = Properties(configMap.size)
        file.inputStream().use { properties.load(it) }
        for (propertyName in properties.propertyNames()) {
            if (!configMap.containsKey(propertyName)) {
                logger.warn { "Unknown property: $propertyName" }
            }
            check(propertyName is String) { "Property name is not a string" }
            configMap[propertyName] = properties.getProperty(propertyName)
        }
    }

    private fun writeToFile(file: File) {
        val properties = Properties(configMap.size)
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

    private const val TELEGRAM_CHANNEL_CHAT_ID = "telegram_channel_chat_id"

    fun getTelegramChannelChatId(): Long? {
        val textValue = configMap[TELEGRAM_CHANNEL_CHAT_ID]!!
        return if (textValue.isBlank()) {
            null
        } else {
            textValue.toLongOrNull()
        }
    }

    private const val BOT_START_COLLECTING_CRON = "bot_start_collecting_cron"
    private const val BOT_START_VOTING_CRON = "bot_start_voting_cron"
    private const val BOT_START_PUBLISHING_CRON = "bot_start_publishing_cron"

    fun getBotStartCollectingCron(): String = getConfig(BOT_START_COLLECTING_CRON)
    fun getBotStartVotingCron(): String = getConfig(BOT_START_VOTING_CRON)
    fun getBotStartPublishingCron(): String = getConfig(BOT_START_PUBLISHING_CRON)

    private val configMap = mutableMapOf(
        JDBC_CONNECT_STRING to "",
        JDBC_USERNAME to "",
        JDBC_PASSWORD to "",
        TELEGRAM_BOT_TOKEN to "",
        TELEGRAM_BOT_USERNAME to "",
        TELEGRAM_BOT_PROXY_TYPE to DefaultBotOptions.ProxyType.NO_PROXY.name,
        TELEGRAM_BOT_PROXY_HOST to "",
        TELEGRAM_BOT_PROXY_PORT to "",
        TELEGRAM_CHANNEL_CHAT_ID to "",
        // default to every Monday 00:00:30
        BOT_START_COLLECTING_CRON to "30 0 0 ? * 2 *",
        // default to every Thursday 18:00:00
        BOT_START_VOTING_CRON to "0 0 18 ? * 5 *",
        // default to every Friday 21:30:00
        BOT_START_PUBLISHING_CRON to "0 30 21 ? * 6 *",
    )

}
