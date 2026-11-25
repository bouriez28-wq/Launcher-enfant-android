package com.example.launchertlphoneenfant

import com.example.launchertlphoneenfant.domain.KioskManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KioskManagerTest {

    private val kioskManager = KioskManager()

    @Test
    fun verifyPin_correctPin_returnsTrue() {
        val result = kioskManager.verifyPin("1234", "1234")
        assertTrue(result)
    }

    @Test
    fun verifyPin_wrongPin_returnsFalse() {
        val result = kioskManager.verifyPin("9999", "1234")
        assertFalse(result)
    }
}
