/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2024 Ivan Kniazkov
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
package org.cqfn.astranaut.core.algorithms.patching;

import java.util.Set;
import org.cqfn.astranaut.core.base.ActionList;
import org.cqfn.astranaut.core.base.DiffNode;
import org.cqfn.astranaut.core.base.Node;
import org.cqfn.astranaut.core.base.PatternNode;

/**
 * Default algorithm that applies patches, i.e. makes some changes in the syntax tree
 * based on patterns describing such changes. Patterns are differential trees.
 *
 * @since 1.1.5
 */
public final class DefaultPatcher implements Patcher {
    @Override
    public Node patch(final Node source, final PatternNode pattern) {
        final PatternMatcher matcher = new PatternMatcher(source);
        final Set<Node> nodes = matcher.match(pattern);
        final Node result;
        if (nodes.isEmpty()) {
            result = source;
        } else {
            final ActionList actions = matcher.getActionList();
            final DiffNode diff = actions.convertTreeToDifferenceTree(source);
            result = diff.getAfter();
        }
        return result;
    }
}
