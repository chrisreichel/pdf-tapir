package com.pdfescroto.command;

import com.pdfescroto.model.Annotation;

/**
 * Command that moves an {@link Annotation} from one position to another.
 * Both the old and new coordinates are captured at construction time so
 * the operation is fully reversible.
 */
public class MoveAnnotationCommand implements Command {
    private final Annotation annotation;
    private final double oldX, oldY, newX, newY;

    /**
     * @param annotation the annotation to move
     * @param oldX       original x coordinate in PDF points
     * @param oldY       original y coordinate in PDF points
     * @param newX       target x coordinate in PDF points
     * @param newY       target y coordinate in PDF points
     */
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
