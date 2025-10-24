package org.octopusden.octopus.license.management.plugins.gradle.dto;

import java.util.Objects;

public class ExcludeRule {
    private final String group;
    private final String artifact;

    public ExcludeRule(final String group, final String artifact) {
        this.group = group;
        this.artifact = artifact;
    }

    public String getGroup() {
        return group;
    }

    public String getArtifact() {
        return artifact;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExcludeRule)) return false;
        ExcludeRule that = (ExcludeRule) o;
        return Objects.equals(group, that.group) && Objects.equals(artifact, that.artifact);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, artifact);
    }

}
