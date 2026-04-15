## 1. Model Changes

- [x] 1.1 Add `bold` (boolean, default false), `italic` (boolean, default false), and `textAlign` (String, default "LEFT") fields with getters/setters to `TextAnnotation`
- [x] 1.2 Add `borderless` (boolean, default false) field with getter/setter to `CheckboxAnnotation`

## 2. Serialization

- [x] 2.1 In `PdfTapirPackageService.serializeAnnotations()`, write `bold`, `italic`, and `textAlign` keys for text annotations
- [x] 2.2 In `PdfTapirPackageService.deserializeAnnotations()`, read `bold`, `italic`, and `textAlign` with safe defaults (false/false/"LEFT") for backward compatibility
- [x] 2.3 In `PdfTapirPackageService.serializeAnnotations()`, write `borderless` key for checkbox annotations
- [x] 2.4 In `PdfTapirPackageService.deserializeAnnotations()`, read `borderless` with safe default (false) for backward compatibility

## 3. Flattener Rendering

- [x] 3.1 Update `PdfAnnotationFlattener.drawText()` to select the correct `PDType1Font` variant based on `bold`/`italic` flags: HELVETICA, HELVETICA_BOLD, HELVETICA_OBLIQUE, or HELVETICA_BOLD_OBLIQUE
- [x] 3.2 Update `PdfAnnotationFlattener.drawText()` to compute per-line x-offset for CENTER and RIGHT alignment using `font.getStringWidth(line) / 1000f * fontSize` against the annotation bounding box
- [x] 3.3 Update `PdfAnnotationFlattener.drawCheckbox()` to skip the `addRect`/`stroke` border call when `ca.isBorderless()` is true

## 4. PropertiesPanel UI

- [x] 4.1 Add `boldBox` (`CheckBox("Bold")`), `italicBox` (`CheckBox("Italic")`), and alignment `ToggleButton`s (Left / Center / Right in a `ToggleGroup`) to the text section of `PropertiesPanel`
- [x] 4.2 Wire `boldBox` action: create `EditAnnotationCommand` that sets `ta.setBold()`, commit with undo support, call `onRedraw`
- [x] 4.3 Wire `italicBox` action: create `EditAnnotationCommand` that sets `ta.setItalic()`, commit with undo support, call `onRedraw`
- [x] 4.4 Wire alignment toggle group: on selection change create `EditAnnotationCommand` that sets `ta.setTextAlign()`, commit with undo support, call `onRedraw`
- [x] 4.5 Update `showAnnotation()` to populate `boldBox`, `italicBox`, and the alignment toggle group from the current `TextAnnotation` state
- [x] 4.6 Add `borderlessBox` (`CheckBox("Borderless")`) to the checkbox section of `PropertiesPanel`
- [x] 4.7 Wire `borderlessBox` action: create `EditAnnotationCommand` that sets `ca.setBorderless()`, commit with undo support, call `onRedraw`
- [x] 4.8 Update `showAnnotation()` to populate `borderlessBox` from the current `CheckboxAnnotation` state

## 5. About Screen Version

- [x] 5.1 Create `src/main/resources/com/pdftapir/build.properties` with content `version=${project.version}`
- [x] 5.2 In `pom.xml`, configure `maven-resources-plugin` to enable filtering for `src/main/resources` so `${project.version}` is substituted at build time
- [x] 5.3 In `AboutDialog.show()`, read `build.properties` via `getClass().getResourceAsStream("build.properties")`, load as `Properties`, and display the version in a `Label` beneath the app name label

## 6. Verification

- [x] 6.1 Manually test: create a text annotation, toggle bold + italic, verify canvas redraws and undo/redo works
- [x] 6.2 Manually test: set alignment to center and right, save and reopen — verify alignment persists
- [x] 6.3 Manually test: export flattened PDF with bold/italic/center-aligned text — verify correct rendering in an external viewer
- [x] 6.4 Manually test: create a borderless checked checkbox — verify only checkmark is visible; borderless unchecked — verify nothing renders
- [x] 6.5 Manually test: save and reopen a borderless checkbox — verify borderless state persists
- [x] 6.6 Manually test: open About dialog — verify version number matches the POM version
