## Context

`TextAnnotation` currently stores `text`, `fontSize`, `fontColor`, and `fontFamily`. `CheckboxAnnotation` stores `label`, `checked`, and `checkmarkColor`. Rendering happens in `PdfAnnotationFlattener` using PDFBox's Type1 standard fonts (Helvetica). `PropertiesPanel` exposes the existing fields via a `VBox` of labeled controls. Annotation state is serialised to a Java `Properties` object inside a private PDF catalog entry by `PdfTapirPackageService`.

`AboutDialog` hardcodes the app name but has no version display. The POM declares `<version>1.0.0</version>` but nothing surfaces it to the UI.

## Goals / Non-Goals

**Goals:**
- Add bold, italic, and text alignment (left/center/right) to `TextAnnotation` — model, rendering, UI, serialization
- Add a borderless mode to `CheckboxAnnotation` — model, rendering, UI, serialization
- Show the app version on the About screen, sourced from the POM at build time

**Non-Goals:**
- Custom font file embedding for bold/italic (we use the Standard 14 Type1 variants already bundled with PDFBox)
- Per-word or per-character formatting (the entire annotation is styled uniformly)
- Alignment affecting how the JavaFX canvas preview renders during editing (canvas rendering uses JavaFX `Font`, not PDFBox — that complexity is deferred)

## Decisions

**Bold/italic use Standard 14 Type1 font variants, not arbitrary TTF/OTF files.**
- `PDType1Font` with `Standard14Fonts.FontName.HELVETICA_BOLD`, `HELVETICA_OBLIQUE`, `HELVETICA_BOLD_OBLIQUE` are already available in PDFBox with no additional dependencies.
- Applying true bold/italic to arbitrary font families would require loading and embedding TTF variants, which is a much larger change. We scope this to Helvetica variants for the flattened output (the canvas preview uses JavaFX font which handles `FontWeight` and `FontPosture` for any family).
- Trade-off: if the user picks e.g. "Times New Roman" as the font family, the flattened PDF will still use the Helvetica bold/italic variant. Acceptable for now given scope.

**Text alignment is computed manually per line in the flattener.**
- PDFBox `PDPageContentStream` has no built-in alignment. For each line we measure `font.getStringWidth(line) / 1000f * fontSize` and offset the `newLineAtOffset` x-coordinate accordingly.
- CENTER: `annotationX + (annotationWidth - lineWidth) / 2`
- RIGHT: `annotationX + annotationWidth - lineWidth - 3`
- LEFT: `annotationX + 3` (existing behaviour)

**`textAlign` stored as a String ("LEFT"/"CENTER"/"RIGHT") in the model.**
- Avoids creating a new enum in the model package; aligns with how `fontColor` and `fontFamily` are already handled as strings.
- Default is `"LEFT"` to match the existing rendering behaviour — backward compatible.

**`borderless` field on `CheckboxAnnotation` defaults to `false`.**
- When `false`, existing rendering is unchanged.
- When `true`, `PdfAnnotationFlattener` skips the `addRect`/`stroke` call and draws only the checkmark (if checked).
- The checkmark is still drawn even when borderless and checked, so a borderless unchecked checkbox is invisible — which is the expected "no border, no checkmark" state.

**Version injection via filtered `build.properties` resource.**
- Add `src/main/resources/com/pdftapir/build.properties` containing `version=${project.version}`.
- Configure `maven-resources-plugin` with `<filtering>true</filtering>` scoped to that directory.
- `AboutDialog` reads the file at runtime via `getClass().getResourceAsStream("build.properties")` and loads it as a `Properties` object.
- This is the idiomatic Maven approach with no new dependencies.

**PropertiesPanel UI controls:**
- Bold/italic: two `CheckBox` controls (consistent with the existing `checkedBox` / `borderlessBox` pattern)
- Alignment: a `SegmentedButton`-style `ToggleGroup` with 3 `ToggleButton`s ("L / C / R") — gives compact, visual alignment selection without a ComboBox
- Borderless: a `CheckBox("Borderless")` in the checkbox section, above the checkmark color picker

## Risks / Trade-offs

- **Bold/italic font mismatch in flattened output**: As noted, the flattened PDF uses Helvetica variants regardless of font family. A user who picks "Times New Roman" + bold will see Helvetica Bold in the exported/saved PDF. This is a known limitation and acceptable for this scope.
- **Alignment in canvas preview**: The live JavaFX canvas preview (`PdfCanvas`) uses JavaFX rendering, not the PDFBox flattener. Alignment is NOT implemented in the canvas preview in this change — the preview will always show left-aligned text. This is a visible gap and should be addressed in a follow-up.

## Migration Plan

No file migration required. New model fields (`bold`, `italic`, `textAlign`, `borderless`) default to `false`/`"LEFT"` when absent, so existing saved PDFs load correctly via the updated deserializer's fallback defaults.
