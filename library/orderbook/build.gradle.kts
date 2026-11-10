plugins {
    alias(libs.plugins.tddy.ko.kotlin.library)
    alias(libs.plugins.vanniktech.maven)
}

/**
 * ponytail: POM block duplicated from library/ui, extract to a convention plugin when a third module publishes.
 */
mavenPublishing {
    publishToMavenCentral()

    val toCentral = gradle.startParameter.taskNames.any { "MavenCentral" in it }
    val hasKey = providers.gradleProperty("signingInMemoryKey").isPresent ||
        providers.gradleProperty("signing.keyId").isPresent
    if (toCentral || hasKey) {
        signAllPublications()
    }

    coordinates("io.github.teddko", "linechart-orderbook", libs.versions.linechart.get())

    pom {
        name.set("LineChart OrderBook")
        description.set("Pure Kotlin order book and matching engine for marketplace UIs")
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