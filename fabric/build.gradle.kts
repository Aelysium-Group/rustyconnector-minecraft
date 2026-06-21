import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    id("rustyconnector.loom-platform")
}

// JiJ is not transitive: every runtime artifact is include()'d explicitly, and
// anything the source compiles against is also on implementation.
dependencies {
    "minecraft"("com.mojang:minecraft:${libs.versions.minecraft.get()}")
    "mappings"("net.fabricmc:yarn:${libs.versions.yarn.get()}:v2")
    "modImplementation"(libs.fabric.loader)
    "modImplementation"(libs.fabric.api)

    "include"(libs.core)

    // :common/:serverCommon carry a stub fabric.mod.json so Loom's JiJ loader finds them.
    "implementation"(project(":common"))
    "include"(project(":common"))
    "implementation"(project(":serverCommon"))
    "include"(project(":serverCommon"))

    "modImplementation"(libs.cloud.fabric)
    "include"(libs.cloud.fabric)
    "implementation"(libs.cloud.annotations)
    "include"(libs.cloud.annotations)

    "implementation"(libs.adventure.api)
    "include"(libs.adventure.api)
    "implementation"(libs.adventure.key)
    "include"(libs.adventure.key)
    "implementation"(libs.adventure.serializer.legacy)
    "include"(libs.adventure.serializer.legacy)
    "implementation"(libs.examination.api)
    "include"(libs.examination.api)

    "implementation"(libs.configurate.core)
    "include"(libs.configurate.core)
    "implementation"(libs.configurate.yaml)
    "include"(libs.configurate.yaml)
    "implementation"(libs.snakeyaml)
    "include"(libs.snakeyaml)

    "implementation"(libs.jnanoid)
    "include"(libs.jnanoid)
    "implementation"(libs.declarative.yaml)
    "include"(libs.declarative.yaml)
}

tasks.named<ProcessResources>("processResources") {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("fabric.mod.json") { expand(props) }
}
