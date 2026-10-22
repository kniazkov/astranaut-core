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
import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.NodeStructure;
import com.kniazkov.astranaut.core.base.NodeWithStructuralEquality;
import com.kniazkov.astranaut.core.base.ParallelNodeStructure;
import com.kniazkov.astranaut.core.base.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/** Standalone diagnostic benchmark, not a CI timing assertion. */
public final class StructuralBenchmark {
    private static volatile Object sink;

    public static void main(final String[] args) {
        System.out.printf("Java %s; processors %d%n", System.getProperty("java.version"),
            Runtime.getRuntime().availableProcessors());
        System.out.println("shape,mode,hash_us,equality_us");
        run("tiny-3", new BenchNode(List.of(new BenchNode(List.of()), new BenchNode(List.of()))),
            new BenchNode(List.of(new BenchNode(List.of()), new BenchNode(List.of()))), 1000);
        run("chain-30001", chain(30000), chain(30000), 1);
        run("branches-131009", branches(64, 10), branches(64, 10), 1);
    }

    private static void run(final String shape, final Node left, final Node right, final int batch) {
        if (!NodeStructure.equal(left, right)) {
            throw new AssertionError("Benchmark inputs must be structurally equal");
        }
        System.out.printf(java.util.Locale.ROOT, "%s,sequential,%.3f,%.3f%n", shape,
            measure(() -> NodeStructure.hash(left), batch),
            measure(() -> NodeStructure.equal(left, right), batch));
        for (final int workers : List.of(1, 2, 4)) {
            try (var executor = Executors.newFixedThreadPool(workers)) {
                final var algorithm = new ParallelNodeStructure(executor, workers, 5000);
                if (algorithm.hash(left) != left.hashCode() || !algorithm.equal(left, right)) {
                    throw new AssertionError("Parallel result differs");
                }
                System.out.printf(java.util.Locale.ROOT, "%s,workers-%d,%.3f,%.3f%n", shape,
                    workers, measure(() -> algorithm.hash(left), batch),
                    measure(() -> algorithm.equal(left, right), batch));
            }
        }
    }

    private static double measure(final Supplier<Object> operation, final int batch) {
        for (int warm = 0; warm < 6; warm++) {
            for (int count = 0; count < batch; count++) {
                sink = operation.get();
            }
        }
        final long[] times = new long[9];
        for (int trial = 0; trial < times.length; trial++) {
            final long start = System.nanoTime();
            for (int count = 0; count < batch; count++) {
                sink = operation.get();
            }
            times[trial] = System.nanoTime() - start;
        }
        Arrays.sort(times);
        return times[times.length / 2] / (1000.0 * batch);
    }

    private static Node chain(final int depth) {
        Node node = new BenchNode(List.of());
        for (int index = 0; index < depth; index++) {
            node = new BenchNode(List.of(node));
        }
        return node;
    }

    private static Node branches(final int count, final int depth) {
        final List<Node> children = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            children.add(binary(depth));
        }
        return new BenchNode(children);
    }

    private static Node binary(final int depth) {
        if (depth == 0) {
            return new BenchNode(List.of());
        }
        return new BenchNode(List.of(binary(depth - 1), binary(depth - 1)));
    }

    private static final class BenchNode extends NodeWithStructuralEquality {
        private static final Type TYPE = DraftNode.create("Benchmark").getType();
        private final List<Node> children;

        private BenchNode(final List<Node> children) {
            this.children = List.copyOf(children);
        }

        @Override
        public Type getType() {
            return TYPE;
        }

        @Override
        public String getData() {
            return "";
        }

        @Override
        public int getChildCount() {
            return this.children.size();
        }

        @Override
        public Node getChild(final int index) {
            return this.children.get(index);
        }
    }
}
