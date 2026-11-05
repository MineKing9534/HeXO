package de.mineking.hexo.detekt

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

class CustomRuleSetProvider : RuleSetProvider {
    override val ruleSetId = RuleSetId("custom")

    override fun instance() = RuleSet(ruleSetId, listOf(::NonSerializableInterfaceProperty))
}
