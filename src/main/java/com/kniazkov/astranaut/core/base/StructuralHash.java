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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * One invocation's iterative postorder hash calculation.
 * @since 0.0.1
 */
final class StructuralHash {
    /**
     * Completed hashes, keyed by object identity.
     */
    private final Map<Node, Integer> completed = new IdentityHashMap<>();

    /**
     * Calculates a root hash and releases all state with this instance.
     * @param root Root node
     * @return Identity-keyed hashes of all visited nodes
     */
    public Map<Node, Integer> calculate(final Node root) {
        final Deque<Frame> stack = new ArrayDeque<>();
        final Map<Node, Boolean> active = new IdentityHashMap<>();
        stack.push(new Frame(root));
        active.put(root, Boolean.TRUE);
        while (!stack.isEmpty()) {
            final Frame frame = stack.peek();
            if (frame.index == frame.node.getChildCount()) {
                this.completed.put(frame.node, frame.hash);
                active.remove(frame.node);
                stack.pop();
            } else {
                final Node child = frame.node.getChild(frame.index);
                final Integer cached = this.completed.get(child);
                if (cached == null) {
                    StructuralHash.descend(stack, active, child);
                } else {
                    frame.hash = 31 * frame.hash + NodeStructure.tagHash(frame.node, frame.index);
                    frame.hash = 31 * frame.hash + cached;
                    frame.index += 1;
                }
            }
        }
        return java.util.Collections.unmodifiableMap(this.completed);
    }

    /**
     * Enters an unfinished child, rejecting cycles before growing the stack.
     * @param stack Work stack
     * @param active Current ancestry
     * @param child Child
     * @throws IllegalArgumentException If a cycle is encountered
     */
    private static void descend(final Deque<Frame> stack, final Map<Node, Boolean> active,
        final Node child) {
        if (active.put(child, Boolean.TRUE) != null) {
            throw new IllegalArgumentException("A syntax graph must not contain cycles");
        }
        stack.push(new Frame(child));
    }

    /**
     * A postorder frame.
     * @since 0.0.1
     */
    private static final class Frame {
        /**
         * Node being evaluated.
         */
        private final Node node;

        /**
         * Next child index.
         */
        private int index;

        /**
         * Accumulated ordered hash.
         */
        private int hash;

        /**
         * Starts a node.
         * @param node Node
         */
        private Frame(final Node node) {
            this.node = node;
            this.hash = NodeStructure.startHash(node);
        }
    }
}
