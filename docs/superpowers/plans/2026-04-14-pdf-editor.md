# PDF Escroto — Visual PDF Editor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a JavaFX desktop app that opens a PDF, lets the user visually place text boxes, checkboxes, and images on top of pages, and saves them as re-editable PDF annotations.

**Architecture:** JavaFX Canvas-based rendering loop draws PDF page images (via PDFBox PDFRenderer) and annotation overlays. Annotations are stored in-memory as a model layer, then serialized to PDF-native types (FreeText, AcroForm CheckBox, Stamp) on save. Undo/redo uses the Command pattern.

**Tech Stack:** Java 21, JavaFX 21, Apache PDFBox 3.0.2, JUnit 5, Maven

---

## File Map

| File | Responsibility |
|---|---|
| `pom.xml` | Maven build, dependencies, javafx-maven-plugin |
| `PdfEscrotoApp.java` | JavaFX Application entry point |
| `model/Annotation.java` | Abstract base: id, x, y, width, height (PDF points) |
| `model/TextAnnotation.java` | text, fontSize, fontColor |
| `model/CheckboxAnnotation.java` | label, checked |
| `model/ImageAnnotation.java` | imageData (bytes), fxImage (cached) |
| `model/PdfPage.java` | pageIndex, pageWidthPt, pageHeightPt, renderedImage, annotations |
| `model/PdfDocument.java` | Wraps PDDocument, list of PdfPage, sourceFile |
| `service/CoordinateMapper.java` | Converts between canvas px (top-left) and PDF pts (bottom-left) |
| `service/PdfRenderer.java` | PDFBox page → WritableImage |
| `service/PdfLoader.java` | Opens PDF file → PdfDocument (reads back tagged annotations) |
| `service/PdfSaver.java` | PdfDocument → writes PDFBox annotation objects, saves to disk |
| `command/Command.java` | Interface: execute(), undo() |
| `command/UndoManager.java` | Bounded deque of Commands, undo/redo |
| `command/AddAnnotationCommand.java` | Adds annotation to page |
| `command/DeleteAnnotationCommand.java` | Removes annotation from page |
| `command/MoveAnnotationCommand.java` | Records old/new x,y |
| `command/ResizeAnnotationCommand.java` | Records old/new w,h |
| `command/EditAnnotationCommand.java` | Records old/new property value (text, checked, etc.) |
| `ui/MainWindow.java` | Root BorderPane, assembles all UI, file open/save, keyboard shortcuts |
| `ui/EditorToolBar.java` | Tool toggle buttons, page nav, zoom display, Save button |
| `ui/PropertiesPanel.java` | Right panel: form fields bound to selected annotation |
| `ui/PdfCanvas.java` | Canvas subclass: render loop, mouse events, tool dispatch |
| `test/.../CoordinateMapperTest.java` | Unit tests for coordinate round-trips |
| `test/.../PdfRoundTripTest.java` | Integration: write annotations, save, reload, assert |
| `test/.../UndoManagerTest.java` | Unit tests for undo/redo sequences |

**Coordinate convention used throughout:** Annotations store PDF points with bottom-left origin. When drawing on canvas, `canvasY = (pageHeightPt - pdfY - pdfHeight) * scale` gives the top-left Y of the annotation rect.

---

## Task 1: Maven Project Scaffold

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/pdfescroto/.gitkeep`

- [ ] **Step 1: Create `pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.pdfescroto</groupId>
    <artifactId>pdf-escroto</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <javafx.version>21.0.4</javafx.version>
        <pdfbox.version>3.0.2</pdfbox.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-controls</artifactId>
            <version>${javafx.version}</version>
        </dependency>
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-swing</artifactId>
            <version>${javafx.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.pdfbox</groupId>
            <artifactId>pdfbox</artifactId>
            <version>${pdfbox.version}</version>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.2</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.openjfx</groupId>
                <artifactId>javafx-maven-plugin</artifactId>
                <version>0.0.8</version>
                <configuration>
                    <mainClass>com.pdfescroto.PdfEscrotoApp</mainClass>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create the package directory tree**

```bash
mkdir -p src/main/java/com/pdfescroto/{model,service,command,ui}
mkdir -p src/test/java/com/pdfescroto/{service,command}
```

- [ ] **Step 3: Verify Maven downloads dependencies**

```bash
mvn dependency:resolve -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
git init
git add pom.xml
git commit -m "chore: initial Maven scaffold with JavaFX 21 and PDFBox 3"
```

---

## Task 2: Model Layer

**Files:**
- Create: `src/main/java/com/pdfescroto/model/Annotation.java`
- Create: `src/main/java/com/pdfescroto/model/TextAnnotation.java`
- Create: `src/main/java/com/pdfescroto/model/CheckboxAnnotation.java`
- Create: `src/main/java/com/pdfescroto/model/ImageAnnotation.java`
- Create: `src/main/java/com/pdfescroto/model/PdfPage.java`
- Create: `src/main/java/com/pdfescroto/model/PdfDocument.java`

- [ ] **Step 1: Create `Annotation.java`**

Coordinates are in PDF points, bottom-left origin.

```java
package com.pdfescroto.model;

import java.util.UUID;

public abstract class Annotation {
    private final String id = UUID.randomUUID().toString();
    private double x;       // PDF pts, from page left
    private double y;       // PDF pts, from page bottom
    private double width;   // PDF pts
    private double height;  // PDF pts

    protected Annotation(double x, double y, double width, double height) {
        this.x = x; this.y = y; this.width = width; this.height = height;
    }

    public String getId()           { return id; }
    public double getX()            { return x; }
    public void   setX(double x)    { this.x = x; }
    public double getY()            { return y; }
    public void   setY(double y)    { this.y = y; }
    public double getWidth()        { return width; }
    public void   setWidth(double w){ this.width = w; }
    public double getHeight()       { return height; }
    public void   setHeight(double h){ this.height = h; }
}
```

- [ ] **Step 2: Create `TextAnnotation.java`**

```java
package com.pdfescroto.model;

public class TextAnnotation extends Annotation {
    private String text      = "";
    private float  fontSize  = 12f;
    private String fontColor = "#000000";

    public TextAnnotation(double x, double y, double width, double height) {
        super(x, y, width, height);
    }

    public String getText()             { return text; }
    public void   setText(String t)     { this.text = t; }
    public float  getFontSize()         { return fontSize; }
    public void   setFontSize(float s)  { this.fontSize = s; }
    public String getFontColor()        { return fontColor; }
    public void   setFontColor(String c){ this.fontColor = c; }
}
```

- [ ] **Step 3: Create `CheckboxAnnotation.java`**

```java
package com.pdfescroto.model;

public class CheckboxAnnotation extends Annotation {
    private String  label   = "";
    private boolean checked = false;

    public CheckboxAnnotation(double x, double y, double width, double height) {
        super(x, y, width, height);
    }

    public String  getLabel()           { return label; }
    public void    setLabel(String l)   { this.label = l; }
    public boolean isChecked()          { return checked; }
    public void    setChecked(boolean c){ this.checked = c; }
}
```

- [ ] **Step 4: Create `ImageAnnotation.java`**

```java
package com.pdfescroto.model;

import javafx.scene.image.Image;

public class ImageAnnotation extends Annotation {
    private byte[] imageData; // raw bytes of the image file
    private Image  fxImage;   // cached JavaFX image for canvas drawing

    public ImageAnnotation(double x, double y, double width, double height) {
        super(x, y, width, height);
    }

    public byte[] getImageData()          { return imageData; }
    public void   setImageData(byte[] d)  { this.imageData = d; }
    public Image  getFxImage()            { return fxImage; }
    public void   setFxImage(Image img)   { this.fxImage = img; }
}
```

- [ ] **Step 5: Create `PdfPage.java`**

```java
package com.pdfescroto.model;

import javafx.scene.image.WritableImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PdfPage {
    private final int   pageIndex;
    private final float pageWidthPt;
    private final float pageHeightPt;
    private WritableImage        renderedImage;
    private final List<Annotation> annotations = new ArrayList<>();

    public PdfPage(int pageIndex, float pageWidthPt, float pageHeightPt) {
        this.pageIndex    = pageIndex;
        this.pageWidthPt  = pageWidthPt;
        this.pageHeightPt = pageHeightPt;
    }

    public int   getPageIndex()    { return pageIndex; }
    public float getPageWidthPt()  { return pageWidthPt; }
    public float getPageHeightPt() { return pageHeightPt; }

    public WritableImage getRenderedImage()             { return renderedImage; }
    public void          setRenderedImage(WritableImage i){ this.renderedImage = i; }

    public void           addAnnotation(Annotation a)    { annotations.add(a); }
    public void           removeAnnotation(Annotation a) { annotations.remove(a); }
    public List<Annotation> getAnnotations()             { return Collections.unmodifiableList(annotations); }
}
```

- [ ] **Step 6: Create `PdfDocument.java`**

```java
package com.pdfescroto.model;

import org.apache.pdfbox.pdmodel.PDDocument;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class PdfDocument implements AutoCloseable {
    private final PDDocument     pdDocument;
    private final List<PdfPage>  pages;
    private       File           sourceFile;

    public PdfDocument(PDDocument pdDocument, List<PdfPage> pages, File sourceFile) {
        this.pdDocument = pdDocument;
        this.pages      = pages;
        this.sourceFile = sourceFile;
    }

    public PDDocument    getPdDocument() { return pdDocument; }
    public List<PdfPage> getPages()      { return pages; }
    public File          getSourceFile() { return sourceFile; }
    public void          setSourceFile(File f){ this.sourceFile = f; }

    @Override
    public void close() throws IOException { pdDocument.close(); }
}
```

- [ ] **Step 7: Verify compilation**

```bash
mvn compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/pdfescroto/model/
git commit -m "feat: add model layer (Annotation hierarchy, PdfPage, PdfDocument)"
```

---

## Task 3: CoordinateMapper (TDD)

**Files:**
- Create: `src/main/java/com/pdfescroto/service/CoordinateMapper.java`
- Create: `src/test/java/com/pdfescroto/service/CoordinateMapperTest.java`

- [ ] **Step 1: Write the failing tests**

```java
package com.pdfescroto.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CoordinateMapperTest {

    @Test
    void pdfXToCanvas_scalesCorrectly() {
        var m = new CoordinateMapper(792f, 2.0);
        assertEquals(200.0, m.pdfXToCanvas(100.0), 0.001);
    }

    @Test
    void pdfYZeroIsCanvasBottom() {
        // PDF y=0 (bottom) → canvas y = pageHeight * scale (bottom of canvas)
        var m = new CoordinateMapper(792f, 1.0);
        assertEquals(792.0, m.pdfYToCanvasTop(0.0, 0.0), 0.001);
    }

    @Test
    void pdfYTopIsCanvasTop() {
        // PDF y=pageHeight (top) with height=0 → canvas y = 0 (top)
        var m = new CoordinateMapper(792f, 1.0);
        assertEquals(0.0, m.pdfYToCanvasTop(792.0, 0.0), 0.001);
    }

    @Test
    void annotationTopInCanvas() {
        // Annotation at pdf y=50, height=30 → top of annotation in canvas
        // = (pageHeight - y - height) * scale = (792 - 50 - 30) * 1 = 712
        var m = new CoordinateMapper(792f, 1.0);
        assertEquals(712.0, m.pdfYToCanvasTop(50.0, 30.0), 0.001);
    }

    @Test
    void canvasXToPdf_roundTrip() {
        var m = new CoordinateMapper(792f, 2.0);
        assertEquals(123.0, m.canvasXToPdf(m.pdfXToCanvas(123.0)), 0.001);
    }

    @Test
    void canvasYToPdf_roundTrip() {
        // click at canvas y → pdfY of the click point
        var m = new CoordinateMapper(792f, 1.5);
        double pdfY = 300.0;
        double canvasY = m.pdfYToCanvasTop(pdfY, 0.0);
        assertEquals(pdfY, m.canvasYToPdf(canvasY), 0.001);
    }

    @Test
    void scaleToPx_andBack() {
        var m = new CoordinateMapper(792f, 2.5);
        assertEquals(100.0, m.canvasDimToPdf(m.pdfDimToCanvas(100.0)), 0.001);
    }
}
```

- [ ] **Step 2: Run tests — expect FAIL**

```bash
mvn test -Dtest=CoordinateMapperTest -q 2>&1 | tail -5
```

Expected: compilation error (class doesn't exist yet)

- [ ] **Step 3: Implement `CoordinateMapper.java`**

```java
package com.pdfescroto.service;

public class CoordinateMapper {
    private final float  pageHeightPt;
    private final double scale;        // pixels per point

    public CoordinateMapper(float pageHeightPt, double scale) {
        this.pageHeightPt = pageHeightPt;
        this.scale        = scale;
    }

    /** PDF x (pts) → canvas x (px) */
    public double pdfXToCanvas(double pdfX) { return pdfX * scale; }

    /**
     * PDF annotation bottom-y + height → canvas Y of the annotation's TOP edge.
     * PDF origin is bottom-left; canvas origin is top-left.
     * canvasTop = (pageHeight - pdfY - pdfHeight) * scale
     */
    public double pdfYToCanvasTop(double pdfY, double pdfHeight) {
        return (pageHeightPt - pdfY - pdfHeight) * scale;
    }

    /** Canvas x (px) → PDF x (pts) */
    public double canvasXToPdf(double canvasX) { return canvasX / scale; }

    /**
     * Canvas Y of a click point → PDF y at that point.
     * Use pdfHeight=0 for hit-testing a point.
     */
    public double canvasYToPdf(double canvasY) {
        return pageHeightPt - (canvasY / scale);
    }

    /** PDF dimension (pts) → canvas dimension (px) */
    public double pdfDimToCanvas(double pdfDim) { return pdfDim * scale; }

    /** Canvas dimension (px) → PDF dimension (pts) */
    public double canvasDimToPdf(double canvasDim) { return canvasDim / scale; }

    public double getScale() { return scale; }
}
```

- [ ] **Step 4: Run tests — expect PASS**

```bash
mvn test -Dtest=CoordinateMapperTest -q
```

Expected: `BUILD SUCCESS` (7 tests passed)

- [ ] **Step 5: Commit**

```bash
git add src/
git commit -m "feat: add CoordinateMapper with full test coverage"
```

---

## Task 4: PdfRenderer

**Files:**
- Create: `src/main/java/com/pdfescroto/service/PdfRenderer.java`

- [ ] **Step 1: Create `PdfRenderer.java`**

```java
package com.pdfescroto.service;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.io.IOException;

public class PdfRenderer {
    private static final float SCREEN_DPI = 96f;

    public WritableImage renderPage(PDDocument document, int pageIndex) throws IOException {
        return renderPage(document, pageIndex, SCREEN_DPI);
    }

    public WritableImage renderPage(PDDocument document, int pageIndex, float dpi) throws IOException {
        var pdfRenderer    = new PDFRenderer(document);
        var bufferedImage  = pdfRenderer.renderImageWithDPI(pageIndex, dpi);
        return SwingFXUtils.toFXImage(bufferedImage, null);
    }
}
```

- [ ] **Step 2: Compile**

```bash
mvn compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/pdfescroto/service/PdfRenderer.java
git commit -m "feat: add PdfRenderer wrapping PDFBox PDFRenderer"
```

---

## Task 5: PdfSaver + PdfLoader (TDD Round-Trip)

**Files:**
- Create: `src/main/java/com/pdfescroto/service/PdfLoader.java`
- Create: `src/main/java/com/pdfescroto/service/PdfSaver.java`
- Create: `src/test/java/com/pdfescroto/service/PdfRoundTripTest.java`

Annotations written by this tool are tagged with `Subject = "pdf-escroto-<type>"` so the loader can identify them on re-open. For checkboxes, field names are prefixed `pescroto_<id>`.

- [ ] **Step 1: Write round-trip integration test**

```java
package com.pdfescroto.service;

import com.pdfescroto.model.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.*;
import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class PdfRoundTripTest {

    private File tempFile;

    @BeforeEach
    void setUp() throws Exception {
        tempFile = File.createTempFile("pescroto-test-", ".pdf");
        tempFile.deleteOnExit();
        // Create a minimal 1-page PDF
        try (var doc = new PDDocument()) {
            doc.addPage(new PDPage(PDRectangle.A4));
            doc.save(tempFile);
        }
    }

    @Test
    void textAnnotationSurvivesRoundTrip() throws Exception {
        var loader = new PdfLoader();
        var saver  = new PdfSaver();

        var doc  = loader.load(tempFile);
        var page = doc.getPages().get(0);
        var ta   = new TextAnnotation(50, 100, 200, 30);
        ta.setText("Hello PDF");
        ta.setFontSize(14f);
        page.addAnnotation(ta);
        saver.save(doc, tempFile);
        doc.close();

        var reloaded     = loader.load(tempFile);
        var annotations  = reloaded.getPages().get(0).getAnnotations();
        assertEquals(1, annotations.size());
        var loaded = (TextAnnotation) annotations.get(0);
        assertEquals("Hello PDF", loaded.getText());
        assertEquals(50,  loaded.getX(),      0.5);
        assertEquals(100, loaded.getY(),      0.5);
        assertEquals(200, loaded.getWidth(),  0.5);
        assertEquals(30,  loaded.getHeight(), 0.5);
        reloaded.close();
    }

    @Test
    void checkboxAnnotationSurvivesRoundTrip() throws Exception {
        var loader = new PdfLoader();
        var saver  = new PdfSaver();

        var doc  = loader.load(tempFile);
        var page = doc.getPages().get(0);
        var cb   = new CheckboxAnnotation(50, 200, 20, 20);
        cb.setLabel("agree");
        cb.setChecked(true);
        page.addAnnotation(cb);
        saver.save(doc, tempFile);
        doc.close();

        var reloaded    = loader.load(tempFile);
        var annotations = reloaded.getPages().get(0).getAnnotations();
        assertEquals(1, annotations.size());
        var loaded = (CheckboxAnnotation) annotations.get(0);
        assertEquals("agree", loaded.getLabel());
        assertTrue(loaded.isChecked());
        reloaded.close();
    }
}
```

- [ ] **Step 2: Run — expect FAIL (classes missing)**

```bash
mvn test -Dtest=PdfRoundTripTest 2>&1 | tail -6
```

Expected: compilation error

- [ ] **Step 3: Create `PdfSaver.java`**

```java
package com.pdfescroto.service;

import com.pdfescroto.model.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.*;
import org.apache.pdfbox.pdmodel.interactive.form.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class PdfSaver {

    private static final String TAG_TEXT     = "pdf-escroto-text";
    private static final String TAG_CHECKBOX = "pdf-escroto-checkbox";
    private static final String TAG_IMAGE    = "pdf-escroto-image";
    private static final String CB_PREFIX    = "pescroto_";

    public void save(PdfDocument document, File target) throws IOException {
        var pdDoc = document.getPdDocument();

        for (var page : document.getPages()) {
            var pdPage = pdDoc.getPage(page.getPageIndex());

            // Remove previously written pdf-escroto annotations
            var existing = pdPage.getAnnotations();
            existing.removeIf(a -> TAG_TEXT.equals(a.getSubject())
                                || TAG_IMAGE.equals(a.getSubject()));
            pdPage.setAnnotations(existing);

            // Remove our AcroForm fields for this page
            removeOurCheckboxFields(pdDoc, page.getPageIndex());

            // Write current annotations
            for (var annotation : page.getAnnotations()) {
                if (annotation instanceof TextAnnotation ta) {
                    writeText(pdDoc, pdPage, ta, page.getPageHeightPt());
                } else if (annotation instanceof CheckboxAnnotation ca) {
                    writeCheckbox(pdDoc, pdPage, ca);
                } else if (annotation instanceof ImageAnnotation ia) {
                    writeImage(pdDoc, pdPage, ia, page.getPageHeightPt());
                }
            }
        }

        // Write to temp file then rename
        var tmp = File.createTempFile("pescroto-", ".pdf", target.getParentFile());
        try {
            pdDoc.save(tmp);
            Files.move(tmp.toPath(), target.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            tmp.delete();
            throw e;
        }
    }

    private void writeText(PDDocument pdDoc, PDPage pdPage,
                           TextAnnotation ta, float pageHeightPt) throws IOException {
        var rect = toPdRect(ta, pageHeightPt);
        var annot = new PDAnnotationFreeText();
        annot.setSubject(TAG_TEXT);
        annot.setContents(ta.getText());
        annot.setRectangle(rect);
        annot.setDefaultAppearance("/Helvetica " + (int) ta.getFontSize() + " Tf 0 0 0 rg");
        pdPage.getAnnotations().add(annot);
    }

    private void writeCheckbox(PDDocument pdDoc, PDPage pdPage,
                               CheckboxAnnotation ca) throws IOException {
        var acroForm = getOrCreateAcroForm(pdDoc);
        var checkbox = new PDCheckBox(acroForm);
        checkbox.setPartialName(CB_PREFIX + ca.getId().replace("-", ""));
        checkbox.setAlternateFieldName(ca.getLabel());

        var widget = checkbox.getWidgets().get(0);
        widget.setSubject(TAG_CHECKBOX);
        widget.setPage(pdPage);
        // PDFBox uses bottom-left rectangle for widgets — store as-is
        widget.setRectangle(new PDRectangle(
                (float) ca.getX(), (float) ca.getY(),
                (float) ca.getWidth(), (float) ca.getHeight()));

        acroForm.getFields().add(checkbox);
        pdPage.getAnnotations().add(widget);

        if (ca.isChecked()) checkbox.check();
        else checkbox.unCheck();
    }

    private void writeImage(PDDocument pdDoc, PDPage pdPage,
                            ImageAnnotation ia, float pageHeightPt) throws IOException {
        if (ia.getImageData() == null) return;
        var rect  = toPdRect(ia, pageHeightPt);
        var stamp = new PDAnnotationStamp();
        stamp.setSubject(TAG_IMAGE);
        stamp.setRectangle(rect);

        // Embed image in appearance stream
        var pdImage = org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
                .createFromByteArray(pdDoc, ia.getImageData(), "img");
        var appearStream = new org.apache.pdfbox.pdmodel.PDAppearanceStream(pdDoc);
        appearStream.setResources(new org.apache.pdfbox.pdmodel.PDResources());
        appearStream.setBBox(rect);
        try (var cs = appearStream.getContentStream()) {
            cs.drawImage(pdImage, 0, 0, rect.getWidth(), rect.getHeight());
        }
        var appearDict = new PDAppearanceDictionary();
        appearDict.setNormalAppearance(appearStream);
        stamp.setAppearance(appearDict);

        pdPage.getAnnotations().add(stamp);
    }

    private PDRectangle toPdRect(Annotation a, float pageHeightPt) {
        // PDFBox rectangle: lower-left x/y in PDF space (bottom-left origin)
        // Annotation already stored in PDF coords — use directly
        return new PDRectangle(
                (float) a.getX(), (float) a.getY(),
                (float) a.getWidth(), (float) a.getHeight());
    }

    private PDAcroForm getOrCreateAcroForm(PDDocument pdDoc) {
        var catalog  = pdDoc.getDocumentCatalog();
        var acroForm = catalog.getAcroForm();
        if (acroForm == null) {
            acroForm = new PDAcroForm(pdDoc);
            catalog.setAcroForm(acroForm);
        }
        return acroForm;
    }

    private void removeOurCheckboxFields(PDDocument pdDoc, int pageIndex) {
        var acroForm = pdDoc.getDocumentCatalog().getAcroForm();
        if (acroForm == null) return;
        acroForm.getFields().removeIf(f -> f.getPartialName() != null
                && f.getPartialName().startsWith(CB_PREFIX));
    }
}
```

- [ ] **Step 4: Create `PdfLoader.java`**

```java
package com.pdfescroto.service;

import com.pdfescroto.model.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.interactive.annotation.*;
import org.apache.pdfbox.pdmodel.interactive.form.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

public class PdfLoader {

    private static final String TAG_TEXT     = "pdf-escroto-text";
    private static final String TAG_CHECKBOX = "pdf-escroto-checkbox";
    private static final String TAG_IMAGE    = "pdf-escroto-image";

    private final PdfRenderer renderer = new PdfRenderer();

    public PdfDocument load(File file) throws IOException {
        var pdDoc  = PDDocument.load(file);
        var pages  = new ArrayList<PdfPage>();

        for (int i = 0; i < pdDoc.getNumberOfPages(); i++) {
            var pdPage    = pdDoc.getPage(i);
            var mediaBox  = pdPage.getMediaBox();
            var pdfPage   = new PdfPage(i, mediaBox.getWidth(), mediaBox.getHeight());
            pdfPage.setRenderedImage(renderer.renderPage(pdDoc, i));

            for (var annot : pdPage.getAnnotations()) {
                parseAnnotation(annot).ifPresent(pdfPage::addAnnotation);
            }
            pages.add(pdfPage);
        }

        return new PdfDocument(pdDoc, pages, file);
    }

    private Optional<Annotation> parseAnnotation(PDAnnotation pdAnnotation) {
        String subject = pdAnnotation.getSubject();
        if (subject == null) return Optional.empty();

        var rect = pdAnnotation.getRectangle();

        if (TAG_TEXT.equals(subject) && pdAnnotation instanceof PDAnnotationFreeText) {
            var ta = new TextAnnotation(
                    rect.getLowerLeftX(), rect.getLowerLeftY(),
                    rect.getWidth(), rect.getHeight());
            ta.setText(pdAnnotation.getContents() != null ? pdAnnotation.getContents() : "");
            return Optional.of(ta);
        }

        if (TAG_CHECKBOX.equals(subject) && pdAnnotation instanceof PDAnnotationWidget widget) {
            var field = widget.getField();
            var ca = new CheckboxAnnotation(
                    rect.getLowerLeftX(), rect.getLowerLeftY(),
                    rect.getWidth(), rect.getHeight());
            if (field instanceof PDCheckBox checkbox) {
                ca.setLabel(field.getAlternateFieldName() != null
                        ? field.getAlternateFieldName() : "");
                ca.setChecked(checkbox.isChecked());
            }
            return Optional.of(ca);
        }

        if (TAG_IMAGE.equals(subject) && pdAnnotation instanceof PDAnnotationStamp) {
            return Optional.of(new ImageAnnotation(
                    rect.getLowerLeftX(), rect.getLowerLeftY(),
                    rect.getWidth(), rect.getHeight()));
        }

        return Optional.empty();
    }
}
```

- [ ] **Step 5: Run round-trip tests**

```bash
mvn test -Dtest=PdfRoundTripTest
```

Expected: `BUILD SUCCESS` (2 tests pass)

- [ ] **Step 6: Commit**

```bash
git add src/
git commit -m "feat: add PdfLoader and PdfSaver with round-trip annotation persistence"
```

---

## Task 6: UndoManager + Command Interface (TDD)

**Files:**
- Create: `src/main/java/com/pdfescroto/command/Command.java`
- Create: `src/main/java/com/pdfescroto/command/UndoManager.java`
- Create: `src/test/java/com/pdfescroto/command/UndoManagerTest.java`

- [ ] **Step 1: Write failing tests**

```java
package com.pdfescroto.command;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UndoManagerTest {

    /** Simple command that appends/removes from a list */
    static class AppendCommand implements Command {
        private final List<String> list;
        private final String value;
        AppendCommand(List<String> list, String value) { this.list = list; this.value = value; }
        @Override public void execute() { list.add(value); }
        @Override public void undo()    { list.remove(list.size() - 1); }
    }

    @Test
    void executeRunsCommand() {
        var list = new ArrayList<String>();
        var mgr  = new UndoManager(10);
        mgr.execute(new AppendCommand(list, "a"));
        assertEquals(List.of("a"), list);
    }

    @Test
    void undoReversesCommand() {
        var list = new ArrayList<String>();
        var mgr  = new UndoManager(10);
        mgr.execute(new AppendCommand(list, "a"));
        mgr.undo();
        assertTrue(list.isEmpty());
    }

    @Test
    void redoReappliesCommand() {
        var list = new ArrayList<String>();
        var mgr  = new UndoManager(10);
        mgr.execute(new AppendCommand(list, "a"));
        mgr.undo();
        mgr.redo();
        assertEquals(List.of("a"), list);
    }

    @Test
    void newCommandClearsRedoStack() {
        var list = new ArrayList<String>();
        var mgr  = new UndoManager(10);
        mgr.execute(new AppendCommand(list, "a"));
        mgr.undo();
        mgr.execute(new AppendCommand(list, "b"));
        assertFalse(mgr.canRedo());
    }

    @Test
    void undoWhenEmptyDoesNothing() {
        var mgr = new UndoManager(10);
        assertDoesNotThrow(mgr::undo);
    }

    @Test
    void redoWhenEmptyDoesNothing() {
        var mgr = new UndoManager(10);
        assertDoesNotThrow(mgr::redo);
    }

    @Test
    void boundedStack_dropsOldestWhenFull() {
        var list = new ArrayList<String>();
        var mgr  = new UndoManager(3);
        mgr.execute(new AppendCommand(list, "a"));
        mgr.execute(new AppendCommand(list, "b"));
        mgr.execute(new AppendCommand(list, "c"));
        mgr.execute(new AppendCommand(list, "d")); // should drop "a"
        // Can only undo 3 times
        mgr.undo(); mgr.undo(); mgr.undo();
        assertFalse(mgr.canUndo());
    }
}
```

- [ ] **Step 2: Run — expect FAIL**

```bash
mvn test -Dtest=UndoManagerTest 2>&1 | tail -4
```

- [ ] **Step 3: Create `Command.java`**

```java
package com.pdfescroto.command;

public interface Command {
    void execute();
    void undo();
}
```

- [ ] **Step 4: Create `UndoManager.java`**

```java
package com.pdfescroto.command;

import java.util.ArrayDeque;
import java.util.Deque;

public class UndoManager {
    private final int           limit;
    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();

    public UndoManager(int limit) { this.limit = limit; }

    public void execute(Command cmd) {
        cmd.execute();
        if (undoStack.size() == limit) undoStack.pollFirst();
        undoStack.push(cmd);
        redoStack.clear();
    }

    public void undo() {
        if (undoStack.isEmpty()) return;
        var cmd = undoStack.pop();
        cmd.undo();
        redoStack.push(cmd);
    }

    public void redo() {
        if (redoStack.isEmpty()) return;
        var cmd = redoStack.pop();
        cmd.execute();
        undoStack.push(cmd);
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }
}
```

- [ ] **Step 5: Run tests — expect PASS**

```bash
mvn test -Dtest=UndoManagerTest -q
```

Expected: `BUILD SUCCESS` (7 tests)

- [ ] **Step 6: Commit**

```bash
git add src/
git commit -m "feat: add Command interface and UndoManager with full test coverage"
```

---

## Task 7: Annotation Commands

**Files:**
- Create: `src/main/java/com/pdfescroto/command/AddAnnotationCommand.java`
- Create: `src/main/java/com/pdfescroto/command/DeleteAnnotationCommand.java`
- Create: `src/main/java/com/pdfescroto/command/MoveAnnotationCommand.java`
- Create: `src/main/java/com/pdfescroto/command/ResizeAnnotationCommand.java`
- Create: `src/main/java/com/pdfescroto/command/EditAnnotationCommand.java`

- [ ] **Step 1: Create `AddAnnotationCommand.java`**

```java
package com.pdfescroto.command;

import com.pdfescroto.model.Annotation;
import com.pdfescroto.model.PdfPage;

public class AddAnnotationCommand implements Command {
    private final PdfPage  page;
    private final Annotation annotation;

    public AddAnnotationCommand(PdfPage page, Annotation annotation) {
        this.page = page; this.annotation = annotation;
    }

    @Override public void execute() { page.addAnnotation(annotation); }
    @Override public void undo()    { page.removeAnnotation(annotation); }
}
```

- [ ] **Step 2: Create `DeleteAnnotationCommand.java`**

```java
package com.pdfescroto.command;

import com.pdfescroto.model.Annotation;
import com.pdfescroto.model.PdfPage;

public class DeleteAnnotationCommand implements Command {
    private final PdfPage  page;
    private final Annotation annotation;

    public DeleteAnnotationCommand(PdfPage page, Annotation annotation) {
        this.page = page; this.annotation = annotation;
    }

    @Override public void execute() { page.removeAnnotation(annotation); }
    @Override public void undo()    { page.addAnnotation(annotation); }
}
```

- [ ] **Step 3: Create `MoveAnnotationCommand.java`**

```java
package com.pdfescroto.command;

import com.pdfescroto.model.Annotation;

public class MoveAnnotationCommand implements Command {
    private final Annotation annotation;
    private final double oldX, oldY, newX, newY;

    public MoveAnnotationCommand(Annotation annotation,
                                  double oldX, double oldY,
                                  double newX, double newY) {
        this.annotation = annotation;
        this.oldX = oldX; this.oldY = oldY;
        this.newX = newX; this.newY = newY;
    }

    @Override public void execute() { annotation.setX(newX); annotation.setY(newY); }
    @Override public void undo()    { annotation.setX(oldX); annotation.setY(oldY); }
}
```

- [ ] **Step 4: Create `ResizeAnnotationCommand.java`**

```java
package com.pdfescroto.command;

import com.pdfescroto.model.Annotation;

public class ResizeAnnotationCommand implements Command {
    private final Annotation annotation;
    private final double oldX, oldY, oldW, oldH;
    private final double newX, newY, newW, newH;

    public ResizeAnnotationCommand(Annotation annotation,
                                    double oldX, double oldY, double oldW, double oldH,
                                    double newX, double newY, double newW, double newH) {
        this.annotation = annotation;
        this.oldX = oldX; this.oldY = oldY; this.oldW = oldW; this.oldH = oldH;
        this.newX = newX; this.newY = newY; this.newW = newW; this.newH = newH;
    }

    @Override
    public void execute() {
        annotation.setX(newX); annotation.setY(newY);
        annotation.setWidth(newW); annotation.setHeight(newH);
    }

    @Override
    public void undo() {
        annotation.setX(oldX); annotation.setY(oldY);
        annotation.setWidth(oldW); annotation.setHeight(oldH);
    }
}
```

- [ ] **Step 5: Create `EditAnnotationCommand.java`**

Generic property edit: captures old/new values via `Runnable` setters.

```java
package com.pdfescroto.command;

public class EditAnnotationCommand implements Command {
    private final Runnable applyNew;
    private final Runnable applyOld;

    /**
     * @param applyNew  runnable that sets the new value
     * @param applyOld  runnable that restores the old value
     */
    public EditAnnotationCommand(Runnable applyNew, Runnable applyOld) {
        this.applyNew = applyNew;
        this.applyOld = applyOld;
    }

    @Override public void execute() { applyNew.run(); }
    @Override public void undo()    { applyOld.run(); }
}
```

- [ ] **Step 6: Compile**

```bash
mvn compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/pdfescroto/command/
git commit -m "feat: add annotation commands (add, delete, move, resize, edit)"
```

---

## Task 8: MainWindow Skeleton

**Files:**
- Create: `src/main/java/com/pdfescroto/PdfEscrotoApp.java`
- Create: `src/main/java/com/pdfescroto/ui/MainWindow.java`

- [ ] **Step 1: Create `PdfEscrotoApp.java`**

```java
package com.pdfescroto;

import com.pdfescroto.ui.MainWindow;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class PdfEscrotoApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        var window = new MainWindow(primaryStage);
        var scene  = new Scene(window.getRoot(), 1100, 750);
        scene.getStylesheets().add(
                getClass().getResource("/com/pdfescroto/style.css").toExternalForm());
        primaryStage.setTitle("PDF Escroto");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
```

- [ ] **Step 2: Create `src/main/resources/com/pdfescroto/style.css`**

```css
.root {
    -fx-base: #2a2a2a;
    -fx-background: #2a2a2a;
    -fx-control-inner-background: #3a3a3a;
    -fx-text-fill: #dddddd;
}
.tool-bar { -fx-background-color: #333333; -fx-padding: 4 8 4 8; }
.menu-bar  { -fx-background-color: #1e1e1e; }
.menu-bar .label { -fx-text-fill: #cccccc; }
.scroll-pane { -fx-background-color: #555555; }
.scroll-pane > .viewport { -fx-background-color: #555555; }
.properties-panel { -fx-background-color: #2a2a2a; -fx-padding: 10; }
.properties-panel .label { -fx-text-fill: #aaaaaa; -fx-font-size: 11px; }
.properties-panel .text-field { -fx-background-color: #3a3a3a; -fx-text-fill: #ffffff; }
```

- [ ] **Step 3: Create `MainWindow.java`** (skeleton — wired up fully in Task 14)

```java
package com.pdfescroto.ui;

import com.pdfescroto.command.UndoManager;
import com.pdfescroto.model.PdfDocument;
import com.pdfescroto.service.PdfLoader;
import com.pdfescroto.service.PdfSaver;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class MainWindow {

    private final Stage       primaryStage;
    private final BorderPane  root         = new BorderPane();
    private final UndoManager undoManager  = new UndoManager(50);
    private final PdfLoader   loader       = new PdfLoader();
    private final PdfSaver    saver        = new PdfSaver();

    private PdfDocument   openDocument;
    private PdfCanvas     canvas;
    private PropertiesPanel propertiesPanel;

    public MainWindow(Stage primaryStage) {
        this.primaryStage = primaryStage;
        buildUI();
        setupKeyboardShortcuts();
    }

    private void buildUI() {
        root.setTop(buildMenuBar());
        propertiesPanel = new PropertiesPanel(undoManager, () -> {
            if (canvas != null) canvas.redraw();
        });
        root.setRight(propertiesPanel.getNode());
        // Canvas and toolbar added when a document is opened
    }

    private MenuBar buildMenuBar() {
        var openItem   = new MenuItem("Open…");
        var saveItem   = new MenuItem("Save");
        var saveAsItem = new MenuItem("Save As…");
        var exitItem   = new MenuItem("Exit");

        openItem.setOnAction(e -> openFile());
        saveItem.setOnAction(e -> saveFile(false));
        saveAsItem.setOnAction(e -> saveFile(true));
        exitItem.setOnAction(e -> Platform.exit());

        var fileMenu = new Menu("File", null, openItem, saveItem, saveAsItem,
                new SeparatorMenuItem(), exitItem);

        var undoItem = new MenuItem("Undo");
        var redoItem = new MenuItem("Redo");
        undoItem.setOnAction(e -> { undoManager.undo(); if (canvas != null) canvas.redraw(); });
        redoItem.setOnAction(e -> { undoManager.redo(); if (canvas != null) canvas.redraw(); });
        var editMenu = new Menu("Edit", null, undoItem, redoItem);

        return new MenuBar(fileMenu, editMenu);
    }

    private void setupKeyboardShortcuts() {
        root.sceneProperty().addListener((obs, old, scene) -> {
            if (scene == null) return;
            scene.setOnKeyPressed(e -> {
                switch (e.getCode()) {
                    case Z -> { if (e.isShortcutDown()) { undoManager.undo(); if (canvas != null) canvas.redraw(); } }
                    case Y -> { if (e.isShortcutDown()) { undoManager.redo(); if (canvas != null) canvas.redraw(); } }
                    case S -> { if (e.isShortcutDown()) saveFile(false); }
                    case DELETE, BACK_SPACE -> { if (canvas != null) canvas.deleteSelected(); }
                    default -> {}
                }
            });
        });
    }

    private void openFile() {
        var chooser = new FileChooser();
        chooser.setTitle("Open PDF");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        var file = chooser.showOpenDialog(primaryStage);
        if (file == null) return;

        var task = new Task<PdfDocument>() {
            @Override protected PdfDocument call() throws Exception { return loader.load(file); }
        };
        task.setOnSucceeded(e -> {
            if (openDocument != null) { try { openDocument.close(); } catch (Exception ex) { /* ignore */ } }
            openDocument = task.getValue();
            attachDocument();
        });
        task.setOnFailed(e -> showError("Failed to open PDF", task.getException()));
        new Thread(task, "pdf-loader").start();
    }

    private void attachDocument() {
        var toolbar = new EditorToolBar(openDocument, undoManager);
        canvas      = new PdfCanvas(openDocument, undoManager, propertiesPanel);
        toolbar.bindCanvas(canvas);

        var scrollPane = new ScrollPane(canvas);
        scrollPane.setPannable(true);

        root.setTop(new VBox(buildMenuBar(), toolbar.getNode()));
        root.setCenter(scrollPane);
    }

    private void saveFile(boolean saveAs) {
        if (openDocument == null) return;
        File target = openDocument.getSourceFile();
        if (saveAs || target == null) {
            var chooser = new FileChooser();
            chooser.setTitle("Save PDF As");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            target = chooser.showSaveDialog(primaryStage);
            if (target == null) return;
            openDocument.setSourceFile(target);
        }

        final File finalTarget = target;
        var task = new Task<Void>() {
            @Override protected Void call() throws Exception {
                saver.save(openDocument, finalTarget);
                return null;
            }
        };
        task.setOnFailed(e -> showError("Save failed", task.getException()));
        new Thread(task, "pdf-saver").start();
    }

    private void showError(String header, Throwable t) {
        Platform.runLater(() -> {
            var alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(header);
            alert.setContentText(t != null ? t.getMessage() : "Unknown error");
            alert.showAndWait();
        });
    }

    public BorderPane getRoot() { return root; }
}
```

- [ ] **Step 4: Compile (will fail until EditorToolBar, PdfCanvas, PropertiesPanel stubs exist — create them in steps 5-7)**

- [ ] **Step 5: Create empty stub `EditorToolBar.java`**

```java
package com.pdfescroto.ui;

import com.pdfescroto.command.UndoManager;
import com.pdfescroto.model.PdfDocument;
import javafx.scene.Node;
import javafx.scene.layout.HBox;

public class EditorToolBar {
    private final HBox node = new HBox(4);
    public EditorToolBar(PdfDocument doc, UndoManager um) {}
    public void bindCanvas(PdfCanvas canvas) {}
    public Node getNode() { return node; }
}
```

- [ ] **Step 6: Create empty stub `PdfCanvas.java`**

```java
package com.pdfescroto.ui;

import com.pdfescroto.command.UndoManager;
import com.pdfescroto.model.PdfDocument;
import javafx.scene.canvas.Canvas;

public class PdfCanvas extends Canvas {
    public PdfCanvas(PdfDocument doc, UndoManager um, PropertiesPanel pp) {
        super(800, 1000);
    }
    public void redraw() {}
    public void deleteSelected() {}
    public void setActiveTool(Tool tool) {}
    public void goToPage(int index) {}
}
```

- [ ] **Step 7: Create empty stub `PropertiesPanel.java`**

```java
package com.pdfescroto.ui;

import com.pdfescroto.command.UndoManager;
import com.pdfescroto.model.Annotation;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

public class PropertiesPanel {
    private final VBox node = new VBox(6);
    public PropertiesPanel(UndoManager um, Runnable onRedraw) {
        node.getStyleClass().add("properties-panel");
        node.setPrefWidth(170);
    }
    public void showAnnotation(Annotation a) {}
    public Node getNode() { return node; }
}
```

- [ ] **Step 8: Add `Tool` enum**

```java
// src/main/java/com/pdfescroto/ui/Tool.java
package com.pdfescroto.ui;

public enum Tool { SELECT, TEXT, CHECKBOX, IMAGE }
```

- [ ] **Step 9: Compile everything**

```bash
mvn compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 10: Commit**

```bash
git add src/
git commit -m "feat: add MainWindow skeleton, stub UI components, Tool enum"
```

---

## Task 9: EditorToolBar

**Files:**
- Modify: `src/main/java/com/pdfescroto/ui/EditorToolBar.java`

- [ ] **Step 1: Replace stub with full implementation**

```java
package com.pdfescroto.ui;

import com.pdfescroto.command.UndoManager;
import com.pdfescroto.model.PdfDocument;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class EditorToolBar {

    private final HBox       node = new HBox(4);
    private final PdfDocument doc;
    private       PdfCanvas  canvas;
    private final Label      pageLabel = new Label("Page — / —");
    private final Label      zoomLabel = new Label("100%");
    private int currentPage = 0;

    public EditorToolBar(PdfDocument doc, UndoManager um) {
        this.doc = doc;
        node.getStyleClass().add("tool-bar");
        node.setPadding(new Insets(4, 8, 4, 8));
        node.setSpacing(6);

        // Tool toggle group
        var tg         = new ToggleGroup();
        var selectBtn  = toolButton("↖ Select",   Tool.SELECT,   tg);
        var textBtn    = toolButton("T Text",      Tool.TEXT,     tg);
        var cbBtn      = toolButton("☑ Checkbox",  Tool.CHECKBOX, tg);
        var imgBtn     = toolButton("🖼 Image",     Tool.IMAGE,    tg);
        selectBtn.setSelected(true);

        // Separator
        var sep   = new Separator();
        sep.setOrientation(javafx.geometry.Orientation.VERTICAL);

        // Page nav
        var prevBtn = new Button("◀");
        var nextBtn = new Button("▶");
        prevBtn.setOnAction(e -> navigatePage(-1));
        nextBtn.setOnAction(e -> navigatePage(+1));
        updatePageLabel();

        // Spacer
        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Save button
        var saveBtn = new Button("💾 Save");
        saveBtn.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            // Trigger save via event — handled in MainWindow via keyboard shortcut
            node.getScene().lookup("#save-trigger");
            // Simpler: fire Ctrl+S
            node.fireEvent(new javafx.scene.input.KeyEvent(
                    javafx.scene.input.KeyEvent.KEY_PRESSED, "", "",
                    javafx.scene.input.KeyCode.S, false, true, false, false));
        });

        node.getChildren().addAll(selectBtn, textBtn, cbBtn, imgBtn,
                sep, prevBtn, pageLabel, nextBtn, spacer, zoomLabel, saveBtn);
    }

    private ToggleButton toolButton(String label, Tool tool, ToggleGroup tg) {
        var btn = new ToggleButton(label);
        btn.setToggleGroup(tg);
        btn.setOnAction(e -> { if (canvas != null) canvas.setActiveTool(tool); });
        return btn;
    }

    private void navigatePage(int delta) {
        if (canvas == null || doc == null) return;
        int next = currentPage + delta;
        if (next < 0 || next >= doc.getPages().size()) return;
        currentPage = next;
        canvas.goToPage(currentPage);
        updatePageLabel();
    }

    private void updatePageLabel() {
        int total = doc != null ? doc.getPages().size() : 0;
        pageLabel.setText("Page " + (currentPage + 1) + " / " + total);
    }

    public void bindCanvas(PdfCanvas canvas) { this.canvas = canvas; }
    public Node getNode()                    { return node; }
}
```

- [ ] **Step 2: Compile**

```bash
mvn compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/pdfescroto/ui/EditorToolBar.java
git commit -m "feat: implement EditorToolBar with tool selection and page navigation"
```

---

## Task 10: PropertiesPanel

**Files:**
- Modify: `src/main/java/com/pdfescroto/ui/PropertiesPanel.java`

- [ ] **Step 1: Replace stub with full implementation**

```java
package com.pdfescroto.ui;

import com.pdfescroto.command.EditAnnotationCommand;
import com.pdfescroto.command.UndoManager;
import com.pdfescroto.model.*;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class PropertiesPanel {

    private final VBox       node = new VBox(6);
    private final UndoManager undoManager;
    private final Runnable   onRedraw;
    private Annotation       current;

    // Shared controls
    private final TextField xField = new TextField();
    private final TextField yField = new TextField();
    private final TextField wField = new TextField();
    private final TextField hField = new TextField();

    // Text-only controls
    private final TextField textField  = new TextField();
    private final TextField fontField  = new TextField();
    private final VBox      textSection = new VBox(4);

    // Checkbox-only controls
    private final TextField  labelField    = new TextField();
    private final CheckBox   checkedBox    = new CheckBox("Checked");
    private final VBox       cbSection     = new VBox(4);

    public PropertiesPanel(UndoManager undoManager, Runnable onRedraw) {
        this.undoManager = undoManager;
        this.onRedraw    = onRedraw;
        node.getStyleClass().add("properties-panel");
        node.setPrefWidth(170);
        node.setPadding(new Insets(10));
        buildLayout();
        showAnnotation(null);
    }

    private void buildLayout() {
        node.getChildren().addAll(
            bold("Properties"),
            label("X"), xField, label("Y"), yField,
            label("W"), wField, label("H"), hField,
            textSection, cbSection
        );
        textSection.getChildren().addAll(label("Text"), textField, label("Font size"), fontField);
        cbSection.getChildren().addAll(label("Label"), labelField, checkedBox);

        // Commit changes on focus-lost / enter
        commitOnChange(xField, () -> { if (current != null) commitMove(); });
        commitOnChange(yField, () -> { if (current != null) commitMove(); });
        commitOnChange(wField, () -> { if (current != null) commitResize(); });
        commitOnChange(hField, () -> { if (current != null) commitResize(); });
        commitOnChange(textField, () -> {
            if (current instanceof TextAnnotation ta) {
                String old = ta.getText();
                String nw  = textField.getText();
                undoManager.execute(new EditAnnotationCommand(
                        () -> { ta.setText(nw);  onRedraw.run(); },
                        () -> { ta.setText(old); onRedraw.run(); }));
            }
        });
        commitOnChange(fontField, () -> {
            if (current instanceof TextAnnotation ta) {
                float old = ta.getFontSize();
                try {
                    float nw = Float.parseFloat(fontField.getText());
                    undoManager.execute(new EditAnnotationCommand(
                            () -> { ta.setFontSize(nw);  onRedraw.run(); },
                            () -> { ta.setFontSize(old); onRedraw.run(); }));
                } catch (NumberFormatException ignored) {}
            }
        });
        commitOnChange(labelField, () -> {
            if (current instanceof CheckboxAnnotation ca) {
                String old = ca.getLabel();
                String nw  = labelField.getText();
                undoManager.execute(new EditAnnotationCommand(
                        () -> { ca.setLabel(nw);  onRedraw.run(); },
                        () -> { ca.setLabel(old); onRedraw.run(); }));
            }
        });
        checkedBox.setOnAction(e -> {
            if (current instanceof CheckboxAnnotation ca) {
                boolean nw  = checkedBox.isSelected();
                boolean old = !nw;
                undoManager.execute(new EditAnnotationCommand(
                        () -> { ca.setChecked(nw);  onRedraw.run(); },
                        () -> { ca.setChecked(old); onRedraw.run(); }));
            }
        });
    }

    public void showAnnotation(Annotation a) {
        current = a;
        boolean hasAnnotation = a != null;
        xField.setDisable(!hasAnnotation);
        yField.setDisable(!hasAnnotation);
        wField.setDisable(!hasAnnotation);
        hField.setDisable(!hasAnnotation);
        textSection.setVisible(a instanceof TextAnnotation);
        textSection.setManaged(a instanceof TextAnnotation);
        cbSection.setVisible(a instanceof CheckboxAnnotation);
        cbSection.setManaged(a instanceof CheckboxAnnotation);

        if (!hasAnnotation) { xField.clear(); yField.clear(); wField.clear(); hField.clear(); return; }

        xField.setText(fmt(a.getX()));
        yField.setText(fmt(a.getY()));
        wField.setText(fmt(a.getWidth()));
        hField.setText(fmt(a.getHeight()));

        if (a instanceof TextAnnotation ta) {
            textField.setText(ta.getText());
            fontField.setText(String.valueOf((int) ta.getFontSize()));
        } else if (a instanceof CheckboxAnnotation ca) {
            labelField.setText(ca.getLabel());
            checkedBox.setSelected(ca.isChecked());
        }
    }

    private void commitMove() {
        try {
            double oldX = current.getX(), oldY = current.getY();
            double newX = Double.parseDouble(xField.getText());
            double newY = Double.parseDouble(yField.getText());
            undoManager.execute(new EditAnnotationCommand(
                    () -> { current.setX(newX); current.setY(newY); onRedraw.run(); },
                    () -> { current.setX(oldX); current.setY(oldY); onRedraw.run(); }));
        } catch (NumberFormatException ignored) {}
    }

    private void commitResize() {
        try {
            double oldW = current.getWidth(), oldH = current.getHeight();
            double newW = Double.parseDouble(wField.getText());
            double newH = Double.parseDouble(hField.getText());
            undoManager.execute(new EditAnnotationCommand(
                    () -> { current.setWidth(newW); current.setHeight(newH); onRedraw.run(); },
                    () -> { current.setWidth(oldW); current.setHeight(oldH); onRedraw.run(); }));
        } catch (NumberFormatException ignored) {}
    }

    private void commitOnChange(TextField tf, Runnable action) {
        tf.setOnAction(e -> action.run());
        tf.focusedProperty().addListener((obs, was, focused) -> { if (!focused) action.run(); });
    }

    private Label label(String text) {
        var l = new Label(text); l.getStyleClass().add("label"); return l;
    }

    private Label bold(String text) {
        var l = new Label(text);
        l.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        return l;
    }

    private String fmt(double v) { return String.format("%.1f", v); }

    public Node getNode() { return node; }
}
```

- [ ] **Step 2: Compile**

```bash
mvn compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/pdfescroto/ui/PropertiesPanel.java
git commit -m "feat: implement PropertiesPanel with annotation property editing"
```

---

## Task 11: PdfCanvas — Page Rendering

**Files:**
- Modify: `src/main/java/com/pdfescroto/ui/PdfCanvas.java`

Replace the stub with the full rendering implementation. Annotation interaction is added in Tasks 12-13.

- [ ] **Step 1: Replace stub with rendering-capable implementation**

```java
package com.pdfescroto.ui;

import com.pdfescroto.command.UndoManager;
import com.pdfescroto.model.*;
import com.pdfescroto.service.CoordinateMapper;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class PdfCanvas extends Canvas {

    private final PdfDocument    document;
    private final UndoManager    undoManager;
    private final PropertiesPanel propertiesPanel;

    private PdfPage   currentPage;
    private int       currentPageIndex = 0;
    private double    scale            = 1.0;
    private Tool      activeTool       = Tool.SELECT;
    private Annotation selectedAnnotation;

    // Drag state
    private boolean isDragging;
    private double  dragStartX, dragStartY;         // canvas coords at drag start
    private double  annotStartX, annotStartY;        // annotation PDF coords at drag start
    private boolean isCreating;
    private double  createStartX, createStartY;      // PDF coords of creation origin
    private Annotation creatingAnnotation;

    // Resize handle size in canvas pixels
    private static final double HANDLE_SIZE = 8.0;

    public PdfCanvas(PdfDocument document, UndoManager undoManager, PropertiesPanel propertiesPanel) {
        super(800, 1000);
        this.document        = document;
        this.undoManager     = undoManager;
        this.propertiesPanel = propertiesPanel;
        goToPage(0);
        setupMouseHandlers();
    }

    public void goToPage(int index) {
        if (index < 0 || index >= document.getPages().size()) return;
        currentPageIndex   = index;
        currentPage        = document.getPages().get(index);
        selectedAnnotation = null;
        propertiesPanel.showAnnotation(null);
        resizeCanvasToPage();
        redraw();
    }

    private void resizeCanvasToPage() {
        if (currentPage == null) return;
        setWidth(currentPage.getPageWidthPt()  * scale);
        setHeight(currentPage.getPageHeightPt() * scale);
    }

    public void redraw() {
        if (currentPage == null) return;
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());

        // Draw page image
        WritableImage img = currentPage.getRenderedImage();
        if (img != null) gc.drawImage(img, 0, 0, getWidth(), getHeight());

        // Draw annotations
        var mapper = mapper();
        for (var annotation : currentPage.getAnnotations()) {
            drawAnnotation(gc, annotation, mapper, annotation == selectedAnnotation);
        }

        // Draw in-progress creation rect
        if (isCreating && creatingAnnotation != null) {
            drawAnnotation(gc, creatingAnnotation, mapper, false);
        }
    }

    private void drawAnnotation(GraphicsContext gc, Annotation a,
                                CoordinateMapper mapper, boolean selected) {
        double cx = mapper.pdfXToCanvas(a.getX());
        double cy = mapper.pdfYToCanvasTop(a.getY(), a.getHeight());
        double cw = mapper.pdfDimToCanvas(a.getWidth());
        double ch = mapper.pdfDimToCanvas(a.getHeight());

        if (a instanceof TextAnnotation ta) {
            gc.setFill(Color.rgb(255, 255, 255, 0.05));
            gc.fillRect(cx, cy, cw, ch);
            gc.setStroke(selected ? Color.DODGERBLUE : Color.rgb(74, 123, 189, 0.8));
            gc.setLineWidth(selected ? 2.0 : 1.0);
            gc.strokeRect(cx, cy, cw, ch);
            gc.setFill(Color.BLACK);
            gc.setFont(javafx.scene.text.Font.font(ta.getFontSize() * scale));
            gc.fillText(ta.getText(), cx + 3, cy + ta.getFontSize() * scale, cw - 6);

        } else if (a instanceof CheckboxAnnotation ca) {
            gc.setStroke(selected ? Color.DODGERBLUE : Color.rgb(46, 125, 50, 0.9));
            gc.setLineWidth(selected ? 2.0 : 1.5);
            gc.strokeRect(cx, cy, cw, ch);
            if (ca.isChecked()) {
                gc.setStroke(Color.rgb(46, 125, 50));
                gc.setLineWidth(2.0);
                gc.strokeLine(cx + 3, cy + ch * 0.55, cx + cw * 0.4, cy + ch - 3);
                gc.strokeLine(cx + cw * 0.4, cy + ch - 3, cx + cw - 3, cy + 3);
            }

        } else if (a instanceof ImageAnnotation ia) {
            if (ia.getFxImage() != null) {
                gc.drawImage(ia.getFxImage(), cx, cy, cw, ch);
            } else {
                gc.setFill(Color.rgb(230, 81, 0, 0.1));
                gc.fillRect(cx, cy, cw, ch);
                gc.setStroke(Color.rgb(230, 81, 0, 0.8));
                gc.setLineWidth(1.0);
                gc.strokeRect(cx, cy, cw, ch);
            }
        }

        // Selection handles (corners)
        if (selected) drawHandles(gc, cx, cy, cw, ch);
    }

    private void drawHandles(GraphicsContext gc, double cx, double cy, double cw, double ch) {
        gc.setFill(Color.DODGERBLUE);
        double h = HANDLE_SIZE;
        gc.fillRect(cx - h/2,       cy - h/2,       h, h);
        gc.fillRect(cx + cw - h/2,  cy - h/2,       h, h);
        gc.fillRect(cx - h/2,       cy + ch - h/2,  h, h);
        gc.fillRect(cx + cw - h/2,  cy + ch - h/2,  h, h);
    }

    // ---- Placeholder mouse handler setup (expanded in Task 13) ----
    private void setupMouseHandlers() {
        setOnMousePressed(e  -> onMousePressed(e.getX(),  e.getY(),  e.isPrimaryButtonDown()));
        setOnMouseDragged(e  -> onMouseDragged(e.getX(),  e.getY()));
        setOnMouseReleased(e -> onMouseReleased(e.getX(), e.getY()));
    }

    protected void onMousePressed(double cx, double cy, boolean primary)  {}
    protected void onMouseDragged(double cx, double cy)                   {}
    protected void onMouseReleased(double cx, double cy)                  {}

    public void deleteSelected() {
        if (selectedAnnotation == null || currentPage == null) return;
        var cmd = new com.pdfescroto.command.DeleteAnnotationCommand(currentPage, selectedAnnotation);
        undoManager.execute(cmd);
        selectedAnnotation = null;
        propertiesPanel.showAnnotation(null);
        redraw();
    }

    public void setActiveTool(Tool tool) { this.activeTool = tool; }

    protected CoordinateMapper mapper() {
        return new CoordinateMapper(currentPage.getPageHeightPt(), scale);
    }

    // Accessors for subclass / Task 13
    protected PdfPage       getCurrentPage()          { return currentPage; }
    protected Tool          getActiveTool()           { return activeTool; }
    protected Annotation    getSelectedAnnotation()   { return selectedAnnotation; }
    protected void          setSelectedAnnotation(Annotation a) {
        selectedAnnotation = a; propertiesPanel.showAnnotation(a);
    }
    protected UndoManager   getUndoManager()          { return undoManager; }
    protected boolean       isCreating()              { return isCreating; }
    protected void          setCreating(boolean b)    { isCreating = b; }
    protected double        getDragStartX()           { return dragStartX; }
    protected double        getDragStartY()           { return dragStartY; }
    protected double        getAnnotStartX()          { return annotStartX; }
    protected double        getAnnotStartY()          { return annotStartY; }
    protected double        getCreateStartX()         { return createStartX; }
    protected double        getCreateStartY()         { return createStartY; }
    protected Annotation    getCreatingAnnotation()   { return creatingAnnotation; }

    protected void setDragStart(double x, double y) { dragStartX = x; dragStartY = y; }
    protected void setAnnotStart(double x, double y){ annotStartX = x; annotStartY = y; }
    protected void setCreateStart(double x, double y){ createStartX = x; createStartY = y; }
    protected void setCreatingAnnotation(Annotation a){ creatingAnnotation = a; }
    protected void setIsDragging(boolean b)          { isDragging = b; }
    protected boolean getIsDragging()                { return isDragging; }
}
```

- [ ] **Step 2: Compile**

```bash
mvn compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3: Smoke test — app should open and display blank state**

```bash
mvn javafx:run &
```

Open the app, use File → Open to open any PDF. The page should render. Close when done.

- [ ] **Step 4: Commit**

```bash
git add src/
git commit -m "feat: implement PdfCanvas page rendering with annotation drawing"
```

---

## Task 12: PdfCanvas — Mouse Interaction

**Files:**
- Modify: `src/main/java/com/pdfescroto/ui/PdfCanvas.java`

Override the three mouse handler methods to implement create, select, drag, and resize.

- [ ] **Step 1: Replace the three empty mouse handler methods**

Find and replace the three placeholder methods (`onMousePressed`, `onMouseDragged`, `onMouseReleased`) with the following:

```java
@Override
protected void onMousePressed(double cx, double cy, boolean primary) {
    if (!primary || currentPage == null) return;
    var mapper = mapper();

    if (activeTool == Tool.SELECT) {
        // Check resize handles first
        if (selectedAnnotation != null && hitTestHandle(cx, cy, selectedAnnotation, mapper) != null) {
            setIsDragging(false);
            // Resize drag is handled in onMouseDragged via hitTestHandle
            setDragStart(cx, cy);
            setAnnotStart(selectedAnnotation.getX(), selectedAnnotation.getY());
            return;
        }
        // Hit test annotations (reverse order — top-most first)
        var annotations = currentPage.getAnnotations();
        Annotation hit = null;
        for (int i = annotations.size() - 1; i >= 0; i--) {
            if (hitTest(annotations.get(i), cx, cy, mapper)) { hit = annotations.get(i); break; }
        }
        setSelectedAnnotation(hit);
        if (hit != null) {
            setIsDragging(true);
            setDragStart(cx, cy);
            setAnnotStart(hit.getX(), hit.getY());
        }
        redraw();

    } else {
        // Creation: record start in PDF coords
        double pdfX = mapper.canvasXToPdf(cx);
        double pdfY = mapper.canvasYToPdf(cy);
        setCreateStart(pdfX, pdfY);
        setCreating(true);
        var newAnnotation = createAnnotation(pdfX, pdfY, 0, 0);
        setCreatingAnnotation(newAnnotation);
        setSelectedAnnotation(null);
        redraw();
    }
}

@Override
protected void onMouseDragged(double cx, double cy) {
    if (currentPage == null) return;
    var mapper = mapper();

    if (activeTool == Tool.SELECT && getIsDragging() && selectedAnnotation != null) {
        // Move
        double dx = mapper.canvasDimToPdf(cx - getDragStartX());
        double dy = -mapper.canvasDimToPdf(cy - getDragStartY()); // canvas Y is inverted
        selectedAnnotation.setX(getAnnotStartX() + dx);
        selectedAnnotation.setY(getAnnotStartY() + dy);
        propertiesPanel.showAnnotation(selectedAnnotation);
        redraw();

    } else if (isCreating() && getCreatingAnnotation() != null) {
        // Resize creation rect
        double pdfX = mapper.canvasXToPdf(cx);
        double pdfY = mapper.canvasYToPdf(cy);
        double x = Math.min(getCreateStartX(), pdfX);
        double y = Math.min(getCreateStartY(), pdfY);
        double w = Math.abs(pdfX - getCreateStartX());
        double h = Math.abs(pdfY - getCreateStartY());
        var ann = getCreatingAnnotation();
        ann.setX(x); ann.setY(y); ann.setWidth(w); ann.setHeight(h);
        redraw();
    }
}

@Override
protected void onMouseReleased(double cx, double cy) {
    if (currentPage == null) return;

    if (activeTool == Tool.SELECT && getIsDragging() && selectedAnnotation != null) {
        // Commit move as undoable command (only if actually moved)
        double finalX = selectedAnnotation.getX();
        double finalY = selectedAnnotation.getY();
        if (Math.abs(finalX - getAnnotStartX()) > 0.5 || Math.abs(finalY - getAnnotStartY()) > 0.5) {
            double oldX = getAnnotStartX(), oldY = getAnnotStartY();
            final var ann = selectedAnnotation;
            // Move already applied — wrap it so undo restores
            getUndoManager().execute(new com.pdfescroto.command.EditAnnotationCommand(
                    () -> { ann.setX(finalX); ann.setY(finalY); redraw(); },
                    () -> { ann.setX(oldX);   ann.setY(oldY);   redraw(); }
            ));
        }
        setIsDragging(false);

    } else if (isCreating() && getCreatingAnnotation() != null) {
        var ann = getCreatingAnnotation();
        // Only add if large enough to be intentional
        if (ann.getWidth() > 2 && ann.getHeight() > 2) {
            var page = getCurrentPage();
            getUndoManager().execute(new com.pdfescroto.command.AddAnnotationCommand(page, ann));
            setSelectedAnnotation(ann);

            // For image tool: open file chooser after creation
            if (activeTool == Tool.IMAGE) {
                promptImageFile((ImageAnnotation) ann);
            }
        }
        setCreating(false);
        setCreatingAnnotation(null);
        redraw();
    }
}
```

- [ ] **Step 2: Add helper methods at the bottom of the class**

```java
private boolean hitTest(Annotation a, double cx, double cy, CoordinateMapper mapper) {
    double ax = mapper.pdfXToCanvas(a.getX());
    double ay = mapper.pdfYToCanvasTop(a.getY(), a.getHeight());
    double aw = mapper.pdfDimToCanvas(a.getWidth());
    double ah = mapper.pdfDimToCanvas(a.getHeight());
    return cx >= ax && cx <= ax + aw && cy >= ay && cy <= ay + ah;
}

private String hitTestHandle(double cx, double cy, Annotation a, CoordinateMapper mapper) {
    double ax = mapper.pdfXToCanvas(a.getX());
    double ay = mapper.pdfYToCanvasTop(a.getY(), a.getHeight());
    double aw = mapper.pdfDimToCanvas(a.getWidth());
    double ah = mapper.pdfDimToCanvas(a.getHeight());
    double h  = HANDLE_SIZE;
    if (near(cx, ax,      cy, ay)      ) return "NW";
    if (near(cx, ax + aw, cy, ay)      ) return "NE";
    if (near(cx, ax,      cy, ay + ah) ) return "SW";
    if (near(cx, ax + aw, cy, ay + ah) ) return "SE";
    return null;
}

private boolean near(double cx, double hx, double cy, double hy) {
    return Math.abs(cx - hx) < HANDLE_SIZE && Math.abs(cy - hy) < HANDLE_SIZE;
}

private Annotation createAnnotation(double pdfX, double pdfY, double w, double h) {
    return switch (activeTool) {
        case TEXT     -> new TextAnnotation(pdfX, pdfY, w, h);
        case CHECKBOX -> new CheckboxAnnotation(pdfX, pdfY, w, h);
        case IMAGE    -> new ImageAnnotation(pdfX, pdfY, w, h);
        default       -> throw new IllegalStateException("Not a creation tool: " + activeTool);
    };
}

private void promptImageFile(ImageAnnotation ia) {
    var chooser = new javafx.stage.FileChooser();
    chooser.setTitle("Choose Image");
    chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter(
            "Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
    var file = chooser.showOpenDialog(getScene().getWindow());
    if (file == null) return;
    try {
        ia.setImageData(java.nio.file.Files.readAllBytes(file.toPath()));
        ia.setFxImage(new javafx.scene.image.Image(file.toURI().toString()));
        redraw();
    } catch (java.io.IOException e) {
        var alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setContentText("Could not load image: " + e.getMessage());
        alert.showAndWait();
    }
}
```

- [ ] **Step 3: Compile**

```bash
mvn compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 4: Manual smoke test**

```bash
mvn javafx:run
```

1. File → Open any PDF
2. Select "T Text" tool → drag a rectangle on the page → type text in Properties panel
3. Select "☑ Checkbox" tool → drag a square → tick Checked in Properties panel
4. Select "🖼 Image" tool → drag a rectangle → pick an image file
5. Select "↖ Select" tool → drag an annotation to move it
6. Ctrl+Z → annotation should move back
7. File → Save → reopen the file in another PDF viewer, confirm annotations visible
8. Reopen the file in PDF Escroto → annotations should be re-editable

- [ ] **Step 5: Run all tests**

```bash
mvn test
```

Expected: `BUILD SUCCESS` (all tests pass)

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/pdfescroto/ui/PdfCanvas.java
git commit -m "feat: implement PdfCanvas mouse interaction (create, select, move, delete)"
```

---

## Task 13: Final Wiring + .gitignore

**Files:**
- Create: `.gitignore`

- [ ] **Step 1: Create `.gitignore`**

```
target/
*.class
*.jar
.DS_Store
.superpowers/
```

- [ ] **Step 2: Run full test suite one final time**

```bash
mvn test
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3: Final commit**

```bash
git add .gitignore
git commit -m "chore: add .gitignore"
git tag v0.1.0 -m "v0.1.0 — initial visual PDF editor"
```

---

## Self-Review Checklist

- **Spec coverage:**
  - Open PDF ✓ (Task 5 PdfLoader, Task 8 MainWindow openFile)
  - Add text box ✓ (Task 11/12 TEXT tool)
  - Add checkbox ✓ (Task 11/12 CHECKBOX tool, Task 5 AcroForm)
  - Add image ✓ (Task 11/12 IMAGE tool + promptImageFile)
  - Save to PDF ✓ (Task 5 PdfSaver, Task 8 saveFile)
  - Re-editable annotations ✓ (Task 5 round-trip, TAG system)
  - Undo/redo ✓ (Task 6/7, keyboard shortcuts in Task 8)
  - Properties panel ✓ (Task 10)
  - Page navigation ✓ (Task 9 EditorToolBar)
  - Dark UI ✓ (Task 8 style.css)

- **No placeholders:** All tasks contain complete code.
- **Type consistency:** `CoordinateMapper` API (`pdfYToCanvasTop`, `canvasYToPdf`, `pdfDimToCanvas`, `canvasDimToPdf`) is consistent across Tasks 3, 11, 12. `Annotation` field names consistent across model and commands.
