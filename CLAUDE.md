# CLAUDE.md — Bootstrap Toolkit

JetBrains IDE plugin (Kotlin) with Bootstrap 5.3 live templates for HTML, React (JSX/TSX) and Vue,
planned to grow into a component generator dialog.
Owner: James Mosquera (jamesmosq). Built on the official IntelliJ Platform Plugin Template.
Sister project (same build setup, already on the Marketplace): `../photo-placeholders`.

## Status — read first
- 50 templates + 1 page (`bs5-starter`), sources grouped like the docs: `src/templates/{pages,layout,components,content,forms}`, generated into 2 dialects (HTML, JSX).
  See README for the list.
  No plugin Kotlin code yet; the only Kotlin is the build-time generator in `buildSrc`.
- **Verified manually (2026-09-24)** in WebStorm 2026.2 with the zip installed from disk: `bs5-btn` /
  `bs5-alert` expand correctly in .html, .jsx, .tsx (`className`) and Vue `<template>` (`class`),
  and take priority over Emmet.
- `runIde` launches an unlicensed IDEA where JavaScript/CSS/React/Vue plugins do not load
  (they need `com.intellij.modules.ultimate`), so it can only test HTML. Test JSX/TSX/Vue by
  installing `build/distributions/*.zip` into WebStorm (Settings > Plugins > Install Plugin from Disk).
  Test files live in `../bootstrap-toolkit-sandbox`.

## Why this plugin exists (research, Sep 2026)
- Reference: VS Code "Bootstrap 5 Quick Snippets" (anburocky3/bootstrap5-snippets, MIT, 177 snippets).
- Existing JetBrains options are stale: "Bootstrap 5 Templates" (id 18750, 145 templates) declares
  `until-build 253.*` (not installable on 2026.x) and only enables the `HTML` and `PHP` contexts, so it
  does not work in JSX. "Bootstrap 4 ..." (id 9341) was last updated in 2019.
- Defects of the reference we must NOT reproduce: `holder.js/...` image sources (need an extra script),
  `via.placeholder.com` (offline), no JSX, no full-page templates, missing 5.3 utilities
  (hstack/vstack, ratio, focus-ring, link-*, z-*, stretched-link — unconfirmed, verify before adding).
- Our differentiators: works in JSX/TSX/Vue, alive on new IDE builds, a generator dialog (later),
  and real photos via picsum.photos (see the photo-placeholders plugin).

## Working rules
- Read the official docs (https://plugins.jetbrains.com/docs/intellij/) before touching platform/Gradle code. Do not guess APIs.
- **Never change the plugin id** `com.jamesmosquera.bootstraptoolkit` (permanent once published).
- Never push to `main`, publish, create releases or tags without the owner's explicit OK.
  A push to `main` makes CI create a draft GitHub release; work on `develop`.
- Bootstrap 5.3 only. No Bootstrap 4 syntax (`data-toggle`, `ml-`, `text-left`, `badge-*`, ...).
- **Templates are written once, in HTML, in `src/templates/**/bs5-*.html`. Never hand-edit live template
  XML** — `generateLiveTemplates` (buildSrc) produces both groups at build time: HTML/Vue as-is, and JSX/TSX
  via `JsxConverter` (`className`, `htmlFor`, camelCase attrs, self-closed void tags, `style={{}}`,
  `{/* */}`, `defaultValue`/`defaultChecked`). Same abbreviation in both, so they are separate templateSet
  groups (same abbreviation twice in one group collides). If a source can't be converted safely the build
  fails with the file name — fix the source, don't special-case the converter.
- Source format: header `<!-- description: ...  /  kind: page (optional)  /  var NAME: default  /
  options NAME: a, b, c (optional, after its var; becomes enum(...), default must be one of them)  /
  expr NAME: date("yyyy") (optional raw live template expression; default is the fallback) -->` (var order =
  Tab order), then the body. `kind: page` = whole `<!doctype html>` document: HTML context only (not Vue,
  no JSX); lives in `src/templates/pages`. Every `$VAR$` used must be declared and vice versa; `$END$` is appended if missing.
- Never name a source directory or package `build`: `.gitignore` ignores every `build` path, so the files
  silently stay out of git (this happened once with the generator package).
- Multi-line templates are not yet tested in an IDE (indentation on expansion, `toReformat`) — verify
  with the first one.
- Context ids that exist in IDEA 2025.2 (verified): `HTML`, `HTML_TEXT`, `JSX_HTML` (base JAVA_SCRIPT),
  `TSX_HTML` (base TypeScript — must be declared separately), `VUE_TEMPLATE`, `ANGULAR_TEMPLATE`.
  PHP/Blade/Twig contexts only exist in PhpStorm.
- CDN: Bootstrap 5.3.8 and Bootstrap Icons 1.13.1 from jsDelivr with `integrity` + `crossorigin` (test
  enforces one pinned version per library). Bootstrap hashes come from its `config.yml`; Icons publishes
  none, so its hash is ours. All were recomputed from the real files
  (`curl -sL URL | openssl dgst -sha384 -binary | openssl base64 -A`). Redo both when bumping the version.
- Icons: Bootstrap Icons (`<i class="bi bi-NAME" aria-hidden="true">`). Every name in `bs5-icon` options was
  checked against the 1.13.1 stylesheet. Heroicons (SVG only, no CDN font) is deferred to the generator dialog.
- Classes without a CSS rule are only allowed when verified: Bootstrap JS hooks (`slide`, checked in the vendored
  bundle) and docs-script hooks (`needs-validation`, checked in the vendored `validate-forms.js`).
- Automatic checks (all in CI): `LiveTemplatesTest` (rules), `BootstrapAssetsTest` (every class of every
  template AND every `options` value exists in the pinned Bootstrap/Icons CSS; SRI hashes recomputed from
  the vendored copies in `src/test/resources/vendor/<pkg>@<ver>/...`), `tools/tsx-check` (every JSX template
  and option compiles with TypeScript 5 + @types/react 19). A new class or CDN file needs no manual check.
- Markup source: the official docs sources at the pinned tag, e.g.
  `https://raw.githubusercontent.com/twbs/bootstrap/v5.3.8/site/src/content/docs/components/<name>.mdx`
  (same content as getbootstrap.com/docs/5.3, exact version).
- Images: use https://picsum.photos, never `holder.js` or `via.placeholder.com` (tests enforce this).
- Anything copied from anburocky3/bootstrap5-snippets (MIT, (c) 2021 Anbuselvan Annamalai) requires his
  copyright + MIT notice in `THIRD_PARTY_NOTICES.md` and a credit in the README. Create the file the
  moment the first copied snippet lands.
- Plugin name must not contain "Plugin", "IntelliJ", "JetBrains". Whether "Bootstrap" in the name passes
  Marketplace trademark review is UNCONFIRMED — check when uploading.
- All UI strings (once a UI exists) go through a resource bundle: English + Spanish, kept in sync.
- Every user-visible change gets a line under `## [Unreleased]` in CHANGELOG.md.
- Commit messages in English, conventional style (`fix:`, `feat:`, `docs:`).

## Commands
```bash
./gradlew build          # compile + tests
./gradlew check          # plugin guard tests (generated XML)
./gradlew -p buildSrc test  # generator unit tests
npm ci --prefix tools/tsx-check && npm --prefix tools/tsx-check run check   # TSX type check (after check/build)
./gradlew verifyPlugin   # JetBrains Plugin Verifier (must pass before any release)
./gradlew runIde         # sandbox IDE for manual testing
./gradlew buildPlugin    # build/distributions/*.zip
```
Signing (later): keys go OUTSIDE the repo in `~/.bootstrap-toolkit-signing/` (chain.crt, private.pem,
password.txt); CI uses CERTIFICATE_CHAIN / PRIVATE_KEY / PRIVATE_KEY_PASSWORD. Never commit or print them.
Run `signPlugin` and `verifyPluginSignature` in separate invocations.

## Layout
- `src/templates/<category>/bs5-*.html` — the single source of every template
- `buildSrc/` — generator: `TemplateSource` (parse/validate), `JsxConverter`, `LiveTemplateXml`,
  `GenerateLiveTemplates` task. Unit tests: `./gradlew -p buildSrc test` (NOT run by `check`; CI runs both)
- `build/generated/liveTemplates/liveTemplates/BootstrapToolkit{Html,Jsx}.xml` — generated, registered as
  a resource root; not in git
- `src/main/resources/META-INF/plugin.xml`, `pluginIcon*.svg`
- `src/test/kotlin/.../LiveTemplatesTest.kt` — XML guard tests (BS4 syntax, className, HTML/JSX parity, dead placeholders)

## Naming convention (owner, 2026-09-24)
One prefix per technology, so typing the prefix + Ctrl+J lists that family: `bs5-` Bootstrap (done),
`css-` CSS, and likewise for the next ones (e.g. `vue-`, `ts-`). The **context** decides where a template
appears, so the same prefix can live in HTML, JSX and CSS files. Rule of value: a template must carry
knowledge the IDE/Emmet does not already give (no generic filler).

## Work order (by technology)
Coverage audit vs. Bootstrap 5.3.8 docs (2026-09-24): components 24/24 pages (close button, placeholders,
scrollspy added), forms 9/9 applicable, layout grid/containers/stacks, content images/figures/typography,
helpers ratio/stretched-link/icon-link. Not covered on purpose: utilities (single classes, no template
value), RTL, Sass; page examples (sign-in, pricing, heroes, album, dashboard...) and color modes are 0.2.0.
Done: single source + generator, 50 templates + starter, automatic class/SRI/TSX checks, verifyPlugin
(Compatible on 252, 253, 261, 262, 263 — 2026-09-24).

1. **Bootstrap `bs5-` — release 0.1.0.** Blocked on owner decisions: plugin name/scope (see open question),
   signing keys (reuse photo-placeholders' or new), screenshots/GIF. Re-run verifyPlugin on the final zip.
2. **Bootstrap in CSS (`bs5-` in CSS contexts) — 0.2.0.** New `kind: css` in the generator (no JSX group).
   Contexts exist in 2025.2: `CSS`, `CSS_RULESET_LIST`, `CSS_DECLARATION_BLOCK`, `CSS_PROPERTY_VALUE`
   (no SCSS/Less ids; Vue `<style>` and .scss inheriting them is UNVERIFIED). Content: component recolor via
   component vars (`--bs-btn-*`; changing `--bs-primary` does NOT recolor `.btn-primary`), `[data-bs-theme=dark]`
   block, breakpoint media queries (576/768/992/1200/1400), Vue scoped `:deep(.btn)`. Plus a dark-mode option in
   `bs5-starter`. Test: every `--bs-*` var used exists in the vendored CSS (449 defined in 5.3.8).
3. **Bootstrap JS/TS (`bs5-` in JS/TS/Vue script contexts).** `import { Modal } from 'bootstrap'` +
   `getOrCreateInstance`, React `useEffect` (with `dispose()` cleanup), Vue `onMounted`. Verify context ids
   (`JAVA_SCRIPT`, `TypeScript`, `VUE_SCRIPT`) first. Bootstrap ships no TS types; `@types/bootstrap` is 5.2.11.
4. **Modern CSS (`css-`).** Only what Emmet lacks: container queries, `:has()`, fluid `clamp()` type, grid
   `auto-fit`, `prefers-color-scheme` / `prefers-reduced-motion`, logical properties.
5. **Other technologies (`vue-`, `ts-`, ...)** — only after deciding whether they belong in this plugin.
6. Generator dialog (Heroicons search/insert fits here).

Decided (owner, 2026-09-24): **option A** — this plugin stays focused on Bootstrap (steps 1-3). `css-`, `vue-`,
`ts-` families will be separate plugins, each built to the same standard (single source, generator, automatic
checks). Step 4-5 above therefore belong to future plugins, not this one.

Picsum across plugins (owner idea, 2026-09-24): do NOT copy the Photo Placeholders dialog/templates into each
plugin (duplicate menus and `picsum*` templates if both are installed, double maintenance). Instead:
(1) picsum URLs inside templates (`/seed/<name>/W/H` so each image is distinct and stable);
(2) recommend Photo Placeholders in the description — ONLY once it is approved (on 2026-09-24 the API still
says `approve: false`; a link to a non-public listing counts as a broken link in review);
(3) later, an optional plugin dependency on `com.jamesmosquera.photoplaceholders`
(https://plugins.jetbrains.com/docs/intellij/plugin-dependencies.html, `<depends optional="true" config-file=...>`)
to reuse its gallery, e.g. to change a card's photo. Needs Kotlin code; after 0.1.0.
