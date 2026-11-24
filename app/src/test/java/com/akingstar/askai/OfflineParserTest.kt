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
}
