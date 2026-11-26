package com.akingstar.askai

import org.junit.Assert.*
import org.junit.Test

class SensitiveDataFilterTest {
    @Test
    fun masksAadhaarNumber() {
        assertTrue(SensitiveDataFilter.containsSensitive("123456789012"))
        assertTrue(SensitiveDataFilter.mask("1234 5678 9012").contains("छिपाई गई"))
    }

    @Test
    fun masksOtpAndCredentialValues() {
        assertTrue(SensitiveDataFilter.mask("OTP 123456").contains("छिपाई गई"))
        assertTrue(SensitiveDataFilter.mask("password is hunter2").contains("छिपाई गई"))
        assertTrue(SensitiveDataFilter.mask("PIN: 1234").contains("छिपाई गई"))
    }

    @Test
    fun masksHindiOtpAndCredentialValues() {
        assertTrue(SensitiveDataFilter.mask("ओटीपी 123456").contains("छिपाई गई"))
        assertTrue(SensitiveDataFilter.mask("पासवर्ड: secret123").contains("छिपाई गई"))
        assertTrue(SensitiveDataFilter.mask("पिन: 1234").contains("छिपाई गई"))
        assertTrue(SensitiveDataFilter.mask("पासकोड abc123").contains("छिपाई गई"))
        assertTrue(SensitiveDataFilter.containsSensitive("सुरक्षा कोड 456789"))
    }

    @Test
    fun hindiCredentialFilterAvoidsOrdinaryQuestionsAndPinCodeText() {
        assertEquals(
            "पासवर्ड क्या होता है?",
            SensitiveDataFilter.mask("पासवर्ड क्या होता है?")
        )
        assertEquals(
            "पिन कोड क्या है?",
            SensitiveDataFilter.mask("पिन कोड क्या है?")
        )
        assertFalse(SensitiveDataFilter.containsSensitive("पिन कोड"))
        assertTrue(SensitiveDataFilter.containsSensitive("पिन 1234"))
        assertTrue(SensitiveDataFilter.containsSensitive("सीवीवी 123"))
        assertTrue(SensitiveDataFilter.containsSensitive("पासवर्ड: secret123"))
        assertTrue(SensitiveDataFilter.containsSensitive("पासवर्ड secret123"))
        assertTrue(SensitiveDataFilter.containsSensitive("पासवर्ड है secret123"))
    }

    @Test
    fun masksEmailAndUpiIdentifiers() {
        assertTrue(SensitiveDataFilter.containsSensitive("Contact test@example.com"))
        assertTrue(SensitiveDataFilter.containsSensitive("UPI ID: name@okaxis"))
    }

    @Test
    fun masksIndianAndInternationalPhoneNumbers() {
        assertTrue(SensitiveDataFilter.containsSensitive("Call 9876543210"))
        assertTrue(SensitiveDataFilter.containsSensitive("Call +91 98765 43210"))
        assertTrue(SensitiveDataFilter.containsSensitive("Call +1 202 555 0123"))
    }

    @Test
    fun masksOnlyLuhnValidCardCandidates() {
        assertTrue(SensitiveDataFilter.containsSensitive("4111111111111111"))
        assertFalse(SensitiveDataFilter.containsSensitive("4111111111111112"))
    }

    @Test
    fun doesNotMaskOrdinaryShortNumbers() {
        assertFalse(SensitiveDataFilter.containsSensitive("Brightness 50 percent"))
        assertFalse(SensitiveDataFilter.containsSensitive("Year 2026"))
        assertFalse(SensitiveDataFilter.containsSensitive("The answer is 12345"))
    }

    @Test
    fun leavesOrdinaryTextUnchanged() {
        val text = "Open settings and increase brightness"
        assertEquals(text, SensitiveDataFilter.mask(text))
    }
}
