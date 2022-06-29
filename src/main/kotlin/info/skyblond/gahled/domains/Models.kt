package info.skyblond.gahled.domains

import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction

fun createTablesIfNotExists() {
    transaction {
        // create if now exists
        SchemaUtils.create(KeyValues, CollectingNames)
    }
}

object KeyValues : Table("key_value_table") {
    val key = varchar("key", 255)
    val value = text("value")

    override val primaryKey = PrimaryKey(key)
}

object CollectingNames : Table("collecting_name_table") {
    val userId = long("user_id")
    val version = long("version")
    val movie = text("movie").nullable()
    val note = text("note").nullable()

    override val primaryKey = PrimaryKey(userId)
}
