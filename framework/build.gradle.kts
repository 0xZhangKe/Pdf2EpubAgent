plugins {
    id("pdf2epub.kmp.library")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.koin.core)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.client.serialization.kotlinx.json)
                implementation(libs.kotlinx.serialization.json)
            }
        }
        desktopMain {
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }
    }
}
