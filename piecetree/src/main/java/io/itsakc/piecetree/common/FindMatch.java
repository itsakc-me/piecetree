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

import java.util.List;

/**
 * Represents a single match found during a search.
 *
 * @since 1.0
 * @author <a href="https://github.com/itsakc-me">@itsakc.me</a>
 */
public class FindMatch {
    /** The start offset of the match. */
    public int startOffset;

    /** The end offset of the match (exclusive). */
    public int endOffset;

    /** The actual match content (can include capture groups). */
    public List<String> matches;

    /**
     * Constructs a found match.
     *
     * @param startOffset The start offset of the match.
     * @param endOffset   The end offset of the match.
     * @param matches  Matched text(s).
     */
    public FindMatch(int startOffset, int endOffset, List<String> matches) {
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.matches = matches;
    }

    @Override
    public String toString() {
        return "[" + startOffset + " -> " + endOffset + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FindMatch match = (FindMatch) o;
        return startOffset == match.startOffset && endOffset == match.endOffset;
    }

    @Override
    public int hashCode() {
        int result = startOffset;
        result = 31 * result + endOffset;
        return result;
    }
}