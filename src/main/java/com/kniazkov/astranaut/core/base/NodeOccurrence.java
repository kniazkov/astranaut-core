/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2026 Ivan Kniazkov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.kniazkov.astranaut.core.base;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

/**
 * An address in an immutable tree: root identity followed by child indices.
 * Sharing and structural equality do not merge addresses. Parent links use constant
 * space per occurrence; no full path is copied. Cyclic node graphs are not trees.
 * @since 0.0.1
 */
public final class NodeOccurrence {
    /**
     * Addressed node.
     */
    private final Node node;

    /**
     * Parent address, absent only at the root.
     */
    private final NodeOccurrence parent;

    /**
     * Child index, or minus one for the root.
     */
    private final int index;

    /**
     * Distance from the root.
     */
    private final int depth;

    /**
     * Cached address hash, independent of structural hashes.
     */
    private final int hash;

    /**
     * Addresses a root by object identity.
     * @param root Non-null root
     */
    public NodeOccurrence(final Node root) {
        this(root, null, -1);
    }

    /**
     * Creates a validated address.
     * @param node Addressed node
     * @param parent Parent, or null at the root
     * @param index Child index
     */
    @SuppressWarnings("PMD.ConstructorOnlyInitializesOrCallOtherConstructors")
    private NodeOccurrence(final Node node, final NodeOccurrence parent, final int index) {
        this.node = Objects.requireNonNull(node);
        this.parent = parent;
        this.index = index;
        if (parent == null) {
            this.depth = 0;
            this.hash = System.identityHashCode(node);
        } else {
            this.depth = parent.depth + 1;
            this.hash = parent.hash * 31 + index;
        }
    }

    /**
     * Gets the addressed node; source information remains on the node.
     * @return Node
     */
    public Node getNode() {
        return this.node;
    }

    /**
     * Gets the parent address.
     * @return Parent, empty at the root
     */
    public Optional<NodeOccurrence> getParent() {
        return Optional.ofNullable(this.parent);
    }

    /**
     * Gets the index in the parent's compact ordered children.
     * @return Index, minus one at the root
     */
    public int getIndex() {
        return this.index;
    }

    /**
     * Gets the distance from the root.
     * @return Depth, zero at the root
     */
    public int getDepth() {
        return this.depth;
    }

    /**
     * Reads the tag from the parent edge, not from the child object.
     * @return Tag, empty for the root or an unnamed edge
     */
    public Optional<String> getTag() {
        Optional<String> result = Optional.empty();
        if (this.parent != null) {
            result = this.parent.node.getChildTag(this.index);
        }
        return result;
    }

    /**
     * Addresses a child without copying the path.
     * @param position Child index
     * @return Child occurrence
     * @throws IndexOutOfBoundsException If there is no such child
     */
    public NodeOccurrence getChild(final int position) {
        Objects.checkIndex(position, this.node.getChildCount());
        return new NodeOccurrence(this.node.getChild(position), this, position);
    }

    /**
     * Addresses a sibling by its index in the same parent.
     * @param position Sibling index
     * @return Sibling, empty for the root or an out-of-range index
     */
    public Optional<NodeOccurrence> getSibling(final int position) {
        Optional<NodeOccurrence> result = Optional.empty();
        if (this.parent != null && position >= 0
            && position < this.parent.node.getChildCount()) {
            result = Optional.of(this.parent.getChild(position));
        }
        return result;
    }

    /**
     * Enumerates ancestors nearest first. Iterators are independent and may be retained.
     * @return Stable ancestor sequence, excluding this occurrence
     */
    public Iterable<Node> getAncestors() {
        return () -> new Ancestors(this.parent);
    }

    @Override
    public int hashCode() {
        return this.hash;
    }

    @Override
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    public boolean equals(final Object other) {
        boolean equal = false;
        if (other instanceof NodeOccurrence occurrence && this.depth == occurrence.depth
            && this.hash == occurrence.hash) {
            NodeOccurrence left = this;
            NodeOccurrence right = occurrence;
            while (left.parent != null && left.index == right.index) {
                left = left.parent;
                right = right.parent;
            }
            equal = left == right || left.parent == null && right.parent == null
                && left.node == right.node;
        }
        return equal;
    }

    /**
     * Independent nearest-first ancestor iterator.
     * @since 0.0.1
     */
    private static final class Ancestors implements Iterator<Node> {
        /**
         * Next ancestor, null at the end.
         */
        private NodeOccurrence current;

        /**
         * Creates an iterator.
         * @param first Immediate parent
         */
        private Ancestors(final NodeOccurrence first) {
            this.current = first;
        }

        @Override
        public boolean hasNext() {
            return this.current != null;
        }

        @Override
        public Node next() {
            if (this.current == null) {
                throw new NoSuchElementException();
            }
            final Node result = this.current.node;
            this.current = this.current.parent;
            return result;
        }
    }
}
