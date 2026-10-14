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
     * Allowed type shared by the tests.
     */
    private static final AbstractType TYPE = new SimpleAbstractType("Expression");

    @Test
    void describesAllOverloadsInOrder() {
        final List<ChildDescriptor> schema = ChildDescriptor.create()
            .required(ChildSchemaTest.TYPE)
            .required(ChildSchemaTest.TYPE, "left")
            .required(ChildSchemaTest.TYPE, Optional.of("right"))
            .required(ChildSchemaTest.TYPE, Optional.empty())
            .optional(ChildSchemaTest.TYPE)
            .optional(ChildSchemaTest.TYPE, "first")
            .optional(ChildSchemaTest.TYPE, Optional.of("second"))
            .optional(ChildSchemaTest.TYPE, Optional.empty())
            .repeated(ChildSchemaTest.TYPE)
            .build();
        Assertions.assertEquals(9, schema.size());
        Assertions.assertEquals(
            List.of(
                "Expression", "left@Expression", "right@Expression", "Expression",
                "[Expression]", "[first@Expression]", "[second@Expression]",
                "[Expression]", "Expression*"
            ),
            schema.stream().map(ChildDescriptor::toString).toList()
        );
        for (int index = 0; index < schema.size(); index = index + 1) {
            final ChildDescriptor descriptor = schema.get(index);
            Assertions.assertSame(ChildSchemaTest.TYPE, descriptor.getType());
            final Cardinality cardinality;
            if (index < 4) {
                cardinality = Cardinality.REQUIRED;
            } else if (index < 8) {
                cardinality = Cardinality.OPTIONAL;
            } else {
                cardinality = Cardinality.REPEATED;
            }
            Assertions.assertEquals(cardinality, descriptor.getCardinality());
        }
        Assertions.assertEquals(Optional.of("left"), schema.get(1).getTag());
        Assertions.assertTrue(schema.getLast().getTag().isEmpty());
    }

    @Test
    void validatesTagUniquenessAcrossTypesAndCardinalities() {
        final ChildDescriptor.Constructor builder = ChildDescriptor.create()
            .required(ChildSchemaTest.TYPE, "same")
            .optional(new SimpleAbstractType("Statement"), Optional.of("same"));
        Assertions.assertThrows(IllegalArgumentException.class, builder::build);
        final ChildDescriptor child = ChildDescriptor.create()
            .required(ChildSchemaTest.TYPE, "shared")
            .build().getFirst();
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> ChildDescriptor.create().add(child).add(child).build()
        );
    }

    @Test
    void makesIndependentImmutableSnapshots() {
        final ChildDescriptor.Constructor builder = ChildDescriptor.create();
        final List<ChildDescriptor> empty = builder.build();
        builder.required(ChildSchemaTest.TYPE);
        final List<ChildDescriptor> first = builder.build();
        builder.optional(ChildSchemaTest.TYPE);
        Assertions.assertTrue(empty.isEmpty());
        Assertions.assertEquals(1, first.size());
        Assertions.assertEquals(2, builder.build().size());
        Assertions.assertThrows(UnsupportedOperationException.class, first::clear);
        final List<ChildDescriptor> reused = ChildDescriptor.create().add(first.getFirst()).build();
        Assertions.assertEquals(first, reused);
    }

    @Test
    void rejectsNullComponentsWithoutChangingBuilder() {
        final ChildDescriptor.Constructor builder = ChildDescriptor.create();
        Assertions.assertThrows(NullPointerException.class, () -> builder.required(null));
        Assertions.assertThrows(NullPointerException.class, () -> builder.optional(null));
        Assertions.assertThrows(NullPointerException.class, () -> builder.repeated(null));
        Assertions.assertThrows(NullPointerException.class, () -> builder.add(null));
        Assertions.assertThrows(
            NullPointerException.class, () -> builder.required(ChildSchemaTest.TYPE, (String) null)
        );
        Assertions.assertThrows(
            NullPointerException.class, () -> builder.optional(ChildSchemaTest.TYPE, (String) null)
        );
        Assertions.assertThrows(
            NullPointerException.class,
            () -> builder.required(ChildSchemaTest.TYPE, (Optional<String>) null)
        );
        Assertions.assertThrows(
            NullPointerException.class,
            () -> builder.optional(ChildSchemaTest.TYPE, (Optional<String>) null)
        );
        Assertions.assertTrue(builder.build().isEmpty());
    }
}
