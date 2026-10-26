/*
 * Copyright (c) 2026 Ivan Kniazkov
 * SPDX-License-Identifier: MIT
 */
import com.kniazkov.astranaut.core.algorithms.Replacement;
import com.kniazkov.astranaut.core.algorithms.RewriteRule;
import com.kniazkov.astranaut.core.algorithms.RuleEngine;
import com.kniazkov.astranaut.core.algorithms.RuleStabilizer;
import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.Tree;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Manual bounded-heap probe. Compile/run with Java 21 and -Xmx64m. */
class StabilizationBenchmark {
    public static void main(String[] args) {
        run(100_000, 0);
        run(1_000, 10_000);
    }

    private static void run(int passes, int children) {
        Node[] leaves = new Node[children];
        Arrays.fill(leaves, DraftNode.create("Leaf", ""));
        RewriteRule rule = (nodes, index) -> {
            Node node = nodes.get(index);
            if (node.getTypeName().equals("Counter")) {
                int value = Integer.parseInt(node.getData());
                if (value < passes) {
                    Node next = DraftNode.create("Counter", Integer.toString(value + 1), leaves);
                    return Optional.of(new Replacement(1, List.of(next)));
                }
            }
            return Optional.empty();
        };
        long begin = System.nanoTime();
        Tree result = new RuleStabilizer(new RuleEngine(List.of(rule)))
            .apply(new Tree(DraftNode.create("Counter", "0", leaves)));
        if (!result.getRoot().orElseThrow().getData().equals(Integer.toString(passes))) {
            throw new AssertionError("Wrong stable result");
        }
        System.out.printf(java.util.Locale.ROOT, "passes=%d children=%d seconds=%.3f%n",
            passes, children, (System.nanoTime() - begin) / 1_000_000_000.0);
    }
}
