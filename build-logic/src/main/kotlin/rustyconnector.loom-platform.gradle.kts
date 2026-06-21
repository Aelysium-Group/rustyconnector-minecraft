// Fabric packaging: Loom remap + Jar-in-Jar. No Shadow.
plugins {
    id("rustyconnector.java-conventions")
    id("fabric-loom")
}

base {
    archivesName.set("rustyconnector-${project.name}")
}
