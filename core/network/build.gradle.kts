import java.util.Properties

plugins {
    id("recipes.android.library")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val spoonacularKey: String = (
    localProps.getProperty("SPOONACULAR_API_KEY")
        ?: System.getenv("SPOONACULAR_API_KEY")
        ?: ""
    )

android {
    namespace = "nl.ing.assessment.recipes.core.network"

    defaultConfig {
        buildConfigField(
            "String",
            "VERSION_NAME",
            "\"${rootProject.findProperty("appVersionName") ?: "1.0.0"}\""
        )
        buildConfigField("String", "SPOONACULAR_API_KEY", "\"$spoonacularKey\"")
    }

    productFlavors {
        getByName("dev") {
            buildConfigField("String", "BASE_URL", "\"https://api.spoonacular.com/\"")
        }
        getByName("prod") {
            buildConfigField("String", "BASE_URL", "\"https://api.spoonacular.com/\"")
        }
    }

    buildTypes {
        debug {
            buildConfigField("Boolean", "DEBUG_INTERCEPTORS", "true")
        }
        release {
            buildConfigField("Boolean", "DEBUG_INTERCEPTORS", "false")
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:domain"))

    api(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    api(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
}
