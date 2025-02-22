package org.octopusden.octopus.license.management.plugins.gradle.tasks

import org.octopusden.octopus.license.management.plugins.gradle.LicenseGradlePlugin
import org.octopusden.octopus.license.management.plugins.gradle.dto.ArtifactGAV
import org.octopusden.octopus.license.management.plugins.gradle.dto.MavenGAV
import org.octopusden.octopus.license.management.plugins.gradle.utils.MavenParametersUtils

import com.platformlib.process.local.factory.LocalProcessBuilderFactory
import groovy.json.JsonSlurper
import groovy.xml.MarkupBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.CopyOnWriteArrayList

class LicenseTask extends DefaultTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(LicenseTask.class)

    @Input
    String sourceDependencies = "build/dependencies.json"

    @OutputDirectory
    File getLicensesDirectory() {
        return project.file("build/licenses")
    }

    LicenseTask() {
        outputs.dir(licensesDirectory)
    }

    @TaskAction
    void processLicenses() {
        if (project.gradle.startParameter.offline) {
            LOGGER.info("Skip generating licenses because of offline mode")
            return
        }

        def licensesPom = project.file("build/licenses-pom.xml")
        Set<MavenGAV> loadedResolvedArtifacts = new JsonSlurper().parse(project.file(sourceDependencies))
        def resolvedArtifacts = loadedResolvedArtifacts.collect {
            new ArtifactGAV(
                    it.group,
                    it.artifact,
                    it.version,
                    it.classifier,
                    it.extension
            )
        }.toSet()

        def octopusLicenseMavenPluginVersion = MavenParametersUtils.getLicenseParametersProperty(project, "octopus-license-maven-plugin.version")
                ?: project.findProperty("octopus-license-maven-plugin.version")

        if (!octopusLicenseMavenPluginVersion) {
            throw new IllegalArgumentException("Property 'octopus-license-maven-plugin.version' must be specified")
        }

        licensesPom.withWriter { writer ->
            def markupBuilder = new MarkupBuilder(writer)
            markupBuilder.project {
                modelVersion("4.0.0")
                groupId(project.group)
                artifactId(project.name)
                version(project.version)
                markupBuilder.build {
                    plugins {
                        plugin {
                            groupId("org.octopusden.octopus")
                            artifactId("license-maven-plugin")
                            version(octopusLicenseMavenPluginVersion)
                            configuration {
                                acceptPomPackaging("true")
                                excludedScopes("test,provided")
                                failIfWarning("false")
                                failOnMissing("\${license.failOnMissing}")
                                failOnBlacklist("\${license.failOnBlacklist}")
                                if (project.rootProject.hasProperty("excludeIbmGroups")) {
                                    excludedGroups('com.ibm.mq|com.ibm')
                                }
                                useMissingFile("false")
                                useRepositoryMissingFiles("false")
                                licensesOutputDirectory("\${license.output.directory}")
                                outputDirectory("\${license.output.directory}")
                                skip("\${license.skip}")
                            }
                            executions {
                                execution {
                                    id("license-check")
                                    phase("generate-resources")
                                    goals {
                                        goal("add-third-party")
                                        goal("download-licenses")
                                    }
                                }
                            }
                        }
                    }
                }
                markupBuilder.dependencies {
                    resolvedArtifacts.forEach { artifactGAV ->
                        dependency {
                            groupId(artifactGAV.group)
                            artifactId(artifactGAV.artifact)
                            version(artifactGAV.version)
                            if (artifactGAV.classifier != null) {
                                classifier(artifactGAV.classifier)
                            }
                            if (artifactGAV.extension != null) {
                                type(artifactGAV.extension)
                            }
                        }
                    }
                }

            }
            def output = new CopyOnWriteArrayList<String>()
            def mavenHome = System.getProperty("MAVEN_HOME", System.getenv("MAVEN_HOME"))
            def command
            if (mavenHome == null) {
                command = "mvn"
            } else {
                command = "$mavenHome/bin/mvn"
            }

            def licenseArgs
            def mavenParameters = project.findProperty(MavenParametersUtils.MAVEN_LICENSE_PARAMETERS)
            if (mavenParameters != null) {
                LOGGER.info("Maven license parameters: $mavenParameters")
                licenseArgs = mavenParameters
                        .toString()
                        .replaceAll(/^['"]|['"]$/, '')
                        .plus(" -Dlicense.output.directory=${licensesDirectory.toPath().toAbsolutePath().normalize()}")
                        .split(" ")
            } else {
                LOGGER.warn(("You're using a legacy method for parameter configuration that could be deleted in future. Please wrap all of the parameters in the 'maven-license-parameters'"))
                def licenseRegistryGitRepository = LicenseGradlePlugin.getLicenseRegistryGitRepository(project)
                if (licenseRegistryGitRepository == null) {
                    throw new IllegalArgumentException("Property '${LicenseGradlePlugin.LICENSE_REGISTRY_GIT_REPOSITORY_PROPERTY_NAME}' must be specified")
                }
                licenseArgs = [
                        "-Doctopus-license-maven-plugin.version=${octopusLicenseMavenPluginVersion}",
                        "-Dlicense.skip=false",
                        "-Dlicense.includeTransitiveDependencies=false",
                        "-Dlicense-registry.git-repository=${licenseRegistryGitRepository}",
                        "-Dlicense.failOnMissing=${(project.findProperty("license-fail-missing") ?: "true")}",
                        "-Dlicense.failOnBlacklist=${(project.findProperty("license-fail-on-black-list") ?: "true")}",
                        "-Dlicense.output.directory=${licensesDirectory.toPath().toAbsolutePath().normalize()}",
                        "-Dlicense.includedDependenciesWhitelist=${(project.findProperty("license-deps-whitelist") ?: "")}",
                        "-Dlicense.failOnNotWhitelistedDependency=${(project.findProperty("license-fail-on-not-whitelisted-dep") ?: "false")}",
                        "-Dlicense.fileWhitelist=${LicenseGradlePlugin.getLicenseWhitelistParameter(project)}"
                ]
            }
            def processInstance = LocalProcessBuilderFactory.newLocalProcessBuilder()
                    .command(command)
                    .mapBatExtension()
                    .mapCmdExtension()
                    .mapBashExtension()
                    .mapShExtension()
                    .envVariables(Collections.singletonMap("JAVA_HOME", System.getProperty("java.home")))
                    .logger { it.logger(LOGGER) }
                    .stdOutConsumer { out ->
                        output.add(out)
                    }.stdErrConsumer { err ->
                output.add(err)
            }.processInstance {
                it.headLimit(5)
                it.tailLimit(20)
            }
                    .build()
                    .execute("-f",
                            licensesPom.toPath().toAbsolutePath().normalize(),
                            "-B",
                            "-Pnexus-staging",
                            *licenseArgs,
                            "generate-resources").toCompletableFuture().get()
            def retCode = processInstance.exitCode
            project.file("build/licenses-mvn.log").text = output.join('\n')
            if (retCode != 0) {
                if (!LOGGER.isDebugEnabled()) {
                    LOGGER.error("License processing output is {}", output.join('\n'))
                }
                throw new GradleException("Fail to execute maven command $retCode:\n\t" + String.join('\n\t', processInstance.stdErr))
            }
        }
    }

}
