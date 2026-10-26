/*
 * Copyright (c) 2026 Ivan Kniazkov
 * SPDX-License-Identifier: MIT
 */
import com.kniazkov.astranaut.core.algorithms.ParallelOccurrenceWalker;
import com.kniazkov.astranaut.core.algorithms.normalization.Normalizer;
import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.Node;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/** Manual timing probe, deliberately outside CI assertions. Run with Java 21. */
class NormalizationBenchmark {
    private static volatile Node result;

    public static void main(String[] args) {
        try (var pool = Executors.newFixedThreadPool(4)) {
            var parallel = new ParallelOccurrenceWalker(pool, 4, 1);
            var normalizer = new Normalizer();
            System.out.println("nodes,sequential_ms,parallel_ms");
            for (int count : new int[] {128, 1024, 8192, 32768, 65536}) {
                Node root = tree(count);
                Supplier<Node> sequential = () -> normalizer.normalize(root);
                Supplier<Node> concurrent = () -> normalizer.normalize(root, parallel);
                for (int warmup = 0; warmup < 6; warmup++) {
                    result = sequential.get();
                    result = concurrent.get();
                }
                double[] serial = new double[9];
                double[] split = new double[9];
                for (int sample = 0; sample < serial.length; sample++) {
                    serial[sample] = measure(sequential);
                    split[sample] = measure(concurrent);
                }
                Arrays.sort(serial);
                Arrays.sort(split);
                System.out.printf(java.util.Locale.ROOT, "%d,%.3f,%.3f%n",
                    count + 1, serial[4], split[4]);
            }
        }
    }

    private static double measure(Supplier<Node> operation) {
        long begin = System.nanoTime();
        result = operation.get();
        return (System.nanoTime() - begin) / 1_000_000.0;
    }

    private static Node tree(int count) {
        List<Node> branches = new ArrayList<>(32);
        for (int branch = 0; branch < 32; branch++) {
            Node node = DraftNode.create("Leaf", Integer.toString(branch));
            for (int depth = 1; depth < count / 32; depth++) {
                node = DraftNode.create("Branch", "", node);
            }
            branches.add(node);
        }
        var builder = new DraftNode.Constructor();
        builder.setName("Root");
        builder.setChildren(branches);
        return builder.build();
    }
}
