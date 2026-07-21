package com.kone.assistant.action

import com.kone.assistant.command.Intent

enum class ActionStatus { SIMULATED, NO_OP }

data class ActionResult(
    val status: ActionStatus,
    val actionId: String,
    val summary: String,
)

/** Demo boundary: describes an action but never invokes Android or external apps. */
fun interface ActionExecutor {
    fun execute(intent: Intent): ActionResult
}

class FakeActionExecutor : ActionExecutor {
    override fun execute(intent: Intent): ActionResult {
        if (intent.isNoOp) {
            return ActionResult(ActionStatus.NO_OP, "NO_OP", "İşlem yapılmadı: ${intent.reason.code}")
        }
        val slotSummary = intent.slots.joinToString { "${it.name}=${it.value}" }
        return ActionResult(
            status = ActionStatus.SIMULATED,
            actionId = "FAKE_${intent.id.name}",
            summary = buildString {
                append("Simüle edildi: ${intent.id.name}")
                if (slotSummary.isNotEmpty()) append(" ($slotSummary)")
            },
        )
    }
}
