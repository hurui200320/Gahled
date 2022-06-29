package info.skyblond.gahled.bot

import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update

class GetIdBot(options: DefaultBotOptions) : AbstractBot(options, "GetIdBot") {

    override fun onUpdateReceived(update: Update) {
        if (update.hasMessage()) {
            val message = update.message
            val messageId = message.messageId
            val directChatId = message.chatId
            val originalChatId = message.forwardFromChat?.id

            val text = "<i>Your</i> chat id is: <b>${directChatId}</b>\n" +
                    (originalChatId?.let { "<i>Your forwarded message's</i> chat id is: <b>${originalChatId}</b>\n" }
                        ?: "")

            val replyMessage = SendMessage.builder()
                .chatId(directChatId.toString())
                .replyToMessageId(messageId)
                .parseMode("HTML")
                .text(text)
                .build()
            execute(replyMessage)
        }
    }
}
