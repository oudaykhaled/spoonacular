plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "recipes.android.library"
            implementationClass = "RecipesAndroidLibraryPlugin"
        }
        register("androidFeature") {
            id = "recipes.android.feature"
            implementationClass = "RecipesAndroidFeaturePlugin"
        }
        register("androidApplication") {
            id = "recipes.android.application"
            implementationClass = "RecipesAndroidApplicationPlugin"
        }
    }
}
