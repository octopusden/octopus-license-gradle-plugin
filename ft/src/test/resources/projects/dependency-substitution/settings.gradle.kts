pluginManagement {
    plugins {
        id("org.octopusden.octopus.license-management") version (extra["octopus-license-management-gradle-plugin.version"] as String)
    }
}

include("sub1")
include("sub2")
include("sub3")

rootProject.name = "dependency-substitution"