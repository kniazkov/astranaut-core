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
import com.kniazkov.astranaut.core.algorithms.conversion.ConverterRule;
import com.kniazkov.astranaut.core.algorithms.conversion.DefaultTransformer;
import com.kniazkov.astranaut.core.algorithms.patching.DefaultPatcher;
import com.kniazkov.astranaut.core.base.ConstructionException;
import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.EmptyTree;
import com.kniazkov.astranaut.core.base.Factory;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.Pattern;
import com.kniazkov.astranaut.core.base.Replace;
import com.kniazkov.astranaut.core.base.Tree;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Atomic bottom-up rewriting contracts, including independent consumer builders.
 * @since 0.0.1
 */
final class RuleEngineTest {
    /**
     * Root type used by consumer fixtures.
     */
    private static final String ROOT_NAME = "Root";

    @Test
    void nestedChangesReachTheRootAcrossAllAdapters() {
        final Tree source = Tree.createDraft("Root<'root'>(Inner<'inner'>(A))");
        final Node before = DraftNode.create("A");
        final Node after = DraftNode.create("B");
        final RewriteRule template = new TemplateRule(before, after);
        final Converter converter = new LetterConverter();
        final Factory factory = name -> DraftNode.create("Unused").getType();
        final Tree expected = Tree.createDraft("Root<'root'>(Inner<'inner'>(B))");
        for (final Tree result : List.of(
            new RuleEngine(List.of(template)).apply(source),
            new RuleEngine(List.of(new ConverterRule(converter, factory))).apply(source),
            new DefaultTransformer(List.of(converter), factory).transform(source),
            DefaultPatcher.INSTANCE.patch(source, new Pattern(new Replace(before, after)))
        )) {
            Assertions.assertTrue(expected.deepCompare(result));
        }
        Assertions.assertEquals(
            "A", source.getRoot().orElseThrow().getChild(0).getChild(0)
            .getTypeName()
        );
    }

    @Test
    void rejectedAncestorRollsBackLocalRuleThenTriesTheNextRule() {
        final Node original = new GuardedNode(
            DraftNode.create("Root(Inner(A),Free(A))"),
            nodes -> !nodes.getFirst().getChild(0).getTypeName().equals("B")
        );
        final RuleEngine engine = new RuleEngine(
            List.of(
                new TemplateRule(DraftNode.create("A"), DraftNode.create("B")),
                new TemplateRule(DraftNode.create("A"), DraftNode.create("C"))
        )
        );
        final Node result = engine.apply(new Tree(original)).getRoot().orElseThrow();
        Assertions.assertEquals(DraftNode.create("Root(Inner(C),Free(B))"), result);
        Assertions.assertEquals(DraftNode.create("Root(Inner(A),Free(A))"), original);
    }

    @Test
    void independentSuccessSurvivesLaterRefusal() {
        final Node original = new GuardedNode(
            DraftNode.create("Root(Free(A),Inner(A))"),
            nodes -> !nodes.getLast().getChild(0).getTypeName().equals("B")
        );
        final Node result = new RuleEngine(
            List.of(
                new TemplateRule(DraftNode.create("A"), DraftNode.create("B"))
        )
        ).apply(new Tree(original)).getRoot().orElseThrow();
        Assertions.assertEquals(DraftNode.create("Root(Free(B),Inner(A))"), result);
        Assertions.assertSame(original.getChild(1), result.getChild(1));
    }

    @Test
    void sharedObjectsAreDifferentOccurrencesAndParentRulesSeeChangedChildren() {
        final Node shared = DraftNode.create("A");
        final Node original = DraftNode.create(RuleEngineTest.ROOT_NAME, "", shared, shared);
        final RewriteRule occurrences = (nodes, index) -> {
            Optional<Replacement> result = Optional.empty();
            if (nodes.get(index).getTypeName().equals("A")) {
                result = Optional.of(
                    new Replacement(
                    1, List.of(
                    DraftNode.create(
                    "B", Integer.toString(index)
                )
                )
                )
                );
            }
            return result;
        };
        final Node result = new RuleEngine(List.of(occurrences)).apply(new Tree(original))
            .getRoot().orElseThrow();
        Assertions.assertEquals("0", result.getChild(0).getData());
        Assertions.assertEquals("1", result.getChild(1).getData());
        final Tree collapsed = new RuleEngine(
            List.of(
                new TemplateRule(DraftNode.create("A"), DraftNode.create("B")),
                new TemplateRule(DraftNode.create("Root(B,B)"), DraftNode.create("Done"))
        )
        ).apply(new Tree(original));
        Assertions.assertEquals(DraftNode.create("Done"), collapsed.getRoot().orElseThrow());
    }

    @Test
    void emptyAndMultipleRootsAndNoOpRulesAreHandled() {
        final RewriteRule erase = (nodes, index) -> Optional.of(new Replacement(1, List.of()));
        Assertions.assertTrue(
            new RuleEngine(List.of(erase)).apply(Tree.createDraft("A"))
            .getRoot().isEmpty()
        );
        final RewriteRule multiply = (nodes, index) -> Optional.of(
            new Replacement(1, List.of(DraftNode.create("B"), DraftNode.create("C")))
        );
        Assertions.assertTrue(
            new RuleEngine(List.of(multiply)).apply(Tree.createDraft("A"))
            .deepCompare(Tree.createDraft("A"))
        );
        final RewriteRule fail = (nodes, index) -> {
            throw new AssertionError("Empty input must not invoke a rule");
        };
        Assertions.assertSame(
            EmptyTree.INSTANCE, new RuleEngine(
            List.of(
            fail
        )
        ).apply(
            EmptyTree.INSTANCE
        )
        );
        final RewriteRule noop = (nodes, index) -> Optional.of(
            new Replacement(1, List.of(nodes.get(index)))
        );
        Assertions.assertTrue(
            new RuleEngine(
            List.of(
            noop, new TemplateRule(DraftNode.create("A"), DraftNode.create("B"))
        )
        ).apply(Tree.createDraft("A")).deepCompare(Tree.createDraft("B"))
        );
    }

    @Test
    @SuppressWarnings("PMD.AvoidThrowingNullPointerException")
    void expectedFailureIsNormalButProgrammingErrorsPropagate() {
        final RewriteRule refused = (nodes, index) -> {
            throw new ConstructionException("Expected");
        };
        final RewriteRule unexpected = (nodes, index) -> {
            throw new NullPointerException("Bug");
        };
        Assertions.assertTrue(
            new RuleEngine(
            List.of(
            refused, new TemplateRule(DraftNode.create("A"), DraftNode.create("B"))
        )
        ).apply(Tree.createDraft("A")).deepCompare(Tree.createDraft("B"))
        );
        Assertions.assertThrows(
            NullPointerException.class, () -> new RuleEngine(
            List.of(
            unexpected
        )
        )
            .apply(Tree.createDraft("A"))
        );
        final RewriteRule invalid = (nodes, index) -> Optional.of(new Replacement(2, List.of()));
        Assertions.assertThrows(
            IllegalArgumentException.class, () -> new RuleEngine(
            List.of(
            invalid
        )
        )
            .apply(Tree.createDraft("A"))
        );
    }

    @Test
    void deepChainIsRebuiltWithoutTheJavaCallStack() {
        Node root = DraftNode.create("A");
        for (int index = 0; index < 20_000; index += 1) {
            root = DraftNode.create("Link", "keep", root);
        }
        final Tree result = new RuleEngine(
            List.of(
                new TemplateRule(DraftNode.create("A"), DraftNode.create("B"))
        )
        ).apply(new Tree(root));
        Node leaf = result.getRoot().orElseThrow();
        for (int index = 0; index < 20_000; index += 1) {
            Assertions.assertEquals("keep", leaf.getData());
            leaf = leaf.getChild(0);
        }
        Assertions.assertEquals("B", leaf.getTypeName());
    }

    @Test
    void deepStructuralPatternMatchingDoesNotRecurse() {
        Node pattern = new com.kniazkov.astranaut.core.base.Hole(0);
        Node node = DraftNode.create("Leaf");
        for (int index = 0; index < 20_000; index += 1) {
            pattern = DraftNode.create("Link", "", pattern);
            node = DraftNode.create("Link", "", node);
        }
        final List<Node> captured = new PatternMatcher().match(pattern, node).orElseThrow()
            .getNodes(0);
        Assertions.assertEquals(List.of(DraftNode.create("Leaf")), captured);
    }

    @Test
    void minedPatchDistinguishesSharedOccurrencesWithinOneMatch() {
        final Node shared = DraftNode.create("A");
        final Tree source = new Tree(
            DraftNode.create(
            RuleEngineTest.ROOT_NAME, "", shared, shared
        )
        );
        final Pattern pattern = new Pattern(
            DraftNode.create(
            RuleEngineTest.ROOT_NAME, "", new Replace(shared, DraftNode.create("B")),
                new com.kniazkov.astranaut.core.base.Delete(shared)
        )
        );
        final Tree result = DefaultPatcher.INSTANCE.patch(source, pattern);
        Assertions.assertTrue(result.deepCompare(Tree.createDraft("Root(B)")));
        Assertions.assertEquals(2, source.getRoot().orElseThrow().getChildCount());
    }

    @Test
    void minedPatchCanReuseCapturedNodesInOutput() {
        final Node captured = DraftNode.create("A<'value'>");
        final Node replaced = DraftNode.create("B");
        final Tree source = new Tree(
            DraftNode.create(
            RuleEngineTest.ROOT_NAME, "", captured, replaced
        )
        );
        final Pattern pattern = new Pattern(
            DraftNode.create(
                RuleEngineTest.ROOT_NAME, "", new com.kniazkov.astranaut.core.base.Hole(
                    captured, 0
                ),
                new Replace(replaced, new com.kniazkov.astranaut.core.base.Hole(0))
            )
        );
        final Node result = DefaultPatcher.INSTANCE.patch(source, pattern).getRoot().orElseThrow();
        Assertions.assertSame(captured, result.getChild(0));
        Assertions.assertSame(captured, result.getChild(1));
    }

    /**
     * Minimal generated-style converter.
     * @since 0.0.1
     */
    private static final class LetterConverter implements Converter {
        @Override
        public int getMinConsumed() {
            return 1;
        }

        @Override
        public Optional<ConversionResult> convert(
            final List<Node> nodes, final int index,
            final Factory factory
        ) {
            Optional<ConversionResult> result = Optional.empty();
            if (nodes.get(index).getTypeName().equals("A")) {
                result = Optional.of(new ConversionResult(DraftNode.create("B"), 1));
            }
            return result;
        }
    }
}
