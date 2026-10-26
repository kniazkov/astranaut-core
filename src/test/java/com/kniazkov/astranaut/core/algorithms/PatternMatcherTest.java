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

import com.kniazkov.astranaut.core.algorithms.conversion.Extracted;
import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.Hole;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.PatternOperator;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Transactional matching contracts and pinned Astranaut DSL semantics.
 * @since 0.0.1
 */
final class PatternMatcherTest {
    @Test
    void failedStartDoesNotPolluteLaterCapture() {
        final Node pattern = PatternMatcherTest.operator(
            PatternOperator.Kind.SEQUENCE,
            new Hole(DraftNode.create("T"), 1), DraftNode.create("Stop")
        );
        final Node root = DraftNode.create("Root(T<'bad'>,T<'good'>,Stop)");
        final PatternMatcher.Match match = new PatternMatcher()
            .find(pattern, root.getChildrenList(), false).orElseThrow();
        Assertions.assertEquals(1, match.start());
        Assertions.assertEquals(3, match.end());
        Assertions.assertEquals(List.of(root.getChild(1)), match.captures().getNodes(1));
    }

    @Test
    void dataAndNodeSlotsAccumulateIndependently() {
        final Node capture = PatternMatcherTest.operator(
            PatternOperator.Kind.AND, new Hole(1),
            new PatternOperator(PatternOperator.Kind.DATA_HOLE, "1", List.of())
        );
        final Node repeated = PatternMatcherTest.operator(PatternOperator.Kind.REPEAT, capture);
        final Node root = DraftNode.create("Root(T<'a'>,T<'b'>,T<'c'>)");
        final Extracted result = new PatternMatcher().find(
            repeated, root.getChildrenList(), false
        ).orElseThrow().captures();
        Assertions.assertEquals("abc", result.getData(1));
        Assertions.assertEquals(root.getChildrenList(), result.getNodes(1));
        Assertions.assertEquals("", result.getData(2));
        Assertions.assertTrue(result.getNodes(2).isEmpty());
        Assertions.assertThrows(
            UnsupportedOperationException.class, () -> result.getNodes(
            1
        ).clear()
        );
    }

    @Test
    void alternativesAndNegationDiscardFailedCaptures() {
        final Node capture = new PatternOperator(PatternOperator.Kind.DATA_HOLE, "1", List.of());
        final Node reject = PatternMatcherTest.operator(
            PatternOperator.Kind.AND, capture,
            new PatternOperator(PatternOperator.Kind.DATA, "no", List.of())
        );
        final Node choose = PatternMatcherTest.operator(PatternOperator.Kind.OR, reject, capture);
        final PatternMatcher matcher = new PatternMatcher();
        final Node node = DraftNode.create("T<'yes'>");
        Assertions.assertEquals("yes", matcher.match(choose, node).orElseThrow().getData(1));
        final Node negate = PatternMatcherTest.operator(PatternOperator.Kind.NOT, reject);
        Assertions.assertEquals("", matcher.match(negate, node).orElseThrow().getData(1));
        Assertions.assertTrue(
            matcher.match(
            PatternMatcherTest.operator(
            PatternOperator.Kind.NOT, capture
        ), node
        ).isEmpty()
        );
    }

    @Test
    void optionalAndRepetitionAreGreedyWithoutBacktracking() {
        final Node leaf = DraftNode.create("T");
        final PatternMatcher matcher = new PatternMatcher();
        for (final PatternOperator.Kind kind : List.of(
            PatternOperator.Kind.OPTIONAL, PatternOperator.Kind.REPEAT
        )) {
            final Node pattern = PatternMatcherTest.operator(
                PatternOperator.Kind.SEQUENCE, PatternMatcherTest.operator(kind, new Hole(0)), leaf
            );
            Assertions.assertTrue(matcher.find(pattern, List.of(leaf), false).isEmpty());
        }
        final Node optional = PatternMatcherTest.operator(
            PatternOperator.Kind.SEQUENCE,
            PatternMatcherTest.operator(
                PatternOperator.Kind.OPTIONAL, DraftNode.create(
                "Other"
            )
            ), leaf
        );
        Assertions.assertTrue(matcher.find(optional, List.of(leaf), false).isPresent());
        final Node empty = PatternMatcherTest.operator(PatternOperator.Kind.SEQUENCE);
        final Node repeat = PatternMatcherTest.operator(PatternOperator.Kind.REPEAT, empty);
        Assertions.assertTrue(matcher.find(repeat, List.of(leaf), false).isEmpty());
    }

    @Test
    void reverseChangesCandidatePriorityNotCaptureOrder() {
        final Node root = DraftNode.create("Root(T<'a'>,Stop,T<'b'>,Stop)");
        final Node pattern = PatternMatcherTest.operator(
            PatternOperator.Kind.SEQUENCE, new Hole(0), new Hole(DraftNode.create("Stop"), 0)
        );
        final PatternMatcher matcher = new PatternMatcher();
        Assertions.assertEquals(
            root.getChildrenList().subList(0, 2), matcher.find(
            pattern, root.getChildrenList(), false
        ).orElseThrow().captures().getNodes(0)
        );
        Assertions.assertEquals(
            root.getChildrenList().subList(2, 4), matcher.find(
            pattern, root.getChildrenList(), true
        ).orElseThrow().captures().getNodes(0)
        );
    }

    @Test
    void childPatternsConsumeTheEntireListAndRangesIncludeBounds() {
        final Node range = new PatternOperator(PatternOperator.Kind.RANGE, "az", List.of());
        final Node pattern = PatternMatcherTest.operator(PatternOperator.Kind.CHILDREN, range);
        final PatternMatcher matcher = new PatternMatcher();
        Assertions.assertTrue(
            matcher.match(
            pattern, DraftNode.create(
            "Root(T<'a'>)"
        )
        ).isPresent()
        );
        Assertions.assertTrue(
            matcher.match(
            pattern, DraftNode.create(
            "Root(T<'z'>)"
        )
        ).isPresent()
        );
        Assertions.assertTrue(matcher.match(pattern, DraftNode.create("Root(T<'A'>)")).isEmpty());
        Assertions.assertTrue(
            matcher.match(pattern, DraftNode.create("Root(T<'a'>,T<'b'>)"))
            .isEmpty()
        );
    }

    @Test
    void parallelAttemptsAndSharedOccurrencesRemainIndependent() {
        final Node pattern = PatternMatcherTest.operator(
            PatternOperator.Kind.SEQUENCE, new Hole(0), new Hole(0)
        );
        final PatternMatcher matcher = new PatternMatcher();
        final Node shared = DraftNode.create("Shared");
        final List<Node> nodes = List.of(shared, shared);
        final List<List<Node>> result = IntStream.range(0, 100).parallel().mapToObj(
            index -> matcher.find(pattern, nodes, false).orElseThrow().captures().getNodes(0)
        ).toList();
        for (final List<Node> capture : result) {
            Assertions.assertEquals(nodes, capture);
            Assertions.assertSame(shared, capture.getFirst());
            Assertions.assertSame(shared, capture.getLast());
        }
    }

    @Test
    void largeRepeatedCaptureUsesIndependentPersistentSnapshots() {
        final Node shared = DraftNode.create("Leaf<'x'>");
        final Node pattern = PatternMatcherTest.operator(
            PatternOperator.Kind.REPEAT, new Hole(0)
        );
        final List<Node> nodes = java.util.Collections.nCopies(30_000, shared);
        final Extracted captures = new PatternMatcher().find(pattern, nodes, false)
            .orElseThrow().captures();
        Assertions.assertEquals(nodes, captures.getNodes(0));
        final Extracted fork = captures.fork();
        fork.addNode(0, DraftNode.create("Other"));
        Assertions.assertEquals(30_000, captures.getNodes(0).size());
        Assertions.assertEquals(30_001, fork.getNodes(0).size());
    }

    /**
     * Creates an operator with no payload.
     * @param kind Operator
     * @param nodes Operands
     * @return Expression
     */
    private static Node operator(final PatternOperator.Kind kind, final Node... nodes) {
        return new PatternOperator(kind, "", List.of(nodes));
    }
}
