package com.akingstar.askai

/**
 * Decides whether an already-validated offline plan may run before any online
 * provider is contacted. Free-form actions (tap/type/long press) and multi-step
 * plans containing a web search stay online-first, because the offline parser
 * splits on "and"/"aur" and could mis-split natural language.
 */
internal object OfflineFirstPolicy {
    private val freeFormActions = setOf("tap_text", "long_press_text", "type_text")

    fun isFastPathSafe(plan: ActionPlan): Boolean {
        if (plan.steps.isEmpty() || plan.steps.size > 5) return false
        if (plan.steps.any { it.condition != null }) return false
        if (plan.steps.any { it.action in freeFormActions }) return false
        if (plan.steps.size > 1 && plan.steps.any { it.action == "web" }) return false
        return true
    }
}
