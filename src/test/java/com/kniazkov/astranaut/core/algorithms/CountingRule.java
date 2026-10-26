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

import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.Node;
import java.util.List;
import java.util.Optional;

/**
 * Deterministic data transition with a finite prefix and optional cycle.
 * @since 0.0.1
 */
final class CountingRule implements RewriteRule {
    /**
     * Last state before stability or cycling.
     */
    private final int limit;

    /**
     * Cycle length, or zero for stability.
     */
    private final int cycle;

    /**
     * Configures the state sequence.
     * @param limit Largest state
     * @param cycle Cycle length
     */
    CountingRule(final int limit, final int cycle) {
        this.limit = limit;
        this.cycle = cycle;
    }

    @Override
    public Optional<Replacement> apply(final List<Node> nodes, final int index) {
        Optional<Replacement> result = Optional.empty();
        final Node node = nodes.get(index);
        if (node.getTypeName().equals("Counter")) {
            final int value = Integer.parseInt(node.getData());
            int next = value + 1;
            if (value == this.limit) {
                next = value - this.cycle + 1;
            }
            if (value < this.limit || this.cycle > 0) {
                final Node output = DraftNode.create("Counter", Integer.toString(next));
                result = Optional.of(new Replacement(1, List.of(output)));
            }
        }
        return result;
    }

    @Override
    public boolean isConcurrent() {
        return true;
    }
}
