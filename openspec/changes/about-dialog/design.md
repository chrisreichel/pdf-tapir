## Context

PDF Tapir uses JavaFX for its UI. The menu bar is built in `MainWindow.buildMenuBar()` and currently returns a `MenuBar` with File, Edit, View, and Document menus. There is no Help menu. The app icon lives at `src/main/resources/ICON.png`.

## Goals / Non-Goals

**Goals:**
- Add a **Help** menu as the rightmost menu in the menu bar
- Implement an About dialog as a JavaFX `Stage` (modal) opened from Help → About PDF Tapir
- Display icon, project name, URL (as a clickable hyperlink), developer, license, and an as-is warranty disclaimer

**Non-Goals:**
- Auto-update checks or version fetching from the network
- Localisation / i18n
- Linking to an external browser from other places in the app

## Decisions

### 1. JavaFX `Stage` (not `Alert`) for the dialog

`Alert` is quick but offers little layout control. A dedicated `Stage` with `initModality(WINDOW_MODAL)` gives full control over the icon display, hyperlink widget, and disclaimer text wrapping without hacks.

**Alternative considered**: `Dialog<Void>` — offers more layout flexibility than `Alert` but still wraps in a `DialogPane` that limits styling. A plain `Stage` is simpler and more flexible here.

### 2. `AboutDialog` as a standalone class in `ui/`

Keeps `MainWindow` focused on wiring and delegates dialog construction to `AboutDialog`. Consistent with how the rest of the UI is organised.

### 3. App icon loaded via `getClass().getResource()`

The icon is already on the classpath at `/com/pdftapir/ICON.png` (moved during rebrand). We load it with `new Image(...)` and display it in an `ImageView` scaled to ~64×64 px. The same image is set on the dialog `Stage` icons list so the OS taskbar/title bar shows it.

### 4. URL as a JavaFX `Hyperlink`

Opens the default browser via `Desktop.getDesktop().browse(URI)` on click. Falls back silently if `Desktop` is not supported (headless environments).

### 5. Disclaimer text

Displayed as a wrapped `Label` or `TextArea` (read-only, no border). Text:

> PDF Tapir is provided "as is", without warranty of any kind, express or implied. In no event shall the author be liable for any claim, damages, or other liability arising from, out of, or in connection with the software or its use.

## Risks / Trade-offs

- [`Desktop.browse()` unavailable on some Linux headless setups] → Caught and silently ignored; URL is visible as plain text fallback
- [Icon resource missing at runtime] → Wrap in null check; dialog still opens without icon rather than crashing
