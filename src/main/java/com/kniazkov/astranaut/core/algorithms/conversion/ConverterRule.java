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
package com.kniazkov.astranaut.core.algorithms.conversion;

import com.kniazkov.astranaut.core.algorithms.DepthFirstWalker;
import com.kniazkov.astranaut.core.algorithms.OccurrenceReplacer;
import com.kniazkov.astranaut.core.algorithms.Replacement;
import com.kniazkov.astranaut.core.algorithms.RewriteRule;
import com.kniazkov.astranaut.core.base.ConstructionException;
import com.kniazkov.astranaut.core.base.Factory;
import com.kniazkov.astranaut.core.base.Fragment;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.NullNode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Adapter preserving specialized generated matchers/builders while sharing traversal
 * and atomic acceptance with template rules. Factories are sequential by default.
 * @since 0.0.1
 */
public final class ConverterRule implements RewriteRule {
    /**
     * Generated converter.
     */
    private final Converter converter;

    /**
     * Caller factory.
     */
    private final Factory factory;

    /**
     * Binds converter and factory.
     * @param converter Converter
     * @param factory Factory
     */
    public ConverterRule(final Converter converter, final Factory factory) {
        this.converter = Objects.requireNonNull(converter);
        this.factory = Objects.requireNonNull(factory);
    }

    @Override
    public Optional<Replacement> apply(final List<Node> nodes, final int index) {
        Optional<Replacement> result = Optional.empty();
        if (nodes.size() - index >= this.converter.getMinConsumed()) {
            result = this.converter.convert(nodes, index, this.factory).map(
                converted -> {
                    List<Node> output = List.of(converted.getNode());
                    if (converted.getNode() == NullNode.INSTANCE) {
                        output = List.of();
                    } else {
                        output = List.of(ConverterRule.provenance(nodes, index, converted));
                    }
                    return new Replacement(converted.getConsumed(), output);
                }
            );
        }
        return result;
    }

    @Override
    public boolean isRightToLeft() {
        return this.converter.isRightToLeft();
    }

    /**
     * Gives new output roots the full matched span, without rewriting captured objects.
     * @param nodes Input
     * @param index Start
     * @param result Converter output
     * @return Output node
     * @throws ConstructionException If the output builder rejects provenance reconstruction
     */
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private static Node provenance(final List<Node> nodes, final int index,
        final ConversionResult result) {
        final List<Node> matched = nodes.subList(index, index + result.getConsumed());
        final Node output = result.getNode();
        final boolean captured = matched.stream().anyMatch(
            node -> new DepthFirstWalker(node).findFirst(child -> child == output).isPresent()
        );
        Node rebuilt = output;
        if (!captured) {
            rebuilt = new OccurrenceReplacer().withFragment(output, Fragment.fromNodes(matched))
                .orElseThrow(() -> new ConstructionException("Output builder refused provenance"));
        }
        return rebuilt;
    }
}
