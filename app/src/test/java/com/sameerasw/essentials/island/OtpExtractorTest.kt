package com.sameerasw.essentials.island

import com.sameerasw.essentials.island.plugins.notifications.OtpExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OtpExtractorTest {
    private fun code(text: String) = OtpExtractor.extract(text)

    @Test
    fun findsCommonEnglishCodes() {
        assertEquals("482913", code("G-482913 is your Google verification code."))
        assertEquals("482913", code("Your WhatsApp code: 482-913. Don't share this code with others"))
        assertEquals("482913", code("Your Uber code is 482 913. Never share this code."))
        assertEquals("4829", code("Use 4829 as your one-time passcode"))
        assertEquals("48291374", code("Microsoft account security code: 48291374"))
        assertEquals("AB12CD", code("Your login code is AB12CD"))
        assertEquals("482913", code("482913 is your Instagram code. Don't share it."))
    }

    @Test
    fun findsSpanishAndPortugueseCodes() {
        assertEquals("482913", code("Tu código de verificación es 482913"))
        assertEquals("739104", code("BBVA: Tu clave de operación es 739104. No la compartas."))
        assertEquals("1234", code("Tu codigo OTP es 1234"))
        assertEquals("552190", code("Seu código de verificação é 552190"))
    }

    @Test
    fun picksCodeOverOtherNumbers() {
        assertEquals("482913", code("Code 482913 for your card ending in 1234"))
        assertEquals("739104", code("Your code is 739104. Purchase of $1,250.00 at Amazon"))
        assertEquals("903311", code("Pedido 12345678: tu código de entrega es 903311"))
        assertEquals("2024", code("Your PIN code 2024"))
    }

    @Test
    fun findsDomainBoundAndShortCodes() {
        assertEquals("123456", code("Your Apple Account code is: 123456. Don't share it with anyone.\n\n@apple.com #123456 %apple.com"))
        assertEquals("847261", code("847261 is your login code\n\n@example.com #847261"))
        assertEquals("FR4NC", code("Your Steam Guard code is FR4NC"))
        assertEquals("X7K2", code("Your verification code: X7K2"))
        assertEquals("592013", code("Tu token de acceso es 592013"))
        assertEquals("4821", code("Tu NIP dinámico es 4821"))
        assertEquals("482913", code("<#> Your App verification code is: 482913 FA+9qCX9VSu"))
    }

    @Test
    fun findsCodesInEmailNotifications() {
        assertEquals("639201", code("Your Slack confirmation code\nConfirm your email address. Your confirmation code is below — enter it in your browser window. 639201"))
        assertEquals("481920", code("481920 is your Microsoft account verification code"))
        assertEquals("773810", code("Verify your sign-in\nHi Isaac, use this code to finish signing in: 773810. It expires in 10 minutes. © 2026 Example Inc."))
        assertNull(code("Your receipt from Example\nThanks for your order 2026-1043. Total $54.20. Use promo code SAVE10 next time"))
    }

    @Test
    fun ignoresMessagesWithoutCodes() {
        assertNull(code("Your order #123456 has shipped"))
        assertNull(code("You paid $1234 to Maria"))
        assertNull(code("Meeting moved to 2026, see you there"))
        assertNull(code("Hey, call me at 5551234567"))
        assertNull(code("Code review at 14:30 in room 4"))
        assertNull(code("The code for 2026 changed"))
        assertNull(code("Tu pedido 98765432 está en camino"))
        assertNull(code(""))
    }

    @Test
    fun ignoresAmountsAndDecimalsNearKeywords() {
        assertNull(code("Promo code applied: you saved 1,500.00"))
        assertNull(code("Verification of payment MXN 4500 completed"))
        assertNull(code("Your code discount is 15%"))
        assertNull(code("Do not share your OTP. Txn of 4500 INR on card XX12 at Amazon"))
        assertNull(code("Your code: pay 4500 pesos before Friday"))
        assertEquals("739104", code("OTP 739104 for txn of 4500 INR on card XX12"))
    }

    @Test
    fun domainBoundNeedsADigit() {
        assertNull(code("Ping alex@acme.com #engineering before the code freeze"))
    }
}
