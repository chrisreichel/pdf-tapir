package com.pdftapir.service;

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
        // PDF y=0 (bottom of page) → canvas y = pageHeight * scale (bottom of canvas area)
        var m = new CoordinateMapper(792f, 1.0);
        assertEquals(792.0, m.pdfYToCanvasTop(0.0, 0.0), 0.001);
    }

    @Test
    void pdfYTopIsCanvasTop() {
        // PDF y=pageHeight (top of page) with height=0 → canvas y = 0 (top of canvas)
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
