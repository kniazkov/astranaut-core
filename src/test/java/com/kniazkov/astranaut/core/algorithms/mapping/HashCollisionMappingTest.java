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
package com.kniazkov.astranaut.core.algorithms.mapping;

import com.kniazkov.astranaut.core.algorithms.ExtNodeCreator;
import com.kniazkov.astranaut.core.algorithms.IdenticalNodeFinder;
import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.Node;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Hashes select candidates, never prove equality in mapping or local grouping.
 * @since 0.0.1
 */
class HashCollisionMappingTest {
    @Test
    void collidingRootsAreNotMappedAsIdenticalOrLocallyEqual() {
        final Node left = DraftNode.create("Literal<\"Aa\">");
        final Node right = DraftNode.create("Literal<\"BB\">");
        Assertions.assertEquals(left.hashCode(), right.hashCode());
        final Mapping mapping = TopDownMapper.INSTANCE.map(left, right);
        Assertions.assertNull(mapping.getRight(left));
        Assertions.assertNull(mapping.getLeft(right));
    }

    @Test
    void pairFinderChecksActualStructureInBothModes() {
        final ExtNodeCreator creator = new ExtNodeCreator();
        final Section section = new Section(
            creator.create(DraftNode.create("Root(Literal<\"Aa\">)")),
            creator.create(DraftNode.create("Root(Literal<\"BB\">)"))
        );
        Assertions.assertEquals(
            0, new NodePairFinder(section, NodePairFinder.ABSOLUTE_HASH)
                .findMatchingSequence().getCount()
        );
        Assertions.assertEquals(
            0, new NodePairFinder(section, NodePairFinder.LOCAL_HASH)
                .findMatchingSequence().getCount()
        );
    }

    @Test
    void localGroupingSeparatesCollisionsAndRetainsEqualObjects() {
        final Node root = DraftNode.create(
            "Root(Literal<\"Aa\">,Literal<\"BB\">,Literal<\"Aa\">,Literal<\"BB\">)"
        );
        final var groups = new IdenticalNodeFinder(root).find();
        Assertions.assertEquals(2, groups.size());
        for (final var group : groups) {
            Assertions.assertEquals(2, group.size());
            Assertions.assertEquals(1, group.stream().map(Node::getData).distinct().count());
        }
    }

    @Test
    void mappingRetainsDistinctEqualSiblings() {
        final Node left = DraftNode.create("Root(Leaf,Leaf,Other)");
        final Node right = DraftNode.create("Root(Leaf,Leaf,Other)");
        final Mapping mapping = TopDownMapper.INSTANCE.map(left, right);
        Assertions.assertSame(right.getChild(0), mapping.getRight(left.getChild(0)));
        Assertions.assertSame(right.getChild(1), mapping.getRight(left.getChild(1)));
        Assertions.assertSame(left.getChild(1), mapping.getLeft(right.getChild(1)));
    }
}
