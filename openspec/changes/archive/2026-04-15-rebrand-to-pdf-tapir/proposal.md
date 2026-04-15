## Why

The project was renamed from "PDF Escroto" to "PDF Tapir" and the git remote and local folder have already been updated. The remaining codebase still contains the old name in Java package names, class names, configuration files, documentation, and CI/CD workflows, which creates confusion and inconsistency.

## What Changes

- Rename Java package `com.pdfescroto` → `com.pdftapir` across all source files
- Update `pom.xml`: `groupId`, `artifactId`, application name, and main class references
- Update GitHub Actions workflow files referencing the old name
- Update `README.md` and all documentation files
- Update `AGENTS.md` and any other project-level config files
- Update OpenSpec artifacts (specs, change archives) that reference the old name
- Update `.claude/settings.local.json` references

## Capabilities

### New Capabilities
<!-- None - this is a pure rename/rebrand with no new feature capabilities -->

### Modified Capabilities

- `native-installers`: App name and bundle identifiers reference old name — needs update
- `release-pipeline`: Workflow references old artifact/app name — needs update

## Impact

- All Java source files under `src/` (package declarations, imports, class paths)
- `pom.xml` (groupId, artifactId, mainClass, app name)
- `.github/workflows/release.yml`
- `README.md`, `AGENTS.md`, `docs/` files
- OpenSpec spec files and change archive documents
- `.claude/settings.local.json`
- Target directory artifacts (will be regenerated on next build)
