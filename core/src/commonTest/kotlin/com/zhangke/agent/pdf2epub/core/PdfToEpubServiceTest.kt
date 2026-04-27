package com.zhangke.agent.pdf2epub.core

import kotlin.test.Test
import kotlin.test.assertEquals

class PdfToEpubServiceTest {

    @Test
    fun `createPlan trims paths and records desktop platform`() {
        val plan = PdfToEpubService().createPlan(
            PdfToEpubRequest(
                pdfPath = " /tmp/source.pdf ",
                epubPath = " /tmp/output.epub ",
            ),
        )

        assertEquals("/tmp/source.pdf", plan.sourcePath)
        assertEquals("/tmp/output.epub", plan.targetPath)
        assertEquals("desktop", plan.platformName)
    }
}
