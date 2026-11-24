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

}
