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

import java.util.ArrayList;
import java.util.List;

/**
 * Manages text buffers for the PieceTree library, handling the original document
 * content in multiple 64KB buffers and new text in a single growable add buffer.
 * Optimized for Android's memory constraints, ensuring efficient storage and access.
 *
 * @since 1.0
 * @author <a href="https://github.com/itsakc-me">@itsakc.me</a>
 */
public class BufferManager {
    private final List<char[]> originalBuffers; // List of original text buffers (64KB chunks)
    private char[] addBuffer; // Single growable buffer for added text
    private int addBufferLength; // Current used length of add buffer
    private static final int INITIAL_ADD_CAPACITY = 1024; // Initial add buffer size
    public static final int ORIGINAL_BUFFER_SIZE = 64 * 1024; // 64KB per original buffer

    /**
     * Constructs a BufferManager with empty buffers.
     */
    public BufferManager() {
        originalBuffers = new ArrayList<>();
        addBuffer = new char[INITIAL_ADD_CAPACITY];
        addBufferLength = 0;
    }

    /**
     * Adds the initial text, splitting it into 64KB buffers.
     *
     * @param initialText The initial document content.
     * @return The number of buffers created.
     */
    public int addOriginalBuffer(String initialText) {
        if (initialText == null || initialText.isEmpty()) {
            return 0;
        }

        char[] textArray = initialText.toCharArray();
        int textLength = textArray.length;
        int buffersNeeded = (textLength + ORIGINAL_BUFFER_SIZE - 1) / ORIGINAL_BUFFER_SIZE;

        for (int i = 0; i < buffersNeeded; i++) {
            int start = i * ORIGINAL_BUFFER_SIZE;
            int length = Math.min(ORIGINAL_BUFFER_SIZE, textLength - start);
            char[] buffer = new char[length];
            System.arraycopy(textArray, start, buffer, 0, length);
            originalBuffers.add(buffer);
        }

        return buffersNeeded;
    }

    /**
     * Appends new text to the add buffer and returns the starting position.
     *
     * @param text The text to add.
     * @return The starting position in the add buffer.
     */
    public int addAddedText(String text) {
        if (text == null || text.isEmpty()) {
            return addBufferLength;
        }

        char[] textArray = text.toCharArray();
        int textLen = textArray.length;

        // Ensure add buffer has enough capacity
        while (addBufferLength + textLen > addBuffer.length) {
            int newCapacity = Math.max(addBuffer.length * 2, addBufferLength + textLen);
            char[] newAddBuffer = new char[newCapacity];
            System.arraycopy(addBuffer, 0, newAddBuffer, 0, addBufferLength);
            addBuffer = newAddBuffer;
        }

        // Append text to add buffer
        System.arraycopy(textArray, 0, addBuffer, addBufferLength, textLen);
        int start = addBufferLength;
        addBufferLength += textLen;
        return start;
    }

    /**
     * Gets the buffer at the specified index.
     *
     * @param bufferIndex The index of the buffer (0 for add buffer, 1+ for original buffers).
     * @return The buffer, or null if index is invalid.
     */
    public char[] getBuffer(int bufferIndex) {
        if (bufferIndex == 0) {
            return addBuffer;
        } else if (bufferIndex > 0 && bufferIndex <= originalBuffers.size()) {
            return originalBuffers.get(bufferIndex - 1);
        }
        return null;
    }

    /**
     * Gets the current used length of the add buffer.
     *
     * @return The current length of the add buffer.
     */
    public int getAddBufferLength() {
        return addBufferLength;
    }

    /**
     * Gets the number of original buffers.
     *
     * @return The number of original buffers.
     */
    public int getOriginalBufferCount() {
        return originalBuffers.size();
    }
}