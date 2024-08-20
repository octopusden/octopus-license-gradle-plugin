package org.octopusden.octopus.license.management.plugins.gradle

import com.github.gradle.node.npm.task.NpmTask
import com.github.gradle.node.npm.task.NpxTask
import org.octopusden.octopus.license.management.plugins.gradle.tasks.ProcessNpmLicensesTask
import org.octopusden.octopus.license.management.plugins.gradle.tasks.LicenseTask
import org.octopusden.octopus.license.management.plugins.gradle.tasks.LicensedDependenciesAnalyzingTask
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.util.GradleVersion

class LicenseGradlePlugin implements Plugin<Project> {

    public static final String NODE_SKIP_PROPERTY = "node.skip"
    private final static String NPM_LICENSE_GROUP = 'NpmLicense'
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

    private boolean isTrue(Project project, String property) {
        return 'true'.equalsIgnoreCase(project.rootProject.findProperty(property) as String ?: 'true')
    }

    private boolean nodeOnlyIf(Project project) {
        return !isTrue(project, 'license.skip') && !isTrue(project, NODE_SKIP_PROPERTY) && !project.gradle.startParameter.offline
    }

    private String getEnvPath(Project project) {
        return ProcessNpmLicensesTask.getEnvPath(project)
    }

    private void addNodeTasks(Project project) {
        boolean useNode = nodeOnlyIf(project)
        if (useNode) {
            def path = getEnvPath(project)
            project.tasks.register("nodeModulesInstall", NpmTask) {
                group = null
                args = ['install']
                environment['PATH'] = getEnvPath(project)
                doFirst {
                    ProcessNpmLicensesTask processNpmLicensesTask = project.tasks.findByName(ProcessNpmLicensesTask.NAME)
                    def nodeLicenseWorkDir = ProcessNpmLicensesTask.getWorkingDir(project)
                    assert new File(nodeLicenseWorkDir, 'package.json').exists()
                }
            }
            project.tasks.register("nodeLicenseCheckerInstall", NpmTask) {
                args = ['install', '-g', 'license-checker']
                environment['PATH'] = getEnvPath(project)
                group = null
            }
            project.tasks.register("nodeLicenseCheckerProcess", NpxTask) {
                dependsOn('nodeLicenseCheckerInstall')
                group = null
                command = 'license-checker'
                description 'Run npm license-checker. Required:-Plicense.skip=false -Pnode.skip=false'
                environment['PATH'] = getEnvPath(project)
                args = ['--verbose']
            }
            project.tasks.create(ProcessNpmLicensesTask.NAME, ProcessNpmLicensesTask) {
                group = NPM_LICENSE_GROUP
                dependsOn(['nodeLicenseCheckerInstall', 'nodeModulesInstall'])
            }
            project.afterEvaluate {
                ProcessNpmLicensesTask processNpmLicensesTask = project.tasks.findByName(ProcessNpmLicensesTask.NAME)
                if (processNpmLicensesTask.licenseRegistry == null) {
                    throw new IllegalArgumentException("Property 'license-registry.git-repository' must be specified")
                }
                if (processNpmLicensesTask.production) {
                    (project.tasks.findByName('nodeModulesInstall') as NpmTask)?.args.with {
                        addAll('--omit=dev')
                    }
                }
                project.node.nodeProjectDir = processNpmLicensesTask.start
            }
        } else {
            project.tasks.create(ProcessNpmLicensesTask.NAME, DefaultTask) {
                group = NPM_LICENSE_GROUP
            }
        }
    }

    @Override
    void apply(Project project) {
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
        processLicenses.dependsOn(processLicensedDependencies)
        processLicenses.onlyIf {
            "false".equalsIgnoreCase(project.findProperty("license.skip") as String ?: "true")
        }
    }
}
