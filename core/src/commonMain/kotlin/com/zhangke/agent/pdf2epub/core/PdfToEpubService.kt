package com.zhangke.agent.pdf2epub.core

import com.zhangke.agent.pdf2epub.framework.currentPlatform
import kotlinx.serialization.Serializable

@Serializable
data class PdfToEpubRequest(
    val pdfPath: String,
    val epubPath: String,
)

@Serializable
data class PdfToEpubPlan(
    val sourcePath: String,
    val targetPath: String,
    val platformName: String,
)

class PdfToEpubService {

    fun createPlan(request: PdfToEpubRequest): PdfToEpubPlan {
        require(request.pdfPath.isNotBlank()) { "pdfPath is blank" }
        require(request.epubPath.isNotBlank()) { "epubPath is blank" }

        return PdfToEpubPlan(
            sourcePath = request.pdfPath.trim(),
            targetPath = request.epubPath.trim(),
            platformName = currentPlatform().name,
        )
    }
}
