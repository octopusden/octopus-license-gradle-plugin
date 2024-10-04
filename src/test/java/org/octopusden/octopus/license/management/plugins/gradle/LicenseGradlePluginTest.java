package org.octopusden.octopus.license.management.plugins.gradle;

import org.gradle.api.Project;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.octopusden.octopus.license.management.plugins.gradle.utils.MavenParametersUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

public class LicenseGradlePluginTest {
    @Test
    void testIsFalseMustReturnTrue() {
        Project mockProject = mock(Project.class);

        try (MockedStatic<MavenParametersUtils> mockedStatic = mockStatic(MavenParametersUtils.class)) {
            when(mockProject.getRootProject()).thenReturn(mockProject);
            when(mockProject.findProperty("falseProp")).thenReturn(false);
            when(mockProject.findProperty("nullProp")).thenReturn(null);

            mockedStatic.when(() -> MavenParametersUtils.getProjectProperty(mockProject, "falseStringProp"))
                    .thenReturn("false");
            mockedStatic.when(() -> MavenParametersUtils.getProjectProperty(mockProject, "nullStringProp"))
                    .thenReturn("null");

            assertTrue(LicenseGradlePlugin.isFalse(mockProject, "falseProp"));
            assertTrue(LicenseGradlePlugin.isFalse(mockProject, "nullProp"));
            assertTrue(LicenseGradlePlugin.isFalse(mockProject, "falseStringProp"));
            assertTrue(LicenseGradlePlugin.isFalse(mockProject, "nullStringProp"));
        }
    }

    @Test
    void testIsFalseMustReturnFalse() {
        Project mockProject = mock(Project.class);

        try (MockedStatic<MavenParametersUtils> mockedStatic = mockStatic(MavenParametersUtils.class)) {
            when(mockProject.getRootProject()).thenReturn(mockProject);
            when(mockProject.findProperty("trueProp")).thenReturn(true);

            mockedStatic.when(() -> MavenParametersUtils.getProjectProperty(mockProject, "trueStringProp"))
                    .thenReturn("true");
            mockedStatic.when(() -> MavenParametersUtils.getProjectProperty(mockProject, "emptyStringProp"))
                    .thenReturn("");
            mockedStatic.when(() -> MavenParametersUtils.getProjectProperty(mockProject, "whitespaceStringProp"))
                    .thenReturn("    ");
            mockedStatic.when(() -> MavenParametersUtils.getProjectProperty(mockProject, "randomStringProp"))
                    .thenReturn("random");

            assertFalse(LicenseGradlePlugin.isFalse(mockProject, "trueProp"));
            assertFalse(LicenseGradlePlugin.isFalse(mockProject, "trueStringProp"));
            assertFalse(LicenseGradlePlugin.isFalse(mockProject, "emptyStringProp"));
            assertFalse(LicenseGradlePlugin.isFalse(mockProject, "whitespaceStringProp"));
            assertFalse(LicenseGradlePlugin.isFalse(mockProject, "randomStringProp"));
        }
    }

    @Test
    void testProjectPropDoesNotOverrideMvnProp() {
        // The project property is ignored if the 'maven-license-parameters' property exists (returns not null value)
        Project mockProject = mock(Project.class);

        try (MockedStatic<MavenParametersUtils> mockedStatic = mockStatic(MavenParametersUtils.class)) {
            mockedStatic.when(() -> MavenParametersUtils.getProjectProperty(mockProject, "falseStringProp"))
                    .thenReturn("false");
            mockedStatic.when(() -> MavenParametersUtils.getProjectProperty(mockProject, "trueStringProp"))
                    .thenReturn("true");
            mockedStatic.when(() -> MavenParametersUtils.getProjectProperty(mockProject, "nullStringProp"))
                    .thenReturn("null");
            mockedStatic.when(() -> MavenParametersUtils.getProjectProperty(mockProject, "emptyStringProp"))
                    .thenReturn("");
            mockedStatic.when(() -> MavenParametersUtils.getProjectProperty(mockProject, "randomStringProp"))
                    .thenReturn("random");

            when(mockProject.getRootProject()).thenReturn(mockProject);
            when(mockProject.findProperty("falseStringProp")).thenReturn(true);
            when(mockProject.findProperty("trueStringProp")).thenReturn(false);
            when(mockProject.findProperty("nullStringProp")).thenReturn(true);
            when(mockProject.findProperty("emptyStringProp")).thenReturn(false);
            when(mockProject.findProperty("randomStringProp")).thenReturn(false);

            assertTrue(LicenseGradlePlugin.isFalse(mockProject, "falseStringProp"));
            assertFalse(LicenseGradlePlugin.isFalse(mockProject, "trueStringProp"));
            assertTrue(LicenseGradlePlugin.isFalse(mockProject, "nullStringProp"));
            assertFalse(LicenseGradlePlugin.isFalse(mockProject, "emptyStringProp"));
            assertFalse(LicenseGradlePlugin.isFalse(mockProject, "randomStringProp"));
        }
    }

    @Test
    void testProjectPropOverridesOnlyIfMvnPropIsNull() {
        // The project property is used only if the 'maven-license-parameters' property is missing (returns null value).
        Project mockProject = mock(Project.class);

        try (MockedStatic<MavenParametersUtils> mockedStatic = mockStatic(MavenParametersUtils.class)) {
            mockedStatic.when(() -> MavenParametersUtils.getProjectProperty(mockProject, "nullProp"))
                    .thenReturn(null);

            when(mockProject.getRootProject()).thenReturn(mockProject);
            when(mockProject.findProperty("nullProp")).thenReturn(true);

            assertFalse(LicenseGradlePlugin.isFalse(mockProject, "nullProp"));
        }
    }
}
