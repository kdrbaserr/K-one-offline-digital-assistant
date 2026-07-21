package com.kone.assistant.command

import java.util.Locale

data class NormalizedCommand(
    val original: String,
    val text: String,
    val appliedSynonyms: List<String>,
)

class TurkishCommandNormalizer {
    private val locale = Locale.forLanguageTag("tr-TR")
    private val synonyms = linkedMapOf(
        words("yol tarifi ver|navigasyonu başlat|rota oluştur") to "yol tarifi başlat",
        words("civarındaki|yakınımdaki|yakınlarda bulunan") to "en yakın",
        words("telefon et|arama yap") to "ara",
        words("mesaj at|mesaj gönder") to "yaz",
        words("konumumu paylaş|lokasyonumu gönder") to "konumumu gönder",
        words("oynat|dinlet") to "çal",
        words("müziği durdur|şarkıyı durdur|oynatmayı durdur") to "müziği duraklat",
        words("bul|arama yap") to "ara",
        words("bir sonraki|sıradaki") to "sonraki",
        words("çalıştır") to "aç",
        words("flaş|el feneri") to "fener",
        words("söndür|devre dışı bırak") to "kapat",
        words("ayarla") to "kur",
        words("durdur|sil") to "iptal et",
    )

    fun normalize(input: String): NormalizedCommand {
        var text = input.lowercase(locale)
            .replace('’', '\'')
            .replace(Regex("([a-zçğıöşü0-9])'[a-zçğıöşü]*(?![a-zçğıöşü0-9])"), "$1")
            .replace(Regex("[^a-zçğıöşü0-9+ ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .replace(words("kamerayı"), "kamera")
            .replace(words("feneri"), "fener")
            .replace(words("fenerini"), "fener")
            .replace(words("flaşı"), "fener")
            .replace(words("el fener"), "fener")
            .replace(words("zamanlayıcıyı"), "zamanlayıcı")
        val applied = mutableListOf<String>()
        synonyms.forEach { (pattern, replacement) ->
            if (pattern.containsMatchIn(text)) {
                text = pattern.replace(text, replacement)
                applied += "${pattern.pattern}=>$replacement"
            }
        }
        return NormalizedCommand(input, text.replace(Regex("\\s+"), " ").trim(), applied)
    }

    private companion object {
        fun words(alternatives: String): Regex =
            Regex("(?<![a-zçğıöşü0-9])(?:$alternatives)(?![a-zçğıöşü0-9])")
    }
}
