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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Explicit parallel operations agree with sequential structural semantics.
 * @since 0.0.1
 */
class ParallelNodeStructureTest {
    @Test
    void matchesSequentialResultsWithDifferentWorkerCountsAndSharing() throws Exception {
        final Node branch = DraftNode.create("Branch(Literal<\"Aa\">,Leaf)");
        final Node shared = this.tree(branch);
        final Node copied = this.tree(null);
        final Node different = this.tree(DraftNode.create("Branch(Literal<\"BB\">,Leaf)"));
        Assertions.assertEquals(shared.hashCode(), different.hashCode());
        for (final int workers : List.of(1, 2, 4)) {
            try (var pool = Executors.newFixedThreadPool(workers)) {
                final ParallelNodeStructure algorithm = new ParallelNodeStructure(pool, workers, 2);
                for (int repeat = 0; repeat < 4; repeat += 1) {
                    Assertions.assertEquals(shared.hashCode(), algorithm.hash(shared));
                    Assertions.assertEquals(copied.hashCode(), algorithm.hash(copied));
                    Assertions.assertTrue(algorithm.equal(shared, copied));
                    Assertions.assertFalse(algorithm.equal(shared, different));
                }
                Assertions.assertEquals(7, pool.submit(() -> 7).get());
            }
        }
    }

    @Test
    void tagDifferencesAndNullAreNotEqual() {
        final Node child = DraftNode.create("Leaf");
        final Node first = new StructuralNodeFixture(
            DraftNode.create("Root"), List.of(child, child),
            List.of(Optional.of("Aa"), Optional.empty())
        );
        final Node second = new StructuralNodeFixture(
            DraftNode.create("Root"), List.of(child, child),
            List.of(Optional.of("BB"), Optional.empty())
        );
        try (var pool = Executors.newFixedThreadPool(2)) {
            final ParallelNodeStructure algorithm = new ParallelNodeStructure(pool, 2, 1);
            Assertions.assertEquals(algorithm.hash(first), algorithm.hash(second));
            Assertions.assertFalse(algorithm.equal(first, second));
            Assertions.assertFalse(algorithm.equal(first, null));
        }
    }

    @Test
    void smallTreesAndChainsDoNotSubmitWork() {
        try (var pool = Executors.newFixedThreadPool(2)) {
            pool.shutdown();
            final Node leaf = DraftNode.create("Leaf");
            final Node chain = DraftNode.create("Chain(Link(Leaf))");
            final Node small = DraftNode.create("Pair(Leaf,Leaf)");
            final ParallelNodeStructure algorithm = new ParallelNodeStructure(pool, 2, 100);
            Assertions.assertEquals(leaf.hashCode(), algorithm.hash(leaf));
            Assertions.assertEquals(chain.hashCode(), algorithm.hash(chain));
            Assertions.assertTrue(algorithm.equal(small, DraftNode.create("Pair(Leaf,Leaf)")));
            Assertions.assertThrows(
                RejectedExecutionException.class,
                () -> new ParallelNodeStructure(pool, 2, 1).hash(small)
            );
            Assertions.assertThrows(
                IllegalArgumentException.class, () -> new ParallelNodeStructure(pool, 0, 1)
            );
            Assertions.assertThrows(
                IllegalArgumentException.class, () -> new ParallelNodeStructure(pool, 1, 0)
            );
        }
    }

    /**
     * Creates wide tagged trees with shared or independently constructed branches.
     * @param shared Shared branch, or null for independent copies
     * @return Root
     */
    private Node tree(final Node shared) {
        final List<Node> children = new ArrayList<>(64);
        final List<Optional<String>> tags = new ArrayList<>(64);
        for (int index = 0; index < 64; index += 1) {
            Node branch = shared;
            if (branch == null) {
                branch = DraftNode.create("Branch(Literal<\"Aa\">,Leaf)");
            }
            children.add(branch);
            tags.add(Optional.of(Integer.toString(index)));
        }
        return new StructuralNodeFixture(DraftNode.create("Root"), children, tags);
    }
}
