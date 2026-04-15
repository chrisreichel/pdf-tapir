package com.pdftapir.command;

import com.pdftapir.model.Annotation;
import com.pdftapir.model.PdfPage;

/**
 * Command that adds an {@link Annotation} to a {@link PdfPage}.
 * Undoing the command removes the annotation from the page.
 */
public class AddAnnotationCommand implements Command {
    private final PdfPage    page;
    private final Annotation annotation;

    /**
     * @param page       the page to which the annotation will be added
     * @param annotation the annotation to add
     */
    public AddAnnotationCommand(PdfPage page, Annotation annotation) {
        this.page = page;
        this.annotation = annotation;
    }

    @Override public void execute() { page.addAnnotation(annotation); }
    @Override public void undo()    { page.removeAnnotation(annotation); }
}
