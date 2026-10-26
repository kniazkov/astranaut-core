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

import com.kniazkov.astranaut.core.algorithms.Replacement;
import com.kniazkov.astranaut.core.algorithms.RewriteRule;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.Pattern;
import com.kniazkov.astranaut.core.base.Tree;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Adapts mined differential patterns to common occurrence traversal and acceptance.
 * The legacy local matcher retains its child-subsequence semantics.
 * @since 0.0.1
 */
public final class PatternRule implements RewriteRule {
    /**
     * Immutable mined pattern.
     */
    private final Pattern pattern;

    /**
     * Binds a pattern.
     * @param pattern Pattern
     */
    public PatternRule(final Pattern pattern) {
        this.pattern = Objects.requireNonNull(pattern);
    }

    @Override
    public Optional<Replacement> apply(final List<Node> nodes, final int index) {
        final Node node = nodes.get(index);
        return new Matcher(new Tree(node)).matchRoot(this.pattern)
            .flatMap(actions -> actions.applyValidated(node))
            .map(output -> new Replacement(1, output));
    }
}
