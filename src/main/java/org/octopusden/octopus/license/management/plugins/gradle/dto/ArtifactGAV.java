package org.octopusden.octopus.license.management.plugins.gradle.dto;

import java.util.List;
import java.util.Objects;

public class ArtifactGAV {
    private final String group;
    private final String artifact;
    private final String version;
    private final String classifier;
    private final String extension;
    private final List<ExcludeRule> excludeRules;

    public ArtifactGAV(String group, String artifact, String version, String classifier, String extension, List<ExcludeRule> excludeRules) {
        this.group = group;
        this.artifact = artifact;
        this.version = version;
        this.classifier = classifier;
        this.extension = extension;
        this.excludeRules = excludeRules;
    }

    public String getGroup() {
        return group;
    }

    public String getArtifact() {
        return artifact;
    }

    public String getVersion() {
        return version;
    }

    public String getClassifier() {
        return classifier;
    }

    public String getExtension() {
        return extension;
    }

    public List<ExcludeRule> getExcludeRules() {
        return excludeRules;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArtifactGAV)) return false;
        ArtifactGAV that = (ArtifactGAV) o;
        return Objects.equals(group, that.group) && Objects.equals(artifact, that.artifact) && Objects.equals(version, that.version) && Objects.equals(classifier, that.classifier) && Objects.equals(extension, that.extension) && excludeRules != null ? excludeRules.equals(that.excludeRules) : that.excludeRules == null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, artifact, version, classifier, extension, excludeRules != null ? excludeRules.hashCode() : 0);
    }
}
