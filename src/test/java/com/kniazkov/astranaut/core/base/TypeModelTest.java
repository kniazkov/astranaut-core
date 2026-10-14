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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Contracts for independently created immutable type descriptors.
 * @since 0.0.1
 */
class TypeModelTest {
    /**
     * Shared test type name.
     */
    private static final String LITERAL = "Literal";

    @Test
    void identifiesTypesByNameAcrossImplementations() {
        final AbstractType first = new SimpleAbstractType(TypeModelTest.LITERAL);
        final AbstractType second = new TypeWithNameBasedEquality() {
            @Override
            public String getName() {
                return TypeModelTest.LITERAL;
            }
        };
        Assertions.assertEquals(first, second);
        Assertions.assertEquals(second, first);
        Assertions.assertEquals(first.hashCode(), second.hashCode());
        Assertions.assertEquals(1, new HashSet<>(List.of(first, second)).size());
        Assertions.assertNotEquals(first, new SimpleAbstractType("Expression"));
        Assertions.assertNotEquals(first, TypeModelTest.LITERAL);
        Assertions.assertNotEquals(first, null);
    }

    @Test
    void snapshotsBasesAndOwnProperties() {
        final AbstractType base = new SimpleAbstractType("Base", Set.of(), Map.of("base", "yes"));
        final Set<AbstractType> bases = new HashSet<>(Set.of(base));
        final Map<String, String> properties = new HashMap<>(Map.of("own", "yes"));
        final AbstractType type = new SimpleAbstractType("Derived", bases, properties);
        bases.clear();
        properties.clear();
        Assertions.assertEquals(Set.of(base), type.getBaseTypes());
        Assertions.assertEquals(Map.of("own", "yes"), type.getProperties());
        Assertions.assertThrows(
            UnsupportedOperationException.class,
            () -> type.getBaseTypes().clear()
        );
        Assertions.assertThrows(
            UnsupportedOperationException.class,
            () -> type.getProperties().clear()
        );
    }

    @Test
    void handlesDiamondAndMultipleInheritance() {
        final AbstractType root = new SimpleAbstractType("Root");
        final AbstractType left = this.derived("Left", root);
        final AbstractType right = this.derived("Right", root);
        final AbstractType bottom = this.derived("Bottom", left, right);
        Assertions.assertEquals(Set.of(left, right), bottom.getBaseTypes());
        Assertions.assertTrue(bottom.isSubtypeOf(root));
        Assertions.assertTrue(bottom.isSubtypeOf(right));
        Assertions.assertTrue(bottom.isSubtypeOf(new SimpleAbstractType("Bottom")));
        Assertions.assertFalse(root.isSubtypeOf(bottom));
        Assertions.assertEquals(Optional.of(root), left.findCommonBaseType(right));
        Assertions.assertEquals(Optional.of(left), bottom.findCommonBaseType(left));
        Assertions.assertEquals(Optional.of(bottom), bottom.findCommonBaseType(bottom));
    }

    @Test
    void rejectsAmbiguousCommonBaseEvenWithCommonRoot() {
        final AbstractType root = new SimpleAbstractType("Root");
        final AbstractType left = this.derived("Left", root);
        final AbstractType right = this.derived("Right", root);
        final AbstractType first = this.derived("First", left, right);
        final AbstractType second = this.derived("Second", right, left);
        Assertions.assertTrue(first.findCommonBaseType(second).isEmpty());
        Assertions.assertTrue(second.findCommonBaseType(first).isEmpty());
        Assertions.assertTrue(
            first.findCommonBaseType(new SimpleAbstractType("Unrelated")).isEmpty()
        );
    }

    @Test
    void removesTransitiveCandidatesWithRedundantBaseEdges() {
        final AbstractType root = new SimpleAbstractType("Root");
        final AbstractType middle = this.derived("Middle", root);
        final AbstractType close = this.derived("Close", middle);
        final AbstractType first = this.derived("First", close, root);
        final AbstractType second = this.derived("Second", middle, close);
        Assertions.assertEquals(Optional.of(close), first.findCommonBaseType(second));
        Assertions.assertEquals(Optional.of(close), second.findCommonBaseType(first));
    }

    @Test
    void traversesDeepHierarchyWithoutJavaRecursion() {
        final AbstractType root = new SimpleAbstractType("Root");
        AbstractType current = root;
        for (int index = 0; index < 30_000; index = index + 1) {
            current = this.derived(String.format("Level%d", index), current);
        }
        final AbstractType branch = this.derived("Branch", current);
        Assertions.assertTrue(branch.isSubtypeOf(root));
        Assertions.assertEquals(Optional.of(current), branch.findCommonBaseType(current));
    }

    @Test
    void allowsConcreteBasesAndDistinguishesUnknownSchemaFromLeaf() {
        final Type leaf = new ConcreteType("Leaf");
        final AbstractType derived = this.derived("Derived", leaf);
        Assertions.assertTrue(derived.isSubtypeOf(leaf));
        Assertions.assertEquals(Optional.of(List.of()), leaf.getChildDescriptors());
        Assertions.assertTrue(DraftNode.create("Draft").getType().getChildDescriptors().isEmpty());
        Assertions.assertNotNull(leaf.createBuilder());
    }

    @Test
    void separatesNodeIdentityFromTypeIdentity() {
        final Node first = DraftNode.create(TypeModelTest.LITERAL, "first");
        final Node second = DraftNode.create(TypeModelTest.LITERAL, "second");
        Assertions.assertFalse(first instanceof AbstractType);
        Assertions.assertEquals(first.getType(), second.getType());
        Assertions.assertNotEquals(first, second);
        Assertions.assertFalse(first.deepCompare(second));
    }

    @Test
    void rejectsNullDefinitionPartsAndSelfBase() {
        Assertions.assertThrows(NullPointerException.class, () -> new SimpleAbstractType(null));
        Assertions.assertThrows(
            NullPointerException.class,
            () -> new SimpleAbstractType("Name", null, Map.of())
        );
        Assertions.assertThrows(
            NullPointerException.class,
            () -> new SimpleAbstractType("Name", Set.of(), null)
        );
        final AbstractType same = new SimpleAbstractType("Name");
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new SimpleAbstractType("Name", Set.of(same), Map.of())
        );
        Assertions.assertThrows(NullPointerException.class, () -> same.isSubtypeOf(null));
        Assertions.assertThrows(NullPointerException.class, () -> same.findCommonBaseType(null));
    }

    /**
     * Creates a derived test type.
     * @param name Name
     * @param bases Immediate bases
     * @return Type
     */
    private AbstractType derived(final String name, final AbstractType... bases) {
        return new SimpleAbstractType(name, Set.of(bases), Map.of());
    }

    /**
     * Concrete descriptor independent of SimpleAbstractType.
     * @since 0.0.1
     */
    private static final class ConcreteType extends TypeWithNameBasedEquality implements Type {
        /**
         * Name.
         */
        private final String name;

        /**
         * Creates a type.
         * @param name Name
         */
        ConcreteType(final String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public Optional<List<ChildDescriptor>> getChildDescriptors() {
            return Optional.of(ChildDescriptor.create().build());
        }

        @Override
        public Builder createBuilder() {
            final DraftNode.Constructor builder = new DraftNode.Constructor();
            builder.setName(this.name);
            return builder;
        }
    }
}
