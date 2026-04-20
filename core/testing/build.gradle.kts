plugins {
    id("recipes.android.library")
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "nl.ing.assessment.recipes.core.testing"
}

tasks.withType<Test>().configureEach {
    failOnNoDiscoveredTests = false
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:telemetry"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    implementation(libs.kotlinx.coroutines.core)

    api(libs.junit)
    api(libs.mockk)
    api(libs.turbine)
    api(libs.kotlinx.coroutines.test)
}
