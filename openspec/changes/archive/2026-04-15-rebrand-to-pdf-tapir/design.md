## Context

The project was originally named "PDF Escroto" with Java package `com.pdfescroto`. The git remote and local folder have already been renamed to `pdf-tapir`. The remaining codebase — Java source files, build config, CI/CD, docs, and OpenSpec artifacts — still uses the old name in various forms: `pdfescroto`, `pdf-escroto`, `PDF-Escroto`, `PDF Escroto`.

The rename affects multiple layers:
- Java package namespace (`com.pdfescroto` → `com.pdftapir`)
- Maven build config (`pom.xml`)
- GitHub Actions workflow
- Documentation and config files
- OpenSpec specs and archives

## Goals / Non-Goals

**Goals:**
- Replace all occurrences of the old name in all its variants throughout the codebase
- Ensure the app still builds, tests pass, and installers are named correctly after the rename
- Update OpenSpec artifacts to reflect the new name

**Non-Goals:**
- Changing any application behavior or feature set
- Renaming the `target/` directory (regenerated on next build)
- Migrating `target/surefire-reports/` files (generated artifacts, not source)

## Decisions

### 1. Java package rename: `com.pdfescroto` → `com.pdftapir`

All Java source files under `src/main/java/` and `src/test/java/` must have their package declarations and import statements updated. The directory structure must also be renamed from `com/pdfescroto/` to `com/pdftapir/`.

**Approach**: Use a combination of directory rename and bulk find-and-replace across `.java` files. The `pom.xml` `<mainClass>` and any other references must also be updated.

**Why not leave the package as-is?** The package name is a public API surface and a source of confusion — it should match the project identity.

### 2. Name variants to replace

| Old | New |
|-----|-----|
| `com.pdfescroto` | `com.pdftapir` |
| `pdfescroto` | `pdftapir` |
| `pdf-escroto` | `pdf-tapir` |
| `PDF-Escroto` | `PDF-Tapir` |
| `PDF Escroto` | `PDF Tapir` |
| `PdfEscroto` | `PdfTapir` |

### 3. OpenSpec artifacts

Spec files in `openspec/specs/` and `openspec/changes/archive/` that reference the old name should be updated in-place. These are living documentation, not generated files.

## Risks / Trade-offs

- **Directory rename breaks IDE indexes** → Developers should re-import/refresh the project in their IDE after the rename
- **Missed references** → A grep scan after implementation is recommended to verify no `escroto` strings remain in non-target source files
- **Archive files** → The change archives under `openspec/changes/archive/` are historical records; updating them keeps the repo consistent but changes immutable history. Decision: update them anyway since they are living docs in the same repo.

## Migration Plan

1. Rename Java source directory tree: `com/pdfescroto/` → `com/pdftapir/`
2. Bulk replace all name variants in `.java` files
3. Update `pom.xml`
4. Update `.github/workflows/release.yml`
5. Update `README.md`, `AGENTS.md`, `docs/` files
6. Update `openspec/` spec and archive files
7. Update `.claude/settings.local.json`
8. Run `mvn clean test` to verify build and tests pass
9. Grep for remaining `escroto` occurrences outside `target/` to confirm completeness
