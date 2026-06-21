plugins {
    `kotlin-dsl`
}

dependencies {
    // api so the platform build scripts can reference the ShadowJar type.
    api(libs.shadow.plugin)
    implementation(libs.loom.plugin)
}
