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
import java.util.Map;

/**
 * A special pattern node that can substitute for any node of a suitable type.
 * @since 0.0.1
 */
public final class Hole extends NodeWithStructuralEquality
    implements PatternItem, PrototypeBasedNode {
    /**
     * Independent type descriptor.
     */
    private final Type type;

    /**
     * The prototype node, i.e., a node turned into hole.
     */
    private final Node prototype;

    /**
     * The number of the hole.
     */
    private final int number;

    /**
     * Hole properties.
     */
    private final Map<String, String> properties;

    /**
     * Constructor.
     * @param prototype Prototype of a hole, i.e., a node turned into hole
     * @param number The number of the hole
     */
    public Hole(final Node prototype, final int number) {
        this.prototype = prototype;
        this.type = new TypeImpl(prototype.getType());
        this.number = number;
        this.properties = Hole.snapshotProperties(prototype);
    }

    @Override
    public Type getType() {
        return this.type;
    }

    @Override
    public Fragment getFragment() {
        return EmptyFragment.INSTANCE;
    }

    @Override
    public String getData() {
        return String.format("#%d", this.number);
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public Node getChild(final int index) {
        return null;
    }

    @Override
    public Map<String, String> getProperties() {
        return this.properties;
    }

    /**
     * Return number of the hole.
     * @return The number
     */
    public int getNumber() {
        return this.number;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(this.getTypeName()).append("<#").append(this.number).append('>');
        return builder.toString();
    }

    @Override
    public Node getPrototype() {
        return this.prototype;
    }

    /**
     * Snapshots the prototype properties, overriding the display color.
     * @param prototype Original node
     * @return Immutable properties
     */
    private static Map<String, String> snapshotProperties(final Node prototype) {
        final Map<String, String> result = new HashMap<>(prototype.getProperties());
        result.put("color", "purple");
        return Map.copyOf(result);
    }

    /**
     * Type descriptor independent of node instances.
     * @since 0.0.1
     */
    private static final class TypeImpl extends TypeWithNameBasedEquality implements Type {
        /**
         * Immutable type definition.
         */
        private final Type original;

        /**
         * Creates the descriptor.
         * @param original Prototype type
         */
        private TypeImpl(final Type original) {
            this.original = original;
        }

        @Override
        public String getName() {
            return this.original.getName();
        }

        @Override
        public Map<String, String> getProperties() {
            return this.original.getProperties();
        }

        @Override
        public Builder createBuilder() {
            throw new UnsupportedOperationException();
        }
    }
}
