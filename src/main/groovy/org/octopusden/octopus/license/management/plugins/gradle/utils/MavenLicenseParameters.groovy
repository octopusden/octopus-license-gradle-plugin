package org.octopusden.octopus.license.management.plugins.gradle.utils

import org.gradle.api.Project

class MavenLicenseParameters {
    static String getProjectProperty(Project project, String key) {
        def targetParameter = getAllProjectProperties(project)?.find {
            it.startsWith("-D$key=")
        }
        if (targetParameter) {
            return targetParameter.split("=")[1]
        }
        return null
    }

    static String[] getAllProjectProperties(Project project) {
        def mavenLicenseParametersProp = project.findProperty("maven-license-parameters")
        if (mavenLicenseParametersProp != null) {
            return mavenLicenseParametersProp.toString()
                    .replaceAll(/^['"]|['"]$/, '')
                    .toString().split(" ")
        }
        return null
    }
}