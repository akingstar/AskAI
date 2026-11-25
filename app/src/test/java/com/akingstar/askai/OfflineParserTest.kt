package com.akingstar.askai

import org.junit.Assert.*
import org.junit.Test

class OfflineParserTest {
    @Test fun recognizesHindiTorch() {
        assertEquals("torch", OfflineParser.parse("टॉर्च जलाओ")?.steps?.first()?.action)
    }
    @Test fun recognizesEnglishCamera() {
        assertEquals("camera", OfflineParser.parse("open camera")?.steps?.first()?.action)
    }


    @Test fun rejectsHindiConditionalCommandInOfflineMode() {
        assertNull(OfflineParser.parse("अगर बैटरी 20 प्रतिशत से कम हो तो टॉर्च जलाओ"))
    }

    @Test fun rejectsHinglishConditionalCommandInOfflineMode() {
        assertNull(OfflineParser.parse("battery 20 percent se kam ho to torch jalao"))
    }

    @Test fun rejectsEnglishConditionalCommandInOfflineMode() {
        assertNull(OfflineParser.parse("if battery is below 20 then turn on torch"))
    }

    @Test fun rejectsPreviousStepConditionInOfflineMode() {
        assertNull(OfflineParser.parse("if previous step succeeds then open camera"))
    }

    @Test fun preservesNormalOfflineCommands() {
        assertEquals("torch", OfflineParser.parse("टॉर्च जलाओ")?.steps?.first()?.action)
        assertEquals("camera", OfflineParser.parse("open camera")?.steps?.first()?.action)
    }

    @Test fun rejectsEntireCommandWhenOnePartIsUnknown() {
        assertNull(OfflineParser.parse("youtube kholo and send sms"))
    }

    @Test fun preservesMultiStepCommandWhenEveryPartIsRecognized() {
        val plan = OfflineParser.parse("टॉर्च जलाओ और कैमरा खोलो")
        assertNotNull(plan)
        assertEquals(2, plan!!.steps.size)
        assertEquals("torch", plan.steps[0].action)
        assertEquals("camera", plan.steps[1].action)
    }


    @Test fun parsesHindiTapCommand() {
        val step = OfflineParser.parse("Search बटन दबाओ")?.steps?.single()
        assertEquals("tap_text", step?.action)
        assertEquals("search", step?.target)
    }

    @Test fun parsesHinglishTapCommandWithoutIncludingParInTarget() {
        val step = OfflineParser.parse("Search par click karo")?.steps?.single()
        assertEquals("tap_text", step?.action)
        assertEquals("search", step?.target)
    }

    @Test fun parsesEnglishTapCommand() {
        val step = OfflineParser.parse("tap Search")?.steps?.single()
        assertEquals("tap_text", step?.action)
        assertEquals("search", step?.target)
    }

    @Test fun parsesHindiTypeCommand() {
        val step = OfflineParser.parse("Search में मौसम लिखो")?.steps?.single()
        assertEquals("type_text", step?.action)
        assertEquals("search", step?.target)
        assertEquals("मौसम", step?.value)
    }

    @Test fun parsesEnglishTypeCommand() {
        val step = OfflineParser.parse("type weather in Search")?.steps?.single()
        assertEquals("type_text", step?.action)
        assertEquals("search", step?.target)
        assertEquals("weather", step?.value)
    }

    @Test fun parsesLongPressCommand() {
        val step = OfflineParser.parse("Photo पर long press करो")?.steps?.single()
        assertEquals("long_press_text", step?.action)
        assertEquals("photo", step?.target)
    }

    @Test fun rejectsMissingDynamicTarget() {
        assertNull(OfflineParser.parse("बटन दबाओ"))
        assertNull(OfflineParser.parse("type weather in"))
    }

    @Test fun rejectsRiskyDynamicTap() {
        assertNull(OfflineParser.parse("Delete बटन दबाओ"))
    }

    @Test fun rejectsRiskyDynamicLongPress() {
        assertNull(OfflineParser.parse("Delete पर long press करो"))
    }

    @Test fun rejectsSensitiveTypingTarget() {
        assertNull(OfflineParser.parse("Password में hello लिखो"))
    }

}
