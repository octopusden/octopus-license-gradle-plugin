package org.octopusden.octopus.license.management.plugins.gradle.dto

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class MavenExcludeRule {
    String group
    String artifact
}
