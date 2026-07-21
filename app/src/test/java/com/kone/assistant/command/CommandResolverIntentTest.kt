package com.kone.assistant.command

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class CommandResolverIntentTest(private val case: Case) {
    private val resolver = CommandResolver()

    @Test fun positive_expression_matches_expected_intent() {
        val result = resolver.resolve(case.positive)
        assertEquals(result.toString(), case.id, result.id)
        assertEquals(ReasonCode.MATCHED, result.reason.code)
        assertTrue(result.confidence.score >= 0.9)
        assertFalse(result.isNoOp)
    }

    @Test fun paraphrase_matches_expected_intent() {
        val result = resolver.resolve(case.paraphrase)
        assertEquals(result.toString(), case.id, result.id)
        assertEquals(ReasonCode.MATCHED, result.reason.code)
        assertTrue(result.confidence.score >= 0.8)
    }

    @Test fun negative_expression_does_not_match_expected_intent() {
        val result = resolver.resolve(case.negative)
        assertTrue("${case.id} incorrectly matched '${case.negative}'", result.id != case.id)
    }

    data class Case(
        val id: IntentId,
        val positive: String,
        val paraphrase: String,
        val negative: String,
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases() = listOf(
            Case(IntentId.MAP_HOME, "Eve yol tarifi başlat", "Evime navigasyonu başlat", "Eve mesaj yaz"),
            Case(IntentId.MAP_NEARBY, "En yakın eczane ara", "Yakınımdaki eczane bul", "Eczaneyi ara"),
            Case(IntentId.CALL_CONTACT, "Annemi ara", "Mehmet'e telefon et", "Mehmet'e mesaj yaz"),
            Case(IntentId.CALL_NUMBER, "0212 555 00 00'ı ara", "+90 212 555 00 00 telefon et", "0212 555 00 00 kaydet"),
            Case(IntentId.MESSAGE_TEXT, "Ayşe'ye gecikeceğim yaz", "Mehmet'e eve geliyorum mesaj at", "Ayşe'yi ara"),
            Case(IntentId.SHARE_LOCATION, "Mehmet'e konumumu gönder", "Ayşe'ye konumumu paylaş", "Mehmet'e fotoğraf gönder"),
            Case(IntentId.PLAY_SPOTIFY, "Spotify'da caz çal", "Spotify'da rock oynat", "YouTube'da caz ara"),
            Case(IntentId.PAUSE_MEDIA, "Müziği duraklat", "Şarkıyı durdur", "Müziği aç"),
            Case(IntentId.SEARCH_YOUTUBE, "YouTube'da Kotlin dersi ara", "YouTube'da Compose dersi bul", "Spotify'da Kotlin çal"),
            Case(IntentId.NEXT_MEDIA, "Sonraki videoya geç", "Sıradaki şarkıya geç", "Önceki videoya geç"),
            Case(IntentId.OPEN_APP, "WhatsApp'ı aç", "Telegram'ı çalıştır", "WhatsApp'ı kaldır"),
            Case(IntentId.OPEN_CAMERA, "Kamerayı aç", "Kamera çalıştır", "Galeriyi aç"),
            Case(IntentId.TORCH_ON, "Feneri aç", "Flaşı çalıştır", "Feneri kapat"),
            Case(IntentId.TORCH_OFF, "Feneri kapat", "El fenerini söndür", "Feneri aç"),
            Case(IntentId.SET_TIMER, "10 dakikalık zamanlayıcı kur", "30 saniyelik zamanlayıcı ayarla", "10 dakikalık alarm kur"),
            Case(IntentId.CANCEL_TIMER, "Zamanlayıcıyı iptal et", "Zamanlayıcıyı durdur", "Alarmı iptal et"),
        )
    }
}
