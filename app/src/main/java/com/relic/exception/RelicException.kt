package com.relic.exception

/**
 * Base exception for the application
 */
abstract class RelicException(
    message: String?,
    cause: Throwable?
) : Exception(message, cause)