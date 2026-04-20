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

// =============================================================================
// Coverage Dashboard — custom-rendered web dashboard for the merged JaCoCo XML
// =============================================================================
//
// Pipeline:
//   ./gradlew coverageReport
//     → runs all unit tests (via jacocoCombinedReport deps)
//     → optionally runs instrumented tests (-PwithInstrumentation=true)
//     → jacocoCombinedReport emits build/reports/jacoco/combined/jacoco.xml
//     → generateCoverageDashboard parses it into coverage.json and copies the
//       static web project (coverage-dashboard/) into build/reports/coverage-dashboard/
//
// The web project at /coverage-dashboard/ is a plain HTML/CSS/JS app — see its README
// for the data schema and customisation notes.

tasks.register("generateCoverageDashboard") {
    group = "verification"
    description = "Renders coverage-dashboard/ as a static site populated with merged JaCoCo data"
    dependsOn("jacocoCombinedReport")

    val xmlFile = layout.buildDirectory.file("reports/jacoco/combined/jacoco.xml").map { it.asFile }
    val outputDir = layout.buildDirectory.dir("reports/coverage-dashboard").map { it.asFile }
    val templateDir = rootProject.file("coverage-dashboard")

    inputs.file(xmlFile)
    inputs.dir(templateDir)
    outputs.dir(outputDir)

    doLast {
        val xml = xmlFile.get()
        val out = outputDir.get()
        if (!xml.exists()) {
            throw org.gradle.api.GradleException(
                "Combined JaCoCo XML not found at ${xml.path}. Run ./gradlew jacocoCombinedReport first."
            )
        }
        out.mkdirs()
        copy {
            from(templateDir)
            into(out)
            exclude("README.md")
        }
        val json = renderCoverageJson(xml)
        java.io.File(out, "coverage.json").writeText(json)

        // Inline the JSON into index.html as window.__COVERAGE__ so the dashboard
        // works when opened via file:// (browsers block fetch() for local files).
        val indexFile = java.io.File(out, "index.html")
        val indexHtml = indexFile.readText()
        val inlineScript = buildString {
            append("<script id=\"coverage-data\">window.__COVERAGE__ = ")
            append(json.trimEnd())
            append(";</script>\n    <script src=\"assets/app.js\"></script>")
        }
        val patched = indexHtml.replace(
            "<script src=\"assets/app.js\"></script>",
            inlineScript,
        )
        indexFile.writeText(patched)

        logger.lifecycle("Coverage dashboard ready (double-click to open):")
        logger.lifecycle("  file://${indexFile.absolutePath}")
    }
}

tasks.register("coverageReport") {
    group = "verification"
    description =
        "One-shot coverage pipeline: runs all tests, merges JaCoCo, renders the HTML dashboard"
    dependsOn("jacocoCombinedReport")
    dependsOn("generateCoverageDashboard")

    if (providers.gradleProperty("withInstrumentation").orNull == "true") {
        dependsOn("allInstrumentedTests")
    } else {
        logger.lifecycle(
            "coverageReport: instrumentation tests skipped " +
                "(use -PwithInstrumentation=true to include them)"
        )
    }
}

fun renderCoverageJson(xmlFile: java.io.File): String {
    // Minimal hand-rolled parser for the JaCoCo XML dialect. We use StAX via
    // DocumentBuilder because it's Gradle-classpath-safe and avoids depending on
    // third-party XML libs. Only the shapes we need are extracted.
    val dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance().apply {
        // Disable DTD resolution — the JaCoCo report references an external DTD we don't ship
        setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
        isValidating = false
        isNamespaceAware = false
    }
    val builder = dbf.newDocumentBuilder()
    builder.setEntityResolver { _, _ -> org.xml.sax.InputSource(java.io.StringReader("")) }
    val doc = builder.parse(xmlFile)
    val root = doc.documentElement // <report>

    data class Counter(var covered: Long = 0, var missed: Long = 0)
    fun counterFromChild(el: org.w3c.dom.Element, type: String): Counter {
        val children = el.childNodes
        for (i in 0 until children.length) {
            val n = children.item(i)
            if (n.nodeType == org.w3c.dom.Node.ELEMENT_NODE) {
                val e = n as org.w3c.dom.Element
                if (e.tagName == "counter" && e.getAttribute("type") == type) {
                    return Counter(
                        covered = e.getAttribute("covered").toLongOrNull() ?: 0,
                        missed = e.getAttribute("missed").toLongOrNull() ?: 0,
                    )
                }
            }
        }
        return Counter()
    }

    val counterTypes = listOf("LINE", "BRANCH", "METHOD", "CLASS", "INSTRUCTION", "COMPLEXITY")
    val jsonKeys = mapOf(
        "LINE" to "line",
        "BRANCH" to "branch",
        "METHOD" to "method",
        "CLASS" to "class",
        "INSTRUCTION" to "instruction",
        "COMPLEXITY" to "complexity",
    )

    val sb = StringBuilder()
    sb.append("{\n")

    // Total counters — defined as direct children of <report>
    val totalCounters = counterTypes.associateWith { counterFromChild(root, it) }
    sb.append("  \"total\": {\n")
    sb.append("    \"generatedAt\": ${System.currentTimeMillis()},\n")
    val totalParts = counterTypes.map { type ->
        val c = totalCounters.getValue(type)
        val key = jsonKeys.getValue(type)
        "    \"$key\": { \"covered\": ${c.covered}, \"missed\": ${c.missed} }"
    }
    sb.append(totalParts.joinToString(",\n"))
    sb.append("\n  },\n")

    // Per-package, per-class
    sb.append("  \"packages\": [\n")
    val packages = root.getElementsByTagName("package")
    val pkgJson = mutableListOf<String>()
    for (i in 0 until packages.length) {
        val pkg = packages.item(i) as org.w3c.dom.Element
        // Skip nested <package> elements (there shouldn't be any, but guard anyway).
        if (pkg.parentNode !== root) continue

        val pkgCounters = counterTypes.associateWith { counterFromChild(pkg, it) }
        val classChildren = pkg.childNodes
        val classJson = mutableListOf<String>()
        for (j in 0 until classChildren.length) {
            val node = classChildren.item(j)
            if (node.nodeType != org.w3c.dom.Node.ELEMENT_NODE) continue
            val cls = node as org.w3c.dom.Element
            if (cls.tagName != "class") continue

            val classCounters = counterTypes.associateWith { counterFromChild(cls, it) }
            // Class name is the basename of the class attribute (drop the package prefix)
            val fqn = cls.getAttribute("name").replace('/', '.')
            val simpleName = fqn.substringAfterLast('.')
            val classParts = listOf("LINE", "BRANCH", "METHOD", "CLASS", "INSTRUCTION").map { type ->
                val c = classCounters.getValue(type)
                val key = jsonKeys.getValue(type)
                "\"$key\": { \"covered\": ${c.covered}, \"missed\": ${c.missed} }"
            }
            classJson.add(
                "        { \"name\": \"${simpleName.jsEscape()}\", ${classParts.joinToString(", ")} }"
            )
        }

        val pkgMetricParts = listOf("LINE", "BRANCH", "METHOD", "CLASS", "INSTRUCTION").map { type ->
            val c = pkgCounters.getValue(type)
            val key = jsonKeys.getValue(type)
            "      \"$key\": { \"covered\": ${c.covered}, \"missed\": ${c.missed} }"
        }
        val block = buildString {
            append("    {\n")
            append("      \"name\": \"${pkg.getAttribute("name").jsEscape()}\",\n")
            append(pkgMetricParts.joinToString(",\n"))
            append(",\n      \"classes\": [\n")
            append(classJson.joinToString(",\n"))
            append("\n      ]\n")
            append("    }")
        }
        pkgJson.add(block)
    }
    sb.append(pkgJson.joinToString(",\n"))
    sb.append("\n  ]\n")
    sb.append("}\n")
    return sb.toString()
}

fun String.jsEscape(): String = this
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
    .replace("\n", "\\n")
    .replace("\r", "\\r")
