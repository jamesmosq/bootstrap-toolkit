# Contributing

Thanks for helping improve Bootstrap Toolkit! Contributions in English or Spanish are welcome.

## Getting started

1. Fork and clone the repository, then open it in IntelliJ IDEA (JDK 21).
2. Run the **Run Plugin** configuration (or `./gradlew runIde`) to try your changes in a sandbox IDE.
3. Before opening a pull request, run:

   ```bash
   ./gradlew check verifyPlugin
   ```

## Guidelines

- Bootstrap 5.3 only. JSX/TSX templates must use `className` (never `class`); every HTML template needs a JSX counterpart.
- Keep UI strings in a resource bundle (English) with a Spanish translation once a UI exists.
- Add or update tests in `src/test/kotlin` when you touch the live templates.
- Add a line under `## [Unreleased]` in `CHANGELOG.md` describing your change.

## Reporting bugs

Open an issue with your IDE version (<kbd>Help</kbd> > <kbd>About</kbd>), the plugin version,
steps to reproduce and, if possible, the relevant part of `idea.log`.
