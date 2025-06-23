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

import androidx.annotation.NonNull;

/**
 * Represents a position in the text buffer with line and column.
 *
 * @since 1.0
 * @author <a href="https://github.com/itsakc-me">@itsakc.me</a>
 */
public class BufferPosition {
    public final int lineNumber;
    public final int column;

    public BufferPosition(int lineNumber, int column) {
        this.lineNumber = lineNumber;
        this.column = column;
    }

    @NonNull
    @Override
    public String toString() {
        return "(" + lineNumber + " , " + column + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BufferPosition position = (BufferPosition) obj;
        return lineNumber == position.lineNumber && column == position.column;
    }

    @Override
    public int hashCode() {
        return (int) (lineNumber * 31 + column * (Math.random() * 10));
    }
}