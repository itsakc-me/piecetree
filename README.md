# PieceTree

**PieceTree** is a high-performance Java library for text editors, optimized for Android development. It is an adaptation of the [VSCode PieceTree](https://code.visualstudio.com/blogs/2018/03/23/text-buffer-reimplementation), for short leveraging a piece table data structure with a Red-Black Tree to provide efficient text manipulation. It is designed to handle large documents with fast insertions, deletions, and searches, making it ideal for Android-based text editing applications.

## Key Points
- **Efficient Text Editing**: Likely enables O(log N) operations for insertions and deletions, optimized for Android's memory constraints.
- **Flexible API**: Offers methods using both line/column coordinates and document offsets for user-friendly and efficient text manipulation.
- **Advanced Features**: Supports undo/redo, regex search, snapshots, and EOL normalization, suitable for modern text editors.
- **Android Optimization**: Appears to use memory-efficient buffer management to suit Android's resource limitations.

## Purpose
PieceTree is designed to manage text buffers efficiently in applications like text editors or IDEs. It uses a piece table, where text is split into pieces managed by a Red-Black Tree, ensuring fast operations. The library is tailored for Android, with optimizations like 64KB buffer chunks to minimize memory usage, making it suitable for mobile text editing.

## Peeks
### NOTE: The File used here in order to test is [Russian English Bilingual dictionary](https://github.com/titoBouzout/Dictionaries/blob/master/Russian-English%20Bilingual.dic).

- These Peeks are tested on a AVD (Android Virtual Device), having Ram of 2GiB.
![Peek 1](/peeks/piecetree_peek_1.png)
![Peek 2](/peeks/piecetree_peek_2.png)

- These Peeks is tested on a Real Device, having Ram of 3GiB.
- NOTE: This device is 6years old and laggy, you can't do anything without having lag.
![Peek 3](/peeks/piecetree_peek_3.png)
![Peek 4](/peeks/piecetree_peek_4.png)

- For testing and checking the difference between older version and new version see these, these are the Peeks of version 1.0 PieceTree and above are the Peeks of version 1.1 PieceTree / Latest.
![Peek Comparing 1](/peeks/piecetree_peek_comparing_1.png)
![Peek Comparing 2](/peeks/piecetree_peek_comparing_2.png)

---

## Getting Started
To use PieceTree, include the library in your Android project by cloning the repository and adding the `io.itsakc.piecetree` package to your classpath. The library has no external dependencies, making integration straightforward.

## Basic Usage
```java
package any.demo;

import io.itsakc.piecetree.PieceTree;

public class Example {
    public Example() {
        PieceTree pt = new PieceTree();
        pt.initialize("Initial text");
        pt.insert(1, 1, "Hello, ");
        pt.append("World!");
        System.out.println(pt.text()); // Outputs: Hello, Initial textWorld!
    }
}
```

## Key Features
- **Efficient Operations**: Insert, delete, and replace text with O(log N) time complexity.
- **Undo/Redo**: Supports undoing and redoing changes with operation grouping.
- **Search**: Provides literal and regex-based search with customizable options.
- **Snapshots**: Allows saving and restoring document states.
- **EOL Normalization**: Handles different line endings (LF, CRLF) for cross-platform compatibility.
- **Dual API**: Methods support both line/column and offset-based inputs for flexibility.

---

# Comprehensive PieceTree Documentation

## Introduction
PieceTree is a Android Java library that is an adaptation of the [VSCode PieceTree](https://code.visualstudio.com/blogs/2018/03/23/text-buffer-reimplementation), designed for efficient text buffer management in text editors or IDEs. It uses a piece table data structure, where text is divided into pieces stored in a Red-Black Tree, enabling O(log N) time complexity for operations like insertions, deletions, and searches. The library is optimized for Android, with memory-efficient buffer management to handle large documents on resource-constrained devices.

## Features
The evidence leans toward PieceTree offering a robust set of features for text editing applications:
- **Efficient Text Editing**: Operations like insert, delete, and replace are performed with O(log N) time complexity, where N is the number of pieces, due to the Red-Black Tree implementation.
- **Undo/Redo Support**: The `UndoRedoManager` uses the Command Pattern to track changes, supporting unlimited undo/redo with grouping for complex operations.
- **Search and Replace**: Supports both literal and regex-based searches with options for case sensitivity, word boundaries, and capturing groups.
- **Snapshot Management**: The `PieceTreeSnapshot` class allows capturing and restoring document states, useful for history tracking or auto-save.
- **EOL Normalization**: Handles different end-of-line sequences (e.g., LF, CRLF) with configurable normalization for cross-platform compatibility.
- **Position and Range Management**: The `Position` and `Range` classes enable conversion between line/column coordinates and character offsets, supporting both user-friendly and efficient operations.
- **Memory Optimization**: The `BufferManager` splits original text into 64KB chunks and uses a single growable buffer for new text, optimized for Android's memory constraints.

## Android Optimizations
PieceTree is tailored for Android development, addressing memory limitations:
- **Buffer Management**: The `BufferManager` splits the original document into 64KB buffers and maintains a single growable buffer for new text, reducing memory overhead.
- **Efficient Data Structures**: The Red-Black Tree ensures balanced operations, minimizing computational overhead, which is critical for mobile devices.
- **No External Dependencies**: The library is self-contained, simplifying integration into Android projects.

## Project Structure
The PieceTree library is organized under the `io.itsakc.piecetree` package, with the following key classes:

| **File Name**              | **Description**                                   |
|----------------------------|---------------------------------------------------|
| `PieceTree.java`           | Core class for text management and API.           |
| `BufferManager.java`       | Manages text buffers for memory efficiency.       |
| `UndoRedoManager.java`     | Handles undo and redo operations with grouping.   |
| `RedBlackTree.java`        | Implements the Red-Black Tree for piece ordering. |
| `Position.java`            | Represents a position with line and column.       |
| `Range.java`               | Represents a text range with start and end.       |
| `FindMatch.java`           | Represents a search match with details.           |
| `PieceTreeSnapshot.java`   | Captures document states for snapshots.           |
| `Node.java`                | Defines the node structure for the Red-Black Tree.|

## API Overview
PieceTree provides a flexible API with methods supporting both line/column coordinates (1-based, for user interfaces) and document offsets (0-based, for efficiency).
- NOTE: Any offset in API is 0-based and line/column are 1-based.
- NOTE: API with searching functionalities and doesn't take `wholeWord` parameter, assume it as `true` for the search.

### Initialization
- `PieceTree()`: Creates a new instance.
- `initialize(String initialText)`: Initializes with text.
- `initialize(String initialText, boolean normalizeEOL, EOLNormalization eolNormalization)`: Initializes with text, whether to enable auto `normalize` and set EOL normalization.
- `initialize(File file)`: Loads text from a file.
- `initialize(File file, boolean normalizeEOL, EOLNormalization eolNormalization)`: Loads text from a file, whether to enable auto `normalize` and set EOL normalization.

- `reset()`: Resets the PieceTree to an empty state.

### Text Manipulation
- **Append**:
    - `append(String text)`: Appends text to the document.
- **Insert**:
    - `insert(int lineNumber, int column, String text)`: Inserts at line/column.
    - `insert(int offset, String text)`: Inserts at offset.
- **Delete**:
    - `delete(int startLineNumber, int startColumn, int endLineNumber, int endColumn)`: Deletes a range by line/column.
    - `delete(int startOffset, int endOffset)`: Deletes a range by offset.
- **Replace**:
    - `replace(int startLineNumber, int startColumn, int endLineNumber, int endColumn, String replacement)`: Replaces a range by line/column.
    - `replace(int startOffset, int endOffset, String replacement)`: Replaces a range by offset.
    - `replace(String regex, String replacement`: Replace the first match of the pattern.
    - `replace(String regex, boolean wholeWord, String replacement)`: Replace the first match of the pattern while obeying `wholeWord`.
    - `replace(String query, int startOffset, boolean useRegex, String replacement)`: Replace the first match of query from `startOffset`.
    - `replace(String query, int startOffset, boolean useRegex, boolean wholeWord, String replacement)`: Replace the first match of query from `startOffset` while obeying `wholeWord`.
    - `replaceAll(String regex, String replacement, int maxReplace)`: Replaces all matches of the pattern till `maxReplace`.
    - `replaceAll(String regex, boolean wholeWord, String replacement, int maxReplace)`: Replaces all matches of the pattern till `maxReplace` while obeying `wholeWord`.
    - `replaceAll(String query, int startOffset, boolean useRegex, String replacement, int maxReplace)`: Replaces all matches of the query from `startOffset` till `maxReplace`.
    - `replaceAll(String query, int startOffset, boolean useRegex, boolean wholeWord, String replacement, int maxReplace)`: Replaces all matches of the query from `startOffset` till `maxReplace` while obeying `wholeWord`.

### Content Retrieval
- `text()`: Returns the entire text.
- `text(String eol)`: Returns the entire text with the specified EOL sequence.
- `lineContent(int lineNumber)`: Returns a specific line.
- `linesContent(int startLineNumber, int endLineNumber`: Returns a range of lines.
- `textRange(int startLineNumber, int startColumn, int endLineNumber, int endColumn)`: Retrieves text by line/column.
- `textRange(int start, int end)`: Retrieves text by offset.
- `positionAt(int offset)`: Converts offset to line/column.
- `offsetAt(int lineNumber, int column)`: Converts line/column to offset.

### Search
- `findMatches(String query, int startOffset, boolean useRegex, boolean caseSensitive, String wordSeparators, boolean captureGroups, int maxMatch)`: Finds all matches from `startOffset` till `maxMatch`.
- `findMatches(String query, int startOffset, boolean useRegex, boolean caseSensitive, String wordSeparators, boolean captureGroups, boolean wholeWord, int maxMatch)`: Finds all matches from `startOffset` till `maxMatch` while obeying `wholeWord`.
- `findNext(String query, Position startPos, boolean useRegex, boolean caseSensitive, String wordSeparators, boolean captureGroups)`: Finds next match by position.
- `findNext(String query, Position startPos, boolean useRegex, boolean caseSensitive, String wordSeparators, boolean captureGroups, boolean wholeWord)`: Finds next match by position while obeying `wholeWord`.
- `findNext(String query, int startOffset, boolean useRegex, boolean caseSensitive, String wordSeparators, boolean captureGroups)`: Finds next match by offset.
- `findNext(String query, int startOffset, boolean useRegex, boolean caseSensitive, String wordSeparators, boolean captureGroups, boolean wholeWord)`: Finds next match by offset while obeying `wholeWord`.
- `findPrevious(String query, Position endPos, boolean useRegex, boolean caseSensitive, String wordSeparators, boolean captureGroups)`: Finds previous match by position.
- `findPrevious(String query, Position endPos, boolean useRegex, boolean caseSensitive, String wordSeparators, boolean captureGroups, boolean wholeWord)`: Finds previous match by position while obeying `wholeWord`.
- `findPrevious(String query, int endOffset, boolean useRegex, boolean caseSensitive, String wordSeparators, boolean captureGroups)`: Finds previous match by offset.
- `findPrevious(String query, int endOffset, boolean useRegex, boolean caseSensitive, String wordSeparators, boolean captureGroups, boolean wholeWord)`: Finds previous match by offset while obeying `wholeWord`.

### Undo/Redo
- `getUndoRedoManager()`: Returns the UndoRedoManager.
- `setUndoRedoManager(UndoRedoManager undoRedoManager)`: Sets the UndoRedoManager for current PieceTree.
- `undo()`: Reverts the last operation.
- `redo()`: Reapplies the last undone operation.
- `beginGroup(String description)`: Groups operations for a single undo/redo.
- `endGroup()`: Ends grouping.
- `canUndo()`: Checks if undo is possible.
- `canRedo()`: Checks if redo is possible.
- `getUndoDescription()`: Returns the next undo operation's description, can be helpful in updating UI Elements.
- `getRedoDescription()`: Returns the next redo operation's description, can be helpful in updating UI Elements.
- `clear()`:  Clears the UndoRedoManager for the current PieceTree.
- `getUndoSize()`: Returns the size of undo stack.
- `getRedoSize()`: Returns the size of redo stack.
- `setMaxUndoLevels(int maxLevels)`: Sets the maximum number of undo levels.
- `getMaxUndoLevels()`: Returns the maximum number of undo levels.
- `addUndoRedoListener(UndoRedoManager.UndoRedoListener listener)`: Adds a listener for undo/redo events.
- `removeUndoRedoListener(UndoRedoManager.UndoRedoListener listener)`: Removes a listener from the List of UndoRedoManager.

### Snapshots
- `createSnapshot()`: Captures the current state.
- `restoreSnapshot(PieceTreeSnapshot snapshot)`: Restores a snapshot.

### EOL Management
- `getEOL()`: Returns the current EOL sequence.
- `setEOL(String eol)`: Sets the EOL sequence.
- `isNormalizeEOL()`: Returns whether EOL normalization is enabled.
- `setNormalizeEOL(boolean normalizeEOL)`: Enables or disables EOL normalization.

## Usage Examples

### Basic Example
```java
package any.demo;

import io.itsakc.piecetree.PieceTree;

public class BasicExample {
    public BasicExample() {
        PieceTree pt = new PieceTree();
        pt.initialize("Initial text");
        pt.insert(1, 1, "Hello, ");
        pt.append("World!");
        System.out.println(pt.text()); // Outputs: Hello, Initial textWorld!
    }
}
```

### Advanced Example
```java
package any.demo;

import io.itsakc.piecetree.PieceTree;
import io.itsakc.piecetree.common.FindMatch;
import io.itsakc.piecetree.common.Position;
import java.util.List;

public class AdvancedExample {
    public AdvancedExample() {
        PieceTree pt = new PieceTree();
        pt.initialize("Hello, World!\nThis is a test.");
        
        // Replace using offset
        pt.replace(7, 12, "Universe");
        
        // Search using position
        List<FindMatch> matches = pt.findMatches("test", false, true, "", false);
        for (FindMatch match : matches) {
            System.out.println("Match at offset: " + match.startOffset);
        }
        
        // Create and restore snapshot
        PieceTreeSnapshot snapshot = pt.createSnapshot();
        pt.undo();
        pt.restoreSnapshot(snapshot);
        
        System.out.println(pt.text());
    }
}
```

## Installation
### NOTE: There is no dependency to download it from the internet, at least till now. So, Use the approach specified below.
1. Clone the repository: `git clone https://github.com/itsakc-me/piecetree`.
2. Include this library in your project by adding in your `settings.gradle`, `include ..., ':piecetree'`.
3. Implement project in the module based `build.gradle`, where you want to use it, as `implementation project(':piecetree')`
4. Start importing the necessary classes for your need and you are good to go.

## Dependencies
PieceTree is a standalone and fully built from mostly scratch (only Android Utilities), Java library with no external dependencies, making it easy to integrate into Android projects.

## Contributing
Contributions are welcome! Submit issues or pull requests on the [GitHub repository](https://github.com/itsakc-me/piecetree). Suggestions for improving performance, adding features, or enhancing documentation are appreciated.

## License
PieceTree is licensed under the MIT License. See the [LICENSE](https://github.com/itsakc-me/piecetree/blob/main/LICENSE) file for details.

## Key Citations
- [PieceTree GitHub Repository](https://github.com/itsakc-me/piecetree)
- [VSCode Official Website](https://code.visualstudio.com/)
- [Wikipedia: Piece Table Data Structure](https://en.wikipedia.org/wiki/Piece_table)
- [Wikipedia: Red-Black Tree](https://en.wikipedia.org/wiki/Red%E2%80%93black_tree)

## Inspired
- [VSCode PieceTree](https://code.visualstudio.com/blogs/2018/03/23/text-buffer-reimplementation)
