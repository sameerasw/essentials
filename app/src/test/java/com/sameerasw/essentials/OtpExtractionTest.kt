package com.sameerasw.essentials

import com.sameerasw.essentials.services.NotificationListener
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OtpExtractionTest {

    @Test
    fun testInstagramCodeFormats() {
        assertEquals("123456", NotificationListener.extractOtpCode("123 456 is your Instagram code"))
        assertEquals("123456", NotificationListener.extractOtpCode("123456 is your Instagram security code"))
        assertEquals("123456", NotificationListener.extractOtpCode("Instagram: 123 456 is your login code. Don't share it."))
        assertEquals("123456", NotificationListener.extractOtpCode("Your Instagram code is 123456"))
        assertEquals("123456", NotificationListener.extractOtpCode("Use 123 456 to verify your Instagram account"))
        assertEquals("123456", NotificationListener.extractOtpCode("Use 123456 to log into your account."))
        assertEquals("123456", NotificationListener.extractOtpCode("Security code: 123456"))
        assertEquals("123456", NotificationListener.extractOtpCode("Instagram: 123456 is your confirmation code"))
        assertEquals("123456", NotificationListener.extractOtpCode("123-456 is your Instagram code"))
        assertEquals("123456", NotificationListener.extractOtpCode("<#> 123 456 is your Instagram code. Go to https://instagram.com/..."))
    }

    @Test
    fun testWhatsAppAndMessagingFormats() {
        assertEquals("123456", NotificationListener.extractOtpCode("Your WhatsApp code: 123-456"))
        assertEquals("123456", NotificationListener.extractOtpCode("123-456 is your WhatsApp code"))
        assertEquals("123456", NotificationListener.extractOtpCode("WhatsApp code 123-456. Do not share this code with anyone."))
        assertEquals("12345", NotificationListener.extractOtpCode("Telegram code: 12345"))
        assertEquals("123456", NotificationListener.extractOtpCode("Signal verification code: 123-456"))
        assertEquals("123456", NotificationListener.extractOtpCode("Your Discord verification code is: 123456"))
    }

    @Test
    fun testGoogleAndTOTPFormats() {
        assertEquals("G123456", NotificationListener.extractOtpCode("G-123456 is your Google verification code."))
        assertEquals("123456", NotificationListener.extractOtpCode("123456 is your Google verification code."))
        assertEquals("987654", NotificationListener.extractOtpCode("TOTP: 987 654"))
        assertEquals("987654", NotificationListener.extractOtpCode("987 654 is your TOTP"))
        assertEquals("987654", NotificationListener.extractOtpCode("Your 2FA code is 987654"))
    }

    @Test
    fun testBankingAndTransactionFormats() {
        assertEquals("123456", NotificationListener.extractOtpCode("123456 is the OTP for your txn of INR 500 at Amazon."))
        assertEquals("1234", NotificationListener.extractOtpCode("1234 is your Swiggy OTP. Do not share."))
        assertEquals("1234", NotificationListener.extractOtpCode("Uber: 1234 is your verification code."))
        assertEquals("987654", NotificationListener.extractOtpCode("Your OTP for INR 500 on 12/09/2026 is 987654. Do not share."))
    }

    @Test
    fun testNonOtpExclusions() {
        assertNull(NotificationListener.extractOtpCode("Hey, what are you doing?"))
        assertNull(NotificationListener.extractOtpCode("Meeting scheduled for 2026."))
    }
}
