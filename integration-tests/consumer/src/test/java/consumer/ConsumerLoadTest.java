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
package consumer;

import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.NodeStructure;
import com.kniazkov.astranaut.core.base.Tree;
import com.kniazkov.astranaut.core.algorithms.ParallelOccurrenceWalker;
import com.kniazkov.astranaut.core.algorithms.RewriteRule;
import com.kniazkov.astranaut.core.algorithms.RuleEngine;
import com.kniazkov.astranaut.core.algorithms.normalization.Normalizer;
import com.kniazkov.astranaut.core.utils.JsonDeserializer;
import com.kniazkov.astranaut.core.utils.JsonSerializer;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Bounded-heap correctness and observational load probes.
 * @since 0.0.1
 */
class ConsumerLoadTest {
    @Test void deepAndWideJsonAndManyUnmatchedRules() {
        for (Node root : List.of(chain(20_000), parent(Collections.nCopies(20_000, Adapter.leaf("x"))))) {
            String encoded = new JsonSerializer(new Tree(root)).serialize();
            Tree decoded = new JsonDeserializer(encoded, name -> Optional.empty()).convert();
            assertTrue(NodeStructure.equal(root, decoded.getRoot().orElseThrow()));
            assertEquals(NodeStructure.hash(root), NodeStructure.hash(decoded.getRoot().orElseThrow()));
        }
        List<RewriteRule> rules = new ArrayList<>();
        for (int index = 0; index < 512; index++) {
            rules.add((nodes, start) -> Optional.empty());
        }
        Node root = parent(Collections.nCopies(1000, Adapter.leaf("unchanged")));
        assertSame(root, new RuleEngine(rules).apply(new Tree(root)).getRoot().orElseThrow());
    }

    @Test void measuresEquivalentNormalizationAcrossShapesAndPools() throws Exception {
        Map<String, Node> shapes = new LinkedHashMap<>();
        shapes.put("small", parent(List.of(Adapter.leaf("small"))));
        shapes.put("wide-shared", parent(Collections.nCopies(20_000, Adapter.leaf("wide"))));
        shapes.put("deep", chain(20_000));
        List<Node> branches = new ArrayList<>();
        for (int index = 0; index < 8; index++) { branches.add(chain(2500)); }
        shapes.put("branches", parent(branches));
        List<String> report = new ArrayList<>(List.of(
            "shape,pool,sample,nanoseconds,peak_heap_bytes,submitted_tasks"));
        Path directory = Path.of(System.getProperty("consumer.reports"));
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("environment.txt"),
            "java=" + System.getProperty("java.version") + "\nos=" + System.getProperty("os.name")
            + "\nprocessors=" + Runtime.getRuntime().availableProcessors()
            + "\nmaxHeap=" + Runtime.getRuntime().maxMemory() + "\n");
        try {
            for (var shape : shapes.entrySet()) {
                Node expected = new Normalizer().normalize(shape.getValue());
                for (int size : new int[] {0, 1, 2, 4}) {
                    try (CountingPool pool = new CountingPool(Math.max(1, size))) {
                        ParallelOccurrenceWalker strategy = new ParallelOccurrenceWalker(pool,
                            Math.max(1, size), 1024);
                        for (int sample = -1; sample < 3; sample++) {
                            pool.submitted.set(0);
                            ManagementFactory.getMemoryPoolMXBeans().forEach(bean -> bean.resetPeakUsage());
                            long start = System.nanoTime();
                            Node result = size == 0 ? new Normalizer().normalize(shape.getValue())
                                : new Normalizer().normalize(shape.getValue(), strategy);
                            long elapsed = System.nanoTime() - start;
                            long peak = ManagementFactory.getMemoryPoolMXBeans().stream()
                                .filter(bean -> bean.getType() == MemoryType.HEAP)
                                .mapToLong(bean -> bean.getPeakUsage().getUsed()).sum();
                            assertEquals(expected, result);
                            assertEquals(expected.hashCode(), result.hashCode());
                            assertFalse(pool.isShutdown());
                            if (sample >= 0) {
                                report.add(String.format(Locale.ROOT, "%s,%d,%d,%d,%d,%d",
                                    shape.getKey(), size, sample, elapsed, peak, pool.submitted.get()));
                            }
                        }
                    }
                }
            }
        } finally {
            Files.write(directory.resolve("normalization.csv"), report);
        }
    }

    private static Node chain(int count) {
        Node node = Adapter.leaf("end");
        for (int index = 0; index < count; index++) { node = parent(List.of(node)); }
        return node;
    }

    private static Node parent(List<Node> children) {
        DraftNode.Constructor builder = new DraftNode.Constructor();
        builder.setName("Container");
        assertTrue(builder.setChildren(children));
        return builder.build();
    }

    /** Measures operation-owned task submissions.
     * @since 0.0.1
     */
    private static final class CountingPool extends ThreadPoolExecutor {
        private final AtomicInteger submitted = new AtomicInteger();
        CountingPool(int size) {
            super(size, size, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        }
        @Override public void execute(Runnable command) {
            this.submitted.incrementAndGet();
            super.execute(command);
        }
    }
}
