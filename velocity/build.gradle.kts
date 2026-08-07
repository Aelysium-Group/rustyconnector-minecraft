import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    id("rustyconnector.shadow-platform")
}

dependencies {
    implementation(libs.bstats.velocity)
    compileOnly(libs.velocity.api)
    implementation(libs.cloud.velocity)
    implementation(libs.cloud.annotations)
    implementation(project(":common"))
}

tasks.named<ShadowJar>("shadowJar") {
    val prefix = "group.aelysium.rustyconnector.velocity"
    relocate("org.bstats", "$prefix.org.bstats")
    relocate("org.incendo", "$prefix.org.incendo")
}

tasks.named<ProcessResources>("processResources") {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("velocity-plugin.json") { expand(props) }
}
