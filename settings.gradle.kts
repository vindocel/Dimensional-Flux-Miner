pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.neoforged.net/releases/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.kikugie.stonecutter") version "0.9.8"
}

rootProject.name = "dimensionalfluxminer"

// Every Minecraft version lives on this branch. Each one is the Gradle project :<minecraft> (versions/<minecraft>);
// the active one (stonecutter.gradle.kts) is the version the sources are written for in Git.
stonecutter {
    create(rootProject) {
        versions("1.21.1", "26.1.2", "26.3")
        vcsVersion = "26.1.2"
    }
}
