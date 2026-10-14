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
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Iterative queries over a type DAG, with no global cache or traversal state.
 * @since 0.0.1
 */
final class TypeHierarchy {
    /**
     * Starting type.
     */
    private final AbstractType start;

    /**
     * Creates a query.
     * @param start Starting type
     */
    TypeHierarchy(final AbstractType start) {
        this.start = start;
    }

    /**
     * Tests reachability, stopping at the first equal type.
     * @param target Requested abstract or concrete type
     * @return Whether reachable
     */
    boolean contains(final AbstractType target) {
        Objects.requireNonNull(target);
        final Deque<AbstractType> pending = new ArrayDeque<>();
        final Set<AbstractType> visited = new HashSet<>();
        pending.add(this.start);
        boolean found = false;
        while (!pending.isEmpty()) {
            final AbstractType current = pending.removeFirst();
            if (target.equals(current)) {
                found = true;
                break;
            }
            if (visited.add(current)) {
                pending.addAll(current.getBaseTypes());
            }
        }
        return found;
    }

    /**
     * Finds the unique minimal common ancestor in the subtype ordering.
     * Common ancestors form an upward-closed set. Removing their direct bases
     * therefore removes all non-minimal candidates.
     * @param other Other operand
     * @return Most specific common base
     */
    Optional<AbstractType> commonBase(final AbstractType other) {
        final Set<AbstractType> common = this.ancestors();
        final Set<AbstractType> ancestors =
            new TypeHierarchy(Objects.requireNonNull(other)).ancestors();
        common.retainAll(ancestors);
        final Set<AbstractType> candidates = new HashSet<>(common);
        for (final AbstractType type : common) {
            for (final AbstractType base : type.getBaseTypes()) {
                candidates.remove(base);
            }
        }
        final Optional<AbstractType> result;
        if (candidates.size() == 1) {
            result = Optional.of(candidates.iterator().next());
        } else {
            result = Optional.empty();
        }
        return result;
    }

    /**
     * Collects ancestors, including the starting type, once per equal type.
     * @return Set of ancestor types
     */
    private Set<AbstractType> ancestors() {
        final Set<AbstractType> result = new HashSet<>();
        final Deque<AbstractType> pending = new ArrayDeque<>();
        pending.add(this.start);
        while (!pending.isEmpty()) {
            final AbstractType current = pending.removeFirst();
            if (result.add(current)) {
                pending.addAll(current.getBaseTypes());
            }
        }
        return result;
    }
}
