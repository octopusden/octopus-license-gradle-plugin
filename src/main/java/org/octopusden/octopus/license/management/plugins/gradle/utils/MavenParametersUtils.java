package org.octopusden.octopus.license.management.plugins.gradle.utils;

import org.gradle.api.Project;

public final class MavenParametersUtils {
    public static final String MAVEN_LICENSE_PARAMETERS = "maven-license-parameters";

    private MavenParametersUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static String getLicenseParametersProperty(Project project, String key) {
        Object mavenLicenseParametersProp = project.findProperty(MAVEN_LICENSE_PARAMETERS);
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
     * Checks if {@code property} is considered false on the {@code maven-license-parameters},
     * if {@code null}, will check the project properties
     *
     * @param project the project containing the property
     * @param property the name of the property to check
     * @return {@code true} if the property value is {@code false}, {@code null}, {@code "false"}, or {@code "null"};
     *         {@code false} otherwise
     */
    public static Boolean propertyIsFalse(Project project, String property) {
        String propertyValue = getLicenseParametersProperty(project, property);

        if (propertyValue == null) {
            propertyValue = String.valueOf(project.getRootProject().findProperty(property));
        }

        return propertyValue == null || propertyValue.equalsIgnoreCase("false") || propertyValue.equalsIgnoreCase("null");
    }
}