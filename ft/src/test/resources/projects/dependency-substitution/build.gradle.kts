plugins {
    id("org.octopusden.octopus.license-management")
    java
}

group = "org.octopusden.octopus.mytest"

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.auth0:jwks-rsa:0.22.0")
}

subprojects {
    version = project.version
}

java {
    withSourcesJar()
}

tasks.register<Zip>("zipDistributive") {
    archiveFileName = "${project.name}.zip"
    destinationDirectory = file("build/distr")
    from(tasks.processLicenses) {
        into("licenses")
    }
}
tasks.assemble {
    dependsOn(tasks["zipDistributive"])
}
tasks.build {
    dependsOn(tasks["assemble"])
}
