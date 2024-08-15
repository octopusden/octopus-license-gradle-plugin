# License Management Gradle Plugin

- [Automated License Control](#automated-license-control)
- [The License Plugins DSL](#the-license-plugins-dsl)
    - [License Legacy Plugin Application](#license-legacy-plugin-application)
    - [Include Licenses in Distribution](#include-licenses-in-distribution)
- [Verify Used Licenses](#verify-used-licenses)
    - [Manually Checking Used Licenses](#manually-checking-used-licenses)
- [The Node Packages License Control](#the-node-packages-license-control)
    - [Include Node Packages Licenses in Distribution](#include-node-packages-licenses-in-distribution)
    - [Parameters of `processNpmLicenses` Task](#parameters-of-processnpmlicenses-task)
    - [Manually Checking Used Node Packages Licenses](#manually-checking-used-node-packages-licenses)


## Automated License Control
### The License Plugins DSL

To support Escrow and Continuous Integration processes, the project has to be configured to use the `org.octopusden.octopus.license-management` plugin.

**build.gradle**
```groovy
plugins {
    id 'org.octopusden.octopus.license-management'
}
```

**settings.gradle**
```groovy
pluginManagement {
    plugins {
        id 'org.octopusden.octopus.license-management' version settings['license-management.version']
    }
}
```

#### License Legacy Plugin Application

Applying the *org.octopusden.octopus.license-management* plugin with the `buildscript` block:

**build.gradle**
```groovy
buildscript {
    dependencies {
        classpath "org.octopusden.octopus:org.octopusden.octopus.license-management:${project.findProperty('license-management.version') ?: '{version-label}'}"
    }
}

apply plugin: 'org.octopusden.octopus.license-management'
```

#### Include Licenses in Distribution

There are 2 strategies for processing:

##### Full Project (default)

Licenses of the root project and all subprojects are placed into the directory of the module declaring license plugin usage.

To include licenses in distribution, the output of the `processLicenses` task should be used as source.

**build.gradle**
```groovy
plugins {
    id 'org.octopusden.octopus.license-management'
}

task componentDistribution(type: Zip) {
    from (processLicenses) {
        into 'licenses'
    }
}
```

To include licenses in a JAR:

**build.gradle**
```groovy
plugins {
    id 'org.octopusden.octopus.license-management'
    id 'java-library'
}

processResources {
    from (processLicenses) {
        into 'licenses'
    }
}
```

##### Per Module

Licenses of the module only are placed into the directory of the module declaring license plugin usage.

**build.gradle**
```groovy
plugins {
    id 'org.octopusden.octopus.license-management'
}

task componentDistribution(type: Zip) {
    from (processModuleLicenses) {
        into 'licenses'
    }
}
```

To include licenses in a JAR:

**build.gradle**
```groovy
plugins {
    id 'org.octopusden.octopus.license-management'
    id 'java-library'
}

processResources {
    from (processModuleLicenses) {
        into 'licenses'
    }
}
```

### Verify Used Licenses

To verify used licenses, specify a dependency for the `build` task on the `processLicenses` task.

**build.gradle**
```groovy
plugins {
    id 'org.octopusden.octopus.license-management'
    id 'java-library'
}
build.dependsOn processLicenses
```

#### Manually Checking Used Licenses

To process licenses (verify or include in distribution), set the project parameter `license.skip` to `false` (it is already configured on TeamCity C&UT standard templates).

```shell
gradle -Plicense.skip=false processLicenses
```

### The Node Packages License Control

The Node packages license is a part of the `org.octopusden.octopus.license-management` plugin.

Required Gradle version: 7.5.1 or above

#### Include Node Packages Licenses in Distribution

To include licenses in distribution, the output of the `processNpmLicenses` task should be used as source.

**build.gradle**
```groovy
plugins {
    id 'org.octopusden.octopus.license-management'
}

task componentDistribution(type: Zip) {
    from (processNpmLicenses) {
        into 'licenses'
    }
}
```

To include both Gradle dependencies and JS licenses in a JAR:

**build.gradle**
```groovy
plugins {
    id 'org.octopusden.octopus.license-management'
    id 'java-library'
}

processResources {
    dependsOn('processLicenses', 'processNpmLicenses')
    from(layout.buildDirectory.dir('licenses')) {
        into 'licenses'
    }
}
```

#### Parameters of `processNpmLicenses` Task

See [NPM License Checker Options](https://www.npmjs.com/package/license-checker#options). Example:

**build.gradle**
```groovy
plugins {
    id 'org.octopusden.octopus.license-management'
}

processNpmLicenses {
    start = file("$projectDir/node-app")
}
```

#### Manually Checking Used Node Packages Licenses

To process licenses (verify or include in distribution), set the project parameters `license.skip` and `node.skip` to `false`.

```shell
gradle -Plicense.skip=false -Pnode.skip=false processNpmLicenses
```