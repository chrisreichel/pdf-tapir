package com.pdfescroto.command;

import com.pdfescroto.model.Annotation;
import com.pdfescroto.model.PdfPage;

/**
 * Command that removes an {@link Annotation} from a {@link PdfPage}.
 * Undoing the command re-adds the annotation to the page.
 */
public class DeleteAnnotationCommand implements Command {
    private final PdfPage    page;
    private final Annotation annotation;

    /**
     * @param page       the page from which the annotation will be removed
     * @param annotation the annotation to remove
     */
    public DeleteAnnotationCommand(PdfPage page, Annotation annotation) {
        this.page = page;
        this.annotation = annotation;
    }

    @Override public void execute() { page.removeAnnotation(annotation); }
    @Override public void undo()    { page.addAnnotation(annotation); }
}
