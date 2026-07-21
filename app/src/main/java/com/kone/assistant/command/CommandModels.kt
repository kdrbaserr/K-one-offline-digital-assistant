package com.kone.assistant.command

enum class IntentId {
    MAP_HOME,
    MAP_NEARBY,
    CALL_CONTACT,
    CALL_NUMBER,
    MESSAGE_TEXT,
    SHARE_LOCATION,
    PLAY_SPOTIFY,
    PAUSE_MEDIA,
    SEARCH_YOUTUBE,
    NEXT_MEDIA,
    OPEN_APP,
    OPEN_CAMERA,
    TORCH_ON,
    TORCH_OFF,
    SET_TIMER,
    CANCEL_TIMER,
    NO_OP,
}

data class Slot(
    val name: String,
    val value: String,
)

enum class ConfidenceLevel { HIGH, MEDIUM, LOW, NONE }

data class Confidence(
    val score: Double,
    val level: ConfidenceLevel,
    val evidence: List<String> = emptyList(),
) {
    init { require(score in 0.0..1.0) }
}

enum class ReasonCode { MATCHED, NO_MATCH, AMBIGUOUS, NOT_ALLOWED, INVALID_SLOT }

data class Reason(
    val code: ReasonCode,
    val message: String,
    val matchedRules: List<String> = emptyList(),
)

/** Domain intent; it does not execute an Android action. */
data class Intent(
    val id: IntentId,
    val slots: List<Slot>,
    val confidence: Confidence,
    val reason: Reason,
) {
    val isNoOp: Boolean get() = id == IntentId.NO_OP
    fun slot(name: String): String? = slots.firstOrNull { it.name == name }?.value

    companion object {
        fun noOp(code: ReasonCode, message: String, matchedRules: List<String> = emptyList()) = Intent(
            id = IntentId.NO_OP,
            slots = emptyList(),
            confidence = Confidence(0.0, ConfidenceLevel.NONE),
            reason = Reason(code, message, matchedRules),
        )
    }
}
