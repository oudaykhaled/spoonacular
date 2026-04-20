plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.androidx.room) apply false
    alias(libs.plugins.detekt) apply false
    jacoco
}

jacoco {
    toolVersion = "0.8.12"
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
        parallel = true
    }
}

tasks.register("allInstrumentedTests") {
    group = "verification"
    description = "Run instrumented tests only on modules that have androidTest sources"
    dependsOn(
        subprojects.filter { sub ->
            sub.file("src/androidTest").let { dir ->
                dir.exists() && dir.walkTopDown().any { it.extension == "kt" || it.extension == "java" }
            }
        }.map { "${it.path}:connectedDevDebugAndroidTest" }
    )
}

val coverageExcludedModules = setOf(":benchmark", ":core:testing")

tasks.register<JacocoReport>("jacocoCombinedReport") {
    group = "verification"
    description = "Aggregated code-coverage report across all modules"

    subprojects.filter { it.path !in coverageExcludedModules }.forEach { sub ->
        dependsOn(sub.tasks.matching { it.name == "testDevDebugUnitTest" })
    }

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/combined/html"))
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/combined/jacoco.xml"))
    }

    val fileFilter = listOf(
        "**/R.class", "**/R\$*.class", "**/BuildConfig.*", "**/Manifest*.*",
        "**/*_Hilt*.class", "**/Hilt_*.class", "**/*_Factory.class",
        "**/*_MembersInjector.class", "**/*Module.class", "**/*Module\$*.class",
        "**/*Injector*.class", "**/*Component*.class", "**/dagger/**",
        "**/hilt_aggregated_deps/**", "**/*_GeneratedInjector.class",
        "**/*\$\$serializer.class", "**/ComposableSingletons*.class",
        "**/*Factory\$InstanceHolder.class", "**/*\$Companion.class",
        "**/*\$*Preview*.class", "**/composepreviews/**",
        "**/*\$lambda\$*.class", "**/*Kt.class", "**/*Kt\$*.class",
        "**/*_Impl.class", "**/*_Impl\$*.class", "**/*Database_Impl*.class",
        "**/*\$DefaultImpls.class", "**/*\$\$inlined\$*.class",
        "**/IngRecipesApplication.class", "**/MainActivity.class",
        "**/*Activity.class", "**/RecipesDatabase.class",
        "**/*Route*.class", "**/BottomTab.class", "**/BottomTab\$*.class"
    )

    val modules = subprojects.filter { it.path !in coverageExcludedModules }

    classDirectories.setFrom(
        modules.map { sub ->
            fileTree(sub.layout.buildDirectory) {
                include(
                    "intermediates/built_in_kotlinc/devDebug/compileDevDebugKotlin/classes/**",
                    "tmp/kotlin-classes/devDebug/**"
                )
                exclude(fileFilter)
            }
        }
    )

    sourceDirectories.setFrom(
        modules.flatMap { sub ->
            listOf(
                "${sub.projectDir}/src/main/java",
                "${sub.projectDir}/src/main/kotlin"
            )
        }
    )

    executionData.setFrom(
        modules.map { sub ->
            fileTree(sub.layout.buildDirectory) {
                include(
                    "outputs/unit_test_code_coverage/devDebugUnitTest/**/*.exec",
                    "outputs/managed_device_code_coverage/devDebug/**/*.ec",
                    "outputs/code_coverage/devDebugAndroidTest/**/*.ec"
                )
            }
        }
    )
}

tasks.register("jacocoCoverageCheck") {
    group = "verification"
    description = "Fails the build if combined line coverage falls below the minimum threshold"
    dependsOn("jacocoCombinedReport")
    doLast {
        val xmlReport = layout.buildDirectory.file("reports/jacoco/combined/jacoco.xml").get().asFile
        if (!xmlReport.exists()) {
            logger.warn("JaCoCo XML report not found, skipping coverage check")
            return@doLast
        }
        val xml = xmlReport.readText()
        // Extract LINE coverage: <counter type="LINE" missed="X" covered="Y"/>
        val regex = Regex("""<counter type="LINE" missed="(\d+)" covered="(\d+)"/>""")
        val match = regex.findAll(xml).lastOrNull()
        if (match != null) {
            val missed = match.groupValues[1].toLong()
            val covered = match.groupValues[2].toLong()
            val total = missed + covered
            val pct = if (total > 0) covered * 100.0 / total else 0.0
            logger.lifecycle("Combined line coverage: %.1f%% ($covered/$total lines)".format(pct))
            val minimum = 50.0
            if (pct < minimum) {
                throw org.gradle.api.GradleException(
                    "Line coverage %.1f%% is below the minimum %.1f%%".format(pct, minimum)
                )
            }
        }
    }
}
