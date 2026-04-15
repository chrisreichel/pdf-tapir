package com.pdftapir.command;

/**
 * Generic command for editing a single property on an annotation.
 *
 * <p>Rather than hard-coding a specific property, this command accepts two
 * {@link Runnable} lambdas — one that applies the new value and one that
 * restores the old value — making it reusable for any property type.
 *
 * <p>Example usage:
 * <pre>{@code
 * String oldText = textAnnotation.getText();
 * String newText = "Hello";
 * undoManager.execute(new EditAnnotationCommand(
 *     () -> textAnnotation.setText(newText),
 *     () -> textAnnotation.setText(oldText)
 * ));
 * }</pre>
 */
public class EditAnnotationCommand implements Command {
    private final Runnable applyNew;
    private final Runnable applyOld;

    /**
     * @param applyNew runnable that sets the new value on the annotation
     * @param applyOld runnable that restores the old value on the annotation
     */
    public EditAnnotationCommand(Runnable applyNew, Runnable applyOld) {
        this.applyNew = applyNew;
        this.applyOld = applyOld;
    }

    @Override public void execute() { applyNew.run(); }
    @Override public void undo()    { applyOld.run(); }
}
