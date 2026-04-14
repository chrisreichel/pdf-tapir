package com.pdfescroto.command;

import com.pdfescroto.model.Annotation;

/**
 * Command that resizes (and optionally repositions) an {@link Annotation}.
 *
 * <p>Both position and dimensions are captured for old and new states because
 * resizing from a top-left handle changes both the x/y origin and the
 * width/height simultaneously.
 */
public class ResizeAnnotationCommand implements Command {
    private final Annotation annotation;
    private final double oldX, oldY, oldW, oldH;
    private final double newX, newY, newW, newH;

    /**
     * @param annotation the annotation to resize
     * @param oldX       original x coordinate in PDF points
     * @param oldY       original y coordinate in PDF points
     * @param oldW       original width in PDF points
     * @param oldH       original height in PDF points
     * @param newX       target x coordinate in PDF points
     * @param newY       target y coordinate in PDF points
     * @param newW       target width in PDF points
     * @param newH       target height in PDF points
     */
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
