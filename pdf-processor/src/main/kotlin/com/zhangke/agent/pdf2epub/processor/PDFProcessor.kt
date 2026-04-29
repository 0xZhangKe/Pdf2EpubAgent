package com.zhangke.agent.pdf2epub.processor

import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import java.io.File
import javax.imageio.ImageIO

class PDFProcessor {

    companion object {

        private const val RENDER_DPI = 200f
    }

    fun pdf2Images(path: String): List<String> {
        require(path.isNotBlank()) {
            "Please pass a pdf file path."
        }

        val pdfFile = File(path).absoluteFile
        require(pdfFile.isFile) {
            "PDF file does not exist: ${pdfFile.absolutePath}"
        }
        require(pdfFile.extension.equals("pdf", ignoreCase = true)) {
            "Input file is not a PDF: ${pdfFile.absolutePath}"
        }

        val outputDirectory = File("files", pdfFile.nameWithoutExtension).absoluteFile

        Loader.loadPDF(pdfFile).use { document ->
            val numberOfPages = document.numberOfPages
            val expectedFiles = expectedPageFiles(outputDirectory, numberOfPages)
            if (outputDirectory.isDirectory && containsExpectedPageImages(outputDirectory, expectedFiles)) {
                return expectedFiles.map { it.absolutePath }
            }

            outputDirectory.mkdirs()
            require(outputDirectory.isDirectory) {
                "Failed to create output directory: ${outputDirectory.absolutePath}"
            }

            outputDirectory.listFiles()
                .orEmpty()
                .filter { it.isFile && it.isImageFile() }
                .forEach {
                    require(it.delete()) {
                        "Failed to delete old image: ${it.absolutePath}"
                    }
                }

            val renderer = PDFRenderer(document)
            expectedFiles.forEachIndexed { pageIndex, outputFile ->
                val image = renderer.renderImageWithDPI(pageIndex, RENDER_DPI, ImageType.RGB)
                require(ImageIO.write(image, "png", outputFile)) {
                    "Failed to write image: ${outputFile.absolutePath}"
                }
                println("Rendered page ${pageIndex + 1}/$numberOfPages: ${outputFile.absolutePath}")
            }

            return expectedFiles.map { it.absolutePath }
        }
    }

    private fun expectedPageFiles(outputDirectory: File, numberOfPages: Int): List<File> {
        return (1..numberOfPages).map { pageNumber ->
            outputDirectory.resolve("page-$pageNumber.png")
        }
    }

    private fun containsExpectedPageImages(outputDirectory: File, expectedFiles: List<File>): Boolean {
        val imageFiles = outputDirectory.listFiles()
            .orEmpty()
            .filter { it.isFile && it.isImageFile() }
            .map { it.name }
            .toSet()
        val expectedFileNames = expectedFiles.map { it.name }.toSet()
        return imageFiles.size == expectedFiles.size && imageFiles == expectedFileNames
    }

    private fun File.isImageFile(): Boolean {
        return extension.lowercase() in setOf("png", "jpg", "jpeg", "webp", "bmp", "gif")
    }
}
