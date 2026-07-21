package com.kone.assistant.command

class CommandResolver(
    private val allowedIntents: Set<IntentId> = IntentId.entries.filterNot { it == IntentId.NO_OP }.toSet(),
    private val normalizer: TurkishCommandNormalizer = TurkishCommandNormalizer(),
) {
    private data class Rule(
        val id: IntentId,
        val regex: Regex,
        val slots: (MatchResult) -> List<Slot> = { emptyList() },
    )

    private val rules = listOf(
        Rule(IntentId.MAP_HOME, Regex("^(eve|evime) yol tarifi başlat$")),
        Rule(IntentId.MAP_NEARBY, Regex("^en yakın (.+?)( ara)?$")) { listOf(Slot("query", it.groupValues[1])) },
        Rule(IntentId.CALL_NUMBER, Regex("^([+0-9][0-9 ]{6,}) ara$")) { match ->
            listOf(Slot("phone", match.groupValues[1].filter { it.isDigit() || it == '+' }))
        },
        Rule(IntentId.CALL_CONTACT, Regex("^((?!(?:en yakın|youtube)\\b)[a-zçğıöşü]+(?: [a-zçğıöşü]+)?) ara$")) { listOf(Slot("contact", it.groupValues[1])) },
        Rule(IntentId.SHARE_LOCATION, Regex("^([a-zçğıöşü]+) konumumu gönder$")) { listOf(Slot("recipient", it.groupValues[1])) },
        Rule(IntentId.MESSAGE_TEXT, Regex("^([a-zçğıöşü]+) (.+) yaz$")) { listOf(Slot("recipient", it.groupValues[1]), Slot("message", it.groupValues[2])) },
        Rule(IntentId.PLAY_SPOTIFY, Regex("^spotify(?:da)? (.+) çal$")) { listOf(Slot("query", it.groupValues[1])) },
        Rule(IntentId.PAUSE_MEDIA, Regex("^müziği duraklat$")),
        Rule(IntentId.SEARCH_YOUTUBE, Regex("^youtube(?:da)? (.+) ara$")) { listOf(Slot("query", it.groupValues[1])) },
        Rule(IntentId.NEXT_MEDIA, Regex("^sonraki (video|şarkı)(?:ya)? geç$")) { listOf(Slot("media", it.groupValues[1])) },
        Rule(IntentId.OPEN_CAMERA, Regex("^kamera aç$")),
        Rule(IntentId.TORCH_ON, Regex("^fener aç$")),
        Rule(IntentId.TORCH_OFF, Regex("^fener kapat$")),
        Rule(IntentId.SET_TIMER, Regex("^(\\d+) (saniyelik|dakikalık|saatlik|saniye|dakika|saat) zamanlayıcı kur$")) { match ->
            val value = match.groupValues[1]
            val unit = mapOf("saniyelik" to "saniye", "dakikalık" to "dakika", "saatlik" to "saat")
                .getOrDefault(match.groupValues[2], match.groupValues[2])
            val multiplier = mapOf("saniye" to 1L, "dakika" to 60L, "saat" to 3_600L).getValue(unit)
            listOf(Slot("duration", value), Slot("unit", unit), Slot("seconds", (value.toLong() * multiplier).toString()))
        },
        Rule(IntentId.CANCEL_TIMER, Regex("^zamanlayıcı iptal et$")),
        Rule(IntentId.OPEN_APP, Regex("^((?!(?:kamera|fener)$)[a-zçğıöşü0-9 ]+) aç$")) { listOf(Slot("app", it.groupValues[1])) },
    )

    fun resolve(input: String): Intent {
        val normalized = normalizer.normalize(input)
        if (normalized.text.isBlank()) return Intent.noOp(ReasonCode.NO_MATCH, "Komut metni boş")
        val clauseMatches = normalized.text.split(" ve ").map { matchRules(it) }.filter { it.isNotEmpty() }
        if (clauseMatches.size > 1) {
            return Intent.noOp(
                ReasonCode.AMBIGUOUS,
                "Aynı ifadede birden fazla komut var",
                clauseMatches.flatten().map { it.first.id.name }.distinct(),
            )
        }
        val matches = matchRules(normalized.text)
        if (matches.isEmpty()) return Intent.noOp(ReasonCode.NO_MATCH, "Desteklenen komut bulunamadı")
        if (matches.size > 1) {
            return Intent.noOp(ReasonCode.AMBIGUOUS, "Komut birden fazla intent ile eşleşti", matches.map { it.first.id.name })
        }
        val (rule, match) = matches.single()
        if (rule.id !in allowedIntents) {
            return Intent.noOp(ReasonCode.NOT_ALLOWED, "${rule.id} bu bağlamda izinli değil", listOf(rule.id.name))
        }
        val slots = runCatching { rule.slots(match) }.getOrElse {
            return Intent.noOp(ReasonCode.INVALID_SLOT, "Komut parametreleri geçersiz", listOf(rule.id.name))
        }
        val synonymEvidence = normalized.appliedSynonyms
        val score = if (synonymEvidence.isEmpty()) 0.96 else 0.88
        return Intent(
            id = rule.id,
            slots = slots,
            confidence = Confidence(score, if (score >= 0.9) ConfidenceLevel.HIGH else ConfidenceLevel.MEDIUM,
                listOf("regex:${rule.id}") + synonymEvidence),
            reason = Reason(ReasonCode.MATCHED, "Tek ve izinli kural eşleşti", listOf(rule.id.name)),
        )
    }

    private fun matchRules(text: String): List<Pair<Rule, MatchResult>> {
        val rawMatches = rules.mapNotNull { rule -> rule.regex.matchEntire(text)?.let { rule to it } }
        // OPEN_APP is deliberately the fallback for "<name> aç"; named hardware rules are more specific.
        return if (rawMatches.size > 1 && rawMatches.any { it.first.id == IntentId.OPEN_APP }) {
            rawMatches.filterNot { it.first.id == IntentId.OPEN_APP }
        } else rawMatches
    }
}
