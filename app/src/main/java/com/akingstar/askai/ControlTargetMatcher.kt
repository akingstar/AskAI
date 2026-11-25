package com.akingstar.askai

import java.text.Normalizer
import java.util.Locale

object ControlTargetMatcher {
    fun normalize(label: String): String =
        Normalizer.normalize(label, Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT)
            .replace(Regex("\\s+"), " ")
            .trim()

    fun matches(label: String, target: String): Boolean =
        label.isNotBlank() && target.isNotBlank() &&
            normalize(label) == normalize(target)

    fun uniqueExactIndex(labels: List<String>, target: String): Int? {
        if (target.isBlank()) return null
        return labels.indices.filter { matches(labels[it], target) }.singleOrNull()
    }
}
