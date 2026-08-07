import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

// Velocity + Paper packaging. Not applied to :fabric (Shadow and Loom conflict).
plugins {
    id("rustyconnector.java-conventions")
    id("com.gradleup.shadow")
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("rustyconnector-${project.name}")
    archiveClassifier.set("")
    mergeServiceFiles()

    // Velocity and Paper provide Adventure + SLF4J natively; a bundled copy is dead
    // weight (the platform's classes win under parent-first loading).
    exclude("net/kyori/**")
    exclude("org/slf4j/**")
    // Loom JiJ stubs that :common / :serverCommon carry for Fabric mean nothing here.
    exclude("fabric.mod.json")
}

// Shadow already wires shadowJar into `assemble`; the plain jar would only emit
// an unusable thin artifact next to the fat one.
tasks.named<Jar>("jar") {
    enabled = false
}
