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
package com.kniazkov.astranaut.core.algorithms.normalization;

import com.kniazkov.astranaut.core.algorithms.ParallelOccurrenceWalker;
import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.NodeOccurrence;
import com.kniazkov.astranaut.core.base.NodeWithStructuralEquality;
import com.kniazkov.astranaut.core.base.Type;
import com.kniazkov.astranaut.core.utils.parsing.StringSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Parallel normalization has deterministic output and isolated state.
 * @since 0.0.1
 */
class ParallelNormalizationTest {
    @Test
    void agreesAcrossPoolSizesOnWideSharedAndDeepBranches() throws Exception {
        Node chain = DraftNode.create("Leaf");
        for (int index = 0; index < 5000; index += 1) {
            chain = DraftNode.create("Branch", "", chain);
        }
        final List<Node> nodes = new ArrayList<>(32);
        nodes.add(chain);
        nodes.addAll(Collections.nCopies(30, DraftNode.create("Shared(Twig(Leaf))")));
        nodes.add(new StringSource("x").parseIntoList().getFirst());
        final DraftNode.Constructor builder = new DraftNode.Constructor();
        builder.setName("Root");
        builder.setChildren(nodes);
        final Node root = builder.build();
        final Normalizer normalizer = new Normalizer();
        final Node sequential = normalizer.normalize(root);
        for (final int count : List.of(1, 2, 4)) {
            try (var pool = Executors.newFixedThreadPool(count)) {
                final Node parallel = normalizer.normalize(
                    root, new ParallelOccurrenceWalker(pool, count, 2)
                );
                Assertions.assertEquals(sequential, parallel);
                Assertions.assertEquals(sequential.hashCode(), parallel.hashCode());
                Assertions.assertSame(
                    nodes.getLast().getFragment(), parallel.getChild(nodes.size() + 1).getFragment()
                );
                Assertions.assertEquals(7, pool.submit(() -> 7).get());
            }
        }
    }

    @Test
    void branchMappingPreservesOrderWhenCompletionOrderIsReversed() {
        final NodeOccurrence root = new NodeOccurrence(DraftNode.create("Root(First,Second)"));
        final CountDownLatch later = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            final ParallelOccurrenceWalker strategy = new ParallelOccurrenceWalker(pool, 2, 1);
            final List<Node> actual = strategy.mapChildren(
                root, occurrence -> {
                    if (occurrence.getIndex() == 0) {
                        this.await(later);
                    } else {
                        later.countDown();
                    }
                    return new Normalizer().normalize(occurrence.getNode());
                }
            );
            Assertions.assertEquals(
                "First", ((NormalizedNode) actual.getFirst()).getOriginalTypeName()
            );
            Assertions.assertEquals(
                "Second", ((NormalizedNode) actual.getLast()).getOriginalTypeName()
            );
        }
    }

    @Test
    void rejectsCyclesOnBothPathsAndKeepsSmallInputsOnCaller() {
        final Node cyclic = new CyclicNode();
        final Normalizer normalizer = new Normalizer();
        Assertions.assertThrows(IllegalArgumentException.class, () -> normalizer.normalize(cyclic));
        try (var pool = Executors.newFixedThreadPool(2)) {
            final ParallelOccurrenceWalker strategy = new ParallelOccurrenceWalker(pool, 2, 1);
            Assertions.assertThrows(
                IllegalArgumentException.class, () -> normalizer.normalize(cyclic, strategy)
            );
            final Thread caller = Thread.currentThread();
            new ParallelOccurrenceWalker(pool, 2, 100).mapChildren(
                new NodeOccurrence(DraftNode.create("Root(A,B)")), occurrence -> {
                    Assertions.assertSame(caller, Thread.currentThread());
                    return occurrence;
                }
            );
        }
    }

    /**
     * Awaits completion of the later branch without an unbounded test hang.
     * @param latch Later branch signal
     */
    private void await(final CountDownLatch latch) {
        try {
            Assertions.assertTrue(latch.await(5, TimeUnit.SECONDS));
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            Assertions.fail(exception);
        }
    }

    /**
     * Deliberately invalid external adapter.
     * @since 0.0.1
     */
    private static final class CyclicNode extends NodeWithStructuralEquality {
        @Override
        public Type getType() {
            return DraftNode.create("Cycle").getType();
        }

        @Override
        public String getData() {
            return "";
        }

        @Override
        public int getChildCount() {
            return 2;
        }

        @Override
        public Node getChild(final int index) {
            return this;
        }
    }
}
