import org.gradle.api.Plugin
import org.gradle.api.Project
import tddy.ko.linechart.build_logic.extension.applicationExtension
import tddy.ko.linechart.build_logic.extension.configureAndroidExtension
import tddy.ko.linechart.build_logic.extension.libs
import tddy.ko.linechart.build_logic.extension.plugin

class AndroidApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply(libs.plugin("android-application").pluginId)
                apply(libs.plugin("kotlin-android").pluginId)
            }
            applicationExtension.apply {
                configureAndroidExtension(this)
                defaultConfig.targetSdk = 35
            }
        }
    }
}