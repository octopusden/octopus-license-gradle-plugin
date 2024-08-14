package org.octopusden.octopus.license.management.plugins.gradle.utils

import com.platformlib.process.api.ProcessInstance
import com.platformlib.process.configurator.ProcessOutputConfigurator
import com.platformlib.process.local.factory.LocalProcessBuilderFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LicenseRegistryClient {
    private String vcsUrl
    private File repo = null
    private final Logger LOGGER = LoggerFactory.getLogger(LicenseRegistryClient.class);

    LicenseRegistryClient(String vcsUrl) {
        this.vcsUrl = vcsUrl
    }

    private File getCopy() {
        File dir = File.createTempDir()
        dir.deleteOnExit()
        final ProcessInstance processInstance = LocalProcessBuilderFactory
                .newLocalProcessBuilder()
                .logger(configuration -> configuration.logger(LOGGER))
                .processInstance(ProcessOutputConfigurator::unlimited)
                .command("git")
                .build()
                .execute("clone", "--depth=1", vcsUrl, dir)
                .toCompletableFuture()
                .join()
        if (processInstance.getExitCode() != 0) {
            LOGGER.error("The git clone command stdout: {}", String.join("\n", processInstance.getStdOut()));
            LOGGER.error("The git clone command stderr: {}", String.join("\n", processInstance.getStdErr()));
            throw new IllegalStateException("Unable to clone git repository " + dir);
        }
        return dir
    }

    String getFileContent(String file) {
        if (!file) {
            return ""
        }
        if (!repo) {
            repo = getCopy()
        }
        return new File(repo, file).text
    }
}
