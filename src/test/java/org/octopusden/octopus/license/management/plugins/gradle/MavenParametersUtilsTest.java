package org.octopusden.octopus.license.management.plugins.gradle;

import org.gradle.api.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.octopusden.octopus.license.management.plugins.gradle.utils.MavenParametersUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.octopusden.octopus.license.management.plugins.gradle.utils.MavenParametersUtils.getLicenseParametersProperty;
import static org.octopusden.octopus.license.management.plugins.gradle.utils.MavenParametersUtils.propertyIsFalse;
import static org.octopusden.octopus.license.management.plugins.gradle.utils.MavenParametersUtils.MAVEN_LICENSE_PARAMETERS;

public class MavenParametersUtilsTest {
    private final Project mockProject = mock(Project.class);

    @BeforeEach
    void setUp() {
        when(mockProject.getRootProject()).thenReturn(mockProject);
    }

    @Test
    void testGetLicenseParametersProperty() {
        when(mockProject.findProperty(MAVEN_LICENSE_PARAMETERS))
                .thenReturn("-Dparameter1=value1 -Dparameter2=value2 -Dparameter3=value3");

        assertEquals("value1", getLicenseParametersProperty(mockProject, "parameter1"));
        assertEquals("value2", getLicenseParametersProperty(mockProject, "parameter2"));
        assertEquals("value3", getLicenseParametersProperty(mockProject, "parameter3"));
    }

    @Test
    void testGetLicenseParametersPropertyWithSingleQuotedParameters() {
        when(mockProject.findProperty(MAVEN_LICENSE_PARAMETERS))
                .thenReturn("'-Dparameter1=value1 -Dparameter2=value2 -Dparameter3=value3'");

        assertEquals("value1", getLicenseParametersProperty(mockProject, "parameter1"));
        assertEquals("value2", getLicenseParametersProperty(mockProject, "parameter2"));
        assertEquals("value3", getLicenseParametersProperty(mockProject, "parameter3"));
    }

    @Test
    void testGetLicenseParametersPropertyWithDoubleQuotedParameters() {
        when(mockProject.findProperty(MAVEN_LICENSE_PARAMETERS))
                .thenReturn("\"-Dparameter1=value1 -Dparameter2=value2 -Dparameter3=value3\"");

        assertEquals("value1", getLicenseParametersProperty(mockProject, "parameter1"));
        assertEquals("value2", getLicenseParametersProperty(mockProject, "parameter2"));
        assertEquals("value3", getLicenseParametersProperty(mockProject, "parameter3"));
    }

    @Test
    void testGetLicenseParametersPropertyReturnsNullWhenKeyNotFound() {
        when(mockProject.findProperty(MAVEN_LICENSE_PARAMETERS))
                .thenReturn("-Dparameter1=value1 -Dparameter2=value2");

        assertNull(getLicenseParametersProperty(mockProject, "nonExistentParameter"));
    }

    @Test
    void testGetLicenseParametersPropertyReturnsNullWhenParametersNotSet() {
        assertNull(getLicenseParametersProperty(mockProject, "parameter"));
    }

    /**
     * Tests that {@code propertyIsFalse()} returns {@code true} for properties
     * with values {@code false}, {@code "false"} in
     * {@code maven-license-parameters} or project properties.
     */
    @Test
    void testPropertyIsFalseForFalseValue() {
        try (MockedStatic<MavenParametersUtils> mockedStatic = mockStatic(MavenParametersUtils.class)) {
            when(mockProject.findProperty("falseProp")).thenReturn(false);

            mockedStatic.when(() -> getLicenseParametersProperty(mockProject, "falseStringProp"))
                    .thenReturn("false");

            String[] properties = {"falseProp", "falseStringProp"};
            mockPropertyIsFalseToCallRealMethod(mockedStatic, properties);
            for (String property: properties) assertTrue(propertyIsFalse(mockProject, property));
        }
    }

    /**
     * Tests that {@code propertyIsFalse()} returns {@code false} for properties
     * with values other than {@code false}, {@code "false"} in
     * {@code maven-license-parameters} or project properties.
     */
    @Test
    void testPropertyIsFalseForNonFalseValues() {
        try (MockedStatic<MavenParametersUtils> mockedStatic = mockStatic(MavenParametersUtils.class)) {
            when(mockProject.findProperty("trueProp")).thenReturn(true);
            when(mockProject.findProperty("nullProp")).thenReturn(null);

            mockedStatic.when(() -> getLicenseParametersProperty(mockProject, "trueStringProp"))
                    .thenReturn("true");
            mockedStatic.when(() -> getLicenseParametersProperty(mockProject, "nullStringProp"))
                    .thenReturn("null");
            mockedStatic.when(() -> getLicenseParametersProperty(mockProject, "emptyStringProp"))
                    .thenReturn("");
            mockedStatic.when(() -> getLicenseParametersProperty(mockProject, "whitespaceStringProp"))
                    .thenReturn("    ");
            mockedStatic.when(() -> getLicenseParametersProperty(mockProject, "randomStringProp"))
                    .thenReturn("random");

            String[] properties = {"trueProp", "nullProp", "trueStringProp", "nullStringProp", "emptyStringProp", "whitespaceStringProp", "randomStringProp"};
            mockPropertyIsFalseToCallRealMethod(mockedStatic, properties);
            for (String property: properties) assertFalse(propertyIsFalse(mockProject, property));
        }
    }

    /**
     * Tests that {@code propertyIsFalse()} method prioritizes the value
     * from {@code maven-license-parameters} over the project property value.
     */
    @Test
    void testProjectPropDoesNotOverrideMvnProp() {
        try (MockedStatic<MavenParametersUtils> mockedStatic = mockStatic(MavenParametersUtils.class)) {
            mockedStatic.when(() -> getLicenseParametersProperty(mockProject, "falseStringProp"))
                    .thenReturn("false");
            mockedStatic.when(() -> getLicenseParametersProperty(mockProject, "trueStringProp"))
                    .thenReturn("true");
            mockedStatic.when(() -> getLicenseParametersProperty(mockProject, "nullStringProp"))
                    .thenReturn("null");
            mockedStatic.when(() -> getLicenseParametersProperty(mockProject, "emptyStringProp"))
                    .thenReturn("");
            mockedStatic.when(() -> getLicenseParametersProperty(mockProject, "randomStringProp"))
                    .thenReturn("random");

            when(mockProject.findProperty("falseStringProp")).thenReturn(true);
            when(mockProject.findProperty("trueStringProp")).thenReturn(false);
            when(mockProject.findProperty("nullStringProp")).thenReturn(false);
            when(mockProject.findProperty("emptyStringProp")).thenReturn(false);
            when(mockProject.findProperty("randomStringProp")).thenReturn(false);

            String[] properties = {"falseStringProp", "trueStringProp", "nullStringProp", "emptyStringProp", "randomStringProp"};
            mockPropertyIsFalseToCallRealMethod(mockedStatic, properties);
            assertTrue(propertyIsFalse(mockProject, "falseStringProp"));
            assertFalse(propertyIsFalse(mockProject, "trueStringProp"));
            assertFalse(propertyIsFalse(mockProject, "nullStringProp"));
            assertFalse(propertyIsFalse(mockProject, "emptyStringProp"));
            assertFalse(propertyIsFalse(mockProject, "randomStringProp"));
        }
    }

    /**
     * Tests that {@code propertyIsFalse()} method uses the project property value only
     * when the corresponding property in {@code maven-license-parameters} is not set
     * (returns {@code null}).
     */
    @Test
    void testProjectPropOverridesOnlyIfMvnPropIsNull() {
        try (MockedStatic<MavenParametersUtils> mockedStatic = mockStatic(MavenParametersUtils.class)) {
            mockedStatic.when(() -> getLicenseParametersProperty(mockProject, "nullProp"))
                    .thenReturn(null);

            when(mockProject.findProperty("nullProp")).thenReturn(true);

            String[] properties = {"nullProp"};
            mockPropertyIsFalseToCallRealMethod(mockedStatic, properties);
            assertFalse(propertyIsFalse(mockProject, "nullProp"));
        }
    }

    private void mockPropertyIsFalseToCallRealMethod(MockedStatic<MavenParametersUtils> mockedStatic, String[] properties) {
        for (String property: properties) {
            mockedStatic.when(() -> propertyIsFalse(mockProject, property))
                    .thenCallRealMethod();
        }
    }
}
