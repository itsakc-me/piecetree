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

package io.itsakc.piecetree.common;

/**
 * A snapshot of the piece tree at a point in time.
 * Used for history, copy-paste, or undo/redo mechanisms.
 *
 * @since 1.0
 * @author <a href="https://github.com/itsakc-me">@itsakc.me</a>
 */
public class PieceTreeSnapshot {
    /** Full textual content at the snapshot point. */
    public String content;

    /** Total number of lines in the snapshot. */
    public int lineCount;

    /** End-of-line format used in the snapshot (e.g., "\n", "\r\n"). */
    public String eol;

    /**
     * Constructs a snapshot of the current state.
     *
     * @param content   The full content string.
     * @param lineCount Number of lines in the document.
     * @param eol       EOL character(s) used.
     */
    public PieceTreeSnapshot(String content, int lineCount, String eol) {
        this.content = content;
        this.lineCount = lineCount;
        this.eol = eol;
    }
}