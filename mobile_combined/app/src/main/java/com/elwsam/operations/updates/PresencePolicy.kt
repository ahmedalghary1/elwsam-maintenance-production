package com.elwsam.operations.updates

object PresencePolicy {
    fun screen(activity: String?, loggedIn: Boolean): String = when {
        activity == null -> "background"
        activity.startsWith("com.maintenance.supervisor.") -> "maintenance"
        activity.startsWith("com.production.supervisor.") -> "production"
        activity.endsWith(".updates.UpdateActivity") -> "update"
        !loggedIn -> "login"
        else -> "main"
    }
}
