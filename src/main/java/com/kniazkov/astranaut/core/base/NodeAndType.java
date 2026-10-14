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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Convenience base for nodes supplying a name and a builder factory.
 * The descriptor is a separate object: node identity must not be confused
 * with name-based type identity. Implementations must keep their definition stable.
 * @since 0.0.1
 */
public abstract class NodeAndType implements Node {
    @Override
    public final Type getType() {
        return new Descriptor(this);
    }

    @Override
    public final String getTypeName() {
        return this.getName();
    }

    /**
     * Returns the globally unique type name.
     * @return Type name
     */
    public abstract String getName();

    /**
     * Creates a builder for this type.
     * @return Builder
     */
    public abstract Builder createBuilder();

    /**
     * Returns the optional complete child schema.
     * @return Schema or empty if unknown
     * @checkstyle NonStaticMethodCheck (4 lines)
     */
    public Optional<List<ChildDescriptor>> getChildDescriptors() {
        return Optional.empty();
    }

    /**
     * Returns immediate base types.
     * @return Immutable bases
     * @checkstyle NonStaticMethodCheck (4 lines)
     */
    public Set<AbstractType> getBaseTypes() {
        return Set.of();
    }

    @Override
    public Map<String, String> getProperties() {
        return Map.of();
    }

    /**
     * A separate immutable snapshot of the type definition.
     * @since 0.0.1
     */
    private static final class Descriptor extends NamedType implements Type {
        /**
         * Builder provider.
         */
        private final NodeAndType provider;

        /**
         * Name snapshot.
         */
        private final String name;

        /**
         * Base snapshot.
         */
        private final Set<AbstractType> bases;

        /**
         * Own properties.
         */
        private final Map<String, String> properties;

        /**
         * Optional schema snapshot.
         */
        private final Optional<List<ChildDescriptor>> schema;

        /**
         * Takes a snapshot of the definition.
         * @param provider Node providing the definition and factory
         */
        Descriptor(final NodeAndType provider) {
            this.provider = provider;
            this.name = provider.getName();
            this.bases = Set.copyOf(provider.getBaseTypes());
            this.properties = Map.copyOf(provider.getProperties());
            this.schema = provider.getChildDescriptors().map(ChildDescriptor::copySchema);
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

        @Override
        public Optional<List<ChildDescriptor>> getChildDescriptors() {
            return this.schema;
        }

        @Override
        public Builder createBuilder() {
            return this.provider.createBuilder();
        }
    }
}
