plugins {
    java
}

group = "org.eclipse.jetty"

dependencies {
    implementation("org.apache.velocity:velocity-engine-core:2.3")
    implementation("org.apache.velocity:velocity-tools:2.0")
}

configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("org.apache.velocity:velocity-engine-core"))
            .using(project(":sub3"))
        substitute(module("org.apache.velocity:velocity-tools"))
            .using(module("org.apache.ant:ant:1.10.14"))
    }
}