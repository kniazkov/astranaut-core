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

import com.kniazkov.astranaut.core.base.Builder;
import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.Fragment;
import com.kniazkov.astranaut.core.base.Hole;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.PatternOperator;
import com.kniazkov.astranaut.core.base.Tree;
import com.kniazkov.astranaut.core.utils.parsing.StringSource;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Named occurrence and matched-span contracts of the common engine.
 * @since 0.0.1
 */
final class RuleBindingsTest {
    /**
     * Left binding.
     */
    private static final String LEFT_TAG = "left";

    /**
     * Right binding.
     */
    private static final String RIGHT_TAG = "right";

    @Test
    void rightOnlyOptionalPositionIsNotReallocatedToTheLeft() {
        final Builder builder = OptionalPair.TYPE.createBuilder();
        Assertions.assertTrue(
            builder.setChild(
            RuleBindingsTest.RIGHT_TAG, DraftNode.create(
            "Item<'old'>"
        )
        )
        );
        final Node source = builder.build();
        final Node result = new RuleEngine(
            List.of(
                new TemplateRule(
            DraftNode.create("Item<'old'>"), DraftNode.create("Item<'new'>")
        )
        )
        ).apply(new Tree(source)).getRoot().orElseThrow();
        Assertions.assertTrue(result.getChild(RuleBindingsTest.LEFT_TAG).isEmpty());
        Assertions.assertEquals(
            "new", result.getChild(
            RuleBindingsTest.RIGHT_TAG
        ).orElseThrow().getData()
        );
        Assertions.assertEquals(Optional.of(RuleBindingsTest.RIGHT_TAG), result.getChildTag(0));
    }

    @Test
    void sharedChildrenKeepDifferentTagsAndTagOnlyEditIsProgress() {
        final Node shared = DraftNode.create("Item");
        final Builder builder = OptionalPair.TYPE.createBuilder();
        Assertions.assertTrue(builder.setChild(RuleBindingsTest.LEFT_TAG, shared));
        Assertions.assertTrue(builder.setChild(RuleBindingsTest.RIGHT_TAG, shared));
        final Node source = builder.build();
        final Node result = new RuleEngine(
            List.of(
                new TemplateRule(
            shared, DraftNode.create("Item<'changed'>")
        )
        )
        ).apply(new Tree(source)).getRoot().orElseThrow();
        Assertions.assertEquals(Optional.of(RuleBindingsTest.LEFT_TAG), result.getChildTag(0));
        Assertions.assertEquals(Optional.of(RuleBindingsTest.RIGHT_TAG), result.getChildTag(1));
        Assertions.assertTrue(builder.removeChild(RuleBindingsTest.LEFT_TAG));
        final Node right = builder.build();
        final RewriteRule retag = (nodes, index) -> {
            Optional<Replacement> replacement = Optional.empty();
            if (nodes.get(index).getTypeName().equals("Item")) {
                replacement = Optional.of(
                    new Replacement(
                    1, List.of(
                        nodes.get(
                        index
                    )
                    ), Optional.of(
                        List.of(
                        Optional.of(
                        RuleBindingsTest.LEFT_TAG
                    )
                    )
                    )
                )
                );
            }
            return replacement;
        };
        final Node left = new RuleEngine(List.of(retag)).apply(new Tree(right))
            .getRoot().orElseThrow();
        Assertions.assertNotEquals(right, left);
        Assertions.assertSame(shared, left.getChild(RuleBindingsTest.LEFT_TAG).orElseThrow());
        Assertions.assertTrue(left.getChild(RuleBindingsTest.RIGHT_TAG).isEmpty());
    }

    @Test
    void fullMatchedSpanIncludesDiscardedSeparatorsButCapturesRetainProvenance() {
        final List<Node> chars = new StringSource("a+b").parseIntoList().stream()
            .<Node>map(node -> node).toList();
        final Node source = DraftNode.create("Root", "data", chars.toArray(Node[]::new));
        final Node pattern = new PatternOperator(
            PatternOperator.Kind.SEQUENCE, "", List.of(
                new Hole(
                0
            ), new PatternOperator(
                PatternOperator.Kind.DATA, "+", List.of()
            ), new Hole(
                0
            )
        )
        );
        final Node output = DraftNode.create("Pair", "", new Hole(0));
        final Node result = new RuleEngine(List.of(new TemplateRule(pattern, output)))
            .apply(new Tree(source)).getRoot().orElseThrow().getChild(0);
        Assertions.assertEquals("a+b", result.getFragment().getCode());
        Assertions.assertSame(chars.get(0), result.getChild(0));
        Assertions.assertSame(chars.get(2), result.getChild(1));
        Assertions.assertEquals("a", result.getChild(0).getFragment().getCode());
        Assertions.assertEquals("b", result.getChild(1).getFragment().getCode());
        final Fragment span = Fragment.fromNodes(chars);
        final DraftNode.Constructor parent = new DraftNode.Constructor();
        parent.setName("Outer");
        parent.setData("preserve");
        parent.setFragment(span);
        parent.setChildren(List.of(source));
        final Node ancestor = new RuleEngine(List.of(new TemplateRule(pattern, output)))
            .apply(new Tree(parent.build())).getRoot().orElseThrow();
        Assertions.assertEquals("preserve", ancestor.getData());
        Assertions.assertEquals(span, ancestor.getFragment());
    }

    @Test
    void directionControlsOverlappingMatchesAndPriorityPreventsRewritingOutputs() {
        final Node pattern = new PatternOperator(
            PatternOperator.Kind.SEQUENCE, "", List.of(
            DraftNode.create("A"), DraftNode.create("A")
        )
        );
        final Tree input = Tree.createDraft("Root(A,A,A)");
        final Node output = DraftNode.create("B");
        final Tree forward = new RuleEngine(List.of(new TemplateRule(pattern, output)))
            .apply(input);
        final Tree reverse = new RuleEngine(List.of(new TemplateRule(pattern, output, true)))
            .apply(input);
        Assertions.assertTrue(forward.deepCompare(Tree.createDraft("Root(B,A)")));
        Assertions.assertTrue(reverse.deepCompare(Tree.createDraft("Root(A,B)")));
        final Tree prioritized = new RuleEngine(
            List.of(
                new TemplateRule(DraftNode.create("A"), DraftNode.create("B")),
                new TemplateRule(DraftNode.create("B"), DraftNode.create("C")),
                new TemplateRule(DraftNode.create("A"), DraftNode.create("D"))
        )
        ).apply(Tree.createDraft("Root(A,A)"));
        Assertions.assertTrue(prioritized.deepCompare(Tree.createDraft("Root(B,B)")));
    }
}
