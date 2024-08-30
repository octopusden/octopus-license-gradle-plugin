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
const val LICENSE_REGISTRY_GIT_REPOSITORY_PROPERTY = "license-registry.git-repository"

open class TestGradleDSL {
    lateinit var testProjectName: String
    var additionalArguments: Array<String> = arrayOf()
    var tasks: Array<String> = arrayOf()
    var clean = false
}

data class ZipTreeEntry(val name: String)

fun zipTreeEntries(zipPath: Path): Collection<ZipTreeEntry> {
    return FileSystems.newFileSystem(zipPath, null).use { fileSystem ->
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
    val lrgr = System.getProperty(LICENSE_REGISTRY_GIT_REPOSITORY_PROPERTY,
        System.getenv().getOrDefault(LICENSE_REGISTRY_GIT_REPOSITORY_PROPERTY,
            System.getenv(LICENSE_REGISTRY_GIT_REPOSITORY_PROPERTY.uppercase().replace(Regex("[.-]"), "_"))))

    val defaultOptions = arrayOf(
        "-s",
        "--info",
        "-Plicense.skip=false",
        "build",
        "-Plicense-management.version=${System.getenv().getOrDefault("license-management.version", "1.0-SNAPSHOT")}",
        "-Psupported-groups=${System.getenv().getOrDefault("supported-groups", System.getenv("SUPPORTED_GROUPS"))}",
        "-Plicense-registry.git-repository=${System.getenv().getOrDefault("license-registry.git-repository", System.getenv("LICENSE_REGISTRY_GIT_REPOSITORY"))}"
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
            .envVariables(mapOf("JAVA_HOME" to System.getProperties().getProperty("java.home")))
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
