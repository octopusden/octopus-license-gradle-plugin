plugins {
    java
}

group = "org.eclipse.jetty"

dependencies {
    implementation("org.eclipse.jetty:jetty-util:9.4.51.v20230217") {

    }
}

configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("org.eclipse.jetty:jetty-util"))
            .using(project(":sub3"))
    }
}