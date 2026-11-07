package com.maintenance.supervisor.data.remote

import org.junit.Assert.*
import org.junit.Test

class ApiErrorsTest {
    @Test fun forbiddenPreservesTheServersActualPermissionReason() {
        val message = maintenanceHttpError(403, """{"detail":"This report belongs to another account."}""")
        assertTrue(message.contains("This report belongs to another account."))
        assertTrue(message.contains("HTTP 403"))
    }

    @Test fun htmlForbiddenPageDoesNotLeakMarkupOrPretendThePhoneIsOffline() {
        val message = maintenanceHttpError(403, "<html>Forbidden</html>")
        assertFalse(message.contains("<html>"))
        assertTrue(message.contains("HTTP 403"))
    }

    @Test fun validationReasonsSupportListsAndFieldErrors() {
        assertTrue(maintenanceHttpError(400, """{"answers":["missing checklist items"]}""").contains("missing checklist items"))
    }
}
