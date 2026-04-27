package com.zhangke.agent.pdf2epub.core

import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import java.io.File
import javax.imageio.ImageIO

private const val RENDER_DPI = 200f

fun main() {
    val filePath = "/Users/zhangke/Downloads/A-Historical-Introduction-to-Philosophy.pdf"
    require(filePath.isNotBlank()) {
        "Please pass a pdf file path as the first argument."
    }

    val pdfFile = File(filePath).absoluteFile
    require(pdfFile.isFile) {
        "PDF file does not exist: ${pdfFile.absolutePath}"
    }
    require(pdfFile.extension.equals("pdf", ignoreCase = true)) {
        "Input file is not a PDF: ${pdfFile.absolutePath}"
    }

    val outputDirectory = File("files", pdfFile.nameWithoutExtension).absoluteFile
    outputDirectory.mkdirs()
    require(outputDirectory.isDirectory) {
        "Failed to create output directory: ${outputDirectory.absolutePath}"
    }

    Loader.loadPDF(pdfFile).use { document ->
        val renderer = PDFRenderer(document)
        for (pageIndex in 0 until document.numberOfPages.coerceAtMost(50)) {
            val image = renderer.renderImageWithDPI(pageIndex, RENDER_DPI, ImageType.RGB)
            val outputFile = outputDirectory.resolve("page-${(pageIndex + 1).toString().padStart(4, '0')}.png")
            ImageIO.write(image, "png", outputFile)
            println("Rendered page ${pageIndex + 1}/${document.numberOfPages}: ${outputFile.absolutePath}")
        }
    }

    println("PDF images saved to: ${outputDirectory.absolutePath}")
}
