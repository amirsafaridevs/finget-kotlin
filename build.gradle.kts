plugins {
    kotlin("jvm") version "1.9.24"
    `maven-publish`
}

group = "ir.finget"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
}

java {
    withSourcesJar()
    withJavadocJar()
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    jvmToolchain(11)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = "ir.finget"
            artifactId = "sdk"
            version = project.version.toString()
            pom {
                name.set("Finget Kotlin SDK")
                description.set("Official Kotlin SDK for the Finget passwordless login and notification API")
                url.set("https://finget.ir")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        name.set("Finget")
                        url.set("https://finget.ir")
                    }
                }
                scm {
                    url.set("https://github.com/amirsafaridevs/finget-kotlin")
                    connection.set("scm:git:git://github.com/amirsafaridevs/finget-kotlin.git")
                }
            }
        }
    }
}
