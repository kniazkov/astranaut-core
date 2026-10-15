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

import com.kniazkov.astranaut.core.base.Cardinality;
import com.kniazkov.astranaut.core.base.ChildDescriptor;
import com.kniazkov.astranaut.core.base.Node;
import java.util.List;
import java.util.Optional;

/**
 * Allocates child occurrences to a schema, rejecting ambiguous allocations.
 * Interchangeable occurrences retain their source order. Each invocation owns its
 * search state; neither nodes nor caller output are changed on rejection.
 * @since 0.0.1
 */
public final class NodeAllocator {
    /**
     * Immutable schema.
     */
    private final List<ChildDescriptor> descriptors;

    /**
     * Creates an allocator.
     * @param descriptors Validated schema
     */
    public NodeAllocator(final List<ChildDescriptor> descriptors) {
        this.descriptors = List.copyOf(descriptors);
    }

    /**
     * Allocates into single-valued schema positions.
     * @param destination Output array, modified only on success
     * @param source Source occurrences
     * @return Whether there is exactly one allocation
     * @throws IllegalArgumentException If output is too small or schema has repeated positions
     */
    public boolean allocate(final Node[] destination, final List<Node> source) {
        if (destination.length < this.descriptors.size()
            || this.descriptors.stream()
                .anyMatch(item -> item.getCardinality() == Cardinality.REPEATED)) {
            throw new IllegalArgumentException("Array allocation requires single-valued slots");
        }
        final Optional<List<List<Node>>> result = this.allocate(source);
        if (result.isPresent()) {
            final List<List<Node>> slots = result.orElseThrow();
            for (int index = 0; index < slots.size(); index = index + 1) {
                destination[index] = slots.get(index).stream().findFirst().orElse(null);
            }
        }
        return result.isPresent();
    }

    /**
     * Allocates occurrences, including repeated positions.
     * @param source Source occurrences, without nulls
     * @return Immutable per-position lists, or empty on failure or ambiguity
     */
    public Optional<List<List<Node>>> allocate(final List<Node> source) {
        return new AllocationSearch(this.descriptors, List.copyOf(source)).solve();
    }
}
