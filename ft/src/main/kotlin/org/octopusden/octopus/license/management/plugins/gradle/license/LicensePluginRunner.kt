package org.octopusden.octopus.license.management.plugins.gradle.license

import com.platformlib.process.api.ProcessInstance
import com.platformlib.process.builder.ProcessBuilder
import com.platformlib.process.factory.ProcessBuilders
import com.platformlib.process.local.specification.LocalProcessSpec
import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.lang.System
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors

val LOGGER = LoggerFactory.getLogger("org.octopusden.octopus.license.management.plugins.gradle.license")
val testLicenseRegistryGitRepository by lazy {
    val resource = TestGradleDSL::class.java.getResource("/license-registry.git")
        ?: throw IllegalArgumentException("License registry not found in resources")
    Paths.get(resource.toURI())
}

open class TestGradleDSL {
    lateinit var testProjectName: String
    var additionalArguments: Array<String> = arrayOf()
    var additionalEnvVariables: Map<String, String> = mapOf()
    var tasks: Array<String> = arrayOf()
    var clean = false
}

data class ZipTreeEntry(val name: String)

fun zipTreeEntries(zipPath: Path): Collection<ZipTreeEntry> {
    val classLoader: ClassLoader? = null
    return FileSystems.newFileSystem(zipPath, classLoader).use { fileSystem ->
        Files.walk(fileSystem.getPath("/")).filter { !Files.isDirectory(it) }.map { ZipTreeEntry(it.toString().replaceFirst("/", "")) }.collect(Collectors.toList())
    }
}

fun gradle(init: TestGradleDSL.() -> Unit): Path {
    val (processInstance, projectPath) = gradleProcessInstance(init)
    if (processInstance.exitCode != 0) {
        throw IllegalStateException("An error while gradle exec, exit code is ${processInstance.exitCode}")
    }
    return projectPath
}

fun gradleProcessInstance(init: TestGradleDSL.() -> Unit): Pair<ProcessInstance, Path> {
    val testGradleDSL = TestGradleDSL()
    init.invoke(testGradleDSL)
    val resource = TestGradleDSL::class.java.getResource("/projects/${testGradleDSL.testProjectName}") ?: throw IllegalArgumentException("The specified project ${testGradleDSL.testProjectName} hasn't been found in resources")
    val projectPath = Paths.get(resource.toURI())
    if (!Files.isDirectory(projectPath)) {
        throw IllegalArgumentException("The specified project '${testGradleDSL.testProjectName}' hasn't been found at $projectPath")
    }

    val octopusLicenseManagementGradlePluginVersion = System.getenv().getOrDefault("OCTOPUS_LICENSE_GRADLE_PLUGIN_VERSION", "2.0-SNAPSHOT")
    val licenseMavenPluginVersion = System.getenv("OCTOPUS_LICENSE_MAVEN_PLUGIN_VERSION")
    val supportedGroups = System.getenv("SUPPORTED_GROUPS")
    val octopusReleaseManagementVersion = System.getenv().getOrDefault(
        "OCTOPUS_RELEASE_MANAGEMENT_PLUGIN_VERSION", System.getenv("OCTOPUS_RELEASE_MANAGEMENT_GRADLE_PLUGIN_VERSION")
    )

    val missingProperties = listOfNotNull(
        if (licenseMavenPluginVersion == null) "OCTOPUS_LICENSE_MAVEN_PLUGIN_VERSION" else null,
        if (octopusReleaseManagementVersion == null) "OCTOPUS_RELEASE_MANAGEMENT_PLUGIN_VERSION or OCTOPUS_RELEASE_MANAGEMENT_GRADLE_PLUGIN_VERSION" else null
    )

    if (missingProperties.isNotEmpty()) {
        throw IllegalArgumentException(
            "The following properties must be set on environment variables:\n" +
                    missingProperties.joinToString("\n") { "  - $it" }
        )
    }

    val mavenLicenseParameters = "-Dlicense-registry.git-repository=$testLicenseRegistryGitRepository " +
            "-Dlicense.skip=false " +
            "-Doctopus-license-maven-plugin.version=$licenseMavenPluginVersion " +
            "-Dlicense.includeTransitiveDependencies=false " +
            "-Dlicense.failOnMissing=true " +
            "-Dlicense.failOnBlacklist=true"

    val defaultOptions = arrayOf(
        "-s",
        "--info",
        "build",
        "-Poctopus-license-management-gradle-plugin.version=${octopusLicenseManagementGradlePluginVersion}",
        "-Poctopus-release-management.version=${octopusReleaseManagementVersion}",
        "-Puse_dev_repository=plugins",
        "-Pmaven-license-parameters=\'${mavenLicenseParameters}\'",
        "-Psupported-groups=${supportedGroups}"
    )
    val caa = ArrayList<String>()
    if (testGradleDSL.clean) {
        caa.add("clean")
    }
    if (testGradleDSL.tasks.isEmpty()) {
        caa.addAll(defaultOptions)
    } else {
        caa.addAll(testGradleDSL.tasks)
    }
    return Pair(ProcessBuilders
            .newProcessBuilder<ProcessBuilder>(LocalProcessSpec.LOCAL_COMMAND)
            .envVariables(mapOf("JAVA_HOME" to System.getProperties().getProperty("java.home")) + testGradleDSL.additionalEnvVariables)
            .logger { it.logger(LOGGER) }
            .defaultExtensionMapping()
            .workDirectory(projectPath)
            .processInstance { processInstanceConfiguration -> processInstanceConfiguration.stdErr { it.unlimited()} }
            .commandAndArguments("$projectPath/gradlew", "--no-daemon")
            .build()
            .execute(*(caa + testGradleDSL.additionalArguments).toTypedArray())
            .toCompletableFuture()
            .join(), projectPath)
}
