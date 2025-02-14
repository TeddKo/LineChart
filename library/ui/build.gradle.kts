import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.tddy.ko.android.library)
    alias(libs.plugins.tddy.ko.compose.library)
    alias(libs.plugins.tddy.ko.test.library)
    alias(libs.plugins.vanniktech.maven)
}

android { namespace = "tddy.ko.linechart.ui" }

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    signAllPublications()

    coordinates("io.github.teddko", "linechart", "1.0.0")

    pom {
        name.set("linechart")
        description.set("Jetpack Compose linechart Library")
        url.set("https://github.com/TeddKo/linechart")
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
            url.set("https://github.com/TeddKo/linechart")
            connection.set("scm:git:git://github.com/TeddKo/linechart.git")
            developerConnection.set("scm:git:git://github.com/TeddKo/linechart.git")
        }
    }
}