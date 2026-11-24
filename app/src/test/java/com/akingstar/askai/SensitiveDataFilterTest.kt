package com.akingstar.askai

import org.junit.Assert.*
import org.junit.Test

class SensitiveDataFilterTest {
    @Test fun masksAadhaarLengthNumber() {
        assertTrue(SensitiveDataFilter.containsSensitive("123456789012"))
    }
    @Test fun masksOtpLabel() {
        assertTrue(SensitiveDataFilter.mask("OTP 123456").contains("छिपाई गई"))
    }
}
