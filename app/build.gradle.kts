plugins {
    alias(libs.plugins.tddy.ko.android.application)
    alias(libs.plugins.tddy.ko.compose.application)
    alias(libs.plugins.tddy.ko.test.library)
}

android {
    namespace = "tddy.ko.linechart"

    defaultConfig {
        applicationId = "tddy.ko.linechart"
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation(projects.library.ui)
    implementation(projects.demo.designSystem)
    implementation(projects.demo.feature)
}