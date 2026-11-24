package com.akingstar.askai.engine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
@Serializable data class PlanStep(val action:String,val value:String="",val requiresConfirmation:Boolean=false)
@Serializable data class ActionPlan(val steps:List<PlanStep>)
object Planner {
 private val json=Json{ignoreUnknownKeys=false;isLenient=false}
 private val stop=Regex("(^|\\s)(ruko|bas|band karo|stop|cancel)(\\s|$)",RegexOption.IGNORE_CASE)
 private val scams=listOf("digital arrest","otp batao","kyc update","lottery jeet","cbi","pin batao","password batao","unknown link")
 fun isStop(text:String)=stop.containsMatchIn(text.trim())
 fun scamWarning(text:String):String?=if(scams.any{text.contains(it,true)}) "सावधान जी। यह धोखाधड़ी हो सकती है। OTP, PIN या पासवर्ड किसी को न बताएं और अनजान लिंक न खोलें।" else null
 fun plan(text:String):ActionPlan { val t=text.trim().lowercase(); if(isStop(t))return ActionPlan(listOf(PlanStep("stop"))); scamWarning(t)?.let{return ActionPlan(listOf(PlanStep("speak",it)))}; val s=mutableListOf<PlanStep>()
  if(Regex("(^|\\s)(call|phone|कॉल|फोन)(\\s|$)").containsMatchIn(t))s+=PlanStep("call",t.substringAfterLast(" "),true)
  if(t.contains("whatsapp")||t.contains("व्हाट्सएप"))s+=PlanStep("whatsapp",t,false)
  if(t.contains("photo")||t.contains("फोटो")||t.contains("तस्वीर"))s+=PlanStep("camera")
  if(t.contains("alarm")||t.contains("अलार्म")||t.contains("जगाना"))s+=PlanStep("alarm")
  if(t.contains("youtube")||t.contains("यूट्यूब")||t.contains("भजन"))s+=PlanStep("youtube",if(t.contains("भजन"))"भजन" else "")
  if(t.contains("map")||t.contains("रास्ता")||t.contains("नेविगेशन"))s+=PlanStep("maps",t)
  if(t.contains("मदद")||t.contains("sos")||t.contains("बचाओ"))s+=PlanStep("sos","",true)
  if(s.isEmpty())s+=PlanStep("speak","माफ कीजिए जी, मैं समझ नहीं पाया। कृपया थोड़ा आसान शब्दों में बताइए।")
  return ActionPlan(s.take(5)) }
 fun validate(raw:String):ActionPlan?=runCatching{json.decodeFromString<ActionPlan>(raw).takeIf{it.steps.isNotEmpty()&&it.steps.size<=5&&it.steps.all{s->s.action in setOf("stop","speak","call","whatsapp","camera","alarm","youtube","maps","settings","sos")&&s.value.length<=300}}}.getOrNull()
}
