import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    `java-library`
}

// libs.* accessors aren't available in precompiled plugins; reach the catalog directly.
val libs = the<VersionCatalogsExtension>().named("libs")

group = providers.gradleProperty("maven_group").get()
version = providers.gradleProperty("plugin_version").get()

// Declared per-project rather than in settings dependencyResolutionManagement:
// Loom registers a project-level repo for its generated artifacts, which
// PREFER_SETTINGS would ignore.
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.fabricmc.net/")
    maven("https://hub.spigotmc.org/nexus/content/groups/public/")
    maven("https://repo.eclipse.org/content/groups/releases/")
    maven("https://maven.mrnavastar.me/releases")
    maven("https://maven.mrnavastar.me/snapshots")
    mavenLocal { content { includeGroup("group.aelysium.rustyconnector") } }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

dependencies {
    add("implementation", libs.findLibrary("core").get())
}
