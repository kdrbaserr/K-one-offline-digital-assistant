package com.kone.assistant.evaluation

import com.kone.assistant.action.ActionStatus
import com.kone.assistant.action.ActionExecutor
import com.kone.assistant.action.FakeActionExecutor
import com.kone.assistant.command.CommandResolver
import com.kone.assistant.command.IntentId
import com.kone.assistant.command.TurkishCommandNormalizer

enum class ErrorLabel { NONE, STT, INTENT, ACTION }

data class EvaluationCase(
    val id: String,
    val expectedTranscript: String,
    val actualTranscript: String,
    val expectedIntent: IntentId,
)

data class EvaluationResult(
    val case: EvaluationCase,
    val actualIntent: IntentId,
    val actionId: String,
    val error: ErrorLabel,
)

class MiniEvaluation(
    private val resolver: CommandResolver = CommandResolver(),
    private val actionExecutor: ActionExecutor = FakeActionExecutor(),
    private val normalizer: TurkishCommandNormalizer = TurkishCommandNormalizer(),
) {
    fun run(cases: List<EvaluationCase>): List<EvaluationResult> = cases.map { case ->
        val intent = resolver.resolve(case.actualTranscript)
        val action = actionExecutor.execute(intent)
        val label = when {
            normalizer.normalize(case.actualTranscript).text != normalizer.normalize(case.expectedTranscript).text -> ErrorLabel.STT
            intent.id != case.expectedIntent -> ErrorLabel.INTENT
            action.status != ActionStatus.SIMULATED || action.actionId != "FAKE_${case.expectedIntent.name}" -> ErrorLabel.ACTION
            else -> ErrorLabel.NONE
        }
        EvaluationResult(case, intent.id, action.actionId, label)
    }

    companion object {
        val DEMO_CASES = listOf(
            EvaluationCase("DEMO-01", "Eve yol tarifi başlat", "Eve yol tarifi başlat", IntentId.MAP_HOME),
            EvaluationCase("DEMO-02", "En yakın eczane ara", "En yakın eczane ara", IntentId.MAP_NEARBY),
            EvaluationCase("DEMO-03", "Annemi ara", "Annemi ara", IntentId.CALL_CONTACT),
            EvaluationCase("DEMO-04", "Ayşe'ye gecikeceğim yaz", "Ayşe'ye gecikeceğim yaz", IntentId.MESSAGE_TEXT),
            EvaluationCase("DEMO-05", "Spotify'da caz çal", "Spotify'da caz çal", IntentId.PLAY_SPOTIFY),
            EvaluationCase("DEMO-06", "YouTube'da Kotlin dersi ara", "YouTube'da Kotlin dersi ara", IntentId.SEARCH_YOUTUBE),
            EvaluationCase("DEMO-07", "WhatsApp'ı aç", "WhatsApp'ı aç", IntentId.OPEN_APP),
            EvaluationCase("DEMO-08", "Kamerayı aç", "Kamerayı aç", IntentId.OPEN_CAMERA),
            EvaluationCase("DEMO-09", "Feneri aç", "Feneri aç", IntentId.TORCH_ON),
            EvaluationCase("DEMO-10", "10 dakikalık zamanlayıcı kur", "10 dakikalık zamanlayıcı kur", IntentId.SET_TIMER),
        )
    }
}
