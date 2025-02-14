import org.gradle.api.Plugin
import org.gradle.api.Project
import tddy.ko.linechart.build_logic.extension.configureComposeExtension
import tddy.ko.linechart.build_logic.extension.libraryExtension
import tddy.ko.linechart.build_logic.extension.libs
import tddy.ko.linechart.build_logic.extension.plugin

class ComposeLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply(libs.plugin("android-library").pluginId)
                apply(libs.plugin("compose-compiler").pluginId)
            }

            configureComposeExtension(commonExtension = libraryExtension)
        }
    }
}