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
package com.kniazkov.astranaut.core.base;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Deep and shared graph regressions.
 * @since 0.0.1
 */
class DeepNodeStructureTest {
    @Test
    void comparesAndHashesThirtyThousandLevelsWithoutRecursion() {
        final Node first = this.chain(DraftNode.create("Leaf"), 30_000);
        final Node second = this.chain(DraftNode.create("Leaf"), 30_000);
        final Node different = this.chain(DraftNode.create("Other"), 30_000);
        Assertions.assertEquals(first, second);
        Assertions.assertEquals(first.hashCode(), second.hashCode());
        Assertions.assertNotEquals(first, different);
    }

    @Test
    void sharingTopologyDoesNotAffectStructure() {
        final Node branch = DraftNode.create("Branch(Leaf)");
        final Node shared = this.parent(List.of(branch, branch));
        final Node copied = this.parent(
            List.of(DraftNode.create("Branch(Leaf)"), DraftNode.create("Branch(Leaf)"))
        );
        Assertions.assertEquals(shared, copied);
        Assertions.assertEquals(shared.hashCode(), copied.hashCode());
    }

    @Test
    void sharedDiamondsAreNotExpandedExponentially() {
        Node first = DraftNode.create("Leaf");
        Node second = DraftNode.create("Leaf");
        for (int depth = 0; depth < 70; depth += 1) {
            first = this.parent(List.of(first, first));
            second = this.parent(List.of(second, second));
        }
        Assertions.assertEquals(first, second);
        Assertions.assertEquals(first.hashCode(), second.hashCode());
        Assertions.assertEquals(71, NodeStructure.hashes(first).size());
    }

    /**
     * Constructs a chain without parsing or recursive builders.
     * @param leaf Leaf
     * @param depth Depth
     * @return Root
     */
    private Node chain(final Node leaf, final int depth) {
        Node result = leaf;
        for (int index = 0; index < depth; index += 1) {
            result = new StructuralNodeFixture(
                DraftNode.create("Link"), List.of(result), List.of(Optional.of("next"))
            );
        }
        return result;
    }

    /**
     * Creates a binary parent.
     * @param children Children
     * @return Parent
     */
    private Node parent(final List<Node> children) {
        return new StructuralNodeFixture(
            DraftNode.create("Pair"), children, List.of(Optional.of("one"), Optional.of("two"))
        );
    }
}
