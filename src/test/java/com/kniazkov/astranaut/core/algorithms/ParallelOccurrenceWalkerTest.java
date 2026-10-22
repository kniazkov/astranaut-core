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

import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.NodeOccurrence;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Parallel traversal preserves order, pruning and every shared occurrence.
 * @since 0.0.1
 */
class ParallelOccurrenceWalkerTest {
    @Test
    void agreesWithSequentialTraversalAcrossPoolSizesAndBroadSharing() throws Exception {
        final Node branch = DraftNode.create("Branch(Leaf,Twig(Leaf))");
        final DraftNode.Constructor builder = new DraftNode.Constructor();
        builder.setName("Root");
        builder.setChildren(Collections.nCopies(10_000, branch));
        final NodeOccurrence root = new NodeOccurrence(builder.build());
        final OccurrenceWalker sequential = new OccurrenceWalker(root);
        for (final int workers : List.of(1, 2, 4)) {
            try (var pool = Executors.newFixedThreadPool(workers)) {
                final ParallelOccurrenceWalker parallel = new ParallelOccurrenceWalker(
                    pool, workers, 2
                );
                Assertions.assertEquals(sequential.collectAll(), parallel.collectAll(root));
                Assertions.assertEquals(
                    sequential.findAll(item -> item.getDepth() == 2),
                    parallel.findAll(root, item -> item.getDepth() == 2)
                );
                Assertions.assertEquals(
                    sequential.findFirst(item -> item.getDepth() == 3),
                    parallel.findFirst(root, item -> item.getDepth() == 3)
                );
                Assertions.assertEquals(4, parallel.depth(root));
                Assertions.assertTrue(parallel.findFirst(root, item -> false).isEmpty());
                Assertions.assertEquals(7, pool.submit(() -> 7).get());
            }
        }
    }

    @Test
    void returnsEarlierMatchEvenWhenLaterBranchFinishesFirst() {
        final NodeOccurrence root = new NodeOccurrence(DraftNode.create("Root(First,Second)"));
        final CountDownLatch second = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            final ParallelOccurrenceWalker parallel = new ParallelOccurrenceWalker(pool, 2, 1);
            final NodeOccurrence found = parallel.findFirst(
                root, item -> {
                    if (item.getIndex() == 0) {
                        this.await(second);
                    } else if (item.getIndex() == 1) {
                        second.countDown();
                    }
                    return item.getDepth() > 0;
                }
            ).orElseThrow();
            Assertions.assertEquals(root.getChild(0), found);
        }
    }

    @Test
    void prunesAtRootAndWithinBranchesWithoutConcurrentOptInOnSequentialPath() {
        final NodeOccurrence root = new NodeOccurrence(DraftNode.create("Root(A(X),B(Y))"));
        final AtomicInteger visits = new AtomicInteger();
        try (var pool = Executors.newFixedThreadPool(2)) {
            final ParallelOccurrenceWalker parallel = new ParallelOccurrenceWalker(pool, 2, 1);
            Assertions.assertEquals(
                List.of(root), parallel.findAll(
                    root, item -> {
                        visits.incrementAndGet();
                        return true;
                    }
                )
            );
            Assertions.assertEquals(1, visits.get());
            visits.set(0);
            Assertions.assertEquals(
                List.of(root.getChild(0), root.getChild(1)), parallel.findAll(
                    root, item -> {
                        visits.incrementAndGet();
                        return item.getDepth() == 1;
                    }
                )
            );
            Assertions.assertEquals(3, visits.get());
        }
        final Thread caller = Thread.currentThread();
        new OccurrenceWalker(root).findFirst(
            item -> {
                Assertions.assertSame(caller, Thread.currentThread());
                return false;
            }
        );
    }

    @Test
    void chainsAndSmallTreesStaySequentialAndWorkerErrorsPropagate() {
        Node chain = DraftNode.create("Leaf");
        for (int index = 0; index < 30_000; index += 1) {
            chain = DraftNode.create("Link", "", chain);
        }
        final NodeOccurrence root = new NodeOccurrence(chain);
        final NodeOccurrence broad = new NodeOccurrence(DraftNode.create("Root(A,B)"));
        try (var pool = Executors.newFixedThreadPool(2)) {
            final ParallelOccurrenceWalker parallel = new ParallelOccurrenceWalker(pool, 2, 1);
            final IllegalArgumentException failure = new IllegalArgumentException("predicate");
            Assertions.assertSame(
                failure, Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> parallel.findAll(
                        broad, item -> {
                            if (item.getDepth() > 0) {
                                throw failure;
                            }
                            return false;
                        }
                    )
                )
            );
            pool.shutdown();
            Assertions.assertEquals(30_001, parallel.collectAll(root).size());
            Assertions.assertEquals(30_001, parallel.depth(root));
            Assertions.assertEquals(
                3, new ParallelOccurrenceWalker(pool, 2, 100).collectAll(broad).size()
            );
            Assertions.assertThrows(RejectedExecutionException.class, () -> parallel.depth(broad));
            Assertions.assertThrows(
                IllegalArgumentException.class, () -> new ParallelOccurrenceWalker(pool, 0, 1)
            );
        }
    }

    /**
     * Waits for the other worker without relying on timing-based sleeps.
     * @param latch Worker completion signal
     * @throws IllegalStateException If interrupted
     */
    private void await(final CountDownLatch latch) {
        try {
            Assertions.assertTrue(latch.await(10, TimeUnit.SECONDS));
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
