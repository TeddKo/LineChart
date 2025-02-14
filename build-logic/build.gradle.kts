plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
}

dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.ksp.gradlePlugin)
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

        register("ComposeApplication") {
            id = "tddy.ko.linechart.compose.application"
            implementationClass = "ComposeApplicationPlugin"
        }

        register("ComposeLibrary") {
            id = "tddy.ko.linechart.compose.library"
            implementationClass = "ComposeLibraryPlugin"
        }

        register("TestLibrary") {
            id = "tddy.ko.linechart.test.library"
            implementationClass = "TestLibraryPlugin"
        }
    }
}