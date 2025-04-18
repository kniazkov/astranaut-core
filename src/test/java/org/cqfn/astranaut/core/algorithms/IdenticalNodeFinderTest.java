/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2025 Ivan Kniazkov
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
package org.cqfn.astranaut.core.algorithms;

import java.util.Set;
import org.cqfn.astranaut.core.base.DraftNode;
import org.cqfn.astranaut.core.base.Node;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link IdenticalNodeFinder} class.
 * @since 1.1.5
 */
class IdenticalNodeFinderTest {
    @Test
    void testIdenticalSets() {
        final Node original = DraftNode.create(
            "T<\"a\">(T<\"b\">,T<\"c\">(F<\"a\">,T<\"b\">,T<\"a\">,F<\"a\">))"
        );
        final IdenticalNodeFinder identical = new IdenticalNodeFinder(original);
        final Set<Set<Node>> identicals = identical.find();
        Assertions.assertEquals(3, identicals.size());
    }

    @Test
    void testNoIdenticals() {
        final Node original = DraftNode.create(
            "T<\"a\">(T<\"b\">,T<\"c\">(F<\"a\">,F<\"b\">,F<\"c\">,K<\"a\">))"
        );
        final IdenticalNodeFinder identical = new IdenticalNodeFinder(original);
        final Set<Set<Node>> identicals = identical.find();
        Assertions.assertEquals(0, identicals.size());
    }

    @Test
    void testIdenticalsAndEmptyData() {
        final Node original = DraftNode.create(
            "T<\"a\">(T<\"b\">,T<\"c\">(F<\"a\">,T<\"b\">,T<\"a\">,F,F,T))"
        );
        final IdenticalNodeFinder identical = new IdenticalNodeFinder(original);
        final Set<Set<Node>> identicals = identical.find();
        Assertions.assertEquals(2, identicals.size());
    }
}
