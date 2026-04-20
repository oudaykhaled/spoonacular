# Coverage Dashboard

A small, self-contained web project that renders Katty's JaCoCo coverage data as an
interactive dashboard — instead of the default (visually plain) JaCoCo HTML report.

## What gets rendered

- Overall coverage for **line**, **branch**, **method** and **class** metrics
- Per-module breakdown (`:core:*`, `:feature:*`) with sortable columns
- Top 10 best/worst covered classes (minimum 5 lines, so trivial classes don't skew the list)
- Full package drill-down
- Dark / light theme toggle (persisted in `localStorage`)

## How it works

```
./gradlew coverageReport
    │
    ├── 1. runs all unit tests (testDevDebugUnitTest on every module)
    ├── 2. optionally runs instrumented tests (-PwithInstrumentation=true)
    ├── 3. jacocoCombinedReport → build/reports/jacoco/combined/jacoco.xml
    └── 4. generateCoverageDashboard
           ├── parses jacoco.xml → coverage.json
           └── copies coverage-dashboard/{index.html,assets/*} into
               build/reports/coverage-dashboard/
```

Open `build/reports/coverage-dashboard/index.html` in any modern browser.

## Files

| File | Purpose |
|------|---------|
| `index.html` | Dashboard markup and section structure |
| `assets/styles.css` | Theming tokens (dark/light), layout, card + table styles |
| `assets/app.js` | Fetches `coverage.json`, renders tables, sorts, handles theme toggle |

## Customising

- **Colour scheme**: edit the CSS variables at the top of `styles.css` (`--color-brand`,
  `--color-success`, etc.).
- **Tier thresholds** (green/blue/orange/red): edit the `tierClass` function in `app.js`.
- **Adding a new metric column**: extend the parser in
  `build-logic/convention/src/main/kotlin/CoverageDashboardTask.kt`, then add a `<th>` /
  `<td>` pair in `index.html` and wire it in `app.js::renderModules`.

## Running without Gradle

The dashboard is plain static HTML. After any `./gradlew coverageReport` run you can
`cp -r build/reports/coverage-dashboard /tmp/katty-coverage && open
/tmp/katty-coverage/index.html`, or upload the folder to a static host.

## Schema of `coverage.json`

```jsonc
{
  "total": {
    "generatedAt": 1713620000000,        // epoch millis
    "line":        { "covered": 523, "missed": 412 },
    "branch":      { "covered":  64, "missed":  72 },
    "method":      { "covered": 112, "missed":  45 },
    "class":       { "covered":  23, "missed":   4 },
    "instruction": { "covered":2100, "missed":1800 }
  },
  "packages": [
    {
      "name": "nl/ing/assessment/recipes/feature/search/viewmodel",
      "line":   { "covered": 120, "missed":  42 },
      "branch": { "covered":  15, "missed":  23 },
      "method": { "covered":  18, "missed":   3 },
      "class":  { "covered":   4, "missed":   0 },
      "instruction": { "covered": 420, "missed": 120 },
      "classes": [
        { "name": "SearchViewModel",
          "line":   { "covered": 50, "missed": 5 },
          "branch": { "covered":  9, "missed": 3 },
          "method": { "covered":  7, "missed": 1 },
          "class":  { "covered":  1, "missed": 0 } }
      ]
    }
  ]
}
```
