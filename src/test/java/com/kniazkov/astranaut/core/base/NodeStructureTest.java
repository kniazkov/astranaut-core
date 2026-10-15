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

import com.kniazkov.astranaut.core.algorithms.LabeledTreeBuilder;
import com.kniazkov.astranaut.core.algorithms.hash.AbsoluteHash;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Structural equality laws and deliberate hash collisions.
 * @since 0.0.1
 */
class NodeStructureTest {
    @Test
    void stringHashCollisionDoesNotProveEquality() {
        final Node first = DraftNode.create("Literal<\"Aa\">");
        final Node second = DraftNode.create("Literal<\"BB\">");
        Assertions.assertEquals(first.getLocalHash(), second.getLocalHash());
        Assertions.assertEquals(first.hashCode(), second.hashCode());
        Assertions.assertNotEquals(first, second);
        Assertions.assertFalse(first.deepCompare(second));
        Assertions.assertEquals(2, new HashSet<>(List.of(first, second)).size());
    }

    @Test
    @SuppressWarnings({"PMD.EqualsNull", "PMD.SimplifiableTestAssertion"})
    void implementsEqualityLawsAcrossIndependentClasses() {
        final Node first = DraftNode.create("Root(A,B)");
        final Node second = DraftNode.create("Root(A,B)");
        final Node third = new StructuralNodeFixture(
            second, second.getChildrenList(), List.of(Optional.empty(), Optional.empty())
        );
        Assertions.assertEquals(first, first);
        Assertions.assertEquals(first, second);
        Assertions.assertEquals(second, third);
        Assertions.assertEquals(third, first);
        Assertions.assertEquals(first, third);
        Assertions.assertEquals(second, first);
        Assertions.assertEquals(first.hashCode(), third.hashCode());
        Assertions.assertFalse(first.equals(null));
        Assertions.assertFalse(first.equals("Root"));
        Assertions.assertFalse(NodeStructure.equal(first, null));
        Assertions.assertEquals(1, new HashSet<>(List.of(first, second, third)).size());
        Assertions.assertEquals(new AbsoluteHash().calculate(first), first.hashCode());
    }

    @Test
    void comparesChildOrderAndType() {
        final Node original = DraftNode.create("Root(A,B)");
        Assertions.assertNotEquals(original, DraftNode.create("Root(B,A)"));
        Assertions.assertNotEquals(original, DraftNode.create("Other(A,B)"));
        Assertions.assertNotEquals(original, DraftNode.create("Root(A)"));
    }

    @Test
    void occurrenceTagsAndTheirAbsenceAreSignificant() {
        final Node child = DraftNode.create("Child");
        final Node root = DraftNode.create("Root");
        final List<Node> children = List.of(child, child);
        final Node first = new StructuralNodeFixture(
            root, children, List.of(Optional.of("left"), Optional.of("right"))
        );
        final Node reversed = new StructuralNodeFixture(
            root, children, List.of(Optional.of("right"), Optional.of("left"))
        );
        final Node absent = new StructuralNodeFixture(
            root, children, List.of(Optional.empty(), Optional.of("right"))
        );
        Assertions.assertNotEquals(first, reversed);
        Assertions.assertNotEquals(first, absent);
        Assertions.assertNotEquals(first.hashCode(), reversed.hashCode());
        Assertions.assertEquals(first.hashCode(), new AbsoluteHash().calculate(first));
        final Node empty = new StructuralNodeFixture(
            root, children, List.of(Optional.of(""), Optional.of("right"))
        );
        Assertions.assertNotEquals(absent, empty);
        Assertions.assertNotEquals(absent.hashCode(), empty.hashCode());
    }

    @Test
    void propertiesAndSourceFragmentsDoNotAffectEquality() {
        final DraftNode.Constructor builder = new DraftNode.Constructor();
        builder.setName("Leaf");
        final Node first = builder.build();
        final Source source = (begin, end) -> "source";
        builder.setFragment(
            new DefaultFragment(
                new DefaultPosition(source, 1, 1), new DefaultPosition(source, 1, 3)
            )
        );
        final Node second = builder.build();
        final Node third = new LabeledTreeBuilder(second)
            .build(Set.of(second), "color", "red").getRoot();
        Assertions.assertNotEquals(first.getFragment(), second.getFragment());
        Assertions.assertNotEquals(first.getProperties(), third.getProperties());
        Assertions.assertEquals(first, second);
        Assertions.assertEquals(second, third);
        Assertions.assertEquals(first.hashCode(), third.hashCode());
    }

    @Test
    void repeatedHashCallsDoNotRetainMutableWrapperState() {
        final Node original = DraftNode.create("Root(Leaf)");
        final PatternNode pattern = new PatternNode(original);
        final AbsoluteHash algorithm = new AbsoluteHash();
        final int before = algorithm.calculate(pattern);
        Assertions.assertTrue(pattern.makeHole(original.getChild(0), 7));
        final int after = algorithm.calculate(pattern);
        Assertions.assertNotEquals(before, after);
        Assertions.assertEquals(pattern.hashCode(), after);
        Assertions.assertNotEquals(original, pattern);
    }
}
