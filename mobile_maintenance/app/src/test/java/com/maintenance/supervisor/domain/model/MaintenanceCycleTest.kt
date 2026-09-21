package com.maintenance.supervisor.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MaintenanceCycleTest {
    @Test fun `empty cycle has no asset`() = assertNull(MaintenanceCycle.assetAt(emptyList<Int>(), 0, 2))
    @Test fun `stays when no prior day was completed`() = assertEquals("regular-1", MaintenanceCycle.assetAt(listOf("regular-1", "regular-2", "press"), 0, 0))
    @Test fun `advances in server order`() = assertEquals("press", MaintenanceCycle.assetAt(listOf("regular-1", "regular-2", "press"), 0, 2))
    @Test fun `wraps after special machines`() = assertEquals("regular-1", MaintenanceCycle.assetAt(listOf("regular-1", "regular-2", "press"), 0, 3))
}
