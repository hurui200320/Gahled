package info.skyblond.gahled.bot

import info.skyblond.gahled.ConfigService
import mu.KotlinLogging
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.bots.TelegramLongPollingBot

abstract class AbstractBot(options: DefaultBotOptions, loggerName: String? = null) : TelegramLongPollingBot(options) {
    protected val logger = loggerName?.let { KotlinLogging.logger(it) } ?: KotlinLogging.logger {}

    override fun getBotToken(): String = ConfigService.getTelegramBotToken()

    override fun getBotUsername(): String = ConfigService.getTelegramBotUsername()
}
