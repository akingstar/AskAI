package com.akingstar.askai

object SensitiveDataFilter {
    private const val MASK = "[छिपाई गई निजी जानकारी]"

    private val otpPattern = Regex(
        """(?i)\b(?:otp|one[\s-]?time password|verification code|security code)\s*(?::|=|-|\bis\b|\s)\s*\d{4,8}\b"""
    )

    private val credentialPattern = Regex(
        """(?i)\b(?:password|passcode)\s*(?::|=|\bis\b)\s*\S+|\b(?:pin|cvv|cvc)\s*(?::|=|\bis\b|\s)\s*[A-Za-z0-9]{3,8}\b"""
    )

    private val hindiOtpPattern = Regex(
        """(?:ओटीपी|वन[\s-]?टाइम पासवर्ड|सत्यापन कोड|सुरक्षा कोड)\s*(?::|=|-)?\s*\d{4,8}"""
    )
    private val hindiCredentialPattern = Regex(
        """(?:पासवर्ड|पासकोड)(?:\s*[:=]\s*|\s+(?:है\s+)?)(?!क्या(?:\s|$)|कैसे(?:\s|$)|क्यों(?:\s|$)|कहाँ(?:\s|$))\S+|पिन\s*(?::|=|-|\s)\s*\d{4,8}(?!\d)|(?:सीवीवी|सीवीसी)\s*(?::|=|-|\s)\s*\d{3,4}(?!\d)"""
    )

    private val paymentIdPattern = Regex(
        """(?i)(?<![A-Z0-9._-])[A-Z0-9._-]{2,}@[A-Z0-9][A-Z0-9.-]{1,}(?![A-Z0-9.-])"""
    )

    private val aadhaarPattern = Regex(
        """(?<!\d)\d{4}[ -]?\d{4}[ -]?\d{4}(?!\d)"""
    )

    private val indianMobilePattern = Regex(
        """(?<!\d)(?:\+?91[\s().-]?)?[6-9]\d{4}[\s().-]?\d{5}(?!\d)"""
    )

    private val formattedInternationalPhonePattern = Regex(
        """(?<!\d)\+\d{1,3}[\s().-]\d{2,4}[\s().-]\d{3,4}[\s().-]\d{3,4}(?!\d)"""
    )

    private val cardCandidatePattern = Regex(
        """(?<!\d)(?:\d[ -]?){13,19}(?!\d)"""
    )

    private fun passesLuhn(value: String): Boolean {
        val digits = value.filter(Char::isDigit)
        if (digits.length !in 13..19) return false

        var sum = 0
        var doubleDigit = false
        for (index in digits.indices.reversed()) {
            var digit = digits[index].digitToInt()
            if (doubleDigit) {
                digit *= 2
                if (digit > 9) digit -= 9
            }
            sum += digit
            doubleDigit = !doubleDigit
        }
        return sum % 10 == 0
    }

    fun mask(input: String): String {
        var output = input
        output = hindiCredentialPattern.replace(output, MASK)
        output = hindiOtpPattern.replace(output, MASK)
        output = credentialPattern.replace(output, MASK)
        output = otpPattern.replace(output, MASK)
        output = paymentIdPattern.replace(output, MASK)
        output = aadhaarPattern.replace(output, MASK)
        output = indianMobilePattern.replace(output, MASK)
        output = formattedInternationalPhonePattern.replace(output, MASK)
        output = cardCandidatePattern.replace(output) { match ->
            if (passesLuhn(match.value)) MASK else match.value
        }
        return output
    }

    fun containsSensitive(input: String): Boolean = mask(input) != input
}
