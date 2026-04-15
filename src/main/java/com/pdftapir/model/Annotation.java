package com.pdftapir.model;

import java.util.UUID;

/**
 * Abstract base class for all annotations placed on a PDF page.
 * Coordinates are in PDF points with a bottom-left origin; x/y represent
 * the lower-left corner of the annotation's bounding rectangle.
 */
public abstract class Annotation {
    private final String id = UUID.randomUUID().toString();
    private double x;       // PDF pts, from page left
    private double y;       // PDF pts, from page bottom
    private double width;   // PDF pts
    private double height;  // PDF pts

    protected Annotation(double x, double y, double width, double height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException(
                "Annotation dimensions must be non-negative; got width=" + width + ", height=" + height);
        }
        this.x = x; this.y = y; this.width = width; this.height = height;
    }

    /** Returns the unique identifier for this annotation. */
    public String getId()            { return id; }

    /** Returns the x coordinate of the lower-left corner in PDF points. */
    public double getX()             { return x; }

    /** Sets the x coordinate of the lower-left corner in PDF points. */
    public void   setX(double x)     { this.x = x; }

    /** Returns the y coordinate of the lower-left corner in PDF points. */
    public double getY()             { return y; }

    /** Sets the y coordinate of the lower-left corner in PDF points. */
    public void   setY(double y)     { this.y = y; }

    /** Returns the width of the annotation in PDF points. */
    public double getWidth()         { return width; }

    /** Sets the width of the annotation in PDF points. */
    public void   setWidth(double w) { this.width = w; }

    /** Returns the height of the annotation in PDF points. */
    public double getHeight()        { return height; }

    /** Sets the height of the annotation in PDF points. */
    public void   setHeight(double h){ this.height = h; }
}
