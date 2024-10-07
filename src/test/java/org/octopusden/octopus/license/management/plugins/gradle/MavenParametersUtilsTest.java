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
import static org.octopusden.octopus.license.management.plugins.gradle.utils.MavenParametersUtils.getProjectProperty;
import static org.octopusden.octopus.license.management.plugins.gradle.utils.MavenParametersUtils.isFalse;
import static org.octopusden.octopus.license.management.plugins.gradle.utils.MavenParametersUtils.MAVEN_LICENSE_PARAMETERS;

public class MavenParametersUtilsTest {
    private final Project mockProject = mock(Project.class);

    @BeforeEach
    void setUp() {
        when(mockProject.getRootProject()).thenReturn(mockProject);
    }

    @Test
    void testGetProjectProperty() {
        when(mockProject.findProperty(MAVEN_LICENSE_PARAMETERS))
                .thenReturn("-Dparameter1=value1 -Dparameter2=value2 -Dparameter3=value3");

        assertEquals("value1", getProjectProperty(mockProject, "parameter1"));
        assertEquals("value2", getProjectProperty(mockProject, "parameter2"));
        assertEquals("value3", getProjectProperty(mockProject, "parameter3"));
    }

    @Test
    void testGetProjectPropertyWithSingleQuotedParameters() {
        when(mockProject.findProperty(MAVEN_LICENSE_PARAMETERS))
                .thenReturn("'-Dparameter1=value1 -Dparameter2=value2 -Dparameter3=value3'");

        assertEquals("value1", getProjectProperty(mockProject, "parameter1"));
        assertEquals("value2", getProjectProperty(mockProject, "parameter2"));
        assertEquals("value3", getProjectProperty(mockProject, "parameter3"));
    }

    @Test
    void testGetProjectPropertyWithDoubleQuotedParameters() {
        when(mockProject.findProperty(MAVEN_LICENSE_PARAMETERS))
                .thenReturn("\"-Dparameter1=value1 -Dparameter2=value2 -Dparameter3=value3\"");

        assertEquals("value1", getProjectProperty(mockProject, "parameter1"));
        assertEquals("value2", getProjectProperty(mockProject, "parameter2"));
        assertEquals("value3", getProjectProperty(mockProject, "parameter3"));
    }

    @Test
    void testGetProjectPropertyReturnsNullWhenKeyNotFound() {
        when(mockProject.findProperty(MAVEN_LICENSE_PARAMETERS))
                .thenReturn("-Dparameter1=value1 -Dparameter2=value2");

        assertNull(getProjectProperty(mockProject, "nonExistentParameter"));
    }

    @Test
    void testGetProjectPropertyReturnsNullWhenParametersNotSet() {
        assertNull(getProjectProperty(mockProject, "parameter"));
    }

    /**
     * Tests that {@code isFalse()} returns {@code true} for properties
     * with values {@code false}, {@code null}, {@code "false"}, {@code "null"} in
     * {@code maven-license-parameters} or project properties.
     */
    @Test
    void testIsFalseForFalseOrNullValues() {
        try (MockedStatic<MavenParametersUtils> mockedStatic = mockStatic(MavenParametersUtils.class)) {
            when(mockProject.findProperty("falseProp")).thenReturn(false);
            when(mockProject.findProperty("nullProp")).thenReturn(null);

            mockedStatic.when(() -> getProjectProperty(mockProject, "falseStringProp"))
                    .thenReturn("false");
            mockedStatic.when(() -> getProjectProperty(mockProject, "nullStringProp"))
                    .thenReturn("null");

            String[] properties = {"falseProp", "newProp", "falseStringProp", "nullStringProp"};
            mockIsFalseToCallRealMethod(mockedStatic, properties);
            for (String property: properties) assertTrue(isFalse(mockProject, property));
        }
    }

    /**
     * Tests that {@code isFalse()} returns {@code false} for properties
     * with values other than {@code false}, {@code null}, {@code "false"}, {@code "null"} in
     * {@code maven-license-parameters} or project properties.
     */
    @Test
    void testIsFalseForNonFalseValues() {
        try (MockedStatic<MavenParametersUtils> mockedStatic = mockStatic(MavenParametersUtils.class)) {
            when(mockProject.findProperty("trueProp")).thenReturn(true);

            mockedStatic.when(() -> getProjectProperty(mockProject, "trueStringProp"))
                    .thenReturn("true");
            mockedStatic.when(() -> getProjectProperty(mockProject, "emptyStringProp"))
                    .thenReturn("");
            mockedStatic.when(() -> getProjectProperty(mockProject, "whitespaceStringProp"))
                    .thenReturn("    ");
            mockedStatic.when(() -> getProjectProperty(mockProject, "randomStringProp"))
                    .thenReturn("random");

            String[] properties = {"trueStringProp", "emptyStringProp", "whitespaceStringProp", "randomStringProp"};
            mockIsFalseToCallRealMethod(mockedStatic, properties);
            for (String property: properties) assertFalse(isFalse(mockProject, property));
        }
    }

    /**
     * Tests that {@code isFalse()} method prioritizes the value
     * from {@code maven-license-parameters} over the project property value.
     */
    @Test
    void testProjectPropDoesNotOverrideMvnProp() {
        try (MockedStatic<MavenParametersUtils> mockedStatic = mockStatic(MavenParametersUtils.class)) {
            mockedStatic.when(() -> getProjectProperty(mockProject, "falseStringProp"))
                    .thenReturn("false");
            mockedStatic.when(() -> getProjectProperty(mockProject, "trueStringProp"))
                    .thenReturn("true");
            mockedStatic.when(() -> getProjectProperty(mockProject, "nullStringProp"))
                    .thenReturn("null");
            mockedStatic.when(() -> getProjectProperty(mockProject, "emptyStringProp"))
                    .thenReturn("");
            mockedStatic.when(() -> getProjectProperty(mockProject, "randomStringProp"))
                    .thenReturn("random");

            when(mockProject.findProperty("falseStringProp")).thenReturn(true);
            when(mockProject.findProperty("trueStringProp")).thenReturn(false);
            when(mockProject.findProperty("nullStringProp")).thenReturn(true);
            when(mockProject.findProperty("emptyStringProp")).thenReturn(false);
            when(mockProject.findProperty("randomStringProp")).thenReturn(false);

            String[] properties = {"falseStringProp", "trueStringProp", "nullStringProp", "emptyStringProp", "randomStringProp"};
            mockIsFalseToCallRealMethod(mockedStatic, properties);
            assertTrue(isFalse(mockProject, "falseStringProp"));
            assertFalse(isFalse(mockProject, "trueStringProp"));
            assertTrue(isFalse(mockProject, "nullStringProp"));
            assertFalse(isFalse(mockProject, "emptyStringProp"));
            assertFalse(isFalse(mockProject, "randomStringProp"));
        }
    }

    /**
     * Tests that {@code isFalse()} method uses the project property value only
     * when the corresponding property in {@code maven-license-parameters} is not set
     * (returns {@code null}).
     */
    @Test
    void testProjectPropOverridesOnlyIfMvnPropIsNull() {
        try (MockedStatic<MavenParametersUtils> mockedStatic = mockStatic(MavenParametersUtils.class)) {
            mockedStatic.when(() -> getProjectProperty(mockProject, "nullProp"))
                    .thenReturn(null);

            when(mockProject.findProperty("nullProp")).thenReturn(true);

            String[] properties = {"nullProp"};
            mockIsFalseToCallRealMethod(mockedStatic, properties);
            assertFalse(isFalse(mockProject, "nullProp"));
        }
    }

    private void mockIsFalseToCallRealMethod(MockedStatic<MavenParametersUtils> mockedStatic, String[] properties) {
        for (String property: properties) {
            mockedStatic.when(() -> isFalse(mockProject, property))
                    .thenCallRealMethod();
        }
    }
}
