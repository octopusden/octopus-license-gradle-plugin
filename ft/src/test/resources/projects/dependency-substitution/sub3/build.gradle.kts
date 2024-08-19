plugins{
    java
}

group = "org.eclipse.jetty"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
}

dependencies{
    implementation("com.google.guava:guava:33.2.1-jre")
    implementation("org.eclipse.jetty:jetty-util:9.4.51.v20230217")
}

