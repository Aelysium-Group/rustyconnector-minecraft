plugins {
    id("rustyconnector.java-conventions")
}

dependencies {
    api(project(":common")) // visible to consumers + Loom JiJ
    implementation(libs.cloud.annotations)
}
