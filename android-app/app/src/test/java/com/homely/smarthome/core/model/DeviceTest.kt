package com.homely.smarthome.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceTest {
    @Test
    fun defaultsToSafeOffState() {
        val device = Device()

        assertEquals(DeviceStatus.OFF, device.status)
        assertEquals(DeviceType.OUTLET, device.type)
    }
}

