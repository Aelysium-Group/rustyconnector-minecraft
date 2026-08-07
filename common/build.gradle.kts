plugins {
    id("rustyconnector.java-conventions")
}

dependencies {
    compileOnly(libs.annotations)
    api(libs.adventure.api) // exposed in :common's public API
    implementation(libs.cloud.annotations)
}
