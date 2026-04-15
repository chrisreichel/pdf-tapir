package com.pdftapir.command;

/**
 * Represents a reversible operation in the PDF editor.
 *
 * <p>Implementations must be able to both apply ({@link #execute()}) and reverse
 * ({@link #undo()}) the operation so that {@link UndoManager} can maintain a
 * navigable command history.
 */
public interface Command {

    /**
     * Applies the operation.
     */
    void execute();

    /**
     * Reverses the operation, restoring the state to what it was before
     * {@link #execute()} was called.
     */
    void undo();
}
