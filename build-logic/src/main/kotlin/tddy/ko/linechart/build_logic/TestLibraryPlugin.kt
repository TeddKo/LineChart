import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import tddy.ko.linechart.build_logic.extension.library
import tddy.ko.linechart.build_logic.extension.libs

class TestLibraryPlugin: Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            dependencies {
                add("api", libs.library("junit"))
                add("implementation", libs.library("androidx-junit"))
                add("implementation", libs.library("androidx-espresso-core"))
                add("implementation", libs.library("androidx-compose-ui-test-manifest"))
                add("implementation", libs.library("androidx-compose-ui-test"))
            }
        }
    }
}