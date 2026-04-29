package com.zhangke.agent.pdf2epub.core

fun main() {
    val filePath = "/Users/zhangke/Downloads/Technology-and-Civilization.pdf"
    val imagePaths = PDFProcessor().pdf2Images(filePath)
    println("PDF images saved: ${imagePaths.joinToString()}")
}
