package com.homely.smarthome.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FloorValidationTest {
    @Test
    fun acceptsAConfiguredPlan() {
        val floor = Floor(
            name = "Upper floor",
            imageUrl = "https://example.com/upper-floor.png",
            gridRows = 8,
            gridColumns = 12,
            sortOrder = 2,
        )

        assertEquals(null, FloorValidation.errorFor(floor))
    }

    @Test
    fun rejectsInvalidGridDimensions() {
        val floor = Floor(name = "Small floor", gridRows = 0, gridColumns = 65)

        assertEquals("Rows must be between 1 and 64.", FloorValidation.errorFor(floor))
    }

    @Test
    fun newFloorsUseASharedPendingOperationId() {
        assertEquals(FloorValidation.NEW_OPERATION_ID, FloorValidation.operationId(Floor()))
        assertEquals("existing-floor", FloorValidation.operationId(Floor(id = "existing-floor")))
    }
}
