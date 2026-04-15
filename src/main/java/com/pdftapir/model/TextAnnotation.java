package com.pdftapir.model;

/**
 * An annotation that renders a text string on a PDF page.
 * Supports configurable font size and color.
 */
public class TextAnnotation extends Annotation {
    private String  text       = "";
    private float   fontSize   = 12f;
    private String  fontColor  = "#000000";
    private String  fontFamily = "System";
    private boolean bold       = false;
    private boolean italic     = false;
    private String  textAlign  = "LEFT";

    /**
     * Creates a TextAnnotation with the given bounding rectangle.
     *
     * @param x      lower-left x in PDF points
     * @param y      lower-left y in PDF points
     * @param width  width in PDF points
     * @param height height in PDF points
     */
    public TextAnnotation(double x, double y, double width, double height) {
        super(x, y, width, height);
    }

    /** Returns the text content of this annotation. */
    public String getText()              { return text; }

    /** Sets the text content of this annotation. */
    public void   setText(String t)      { this.text = t; }

    /** Returns the font size in points. */
    public float  getFontSize()          { return fontSize; }

    /** Sets the font size in points. */
    public void   setFontSize(float s)   { this.fontSize = s; }

    /** Returns the font color as a CSS hex string (e.g. {@code "#000000"}). */
    public String getFontColor()         { return fontColor; }

    /** Sets the font color as a CSS hex string (e.g. {@code "#000000"}). */
    public void   setFontColor(String c) { this.fontColor = c; }

    /** Returns the font family name (e.g. {@code "Arial"}). */
    public String getFontFamily()         { return fontFamily; }

    /** Sets the font family name (e.g. {@code "Arial"}). */
    public void   setFontFamily(String f) { this.fontFamily = f; }

    /** Returns {@code true} if the text is rendered bold. */
    public boolean isBold()              { return bold; }

    /** Sets bold rendering for this annotation. */
    public void    setBold(boolean b)    { this.bold = b; }

    /** Returns {@code true} if the text is rendered italic. */
    public boolean isItalic()            { return italic; }

    /** Sets italic rendering for this annotation. */
    public void    setItalic(boolean i)  { this.italic = i; }

    /** Returns the text alignment: {@code "LEFT"}, {@code "CENTER"}, or {@code "RIGHT"}. */
    public String  getTextAlign()             { return textAlign; }

    /** Sets the text alignment: {@code "LEFT"}, {@code "CENTER"}, or {@code "RIGHT"}. */
    public void    setTextAlign(String align) { this.textAlign = align; }
}
