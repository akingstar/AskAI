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
    fun accessibilityTextActionsRequireConfirmation() {
        assertTrue(SafetyGate.needsConfirmation("tap_text", "Settings"))
        assertTrue(SafetyGate.needsConfirmation("long_press_text", "Photo"))
        assertTrue(SafetyGate.needsConfirmation("type_text", "Search"))
    }

    @Test
    fun blocksRiskyUiLabelsAndSensitiveFields() {
        assertTrue(SafetyGate.isRiskyTapTarget("Send"))
        assertTrue(SafetyGate.isRiskyTapTarget("भुगतान करें"))
        assertFalse(SafetyGate.isRiskyTapTarget("Settings"))
        assertTrue(SafetyGate.isSensitiveInputTarget("Password"))
        assertTrue(SafetyGate.isSensitiveInputTarget("OTP"))
        assertFalse(SafetyGate.isSensitiveInputTarget("Search"))
    }

    @Test
    fun rejectsUnsupportedSensitiveActions() {
        assertTrue(SafetyGate.isNeverAllowedAction("sms"))
        assertTrue(SafetyGate.isNeverAllowedAction("call"))
        assertFalse(SafetyGate.isNeverAllowedAction("open_app"))
    }
}
