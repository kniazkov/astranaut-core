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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Invalid graphs and worker failures cannot masquerade as structural results.
 * @since 0.0.1
 */
class StructuralFailureTest {
    @Test
    void rejectsCyclicHashInput() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CyclicNode().hashCode());
    }

    @Test
    void propagatesWorkerFailuresAndKeepsCallerExecutorOpen() throws Exception {
        final Node failing = new StructuralNodeFixture(
            new CyclicNode(), List.of(new CyclicNode()), List.of(Optional.empty())
        );
        final Node root = new StructuralNodeFixture(
            DraftNode.create("Root"), List.of(failing, failing),
            List.of(Optional.empty(), Optional.empty())
        );
        try (var pool = Executors.newFixedThreadPool(2)) {
            final ParallelNodeStructure algorithm = new ParallelNodeStructure(pool, 2, 1);
            Assertions.assertThrows(IllegalArgumentException.class, () -> algorithm.hash(root));
            Assertions.assertEquals(3, pool.submit(() -> 3).get());
        }
    }

    @Test
    void restoresInterruptStatusAndCancelsQueuedWorkers() {
        final CountDownLatch release = new CountDownLatch(1);
        try (var pool = Executors.newSingleThreadExecutor()) {
            pool.submit(
                () -> {
                    release.await();
                    return 0;
                }
            );
            final ParallelNodeStructure algorithm = new ParallelNodeStructure(pool, 2, 1);
            final Node root = DraftNode.create("Root(Leaf,Leaf)");
            Thread.currentThread().interrupt();
            try {
                Assertions.assertThrows(IllegalStateException.class, () -> algorithm.hash(root));
                Assertions.assertTrue(Thread.currentThread().isInterrupted());
            } finally {
                Thread.interrupted();
                release.countDown();
            }
        }
    }

    /**
     * Deliberately violates the acyclic graph contract.
     * @since 0.0.1
     */
    private static final class CyclicNode extends NodeWithStructuralEquality {
        @Override
        public Type getType() {
            return DummyNode.INSTANCE.getType();
        }

        @Override
        public String getData() {
            return "";
        }

        @Override
        public int getChildCount() {
            return 1;
        }

        @Override
        public Node getChild(final int index) {
            return this;
        }
    }
}
