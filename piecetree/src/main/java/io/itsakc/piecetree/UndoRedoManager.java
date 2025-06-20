/*
 *  @date   : 2025/06/20
 *  @author : itsakc.me (itsakc.me@gmail.com)
 *  https://github.com/itsakc-me/piecetree
 */
/*
 MIT License

 Copyright (c) 2025 itsakc.me (itsakc.me@gmail.com)

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */

package io.itsakc.piecetree;

import java.util.*;

/**
 * UndoRedoManager - A comprehensive undo/redo system for text editors
 * Based on the Command Pattern for encapsulating operations as objects
 * Features:
 * - Unlimited undo/redo operations
 * - Command grouping for complex operations
 * - Memory management with configurable limits
 * - Event notifications for UI updates
 * This class fully incorporate with the {@link PieceTree} class.
 *
 * @see PieceTree
 * @since 1.0
 * @author <a href="https://github.com/itsakc-me">@itsakc.me</a>
 */
public class UndoRedoManager {

    // Stack for undo operations (commands that have been executed)
    private final Stack<Command> undoStack;

    // Stack for redo operations (commands that have been undone)
    private final Stack<Command> redoStack;

    // Maximum number of commands to keep in memory
    private int maxUndoLevels;

    // Flag to indicate if we're currently executing an undo/redo
    private boolean isExecuting;

    // Listeners for undo/redo events
    private final List<UndoRedoListener> listeners;

    // Group nesting level for compound operations
    private int groupLevel;
    private CompositeCommand currentGroup;

    /**
     * Creates a new UndoRedoManager with default settings
     */
    public UndoRedoManager() {
        this(100); // Default: keep 100 undo levels
    }

    /**
     * Creates a new UndoRedoManager with specified undo limit
     * @param maxUndoLevels Maximum number of undo operations to keep
     */
    public UndoRedoManager(int maxUndoLevels) {
        this.undoStack = new Stack<>();
        this.redoStack = new Stack<>();
        this.maxUndoLevels = maxUndoLevels;
        this.isExecuting = false;
        this.listeners = new ArrayList<>();
        this.groupLevel = 0;
        this.currentGroup = null;
    }

    /**
     * Executes a command and adds it to the undo stack
     * @param command The command to execute
     */
    public void executeCommand(Command command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }

        // Execute the command
        command.execute();

        // Add to appropriate stack based on grouping
        if (groupLevel > 0 && currentGroup != null) {
            currentGroup.addCommand(command);
        } else {
            addToUndoStack(command);
        }

        // Clear redo stack since we've executed a new command
        if (!isExecuting) {
            redoStack.clear();
            notifyListeners();
        }
    }

    /**
     * Starts a group of commands that will be treated as a single operation
     * @param description Description of the grouped operation
     */
    public void beginGroup(String description) {
        groupLevel++;
        if (groupLevel == 1) {
            currentGroup = new CompositeCommand(description);
        }
    }

    /**
     * Ends the current command group
     */
    public void endGroup() {
        if (groupLevel <= 0) {
            throw new IllegalStateException("No group to end");
        }

        groupLevel--;
        if (groupLevel == 0 && currentGroup != null) {
            if (!currentGroup.isEmpty()) {
                addToUndoStack(currentGroup);
            }
            currentGroup = null;
            notifyListeners();
        }
    }

    /**
     * Undoes the last command
     * @return true if undo was successful, false if nothing to undo
     */
    public boolean undo() {
        if (!canUndo()) {
            return false;
        }

        isExecuting = true;
        try {
            Command command = undoStack.pop();
            command.undo();
            redoStack.push(command);
            notifyListeners();
            return true;
        } finally {
            isExecuting = false;
        }
    }

    /**
     * Redoes the last undone command
     * @return true if redo was successful, false if nothing to redo
     */
    public boolean redo() {
        if (!canRedo()) {
            return false;
        }

        isExecuting = true;
        try {
            Command command = redoStack.pop();
            command.execute();
            undoStack.push(command);
            notifyListeners();
            return true;
        } finally {
            isExecuting = false;
        }
    }

    /**
     * Checks if undo operation is possible
     * @return true if there are commands to undo
     */
    public boolean canUndo() {
        return !undoStack.isEmpty() && groupLevel == 0;
    }

    /**
     * Checks if redo operation is possible
     * @return true if there are commands to redo
     */
    public boolean canRedo() {
        return !redoStack.isEmpty() && groupLevel == 0;
    }

    /**
     * Gets the description of the next command to be undone
     * @return Description string or null if nothing to undo
     */
    public String getUndoDescription() {
        return canUndo() ? undoStack.peek().getDescription() : null;
    }

    /**
     * Gets the description of the next command to be redone
     * @return Description string or null if nothing to redo
     */
    public String getRedoDescription() {
        return canRedo() ? redoStack.peek().getDescription() : null;
    }

    /**
     * Clears all undo and redo history
     */
    public void clear() {
        undoStack.clear();
        redoStack.clear();
        groupLevel = 0;
        currentGroup = null;
        notifyListeners();
    }

    /**
     * Gets the current number of undo levels available
     * @return Number of commands that can be undone
     */
    public int getUndoSize() {
        return undoStack.size();
    }

    /**
     * Gets the current number of redo levels available
     * @return Number of commands that can be redone
     */
    public int getRedoSize() {
        return redoStack.size();
    }

    /**
     * Sets the maximum number of undo levels to keep
     * @param maxLevels Maximum undo levels (must be > 0)
     */
    public void setMaxUndoLevels(int maxLevels) {
        if (maxLevels <= 0) {
            throw new IllegalArgumentException("Max undo levels must be positive");
        }
        this.maxUndoLevels = maxLevels;
        trimUndoStack();
    }

    /**
     * Gets the maximum number of undo levels
     * @return Maximum undo levels
     */
    public int getMaxUndoLevels() {
        return maxUndoLevels;
    }

    /**
     * Adds a listener for undo/redo events
     * @param listener The listener to add
     */
    public void addUndoRedoListener(UndoRedoListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a listener for undo/redo events
     * @param listener The listener to remove
     */
    public void removeUndoRedoListener(UndoRedoListener listener) {
        listeners.remove(listener);
    }

    /**
     * Private helper to add command to undo stack with size management
     */
    private void addToUndoStack(Command command) {
        undoStack.push(command);
        trimUndoStack();
    }

    /**
     * Private helper to trim undo stack to maximum size
     */
    private void trimUndoStack() {
        while (undoStack.size() > maxUndoLevels) {
            undoStack.remove(0); // Remove oldest command
        }
    }

    /**
     * Private helper to notify all listeners of state changes
     */
    private void notifyListeners() {
        for (UndoRedoListener listener : listeners) {
            listener.undoRedoStateChanged(canUndo(), canRedo(),
                    getUndoDescription(), getRedoDescription());
        }
    }
}

/**
 * Base interface for all commands that can be executed and undone
 */
interface Command {
    /**
     * Executes the command
     */
    void execute();

    /**
     * Undoes the command (reverses its effect)
     */
    void undo();

    /**
     * Gets a human-readable description of the command
     * @return Description string
     */
    String getDescription();
}

/**
 * Composite command that groups multiple commands together
 * Useful for complex operations that should be undone as a single unit
 */
class CompositeCommand implements Command {
    private final List<Command> commands;
    private final String description;

    /**
     * Creates a composite command with a description
     * @param description Description of the grouped operation
     */
    public CompositeCommand(String description) {
        this.commands = new ArrayList<>();
        this.description = description;
    }

    /**
     * Adds a command to this composite
     * @param command The command to add
     */
    public void addCommand(Command command) {
        if (command != null) {
            commands.add(command);
        }
    }

    /**
     * Checks if this composite has any commands
     * @return true if empty, false otherwise
     */
    public boolean isEmpty() {
        return commands.isEmpty();
    }

    @Override
    public void execute() {
        // Execute all commands in order
        for (Command command : commands) {
            command.execute();
        }
    }

    @Override
    public void undo() {
        // Undo all commands in reverse order
        for (int i = commands.size() - 1; i >= 0; i--) {
            commands.get(i).undo();
        }
    }

    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Gets the number of commands in this composite
     * @return Number of commands
     */
    public int size() {
        return commands.size();
    }
}

/**
 * Listener interface for undo/redo state changes
 */
interface UndoRedoListener {
    /**
     * Called when the undo/redo state changes
     * @param canUndo Whether undo is currently possible
     * @param canRedo Whether redo is currently possible
     * @param undoDescription Description of next undo operation (or null)
     * @param redoDescription Description of next redo operation (or null)
     */
    void undoRedoStateChanged(boolean canUndo, boolean canRedo,
                              String undoDescription, String redoDescription);
}

// Example concrete commands for text editing operations

/**
 * Command for inserting text at a specific position
 */
class InsertTextCommand implements Command {
    private final PieceTree pieceTree;
    private final int position;
    private final String text;

    public InsertTextCommand(PieceTree pieceTree, int position, String text) {
        this.pieceTree = pieceTree;
        this.position = position;
        this.text = text;
    }

    @Override
    public void execute() {
        pieceTree.doInsert(position, text);
    }

    @Override
    public void undo() {
        pieceTree.doDelete(position, position + text.length());
    }

    @Override
    public String getDescription() {
        return "Inserted Text :- Pos = " + position + " | Text = " + text;
    }
}

/**
 * Command for deleting text from a specific range
 */
class DeleteTextCommand implements Command {
    private final PieceTree pieceTree;
    private final int startPosition;
    private final int endPosition;
    private String deletedText; // Store for undo

    public DeleteTextCommand(PieceTree pieceTree, int startPosition, int endPosition) {
        this.pieceTree = pieceTree;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }

    @Override
    public void execute() {
        // Store the text being deleted for undo
        deletedText = pieceTree.textRange(startPosition, endPosition);
        pieceTree.doDelete(startPosition, endPosition);
    }

    @Override
    public void undo() {
        if (deletedText != null) {
            pieceTree.doInsert(startPosition, deletedText);
        }
    }

    @Override
    public String getDescription() {
        return "Deleted Text :- Pos = start - " + startPosition + ", end - " + endPosition + " | Text = " + deletedText;
    }
}

/**
 * Command for replacing text in a specific range
 */
class ReplaceTextCommand implements Command {
    private final PieceTree pieceTree;
    private final int startPosition;
    private final int endPosition;
    private final String newText;
    private String originalText; // Store for undo

    public ReplaceTextCommand(PieceTree pieceTree, int startPosition,
                              int endPosition, String newText) {
        this.pieceTree = pieceTree;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.newText = newText;
    }

    @Override
    public void execute() {
        // Store the original text for undo
        originalText = pieceTree.textRange(startPosition, endPosition);
        pieceTree.doReplace(startPosition, endPosition, newText);
    }

    @Override
    public void undo() {
        if (originalText != null) {
            pieceTree.doReplace(startPosition, startPosition + newText.length(), originalText);
        }
    }

    @Override
    public String getDescription() {
        return "Replaced Text :- Pos = start - " + startPosition + ", end - " + endPosition + " | Text = " + newText;
    }
}