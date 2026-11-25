package com.akingstar.askai

/**
 * Safe voice-first WhatsApp draft conversation.
 * This class never sends messages or launches external apps.
 */
class WhatsAppDraftFlow {

    enum class State {
        IDLE,
        AWAITING_MESSAGE,
        AWAITING_CONFIRMATION
    }

    sealed class Result {
        data object AskForMessage : Result()
        data class Preview(val message: String) : Result()
        data class OpenDraft(val message: String) : Result()
        data object Cancelled : Result()
        data object RepeatPreview : Result()
        data object MessageRejected : Result()
        data object Clarify : Result()
        data object Ignored : Result()
    }

    var state: State = State.IDLE
        private set

    private var draft: String = ""

    fun begin(): Result {
        draft = ""
        state = State.AWAITING_MESSAGE
        return Result.AskForMessage
    }

    fun accept(raw: String): Result {
        val input = raw.trim()
        if (input.isBlank()) return Result.MessageRejected

        if (isCancel(input)) {
            reset()
            return Result.Cancelled
        }

        return when (state) {
            State.IDLE -> Result.Ignored

            State.AWAITING_MESSAGE -> {
                val normalized = SmsDraftValidator.normalize(input)
                    ?: return Result.MessageRejected

                draft = normalized
                state = State.AWAITING_CONFIRMATION
                Result.Preview(draft)
            }

            State.AWAITING_CONFIRMATION -> when {
                isRepeat(input) -> Result.RepeatPreview

                isEdit(input) -> {
                    draft = ""
                    state = State.AWAITING_MESSAGE
                    Result.AskForMessage
                }

                isConfirm(input) -> {
                    val confirmed = draft
                    reset()
                    Result.OpenDraft(confirmed)
                }

                else -> Result.Clarify
            }
        }
    }

    fun currentDraft(): String? =
        draft.takeIf { state == State.AWAITING_CONFIRMATION }

    fun reset() {
        draft = ""
        state = State.IDLE
    }

    private fun isCancel(value: String): Boolean {
        val text = value.lowercase()
        return text in setOf(
            "cancel", "stop", "रद्द", "रद्द करो",
            "कैंसिल", "कैंसिल करो", "नहीं", "नहीं चाहिए"
        )
    }

    private fun isRepeat(value: String): Boolean {
        val text = value.lowercase()
        return text in setOf(
            "repeat", "repeat karo", "dobara sunao",
            "फिर सुनाओ", "दोबारा सुनाओ", "दोबारा बोलो",
            "फिर से बोलो"
        )
    }

    private fun isEdit(value: String): Boolean {
        val text = value.lowercase()
        return text in setOf(
            "edit", "change", "badlo", "बदलो",
            "बदल दो", "सुधारो", "संदेश बदलो"
        )
    }

    private fun isConfirm(value: String): Boolean {
        val text = value.lowercase()
        return text in setOf(
            "yes", "haan", "han", "confirm",
            "हाँ", "हां", "जी हाँ", "जी हां",
            "हाँ खोलो", "हां खोलो", "ठीक है"
        )
    }
}
