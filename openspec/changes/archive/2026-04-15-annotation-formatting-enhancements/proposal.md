## Why

Text annotations lack bold, italic, and alignment controls that are standard in any text editor, making it harder to format notes and form labels effectively. Checkbox annotations always render with a visible border, which is too visually heavy when only a checkmark is needed. The About screen has no version number, making it difficult to identify which release is installed.

## What Changes

- **Text annotations** gain bold and italic toggles in the PropertiesPanel
- **Text annotations** gain a text alignment selector (left, center, right) in the PropertiesPanel
- **Checkbox annotations** gain a "borderless" toggle — when enabled, the box border is hidden and only the checkmark is drawn
- **About screen** displays the application version number (read from the Maven POM at build time)

## Capabilities

### New Capabilities
- `text-annotation-formatting`: Bold, italic, and text alignment options for text annotations — model fields, PropertiesPanel controls, canvas rendering, serialization/deserialization, and undo support
- `checkbox-borderless-mode`: Optional borderless rendering for checkbox annotations — model field, PropertiesPanel toggle, canvas rendering, serialization/deserialization, and undo support
- `about-screen-version`: App version displayed on the About screen, injected from the Maven POM via a generated properties file

### Modified Capabilities

## Impact

- **`TextAnnotation` model**: new `bold`, `italic` (boolean), and `textAlign` (String: "LEFT"/"CENTER"/"RIGHT") fields
- **`CheckboxAnnotation` model**: new `borderless` (boolean) field
- **`PdfAnnotationFlattener`**: update text rendering to apply bold/italic font variants and alignment; update checkbox rendering to skip the border rect when borderless
- **`PropertiesPanel`**: new controls for bold/italic toggles, alignment selector, and borderless checkbox toggle
- **`PdfTapirPackageService`**: serialise/deserialise new model fields
- **`AboutDialog`**: read and display version from `build.properties`
- **`pom.xml`**: configure `maven-resources-plugin` to filter a `build.properties` resource with `${project.version}`
