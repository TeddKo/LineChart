import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.tddy.ko.android.library)
    alias(libs.plugins.tddy.ko.compose.library)
    alias(libs.plugins.vanniktech.maven)
}

android {
    namespace = "tddy.ko.linechart.ui"
}

kotlin {
    coreLibrariesVersion = libs.versions.kotlinStdlib.get()
    explicitApi()
    compilerOptions {
        apiVersion.set(KotlinVersion.KOTLIN_2_2)
        languageVersion.set(KotlinVersion.KOTLIN_2_2)
    }
}

dependencies {
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.foundation)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

mavenPublishing {
    publishToMavenCentral()

    val toCentral = gradle.startParameter.taskNames.any { "MavenCentral" in it }
    val hasKey = providers.gradleProperty("signingInMemoryKey").isPresent ||
        providers.gradleProperty("signing.keyId").isPresent
    if (toCentral || hasKey) {
        signAllPublications()
    }

    coordinates("io.github.teddko", "linechart", libs.versions.linechart.get())

    pom {
        name.set("LineChart")
        description.set("Jetpack Compose LineChart Library")
        url.set("https://github.com/TeddKo/LineChart")
        inceptionYear.set("2025")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                id.set("TeddKo")
                name.set("Tedd Ko")
                email.set("tddy.ko@kakao.com")
                url.set("https://github.com/TeddKo")
            }
        }

        scm {
            url.set("https://github.com/TeddKo/LineChart")
            connection.set("scm:git:git://github.com/TeddKo/LineChart.git")
            developerConnection.set("scm:git:git://github.com/TeddKo/LineChart.git")
        }
    }
}