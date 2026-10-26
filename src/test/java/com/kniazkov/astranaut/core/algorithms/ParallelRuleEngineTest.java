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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Deterministic speculative rule evaluation and operation-owned task cancellation.
 * @since 0.0.1
 */
final class ParallelRuleEngineTest {
    @Test
    void poolSizeAndCompletionOrderDoNotChangeResultsOrReuseStaleProposals() {
        final Tree source = Tree.createDraft("Root(A,A,A,A,A,A)");
        final RewriteRule rule = new ContextRule();
        final Tree expected = new RuleEngine(List.of(rule)).apply(source);
        Assertions.assertTrue(
            expected.deepCompare(
            Tree.createDraft("Root(B<'0'>,B<'1'>,B<'2'>,B<'3'>,B<'4'>,B<'5'>)")
        )
        );
        for (final int size : List.of(1, 2, 4)) {
            try (ExecutorService pool = Executors.newFixedThreadPool(size)) {
                final RuleEngine engine = new RuleEngine(
                    List.of(rule), new ParallelOccurrenceWalker(pool, size, 1)
                );
                Assertions.assertTrue(expected.deepCompare(engine.apply(source)));
                Assertions.assertFalse(pool.isShutdown());
            }
        }
    }

    @Test
    void reversePriorityAndParentRefusalRemainDeterministic() {
        final Tree source = new Tree(
            new GuardedNode(
                DraftNode.create("Root(A,A,A,A)"),
                nodes -> nodes.get(1).getTypeName().equals("A")
            )
        );
        final RewriteRule rule = new ContextRule(true);
        final Tree expected = new RuleEngine(List.of(rule)).apply(source);
        Assertions.assertEquals("A", expected.getRoot().orElseThrow().getChild(1).getTypeName());
        for (final int size : List.of(1, 2, 4)) {
            try (ExecutorService pool = Executors.newFixedThreadPool(size)) {
                final RuleEngine engine = new RuleEngine(
                    List.of(rule), new ParallelOccurrenceWalker(pool, size, 1)
                );
                Assertions.assertTrue(expected.deepCompare(engine.apply(source)));
            }
        }
    }

    @Test
    void unsafeRulesAndFactoriesStayOnTheCallingThread() {
        final Thread caller = Thread.currentThread();
        final RewriteRule rule = (nodes, index) -> {
            Assertions.assertSame(caller, Thread.currentThread());
            return Optional.empty();
        };
        try (ExecutorService pool = Executors.newFixedThreadPool(3)) {
            new RuleEngine(List.of(rule), new ParallelOccurrenceWalker(pool, 3, 1))
                .apply(Tree.createDraft("Root(A,B,C)"));
        }
    }

    @Test
    @Timeout(10)
    void unexpectedWorkerFailureCancelsAndJoinsOwnedCallbacks() {
        final AtomicInteger active = new AtomicInteger();
        final CountDownLatch started = new CountDownLatch(2);
        final RewriteRule rule = new FailingRule(active, started);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            final RuleEngine engine = new RuleEngine(
                List.of(rule), new ParallelOccurrenceWalker(pool, 2, 1)
            );
            Assertions.assertThrows(
                IllegalStateException.class,
                () -> engine.apply(Tree.createDraft("Root(A,B)"))
            );
            Assertions.assertEquals(0, active.get());
            Assertions.assertFalse(pool.isShutdown());
        }
    }

    /**
     * Pure rule whose result depends on neighboring accepted changes.
     * @since 0.0.1
     */
    private static final class ContextRule implements RewriteRule {
        /**
         * Candidate direction.
         */
        private final boolean reverse;

        /**
         * Creates a forward rule.
         */
        private ContextRule() {
            this(false);
        }

        /**
         * Creates a rule.
         * @param reverse Direction
         */
        private ContextRule(final boolean reverse) {
            this.reverse = reverse;
        }

        @Override
        public boolean isRightToLeft() {
            return this.reverse;
        }

        @Override
        public Optional<Replacement> apply(final List<Node> nodes, final int index) {
            LockSupport.parkNanos((nodes.size() - index) * 100_000L);
            Optional<Replacement> result = Optional.empty();
            if (nodes.get(index).getTypeName().equals("A")) {
                final long count = nodes.stream().filter(node -> node.getTypeName().equals("B"))
                    .count();
                result = Optional.of(
                    new Replacement(
                    1, List.of(
                    DraftNode.create("B", Long.toString(count))
                )
                )
                );
            }
            return result;
        }

        @Override
        public boolean isConcurrent() {
            return true;
        }
    }

    /**
     * Instrumented cooperative callbacks exercising failure cleanup.
     * @since 0.0.1
     */
    private static final class FailingRule implements RewriteRule {
        /**
         * Running callback count.
         */
        private final AtomicInteger active;

        /**
         * Synchronizes the failure with a running sibling.
         */
        private final CountDownLatch started;

        /**
         * Creates instrumentation.
         * @param active Counter
         * @param started Barrier
         */
        private FailingRule(final AtomicInteger active, final CountDownLatch started) {
            this.active = active;
            this.started = started;
        }

        @Override
        public Optional<Replacement> apply(final List<Node> nodes, final int index) {
            this.active.incrementAndGet();
            try {
                this.started.countDown();
                this.started.await();
                if (index == 0) {
                    throw new IllegalStateException("Unexpected rule error");
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
