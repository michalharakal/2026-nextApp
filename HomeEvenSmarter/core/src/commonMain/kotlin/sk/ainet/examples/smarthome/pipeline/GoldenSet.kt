package sk.ainet.examples.smarthome.pipeline

import sk.ainet.examples.smarthome.tools.HomeTools

/**
 * One rehearsed utterance and the function it must resolve to (`null` = must not call a home function).
 * [remote] marks a tool the companion middleware executes: the case scores on the NLU hit alone, so the
 * golden set keeps separating understanding errors from infrastructure (no companion reachable).
 */
data class GoldenCase(val utterance: String, val expectedTool: String?, val remote: Boolean = false)

/**
 * The utterances spoken on stage, also the typed regression set: run them through the NLU alone to separate
 * recognition errors from understanding errors. Eleven in-catalog (one of them a remote tool the companion
 * executes), one deliberately outside it (it must land on the escalation seam, not on a wrong function).
 */
object GoldenSet {
    val cases: List<GoldenCase> = listOf(
        GoldenCase("Turn on the kitchen light", HomeTools.SET_LIGHT),
        GoldenCase("Switch off the bedroom light", HomeTools.SET_LIGHT),
        GoldenCase("Dim the living room light to twenty percent", HomeTools.SET_LIGHT),
        GoldenCase("Set the bedroom to twenty one degrees", HomeTools.SET_THERMOSTAT),
        GoldenCase("Make it nineteen degrees in the whole house", HomeTools.SET_THERMOSTAT),
        GoldenCase("Close the living room blinds", HomeTools.SET_BLINDS),
        GoldenCase("Open the kitchen blinds halfway", HomeTools.SET_BLINDS),
        GoldenCase("Lock the front door", HomeTools.SET_LOCK),
        GoldenCase("Movie time", HomeTools.SET_SCENE),
        GoldenCase("What is the status of the hallway", HomeTools.GET_STATUS),
        GoldenCase("What is the weather like tomorrow", HomeTools.GET_WEATHER, remote = true),
        GoldenCase("Order a pizza for dinner", null),
    )
}
