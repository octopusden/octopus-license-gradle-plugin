package org.octopusden.octopus.license.management.plugins.gradle.utils

import org.gradle.api.Project

class MavenParametersUtils {
    static String getProjectProperty(Project project, String key) {
        def mavenLicenseParametersProp = project.findProperty("maven-license-parameters")
        if (mavenLicenseParametersProp != null) {
            def matcher = mavenLicenseParametersProp.toString().replaceAll(/^['"]|['"]$/, '')
                    =~ /(?:^|\s)-D${key}=([^\s]+)/
            return matcher ? matcher[0][1] : null
        }
        return null
    }
}