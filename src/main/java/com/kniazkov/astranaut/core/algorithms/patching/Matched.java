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
package com.kniazkov.astranaut.core.algorithms.patching;

import com.kniazkov.astranaut.core.algorithms.conversion.Extracted;
import com.kniazkov.astranaut.core.base.ActionList;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.NodeOccurrence;
import java.util.List;
import java.util.Optional;

/**
 * Data obtained through the matching process.
 * @since 0.0.1
 */
final class Matched extends ActionList {
    /**
     * Data obtained as a result of hole matching.
     */
    private final Extracted holes = new Extracted();

    /**
     * Occurrence-scoped local commands.
     */
    private final OccurrencePatch patch = new OccurrencePatch();

    /**
     * Partially clones this object. The new instance will contain only information
     * about previously extracted holes, without extracted nodes or data.
     * @return A new Matched object with copied hole data
     */
    Matched fork() {
        final Matched obj = new Matched();
        obj.holes.replaceWith(this.holes);
        return obj;
    }

    /**
     * Combines data obtained through the matching process.
     * @param other Other data
     */
    void merge(final Matched other) {
        super.merge(other);
        this.patch.merge(other.patch);
        this.holes.replaceWith(other.holes);
    }

    /**
     * Accumulates captured nodes; repeated numbers never impose equality.
     * @param number Slot
     * @param node Captured node
     * @return Always true
     */
    boolean checkHole(final int number, final Node node) {
        this.holes.addNode(number, node);
        return true;
    }

    /**
     * Records replacement in both occurrence and legacy reporting forms.
     * @param address Occurrence
     * @param node Output
     */
    void replaceAt(final NodeOccurrence address, final Node node) {
        this.replaceNode(address.getNode(), node);
        this.patch.replace(address, List.of(node));
    }

    /**
     * Records deletion of one occurrence.
     * @param address Occurrence
     */
    void deleteAt(final NodeOccurrence address) {
        this.deleteNode(address.getNode());
        this.patch.replace(address, List.of());
    }

    /**
     * Records an insertion boundary.
     * @param parent Parent
     * @param index Boundary
     * @param node Output
     */
    void insertAt(final NodeOccurrence parent, final int index, final Node node) {
        this.patch.insert(parent, index, node);
    }

    /**
     * Builds the complete local edit atomically.
     * @param root Candidate root
     * @return Output or refusal
     */
    Optional<List<Node>> applyValidated(final Node root) {
        return this.patch.apply(root, this.holes);
    }
}
