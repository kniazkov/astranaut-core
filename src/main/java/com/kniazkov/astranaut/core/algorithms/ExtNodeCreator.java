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
package com.kniazkov.astranaut.core.algorithms;

import com.kniazkov.astranaut.core.base.ExtNode;
import com.kniazkov.astranaut.core.base.Fragment;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.NodeStructure;
import com.kniazkov.astranaut.core.base.NodeWithStructuralEquality;
import com.kniazkov.astranaut.core.base.Type;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;

/**
 * Creates extended nodes ({@link ExtNode}) from 'regular' nodes ({@link Node}).
 * @since 0.0.1
 */
public final class ExtNodeCreator {
    /**
     * Creates an extended node from a 'regular' node.
     * @param node Original node
     * @return Extended node
     * @checkstyle NonStaticMethodCheck (45 lines)
     */
    public ExtNode create(final Node node) {
        final Map<Node, Integer> hashes = NodeStructure.hashes(node);
        final ExtNodeImpl root = new ExtNodeImpl(node, null, hashes.get(node));
        final Deque<ExtNodeImpl> pending = new ArrayDeque<>();
        pending.push(root);
        while (!pending.isEmpty()) {
            final ExtNodeImpl parent = pending.pop();
            for (int index = 0; index < parent.children.length; index += 1) {
                final Node prototype = parent.prototype.getChild(index);
                final ExtNodeImpl child = new ExtNodeImpl(prototype, parent, hashes.get(prototype));
                child.index = index;
                parent.children[index] = child;
                if (index > 0) {
                    child.left = parent.children[index - 1];
                    parent.children[index - 1].right = child;
                }
                pending.push(child);
            }
        }
        return root;
    }

    /**
     * Class that implements the extended node interface.
     * @since 0.0.1
     */
    private static final class ExtNodeImpl extends NodeWithStructuralEquality implements ExtNode {
        /**
         * Prototype (not-extended) node.
         */
        private final Node prototype;

        /**
         * Parent node of this node, which is the node that has this node as on of its children.
         */
        private final ExtNode parent;

        /**
         * Index (sequence number) of this node in the list of children of this node's parent.
         */
        private int index;

        /**
         * Left neighbor node of this node, which is the node whose index in the parent node
         *  is one less than the index of this node.
         */
        private ExtNode left;

        /**
         * Right neighbor node of this node, which is the node whose index in the parent node
         *  is one greater than the index of this node.
         */
        private ExtNode right;

        /**
         * Absolute hash.
         */
        private final int hash;

        /**
         * Array of child nodes wrapped in the extended node interface.
         */
        private final ExtNodeImpl[] children;

        /**
         * Creates a wrapper. Links are completed before the root is published.
         * @param prototype Original node
         * @param parent Parent wrapper
         * @param hash Cached structural hash
         */
        private ExtNodeImpl(final Node prototype, final ExtNode parent, final int hash) {
            this.prototype = prototype;
            this.parent = parent;
            this.hash = hash;
            this.children = new ExtNodeImpl[prototype.getChildCount()];
            this.index = -1;
        }

        @Override
        public Fragment getFragment() {
            return this.prototype.getFragment();
        }

        @Override
        public Node getPrototype() {
            return this.prototype;
        }

        @Override
        public ExtNode getParent() {
            return this.parent;
        }

        @Override
        public int getIndex() {
            return this.index;
        }

        @Override
        public ExtNode getLeft() {
            return this.left;
        }

        @Override
        public ExtNode getRight() {
            return this.right;
        }

        @Override
        public int getAbsoluteHash() {
            return this.hash;
        }

        @Override
        public Type getType() {
            return this.prototype.getType();
        }

        @Override
        public String getData() {
            return this.prototype.getData();
        }

        @Override
        public int getChildCount() {
            return this.children.length;
        }

        @Override
        public Node getChild(final int idx) {
            return this.children[idx];
        }

        @Override
        public java.util.Optional<String> getChildTag(final int idx) {
            return this.prototype.getChildTag(idx);
        }

        @Override
        public java.util.Optional<Node> getChild(final String tag) {
            Objects.requireNonNull(tag);
            java.util.Optional<Node> result = java.util.Optional.empty();
            for (int idx = 0; idx < this.children.length; idx += 1) {
                if (this.getChildTag(idx).filter(tag::equals).isPresent()) {
                    result = java.util.Optional.of(this.children[idx]);
                    break;
                }
            }
            return result;
        }

        @Override
        public ExtNode getExtChild(final int idx) {
            return this.children[idx];
        }

        @Override
        public Map<String, String> getProperties() {
            return this.prototype.getProperties();
        }

        @Override
        public String toString() {
            return this.prototype.toString();
        }
    }
}
