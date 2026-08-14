package com.homely.smarthome.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceCapabilitiesTest {
    @Test
    fun parsesMultiSwitchCapabilities() {
        val device = Device(
            type = DeviceType.MULTI_SWITCH,
            capabilities = mapOf(
                "switches" to listOf(
                    mapOf("id" to "one", "label" to "Hall", "isOn" to true),
                    mapOf("id" to "two", "label" to "Porch", "isOn" to false),
                ),
            ),
        )

        assertEquals(2, device.switches.size)
        assertTrue(device.switches.first().isOn)
        assertFalse(device.switches.last().isOn)
    }

    @Test
    fun statusOnIsTreatedAsPoweredEvenBeforeCapabilitiesArrive() {
        assertTrue(Device(status = DeviceStatus.ON).isOn)
    }
}
