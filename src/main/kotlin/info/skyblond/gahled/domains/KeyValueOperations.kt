package info.skyblond.gahled.domains

import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

object KeyValueOperations {
    private fun getKeyValue(key: String): String? = transaction {
        KeyValues.select { KeyValues.key eq key }.singleOrNull()
    }?.get(KeyValues.value)

    private fun setKeyValue(key: String, value: String) {
        transaction {
            if (getKeyValue(key) == null) {
                KeyValues.insert {
                    it[KeyValues.key] = key
                    it[KeyValues.value] = value
                }
            } else {
                KeyValues.update({ KeyValues.key eq key }) {
                    it[KeyValues.value] = value
                }
            }
        }
    }

    enum class State {
        COLLECTING, VOTING, READY
    }

    private const val CURRENT_STATE_KEY = "app.current-state"

    fun getCurrentState(): State {
        val valueText = getKeyValue(CURRENT_STATE_KEY) ?: ""
        if (valueText.isBlank()) {
            return State.READY
        }
        return State.valueOf(valueText)
    }

    fun setCurrentState(state: State) {
        setKeyValue(CURRENT_STATE_KEY, state.name)
    }


    private const val COLLECTING_CURRENT_VERSION = "app.collecting.version"

    fun getCollectingCurrentVersion(): Long? = getKeyValue(COLLECTING_CURRENT_VERSION)?.toLongOrNull()

    fun setCollectingCurrentVersion(version: Long) = setKeyValue(COLLECTING_CURRENT_VERSION, version.toString())


    private const val VOTE_REF_MESSAGE_ID = "app.vote.messageId"

    fun getVoteRefMessageId(): Int? = getKeyValue(VOTE_REF_MESSAGE_ID)?.toIntOrNull()

    fun setVoteRefMessageId(messageId: Int) = setKeyValue(VOTE_REF_MESSAGE_ID, messageId.toString())


    private fun getVotePollMessageIdKeyByIndex(index: Int): String = "app.vote.poll_${index}.messageId"

    fun getVotePollMessageIdByIndex(index: Int): Int? =
        getKeyValue(getVotePollMessageIdKeyByIndex(index))?.toIntOrNull()

    fun setVotePollMessageIdByIndex(index: Int, messageId: Int) =
        setKeyValue(getVotePollMessageIdKeyByIndex(index), messageId.toString())
}
