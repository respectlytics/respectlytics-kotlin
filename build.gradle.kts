plugins {
    kotlin("jvm") version "1.9.22"
    id("maven-publish")
    id("signing")
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
}

group = "io.github.respectlytics"
version = "2.2.0"

repositories {
    mavenCentral()
    google()
}

dependencies {
    // Core dependencies
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.test {
    useJUnit()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "io.github.respectlytics"
            artifactId = "respectlytics-kotlin"
            version = project.version.toString()

            from(components["java"])

            pom {
                name.set("Respectlytics Kotlin SDK")
                description.set("Privacy-first analytics SDK for Android/Kotlin")
                url.set("https://github.com/respectlytics/respectlytics-kotlin")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("respectlytics")
                        name.set("Respectlytics")
                        email.set("respectlytics@loheden.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/respectlytics/respectlytics-kotlin.git")
                    developerConnection.set("scm:git:ssh://github.com:respectlytics/respectlytics-kotlin.git")
                    url.set("https://github.com/respectlytics/respectlytics-kotlin")
                }
            }
        }
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/content/repositories/snapshots/"))
            username.set(project.findProperty("ossrhUsername") as String? ?: System.getenv("OSSRH_USERNAME") ?: "")
            password.set(project.findProperty("ossrhPassword") as String? ?: System.getenv("OSSRH_PASSWORD") ?: "")
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["maven"])
}
