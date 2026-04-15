## 1. App Icon (In-App)

- [x] 1.1 Add `stage.getIcons().add(new Image(getClass().getResourceAsStream("/ICON.png")));` to `PdfTapirApp.start()` before `stage.show()`

## 2. Platform Icon Files

- [x] 2.1 Convert `src/main/resources/ICON.png` to `packaging/icons/ICON.icns` using `sips` (macOS built-in: `sips -s format icns ICON.png --out ICON.icns`)
- [x] 2.2 Convert `src/main/resources/ICON.png` to `packaging/icons/ICON.ico` using ImageMagick: `convert ICON.png -resize 256x256 packaging/icons/ICON.ico`
- [x] 2.3 Copy `src/main/resources/ICON.png` to `packaging/icons/ICON.png`

## 3. pom.xml Changes

- [x] 3.1 Change `<version>1.0.0-SNAPSHOT</version>` to `<version>1.0.0</version>`
- [x] 3.2 Add `maven-dependency-plugin` configuration to copy runtime deps to `target/lib/` on the `prepare-package` phase
- [x] 3.3 Add Maven profile `mac` (activated on `<os><family>mac</family></os>` and `<os><arch>x86_64</arch></os>`) that adds `javafx-controls:mac`, `javafx-swing:mac`, `javafx-graphics:mac` classifier dependencies
- [x] 3.4 Add Maven profile `mac-aarch64` (activated on `<os><family>mac</family></os>` and `<os><arch>aarch64</arch></os>`) that adds `javafx-controls:mac-aarch64`, `javafx-swing:mac-aarch64`, `javafx-graphics:mac-aarch64` classifier dependencies
- [x] 3.5 Add Maven profile `win` (activated on `<os><family>windows</family></os>`) that adds `javafx-controls:win`, `javafx-swing:win`, `javafx-graphics:win` classifier dependencies
- [x] 3.6 Add Maven profile `linux` (activated on `<os><family>unix</family><name>Linux</name></os>`) that adds `javafx-controls:linux`, `javafx-swing:linux`, `javafx-graphics:linux` classifier dependencies

## 4. GitHub Actions Workflow

- [x] 4.1 Create `.github/workflows/release.yml` triggered on `push: tags: ['v*']`
- [x] 4.2 Define matrix strategy with three entries: `{os: macos-latest, jpackage-type: dmg, icon: packaging/icons/ICON.icns}`, `{os: windows-latest, jpackage-type: msi, icon: packaging/icons/ICON.ico}`, `{os: ubuntu-latest, jpackage-type: deb, icon: packaging/icons/ICON.png}`
- [x] 4.3 Add step: extract version from tag (`VERSION=${GITHUB_REF_NAME#v}`) and make available as env var
- [x] 4.4 Add step: `actions/setup-java` with JDK 21 (temurin distribution)
- [x] 4.5 Add step: `mvn package -DskipTests` to build the app JAR
- [x] 4.6 Add step: `mvn dependency:copy-dependencies -DoutputDirectory=target/lib` to gather all runtime JARs
- [x] 4.7 Add step: copy the app JAR from `target/` into `target/lib/`
- [x] 4.8 Add step: run `jpackage` with `--input target/lib/`, `--main-jar pdf-tapir-$VERSION.jar`, `--main-class com.pdftapir.PdfTapirApp`, `--name "PDF Tapir"`, `--app-version $VERSION`, `--icon ${{ matrix.icon }}`, `--type ${{ matrix.jpackage-type }}`
- [x] 4.9 Add step: `softprops/action-gh-release` to create or update the GitHub Release and upload the installer artifact
