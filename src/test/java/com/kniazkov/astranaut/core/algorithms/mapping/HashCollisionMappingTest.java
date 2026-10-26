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

import com.kniazkov.astranaut.core.algorithms.IdenticalNodeFinder;
import com.kniazkov.astranaut.core.base.DiffTree;
import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.Tree;
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
        final DiffTree diff = TopDownMapper.INSTANCE.compare(new Tree(left), new Tree(right));
        Assertions.assertEquals(left, diff.getBefore().getRoot().orElseThrow());
        Assertions.assertEquals(right, diff.getAfter().getRoot().orElseThrow());
    }

    @Test
    void collidingNestedLiteralsAreReplaced() {
        final Tree left = Tree.createDraft("Root(Literal<'Aa'>)");
        final Tree right = Tree.createDraft("Root(Literal<'BB'>)");
        final DiffTree diff = TopDownMapper.INSTANCE.compare(left, right);
        Assertions.assertTrue(left.deepCompare(diff.getBefore()));
        Assertions.assertTrue(right.deepCompare(diff.getAfter()));
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
        final DiffTree diff = TopDownMapper.INSTANCE.compare(new Tree(left), new Tree(right));
        Assertions.assertEquals(3, diff.getAfter().getRoot().orElseThrow().getChildCount());
        Assertions.assertTrue(new Tree(left).deepCompare(diff.getBefore()));
        Assertions.assertTrue(new Tree(right).deepCompare(diff.getAfter()));
    }
}
