package org.octopusden.octopus.license.management.plugins.gradle.dto

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class MavenGAV {
    String project
    String configuration
    String group
    String artifact
    String version
    String classifier
    String extension

    def logString() {
        "$group:$artifact:$version${classifier != null ? ":$classifier" : ""}"
    }
}
