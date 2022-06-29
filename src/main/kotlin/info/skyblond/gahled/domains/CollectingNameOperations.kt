package info.skyblond.gahled.domains

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object CollectingNameOperations {
    fun getUserNamedMovie(userId: Long): String? = transaction {
        val currentVersion = KeyValueOperations.getCollectingCurrentVersion()!!
        CollectingNames
            .select { (CollectingNames.userId eq userId) and (CollectingNames.version eq currentVersion) }
            .singleOrNull()
            ?.get(CollectingNames.movie)
    }

    fun getUserNamedMovieNote(userId: Long): String? = transaction {
        val currentVersion = KeyValueOperations.getCollectingCurrentVersion()!!
        CollectingNames
            .select { (CollectingNames.userId eq userId) and (CollectingNames.version eq currentVersion) }
            .singleOrNull()
            ?.get(CollectingNames.note)
    }

    fun getNamedMovies(): List<String> = transaction {
        val currentVersion = KeyValueOperations.getCollectingCurrentVersion()!!
        CollectingNames
            .select { CollectingNames.version eq currentVersion }
            .mapNotNull { it[CollectingNames.movie] }
    }

    fun removeUserNaming(userId: Long) {
        transaction {
            CollectingNames.deleteWhere { CollectingNames.userId eq userId }
        }
    }

    fun updateUserVersion(userId: Long): Boolean {
        val currentVersion = KeyValueOperations.getCollectingCurrentVersion()!!
        return transaction {
            if (CollectingNames.select { CollectingNames.version eq currentVersion }.count() >= 10) {
                return@transaction false
            }

            val userVersion = CollectingNames
                .select { CollectingNames.userId eq userId }
                .singleOrNull()?.get(CollectingNames.version)
            if (userVersion == null) {
                CollectingNames.insert {
                    it[CollectingNames.userId] = userId
                    it[version] = currentVersion
                    it[movie] = null
                    it[note] = null
                }
            } else {
                CollectingNames.update({ (CollectingNames.userId eq userId) and (CollectingNames.version neq currentVersion) }) {
                    it[version] = currentVersion
                    it[movie] = null
                    it[note] = null
                }
            }
            true
        }
    }

    fun setUserNamedMovie(userId: Long, name: String) {
        transaction {
            CollectingNames.update({ CollectingNames.userId eq userId }) {
                it[movie] = name
            }
        }
    }

    fun setUserNamedMovieNote(userId: Long, note: String) {
        transaction {
            CollectingNames.update({ CollectingNames.userId eq userId }) {
                it[CollectingNames.note] = note
            }
        }
    }

    /**
     * Movie name, recommendation note, userId
     * */
    fun getNamedMoviesWithNoteAndUserId(): List<Triple<String, String?, Long>> = transaction {
        val currentVersion = KeyValueOperations.getCollectingCurrentVersion()!!
        CollectingNames
            .select { (CollectingNames.version eq currentVersion) and (CollectingNames.movie neq null) }
            .mapNotNull {
                Triple(
                    it[CollectingNames.movie]!!,
                    it[CollectingNames.note],
                    it[CollectingNames.userId],
                )
            }
    }
}
