package ru.posttrack.postgres

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import java.sql.SQLException

fun interface DatabaseHealth {
    fun isReady(): Boolean
}

class PostgresDatabase private constructor(
    private val dataSource: HikariDataSource,
) : DatabaseHealth,
    AutoCloseable {
    fun migrate() {
        Flyway
            .configure()
            .dataSource(dataSource)
            .schemas(DATABASE_SCHEMA)
            .defaultSchema(DATABASE_SCHEMA)
            .createSchemas(true)
            .locations("classpath:db/migration")
            .load()
            .migrate()
    }

    override fun isReady(): Boolean =
        try {
            dataSource.connection.use { connection -> connection.isValid(CONNECTION_VALIDATION_TIMEOUT_SECONDS) }
        } catch (_: SQLException) {
            false
        }

    override fun close() {
        dataSource.close()
    }

    companion object {
        private const val DATABASE_SCHEMA = "posttrack"
        private const val CONNECTION_VALIDATION_TIMEOUT_SECONDS = 2

        fun connect(config: DatabaseConfig): PostgresDatabase {
            val hikariConfig =
                HikariConfig().apply {
                    jdbcUrl = config.jdbcUrl
                    username = config.username
                    password = config.password
                    driverClassName = "org.postgresql.Driver"
                    maximumPoolSize = config.maximumPoolSize
                    minimumIdle = 1
                    isAutoCommit = false
                    transactionIsolation = "TRANSACTION_READ_COMMITTED"
                    poolName = "posttrack-postgres"
                }
            val dataSource = HikariDataSource(hikariConfig)
            return PostgresDatabase(dataSource)
        }
    }
}
