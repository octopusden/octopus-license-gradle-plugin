/*********************************************************************************
 * Use:
 * build.gradle
 *     plugins {
 *       id('org.octopusden.octopus.license-management')
 *     }
 *     processNpmLicenses {
 *        //https://www.npmjs.com/package/license-checker#options
 *        excludePackages = 'dom-walk@0.1.2;min-document@2.19.0'
 *        start = file("$projectDir/app")
 *    }
 *
 * Run: gradlew processNpmLicenses -Plicense.skip=false
 */
package org.octopusden.octopus.license.management.plugins.gradle.tasks

import com.github.gradle.node.npm.task.NpmTask
import org.octopusden.octopus.license.management.plugins.gradle.utils.LicenseRegistryClient
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.DefaultTask

class ProcessNpmLicensesTask extends DefaultTask {

    public final static String NAME = 'processNpmLicenses'
    public final static String DESCRIPTION = 'https://www.npmjs.com/package/license-checker#options Required:-Plicense.skip=false -Pnode.skip=false'

    public final static String LICENSE_REGISTRY_GIT_REPOSITORY_PROPERTY_NAME = "license-registry.git-repository"
    public final static String LICENSE_WHITELIST_PROPERTY_NAME = "license.fileWhitelist"
    public final static String LICENSE_REGISTRY_SEPARATOR = "|"
    public final static String NPM_LIST_SEPARATOR = ";"
    public final static String EOL = System.properties.'line.separator'

    public final String licenseRegistryGitRepository = project.findProperty(LICENSE_REGISTRY_GIT_REPOSITORY_PROPERTY_NAME)

    static File getWorkingDir(Project project) { return project.node.nodeProjectDir.get().asFile }

    @Input
    //  only show production dependencies.
    boolean production = true

    @Input
    //  only show development dependencies.
    boolean development = false

    @Input
    //  report guessed licenses as unknown licenses.
    boolean unknown = false

    @Input
    //  only list packages with unknown or guessed licenses.
    boolean onlyunknown = false

    @Input
    //  prefix column for component in csv format.
    boolean csvComponentPrefix = false

    @Input
    //  to add a custom Format file in JSON
    boolean customPath = false

    @Input
    //  look for direct dependencies only
    boolean direct = false

    @Input
    // [list] exclude modules which licenses are in the comma-separated list from the output
    @Optional
    String exclude

    @Input
    // [list] fail (exit with code 1) on the first occurrence of the licenses of the semicolon-separated list
    @Optional
    String failOn

    @Input
    // [list] restrict output to the packages (package@version) in the semicolon-separated list
    @Optional
    String packages

    @Input
    // restrict output to not include any package marked as private
    boolean excludePrivatePackages = false

    @Input
    // output a summary of the license usage',
    boolean summary = false

    @InputDirectory
    // [path] of the initial json to look for
    @Optional
    File start = getWorkingDir(project) ?: project.projectDir


    @Input
    // [list] restrict output to the packages (package@version) not in the semicolon-separated list
    @Optional
    String excludePackages

    @Input
    // output the location of the license files as relative paths
    boolean relativeLicensePath = true

    @Input
    // output in csv format.
    boolean csv = false

    @Input
    // output in json format.
    boolean json = true

    String getLicensesRelativePath(File path) {
        return project.buildDir.parentFile.relativePath(path)
    }

    String getLicensesRelativePath(String path) {
        return getLicensesRelativePath(project.file(path))
    }

    @Internal
    LicenseRegistryClient getLicenseRegistry() {
        return licenseRegistryGitRepository ?
                new LicenseRegistryClient(licenseRegistryGitRepository) : null
    }

    @Internal
    ArrayList<String> getLicenseRegistryWhiteList() {
        return licenseRegistry?.getFileContent(
                System.getProperty(LICENSE_WHITELIST_PROPERTY_NAME)
        )?.split("[$LICENSE_REGISTRY_SEPARATOR]")
    }

    @Internal
    LinkedHashMap<String, String> getLicenseRegistryAliases() {
        LinkedHashMap<String, String> aliases = [:]
        licenseRegistry?.getFileContent("merges.txt")?.eachLine {
            if (it.size()) {
                List<String> a = it.split("[$LICENSE_REGISTRY_SEPARATOR]")
                aliases[a[0]] = a.join(NPM_LIST_SEPARATOR)
            }
        }
        return aliases
    }

    @Internal
    Properties getLicenseFileList() {
        def text = licenseRegistry?.getFileContent("licenses.properties")
        Properties p = new Properties()
        p.load(new StringReader(text))
        return p
    }

    String getAllowedLicensesWithAliases(String licenses) {
        def aliases = licenseRegistryAliases
        return licenses?.split(NPM_LIST_SEPARATOR)?.collect { aliases.get(it, it) }?.join(NPM_LIST_SEPARATOR)
    }

    @Internal
    String getDefaultAllowedLicenses() {
        return licenseRegistryWhiteList?.join(NPM_LIST_SEPARATOR)
    }

    @Input
    // [list] fail (exit with code 1) on the first occurrence of the licenses not in the semicolon-separated list
    String onlyAllow = getAllowedLicensesWithAliases(getDefaultAllowedLicenses())

    @OutputDirectory
    File outDir = project.file(getLicensesRelativePath("${project.buildDir}/licenses"))

    @Internal
    String outFileName = "THIRD-PARTY-${project.name}"

    private static boolean isWindows() {
        return System.getProperty('os.name').containsIgnoreCase('win')
    }

    private static String getNodeBinDir(Project project) {
        String nodeDir = project.tasks.nodeSetup.nodeDir.get().asFile.path
        if (isWindows()) return nodeDir
        return new File(nodeDir, 'bin').path
    }

    static String getEnvPath(Project project) {
        return getNodeBinDir(project) +
                System.properties.'path.separator' +
                System.env.Path
    }

    ProcessNpmLicensesTask() {
        description = DESCRIPTION
    }

    String npmFile(String f) {
        return f.replace('\\', '/')
    }

    String npmFile(File f) {
        return npmFile(f.path)
    }

    def runLicenseChecker(File workDir, File outFile) {
        NpmTask npm = project.tasks.('nodeLicenseCheckerProcess')
        npm.with {
            environment['PATH'] = getEnvPath(project)
            args.addAll('license-checker')
            if (production) args.addAll("--production")
            if (development) args.addAll("--development")
            if (unknown) args.addAll("--unknown")
            if (onlyunknown) args.addAll("--onlyunknown")
            if (csvComponentPrefix) args.addAll("--csvComponentPrefix")
            if (customPath) args.addAll("--customPath")
            if (direct) args.addAll("--direct")
            if (json) args.addAll("--json")
            if (csv) args.addAll("--csv")
            if (onlyAllow) args.addAll('--onlyAllow', onlyAllow)
            if (excludePrivatePackages) args.addAll("--excludePrivatePackages")
            if (packages) args.addAll("--packages", packages)
            if (exclude) args.addAll("--exclude", exclude)
            if (failOn) args.addAll("--failOn", failOn)
            if (summary) args.addAll("--summary")
            if (relativeLicensePath) args.addAll("--relativeLicensePath")
            args.addAll('--out', npmFile(outFile))
            args.addAll("--start", npmFile(workDir))
            return exec()
        }
    }

    @TaskAction
    void execute() {
        def workingDir = getWorkingDir(project)
        File jsonFile = project.file("$workingDir/${outFileName}.json")
        runLicenseChecker(workingDir, jsonFile)
        File licenseFile = new File(outDir, "${outFileName}.txt")
        saveLicenses(workingDir, jsonFile, outDir, licenseFile)
    }

    def saveLicenses(File workDir, File jsonFile, File licenseDir, File mainLicenseFile) {
        def dependencies = []
        final Properties fileList = licenseFileList
        def listAliases = licenseRegistryAliases
        if (json)
        //name:{licenses:,repository:,publisher:,url:,path:,licenseFile:}
            new groovy.json.JsonSlurper().parse(jsonFile).each { k, v ->
                def module = k.replace('/', ' ').replaceAll('(@\\d.*|@)', '')
                def text = "(${v.licenses}) ${module} ($k - ${v.repository})"
                def alias = null
                for (aliases in listAliases) {
                    if (v.licenses in aliases) {
                        logger.debug("Find alias '${aliases[0]}' of license '${v.licenses}'")
                        alias = aliases[0]
                        break
                    }
                }
                if (alias) {
                    text = "($alias) " + text
                }
                dependencies.add(text)
                def license = alias ?: v.licenses
                File licenseFile = new File("${licenseDir}/${license}.txt")
                if (!licenseFile.exists()) {
                    def textLicense = null
                    if (fileList.containsKey(license)) {
                        textLicense = licenseRegistry?.getFileContent(fileList.get(license))
                    } else {
                        logger.warn("Standart license text not found for '${license}'")
                        textLicense = new File(workDir, v.licenseFile as String).text
                    }
                    if (textLicense)
                        licenseFile.text = textLicense
                    else
                        logger.error("License text not found for '${license}'")
                }
            }
        def text = "The product includes a number of subcomponents with separate copyright notices and license terms.$EOL" +
                "Your use of the source code for the these subcomponents is subject to the terms and conditions " +
                "of the following licenses located in files in licenses folder$EOL$EOL" +
                (dependencies ? "Lists of ${dependencies.size()} third-party dependencies.$EOL"
                        : "The project has no dependencies.$EOL")
        dependencies.each { text += "     $it$EOL" }
        mainLicenseFile.text = text
    }

}