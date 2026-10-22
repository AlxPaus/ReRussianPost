package ru.posttrack.postgres

data class DatabaseConfig(
    val jdbcUrl: String,
    val username: String,
    val password: String,
    val maximumPoolSize: Int,
) {
    companion object {
        private const val DEFAULT_MAXIMUM_POOL_SIZE = 10

        fun fromEnvironment(environment: Map<String, String> = System.getenv()): DatabaseConfig =
            DatabaseConfig(
                jdbcUrl = environment.required("DATABASE_URL"),
                username = environment.required("DATABASE_USER"),
                password = environment.required("DATABASE_PASSWORD"),
                maximumPoolSize =
                    environment["DATABASE_MAX_POOL_SIZE"]
                        ?.toIntOrNull()
                        ?.takeIf { it > 0 }
                        ?: DEFAULT_MAXIMUM_POOL_SIZE,
            )

        private fun Map<String, String>.required(name: String): String =
            get(name)?.takeIf(String::isNotBlank)
                ?: error("Required environment variable $name is not configured")
    }
}
