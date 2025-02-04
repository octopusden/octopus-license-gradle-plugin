package org.octopusden.octopus.license.management.plugins.gradle

import com.github.gradle.node.yarn.task.YarnTask
import com.github.gradle.node.npm.task.NpxTask
import org.octopusden.octopus.license.management.plugins.gradle.tasks.LicenseManagementExtension
import org.octopusden.octopus.license.management.plugins.gradle.tasks.ProcessNodeLicensesTask
import org.octopusden.octopus.license.management.plugins.gradle.tasks.LicenseTask
import org.octopusden.octopus.license.management.plugins.gradle.tasks.LicensedDependenciesAnalyzingTask
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.util.GradleVersion

import static org.octopusden.octopus.license.management.plugins.gradle.utils.MavenParametersUtils.propertyIsFalse

class LicenseGradlePlugin implements Plugin<Project> {

    public static final String PACKAGE_JSON = 'package.json'
    public static final String LICENSE_SKIP_PROPERTY = "license.skip"
    public static final String NODE_SKIP_PROPERTY = "node.skip"
    private final static String NODE_LICENSE_GROUP = 'NodeLicense'
    private final static String DEFAULT_NODE_VERSION = '18.12.1'
    private final static String DEFAULT_NPM_VERSION = '8.19.2'
    private final static String DEFAULT_YARN_VERSION = '1.22.19'
    private final static String MINIMAL_GRADLE_VERSION_NODE_PLUGIN = '7.5.1'

    private void applyNodePlugins(Project project) {
        project.getPluginManager().apply('com.github.node-gradle.node')
    }

    private void configureNodePlugins(Project project) {
        project.node {
            download = true
            version = project.findProperty('node.version') ?: DEFAULT_NODE_VERSION
            npmVersion = project.findProperty('npm.version') ?: DEFAULT_NPM_VERSION
            yarnVersion = project.findProperty('yarn.version') ?: DEFAULT_YARN_VERSION
            // workDir = project.file("${project.buildDir}/nodejs")
            // npmWorkDir = project.file("${project.buildDir}/npm")
            // yarnWorkDir = project.file("${project.buildDir}/yarn")
            // nodeProjectDir = project.file("${project.projectDir}/frontend")
        }
    }

    private static String getEnvPath(Project project) {
        return ProcessNodeLicensesTask.getEnvPath(project)
    }

    private static ProcessNodeLicensesTask getProcessNodeLicensesTask(Project project) {
        project.tasks.findByName(ProcessNodeLicensesTask.NAME) as ProcessNodeLicensesTask
    }

    private static File getNodeLicenseWorkDir(Project project) {
        return getProcessNodeLicensesTask(project).getWorkingDir(project)
    }

    private void addNodeTasks(Project project) {
        if (propertyIsFalse(project, LICENSE_SKIP_PROPERTY)
                && propertyIsFalse(project, NODE_SKIP_PROPERTY)
                && !project.gradle.startParameter.offline) {
            project.tasks.register("yarnModulesInstall", YarnTask) {
                group = null
                args = ['install']
                environment['PATH'] = getEnvPath(project)
                doFirst {
                    assert new File(getNodeLicenseWorkDir(project), PACKAGE_JSON).exists()
                }
            }
            project.tasks.register("nodeLicenseCheckerInstall", YarnTask) {
                dependsOn("yarnSetup")
                args = ['global', 'add', 'license-checker']
                environment['PATH'] = getEnvPath(project)
                group = null
            }
            project.tasks.register("nodeLicenseCheckerProcess", NpxTask) {
                dependsOn('nodeLicenseCheckerInstall')
                group = null
                command = 'license-checker'
                description 'Run npm license-checker. Required:-Plicense.skip=false -Pnode.skip=false'
                environment['PATH'] = getEnvPath(project)
            }

            @Deprecated
            def processNpmLicenses = project.tasks.create('processNpmLicenses', ProcessNodeLicensesTask) {
                group = null
                description = "Deprecated task. Use ${ProcessNodeLicensesTask.NAME}"
                dependsOn(['nodeLicenseCheckerInstall', 'yarnModulesInstall'])
            }

            project.tasks.create(ProcessNodeLicensesTask.NAME, ProcessNodeLicensesTask) {
                group = NODE_LICENSE_GROUP
                dependsOn(['nodeLicenseCheckerInstall', 'yarnModulesInstall'])
            }
            project.afterEvaluate {
                ProcessNodeLicensesTask processNodeLicensesTask = getProcessNodeLicensesTask(project)
                if (processNodeLicensesTask.licenseRegistry == null) {
                    throw new IllegalArgumentException("Property 'license-registry.git-repository' must be specified")
                }
                if (processNodeLicensesTask.production) {
                    (project.tasks.findByName('yarnModulesInstall') as YarnTask)?.args.with {
                        addAll('--omit=dev')
                    }
                }

                if(!new File(processNodeLicensesTask.start, PACKAGE_JSON).exists() &&
                new File(processNpmLicenses.start, PACKAGE_JSON).exists()){
                    processNodeLicensesTask.start = processNpmLicenses.start
                }

                project.node.nodeProjectDir = processNodeLicensesTask.start
            }
        } else {
            project.tasks.create(ProcessNodeLicensesTask.NAME, DefaultTask) {
                group = NODE_LICENSE_GROUP
            }
        }
    }

    @Override
    void apply(Project project) {
        if (!project.extensions.findByName("licenseManagement")) {
            project.extensions.create("licenseManagement", LicenseManagementExtension)
        }
        if (GradleVersion.current() >= GradleVersion.version(MINIMAL_GRADLE_VERSION_NODE_PLUGIN)) {
            applyNodePlugins(project)
            configureNodePlugins(project)
            addNodeTasks(project)
        }
        createTask(project, false)
        createTask(project, true)
    }

    static def createTask(Project project, boolean onlyCurrent) {
        def processLicensedDependenciesTaskName = onlyCurrent ? "processModuleLicensedDependencies" : "processLicensedDependencies"
        def processLicensesTaskName = onlyCurrent ? "processModuleLicenses" : "processLicenses"

        Task processLicensedDependencies = project.getTasks().create(processLicensedDependenciesTaskName, LicensedDependenciesAnalyzingTask.class) {
            onlyCurrentProject = onlyCurrent
        }
        Task processLicenses = project.getTasks().create(processLicensesTaskName, LicenseTask.class)
        def isLicenseCheckRequired = propertyIsFalse(project, LICENSE_SKIP_PROPERTY)
        processLicensedDependencies.onlyIf { return isLicenseCheckRequired }
        processLicenses.dependsOn(processLicensedDependencies)
        processLicenses.onlyIf { return isLicenseCheckRequired }
    }
}
