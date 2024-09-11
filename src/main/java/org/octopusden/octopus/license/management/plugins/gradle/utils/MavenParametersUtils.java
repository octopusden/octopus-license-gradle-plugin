package org.octopusden.octopus.license.management.plugins.gradle.utils;

import org.gradle.api.Project;

public class MavenParametersUtils {
    public static String getProjectProperty(Project project, String key) {
        Object mavenLicenseParametersProp = project.findProperty("maven-license-parameters");
        if (mavenLicenseParametersProp != null) {
            String propString = mavenLicenseParametersProp.toString().replaceAll("^['\"]|['\"]$", "");
            String[] parameters = propString.split("\\s+");

            for (String param : parameters) {
                if (param.startsWith("-D" + key + "=")) {
                    return param.substring(param.indexOf('=') + 1);
                }
            }
        }
        return null;
    }
}