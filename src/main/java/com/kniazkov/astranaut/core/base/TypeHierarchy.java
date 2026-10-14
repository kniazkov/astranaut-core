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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
     * Tests reachability, stopping at the first matching name.
     * @param target Requested type name
     * @return Whether reachable
     */
    boolean contains(final String target) {
        final String name = Objects.requireNonNull(target);
        final Deque<AbstractType> pending = new ArrayDeque<>();
        final Set<String> visited = new HashSet<>();
        pending.add(this.start);
        boolean found = false;
        while (!pending.isEmpty()) {
            final AbstractType current = pending.removeFirst();
            if (name.equals(current.getName())) {
                found = true;
                break;
            }
            if (visited.add(current.getName())) {
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
        final Map<String, AbstractType> common = this.ancestors();
        final Map<String, AbstractType> ancestors =
            new TypeHierarchy(Objects.requireNonNull(other)).ancestors();
        common.keySet().retainAll(ancestors.keySet());
        final Set<String> candidates = new HashSet<>(common.keySet());
        for (final AbstractType type : common.values()) {
            for (final AbstractType base : type.getBaseTypes()) {
                candidates.remove(base.getName());
            }
        }
        final Optional<AbstractType> result;
        if (candidates.size() == 1) {
            result = Optional.of(common.get(candidates.iterator().next()));
        } else {
            result = Optional.empty();
        }
        return result;
    }

    /**
     * Collects ancestors, including the starting type, once per name.
     * @return Ancestors keyed by unique name
     */
    private Map<String, AbstractType> ancestors() {
        final Map<String, AbstractType> result = new HashMap<>();
        final Deque<AbstractType> pending = new ArrayDeque<>();
        pending.add(this.start);
        while (!pending.isEmpty()) {
            final AbstractType current = pending.removeFirst();
            if (result.putIfAbsent(current.getName(), current) == null) {
                pending.addAll(current.getBaseTypes());
            }
        }
        return result;
    }
}
