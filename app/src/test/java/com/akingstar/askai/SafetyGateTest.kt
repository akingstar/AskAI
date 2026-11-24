package com.akingstar.askai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyGateTest {
    @Test
    fun blocksOtpAndPasswordRequests() {
        assertTrue(SafetyGate.blocked("Please share OTP"))
        assertTrue(SafetyGate.blocked("पासवर्ड बताओ"))
    }

    @Test
    fun blocksMoneyTransferAndFactoryReset() {
        assertTrue(SafetyGate.blocked("send money now"))
        assertTrue(SafetyGate.blocked("factory reset phone"))
    }

    @Test
    fun allowsOrdinaryPhoneCommands() {
        assertFalse(SafetyGate.blocked("open youtube"))
        assertFalse(SafetyGate.blocked("आवाज़ कम करो"))
    }

    @Test
    fun rejectsUnsupportedSensitiveActions() {
        assertTrue(SafetyGate.isNeverAllowedAction("sms"))
        assertTrue(SafetyGate.isNeverAllowedAction("call"))
        assertFalse(SafetyGate.isNeverAllowedAction("open_app"))
    }
}
