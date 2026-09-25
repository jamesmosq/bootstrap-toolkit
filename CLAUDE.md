# CLAUDE.md — Bootstrap Toolkit

JetBrains IDE plugin (Kotlin) with Bootstrap 5.3 live templates for HTML, React (JSX/TSX) and Vue,
planned to grow into a component generator dialog.
Owner: James Mosquera (jamesmosq). Built on the official IntelliJ Platform Plugin Template.
Sister project (same build setup, already on the Marketplace): `../photo-placeholders`.

## Status — read first
- Early prototype: 6 components (`bs5-btn`, `-alert`, `-card`, `-navbar`, `-modal`, `-form`), generated into
  2 dialects (HTML, JSX).
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
- Source format: header `<!-- description: ...  /  var NAME: default -->` (var order = Tab order), then the
  body. Every `$VAR$` used must be declared and vice versa; `$END$` is appended if missing.
- Never name a source directory or package `build`: `.gitignore` ignores every `build` path, so the files
  silently stay out of git (this happened once with the generator package).
- Multi-line templates are not yet tested in an IDE (indentation on expansion, `toReformat`) — verify
  with the first one.
- Context ids that exist in IDEA 2025.2 (verified): `HTML`, `HTML_TEXT`, `JSX_HTML` (base JAVA_SCRIPT),
  `TSX_HTML` (base TypeScript — must be declared separately), `VUE_TEMPLATE`, `ANGULAR_TEMPLATE`.
  PHP/Blade/Twig contexts only exist in PhpStorm.
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

## Next steps
1. ~~Confirm templates expand in .html, .jsx, .tsx and Vue~~ — done 2026-09-24 (WebStorm 2026.2).
2. ~~Single data source for templates~~ — done: `src/templates` + buildSrc generator.
3. Automate the TSX type check: it was done by hand once (typescript 5 + @types/react 19,
   `tsc --noEmit --strict --jsx react-jsx` over every JSX template with defaults filled in) and caught
   `tabIndex="-1"` (must be `{-1}`). Make it a Gradle/CI step so every new template is compiled.
4. Grow coverage (components, 5.3 utilities, full-page starters), then the generator dialog.
