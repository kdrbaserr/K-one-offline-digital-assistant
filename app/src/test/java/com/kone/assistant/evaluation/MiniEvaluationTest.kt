package com.kone.assistant.evaluation

import com.kone.assistant.action.ActionExecutor
import com.kone.assistant.action.ActionResult
import com.kone.assistant.action.ActionStatus
import com.kone.assistant.command.IntentId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniEvaluationTest {
    @Test fun `ten command demo evaluation passes through intent and fake action`() {
        val results = MiniEvaluation().run(MiniEvaluation.DEMO_CASES)
        assertEquals(10, results.size)
        assertTrue(results.all { it.error == ErrorLabel.NONE })
        assertTrue(results.all { it.actionId == "FAKE_${it.case.expectedIntent.name}" })
    }

    @Test fun `transcript mismatch is labelled STT before downstream errors`() {
        val case = EvaluationCase("ERR-STT", "Feneri aç", "Feneri kapat", IntentId.TORCH_ON)
        assertEquals(ErrorLabel.STT, MiniEvaluation().run(listOf(case)).single().error)
    }

    @Test fun `wrong intent with correct transcript is labelled INTENT`() {
        val case = EvaluationCase("ERR-INTENT", "Feneri aç", "Feneri aç", IntentId.TORCH_OFF)
        assertEquals(ErrorLabel.INTENT, MiniEvaluation().run(listOf(case)).single().error)
    }

    @Test fun `failed action with correct STT and intent is labelled ACTION`() {
        val failingAction = ActionExecutor { ActionResult(ActionStatus.NO_OP, "NO_OP", "Injected failure") }
        val case = EvaluationCase("ERR-ACTION", "Feneri aç", "Feneri aç", IntentId.TORCH_ON)
        assertEquals(ErrorLabel.ACTION, MiniEvaluation(actionExecutor = failingAction).run(listOf(case)).single().error)
    }
}
