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
import com.kniazkov.astranaut.core.base.Tree;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Pass-boundary determinism and cooperative cancellation, including active workers.
 * @since 0.0.1
 */
@Timeout(15)
final class ParallelStabilizerTest {
    @Test
    void sequentialAndParallelModesAgreeOnStabilityAndCycles() {
        final Tree source = Tree.createDraft("Root(Counter<'0'>,Counter<'1'>,Counter<'2'>)");
        for (final int cycle : List.of(0, 2, 3)) {
            final RewriteRule rule = new CountingRule(5, cycle);
            for (final int size : List.of(1, 2, 4)) {
                try (ExecutorService pool = Executors.newFixedThreadPool(size)) {
                    final RuleStabilizer runner = new RuleStabilizer(
                        new RuleEngine(List.of(rule), new ParallelOccurrenceWalker(pool, size, 1))
                    );
                    if (cycle == 0) {
                        final Tree expected = new RuleStabilizer(new RuleEngine(List.of(rule)))
                            .apply(source);
                        Assertions.assertTrue(expected.deepCompare(runner.apply(source)));
                    } else {
                        final RewriteCycleException failure = Assertions.assertThrows(
                            RewriteCycleException.class, () -> runner.apply(source)
                        );
                        Assertions.assertEquals(BigInteger.valueOf(cycle), failure.getPeriod());
                    }
                    Assertions.assertFalse(pool.isShutdown());
                }
            }
        }
    }

    @Test
    void cancelsNonrepeatingGrowth() throws InterruptedException {
        final CountDownLatch started = new CountDownLatch(50);
        final RewriteRule grow = (nodes, index) -> {
            started.countDown();
            final String data = nodes.get(index).getData().concat("x");
            return Optional.of(new Replacement(1, List.of(DraftNode.create("Growing", data))));
        };
        final RuleStabilizer runner = new RuleStabilizer(new RuleEngine(List.of(grow)));
        try (Execution call = new Execution(runner, Tree.createDraft("Growing"))) {
            Assertions.assertTrue(started.await(5, TimeUnit.SECONDS));
            call.cancel();
            Assertions.assertInstanceOf(CancellationException.class, call.failure.get());
            Assertions.assertTrue(call.interrupted);
        }
    }

    @Test
    void cancellationJoinsAllWorkersAndKeepsExternalPoolOpen() throws InterruptedException {
        final BlockingRule rule = new BlockingRule(false);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            final RuleStabilizer runner = new RuleStabilizer(
                new RuleEngine(List.of(rule), new ParallelOccurrenceWalker(pool, 2, 1))
            );
            try (Execution call = new Execution(runner, Tree.createDraft("Root(A,B)"))) {
                Assertions.assertTrue(rule.started.await(5, TimeUnit.SECONDS));
                call.cancel();
                Assertions.assertInstanceOf(CancellationException.class, call.failure.get());
                Assertions.assertTrue(call.interrupted);
                Assertions.assertEquals(0, rule.active.get());
                Assertions.assertFalse(pool.isShutdown());
            }
        }
    }

    @Test
    void laterWorkerFailureDoesNotWaitForeverForEarlierWorker() throws InterruptedException {
        final BlockingRule rule = new BlockingRule(true);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            final RuleStabilizer runner = new RuleStabilizer(
                new RuleEngine(List.of(rule), new ParallelOccurrenceWalker(pool, 2, 1))
            );
            try (Execution call = new Execution(runner, Tree.createDraft("Root(A,B)"))) {
                call.await();
                Assertions.assertInstanceOf(IllegalArgumentException.class, call.failure.get());
                Assertions.assertEquals(0, rule.active.get());
                Assertions.assertFalse(pool.isShutdown());
            }
        }
    }

    /**
     * Separate coordinator, joined even after failed assertions.
     * @since 0.0.1
     */
    private static final class Execution implements AutoCloseable {
        /**
         * Captured completion failure.
         */
        private final AtomicReference<RuntimeException> failure;

        /**
         * Calling thread.
         */
        private final Thread caller;

        /**
         * Interrupt status at completion.
         */
        private volatile boolean interrupted;

        /**
         * Starts an independent invocation.
         * @param runner Stabilizer
         * @param source Input
         */
        private Execution(final RuleStabilizer runner, final Tree source) {
            this.failure = new AtomicReference<>();
            this.caller = Thread.ofPlatform().start(
                () -> {
                    try {
                        runner.apply(source);
                    } catch (final CancellationException | IllegalArgumentException exception) {
                        this.failure.set(exception);
                    } finally {
                        this.interrupted = Thread.currentThread().isInterrupted();
                    }
                }
            );
        }

        @Override
        public void close() throws InterruptedException {
            this.cancel();
        }

        /**
         * Requests cancellation and joins the coordinator.
         * @throws InterruptedException If the test thread is interrupted
         */
        private void cancel() throws InterruptedException {
            this.caller.interrupt();
            this.await();
        }

        /**
         * Waits for completion, bounded only for the regression test.
         * @throws InterruptedException If the test thread is interrupted
         */
        private void await() throws InterruptedException {
            this.caller.join(5_000);
            Assertions.assertFalse(this.caller.isAlive());
        }
    }

    /**
     * Two cooperative callbacks; optionally the later stripe fails first.
     * @since 0.0.1
     */
    private static final class BlockingRule implements RewriteRule {
        /**
         * Active callback count.
         */
        private final AtomicInteger active;

        /**
         * Barrier for both callbacks.
         */
        private final CountDownLatch started;

        /**
         * Whether the last stripe fails.
         */
        private final boolean failing;

        /**
         * Creates instrumentation.
         * @param failing Failure mode
         */
        private BlockingRule(final boolean failing) {
            this.active = new AtomicInteger();
            this.started = new CountDownLatch(2);
            this.failing = failing;
        }

        @Override
        public Optional<Replacement> apply(final List<Node> nodes, final int index) {
            this.active.incrementAndGet();
            try {
                this.started.countDown();
                this.started.await();
                if (this.failing && index == 1) {
                    throw new IllegalArgumentException("Later stripe failed");
                }
                while (!Thread.currentThread().isInterrupted()) {
                    LockSupport.parkNanos(1_000_000L);
                }
            } catch (final InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                this.active.decrementAndGet();
            }
            return Optional.empty();
        }

        @Override
        public boolean isConcurrent() {
            return true;
        }
    }
}
