package com.akingstar.askai

/** Answer mode: a general question gets one spoken answer, never a phone action. */
internal object AnswerPolicy {
    const val MAX_ANSWER_CHARS = 400

    fun isAnswerPlan(plan: ActionPlan): Boolean =
        plan.steps.size == 1 && plan.steps[0].action == "answer"

    fun displayText(plan: ActionPlan): String? =
        if (isAnswerPlan(plan)) plan.steps[0].value.take(MAX_ANSWER_CHARS) else null
}
