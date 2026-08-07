import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    id("rustyconnector.shadow-platform")
}

dependencies {
    compileOnly(libs.paper.api)
    implementation(libs.cloud.paper)
    implementation(libs.cloud.annotations)
    implementation(project(":serverCommon"))
    implementation(project(":common"))
}

tasks.named<ShadowJar>("shadowJar") {
    relocate("org.incendo", "group.aelysium.rustyconnector.paper.org.incendo")
}

tasks.named<ProcessResources>("processResources") {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") { expand(props) }
}
