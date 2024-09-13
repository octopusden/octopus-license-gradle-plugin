package org.octopusden.octopus.license.management.plugins.gradle;

import org.gradle.api.Project;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.octopusden.octopus.license.management.plugins.gradle.utils.MavenParametersUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

public class MavenParametersUtilsTest {
    @Test
    void testGetProjectProperty() {
        Project mockProject = Mockito.mock(Project.class);

        when(mockProject.findProperty(MavenParametersUtils.MAVEN_LICENSE_PARAMETERS))
                .thenReturn("-Dparameter1=value1 -Dparameter2=value2 -Dparameter3=value3");

        assertEquals("value1", MavenParametersUtils.getProjectProperty(mockProject, "parameter1"));
        assertEquals("value2", MavenParametersUtils.getProjectProperty(mockProject, "parameter2"));
        assertEquals("value3", MavenParametersUtils.getProjectProperty(mockProject, "parameter3"));
    }

    @Test
    void testGetProjectPropertyWithSingleQuotedParameters() {
        Project mockProject = Mockito.mock(Project.class);

        when(mockProject.findProperty(MavenParametersUtils.MAVEN_LICENSE_PARAMETERS))
                .thenReturn("'-Dparameter1=value1 -Dparameter2=value2 -Dparameter3=value3'");

        assertEquals("value1", MavenParametersUtils.getProjectProperty(mockProject, "parameter1"));
        assertEquals("value2", MavenParametersUtils.getProjectProperty(mockProject, "parameter2"));
        assertEquals("value3", MavenParametersUtils.getProjectProperty(mockProject, "parameter3"));
    }

    @Test
    void testGetProjectPropertyWithDoubleQuotedParameters() {
        Project mockProject = Mockito.mock(Project.class);

        when(mockProject.findProperty(MavenParametersUtils.MAVEN_LICENSE_PARAMETERS))
                .thenReturn("\"-Dparameter1=value1 -Dparameter2=value2 -Dparameter3=value3\"");

        assertEquals("value1", MavenParametersUtils.getProjectProperty(mockProject, "parameter1"));
        assertEquals("value2", MavenParametersUtils.getProjectProperty(mockProject, "parameter2"));
        assertEquals("value3", MavenParametersUtils.getProjectProperty(mockProject, "parameter3"));
    }

    @Test
    void testGetProjectPropertyReturnsNullWhenKeyNotFound() {
        Project mockProject = Mockito.mock(Project.class);

        when(mockProject.findProperty(MavenParametersUtils.MAVEN_LICENSE_PARAMETERS))
                .thenReturn("-Dparameter1=value1 -Dparameter2=value2");

        assertNull(MavenParametersUtils.getProjectProperty(mockProject, "nonExistentParameter"));
    }

    @Test
    void testGetProjectPropertyReturnsNullWhenParametersNotSet() {
        Project mockProject = Mockito.mock(Project.class);

        assertNull(MavenParametersUtils.getProjectProperty(mockProject, "parameter"));
    }
}
