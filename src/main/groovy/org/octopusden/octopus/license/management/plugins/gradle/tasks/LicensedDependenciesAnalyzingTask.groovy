package org.octopusden.octopus.license.management.plugins.gradle.tasks

import groovy.json.JsonBuilder
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.util.GradleVersion
import org.octopusden.octopus.license.management.plugins.gradle.dto.MavenExcludeRule
import org.octopusden.octopus.license.management.plugins.gradle.dto.MavenGAV

/**
 * Create json file with projects dependencies.
 */
class LicensedDependenciesAnalyzingTask extends DefaultTask {

    public static final String STRICT_RESOLVER = "strictDependencyResolution"

    private static final String DEFAULT_EXCLUDE_PATTERN = "test.*"
    private static final String DEFAULT_INCLUDE_PATTERN = ".*"

    @Input
    String destinationDir = "build"

    @Input
    String destinationFile = "dependencies.json"

    @Input
    boolean onlyCurrentProject = false

    @OutputFile
    File getDependenciesListFile() {
        project.file("$destinationDir/$destinationFile")
    }

    @OutputFile
    File getResolvingProblemsFile() {
        project.file("$destinationDir/resolving_problems.txt")
    }


    LicensedDependenciesAnalyzingTask() {
        outputs.file(dependenciesListFile)
    }

    @SuppressWarnings('ConfigurationAvoidance')
    @TaskAction
    def processLicensedDependencies() {
        if (project.gradle.startParameter.offline) {
            logger.info("Skip extracting licenses for dependencies because of offline mode")
            return
        }

        final LicenseManagementExtension licenseManagementExtension = getProject()
                .getProject()
                .getExtensions()
                .getByType(LicenseManagementExtension.class)

        def includePattern = licenseManagementExtension.includePattern ?: DEFAULT_INCLUDE_PATTERN
        def excludePattern = licenseManagementExtension.excludePattern ?: DEFAULT_EXCLUDE_PATTERN

        logger.info("includePattern: '{}', excludePattern: '{}'", includePattern, excludePattern)
        def resolvedArtifacts = new HashSet<MavenGAV>()
        def resProblemsMessages = new StringBuilder()

        def legacyVersion = GradleVersion.current() < GradleVersion.version('6.0')

        def projectsToAnalyze = new HashSet<Project>()
        if (onlyCurrentProject) {
            projectsToAnalyze.add(project)
        } else {
            projectsToAnalyze.addAll(project.rootProject.allprojects)
        }
        projectsToAnalyze.forEach { project$ ->
            def configurationsToAnalyze = new ArrayList(project$.configurations)
            configurationsToAnalyze.forEach { Configuration configuration ->
                if (excludePattern != null && configuration.name.matches(excludePattern)) {
                    logger.info("Skip '{}' configuration because of exclude pattern '{}'", configuration.name, excludePattern)
                } else if (includePattern != null && !configuration.name.matches(includePattern)) {
                    logger.info("Skip '{}' configuration because of include pattern '{}'", configuration.name, includePattern)
                } else {
                    try {
                        if (legacyVersion) {
                            configuration.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
                                resolvedArtifacts.add(artifactToMavenGav(project$, configuration, artifact))
                            }
                        } else {
                            def configurationName = "${configuration.name}_license_plugin"
                            Configuration toAnalyze = Optional.ofNullable(project$.getConfigurations().findByName(configurationName))
                                    .orElseGet {
                                        Configuration created = project$.getConfigurations().create(configurationName)
                                        created.extendsFrom(configuration)
                                        created.canBeResolved = true
                                        created.canBeConsumed = true
                                        created.transitive = true
                                        created
                                    }
                            toAnalyze.resolvedConfiguration.lenientConfiguration.getFirstLevelModuleDependencies().forEach { dependency ->
                                if (!isProjectDependency(dependency)) {
                                    resolvedArtifacts.addAll(dependencyToMavenGav(project$, configuration, dependency))
                                }
                            }
                            toAnalyze.resolvedConfiguration.lenientConfiguration.getUnresolvedModuleDependencies().forEach { unresolvedDependency ->
                                // Some dependencies couldn't be resolved
                                logger.debug("Unresolved dependency {}", unresolvedDependency)
                            }
                        }
                    } catch (Exception exception) {
                        def innerEx = exception
                        resProblemsMessages.append "Unable to resolve configuration\n"
                        resProblemsMessages.append "The resulting list of licenses may be incomplete or empty\n"
                        do {
                            resProblemsMessages.append "${innerEx.localizedMessage}\n"
                            innerEx = innerEx.cause
                        } while (innerEx)

                        def strictDR = !(project.hasProperty(STRICT_RESOLVER) && project.property(STRICT_RESOLVER) == 'false')
                        resProblemsMessages.append "\nThe '${STRICT_RESOLVER}' mode is set to ${strictDR}\n"

                        if (strictDR) {
                            printFoundProblems(resProblemsMessages)
                            throw exception
                        } else {
                            try {
                                configuration.resolvedConfiguration.resolvedArtifacts.forEach {
                                    resolvedArtifacts.add(artifactToMavenGav(project$, configuration, it))
                                }
                            } catch (Exception exception2) {
                                logger.error "Unable to resolve original configuration ${configuration.name}", exception2
                            }
                        }
                    }
                }
            }

            logger.info("Resolved dependencies:\n${resolvedArtifacts.collect { it.logString() }.join(",\n")}")
            def builder = new JsonBuilder()
            builder(resolvedArtifacts.toList())
            dependenciesListFile.write(builder.toPrettyString())
        }
        printFoundProblems(resProblemsMessages)
    }

    private void printFoundProblems(StringBuilder resProblemsMessages) {
        if (resProblemsMessages.toString().trim()) {
            addChoosingVariantsInstruction(resProblemsMessages)
            logger.error(resProblemsMessages.toString())
            // print to file as well
            getResolvingProblemsFile().withWriterAppend { writer ->
                writer.writeLine(resProblemsMessages.toString() + "\n")
            }
        }
    }

    static Boolean isProjectDependency(ResolvedDependency dependency) {
        return dependency.moduleArtifacts.size() == 1
                && dependency.moduleArtifacts.first().id.componentIdentifier instanceof ProjectComponentIdentifier
    }

    static MavenGAV artifactToMavenGav(Project project, Configuration configuration, ResolvedArtifact resolvedArtifact) {
        return new MavenGAV(project: project.name,
                configuration: configuration.name,
                group: resolvedArtifact.moduleVersion.id.group,
                artifact: resolvedArtifact.moduleVersion.id.name,
                version: resolvedArtifact.moduleVersion.id.version,
                classifier: resolvedArtifact.classifier,
                extension: resolvedArtifact.extension,
                excludeRules: getExcludedRules(configuration, resolvedArtifact)
        )
    }

    static Collection<MavenGAV> dependencyToMavenGav(Project project, Configuration configuration, ResolvedDependency resolvedDependency) {
        return resolvedDependency.moduleArtifacts.collect {
            artifactToMavenGav(project, configuration, it)
        }
    }

    static List<MavenExcludeRule> getExcludedRules(Configuration configuration, ResolvedArtifact artifact) {
        if (!configuration.transitive) {
            return [new MavenExcludeRule(group: "*", artifact: "*")]
        }
        def dep = configuration.allDependencies.withType(ModuleDependency).find {
            it.group == artifact.moduleVersion.id.group && it.name == artifact.moduleVersion.id.name
        }
        if (dep == null) return null
        return !dep.transitive ? [new MavenExcludeRule(group: "*", artifact: "*")] :
                dep.excludeRules?.collect { rule ->
                    new MavenExcludeRule(group: rule.group ?: "*", artifact: rule.module ?: "*")
                }?.toList()
    }

    static def addChoosingVariantsInstruction(StringBuilder sb) {
        if (sb.contains("Cannot choose between the following variants")) {
            def varInstr = """

To resolve dependency conflicts you may add configuration attributes, for example 
-- build.gradle -----------------------------------
configurations {
    externalDependencies {
        ...
        attributes {
            attribute(Attribute.of("org.gradle.dependency.bundling", String), 'external')
        }
    }
  ...
}  
---------------------------------------------------

In the example above, by adding the attribute "org.gradle.dependency.bundling" with the value "external" to the externalDependencies configuration, 
you resolve dependency conflicts that may occur when there are multiple equal variants of artifacts. 

This ensures that Gradle selects the appropriate variant and avoids dependency resolution failures.

"""

            sb.append("\n")
            sb.append(varInstr)
            sb.append("\n")

        }
    }

}
