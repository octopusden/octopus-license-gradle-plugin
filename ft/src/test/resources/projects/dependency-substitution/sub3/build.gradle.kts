plugins{
    java
}

group = "org.eclipse.jetty"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url "https://plugins.gradle.org/m2/"
    }
}

dependencies{
    implementation("com.google.guava:guava:33.2.1-jre")
    implementation("org.eclipse.jetty:jetty-util:9.4.51.v20230217")
}

