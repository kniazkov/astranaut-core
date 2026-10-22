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

import com.kniazkov.astranaut.core.base.NodeOccurrence;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Stack-safe, sequential preorder traversal, left to right, visiting every occurrence.
 * Callbacks run on the calling thread. Matching findAll entries prune their descendants.
 * Graphs must be acyclic. Space for pending traversal is proportional to tree depth.
 * @since 0.0.1
 */
public final class OccurrenceWalker {
    /**
     * Starting occurrence, which may itself have ancestors.
     */
    private final NodeOccurrence root;

    /**
     * Creates a traversal.
     * @param root Starting address
     */
    public OccurrenceWalker(final NodeOccurrence root) {
        this.root = Objects.requireNonNull(root);
    }

    /**
     * Stops at the first match in tree order.
     * @param visitor Matching callback
     * @return First matching occurrence
     */
    public Optional<NodeOccurrence> findFirst(final Predicate<NodeOccurrence> visitor) {
        Objects.requireNonNull(visitor);
        final Deque<Cursor> stack = new ArrayDeque<>();
        stack.push(new Cursor(this.root));
        Optional<NodeOccurrence> result = Optional.empty();
        while (!stack.isEmpty()) {
            final Cursor cursor = stack.peek();
            if (cursor.index == -1) {
                if (visitor.test(cursor.occurrence)) {
                    result = Optional.of(cursor.occurrence);
                    break;
                }
                cursor.index = 0;
            } else if (cursor.index < cursor.occurrence.getNode().getChildCount()) {
                stack.push(new Cursor(cursor.occurrence.getChild(cursor.index)));
                cursor.index += 1;
            } else {
                stack.pop();
            }
        }
        return result;
    }

    /**
     * Collects matches in preorder, pruning descendants of each matching occurrence.
     * @param visitor Matching callback
     * @return Immutable ordered matches
     */
    public List<NodeOccurrence> findAll(final Predicate<NodeOccurrence> visitor) {
        Objects.requireNonNull(visitor);
        final List<NodeOccurrence> result = new ArrayList<>(0);
        final Deque<Cursor> stack = new ArrayDeque<>();
        stack.push(new Cursor(this.root));
        while (!stack.isEmpty()) {
            final Cursor cursor = stack.peek();
            if (cursor.index == -1) {
                cursor.index = 0;
                if (visitor.test(cursor.occurrence)) {
                    result.add(cursor.occurrence);
                    stack.pop();
                }
            } else if (cursor.index < cursor.occurrence.getNode().getChildCount()) {
                stack.push(new Cursor(cursor.occurrence.getChild(cursor.index)));
                cursor.index += 1;
            } else {
                stack.pop();
            }
        }
        return List.copyOf(result);
    }

    /**
     * Collects all occurrences without pruning.
     * @return Immutable preorder sequence
     */
    public List<NodeOccurrence> collectAll() {
        final List<NodeOccurrence> result = new ArrayList<>(0);
        this.findFirst(
            occurrence -> {
                result.add(occurrence);
                return false;
            }
        );
        return List.copyOf(result);
    }

    /**
     * Traversal frame avoids pushing all children of a broad node at once.
     * @since 0.0.1
     */
    private static final class Cursor {
        /**
         * Current address.
         */
        private final NodeOccurrence occurrence;

        /**
         * Next child, minus one before visiting the node.
         */
        private int index;

        /**
         * Creates a frame.
         * @param occurrence Address
         */
        private Cursor(final NodeOccurrence occurrence) {
            this.occurrence = occurrence;
            this.index = -1;
        }
    }
}
