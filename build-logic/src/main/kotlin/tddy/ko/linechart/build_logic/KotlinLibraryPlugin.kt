import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import tddy.ko.linechart.build_logic.extension.libs
import tddy.ko.linechart.build_logic.extension.library
import tddy.ko.linechart.build_logic.extension.plugin

class KotlinLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.plugin("kotlin-jvm").pluginId)

            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            extensions.configure<KotlinJvmProjectExtension> {
                coreLibrariesVersion = libs.findVersion("kotlinStdlib").get().requiredVersion
                explicitApi()
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                    apiVersion.set(KotlinVersion.KOTLIN_2_2)
                    languageVersion.set(KotlinVersion.KOTLIN_2_2)
                    allWarningsAsErrors.set(true)
                }
            }
            dependencies {
                add("testImplementation", libs.library("junit"))
            }
        }
    }
}
