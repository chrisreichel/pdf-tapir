package com.pdftapir.service;

/**
 * Converts between PDF coordinate space and canvas (screen) coordinate space.
 *
 * <p>PDF coordinates use a bottom-left origin with Y increasing upward, measured in points
 * (1/72 inch). Canvas coordinates use a top-left origin with Y increasing downward, measured
 * in pixels. A scale factor (pixels per point) is applied to all conversions.
 */
public class CoordinateMapper {

    private final float pageHeightPt;
    private final double scale; // pixels per point

    /**
     * Creates a new CoordinateMapper.
     *
     * @param pageHeightPt the height of the PDF page in points
     * @param scale        the scale factor (pixels per point) to apply during conversion
     */
    public CoordinateMapper(float pageHeightPt, double scale) {
        this.pageHeightPt = pageHeightPt;
        this.scale        = scale;
    }

    /**
     * Converts a PDF x coordinate (points) to a canvas x coordinate (pixels).
     *
     * @param pdfX the x position in PDF space (points)
     * @return the x position in canvas space (pixels)
     */
    public double pdfXToCanvas(double pdfX) {
        return pdfX * scale;
    }

    /**
     * Converts the lower-left corner of an annotation in PDF space to the upper-left corner
     * in canvas space.
     *
     * <p>PDF origin is bottom-left; canvas origin is top-left. The formula is:
     * {@code canvasTop = (pageHeight - pdfY - pdfHeight) * scale}
     *
     * @param pdfY      the bottom y of the annotation in PDF space (points)
     * @param pdfHeight the height of the annotation in PDF space (points); pass {@code 0.0}
     *                  when converting a single point
     * @return the top y position of the annotation in canvas space (pixels)
     */
    public double pdfYToCanvasTop(double pdfY, double pdfHeight) {
        return (pageHeightPt - pdfY - pdfHeight) * scale;
    }

    /**
     * Converts a canvas x coordinate (pixels) to a PDF x coordinate (points).
     *
     * @param canvasX the x position in canvas space (pixels)
     * @return the x position in PDF space (points)
     */
    public double canvasXToPdf(double canvasX) {
        return canvasX / scale;
    }

    /**
     * Converts a canvas Y coordinate of a click point to a PDF y coordinate (points).
     *
     * <p>Use {@code pdfHeight=0} when hit-testing a single point.
     *
     * @param canvasY the y position in canvas space (pixels)
     * @return the y position in PDF space (points)
     */
    public double canvasYToPdf(double canvasY) {
        return pageHeightPt - (canvasY / scale);
    }

    /**
     * Converts a PDF dimension (points) to a canvas dimension (pixels).
     *
     * @param pdfDim the dimension in PDF space (points)
     * @return the dimension in canvas space (pixels)
     */
    public double pdfDimToCanvas(double pdfDim) {
        return pdfDim * scale;
    }

    /**
     * Converts a canvas dimension (pixels) to a PDF dimension (points).
     *
     * @param canvasDim the dimension in canvas space (pixels)
     * @return the dimension in PDF space (points)
     */
    public double canvasDimToPdf(double canvasDim) {
        return canvasDim / scale;
    }

    /**
     * Returns the scale factor (pixels per point).
     *
     * @return the scale factor
     */
    public double getScale() {
        return scale;
    }
}
