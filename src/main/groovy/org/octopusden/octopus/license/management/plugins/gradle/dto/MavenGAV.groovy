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
    List<MavenExcludeRule> excludeRules

    def logString() {
        "$group:$artifact" + (version ? ":$version" : "") +
                (classifier ? ":$classifier" : "") +
                (extension ? "@$extension" : "") +
                (excludeRules ? "{\n" +
                        excludeRules.each { "exclude(group = ${it.group}, module = ${it.artifact}\n" } +
                        "}" : "")
    }
}
