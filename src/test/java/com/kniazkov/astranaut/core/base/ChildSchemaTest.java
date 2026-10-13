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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Contracts of child schemas and their immutable snapshots.
 * @since 0.0.1
 */
class ChildSchemaTest {
    /**
     * Shared test type name.
     */
    private static final String TYPE_NAME = "Expression";

    @Test
    void describesThreeCardinalities() {
        final AbstractType expression = new SimpleAbstractType(ChildSchemaTest.TYPE_NAME);
        final List<ChildDescriptor> schema = ChildDescriptor.create()
            .required(expression).optional(expression).repeated(expression).build();
        Assertions.assertEquals(Cardinality.REQUIRED, schema.get(0).getCardinality());
        Assertions.assertEquals(Cardinality.OPTIONAL, schema.get(1).getCardinality());
        Assertions.assertEquals(Cardinality.REPEATED, schema.get(2).getCardinality());
        Assertions.assertSame(expression, schema.get(0).getType());
        Assertions.assertEquals("Expression*", schema.get(2).toString());
        Assertions.assertTrue(schema.get(2).getTag().isEmpty());
    }

    @Test
    void allowsTagsOnlyForSingleChildren() {
        final AbstractType type = new SimpleAbstractType(ChildSchemaTest.TYPE_NAME);
        final ChildDescriptor child = new ChildDescriptor(
            type,
            Optional.of("left"),
            Cardinality.OPTIONAL
        );
        Assertions.assertEquals(Optional.of("left"), child.getTag());
        Assertions.assertEquals("[left@Expression]", child.toString());
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new ChildDescriptor(type, Optional.of("left"), Cardinality.REPEATED)
        );
    }

    @Test
    void validatesTagUniqueness() {
        final AbstractType type = new SimpleAbstractType(ChildSchemaTest.TYPE_NAME);
        final ChildDescriptor first = new ChildDescriptor(
            type,
            Optional.of("same"),
            Cardinality.REQUIRED
        );
        final ChildDescriptor second = new ChildDescriptor(
            type,
            Optional.of("same"),
            Cardinality.OPTIONAL
        );
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> ChildDescriptor.copySchema(List.of(first, second))
        );
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> ChildDescriptor.create().add(first).add(second).build()
        );
        Assertions.assertEquals(
            2,
            ChildDescriptor.create().required(type).required(type).build().size()
        );
    }

    @Test
    void makesIndependentImmutableSnapshots() {
        final AbstractType type = new SimpleAbstractType(ChildSchemaTest.TYPE_NAME);
        final ChildDescriptor.Constructor builder = ChildDescriptor.create().required(type);
        final List<ChildDescriptor> first = builder.build();
        builder.optional(type);
        Assertions.assertEquals(1, first.size());
        Assertions.assertEquals(2, builder.build().size());
        final List<ChildDescriptor> input = new ArrayList<>(first);
        final List<ChildDescriptor> copied = ChildDescriptor.copySchema(input);
        input.clear();
        Assertions.assertEquals(first, copied);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> copied.clear());
    }

    @Test
    void rejectsNullComponents() {
        final AbstractType type = new SimpleAbstractType(ChildSchemaTest.TYPE_NAME);
        Assertions.assertThrows(NullPointerException.class, () -> new ChildDescriptor(null));
        Assertions.assertThrows(
            NullPointerException.class,
            () -> new ChildDescriptor(type, null, Cardinality.REQUIRED)
        );
        Assertions.assertThrows(
            NullPointerException.class,
            () -> new ChildDescriptor(type, Optional.empty(), null)
        );
        final List<ChildDescriptor> invalid = new ArrayList<>(2);
        invalid.add(null);
        Assertions.assertThrows(
            NullPointerException.class,
            () -> ChildDescriptor.copySchema(invalid)
        );
    }
}
