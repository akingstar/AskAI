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

    @Test fun preservesHindiMatrasInDynamicCommands() {
        val tap = OfflineParser.parse("Search बटन दबाओ")?.steps?.single()
        val typing = OfflineParser.parse("Search में मौसम लिखो")?.steps?.single()
        assertEquals("tap_text", tap?.action)
        assertEquals("type_text", typing?.action)
        assertEquals("मौसम", typing?.value)
    }

    @Test fun parsesHindiLongPressPhrase() {
        val step = OfflineParser.parse("Photo पर लंबे समय तक दबाओ")?.steps?.single()
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

    @Test fun rejectsStaticCommandWordsInsideUnrelatedSentences() {
        assertNull(OfflineParser.parse("please check my battery status"))
        assertNull(OfflineParser.parse("the wifi signal is weak"))
        assertNull(OfflineParser.parse("my location is not updating"))
        assertNull(OfflineParser.parse("battery 20 percent"))
    }

    @Test fun acceptsKnownCommandWithRecognizedImperativeSuffix() {
        assertEquals("torch", OfflineParser.parse("टॉर्च जलाओ")?.steps?.single()?.action)
        assertEquals("torch", OfflineParser.parse("torch jalao")?.steps?.single()?.action)
        assertEquals("torch_off", OfflineParser.parse("torch band")?.steps?.single()?.action)
    }

    @Test fun staticActionsUseEmptyArgumentsWhereExpected() {
        val cases = listOf(
            "टॉर्च जलाओ" to "torch",
            "open camera" to "camera",
            "go home" to "home",
            "read screen" to "read_screen",
            "wifi" to "wifi_settings",
            "battery" to "battery"
        )

        cases.forEach { (command, expectedAction) ->
            val step = OfflineParser.parse(command)?.steps?.single()
            assertEquals(command, expectedAction, step?.action)
            assertEquals(command, "", step?.target)
            assertEquals(command, "", step?.value)
            assertNotNull(command, PlanValidator.parse(
                PlanValidator.encode(ActionPlan("en-IN", listOf(step!!)))
            ))
        }
    }

    @Test fun youtubeAndMapsOpenCommandsUseCanonicalValues() {
        val youtube = OfflineParser.parse("youtube kholo")?.steps?.single()
        assertEquals("youtube", youtube?.action)
        assertEquals("youtube", youtube?.value)

        val maps = OfflineParser.parse("maps kholo")?.steps?.single()
        assertEquals("maps", maps?.action)
        assertEquals("maps", maps?.value)
    }

    @Test fun webSearchPreservesTheActualSearchQuery() {
        val english = OfflineParser.parse("search weather today")?.steps?.single()
        assertEquals("web", english?.action)
        assertEquals("weather today", english?.value)

        val hinglish = OfflineParser.parse("google par search delhi weather")?.steps?.single()
        assertEquals("web", hinglish?.action)
        assertEquals("delhi weather", hinglish?.value)

        val hindi = OfflineParser.parse("खोजो मौसम")?.steps?.single()
        assertEquals("web", hindi?.action)
        assertEquals("मौसम", hindi?.value)
    }

    @Test fun rejectsSensitiveTypingTarget() {
        assertNull(OfflineParser.parse("Password में hello लिखो"))
    }

    @Test fun parsesBrightnessPercentCommands() {
        val cases = listOf(
            "brightness 30 percent kar do" to "30",
            "Brightness 70%" to "70",
            "screen brightness 45 set karo" to "45",
            "ब्राइटनेस 60 प्रतिशत कर दो" to "60",
            "रोशनी 25 प्रतिशत" to "25"
        )
        cases.forEach { (command, expected) ->
            val step = OfflineParser.parse(command)?.steps?.single()
            assertEquals(command, "brightness", step?.action)
            assertEquals(command, "", step?.target)
            assertEquals(command, expected, step?.value)
            assertNotNull(
                command,
                PlanValidator.parse(
                    PlanValidator.encode(ActionPlan("hi-IN", listOf(step!!)))
                )
            )
        }
    }

    @Test fun parsesBrightnessIncreaseAndDecrease() {
        assertEquals(
            "up",
            OfflineParser.parse("brightness badhao")?.steps?.single()?.value
        )
        assertEquals(
            "down",
            OfflineParser.parse("brightness kam karo")?.steps?.single()?.value
        )
        assertEquals(
            "up",
            OfflineParser.parse("ब्राइटनेस बढ़ाओ")?.steps?.single()?.value
        )
        assertEquals(
            "down",
            OfflineParser.parse("रोशनी घटाओ")?.steps?.single()?.value
        )
    }

    @Test fun rejectsOutOfRangeBrightnessCommands() {
        assertNull(OfflineParser.parse("brightness 0 percent"))
        assertNull(OfflineParser.parse("brightness 101 percent"))
        assertNull(OfflineParser.parse("brightness 150"))
    }

}
