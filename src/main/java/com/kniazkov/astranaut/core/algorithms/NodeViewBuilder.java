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
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Task-local postorder construction of immutable views, memoized by source identity.
 * @since 0.0.1
 */
final class NodeViewBuilder {
    /**
     * Utility class.
     */
    private NodeViewBuilder() { }

    /**
     * Constructs selected edges once per source object, preserving each parent occurrence.
     * @param root Original root
     * @param selection Pure child-index selection
     * @param factory Snapshot publisher
     * @return Completed root
     */
    static Node build(
        final Node root, final Function<Node, List<Integer>> selection,
        final Factory factory
    ) {
        final Map<Node, Node> done = new IdentityHashMap<>();
        final Set<Node> active = Collections.newSetFromMap(new IdentityHashMap<>());
        final List<Frame> stack = new ArrayList<>(1);
        stack.add(new Frame(root, selection.apply(root)));
        active.add(root);
        while (!stack.isEmpty()) {
            final Frame frame = stack.getLast();
            if (frame.next < frame.indexes.size()) {
                final Node child = frame.original.getChild(frame.indexes.get(frame.next));
                if (done.containsKey(child)) {
                    frame.children.add(done.get(child));
                    frame.next += 1;
                } else {
                    stack.add(NodeViewBuilder.frame(child, active, selection));
                }
            } else {
                done.put(
                    frame.original,
                    factory.create(frame.original, frame.indexes, List.copyOf(frame.children))
                );
                active.remove(frame.original);
                stack.removeLast();
            }
        }
        return done.get(root);
    }

    /**
     * Schedules a child without allowing an ancestor to be revisited.
     * @param child Source child
     * @param active Ancestors
     * @param selection Pure selector
     * @return New frame
     * @throws IllegalArgumentException If the child is an ancestor
     */
    private static Frame frame(final Node child, final Set<Node> active,
        final Function<Node, List<Integer>> selection) {
        if (!active.add(child)) {
            throw new IllegalArgumentException("Cyclic node view");
        }
        return new Frame(child, selection.apply(child));
    }

    /**
     * Publishes one completed immutable view.
     * @since 0.0.1
     */
    @FunctionalInterface
    interface Factory {
        /**
         * Creates a snapshot whose child objects are already complete.
         * @param original Source
         * @param indexes Original edge positions
         * @param children Completed children
         * @return Snapshot
         */
        Node create(Node original, List<Integer> indexes, List<Node> children);
    }

    /**
     * Incomplete construction state which never escapes the invocation.
     * @since 0.0.1
     */
    private static final class Frame {
        /**
         * Source object.
         */
        private final Node original;

        /**
         * Selected edge positions.
         */
        private final List<Integer> indexes;

        /**
         * Completed children.
         */
        private final List<Node> children;

        /**
         * Next selected edge.
         */
        private int next;

        /**
         * Starts constructing one source node.
         * @param original Source
         * @param indexes Selected positions
         */
        private Frame(final Node original, final List<Integer> indexes) {
            this.original = original;
            this.indexes = List.copyOf(indexes);
            this.children = new ArrayList<>(indexes.size());
        }
    }
}
