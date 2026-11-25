package com.akingstar.askai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNumberValidatorTest {
    @Test
    fun normalizesIndianNumber() {
        assertEquals("9876543210", PhoneNumberValidator.normalize("98765 43210"))
    }

    @Test
    fun preservesInternationalPrefix() {
        assertEquals("+919876543210", PhoneNumberValidator.normalize("+91 98765-43210"))
    }

    @Test
    fun rejectsTooShortAndTooLongNumbers() {
        assertNull(PhoneNumberValidator.normalize("123456"))
        assertNull(PhoneNumberValidator.normalize("1234567890123456"))
    }

    @Test
    fun rejectsLettersAndMalformedPlusSigns() {
        assertNull(PhoneNumberValidator.normalize("98765ABCDE"))
        assertNull(PhoneNumberValidator.normalize("91+9876543210"))
        assertNull(PhoneNumberValidator.normalize("++919876543210"))
    }

    @Test
    fun rejectsEmptyInput() {
        assertNull(PhoneNumberValidator.normalize("   "))
    }
}
