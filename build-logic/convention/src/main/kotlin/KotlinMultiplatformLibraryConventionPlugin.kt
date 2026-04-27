import com.zhangke.agent.pdf2epub.buildlogic.kotlinMultiplatform
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

class KotlinMultiplatformLibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            kotlinMultiplatform {
                jvm("desktop") {
                    compilerOptions {
                        jvmTarget.set(JvmTarget.JVM_17)
                    }
                }

                sourceSets.apply {
                    commonTest.dependencies {
                        implementation(kotlin("test"))
                    }
                    all {
                        languageSettings.optIn("kotlin.RequiresOptIn")
                    }
                }
            }
        }
    }
}
