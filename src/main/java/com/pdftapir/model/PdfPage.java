package com.pdftapir.model;

import javafx.scene.image.WritableImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a single page in a PDF document, holding its dimensions,
 * the rasterized preview image, and the list of annotations applied to it.
 */
public class PdfPage {
    private final int   pageIndex;
    private final float pageWidthPt;
    private final float pageHeightPt;
    private WritableImage          renderedImage;
    private final List<Annotation> annotations = new ArrayList<>();

    /**
     * Creates a PdfPage for the given zero-based page index with the
     * specified page dimensions in PDF points.
     *
     * @param pageIndex    zero-based index of this page in the document
     * @param pageWidthPt  page width in PDF points
     * @param pageHeightPt page height in PDF points
     */
    public PdfPage(int pageIndex, float pageWidthPt, float pageHeightPt) {
        this.pageIndex    = pageIndex;
        this.pageWidthPt  = pageWidthPt;
        this.pageHeightPt = pageHeightPt;
    }

    /** Returns the zero-based index of this page within the document. */
    public int   getPageIndex()    { return pageIndex; }

    /** Returns the page width in PDF points. */
    public float getPageWidthPt()  { return pageWidthPt; }

    /** Returns the page height in PDF points. */
    public float getPageHeightPt() { return pageHeightPt; }

    /** Returns the rasterized preview image for this page, or {@code null} if not yet rendered. */
    public WritableImage getRenderedImage()               { return renderedImage; }

    /** Sets the rasterized preview image for this page. */
    public void          setRenderedImage(WritableImage i){ this.renderedImage = i; }

    /**
     * Adds an annotation to this page.
     *
     * @param a the annotation to add
     */
    public void             addAnnotation(Annotation a)    { annotations.add(a); }

    /**
     * Removes an annotation from this page.
     *
     * @param a the annotation to remove
     */
    public void             removeAnnotation(Annotation a) { annotations.remove(a); }

    /**
     * Returns an unmodifiable view of the annotations on this page.
     * Use {@link #addAnnotation} and {@link #removeAnnotation} to mutate the list.
     */
    public List<Annotation> getAnnotations()               { return Collections.unmodifiableList(annotations); }
}
