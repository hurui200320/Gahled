package info.skyblond.gahled.bot

import info.skyblond.gahled.ConfigService
import mu.KotlinLogging
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.ChatMemberUpdated
import org.telegram.telegrambots.meta.api.objects.Update

class GahledBot(options: DefaultBotOptions) : TelegramLongPollingBot(options) {
    private val logger = KotlinLogging.logger {}

    override fun getBotToken(): String = ConfigService.getTelegramBotToken()

    override fun getBotUsername(): String = ConfigService.getTelegramBotUsername()

    override fun onUpdateReceived(update: Update) {
        println(update)
        if (update.hasMyChatMember()) {
            val myChatMember = update.myChatMember
            if (myChatMember.chat.isChannelChat) {
                // check if is the bot
                logger.info { "Channel: ${myChatMember.chat.title}" }
                val newMember = myChatMember.newChatMember
                if (newMember.user.isBot && newMember.user.userName == botUsername) {
                    when (newMember.status) {
                        "kicked" -> { // remove admin status
                            logger.info { "I'm banned from channel" }
                        }
                        "administrator" -> { // give admin/added to channel
                            logger.info { "I'm joining new Channel!" }
                            SendMessage(myChatMember.chat.id.toString(), "Hello!")
                        }
                        "left" -> { // removed from subscriber
                            logger.info { "I'm kicked from Channel!" }
                        }
                        else -> {
                            logger.info { "Unknown status: ${newMember.status}" }
                        }
                    }
                }
            }
        }
    }

    private fun handleMyChatMember(myChatMember: ChatMemberUpdated) {

    }
}
