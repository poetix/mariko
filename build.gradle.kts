plugins {
    java
    checkstyle
}

group = "com.codepoetics"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
}

tasks.test {
    useJUnitPlatform()
}