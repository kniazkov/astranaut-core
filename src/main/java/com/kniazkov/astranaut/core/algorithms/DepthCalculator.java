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

import com.kniazkov.astranaut.core.base.Node;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Stack-safe tree depth with operation-local identity memoization for shared subtrees.
 * Instances retain no nodes and can be reused concurrently.
 * @since 0.0.1
 */
public final class DepthCalculator {
    /**
     * Measures depth, counting a leaf as one.
     * @param node Root node
     * @return Maximum number of nodes on a root-to-leaf path
     * @throws IllegalArgumentException If the graph contains a cycle
     * @checkstyle NonStaticMethodCheck (45 lines)
     */
    public int calculate(final Node node) {
        final Map<Node, Integer> calculated = new IdentityHashMap<>();
        final Set<Node> active = Collections.newSetFromMap(new IdentityHashMap<>());
        final Deque<Frame> stack = new ArrayDeque<>();
        stack.push(new Frame(node));
        active.add(node);
        while (!stack.isEmpty()) {
            final Frame frame = stack.peek();
            if (frame.index < frame.node.getChildCount()) {
                final Node child = frame.node.getChild(frame.index);
                final Integer known = calculated.get(child);
                if (known == null) {
                    DepthCalculator.push(child, active, stack);
                } else {
                    frame.maximum = Math.max(frame.maximum, known);
                    frame.index += 1;
                }
            } else {
                calculated.put(frame.node, frame.maximum + 1);
                active.remove(frame.node);
                stack.pop();
            }
        }
        return calculated.get(node);
    }

    /**
     * Adds a frame while rejecting back edges on the active path.
     * @param node Child
     * @param active Active path identities
     * @param stack Traversal frames
     * @throws IllegalArgumentException If the graph contains a cycle
     */
    private static void push(final Node node, final Set<Node> active, final Deque<Frame> stack) {
        if (!active.add(node)) {
            throw new IllegalArgumentException("Cyclic node graph");
        }
        stack.push(new Frame(node));
    }

    /**
     * Postorder state for one node.
     * @since 0.0.1
     */
    private static final class Frame {
        /**
         * Current node.
         */
        private final Node node;

        /**
         * Next child.
         */
        private int index;

        /**
         * Maximum completed child depth.
         */
        private int maximum;

        /**
         * Creates a frame.
         * @param node Current node
         */
        private Frame(final Node node) {
            this.node = node;
            this.index = 0;
            this.maximum = 0;
        }
    }
}
