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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable child constraint: allowed type, optional tag and cardinality.
 * @since 0.0.1
 */
public final class ChildDescriptor {
    /**
     * Allowed type, including its subtypes.
     */
    private final AbstractType type;

    /**
     * Optional unique tag.
     */
    private final Optional<String> tag;

    /**
     * Allowed number of children.
     */
    private final Cardinality cardinality;

    /**
     * Creates a descriptor.
     * @param type Allowed type
     * @param tag Optional unique tag
     * @param cardinality Allowed number of children
     * @throws IllegalArgumentException If a repeated descriptor has a tag
     */
    @SuppressWarnings("PMD.ConstructorOnlyInitializesOrCallOtherConstructors")
    private ChildDescriptor(final AbstractType type, final Optional<String> tag,
        final Cardinality cardinality) {
        this.type = Objects.requireNonNull(type);
        this.tag = Objects.requireNonNull(tag);
        this.cardinality = Objects.requireNonNull(cardinality);
        if (cardinality == Cardinality.REPEATED && tag.isPresent()) {
            throw new IllegalArgumentException("Repeated children cannot have a tag");
        }
    }

    /**
     * Returns the allowed type.
     * @return Allowed abstract or concrete type
     */
    public AbstractType getType() {
        return this.type;
    }

    /**
     * Returns the optional tag.
     * @return Tag
     */
    public Optional<String> getTag() {
        return this.tag;
    }

    /**
     * Returns the cardinality.
     * @return Cardinality
     */
    public Cardinality getCardinality() {
        return this.cardinality;
    }

    /**
     * Creates a schema builder.
     * @return Mutable builder
     */
    @SuppressWarnings("PMD.ProhibitPublicStaticMethods")
    public static Constructor create() {
        return new Constructor();
    }

    @Override
    public String toString() {
        final String name = String.format(
            "%s%s",
            this.tag.map(value -> String.format("%s@", value)).orElse(""),
            this.type.getName()
        );
        return switch (this.cardinality) {
            case REQUIRED -> name;
            case OPTIONAL -> String.format("[%s]", name);
            case REPEATED -> String.format("%s*", name);
        };
    }

    /**
     * Creates a validated immutable schema snapshot.
     * @param descriptors Complete ordered schema
     * @return Snapshot with unique tags
     * @throws IllegalArgumentException If tags are duplicated
     */
    private static List<ChildDescriptor> copySchema(final List<ChildDescriptor> descriptors) {
        final List<ChildDescriptor> result = List.copyOf(descriptors);
        final Set<String> tags = new HashSet<>();
        for (final ChildDescriptor descriptor : result) {
            if (descriptor.tag.isPresent() && !tags.add(descriptor.tag.orElseThrow())) {
                throw new IllegalArgumentException("Duplicate child tag");
            }
        }
        return result;
    }

    /**
     * Mutable builder producing independent schema snapshots.
     * @since 0.0.1
     */
    public static final class Constructor {
        /**
         * Descriptors accumulated so far.
         */
        private final List<ChildDescriptor> list;

        /**
         * Constructor.
         */
        private Constructor() {
            this.list = new ArrayList<>(2);
        }

        /**
         * Adds an untagged required child.
         * @param type Allowed type
         * @return This builder
         */
        public Constructor required(final AbstractType type) {
            return this.required(type, Optional.empty());
        }

        /**
         * Adds a tagged required child.
         * @param type Allowed type
         * @param tag Child tag
         * @return This builder
         */
        public Constructor required(final AbstractType type, final String tag) {
            return this.required(type, Optional.of(tag));
        }

        /**
         * Adds a required child with an optional tag.
         * @param type Allowed type
         * @param tag Optional child tag
         * @return This builder
         */
        public Constructor required(final AbstractType type, final Optional<String> tag) {
            return this.add(new ChildDescriptor(type, tag, Cardinality.REQUIRED));
        }

        /**
         * Adds an untagged optional child.
         * @param type Allowed type
         * @return This builder
         */
        public Constructor optional(final AbstractType type) {
            return this.optional(type, Optional.empty());
        }

        /**
         * Adds a tagged optional child.
         * @param type Allowed type
         * @param tag Child tag
         * @return This builder
         */
        public Constructor optional(final AbstractType type, final String tag) {
            return this.optional(type, Optional.of(tag));
        }

        /**
         * Adds an optional child with an optional tag.
         * @param type Allowed type
         * @param tag Optional child tag
         * @return This builder
         */
        public Constructor optional(final AbstractType type, final Optional<String> tag) {
            return this.add(new ChildDescriptor(type, tag, Cardinality.OPTIONAL));
        }

        /**
         * Adds an untagged repeated child.
         * @param type Allowed type
         * @return This builder
         */
        public Constructor repeated(final AbstractType type) {
            return this.add(new ChildDescriptor(type, Optional.empty(), Cardinality.REPEATED));
        }

        /**
         * Adds an explicitly constructed descriptor.
         * @param descriptor Child descriptor
         * @return This builder
         */
        public Constructor add(final ChildDescriptor descriptor) {
            this.list.add(Objects.requireNonNull(descriptor));
            return this;
        }

        /**
         * Builds a validated immutable snapshot.
         * @return Complete schema
         */
        public List<ChildDescriptor> build() {
            return ChildDescriptor.copySchema(this.list);
        }
    }
}
