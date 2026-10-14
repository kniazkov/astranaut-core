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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Iterative constraint search, stopping as soon as a second allocation is found.
 * Search can be exponential for overlapping schemas; ordinary disjoint schemas
 * and single repeated positions do not branch. No Java recursion is used.
 * @since 0.0.1
 */
final class AllocationSearch {
    /**
     * Schema.
     */
    private final List<ChildDescriptor> schema;

    /**
     * Source occurrences.
     */
    private final List<Node> nodes;

    /**
     * Slot selected for each source occurrence.
     */
    private final int[] selected;

    /**
     * Occupancy of each slot.
     */
    private final int[] counts;

    /**
     * Creates invocation-local search state.
     * @param schema Schema
     * @param nodes Source occurrences
     */
    AllocationSearch(final List<ChildDescriptor> schema, final List<Node> nodes) {
        this.schema = schema;
        this.nodes = nodes;
        this.selected = new int[nodes.size()];
        this.counts = new int[schema.size()];
    }

    /**
     * Finds the unique order-preserving allocation.
     * @return Immutable allocation, or empty
     */
    public Optional<List<List<Node>>> solve() {
        Arrays.fill(this.selected, -1);
        Optional<List<List<Node>>> result = Optional.empty();
        int depth = 0;
        boolean ambiguous = false;
        while (depth >= 0 && !ambiguous) {
            if (depth == this.nodes.size()) {
                if (this.complete()) {
                    ambiguous = result.isPresent();
                    result = Optional.of(this.snapshot());
                }
                depth = depth - 1;
            } else {
                final int previous = this.selected[depth];
                if (previous >= 0) {
                    this.counts[previous] = this.counts[previous] - 1;
                }
                final int next = this.nextSlot(depth, previous + 1);
                this.selected[depth] = next;
                if (next < 0) {
                    depth = depth - 1;
                } else {
                    this.counts[next] = this.counts[next] + 1;
                    depth = depth + 1;
                }
            }
        }
        if (ambiguous) {
            result = Optional.empty();
        }
        return result;
    }

    /**
     * Finds the next compatible position, pruning impossible required counts.
     * @param depth Current source index
     * @param start First candidate
     * @return Position or minus one
     */
    private int nextSlot(final int depth, final int start) {
        int result = -1;
        if (this.missing() <= this.nodes.size() - depth && this.reachable(depth)) {
            for (int slot = start; slot < this.schema.size(); slot = slot + 1) {
                final ChildDescriptor desc = this.schema.get(slot);
                if ((this.counts[slot] == 0 || desc.getCardinality() == Cardinality.REPEATED)
                    && this.nodes.get(depth).isSubtypeOf(desc.getType())
                    && this.ordered(depth, slot, depth)) {
                    result = slot;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Counts unfilled required positions.
     * @return Number of still missing children
     */
    private int missing() {
        int missing = 0;
        for (int slot = 0; slot < this.schema.size(); slot = slot + 1) {
            if (this.schema.get(slot).getCardinality() == Cardinality.REQUIRED
                && this.counts[slot] == 0) {
                missing = missing + 1;
            }
        }
        return missing;
    }

    /**
     * Rejects inversions when both occurrences fit both positions.
     * @param depth Current source index
     * @param slot Proposed position
     * @param prefix Number of already assigned occurrences
     * @return Whether source order is preserved
     */
    private boolean ordered(final int depth, final int slot, final int prefix) {
        boolean result = true;
        if (slot + 1 < this.schema.size()) {
            for (int prev = 0; prev < prefix; prev = prev + 1) {
                final int other = this.selected[prev];
                if (other > slot
                    && this.nodes.get(depth).isSubtypeOf(this.schema.get(other).getType())
                    && this.nodes.get(prev).isSubtypeOf(this.schema.get(slot).getType())) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Checks that every empty required slot remains reachable.
     * @param depth Prefix length
     * @return Whether each required position has a remaining candidate
     */
    private boolean reachable(final int depth) {
        boolean result = true;
        for (int slot = 0; slot < this.schema.size(); slot = slot + 1) {
            if (this.counts[slot] == 0
                && this.schema.get(slot).getCardinality() == Cardinality.REQUIRED) {
                result = this.hasCandidate(depth, slot);
                if (!result) {
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Looks for an unassigned occurrence compatible with a required slot.
     * @param depth Prefix length
     * @param slot Required slot
     * @return Whether reachable
     */
    private boolean hasCandidate(final int depth, final int slot) {
        boolean result = false;
        for (int index = depth; index < this.nodes.size(); index = index + 1) {
            if (this.nodes.get(index).isSubtypeOf(this.schema.get(slot).getType())
                && this.ordered(index, slot, depth)) {
                result = true;
                break;
            }
        }
        return result;
    }

    /**
     * Checks all required positions.
     * @return Whether all required positions are occupied
     */
    private boolean complete() {
        boolean result = true;
        for (int slot = 0; slot < this.schema.size(); slot = slot + 1) {
            if (this.schema.get(slot).getCardinality() == Cardinality.REQUIRED
                && this.counts[slot] == 0) {
                result = false;
                break;
            }
        }
        return result;
    }

    /**
     * Copies a solution, keeping shared nodes as distinct occurrences.
     * @return Immutable per-slot sequences
     */
    private List<List<Node>> snapshot() {
        final List<List<Node>> result = new ArrayList<>(this.schema.size());
        for (int slot = 0; slot < this.schema.size(); slot = slot + 1) {
            result.add(new ArrayList<>(this.counts[slot]));
        }
        for (int index = 0; index < this.nodes.size(); index = index + 1) {
            result.get(this.selected[index]).add(this.nodes.get(index));
        }
        return result.stream().map(List::copyOf).toList();
    }
}
