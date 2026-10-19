package ru.posttrack.app.application

class EntityNotFound(
    entity: String,
) : RuntimeException("$entity not found")

class AccessDenied : RuntimeException("Access denied")
