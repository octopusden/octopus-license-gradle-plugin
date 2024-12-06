package org.octopusden.octopus.license.management.plugins.gradle.tasks

class LicenseManagementExtension {
    public String includePattern = null
    public String excludePattern = null

    def includeConfigurations(String... configurations) {
        includePattern = configurations.join("|")
    }

    def excludeConfigurations(String... configurations) {
        excludePattern = configurations.join("|")
    }
}
