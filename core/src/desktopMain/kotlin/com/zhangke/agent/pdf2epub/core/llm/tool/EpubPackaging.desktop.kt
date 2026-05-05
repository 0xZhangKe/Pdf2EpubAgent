package com.zhangke.agent.pdf2epub.core.llm.tool

import java.io.File
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal actual fun buildEpub(inputDirectoryPath: String, outputPath: String?): String {
    require(inputDirectoryPath.isNotBlank()) {
        "Please pass an EPUB source directory path."
    }

    val sourceDirectory = File(inputDirectoryPath).absoluteFile
    require(sourceDirectory.isDirectory) {
        "EPUB source directory does not exist: ${sourceDirectory.absolutePath}"
    }

    val outputFile = outputPath
        ?.takeIf { it.isNotBlank() }
        ?.let { File(it).absoluteFile }
        ?: sourceDirectory.parentFile.resolve("${sourceDirectory.name}.epub")
    outputFile.parentFile?.mkdirs()
    require(outputFile.parentFile == null || outputFile.parentFile.isDirectory) {
        "Failed to create EPUB output directory: ${outputFile.parentFile.absolutePath}"
    }
    require(!outputFile.isDirectory) {
        "EPUB output path is a directory: ${outputFile.absolutePath}"
    }
    if (outputFile.exists()) {
        require(outputFile.delete()) {
            "Failed to delete existing EPUB file: ${outputFile.absolutePath}"
        }
    }

    ZipOutputStream(outputFile.outputStream().buffered()).use { zip ->
        addMimeTypeEntry(zip, sourceDirectory)
        sourceDirectory.walkTopDown()
            .filter { it.isFile }
            .filterNot { it.name == "mimetype" && it.parentFile == sourceDirectory }
            .sortedBy { it.relativeTo(sourceDirectory).invariantSeparatorsPath }
            .forEach { file ->
                zip.putNextEntry(ZipEntry(file.relativeTo(sourceDirectory).invariantSeparatorsPath))
                file.inputStream().use { input -> input.copyTo(zip) }
                zip.closeEntry()
            }
    }

    return outputFile.absolutePath
}

private fun addMimeTypeEntry(zip: ZipOutputStream, sourceDirectory: File) {
    val mimeTypeFile = sourceDirectory.resolve("mimetype")
    val mimeTypeBytes = if (mimeTypeFile.isFile) {
        mimeTypeFile.readBytes()
    } else {
        "application/epub+zip".toByteArray()
    }

    val crc = CRC32().apply { update(mimeTypeBytes) }
    val entry = ZipEntry("mimetype").apply {
        method = ZipEntry.STORED
        size = mimeTypeBytes.size.toLong()
        compressedSize = mimeTypeBytes.size.toLong()
        this.crc = crc.value
    }

    zip.putNextEntry(entry)
    zip.write(mimeTypeBytes)
    zip.closeEntry()
}
