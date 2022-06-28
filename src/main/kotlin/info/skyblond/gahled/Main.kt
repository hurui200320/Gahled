package info.skyblond.gahled

import info.skyblond.gahled.bot.GahledBot
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

fun main(args: Array<String>) {
    val logger = KotlinLogging.logger("Application")

    logger.info { "Reading config properties..." }

    if (args.isNotEmpty()) {
        ConfigService.setConfigPath(args[0])
    } else {
        ConfigService.setConfigPath("./config.properties")
    }

    logger.info { "Using bot: ${ConfigService.getTelegramBotUsername()}" }

    val datasource = ConfigService.getJdbcDataSource()
    logger.info { "Connecting to database: ${datasource.jdbcUrl}" }
    Database.connect(datasource)

    logger.info { "Setting up telegram bot" }

    val telegramBotsApi = TelegramBotsApi(DefaultBotSession::class.java)
    val botOptions = DefaultBotOptions().also {
        ConfigService.setupTelegramBotOption(it)
    }

    val gahledBot = GahledBot(botOptions)
    telegramBotsApi.registerBot(gahledBot)

    logger.info { "Bot started!" }
}
