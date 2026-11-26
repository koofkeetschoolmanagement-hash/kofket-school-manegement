package com.example

import com.example.core.AppLanguage
import com.example.core.stringsFor
import com.example.data.repository.isValidResetEmail
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthResetValidationTest {

    @Test
    fun `isValidResetEmail accepts standard email addresses and trims whitespace`() {
        assertTrue(isValidResetEmail("teacher@koofkeet.org"))
        assertTrue(isValidResetEmail("  admin.user+kofket@gmail.com  "))
        assertFalse(isValidResetEmail(""))
        assertFalse(isValidResetEmail("   "))
        assertFalse(isValidResetEmail("invalid-email"))
        assertFalse(isValidResetEmail("user@domain"))
        assertFalse(isValidResetEmail("@domain.com"))
    }

    @Test
    fun `forgot password localization strings are populated for all languages`() {
        AppLanguage.entries.forEach { lang ->
            val strings = stringsFor(lang)
            assertTrue(strings.forgotPasswordButton.isNotBlank())
            assertTrue(strings.forgotPasswordTitle.isNotBlank())
            assertTrue(strings.forgotPasswordDescription.isNotBlank())
            assertTrue(strings.emailLabel.isNotBlank())
            assertTrue(strings.sendResetLinkButton.isNotBlank())
            assertTrue(strings.sendingResetLink.isNotBlank())
            assertTrue(strings.cancelLabel.isNotBlank())
            assertTrue(strings.invalidEmailError.isNotBlank())
            assertTrue(strings.resetEmailSentSuccess.contains("%s"))
            assertTrue(strings.resetEmailFailedPrefix.isNotBlank())
        }
    }
}
