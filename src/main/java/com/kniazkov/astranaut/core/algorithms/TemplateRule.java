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

import com.kniazkov.astranaut.core.base.Fragment;
import com.kniazkov.astranaut.core.base.Node;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Bridges immutable matching and output templates to the common rule engine.
 * Concurrent use remains opt-in because output descriptors can contain user factories.
 * @since 0.0.1
 */
public final class TemplateRule implements RewriteRule {
    /**
     * Matching expression.
     */
    private final Node pattern;

    /**
     * Output expression.
     */
    private final Node template;

    /**
     * Scan direction.
     */
    private final boolean reverse;

    /**
     * Creates a left-to-right rule.
     * @param pattern Match
     * @param template Output
     */
    public TemplateRule(final Node pattern, final Node template) {
        this(pattern, template, false);
    }

    /**
     * Creates a rule with explicit direction.
     * @param pattern Match
     * @param template Output
     * @param reverse Right-to-left scan
     */
    public TemplateRule(final Node pattern, final Node template, final boolean reverse) {
        this.pattern = Objects.requireNonNull(pattern);
        this.template = Objects.requireNonNull(template);
        this.reverse = reverse;
    }

    @Override
    public Optional<Replacement> apply(final List<Node> nodes, final int index) {
        return new PatternMatcher().matchAt(this.pattern, nodes, index).flatMap(
            match -> new TemplateSubstitutor(
                Fragment.fromNodes(nodes.subList(match.start(), match.end()))
            ).substituteSequence(this.template, match.captures())
                .map(result -> new Replacement(match.end() - match.start(), result))
        );
    }

    @Override
    public boolean isRightToLeft() {
        return this.reverse;
    }
}
