## Why

The annotation canvas has several interaction issues that degrade usability: image annotations drag the PDF background, there is no auto-switch to selection mode after placing content, text annotations cannot be edited inline, font styling options (color, family) are missing, and there is no scroll-based zoom. These collectively make the tool feel unpolished and slow to use.

## What Changes

- **Fix image drag**: Prevent PDF panning when moving an already-placed image annotation; only the annotation should move.
- **Auto-select after placement**: After placing a checkbox, image, or text annotation and committing the content, automatically switch the active tool to the selection tool.
- **Font color support**: Allow users to change the font color for text annotations and the checkmark color for checkbox annotations.
- **Font family support**: Allow users to select the font family (typeface) for text annotations.
- **Inline text editing**: Allow users to type directly into a placed text box on the canvas, not only via the properties panel.
- **Scroll-to-zoom**: Support Shift + scroll wheel to zoom in/out on the canvas.

## Capabilities

### New Capabilities

- `image-annotation-drag-fix`: Correct event propagation so that moving an image annotation does not pan the underlying PDF.
- `auto-select-after-placement`: After content is set for any annotation tool, the active tool reverts to the selection tool automatically.
- `annotation-font-color`: Users can pick a font/checkmark color for text and checkbox annotations.
- `annotation-font-family`: Users can select a font family for text annotations.
- `inline-text-editing`: Text annotations on the canvas support direct in-place keyboard input without requiring the properties panel.
- `scroll-zoom`: Shift + scroll wheel zooms the canvas in/out.

### Modified Capabilities

- `canvas-zoom`: Zoom interaction now also responds to Shift + scroll wheel in addition to the existing View menu controls.

## Impact

- `PdfCanvas` component: event handling for mouse move/drag, scroll/wheel events, tool-switch logic.
- `AnnotationLayer` / image annotation: pointer event handling to stop propagation to the PDF viewer.
- `PropertiesPanel`: add font color picker and font family selector for text/checkbox annotations.
- `TextAnnotation` component: implement contenteditable or equivalent inline editing.
- Zoom state/controller: wire wheel event listener with Shift modifier.
- No external API or dependency changes required.
