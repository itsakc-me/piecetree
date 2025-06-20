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

/**
 * Represents a node in the Red-Black Tree used by PieceTree.
 * Each node corresponds to a piece of text in the document, storing metadata
 * about the text's location in a buffer and its position in the document.
 *
 * @since 1.0
 * @author <a href="https://github.com/itsakc-me">@itsakc.me</a>
 */
public class Node {
    // Text piece metadata
    public int bufferIndex;
    public int bufferStart;
    public int documentStart;
    public int length;
    public int[] lineStarts;

    // Red-Black Tree structure
    public Node left;
    public Node right;
    public Node parent;
    public boolean isRed;

    // Augmented metadata for efficient operations
    public int left_subtree_length;
    public int left_subtree_lfcnt;

    /**
     * Constructor for a Node.
     *
     * @param bufferIndex Index of the buffer containing this piece
     * @param bufferStart Starting position in the buffer
     * @param length      Length of the text piece
     * @param lineStarts  Array of positions of line breaks within this piece
     */
    public Node(int bufferIndex, int bufferStart, int length, int[] lineStarts) {
        this.bufferIndex = bufferIndex;
        this.bufferStart = bufferStart;
        this.documentStart = 0; // Initialized to 0, set during insertion
        this.length = length;
        this.lineStarts = lineStarts;
        this.left_subtree_length = 0;
        this.left_subtree_lfcnt = 0;
        this.left = null;
        this.right = null;
        this.parent = null;
        this.isRed = true; // New nodes are red by default
    }
}