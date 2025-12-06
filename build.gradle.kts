plugins {
    kotlin("jvm") version "1.9.22"
    id("maven-publish")
    id("signing")
}

group = "io.github.respectlytics"
version = "1.0.0"

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
            version = "1.0.0"
            
            from(components["java"])
            
            pom {
                name.set("Respectlytics Kotlin SDK")
                description.set("Privacy-first analytics SDK for Android/Kotlin")
                url.set("https://github.com/respectlytics/respectlytics-kotlin")
                
                licenses {
                    license {
                        name.set("Proprietary")
                        url.set("https://github.com/respectlytics/respectlytics-kotlin/blob/main/LICENSE")
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
    
    repositories {
        maven {
            name = "OSSRH"
            url = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
            credentials {
                username = project.findProperty("ossrhUsername") as String? ?: ""
                password = project.findProperty("ossrhPassword") as String? ?: ""
            }
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["maven"])
}
