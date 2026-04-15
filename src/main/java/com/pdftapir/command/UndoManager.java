package com.pdftapir.command;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Manages a bounded undo/redo history of {@link Command} objects.
 *
 * <p>Each call to {@link #execute(Command)} runs the command immediately and
 * pushes it onto the undo stack. If the stack has already reached {@code limit},
 * the oldest entry is silently discarded before the new command is added.
 * Calling {@link #undo()} reverses the most recent command and moves it to the
 * redo stack; {@link #redo()} re-applies it and moves it back to the undo stack.
 * Any call to {@link #execute(Command)} clears the redo stack, because the new
 * command creates a new branch of history.
 */
public class UndoManager {

    private final int            limit;
    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();

    /**
     * Creates a new {@code UndoManager} with the given history limit.
     *
     * @param limit maximum number of commands retained in the undo stack;
     *              the oldest command is dropped when this limit is exceeded
     */
    public UndoManager(int limit) {
        this.limit = limit;
    }

    /**
     * Executes {@code cmd} and records it in the undo history.
     *
     * <p>If the undo stack is already at {@code limit}, the oldest entry is
     * removed before the new command is pushed. The redo stack is always cleared.
     *
     * @param cmd the command to execute; must not be {@code null}
     */
    public void execute(Command cmd) {
        cmd.execute();
        if (undoStack.size() == limit) undoStack.pollLast();
        undoStack.push(cmd);
        redoStack.clear();
    }

    /**
     * Undoes the most recently executed command, if any.
     *
     * <p>Does nothing when the undo stack is empty.
     */
    public void undo() {
        if (undoStack.isEmpty()) return;
        var cmd = undoStack.pop();
        cmd.undo();
        redoStack.push(cmd);
    }

    /**
     * Re-applies the most recently undone command, if any.
     *
     * <p>Does nothing when the redo stack is empty.
     */
    public void redo() {
        if (redoStack.isEmpty()) return;
        var cmd = redoStack.pop();
        cmd.execute();
        undoStack.push(cmd);
    }

    /**
     * Returns {@code true} if there is at least one command that can be undone.
     *
     * @return {@code true} when the undo stack is non-empty
     */
    public boolean canUndo() { return !undoStack.isEmpty(); }

    /**
     * Returns {@code true} if there is at least one command that can be redone.
     *
     * @return {@code true} when the redo stack is non-empty
     */
    public boolean canRedo() { return !redoStack.isEmpty(); }

    /**
     * Clears both the undo and redo history. Use after structural document changes
     * (merge, page removal) that invalidate existing command history.
     */
    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}
