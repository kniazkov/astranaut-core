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
import com.kniazkov.astranaut.core.base.Builder;
import com.kniazkov.astranaut.core.base.Delete;
import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.Hole;
import com.kniazkov.astranaut.core.base.Insert;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.NodeOccurrence;
import com.kniazkov.astranaut.core.base.PatternNode;
import com.kniazkov.astranaut.core.base.PatternOperator;
import com.kniazkov.astranaut.core.base.Replace;
import com.kniazkov.astranaut.core.base.Tree;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Immutable substitution, sharing and ordinary tree-operation contracts.
 * @since 0.0.1
 */
final class TemplateSubstitutorTest {
    @Test
    void rootSplicingDistinguishesEmptyFromRefused() {
        final TemplateSubstitutor engine = new TemplateSubstitutor();
        final Extracted captures = new Extracted();
        final Hole hole = new Hole(1);
        Assertions.assertTrue(engine.substitute(hole, captures).orElseThrow().getRoot().isEmpty());
        final Node first = DraftNode.create("A");
        captures.addNode(1, first);
        Assertions.assertSame(
            first, engine.substitute(hole, captures).orElseThrow()
            .getRoot().orElseThrow()
        );
        captures.addNode(1, DraftNode.create("B"));
        Assertions.assertTrue(engine.substitute(hole, captures).isEmpty());
    }

    @Test
    void repeatedOutputSlotsPreserveCapturedObjects() {
        final Node child = DraftNode.create("Leaf<'value'>");
        final Extracted captures = new Extracted();
        captures.addNode(1, child);
        final PatternNode template = new PatternNode(DraftNode.create("Root(A,B)"))
            .withHole(0, 1).withHole(1, 1);
        final Node result = new TemplateSubstitutor().substitute(template, captures)
            .orElseThrow().getRoot().orElseThrow();
        Assertions.assertSame(child, result.getChild(0));
        Assertions.assertSame(child, result.getChild(1));
        Assertions.assertSame(child.getFragment(), result.getChild(0).getFragment());
        Assertions.assertEquals(2, result.getChildCount());
    }

    @Test
    void wholeSplicedSequenceReachesParentBuilder() {
        final PatternNode template = new PatternNode(DraftNode.create("Root(A)"))
            .withHole(0, 2);
        final Extracted captures = new Extracted();
        captures.addNode(2, DraftNode.create("B"));
        captures.addNode(2, DraftNode.create("C"));
        final Node result = new TemplateSubstitutor().substitute(template, captures)
            .orElseThrow().getRoot().orElseThrow();
        Assertions.assertEquals(DraftNode.create("Root(B,C)"), result);
        final Node constrained = new Insert(new Hole(2));
        Assertions.assertTrue(
            new TemplateSubstitutor().substitute(
            constrained, captures
        ).isEmpty()
        );
    }

    @Test
    void dataExpressionsConcatenateIndependentSlotsAndLiterals() {
        final Extracted captures = new Extracted();
        captures.addData(1, "hello");
        captures.addData(1, " world");
        final Node template = new PatternOperator(
            PatternOperator.Kind.WITH_DATA, "", List.of(
            DraftNode.create("Text"),
                new PatternOperator(PatternOperator.Kind.DATA_HOLE, "1", List.of()),
                new PatternOperator(PatternOperator.Kind.DATA, "!", List.of())
        )
        );
        Assertions.assertEquals(
            "hello world!", new TemplateSubstitutor()
            .substitute(template, captures).orElseThrow().getRoot().orElseThrow().getData()
        );
    }

    @Test
    void actionsRemainNodesAndCanBeReconstructed() {
        final Node first = DraftNode.create("A");
        final Node second = DraftNode.create("B");
        for (final Node node : List.of(
            new Insert(first), new Delete(first),
            new Replace(first, second), new Hole(first, 3)
        )) {
            final Builder builder = node.getType().createBuilder();
            Assertions.assertTrue(builder.setData(node.getData()));
            Assertions.assertTrue(builder.setChildren(node.getChildrenList()));
            Assertions.assertEquals(node, builder.build());
            Assertions.assertEquals(node.hashCode(), builder.build().hashCode());
        }
        final Node action = new Replace(first, new Hole(3));
        final Node changed = new OccurrenceReplacer().replace(
            new NodeOccurrence(action).getChild(1), new Hole(4)
        ).orElseThrow();
        Assertions.assertInstanceOf(Replace.class, changed);
        Assertions.assertInstanceOf(Hole.class, changed.getChild(1));
        Assertions.assertEquals("3", action.getChild(1).getData());
    }

    @Test
    void operationsExecuteWithoutRebuildingDeletedInputs() {
        final TemplateSubstitutor engine = new TemplateSubstitutor();
        final Node before = DraftNode.create("Before");
        final Node after = DraftNode.create("After");
        Assertions.assertTrue(
            engine.substitute(new Delete(before), new Extracted())
            .orElseThrow().getRoot().isEmpty()
        );
        Assertions.assertEquals(
            after, engine.substitute(
                new Replace(
            before, after
        ), new Extracted()
        )
            .orElseThrow().getRoot().orElseThrow()
        );
        Assertions.assertEquals(
            after, engine.substitute(new Insert(after), new Extracted())
            .orElseThrow().getRoot().orElseThrow()
        );
    }

    @Test
    void sharedTemplateDependenciesAreBuiltOnlyOnce() {
        final Node shared = DraftNode.create("Leaf");
        final DraftNode.Constructor builder = new DraftNode.Constructor();
        builder.setName("Branch");
        builder.setChildren(List.of(shared));
        final Node branch = builder.build();
        builder.setName("Root");
        builder.setChildren(List.of(branch, shared));
        final PatternNode pattern = new PatternNode(builder.build());
        Assertions.assertSame(pattern.getChild(0).getChild(0), pattern.getChild(1));
        final Node result = new TemplateSubstitutor().substitute(pattern, new Extracted())
            .orElseThrow().getRoot().orElseThrow();
        Assertions.assertSame(result.getChild(0).getChild(0), result.getChild(1));
    }

    @Test
    void publishedMiningPatternsAreImmutable() {
        final Node source = DraftNode.create("Root(Leaf)");
        final PatternBuilder builder = new PatternBuilder(new Tree(source));
        final Node initial = builder.getPattern().getRoot().orElseThrow();
        final int hash = initial.hashCode();
        Assertions.assertTrue(builder.makeHole(source.getChild(0), 1));
        final Node edited = builder.getPattern().getRoot().orElseThrow();
        Assertions.assertEquals(source, initial);
        Assertions.assertEquals(hash, initial.hashCode());
        Assertions.assertInstanceOf(Hole.class, edited.getChild(0));
        Assertions.assertNotEquals(initial, edited);
    }
}
