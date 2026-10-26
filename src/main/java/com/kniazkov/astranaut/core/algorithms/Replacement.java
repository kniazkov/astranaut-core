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
import java.util.List;
import java.util.Optional;

/**
 * Immutable sequence replacement. Explicit tags describe output occurrences, not nodes.
 * Absent tags preserve positional tags for equal arity; changing tagged arity requires
 * explicit bindings, except deletion. The enclosing builder remains authoritative.
 * @param consumed Positive input length
 * @param nodes Output sequence, possibly empty
 * @param tags Optional explicit output bindings
 * @since 0.0.1
 */
public record Replacement(int consumed, List<Node> nodes, Optional<List<Optional<String>>> tags) {
    /**
     * Validates and snapshots a proposal.
     * @throws IllegalArgumentException If consumption or tag count is invalid
     */
    public Replacement {
        nodes = List.copyOf(nodes);
        tags = tags.map(List::copyOf);
        if (consumed < 1 || tags.isPresent() && tags.orElseThrow().size() != nodes.size()) {
            throw new IllegalArgumentException("Invalid replacement arity");
        }
    }

    /**
     * Uses default positional binding preservation.
     * @param consumed Input length
     * @param nodes Output sequence
     */
    public Replacement(final int consumed, final List<Node> nodes) {
        this(consumed, nodes, Optional.empty());
    }
}
