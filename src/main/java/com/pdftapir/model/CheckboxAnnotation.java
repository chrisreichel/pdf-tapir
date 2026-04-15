package com.pdftapir.model;

/**
 * An annotation that renders a labelled checkbox on a PDF page.
 */
public class CheckboxAnnotation extends Annotation {
    private String  label          = "";
    private boolean checked        = false;
    private String  checkmarkColor = "#000000";
    private boolean borderless     = false;

    /**
     * Creates a CheckboxAnnotation with the given bounding rectangle.
     *
     * @param x      lower-left x in PDF points
     * @param y      lower-left y in PDF points
     * @param width  width in PDF points
     * @param height height in PDF points
     */
    public CheckboxAnnotation(double x, double y, double width, double height) {
        super(x, y, width, height);
    }

    /** Returns the label text displayed next to the checkbox. */
    public String  getLabel()            { return label; }

    /** Sets the label text displayed next to the checkbox. */
    public void    setLabel(String l)    { this.label = l; }

    /** Returns {@code true} if the checkbox is in the checked state. */
    public boolean isChecked()           { return checked; }

    /** Sets the checked state of the checkbox. */
    public void    setChecked(boolean c) { this.checked = c; }

    /** Returns the checkmark color as a CSS hex string (e.g. {@code "#000000"}). */
    public String  getCheckmarkColor()         { return checkmarkColor; }

    /** Sets the checkmark color as a CSS hex string (e.g. {@code "#000000"}). */
    public void    setCheckmarkColor(String c) { this.checkmarkColor = c; }

    /** Returns {@code true} if the border rectangle is hidden, showing only the checkmark. */
    public boolean isBorderless()              { return borderless; }

    /** Sets whether the border rectangle is hidden. */
    public void    setBorderless(boolean b)    { this.borderless = b; }
}
