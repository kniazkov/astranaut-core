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
import java.util.Set;

/**
 * Immutable descriptor for an abstract type.
 * @since 0.0.1
 */
public final class SimpleAbstractType extends NamedType {
    /**
     * Type name.
     */
    private final String name;

    /**
     * Immediate bases.
     */
    private final Set<AbstractType> bases;

    /**
     * Own properties.
     */
    private final Map<String, String> properties;

    /**
     * Creates a type with no bases or properties.
     * @param name Unique name
     */
    public SimpleAbstractType(final String name) {
        this(name, Set.of(), Map.of());
    }

    /**
     * Creates a descriptor from immutable snapshots.
     * @param name Unique name
     * @param bases Immediate base types
     * @param properties Own properties
     * @throws IllegalArgumentException If a base has this type's name
     */
    @SuppressWarnings("PMD.ConstructorOnlyInitializesOrCallOtherConstructors")
    public SimpleAbstractType(final String name, final Set<AbstractType> bases,
        final Map<String, String> properties) {
        this.name = Objects.requireNonNull(name);
        this.bases = Set.copyOf(bases);
        this.properties = Map.copyOf(properties);
        if (this.bases.stream().anyMatch(base -> name.equals(base.getName()))) {
            throw new IllegalArgumentException("A type cannot inherit itself");
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Set<AbstractType> getBaseTypes() {
        return this.bases;
    }

    @Override
    public Map<String, String> getProperties() {
        return this.properties;
    }
}
