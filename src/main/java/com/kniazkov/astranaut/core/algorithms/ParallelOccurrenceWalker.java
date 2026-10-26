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

import com.kniazkov.astranaut.core.base.NodeOccurrence;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Explicit parallel strategy for independent root branches. The caller owns the pool
 * and must invoke operations from outside it to avoid pool starvation. No nested tasks
 * are submitted. Predicates must be thread-safe and independent of invocation order.
 * Speculative search may visit later branches even after an earlier match exists;
 * the returned first match still follows sequential preorder. Ordinary OccurrenceWalker
 * remains sequential for callbacks with ordered side effects. Trees must be acyclic.
 * @since 0.0.1
 */
public final class ParallelOccurrenceWalker {
    /**
     * Caller-owned bounded pool.
     */
    private final ExecutorService executor;

    /**
     * Maximum number of submitted tasks per call.
     */
    private final int parallelism;

    /**
     * Minimum occurrence count to split branches.
     */
    private final int threshold;

    /**
     * Configures an explicit parallel strategy.
     * @param executor Caller-owned pool
     * @param parallelism Maximum submitted worker count
     * @param threshold Minimum occurrence count
     * @throws IllegalArgumentException If a numeric parameter is not positive
     */
    @SuppressWarnings("PMD.ConstructorOnlyInitializesOrCallOtherConstructors")
    public ParallelOccurrenceWalker(final ExecutorService executor, final int parallelism,
        final int threshold) {
        this.executor = Objects.requireNonNull(executor);
        if (parallelism < 1 || threshold < 1) {
            throw new IllegalArgumentException("Parallelism and threshold must be positive");
        }
        this.parallelism = parallelism;
        this.threshold = threshold;
    }

    /**
     * Finds the first match in tree order, independently of worker completion order.
     * @param root Starting occurrence
     * @param visitor Explicitly concurrent predicate
     * @return First match
     */
    public Optional<NodeOccurrence> findFirst(final NodeOccurrence root,
        final Predicate<NodeOccurrence> visitor) {
        final Optional<NodeOccurrence> result;
        final boolean split = this.shouldSplit(root);
        if (split && visitor.test(root)) {
            result = Optional.of(root);
        } else if (split) {
            result = this.map(root, child -> new OccurrenceWalker(child).findFirst(visitor))
                .stream().flatMap(Optional::stream).findFirst();
        } else {
            result = new OccurrenceWalker(root).findFirst(visitor);
        }
        return result;
    }

    /**
     * Finds matches in preorder, pruning below matching occurrences.
     * @param root Starting occurrence
     * @param visitor Explicitly concurrent predicate
     * @return Immutable ordered matches
     */
    public List<NodeOccurrence> findAll(final NodeOccurrence root,
        final Predicate<NodeOccurrence> visitor) {
        final List<NodeOccurrence> result;
        final boolean split = this.shouldSplit(root);
        if (split && visitor.test(root)) {
            result = List.of(root);
        } else if (split) {
            result = this.map(root, child -> new OccurrenceWalker(child).findAll(visitor))
                .stream().flatMap(List::stream).toList();
        } else {
            result = new OccurrenceWalker(root).findAll(visitor);
        }
        return result;
    }

    /**
     * Collects every occurrence in preorder, retaining repeated shared objects.
     * @param root Starting occurrence
     * @return Immutable ordered addresses
     */
    public List<NodeOccurrence> collectAll(final NodeOccurrence root) {
        final List<NodeOccurrence> result;
        if (this.shouldSplit(root)) {
            final List<NodeOccurrence> collected = new ArrayList<>(1);
            collected.add(root);
            for (final List<NodeOccurrence> branch : this.map(
                root, child -> new OccurrenceWalker(child).collectAll()
            )) {
                collected.addAll(branch);
            }
            result = List.copyOf(collected);
        } else {
            result = new OccurrenceWalker(root).collectAll();
        }
        return result;
    }

    /**
     * Calculates depth with task-local structural memoization, counting a leaf as one.
     * @param root Starting occurrence
     * @return Maximum root-to-leaf node count
     */
    public int depth(final NodeOccurrence root) {
        final int result;
        if (this.shouldSplit(root)) {
            result = 1 + this.map(root, child -> new DepthCalculator().calculate(child.getNode()))
                .stream().mapToInt(Integer::intValue).max().orElse(0);
        } else {
            result = new DepthCalculator().calculate(root.getNode());
        }
        return result;
    }

    /**
     * Maps direct branches in child order, using this strategy's threshold and task bound.
     * Actions must be independent and safe for concurrent calls. Small inputs run on the caller.
     * @param root Parent occurrence
     * @param action Branch action
     * @param <T> Result type
     * @return Immutable ordered branch results
     */
    public <T> List<T> mapChildren(final NodeOccurrence root,
        final Function<NodeOccurrence, T> action) {
        Objects.requireNonNull(action);
        final List<T> result;
        if (this.shouldSplit(root)) {
            result = this.map(root, action);
        } else {
            final List<T> values = new ArrayList<>(root.getNode().getChildCount());
            for (int index = 0; index < root.getNode().getChildCount(); index += 1) {
                Interruption.check();
                values.add(action.apply(root.getChild(index)));
            }
            result = List.copyOf(values);
        }
        return result;
    }

    /**
     * Probes at most threshold occurrences without invoking user callbacks.
     * @param root Root
     * @return Whether splitting is useful under the explicit caller policy
     */
    private boolean shouldSplit(final NodeOccurrence root) {
        Objects.requireNonNull(root);
        Interruption.check();
        boolean result = false;
        if (this.parallelism > 1 && root.getNode().getChildCount() > 1) {
            final int[] count = {0};
            result = new OccurrenceWalker(root).findFirst(
                occurrence -> {
                    Interruption.check();
                    count[0] += 1;
                    return count[0] >= this.threshold;
                }
            ).isPresent();
        }
        return result;
    }

    /**
     * Maps branches in bounded stripes and collects results in child order.
     * @param root Parent occurrence
     * @param action Operation on each independent branch
     * @param <T> Branch result type
     * @return Ordered results
     * @throws CancellationException If interrupted
     * @throws IllegalStateException If a checked worker failure occurs
     */
    @SuppressWarnings("PMD.PreserveStackTrace")
    private <T> List<T> map(final NodeOccurrence root, final Function<NodeOccurrence, T> action) {
        final int count = root.getNode().getChildCount();
        final int workers = Math.min(this.parallelism, count);
        final AtomicReferenceArray<T> results = new AtomicReferenceArray<>(count);
        final List<Future<?>> futures = new ArrayList<>(workers);
        final TaskGroup group = new TaskGroup();
        final CompletionService<Boolean> completed = new ExecutorCompletionService<>(this.executor);
        try {
            for (int worker = 0; worker < workers; worker += 1) {
                final int stripe = worker;
                futures.add(
                    completed.submit(
                        () -> {
                            if (group.enter()) {
                                try {
                                    for (int index = stripe; index < count; index += workers) {
                                        Interruption.check();
                                        results.set(index, action.apply(root.getChild(index)));
                                    }
                                } finally {
                                    group.leave();
                                }
                            }
                            return true;
                        }
                    )
                );
            }
            for (int worker = 0; worker < workers; worker += 1) {
                completed.take().get();
            }
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            final CancellationException cancelled =
                new CancellationException("Occurrence operation interrupted");
            cancelled.initCause(exception);
            throw cancelled;
        } catch (final ExecutionException exception) {
            throw ParallelOccurrenceWalker.propagate(exception.getCause());
        } finally {
            group.close();
            for (final Future<?> future : futures) {
                future.cancel(true);
            }
            group.await();
        }
        final List<T> ordered = new ArrayList<>(count);
        for (int index = 0; index < count; index += 1) {
            ordered.add(results.get(index));
        }
        return List.copyOf(ordered);
    }

    /**
     * Preserves unexpected failures from predicates and node implementations.
     * @param cause Worker failure
     * @return Failure to propagate
     */
    private static RuntimeException propagate(final Throwable cause) {
        final RuntimeException result;
        if (cause instanceof Error error) {
            throw error;
        } else if (cause instanceof RuntimeException exception) {
            result = exception;
        } else {
            result = new IllegalStateException("Occurrence worker failed", cause);
        }
        return result;
    }

    /**
     * Tracks running callbacks separately from Future cancellation. Closing the gate
     * prevents queued callbacks from starting; await joins already running callbacks.
     * Callbacks must cooperate with interruption to allow cancellation to complete.
     * @since 0.0.1
     */
    private static final class TaskGroup {
        /**
         * Private lifecycle monitor.
         */
        private final Object monitor = new Object();

        /**
         * Running callbacks.
         */
        private int active;

        /**
         * Closed start gate.
         */
        private boolean closed;

        /**
         * Enters unless cancellation closed the gate.
         * @return Whether the callback may run
         */
        private boolean enter() {
            synchronized (this.monitor) {
                final boolean accepted = !this.closed;
                if (accepted) {
                    this.active += 1;
                }
                return accepted;
            }
        }

        /**
         * Completes a running callback.
         */
        private void leave() {
            synchronized (this.monitor) {
                this.active -= 1;
                this.monitor.notifyAll();
            }
        }

        /**
         * Prevents later starts before futures are cancelled.
         */
        private void close() {
            synchronized (this.monitor) {
                this.closed = true;
            }
        }

        /**
         * Waits for operation-owned callbacks, preserving caller interruption.
         */
        private void await() {
            synchronized (this.monitor) {
                boolean interrupted = false;
                while (this.active > 0) {
                    try {
                        this.monitor.wait();
                    } catch (final InterruptedException ignored) {
                        interrupted = true;
                    }
                }
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
