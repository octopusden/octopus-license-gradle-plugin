plugins {
    java
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url "https://plugins.gradle.org/m2/"
    }
}

dependencies {
    implementation("org.apache.ant:ant:1.10.14")
}