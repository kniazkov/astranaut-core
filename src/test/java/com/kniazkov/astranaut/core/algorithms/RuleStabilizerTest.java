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

import com.kniazkov.astranaut.core.algorithms.conversion.ConversionResult;
import com.kniazkov.astranaut.core.algorithms.conversion.Converter;
import com.kniazkov.astranaut.core.algorithms.conversion.DefaultTransformer;
import com.kniazkov.astranaut.core.base.Builder;
import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.EmptyTree;
import com.kniazkov.astranaut.core.base.Factory;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.Tree;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Complete-pass stability, exact cycle detection and bounded history.
 * @since 0.0.1
 */
final class RuleStabilizerTest {
    @Test
    void noOpAndHundredsOfInapplicableRulesAreNormal() {
        final Node node = DraftNode.create("Unchanged");
        final RewriteRule noop = new TemplateRule(node, node);
        final RuleStabilizer runner = new RuleStabilizer(
            new RuleEngine(Collections.nCopies(300, noop))
        );
        Assertions.assertTrue(new Tree(node).deepCompare(runner.apply(new Tree(node))));
        Assertions.assertSame(EmptyTree.INSTANCE, runner.apply(EmptyTree.INSTANCE));
        final RuleStabilizer absent = new RuleStabilizer(
            new RuleEngine(Collections.nCopies(300, (nodes, index) -> Optional.empty()))
        );
        Assertions.assertTrue(new Tree(node).deepCompare(absent.apply(new Tree(node))));
    }

    @Test
    void detectsTwoStateCycleAndLongPrefixBeforeOddCycle() {
        for (final int length : List.of(2, 3, 7)) {
            final RuleStabilizer runner = new RuleStabilizer(
                new RuleEngine(List.of(new CountingRule(37, length)))
            );
            final RewriteCycleException failure = Assertions.assertThrows(
                RewriteCycleException.class,
                () -> runner.apply(Tree.createDraft("Counter<'0'>"))
            );
            Assertions.assertEquals(BigInteger.valueOf(length), failure.getPeriod());
            Assertions.assertTrue(failure.getCheckpointPass().intValueExact() >= 38 - length);
            Assertions.assertTrue(failure.getMessage().contains("period"));
        }
    }

    @Test
    void converterFacadeDiagnosesCyclesInsteadOfLooping() {
        final Converter converter = new Converter() {
            @Override
            public Optional<ConversionResult> convert(final List<Node> nodes, final int index,
                final Factory factory) {
                return new CountingRule(1, 2).apply(nodes, index)
                    .map(replacement -> new ConversionResult(replacement.nodes().getFirst(), 1));
            }

            @Override
            public int getMinConsumed() {
                return 1;
            }
        };
        final DefaultTransformer transformer = new DefaultTransformer(
            List.of(converter), name -> DraftNode.create("Unused").getType()
        );
        final Node input = DraftNode.create("Counter", "0");
        Assertions.assertThrows(RewriteCycleException.class, () -> transformer.transform(input));
        Assertions.assertEquals("0", input.getData());
    }

    @Test
    void equalHashesDoNotMeanEqualStates() {
        final Node first = DraftNode.create("Aa");
        final Node second = DraftNode.create("BB");
        Assertions.assertEquals(first.hashCode(), second.hashCode());
        final RuleStabilizer runner = new RuleStabilizer(
            new RuleEngine(List.of(new TemplateRule(first, second)))
        );
        Assertions.assertEquals(second, runner.apply(new Tree(first)).getRoot().orElseThrow());
    }

    @Test
    @Timeout(30)
    void thirtyThousandPassesHaveNoArbitraryLimit() {
        final RuleStabilizer runner = new RuleStabilizer(
            new RuleEngine(List.of(new CountingRule(30_000, 0)))
        );
        final Tree result = runner.apply(Tree.createDraft("Counter<'0'>"));
        Assertions.assertEquals("30000", result.getRoot().orElseThrow().getData());
    }

    @Test
    void largeSharedTreeAndDeepTreeStabilizeWithoutRecursion() {
        final Node leaf = DraftNode.create("Leaf");
        final Node wide = DraftNode.create(
            "Root", "", Collections.nCopies(20_000, leaf).toArray(Node[]::new)
        );
        Node deep = leaf;
        for (int level = 0; level < 20_000; level += 1) {
            deep = DraftNode.create("Branch", "", deep);
        }
        final RuleStabilizer runner = new RuleStabilizer(new RuleEngine(List.of()));
        Assertions.assertTrue(new Tree(wide).deepCompare(runner.apply(new Tree(wide))));
        Assertions.assertTrue(new Tree(deep).deepCompare(runner.apply(new Tree(deep))));
    }

    @Test
    void detectsTagOnlyCycle() {
        final Builder initial = OptionalPair.TYPE.createBuilder();
        Assertions.assertTrue(initial.setChild("left", DraftNode.create("Item")));
        final RewriteRule rule = (nodes, index) -> {
            final Node node = nodes.get(index);
            Optional<Replacement> result = Optional.empty();
            if (node.getType().equals(OptionalPair.TYPE)) {
                String tag = "left";
                if (node.getChild(tag).isPresent()) {
                    tag = "right";
                }
                final Builder builder = OptionalPair.TYPE.createBuilder();
                Assertions.assertTrue(builder.setChild(tag, node.getChild(0)));
                result = Optional.of(new Replacement(1, List.of(builder.build())));
            }
            return result;
        };
        final RuleStabilizer runner = new RuleStabilizer(new RuleEngine(List.of(rule)));
        final RewriteCycleException failure = Assertions.assertThrows(
            RewriteCycleException.class, () -> runner.apply(new Tree(initial.build()))
        );
        Assertions.assertEquals(BigInteger.TWO, failure.getPeriod());
    }

    @Test
    void deletionStabilizesAtEmptyRoot() {
        final RewriteRule delete = (nodes, index) -> Optional.of(new Replacement(1, List.of()));
        final RuleStabilizer runner = new RuleStabilizer(new RuleEngine(List.of(delete)));
        Assertions.assertTrue(runner.apply(Tree.createDraft("Gone")).getRoot().isEmpty());
    }

    @Test
    void preexistingInterruptionCancelsEvenEmptyInputAndPreservesFlag() {
        final RuleStabilizer runner = new RuleStabilizer(new RuleEngine(List.of()));
        Thread.currentThread().interrupt();
        try {
            Assertions.assertThrows(
                CancellationException.class, () -> runner.apply(EmptyTree.INSTANCE)
            );
            Assertions.assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
