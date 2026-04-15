## 1. Lock Flags in PdfSaver

- [x] 1.1 In `PdfSaver.writeText()`, set `ReadOnly | Locked` annotation flags on the `PDAnnotationFreeText` after constructing it
- [x] 1.2 In `PdfSaver.writeImage()`, set `ReadOnly | Locked` annotation flags on the `PDAnnotationRubberStamp` after constructing it
- [x] 1.3 In `PdfSaver.writeCheckbox()`, set `ReadOnly | Locked` annotation flags on the widget (`PDAnnotationWidget`) after constructing it
- [x] 1.4 In `PdfSaver.writeCheckbox()`, set the field-level `ReadOnly` flag on the `PDCheckBox` field (`Ff` bit 1) so external form editors cannot toggle the value

## 2. Tests

- [x] 2.1 Add a round-trip test verifying that after save, the PDAnnotationFreeText written by PdfSaver has both the `ReadOnly` (64) and `Locked` (128) annotation flags set
- [x] 2.2 Add a round-trip test verifying that after save, the PDAnnotationRubberStamp written by PdfSaver has both flags set
- [x] 2.3 Add a round-trip test verifying that after save, the PDAnnotationWidget for a checkbox has both annotation flags set and the field has the field-level `ReadOnly` flag set
