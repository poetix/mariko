plugins {
    kotlin("jvm") version "1.9.23"
    `maven-publish`
}

group = "com.codepoetics"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":mariko-core"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.codepoetics"
            artifactId = "mariko-kotlin"
            version = "1.0-SNAPSHOT"

            from(components["kotlin"])
        }
    }
}