package org.octopusden.octopus.license.management.plugins.gradle.license

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Files
import java.util.stream.Stream

class LicensePluginTest {
    /**
     * Test that license check is performed for dependencies with transitive = false.
     */
    @ParameterizedTest
    @ValueSource(strings = ["transitive-dep-false", "transitive-conf-false"])
    fun testTransitiveFalse(projectName: String) {
        //Publish zip artifact with declared dependencies
        gradle {
            testProjectName = "zip-with-dependencies"
            tasks = arrayOf("publishToMavenLocal")
        }
        //Build project which depends on published zip artifact
        val projectPath = gradle {
            testProjectName = projectName
            additionalArguments = arrayOf("-PexcludeIbmGroups", "-Psupported-groups=org.octopusden.octopus")
        }
        val jsonDependenciesPath = projectPath.resolve("build/dependencies.json")
        assertThat(jsonDependenciesPath).exists()
        assertThat(zipTreeEntries(projectPath.resolve("build/distr/single-module.zip")))
            .contains(
                ZipTreeEntry("licenses/apache-2.0 - apache-2.0.txt"),
                ZipTreeEntry("licenses/THIRD-PARTY.txt")
            )
    }

    @Test
    fun testSupportedGroups() {
        val projectPath = gradle {
            testProjectName = "supported-groups"
            additionalArguments = arrayOf("-PexcludeIbmGroups", "-Psupported-groups=org.octopusden.octopus.releng,org.octopusden.octopus.jira")
        }
        val jsonDependenciesPath = projectPath.resolve("build/dependencies.json")
        assertThat(jsonDependenciesPath).exists()
        assertThat(String(Files.readAllBytes(jsonDependenciesPath))).doesNotContain("org.octopusden.octopus")
    }

    @Test
    fun testSingleModule() {
        val projectPath = gradle {
            testProjectName = "single-module"
        }
        assertThat(zipTreeEntries(projectPath.resolve("build/distr/single-module.zip")))
            .containsOnly(
                ZipTreeEntry("licenses/apache-2.0 - apache-2.0.txt"),
                ZipTreeEntry("licenses/THIRD-PARTY.txt"),
                ZipTreeEntry("sshd-common-2.6.0.jar"),
                ZipTreeEntry("sshd-core-2.6.0.jar")
            )
    }

    @Test
    fun testMultiModule() {
        val projectPath = gradle {
            testProjectName = "multi-module"
        }
        assertThat(zipTreeEntries(projectPath.resolve("module-2/build/distr/multi-module.zip")))
            .contains(
                ZipTreeEntry("licenses/apache-2.0 - apache-2.0.txt"),
                ZipTreeEntry("licenses/mit - mit.txt"),
                ZipTreeEntry("licenses/THIRD-PARTY.txt")
            )
    }

    @Test
    fun testIbmLibraries() {
        val (instance, _) = gradleProcessInstance {
            testProjectName = "ibm-libraries"
        }
        assertNotEquals(0, instance.exitCode)
        assertThat(instance.stdErr).contains("[ERROR] License \"IBM International Program License Agreement\" used by 1 dependencies:")
        val projectPath = gradle {
            testProjectName = "ibm-libraries"
            additionalArguments = arrayOf("-PexcludeIbmGroups")
        }
        assertThat(zipTreeEntries(projectPath.resolve("build/distr/ibm-module.zip")))
            .contains(
                ZipTreeEntry("licenses/apache-2.0 - apache-2.0.txt"),
                ZipTreeEntry("licenses/mit - mit.txt"),
                ZipTreeEntry("licenses/THIRD-PARTY.txt"),
                ZipTreeEntry("slf4j-api-1.7.30.jar"),
                ZipTreeEntry("sshd-common-2.6.0.jar"),
                ZipTreeEntry("sshd-core-2.6.0.jar"),
                ZipTreeEntry("com.ibm.mq.allclient-9.4.0.0.jar"),
            )
    }

    @ParameterizedTest
    @MethodSource("includePatternData")
    fun testIncludePattern(includePattern: String, expectedEntries: Collection<ZipTreeEntry>) {
        val projectPath = gradle {
            clean = true
            testProjectName = "include-pattern"
            additionalArguments = arrayOf("-PINCLUDE_PATTERN=$includePattern")
        }
        val jsonDependenciesPath = projectPath.resolve("build/dependencies.json")
        assertThat(jsonDependenciesPath).exists()
        assertThat(zipTreeEntries(projectPath.resolve("build/distr/single-module.zip"))).containsExactlyInAnyOrderElementsOf(
            expectedEntries
        )
    }

    @Test
    @DisplayName("Test including dependency only one time")
    fun testLCTRL116() {
        val projectPath = gradle {
            testProjectName = "LCTRL-116"
        }
        val generatedMavenPom = projectPath.resolve("build/licenses-pom.xml")
        assertThat(generatedMavenPom).exists()
        val licensePom = XmlMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .registerKotlinModule()
            .readValue(generatedMavenPom.toFile(), LicensePom::class.java)
        assertThat(licensePom.dependencies).containsExactly(LicenseDependency("org.slf4j", "slf4j-api", "2.0.3", "jar"))
    }

    @DisplayName("Test dependency with variants on Gradle 7.5.1")
    @Test
    fun testLCTRL110() {
        val projectPath = gradle {
            testProjectName = "LCTRL-110"
        }
        val jsonDependenciesPath = projectPath.resolve("build/dependencies.json")
        assertThat(jsonDependenciesPath).exists()
        assertThat(String(Files.readAllBytes(jsonDependenciesPath))).contains("caffeine")
    }

    @Test
    @DisplayName("Test org.octopusden.octopus supporting")
    fun testLCTRL112() {
        val projectPath = gradle {
            testProjectName = "LCTRL-112"
            additionalArguments = arrayOf("-Psupported-groups=org.octopusden.octopus")
        }
        val jsonDependenciesPath = projectPath.resolve("module-2/build/dependencies.json")
        assertThat(jsonDependenciesPath).exists()
        assertThat(String(Files.readAllBytes(jsonDependenciesPath))).doesNotContain("org.octopusden.octopus")
    }

    @Test
    @DisplayName("Test license plugin usage in more than one subproject")
    fun testLCTRL122() {
        val projectPath = gradle {
            testProjectName = "LCTRL-122"
        }
        assertThat(zipTreeEntries(projectPath.resolve("module-1/build/distr/module1.zip")))
            .containsExactlyInAnyOrderElementsOf(
                listOf(
                    ZipTreeEntry("licenses/mit - mit.txt"),
                    ZipTreeEntry("licenses/epl-1.0 - epl-1.0.txt"),
                    ZipTreeEntry("licenses/lgpl-2.1 - lgpl-2.1.txt"),
                    ZipTreeEntry("licenses/apache-2.0 - apache-2.0.txt"),
                    ZipTreeEntry("licenses/THIRD-PARTY.txt")
                )
            )

        assertThat(zipTreeEntries(projectPath.resolve("module-2/build/distr/module2.zip")))
            .containsExactlyInAnyOrderElementsOf(
                listOf(
                    ZipTreeEntry("licenses/mit - mit.txt"),
                    ZipTreeEntry("licenses/epl-1.0 - epl-1.0.txt"),
                    ZipTreeEntry("licenses/lgpl-2.1 - lgpl-2.1.txt"),
                    ZipTreeEntry("licenses/apache-2.0 - apache-2.0.txt"),
                    ZipTreeEntry("licenses/THIRD-PARTY.txt")
                )
            )
    }

    @Test
    @DisplayName("Test license generation per module")
    fun testLCTRL136() {
        val projectPath = gradle {
            testProjectName = "LCTRL-136"
        }
        assertThat(zipTreeEntries(projectPath.resolve("module-1/build/distr/module1.zip")))
            .containsExactlyInAnyOrderElementsOf(
                listOf(
                    ZipTreeEntry("licenses/mit - mit.txt"),
                    ZipTreeEntry("licenses/apache-2.0 - apache-2.0.txt"),
                    ZipTreeEntry("licenses/THIRD-PARTY.txt")
                )
            )

        assertThat(zipTreeEntries(projectPath.resolve("module-2/build/distr/module2.zip")))
            .containsExactlyInAnyOrderElementsOf(
                listOf(
                    ZipTreeEntry("licenses/mit - mit.txt"),
                    ZipTreeEntry("licenses/epl-1.0 - epl-1.0.txt"),
                    ZipTreeEntry("licenses/lgpl-2.1 - lgpl-2.1.txt"),
                    ZipTreeEntry("licenses/THIRD-PARTY.txt")
                )
            )
    }

    @Test
    fun testNodeJava() {
        val projectPath = gradle {
            testProjectName = "node-java"
            additionalArguments = arrayOf("-Pnode.skip=false")
        }
        assertThat(zipTreeEntries(projectPath.resolve("build/libs/node-java.jar")))
            .containsExactlyInAnyOrderElementsOf(
                listOf(
                    ZipTreeEntry("licenses/0BSD.txt"),
                    ZipTreeEntry("licenses/Apache-2.0.txt"),
                    ZipTreeEntry("licenses/apache-2.0 - apache-2.0.txt"),
                    ZipTreeEntry("licenses/BSD-3-Clause.txt"),
                    ZipTreeEntry("licenses/ISC.txt"),
                    ZipTreeEntry("licenses/MIT.txt"),
                    ZipTreeEntry("licenses/THIRD-PARTY.txt"),
                    ZipTreeEntry("licenses/THIRD-PARTY-node-java.txt"),
                    ZipTreeEntry("META-INF/MANIFEST.MF"),
                    ZipTreeEntry("org/octopusden/octopus/nodejava/NodeJava.class")
                )
            )
    }

    @Test
    fun testNodeSingleModule() {
        val projectPath = gradle {
            testProjectName = "node-single-module"
            additionalArguments = arrayOf("-Pnode.skip=false")
        }
        assertThat(zipTreeEntries(projectPath.resolve("build/distr/node-single-module.zip")))
            .containsExactlyInAnyOrderElementsOf(
                listOf(
                    ZipTreeEntry("licenses/0BSD.txt"),
                    ZipTreeEntry("licenses/Apache-2.0.txt"),
                    ZipTreeEntry("licenses/BSD-3-Clause.txt"),
                    ZipTreeEntry("licenses/ISC.txt"),
                    ZipTreeEntry("licenses/MIT.txt"),
                    ZipTreeEntry("licenses/THIRD-PARTY-node-single-module.txt"),
                )
            )
    }

    @Test
    fun testNodeMultiModule() {
        val projectPath = gradle {
            testProjectName = "node-multi-module"
            additionalArguments = arrayOf("-Pnode.skip=false")
        }
        assertThat(zipTreeEntries(projectPath.resolve("module-1/build/distr/node-module1.zip")))
            .containsExactlyInAnyOrderElementsOf(
                listOf(
                    ZipTreeEntry("licenses/0BSD.txt"),
                    ZipTreeEntry("licenses/Apache-2.0.txt"),
                    ZipTreeEntry("licenses/BSD-3-Clause.txt"),
                    ZipTreeEntry("licenses/ISC.txt"),
                    ZipTreeEntry("licenses/MIT.txt"),
                    ZipTreeEntry("licenses/THIRD-PARTY-module-1.txt"),
                )
            )
        assertThat(zipTreeEntries(projectPath.resolve("module-2/build/distr/node-module2.zip")))
            .containsExactlyInAnyOrderElementsOf(
                listOf(
                    ZipTreeEntry("licenses/MIT.txt"),
                    ZipTreeEntry("licenses/THIRD-PARTY-module-2.txt"),
                )
            )
    }

    @Test
    fun testNodeYarnModule() {
        val projectPath = gradle {
            testProjectName = "node-yarn-module"
            additionalArguments = arrayOf("-Pnode.skip=false")
        }
        assertThat(zipTreeEntries(projectPath.resolve("build/distr/node-yarn-module.zip")))
            .containsExactlyInAnyOrderElementsOf(
                listOf(
                    ZipTreeEntry("licenses/(BSD-3-Clause AND Apache-2.0).txt"),
                    ZipTreeEntry("licenses/0BSD.txt"),
                    ZipTreeEntry("licenses/Apache-2.0.txt"),
                    ZipTreeEntry("licenses/BSD-2-Clause.txt"),
                    ZipTreeEntry("licenses/BSD-3-Clause.txt"),
                    ZipTreeEntry("licenses/ISC.txt"),
                    ZipTreeEntry("licenses/MIT.txt"),
                    ZipTreeEntry("licenses/UNLICENSED.txt"),
                    ZipTreeEntry("licenses/THIRD-PARTY-node-yarn-module.txt"),
                )
            )
    }

    @Test
    fun testDependencySubstitution() {
        val projectPath = gradle {
            testProjectName = "dependency-substitution"
        }
        assertThat(zipTreeEntries(projectPath.resolve("build/distr/dependency-substitution.zip")))
            .containsExactlyInAnyOrderElementsOf(
                listOf(
                    ZipTreeEntry("licenses/apache-2.0 - apache-2.0.txt"),
                    ZipTreeEntry("licenses/mit - mit.txt"),
                    ZipTreeEntry("licenses/THIRD-PARTY.txt"),
                )
            )
    }

    @Test
    fun testOctopusRelengPluginCompatibility() {
        val projectPath = gradle {
            testProjectName = "octopus-releng-plugin-compatibility"
            additionalEnvVariables = mapOf("ARTIFACTORY_URL" to "artifactory url")
        }
        assertThat(zipTreeEntries(projectPath.resolve("build/distr/octopus-releng-plugin-compatibility.zip")))
            .containsOnly(
                ZipTreeEntry("licenses/apache-2.0 - apache-2.0.txt"),
                ZipTreeEntry("licenses/THIRD-PARTY.txt"),
                ZipTreeEntry("sshd-common-2.6.0.jar"),
                ZipTreeEntry("sshd-core-2.6.0.jar")
            )
    }

    @Test
    fun testPropertiesOverride() {
        val projectPath = gradle {
            testProjectName = "properties-override"
        }
        assertThat(zipTreeEntries(projectPath.resolve("build/distr/properties-override.zip")))
            .contains(
                ZipTreeEntry("licenses/apache-2.0 - apache-2.0.txt"),
                ZipTreeEntry("licenses/THIRD-PARTY.txt"),
                ZipTreeEntry("sshd-common-2.6.0.jar"),
                ZipTreeEntry("sshd-core-2.6.0.jar")
            )
    }

    companion object {
        @JvmStatic
        fun includePatternData(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "log.*",
                listOf(
                    ZipTreeEntry("licenses/apache-2.0 - apache-2.0.txt"),
                    ZipTreeEntry("slf4j-api-1.6.1.jar"),
                    ZipTreeEntry("log4j-api-2.19.0.jar"),
                    ZipTreeEntry("licenses/THIRD-PARTY.txt")
                )
            ),
            Arguments.of(
                "slf.*",
                listOf(
                    ZipTreeEntry("licenses/mit - mit.txt"),
                    ZipTreeEntry("slf4j-api-1.6.1.jar"),
                    ZipTreeEntry("log4j-api-2.19.0.jar"),
                    ZipTreeEntry("licenses/THIRD-PARTY.txt")
                )
            )
        )
    }
}

data class LicensePom(val dependencies: Collection<LicenseDependency>)

data class LicenseDependency(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val type: String?
)
