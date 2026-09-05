pluginManagement {
    repositories {
        google()
        maven("https://maven.google.com")
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven("https://maven.google.com")
        mavenCentral()
    }
}

rootProject.name = "log.life"
include(":app")
 