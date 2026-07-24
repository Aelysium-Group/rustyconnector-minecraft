pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.fabricmc.net/")
    }
}

rootProject.name = "rustyconnector-minecraft"

include(":common")
include(":serverCommon")
include(":paper")
include(":fabric")
include(":velocity")
