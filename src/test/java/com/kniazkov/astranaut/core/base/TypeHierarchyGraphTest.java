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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Exhaustive comparison with a matrix-based oracle for all ordered five-type DAGs.
 * Every DAG admits a topological ordering, so every five-type shape is represented.
 * @since 0.0.1
 */
class TypeHierarchyGraphTest {
    /**
     * Number of vertices in each generated graph.
     */
    private static final int SIZE = 5;

    @Test
    void agreesWithIndependentOracleForEveryPairInEveryGraph() {
        final int edges = TypeHierarchyGraphTest.SIZE * (TypeHierarchyGraphTest.SIZE - 1) / 2;
        for (int mask = 0; mask < (1 << edges); mask = mask + 1) {
            final boolean[][] reachable = this.edges(mask);
            final List<AbstractType> types = this.types(reachable);
            this.closeTransitively(reachable);
            this.checkPairs(types, reachable, mask);
        }
    }

    /**
     * Checks every ordered pair against the independent oracle.
     * @param types Generated types
     * @param reachable Reflexive transitive closure
     * @param mask Graph identifier for failure diagnostics
     */
    private void checkPairs(final List<AbstractType> types, final boolean[][] reachable,
        final int mask) {
        for (int left = 0; left < types.size(); left = left + 1) {
            for (int right = 0; right < types.size(); right = right + 1) {
                final String context = String.format("graph=%d, pair=%d,%d", mask, left, right);
                Assertions.assertEquals(
                    reachable[left][right],
                    types.get(left).isSubtypeOf(types.get(right)),
                    context
                );
                Assertions.assertEquals(
                    this.commonBase(reachable, left, right).map(types::get),
                    types.get(left).findCommonBaseType(types.get(right)),
                    context
                );
            }
        }
    }

    /**
     * Decodes all possible edges towards an earlier vertex.
     * @param mask Edge bit mask
     * @return Adjacency matrix
     */
    private boolean[][] edges(final int mask) {
        final boolean[][] result =
            new boolean[TypeHierarchyGraphTest.SIZE][TypeHierarchyGraphTest.SIZE];
        int bit = 0;
        for (int child = 0; child < result.length; child = child + 1) {
            for (int base = 0; base < child; base = base + 1) {
                result[child][base] = (mask & (1 << bit)) != 0;
                bit = bit + 1;
            }
        }
        return result;
    }

    /**
     * Creates immutable descriptors from direct edges, without adding transitive edges.
     * @param edges Adjacency matrix
     * @return Types in topological order
     */
    @SuppressWarnings("PMD.UseVarargs")
    private List<AbstractType> types(final boolean[][] edges) {
        final List<AbstractType> result = new ArrayList<>(edges.length);
        for (int child = 0; child < edges.length; child = child + 1) {
            final Set<AbstractType> bases = new HashSet<>();
            for (int base = 0; base < child; base = base + 1) {
                if (edges[child][base]) {
                    bases.add(result.get(base));
                }
            }
            result.add(new SimpleAbstractType(String.format("Type%d", child), bases, Map.of()));
        }
        return result;
    }

    /**
     * Computes reflexive transitive closure with the Floyd-Warshall algorithm.
     * This oracle does not traverse descriptors or use production hierarchy queries.
     * The three nested loops directly express the closure recurrence.
     * @param reachable Matrix updated in place
     * @checkstyle NestedForDepth (16 lines)
     */
    @SuppressWarnings("PMD.UseVarargs")
    private void closeTransitively(final boolean[][] reachable) {
        for (int index = 0; index < reachable.length; index = index + 1) {
            reachable[index][index] = true;
        }
        for (int via = 0; via < reachable.length; via = via + 1) {
            for (int from = 0; from < reachable.length; from = from + 1) {
                for (int target = 0; target < reachable.length; target = target + 1) {
                    reachable[from][target] = reachable[from][target]
                        || reachable[from][via] && reachable[via][target];
                }
            }
        }
    }

    /**
     * Finds the common base below every other common base using the reachability matrix.
     * @param reachable Reflexive transitive closure
     * @param left First operand
     * @param right Second operand
     * @return Index of the unique most specific common base, or empty
     */
    private Optional<Integer> commonBase(final boolean[][] reachable, final int left,
        final int right) {
        Optional<Integer> result = Optional.empty();
        for (int candidate = 0; candidate < reachable.length; candidate = candidate + 1) {
            if (reachable[left][candidate] && reachable[right][candidate]) {
                boolean least = true;
                for (int other = 0; other < reachable.length; other = other + 1) {
                    if (reachable[left][other] && reachable[right][other]
                        && !reachable[candidate][other]) {
                        least = false;
                    }
                }
                if (least) {
                    result = Optional.of(candidate);
                }
            }
        }
        return result;
    }
}
