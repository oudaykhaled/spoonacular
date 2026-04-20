/* Katty Coverage Dashboard — renders coverage.json (produced by the Gradle task) */

(() => {
    const DATA_URL = 'coverage.json';

    // ---------- Theme ----------
    const savedTheme = localStorage.getItem('katty.theme');
    if (savedTheme === 'light' || savedTheme === 'dark') {
        document.documentElement.setAttribute('data-theme', savedTheme);
    }
    document.getElementById('theme-toggle').addEventListener('click', () => {
        const next = document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
        document.documentElement.setAttribute('data-theme', next);
        localStorage.setItem('katty.theme', next);
    });

    // ---------- Helpers ----------
    const fmtInt = (n) => n.toLocaleString('en-US');
    const fmtPct = (p) => (Number.isFinite(p) ? p.toFixed(1) : '0.0');
    const tierClass = (p) => {
        if (p >= 80) return 'tier-good';
        if (p >= 60) return 'tier-ok';
        if (p >= 40) return 'tier-warn';
        return 'tier-bad';
    };
    const pctOf = (counter) => {
        const total = counter.covered + counter.missed;
        return total === 0 ? 0 : (counter.covered * 100) / total;
    };
    const textOf = (sel, v) => {
        const el = document.querySelector(`[data-field="${sel}"]`);
        if (el) el.textContent = v;
    };
    const barOf = (sel, pct) => {
        const el = document.querySelector(`[data-field="${sel}"]`);
        if (!el) return;
        el.style.width = `${Math.min(100, Math.max(0, pct))}%`;
        el.className = 'metric-card__bar-fill ' + tierClass(pct);
    };
    const moduleFrom = (pkgName) => {
        // pkgName uses slashes: nl/ing/assessment/recipes/feature/search/viewmodel
        const parts = pkgName.split('/');
        const idx = parts.indexOf('recipes');
        if (idx === -1 || idx + 2 >= parts.length) return '(root)';
        const group = parts[idx + 1]; // feature | core | app root (e.g. after 'recipes')
        const module = parts[idx + 2];
        return `${group}:${module}`;
    };

    // ---------- Data load ----------
    async function load() {
        try {
            const res = await fetch(DATA_URL, { cache: 'no-store' });
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            return await res.json();
        } catch (e) {
            document.querySelector('#modules-tbody').innerHTML = `
                <tr><td colspan="5" class="empty-state">
                    Could not load <code>coverage.json</code>.<br/>
                    Run <code>./gradlew coverageReport</code> and open the
                    generated <code>build/reports/coverage-dashboard/index.html</code>.
                </td></tr>`;
            textOf('line.pct', '—');
            textOf('branch.pct', '—');
            textOf('method.pct', '—');
            textOf('class.pct', '—');
            console.error(e);
            return null;
        }
    }

    function renderOverall(total) {
        const counters = ['line', 'branch', 'method', 'class'];
        counters.forEach((k) => {
            const c = total[k] || { covered: 0, missed: 0 };
            const p = pctOf(c);
            const totalN = c.covered + c.missed;
            textOf(`${k}.pct`, fmtPct(p));
            textOf(`${k}.covered`, fmtInt(c.covered));
            textOf(`${k}.total`, fmtInt(totalN));
            barOf(`${k}.bar`, p);
        });
        const ts = document.getElementById('generated-at');
        if (ts) {
            const d = new Date(total.generatedAt || Date.now());
            ts.textContent = `Generated ${d.toLocaleString()}`;
        }
    }

    function coverageCell(counter) {
        const pct = pctOf(counter);
        const total = counter.covered + counter.missed;
        if (total === 0) {
            return `<span class="coverage-cell"><span class="coverage-cell__pct">—</span></span>`;
        }
        return `
            <span class="coverage-cell">
                <span class="coverage-cell__bar">
                    <span class="coverage-cell__fill ${tierClass(pct)}" style="width:${pct.toFixed(1)}%"></span>
                </span>
                <span class="coverage-cell__pct">${fmtPct(pct)}%</span>
            </span>`;
    }

    function aggregateModules(packages) {
        const map = new Map();
        for (const pkg of packages) {
            const moduleName = moduleFrom(pkg.name);
            const entry = map.get(moduleName) || {
                name: moduleName,
                packageCount: 0,
                classCount: 0,
                line: { covered: 0, missed: 0 },
                branch: { covered: 0, missed: 0 },
                method: { covered: 0, missed: 0 },
                class: { covered: 0, missed: 0 },
                instruction: { covered: 0, missed: 0 },
            };
            entry.packageCount += 1;
            entry.classCount += pkg.classes.length;
            for (const k of ['line', 'branch', 'method', 'class', 'instruction']) {
                entry[k].covered += (pkg[k] || {}).covered || 0;
                entry[k].missed += (pkg[k] || {}).missed || 0;
            }
            map.set(moduleName, entry);
        }
        return Array.from(map.values()).sort((a, b) => a.name.localeCompare(b.name));
    }

    let currentSort = 'line';
    let modulesCache = [];

    function renderModules() {
        const tbody = document.getElementById('modules-tbody');
        const filter = document.getElementById('module-search').value.trim().toLowerCase();
        const filtered = modulesCache
            .filter((m) => !filter || m.name.toLowerCase().includes(filter))
            .slice()
            .sort((a, b) => pctOf(b[currentSort]) - pctOf(a[currentSort]));

        if (filtered.length === 0) {
            tbody.innerHTML = `<tr><td colspan="5" class="empty-state">No modules match that filter.</td></tr>`;
            return;
        }

        tbody.innerHTML = filtered
            .map(
                (m) => `
                <tr>
                    <td>
                        <div class="module-row__name">
                            <span class="module-row__title">${m.name}</span>
                            <span class="module-row__sub">
                                ${m.packageCount} package${m.packageCount !== 1 ? 's' : ''} ·
                                ${m.classCount} class${m.classCount !== 1 ? 'es' : ''}
                            </span>
                        </div>
                    </td>
                    <td class="num">${coverageCell(m.line)}</td>
                    <td class="num">${coverageCell(m.branch)}</td>
                    <td class="num">${coverageCell(m.method)}</td>
                    <td class="num">${coverageCell(m.class)}</td>
                </tr>`,
            )
            .join('');
    }

    function renderPackages(packages) {
        const tbody = document.getElementById('packages-tbody');
        const sorted = packages
            .slice()
            .sort((a, b) => pctOf(b.line) - pctOf(a.line));
        document.getElementById('packages-count').textContent =
            `${packages.length} package${packages.length !== 1 ? 's' : ''}`;

        tbody.innerHTML = sorted
            .map(
                (p) => `
                <tr>
                    <td>
                        <div class="module-row__name">
                            <span class="module-row__title">${p.name.replace(/\//g, '.')}</span>
                            <span class="module-row__sub">${moduleFrom(p.name)}</span>
                        </div>
                    </td>
                    <td class="num">${coverageCell(p.line)}</td>
                    <td class="num">${coverageCell(p.branch)}</td>
                    <td class="num">${coverageCell(p.method)}</td>
                    <td class="num">${p.classes.length}</td>
                </tr>`,
            )
            .join('');
    }

    function renderTopLists(packages) {
        const allClasses = [];
        for (const p of packages) {
            for (const c of p.classes) {
                allClasses.push({
                    ...c,
                    pkgName: p.name,
                    moduleName: moduleFrom(p.name),
                });
            }
        }
        const withLines = allClasses.filter((c) => (c.line.covered + c.line.missed) >= 5);
        const best = withLines
            .slice()
            .sort((a, b) => pctOf(b.line) - pctOf(a.line))
            .slice(0, 10);
        const worst = withLines
            .slice()
            .sort((a, b) => pctOf(a.line) - pctOf(b.line))
            .slice(0, 10);

        const rank = (list) =>
            list
                .map(
                    (c, i) => `
                <li>
                    <span class="ranking__rank">${i + 1}</span>
                    <span class="ranking__name">
                        <strong>${c.name}</strong>
                        <span>${c.moduleName}</span>
                    </span>
                    <span class="ranking__pct">${fmtPct(pctOf(c.line))}%</span>
                </li>`,
                )
                .join('') || `<li class="empty-state">No classes yet.</li>`;

        document.getElementById('top-covered').innerHTML = rank(best);
        document.getElementById('bottom-covered').innerHTML = rank(worst);
    }

    // ---------- Boot ----------
    (async () => {
        const data = await load();
        if (!data) return;
        renderOverall(data.total);
        modulesCache = aggregateModules(data.packages);
        renderModules();
        renderPackages(data.packages);
        renderTopLists(data.packages);

        document
            .getElementById('module-search')
            .addEventListener('input', renderModules);

        document.querySelectorAll('.segmented__btn').forEach((btn) => {
            btn.addEventListener('click', () => {
                document
                    .querySelectorAll('.segmented__btn')
                    .forEach((b) => b.classList.remove('is-active'));
                btn.classList.add('is-active');
                currentSort = btn.dataset.sort;
                renderModules();
            });
        });
    })();
})();
