# Contributing

Thanks for helping improve Bootstrap Toolkit! Contributions in English or Spanish are welcome.

## Getting started

1. Fork and clone the repository, then open it in IntelliJ IDEA (JDK 21).
2. Run the **Run Plugin** configuration (or `./gradlew runIde`) to try your changes in a sandbox IDE.
3. Before opening a pull request, run:

   ```bash
   ./gradlew check verifyPlugin
   ./gradlew -p buildSrc test
   ```

## Guidelines

- Bootstrap 5.3 only. Add or edit templates only in `src/templates` (plain HTML with a small header, see the
  existing files). The JSX/TSX version is generated from it; never edit live template XML by hand.
- Keep UI strings in a resource bundle (English) with a Spanish translation once a UI exists.
- Add or update tests in `src/test/kotlin` (generated templates) or `buildSrc/src/test` (generator) when you change them.
- Add a line under `## [Unreleased]` in `CHANGELOG.md` describing your change.

## Reporting bugs

Open an issue with your IDE version (<kbd>Help</kbd> > <kbd>About</kbd>), the plugin version,
steps to reproduce and, if possible, the relevant part of `idea.log`.
