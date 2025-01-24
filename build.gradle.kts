plugins {
    java
    `maven-publish`
}

group = "com.codepoetics"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
}

tasks.test {
    useJUnitPlatform()
}