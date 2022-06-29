package info.skyblond.gahled.bot

import info.skyblond.gahled.ConfigService
import info.skyblond.gahled.domains.CollectingNameOperations
import info.skyblond.gahled.domains.KeyValueOperations
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll
import org.telegram.telegrambots.meta.api.methods.polls.StopPoll
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

class GahledBot(options: DefaultBotOptions) : AbstractBot(options, "GahledBot") {
    override fun onUpdateReceived(update: Update) {
        try {
            if (update.hasMessage()) {
                val message = update.message
                if (shouldRejectMessage(message)) {
                    // reject if user is not a member
                    execute(
                        SendMessage.builder()
                            .chatId(message.chatId.toString())
                            .parseMode("HTML")
                            .text("<b><i>无可奉告！</i></b>")
                            .build()
                    )
                    return
                }
                if (message.hasText() && message.text == "/start") {
                    // handle start command
                    sendStartMessage(message)
                } else {
                    // handle other message
                    handleMessage(message)
                }
            }
            if (update.hasCallbackQuery()) {
                // handle callback
                handleCallbackQuery(update.callbackQuery)
            }
        } catch (t: Throwable) {
            // eat unhandled exceptions
            // keeps the bot running
            logger.error(t) { "Eat unhandled throwable when processing update" }
        }
    }

    private val acceptableMemberStatus = listOf(
        "creator", "administrator", "member"
    )

    private fun getUserIdFromMessage(message: Message): Long? = message.from?.id

    /**
     * Check if user is a member of the channel.
     * By "is a member of", it means the user must be owner, admin or member of the channel.
     * */
    private fun shouldRejectMessage(message: Message): Boolean {
        val userId = getUserIdFromMessage(message) ?: return true
        val request = GetChatMember.builder()
            .chatId(ConfigService.getTelegramChannelChatId()!!.toString())
            .userId(userId)
            .build()
        val member = execute(request)
        return member.status !in acceptableMemberStatus
    }

    /**
     * Map<UserId, State>.
     * Current available state:
     * + name -> the next message from user is a name
     * + note -> the next message from user is a recommendation note
     * */
    private val collectStateMachine = ConcurrentHashMap<Long, String>()

    private fun sendInvalidNotice(chatId: Long) {
        execute(
            SendMessage.builder()
                .chatId(chatId.toString())
                .text("请求无效，请使用 /start 查看当前可用的操作")
                .build()
        )
    }

    private fun sendSimpleMessage(chatId: Long, text: String): Int {
        return execute(SendMessage.builder().chatId(chatId.toString()).text(text).build()).messageId
    }

    /**
     * Handle user's incoming message based on the internal state machine.
     * */
    private fun handleMessage(message: Message) {
        val chatId = message.chatId
        val userId = getUserIdFromMessage(message)
        if (userId == null) {
            // reject account don't have a user id
            sendSimpleMessage(chatId, "无法获取您的User ID。")
            return
        }

        when (KeyValueOperations.getCurrentState()) {
            KeyValueOperations.State.COLLECTING -> {
                val state = collectStateMachine[userId]
                if (state == null) {
                    sendInvalidNotice(chatId)
                    return
                }
                if (!message.hasText()) {
                    sendSimpleMessage(chatId, "消息中没有文字内容，请重试")
                    return
                }
                collectStateMachine.remove(userId)
                when (state) {
                    "name" -> {
                        if (CollectingNameOperations.updateUserVersion(userId)) {
                            CollectingNameOperations.setUserNamedMovie(userId, message.text)
                            sendSimpleMessage(chatId, "提名成功，请使用 /start 查看最新状态")
                        } else {
                            sendSimpleMessage(chatId, "提名失败，可能是名额不足，请使用 /start 查看最新状态")
                        }
                    }
                    "note" -> {
                        CollectingNameOperations.setUserNamedMovieNote(userId, message.text)
                        sendSimpleMessage(chatId, "保存成功，请使用 /start 查看最新状态")
                    }
                    else -> sendInvalidNotice(chatId)
                }
            }
            else -> sendInvalidNotice(chatId)
        }
    }

    /**
     * Update the internal state machine, based on user's callback (buttons)
     * */
    private fun handleCallbackQuery(callbackQuery: CallbackQuery) {
        // the original chat id, where the bot send the buttons
        val chatId = callbackQuery.message.chatId
        // check who clicked the button
        val userId = callbackQuery.from?.id
        if (userId == null) {
            sendSimpleMessage(chatId, "无法获取您的User ID。")
            return
        }
        val data = callbackQuery.data.split(",")
        val userIdFromCallback = data[0]
        if (userId.toString() != userIdFromCallback) {
            // wrong user clicked the button
            sendInvalidNotice(chatId)
            return
        }
        when (data[1]) {
            "collect" -> {
                if (KeyValueOperations.getCurrentState() != KeyValueOperations.State.COLLECTING) {
                    sendInvalidNotice(chatId)
                    return
                }
                val versionFromCallback = data[2]
                val currentVersion = KeyValueOperations.getCollectingCurrentVersion()!!
                if (currentVersion.toString() != versionFromCallback) {
                    sendInvalidNotice(chatId)
                    return
                }
                when (data[3]) {
                    "name" -> {
                        if (CollectingNameOperations.getUserNamedMovie(userId) != null) {
                            sendSimpleMessage(chatId, "您已经提名过电影了，请使用 /start 查看最新的选项")
                        } else {
                            collectStateMachine[userId] = "name"
                            sendSimpleMessage(chatId, "请发送要提名的电影名")
                        }
                    }
                    "note" -> {
                        if (CollectingNameOperations.getUserNamedMovie(userId) == null) {
                            sendSimpleMessage(chatId, "您还没提名电影，请使用 /start 查看最新的选项")
                        } else {
                            collectStateMachine[userId] = "note"
                            sendSimpleMessage(chatId, "请发送您的推荐语")
                        }
                    }
                    "cancel" -> {
                        if (CollectingNameOperations.getUserNamedMovie(userId) == null) {
                            sendSimpleMessage(chatId, "您还没提名电影，请使用 /start 查看最新的选项")
                        } else {
                            CollectingNameOperations.removeUserNaming(userId)
                            sendSimpleMessage(chatId, "您的推荐已撤销，请使用 /start 查看最新的选项")
                        }
                    }
                    else -> sendInvalidNotice(chatId)
                }
            }
            else -> sendInvalidNotice(chatId)
        }
    }

    private fun sendStartMessage(message: Message) {
        val userId = getUserIdFromMessage(message)
        if (userId == null) {
            sendSimpleMessage(message.chatId, "无法获取您的User ID。")
            return
        }
        when (KeyValueOperations.getCurrentState()) {
            KeyValueOperations.State.COLLECTING -> {
                // clear state when user called start command
                collectStateMachine.remove(userId)
                // fetch data
                val namedMovie = CollectingNameOperations.getUserNamedMovie(userId)
                val namedMovieNote = CollectingNameOperations.getUserNamedMovieNote(userId)
                val namedMovies = CollectingNameOperations.getNamedMovies()
                // prepare text and button
                val buttonList = mutableListOf<InlineKeyboardButton>()
                val currentVersion = KeyValueOperations.getCollectingCurrentVersion()
                // not named and can name
                if (namedMovie == null && namedMovies.size < 10) {
                    buttonList.add(
                        InlineKeyboardButton.builder()
                            .text("提名电影")
                            .callbackData("${userId},collect,${currentVersion},name")
                            .build()
                    )
                }
                // named
                if (namedMovie != null) {
                    buttonList.add(
                        InlineKeyboardButton.builder()
                            .text("更新推荐语")
                            .callbackData("${userId},collect,${currentVersion},note")
                            .build()
                    )
                    buttonList.add(
                        InlineKeyboardButton.builder()
                            .text("取消推荐")
                            .callbackData("${userId},collect,${currentVersion},cancel")
                            .build()
                    )
                }

                val text = "目前bot正在接受电影提名。\n" +
                        "当前已提名${namedMovies.size}部电影，总名额10部，每人限提名一部电影，先到先得\n" +
                        (if (namedMovies.isNotEmpty()) "\n本次已提名的电影有：\n"
                                + namedMovies.joinToString("\n") else "本次还没有已提名的电影") +
                        "\n\n" +
                        (namedMovie?.let { "您本次提名的电影是：${it}\n" } ?: "您本次还没有提名电影\n") +
                        (namedMovieNote?.let { "您本次提名的推荐语是：${it}\n" } ?: "您本次还没有填写推荐语\n") +
                        if (buttonList.isNotEmpty()) "\n请选择您需要的操作：" else "暂无可用操作"

                val sendMessage = SendMessage.builder()
                    .chatId(message.chatId.toString())
                    .text(text)
                    .replyMarkup(
                        InlineKeyboardMarkup.builder()
                            .also { builder ->
                                buttonList.forEach {
                                    builder.keyboardRow(listOf(it))
                                }
                            }.build()
                    )
                    .build()

                execute(sendMessage)
            }
            KeyValueOperations.State.VOTING -> {
                execute(
                    SendMessage.builder()
                        .chatId(message.chatId.toString())
                        .parseMode("HTML")
                        .text(
                            "请在<a href=\"https://t.me/c/${
                                ConfigService.getTelegramChannelChatId().toString().removePrefix("-100")
                            }/${
                                KeyValueOperations.getVoteRefMessageId()!!
                            }\">此消息</a>中浏览本次投票的详细说明。"
                        )
                        .build()
                )
            }
            KeyValueOperations.State.READY -> {
                sendSimpleMessage(message.chatId, "暂无操作可用，请等待Bot在频道中发送通知")
            }
        }
    }

    /**
     * Start collecting names.
     * */
    fun startCollecting() {
        collectStateMachine.clear()
        val text = "周末观影提名#${KeyValueOperations.getCollectingCurrentVersion()} 已开始。\n" +
                "请私聊 @${botUsername} 进行提名（使用 /start 指令发起会话）。"

        sendSimpleMessage(ConfigService.getTelegramChannelChatId()!!, text)
    }

    /**
     * Stop collecting and start polling.
     * */
    fun startPoll() {
        val channelId = ConfigService.getTelegramChannelChatId()!!
        val movieList = CollectingNameOperations.getNamedMoviesWithNoteAndUserId()
        if (movieList.isEmpty()) {
            // no movie is named
            sendSimpleMessage(channelId, "本周没有电影入选，投票环节自动跳过")
            return
        } else {
            KeyValueOperations.setVoteRefMessageId(
                sendSimpleMessage(
                    channelId,
                    "本周提名${movieList.size}部电影，接下来将依次介绍各提名影片。" +
                            "最后的投票环节请大家依次选出自己最想看的电影。"
                )
            )
        }
        // send movie recommendation
        movieList.forEach { (movieName, note, userId) ->
            val user = execute(GetChatMember.builder().chatId(channelId.toString()).userId(userId).build())
            val text = "${movieName}\n" +
                    "由 ${user.user.firstName} ${user.user.lastName ?: ""} 推荐。\n\n" +
                    "推荐理由：" + note + "\n"

            sendSimpleMessage(channelId, text)
            try { // in case rate limit
                Thread.sleep(1000)
            } catch (_: Throwable) {
            }
        }

        if (movieList.size == 1) {
            sendSimpleMessage(channelId, "本次提名只有一部影片，投票环节自动跳过")
        } else {
            val choiceSet = movieList.map { it.first }.distinct()
            val maxVote = min(choiceSet.size, 3)
            for (i in 1..maxVote) {
                val poll = SendPoll.builder()
                    .chatId(channelId.toString())
                    .allowMultipleAnswers(false)
                    .isAnonymous(true)
                    .question("请选择您第${i}想看的电影：")
                    .options(choiceSet)
                    .build()
                KeyValueOperations.setVotePollMessageIdByIndex(i, execute(poll).messageId)
            }
        }
    }

    /**
     * Stop polling and publish the result.
     * */
    fun stopPoll() {
        val channelId = ConfigService.getTelegramChannelChatId()!!
        val movieList = CollectingNameOperations.getNamedMovies().distinct()

        if (movieList.isEmpty()) {
            // no movie is named
            sendSimpleMessage(channelId, "本周没有电影被提名，因此也没有影片被选中")
            return
        }
        if (movieList.size == 1) {
            sendSimpleMessage(channelId, "本次提名只有一部影片，因此自动选择该部影片：${movieList[0]}")
        } else {
            val maxVote = min(movieList.size, 3)
            val hashMap = mutableMapOf<String, Double>()

            for (i in 1..maxVote) {
                val stopPoll = StopPoll.builder()
                    .chatId(channelId.toString())
                    .messageId(KeyValueOperations.getVotePollMessageIdByIndex(i)!!)
                    .build()
                val poll = execute(stopPoll)
                val point = if (i == 1) 2.5 else if (i == 2) 2.0 else 1.0
                poll.options.forEach {
                    hashMap[it.text] = (hashMap[it.text] ?: 0.0) + it.voterCount * point
                }
            }

            val highScore = hashMap.values.max()
            val choices = hashMap.filter { (_, score) -> score == highScore }.keys

            val text = "投票结果如下：\n\n" +
                    hashMap.map { (name, score) -> "${name}：${String.format("%.1f", score)}" }.joinToString("\n") +
                    "\n\n最高分数为${highScore}，被选中的影片是：\n" +
                    choices.joinToString("\n") + "\n"

            sendSimpleMessage(channelId, text)
        }
    }
}
