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
package com.kniazkov.astranaut.core.algorithms.normalization;

import com.kniazkov.astranaut.core.base.Builder;
import com.kniazkov.astranaut.core.base.Fragment;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.NodeWithStructuralEquality;
import com.kniazkov.astranaut.core.base.Type;
import com.kniazkov.astranaut.core.base.TypeWithNameBasedEquality;
import java.util.List;

/**
 * Property extracted from the original node.
 * @since 0.0.1
 */
public final class Property extends NodeWithStructuralEquality implements Node {
    /**
     * Property type.
     */
    private final Type type;

    /**
     * Property value.
     */
    private final String value;

    /**
     * Constructor.
     * @param name Property name
     * @param value Property value
     */
    public Property(final String name, final String value) {
        this.type = new TypeImpl(name);
        this.value = java.util.Objects.requireNonNull(value);
    }

    /**
     * Returns the original property key.
     * @return Key, independently of the reserved service type name
     */
    public String getName() {
        return ((TypeImpl) this.type).name;
    }

    @Override
    public Type getType() {
        return this.type;
    }

    @Override
    public String getData() {
        return this.value;
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public Node getChild(final int index) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public String toString() {
        return Node.toString(this);
    }

    /**
     * Builder of the property with the specified name.
     * @since 0.0.1
     */
    private static final class PropertyBuilder implements Builder {
        /**
         * Property name.
         */
        private final String name;

        /**
         * Property value.
         */
        private String value;

        /**
         * Constructor.
         * @param name Name
         */
        private PropertyBuilder(final String name) {
            this.name = java.util.Objects.requireNonNull(name);
            this.value = "";
        }

        @SuppressWarnings("PMD.UncommentedEmptyMethodBody")
        @Override
        public void setFragment(final Fragment fragment) {
        }

        @Override
        public boolean setData(final String str) {
            this.value = java.util.Objects.requireNonNull(str);
            return true;
        }

        @Override
        public boolean setChildren(final List<Node> list) {
            return list.isEmpty();
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Node build() {
            return new Property(this.name, this.value);
        }
    }

    /**
     * Type descriptor independent of node instances.
     * @since 0.0.1
     */
    private static final class TypeImpl extends TypeWithNameBasedEquality implements Type {
        /**
         * Immutable type definition.
         */
        private final String name;

        /**
         * Creates the descriptor.
         * @param name Type name
         */
        private TypeImpl(final String name) {
            this.name = java.util.Objects.requireNonNull(name);
        }

        @Override
        public String getName() {
            return "AstranautProperty".concat(NormalizationType.encode(this.name));
        }

        @Override
        public Builder createBuilder() {
            return new PropertyBuilder(this.name);
        }
    }
}
