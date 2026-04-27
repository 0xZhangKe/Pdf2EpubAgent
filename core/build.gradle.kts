plugins {
    id("pdf2epub.kmp.library")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":framework"))
                implementation(libs.koin.core)
                implementation(libs.kotlin.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
            }
        }
        desktopMain {
            dependencies {
                implementation(libs.pdfbox)
            }
        }
    }
}
