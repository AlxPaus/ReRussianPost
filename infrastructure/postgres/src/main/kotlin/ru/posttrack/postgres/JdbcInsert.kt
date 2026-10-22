package ru.posttrack.postgres

import java.sql.Timestamp
import java.time.Instant

internal fun PostgresDatabase.insert(
    table: String,
    values: Map<String, Any?>,
    updateColumns: List<String> = emptyList(),
    conflictColumn: String = "id",
) {
    val columns = values.keys.joinToString()
    val parameters = values.keys.joinToString { "?" }
    val conflict =
        if (updateColumns.isEmpty()) {
            ""
        } else {
            "ON CONFLICT ($conflictColumn) DO UPDATE SET " +
                updateColumns.joinToString { "$it = EXCLUDED.$it" }
        }
    val sql = "INSERT INTO $table ($columns) VALUES ($parameters) $conflict"
    connection().prepareStatement(sql).use { statement ->
        values.values.forEachIndexed { index, value ->
            statement.setObject(index + 1, if (value is Instant) Timestamp.from(value) else value)
        }
        statement.executeUpdate()
    }
}
