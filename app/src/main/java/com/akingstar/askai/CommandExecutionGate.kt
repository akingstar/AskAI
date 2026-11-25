package com.akingstar.askai

/**
 * Allows only one command request or execution at a time.
 * A token prevents an old callback from releasing a newer execution.
 */
internal class CommandExecutionGate {
    private var nextToken = 0L
    private var activeToken: Long? = null

    @Synchronized
    fun tryAcquire(): Long? {
        if (activeToken != null) return null

        nextToken += 1
        activeToken = nextToken
        return nextToken
    }

    @Synchronized
    fun release(token: Long): Boolean {
        if (activeToken != token) return false

        activeToken = null
        return true
    }

    @Synchronized
    fun isBusy(): Boolean = activeToken != null
}
