/*
 * Copyright (c) 2026 Ivan Kniazkov
 * SPDX-License-Identifier: MIT
 */
import com.kniazkov.astranaut.core.algorithms.DiffStrategy;
import com.kniazkov.astranaut.core.algorithms.ParallelOccurrenceWalker;
import com.kniazkov.astranaut.core.algorithms.PositionalDiff;
import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.Tree;
import java.util.Arrays;
import java.util.concurrent.Executors;

/** Manual diff plus both projections, including hash computation, under a bounded heap. */
class DiffBenchmark {
    public static void main(String[] args) {
        try (var pool = Executors.newFixedThreadPool(4)) {
            DiffStrategy sequential = new PositionalDiff();
            DiffStrategy parallel = new PositionalDiff(new ParallelOccurrenceWalker(pool, 4, 1024));
            System.out.println("occurrences,sequential_ms,parallel_ms");
            for (int depth : new int[] {16, 128, 1024}) {
                Tree before = tree(depth, "Aa");
                Tree after = tree(depth, "Different");
                for (int warmup = 0; warmup < 4; warmup++) {
                    measure(sequential, before, after);
                    measure(parallel, before, after);
                }
                double[] first = new double[5];
                double[] second = new double[5];
                for (int sample = 0; sample < first.length; sample++) {
                    first[sample] = measure(sequential, before, after);
                    second[sample] = measure(parallel, before, after);
                }
                Arrays.sort(first);
                Arrays.sort(second);
                System.out.printf(java.util.Locale.ROOT, "%d,%.3f,%.3f%n",
                    1 + 8 * (depth + 1), first[2], second[2]);
            }
        }
    }

    private static Tree tree(int depth, String value) {
        Node[] branches = new Node[8];
        for (int index = 0; index < branches.length; index++) {
            Node node = DraftNode.create("Leaf", value);
            for (int level = 0; level < depth; level++) {
                node = DraftNode.create("Branch", "", node);
            }
            branches[index] = node;
        }
        return new Tree(DraftNode.create("Root", "", branches));
    }

    private static double measure(DiffStrategy strategy, Tree before, Tree after) {
        long start = System.nanoTime();
        var diff = strategy.compare(before, after);
        if (!before.deepCompare(diff.getBefore()) || !after.deepCompare(diff.getAfter())) {
            throw new AssertionError("Invalid projection");
        }
        return (System.nanoTime() - start) / 1_000_000.0;
    }
}
