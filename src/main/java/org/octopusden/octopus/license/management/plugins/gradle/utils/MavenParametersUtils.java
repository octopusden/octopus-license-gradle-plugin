package org.octopusden.octopus.license.management.plugins.gradle.utils;

import org.gradle.api.Project;

public final class MavenParametersUtils {
    public static final String MAVEN_LICENSE_PARAMETERS = "maven-license-parameters";

    private MavenParametersUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

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

    /**
     * Returns {@code true} for properties with the value
     * with values other than {@code false}, {@code null}, {@code "false"}, {@code "null"}.
     * Otherwise, returns {@code false}. Prioritizes the property in
     * {@code maven-license-parameters} unless its value is {@code null}.
     */
    public static Boolean isFalse(Project project, String property) {
        String propertyValue = getProjectProperty(project, property);

        if (propertyValue == null) {
            propertyValue = String.valueOf(project.getRootProject().findProperty(property));
        }

        return propertyValue == null || propertyValue.equalsIgnoreCase("false") || propertyValue.equalsIgnoreCase("null");
    }
}