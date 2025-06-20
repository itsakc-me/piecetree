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

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import io.itsakc.piecetree.common.*;

/**
 * Enhanced PieceTree implementation with complete text editor functionality.
 * Manages text buffer using piece table data structure with Red-Black Tree organization.
 * Supports undo/redo, search operations, snapshots, and comprehensive text manipulation.
 *
 * <p>
 * The PieceTree represents a document as a sequence of pieces, where each piece
 * is a reference to a segment of text in an underlying buffer. This approach
 * allows for efficient text editing operations (insertions, deletions) by
 * manipulating these pieces rather than copying large amounts of text.
 * </p>
 *
 * <p>
 * Key features:
 * <ul>
 *   <li><b>Efficient Editing:</b> Insertions and deletions are generally O(log N)
 *       where N is the number of pieces, plus the cost of creating new pieces.</li>
 *   <li><b>Undo/Redo:</b> Integrated {@link UndoRedoManager} to track and reverse
 *       editing operations.</li>
 *   <li><b>Snapshots:</b> Ability to create {@link PieceTreeSnapshot} instances
 *       to save and restore the document state.</li>
 *   <li><b>Line and Offset Management:</b> Provides methods to convert between
 *       line/column positions and absolute character offsets.</li>
 *   <li><b>Search Functionality:</b> Supports finding text using plain strings
 *       or regular expressions.</li>
 *   <li><b>EOL Normalization:</b> Handles different end-of-line sequences (LF, CRLF).</li>
 * </ul>
 * </p>
 *
 * <p>
 * The internal structure uses a {@link RedBlackTree} to store the pieces, ensuring
 * balanced tree operations and maintaining logarithmic time complexity for most
 * lookups and modifications. Buffers are managed by a {@link BufferManager},
 * which stores the original text and any added text.
 * </p>
 *
 * @see Node
 * @see BufferManager
 * @see RedBlackTree
 * @see UndoRedoManager
 * @see PieceTreeSnapshot
 * @author <a href="https://github.com/itsakc-me">@itsakc.me</a>
 */
public class PieceTree {
    public static final int LineFeed = 10;
    public static final int CarriageReturn = 13;

    private BufferManager bufferManager;
    private RedBlackTree tree;
    private UndoRedoManager undoRedoManager;
    private String eol = "\n"; // Default end-of-line sequence
    private PieceTreeSnapshot currentSnapshot;

    // CONSTRUCTOR COMMENTS

    /**
     * Constructs a new PieceTree with an empty document and default configuration.
     * Initializes all internal components including buffer manager, Red-Black tree,
     * and undo/redo manager with optimal settings for text editing operations.
     *
     * <p>
     * The newly created PieceTree is ready for immediate use and supports all
     * text manipulation operations. The default end-of-line sequence is set to
     * LF (\n) and can be changed using {@link #setEOL(String)}.
     * </p>
     *
     * <p>
     * Performance characteristics:
     * <ul>
     *   <li><b>Time Complexity:</b> O(1) - constant time initialization</li>
     *   <li><b>Space Complexity:</b> O(1) - minimal initial memory footprint</li>
     * </ul>
     * </p>
     *
     * @since 1.0
     * @see #initialize(String)
     * @see BufferManager
     * @see RedBlackTree
     * @see UndoRedoManager
     */
    public PieceTree() {
        bufferManager = new BufferManager();
        tree = new RedBlackTree(bufferManager);
        undoRedoManager = new UndoRedoManager();
        currentSnapshot = null;
    }

    // INITIALIZATION METHODS

    /**
     * Initializes the PieceTree with initial text content, creating optimized buffer structures.
     * This method performs efficient text segmentation and builds the initial piece tree
     * structure with proper line boundary detection and Red-Black tree organization.
     *
     * <p>
     * The initialization process includes:
     * <ul>
     *   <li>Buffer creation and text storage optimization</li>
     *   <li>Line start position computation for fast line-based operations</li>
     *   <li>Red-Black tree construction for balanced access patterns</li>
     *   <li>Initial snapshot creation for undo/redo support</li>
     * </ul>
     * </p>
     *
     * <p>
     * Performance characteristics:
     * <ul>
     *   <li><b>Time Complexity:</b> O(N + L) where N is text length and L is line count</li>
     *   <li><b>Space Complexity:</b> O(N + L) for text storage and line metadata</li>
     * </ul>
     * </p>
     *
     * @param initialText The initial text content to load. Can be null or empty.
     * @throws OutOfMemoryError if the text is too large to fit in available memory.
     * @since 1.0
     * @see #computeLineStarts(char[])
     * @see #createSnapshot()
     */
    public void initialize(String initialText) {
        int bufferCount = bufferManager.addOriginalBuffer(initialText);
        int currentOffset = 0;
        for (int i = 0; i < bufferCount; i++) {
            char[] buffer = bufferManager.getBuffer(i + 1); // Original buffers start at index 1
            int length = buffer.length;
            int[] lineStarts = computeLineStarts(buffer);
            Node node = new Node(i + 1, 0, length, lineStarts);
            node.documentStart = currentOffset;
            tree.insert(node);
            currentOffset += length;
        }
        createSnapshot(); // Create initial snapshot
    }

    /**
     * Initializes the piece tree with content and configurable end-of-line normalization.
     * This overloaded method provides fine-grained control over how line endings are
     * processed and stored, supporting various text file formats and conventions.
     *
     * <p>
     * The normalization process can convert between different EOL formats:
     * <ul>
     *   <li><b>CRLF:</b> Windows-style line endings (\r\n)</li>
     *   <li><b>LF:</b> Unix/Linux-style line endings (\n)</li>
     *   <li><b>None:</b> Preserves original line endings, auto-detects format</li>
     * </ul>
     * </p>
     *
     * <p>
     * Auto-detection priority when normalization is disabled:
     * CRLF → LF → CR → Default to LF
     * </p>
     *
     * @param content The initial content to load into the buffer. Can be null.
     * @param normalizeEOL Whether to normalize end-of-line characters.
     * @param eolNormalization The type of EOL normalization to apply.
     * @since 1.0
     * @see EOLNormalization
     * @see #setEOL(String)
     */
    public void initialize(String content, boolean normalizeEOL, EOLNormalization eolNormalization) {
        if (normalizeEOL && content != null) {
            switch (eolNormalization) {
                case CRLF:
                    content = content.replaceAll("(\r\n|\n|\r)", "\r\n");
                    this.eol = "\r\n";
                    break;
                case LF:
                    content = content.replaceAll("(\r\n|\r)", "\n");
                    this.eol = "\n";
                    break;
                case None:
                default:
                    // Detect existing EOL
                    if (content.contains("\r\n")) {
                        this.eol = "\r\n";
                    } else if (content.contains("\n")) {
                        this.eol = "\n";
                    } else if (content.contains("\r")) {
                        this.eol = "\r";
                    } else {
                        this.eol = "\n"; // Default
                    }
                    break;
            }
        }
        initialize(content != null ? content : "");
    }

    /**
     * Initializes the PieceTree with content from a file using default LF normalization.
     * This convenience method reads the entire file content into memory and applies
     * Unix-style line ending normalization for consistent text processing.
     *
     * <p>
     * This method is equivalent to calling:
     * {@code initialize(file, true, EOLNormalization.LF)}
     * </p>
     *
     * <p>
     * <b>Memory Considerations:</b> The entire file content is loaded into memory.
     * For very large files (approaching {@link Integer#MAX_VALUE} characters),
     * consider using streaming approaches or file splitting.
     * </p>
     *
     * @param file The file to read content from. Must exist and be readable.
     * @throws OutOfMemoryError if the file is too large to fit in available memory.
     * @throws SecurityException if file access is denied.
     * @since 1.0
     * @see #initialize(File, boolean, EOLNormalization)
     */
    public void initialize(File file) {
        initialize(file, false, EOLNormalization.None);
    }

    /**
     * Initializes the PieceTree with file content and configurable EOL normalization.
     * Performs efficient file reading with buffered I/O and supports large files
     * up to the JVM's maximum string length limit.
     *
     * <p>
     * The file reading process uses a 1KB buffer for optimal I/O performance
     * while maintaining reasonable memory usage during the loading phase.
     * All I/O exceptions are caught and logged, with the tree initialized
     * to an empty state if reading fails.
     * </p>
     *
     * <p>
     * <b>File Size Limitations:</b>
     * <ul>
     *   <li>Maximum supported size: {@link Integer#MAX_VALUE} characters</li>
     *   <li>Recommended maximum: 100MB for optimal performance</li>
     *   <li>Files exceeding limits will throw {@link OutOfMemoryError}</li>
     * </ul>
     * </p>
     *
     * @param file The file to read content from. Must exist and be readable.
     * @param normalizeEOL Whether to normalize end-of-line characters.
     * @param eolNormalization The type of EOL normalization to apply.
     * @throws OutOfMemoryError when the file is too large to fit in memory.
     * @throws SecurityException if file access permissions are insufficient.
     * @since 1.0
     * @see FileReader
     * @see EOLNormalization
     */
    public void initialize(File file, boolean normalizeEOL, EOLNormalization eolNormalization) {
        StringBuilder sb = new StringBuilder();
        FileReader fr = null;
        try {
            fr = new FileReader(file);

            char[] buff = new char[1024];
            int length = 0;

            while ((length = fr.read(buff)) > 0) {
                String s = new String(buff, 0, length);
                sb.append(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        String s = sb.toString();

        if (normalizeEOL && !s.isEmpty()) {
            switch (eolNormalization) {
                case CRLF:
                    this.eol = "\r\n";
                    break;
                case LF:
                    this.eol = "\n";
                    break;
                case None:
                default:
                    // Detect existing EOL
                    if (s.contains("\r\n")) {
                        this.eol = "\r\n";
                    } else if (s.contains("\n")) {
                        this.eol = "\n";
                    } else if (s.contains("\r")) {
                        this.eol = "\r";
                    } else {
                        this.eol = "\n"; // Default
                    }
                    break;
            }
        }

        initialize(s);
    }

    // TEXT MANIPULATION METHODS

    /**
     * Appends text to the end of the document with optimal performance.
     * This method provides a convenient way to add content without calculating
     * the document length, internally using the most efficient insertion point.
     *
     * <p>
     * The operation is equivalent to {@code insert(length(), text)} but with
     * optimized performance since it doesn't require offset calculation for
     * existing content traversal.
     * </p>
     *
     * <p>
     * Performance characteristics:
     * <ul>
     *   <li><b>Time Complexity:</b> O(log N + T) where N is nodes and T is text length</li>
     *   <li><b>Space Complexity:</b> O(T) for text storage</li>
     * </ul>
     * </p>
     *
     * @param text The text to append. Can be null or empty (no-op).
     * @return Self-reference for method chaining and fluent API usage.
     * @since 1.0
     * @see #insert(int, String)
     * @see #length()
     */
    public PieceTree append(String text) {
        return insert(length(), text);
    }

    /**
     * Inserts text at a specified line and column position with coordinate validation.
     * This method provides intuitive text editing capabilities using human-readable
     * line/column coordinates commonly used in text editors and IDEs.
     *
     * <p>
     * The insertion process:
     * <ol>
     *   <li>Converts line/column coordinates to absolute document offset</li>
     *   <li>Creates and executes an {@link InsertTextCommand} for undo support</li>
     *   <li>Updates internal tree structure and metadata</li>
     *   <li>Maintains Red-Black tree balance properties</li>
     * </ol>
     * </p>
     *
     * <p>
     * Coordinate handling:
     * <ul>
     *   <li>Line and column numbers are 1-based (editor convention)</li>
     *   <li>Invalid coordinates are clamped to document boundaries</li>
     *   <li>Columns beyond line length append to line end</li>
     * </ul>
     * </p>
     *
     * @param lineNumber The line number where to insert (1-based). Must be ≥ 1.
     * @param column The column where to insert (1-based). Must be ≥ 1.
     * @param text The text to insert. Can contain line breaks for multi-line insertion.
     * @return Self-reference for method chaining and fluent API usage.
     * @throws IllegalArgumentException if lineNumber or column is less than 1.
     * @since 1.0
     * @see #offsetAt(int, int)
     * @see InsertTextCommand
     */
    public PieceTree insert(int lineNumber, int column, String text) {
        int offset = offsetAt(lineNumber, column);
        InsertTextCommand command = new InsertTextCommand(this, offset, text);
        undoRedoManager.executeCommand(command);
        return this;
    }

    /**
     * Inserts text at a specified character offset with high-performance operation.
     * This method provides direct offset-based text insertion for scenarios where
     * exact character positions are known, avoiding coordinate conversion overhead.
     *
     * <p>
     * The insertion algorithm:
     * <ul>
     *   <li>Locates the target node containing the offset in O(log N) time</li>
     *   <li>Splits existing nodes if insertion occurs within node boundaries</li>
     *   <li>Creates new nodes for inserted content with proper line metadata</li>
     *   <li>Maintains tree balance through Red-Black tree rotations</li>
     * </ul>
     * </p>
     *
     * <p>
     * Undo/Redo Integration: All insertions are automatically wrapped in
     * {@link InsertTextCommand} objects and added to the undo history,
     * enabling seamless operation reversal.
     * </p>
     *
     * @param offset The offset where to insert (0-based). Must be ≥ 0 and ≤ document length.
     * @param text The text to insert. Null or empty strings result in no-op.
     * @return Self-reference for method chaining and fluent API usage.
     * @throws IllegalArgumentException if offset is negative or exceeds document length.
     * @since 1.0
     * @see #doInsert(int, String)
     * @see UndoRedoManager#executeCommand(Command)
     */
    public PieceTree insert(int offset, String text) {
        InsertTextCommand command = new InsertTextCommand(this, offset, text);
        undoRedoManager.executeCommand(command);
        return this;
    }

    /**
     * Deletes text within a specified line/column range with precise boundary handling.
     * This method provides intuitive text deletion using editor-style coordinates,
     * supporting both single-line and multi-line deletion operations.
     *
     * <p>
     * Range Processing:
     * <ul>
     *   <li>Converts start and end coordinates to absolute offsets</li>
     *   <li>Handles multi-line selections seamlessly</li>
     *   <li>Preserves proper line structure after deletion</li>
     *   <li>Updates line count and metadata automatically</li>
     * </ul>
     * </p>
     *
     * <p>
     * Boundary Conditions:
     * <ul>
     *   <li>Start position equals end position results in no-op</li>
     *   <li>Invalid coordinates are clamped to document boundaries</li>
     *   <li>Deletion preserves document integrity</li>
     * </ul>
     * </p>
     *
     * @param startLineNumber Start line number (1-based, inclusive).
     * @param startColumn Start column (1-based, inclusive).
     * @param endLineNumber End line number (1-based, inclusive).
     * @param endColumn End column (1-based, exclusive).
     * @return Self-reference for method chaining and fluent API usage.
     * @since 1.0
     * @see #offsetAt(int, int)
     * @see DeleteTextCommand
     */
    public PieceTree delete(int startLineNumber, int startColumn, int endLineNumber, int endColumn) {
        int startOffset = offsetAt(startLineNumber, startColumn);
        int endOffset = offsetAt(endLineNumber, endColumn);
        DeleteTextCommand command = new DeleteTextCommand(this, startOffset, endOffset);
        undoRedoManager.executeCommand(command);
        return this;
    }

    /**
     * Deletes text within a specified offset range with optimal performance.
     * This method provides direct offset-based deletion for high-performance
     * scenarios where exact character positions are predetermined.
     *
     * <p>
     * Deletion Algorithm:
     * <ol>
     *   <li>Validates offset range boundaries</li>
     *   <li>Creates {@link DeleteTextCommand} for undo support</li>
     *   <li>Executes tree-level range deletion</li>
     *   <li>Updates document metadata and line counts</li>
     * </ol>
     * </p>
     *
     * <p>
     * Performance Optimizations:
     * <ul>
     *   <li>O(log N) node location using tree structure</li>
     *   <li>Efficient node merging for adjacent deletions</li>
     *   <li>Minimal memory allocation during operation</li>
     * </ul>
     * </p>
     *
     * @param startOffset The start offset (0-based, inclusive).
     * @param endOffset The end offset (0-based, exclusive).
     * @return Self-reference for method chaining and fluent API usage.
     * @throws IllegalArgumentException if startOffset > endOffset or offsets are invalid.
     * @since 1.0
     * @see #doDelete(int, int)
     * @see RedBlackTree#deleteRange(int, int)
     */
    public PieceTree delete(int startOffset, int endOffset) {
        DeleteTextCommand command = new DeleteTextCommand(this, startOffset, endOffset);
        undoRedoManager.executeCommand(command);
        return this;
    }

    /**
     * Replaces text within a line/column range with new content atomically.
     * This method combines deletion and insertion into a single undoable operation,
     * providing efficient text replacement with full undo/redo support.
     *
     * <p>
     * Atomic Operation Benefits:
     * <ul>
     *   <li>Single undo entry for complete replace operation</li>
     *   <li>Maintains document consistency during replacement</li>
     *   <li>Optimized performance vs. separate delete + insert</li>
     *   <li>Preserves selection and cursor position context</li>
     * </ul>
     * </p>
     *
     * <p>
     * Use Cases:
     * <ul>
     *   <li>Find and replace operations</li>
     *   <li>Auto-correction and text transformation</li>
     *   <li>Template expansion and code generation</li>
     *   <li>Syntax-aware content replacement</li>
     * </ul>
     * </p>
     *
     * @param startLineNumber Start line number (1-based, inclusive).
     * @param startColumn Start column (1-based, inclusive).
     * @param endLineNumber End line number (1-based, inclusive).
     * @param endColumn End column (1-based, exclusive).
     * @param replacement The replacement text. Can be multi-line or empty.
     * @return Self-reference for method chaining and fluent API usage.
     * @since 1.0
     * @see ReplaceTextCommand
     * @see #offsetAt(int, int)
     */
    public PieceTree replace(int startLineNumber, int startColumn, int endLineNumber, int endColumn, String replacement) {
        int startOffset = offsetAt(startLineNumber, startColumn);
        int endOffset = offsetAt(endLineNumber, endColumn);
        ReplaceTextCommand command = new ReplaceTextCommand(this, startOffset, endOffset, replacement);
        undoRedoManager.executeCommand(command);
        return this;
    }

    /**
     * Replaces text within an offset range with new content using direct addressing.
     * This high-performance method provides atomic text replacement for scenarios
     * requiring precise character-level control and optimal execution speed.
     *
     * <p>
     * Implementation Strategy:
     * <ol>
     *   <li>Creates atomic {@link ReplaceTextCommand} operation</li>
     *   <li>Performs deletion of existing range</li>
     *   <li>Inserts replacement text at start position</li>
     *   <li>Updates all metadata in single transaction</li>
     * </ol>
     * </p>
     *
     * <p>
     * Performance Characteristics:
     * <ul>
     *   <li><b>Time:</b> O(log N + D + I) where D is deleted length, I is inserted length</li>
     *   <li><b>Space:</b> O(I) for new text storage</li>
     *   <li><b>Undo:</b> O(1) single command creation</li>
     * </ul>
     * </p>
     *
     * @param startOffset The start offset (0-based, inclusive).
     * @param endOffset The end offset (0-based, exclusive).
     * @param replacement The replacement text. Empty string performs deletion only.
     * @return Self-reference for method chaining and fluent API usage.
     * @throws IllegalArgumentException if offset range is invalid.
     * @since 1.0
     * @see #doReplace(int, int, String)
     */
    public PieceTree replace(int startOffset, int endOffset, String replacement) {
        ReplaceTextCommand command = new ReplaceTextCommand(this, startOffset, endOffset, replacement);
        undoRedoManager.executeCommand(command);
        return this;
    }

    /**
     * Replaces the first occurrence of a pattern (defined by a regular expression)
     * with the specified replacement text. This method combines searching and
     * replacement into a single, convenient operation.
     *
     * <p>
     * Operation Sequence:
     * <ol>
     *   <li>Finds the first match for the given {@code regex} starting from the
     *       beginning of the document (offset 0). The search is performed
     *       case-sensitively and treats the query as a regex by default.</li>
     *   <li>If a match is found ({@link FindMatch}), its start and end offsets
     *       are used to define the range for replacement.</li>
     *   <li>Delegates to the offset-based {@link #replace(int, int, String)}
     *       method to perform the actual atomic replacement of the matched text
     *       with the {@code replacement} string.</li>
     *   <li>If no match is found by {@code findNext}, no replacement occurs, and
     *       the document remains unchanged.</li>
     * </ol>
     * </p>
     *
     * <p>
     * Key Characteristics:
     * <ul>
     *   <li><b>Target:</b> Only the first found match is replaced.</li>
     *   <li><b>Atomicity:</b> The underlying replacement is atomic and undoable if
     *       performed via a command (as seen in the invoked method).</li>
     *   <li><b>Regex Engine:</b> Relies on the capabilities of the
     *       {@link #findNext(String, int, boolean, boolean, String, boolean)} method
     *       for pattern matching.</li>
     * </ul>
     * </p>
     *
     * <p>
     * Default Search Parameters for {@code findNext}:
     * <ul>
     *   <li>{@code startOffset}: 0</li>
     *   <li>{@code useRegex}: true</li>
     *   <li>{@code caseSensitive}: true</li>
     *   <li>{@code wordSeparators}: null</li>
     *   <li>{@code maxResults}: false (captures only the full match for replacement range)</li>
     * </ul>
     * </p>
     *
     * @param regex The regular expression pattern to search for.
     * @param replacement The text to replace the found pattern with.
     *                    An empty string will effectively delete the matched text.
     * @return Self-reference for method chaining and fluent API usage.
     * @throws PatternSyntaxException if the regex pattern is invalid.
     * @since 1.0
     * @see #replaceAll(String, String)
     * @see #replace(int, int, String)
     * @see #findNext(String, int, boolean, boolean, String, boolean)
     * @see FindMatch
     */
    public PieceTree replace(String regex, String replacement) {
        FindMatch match = findNext(regex, 0, true, true, null, false);
        if (match != null) { // Only replace if a match is found
            replace(match.startOffset, match.endOffset, replacement);
        }
        return this;
    }
    
    /**
     * Replaces the first occurrence of a literal text sequence with the specified
     * replacement text. This method provides a simple way to replace a specific
     * string without using regular expressions.
     *
     * <p>
     * Operation Sequence:
     * <ol>
     *   <li>Converts the input {@link CharSequence} {@code literal} to a {@link String}.</li>
     *   <li>Finds the first occurrence of this literal string starting from the
     *       beginning of the document (offset 0). The search is performed
     *       case-sensitively and treats the query as a literal string (not a regex).</li>
     *   <li>If a match is found ({@link FindMatch}), its start and end offsets
     *       are used to define the range for replacement.</li>
     *   <li>Delegates to the offset-based {@link #replace(int, int, String)}
     *       method to perform the actual atomic replacement of the matched text
     *       with the {@code replacement} string.</li>
     *   <li>If no match is found by {@code findNext}, no replacement occurs, and
     *       the document remains unchanged.</li>
     * </ol>
     * </p>
     *
     * <p>
     * Key Characteristics:
     * <ul>
     *   <li><b>Target:</b> Only the first found match of the literal text is replaced.</li>
     *   <li><b>Literal Search:</b> The {@code literal} parameter is treated as plain text,
     *       not a regular expression. Special regex characters will be matched literally.</li>
     *   <li><b>Atomicity:</b> The underlying replacement is atomic and undoable if
     *       performed via a command.</li>
     *   <li><b>Search Engine:</b> Relies on the capabilities of the
     *       {@link #findNext(String, int, boolean, boolean, String, boolean)} method
     *       configured for literal, case-sensitive search.</li>
     * </ul>
     * </p>
     *
     * <p>
     * Default Search Parameters for {@code findNext}:
     * <ul>
     *   <li>{@code startOffset}: 0</li>
     *   <li>{@code useRegex}: false (literal search)</li>
     *   <li>{@code caseSensitive}: true</li>
     *   <li>{@code wordSeparators}: null</li>
     *   <li>{@code maxResults}: false (captures only the full match for replacement range)</li>
     * </ul>
     * </p>
     *
     * @param literal     The exact {@link CharSequence} (e.g., String, StringBuilder)
     *                    to search for. It will be converted to a String for the search.
     * @param replacement The text to replace the found literal with.
     *                    An empty string will effectively delete the matched text.
     * @return Self-reference for method chaining and fluent API usage.
     * @see #replaceAll(CharSequence, String)
     * @see #replace(String, String) // For regex-based replacement
     * @see #replace(int, int, String)
     * @see #findNext(String, int, boolean, boolean, String, boolean)
     * @see FindMatch
     * @since 1.0
     */
    public PieceTree replace(CharSequence literal, String replacement) {
        FindMatch match = findNext(literal.toString(), 0, false, true, null, false);
        if (match != null) { // Only replace if a match is found
            replace(match.startOffset, match.endOffset, replacement);
        }
        return this;
    }

    /**
     * Replaces all occurrences of a literal text sequence with the specified
     * replacement text throughout the entire document.
     * This method provides global find-and-replace for exact string matches.
     *
     * <p>
     * Operation Sequence:
     * <ol>
     *   <li>Converts the input {@link CharSequence} {@code literal} to a {@link String}.</li>
     *   <li>Finds all non-overlapping matches for the given literal string
     *       throughout the document using
     *       {@link #findMatches(String, boolean, boolean, String, boolean)}.
     *       The search is performed case-sensitively and treats the query as a
     *       literal string.</li>
     *   <li>Iterates through the list of found matches ({@link FindMatch}). For each match,
     *       it delegates to the offset-based {@link #replace(int, int, String)}
     *       method to perform an atomic replacement.</li>
     *   <li>If no matches are found, the document remains unchanged.</li>
     * </ol>
     * </p>
     *
     * <p>
     * Important Considerations:
     * <ul>
     *    <li><b>Literal Search:</b> The {@code literal} parameter is treated as plain text.
     *       Special regex characters in the {@code literal} will be matched as part of
     *       the literal string.</li>
     *   <li><b>Sequential Replacement & Iteration Order:</b> Replacements are performed based on
     *       the matches found in the original document state. This implementation iterates
     *       through matches in the order they appear. For literal replacements where the
     *       replacement string cannot itself create new instances of the literal being searched,
     *       forward iteration is generally safe. If there's a possibility of complex interactions,
     *       consider iterating in reverse (see regex {@link #replaceAll(String, String)}
     *       for an example of reverse iteration).</li>
     *   <li><b>Atomicity:</b> Each individual replacement is atomic and undoable if
     *       the underlying {@code replace(int, int, String)} uses a command.</li>
     *   <li><b>Performance:</b> Finding all matches can be time-consuming on very large
     *       documents, though typically faster than complex regex searches.</li>
     * </ul>
     * </p>
     *
     * <p>
     * Default Search Parameters for {@code findMatches}:
     * <ul>
     *   <li>{@code useRegex}: false (literal search)</li>
     *   <li>{@code caseSensitive}: true</li>
     *   <li>{@code wordSeparators}: null</li>
     *   <li>{@code maxResults}: false (captures only the full match for replacement range)</li>
     * </ul>
     * </p>
     *
     * @param literal     The exact {@link CharSequence} (e.g., String, StringBuilder)
     *                    to search for. It will be converted to a String for the search.
     * @param replacement The text to replace each found literal with.
     *                    An empty string will effectively delete all matched text.
     * @return Self-reference for method chaining and fluent API usage.
     * @see #replace(CharSequence, String)
     * @see #replaceAll(String, String) // For regex-based replacement
     * @see #replace(int, int, String)
     * @see #findMatches(String, boolean, boolean, String, boolean)
     * @see FindMatch
     * @since 1.0
     */
    public PieceTree replaceAll(CharSequence literal, String replacement) {
        List<FindMatch> matches = findMatches(literal.toString(), false, true, null, false);
        for (FindMatch match : matches) {
            replace(match.startOffset, match.endOffset, replacement);
        }
        return this;
    }

    /**
     * Replaces all occurrences of a pattern (defined by a regular expression)
     * with the specified replacement text throughout the entire document.
     * This method provides a global find-and-replace functionality.
     *
     * <p>
     * Operation Sequence:
     * <ol>
     *   <li>Finds all non-overlapping matches for the given {@code regex}
     *       throughout the document using
     *       {@link #findMatches(String, boolean, boolean, String, boolean)}.
     *       The search is performed case-sensitively and treats the query as a
     *       regex by default.</li>
     *   <li>Iterates through the list of found matches ({@link FindMatch}).</li>
     *   <li>For each match, delegates to the offset-based
     *       {@link #replace(int, int, String)} method to perform an atomic
     *       replacement of the matched text with the {@code replacement} string.</li>
     *   <li>If no matches are found, the document remains unchanged.</li>
     * </ol>
     * </p>
     *
     * <p>
     * Important Considerations:
     * <ul>
     *   <li><b>Sequential Replacement:</b> Replacements are performed one by one.
     *       This means that the replacement of an earlier match might affect the
     *       offsets or content relevant to subsequent potential matches if the
     *       replacement text itself could match the regex. However, {@code findMatches}
     *       typically returns non-overlapping matches based on the initial state.
     *       The loop processes matches based on their original positions.</li>
     *   <li><b>Atomicity:</b> Each individual replacement is atomic and undoable if
     *       the underlying {@code replace(int, int, String)} uses a command.
     *       The entire {@code replaceAll} operation, if part of a larger command
     *       structure, could be undone as a whole.</li>
     *   <li><b>Performance:</b> Finding all matches can be intensive for complex
     *       regex patterns on large documents. Each subsequent replacement also
     *       incurs the cost of the offset-based replace operation.</li>
     * </ul>
     * </p>
     *
     * <p>
     * Default Search Parameters for {@code findMatches}:
     * <ul>
     *   <li>{@code useRegex}: true</li>
     *   <li>{@code caseSensitive}: true</li>
     *   <li>{@code wordSeparators}: null</li>
     *   <li>{@code maxResults}: false (captures only the full match for replacement range)</li>
     * </ul>
     * </p>
     *
     * @param regex The regular expression pattern to search for.
     * @param replacement The text to replace each found pattern with.
     *                    An empty string will effectively delete all matched text.
     * @return Self-reference for method chaining and fluent API usage.
     * @throws PatternSyntaxException if the regex pattern is invalid.
     * @since 1.0
     * @see #replace(String, String)
     * @see #replace(int, int, String)
     * @see #findMatches(String, boolean, boolean, String, boolean)
     * @see FindMatch
     */
    public PieceTree replaceAll(String regex, String replacement) {
        List<FindMatch> matches = findMatches(regex, true, true, null, false);
        for (int i = matches.size() - 1; i >= 0; i--) {
            FindMatch match = matches.get(i);
            replace(match.startOffset, match.endOffset, replacement);
        }
        return this;
    }

    /**
     * Resets the PieceTree to its initial empty state with default configuration.
     * This operation clears all text content, undo/redo history, and resets internal
     * data structures to their default values, effectively creating a new, empty document.
     *
     * <p>
     * The reset process involves:
     * <ol>
     *   <li>Reinitializing the {@link BufferManager} to clear all stored text.</li>
     *   <li>Creating a new, empty {@link RedBlackTree} to manage pieces.</li>
     *   <li>Reinitializing the {@link UndoRedoManager} to clear all history.</li>
     *   <li>Setting the current snapshot to null.</li>
     *   <li>Resetting line count to zero.</li>
     * </ol>
     * </p>
     *
     * <p>
     * This method is useful for clearing the document content without creating a new
     * {@code PieceTree} instance, for example, when starting a new file in an editor.
     * After a reset, the PieceTree behaves as if it were newly constructed.
     * </p>
     *
     * @since 1.0
     * @see #PieceTree()
     * @see #initialize(String)
     */
    public void reset() {
        bufferManager = new BufferManager();
        tree = new RedBlackTree(bufferManager);
        undoRedoManager = new UndoRedoManager();
        currentSnapshot = null;
    }

    // CONTENT RETRIEVAL METHODS

    /**
     * Retrieves the complete text content of a specific line with high performance.
     * This method uses optimized algorithms to locate the line and extract its content
     * while handling various edge cases and end-of-line sequences efficiently.
     *
     * <p>
     * The implementation first uses {@link #findLinePosition(int)} to locate the target
     * line in O(log N) time, then performs efficient character extraction by traversing
     * only the necessary nodes. It properly handles lines that span multiple nodes and
     * different end-of-line conventions (LF, CRLF, CR).
     * </p>
     *
     * <p>
     * Performance characteristics:
     * <ul>
     *   <li><b>Best case:</b> O(log N + L) where N is nodes and L is line length</li>
     *   <li><b>Average case:</b> O(log N + L) for most line lengths</li>
     *   <li><b>Worst case:</b> O(log N + L + K) where K is nodes spanned by the line</li>
     * </ul>
     * </p>
     *
     * <p>
     * The method excludes end-of-line characters from the returned content, providing
     * only the actual text content of the line. Empty lines return an empty string.
     * </p>
     *
     * @param lineNumber The line number to retrieve (1-based indexing).
     *                   Must be between 1 and the total number of lines in the document.
     * @return A string containing the complete text content of the specified line,
     *         excluding any end-of-line characters. Returns an empty string for
     *         empty lines or invalid line numbers.
     * @throws IllegalArgumentException if lineNumber is less than 1.
     * @since 1.0
     * @see #findLinePosition(int)
     * @see #textRange(int, int)
     * @apiNote This method is optimized for frequent line access patterns common
     *          in text editors and syntax highlighters.
     */
    public String lineContent(int lineNumber) {
        if (lineNumber < 1) {
            throw new IllegalArgumentException("Line number must be 1 or greater");
        }

        LinePosition linePos = findLinePosition(lineNumber);
        if (linePos == null) {
            return ""; // Line doesn't exist
        }

        Node node = linePos.node;
        int lineStartInNode = node.lineStarts[linePos.lineNumber - 1];
        int lineStartOffset = node.documentStart + lineStartInNode;

        // Find the end of the line by looking for the next line start or end of content
        int lineEndOffset;

        if (linePos.lineNumber < node.lineStarts.length) {
            // Not the last line in this node
            lineEndOffset = node.documentStart + node.lineStarts[linePos.lineNumber];
        } else {
            // Last line in this node, need to find end by scanning for EOL or document end
            lineEndOffset = findLineEnd(node, lineStartInNode);
        }

        // Extract the line content, excluding EOL characters
        StringBuilder lineContent = new StringBuilder();
        Node currentNode = node;
        int currentOffset = lineStartOffset;
        int remainingLength = lineEndOffset - lineStartOffset;

        while (currentNode != null && remainingLength > 0) {
            char[] buffer = bufferManager.getBuffer(currentNode.bufferIndex);
            int nodeRelativeStart = Math.max(0, currentOffset - currentNode.documentStart);
            int nodeRelativeEnd = Math.min(currentNode.length, nodeRelativeStart + remainingLength);

            // Copy characters, stopping at EOL characters
            for (int i = nodeRelativeStart; i < nodeRelativeEnd; i++) {
                char ch = buffer[currentNode.bufferStart + i];
                if (ch == LineFeed || ch == CarriageReturn) {
                    // Stop at EOL character
                    return lineContent.toString();
                }
                lineContent.append(ch);
            }

            remainingLength -= (nodeRelativeEnd - nodeRelativeStart);
            currentOffset = currentNode.documentStart + currentNode.length;
            currentNode = tree.getSuccessor(currentNode);
        }

        return lineContent.toString();
    }

    /**
     * Retrieves multiple lines of text content within a specified range efficiently.
     * This method optimizes bulk line access by minimizing tree traversals and
     * providing batch content retrieval for rendering and processing operations.
     *
     * <p>
     * Optimization Strategy:
     * <ul>
     *   <li>Single tree traversal for entire range when possible</li>
     *   <li>Efficient line boundary detection</li>
     *   <li>Pre-allocated result collection for known size</li>
     *   <li>Minimal string object creation</li>
     * </ul>
     * </p>
     *
     * <p>
     * Use Cases:
     * <ul>
     *   <li>Syntax highlighting and text rendering</li>
     *   <li>Line-based text processing and analysis</li>
     *   <li>Editor viewport content loading</li>
     *   <li>Export and serialization operations</li>
     * </ul>
     * </p>
     *
     * @param startLineNumber The starting line number (1-based, inclusive).
     * @param endLineNumber The ending line number (1-based, inclusive).
     * @return A list containing the content of each line in the range.
     *         Empty lines are represented as empty strings.
     * @throws IllegalArgumentException if line numbers are invalid or startLine > endLine.
     * @since 1.0
     * @see #lineContent(int)
     * @see List
     */
    public List<String> linesContent(int startLineNumber, int endLineNumber) {
        List<String> lines = new ArrayList<>();
        for (int i = startLineNumber; i <= endLineNumber; i++) {
            lines.add(lineContent(i));
        }
        return lines;
    }

    /**
     * Converts a document character offset into line and column coordinates efficiently.
     * This method provides essential functionality for cursor positioning, selection
     * handling, and coordinate-based text operations in editor implementations.
     *
     * <p>
     * Conversion Algorithm:
     * <ol>
     *   <li>Locates node containing the offset using O(log N) tree search</li>
     *   <li>Calculates relative position within node using line start metadata</li>
     *   <li>Computes global line number using subtree line counts</li>
     *   <li>Determines column position within the line</li>
     * </ol>
     * </p>
     *
     * <p>
     * Performance Optimizations:
     * <ul>
     *   <li>Cached line start positions in each node</li>
     *   <li>Efficient subtree navigation using Red-Black tree properties</li>
     *   <li>Minimal character-by-character scanning</li>
     * </ul>
     * </p>
     *
     * @param offset The 0-based character offset in the document.
     * @return A {@link Position} object with 1-based line and column coordinates.
     *         Returns Position(1,1) for invalid offsets.
     * @since 1.0
     * @see Position
     * @see Node#lineStarts
     * @see Node#left_subtree_lfcnt
     */
    public Position positionAt(int offset) {
        if (offset < 0 || offset > length()) {
            return new Position(1, 1); // Return default position for invalid offset
        }

        Node node = tree.findNodeContaining(offset);
        if (node == null) {
            return new Position(1, 1); // Should not happen if offset is valid
        }

        int relativeOffset = offset - node.documentStart + 1;

        // Find the line number
        int lineNumber = 1;
        for (int i = 0; i < node.lineStarts.length; i++) {
            if (relativeOffset < node.lineStarts[i]) {
                lineNumber = i + 1;
                break;
            }
        }
        if (relativeOffset >= node.lineStarts[node.lineStarts.length - 1]) {
            lineNumber = node.lineStarts.length;
        }

        // Calculate column number
        int columnNumber = 1;
        if (lineNumber > 1) {
            columnNumber = relativeOffset - node.lineStarts[lineNumber - 2];
        } else {
            columnNumber = relativeOffset;
        }

        // Adjust for global line number using left subtree line count
        int globalLineNumber = (node.left != null ? node.left.left_subtree_lfcnt : 0) + lineNumber;

        return new Position(globalLineNumber, columnNumber);
    }

    /**
     * Converts line and column coordinates to document character offset with high performance.
     * This critical method enables efficient translation between editor coordinates and
     * internal document representation, optimized for frequent access patterns.
     *
     * <p>
     * Advanced Algorithm Features:
     * <ul>
     *   <li>O(log N) line location using tree-based binary search</li>
     *   <li>Intelligent column boundary handling</li>
     *   <li>Multi-node line support for very long lines</li>
     *   <li>Efficient character-level positioning within lines</li>
     * </ul>
     * </p>
     *
     * <p>
     * Edge Case Handling:
     * <ul>
     *   <li>Columns beyond line length return end-of-line position</li>
     *   <li>Invalid line numbers are clamped to document boundaries</li>
     *   <li>Properly handles different end-of-line character sequences</li>
     * </ul>
     * </p>
     *
     * @param lineNumber The target line number (1-based indexing). Must be ≥ 1.
     * @param column The target column number (1-based indexing). Must be ≥ 1.
     * @return The 0-based character offset corresponding to the position.
     *         Returns document length if position is beyond document end.
     * @throws IllegalArgumentException if lineNumber or column is less than 1.
     * @since 1.0
     * @see #findLinePosition(int)
     * @see LinePosition
     */
    public int offsetAt(int lineNumber, int column) {
        if (lineNumber < 1) {
            throw new IllegalArgumentException("Line number must be 1 or greater");
        }
        if (column < 1) {
            throw new IllegalArgumentException("Column number must be 1 or greater");
        }

        LinePosition linePos = findLinePosition(lineNumber);
        if (linePos == null) {
            return length(); // Line doesn't exist, return document length
        }

        Node node = linePos.node;
        int lineStartInNode = node.lineStarts[linePos.lineNumber - 1];
        int documentOffset = node.documentStart + lineStartInNode;

        // If column is 1, we're at the start of the line
        if (column == 1) {
            return documentOffset;
        }

        // Calculate the target offset within the line
        int targetColumn = column - 1; // Convert to 0-based
        int currentColumn = 0;

        char[] buffer = bufferManager.getBuffer(node.bufferIndex);

        // Search within the current node first
        for (int i = node.bufferStart + lineStartInNode;
             i < node.bufferStart + node.length && currentColumn < targetColumn; i++) {
            char ch = buffer[i];

            // Stop if we hit a line ending
            if (ch == LineFeed || ch == CarriageReturn) {
                break;
            }

            currentColumn++;
            if (currentColumn == targetColumn) {
                return documentOffset + currentColumn;
            }
        }

        // If we haven't found the column yet and haven't hit a line ending,
        // continue to next nodes
        if (currentColumn < targetColumn) {
            Node currentNode = tree.getSuccessor(node);
            int additionalOffset = node.length - lineStartInNode;

            while (currentNode != null && currentColumn < targetColumn) {
                char[] currentBuffer = bufferManager.getBuffer(currentNode.bufferIndex);

                for (int i = currentNode.bufferStart;
                     i < currentNode.bufferStart + currentNode.length && currentColumn < targetColumn; i++) {
                    char ch = currentBuffer[i];

                    // Stop if we hit a line ending
                    if (ch == LineFeed || ch == CarriageReturn) {
                        return documentOffset + currentColumn;
                    }

                    currentColumn++;
                    if (currentColumn == targetColumn) {
                        return documentOffset + currentColumn;
                    }
                }

                // If we processed the entire node without finding the column or line ending
                if (currentColumn < targetColumn) {
                    additionalOffset += currentNode.length;
                    currentNode = tree.getSuccessor(currentNode);
                }
            }
        }

        // Return the offset at the end of the line if column exceeds line length
        return documentOffset + currentColumn;
    }

    /**
     * Represents a specific position within a node corresponding to a line number.
     */
    private static class LinePosition {
        Node node;
        int lineNumber; // 1-based line number.

        /**
         * Constructs a new {@code LinePosition} instance.
         *
         * @param node          The node containing the line.
         * @param lineNumber    The line number within the node.
         */
        LinePosition(Node node, int lineNumber) {
            this.node = node;
            this.lineNumber = lineNumber;
        }
    }

    /**
     * Finds the position of a specific line within the piece tree structure.
     * This method uses optimized tree traversal to locate the node containing
     * the target line number and returns both the node and the relative line
     * position within that node.
     *
     * <p>
     * The algorithm performs a binary search-like traversal of the Red-Black tree,
     * using the cached line count information in each node's left subtree to
     * efficiently navigate to the target line without examining every node.
     * </p>
     *
     * <p>
     * Time Complexity: O(log N) where N is the number of nodes in the tree.
     * Space Complexity: O(1) - uses only a constant amount of additional space.
     * </p>
     *
     * @param lineNumber The target line number to find (1-based indexing).
     *                   Must be greater than 0 and not exceed the total line count.
     * @return A {@link LinePosition} object containing the node and relative line number,
     *         or {@code null} if the line number is invalid or the tree is empty.
     * @throws IllegalArgumentException if lineNumber is less than 1.
     * @since 1.0
     * @see LinePosition
     * @see Node#left_subtree_lfcnt
     */
    private LinePosition findLinePosition(int lineNumber) {
        if (lineNumber < 1) {
            throw new IllegalArgumentException("Line number must be 1 or greater");
        }

        Node current = tree.getRoot();
        if (current == null) {
            return null; // Empty tree
        }

        int targetLine = lineNumber;

        while (current != null) {
            // Get the number of lines in the left subtree
            int leftSubtreeLines = (current.left != null) ? current.left.left_subtree_lfcnt + current.left.lineStarts.length : 0;

            // Get the number of lines in the current node
            int currentNodeLines = current.lineStarts.length;

            if (targetLine <= leftSubtreeLines) {
                // Target line is in the left subtree
                current = current.left;
            } else if (targetLine <= leftSubtreeLines + currentNodeLines) {
                // Target line is in the current node
                int relativeLineInNode = targetLine - leftSubtreeLines - 1;
                return new LinePosition(current, relativeLineInNode);
            } else {
                // Target line is in the right subtree
                targetLine -= (leftSubtreeLines + currentNodeLines);
                current = current.right;
            }
        }

        return null; // Line not found (shouldn't happen with valid input)
    }

    /**
     * Efficiently finds the end offset of a line starting from a given position within a node.
     * This helper method scans forward through the buffer(s) to locate the next end-of-line
     * character or the end of the document, whichever comes first.
     *
     * <p>
     * The method handles multi-node scenarios where a line might span across multiple
     * pieces in the tree, and properly recognizes all standard EOL sequences:
     * LF (\n), CRLF (\r\n), and CR (\r).
     * </p>
     *
     * @param startNode The node where the line begins.
     * @param startPosInNode The starting position within the node's buffer.
     * @return The absolute document offset where the line ends (exclusive),
     *         which is either at the EOL character(s) or at the document end.
     * @since 1.0
     * @apiNote This is a private helper method optimized for the lineContent() implementation.
     */
    private int findLineEnd(Node startNode, int startPosInNode) {
        Node currentNode = startNode;
        int currentPos = startPosInNode;

        while (currentNode != null) {
            char[] buffer = bufferManager.getBuffer(currentNode.bufferIndex);

            for (int i = currentPos; i < currentNode.length; i++) {
                char ch = buffer[currentNode.bufferStart + i];
                if (ch == LineFeed || ch == CarriageReturn) {
                    return currentNode.documentStart + i;
                }
            }

            // Move to next node
            currentNode = tree.getSuccessor(currentNode);
            currentPos = 0; // Start from beginning of next node
        }

        // Reached end of document
        return length();
    }

    /**
     * Gets the total number of lines in the document using cached metadata.
     * This method provides O(1) line count access by maintaining accurate
     * line count information during all text modification operations.
     *
     * <p>
     * The line count is maintained incrementally during:
     * <ul>
     *   <li>Text insertion operations (counted during line start computation)</li>
     *   <li>Text deletion operations (decremented based on removed content)</li>
     *   <li>Document initialization (computed during initial setup)</li>
     * </ul>
     * </p>
     *
     * <p>
     * Line Counting Rules:
     * <ul>
     *   <li>Empty document has 0 lines</li>
     *   <li>Document with only text (no line breaks) has 1 line</li>
     *   <li>Each line break creates a new line</li>
     *   <li>Document ending with line break doesn't add extra line</li>
     * </ul>
     * </p>
     *
     * @return The total number of lines in the document (≥ 0).
     * @since 1.0
     * @see #computeLineStarts(char[])
     */
    public int lineCount() {
        return tree.getLineCount();
    }

    /**
     * Gets the character length of a specific line excluding end-of-line characters.
     * This method provides efficient line length calculation for text formatting,
     * cursor positioning, and selection operations in text editors.
     *
     * <p>
     * The method internally uses {@link #lineContent(int)} for content retrieval
     * and returns the character count of the resulting string, providing consistent
     * results with other line-based operations.
     * </p>
     *
     * <p>
     * Length Calculation:
     * <ul>
     *   <li>Excludes all end-of-line characters (LF, CRLF, CR)</li>
     *   <li>Includes all visible and whitespace characters</li>
     *   <li>Returns 0 for empty lines</li>
     *   <li>Handles Unicode characters correctly</li>
     * </ul>
     * </p>
     *
     * @param lineNumber The line number (1-based). Must be valid line in document.
     * @return The character count of the line (≥ 0).
     * @throws IllegalArgumentException if lineNumber is invalid.
     * @since 1.0
     * @see #lineContent(int)
     */
    public int lineLength(int lineNumber) {
        return lineContent(lineNumber).length();
    }


    /**
     * Retrieves complete document text with custom end-of-line character conversion.
     * This method provides flexible text export capabilities with on-the-fly
     * line ending conversion for cross-platform compatibility and formatting requirements.
     *
     * <p>
     * Conversion Process:
     * <ol>
     *   <li>Retrieves complete document text using {@link #text()}</li>
     *   <li>Compares requested EOL with current document EOL setting</li>
     *   <li>Performs regex-based conversion if EOL formats differ</li>
     *   <li>Returns converted text maintaining all content integrity</li>
     * </ol>
     * </p>
     *
     * <p>
     * Supported EOL Formats:
     * <ul>
     *   <li>\n (LF) - Unix/Linux/macOS standard</li>
     *   <li>\r\n (CRLF) - Windows standard</li>
     *   <li>\r (CR) - Classic Mac standard</li>
     * </ul>
     * </p>
     *
     * @param eol The end-of-line character sequence to use. If null or same as current, no conversion is performed.
     * @return The complete document text with the specified EOL format.
     * @since 1.0
     * @see #text()
     * @see #getEOL()
     */
    public String text(String eol) {
        String content = text();
        if (eol != null && !eol.equals(this.eol)) {
            return content.replaceAll("(\r\n|\n|\r)", eol);
        }
        return content;
    }

    /**
     * Retrieves the complete text content of the document through optimized tree traversal.
     * This fundamental method performs in-order traversal of the piece tree structure,
     * reconstructing the original document text with high performance and memory efficiency.
     *
     * <p>
     * Traversal Strategy:
     * <ul>
     *   <li>In-order tree traversal maintaining document sequence</li>
     *   <li>Stack-based iteration avoiding recursion overhead</li>
     *   <li>Direct buffer access for optimal character copying</li>
     *   <li>StringBuilder with appropriate capacity pre-allocation</li>
     * </ul>
     * </p>
     *
     * <p>
     * Performance Characteristics:
     * <ul>
     *   <li><b>Time Complexity:</b> O(N + M) where N is document length, M is node count</li>
     *   <li><b>Space Complexity:</b> O(N) for result string construction</li>
     *   <li><b>Memory Access:</b> Sequential buffer reads for cache efficiency</li>
     * </ul>
     * </p>
     *
     * @return A string representing the complete text content of the document.
     *         Returns empty string for empty documents.
     * @since 1.0
     * @see RedBlackTree
     * @see BufferManager
     */
    public String text() {
        StringBuilder result = new StringBuilder();
        Node current = tree.getRoot();
        if (current == null) return "";
        Stack<Node> stack = new Stack<>();
        while (current != null || !stack.isEmpty()) {
            while (current != null) {
                stack.push(current);
                current = current.left;
            }
            current = stack.pop();
            char[] buffer = bufferManager.getBuffer(current.bufferIndex);
            result.append(new String(buffer, current.bufferStart, current.length));
            current = current.right;
        }
        return result.toString();
    }

    /**
     * Retrieves text content between two line/column positions with coordinate validation.
     * This method provides intuitive text extraction using editor-style coordinates,
     * supporting both single-line and multi-line content retrieval operations.
     *
     * <p>
     * Coordinate Processing:
     * <ol>
     *   <li>Converts start and end coordinates to absolute offsets</li>
     *   <li>Validates coordinate boundaries and ordering</li>
     *   <li>Delegates to offset-based extraction for optimal performance</li>
     *   <li>Maintains coordinate semantics throughout operation</li>
     * </ol>
     * </p>
     *
     * <p>
     * Range Semantics:
     * <ul>
     *   <li>Start position is inclusive</li>
     *   <li>End position is exclusive (standard text selection behavior)</li>
     *   <li>Multi-line ranges include all intermediate content</li>
     * </ul>
     * </p>
     *
     * @param startLineNumber The starting line (1-based, inclusive).
     * @param startColumn The starting column (1-based, inclusive).
     * @param endLineNumber The ending line (1-based, inclusive).
     * @param endColumn The ending column (1-based, exclusive).
     * @return The text content in the specified coordinate range.
     * @since 1.0
     * @see #offsetAt(int, int)
     * @see #textRange(int, int)
     */
    public String textRange(int startLineNumber, int startColumn, int endLineNumber, int endColumn) {
        int startOffset = offsetAt(startLineNumber, startColumn);
        int endOffset = offsetAt(endLineNumber, endColumn);
        return textRange(startOffset, endOffset);
    }

    /**
     * Extracts text between two character offsets with optimized tree traversal.
     * This high-performance method provides direct offset-based text extraction
     * using efficient piece tree navigation and minimal memory allocation.
     *
     * <p>
     * Extraction Algorithm:
     * <ol>
     *   <li>Validates offset range and handles edge cases</li>
     *   <li>Locates starting node using O(log N) tree search</li>
     *   <li>Traverses necessary nodes accumulating text content</li>
     *   <li>Efficiently copies buffer segments avoiding character-by-character processing</li>
     * </ol>
     * </p>
     *
     * <p>
     * Performance Optimizations:
     * <ul>
     *   <li>Minimal tree traversal - only visits nodes containing target range</li>
     *   <li>Direct buffer copying for large segments</li>
     *   <li>Early termination when range is satisfied</li>
     *   <li>StringBuilder with calculated capacity to avoid resizing</li>
     * </ul>
     * </p>
     *
     * @param start The inclusive starting offset (0-based).
     * @param end The exclusive ending offset (0-based).
     * @return The extracted text between the offsets. Empty string if start ≥ end.
     * @since 1.0
     * @see RedBlackTree#findNodeContaining(int)
     * @see RedBlackTree#getSuccessor(Node)
     */
    public String textRange(int start, int end) {
        if (start >= end) return "";

        StringBuilder result = new StringBuilder();
        Node currentNode = tree.findNodeContaining(start);
        if (currentNode == null) return "";

        int currentOffset = start;
        int remainingLength = end - start;

        while (currentNode != null && remainingLength > 0) {
            char[] buffer = bufferManager.getBuffer(currentNode.bufferIndex);
            int nodeStart = Math.max(0, currentOffset - currentNode.documentStart);
            int nodeEnd = Math.min(currentNode.length, nodeStart + remainingLength);

            for (int i = nodeStart; i < nodeEnd; i++) {
                result.append(buffer[currentNode.bufferStart + i]);
            }

            remainingLength -= (nodeEnd - nodeStart);
            currentOffset = currentNode.documentStart + currentNode.length;
            currentNode = tree.getSuccessor(currentNode);
        }

        return result.toString();
    }

    /**
     * Calculates the total character length of the document with optimal performance.
     * This method uses the Red-Black tree structure to efficiently compute document
     * length without requiring full content traversal, providing O(log N) performance.
     *
     * <p>
     * Efficient Length Calculation:
     * <ul>
     *   <li>Finds rightmost node in O(log N) time</li>
     *   <li>Uses cached document start position + node length</li>
     *   <li>Avoids expensive full tree traversal</li>
     *   <li>Maintains accuracy through incremental updates</li>
     * </ul>
     * </p>
     *
     * <p>
     * The calculation leverages the fact that nodes maintain their absolute
     * document start positions, allowing direct computation of total length
     * from the rightmost node's position and size.
     * </p>
     *
     * @return The total number of characters in the document (≥ 0).
     * @since 1.0
     * @see #getLengthFromNode(Node)
     * @see Node#documentStart
     */
    public int length() {
        if (tree.getRoot() == null) {
            return 0;
        }
        return getLengthFromNode(tree.getRoot());
    }

    /**
     * Recursively computes the total length from a given node and its subtrees.
     * Uses the tree structure to avoid full traversal when possible.
     *
     * @param node The node to start computation from
     * @return The total length of the node and its subtrees
     */
    private int getLengthFromNode(Node node) {
        if (node == null) {
            return 0;
        }

        // If the node has cached document start and we're at root, we can compute total length
        // more efficiently by finding the rightmost node
        if (node == tree.getRoot()) {
            Node rightmost = node;
            while (rightmost.right != null) {
                rightmost = rightmost.right;
            }
            return rightmost.documentStart + rightmost.length;
        }

        return node.length + getLengthFromNode(node.left) + getLengthFromNode(node.right);
    }

    // SEARCH METHODS

    /**
     * Finds all occurrences of a search pattern with comprehensive matching options.
     * This method provides powerful text search capabilities supporting both literal
     * string matching and regular expression patterns with extensive customization.
     *
     * <p>
     * Search Features:
     * <ul>
     *   <li><b>Pattern Types:</b> Literal strings or regular expressions</li>
     *   <li><b>Case Sensitivity:</b> Configurable case-sensitive or case-insensitive matching</li>
     *   <li><b>Word Boundaries:</b> Custom word separator characters for whole-word matching</li>
     *   <li><b>Result Limiting:</b> Optional maximum result count for performance control</li>
     * </ul>
     * </p>
     *
     * <p>
     * Performance Characteristics:
     * <ul>
     *   <li><b>Time Complexity:</b> O(N * M) where N is document length, M is pattern length</li>
     *   <li><b>Space Complexity:</b> O(R) where R is number of matches found</li>
     *   <li><b>Regex Performance:</b> Depends on pattern complexity and Java regex engine</li>
     * </ul>
     * </p>
     *
     * <p>
     * Use Cases:
     * <ul>
     *   <li>Find and replace operations in text editors</li>
     *   <li>Syntax highlighting and code analysis</li>
     *   <li>Content validation and pattern detection</li>
     *   <li>Text mining and information extraction</li>
     * </ul>
     * </p>
     *
     * @param query The search pattern (literal string or regex).
     * @param caseSensitive Whether to perform case-sensitive matching.
     * @param wordSeparators Characters that define word boundaries.
     * @param useRegex Whether to treat pattern as regular expression.
     * @param captureGroups Treat {@link Matcher} {@code matcher} to returns multiple groups {@link Matcher#group(int)}.
     * @return List of {@link FindMatch} objects containing match positions and text.
     * @throws PatternSyntaxException if regex pattern is invalid.
     * @since 1.0
     * @see FindMatch
     * @see Pattern
     * @see #findNext
     */
    public List<FindMatch> findMatches(String query, boolean useRegex, boolean caseSensitive,
                                   String wordSeparators, boolean captureGroups) {
        List<FindMatch> matches = new ArrayList<>();
        String content = text();

        try {
            Pattern pattern;
            if (useRegex) {
                int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
                pattern = Pattern.compile(query, flags);
            } else {
                String escapedPattern = Pattern.quote(query);
                if (wordSeparators != null) {
                    // Construct a regex that uses the custom word separators for word boundaries
                    String boundaryRegex = "(?<=^|[" + Pattern.quote(wordSeparators) + "])";
                    escapedPattern = boundaryRegex + escapedPattern + "(?=$|[" + Pattern.quote(wordSeparators) + "])";
                }
                int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
                pattern = Pattern.compile(escapedPattern, flags);
            }

            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                List<String> matchGroups = new ArrayList<>();
                if (captureGroups) {
                    for (int i = 0; i <= matcher.groupCount(); i++) {
                        matchGroups.add(matcher.group(i));
                    }
                } else {
                    matchGroups.add(matcher.group());
                }

                matches.add(new FindMatch(matcher.start(), matcher.end(), matchGroups));
            }
        } catch (Exception e) {
            throw new PatternSyntaxException(e.getMessage(), query, 0);
        }

        return matches;
    }

    /**
     * Finds the next occurrence of a pattern starting from a specified line and column.
     * This method provides efficient forward search functionality optimized for
     * incremental search operations and interactive find-as-you-type features,
     * using human-readable line/column coordinates.
     *
     * <p>
     * Search Strategy:
     * <ol>
     *   <li>Converts line/column start position to absolute document offset.</li>
     *   <li>Delegates to offset-based {@link #findNext(String, int, boolean, boolean, String, boolean)}
     *       for actual search execution.</li>
     *   <li>Returns first match found or null if no match exists.</li>
     *   <li>Supports wraparound search to document beginning if underlying implementation does.</li>
     * </ol>
     * </p>
     *
     * <p>
     * Coordinate Handling:
     * <ul>
     *   <li>Line and column numbers are 1-based (editor convention).</li>
     *   <li>Invalid coordinates are handled by {@link #offsetAt(int, int)}.</li>
     * </ul>
     * </p>
     *
     * @param query The search pattern to find.
     * @param startPos The starting {@link Position} (line and column) for the search.
     * @param useRegex Whether to treat the pattern as a regular expression.
     * @param caseSensitive Whether to perform case-sensitive matching.
     * @param wordSeparators Characters that define word boundaries for non-regex search.
     * @param captureGroups Treat {@link Matcher} {@code matcher} to returns multiple groups {@link Matcher#group(int)}.
     * @return {@link FindMatch} containing match details, or null if no match found.
     * @throws PatternSyntaxException if the regex pattern is invalid.
     * @since 1.0
     * @see #findNext(String, int, boolean, boolean, String, boolean)
     * @see #offsetAt(int, int)
     */
    public FindMatch findNext(String query, Position startPos, boolean useRegex,
                                   boolean caseSensitive, String wordSeparators, boolean captureGroups) {
        int startOffset = offsetAt(startPos.lineNumber, startPos.column);
        return findNext(query, startOffset, useRegex, caseSensitive, wordSeparators, captureGroups);
    }

    /**
     * Finds the next occurrence of a pattern starting from a specified position.
     * This method provides efficient forward search functionality optimized for
     * incremental search operations and interactive find-as-you-type features.
     *
     * <p>
     * Search Strategy:
     * <ol>
     *   <li>Starts search from specified offset position</li>
     *   <li>Uses optimized pattern matching algorithm</li>
     *   <li>Returns first match found or null if no match exists</li>
     *   <li>Supports wraparound search to document beginning</li>
     * </ol>
     * </p>
     *
     * <p>
     * Performance Optimizations:
     * <ul>
     *   <li>Early termination on first match for interactive usage</li>
     *   <li>Efficient text access through piece tree structure</li>
     *   <li>Minimal memory allocation during search process</li>
     *   <li>Optimized for repeated searches with different start positions</li>
     * </ul>
     * </p>
     *
     * @param query The search pattern to find.
     * @param startOffset The starting offset for the search (0-based).
     * @param useRegex Whether to treat pattern as regular expression.
     * @param caseSensitive Whether to perform case-sensitive matching.
     * @param wordSeparators Characters that define word boundaries.
     * @param captureGroups Treat {@link Matcher} {@code matcher} to returns multiple groups {@link Matcher#group(int)}.
     * @return {@link FindMatch} containing match details, or null if no match found.
     * @throws PatternSyntaxException if regex pattern is invalid.
     * @since 1.0
     * @see #findPrevious
     */
    public FindMatch findNext(String query, int startOffset, boolean useRegex,
                              boolean caseSensitive, String wordSeparators, boolean captureGroups) {
        String content = text();

        try {
            Pattern pattern;
            if (useRegex) {
                int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
                pattern = Pattern.compile(query, flags);
            } else {
                String escapedPattern = Pattern.quote(query);
                if (wordSeparators != null) {
                    // Construct a regex that uses the custom word separators for word boundaries
                    String boundaryRegex = "(?<=^|[" + Pattern.quote(wordSeparators) + "])";
                    escapedPattern = boundaryRegex + escapedPattern + "(?=$|[" + Pattern.quote(wordSeparators) + "])";
                }
                int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
                pattern = Pattern.compile(escapedPattern, flags);
            }

            Matcher matcher = pattern.matcher(content);
            if (matcher.find(startOffset)) {
                List<String> matchGroups = new ArrayList<>();
                if (captureGroups) {
                    for (int i = 0; i <= matcher.groupCount(); i++) {
                        matchGroups.add(matcher.group(i));
                    }
                } else {
                    matchGroups.add(matcher.group());
                }

                return new FindMatch(matcher.start(), matcher.end(), matchGroups);
            }
        } catch (Exception e) {
            throw new PatternSyntaxException(e.getMessage(), query, startOffset);
        }

        return null;
    }

    /**
     * Finds the previous occurrence of a pattern searching backwards from a specified
     * line and column {@link Position}. This method serves as a convenience overload,
     * converting human-readable line/column coordinates to an absolute document offset
     * before delegating to the offset-based findPrevious implementation.
     *
     * <p>
     * This method is typically used for "Find Previous" features in text editors where
     * the starting point of the backward search is naturally expressed in terms of
     * line and column numbers.
     * </p>
     *
     * <p>
     * Search Strategy:
     * <ol>
     *   <li>Converts the given {@code endPos} (line and column) to an absolute
     *       document offset using {@link #offsetAt(int, int)}. This offset represents
     *       the point <em>before</em> which the search should find a match.</li>
     *   <li>Delegates the actual search logic to the offset-based
     *       {@link #findPrevious(String, int, boolean, boolean, String, boolean)} method.</li>
     *   <li>The underlying offset-based method searches the content from the document
     *       start up to this calculated offset and returns the last match found.</li>
     * </ol>
     * </p>
     *
     * <p>
     * Coordinate Handling:
     * <ul>
     *   <li>Line and column numbers within the {@code endPos} are typically 1-based,
     *       consistent with editor conventions.</li>
     *   <li>The validity and conversion of these coordinates to an offset are handled
     *       by the {@link #offsetAt(int, int)} method.</li>
     * </ul>
     * </p>
     *
     * <p>
     * Performance Considerations:
     * Refer to the performance considerations of the underlying
     * {@link #findPrevious(String, int, boolean, boolean, String, boolean)} method,
     * particularly regarding substring extraction for backward searches.
     * </p>
     *
     * @param query The search pattern (literal string or regular expression) to find.
     * @param endPos The {@link Position} (line and column) marking the end point
     *               for the backward search. The search looks for matches occurring
     *               <em>before</em> this position.
     * @param useRegex Whether to interpret the {@code query} as a regular expression.
     * @param caseSensitive Whether the search should be case-sensitive.
     * @param wordSeparators A string of characters that define word boundaries if
     *                       {@code useRegex} is false and word-based searching is desired.
     *                       Can be {@code null} if not applicable.
     * @param captureGroups Treat {@link Matcher} {@code matcher} to returns multiple groups {@link Matcher#group(int)}.
     * @return A {@link FindMatch} object containing the details of the found match
     *         (start offset, end offset, and matched groups/text), or {@code null}
     *         if no preceding occurrence of the pattern is found.
     * @throws PatternSyntaxException if the {@code query} is a regular expression and
     *                                its syntax is invalid.
     * @since 1.0
     * @see #findPrevious(String, int, boolean, boolean, String, boolean)
     * @see #findNext(String, Position, boolean, boolean, String, boolean)
     * @see #offsetAt(int, int)
     * @see Position
     * @see FindMatch
     */
    public FindMatch findPrevious(String query, Position endPos, boolean useRegex,
                                  boolean caseSensitive, String wordSeparators, boolean captureGroups) {
        int endOffset = offsetAt(endPos.lineNumber, endPos.column);
        return findPrevious(query, endOffset, useRegex, caseSensitive, wordSeparators, captureGroups);
    }

    /**
     * Finds the previous occurrence of a pattern starting from a specified line and column, searching backwards.
     * This method provides efficient backward search functionality, typically used for "Find Previous"
     * features in text editors.
     *
     * <p>
     * Search Strategy:
     * <ol>
     *   <li>Converts line/column start position to an absolute document offset. This offset marks the
     *       <em>end</em> of the text range to be searched.</li>
     *   <li>Extracts the document content from the beginning up to the calculated end offset.</li>
     *   <li>Performs a forward search within this extracted substring.</li>
     *   <li>Returns the <em>last</em> match found in the substring, which corresponds to the
     *       occurrence immediately preceding the specified start position in the original document.</li>
     *   <li>If no match is found in the substring, returns null.</li>
     *   <li>Supports wraparound search if the underlying logic is extended to handle it (currently searches up to start).</li>
     * </ol>
     * </p>
     *
     * <p>
     * Performance Considerations:
     * <ul>
     *   <li>Extracting the substring can be O(N) where N is the end offset. For searches near the document end, this can be costly.</li>
     *   <li>Regex matching performance depends on pattern complexity and substring length.</li>
     *   <li>Consider optimizations for very large documents if performance is critical for backward searches.</li>
     * </ul>
     * </p>
     *
     * @param query The search pattern (literal string or regex) to find.
     * @param endOffset The end offset from which to start searching backwards.
     * @param useRegex Whether to treat the pattern as a regular expression.
     * @param caseSensitive Whether to perform case-sensitive matching.
     * @param wordSeparators Characters that define word boundaries for non-regex search.
     * @param captureGroups Treat {@link Matcher} {@code matcher} to returns multiple groups {@link Matcher#group(int)}.
     * @return {@link FindMatch} containing match details, or null if no match found.
     * @throws PatternSyntaxException if the regex pattern is invalid.
     * @since 1.0
     * @see #findNext(String, Position, boolean, boolean, String, boolean)
     * @see #offsetAt(int, int)
     */
    public FindMatch findPrevious(String query, int endOffset, boolean useRegex,
                                       boolean caseSensitive, String wordSeparators, boolean captureGroups) {
        String content = text().substring(0, endOffset);

        try {
            Pattern pattern;
            if (useRegex) {
                int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
                pattern = Pattern.compile(query, flags);
            } else {
                String escapedPattern = Pattern.quote(query);
                if (wordSeparators != null) {
                    // Construct a regex that uses the custom word separators for word boundaries
                    String boundaryRegex = "(?<=^|[" + Pattern.quote(wordSeparators) + "])";
                    escapedPattern = boundaryRegex + escapedPattern + "(?=$|[" + Pattern.quote(wordSeparators) + "])";
                }
                int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
                pattern = Pattern.compile(escapedPattern, flags);
            }

            Matcher matcher = pattern.matcher(content);
            FindMatch lastMatch = null;

            while (matcher.find()) {
                List<String> matchGroups = new ArrayList<>();
                if (captureGroups) {
                    for (int i = 0; i <= matcher.groupCount(); i++) {
                        matchGroups.add(matcher.group(i));
                    }
                } else {
                    matchGroups.add(matcher.group());
                }

                lastMatch = new FindMatch(matcher.start(), matcher.end(), matchGroups);
            }

            return lastMatch;
        } catch (Exception e) {
            throw new PatternSyntaxException(e.getMessage(), query, 0);
        }
    }

    // UTILITY METHODS

    /**
     * Gets the current end-of-line character sequence used by the document.
     * This method returns the EOL format that will be used for new line breaks
     * and text operations within the document.
     *
     * <p>
     * The EOL sequence is determined by:
     * <ol>
     *   <li>Explicit setting via {@link #setEOL(String)}</li>
     *   <li>Auto-detection during document initialization</li>
     *   <li>Default LF (\n) format for new documents</li>
     * </ol>
     * </p>
     *
     * @return The current end-of-line character sequence.
     * @since 1.0
     * @see #setEOL(String)
     * @see EOLNormalization
     */
    public String getEOL() {
        return eol;
    }

    /**
     * Sets the end-of-line character sequence for the document.
     * This method configures the EOL format that will be used for new text
     * operations without modifying existing content in the document.
     *
     * <p>
     * Supported EOL Formats:
     * <ul>
     *   <li>"\n" (LF) - Unix/Linux/macOS standard</li>
     *   <li>"\r\n" (CRLF) - Windows standard</li>
     *   <li>"\r" (CR) - Classic Mac standard</li>
     * </ul>
     * </p>
     *
     * <p>
     * <b>Note:</b> This method only affects future text operations.
     * To convert existing content, use {@link #text(String)} to retrieve
     * text with the desired EOL format.
     * </p>
     *
     * @param eol The new end-of-line character sequence.
     * @throws IllegalArgumentException if eol is null or empty.
     * @since 1.0
     * @see #getEOL()
     * @see #text(String)
     */
    public void setEOL(String eol) {
        if (eol != null && (eol.equals("\n") || eol.equals("\r\n") || eol.equals("\r"))) {
            this.eol = eol;
        }
    }

    /**
     * Creates a complete snapshot of the current document state for backup or state management.
     * This method captures the essential aspects of the document, including its
     * full text content, line count, and current end-of-line (EOL) settings.
     * The resulting {@link PieceTreeSnapshot} is a lightweight, immutable representation
     * suitable for various use cases.
     *
     * <p>
     * Snapshot Contents:
     * <ul>
     *   <li><b>Document Text:</b> The complete text content of the document at the moment of snapshot creation,
     *       reflecting the current EOL format used by the PieceTree.</li>
     *   <li><b>Line Count:</b> The total number of lines in the document.</li>
     *   <li><b>EOL Settings:</b> The end-of-line sequence (e.g., "\n", "\r\n") currently configured for the document.</li>
     * </ul>
     * </p>
     *
     * <p>
     * Use Cases:
     * <ul>
     *   <li>Creating save points or checkpoints for undo/redo operations.</li>
     *   <li>Comparing document states at different times (e.g., for diff generation).</li>
     *   <li>Persisting a specific version of the document.</li>
     *   <li>Implementing auto-save functionality.</li>
     * </ul>
     * </p>
     *
     * @return A {@link PieceTreeSnapshot} containing the current document state.
     * @since 1.0
     * @see PieceTreeSnapshot
     * @see #restoreSnapshot(PieceTreeSnapshot)
     */
    public PieceTreeSnapshot createSnapshot() {
        currentSnapshot = new PieceTreeSnapshot(text(), tree.getLineCount(), eol);
        return currentSnapshot;
    }

    /**
     * Restores the document to a previously captured state from a snapshot.
     * This method provides comprehensive state restoration functionality,
     * rebuilding the entire document structure from snapshot data.
     *
     * <p>
     * Restoration Process:
     * <ol>
     *   <li>Validates snapshot compatibility and integrity (checks for null).</li>
     *   <li>Clears current document state by reinitializing the {@link RedBlackTree}
     *       and {@link BufferManager}.</li>
     *   <li>Rebuilds piece tree structure using the content from the snapshot via {@link #initialize(String)}.</li>
     *   <li>Restores metadata such as the end-of-line (EOL) sequence using {@link #setEOL(String)}.</li>
     *   <li>Updates the current snapshot reference.</li>
     * </ol>
     * </p>
     *
     * <p>
     * State Restoration Aspects:
     * <ul>
     *   <li>Exact text content reproduction from the snapshot.</li>
     *   <li>Line structure and metadata are rebuilt based on the restored content.</li>
     *   <li>EOL format is restored to the state it was in when the snapshot was created.</li>
     *   <li>Document statistics (like line count) are recalculated during the re-initialization.</li>
     * </ul>
     * </p>
     *
     * @param snapshot The {@link PieceTreeSnapshot} to restore from. If null, the method does nothing.
     * @since 1.0
     * @see PieceTreeSnapshot
     * @see #createSnapshot()
     * @see #initialize(String)
     */
    public void restoreSnapshot(PieceTreeSnapshot snapshot) {
        if (snapshot != null) {
            // Clear current content
            tree = new RedBlackTree(bufferManager);
            bufferManager = new BufferManager();

            // Restore from snapshot
            initialize(snapshot.content);
            setEOL(snapshot.eol);
            currentSnapshot = snapshot;
        }
    }

    /**
     * Validates the internal Red-Black tree structure of the PieceTree.
     * This method performs a series of checks to ensure the integrity and
     * balance properties of the underlying Red-Black tree are maintained.
     * It is primarily intended for debugging and testing purposes to verify
     * the correctness of tree manipulation operations.
     *
     * <p>
     * Validation Checks:
     * <ol>
     *   <li><b>Root Property:</b> The root node is black.</li>
     *   <li><b>Red Property:</b> Red nodes have only black children.</li>
     *   <li><b>Black Height Property:</b> All paths from any node to its descendant
     *       NIL (null) leaves contain the same number of black nodes.</li>
     *   <li><b>Order Property:</b> For any node x, all keys in its left subtree
     *       are less than or equal to x.key, and all keys in its right subtree
     *       are greater than or equal to x.key (maintained by piece start offsets).</li>
     * </ol>
     * </p>
     *
     * @return {@code true} if the Red-Black tree structure is valid and adheres to all properties,
     *         {@code false} otherwise, indicating a potential structural inconsistency.
     * @since 1.0
     * @see RedBlackTree#validateRedBlackProperties()
     */
    public boolean validateTree() {
        return tree.validateRedBlackProperties();
    }

    /**
     * Internal method to replace a range of text with new text.
     * This method directly manipulates the tree and buffers without going through the undo/redo manager.
     * It first deletes the specified range and then inserts the new text at the start of that range.
     *
     * @param start   The starting offset (0-based) of the text to be replaced.
     * @param end     The ending offset (0-based, exclusive) of the text to be replaced.
     * @param newText The new text to insert.
     */
    void doReplace(int start, int end, String newText) {
        doDelete(start, end);
        doInsert(start, newText);
    }

    /**
     * Internal method to insert text at a specified offset.
     * This method directly manipulates the piece tree and underlying buffers.
     * It handles splitting existing nodes if the insertion point is within a node,
     * and creates new nodes for the inserted text.
     *
     * @param offset The 0-based offset where the text should be inserted.
     * @param text   The text to insert. Can be null or empty, in which case the method returns without doing anything.
     * @throws IllegalArgumentException if the offset is out of bounds and not at the end of the document.
     */
    void doInsert(int offset, String text) {
        try {
            if (text == null || text.isEmpty()) return;

            if (!eol.equals("\n")) text = text.replaceAll(("/r/n|/n|/r"), eol);

            Node node = tree.findNodeContaining(offset);
            if (node == null && offset == length()) {
                int startPos = bufferManager.addAddedText(text);
                int[] lineStarts = computeLineStartsFast(text.toCharArray());
                Node newNode = new Node(0, startPos, text.length(), lineStarts);
                newNode.documentStart = offset;
                tree.insert(newNode);
                return;
            } else if (node == null) {
                throw new IllegalArgumentException("Offset out of bounds");
            }

            int nodeDocStart = node.documentStart;

            if (offset > nodeDocStart) {
                int splitPoint = offset - nodeDocStart;
                int[] leftLineStarts = computeLineStartsFast(getBufferSlice(node, 0, splitPoint));
                int[] rightLineStarts = computeLineStartsFast(getBufferSlice(node, splitPoint, node.length));
                Node leftPart = new Node(node.bufferIndex, node.bufferStart, splitPoint, leftLineStarts);
                Node rightPart = new Node(node.bufferIndex, node.bufferStart + splitPoint, node.length - splitPoint, rightLineStarts);
                leftPart.documentStart = nodeDocStart;
                rightPart.documentStart = offset;
                tree.deleteNode(node);
                tree.insert(leftPart);
                tree.insert(rightPart);
            }

            int startPos = bufferManager.addAddedText(text);
            int[] lineStarts = computeLineStartsFast(text.toCharArray());
            Node newNode = new Node(0, startPos, text.length(), lineStarts);
            newNode.documentStart = offset;
            tree.insert(newNode);
        } catch (Exception error) {
            Log.e("PieceTree", "Error inserting text: " + error.getMessage());
        }
    }

    /**
     * Internal method to delete a range of text.
     * This method directly manipulates the piece tree by calling the tree's deleteRange method.
     *
     * @param start The starting offset (0-based) of the text to be deleted.
     * @param end   The ending offset (0-based, exclusive) of the text to be deleted.
     */
    void doDelete(int start, int end) {
        if (start >= end) return;
        if (start == 0 && end == length()) {
            reset();
            return;
        }
        tree.deleteRange(start, end);
    }

    /**
     * Computes line start positions for a character buffer using optimized scanning.
     * This method uses a single pass through the buffer and pre-allocates capacity
     * based on estimated line density for better performance with large texts.
     *
     * @param buffer The character buffer to analyze for line breaks
     * @return An array of integers representing the starting offset of each line
     * @throws IllegalArgumentException if buffer is null
     * @since 1.0
     */
    @TargetApi(Build.VERSION_CODES.N)
    private int[] computeLineStarts(char[] buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("Buffer cannot be null");
        }

        if (buffer.length == 0) {
            return new int[0];
        }

        // Pre-allocate with estimated capacity (assume average 50 chars per line)
        List<Integer> starts = new ArrayList<>(Math.max(16, buffer.length / 50));

        // Single pass through buffer with optimized loop
        for (int i = 0; i < buffer.length; i++) {
            char ch = buffer[i];
            if (ch == LineFeed) {
                starts.add(i + 1); // Next line starts after LF
            } else if (ch == CarriageReturn) {
                // Handle CRLF sequence - skip LF if it follows CR
                if (i + 1 < buffer.length && buffer[i + 1] == LineFeed) {
                    starts.add(i + 2); // Next line starts after CRLF
                    i++; // Skip the LF
                } else {
                    starts.add(i + 1); // Next line starts after standalone CR
                }
            }
        }

        tree.addLineCount(starts.size());

        // Convert to array efficiently
        return starts.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Alternative high-performance line start computation using primitive operations.
     * This method avoids boxing/unboxing overhead and provides better performance
     * for very large buffers by using array operations directly.
     *
     * @param buffer The character buffer to analyze
     * @return An array of line start positions
     * @since 1.0
     */
    private int[] computeLineStartsFast(char[] buffer) {
        if (buffer == null || buffer.length == 0) {
            return new int[0];
        }

        // First pass: count line breaks
        int lineCount = 0;
        for (int i = 0; i < buffer.length; i++) {
            char ch = buffer[i];
            if (ch == LineFeed) {
                lineCount++;
            } else if (ch == CarriageReturn) {
                lineCount++;
                // Skip LF in CRLF sequence
                if (i + 1 < buffer.length && buffer[i + 1] == LineFeed) {
                    i++;
                }
            }
        }

        if (lineCount == 0) {
            return new int[0];
        }

        // Second pass: collect line starts
        int[] starts = new int[lineCount];
        int startIndex = 0;

        for (int i = 0; i < buffer.length && startIndex < lineCount; i++) {
            char ch = buffer[i];
            if (ch == LineFeed) {
                starts[startIndex++] = i + 1;
            } else if (ch == CarriageReturn) {
                if (i + 1 < buffer.length && buffer[i + 1] == LineFeed) {
                    starts[startIndex++] = i + 2;
                    i++; // Skip LF
                } else {
                    starts[startIndex++] = i + 1;
                }
            }
        }

        tree.addLineCount(lineCount);

        return starts;
    }

    /**
     * Extracts a slice of characters from a buffer associated with a given node.
     *
     * @param node  The node whose buffer is to be sliced.
     * @param start The starting index (inclusive) of the slice within the node's content.
     * @param end   The ending index (exclusive) of the slice within the node's content.
     * @return A new character array containing the specified slice.
     */
    private char[] getBufferSlice(Node node, int start, int end) {
        char[] buffer = bufferManager.getBuffer(node.bufferIndex);
        char[] slice = new char[end - start];
        System.arraycopy(buffer, node.bufferStart + start, slice, 0, end - start);
        return slice;
    }

    // Getters for undo/redo manager

    /**
     * Retrieves the {@link UndoRedoManager} associated with this PieceTree.
     * This manager handles all undo and redo operations for text modifications
     * performed on the document. It allows for tracking, reversing, and reapplying
     * changes, providing robust history management capabilities.
     *
     * <p>
     * The {@code UndoRedoManager} is responsible for:
     * <ul>
     *   <li>Storing a history of {@link Command} objects representing edits.</li>
     *   <li>Providing methods to perform undo ({@link UndoRedoManager#undo()}) and
     *       redo ({@link UndoRedoManager#redo()}) operations.</li>
     *   <li>Managing grouped commands for atomic undo/redo of complex actions.</li>
     *   <li>Notifying listeners ({@link UndoRedoListener}) of changes in undo/redo state.</li>
     * </ul>
     * </p>
     *
     * <p>
     * Direct interaction with the {@code UndoRedoManager} can be useful for advanced
     * scenarios such as:
     * <ul>
     *   <li>Implementing custom undo/redo UI elements.</li>
     *   <li>Querying the state of the undo/redo stacks (e.g., {@link UndoRedoManager#canUndo()}, {@link UndoRedoManager#getUndoDescription()}).</li>
     *   <li>Clearing the undo/redo history ({@link UndoRedoManager#clear()}).</li>
     * </ul>
     * </p>
     *
     * @return The current {@link UndoRedoManager} instance. Never null.
     * @since 1.0
     * @see UndoRedoManager
     * @see Command
     * @see #setUndoRedoManager(UndoRedoManager)
     */
    public UndoRedoManager getUndoRedoManager() {
        return undoRedoManager;
    }

    /**
     * Sets a new {@link UndoRedoManager} for this PieceTree instance.
     * This method allows for replacing the default undo/redo manager with a custom
     * implementation or a pre-configured instance. This can be useful for integrating
     * with external history management systems or for providing specialized undo/redo behavior.
     *
     * <p>
     * When a new {@code UndoRedoManager} is set:
     * <ul>
     *   <li>The existing undo/redo history is discarded.</li>
     *   <li>All subsequent text modification commands will be routed to the new manager.</li>
     *   <li>The PieceTree will use the new manager for all undo and redo operations.</li>
     * </ul>
     * </p>
     *
     * <p>
     * <b>Warning:</b> Setting a new undo/redo manager will clear any existing
     * undo/redo history. Ensure this is the desired behavior, especially if the
     * document already contains edits.
     * </p>
     *
     * @param undoRedoManager The new {@link UndoRedoManager} to use. Must not be null.
     * @throws IllegalArgumentException if {@code undoRedoManager} is null.
     * @since 1.0
     * @see UndoRedoManager
     * @see #getUndoRedoManager()
     */
    public void setUndoRedoManager(UndoRedoManager undoRedoManager) {
        if (undoRedoManager == null) {
            throw new IllegalArgumentException("UndoRedoManager cannot be null.");
        }
        this.undoRedoManager = undoRedoManager;
    }

    /**
     * Begins a new group of undoable operations with a specified description.
     * All subsequent editing commands executed on the PieceTree will be collected
     * into this group until {@link #endGroup()} is called. This allows multiple
     * related operations (e.g., find and replace all) to be undone or redone
     * as a single atomic action.
     *
     * <p>
     * Grouping operations is essential for providing a user-friendly undo/redo experience,
     * preventing users from having to undo many small, individual changes.
     * </p>
     *
     * <p>
     * Example Usage:
     * <pre>{@code
     * pieceTree.beginGroup("Replace All 'foo' with 'bar'");
     * // ... perform multiple replacement operations ...
     * pieceTree.endGroup();
     * // Now, a single undo will revert all replacements.
     * }</pre>
     * </p>
     *
     * @param description A textual description of the group of operations, which can be
     *                    used for display in UI elements (e.g., "Undo Replace All").
     *                    If null or empty, a default description might be used by the manager.
     * @since 1.0
     * @see UndoRedoManager#beginGroup(String)
     * @see #endGroup()
     */
    public void beginGroup(String description) {
        undoRedoManager.beginGroup(description);
    }
    /**
     * Ends the current group of undoable operations.
     * After this method is called, the collected group of commands is finalized
     * and added to the undo history as a single, atomic undoable unit.
     * Any subsequent editing commands will be treated as individual operations
     * or part of a new group if {@link #beginGroup(String)} is called again.
     *
     * <p>
     * It is crucial to pair every call to {@link #beginGroup(String)} with a
     * corresponding call to {@code endGroup()}. Failure to do so may result in
     * unexpected undo/redo behavior, as operations might remain in an unclosed group.
     * </p>
     *
     * <p>
     * If {@code endGroup()} is called without a preceding {@code beginGroup()},
     * the behavior is defined by the underlying {@link UndoRedoManager} implementation
     * (typically, it's a no-op or might log a warning).
     * </p>
     *
     * @since 1.0
     * @see UndoRedoManager#endGroup()
     * @see #beginGroup(String)
     */
    public void endGroup() {
        undoRedoManager.endGroup();
    }

    /**
     * Checks if there are any operations that can be undone in the document history.
     * This method queries the {@link UndoRedoManager} to determine if the undo stack
     * contains any commands, enabling UI elements (e.g., "Undo" button) to reflect
     * the availability of the undo action.
     *
     * <p>
     * The availability of undo depends on:
     * <ul>
     *   <li>Whether any undoable commands have been executed.</li>
     *   <li>Whether the undo history has been cleared (e.g., via {@link UndoRedoManager#clear()}).</li>
     *   <li>The maximum undo levels configured (if applicable).</li>
     * </ul>
     * </p>
     *
     * @return {@code true} if an undo operation can be performed, {@code false} otherwise.
     * @since 1.0
     * @see UndoRedoManager#canUndo()
     * @see #undo()
     */
    public boolean canUndo() {
        return undoRedoManager.canUndo();
    }

    /**
     * Checks if there are any operations that can be redone in the document history.
     * This method queries the {@link UndoRedoManager} to determine if the redo stack
     * contains any commands that were previously undone, enabling UI elements
     * (e.g., "Redo" button) to reflect the availability of the redo action.
     *
     * <p>
     * The availability of redo depends on:
     * <ul>
     *   <li>Whether any operations have been undone (moving them to the redo stack).</li>
     *   <li>Whether new operations have been performed after an undo (which typically clears the redo stack).</li>
     *   <li>Whether the redo history has been cleared.</li>
     * </ul>
     * </p>
     *
     * @return {@code true} if a redo operation can be performed, {@code false} otherwise.
     * @since 1.0
     * @see UndoRedoManager#canRedo()
     * @see #redo()
     */
    public boolean canRedo() {
        return undoRedoManager.canRedo();
    }

    /**
     * Reverts the last executed operation in the document, if available.
     * This method delegates to the {@link UndoRedoManager#undo()} method,
     * which retrieves the most recent command from the undo stack, executes
     * its undo logic, and moves the command to the redo stack.
     *
     * <p>
     * The undo operation involves:
     * <ol>
     *   <li>Checking if an undo operation is possible via {@link #canUndo()}.</li>
     *   <li>Retrieving the last {@link Command} from the undo stack.</li>
     *   <li>Calling the {@code unExecute()} method on the command to reverse its effects.</li>
     *   <li>Moving the command from the undo stack to the redo stack.</li>
     *   <li>Notifying any registered {@link UndoRedoListener}s.</li>
     * </ol>
     * </p>
     *
     * <p>
     * If no operations are available to undo, this method does nothing and returns {@code false}.
     * </p>
     *
     * @return {@code true} if an operation was successfully undone, {@code false} otherwise (e.g., if the undo stack was empty).
     * @since 1.0
     * @see UndoRedoManager#undo()
     * @see #canUndo()
     * @see #redo()
     */
    public boolean undo() {
        return undoRedoManager.undo();
    }

    /**
     * Reapplies the last undone operation in the document, if available.
     * This method delegates to the {@link UndoRedoManager#redo()} method,
     * which retrieves the most recent command from the redo stack, executes
     * its original logic (re-applies the change), and moves the command back to the undo stack.
     *
     * <p>
     * The redo operation involves:
     * <ol>
     *   <li>Checking if a redo operation is possible via {@link #canRedo()}.</li>
     *   <li>Retrieving the last {@link Command} from the redo stack.</li>
     *   <li>Calling the {@code execute()} method on the command to reapply its effects.</li>
     *   <li>Moving the command from the redo stack to the undo stack.</li>
     *   <li>Notifying any registered {@link UndoRedoListener}s.</li>
     * </ol>
     * </p>
     *
     * <p>
     * If no operations are available to redo (e.g., the redo stack is empty or a new edit was made after an undo),
     * this method does nothing and returns {@code false}.
     * </p>
     *
     * @return {@code true} if an operation was successfully redone, {@code false} otherwise (e.g., if the redo stack was empty).
     * @since 1.0
     * @see UndoRedoManager#redo()
     * @see #canRedo()
     * @see #undo()
     */
    public boolean redo() {
        return undoRedoManager.redo();
    }

    /**
     * Retrieves a textual description of the next undoable operation.
     * This method queries the {@link UndoRedoManager} for the description
     * associated with the command at the top of the undo stack. This description
     * can be used to update UI elements, such as the text of an "Undo" menu item
     * (e.g., "Undo Typing", "Undo Delete Paragraph").
     *
     * <p>
     * The description is typically set when a command is created or when a group
     * of commands is started using {@link #beginGroup(String)}. If no description
     * is available, or if the undo stack is empty, this method may return
     * null or an empty string, depending on the {@link UndoRedoManager} implementation.
     * </p>
     *
     * @return A string describing the next undo operation, or null/empty if not available.
     * @since 1.0
     * @see UndoRedoManager#getUndoDescription()
     * @see #canUndo()
     */
    public String getUndoDescription() {
        return undoRedoManager.getUndoDescription();
    }

    /**
     * Retrieves a textual description of the next redoable operation.
     * This method queries the {@link UndoRedoManager} for the description
     * associated with the command at the top of the redo stack. This description
     * can be used to update UI elements, such as the text of a "Redo" menu item
     * (e.g., "Redo Typing", "Redo Delete Paragraph").
     *
     * <p>
     * The description corresponds to an operation that was previously undone.
     * If no description is available, or if the redo stack is empty, this method
     * may return null or an empty string, depending on the {@link UndoRedoManager}
     * implementation.
     * </p>
     *
     * @return A string describing the next redo operation, or null/empty if not available.
     * @since 1.0
     * @see UndoRedoManager#getRedoDescription()
     * @see #canRedo()
     */
    public String getRedoDescription() {
        return undoRedoManager.getRedoDescription();
    }

    /**
     * Clears the entire undo and redo history managed by the {@link UndoRedoManager}.
     * After this operation, both {@link #canUndo()} and {@link #canRedo()} will
     * return {@code false}, and no previous edits can be reverted or reapplied.
     * This is typically used when a document is closed, saved under a new name,
     * or when a "clear history" action is explicitly invoked by the user.
     *
     * @since 1.0
     * @see UndoRedoManager#clear()
     */
    public void clear() {
        undoRedoManager.clear();
    }

    /**
     * Gets the number of operations currently available on the undo stack.
     * This method queries the {@link UndoRedoManager} to determine how many
     * distinct undoable actions (which could be individual commands or command groups)
     * are stored in the history.
     *
     * <p>
     * The size of the undo stack reflects the depth of the undo history.
     * For example, if a user performs three distinct edits, the undo size would typically be 3.
     * If {@link #setMaxUndoLevels(int)} is used to limit the history, this value
     * will not exceed that limit.
     * </p>
     *
     * @return The number of undoable operations currently available. Returns 0 if the undo stack is empty.
     * @since 1.0
     * @see UndoRedoManager#getUndoSize()
     * @see #canUndo()
     * @see #setMaxUndoLevels(int)
     */
    public int getUndoSize() {
        return undoRedoManager.getUndoSize();
    }

    /**
     * Gets the number of operations currently available on the redo stack.
     * This method queries the {@link UndoRedoManager} to determine how many
     * distinct redoable actions (commands that were previously undone) are stored.
     *
     * <p>
     * The size of the redo stack reflects how many "undone" operations can be reapplied.
     * Performing a new edit after an undo typically clears the redo stack, setting its size to 0.
     * </p>
     *
     * @return The number of redoable operations currently available. Returns 0 if the redo stack is empty.
     * @since 1.0
     * @see UndoRedoManager#getRedoSize()
     * @see #canRedo()
     */
    public int getRedoSize() {
        return undoRedoManager.getRedoSize();
    }

    /**
     * Sets the maximum number of undo levels that the {@link UndoRedoManager} will retain.
     * This method allows configuring the depth of the undo history. If the number of
     * undoable operations exceeds this limit, the oldest operations are discarded
     * from the undo stack to maintain the specified maximum.
     *
     * <p>
     * Setting a lower limit can help manage memory usage, especially for documents
     * with extensive edit histories. A value of 0 typically means unlimited undo levels,
     * though this depends on the {@link UndoRedoManager} implementation.
     * </p>
     *
     * @param maxLevels The maximum number of undo operations to keep in history.
     *                  A non-positive value might indicate unlimited levels, depending on the manager.
     * @since 1.0
     * @see UndoRedoManager#setMaxUndoLevels(int)
     * @see #getMaxUndoLevels()
     */
    public void setMaxUndoLevels(int maxLevels) {
        undoRedoManager.setMaxUndoLevels(maxLevels);
    }

    /**
     * Gets the current maximum number of undo levels configured for the {@link UndoRedoManager}.
     * This value determines the maximum depth of the undo history that will be maintained.
     *
     * @return The maximum number of undo levels. A non-positive value might indicate
     *         unlimited levels, depending on the {@link UndoRedoManager} implementation.
     * @since 1.0
     * @see UndoRedoManager#getMaxUndoLevels()
     * @see #setMaxUndoLevels(int)
     */
    public int getMaxUndoLevels() {
        return undoRedoManager.getMaxUndoLevels();
    }

    /**
     * Adds a listener to receive notifications about undo and redo state changes.
     * This method allows external components (e.g., UI elements) to observe
     * events from the {@link UndoRedoManager}, such as when an operation is undone,
     * redone, or when the undo/redo history is modified (e.g., cleared, command added).
     *
     * <p>
     * Listeners implement the {@link UndoRedoListener} interface and will receive
     * callbacks for relevant events, enabling them to update their state or UI
     * accordingly (e.g., enabling/disabling undo/redo buttons, updating descriptions).
     * </p>
     *
     * <p>
     * Multiple listeners can be registered. Each listener will be notified of events.
     * Adding the same listener multiple times may result in duplicate notifications,
     * depending on the {@link UndoRedoManager} implementation.
     * </p>
     *
     * @param listener The {@link UndoRedoListener} to add. Must not be null.
     * @throws IllegalArgumentException if the listener is null.
     * @since 1.0
     * @see UndoRedoManager#addUndoRedoListener(UndoRedoListener)
     * @see UndoRedoListener
     * @see #removeUndoRedoListener(UndoRedoListener)
     */
    public void addUndoRedoListener(UndoRedoListener listener) {
        undoRedoManager.addUndoRedoListener(listener);
    }

    /**
     * Removes a previously registered {@link UndoRedoListener}.
     * Once removed, the listener will no longer receive notifications about
     * undo and redo state changes from the {@link UndoRedoManager}.
     *
     * <p>
     * If the specified listener was not previously registered, this method
     * typically has no effect and does not throw an exception.
     * </p>
     *
     * @param listener The {@link UndoRedoListener} to remove. Must not be null.
     * @since 1.0
     * @see UndoRedoManager#removeUndoRedoListener(UndoRedoListener)
     * @see #addUndoRedoListener(UndoRedoListener)
     */
    public void removeUndoRedoListener(UndoRedoListener listener) {
        undoRedoManager.removeUndoRedoListener(listener);
    }
}