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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * An immutable, globally named type, which need not be instantiable.
 * All definitions of the same name must agree. Implementations must provide
 * name-based equals and hashCode, for example by extending {@link TypeWithNameBasedEquality}.
 * The inheritance graph must be acyclic and safely published.
 * @since 0.0.1
 */
public interface AbstractType {
    /**
     * Returns the globally unique name.
     * @return Non-null type name
     */
    String getName();

    /**
     * Returns immediate, equally ranked base types, excluding this type.
     * @return Immutable set of direct bases
     */
    default Set<AbstractType> getBaseTypes() {
        return Set.of();
    }

    /**
     * Returns only this type's own properties, without inherited entries.
     * @return Immutable properties
     */
    default Map<String, String> getProperties() {
        return Map.of();
    }

    /**
     * Tests transitive inheritance, including equality by name.
     * @param other Requested base type
     * @return Whether this type is a subtype of the argument
     */
    default boolean isSubtypeOf(final AbstractType other) {
        return new TypeHierarchy(this).contains(Objects.requireNonNull(other).getName());
    }

    /**
     * Finds the unique most specific common base, including the operands.
     * Unrelated types and incomparable most specific candidates yield empty.
     * @param other Other type
     * @return Unique common base, if one exists
     */
    default Optional<AbstractType> findCommonBaseType(final AbstractType other) {
        return new TypeHierarchy(this).commonBase(other);
    }
}
