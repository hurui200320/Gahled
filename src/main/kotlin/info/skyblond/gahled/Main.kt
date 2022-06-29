package info.skyblond.gahled

import info.skyblond.gahled.bot.GahledBot
import info.skyblond.gahled.bot.GetIdBot
import info.skyblond.gahled.domains.createTablesIfNotExists
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.quartz.impl.StdSchedulerFactory
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
    logger.info { "Initializing tables..." }
    createTablesIfNotExists()

    logger.info { "Setting up telegram bot" }

    val telegramBotsApi = TelegramBotsApi(DefaultBotSession::class.java)
    val botOptions = DefaultBotOptions().also {
        ConfigService.setupTelegramBotOption(it)
    }


    val bot = ConfigService.getTelegramChannelChatId().let { id ->
        if (id == null) {
            logger.warn { "Channel chat id not configured, switch to get id bot" }
            GetIdBot(botOptions)
        } else {
            logger.info { "Using channel chat id: $id" }
            GahledBot(botOptions).also { StatusService.gahledBot = it }
        }
    }
    telegramBotsApi.registerBot(bot)

    logger.info { "Bot started!" }

    if (StatusService.gahledBot != null) {
        logger.info { "Setting up cron jobs" }
        StatusService.scheduler = StdSchedulerFactory.getDefaultScheduler()

        StatusService.scheduler!!.also {
            setUpStartCollectingJob(it)
            setUpStartVotingJob(it)
            setUpStartPublishingJob(it)
        }

        StatusService.scheduler!!.start()
        logger.info { "Cron jobs done" }
    }

}
