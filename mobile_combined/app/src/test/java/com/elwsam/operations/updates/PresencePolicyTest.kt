package com.elwsam.operations.updates

import org.junit.Assert.assertEquals
import org.junit.Test

class PresencePolicyTest {
    @Test fun backgroundMustNotBeReportedAsOpen() {
        assertEquals("background", PresencePolicy.screen(null, true))
        assertEquals("background", PresencePolicy.screen(null, false))
    }

    @Test fun mainScreenDistinguishesLoginFromAuthenticatedLauncher() {
        assertEquals("login", PresencePolicy.screen("com.elwsam.operations.MainActivity", false))
        assertEquals("main", PresencePolicy.screen("com.elwsam.operations.MainActivity", true))
    }

    @Test fun eachEmbeddedModuleIsIdentifiedWithoutExposingClassNames() {
        assertEquals("maintenance", PresencePolicy.screen("com.maintenance.supervisor.MainActivity", true))
        assertEquals("production", PresencePolicy.screen("com.production.supervisor.MainActivity", true))
        assertEquals("update", PresencePolicy.screen("com.elwsam.operations.updates.UpdateActivity", true))
    }
}
