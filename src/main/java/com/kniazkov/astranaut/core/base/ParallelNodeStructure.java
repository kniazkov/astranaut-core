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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntConsumer;

/**
 * Explicit bounded parallel strategy for large independent root branches.
 * The caller owns the executor, which is never closed. Invoke from outside that
 * executor to avoid starvation on a saturated pool. Workers never submit nested
 * tasks. Small trees and chains use sequential operations. Nodes and callbacks
 * must permit concurrent reads and remain unchanged throughout the operation.
 * @since 0.0.1
 */
public final class ParallelNodeStructure {
    /**
     * Caller-owned pool.
     */
    private final ExecutorService executor;

    /**
     * Maximum tasks per invocation.
     */
    private final int parallelism;

    /**
     * Minimum reachable object count for splitting work.
     */
    private final int threshold;

    /**
     * Creates an explicit parallel strategy.
     * @param executor Caller-owned bounded executor
     * @param parallelism Maximum number of worker tasks
     * @param threshold Minimum object count to parallelize
     * @throws IllegalArgumentException If either numeric argument is not positive
     */
    @SuppressWarnings("PMD.ConstructorOnlyInitializesOrCallOtherConstructors")
    public ParallelNodeStructure(final ExecutorService executor, final int parallelism,
        final int threshold) {
        this.executor = Objects.requireNonNull(executor);
        if (parallelism < 1 || threshold < 1) {
            throw new IllegalArgumentException("Parallelism and threshold must be positive");
        }
        this.parallelism = parallelism;
        this.threshold = threshold;
    }

    /**
     * Calculates the same hash as NodeStructure.hash.
     * @param root Root
     * @return Structural hash
     */
    public int hash(final Node root) {
        final int result;
        if (this.shouldSplit(root)) {
            final int[] hashes = new int[root.getChildCount()];
            this.run(root.getChildCount(), worker -> this.hashBranches(root, hashes, worker));
            int value = NodeStructure.startHash(root);
            for (int index = 0; index < hashes.length; index += 1) {
                value = 31 * value + NodeStructure.tagHash(root, index);
                value = 31 * value + hashes[index];
            }
            result = value;
        } else {
            result = NodeStructure.hash(root);
        }
        return result;
    }

    /**
     * Compares independent branches, stopping further work after a mismatch.
     * @param left Left root
     * @param right Right root, possibly null
     * @return Whether structures match
     */
    public boolean equal(final Node left, final Node right) {
        Objects.requireNonNull(left);
        boolean result = right != null && NodeStructure.sameHeader(left, right);
        if (result && this.shouldSplit(left)) {
            final AtomicBoolean matches = new AtomicBoolean(true);
            this.run(
                left.getChildCount(), worker -> {
                    final int stride = Math.min(this.parallelism, left.getChildCount());
                    for (int index = worker; matches.get() && index < left.getChildCount();
                        index += stride) {
                        if (!NodeStructure.equal(left.getChild(index), right.getChild(index))) {
                            matches.set(false);
                        }
                    }
                }
            );
            result = matches.get();
        } else if (result) {
            result = NodeStructure.equal(left, right);
        }
        return result;
    }

    /**
     * Hashes a disjoint stripe of children with task-local identity memoization.
     * @param root Parent
     * @param hashes Output indexed by occurrence
     * @param worker Stripe index
     */
    @SuppressWarnings("PMD.UseVarargs")
    private void hashBranches(final Node root, final int[] hashes, final int worker) {
        final Map<Node, Integer> cache = new IdentityHashMap<>();
        final int stride = Math.min(this.parallelism, hashes.length);
        for (int index = worker; index < hashes.length; index += stride) {
            final Node child = root.getChild(index);
            if (!cache.containsKey(child)) {
                cache.putAll(NodeStructure.hashes(child));
            }
            hashes[index] = cache.get(child);
        }
    }

    /**
     * Uses a bounded probe; no scheduling occurs for chains or small inputs.
     * @param root Root
     * @return Whether splitting is worthwhile under caller policy
     */
    private boolean shouldSplit(final Node root) {
        Objects.requireNonNull(root);
        final Set<Node> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        final Deque<Node> pending = new ArrayDeque<>();
        if (this.parallelism > 1 && root.getChildCount() > 1) {
            pending.push(root);
        }
        while (!pending.isEmpty() && seen.size() < this.threshold) {
            final Node node = pending.pop();
            if (seen.add(node)) {
                node.forEachChild(pending::push);
            }
        }
        return seen.size() >= this.threshold;
    }

    /**
     * Runs bounded tasks and joins them before returning any result.
     * Worker runtime failures retain their original exception and stack trace.
     * @param count Number of independent child branches
     * @param action Worker action
     * @throws IllegalStateException If interrupted or a worker fails
     */
    @SuppressWarnings("PMD.PreserveStackTrace")
    private void run(final int count, final IntConsumer action) {
        final List<Future<?>> futures = new ArrayList<>(Math.min(this.parallelism, count));
        try {
            for (int worker = 0; worker < Math.min(this.parallelism, count); worker += 1) {
                final int stripe = worker;
                futures.add(this.executor.submit(() -> action.accept(stripe)));
            }
            for (final Future<?> future : futures) {
                future.get();
            }
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Structural operation interrupted", exception);
        } catch (final ExecutionException exception) {
            throw ParallelNodeStructure.propagate(exception.getCause());
        } finally {
            for (final Future<?> future : futures) {
                future.cancel(true);
            }
        }
    }

    /**
     * Preserves unexpected runtime failures from caller-provided nodes.
     * @param cause Worker failure
     * @return Runtime failure to throw
     */
    private static RuntimeException propagate(final Throwable cause) {
        final RuntimeException result;
        if (cause instanceof Error error) {
            throw error;
        } else if (cause instanceof RuntimeException exception) {
            result = exception;
        } else {
            result = new IllegalStateException("Structural worker failed", cause);
        }
        return result;
    }
}
