package com.kone.assistant.command

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandResolverSafetyTest {
    @Test fun `unsupported command is no-op`() {
        val result = CommandResolver().resolve("Bütün dosyalarımı sil")
        assertTrue(result.isNoOp)
        assertEquals(ReasonCode.NO_MATCH, result.reason.code)
    }

    @Test fun `recognized but disallowed command is no-op`() {
        val resolver = CommandResolver(allowedIntents = setOf(IntentId.TORCH_ON, IntentId.TORCH_OFF))
        val result = resolver.resolve("Annemi ara")
        assertTrue(result.isNoOp)
        assertEquals(ReasonCode.NOT_ALLOWED, result.reason.code)
        assertEquals(listOf("CALL_CONTACT"), result.reason.matchedRules)
    }

    @Test fun `blank STT result is no-op`() {
        val result = CommandResolver().resolve("   ")
        assertTrue(result.isNoOp)
        assertEquals(ReasonCode.NO_MATCH, result.reason.code)
    }

    @Test fun `two valid commands in one utterance are ambiguous no-op`() {
        val result = CommandResolver().resolve("Feneri aç ve feneri kapat")
        assertTrue(result.isNoOp)
        assertEquals(ReasonCode.AMBIGUOUS, result.reason.code)
        assertEquals(setOf("TORCH_ON", "TORCH_OFF"), result.reason.matchedRules.toSet())
    }

    @Test fun `slots preserve command parameters`() {
        val message = CommandResolver().resolve("Ayşe'ye gecikeceğim yaz")
        assertEquals("ayşe", message.slot("recipient"))
        assertEquals("gecikeceğim", message.slot("message"))

        val timer = CommandResolver().resolve("10 dakikalık zamanlayıcı kur")
        assertEquals("600", timer.slot("seconds"))

        val phone = CommandResolver().resolve("0212 555 00 00'ı ara")
        assertEquals("02125550000", phone.slot("phone"))
    }
}
