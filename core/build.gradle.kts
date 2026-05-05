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
                api(libs.koog.agents)
                api(libs.koog.openai.client)
            }
        }
        desktopMain {
            dependencies {
                implementation(libs.pdfbox)
            }
        }
    }
}
