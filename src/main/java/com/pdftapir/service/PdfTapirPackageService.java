package com.pdftapir.service;

import com.pdftapir.model.*;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Stores and restores pdf-tapir's private editable project data inside a PDF.
 * External viewers ignore this custom catalog entry; PdfLoader uses it to
 * reconstruct editable annotations after PdfSaver flattens the visible output.
 */
class PdfTapirPackageService {

    static final COSName PACKAGE_NAME = COSName.getPDFName("PETapirPackage");

    private static final COSName VERSION_NAME  = COSName.getPDFName("Version");
    private static final COSName BASE_PDF_NAME = COSName.getPDFName("BasePdf");
    private static final COSName ITEMS_NAME    = COSName.getPDFName("Items");
    private static final int VERSION = 1;

    Optional<PackageData> read(PDDocument document) throws IOException {
        var packageObject = document.getDocumentCatalog().getCOSObject().getDictionaryObject(PACKAGE_NAME);
        if (!(packageObject instanceof COSDictionary dict)) return Optional.empty();
        if (dict.getInt(VERSION_NAME, -1) != VERSION) return Optional.empty();

        var baseObject = dict.getDictionaryObject(BASE_PDF_NAME);
        var itemObject = dict.getDictionaryObject(ITEMS_NAME);
        if (!(baseObject instanceof COSStream baseStream) || !(itemObject instanceof COSStream itemStream)) {
            return Optional.empty();
        }

        byte[] basePdfBytes;
        byte[] itemBytes;
        try (var in = baseStream.createInputStream()) {
            basePdfBytes = in.readAllBytes();
        }
        try (var in = itemStream.createInputStream()) {
            itemBytes = in.readAllBytes();
        }

        return Optional.of(new PackageData(basePdfBytes, deserializeAnnotations(itemBytes)));
    }

    void write(PDDocument document, byte[] basePdfBytes, List<PdfPage> pages) throws IOException {
        var dict = new COSDictionary();
        dict.setInt(VERSION_NAME, VERSION);
        dict.setItem(BASE_PDF_NAME, stream(document, basePdfBytes));
        dict.setItem(ITEMS_NAME, stream(document, serializeAnnotations(pages)));
        document.getDocumentCatalog().getCOSObject().setItem(PACKAGE_NAME, dict);
    }

    void remove(PDDocument document) {
        document.getDocumentCatalog().getCOSObject().removeItem(PACKAGE_NAME);
    }

    private COSStream stream(PDDocument document, byte[] bytes) throws IOException {
        var stream = document.getDocument().createCOSStream();
        try (var out = stream.createOutputStream()) {
            out.write(bytes);
        }
        return stream;
    }

    private byte[] serializeAnnotations(List<PdfPage> pages) throws IOException {
        var props = new Properties();
        int count = 0;
        for (var page : pages) {
            for (var annotation : page.getAnnotations()) {
                String prefix = "item." + count + ".";
                props.setProperty(prefix + "page", Integer.toString(page.getPageIndex()));
                props.setProperty(prefix + "x", Double.toString(annotation.getX()));
                props.setProperty(prefix + "y", Double.toString(annotation.getY()));
                props.setProperty(prefix + "w", Double.toString(annotation.getWidth()));
                props.setProperty(prefix + "h", Double.toString(annotation.getHeight()));

                if (annotation instanceof TextAnnotation ta) {
                    props.setProperty(prefix + "type", "text");
                    props.setProperty(prefix + "text", b64(ta.getText()));
                    props.setProperty(prefix + "fontSize", Float.toString(ta.getFontSize()));
                    props.setProperty(prefix + "fontColor", nullToEmpty(ta.getFontColor()));
                    props.setProperty(prefix + "fontFamily", b64(ta.getFontFamily()));
                } else if (annotation instanceof CheckboxAnnotation ca) {
                    props.setProperty(prefix + "type", "checkbox");
                    props.setProperty(prefix + "label", b64(ca.getLabel()));
                    props.setProperty(prefix + "checked", Boolean.toString(ca.isChecked()));
                    props.setProperty(prefix + "checkmarkColor", nullToEmpty(ca.getCheckmarkColor()));
                } else if (annotation instanceof ImageAnnotation ia) {
                    props.setProperty(prefix + "type", "image");
                    byte[] imageData = ia.getImageData();
                    props.setProperty(prefix + "imageData", imageData == null ? "" : Base64.getEncoder().encodeToString(imageData));
                } else {
                    continue;
                }
                count++;
            }
        }
        props.setProperty("count", Integer.toString(count));

        var out = new ByteArrayOutputStream();
        props.store(out, "pdf-tapir editable items");
        return out.toByteArray();
    }

    private Map<Integer, List<Annotation>> deserializeAnnotations(byte[] bytes) throws IOException {
        var props = new Properties();
        props.load(new ByteArrayInputStream(bytes));

        int count = parseInt(props.getProperty("count"), 0);
        var byPage = new LinkedHashMap<Integer, List<Annotation>>();
        for (int i = 0; i < count; i++) {
            String prefix = "item." + i + ".";
            int pageIndex = parseInt(props.getProperty(prefix + "page"), -1);
            if (pageIndex < 0) continue;

            Annotation annotation = switch (props.getProperty(prefix + "type", "")) {
                case "text" -> textAnnotation(props, prefix);
                case "checkbox" -> checkboxAnnotation(props, prefix);
                case "image" -> imageAnnotation(props, prefix);
                default -> null;
            };
            if (annotation != null) {
                byPage.computeIfAbsent(pageIndex, ignored -> new ArrayList<>()).add(annotation);
            }
        }
        return byPage;
    }

    private TextAnnotation textAnnotation(Properties props, String prefix) {
        var ta = new TextAnnotation(geom(props, prefix, "x"), geom(props, prefix, "y"),
                geom(props, prefix, "w"), geom(props, prefix, "h"));
        ta.setText(unb64(props.getProperty(prefix + "text", "")));
        ta.setFontSize(parseFloat(props.getProperty(prefix + "fontSize"), 12f));
        ta.setFontColor(props.getProperty(prefix + "fontColor", "#000000"));
        ta.setFontFamily(unb64(props.getProperty(prefix + "fontFamily", b64("System"))));
        return ta;
    }

    private CheckboxAnnotation checkboxAnnotation(Properties props, String prefix) {
        var ca = new CheckboxAnnotation(geom(props, prefix, "x"), geom(props, prefix, "y"),
                geom(props, prefix, "w"), geom(props, prefix, "h"));
        ca.setLabel(unb64(props.getProperty(prefix + "label", "")));
        ca.setChecked(Boolean.parseBoolean(props.getProperty(prefix + "checked", "false")));
        ca.setCheckmarkColor(props.getProperty(prefix + "checkmarkColor", "#000000"));
        return ca;
    }

    private ImageAnnotation imageAnnotation(Properties props, String prefix) {
        var ia = new ImageAnnotation(geom(props, prefix, "x"), geom(props, prefix, "y"),
                geom(props, prefix, "w"), geom(props, prefix, "h"));
        String encoded = props.getProperty(prefix + "imageData", "");
        if (!encoded.isEmpty()) {
            ia.setImageData(Base64.getDecoder().decode(encoded));
            try {
                ia.setFxImage(new javafx.scene.image.Image(new ByteArrayInputStream(ia.getImageData())));
            } catch (Exception ignored) {
                // Canvas will lazy-init from imageData when JavaFX is available.
            }
        }
        return ia;
    }

    private double geom(Properties props, String prefix, String key) {
        return parseDouble(props.getProperty(prefix + key), 0.0);
    }

    private static String b64(String value) {
        return Base64.getEncoder().encodeToString(nullToEmpty(value).getBytes(StandardCharsets.UTF_8));
    }

    private static String unb64(String value) {
        if (value == null || value.isEmpty()) return "";
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value); } catch (Exception ignored) { return fallback; }
    }

    private static float parseFloat(String value, float fallback) {
        try { return Float.parseFloat(value); } catch (Exception ignored) { return fallback; }
    }

    private static double parseDouble(String value, double fallback) {
        try { return Double.parseDouble(value); } catch (Exception ignored) { return fallback; }
    }

    record PackageData(byte[] basePdfBytes, Map<Integer, List<Annotation>> annotationsByPage) {}
}
