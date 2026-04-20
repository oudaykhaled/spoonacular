import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Convention for the `:app` module: Android application + JaCoCo unit-test coverage on debug,
 * aligned flavors and Java/Kotlin targets with [RecipesAndroidLibraryPlugin].
 */
class RecipesAndroidApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("jacoco")

            extensions.configure<ApplicationExtension> {
                compileSdk = 36
                defaultConfig {
                    minSdk = 26
                    testInstrumentationRunner = "nl.ing.assessment.recipes.HiltTestRunner"
                    vectorDrawables.useSupportLibrary = true
                }
                flavorDimensions += "environment"
                productFlavors {
                    create("dev") { dimension = "environment" }
                    create("prod") { dimension = "environment" }
                }
                compileOptions {
                    sourceCompatibility = org.gradle.api.JavaVersion.VERSION_11
                    targetCompatibility = org.gradle.api.JavaVersion.VERSION_11
                }
                buildTypes {
                    getByName("debug") {
                        enableUnitTestCoverage = true
                    }
                }
                testOptions {
                    unitTests {
                        isReturnDefaultValues = true
                        isIncludeAndroidResources = true
                    }
                }
                packaging {
                    resources {
                        excludes += "/META-INF/{AL2.0,LGPL2.1}"
                        excludes += "/META-INF/LICENSE.md"
                        excludes += "/META-INF/LICENSE-notice.md"
                    }
                }
                buildFeatures {
                    compose = true
                }
            }

            extensions.configure<KotlinAndroidProjectExtension> {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
            }

            val hasAndroidTestSources = project.file("src/androidTest").let { dir ->
                dir.isDirectory && dir.walkTopDown().any { it.extension == "kt" || it.extension == "java" }
            }
            if (hasAndroidTestSources) {
                val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
                dependencies {
                    add("androidTestImplementation", libs.findLibrary("androidx.junit").get())
                }
            }
        }
    }
}
