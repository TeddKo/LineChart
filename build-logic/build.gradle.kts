plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("AndroidApplication") {
            id = "tddy.ko.linechart.application"
            implementationClass = "AndroidApplicationPlugin"
        }

        register("AndroidLibrary") {
            id = "tddy.ko.linechart.library"
            implementationClass = "AndroidLibraryPlugin"
        }

        register("KotlinLibrary") {
            id = "tddy.ko.linechart.kotlin.library"
            implementationClass = "KotlinLibraryPlugin"
        }

        register("ComposeApplication") {
            id = "tddy.ko.linechart.compose.application"
            implementationClass = "ComposeApplicationPlugin"
        }

        register("ComposeLibrary") {
            id = "tddy.ko.linechart.compose.library"
            implementationClass = "ComposeLibraryPlugin"
        }
    }
}