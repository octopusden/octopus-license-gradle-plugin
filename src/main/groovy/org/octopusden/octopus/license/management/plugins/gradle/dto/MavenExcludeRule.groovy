package org.octopusden.octopus.license.management.plugins.gradle.dto

import groovy.transform.Canonical

@Canonical
class MavenExcludeRule {
    String group
    String artifact
}
