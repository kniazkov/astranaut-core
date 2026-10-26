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

import com.kniazkov.astranaut.core.algorithms.RewriteRule;
import com.kniazkov.astranaut.core.algorithms.RuleEngine;
import com.kniazkov.astranaut.core.base.ConstructionException;
import com.kniazkov.astranaut.core.base.Factory;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.Transformer;
import com.kniazkov.astranaut.core.base.Tree;
import java.util.List;

/**
 * Compatibility facade for generated converters using the common atomic engine.
 * Retains repeated passes until structural stability; cycle diagnostics are a separate
 * stabilization policy. Use RuleEngine directly when exactly one pass is wanted.
 * @since 0.0.1
 */
public class DefaultTransformer implements Transformer {
    /**
     * Shared sequential engine.
     */
    private final RuleEngine engine;

    /**
     * Snapshots converters in declared priority order.
     * @param converters Converters
     * @param factory Output factory
     */
    public DefaultTransformer(final List<Converter> converters, final Factory factory) {
        this.engine = new RuleEngine(
            converters.stream().<RewriteRule>map(converter -> new ConverterRule(converter, factory))
                .toList()
        );
    }

    @Override
    public Tree transform(final Tree original) {
        Tree current = original;
        boolean changed;
        do {
            final Tree next = this.engine.apply(current);
            changed = !current.deepCompare(next);
            current = next;
        } while (changed);
        return current;
    }

    @Override
    public Node transform(final Node original) {
        return this.transform(new Tree(original)).getRoot().orElseThrow(
            () -> new ConstructionException("Node-only transformation cannot return an empty tree")
        );
    }
}
