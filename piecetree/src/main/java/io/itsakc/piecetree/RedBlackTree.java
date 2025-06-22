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

import java.util.ArrayList;
import java.util.List;

/**
 * A Red-Black Tree implementation for managing text pieces in the PieceTree,
 * ordered by document offset. This tree supports efficient insertion, deletion,
 * and search operations, each operating in O(log n) time complexity.
 * <p>
 * The tree maintains the following properties:
 * <ul>
 *   <li>Each node is either red or black.</li>
 *   <li>The root is black.</li>
 *   <li>All leaves (NIL nodes) are black.</li>
 *   <li>If a node is red, then both its children are black.</li>
 *   <li>For each node, all simple paths from the node to descendant leaves contain the same number of black nodes.</li>
 * </ul>
 * </p>
 * <p>
 * This class is designed to work with a {@link BufferManager} to manage text buffers.
 * </p>
 *
 * @see BufferManager
 * @since 1.0
 * @author <a href="https://github.com/itsakc-me">@itsakc.me</a>
 */
public class RedBlackTree {

    private Node root;
    private final BufferManager bufferManager;

    private int lineCount;

    public RedBlackTree(BufferManager bufferManager) {
        this.root = null;
        this.bufferManager = bufferManager;
        this.lineCount = 0;
    }

    /**
     * Inserts a new node into the tree, maintaining Red-Black properties.
     *
     * @param newNode The node to insert.
     */
    public void insert(Node newNode) {
        Node sNode = findNodeContaining(newNode.documentStart);
        updateDocumentStartsFrom(sNode, newNode.documentStart + newNode.length);

        Node y = null;
        Node x = root;

        // Find the position to insert based on document offset
        while (x != null) {
            y = x;
            if (newNode.documentStart < x.documentStart) {
                x = x.left;
            } else {
                x = x.right;
            }
        }

        newNode.parent = y;

        if (y == null) {
            root = newNode;
        } else if (newNode.documentStart < y.documentStart) {
            y.left = newNode;
        } else {
            y.right = newNode;
        }

        newNode.left = null;
        newNode.right = null;
        newNode.isRed = true; // New node is red

        fixInsert(newNode);
    }

    /**
     * Deletes a range of text from start to end, maintaining Red-Black properties.
     * Also updates documentStart values of all subsequent nodes.
     *
     * @param start The starting document offset of the range to delete.
     * @param end   The ending document offset (exclusive).
     */
    public void deleteRange(int start, int end) {
        if (start >= end) return;

        // Find node containing start offset
        Node nStart = findNodeContaining(start);
        if (nStart == null) return;

        int dStart = nStart.documentStart;
        Node leftSplitNode = null;

        // Split node at start if necessary
        if (dStart < start) {
            int splitPoint = start - dStart;
            int[] leftLineStarts = computeLineStarts(nStart, 0, splitPoint);
            int[] rightLineStarts = computeLineStarts(nStart, splitPoint, nStart.length);
            Node left = new Node(nStart.bufferIndex, nStart.bufferStart, splitPoint, leftLineStarts);
            Node right = new Node(nStart.bufferIndex, nStart.bufferStart + splitPoint, nStart.length - splitPoint, rightLineStarts);
            left.documentStart = dStart;
            right.documentStart = start;
            deleteNode(nStart);
            insert(left);
            insert(right);
            leftSplitNode = left; // Keep reference to the left split node
        }

        // Find node containing end offset
        Node nEnd = findNodeContaining(end);
        if (nEnd == null) return;

        int dEnd = nEnd.documentStart;

        // Split node at end if necessary
        if (dEnd + nEnd.length > end) {
            int splitPoint = end - dEnd;
            int[] leftLineStarts = computeLineStarts(nEnd, 0, splitPoint);
            int[] rightLineStarts = computeLineStarts(nEnd, splitPoint, nEnd.length);
            Node left = new Node(nEnd.bufferIndex, nEnd.bufferStart, splitPoint, leftLineStarts);
            Node right = new Node(nEnd.bufferIndex, nEnd.bufferStart + splitPoint, nEnd.length - splitPoint, rightLineStarts);
            left.documentStart = dEnd;
            right.documentStart = end;
            deleteNode(nEnd);
            insert(left);
            insert(right);
        }

        // Calculate the new document start position for updating subsequent nodes
        int newDocumentStart;
        if (leftSplitNode != null) {
            // If we split the start node, use the left split node's start + length
            newDocumentStart = leftSplitNode.documentStart + leftSplitNode.length;
        } else {
            // If no split occurred, use the start offset
            newDocumentStart = start;
        }

        // Find the first node that should have its documentStart updated
        // This is the successor of the last node to be deleted
        Node firstNodeToUpdate = null;

        // Delete nodes with documentStart in [start, end)
        List<Node> nodesToDelete = new ArrayList<>();
        Node current = findNodeContaining(start);
        while (current != null && current.documentStart < end) {
            Node next = getSuccessor(current);
            if (current.documentStart + current.length > start) {
                nodesToDelete.add(current);
                // Keep track of the successor of the last deleted node
                if (next != null && (firstNodeToUpdate == null || next.documentStart > firstNodeToUpdate.documentStart)) {
                    firstNodeToUpdate = next;
                }
            }
            current = next;
        }

        // Delete the collected nodes
        for (Node nodeToDelete : nodesToDelete) {
            lineCount -= nodeToDelete.lineStarts.length;
            deleteNode(nodeToDelete);
        }

        // Update documentStart values for all subsequent nodes
        updateDocumentStartsFrom(firstNodeToUpdate, newDocumentStart);
    }

    /**
     * Updates documentStart values for all nodes starting from the given node and continuing
     * with all subsequent nodes in document order.
     *
     * @param startNode The first node to update
     * @param newStart The new documentStart value for the first node
     */
    private void updateDocumentStartsFrom(Node startNode, int newStart) {
        if (startNode == null || startNode.documentStart == newStart) return;

        int currentStart = newStart;
        Node current = startNode;

        // Collect all nodes that need updating in document order
        List<Node> nodesToUpdate = new ArrayList<>();
        while (current != null) {
            nodesToUpdate.add(current);
            current = getSuccessor(current);
        }

        // Update documentStart values
        for (Node node : nodesToUpdate) {
            node.documentStart = currentStart;
            currentStart += node.length;
        }
    }

    /**
     * Finds the node containing a specific document offset.
     *
     * @param offset The document offset to search for.
     * @return The node containing the offset, or null if not found.
     */
    public Node findNodeContaining(int offset) {
        if (offset == 0) return getFirst();

        Node current = root;
        while (current != null) {
            // Check if offset is within this node's range
            if (offset >= current.documentStart && offset < current.documentStart + current.length) {
                return current;
            }
            // Navigate based on documentStart comparison
            if (offset < current.documentStart) {
                current = current.left;
            } else {
                current = current.right;
            }
        }
        return null;
    }

    /**
     * Gets the root of the tree.
     *
     * @return The root node.
     */
    public Node getRoot() {
        return root;
    }

    /**
     * Gets the first node in the tree.
     *
     * @return The first node.
     */
    public Node getFirst() {
        return getMinimum(root);
    }

    /**
     * Gets the last node in the tree.
     *
     * @return The last node.
     */
    public Node getLast() {
        return getMaximum(root);
    }

    /**
     * Validates whether the current tree satisfies all Red-Black Tree properties.
     *
     * <ul>
     *   <li>Property 1: Every node is either red or black.</li>
     *   <li>Property 2: The root is black.</li>
     *   <li>Property 3: All leaves (null) are black.</li>
     *   <li>Property 4: Red nodes cannot have red children (no two reds in a row).</li>
     *   <li>Property 5: Every path from a node to its descendant null nodes has the same number of black nodes.</li>
     * </ul>
     *
     * @return {@code true} if the tree satisfies all Red-Black properties; {@code false} otherwise.
     */
    public boolean validateRedBlackProperties() {
        if (root != null && root.isRed) {
            return false; // Root must be black
        }
        return validateRedBlackHelper(root) != -1;
    }

    /**
     * Adds to the total number of lines in the document.
     *
     * @param lineCount The number of lines to add.
     */
    public void addLineCount(int lineCount) {
        this.lineCount += lineCount;
    }

    /**
     * Gets the total number of lines in the document.
     *
     * @return The total line count.
     */
    public int getLineCount() {
        return lineCount;
    }

    /**
     * Helper method to recursively validate Red-Black Tree properties.
     * Returns the black height if valid, or -1 if any rule is violated.
     *
     * @param node the current node being validated.
     * @return the black height from this node to its leaves, or -1 if invalid.
     */
    private int validateRedBlackHelper(Node node) {
        if (node == null) return 1; // Null nodes are considered black (black height = 1)

        // Red node cannot have red children
        if (node.isRed) {
            if ((node.left != null && node.left.isRed) ||
                    (node.right != null && node.right.isRed)) {
                return -1;
            }
        }

        int leftBlackHeight = validateRedBlackHelper(node.left);
        int rightBlackHeight = validateRedBlackHelper(node.right);

        // Propagate failure upward if found
        if (leftBlackHeight == -1 || rightBlackHeight == -1) return -1;

        // Both sides must have equal black height
        if (leftBlackHeight != rightBlackHeight) return -1;

        // Add 1 to black height if current node is black
        return leftBlackHeight + (node.isRed ? 0 : 1);
    }

    /**
     * Deletes a single node from the tree, maintaining Red-Black properties.
     *
     * @param node The node to delete.
     */
    public void deleteNode(Node node) {
        if (node == null) return;

        Node y = node;
        boolean yOriginalColor = y.isRed;
        Node x, xParent;

        if (node.left == null) {
            x = node.right;
            xParent = node.parent;
            transplant(node, node.right);
        } else if (node.right == null) {
            x = node.left;
            xParent = node.parent;
            transplant(node, node.left);
        } else {
            y = getMinimum(node.right);
            yOriginalColor = y.isRed;
            x = y.right;
            xParent = y;

            if (y.parent == node) {
                xParent = y;
            } else {
                transplant(y, y.right);
                y.right = node.right;
                y.right.parent = y;
            }

            transplant(node, y);
            y.left = node.left;
            y.left.parent = y;
            y.isRed = node.isRed;
        }

        if (!yOriginalColor) {
            fixDelete(x, xParent);
        }

        updateAugmentedFieldsUpward(xParent);
    }

    /**
     * Transplants a node with another, updating parent pointers.
     *
     * @param u The node to replace.
     * @param v The node to replace with, or null.
     */
    private void transplant(Node u, Node v) {
        if (u.parent == null) {
            root = v;
        } else if (u == u.parent.left) {
            u.parent.left = v;
        } else {
            u.parent.right = v;
        }
        if (v != null) {
            v.parent = u.parent;
        }
    }

    /**
     * Fixes the tree after deletion to maintain Red-Black properties.
     *
     * @param x      The node to start fixing from.
     * @param xParent The parent of x.
     */
    private void fixDelete(Node x, Node xParent) {
        while (x != root && (x == null || !x.isRed)) {
            if (x == xParent.left) {
                Node w = xParent.right;
                if (w != null && w.isRed) {
                    w.isRed = false;
                    xParent.isRed = true;
                    leftRotate(xParent);
                    w = xParent.right;
                }
                if (w == null || (w.left == null || !w.left.isRed) && (w.right == null || !w.right.isRed)) {
                    if (w != null) w.isRed = true;
                    x = xParent;
                    xParent = x.parent;
                } else {
                    if (w.right == null || !w.right.isRed) {
                        if (w.left != null) w.left.isRed = false;
                        w.isRed = true;
                        rightRotate(w);
                        w = xParent.right;
                    }
                    w.isRed = xParent.isRed;
                    xParent.isRed = false;
                    if (w.right != null) w.right.isRed = false;
                    leftRotate(xParent);
                    x = root;
                }
            } else {
                Node w = xParent.left;
                if (w != null && w.isRed) {
                    w.isRed = false;
                    xParent.isRed = true;
                    rightRotate(xParent);
                    w = xParent.left;
                }
                if (w == null || (w.right == null || !w.right.isRed) && (w.left == null || !w.left.isRed)) {
                    if (w != null) w.isRed = true;
                    x = xParent;
                    xParent = x.parent;
                } else {
                    if (w.left == null || !w.left.isRed) {
                        if (w.right != null) w.right.isRed = false;
                        w.isRed = true;
                        leftRotate(w);
                        w = xParent.left;
                    }
                    w.isRed = xParent.isRed;
                    xParent.isRed = false;
                    if (w.left != null) w.left.isRed = false;
                    rightRotate(xParent);
                    x = root;
                }
            }
        }
        if (x != null) x.isRed = false;
    }

    /**
     * Finds the node with the minimum start in a subtree.
     *
     * @param node The root of the subtree.
     * @return The node with the minimum start.
     */
    private Node getMinimum(Node node) {
        while (node != null && node.left != null) {
            node = node.left;
        }
        return node;
    }

    /**
     * Finds the node with the maximum start in a subtree.
     *
     * @param node The root of the subtree.
     * @return The node with the maximum start.
     */
    private Node getMaximum(Node node) {
        while (node != null && node.right != null) {
            node = node.right;
        }
        return node;
    }

    /**
     * Finds the successor of a node in document order.
     *
     * @param node The node to find the successor for.
     * @return The successor node, or null if none exists.
     */
    public Node getSuccessor(Node node) {
        if (node.right != null) {
            return getMinimum(node.right);
        }
        Node y = node.parent;
        while (y != null && node == y.right) {
            node = y;
            y = y.parent;
        }
        return y;
    }

    /**
     * Fixes the tree after insertion to maintain Red-Black properties.
     *
     * @param z The inserted node.
     */
    private void fixInsert(Node z) {
        while (z.parent != null && z.parent.isRed) {
            if (z.parent == z.parent.parent.left) {
                Node y = z.parent.parent.right;
                if (y != null && y.isRed) {
                    z.parent.isRed = false;
                    y.isRed = false;
                    z.parent.parent.isRed = true;
                    z = z.parent.parent;
                } else {
                    if (z == z.parent.right) {
                        z = z.parent;
                        leftRotate(z);
                    }
                    z.parent.isRed = false;
                    z.parent.parent.isRed = true;
                    rightRotate(z.parent.parent);
                }
            } else {
                Node y = z.parent.parent.left;
                if (y != null && y.isRed) {
                    z.parent.isRed = false;
                    y.isRed = false;
                    z.parent.parent.isRed = true;
                    z = z.parent.parent;
                } else {
                    if (z == z.parent.left) {
                        z = z.parent;
                        rightRotate(z);
                    }
                    z.parent.isRed = false;
                    z.parent.parent.isRed = true;
                    leftRotate(z.parent.parent);
                }
            }
        }
        root.isRed = false;
    }

    /**
     * Performs a left rotation on the tree at node x.
     *
     * @param x The node to rotate.
     */
    private void leftRotate(Node x) {
        Node y = x.right;
        x.right = y.left;
        if (y.left != null) {
            y.left.parent = x;
        }
        y.parent = x.parent;
        if (x.parent == null) {
            root = y;
        } else if (x == x.parent.left) {
            x.parent.left = y;
        } else {
            x.parent.right = y;
        }
        y.left = x;
        x.parent = y;
        updateAugmentedFields(x);
        updateAugmentedFields(y);
    }

    /**
     * Performs a right rotation on the tree at node y.
     *
     * @param y The node to rotate.
     */
    private void rightRotate(Node y) {
        Node x = y.left;
        y.left = x.right;
        if (x.right != null) {
            x.right.parent = y;
        }
        x.parent = y.parent;
        if (y.parent == null) {
            root = x;
        } else if (y == y.parent.right) {
            y.parent.right = x;
        } else {
            y.parent.left = x;
        }
        x.right = y;
        y.parent = x;
        updateAugmentedFields(y);
        updateAugmentedFields(x);
    }

    /**
     * Updates the augmented fields (left_subtree_length, left_subtree_lfcnt) for a node.
     *
     * @param node The node to update.
     */
    private void updateAugmentedFields(Node node) {
        if (node == null) return;
        int leftLength = node.left != null ? node.left.left_subtree_length : 0;
        int leftLfcnt = node.left != null ? node.left.left_subtree_lfcnt : 0;
        node.left_subtree_length = leftLength + (node.left != null ? node.left.length : 0);
        node.left_subtree_lfcnt = leftLfcnt + (node.left != null ? node.left.lineStarts.length - 1 : 0);
    }

    /**
     * Updates augmented fields for a node and its ancestors.
     *
     * @param node The starting node.
     */
    private void updateAugmentedFieldsUpward(Node node) {
        while (node != null) {
            updateAugmentedFields(node);
            node = node.parent;
        }
    }

    /**
     * Computes line starts for a portion of a node's text.
     *
     * @param node  The node containing the text.
     * @param start The start offset within the node's text.
     * @param end   The end offset within the node's text.
     * @return An array of line start positions.
     */
    @TargetApi(Build.VERSION_CODES.N)
    private int[] computeLineStarts(Node node, int start, int end) {
        if (node == null) {
            throw new IllegalArgumentException("Buffer cannot be null");
        }

        char[] buffer = bufferManager.getBuffer(node.bufferIndex);

        if (buffer.length == 0) {
            return new int[0];
        }

        // Pre-allocate with estimated capacity (assume average 50 chars per line)
        List<Integer> starts = new ArrayList<>(Math.max(16, (end - start) / 50));

        // Single pass through buffer with optimized loop
        for (int i = start; i < end; i++) {
            char ch = buffer[node.bufferStart + i];
            if (ch == PieceTree.LineFeed) {
                starts.add(i + 1 - start); // Next line starts after LF
            } else if (ch == PieceTree.CarriageReturn) {
                // Handle CRLF sequence - skip LF if it follows CR
                if (node.bufferStart + i + 1 < end && buffer[node.bufferStart + i + 1] == PieceTree.LineFeed) {
                    starts.add(i + 2 - start); // Next line starts after CRLF
                    i++; // Skip the LF
                } else {
                    starts.add(i + 1 - start); // Next line starts after standalone CR
                }
            }
        }

        // Convert to array efficiently
        return starts.stream().mapToInt(Integer::intValue).toArray();
    }
}