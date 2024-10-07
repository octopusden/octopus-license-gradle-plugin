package org.octopusden.octopus.license.management.plugins.gradle;

import org.gradle.api.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.octopusden.octopus.license.management.plugins.gradle.utils.MavenParametersUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.octopusden.octopus.license.management.plugins.gradle.utils.MavenParametersUtils.getProjectProperty;
import static org.octopusden.octopus.license.management.plugins.gradle.utils.MavenParametersUtils.isFalse;

public class LicenseGradlePluginTest {
    private final Project mockProject = mock(Project.class);

    @BeforeEach
    void setUp() {
        when(mockProject.getRootProject()).thenReturn(mockProject);
    }

    /**
     * Tests the behavior of the isFalse() method for both boolean and string values of properties.
     *
     * This test covers cases where the properties are either `false` or `null` (both as booleans and strings).
     * The `getProjectProperty` method (from maven-license-parameters) returns either a String or null.
     * If the property is not found (null), the method checks the project property instead.
     *
     * The test ensures that `isFalse()` correctly returns `true` for properties with values `false`, `null`,
     * or when the project property holds a false-like value.
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
     * Tests that the isFalse() method returns false for non-false-like property values.
     *
     * This test covers property values such as "true", an empty string, a string with only whitespace,
     * and random values. These values are treated as non-false, so the method should return false.
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
     * Tests that the isFalse() method prioritizes the value from `maven-license-parameters`
     * over the project property value.
     *
     * This test ensures that if both `maven-license-parameters` and the project property are set,
     * the value from `maven-license-parameters` is used by the isFalse() method, and the project
     * property is ignored.
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
     * Tests that the isFalse() method uses the project property value only when the
     * corresponding property in `maven-license-parameters` is not set (returns null).
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
            mockedStatic.when(() -> MavenParametersUtils.isFalse(mockProject, property))
                    .thenCallRealMethod();
        }
    }
}