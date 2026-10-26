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
package com.kniazkov.astranaut.core.algorithms.normalization;

import com.kniazkov.astranaut.core.algorithms.ParallelOccurrenceWalker;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.NodeOccurrence;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Stack-safe normalization with operation-local identity memoization and cycle rejection.
 * No source builder is invoked and no original node is retained in the result.
 * @since 0.0.1
 */
public final class Normalizer {
    /**
     * Normalizes each distinct source object once per invocation.
     * @param root Source root
     * @return Independent normalized root
     * @throws IllegalArgumentException If the input contains a cycle
     * @checkstyle NonStaticMethodCheck (45 lines)
     */
    public NormalizedNode normalize(final Node root) {
        Objects.requireNonNull(root);
        final Map<Node, NormalizedNode> done = new IdentityHashMap<>();
        final Set<Node> active = Collections.newSetFromMap(new IdentityHashMap<>());
        final Deque<Frame> stack = new ArrayDeque<>();
        stack.push(new Frame(root));
        active.add(root);
        while (!stack.isEmpty()) {
            final Frame frame = stack.peek();
            if (frame.node instanceof NormalizedNode normalized) {
                done.put(frame.node, normalized);
                active.remove(frame.node);
                stack.pop();
            } else if (frame.index < frame.node.getChildCount()) {
                final Node child = frame.node.getChild(frame.index);
                if (done.containsKey(child)) {
                    frame.children.add(done.get(child));
                    frame.index += 1;
                } else {
                    Normalizer.push(child, active, stack);
                }
            } else {
                done.put(frame.node, Normalizer.assemble(frame.node, frame.children));
                active.remove(frame.node);
                stack.pop();
            }
        }
        return done.get(root);
    }

    /**
     * Uses the caller's bounded branch strategy, preserving deterministic child order.
     * Invoke outside the strategy's executor; source adapters must allow concurrent reads.
     * Identity memoization is local to each branch; sharing across branches may be duplicated.
     * @param root Original root
     * @param strategy Explicit parallel policy, including caller-selected threshold
     * @return Normalized tree, structurally equal to sequential normalization
     */
    public NormalizedNode normalize(final Node root, final ParallelOccurrenceWalker strategy) {
        Objects.requireNonNull(strategy);
        final NormalizedNode result;
        if (root instanceof NormalizedNode normalized) {
            result = normalized;
        } else {
            result = Normalizer.assemble(
                root, strategy.mapChildren(
                    new NodeOccurrence(root), child -> this.normalize(child.getNode())
                )
            );
        }
        return result;
    }

    /**
     * Publishes metadata and already-normalized children.
     * @param original Source node
     * @param children Normalized children
     * @return Immutable independent node
     * @throws IllegalArgumentException If an adapter supplies duplicate tags
     */
    private static NormalizedNode assemble(final Node original,
        final List<? extends Node> children) {
        final List<Optional<String>> tags = new ArrayList<>(children.size());
        final Set<String> unique = new HashSet<>();
        for (int index = 0; index < children.size(); index += 1) {
            final Optional<String> tag = original.getChildTag(index);
            if (tag.isPresent() && !unique.add(tag.orElseThrow())) {
                throw new IllegalArgumentException("Duplicate source child tag");
            }
            tags.add(tag);
        }
        final List<Node> nodes = new ArrayList<>(children.size() + 2);
        nodes.add(new Properties(original.getProperties()));
        nodes.add(new Properties(original.getType().getProperties()));
        for (int index = 0; index < children.size(); index += 1) {
            nodes.add(new NormalizedEdge(tags.get(index), children.get(index)));
        }
        return new NormalizedNode(
            new NormalizationType(original.getTypeName()),
            new NormalizedNode.State(original.getData(), original.getFragment(), nodes)
        );
    }

    /**
     * Adds a child frame, rejecting an edge back into the active path.
     * @param node Child
     * @param active Active identities
     * @param stack Frames
     * @throws IllegalArgumentException If a cycle is found
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
         * Source node.
         */
        private final Node node;

        /**
         * Completed children.
         */
        private final List<Node> children;

        /**
         * Next source child index.
         */
        private int index;

        /**
         * Creates a frame.
         * @param node Source node
         */
        private Frame(final Node node) {
            this.node = node;
            this.children = new ArrayList<>(node.getChildCount());
            this.index = 0;
        }
    }
}
