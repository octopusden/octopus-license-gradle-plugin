package org.octopusden.octopus.license.management.plugins.gradle.tasks

import org.octopusden.octopus.components.registry.client.impl.ClassicComponentsRegistryServiceClient
import org.octopusden.octopus.components.registry.client.impl.ClassicComponentsRegistryServiceClientUrlProvider
import org.octopusden.octopus.license.management.plugins.gradle.dto.MavenGAV
import groovy.json.JsonBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.util.GradleVersion

/**
 * Create json file with projects dependencies.
 */
class LicensedDependenciesAnalyzingTask extends DefaultTask {

    public static final String STRICT_RESOLVER = "strictDependencyResolution"
    public static final String SUPPORTED_GROUPS = "supported-groups"
    public static final String CRS_URL ="component-registry-service-url"

    @Input
    String excludePattern = "test.*"

    @Input
    String includePattern = ".*"

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

    @TaskAction
    def processLicensedDependencies() {
        if (project.gradle.startParameter.offline) {
            logger.info("Skip extracting licenses for dependencies because of offline mode")
            return
        }
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
                            configuration.resolvedConfiguration.resolvedArtifacts.forEach { artifact -> resolvedArtifacts.add(artifactToMavenGav(project$, configuration, artifact)) }
                        } else {
                            def configurationName = "${configuration.name}_license_plugin"
                            Configuration toAnalyze = Optional.ofNullable(project$.getConfigurations().findByName(configurationName))
                                    .orElseGet {
                                        Configuration created = project$.getConfigurations().create(configurationName)
                                        created.extendsFrom(configuration)
                                        created.canBeResolved = true
                                        created.canBeConsumed = true
                                        created.transitive = true
                                        configuration.dependencies.each { dep ->
                                            if (dep instanceof DefaultExternalModuleDependency && !dep.transitive) {
                                                def copyDep = dep.copy()
                                                copyDep.transitive = true
                                                created.dependencies.add(copyDep)
                                            }
                                        }
                                        created
                                    }
                            toAnalyze.resolvedConfiguration.lenientConfiguration.getAllModuleDependencies().forEach { artifact ->
                                if (!isProjectArtifact(artifact)) {
                                    resolvedArtifacts.addAll(dependencyToMavenGav(project$, configuration, artifact))
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

                        def strictDR = ! ( project.hasProperty(STRICT_RESOLVER) && project.property(STRICT_RESOLVER) == 'false' )
                        resProblemsMessages.append "\nThe '${STRICT_RESOLVER}' mode is set to ${strictDR}\n"

                        if (strictDR) {
                            printFoundProblems(resProblemsMessages)
                            throw exception
                        } else {
                            try {
                                configuration.resolvedConfiguration.resolvedArtifacts.forEach { resolvedArtifacts.add(toMavenGav(project$, configuration, it)) }
                            } catch (Exception exception2) {
                                logger.error "Unable to resolve original configuration ${configuration.name}", exception2
                            }
                        }
                    }
                }
            }

            def supportedGroups = new HashSet<String>()
            def hasSupportedGroups = project.hasProperty(SUPPORTED_GROUPS)
            def hasCrsUrl = project.hasProperty(CRS_URL)

            if (hasSupportedGroups == hasCrsUrl) {
                throw new IllegalArgumentException(
                    hasSupportedGroups
                        ? "Can only 1 property exist: either $SUPPORTED_GROUPS or $CRS_URL, not both."
                        : "At least one property must exist: $SUPPORTED_GROUPS or $CRS_URL."
                )
            }

            if (hasSupportedGroups) {
                def supportedGroupsString = project.property(SUPPORTED_GROUPS) as String
                logger.info("Supported groups from property: ${supportedGroupsString}")
                supportedGroups.addAll(supportedGroupsString.split(",").collect { it.trim() })
            }

            if (hasCrsUrl) {
                def componentsRegistryApiUrl = project.findProperty(CRS_URL).toString()
                def componentsRegistryServiceClient = new ClassicComponentsRegistryServiceClient(
                    new ClassicComponentsRegistryServiceClientUrlProvider() {
                        @Override
                        String getApiUrl() {
                            return componentsRegistryApiUrl
                        }
                    }
                )
                try {
                    supportedGroups.addAll(componentsRegistryServiceClient.supportedGroupIds)
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to get a successful supported groups response!", e)
                }
            }

            if (supportedGroups.isEmpty()) {
                throw new IllegalArgumentException("Can't found any supported groups, please check your $SUPPORTED_GROUPS or $CRS_URL values!")
            }

            def builder = new JsonBuilder()
            supportedGroups.each { group ->
                builder(resolvedArtifacts.findAll { !it.getGroup().startsWith(group.toString()) && !it.getGroup().startsWith(group.toString()) }.collect { it })
            }
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

    Boolean isProjectArtifact(ResolvedDependency artifact) {
        return artifact.moduleArtifacts.size() == 1
                && artifact.moduleArtifacts.first().id.componentIdentifier instanceof ProjectComponentIdentifier
    }

    MavenGAV artifactToMavenGav(Project project, Configuration configuration, ResolvedArtifact resolvedArtifact) {
        return new MavenGAV(project: project.name,
                configuration: configuration.name,
                group: resolvedArtifact.moduleVersion.id.group,
                artifact: resolvedArtifact.moduleVersion.id.name,
                version: resolvedArtifact.moduleVersion.id.version,
                classifier: resolvedArtifact.classifier,
                extension: resolvedArtifact.extension)
    }

    Collection<MavenGAV> dependencyToMavenGav(Project project, Configuration configuration, ResolvedDependency resolvedDependency) {
        return resolvedDependency.moduleArtifacts.collect {
            artifactToMavenGav(project, configuration, it)
        }
    }

    def addChoosingVariantsInstruction(StringBuilder sb) {
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
