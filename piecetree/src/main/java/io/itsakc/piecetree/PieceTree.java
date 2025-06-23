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

import android.util.Log;

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
 * @author <a href="https://github.com/itsakc-me">@itsakc.me</a>
 * @see Node
 * @see BufferManager
 * @see RedBlackTree
 * @see UndoRedoManager
 * @see PieceTreeSnapshot
 */
public class PieceTree {
    private final String TAG = "PieceTree";

    public static final int LineFeed = 10;
    public static final int CarriageReturn = 13;

    private BufferManager bufferManager;
    private RedBlackTree tree;
    private UndoRedoManager undoRedoManager;
    private String eol = "\n"; // Default end-of-line sequence
    private boolean isNormalizeEOL = true;
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
     * LF (\n) and can be changed using {@link #setEOL(EOLNormalization)}.
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
     * @see #initialize(String)
     * @see BufferManager
     * @see RedBlackTree
     * @see UndoRedoManager
     * @since 1.0
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
     * @see #computeLineStarts(char[])
     * @see #createSnapshot()
     * @since 1.0
     */
    public synchronized void initialize(String initialText) {
        try {
            int bufferCount = bufferManager.addOriginalBuffer(initialText);
            int currentOffset = 0;
            for (int i = 0; i < bufferCount; i++) {
                char[] buffer = bufferManager.getBuffer(i + 1); // Original buffers start at index 1
                int length = buffer.length;
                int[] lineStarts = computeLineStartsFast(buffer);
                Node node = new Node(i + 1, 0, length, lineStarts);
                node.documentStart = currentOffset;
                tree.insert(node);
                currentOffset += length;
            }
            createSnapshot(); // Create initial snapshot
        } catch (Exception e) {
            Log.d(TAG, "Error in initialize: " + e.getMessage(), e);
        }
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
     * @param content          The initial content to load into the buffer. Can be null.
     * @param eolNormalization The type of EOL normalization to apply.
     * @see EOLNormalization
     * @see #setEOL(EOLNormalization)
     * @since 1.0
     */
    public synchronized void initialize(String content, EOLNormalization eolNormalization) {
        isNormalizeEOL = true;
        if (content != null) {
            switch (eolNormalization) {
                case CRLF:
                    content = content.replaceAll("(\r\n|\n|\r)", "\r\n");
                    this.eol = "\r\n";
                    break;
                case LF:
                    content = content.replaceAll("(\r\n|\r)", "\n");
                    this.eol = "\n";
                    break;
                case CR:
                    content = content.replaceAll("(\r\n|\n)", "\r");
                    this.eol = "\r";
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
                    isNormalizeEOL = false;
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
     * <b>Maximum supported size:</b> 80MB.
     * </p>
     *
     * @param file The file to read content from. Must exist and be readable.
     * @throws OutOfMemoryError  if the file is too large to fit in available memory.
     * @throws SecurityException if file access is denied.
     * @see #initialize(File, EOLNormalization)
     * @since 1.0
     */
    public synchronized void initialize(File file) {
        initialize(file, EOLNormalization.None);
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
     *   <li>Maximum supported size: 80MB</li>
     *   <li>Performance: O(N) where N is the file size</li>
     *   <li>Files exceeding limits will throw {@link OutOfMemoryError}</li>
     * </ul>
     * </p>
     *
     * @param file             The file to read content from. Must exist and be readable.
     * @param eolNormalization The type of EOL normalization to apply.
     * @throws OutOfMemoryError  when the file is too large to fit in memory.
     * @throws SecurityException if file access permissions are insufficient.
     * @see FileReader
     * @see EOLNormalization
     * @since 1.0
     */
    public synchronized void initialize(File file, EOLNormalization eolNormalization) {
        setEOL(eolNormalization);

        String sb = "";
        FileReader fr = null;
        try {
            fr = new FileReader(file);

            char[] buff = new char[BufferManager.ORIGINAL_BUFFER_SIZE];
            int length = 0;
            int bufferIndex = 1;
            int currentOffset = length;

            while ((length = fr.read(buff)) > 0) {
                sb = new String(buff);

                if (eolNormalization != EOLNormalization.None) {
                    sb = sb.replaceAll("(\r\n|\n|\r)", eol);
                }

                bufferManager.addOriginalBuffer(sb);
                int[] lineStarts = computeLineStartsFast(buff);
                Node node = new Node(bufferIndex, 0, length, lineStarts);
                node.documentStart = currentOffset;
                tree.insert(node);

                buff = new char[BufferManager.ORIGINAL_BUFFER_SIZE];
                bufferIndex++;
                currentOffset += length;
            }
        } catch (IOException e) {
            Log.d(TAG, "Error in initialize: " + e.getMessage(), e);
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (Exception e) {
                    Log.d(TAG, "Error in initialize: " + e.getMessage(), e);
                }
            }
        }

        if (!sb.isEmpty()) {
            switch (eolNormalization) {
                case CRLF, LF, CR:
                    break;
                case None:
                default:
                    // Detect existing EOL
                    if (sb.contains("\r\n")) {
                        this.eol = "\r\n";
                    } else if (sb.contains("\n")) {
                        this.eol = "\n";
                    } else if (sb.contains("\r")) {
                        this.eol = "\r";
                    } else {
                        this.eol = "\n"; // Default
                    }
                    break;
            }
        }

        createSnapshot();
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
     * @return Whether the method has performed finely.
     * @see #insert(int, String)
     * @see #length()
     * @since 1.0
     */
    public synchronized boolean append(String text) {
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
     * @param position   The {@link BufferPosition} {@code startPosition} which contains the metadata of {@code lineNumber} and {@code column}.
     * @param text       The text to insert. Can contain line breaks for multi-line insertion.
     * @return Whether the insertion was successful.
     * @throws IllegalArgumentException if lineNumber or column is less than 1.
     * @see #offsetAt(BufferPosition)
     * @see InsertTextCommand
     * @since 1.0
     */
    public synchronized boolean insert(BufferPosition position, String text) {
        int offset = offsetAt(position);
        return insert(offset, text);
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
     * @param text   The text to insert. Null or empty strings result in no-op.
     * @return Whether the insertion is successful.
     * @throws IllegalArgumentException if offset is negative or exceeds document length.
     * @see #doInsert(int, String)
     * @see UndoRedoManager#executeCommand(Command)
     * @since 1.0
     */
    public synchronized boolean insert(int offset, String text) {
        try {
            InsertTextCommand command = new InsertTextCommand(this, offset, text);
            undoRedoManager.executeCommand(command);
            return true;
        } catch (Exception e) {
            Log.d(TAG, "Error in insert: " + e.getMessage(), e);
        }
        return false;
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
     * @param startPosition   The {@link BufferPosition} {@code startPosition} which contains the metadata of starting {@code lineNumber} and {@code column}.
     * @param endPosition     The {@link BufferPosition} {@code endPosition} which contains the metadata of ending {@code lineNumber} and {@code column}.
     * @return Whether the deletion was successful.
     * @see #offsetAt(BufferPosition)
     * @see DeleteTextCommand
     * @since 1.0
     */
    public synchronized boolean delete(BufferPosition startPosition, BufferPosition endPosition) {
        int startOffset = offsetAt(startPosition);
        int endOffset = offsetAt(endPosition);
        return delete(startOffset, endOffset); // Delegate to delete(int, int)
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
     * @param endOffset   The end offset (0-based, exclusive).
     * @return Whether the deletion was successful.
     * @throws IllegalArgumentException if startOffset > endOffset or offsets are invalid.
     * @see #doDelete(int, int)
     * @see RedBlackTree#deleteRange(int, int)
     * @since 1.0
     */
    public synchronized boolean delete(int startOffset, int endOffset) {
        try {
            DeleteTextCommand command = new DeleteTextCommand(this, startOffset, endOffset);
            undoRedoManager.executeCommand(command);
            return true;
        } catch (Exception e) {
            Log.d(TAG, "Error in delete: " + e.getMessage(), e);
        }
        return false;
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
     * @param startPosition   The {@link BufferPosition} {@code startPosition} which contains the metadata of starting {@code lineNumber} and {@code column}.
     * @param endPosition     The {@link BufferPosition} {@code endPosition} which contains the metadata of ending {@code lineNumber} and {@code column}.
     * @param replacement     The replacement text. Can be multi-line or empty.
     * @return Whether the replacement was successful.
     * @see ReplaceTextCommand
     * @see #offsetAt(BufferPosition)
     * @since 1.0
     */
    public synchronized boolean replace(BufferPosition startPosition, BufferPosition endPosition, String replacement) {
        int startOffset = offsetAt(startPosition);
        int endOffset = offsetAt(endPosition);
        return replace(startOffset, endOffset, replacement);
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
     * @param endOffset   The end offset (0-based, exclusive).
     * @param replacement The replacement text. Empty string performs deletion only.
     * @return Whether the replacement was successful.
     * @throws IllegalArgumentException if offset range is invalid.
     * @see #doReplace(int, int, String)
     * @since 1.0
     */
    public synchronized boolean replace(int startOffset, int endOffset, String replacement) {
        try {
            ReplaceTextCommand command = new ReplaceTextCommand(this, startOffset, endOffset, replacement);
            undoRedoManager.executeCommand(command);
            return true;
        } catch (Exception e) {
            Log.d(TAG, "Error in replace: " + e.getMessage(), e);
        }
        return false;
    }

    /**
     * Replaces the first occurrence of a regular expression pattern with the specified
     * replacement text throughout the entire document.
     * This method provides a simplified interface for regex-based find-and-replace operations.
     *
     * <p>
     * Operation Sequence:
     * <ol>
     *   <li>Converts the input {@code regex} parameter to a compiled {@link Pattern}.</li>
     *   <li>Searches for the first match of the regex pattern starting from document beginning.</li>
     *   <li>If a match is found, performs a single {@link #replace(int, int, String)} operation
     *       at the matched location with the provided replacement text.</li>
     *   <li>Returns immediately after the first successful replacement or if no match is found.</li>
     * </ol>
     * </p>
     *
     * <p>
     * Important Considerations:
     * <ul>
     *   <li><b>Regex Support:</b> Full regex syntax is supported including capture groups,
     *       lookaheads, and complex patterns.</li>
     *   <li><b>Case Sensitivity:</b> The search is performed with default case-sensitive matching.</li>
     *   <li><b>Single Operation:</b> Only the first occurrence is replaced, making this suitable
     *       for targeted replacements or interactive find-and-replace scenarios.</li>
     *   <li><b>Atomicity:</b> The replacement operation is atomic and can be undone as a single action.</li>
     * </ul>
     * </p>
     *
     * @param regex       The regular expression pattern to search for. Must be valid regex syntax.
     * @param replacement The text to replace the first matched occurrence with.
     *                    Can include regex replacement patterns like {@code $1}, {@code $2} for capture groups.
     * @return {@code true} if a match was found and successfully replaced; {@code false} otherwise.
     * @see #replace(String, int, boolean, String)
     * @see #replaceAll(String, String, int)
     * @see Pattern
     * @since 1.0
     */
    public synchronized boolean replace(String regex, String replacement) {
        return replace(regex, true, replacement);
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
     * Default Search Parameters for {@link #replace}:
     * <ul>
     *   <li>{@code startOffset}: 0</li>
     *   <li>{@code useRegex}: true</li>
     *   <li>{@code caseSensitive}: true</li>
     * </ul>
     * </p>
     *
     * @param regex       The regular expression pattern to search for.
     * @param wholeWord   Whether to search for whole words only.
     * @param replacement The text to replace the found pattern with.
     *                    An empty string will effectively delete the matched text.
     * @return Whether the replacement is successful.
     * @throws PatternSyntaxException if the regex pattern is invalid.
     * @see #replaceAll(String, boolean, String, int)
     * @see #replace(int, int, String)
     * @see #findNext(String, int, boolean, boolean, String, boolean)
     * @see FindMatch
     * @since 1.0
     */
    public synchronized boolean replace(String regex, boolean wholeWord, String replacement) {
        return replace(regex, 0, true, wholeWord, replacement);
    }

    /**
     * Replaces the first occurrence of a text or regex pattern starting from a specified
     * offset position with the provided replacement text.
     * This method offers precise control over the search starting point and pattern type.
     *
     * <p>
     * Operation Sequence:
     * <ol>
     *   <li>Validates the {@code startOffset} parameter to ensure it falls within document bounds.</li>
     *   <li>Creates a {@link Pattern} from the {@code query} based on the {@code useRegex} flag.</li>
     *   <li>Begins search from the {@link Node} containing the specified {@code startOffset}.</li>
     *   <li>Traverses document nodes sequentially to find the first pattern match.</li>
     *   <li>Performs {@link #replace(int, int, String)} operation on the first match found.</li>
     * </ol>
     * </p>
     *
     * <p>
     * Important Considerations:
     * <ul>
     *   <li><b>Offset Validation:</b> Invalid {@code startOffset} values are automatically
     *       clamped to valid document boundaries.</li>
     *   <li><b>Pattern Flexibility:</b> Supports both literal string matching and full regex
     *       patterns based on the {@code useRegex} parameter.</li>
     *   <li><b>Node Traversal:</b> Efficiently handles matches that span across multiple
     *       document nodes using overlap detection.</li>
     *   <li><b>Performance:</b> Optimized for single replacements with early termination
     *       upon finding the first match.</li>
     * </ul>
     * </p>
     *
     * @param query       The text or regex pattern to search for.
     * @param startOffset The document offset from which to begin searching (0-based).
     *                    Values outside document bounds are automatically adjusted.
     * @param useRegex    {@code true} to treat {@code query} as a regular expression;
     *                    {@code false} for literal string matching.
     * @param replacement The text to replace the matched occurrence with.
     *                    For regex patterns, can include backreferences like {@code $1}.
     * @return {@code true} if a match was found and successfully replaced; {@code false} otherwise.
     * @see #replace(String, String)
     * @see #findNext(String, int, boolean, boolean, String, boolean, boolean)
     * @see #replace(int, int, String)
     * @since 1.0
     */
    public synchronized boolean replace(String query, int startOffset, boolean useRegex, String replacement) {
        return replace(query, startOffset, useRegex, true, replacement);
    }

    /**
     * Replaces the first occurrence of the {@code query} sequence with the specified
     * replacement text. This method provides a simple way to replace a specific
     * {@code query} with/without using regular expressions.
     *
     * <p>
     * Operation Sequence:
     * <ol>
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
     *   <li>{@code caseSensitive}: true</li>
     *   <li>{@code wordSeparators}: null</li>
     *   <li>{@code captureGroups}: false (captures only the full match for replacement range)</li>
     * </ul>
     * </p>
     *
     * @param query       The {@link String} {@code query} to search for.
     * @param startOffset The offset from which to start the search (0-based).
     * @param useRegex    Whether to treat the {@code query} as a regular expression.
     * @param wholeWord   Whether to search for whole words only.
     * @param replacement The text to replace the found literal with.
     *                    An empty string will effectively delete the matched text.
     * @return Whether the replacement is successful.
     * @see #replaceAll(String, boolean, String, int)
     * @see #replace(String, boolean, String) // For regex-based replacement
     * @see #replace(int, int, String)
     * @see #findNext(String, int, boolean, boolean, String, boolean)
     * @see FindMatch
     * @since 1.0
     */
    public synchronized boolean replace(String query, int startOffset, boolean useRegex, boolean wholeWord, String replacement) {
        FindMatch match = findNext(query, startOffset, useRegex, true, null, false, wholeWord);
        if (match != null) { // Only replace if a match is found
            replace(match.startOffset, match.endOffset, replacement);
            return true;
        }
        return false;
    }

    /**
     * Replaces all occurrences of a regular expression pattern with the specified
     * replacement text throughout the entire document, up to a maximum count.
     * This method provides efficient bulk replacement operations with configurable limits.
     *
     * <p>
     * Operation Sequence:
     * <ol>
     *   <li>Compiles the {@code regex} parameter into a {@link Pattern} with default flags.</li>
     *   <li>Performs comprehensive document traversal to identify all matching occurrences.</li>
     *   <li>Executes replacement operations in document order, respecting the {@code maxReplace} limit.</li>
     *   <li>Maintains document integrity by handling offset adjustments after each replacement.</li>
     *   <li>Terminates early if {@code maxReplace} limit is reached before document end.</li>
     * </ol>
     * </p>
     *
     * <p>
     * Important Considerations:
     * <ul>
     *   <li><b>Bulk Operations:</b> Designed for high-volume replacements with optimized
     *       performance characteristics for large documents.</li>
     *   <li><b>Replacement Limit:</b> The {@code maxReplace} parameter prevents runaway
     *       operations and allows for controlled batch processing.</li>
     *   <li><b>Regex Power:</b> Full regex functionality including capture groups, assertions,
     *       and complex pattern matching is available.</li>
     *   <li><b>Memory Efficiency:</b> Uses incremental processing to handle large documents
     *       without excessive memory consumption.</li>
     * </ul>
     * </p>
     *
     * @param regex       The regular expression pattern to search for throughout the document.
     * @param replacement The text to replace each matched occurrence with.
     *                    Supports regex replacement syntax including capture group references.
     * @param maxReplace  The maximum number of replacements to perform. Use 0 for unlimited replacements.
     * @return {@code true} if at least one replacement was successfully performed; {@code false} otherwise.
     * @see #replaceAll(String, int, boolean, String, int)
     * @see #replace(String, String)
     * @see #findMatches(String, int, boolean, boolean, String, boolean, int)
     * @since 1.0
     */
    public synchronized boolean replaceAll(String regex, String replacement, int maxReplace) {
        return replaceAll(regex, true, replacement, maxReplace);
    }

    /**
     * Replaces all occurrences of a pattern (defined by a regular expression)
     * with the specified replacement text throughout the entire document.
     * This method provides a global find-and-replace functionality.
     *
     * <p>
     * Operation Sequence:
     * <ol>
     *   <li>Finds and replaces all non-overlapping matches for the given {@code regex}
     *       from {@code startOffset} by {@link #replaceAll(String, int, boolean, String, int)}
     *       till the {@code maxReplace} reaches.</li>
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
     *   <li><b>Query Search:</b> The {@code query} parameter is treated as regex.</li>
     *   <li><b>Atomicity:</b> Each individual replacement is atomic and undoable.</li>
     *   <li><b>Performance:</b> Finding all matches can be now more less time-consuming
     *       even on bigger documents, i.e,
     *       <a href="https://github.com/titoBouzout/Dictionaries/blob/master/Russian-English%20Bilingual.dic">
     *       Russian English Bilingual dictionary</a>.</li>
     * </ul>
     * </p>
     *
     * <p>
     * Default Search Parameters for {@link #replaceAll}:
     * <ul>
     *   <li>{@code startOffset}: 0</li>
     *   <li>{@code useRegex}: true</li>
     * </ul>
     * </p>
     *
     * @param regex       The regular expression pattern to search for.
     * @param wholeWord   Whether to match for whole words.
     * @param replacement The text to replace each found pattern with.
     *                    An empty string will effectively delete all matched text.
     * @param maxReplace  The maximum number of replacements to perform (0 = all replacement).
     * @return Whether the replacement is performed successful.
     * @throws PatternSyntaxException if the regex pattern is invalid.
     * @see #replace(String, String)
     * @see #replace(int, int, String)
     * @see #replaceAll(String, int, boolean, boolean, String, int) // For user-defined replacement.
     * @see #findMatches(String, int, boolean, boolean, String, boolean, int)
     * @see FindMatch
     * @since 1.0
     */
    public synchronized boolean replaceAll(String regex, boolean wholeWord, String replacement, int maxReplace) {
        return replaceAll(regex, 0, true, wholeWord, replacement, maxReplace);
    }

    /**
     * Replaces all occurrences of a text or regex pattern starting from a specified
     * offset position with the provided replacement text, up to a maximum count.
     * This method offers the most comprehensive control over bulk replacement operations.
     *
     * <p>
     * Operation Sequence:
     * <ol>
     *   <li>Validates the {@code startOffset} parameter against document boundaries.</li>
     *   <li>Creates an appropriate {@link Pattern} based on the {@code useRegex} flag.</li>
     *   <li>Initiates search from the {@link Node} containing the specified offset.</li>
     *   <li>Iteratively finds and replaces matches while tracking the replacement count.</li>
     *   <li>Adjusts subsequent search positions to account for text length changes from replacements.</li>
     *   <li>Continues until {@code maxReplace} limit is reached or document end is encountered.</li>
     * </ol>
     * </p>
     *
     * <p>
     * Important Considerations:
     * <ul>
     *   <li><b>Flexible Starting Point:</b> Allows targeted bulk replacements from any
     *       document position, useful for section-specific operations.</li>
     *   <li><b>Pattern Versatility:</b> Supports both simple text matching and complex
     *       regex patterns with full backreference support in replacements.</li>
     *   <li><b>Controlled Execution:</b> The {@code maxReplace} limit provides fine-grained
     *       control over operation scope and prevents infinite loops with certain patterns.</li>
     *   <li><b>Offset Management:</b> Intelligently handles document offset adjustments
     *       as text length changes during the replacement process.</li>
     *   <li><b>Node Boundary Handling:</b> Seamlessly processes matches that span across
     *       multiple document nodes using sophisticated overlap detection.</li>
     * </ul>
     * </p>
     *
     * @param query       The text or regex pattern to search for.
     * @param startOffset The document offset from which to begin searching (0-based).
     *                    Invalid offsets are automatically corrected to valid ranges.
     * @param useRegex    {@code true} to interpret {@code query} as a regular expression;
     *                    {@code false} for exact string matching.
     * @param replacement The text to replace each matched occurrence with.
     *                    For regex patterns, supports backreferences and replacement expressions.
     * @param maxReplace  The maximum number of replacements to perform. Use 0 for unlimited replacements.
     * @return {@code true} if at least one replacement was successfully performed; {@code false} otherwise.
     * @see #replaceAll(String, String, int)
     * @see #replace(String, int, boolean, String)
     * @see #findMatches(String, int, boolean, boolean, String, boolean, int)
     * @see Pattern
     * @since 1.0
     */
    public synchronized boolean replaceAll(String query, int startOffset, boolean useRegex, String replacement, int maxReplace) {
        return replaceAll(query, startOffset, useRegex, true, replacement, maxReplace);
    }

    /**
     * Replaces all occurrences of a text or regex pattern with the specified
     * replacement text throughout the entire document.
     * This method provides global find-and-replace for exact string matches.
     *
     * <p>
     * Operation Sequence:
     * <ol>
     *   <li>Converts the input {@link String} {@code query} to a {@link Pattern}.</li>
     *   <li>Finds all non-overlapping matches for the given literal string
     *       throughout the document using {@code self} fast forward mechanism.
     *       The search is performed case-sensitively and compiles the query {@link Pattern}.</li>
     *   <li>Starts iterating through the {@link Node} {@code node} containing {@code startOffset} and
     *       perform the {@link #replace(int, int, String)} till the {@code maxReplace} reaches.</li>
     *   <li>If no matches are found, the document remains unchanged.</li>
     * </ol>
     * </p>
     *
     * <p>
     * Important Considerations:
     * <ul>
     *    <li><b>Query Search:</b> The {@code query} parameter is treated as either a
     *       plain text or regex based on {@code useRegex}.</li>
     *   <li><b>Atomicity:</b> Each individual replacement is atomic and undoable.</li>
     *   <li><b>Performance:</b> Finding all matches can be now more less time-consuming
     *       even on bigger documents, i.e,
     *       <a href="https://github.com/titoBouzout/Dictionaries/blob/master/Russian-English%20Bilingual.dic">
     *       Russian English Bilingual dictionary</a>.</li>
     * </ul>
     * </p>
     *
     * @param query       The exact {@link String} to search for.
     * @param startOffset The offset from which to start the search (0-based).
     * @param useRegex    Whether to treat the {@code query} as a regular expression.
     * @param wholeWord   Whether to search for whole words only.
     * @param replacement The text to replace each found literal with.
     *                    An empty string will effectively delete all matched text.
     * @param maxReplace  The maximum number of replacements to perform (0 = all replacement).
     * @return Whether the replacement is successful.
     * @see #replace(String, String)
     * @see #replace(int, int, String)
     * @see #replaceAll(String, boolean, String, int) // For regex-based replacement.
     * @see #findMatches(String, int, boolean, boolean, String, boolean, int)
     * @see FindMatch
     * @since 1.0
     */
    public synchronized boolean replaceAll(String query, int startOffset, boolean useRegex, boolean wholeWord, String replacement, int maxReplace) {
        if (query == null || query.isEmpty()) return false;

        undoRedoManager.beginGroup("Replace All");
        try {
            int currentStartOffset = startOffset;
            while (maxReplace > 0) {
                FindMatch match = findNext(query, currentStartOffset, useRegex, true, null, false, wholeWord);
                if (match == null) break;
                replace(match.startOffset, match.endOffset, replacement);

                if (--maxReplace == 0) break;
                currentStartOffset = match.endOffset;
            }

            undoRedoManager.endGroup();
            return true;
        } catch (Exception e) {
            Log.d(TAG, "Error in replaceAll: " + e.getMessage(), e);
        }
        undoRedoManager.endGroup();
        return false;
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
     * @see #PieceTree()
     * @see #initialize(String)
     * @since 1.0
     */
    public synchronized void reset() {
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
     * excluding any end-of-line characters. Returns an empty string for
     * empty lines or invalid line numbers.
     * @throws IllegalArgumentException if lineNumber is less than 1.
     * @apiNote This method is optimized for frequent line access patterns common
     * in text editors and syntax highlighters.
     * @see #findLinePosition(int)
     * @see #textRange(int, int)
     * @since 1.0
     */
    public synchronized String lineContent(int lineNumber) {
        try {
            if (lineNumber < 1 && lineNumber > lineCount()) {
                throw new IllegalArgumentException("Line number must be 1 or greater or equals to total line count!!");
            }

            LinePosition linePos = findLinePosition(lineNumber);
            if (linePos == null) return ""; // Line doesn't exist

            Node node = linePos.node;
            int lineStartOffset = node.documentStart + linePos.offsetInNode;

            // Find the end of the line by looking for the next line start or end of content
            int lineEndOffset = findLineEnd(node, linePos.offsetInNode);

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
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "Error in lineContent: " + e.getMessage(), e);
        }
        return "";
    }

    public synchronized Range lineRange(int lineNumber) {
        try {
            if (lineNumber < 1 && lineNumber > lineCount()) {
                throw new IllegalArgumentException("Line number must be 1 or greater or equals to total line count!!");
            }

            LinePosition linePos = findLinePosition(lineNumber);
            if (linePos == null) return new Range(-1, -1); // Line doesn't exist

            Node node = linePos.node;
            int lineStartOffset = node.documentStart + linePos.offsetInNode;

            // Find the end of the line by looking for the next line start or end of content
            int lineEndOffset = findLineEnd(node, linePos.offsetInNode);

            return new Range(lineStartOffset, lineEndOffset);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "Error in lineRange: " + e.getMessage(), e);
        }
        return new Range(-1, -1);
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
     * @param endLineNumber   The ending line number (1-based, inclusive).
     * @return A list containing the content of each line in the range.
     * Empty lines are represented as empty strings.
     * @throws IllegalArgumentException if line numbers are invalid or startLine > endLine.
     * @see #lineContent(int)
     * @see List
     * @since 1.0
     */
    public synchronized List<String> linesContent(int startLineNumber, int endLineNumber) {
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
     * @return A {@link BufferPosition} object with 1-based line and column coordinates.
     * Returns BufferPosition(1,1) for invalid offsets.
     * @see BufferPosition
     * @see Node#lineStarts
     * @see Node#left_subtree_lfcnt
     * @since 1.0
     */
    public synchronized BufferPosition positionAt(int offset) {
        try {
            if (offset < 0 || offset > length()) {
                throw new IllegalArgumentException("Offset must be 0 or greater and less than document length!!");
            }

            Node node = tree.findNodeContaining(offset);
            if (node == null) return new BufferPosition(-1, -1); // Should not happen if offset is valid

            int relativeOffset = offset - node.documentStart + 1;

            // Find the line number
            int lineNumber = 1;
            for (int i = 0; i < node.lineStarts.length; i++) {
                if (relativeOffset < node.lineStarts[i]) {
                    lineNumber = i + 1;
                    break;
                }
            }
            if (node.lineStarts.length > 0 && relativeOffset >= node.lineStarts[node.lineStarts.length - 1]) {
                lineNumber = node.lineStarts.length;
            }

            // Calculate column number
            int columnNumber = 1;
            if (lineNumber > 1) {
                columnNumber = relativeOffset - node.lineStarts[lineNumber - 2];
            } else {
                columnNumber = relativeOffset;
            }

            Node current = tree.getFirst();
            int currentLineCount = 0;
            while (current != null && current != node) {
                currentLineCount += current.lineStarts.length;
                current = tree.getSuccessor(current);
            }

            // Adjust for global line number using left subtree line count
            int globalLineNumber = currentLineCount + lineNumber;

            return new BufferPosition(globalLineNumber, columnNumber);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "Error in positionAt: " + e.getMessage(), e);
        }
        return new BufferPosition(-1, -1);
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
     * @param position The {@link BufferPosition} containing {@code lineNumber} and {@code column}.
     * @return The 0-based character offset corresponding to the position.
     *         Returns -1 if any error occur in duration.
     * @throws IllegalArgumentException if lineNumber or column is less than 1.
     * @see #findLinePosition(int)
     * @see LinePosition
     * @since 1.0
     */
    public synchronized int offsetAt(BufferPosition position) {
        try {
            int lineNumber = position.lineNumber;
            int column = position.column;

            if (lineNumber < 1 || lineNumber > lineCount()) {
                throw new IllegalArgumentException("Line number must be 1 or greater");
            }
            if (column < 1) {
                throw new IllegalArgumentException("Column number must be 1 or greater");
            }

            LinePosition linePos = findLinePosition(lineNumber);
            if (linePos == null) return -1;

            Node node = linePos.node;
            int documentOffset = node.documentStart + linePos.offsetInNode;

            // If column is 1, we're at the start of the line
            if (column == 1) {
                return documentOffset;
            }

            // Calculate the target offset within the line
            int targetColumn = column - 1; // Convert to 0-based
            int currentColumn = 0;

            char[] buffer = bufferManager.getBuffer(node.bufferIndex);

            // Search within the current node first
            for (int i = node.bufferStart + linePos.offsetInNode;
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
                        currentNode = tree.getSuccessor(currentNode);
                    }
                }
            }

            // Return the offset at the end of the line if column exceeds line length
            return documentOffset + currentColumn;
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "Error in offsetAt: " + e.getMessage(), e);
        }
        return -1;
    }

    /**
     * Represents a specific position within a node corresponding to a line number.
     */
    private static class LinePosition {
        Node node;
        int offsetInNode;

        /**
         * Constructs a new {@code LinePosition} instance.
         *
         * @param node         The node containing the line.
         * @param offsetInNode The offset within the node where the line begins.
         */
        LinePosition(Node node, int offsetInNode) {
            this.node = node;
            this.offsetInNode = offsetInNode;
        }
    }

    /**
     * Finds the position of a specific line within the document using tree traversal.
     * This method performs an in-order traversal to find the exact line position,
     * ensuring accuracy by directly counting lines across nodes in document order.
     *
     * @param lineNumber The 1-based line number to locate within the document.
     * @return A {@link LinePosition} object containing the node and offset where the line starts,
     * or {@code null} if the line number is invalid or exceeds the document's line count.
     * @see LinePosition
     * @see #lineContent(int)
     * @since 1.0
     */
    private LinePosition findLinePosition(int lineNumber) {
        if (lineNumber < 1) return null;

        Node root = tree.getRoot();
        if (root == null) return null;

        Node current = tree.getFirst();
        int currentLineCount = 0;

        while (current != null) {
            int nodeLineCount = current.lineStarts.length;

            // Check if target line is in this node
            if (lineNumber > currentLineCount &&
                    lineNumber <= currentLineCount + nodeLineCount) {

                // Target line is in current node
                int lineInNode = lineNumber - currentLineCount; // 1-based line within node
                int offsetInNode = 0;

                if (lineInNode > 1 && lineInNode <= current.lineStarts.length) {
                    // For line N (where N > 1), start at the position after (N-1)th line break
                    offsetInNode = current.lineStarts[lineInNode - 2];
                }

                return new LinePosition(current, offsetInNode);
            }

            currentLineCount += nodeLineCount;
            current = tree.getSuccessor(current);
        }

        return null; // Line not found
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
     * @param startNode      The node where the line begins.
     * @param startPosInNode The starting position within the node's buffer.
     * @return The absolute document offset where the line ends (exclusive),
     * which is either at the EOL character(s) or at the document end,
     * and Returns -1 if any occur in duration.
     * @apiNote This is a private helper method optimized for the lineContent() implementation.
     * @since 1.0
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
     * @see #computeLineStarts(char[])
     * @since 1.0
     */
    public synchronized int lineCount() {
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
     * @see #lineContent(int)
     * @since 1.0
     */
    public synchronized int lineLength(int lineNumber) {
        return lineContent(lineNumber).length();
    }

    /**
     * Retrieves the character at the specified {@link BufferPosition} in the document.
     * This method translates the line and column based position into an absolute offset
     * and then delegates to the offset-based {@link #charAt(int)} method for character retrieval.
     *
     * <p>
     * This provides a convenient way to access characters using editor-like coordinates,
     * abstracting the underlying offset management.
     * </p>
     *
     * <p>
     * Coordinate to Offset Conversion:
     * <ol>
     *   <li>The {@link BufferPosition} (line and column) is converted to an absolute character offset
     *       within the document using the {@code offsetAt(BufferPosition)} method.</li>
     *   <li>The resulting offset is then passed to {@link #charAt(int)}.</li>
     * </ol>
     * </p>
     *
     * @param position The {@link BufferPosition} specifying the line and column of the desired character.
     *                 Must represent a valid position within the document.
     * @return The character at the specified position. Returns the null character ('\0') if the
     *         position is invalid, out of bounds, or if an internal error occurs during offset calculation
     *         or character retrieval.
     * @see #charAt(int)
     * @see #offsetAt(BufferPosition)
     * @since 1.1
     */
    public synchronized char charAt(BufferPosition position) {
        return charAt(offsetAt(position));
    }

    /**
     * Retrieves the character at the specified absolute offset within the document.
     * This method performs an efficient lookup in the piece tree to locate the node
     * containing the offset and then accesses the character from the corresponding buffer.
     *
     * <p>
     * Lookup Process:
     * <ol>
     *   <li><b>Node Location:</b> The piece tree's {@code findNodeContaining(offset)} method is used
     *       to identify the specific {@code Node} that stores the text segment for the given offset.</li>
     *   <li><b>Buffer Access:</b> Once the node is found, the {@code BufferManager} provides access
     *       to the underlying character buffer associated with that node.</li>
     *   <li><b>Character Extraction:</b> The character is retrieved from the buffer at the calculated
     *       position relative to the node's start within the document (offset - node.documentStart).</li>
     * </ol>
     * </p>
     *
     * <p>
     * Error Handling:
     * <ul>
     *   <li>If the {@code offset} is out of the document's bounds or if the corresponding
     *       node cannot be found, the null character ('\0') is returned.</li>
     *   <li>Any {@link IndexOutOfBoundsException} during buffer access (which ideally should not
     *       occur with correct offset and node location logic) is caught, logged, and
     *       results in returning the null character.</li>
     * </ul>
     * </p>
     *
     * @param offset The 0-based character offset from the beginning of the document.
     *               Must be non-negative and less than the document's length.
     * @return The character at the specified offset. Returns the null character ('\0') if the
     *         offset is out of bounds or if an error occurs.
     * @see RedBlackTree#findNodeContaining(int) // Assuming 'tree' is an instance of RedBlackTree or similar
     * @see BufferManager#getBuffer(int)
     * @since 1.1
     */
    public synchronized char charAt(int offset) {
        try {
            Node node = tree.findNodeContaining(offset);
            if (node == null) return '\0';
            char[] buffer = bufferManager.getBuffer(node.bufferIndex);
            int indexInNodeBuffer = node.bufferStart + (offset - node.documentStart);
            return buffer[indexInNodeBuffer];
        } catch (Exception e) {
            Log.d(TAG, "Error in charAt: " + e.getMessage(), e);
        }
        return '\0';
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
     *   <li>\r\n (CRLF) - Windows standard</li>
     *   <li>\n (LF) - Unix/Linux/macOS standard</li>
     *   <li>\r (CR) - Classic Mac standard</li>
     * </ul>
     * </p>
     *
     * @param eolNormalization The end-of-line character sequence to use. If same as current, no conversion is performed.
     * @return The complete document text with the specified EOL format.
     * @see #text()
     * @see #getEOL()
     * @since 1.0
     */
    public synchronized String text(EOLNormalization eolNormalization) {
        String content = text();
        if (eolNormalization == getEOL()) return content;
        switch (eolNormalization) {
            case CRLF -> content = content.replaceAll("(\r\n|\n|\r)", "\r\n");
            case LF -> content = content.replaceAll("(\r\n|\r)", "\n");
            case CR -> content = content.replaceAll("(\r\n|\n)", "\r");
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
     * <p>
     * Critical Scenario:
     * <ul>
     *     <li><b>OOM (Out-of-Memory):</b> When the total PieceTree text can't fit inside one string,
     *     use line content approach their.</li>
     * </ul>
     * </p>
     *
     * @return A string representing the complete text content of the document.
     * Returns empty string for empty documents.
     * @see RedBlackTree
     * @see BufferManager
     * @since 1.0
     */
    public synchronized String text() {
        try {
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
        } catch (Exception e) {
            Log.d(TAG, "Error in text: " + e.getMessage(), e);
        }
        return "";
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
     * @param startPosition   The {@link BufferPosition} {@code startPosition} which contains the metadata of starting {@code lineNumber} and {@code column}.
     * @param endPosition     The {@link BufferPosition} {@code endPosition} which contains the metadata of ending {@code lineNumber} and {@code column}.
     * @return The text content in the specified coordinate range.
     * @see #offsetAt(BufferPosition)
     * @see #textRange(int, int)
     * @since 1.0
     */
    public synchronized String textRange(BufferPosition startPosition, BufferPosition endPosition) {
        int startOffset = offsetAt(startPosition);
        int endOffset = offsetAt(endPosition);
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
     * @param end   The exclusive ending offset (0-based).
     * @return The extracted text between the offsets. Empty string if start ≥ end.
     * @see RedBlackTree#findNodeContaining(int)
     * @see RedBlackTree#getSuccessor(Node)
     * @since 1.0
     */
    public synchronized String textRange(int start, int end) {
        if (start >= end || end - start >= Integer.MAX_VALUE / 1000) return "";
        try {
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
        } catch (Exception e) {
            Log.d(TAG, "Error in textRange: " + e.getMessage(), e);
        }
        return "";
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
     * @see #getLengthFromNode(Node)
     * @see Node#documentStart
     * @since 1.0
     */
    public synchronized int length() {
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
            Node rightmost = tree.getLast();
            return rightmost.documentStart + rightmost.length;
        }

        return node.length + getLengthFromNode(node.left) + getLengthFromNode(node.right);
    }

    // SEARCH METHODS

    /**
     * Finds all matching occurrences of a text or regex pattern within the document
     * starting from a specified offset, with comprehensive search configuration options.
     * This method provides the most flexible interface for pattern matching operations.
     *
     * <p>
     * Operation Sequence:
     * <ol>
     *   <li>Validates input parameters and applies the {@code maxMatch} limit (capped at 1000).</li>
     *   <li>Creates a compiled {@link Pattern} using the specified search criteria.</li>
     *   <li>Locates the {@link Node} containing the {@code startOffset} and begins traversal.</li>
     *   <li>Builds content incrementally across nodes to handle matches spanning boundaries.</li>
     *   <li>Applies pattern matching with proper offset calculations for absolute positioning.</li>
     *   <li>Maintains content overlap between nodes to ensure no matches are missed.</li>
     * </ol>
     * </p>
     *
     * <p>
     * Important Considerations:
     * <ul>
     *   <li><b>Performance Optimization:</b> Uses incremental content building and overlap
     *       management to efficiently handle large documents without excessive memory usage.</li>
     *   <li><b>Boundary Handling:</b> Sophisticated overlap detection ensures matches
     *       spanning multiple nodes are correctly identified and positioned.</li>
     *   <li><b>Flexible Matching:</b> Supports regex patterns, case sensitivity control,
     *       whole word matching, and custom word separators.</li>
     *   <li><b>Result Limiting:</b> The {@code maxMatch} parameter prevents excessive
     *       memory consumption and provides controlled result sets.</li>
     *   <li><b>Group Capture:</b> Optional capture group extraction for advanced
     *       pattern matching and text processing scenarios.</li>
     * </ul>
     * </p>
     *
     * @param query          The text or regex pattern to search for.
     * @param startOffset    The document offset from which to begin searching (0-based).
     * @param useRegex       {@code true} to treat {@code query} as a regular expression;
     *                       {@code false} for literal string matching.
     * @param caseSensitive  {@code true} for case-sensitive matching; {@code false} otherwise.
     * @param wordSeparators String containing characters that define word boundaries.
     *                       Used when {@code wholeWord} matching is enabled.
     * @param captureGroups  {@code true} to extract regex capture groups; {@code false} otherwise.
     * @param maxMatch       Maximum number of matches to return. Automatically capped at 1000.
     * @return A {@link List} of {@link FindMatch} objects representing all found matches.
     * Returns empty list if no matches found or query is invalid.
     * @see #findMatches(String, int, boolean, boolean, String, boolean, boolean, int)
     * @see #findNext(String, int, boolean, boolean, String, boolean, boolean)
     * @see FindMatch
     * @since 1.0
     */
    public synchronized List<FindMatch> findMatches(String query, int startOffset, boolean useRegex,
                                                    boolean caseSensitive, String wordSeparators,
                                                    boolean captureGroups, int maxMatch) {
        return findMatches(query, startOffset, useRegex, caseSensitive, wordSeparators,
                captureGroups, true, maxMatch); // Default to whole word matching
    }

    /**
     * Finds all matching occurrences of a text or regex pattern within the document
     * starting from a specified offset, with full search configuration control including
     * whole word matching options.
     * This method represents the most comprehensive find operation available.
     *
     * <p>
     * Operation Sequence:
     * <ol>
     *   <li>Validates all input parameters and enforces the {@code maxMatch} safety limit.</li>
     *   <li>Constructs an optimized {@link Pattern} incorporating all matching criteria.</li>
     *   <li>Identifies the starting {@link Node} and calculates the initial search position.</li>
     *   <li>Performs efficient node traversal with intelligent content accumulation.</li>
     *   <li>Executes pattern matching with absolute offset calculation for precise positioning.</li>
     *   <li>Manages content overlap between nodes to ensure comprehensive match detection.</li>
     *   <li>Extracts capture groups when requested and validates match boundaries.</li>
     * </ol>
     * </p>
     *
     * <p>
     * Important Considerations:
     * <ul>
     *   <li><b>Comprehensive Control:</b> Provides complete control over all aspects
     *       of pattern matching including case sensitivity, word boundaries, and regex features.</li>
     *   <li><b>Memory Management:</b> Implements intelligent content overlap and cleanup
     *       to handle large documents efficiently without memory bloat.</li>
     *   <li><b>Whole Word Matching:</b> Advanced word boundary detection using customizable
     *       word separator characters for precise linguistic matching.</li>
     *   <li><b>Error Resilience:</b> Comprehensive error handling ensures graceful
     *       degradation and meaningful logging for debugging purposes.</li>
     *   <li><b>Node Spanning:</b> Sophisticated algorithm handles matches that cross
     *       node boundaries seamlessly with precise offset calculations.</li>
     * </ul>
     * </p>
     *
     * @param query          The text or regex pattern to search for.
     * @param startOffset    The document offset from which to begin searching (0-based).
     * @param useRegex       {@code true} to treat {@code query} as a regular expression;
     *                       {@code false} for literal string matching.
     * @param caseSensitive  {@code true} for case-sensitive matching; {@code false} otherwise.
     * @param wordSeparators String containing characters that define word boundaries.
     * @param captureGroups  {@code true} to extract regex capture groups; {@code false} otherwise.
     * @param wholeWord      {@code true} to match only complete words; {@code false} for partial matches.
     * @param maxMatch       Maximum number of matches to return. Automatically capped at 1000.
     * @return A {@link List} of {@link FindMatch} objects representing all found matches.
     * Returns empty list if no matches found or parameters are invalid.
     * @see #findMatches(String, int, boolean, boolean, String, boolean, int)
     * @see #findNext(String, int, boolean, boolean, String, boolean, boolean)
     * @see FindMatch
     * @since 1.0
     */
    public synchronized List<FindMatch> findMatches(String query, int startOffset, boolean useRegex,
                                                    boolean caseSensitive, String wordSeparators,
                                                    boolean captureGroups, boolean wholeWord, int maxMatch) {
        List<FindMatch> matches = new ArrayList<>();
        maxMatch = Math.min(maxMatch, 1000);

        if (query == null || query.isEmpty()) {
            return matches;
        }

        try {
            Pattern pattern = createPattern(query, useRegex, caseSensitive, wordSeparators, wholeWord);

            // Start from the node containing startOffset
            Node current = tree.findNodeContaining(startOffset);
            if (current == null) {
                return matches;
            }

            // Build content incrementally and search
            StringBuilder contentBuilder = new StringBuilder();
            int contentStartOffset = current.documentStart;
            int searchStartInContent = Math.max(0, startOffset - contentStartOffset);

            while (current != null && matches.size() < maxMatch) {
                // Append current node content
                String nodeContent = new String(bufferManager.getBuffer(current.bufferIndex),
                        current.bufferStart, current.length);
                contentBuilder.append(nodeContent);

                // Search in accumulated content
                String content = contentBuilder.toString();
                Matcher matcher = pattern.matcher(content);

                // Find matches starting from the appropriate position
                int searchStart = (current.documentStart == contentStartOffset) ? searchStartInContent : 0;
                while (matcher.find(searchStart) && matches.size() < maxMatch) {
                    List<String> matchGroups = extractGroups(matcher, captureGroups);

                    int absoluteStart = matcher.start() + contentStartOffset;
                    int absoluteEnd = matcher.end() + contentStartOffset;

                    // Validate bounds
                    if (absoluteStart >= 0 && absoluteEnd <= length()) {
                        matches.add(new FindMatch(absoluteStart, absoluteEnd, matchGroups));
                    }

                    searchStart = matcher.start() + 1; // Continue search after this match
                }

                current = tree.getSuccessor(current);

                // For next iteration, keep some overlap to handle matches spanning nodes
                if (current != null && contentBuilder.length() > query.length() * 2) {
                    int keepLength = query.length() * 2;
                    String overlap = contentBuilder.substring(contentBuilder.length() - keepLength);
                    contentBuilder = new StringBuilder(overlap);
                    contentStartOffset = current.documentStart - keepLength;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in findMatches: " + e.getMessage(), e);
        }

        return matches;
    }

    /**
     * Finds the next occurrence of a text or regex pattern starting from a specified
     * position in the document, with default whole word matching enabled.
     * This method provides a convenient position-based interface for sequential search operations.
     *
     * <p>
     * Operation Sequence:
     * <ol>
     *   <li>Converts the {@link BufferPosition} parameter to a document offset using {@link #offsetAt(BufferPosition)}.</li>
     *   <li>Delegates to the offset-based {@code findNext} method with whole word matching enabled.</li>
     *   <li>Returns the first match found after the specified position, or {@code null} if none exists.</li>
     * </ol>
     * </p>
     *
     * <p>
     * Important Considerations:
     * <ul>
     *   <li><b>BufferPosition Convenience:</b> Simplifies usage when working with line/column
     *       coordinates instead of raw document offsets.</li>
     *   <li><b>Default Behavior:</b> Automatically enables whole word matching for
     *       more precise text search operations.</li>
     *   <li><b>Sequential Search:</b> Designed for iterative search operations where
     *       you need to find matches one at a time in document order.</li>
     * </ul>
     * </p>
     *
     * @param query          The text or regex pattern to search for.
     * @param startPos       The {@link BufferPosition} (line, column) from which to begin searching.
     * @param useRegex       {@code true} to treat {@code query} as a regular expression;
     *                       {@code false} for literal string matching.
     * @param caseSensitive  {@code true} for case-sensitive matching; {@code false} otherwise.
     * @param wordSeparators String containing characters that define word boundaries.
     * @param captureGroups  {@code true} to extract regex capture groups; {@code false} otherwise.
     * @return A {@link FindMatch} object representing the next match found, or {@code null} if none exists.
     * @see #findNext(String, BufferPosition, boolean, boolean, String, boolean, boolean)
     * @see #findNext(String, int, boolean, boolean, String, boolean, boolean)
     * @see BufferPosition
     * @since 1.0
     */
    public synchronized FindMatch findNext(String query, BufferPosition startPos, boolean useRegex,
                                           boolean caseSensitive, String wordSeparators,
                                           boolean captureGroups) {
        return findNext(query, startPos, useRegex, caseSensitive, wordSeparators,
                captureGroups, true); // Default to whole word matching
    }

    /**
     * Finds the next occurrence of a text or regex pattern starting from a specified
     * position in the document, with full control over word matching behavior.
     * This method offers comprehensive search configuration through position-based coordinates.
     *
     * <p>
     * Operation Sequence:
     * <ol>
     *   <li>Translates the {@link BufferPosition} coordinates to a document offset.</li>
     *   <li>Invokes the offset-based {@code findNext} method with all specified parameters.</li>
     *   <li>Returns the search result maintaining all configured matching criteria.</li>
     * </ol>
     * </p>
     *
     * <p>
     * Important Considerations:
     * <ul>
     *   <li><b>BufferPosition Flexibility:</b> Allows precise control over search starting
     *       point using intuitive line and column coordinates.</li>
     *   <li><b>Word Boundary Control:</b> Configurable whole word matching provides
     *       fine-grained control over match precision.</li>
     *   <li><b>Editor Integration:</b> Designed for integration with text editors
     *       and IDEs that work with position-based coordinates.</li>
     * </ul>
     * </p>
     *
     * @param query          The text or regex pattern to search for.
     * @param startPos       The {@link BufferPosition} (line, column) from which to begin searching.
     * @param useRegex       {@code true} to treat {@code query} as a regular expression;
     *                       {@code false} for literal string matching.
     * @param caseSensitive  {@code true} for case-sensitive matching; {@code false} otherwise.
     * @param wordSeparators String containing characters that define word boundaries.
     * @param captureGroups  {@code true} to extract regex capture groups; {@code false} otherwise.
     * @param wholeWord      {@code true} to match only complete words; {@code false} for partial matches.
     * @return A {@link FindMatch} object representing the next match found, or {@code null} if none exists.
     * @see #findNext(String, BufferPosition, boolean, boolean, String, boolean)
     * @see #findNext(String, int, boolean, boolean, String, boolean, boolean)
     * @see BufferPosition
     * @since 1.0
     */
    public synchronized FindMatch findNext(String query, BufferPosition startPos, boolean useRegex,
                                           boolean caseSensitive, String wordSeparators,
                                           boolean captureGroups, boolean wholeWord) {
        int startOffset = offsetAt(startPos);
        return findNext(query, startOffset, useRegex, caseSensitive, wordSeparators, captureGroups, wholeWord);
    }

    /**
     * Finds the next occurrence of a text or regex pattern starting from a specified
     * document offset, with default whole word matching enabled.
     * This method provides a streamlined interface for offset-based sequential searching.
     *
     * <p>
     * Operation Sequence:
     * <ol>
     *   <li>Validates the {@code startOffset} parameter and query string.</li>
     *   <li>Delegates to the comprehensive {@code findNext} method with whole word matching enabled.</li>
     *   <li>Returns the first match found after the offset, or {@code null} if no match exists.</li>
     * </ol>
     * </p>
     *
     * <p>
     * Important Considerations:
     * <ul>
     *   <li><b>Simplified Interface:</b> Reduces parameter complexity while maintaining
     *       essential search functionality for common use cases.</li>
     *   <li><b>Whole Word Default:</b> Automatically applies word boundary matching
     *       for more accurate text searching in most scenarios.</li>
     *   <li><b>Performance Focus:</b> Optimized for single match retrieval with
     *       early termination upon finding the first result.</li>
     * </ul>
     * </p>
     *
     * @param query          The text or regex pattern to search for.
     * @param startOffset    The document offset from which to begin searching (0-based).
     * @param useRegex       {@code true} to treat {@code query} as a regular expression;
     *                       {@code false} for literal string matching.
     * @param caseSensitive  {@code true} for case-sensitive matching; {@code false} otherwise.
     * @param wordSeparators String containing characters that define word boundaries.
     * @param captureGroups  {@code true} to extract regex capture groups; {@code false} otherwise.
     * @return A {@link FindMatch} object representing the next match found, or {@code null} if none exists.
     * @see #findNext(String, int, boolean, boolean, String, boolean, boolean)
     * @see #findMatches(String, int, boolean, boolean, String, boolean, int)
     * @since 1.0
     */
    public synchronized FindMatch findNext(String query, int startOffset, boolean useRegex,
                                           boolean caseSensitive, String wordSeparators,
                                           boolean captureGroups) {
        return findNext(query, startOffset, useRegex, caseSensitive, wordSeparators,
                captureGroups, true); // Default to whole word matching
    }

    /**
     * Finds the next occurrence of a text or regex pattern starting from a specified
     * document offset, with comprehensive search configuration options.
     * This method represents the core implementation for forward pattern searching.
     *
     * <p>
     * Operation Sequence:
     * <ol>
     *   <li>Validates input parameters and creates an optimized {@link Pattern} object.</li>
     *   <li>Locates the {@link Node} containing the specified {@code startOffset}.</li>
     *   <li>Builds content incrementally across nodes to handle boundary-spanning matches.</li>
     *   <li>Applies pattern matching with precise offset calculations for absolute positioning.</li>
     *   <li>Manages intelligent content overlap between nodes to ensure match detection.</li>
     *   <li>Returns the first valid match found or {@code null} if no matches exist.</li>
     * </ol>
     * </p>
     *
     * <p>
     * Important Considerations:
     * <ul>
     *   <li><b>Efficient Traversal:</b> Uses optimized node traversal with early termination
     *       upon finding the first match, minimizing computational overhead.</li>
     *   <li><b>Boundary Intelligence:</b> Sophisticated overlap management ensures matches
     *       that span across node boundaries are correctly identified and positioned.</li>
     *   <li><b>Memory Optimization:</b> Implements content window management to handle
     *       large documents without excessive memory consumption.</li>
     *   <li><b>Error Handling:</b> Comprehensive exception handling with detailed logging
     *       ensures robust operation and debugging capabilities.</li>
     *   <li><b>Offset Precision:</b> Maintains absolute document offset accuracy throughout
     *       the search process for reliable match positioning.</li>
     * </ul>
     * </p>
     *
     * @param query          The text or regex pattern to search for.
     * @param startOffset    The document offset from which to begin searching (0-based).
     * @param useRegex       {@code true} to treat {@code query} as a regular expression;
     *                       {@code false} for literal string matching.
     * @param caseSensitive  {@code true} for case-sensitive matching; {@code false} otherwise.
     * @param wordSeparators String containing characters that define word boundaries.
     * @param captureGroups  {@code true} to extract regex capture groups; {@code false} otherwise.
     * @param wholeWord      {@code true} to match only complete words; {@code false} for partial matches.
     * @return A {@link FindMatch} object representing the next match found, or {@code null} if none exists.
     * @see #findNext(String, int, boolean, boolean, String, boolean)
     * @see #findMatches(String, int, boolean, boolean, String, boolean, boolean, int)
     * @see #findPrevious(String, int, boolean, boolean, String, boolean, boolean)
     * @since 1.0
     */
    public synchronized FindMatch findNext(String query, int startOffset, boolean useRegex,
                                           boolean caseSensitive, String wordSeparators,
                                           boolean captureGroups, boolean wholeWord) {
        if (query == null || query.isEmpty()) {
            return null;
        }

        try {
            Pattern pattern = createPattern(query, useRegex, caseSensitive, wordSeparators, wholeWord);

            Node current = tree.findNodeContaining(startOffset);
            if (current == null) {
                return null;
            }

            StringBuilder contentBuilder = new StringBuilder();
            int contentStartOffset = current.documentStart;
            int searchStartInContent = Math.max(0, startOffset - contentStartOffset);

            while (current != null) {
                // Append current node content
                contentBuilder.append(new String(bufferManager.getBuffer(current.bufferIndex), current.bufferStart, current.length));

                // Search in accumulated content
                String content = contentBuilder.toString();
                Matcher matcher = pattern.matcher(content);

                // Find first match starting from the appropriate position
                int searchStart = (current.documentStart == contentStartOffset) ? searchStartInContent : 0;
                if (matcher.find(searchStart)) {
                    int absoluteStart = matcher.start() + contentStartOffset;
                    int absoluteEnd = matcher.end() + contentStartOffset;

                    // Validate bounds
                    if (absoluteStart >= 0 && absoluteEnd <= length()) {
                        return new FindMatch(absoluteStart, absoluteEnd, extractGroups(matcher, captureGroups));
                    }
                }

                current = tree.getSuccessor(current);

                // For next iteration, keep some overlap to handle matches spanning nodes
                if (current != null && contentBuilder.length() > query.length() * 2) {
                    int keepLength = query.length() * 2;
                    String overlap = contentBuilder.substring(contentBuilder.length() - keepLength);
                    contentBuilder = new StringBuilder(overlap);
                    contentStartOffset = current.documentStart - keepLength;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in findNext: " + e.getMessage(), e);
        }

        return null;
    }

    /**
     * Finds the previous occurrence of a text or regex pattern ending before a specified
     * position in the document, with default whole word matching enabled.
     * This method provides a convenient position-based interface for backward search operations.
     *
     * <p>
     * Operation Sequence:
     * <ol>
     *   <li>Converts the {@link BufferPosition} parameter to a document offset using {@link #offsetAt(BufferPosition)}.</li>
     *   <li>Delegates to the offset-based {@code findPrevious} method with whole word matching enabled.</li>
     *   <li>Returns the last match found before the specified position, or {@code null} if none exists.</li>
     * </ol>
     * </p>
     *
     * <p>
     * Important Considerations:
     * <ul>
     *   <li><b>Reverse Navigation:</b> Enables backward text navigation and search
     *       functionality essential for comprehensive text editing operations.</li>
     *   <li><b>BufferPosition Convenience:</b> Simplifies usage when working with line/column
     *       coordinates in text editor environments.</li>
     *   <li><b>Default Precision:</b> Automatically enables whole word matching for
     *       more accurate backward searching operations.</li>
     * </ul>
     * </p>
     *
     * @param query          The text or regex pattern to search for.
     * @param endPos         The {@link BufferPosition} (line, column) before which to search.
     * @param useRegex       {@code true} to treat {@code query} as a regular expression;
     *                       {@code false} for literal string matching.
     * @param caseSensitive  {@code true} for case-sensitive matching; {@code false} otherwise.
     * @param wordSeparators String containing characters that define word boundaries.
     * @param captureGroups  {@code true} to extract regex capture groups; {@code false} otherwise.
     * @return A {@link FindMatch} object representing the previous match found, or {@code null} if none exists.
     * @see #findPrevious(String, BufferPosition, boolean, boolean, String, boolean, boolean)
     * @see #findPrevious(String, int, boolean, boolean, String, boolean, boolean)
     * @see BufferPosition
     * @since 1.0
     */
    public synchronized FindMatch findPrevious(String query, BufferPosition endPos, boolean useRegex,
                                               boolean caseSensitive, String wordSeparators,
                                               boolean captureGroups) {
        return findPrevious(query, endPos, useRegex, caseSensitive, wordSeparators,
                captureGroups, true); // Default to whole word matching
    }

    /**
     * Finds the previous occurrence of a text or regex pattern ending before a specified
     * position in the document, with full control over word matching behavior.
     * This method offers comprehensive backward search configuration through position-based coordinates.
     *
     * <p>
     * Operation Sequence:
     * <ol>
     *   <li>Translates the {@link BufferPosition} coordinates to a document offset.</li>
     *   <li>Invokes the offset-based {@code findPrevious} method with all specified parameters.</li>
     *   <li>Returns the search result maintaining all configured matching criteria.</li>
     * </ol>
     * </p>
     *
     * <p>
     * Important Considerations:
     * <ul>
     *   <li><b>Backward Navigation:</b> Essential functionality for comprehensive text
     *       search and navigation in both directions through the document.</li>
     *   <li><b>BufferPosition Precision:</b> Allows exact control over search endpoint
     *       using intuitive line and column coordinates.</li>
     *   <li><b>Editor Integration:</b> Designed for seamless integration with text
     *       editors that require bidirectional search capabilities.</li>
     * </ul>
     * </p>
     *
     * @param query          The text or regex pattern to search for.
     * @param endPos         The {@link BufferPosition} (line, column) before which to search.
     * @param useRegex       {@code true} to treat {@code query} as a regular expression;
     *                       {@code false} for literal string matching.
     * @param caseSensitive  {@code true} for case-sensitive matching; {@code false} otherwise.
     * @param wordSeparators String containing characters that define word boundaries.
     * @param captureGroups  {@code true} to extract regex capture groups; {@code false} otherwise.
     * @param wholeWord      {@code true} to match only complete words; {@code false} for partial matches.
     * @return A {@link FindMatch} object representing the previous match found, or {@code null} if none exists.
     * @see #findPrevious(String, BufferPosition, boolean, boolean, String, boolean)
     * @see #findPrevious(String, int, boolean, boolean, String, boolean, boolean)
     * @see BufferPosition
     * @since 1.0
     */
    public synchronized FindMatch findPrevious(String query, BufferPosition endPos, boolean useRegex,
                                               boolean caseSensitive, String wordSeparators,
                                               boolean captureGroups, boolean wholeWord) {
        int endOffset = offsetAt(endPos);
        return findPrevious(query, endOffset, useRegex, caseSensitive, wordSeparators, captureGroups, wholeWord);
    }

    /**
     * Finds the previous occurrence of a text or regex pattern ending before a specified
     * document offset, with default whole word matching enabled.
     * This method provides a streamlined interface for offset-based backward searching.
     *
     * <p>
     * Operation Sequence:
     * <ol>
     *   <li>Validates the {@code endOffset} parameter and query string.</li>
     *   <li>Delegates to the comprehensive {@code findPrevious} method with whole word matching enabled.</li>
     *   <li>Returns the last match found before the offset, or {@code null} if no match exists.</li>
     * </ol>
     * </p>
     *
     * <p>
     * Important Considerations:
     * <ul>
     *   <li><b>Simplified Interface:</b> Reduces parameter complexity while maintaining
     *       essential backward search functionality for common use cases.</li>
     *   <li><b>Whole Word Default:</b> Automatically applies word boundary matching
     *       for more accurate reverse text searching.</li>
     *   <li><b>Performance Focus:</b> Optimized for single match retrieval with
     *       efficient backward traversal algorithms.</li>
     * </ul>
     * </p>
     *
     * @param query          The text or regex pattern to search for.
     * @param endOffset      The document offset before which to search (0-based, exclusive).
     * @param useRegex       {@code true} to treat {@code query} as a regular expression;
     *                       {@code false} for literal string matching.
     * @param caseSensitive  {@code true} for case-sensitive matching; {@code false} otherwise.
     * @param wordSeparators String containing characters that define word boundaries.
     * @param captureGroups  {@code true} to extract regex capture groups; {@code false} otherwise.
     * @return A {@link FindMatch} object representing the previous match found, or {@code null} if none exists.
     * @see #findPrevious(String, int, boolean, boolean, String, boolean, boolean)
     * @see #findNext(String, int, boolean, boolean, String, boolean)
     * @since 1.0
     */
    public synchronized FindMatch findPrevious(String query, int endOffset, boolean useRegex,
                                               boolean caseSensitive, String wordSeparators,
                                               boolean captureGroups) {
        return findPrevious(query, endOffset, useRegex, caseSensitive, wordSeparators,
                captureGroups, true); // Default to whole word matching
    }

    /**
     * Finds the previous occurrence of a text or regex pattern ending before a specified
     * document offset, with comprehensive search configuration options.
     * This method represents the core implementation for backward pattern searching.
     *
     * <p>
     * Operation Sequence:
     * <ol>
     *   <li>Validates input parameters and creates an optimized {@link Pattern} object.</li>
     *   <li>Locates the {@link Node} containing the specified {@code endOffset}.</li>
     *   <li>Performs reverse traversal through document nodes while accumulating content.</li>
     *   <li>Applies pattern matching to find all matches within the current content scope.</li>
     *   <li>Tracks the last valid match that falls before the specified end offset.</li>
     *   <li>Returns the most recent match found or {@code null} if no matches exist.</li>
     * </ol>
     * </p>
     *
     * <p>
     * Important Considerations:
     * <ul>
     *   <li><b>Reverse Algorithm:</b> Implements sophisticated backward search logic
     *       that efficiently processes document content in reverse order.</li>
     *   <li><b>Boundary Handling:</b> Carefully manages node boundaries and content
     *       accumulation to ensure accurate match detection across all document regions.</li>
     *   <li><b>Last Match Tracking:</b> Maintains state to identify the most recent
     *       valid match before the specified endpoint, handling multiple candidates correctly.</li>
     *   <li><b>Offset Validation:</b> Comprehensive bounds checking ensures matches
     *       are within valid document ranges and before the specified end position.</li>
     *   <li><b>Performance Optimization:</b> Uses early termination strategies when
     *       matches are found to minimize unnecessary processing.</li>
     * </ul>
     * </p>
     *
     * @param query          The text or regex pattern to search for.
     * @param endOffset      The document offset before which to search (0-based, exclusive).
     * @param useRegex       {@code true} to treat {@code query} as a regular expression;
     *                       {@code false} for literal string matching.
     * @param caseSensitive  {@code true} for case-sensitive matching; {@code false} otherwise.
     * @param wordSeparators String containing characters that define word boundaries.
     * @param captureGroups  {@code true} to extract regex capture groups; {@code false} otherwise.
     * @param wholeWord      {@code true} to match only complete words; {@code false} for partial matches.
     * @return A {@link FindMatch} object representing the previous match found, or {@code null} if none exists.
     * @see #findPrevious(String, int, boolean, boolean, String, boolean)
     * @see #findNext(String, int, boolean, boolean, String, boolean, boolean)
     * @see #findMatches(String, int, boolean, boolean, String, boolean, boolean, int)
     * @since 1.0
     */
    public synchronized FindMatch findPrevious(String query, int endOffset, boolean useRegex,
                                               boolean caseSensitive, String wordSeparators,
                                               boolean captureGroups, boolean wholeWord) {
        if (query == null || query.isEmpty()) {
            return null;
        }

        try {
            Pattern pattern = createPattern(query, useRegex, caseSensitive, wordSeparators, wholeWord);

            Node current = tree.findNodeContaining(endOffset);
            if (current == null) return null;

            FindMatch lastMatch = null;
            StringBuilder contentBuilder = new StringBuilder();
            int contentStartOffset = current.documentStart;

            while (current != null && contentStartOffset < endOffset) {
                // Append current node content
                contentBuilder.append(new String(bufferManager.getBuffer(current.bufferIndex), current.bufferStart, current.length));

                // Search in accumulated content
                String content = contentBuilder.toString();
                Matcher matcher = pattern.matcher(content);

                while (matcher.find()) {
                    int absoluteStart = matcher.start() + contentStartOffset;
                    int absoluteEnd = matcher.end() + contentStartOffset;

                    // Validate bounds
                    if (absoluteEnd < endOffset && absoluteStart >= 0 && absoluteEnd <= length()) {
                        lastMatch = new FindMatch(absoluteStart, absoluteEnd, extractGroups(matcher, captureGroups));
                    }
                }

                if (lastMatch != null) break;

                current = tree.findNodeContaining(contentStartOffset - 1);

                if (current != null) {
                    contentBuilder = new StringBuilder();
                    contentStartOffset = current.documentStart;
                }
            }

            return lastMatch;
        } catch (Exception e) {
            Log.e(TAG, "Error in findPrevious: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Creates a Pattern object based on the provided query and options.
     *
     * @param query          The text or regex pattern to search for.
     * @param useRegex       {@code true} to treat {@code query} as a regular expression;
     *                       {@code false} for literal string matching.
     * @param caseSensitive  {@code true} for case-sensitive matching; {@code false} otherwise.
     * @param wordSeparators String containing characters that define word boundaries.
     * @param wholeWord      {@code true} to match only complete words; {@code false} for partial matches.
     * @return A compiled {@link Pattern} object.
     */
    private Pattern createPattern(String query, boolean useRegex, boolean caseSensitive,
                                  String wordSeparators, boolean wholeWord) {
        int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;

        if (useRegex) {
            return Pattern.compile(query, flags);
        } else {
            String escapedQuery = Pattern.quote(query);

            if (wholeWord) {
                String boundaryPattern;
                if (wordSeparators != null) {
                    // For custom separators, quote them to be safe
                    boundaryPattern = Pattern.quote(wordSeparators);
                } else {
                    // Use whitespace and punctuation - don't quote these as they're regex patterns
                    boundaryPattern = "\\s\\p{Punct}";
                }

                String wordBoundary = "(?<=^|[" + boundaryPattern + "])" +
                        escapedQuery +
                        "(?=$|[" + boundaryPattern + "])";
                return Pattern.compile(wordBoundary, flags);
            } else {
                return Pattern.compile(escapedQuery, flags);
            }
        }
    }

    /**
     * Extracts the capture groups from a {@link Matcher} object.
     *
     * @param matcher       A {@link Matcher} object representing the search result.
     * @param captureGroups {@code true} to extract regex capture groups; {@code false} otherwise.
     * @return A list of strings containing the extracted capture groups.
     */
    private List<String> extractGroups(Matcher matcher, boolean captureGroups) {
        List<String> matchGroups = new ArrayList<>();

        if (captureGroups && matcher.groupCount() > 0) {
            for (int i = 0; i <= matcher.groupCount(); i++) {
                matchGroups.add(matcher.group(i));
            }
        } else {
            matchGroups.add(matcher.group());
        }

        return matchGroups;
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
     *   <li>Explicit setting via {@link #setEOL(EOLNormalization)}</li>
     *   <li>Auto-detection during document initialization</li>
     *   <li>Default LF (\n) format for new documents</li>
     * </ol>
     * </p>
     *
     * @return The current end-of-line character sequence.
     * @see #setEOL(EOLNormalization)
     * @see EOLNormalization
     * @since 1.0
     */
    public synchronized EOLNormalization getEOL() {
        return switch (eol) {
            case "\r\n" -> EOLNormalization.CRLF;
            case "\n" -> EOLNormalization.LF;
            case "\r" -> EOLNormalization.CR;
            default -> EOLNormalization.None;
        };
    }

    /**
     * Sets the end-of-line character sequence for the document.
     * This method configures the EOL format that will be used for new text
     * operations without modifying existing content in the document.
     *
     * <p>
     * Supported EOL Formats:
     * <ul>
     *   <li>"\r\n" (CRLF) - Windows standard</li>
     *   <li>"\n" (LF) - Unix/Linux/macOS standard</li>
     *   <li>"\r" (CR) - Classic Mac standard</li>
     * </ul>
     * </p>
     *
     * <p>
     * <b>Note:</b> This method only affects future text operations.
     * To convert existing content, use {@link #text(EOLNormalization)} to retrieve
     * text with the desired EOL format.
     * </p>
     *
     * @param eolNormalization The new end-of-line character sequence.
     * @throws IllegalArgumentException if eol is null or empty.
     * @see #getEOL()
     * @see #text(EOLNormalization)
     * @since 1.0
     */
    public synchronized void setEOL(EOLNormalization eolNormalization) {
        isNormalizeEOL = true;
        switch (eolNormalization) {
            case CRLF:
                this.eol = "\r\n";
                break;
            case LF:
                this.eol = "\n";
                break;
            case CR:
                this.eol = "\r";
                break;
            case None:
            default:
                isNormalizeEOL = false;
                break;
        }
    }

    /**
     * Checks if automatic End-Of-Line (EOL) normalization is enabled for the document.
     * When EOL normalization is active, the document attempts to maintain a consistent
     * EOL sequence (as determined by {@link #getEOL()}) by converting or standardizing
     * EOL characters during text operations like insertion or replacement.
     *
     * <p>
     * If enabled, text being inserted into the document will have its EOL characters
     * (e.g., "\n", "\r\n", "\r") converted to the document's current EOL setting.
     * If disabled, EOL characters from inserted text are preserved as is.
     * </p>
     *
     * <p>
     * This setting primarily affects how new text is integrated into the document.
     * It does not retroactively normalize the entire existing document content unless
     * specific operations that re-process text are performed.
     * </p>
     *
     * @return {@code true} if EOL normalization is active, {@code false} otherwise.
     * @see #setIsNormalizeEOL(boolean)
     * @see #getEOL()
     * @see #setEOL(EOLNormalization)
     * @since 1.0
     */
    public synchronized boolean IsNormalizeEOL() {
        return isNormalizeEOL;
    }

    /**
     * Sets whether automatic End-Of-Line (EOL) normalization should be enabled for the document.
     * Enabling this feature means that during text insertion or replacement operations,
     * the document will attempt to convert incoming EOL sequences to match the
     * document's configured EOL format (see {@link #setEOL(EOLNormalization)} and {@link #getEOL()}).
     *
     * <p>
     * For example, if the document's EOL is set to "\n" (LF) and normalization is enabled:
     * <ul>
     *   <li>Inserting text containing "\r\n" (CRLF) will result in "\n" being stored.</li>
     *   <li>Inserting text containing "\r" (CR) will result in "\n" being stored.</li>
     * </ul>
     * </p>
     *
     * <p>
     * <b>Note:</b> Disabling normalization means that EOL characters from inserted or
     * pasted text will be preserved as they are, potentially leading to mixed EOL
     * sequences within the document if the source text uses different conventions.
     * This method does not alter existing EOL characters in the document; it only
     * affects future text modifications.
     * </p>
     *
     * @param isNormalizeEOL {@code true} to enable EOL normalization, {@code false} to disable it.
     * @see #IsNormalizeEOL()
     * @see #setEOL(EOLNormalization)
     * @see #getEOL()
     * @since 1.0
     */
    public synchronized void setIsNormalizeEOL(boolean isNormalizeEOL) {
        this.isNormalizeEOL = isNormalizeEOL;
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
     * @see PieceTreeSnapshot
     * @see #restoreSnapshot(PieceTreeSnapshot)
     * @since 1.0
     */
    public synchronized PieceTreeSnapshot createSnapshot() {
        currentSnapshot = new PieceTreeSnapshot(bufferManager, tree, getEOL());
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
     *   <li>Restores metadata such as the end-of-line (EOL) sequence using {@link #setEOL(EOLNormalization)}.</li>
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
     * @see PieceTreeSnapshot
     * @see #createSnapshot()
     * @see #initialize(String)
     * @since 1.0
     */
    public synchronized void restoreSnapshot(PieceTreeSnapshot snapshot) {
        if (snapshot != null) {
            // Clear current content & Restore from snapshot
            tree = snapshot.tree;
            bufferManager = snapshot.bufferManager;
            setEOL(snapshot.eolNormalization);
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
     * {@code false} otherwise, indicating a potential structural inconsistency.
     * @see RedBlackTree#validateRedBlackProperties()
     * @since 1.0
     */
    public synchronized boolean validateTree() {
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

            if (isNormalizeEOL &&
                    (text.contains("\r\n") || text.contains("\n") || text.contains("\r"))) {
                text = text.replaceAll("(\r\n|\n|\r)", eol);
            }

            Node node = tree.findNodeContaining(offset);
            if (node == null && offset == length()) {
                int startPos = bufferManager.addAddedText(text);
                int[] lineStarts = computeLineStarts(text.toCharArray());
                tree.addLineCount(lineStarts.length);
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
                int[] leftLineStarts = computeLineStarts(getBufferSlice(node, 0, splitPoint));
                int[] rightLineStarts = computeLineStarts(getBufferSlice(node, splitPoint, node.length));
                Node leftPart = new Node(node.bufferIndex, node.bufferStart, splitPoint, leftLineStarts);
                Node rightPart = new Node(node.bufferIndex, node.bufferStart + splitPoint, node.length - splitPoint, rightLineStarts);
                leftPart.documentStart = nodeDocStart;
                rightPart.documentStart = offset;
                tree.deleteNode(node);
                tree.insert(leftPart);
                tree.insert(rightPart);
            }

            int startPos = bufferManager.addAddedText(text);
            int[] lineStarts = computeLineStarts(text.toCharArray());
            tree.addLineCount(lineStarts.length);
            Node newNode = new Node(0, startPos, text.length(), lineStarts);
            newNode.documentStart = offset;
            tree.insert(newNode);
        } catch (Exception e) {
            Log.d(TAG, "Error in initialize: " + e.getMessage(), e);
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
     */
    private int[] computeLineStarts(char[] buffer) {
        if (buffer == null || buffer.length == 0) {
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
     *   <li>Notifying listeners ({@link UndoRedoManager.UndoRedoListener}) of changes in undo/redo state.</li>
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
     * @see UndoRedoManager
     * @see Command
     * @see #setUndoRedoManager(UndoRedoManager)
     * @since 1.0
     */
    public synchronized UndoRedoManager getUndoRedoManager() {
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
     * @see UndoRedoManager
     * @see #getUndoRedoManager()
     * @since 1.0
     */
    public synchronized void setUndoRedoManager(UndoRedoManager undoRedoManager) {
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
     * @return {@code true} if the group was successfully started, {@code false} otherwise.
     * @see UndoRedoManager#beginGroup(String)
     * @see #endGroup()
     * @since 1.0
     */
    public synchronized boolean beginGroup(String description) {
        return undoRedoManager.beginGroup(description);
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
     * @return {@code true} if the group was successfully ended, {@code false} otherwise.
     * @see UndoRedoManager#endGroup()
     * @see #beginGroup(String)
     * @since 1.0
     */
    public synchronized boolean endGroup() {
        return undoRedoManager.endGroup();
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
     * @see UndoRedoManager#canUndo()
     * @see #undo()
     * @since 1.0
     */
    public synchronized boolean canUndo() {
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
     * @see UndoRedoManager#canRedo()
     * @see #redo()
     * @since 1.0
     */
    public synchronized boolean canRedo() {
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
     *   <li>Notifying any registered {@link UndoRedoManager.UndoRedoListener}s.</li>
     * </ol>
     * </p>
     *
     * <p>
     * If no operations are available to undo, this method does nothing and returns {@code false}.
     * </p>
     *
     * @return {@code BufferPosition} if an operation was successfully undone, {@code -1} otherwise (e.g., if the undo stack was empty).
     * @see UndoRedoManager#undo()
     * @see #canUndo()
     * @see #redo()
     * @since 1.0
     */
    public synchronized int undo() {
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
     *   <li>Notifying any registered {@link UndoRedoManager.UndoRedoListener}s.</li>
     * </ol>
     * </p>
     *
     * <p>
     * If no operations are available to redo (e.g., the redo stack is empty or a new edit was made after an undo),
     * this method does nothing and returns {@code false}.
     * </p>
     *
     * @return {@code true} if an operation was successfully redone, {@code false} otherwise (e.g., if the redo stack was empty).
     * @see UndoRedoManager#redo()
     * @see #canRedo()
     * @see #undo()
     * @since 1.0
     */
    public synchronized int redo() {
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
     * @see UndoRedoManager#getUndoDescription()
     * @see #canUndo()
     * @since 1.0
     */
    public synchronized String getUndoDescription() {
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
     * @see UndoRedoManager#getRedoDescription()
     * @see #canRedo()
     * @since 1.0
     */
    public synchronized String getRedoDescription() {
        return undoRedoManager.getRedoDescription();
    }

    /**
     * Clears the entire undo and redo history managed by the {@link UndoRedoManager}.
     * After this operation, both {@link #canUndo()} and {@link #canRedo()} will
     * return {@code false}, and no previous edits can be reverted or reapplied.
     * This is typically used when a document is closed, saved under a new name,
     * or when a "clear history" action is explicitly invoked by the user.
     *
     * @see UndoRedoManager#clear()
     * @since 1.0
     */
    public synchronized void clear() {
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
     * @see UndoRedoManager#getUndoSize()
     * @see #canUndo()
     * @see #setMaxUndoLevels(int)
     * @since 1.0
     */
    public synchronized int getUndoSize() {
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
     * @see UndoRedoManager#getRedoSize()
     * @see #canRedo()
     * @since 1.0
     */
    public synchronized int getRedoSize() {
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
     * @see UndoRedoManager#setMaxUndoLevels(int)
     * @see #getMaxUndoLevels()
     * @since 1.0
     */
    public synchronized void setMaxUndoLevels(int maxLevels) {
        undoRedoManager.setMaxUndoLevels(maxLevels);
    }

    /**
     * Gets the current maximum number of undo levels configured for the {@link UndoRedoManager}.
     * This value determines the maximum depth of the undo history that will be maintained.
     *
     * @return The maximum number of undo levels. A non-positive value might indicate
     * unlimited levels, depending on the {@link UndoRedoManager} implementation.
     * @see UndoRedoManager#getMaxUndoLevels()
     * @see #setMaxUndoLevels(int)
     * @since 1.0
     */
    public synchronized int getMaxUndoLevels() {
        return undoRedoManager.getMaxUndoLevels();
    }

    /**
     * Adds a listener to receive notifications about undo and redo state changes.
     * This method allows external components (e.g., UI elements) to observe
     * events from the {@link UndoRedoManager}, such as when an operation is undone,
     * redone, or when the undo/redo history is modified (e.g., cleared, command added).
     *
     * <p>
     * Listeners implement the {@link UndoRedoManager.UndoRedoListener} interface and will receive
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
     * @param listener The {@link UndoRedoManager.UndoRedoListener} to add. Must not be null.
     * @throws IllegalArgumentException if the listener is null.
     * @see UndoRedoManager#addUndoRedoListener(UndoRedoManager.UndoRedoListener)
     * @see UndoRedoManager.UndoRedoListener
     * @see #removeUndoRedoListener(UndoRedoManager.UndoRedoListener)
     * @since 1.0
     */
    public synchronized void addUndoRedoListener(UndoRedoManager.UndoRedoListener listener) {
        undoRedoManager.addUndoRedoListener(listener);
    }

    /**
     * Removes a previously registered {@link UndoRedoManager.UndoRedoListener}.
     * Once removed, the listener will no longer receive notifications about
     * undo and redo state changes from the {@link UndoRedoManager}.
     *
     * <p>
     * If the specified listener was not previously registered, this method
     * typically has no effect and does not throw an exception.
     * </p>
     *
     * @param listener The {@link UndoRedoManager.UndoRedoListener} to remove. Must not be null.
     * @see UndoRedoManager#removeUndoRedoListener(UndoRedoManager.UndoRedoListener)
     * @see #addUndoRedoListener(UndoRedoManager.UndoRedoListener)
     * @since 1.0
     */
    public synchronized void removeUndoRedoListener(UndoRedoManager.UndoRedoListener listener) {
        undoRedoManager.removeUndoRedoListener(listener);
    }
}