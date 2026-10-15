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
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.NodeStructure;
import com.kniazkov.astranaut.core.base.NodeWithStructuralEquality;
import com.kniazkov.astranaut.core.base.Type;
import java.util.Map;

/**
 * Creates extended nodes ({@link ExtNode}) from 'regular' nodes ({@link Node}).
 * @since 0.0.1
 */
public final class ExtNodeCreator {
    /**
     * Creates an extended node from a 'regular' node.
     * @param node Original node
     * @return Extended node
     */
    public ExtNode create(final Node node) {
        return this.create(node, null, NodeStructure.hashes(node));
    }

    /**
     * Creates an extended node from a 'regular' node (recursive method).
     * @param node Original node
     * @param parent Parent node of the node to be created
     * @param hashes Operation-local hash snapshot
     * @return Extended node
     */
    private ExtNodeImpl create(final Node node, final ExtNode parent,
        final Map<Node, Integer> hashes) {
        final ExtNodeImpl ext = new ExtNodeImpl();
        ext.prototype = node;
        ext.parent = parent;
        ext.index = -1;
        ext.hash = hashes.get(node);
        final int count = node.getChildCount();
        ext.children = new ExtNodeImpl[count];
        int index;
        for (index = 0; index < count; index = index + 1) {
            final ExtNodeImpl child = this.create(node.getChild(index), ext, hashes);
            child.index = index;
            ext.children[index] = child;
        }
        for (index = 1; index < count; index = index + 1) {
            ext.children[index].left = ext.children[index - 1];
        }
        for (index = 0; index < count - 1; index = index + 1) {
            ext.children[index].right = ext.children[index + 1];
        }
        return ext;
    }

    /**
     * Class that implements the extended node interface.
     * @since 0.0.1
     */
    private static final class ExtNodeImpl extends NodeWithStructuralEquality implements ExtNode {
        /**
         * Prototype (not-extended) node.
         */
        private Node prototype;

        /**
         * Parent node of this node, which is the node that has this node as on of its children.
         */
        private ExtNode parent;

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
        private int hash;

        /**
         * Array of child nodes wrapped in the extended node interface.
         */
        private ExtNodeImpl[] children;

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
