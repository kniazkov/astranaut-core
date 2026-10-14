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

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Edge cases for hierarchy queries, including repeated visits in shared graphs.
 * @since 0.0.1
 */
class TypeHierarchyTest {
    /**
     * Root type name.
     */
    private static final String ROOT = "Root";

    /**
     * Left type name.
     */
    private static final String LEFT = "Left";

    /**
     * Right type name.
     */
    private static final String RIGHT = "Right";

    @Test
    void isolatedTypesHaveOnlyThemselvesAsBases() {
        final AbstractType first = new SimpleAbstractType("First");
        final AbstractType second = new SimpleAbstractType("Second");
        Assertions.assertTrue(first.isSubtypeOf(first));
        Assertions.assertFalse(first.isSubtypeOf(second));
        Assertions.assertFalse(second.isSubtypeOf(first));
        Assertions.assertEquals(Optional.of(first), first.findCommonBaseType(first));
        Assertions.assertTrue(first.findCommonBaseType(second).isEmpty());
        Assertions.assertTrue(second.findCommonBaseType(first).isEmpty());
    }

    @Test
    void exhaustsDiamondWhenTargetIsAbsent() {
        final AbstractType root = new SimpleAbstractType(TypeHierarchyTest.ROOT);
        final AbstractType left = this.derived(TypeHierarchyTest.LEFT, root);
        final AbstractType right = this.derived(TypeHierarchyTest.RIGHT, root);
        final AbstractType bottom = this.derived("Bottom", left, right);
        final TypeHierarchy hierarchy = new TypeHierarchy(bottom);
        Assertions.assertFalse(hierarchy.contains(new SimpleAbstractType("Missing")));
        Assertions.assertTrue(hierarchy.contains(root));
        Assertions.assertTrue(hierarchy.contains(left));
        Assertions.assertTrue(hierarchy.contains(right));
        Assertions.assertTrue(hierarchy.contains(bottom));
    }

    @Test
    void commonBaseUsesSubtypeOrderRatherThanShortestDistance() {
        final AbstractType root = new SimpleAbstractType(TypeHierarchyTest.ROOT);
        final AbstractType specific = this.derived("Specific", root);
        final AbstractType left =
            this.derived(TypeHierarchyTest.LEFT, this.derived("Bridge", specific), root);
        final AbstractType right = this.derived(TypeHierarchyTest.RIGHT, specific, root);
        Assertions.assertEquals(Optional.of(specific), left.findCommonBaseType(right));
        Assertions.assertEquals(Optional.of(specific), right.findCommonBaseType(left));
        Assertions.assertEquals(Optional.of(root), specific.findCommonBaseType(root));
        Assertions.assertEquals(Optional.of(root), root.findCommonBaseType(specific));
    }

    @Test
    void rejectsThreeIncomparableCommonBasesAcrossDisconnectedRoots() {
        final AbstractType first = new SimpleAbstractType("First");
        final AbstractType second = new SimpleAbstractType("Second");
        final AbstractType third = new SimpleAbstractType("Third");
        final AbstractType left = this.derived(TypeHierarchyTest.LEFT, first, second, third);
        final AbstractType right = this.derived(TypeHierarchyTest.RIGHT, third, first, second);
        Assertions.assertTrue(left.findCommonBaseType(right).isEmpty());
        Assertions.assertTrue(right.findCommonBaseType(left).isEmpty());
    }

    @Test
    void matchesIndependentEquivalentDefinitionsByName() {
        final AbstractType first =
            this.derived("Base", new SimpleAbstractType(TypeHierarchyTest.ROOT));
        final AbstractType second =
            this.derived("Base", new SimpleAbstractType(TypeHierarchyTest.ROOT));
        final AbstractType left = this.derived(TypeHierarchyTest.LEFT, first);
        final AbstractType right = this.derived(TypeHierarchyTest.RIGHT, second);
        Assertions.assertNotSame(first, second);
        Assertions.assertTrue(left.isSubtypeOf(second));
        Assertions.assertTrue(right.isSubtypeOf(first));
        Assertions.assertEquals(Optional.of(first), left.findCommonBaseType(right));
        Assertions.assertEquals(Optional.of(second), right.findCommonBaseType(left));
    }

    @Test
    void distinguishesNamesWithIdenticalHashCodes() {
        final AbstractType first = new SimpleAbstractType("Aa");
        final AbstractType second = new SimpleAbstractType("BB");
        final AbstractType left = this.derived(TypeHierarchyTest.LEFT, first, second);
        final AbstractType right = this.derived(TypeHierarchyTest.RIGHT, second);
        Assertions.assertEquals(first.getName().hashCode(), second.getName().hashCode());
        Assertions.assertFalse(first.isSubtypeOf(second));
        Assertions.assertTrue(first.findCommonBaseType(second).isEmpty());
        Assertions.assertEquals(Optional.of(second), left.findCommonBaseType(right));
        Assertions.assertEquals(Optional.of(second), right.findCommonBaseType(left));
    }

    @Test
    void repeatedQueriesDoNotRetainState() {
        final AbstractType root = new SimpleAbstractType(TypeHierarchyTest.ROOT);
        final AbstractType left = this.derived(TypeHierarchyTest.LEFT, root);
        final AbstractType right = this.derived(TypeHierarchyTest.RIGHT, root);
        final TypeHierarchy hierarchy = new TypeHierarchy(left);
        Assertions.assertTrue(hierarchy.commonBase(new SimpleAbstractType("Other")).isEmpty());
        Assertions.assertEquals(Optional.of(root), hierarchy.commonBase(right));
        Assertions.assertEquals(Optional.of(left), hierarchy.commonBase(left));
        Assertions.assertFalse(hierarchy.contains(right));
        Assertions.assertTrue(hierarchy.contains(root));
        Assertions.assertEquals(Optional.of(root), hierarchy.commonBase(right));
    }

    @Test
    void rejectsNullQueryArguments() {
        final TypeHierarchy hierarchy =
            new TypeHierarchy(new SimpleAbstractType(TypeHierarchyTest.ROOT));
        Assertions.assertThrows(NullPointerException.class, () -> hierarchy.contains(null));
        Assertions.assertThrows(NullPointerException.class, () -> hierarchy.commonBase(null));
    }

    /**
     * Creates a type with the specified immediate bases.
     * @param name Type name
     * @param bases Immediate bases
     * @return Immutable type
     */
    private AbstractType derived(final String name, final AbstractType... bases) {
        return new SimpleAbstractType(name, Set.of(bases), Map.of());
    }
}
