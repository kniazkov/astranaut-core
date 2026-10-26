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
import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.Hole;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.NodeOccurrence;
import com.kniazkov.astranaut.core.base.PatternNode;
import com.kniazkov.astranaut.core.base.PatternOperator;
import com.kniazkov.astranaut.core.base.SimpleAbstractType;
import com.kniazkov.astranaut.core.example.LittleTrees;
import com.kniazkov.astranaut.core.example.green.Expression;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Consumer contracts for typed holes, parent validation and tagged template edges.
 * @since 0.0.1
 */
final class PatternContractsTest {
    @Test
    void abstractConstraintAcceptsSubtypesWithoutCoercion() {
        final Node number = LittleTrees.createIntegerLiteral(42);
        final Hole hole = new Hole(Expression.TYPE, 0);
        final PatternMatcher matcher = new PatternMatcher();
        final Extracted capture = matcher.match(hole, number).orElseThrow();
        Assertions.assertSame(number, capture.getNodes(0).getFirst());
        Assertions.assertTrue(matcher.match(hole, DraftNode.create("Other")).isEmpty());
        Assertions.assertTrue(new Hole(new SimpleAbstractType("Expression"), 0).accepts(number));
        Assertions.assertNotEquals(hole, new Hole(0));
        Assertions.assertNotEquals(hole, new Hole(number, 0));
        final Builder rebuilt = hole.getType().createBuilder();
        Assertions.assertTrue(rebuilt.setData("2"));
        Assertions.assertTrue(((Hole) rebuilt.build()).accepts(number));
    }

    @Test
    void taggedTemplateKeepsBindingsAndUsesConcreteParentBuilder() {
        final Node first = LittleTrees.createIntegerLiteral(1);
        final Node second = LittleTrees.createIntegerLiteral(2);
        final Node original = LittleTrees.createAddition(first, second);
        final PatternNode template = new PatternNode(original).withHole(0, 0).withHole(1, 1);
        final Extracted capture = new Extracted();
        capture.addNode(0, second);
        capture.addNode(1, first);
        final TemplateSubstitutor engine = new TemplateSubstitutor();
        final Node result = engine.substitute(
            template, capture
        ).orElseThrow().getRoot().orElseThrow();
        Assertions.assertSame(second, result.getChild("left").orElseThrow());
        Assertions.assertSame(first, result.getChild("right").orElseThrow());
        Assertions.assertEquals(original.getChildTag(0), result.getChildTag(0));
        final Extracted incompatible = new Extracted();
        incompatible.addNode(0, DraftNode.create("Statement"));
        incompatible.addNode(1, first);
        Assertions.assertTrue(engine.substitute(template, incompatible).isEmpty());
        final Builder copy = template.getType().createBuilder();
        Assertions.assertTrue(copy.setChildren(template.getChildrenList()));
        Assertions.assertTrue(engine.substitute(copy.build(), incompatible).isEmpty());
        Assertions.assertTrue(engine.substitute(template, new Extracted()).isEmpty());
        capture.addNode(0, first);
        Assertions.assertTrue(engine.substitute(template, capture).isEmpty());
    }

    @Test
    void ordinaryReplacementKeepsTaggedPatternAndOperators() {
        final Node number = LittleTrees.createIntegerLiteral(1);
        final PatternNode pattern = new PatternNode(LittleTrees.createAddition(number, number));
        final Node changed = new OccurrenceReplacer().replace(
            new NodeOccurrence(pattern).getChild(0), new Hole(Expression.TYPE, 0)
        ).orElseThrow();
        Assertions.assertInstanceOf(PatternNode.class, changed);
        Assertions.assertInstanceOf(Hole.class, changed.getChild("left").orElseThrow());
        Assertions.assertEquals(pattern.getChildTag(0), changed.getChildTag(0));
        final Node expression = new PatternOperator(
            PatternOperator.Kind.AND, "", List.of(
                new Hole(0), new PatternOperator(PatternOperator.Kind.DATA_HOLE, "0", List.of())
        )
        );
        final Builder builder = expression.getType().createBuilder();
        Assertions.assertTrue(builder.setData(expression.getData()));
        Assertions.assertTrue(builder.setChildren(expression.getChildrenList()));
        Assertions.assertEquals(expression, builder.build());
        Assertions.assertInstanceOf(PatternOperator.class, builder.build());
    }

    @Test
    void forksAreIndependentAndCommitsCopyCollections() {
        final Extracted initial = new Extracted();
        final Node first = DraftNode.create("A");
        initial.addNode(0, first);
        initial.addData(0, "a");
        final Extracted fork = initial.fork();
        fork.addNode(0, DraftNode.create("B"));
        fork.addData(0, "b");
        Assertions.assertEquals(List.of(first), initial.getNodes(0));
        Assertions.assertEquals("a", initial.getData(0));
        initial.replaceWith(fork);
        fork.addData(0, "c");
        Assertions.assertEquals("ab", initial.getData(0));
        Assertions.assertEquals(2, initial.getNodes(0).size());
        initial.replaceWith(initial);
        Assertions.assertEquals("ab", initial.getData(0));
    }
}
