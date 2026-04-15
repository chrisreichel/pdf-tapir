## 1. Rename Java Source Directory

- [x] 1.1 Rename `src/main/java/com/pdfescroto/` directory tree to `src/main/java/com/pdftapir/`
- [x] 1.2 Rename `src/test/java/com/pdfescroto/` directory tree to `src/test/java/com/pdftapir/`

## 2. Update Java Source Files

- [x] 2.1 Replace `package com.pdfescroto` with `package com.pdftapir` in all `.java` files under `src/`
- [x] 2.2 Replace `import com.pdfescroto` with `import com.pdftapir` in all `.java` files under `src/`
- [x] 2.3 Replace any remaining `pdfescroto` string references in `.java` files (e.g., string literals, comments)

## 3. Update Build Configuration

- [x] 3.1 Update `pom.xml`: change `<groupId>com.pdfescroto</groupId>` to `<groupId>com.pdftapir</groupId>`
- [x] 3.2 Update `pom.xml`: change `<artifactId>` value from `pdf-escroto` to `pdf-tapir`
- [x] 3.3 Update `pom.xml`: change `<mainClass>` to reference `com.pdftapir` package
- [x] 3.4 Update `pom.xml`: change app name references (`PDF-Escroto` / `PDF Escroto`) to `PDF-Tapir` / `PDF Tapir`
- [x] 3.5 Update `pom.xml`: change Linux installer name pattern from `pdf-escroto` to `pdf-tapir`

## 4. Update CI/CD

- [x] 4.1 Update `.github/workflows/release.yml`: replace all `pdf-escroto`, `PDF-Escroto`, `pdfescroto` references with `pdf-tapir`, `PDF-Tapir`, `pdftapir`

## 5. Update Documentation and Config

- [x] 5.1 Update `README.md`: replace all old name references with `PDF Tapir` / `pdf-tapir`
- [x] 5.2 Update `AGENTS.md`: replace all old name references
- [x] 5.3 Update any files under `docs/`: replace all old name references
- [x] 5.4 Update `.claude/settings.local.json`: replace any `pdfescroto` or `pdf-escroto` references

## 6. Update OpenSpec Artifacts

- [x] 6.1 Update `openspec/specs/native-installers/spec.md`: replace `pdf-escroto` and `PDF-Escroto` with `pdf-tapir` and `PDF-Tapir`
- [x] 6.2 Update `openspec/specs/release-pipeline/spec.md` if any old name references exist
- [x] 6.3 Update all files under `openspec/changes/archive/` that reference the old name

## 7. Verify

- [x] 7.1 Run `mvn clean test` and confirm all tests pass
- [x] 7.2 Grep for remaining `escroto` (case-insensitive) outside `target/` to confirm no stragglers
